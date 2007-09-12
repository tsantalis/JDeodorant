package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.StatementExtractor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;

public class MoveMethodRefactoring implements Refactoring {
	private IFile sourceFile;
	private IFile targetFile;
	private CompilationUnit sourceCompilationUnit;
	private CompilationUnit targetCompilationUnit;
	private TypeDeclaration sourceTypeDeclaration;
	private TypeDeclaration targetTypeDeclaration;
	private MethodDeclaration sourceMethod;
	private ASTRewrite sourceRewriter;
	private ASTRewrite targetRewriter;
	private UndoRefactoring undoRefactoring;
	private Set<ITypeBinding> requiredTargetImportDeclarationSet;
	private String targetClassVariableName;
	private Set<String> additionalArgumentsAddedToMovedMethod;
	private Map<MethodInvocation, MethodDeclaration> additionalMethodsToBeMoved;
	private boolean leaveDelegate;
	
	public MoveMethodRefactoring(IFile sourceFile, IFile targetFile, CompilationUnit sourceCompilationUnit, CompilationUnit targetCompilationUnit, 
			TypeDeclaration sourceTypeDeclaration, TypeDeclaration targetTypeDeclaration, MethodDeclaration sourceMethod, Map<MethodInvocation, MethodDeclaration> additionalMethodsToBeMoved, boolean leaveDelegate) {
		this.sourceFile = sourceFile;
		this.targetFile = targetFile;
		this.sourceCompilationUnit = sourceCompilationUnit;
		this.targetCompilationUnit = targetCompilationUnit;
		this.sourceTypeDeclaration = sourceTypeDeclaration;
		this.targetTypeDeclaration = targetTypeDeclaration;
		this.sourceMethod = sourceMethod;
		this.undoRefactoring = new UndoRefactoring();
		this.requiredTargetImportDeclarationSet = new LinkedHashSet<ITypeBinding>();
		this.sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		this.targetRewriter = ASTRewrite.create(targetCompilationUnit.getAST());
		this.targetClassVariableName = null;
		this.additionalArgumentsAddedToMovedMethod = new LinkedHashSet<String>();
		this.additionalMethodsToBeMoved = additionalMethodsToBeMoved;
		this.leaveDelegate = leaveDelegate;
	}

	public UndoRefactoring getUndoRefactoring() {
		return undoRefactoring;
	}
	
	public void apply() {
		addRequiredTargetImportDeclarations();
		createMovedMethod();
		moveAdditionalMethods();
		modifyMovedMethodInvocationInTargetClass();
		ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
		ITextFileBuffer targetTextFileBuffer = bufferManager.getTextFileBuffer(targetFile.getFullPath(), LocationKind.IFILE);
		IDocument targetDocument = targetTextFileBuffer.getDocument();
		TextEdit targetEdit = targetRewriter.rewriteAST(targetDocument, null);
		try {
			UndoEdit undoEdit = targetEdit.apply(targetDocument, UndoEdit.CREATE_UNDO);
			undoRefactoring.put(targetFile, targetDocument, undoEdit);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		if(leaveDelegate) {
			addDelegationInSourceMethod();
		}
		else {
			removeSourceMethod();
		}
		modifyMovedMethodInvocationInSourceClass();
		ITextFileBuffer sourceTextFileBuffer = bufferManager.getTextFileBuffer(sourceFile.getFullPath(), LocationKind.IFILE);
		IDocument sourceDocument = sourceTextFileBuffer.getDocument();
		TextEdit sourceEdit = sourceRewriter.rewriteAST(sourceDocument, null);
		try {
			UndoEdit undoEdit = sourceEdit.apply(sourceDocument, UndoEdit.CREATE_UNDO);
			undoRefactoring.put(sourceFile, sourceDocument, undoEdit);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	private void addRequiredTargetImportDeclarations() {
		List<ITypeBinding> typeBindings = new ArrayList<ITypeBinding>();
		Type returnType = sourceMethod.getReturnType2();
		ITypeBinding returnTypeBinding = returnType.resolveBinding();
		if(!typeBindings.contains(returnTypeBinding))
			typeBindings.add(returnTypeBinding);
		
		List<SingleVariableDeclaration> parameters = sourceMethod.parameters();
		for(SingleVariableDeclaration parameter : parameters) {
			Type parameterType = parameter.getType();
			ITypeBinding parameterTypeBinding = parameterType.resolveBinding();
			if(!typeBindings.contains(parameterTypeBinding))
				typeBindings.add(parameterTypeBinding);			
		}
		
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> variableInstructions = expressionExtractor.getVariableInstructions(sourceMethod.getBody());
		for(Expression variableInstruction : variableInstructions) {
			SimpleName simpleName = (SimpleName)variableInstruction;
			IBinding binding = simpleName.resolveBinding();
			if(binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				ITypeBinding variableTypeBinding = variableBinding.getType();
				if(!typeBindings.contains(variableTypeBinding))
					typeBindings.add(variableTypeBinding);
				ITypeBinding declaringClassTypeBinding = variableBinding.getDeclaringClass();
				if(declaringClassTypeBinding != null && !typeBindings.contains(declaringClassTypeBinding))
					typeBindings.add(declaringClassTypeBinding);
			}
		}
		
		List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(sourceMethod.getBody());
		for(Expression expression : methodInvocations) {
			if(expression instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)expression;
				IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
				ITypeBinding declaringClassTypeBinding = methodBinding.getDeclaringClass();
				if(declaringClassTypeBinding != null && !typeBindings.contains(declaringClassTypeBinding))
					typeBindings.add(declaringClassTypeBinding);
			}
		}
		
		List<Expression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(sourceMethod.getBody());
		for(Expression expression : classInstanceCreations) {
			ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)expression;
			Type classInstanceCreationType = classInstanceCreation.getType();
			ITypeBinding classInstanceCreationTypeBinding = classInstanceCreationType.resolveBinding();
			if(!typeBindings.contains(classInstanceCreationTypeBinding))
				typeBindings.add(classInstanceCreationTypeBinding);
		}
		getSimpleTypeBindings(typeBindings);
		for(ITypeBinding typeBinding : requiredTargetImportDeclarationSet)
			addImportDeclaration(typeBinding);
	}

	private void getSimpleTypeBindings(List<ITypeBinding> typeBindings) {
		for(ITypeBinding typeBinding : typeBindings) {
			if(typeBinding.isPrimitive()) {
				
			}
			else if(typeBinding.isArray()) {
				ITypeBinding elementTypeBinding = typeBinding.getElementType();
				List<ITypeBinding> typeBindingList = new ArrayList<ITypeBinding>();
				typeBindingList.add(elementTypeBinding);
				getSimpleTypeBindings(typeBindingList);
			}
			else if(typeBinding.isParameterizedType()) {
				List<ITypeBinding> typeBindingList = new ArrayList<ITypeBinding>();
				typeBindingList.add(typeBinding.getTypeDeclaration());
				ITypeBinding[] typeArgumentBindings = typeBinding.getTypeArguments();
				for(ITypeBinding typeArgumentBinding : typeArgumentBindings)
					typeBindingList.add(typeArgumentBinding);
				getSimpleTypeBindings(typeBindingList);
			}
			else if(typeBinding.isWildcardType()) {
				List<ITypeBinding> typeBindingList = new ArrayList<ITypeBinding>();
				typeBindingList.add(typeBinding.getBound());
				getSimpleTypeBindings(typeBindingList);
			}
			else {
				requiredTargetImportDeclarationSet.add(typeBinding);
			}
		}
	}

	private void addImportDeclaration(ITypeBinding typeBinding) {
		String qualifiedName = typeBinding.getQualifiedName();
		String qualifiedPackageName = "";
		if(qualifiedName.contains("."))
			qualifiedPackageName = qualifiedName.substring(0,qualifiedName.lastIndexOf("."));
		PackageDeclaration targetPackageDeclaration = targetCompilationUnit.getPackage();
		String targetPackageDeclarationName = "";
		if(targetPackageDeclaration != null)
			targetPackageDeclarationName = targetPackageDeclaration.getName().getFullyQualifiedName();	
		if(!qualifiedPackageName.equals("") && !qualifiedPackageName.equals("java.lang") && !qualifiedPackageName.equals(targetPackageDeclarationName)) {
			List<ImportDeclaration> importDeclarationList = targetCompilationUnit.imports();
			boolean found = false;
			for(ImportDeclaration importDeclaration : importDeclarationList) {
				if(!importDeclaration.isOnDemand()) {
					if(qualifiedName.equals(importDeclaration.getName().getFullyQualifiedName())) {
						found = true;
						break;
					}
				}
				else {
					if(qualifiedPackageName.equals(importDeclaration.getName().getFullyQualifiedName())) {
						found = true;
						break;
					}
				}
			}
			if(!found) {
				AST ast = targetCompilationUnit.getAST();
				ImportDeclaration importDeclaration = ast.newImportDeclaration();
				targetRewriter.set(importDeclaration, ImportDeclaration.NAME_PROPERTY, ast.newName(qualifiedName), null);
				ListRewrite importRewrite = targetRewriter.getListRewrite(targetCompilationUnit, CompilationUnit.IMPORTS_PROPERTY);
				importRewrite.insertLast(importDeclaration, null);
			}
		}
	}

	private void createMovedMethod() {
		AST ast = targetTypeDeclaration.getAST();
		MethodDeclaration newMethodDeclaration = (MethodDeclaration)ASTNode.copySubtree(ast, sourceMethod);
		
		ListRewrite modifierRewrite = targetRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		Modifier publicModifier = newMethodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
		boolean modifierFound = false;
		List<Modifier> modifiers = newMethodDeclaration.modifiers();
		for(Modifier modifier : modifiers){
			if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PUBLIC_KEYWORD)){
				modifierFound = true;
			}
			else if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PRIVATE_KEYWORD) ||
					modifier.getKeyword().equals(Modifier.ModifierKeyword.PROTECTED_KEYWORD)){
				modifierFound = true;
				modifierRewrite.replace(modifier, publicModifier, null);
			}
		}
		if(!modifierFound){
			modifierRewrite.insertFirst(publicModifier, null);
		}
		
		ListRewrite parametersRewrite = targetRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		List<SingleVariableDeclaration> sourceMethodParameters = sourceMethod.parameters();
		List<SingleVariableDeclaration> newMethodParameters = newMethodDeclaration.parameters();
		
		int i = 0;
		for(SingleVariableDeclaration parameter : sourceMethodParameters) {
			ITypeBinding parameterTypeBinding = parameter.getType().resolveBinding();
			if(parameterTypeBinding.getQualifiedName().equals(targetTypeDeclaration.resolveBinding().getQualifiedName())){
				targetClassVariableName = parameter.getName().getIdentifier();
				parametersRewrite.remove(newMethodParameters.get(i), null);
				break;
			}
			i++;
		}
		if(targetClassVariableName == null) {
			FieldDeclaration[] fieldDeclarations = sourceTypeDeclaration.getFields();
        	for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
        		List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
        		for(VariableDeclarationFragment fragment : fragments) {
	        		if(fieldDeclaration.getType().resolveBinding().getQualifiedName().equals(targetTypeDeclaration.resolveBinding().getQualifiedName())) {
	        			targetClassVariableName = fragment.getName().getIdentifier();
	        			break;
	        		}
        		}
        	}
		}
		
		modifySourceMemberAccessesInTargetClass(newMethodDeclaration);
		if(targetClassVariableName != null) {
			modifyTargetMethodInvocations(newMethodDeclaration);
			modifyTargetPublicFieldInstructions(newMethodDeclaration);
		}
		modifySourceStaticFieldInstructionsInTargetClass(newMethodDeclaration);
		replaceTargetClassVariableNameWithThisExpressionInMethodInvocationArguments(newMethodDeclaration);
		insertTargetClassVariableNameAsVariableDeclaration(newMethodDeclaration);
		replaceThisExpressionWithSourceClassParameterInMethodInvocationArguments(newMethodDeclaration);

		ListRewrite targetClassBodyRewrite = targetRewriter.getListRewrite(targetTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		targetClassBodyRewrite.insertLast(newMethodDeclaration, null);
	}

	private void moveAdditionalMethods() {
		AST ast = targetTypeDeclaration.getAST();
		Set<MethodDeclaration> methodsToBeMoved = new LinkedHashSet<MethodDeclaration>(additionalMethodsToBeMoved.values());
		for(MethodDeclaration methodDeclaration : methodsToBeMoved) {
			MethodDeclaration newMethodDeclaration = (MethodDeclaration)ASTNode.copySubtree(ast, methodDeclaration);
			ListRewrite targetClassBodyRewrite = targetRewriter.getListRewrite(targetTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			targetClassBodyRewrite.insertLast(newMethodDeclaration, null);
			ListRewrite sourceClassBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			sourceClassBodyRewrite.remove(methodDeclaration, null);
		}
	}

	private void removeSourceMethod() {
		ListRewrite classBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		classBodyRewrite.remove(sourceMethod, null);
	}

	private void addDelegationInSourceMethod() {
		List<SingleVariableDeclaration> sourceMethodParameters = sourceMethod.parameters();
		String targetClassVariableName = null;
		for(SingleVariableDeclaration parameter : sourceMethodParameters) {
			ITypeBinding parameterTypeBinding = parameter.getType().resolveBinding();
			if(parameterTypeBinding.getQualifiedName().equals(targetTypeDeclaration.resolveBinding().getQualifiedName())){
				targetClassVariableName = parameter.getName().getIdentifier();
				break;
			}
		}
		if(targetClassVariableName == null) {
			FieldDeclaration[] fieldDeclarations = sourceTypeDeclaration.getFields();
        	for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
        		List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
        		for(VariableDeclarationFragment fragment : fragments) {
	        		if(fieldDeclaration.getType().resolveBinding().getQualifiedName().equals(targetTypeDeclaration.resolveBinding().getQualifiedName())) {
	        			targetClassVariableName = fragment.getName().getIdentifier();
	        			break;
	        		}
        		}
        	}
		}
		
		ListRewrite methodBodyRewrite = sourceRewriter.getListRewrite(sourceMethod.getBody(), Block.STATEMENTS_PROPERTY);
		List<Statement> sourceMethodStatements = sourceMethod.getBody().statements();
		for(Statement statement : sourceMethodStatements) {
			methodBodyRewrite.remove(statement, null);
		}
		
		Type sourceMethodReturnType = sourceMethod.getReturnType2();
		ITypeBinding sourceMethodReturnTypeBinding = sourceMethodReturnType.resolveBinding();
		AST ast = sourceMethod.getBody().getAST();
		MethodInvocation delegation = ast.newMethodInvocation();
		sourceRewriter.set(delegation, MethodInvocation.NAME_PROPERTY, sourceMethod.getName(), null);
		SimpleName expressionName = ast.newSimpleName(targetClassVariableName);
		sourceRewriter.set(delegation, MethodInvocation.EXPRESSION_PROPERTY, expressionName, null);
		
		ListRewrite argumentRewrite = sourceRewriter.getListRewrite(delegation, MethodInvocation.ARGUMENTS_PROPERTY);
		for(SingleVariableDeclaration parameter : sourceMethodParameters) {
			if(!targetClassVariableName.equals(parameter.getName().getIdentifier())) {
				SimpleName argumentName = ast.newSimpleName(parameter.getName().getIdentifier());
				argumentRewrite.insertLast(argumentName, null);
			}
		}
		for(String argument : additionalArgumentsAddedToMovedMethod) {
			if(argument.equals("this"))
				argumentRewrite.insertLast(ast.newThisExpression(), null);
			else
				argumentRewrite.insertLast(ast.newSimpleName(argument), null);
		}
		if(sourceMethodReturnTypeBinding.getName().equals("void")) {
			ExpressionStatement expressionStatement = ast.newExpressionStatement(delegation);
			methodBodyRewrite.insertLast(expressionStatement, null);
		}
		else {
			ReturnStatement returnStatement = ast.newReturnStatement();
			sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, delegation, null);
			methodBodyRewrite.insertLast(returnStatement, null);
		}
	}

	private void modifyMovedMethodInvocationInSourceClass() {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		MethodDeclaration[] methodDeclarations = sourceTypeDeclaration.getMethods();
    	for(MethodDeclaration methodDeclaration : methodDeclarations) {
    		Block methodBody = methodDeclaration.getBody();
    		if(methodBody != null) {
	    		List<Statement> statements = methodBody.statements();
	    		for(Statement statement : statements) {
	    			List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(statement);
	    			for(Expression expression : methodInvocations) {
	    				if(expression instanceof MethodInvocation) {
	    					MethodInvocation methodInvocation = (MethodInvocation)expression;
	    					if(identicalSignature(sourceMethod, methodInvocation)) {
	    						List<Expression> arguments = methodInvocation.arguments();
	    						boolean foundInArguments = false;
	    						for(Expression argument : arguments) {
	    							if(argument.resolveTypeBinding().getQualifiedName().equals(targetTypeDeclaration.resolveBinding().getQualifiedName())) {
	    								foundInArguments = true;
	    								ListRewrite argumentRewrite = sourceRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
	    								argumentRewrite.remove(argument, null);
	    								sourceRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, argument, null);
	    								break;
	    							}
	    						}
	    						boolean foundInLocalVariableDeclarations = false;
	    						if(!foundInArguments) {
	    							StatementExtractor statementExtractor = new StatementExtractor();
	    							List<Statement> variableDeclarationStatements = statementExtractor.getVariableDeclarations(methodBody);
	    							for(Statement variableDeclarationStatement : variableDeclarationStatements) {
	    								VariableDeclarationStatement variableDeclaration = (VariableDeclarationStatement)variableDeclarationStatement;
	    								List<VariableDeclarationFragment> fragments = variableDeclaration.fragments();
	    				        		for(VariableDeclarationFragment fragment : fragments) {
	    				        			if(variableDeclaration.getType().resolveBinding().getQualifiedName().equals(targetTypeDeclaration.resolveBinding().getQualifiedName()) &&
	    				        					variableDeclaration.getStartPosition() < methodInvocation.getStartPosition()) {
	    				        				foundInLocalVariableDeclarations = true;
	    				        				sourceRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, fragment.getName(), null);
	    				        				break;
	    				        			}
	    				        		}
	    							}
	    						}
	    						if(!foundInArguments && !foundInLocalVariableDeclarations) {
	    							FieldDeclaration[] fieldDeclarations = sourceTypeDeclaration.getFields();
	    				        	for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
	    				        		List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
	    				        		for(VariableDeclarationFragment fragment : fragments) {
		    				        		if(fieldDeclaration.getType().resolveBinding().getQualifiedName().equals(targetTypeDeclaration.resolveBinding().getQualifiedName())) {
		    				        			sourceRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, fragment.getName(), null);
		    				        			break;
		    				        		}
	    				        		}
	    				        	}
	    						}
	    						ListRewrite argumentRewrite = sourceRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
	    						AST ast = methodInvocation.getAST();
	    						for(String argument : additionalArgumentsAddedToMovedMethod) {
	    							if(argument.equals("this"))
	    								argumentRewrite.insertLast(ast.newThisExpression(), null);
	    							else
	    								argumentRewrite.insertLast(ast.newSimpleName(argument), null);
	    						}
	    					}
	    				}
	    			}
	    		}
    		}
    	}
	}

	private void modifyMovedMethodInvocationInTargetClass() {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		MethodDeclaration[] methodDeclarations = targetTypeDeclaration.getMethods();
    	for(MethodDeclaration methodDeclaration : methodDeclarations) {
    		Block methodBody = methodDeclaration.getBody();
    		if(methodBody != null) {
	    		List<Statement> statements = methodBody.statements();
	    		Map<String, Integer> invokerCounterMap = new LinkedHashMap<String, Integer>();
	    		for(Statement statement : statements) {
	    			List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(statement);
	    			for(Expression expression : methodInvocations) {
	    				if(expression instanceof MethodInvocation) {
	    					MethodInvocation methodInvocation = (MethodInvocation)expression;
	    					if(identicalSignature(sourceMethod, methodInvocation)) {
	    						Expression invoker = methodInvocation.getExpression();
	    						if(invoker instanceof SimpleName) {
	    							SimpleName simpleName = (SimpleName)invoker;
	    							targetRewriter.remove(simpleName, null);
	    							String identifier = simpleName.getIdentifier();
	    							if(invokerCounterMap.containsKey(identifier)) {
	    								invokerCounterMap.put(identifier, invokerCounterMap.get(identifier)+1);
	    							}
	    							else {
	    								invokerCounterMap.put(identifier, 1);
	    							}
	    						}
	    						List<Expression> arguments = methodInvocation.arguments();
	    						for(Expression argument : arguments) {
	    							if(argument instanceof ThisExpression) {
	    								ListRewrite argumentRewrite = targetRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
	    								argumentRewrite.remove(argument, null);
	    							}
	    						}
	    					}
	    				}
	    			}
	    		}
    		}
    		/*
    		List<Expression> variableInstructions = expressionExtractor.getVariableInstructions(methodBody);
    		for(String invoker : invokerCounterMap.keySet()) {
    			int invokerCounter = 0;
    			for(Expression expression : variableInstructions) {
    				SimpleName simpleName = (SimpleName)expression;
    				if(simpleName.getIdentifier().equals(invoker))
    					invokerCounter++;
    			}
    			if(invokerCounter == invokerCounterMap.get(invoker)) {
    				List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
    				for(SingleVariableDeclaration parameter : parameters) {
    					if(parameter.getName().getIdentifier().equals(invoker)) {
    						ListRewrite parameterRewrite = targetRewriter.getListRewrite(methodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
    						parameterRewrite.remove(parameter, null);
    					}
    				}
    			}
    		}
    		*/
    	}
	}

	private boolean identicalSignature(MethodDeclaration methodDeclaration, MethodInvocation methodInvocation) {
		if(!methodInvocation.getName().getIdentifier().equals(methodDeclaration.getName().getIdentifier()))
			return false;
		List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
		List<Expression> arguments = methodInvocation.arguments();
		if(arguments.size() != parameters.size()) {
			return false;
		}
		else {
			for(int i=0; i<arguments.size(); i++) {
				SingleVariableDeclaration parameter = parameters.get(i);
				Expression argument = arguments.get(i);
				ITypeBinding argumentTypeBinding = argument.resolveTypeBinding();
				ITypeBinding parameterTypeBinding = parameter.getType().resolveBinding();
				if(!argumentTypeBinding.getQualifiedName().equals(parameterTypeBinding.getQualifiedName()))
					return false;
			}
		}
		
		return true;
	}

	private void modifyTargetMethodInvocations(MethodDeclaration newMethodDeclaration) {
		ExpressionExtractor extractor = new ExpressionExtractor();
		List<Expression> sourceMethodInvocations = extractor.getMethodInvocations(sourceMethod.getBody());
		List<Expression> newMethodInvocations = extractor.getMethodInvocations(newMethodDeclaration.getBody());
		int i = 0;
		for(Expression expression : sourceMethodInvocations) {
			if(expression instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)expression;
				Expression methodInvocationExpression = methodInvocation.getExpression();
				if(methodInvocationExpression instanceof SimpleName) {
					SimpleName methodInvocationExpressionSimpleName = (SimpleName)methodInvocationExpression;
					if(methodInvocationExpressionSimpleName.resolveTypeBinding().getQualifiedName().equals(targetTypeDeclaration.resolveBinding().getQualifiedName()) &&
							methodInvocationExpressionSimpleName.getIdentifier().equals(targetClassVariableName)) {
						MethodInvocation newMethodInvocation = (MethodInvocation)newMethodInvocations.get(i);
						targetRewriter.remove(newMethodInvocation.getExpression(), null);
					}
				}
				else if(methodInvocationExpression instanceof FieldAccess) {
					FieldAccess methodInvocationExpressionFieldAccess = (FieldAccess)methodInvocationExpression;
					if(methodInvocationExpressionFieldAccess.getName().resolveTypeBinding().getQualifiedName().equals(targetTypeDeclaration.resolveBinding().getQualifiedName()) &&
							methodInvocationExpressionFieldAccess.getName().getIdentifier().equals(targetClassVariableName)) {
						MethodInvocation newMethodInvocation = (MethodInvocation)newMethodInvocations.get(i);
						targetRewriter.remove(newMethodInvocation.getExpression(), null);
					}
				}
			}
			i++;
		}
	}

	private void modifyTargetPublicFieldInstructions(MethodDeclaration newMethodDeclaration) {
		ExpressionExtractor extractor = new ExpressionExtractor();
		List<Expression> sourceFieldInstructions = extractor.getVariableInstructions(sourceMethod.getBody());
		List<Expression> newFieldInstructions = extractor.getVariableInstructions(newMethodDeclaration.getBody());
		FieldDeclaration[] fields = targetTypeDeclaration.getFields();
		for(FieldDeclaration field : fields) {
			List<VariableDeclarationFragment> fragments = field.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				SimpleName fragmentName = fragment.getName();
				int i = 0;
				for(Expression expression : sourceFieldInstructions) {
					SimpleName simpleName = (SimpleName)expression;
					if(simpleName.getParent() instanceof QualifiedName && fragmentName.getIdentifier().equals(simpleName.getIdentifier())) {
						QualifiedName qualifiedName = (QualifiedName)simpleName.getParent();
						if(qualifiedName.getQualifier().resolveTypeBinding().getQualifiedName().equals(targetTypeDeclaration.resolveBinding().getQualifiedName()) &&
								qualifiedName.getQualifier().getFullyQualifiedName().equals(targetClassVariableName)) {
							SimpleName newSimpleName = (SimpleName)newFieldInstructions.get(i);
							targetRewriter.replace(newSimpleName.getParent(), simpleName, null);
						}
					}
					else if(simpleName.getParent() instanceof FieldAccess && fragmentName.getIdentifier().equals(simpleName.getIdentifier())) {
						FieldAccess fieldAccess = (FieldAccess)simpleName.getParent();
						Expression fieldAccessExpression = fieldAccess.getExpression();
						if(fieldAccessExpression instanceof FieldAccess) {
							FieldAccess invokerFieldAccess = (FieldAccess)fieldAccessExpression;
							if(invokerFieldAccess.resolveTypeBinding().getQualifiedName().equals(targetTypeDeclaration.resolveBinding().getQualifiedName()) &&
									invokerFieldAccess.getName().getIdentifier().equals(targetClassVariableName) && invokerFieldAccess.getExpression() instanceof ThisExpression) {
								SimpleName newSimpleName = (SimpleName)newFieldInstructions.get(i);
								FieldAccess newFieldAccess = (FieldAccess)newSimpleName.getParent();
								targetRewriter.replace(newFieldAccess.getExpression(), newMethodDeclaration.getAST().newThisExpression(), null);
							}
						}
					}
					i++;
				}
			}
		}
	}
	
	private void modifySourceStaticFieldInstructionsInTargetClass(MethodDeclaration newMethodDeclaration) {
		ExpressionExtractor extractor = new ExpressionExtractor();
		List<Expression> variableInstructions = extractor.getVariableInstructions(newMethodDeclaration.getBody());
		FieldDeclaration[] fields = sourceTypeDeclaration.getFields();
		AST ast = newMethodDeclaration.getAST();
		for(FieldDeclaration field : fields) {
			if((field.getModifiers() & Modifier.STATIC) != 0) {
				List<VariableDeclarationFragment> fragments = field.fragments();
				for(VariableDeclarationFragment fragment : fragments) {
					SimpleName fragmentName = fragment.getName();
					for(Expression expression : variableInstructions) {
						SimpleName expressionName = (SimpleName)expression;
						if(!(expressionName.getParent() instanceof QualifiedName) && fragmentName.getIdentifier().equals(expressionName.getIdentifier())){
							SimpleName newSimpleName = ast.newSimpleName(expressionName.getIdentifier());
							SimpleName qualifier = ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
							QualifiedName newQualifiedName = ast.newQualifiedName(qualifier, newSimpleName);
							targetRewriter.replace(expressionName, newQualifiedName, null);
						}
					}
				}
			}
		}
	}
	
	private void modifySourceMemberAccessesInTargetClass(MethodDeclaration newMethodDeclaration) {
		ExpressionExtractor extractor = new ExpressionExtractor();	
		List<Expression> sourceMethodInvocations = extractor.getMethodInvocations(sourceMethod.getBody());
		List<Expression> newMethodInvocations = extractor.getMethodInvocations(newMethodDeclaration.getBody());
		List<Expression> expressionsToBeRemoved = new ArrayList<Expression>();
		for(MethodInvocation methodInvocation : additionalMethodsToBeMoved.keySet()) {
			for(Expression expression : sourceMethodInvocations) {
				if(expression instanceof MethodInvocation) {
					MethodInvocation sourceMethodInvocation = (MethodInvocation)expression;
					if(methodInvocation.equals(sourceMethodInvocation)) {
						expressionsToBeRemoved.add(methodInvocation);
					}
				}
			}
		}
		for(Expression expression : expressionsToBeRemoved) {
			int index = sourceMethodInvocations.indexOf(expression);
			sourceMethodInvocations.remove(index);
			newMethodInvocations.remove(index);
		}
		expressionsToBeRemoved.clear();
		int k = 0;
		for(Expression expression : sourceMethodInvocations) {
			if(expression instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)expression;
				ITypeBinding methodInvocationDeclaringClassTypeBinding = methodInvocation.resolveMethodBinding().getDeclaringClass();
				if(methodInvocationDeclaringClassTypeBinding.getQualifiedName().equals(sourceTypeDeclaration.resolveBinding().getQualifiedName()) &&
						(methodInvocation.getExpression() == null || methodInvocation.getExpression() instanceof ThisExpression)) {
					MethodDeclaration[] sourceMethodDeclarations = sourceTypeDeclaration.getMethods();
					for(MethodDeclaration sourceMethodDeclaration : sourceMethodDeclarations) {
						if(identicalSignature(sourceMethodDeclaration, methodInvocation)) {
							MethodInvocation delegation = isDelegate(sourceMethodDeclaration);
							if(delegation != null) {
								ITypeBinding delegationDeclaringClassTypeBinding = delegation.resolveMethodBinding().getDeclaringClass();
								if(delegationDeclaringClassTypeBinding.getQualifiedName().equals(targetTypeDeclaration.resolveBinding().getQualifiedName())) {
									if(delegation.getExpression() != null) {
										MethodInvocation newMethodInvocation = (MethodInvocation)ASTNode.copySubtree(newMethodDeclaration.getAST(), delegation);
										targetRewriter.remove(newMethodInvocation.getExpression(), null);
										targetRewriter.replace(newMethodInvocations.get(k), newMethodInvocation, null);
									}
									expressionsToBeRemoved.add(methodInvocation);
								}
							}
						}
					}
				}
				else if(methodInvocationDeclaringClassTypeBinding.getQualifiedName().equals(targetTypeDeclaration.resolveBinding().getQualifiedName()) && methodInvocation.getExpression() != null) {
					Expression methodInvocationExpression = methodInvocation.getExpression();
					if(methodInvocationExpression instanceof MethodInvocation) {
						MethodInvocation invoker = (MethodInvocation)methodInvocationExpression;
						if(invoker.getExpression() == null || invoker.getExpression() instanceof ThisExpression) {
							MethodDeclaration[] sourceMethodDeclarations = sourceTypeDeclaration.getMethods();
							for(MethodDeclaration sourceMethodDeclaration : sourceMethodDeclarations) {
								if(identicalSignature(sourceMethodDeclaration, invoker)) {
									SimpleName fieldInstruction = isGetter(sourceMethodDeclaration);
									if(fieldInstruction != null && fieldInstruction.resolveTypeBinding().getQualifiedName().equals(targetTypeDeclaration.resolveBinding().getQualifiedName())) {
										int index = sourceMethodInvocations.indexOf(invoker);
										targetRewriter.remove(newMethodInvocations.get(index), null);
										expressionsToBeRemoved.add(invoker);
										expressionsToBeRemoved.add(methodInvocation);
									}
								}
							}
						}
					}
				}
			}
			k++;
		}
		for(Expression expression : expressionsToBeRemoved) {
			int index = sourceMethodInvocations.indexOf(expression);
			sourceMethodInvocations.remove(index);
			newMethodInvocations.remove(index);
		}
		
		List<Expression> sourceFieldInstructions = extractor.getVariableInstructions(sourceMethod.getBody());
		List<Expression> newFieldInstructions = extractor.getVariableInstructions(newMethodDeclaration.getBody());
		
		int i = 0;
		for(Expression expression : sourceFieldInstructions) {
			SimpleName simpleName = (SimpleName)expression;
			IBinding binding = simpleName.resolveBinding();
			if(binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				if(variableBinding.isField() && sourceTypeDeclaration.resolveBinding().equals(variableBinding.getDeclaringClass()) &&
						(variableBinding.getModifiers() & Modifier.STATIC) == 0) {
					SimpleName expressionName = (SimpleName)newFieldInstructions.get(i);
					if(expressionName.getParent() instanceof FieldAccess) {
						FieldAccess fieldAccess = (FieldAccess)expressionName.getParent();
						if(fieldAccess.getExpression() instanceof ThisExpression && !expressionName.getIdentifier().equals(targetClassVariableName)) {
							targetRewriter.replace(expressionName.getParent(), expressionName, null);
							if(!additionalArgumentsAddedToMovedMethod.contains(expressionName.getIdentifier()))
								addParameterToMovedMethod(newMethodDeclaration, expressionName);
						}
					}
					else if(!expressionName.getIdentifier().equals(targetClassVariableName) && !additionalArgumentsAddedToMovedMethod.contains(expressionName.getIdentifier()))
						addParameterToMovedMethod(newMethodDeclaration, expressionName);
				}
			}
			i++;
		}
		SimpleName parameterName = null;
		int j = 0;
		for(Expression expression : sourceMethodInvocations) {
			if(expression instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)expression;
				if(methodInvocation.getExpression() == null || methodInvocation.getExpression() instanceof ThisExpression) {
					IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
					if(methodBinding.getDeclaringClass().equals(sourceTypeDeclaration.resolveBinding())) {
						MethodDeclaration[] sourceMethodDeclarations = sourceTypeDeclaration.getMethods();
						for(MethodDeclaration sourceMethodDeclaration : sourceMethodDeclarations) {
							if(identicalSignature(sourceMethodDeclaration, methodInvocation)) {
								SimpleName fieldName = isGetter(sourceMethodDeclaration);
								MethodInvocation newMethodInvocation = (MethodInvocation)newMethodInvocations.get(j);
								if(fieldName != null) {
									AST ast = newMethodDeclaration.getAST();
									targetRewriter.replace(newMethodInvocation, ast.newSimpleName(fieldName.getIdentifier()), null);
									if(!fieldName.getIdentifier().equals(targetClassVariableName) && !additionalArgumentsAddedToMovedMethod.contains(fieldName.getIdentifier()))
										addParameterToMovedMethod(newMethodDeclaration, fieldName);
								}
								else {
									if(!additionalArgumentsAddedToMovedMethod.contains("this")) {
										parameterName = addSourceClassParameterToMovedMethod(newMethodDeclaration);
									}
									targetRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, parameterName, null);
									setPublicModifierToSourceMethod(methodInvocation);
								}
							}
						}
					}
				}
			}
			j++;
		}
	}

	private SimpleName addSourceClassParameterToMovedMethod(MethodDeclaration newMethodDeclaration) {
		AST ast = newMethodDeclaration.getAST();
		SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
		SimpleName typeName = ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
		Type parameterType = ast.newSimpleType(typeName);
		targetRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, parameterType, null);
		String sourceTypeName = sourceTypeDeclaration.getName().getIdentifier();
		SimpleName parameterName = ast.newSimpleName(sourceTypeName.replaceFirst(Character.toString(sourceTypeName.charAt(0)), Character.toString(Character.toLowerCase(sourceTypeName.charAt(0)))));
		targetRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, parameterName, null);
		ListRewrite parametersRewrite = targetRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		parametersRewrite.insertLast(parameter, null);
		this.additionalArgumentsAddedToMovedMethod.add("this");
		return parameterName;
	}

	private void addParameterToMovedMethod(MethodDeclaration newMethodDeclaration, SimpleName fieldName) {
		AST ast = newMethodDeclaration.getAST();
		SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
		Type fieldType = null;
		FieldDeclaration[] fields = sourceTypeDeclaration.getFields();
		for(FieldDeclaration field : fields) {
			List<VariableDeclarationFragment> fragments = field.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				if(fragment.getName().getIdentifier().equals(fieldName.getIdentifier())) {
					fieldType = field.getType();
					break;
				}
			}
		}
		targetRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, fieldType, null);
		targetRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, ast.newSimpleName(fieldName.getIdentifier()), null);
		ListRewrite parametersRewrite = targetRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		parametersRewrite.insertLast(parameter, null);
		this.additionalArgumentsAddedToMovedMethod.add(fieldName.getIdentifier());
	}

	private void setPublicModifierToSourceField(SimpleName simpleName) {
		FieldDeclaration[] fieldDeclarations = sourceTypeDeclaration.getFields();
		for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
			if(fieldDeclaration.getType().resolveBinding().equals(simpleName.resolveTypeBinding())) {
				List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
				for(VariableDeclarationFragment fragment : fragments) {
					if(fragment.getName().getIdentifier().equals(simpleName.getIdentifier())) {
						ListRewrite modifierRewrite = sourceRewriter.getListRewrite(fieldDeclaration, FieldDeclaration.MODIFIERS2_PROPERTY);
						Modifier publicModifier = fieldDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
						boolean modifierFound = false;
						List<Modifier> modifiers = fieldDeclaration.modifiers();
						for(Modifier modifier : modifiers){
							if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PUBLIC_KEYWORD)){
								modifierFound = true;
							}
							else if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PRIVATE_KEYWORD) ||
									modifier.getKeyword().equals(Modifier.ModifierKeyword.PROTECTED_KEYWORD)){
								modifierFound = true;
								modifierRewrite.replace(modifier, publicModifier, null);
							}
						}
						if(!modifierFound){
							modifierRewrite.insertFirst(publicModifier, null);
						}
					}
				}
			}
		}
	}

	private void setPublicModifierToSourceMethod(MethodInvocation methodInvocation) {
		MethodDeclaration[] methodDeclarations = sourceTypeDeclaration.getMethods();
		for(MethodDeclaration methodDeclaration : methodDeclarations) {
			if(identicalSignature(methodDeclaration, methodInvocation)) {
				ListRewrite modifierRewrite = sourceRewriter.getListRewrite(methodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
				Modifier publicModifier = methodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
				boolean modifierFound = false;
				List<Modifier> modifiers = methodDeclaration.modifiers();
				for(Modifier modifier : modifiers){
					if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PUBLIC_KEYWORD)){
						modifierFound = true;
					}
					else if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PRIVATE_KEYWORD) ||
							modifier.getKeyword().equals(Modifier.ModifierKeyword.PROTECTED_KEYWORD)){
						modifierFound = true;
						modifierRewrite.replace(modifier, publicModifier, null);
					}
				}
				if(!modifierFound){
					modifierRewrite.insertFirst(publicModifier, null);
				}
			}
		}
	}

	private MethodInvocation isDelegate(MethodDeclaration methodDeclaration) {
		Block methodBody = methodDeclaration.getBody();
		if(methodBody != null) {
			List<Statement> statements = methodBody.statements();
			if(statements.size() == 1) {
				Statement statement = statements.get(0);
	    		if(statement instanceof ReturnStatement) {
	    			ReturnStatement returnStatement = (ReturnStatement)statement;
	    			if(returnStatement.getExpression() instanceof MethodInvocation) {
	    				return (MethodInvocation)returnStatement.getExpression();
	    			}
	    		}
	    		else if(statement instanceof ExpressionStatement) {
	    			ExpressionStatement expressionStatement = (ExpressionStatement)statement;
	    			if(expressionStatement.getExpression() instanceof MethodInvocation) {
	    				return (MethodInvocation)expressionStatement.getExpression();
	    			}
	    		}
			}
		}
		return null;
	}

	private SimpleName isGetter(MethodDeclaration methodDeclaration) {
		Block methodBody = methodDeclaration.getBody();
		if(methodBody != null) {
			List<Statement> statements = methodBody.statements();
			if(statements.size() == 1) {
				Statement statement = statements.get(0);
	    		if(statement instanceof ReturnStatement) {
	    			ReturnStatement returnStatement = (ReturnStatement)statement;
	    			Expression returnStatementExpression = returnStatement.getExpression();
	    			if(returnStatementExpression instanceof SimpleName) {
	    				return (SimpleName)returnStatementExpression;
	    			}
	    			else if(returnStatementExpression instanceof FieldAccess) {
	    				FieldAccess fieldAccess = (FieldAccess)returnStatementExpression;
	    				return fieldAccess.getName();
	    			}
	    		}
			}
		}
		return null;
	}
	
	private void replaceTargetClassVariableNameWithThisExpressionInMethodInvocationArguments(MethodDeclaration newMethodDeclaration) {
		ExpressionExtractor extractor = new ExpressionExtractor();
		List<Expression> methodInvocations = extractor.getMethodInvocations(newMethodDeclaration.getBody());
		for(Expression invocation : methodInvocations) {
			if(invocation instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)invocation;
				List<Expression> arguments = methodInvocation.arguments();
				for(Expression argument : arguments) {
					if(argument instanceof SimpleName) {
						SimpleName simpleNameArgument = (SimpleName)argument;
						if(simpleNameArgument.getIdentifier().equals(targetClassVariableName)) {
							ListRewrite argumentRewrite = targetRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
							AST ast = newMethodDeclaration.getAST();
							argumentRewrite.replace(argument, ast.newThisExpression(), null);
						}
					}
				}
			}
		}
	}
	
	private void insertTargetClassVariableNameAsVariableDeclaration(MethodDeclaration newMethodDeclaration) {
		StatementExtractor extractor = new StatementExtractor();
		List<Statement> variableDeclarations = extractor.getVariableDeclarations(newMethodDeclaration.getBody());
		for(Statement declaration : variableDeclarations) {
			VariableDeclarationStatement variableDeclaration = (VariableDeclarationStatement)declaration;
			List<VariableDeclarationFragment> fragments = variableDeclaration.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				Expression initializer = fragment.getInitializer();
				if(initializer instanceof SimpleName) {
					SimpleName simpleNameInitializer = (SimpleName)initializer;
					if(simpleNameInitializer.getIdentifier().equals(targetClassVariableName)) {
						AST ast = newMethodDeclaration.getAST();
						VariableDeclarationFragment variableDeclarationFragment = ast.newVariableDeclarationFragment();
						targetRewriter.set(variableDeclarationFragment, VariableDeclarationFragment.NAME_PROPERTY, simpleNameInitializer, null);
						targetRewriter.set(variableDeclarationFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, ast.newThisExpression(), null);
						VariableDeclarationStatement variableDeclarationStatement = ast.newVariableDeclarationStatement(variableDeclarationFragment);
						targetRewriter.set(variableDeclarationStatement, VariableDeclarationStatement.TYPE_PROPERTY, variableDeclaration.getType(), null);
						ListRewrite bodyRewrite = targetRewriter.getListRewrite(newMethodDeclaration.getBody(), Block.STATEMENTS_PROPERTY);
						bodyRewrite.insertFirst(variableDeclarationStatement, null);
					}
				}
			}
		}
	}
	
	private void replaceThisExpressionWithSourceClassParameterInMethodInvocationArguments(MethodDeclaration newMethodDeclaration) {
		ExpressionExtractor extractor = new ExpressionExtractor();
		List<Expression> methodInvocations = extractor.getMethodInvocations(newMethodDeclaration.getBody());
		for(Expression invocation : methodInvocations) {
			if(invocation instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)invocation;
				List<Expression> arguments = methodInvocation.arguments();
				for(Expression argument : arguments) {
					if(argument instanceof ThisExpression) {
						SimpleName parameterName = null;
						if(!additionalArgumentsAddedToMovedMethod.contains("this")) {
							parameterName = addSourceClassParameterToMovedMethod(newMethodDeclaration);
						}
						else {
							AST ast = newMethodDeclaration.getAST();
							String sourceTypeName = sourceTypeDeclaration.getName().getIdentifier();
							parameterName = ast.newSimpleName(sourceTypeName.replaceFirst(Character.toString(sourceTypeName.charAt(0)), Character.toString(Character.toLowerCase(sourceTypeName.charAt(0)))));
						}
						ListRewrite argumentRewrite = targetRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
						argumentRewrite.replace(argument, parameterName, null);
					}
				}
			}
		}
	}
}
