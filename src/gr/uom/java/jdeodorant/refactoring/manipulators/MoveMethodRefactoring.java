package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.decomposition.cfg.MethodCallAnalyzer;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.MethodDeclarationUtility;
import gr.uom.java.ast.util.StatementExtractor;
import gr.uom.java.ast.util.TypeVisitor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class MoveMethodRefactoring extends Refactoring {
	
	private CompilationUnit sourceCompilationUnit;
	private CompilationUnit targetCompilationUnit;
	private TypeDeclaration sourceTypeDeclaration;
	private TypeDeclaration targetTypeDeclaration;
	private MethodDeclaration sourceMethod;
	private String targetClassVariableName;
	private Set<String> additionalArgumentsAddedToMovedMethod;
	private Set<ITypeBinding> additionalTypeBindingsToBeImportedInTargetClass;
	private Map<MethodInvocation, MethodDeclaration> additionalMethodsToBeMoved;
	private Set<FieldDeclaration> fieldDeclarationsChangedWithPublicModifier;
	private Set<BodyDeclaration> memberTypeDeclarationsChangedWithPublicModifier;
	private boolean leaveDelegate;
	private String movedMethodName;
	private boolean isTargetClassVariableParameter;
	private int targetClassVariableParameterIndex;
	private Map<ICompilationUnit, CompilationUnitChange> fChanges;
	private MultiTextEdit sourceMultiTextEdit;
	private MultiTextEdit targetMultiTextEdit;
	private CompilationUnitChange sourceCompilationUnitChange;
	private CompilationUnitChange targetCompilationUnitChange;
	
	public MoveMethodRefactoring(CompilationUnit sourceCompilationUnit, CompilationUnit targetCompilationUnit, 
			TypeDeclaration sourceTypeDeclaration, TypeDeclaration targetTypeDeclaration, MethodDeclaration sourceMethod,
			Map<MethodInvocation, MethodDeclaration> additionalMethodsToBeMoved, boolean leaveDelegate, String movedMethodName) {
		this.sourceCompilationUnit = sourceCompilationUnit;
		this.targetCompilationUnit = targetCompilationUnit;
		this.sourceTypeDeclaration = sourceTypeDeclaration;
		this.targetTypeDeclaration = targetTypeDeclaration;
		this.sourceMethod = sourceMethod;
		this.targetClassVariableName = null;
		this.additionalArgumentsAddedToMovedMethod = new LinkedHashSet<String>();
		this.additionalTypeBindingsToBeImportedInTargetClass = new LinkedHashSet<ITypeBinding>();
		this.additionalMethodsToBeMoved = additionalMethodsToBeMoved;
		this.fieldDeclarationsChangedWithPublicModifier = new LinkedHashSet<FieldDeclaration>();
		this.memberTypeDeclarationsChangedWithPublicModifier = new LinkedHashSet<BodyDeclaration>();
		this.leaveDelegate = leaveDelegate;
		this.movedMethodName = movedMethodName;
		this.isTargetClassVariableParameter = false;
		this.targetClassVariableParameterIndex = -1;
		this.fChanges = new LinkedHashMap<ICompilationUnit, CompilationUnitChange>();
		
		ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
		this.sourceMultiTextEdit = new MultiTextEdit();
		this.sourceCompilationUnitChange = new CompilationUnitChange("", sourceICompilationUnit);
		sourceCompilationUnitChange.setEdit(sourceMultiTextEdit);
		fChanges.put(sourceICompilationUnit, sourceCompilationUnitChange);
		
		ICompilationUnit targetICompilationUnit = (ICompilationUnit)targetCompilationUnit.getJavaElement();
		if(sourceICompilationUnit.equals(targetICompilationUnit)) {
			this.targetMultiTextEdit = sourceMultiTextEdit;
			this.targetCompilationUnitChange = sourceCompilationUnitChange;
		}
		else {
			this.targetMultiTextEdit = new MultiTextEdit();
			this.targetCompilationUnitChange = new CompilationUnitChange("", targetICompilationUnit);
			targetCompilationUnitChange.setEdit(targetMultiTextEdit);
			fChanges.put(targetICompilationUnit, targetCompilationUnitChange);
		}
	}

	public Map<ICompilationUnit, CompilationUnitChange> getChanges() {
		return fChanges;
	}

	public String getMovedMethodName() {
		return movedMethodName;
	}

	public void setMovedMethodName(String movedMethodName) {
		this.movedMethodName = movedMethodName;
	}

	public void setLeaveDelegate(boolean leaveDelegate) {
		this.leaveDelegate = leaveDelegate;
	}

	public void apply() {
		createMovedMethod();
		addAdditionalMethodsToTargetClass();
		if(!sourceCompilationUnit.equals(targetCompilationUnit))
			addRequiredTargetImportDeclarations();
		
		modifyMovedMethodInvocationInSourceClass();
		if(leaveDelegate) {
			addDelegationInSourceMethod();
			removeAdditionalMethodsFromSourceClass();
		}
		else {
			//removes also additional methods used only by the moved method
			removeSourceMethod();
		}
	}

	private void addRequiredTargetImportDeclarations() {
		TypeVisitor typeVisitor = new TypeVisitor();
		sourceMethod.accept(typeVisitor);
		ImportRewrite targetImportRewrite = ImportRewrite.create(targetCompilationUnit, true);
		
		for(ITypeBinding typeBinding : typeVisitor.getTypeBindings()) {
			if(!typeBinding.isNested() || (typeBinding.isNested() && sourceTypeDeclaration.resolveBinding().isEqualTo(typeBinding.getDeclaringClass())))
				targetImportRewrite.addImport(typeBinding);
		}
		for(ITypeBinding typeBinding : additionalTypeBindingsToBeImportedInTargetClass) {
			if(!typeBinding.isNested() || (typeBinding.isNested() && sourceTypeDeclaration.resolveBinding().isEqualTo(typeBinding.getDeclaringClass())))
				targetImportRewrite.addImport(typeBinding);
		}
		
		try {
			TextEdit targetImportEdit = targetImportRewrite.rewriteImports(null);
			if(targetImportRewrite.getCreatedImports().length > 0) {
				targetMultiTextEdit.addChild(targetImportEdit);
				targetCompilationUnitChange.addTextEditGroup(new TextEditGroup("Add required import declarations", new TextEdit[] {targetImportEdit}));
			}
		}
		catch(CoreException coreException) {
			coreException.printStackTrace();
		}
	}

	private void createMovedMethod() {
		ASTRewrite targetRewriter = ASTRewrite.create(targetCompilationUnit.getAST());
		AST ast = targetTypeDeclaration.getAST();
		MethodDeclaration newMethodDeclaration = (MethodDeclaration)ASTNode.copySubtree(ast, sourceMethod);
		
		targetRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, ast.newSimpleName(movedMethodName), null);
		ListRewrite modifierRewrite = targetRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		Modifier publicModifier = newMethodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
		boolean modifierFound = false;
		List<IExtendedModifier> modifiers = newMethodDeclaration.modifiers();
		for(IExtendedModifier extendedModifier : modifiers) {
			if(extendedModifier.isModifier()) {
				Modifier modifier = (Modifier)extendedModifier;
				if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PUBLIC_KEYWORD)) {
					modifierFound = true;
				}
				else if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PRIVATE_KEYWORD) ||
						modifier.getKeyword().equals(Modifier.ModifierKeyword.PROTECTED_KEYWORD)) {
					modifierFound = true;
					modifierRewrite.replace(modifier, publicModifier, null);
				}
			}
		}
		if(!modifierFound) {
			modifierRewrite.insertFirst(publicModifier, null);
		}
		
		ListRewrite parametersRewrite = targetRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		List<SingleVariableDeclaration> sourceMethodParameters = sourceMethod.parameters();
		List<SingleVariableDeclaration> newMethodParameters = newMethodDeclaration.parameters();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> newVariableInstructions = expressionExtractor.getVariableInstructions(newMethodDeclaration.getBody());
		
		int i = 0;
		for(SingleVariableDeclaration parameter : sourceMethodParameters) {
			ITypeBinding parameterTypeBinding = parameter.getType().resolveBinding();
			if(parameterTypeBinding.isEqualTo(targetTypeDeclaration.resolveBinding())){
				for(Expression expression : newVariableInstructions) {
					SimpleName simpleName = (SimpleName)expression;
					if(parameter.getName().getIdentifier().equals(simpleName.getIdentifier())) {
						targetClassVariableName = parameter.getName().getIdentifier();
						parametersRewrite.remove(newMethodParameters.get(i), null);
						removeParamTagElementFromJavadoc(newMethodDeclaration, targetRewriter, targetClassVariableName);
						isTargetClassVariableParameter = true;
						targetClassVariableParameterIndex = i;
						break;
					}
				}
				if(targetClassVariableName != null)
    				break;
			}
			i++;
		}
		
		FieldDeclaration[] fieldDeclarations = sourceTypeDeclaration.getFields();
		
		if(targetClassVariableName == null) {
        	for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
        		List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
        		for(VariableDeclarationFragment fragment : fragments) {
	        		if(fieldDeclaration.getType().resolveBinding().isEqualTo(targetTypeDeclaration.resolveBinding())) {
	        			for(Expression expression : newVariableInstructions) {
	        				SimpleName simpleName = (SimpleName)expression;
	        				if(fragment.getName().getIdentifier().equals(simpleName.getIdentifier())) {
	        					targetClassVariableName = fragment.getName().getIdentifier();
	        					break;
	        				}
	        			}
	        			if(targetClassVariableName != null)
	        				break;
	        		}
        		}
        		if(targetClassVariableName != null)
    				break;
        	}
		}
		
		List<Expression> oldMethodInvocations = expressionExtractor.getMethodInvocations(sourceMethod.getBody());
		MethodDeclaration[] methodDeclarations = sourceTypeDeclaration.getMethods();
		
		if(targetClassVariableName == null) {
			for(Expression oldMethodInvocation : oldMethodInvocations) {
				if(oldMethodInvocation instanceof MethodInvocation) {
					MethodInvocation methodInvocation = (MethodInvocation)oldMethodInvocation;
					if(methodInvocation.resolveMethodBinding().getDeclaringClass().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
						for(MethodDeclaration methodDeclaration : methodDeclarations) {
							if(methodInvocation.resolveMethodBinding().isEqualTo(methodDeclaration.resolveBinding())) {
								SimpleName fieldInstruction = MethodDeclarationUtility.isGetter(methodDeclaration);
								if(fieldInstruction != null && fieldInstruction.resolveTypeBinding().isEqualTo(targetTypeDeclaration.resolveBinding())) {
									targetClassVariableName = fieldInstruction.getIdentifier();
									break;
								}
							}
						}
						if(targetClassVariableName != null)
	        				break;
					}
				}
			}
		}
		
		if(targetClassVariableName == null) {
			for(Expression oldMethodInvocation : oldMethodInvocations) {
				if(oldMethodInvocation instanceof MethodInvocation) {
					MethodInvocation methodInvocation = (MethodInvocation)oldMethodInvocation;
					if(methodInvocation.resolveMethodBinding().getDeclaringClass().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
						for(MethodDeclaration methodDeclaration : methodDeclarations) {
							if(methodInvocation.resolveMethodBinding().isEqualTo(methodDeclaration.resolveBinding())) {
								MethodInvocation delegation = MethodDeclarationUtility.isDelegate(methodDeclaration);
								if(delegation != null && delegation.resolveMethodBinding().getDeclaringClass().isEqualTo(targetTypeDeclaration.resolveBinding())) {
									List<Expression> delegateMethodVariableInstructions = expressionExtractor.getVariableInstructions(methodDeclaration.getBody());
									for(Expression expression : delegateMethodVariableInstructions) {
										SimpleName fieldInstruction = (SimpleName)expression;
										IBinding fieldInstructionBinding = fieldInstruction.resolveBinding();
										if(fieldInstructionBinding != null && fieldInstructionBinding.getKind() == IBinding.VARIABLE) {
											IVariableBinding fieldInstructionVariableBinding = (IVariableBinding)fieldInstructionBinding;
											if(fieldInstructionVariableBinding.isField() && fieldInstructionVariableBinding.getType().isEqualTo(targetTypeDeclaration.resolveBinding())) {
												targetClassVariableName = fieldInstruction.getIdentifier();
												break;
											}
										}
									}
									if(targetClassVariableName != null)
				        				break;
								}
							}
						}
						if(targetClassVariableName != null)
	        				break;
					}
				}
			}
		}
		
		if(targetClassVariableName == null) {
        	for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
        		List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
        		for(VariableDeclarationFragment fragment : fragments) {
	        		if(targetTypeDeclaration.resolveBinding().isEqualTo(fieldDeclaration.getType().resolveBinding().getSuperclass())) {
	        			for(Expression expression : newVariableInstructions) {
	        				SimpleName simpleName = (SimpleName)expression;
	        				if(fragment.getName().getIdentifier().equals(simpleName.getIdentifier())) {
	        					targetClassVariableName = fragment.getName().getIdentifier();
	        					break;
	        				}
	        			}
	        			if(targetClassVariableName != null)
	        				break;
	        		}
        		}
        		if(targetClassVariableName != null)
    				break;
        	}
		}
		
		modifySourceMemberAccessesInTargetClass(newMethodDeclaration, targetRewriter);
		if(targetClassVariableName != null) {
			modifyTargetMethodInvocations(newMethodDeclaration, targetRewriter);
			modifyTargetPublicFieldInstructions(newMethodDeclaration, targetRewriter);
		}
		modifySourceStaticFieldInstructionsInTargetClass(newMethodDeclaration, targetRewriter);
		modifySourceStaticMethodInvocationsInTargetClass(newMethodDeclaration, targetRewriter);
		modifyRecursiveMethodInvocationsOfTheMovedMethod(newMethodDeclaration, targetRewriter);
		replaceTargetClassVariableNameWithThisExpressionInMethodInvocationArguments(newMethodDeclaration, targetRewriter);
		replaceTargetClassVariableNameWithThisExpressionInClassInstanceCreationArguments(newMethodDeclaration, targetRewriter);
		replaceTargetClassVariableNameWithThisExpressionInVariableDeclarationInitializers(newMethodDeclaration, targetRewriter);
		replaceTargetClassVariableNameWithThisExpressionInInfixExpressions(newMethodDeclaration, targetRewriter);
		replaceTargetClassVariableNameWithThisExpressionInCastExpressions(newMethodDeclaration, targetRewriter);
		replaceTargetClassVariableNameWithThisExpressionInInstanceofExpressions(newMethodDeclaration, targetRewriter);
		replaceTargetClassVariableNameWithThisExpressionInAssignments(newMethodDeclaration, targetRewriter);
		replaceTargetClassVariableNameWithThisExpressionInReturnStatements(newMethodDeclaration, targetRewriter);
		replaceThisExpressionWithSourceClassParameterInMethodInvocationArguments(newMethodDeclaration, targetRewriter);
		replaceThisExpressionWithSourceClassParameterInClassInstanceCreationArguments(newMethodDeclaration, targetRewriter);
		replaceThisExpressionWithSourceClassParameterInVariableDeclarationInitializers(newMethodDeclaration, targetRewriter);

		ListRewrite targetClassBodyRewrite = targetRewriter.getListRewrite(targetTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		targetClassBodyRewrite.insertLast(newMethodDeclaration, null);
		
		try {
			TextEdit targetEdit = targetRewriter.rewriteAST();
			targetMultiTextEdit.addChild(targetEdit);
			targetCompilationUnitChange.addTextEditGroup(new TextEditGroup("Add moved method", new TextEdit[] {targetEdit}));
		}
		catch(JavaModelException javaModelException) {
			javaModelException.printStackTrace();
		}
	}

	private void addAdditionalMethodsToTargetClass() {
		AST ast = targetTypeDeclaration.getAST();
		Set<MethodDeclaration> methodsToBeMoved = new LinkedHashSet<MethodDeclaration>(additionalMethodsToBeMoved.values());
		for(MethodDeclaration methodDeclaration : methodsToBeMoved) {
			TypeVisitor typeVisitor = new TypeVisitor();
			methodDeclaration.accept(typeVisitor);
			for(ITypeBinding typeBinding : typeVisitor.getTypeBindings()) {
				this.additionalTypeBindingsToBeImportedInTargetClass.add(typeBinding);
			}
			MethodDeclaration newMethodDeclaration = (MethodDeclaration)ASTNode.copySubtree(ast, methodDeclaration);
			ASTRewrite targetRewriter = ASTRewrite.create(targetCompilationUnit.getAST());
			ListRewrite targetClassBodyRewrite = targetRewriter.getListRewrite(targetTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			targetClassBodyRewrite.insertLast(newMethodDeclaration, null);
			try {
				TextEdit targetEdit = targetRewriter.rewriteAST();
				targetMultiTextEdit.addChild(targetEdit);
				targetCompilationUnitChange.addTextEditGroup(new TextEditGroup("Add additional moved method", new TextEdit[] {targetEdit}));
			}
			catch(JavaModelException javaModelException) {
				javaModelException.printStackTrace();
			}
		}
	}

	private void removeAdditionalMethodsFromSourceClass() {
		Set<MethodDeclaration> methodsToBeMoved = new LinkedHashSet<MethodDeclaration>(additionalMethodsToBeMoved.values());
		for(MethodDeclaration methodDeclaration : methodsToBeMoved) {
			ASTRewrite sourceRewriter = ASTRewrite.create(sourceCompilationUnit.getAST());
			ListRewrite sourceClassBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			sourceClassBodyRewrite.remove(methodDeclaration, null);
			try {
				TextEdit sourceEdit = sourceRewriter.rewriteAST();
				sourceMultiTextEdit.addChild(sourceEdit);
				sourceCompilationUnitChange.addTextEditGroup(new TextEditGroup("Remove additional moved method", new TextEdit[] {sourceEdit}));
			}
			catch(JavaModelException javaModelException) {
				javaModelException.printStackTrace();
			}
		}
	}

	private void removeSourceMethod() {
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceCompilationUnit.getAST());
		ListRewrite classBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		classBodyRewrite.remove(sourceMethod, null);
		Set<MethodDeclaration> methodsToBeMoved = new LinkedHashSet<MethodDeclaration>(additionalMethodsToBeMoved.values());
		for(MethodDeclaration methodDeclaration : methodsToBeMoved) {
			classBodyRewrite.remove(methodDeclaration, null);
		}
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			sourceMultiTextEdit.addChild(sourceEdit);
			sourceCompilationUnitChange.addTextEditGroup(new TextEditGroup("Remove moved method", new TextEdit[] {sourceEdit}));
		}
		catch(JavaModelException javaModelException) {
			javaModelException.printStackTrace();
		}
	}

	private void addDelegationInSourceMethod() {
		List<SingleVariableDeclaration> sourceMethodParameters = sourceMethod.parameters();
		String targetClassVariableName = null;
		for(SingleVariableDeclaration parameter : sourceMethodParameters) {
			ITypeBinding parameterTypeBinding = parameter.getType().resolveBinding();
			if(parameterTypeBinding.isEqualTo(targetTypeDeclaration.resolveBinding()) &&
					parameter.getName().getIdentifier().equals(this.targetClassVariableName)) {
				targetClassVariableName = parameter.getName().getIdentifier();
				break;
			}
		}
		FieldDeclaration[] fieldDeclarations = sourceTypeDeclaration.getFields();
		if(targetClassVariableName == null) {
        	for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
        		List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
        		for(VariableDeclarationFragment fragment : fragments) {
	        		if(fieldDeclaration.getType().resolveBinding().isEqualTo(targetTypeDeclaration.resolveBinding()) &&
	        				fragment.getName().getIdentifier().equals(this.targetClassVariableName)) {
	        			targetClassVariableName = fragment.getName().getIdentifier();
	        			break;
	        		}
        		}
        	}
		}
		if(targetClassVariableName == null) {
        	for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
        		List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
        		for(VariableDeclarationFragment fragment : fragments) {
	        		if(targetTypeDeclaration.resolveBinding().isEqualTo(fieldDeclaration.getType().resolveBinding().getSuperclass()) &&
	        				fragment.getName().getIdentifier().equals(this.targetClassVariableName)) {
	        			targetClassVariableName = fragment.getName().getIdentifier();
	        			break;
	        		}
        		}
        	}
		}
		
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceCompilationUnit.getAST());
		ListRewrite methodBodyRewrite = sourceRewriter.getListRewrite(sourceMethod.getBody(), Block.STATEMENTS_PROPERTY);
		List<Statement> sourceMethodStatements = sourceMethod.getBody().statements();
		for(Statement statement : sourceMethodStatements) {
			methodBodyRewrite.remove(statement, null);
		}
		
		Type sourceMethodReturnType = sourceMethod.getReturnType2();
		ITypeBinding sourceMethodReturnTypeBinding = sourceMethodReturnType.resolveBinding();
		AST ast = sourceMethod.getBody().getAST();
		MethodInvocation delegation = ast.newMethodInvocation();
		sourceRewriter.set(delegation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(movedMethodName), null);
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
		
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			sourceMultiTextEdit.addChild(sourceEdit);
			sourceCompilationUnitChange.addTextEditGroup(new TextEditGroup("Leave delegate to moved method", new TextEdit[] {sourceEdit}));
		}
		catch(JavaModelException javaModelException) {
			javaModelException.printStackTrace();
		}
	}

	private void modifyMovedMethodInvocationInSourceClass() {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		MethodDeclaration[] methodDeclarations = sourceTypeDeclaration.getMethods();
		for(MethodDeclaration methodDeclaration : methodDeclarations) {
			if(!sourceMethod.equals(methodDeclaration)) {
				Block methodBody = methodDeclaration.getBody();
				if(methodBody != null) {
					List<Statement> statements = methodBody.statements();
					for(Statement statement : statements) {
						List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(statement);
						for(Expression expression : methodInvocations) {
							if(expression instanceof MethodInvocation) {
								MethodInvocation methodInvocation = (MethodInvocation)expression;
								if(sourceMethod.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
									ASTRewrite sourceRewriter = ASTRewrite.create(sourceCompilationUnit.getAST());
									AST ast = methodInvocation.getAST();
									sourceRewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(movedMethodName), null);
									List<Expression> arguments = methodInvocation.arguments();
									boolean foundInArguments = false;
									for(Expression argument : arguments) {
										if(argument.resolveTypeBinding().isEqualTo(targetTypeDeclaration.resolveBinding())) {
											foundInArguments = true;
											ListRewrite argumentRewrite = sourceRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
											argumentRewrite.remove(argument, null);
											if(argument instanceof CastExpression) {
												ParenthesizedExpression parenthesizedExpression = ast.newParenthesizedExpression();
												sourceRewriter.set(parenthesizedExpression, ParenthesizedExpression.EXPRESSION_PROPERTY, argument, null);
												sourceRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, parenthesizedExpression, null);
											}
											else {
												sourceRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, argument, null);
											}
											break;
										}
									}
									if(!foundInArguments && isTargetClassVariableParameter) {
										for(Expression argument : arguments) {
											if(targetTypeDeclaration.resolveBinding().isEqualTo(argument.resolveTypeBinding().getSuperclass())) {
												foundInArguments = true;
												ListRewrite argumentRewrite = sourceRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
												argumentRewrite.remove(argument, null);
												sourceRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, argument, null);
												break;
											}
										}
									}
									boolean foundInFields = false;
									if(!foundInArguments) {
										FieldDeclaration[] fieldDeclarations = sourceTypeDeclaration.getFields();
										for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
											List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
											for(VariableDeclarationFragment fragment : fragments) {
												if(fieldDeclaration.getType().resolveBinding().isEqualTo(targetTypeDeclaration.resolveBinding()) &&
														fragment.getName().getIdentifier().equals(targetClassVariableName)) {
													foundInFields = true;
													sourceRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, fragment.getName(), null);
													break;
												}
											}
										}
									}
									if(!foundInArguments && !foundInFields) {
										FieldDeclaration[] fieldDeclarations = sourceTypeDeclaration.getFields();
										for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
											List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
											for(VariableDeclarationFragment fragment : fragments) {
												if(targetTypeDeclaration.resolveBinding().isEqualTo(fieldDeclaration.getType().resolveBinding().getSuperclass()) &&
														fragment.getName().getIdentifier().equals(targetClassVariableName)) {
													sourceRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, fragment.getName(), null);
													break;
												}
											}
										}
									}
									ListRewrite argumentRewrite = sourceRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
									for(String argument : additionalArgumentsAddedToMovedMethod) {
										if(argument.equals("this"))
											argumentRewrite.insertLast(ast.newThisExpression(), null);
										else
											argumentRewrite.insertLast(ast.newSimpleName(argument), null);
									}

									try {
										TextEdit sourceEdit = sourceRewriter.rewriteAST();
										sourceMultiTextEdit.addChild(sourceEdit);
										sourceCompilationUnitChange.addTextEditGroup(new TextEditGroup("Change invocation of moved method", new TextEdit[] {sourceEdit}));
									}
									catch(JavaModelException javaModelException) {
										javaModelException.printStackTrace();
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void modifyTargetMethodInvocations(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
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
					if( (methodInvocationExpressionSimpleName.resolveTypeBinding().isEqualTo(targetTypeDeclaration.resolveBinding()) ||
							targetTypeDeclaration.resolveBinding().isEqualTo(methodInvocationExpressionSimpleName.resolveTypeBinding().getSuperclass()) ) &&
							methodInvocationExpressionSimpleName.getIdentifier().equals(targetClassVariableName)) {
						MethodInvocation newMethodInvocation = (MethodInvocation)newMethodInvocations.get(i);
						targetRewriter.remove(newMethodInvocation.getExpression(), null);
					}
				}
				else if(methodInvocationExpression instanceof FieldAccess) {
					FieldAccess methodInvocationExpressionFieldAccess = (FieldAccess)methodInvocationExpression;
					if( (methodInvocationExpressionFieldAccess.getName().resolveTypeBinding().isEqualTo(targetTypeDeclaration.resolveBinding()) ||
							targetTypeDeclaration.resolveBinding().isEqualTo(methodInvocationExpressionFieldAccess.getName().resolveTypeBinding().getSuperclass()) ) &&
							methodInvocationExpressionFieldAccess.getName().getIdentifier().equals(targetClassVariableName)) {
						MethodInvocation newMethodInvocation = (MethodInvocation)newMethodInvocations.get(i);
						targetRewriter.remove(newMethodInvocation.getExpression(), null);
					}
				}
			}
			i++;
		}
	}

	private void modifyTargetPublicFieldInstructions(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
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
						if( (qualifiedName.getQualifier().resolveTypeBinding().isEqualTo(targetTypeDeclaration.resolveBinding()) ||
								targetTypeDeclaration.resolveBinding().isEqualTo(qualifiedName.getQualifier().resolveTypeBinding().getSuperclass()) ) &&
								qualifiedName.getQualifier().getFullyQualifiedName().equals(targetClassVariableName)) {
							SimpleName newSimpleName = (SimpleName)newFieldInstructions.get(i);
							FieldAccess fieldAccess = newMethodDeclaration.getAST().newFieldAccess();
							targetRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, simpleName, null);
							targetRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, newMethodDeclaration.getAST().newThisExpression(), null);
							targetRewriter.replace(newSimpleName.getParent(), fieldAccess, null);
						}
					}
					else if(simpleName.getParent() instanceof FieldAccess && fragmentName.getIdentifier().equals(simpleName.getIdentifier())) {
						FieldAccess fieldAccess = (FieldAccess)simpleName.getParent();
						Expression fieldAccessExpression = fieldAccess.getExpression();
						if(fieldAccessExpression instanceof FieldAccess) {
							FieldAccess invokerFieldAccess = (FieldAccess)fieldAccessExpression;
							if( (invokerFieldAccess.resolveTypeBinding().isEqualTo(targetTypeDeclaration.resolveBinding()) ||
									targetTypeDeclaration.resolveBinding().isEqualTo(invokerFieldAccess.resolveTypeBinding().getSuperclass())) &&
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
		if(!targetTypeDeclaration.resolveBinding().getSuperclass().getQualifiedName().equals("java.lang.Object")) {
			IVariableBinding[] superclassFields = targetTypeDeclaration.resolveBinding().getSuperclass().getDeclaredFields();
			for(IVariableBinding superclassField : superclassFields) {
				int i = 0;
				for(Expression expression : sourceFieldInstructions) {
					SimpleName simpleName = (SimpleName)expression;
					if(simpleName.getParent() instanceof QualifiedName && superclassField.isEqualTo(simpleName.resolveBinding())) {
						QualifiedName qualifiedName = (QualifiedName)simpleName.getParent();
						if(qualifiedName.getQualifier().resolveTypeBinding().isEqualTo(targetTypeDeclaration.resolveBinding().getSuperclass()) &&
								qualifiedName.getQualifier().getFullyQualifiedName().equals(targetClassVariableName)) {
							SimpleName newSimpleName = (SimpleName)newFieldInstructions.get(i);
							FieldAccess fieldAccess = newMethodDeclaration.getAST().newFieldAccess();
							targetRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, simpleName, null);
							targetRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, newMethodDeclaration.getAST().newThisExpression(), null);
							targetRewriter.replace(newSimpleName.getParent(), fieldAccess, null);
						}
					}
					else if(simpleName.getParent() instanceof FieldAccess && superclassField.isEqualTo(simpleName.resolveBinding())) {
						FieldAccess fieldAccess = (FieldAccess)simpleName.getParent();
						Expression fieldAccessExpression = fieldAccess.getExpression();
						if(fieldAccessExpression instanceof FieldAccess) {
							FieldAccess invokerFieldAccess = (FieldAccess)fieldAccessExpression;
							if(invokerFieldAccess.resolveTypeBinding().isEqualTo(targetTypeDeclaration.resolveBinding()) &&
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
	
	private void modifySourceStaticFieldInstructionsInTargetClass(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
		ExpressionExtractor extractor = new ExpressionExtractor();
		List<Expression> sourceVariableInstructions = extractor.getVariableInstructions(sourceMethod.getBody());
		List<Expression> newVariableInstructions = extractor.getVariableInstructions(newMethodDeclaration.getBody());
		int i = 0;
		for(Expression expression : sourceVariableInstructions) {
			SimpleName simpleName = (SimpleName)expression;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null && binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) != 0) {
					if(sourceTypeDeclaration.resolveBinding().isEqualTo(variableBinding.getDeclaringClass())) {
						AST ast = newMethodDeclaration.getAST();
						SimpleName qualifier = ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
						if(simpleName.getParent() instanceof FieldAccess) {
							FieldAccess fieldAccess = (FieldAccess)newVariableInstructions.get(i).getParent();
							targetRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, qualifier, null);
						}
						else if(RefactoringUtility.needsQualifier(simpleName)) {
							SimpleName newSimpleName = ast.newSimpleName(simpleName.getIdentifier());
							QualifiedName newQualifiedName = ast.newQualifiedName(qualifier, newSimpleName);
							targetRewriter.replace(newVariableInstructions.get(i), newQualifiedName, null);
						}
						this.additionalTypeBindingsToBeImportedInTargetClass.add(sourceTypeDeclaration.resolveBinding());
						setPublicModifierToSourceField(variableBinding);
					}
					else {
						AST ast = newMethodDeclaration.getAST();
						SimpleName qualifier = null;
						if((variableBinding.getModifiers() & Modifier.PUBLIC) != 0) {
							qualifier = ast.newSimpleName(variableBinding.getDeclaringClass().getName());
							this.additionalTypeBindingsToBeImportedInTargetClass.add(variableBinding.getDeclaringClass());
						}
						else {
							qualifier = ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
							this.additionalTypeBindingsToBeImportedInTargetClass.add(sourceTypeDeclaration.resolveBinding());
						}
						if(simpleName.getParent() instanceof FieldAccess) {
							FieldAccess fieldAccess = (FieldAccess)newVariableInstructions.get(i).getParent();
							targetRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, qualifier, null);
						}
						else if(RefactoringUtility.needsQualifier(simpleName)) {
							SimpleName newSimpleName = ast.newSimpleName(simpleName.getIdentifier());
							QualifiedName newQualifiedName = ast.newQualifiedName(qualifier, newSimpleName);
							targetRewriter.replace(newVariableInstructions.get(i), newQualifiedName, null);
						}
						ITypeBinding fieldDeclaringClass = variableBinding.getDeclaringClass();
						if(fieldDeclaringClass != null && fieldDeclaringClass.isEnum() && sourceTypeDeclaration.resolveBinding().isEqualTo(fieldDeclaringClass.getDeclaringClass())) {
							setPublicModifierToSourceMemberType(fieldDeclaringClass);
						}
					}
				}
			}
			i++;
		}
	}

	private void setPublicModifierToSourceMemberType(ITypeBinding typeBinding) {
		List<BodyDeclaration> bodyDeclarations = sourceTypeDeclaration.bodyDeclarations();
		for(BodyDeclaration bodyDeclaration : bodyDeclarations) {
			if(bodyDeclaration instanceof TypeDeclaration) {
				TypeDeclaration memberType = (TypeDeclaration)bodyDeclaration;
				ITypeBinding memberTypeBinding = memberType.resolveBinding();
				if(typeBinding.isEqualTo(memberTypeBinding)) {
					updateBodyDeclarationAccessModifier(memberType, TypeDeclaration.MODIFIERS2_PROPERTY);
				}
			}
			else if(bodyDeclaration instanceof EnumDeclaration) {
				EnumDeclaration memberEnum = (EnumDeclaration)bodyDeclaration;
				ITypeBinding memberTypeBinding = memberEnum.resolveBinding();
				if(typeBinding.isEqualTo(memberTypeBinding)) {
					updateBodyDeclarationAccessModifier(memberEnum, EnumDeclaration.MODIFIERS2_PROPERTY);
				}
			}
		}
	}

	private void updateBodyDeclarationAccessModifier(BodyDeclaration memberType, ChildListPropertyDescriptor childListPropertyDescriptor) {
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		ListRewrite modifierRewrite = sourceRewriter.getListRewrite(memberType, childListPropertyDescriptor);
		Modifier publicModifier = memberType.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
		boolean modifierFound = false;
		List<IExtendedModifier> modifiers = memberType.modifiers();
		for(IExtendedModifier extendedModifier : modifiers) {
			if(extendedModifier.isModifier()) {
				Modifier modifier = (Modifier)extendedModifier;
				if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PUBLIC_KEYWORD)) {
					modifierFound = true;
				}
				else if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PRIVATE_KEYWORD)) {
					if(!memberTypeDeclarationsChangedWithPublicModifier.contains(memberType)) {
						memberTypeDeclarationsChangedWithPublicModifier.add(memberType);
						modifierFound = true;
						modifierRewrite.replace(modifier, publicModifier, null);
						try {
							TextEdit sourceEdit = sourceRewriter.rewriteAST();
							sourceMultiTextEdit.addChild(sourceEdit);
							sourceCompilationUnitChange.addTextEditGroup(new TextEditGroup("Change access level to public", new TextEdit[] {sourceEdit}));
						} catch (JavaModelException e) {
							e.printStackTrace();
						}
					}
				}
				else if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PROTECTED_KEYWORD)) {
					modifierFound = true;
				}
			}
		}
		if(!modifierFound) {
			if(!memberTypeDeclarationsChangedWithPublicModifier.contains(memberType)) {
				memberTypeDeclarationsChangedWithPublicModifier.add(memberType);
				modifierRewrite.insertFirst(publicModifier, null);
				try {
					TextEdit sourceEdit = sourceRewriter.rewriteAST();
					sourceMultiTextEdit.addChild(sourceEdit);
					sourceCompilationUnitChange.addTextEditGroup(new TextEditGroup("Set access level to public", new TextEdit[] {sourceEdit}));
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void setPublicModifierToSourceField(IVariableBinding variableBinding) {
		FieldDeclaration[] fieldDeclarations = sourceTypeDeclaration.getFields();
		for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
			List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				boolean modifierIsReplaced = false;
				if(variableBinding.isEqualTo(fragment.resolveBinding())) {
					ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
					ListRewrite modifierRewrite = sourceRewriter.getListRewrite(fieldDeclaration, FieldDeclaration.MODIFIERS2_PROPERTY);
					Modifier publicModifier = fieldDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
					boolean modifierFound = false;
					List<IExtendedModifier> modifiers = fieldDeclaration.modifiers();
					for(IExtendedModifier extendedModifier : modifiers) {
						if(extendedModifier.isModifier()) {
							Modifier modifier = (Modifier)extendedModifier;
							if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PUBLIC_KEYWORD)) {
								modifierFound = true;
							}
							else if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PRIVATE_KEYWORD) ||
									modifier.getKeyword().equals(Modifier.ModifierKeyword.PROTECTED_KEYWORD)) {
								if(!fieldDeclarationsChangedWithPublicModifier.contains(fieldDeclaration)) {
									fieldDeclarationsChangedWithPublicModifier.add(fieldDeclaration);
									modifierFound = true;
									modifierRewrite.replace(modifier, publicModifier, null);
									modifierIsReplaced = true;
									try {
										TextEdit sourceEdit = sourceRewriter.rewriteAST();
										sourceMultiTextEdit.addChild(sourceEdit);
										sourceCompilationUnitChange.addTextEditGroup(new TextEditGroup("Change access level to public", new TextEdit[] {sourceEdit}));
									} catch (JavaModelException e) {
										e.printStackTrace();
									}
								}
							}
						}
					}
					if(!modifierFound) {
						if(!fieldDeclarationsChangedWithPublicModifier.contains(fieldDeclaration)) {
							fieldDeclarationsChangedWithPublicModifier.add(fieldDeclaration);
							modifierRewrite.insertFirst(publicModifier, null);
							modifierIsReplaced = true;
							try {
								TextEdit sourceEdit = sourceRewriter.rewriteAST();
								sourceMultiTextEdit.addChild(sourceEdit);
								sourceCompilationUnitChange.addTextEditGroup(new TextEditGroup("Set access level to public", new TextEdit[] {sourceEdit}));
							} catch (JavaModelException e) {
								e.printStackTrace();
							}
						}
					}
				}
				if(modifierIsReplaced)
					break;
			}
		}
	}

	private void modifySourceStaticMethodInvocationsInTargetClass(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
		ExpressionExtractor extractor = new ExpressionExtractor();	
		List<Expression> sourceMethodInvocations = extractor.getMethodInvocations(sourceMethod.getBody());
		List<Expression> newMethodInvocations = extractor.getMethodInvocations(newMethodDeclaration.getBody());
		int i = 0;
		for(Expression expression : sourceMethodInvocations) {
			if(expression instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)expression;
				IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
				if((methodBinding.getModifiers() & Modifier.STATIC) != 0 &&
						methodBinding.getDeclaringClass().isEqualTo(sourceTypeDeclaration.resolveBinding()) &&
						!additionalMethodsToBeMoved.containsKey(methodInvocation)) {
					AST ast = newMethodDeclaration.getAST();
					SimpleName qualifier = ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
					targetRewriter.set(newMethodInvocations.get(i), MethodInvocation.EXPRESSION_PROPERTY, qualifier, null);
					this.additionalTypeBindingsToBeImportedInTargetClass.add(sourceTypeDeclaration.resolveBinding());
				}
			}
			i++;
		}
	}

	private void modifySourceMemberAccessesInTargetClass(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
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
				if(methodInvocationDeclaringClassTypeBinding.isEqualTo(sourceTypeDeclaration.resolveBinding()) &&
						(methodInvocation.getExpression() == null || methodInvocation.getExpression() instanceof ThisExpression)) {
					MethodDeclaration[] sourceMethodDeclarations = sourceTypeDeclaration.getMethods();
					for(MethodDeclaration sourceMethodDeclaration : sourceMethodDeclarations) {
						if(sourceMethodDeclaration.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
							MethodInvocation delegation = MethodDeclarationUtility.isDelegate(sourceMethodDeclaration);
							if(delegation != null) {
								ITypeBinding delegationDeclaringClassTypeBinding = delegation.resolveMethodBinding().getDeclaringClass();
								if(delegationDeclaringClassTypeBinding.isEqualTo(targetTypeDeclaration.resolveBinding())) {
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
				else if(methodInvocationDeclaringClassTypeBinding.isEqualTo(targetTypeDeclaration.resolveBinding()) && methodInvocation.getExpression() != null) {
					Expression methodInvocationExpression = methodInvocation.getExpression();
					if(methodInvocationExpression instanceof MethodInvocation) {
						MethodInvocation invoker = (MethodInvocation)methodInvocationExpression;
						if(invoker.getExpression() == null || invoker.getExpression() instanceof ThisExpression) {
							MethodDeclaration[] sourceMethodDeclarations = sourceTypeDeclaration.getMethods();
							for(MethodDeclaration sourceMethodDeclaration : sourceMethodDeclarations) {
								if(sourceMethodDeclaration.resolveBinding().isEqualTo(invoker.resolveMethodBinding())) {
									SimpleName fieldInstruction = MethodDeclarationUtility.isGetter(sourceMethodDeclaration);
									if(fieldInstruction != null && fieldInstruction.resolveTypeBinding().isEqualTo(targetTypeDeclaration.resolveBinding())) {
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
			if(binding != null && binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) == 0) {
					if(sourceTypeDeclaration.resolveBinding().isEqualTo(variableBinding.getDeclaringClass())) {
						SimpleName expressionName = (SimpleName)newFieldInstructions.get(i);
						if(expressionName.getParent() instanceof FieldAccess) {
							FieldAccess fieldAccess = (FieldAccess)expressionName.getParent();
							if(fieldAccess.getExpression() instanceof ThisExpression && !expressionName.getIdentifier().equals(targetClassVariableName)) {
								targetRewriter.replace(expressionName.getParent(), expressionName, null);
								if(!additionalArgumentsAddedToMovedMethod.contains(expressionName.getIdentifier()))
									addParameterToMovedMethod(newMethodDeclaration, expressionName, targetRewriter);
							}
						}
						else if(!expressionName.getIdentifier().equals(targetClassVariableName) && !additionalArgumentsAddedToMovedMethod.contains(expressionName.getIdentifier()))
							addParameterToMovedMethod(newMethodDeclaration, expressionName, targetRewriter);
					}
					else {
						Type superclassType = sourceTypeDeclaration.getSuperclassType();
						ITypeBinding superclassTypeBinding = null;
						if(superclassType != null)
							superclassTypeBinding = superclassType.resolveBinding();
						while(superclassTypeBinding != null && !superclassTypeBinding.isEqualTo(variableBinding.getDeclaringClass())) {
							superclassTypeBinding = superclassTypeBinding.getSuperclass();
						}
						if(superclassTypeBinding != null) {
							IVariableBinding[] superclassFieldBindings = superclassTypeBinding.getDeclaredFields();
							for(IVariableBinding superclassFieldBinding : superclassFieldBindings) {
								if(superclassFieldBinding.isEqualTo(variableBinding)) {
									SimpleName expressionName = (SimpleName)newFieldInstructions.get(i);
									if(!expressionName.getIdentifier().equals(targetClassVariableName) && !additionalArgumentsAddedToMovedMethod.contains(expressionName.getIdentifier()))
										addParameterToMovedMethod(newMethodDeclaration, variableBinding, targetRewriter);
								}
							}
						}
					}
				}
			}
			i++;
		}
		SimpleName parameterName = null;
		Set<String> sourceMethodsWithPublicModifier = new LinkedHashSet<String>();
		int j = 0;
		for(Expression expression : sourceMethodInvocations) {
			if(expression instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)expression;
				if(methodInvocation.getExpression() == null || methodInvocation.getExpression() instanceof ThisExpression) {
					IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
					if(methodBinding.getDeclaringClass().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
						MethodDeclaration[] sourceMethodDeclarations = sourceTypeDeclaration.getMethods();
						for(MethodDeclaration sourceMethodDeclaration : sourceMethodDeclarations) {
							if(sourceMethodDeclaration.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding()) &&
									!sourceMethod.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
								SimpleName fieldName = MethodDeclarationUtility.isGetter(sourceMethodDeclaration);
								int modifiers = sourceMethodDeclaration.getModifiers();
								MethodInvocation newMethodInvocation = (MethodInvocation)newMethodInvocations.get(j);
								if((modifiers & Modifier.STATIC) != 0) {
									AST ast = newMethodDeclaration.getAST();
									targetRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier()), null);
									if(!sourceMethodsWithPublicModifier.contains(methodInvocation.resolveMethodBinding().getKey())) {
										setPublicModifierToSourceMethod(methodInvocation.resolveMethodBinding(), sourceTypeDeclaration);
										sourceMethodsWithPublicModifier.add(methodInvocation.resolveMethodBinding().getKey());
										Map<IMethodBinding, TypeDeclaration> subclassTypeDeclarationMap = findSubclassesOverridingMethod(sourceTypeDeclaration, methodInvocation.resolveMethodBinding());
										for(IMethodBinding methodBindingKey : subclassTypeDeclarationMap.keySet()) {
											setPublicModifierToSourceMethod(methodBindingKey, subclassTypeDeclarationMap.get(methodBindingKey));
										}
									}
								}
								else if(fieldName != null) {
									AST ast = newMethodDeclaration.getAST();
									targetRewriter.replace(newMethodInvocation, ast.newSimpleName(fieldName.getIdentifier()), null);
									if(!fieldName.getIdentifier().equals(targetClassVariableName) && !additionalArgumentsAddedToMovedMethod.contains(fieldName.getIdentifier()))
										addParameterToMovedMethod(newMethodDeclaration, fieldName, targetRewriter);
								}
								else {
									if(!additionalArgumentsAddedToMovedMethod.contains("this")) {
										parameterName = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
									}
									targetRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, parameterName, null);
									if(!sourceMethodsWithPublicModifier.contains(methodInvocation.resolveMethodBinding().getKey())) {
										setPublicModifierToSourceMethod(methodInvocation.resolveMethodBinding(), sourceTypeDeclaration);
										sourceMethodsWithPublicModifier.add(methodInvocation.resolveMethodBinding().getKey());
										Map<IMethodBinding, TypeDeclaration> subclassTypeDeclarationMap = findSubclassesOverridingMethod(sourceTypeDeclaration, methodInvocation.resolveMethodBinding());
										for(IMethodBinding methodBindingKey : subclassTypeDeclarationMap.keySet()) {
											setPublicModifierToSourceMethod(methodBindingKey, subclassTypeDeclarationMap.get(methodBindingKey));
										}
									}
								}
							}
						}
					}
					else {
						Type superclassType = sourceTypeDeclaration.getSuperclassType();
						ITypeBinding superclassTypeBinding = null;
						if(superclassType != null)
							superclassTypeBinding = superclassType.resolveBinding();
						while(superclassTypeBinding != null && !methodBinding.getDeclaringClass().isEqualTo(superclassTypeBinding)) {
							superclassTypeBinding = superclassTypeBinding.getSuperclass();
						}
						if(superclassTypeBinding != null) {
							IMethodBinding[] superclassMethodBindings = superclassTypeBinding.getDeclaredMethods();
							for(IMethodBinding superclassMethodBinding : superclassMethodBindings) {
								if(superclassMethodBinding.isEqualTo(methodBinding)) {
									MethodInvocation newMethodInvocation = (MethodInvocation)newMethodInvocations.get(j);
									if((superclassMethodBinding.getModifiers() & Modifier.STATIC) != 0) {
										AST ast = newMethodDeclaration.getAST();
										SimpleName qualifier = ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
										targetRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, qualifier, null);
									}
									else {
										if(!additionalArgumentsAddedToMovedMethod.contains("this")) {
											parameterName = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
										}
										targetRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, parameterName, null);
										if(!sourceMethodsWithPublicModifier.contains(methodBinding.getKey())) {
											TypeDeclaration superclassTypeDeclaration = RefactoringUtility.findDeclaringTypeDeclaration(superclassMethodBinding, sourceTypeDeclaration);
											if(superclassTypeDeclaration != null) {
												setPublicModifierToSourceMethod(methodInvocation.resolveMethodBinding(), superclassTypeDeclaration);
											}
											sourceMethodsWithPublicModifier.add(methodBinding.getKey());
										}
									}
								}
							}
						}
					}
				}
			}
			j++;
		}
	}

	private SimpleName addSourceClassParameterToMovedMethod(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
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
		this.additionalTypeBindingsToBeImportedInTargetClass.add(sourceTypeDeclaration.resolveBinding());
		addParamTagElementToJavadoc(newMethodDeclaration, targetRewriter, parameterName.getIdentifier());
		setPublicModifierToSourceTypeDeclaration();
		return parameterName;
	}

	private Map<IMethodBinding, TypeDeclaration> findSubclassesOverridingMethod(TypeDeclaration typeDeclaration, IMethodBinding methodBinding) {
		Map<IMethodBinding, TypeDeclaration> subclassTypeDeclarationMap = new LinkedHashMap<IMethodBinding, TypeDeclaration>();
		CompilationUnitCache cache = CompilationUnitCache.getInstance();
		Set<IType> subTypes = cache.getSubTypes((IType)typeDeclaration.resolveBinding().getJavaElement());
		for(IType iType : subTypes) {
			String fullyQualifiedTypeName = iType.getFullyQualifiedName();
			ClassObject classObject = ASTReader.getSystemObject().getClassObject(fullyQualifiedTypeName);
			if(classObject != null) {
				AbstractTypeDeclaration abstractTypeDeclaration = classObject.getAbstractTypeDeclaration();
				if(abstractTypeDeclaration instanceof TypeDeclaration) {
					TypeDeclaration subclassTypeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
					for(MethodDeclaration subclassMethodDeclaration : subclassTypeDeclaration.getMethods()) {
						if(MethodCallAnalyzer.equalSignature(subclassMethodDeclaration.resolveBinding(), methodBinding)) {
							subclassTypeDeclarationMap.put(subclassMethodDeclaration.resolveBinding(), subclassTypeDeclaration);
						}
					}
				}
			}
		}
		return subclassTypeDeclarationMap;
	}

	private void setPublicModifierToSourceTypeDeclaration() {
		PackageDeclaration sourcePackageDeclaration = sourceCompilationUnit.getPackage();
		PackageDeclaration targetPackageDeclaration = targetCompilationUnit.getPackage();
		if(sourcePackageDeclaration != null && targetPackageDeclaration != null) {
			String sourcePackageName = sourcePackageDeclaration.getName().getFullyQualifiedName();
			String targetPackageName = targetPackageDeclaration.getName().getFullyQualifiedName();
			if(!sourcePackageName.equals(targetPackageName)) {
				ASTRewrite sourceRewriter = ASTRewrite.create(sourceCompilationUnit.getAST());
				ListRewrite modifierRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
				Modifier publicModifier = sourceTypeDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
				boolean modifierFound = false;
				List<IExtendedModifier> modifiers = sourceTypeDeclaration.modifiers();
				for(IExtendedModifier extendedModifier : modifiers) {
					if(extendedModifier.isModifier()) {
						Modifier modifier = (Modifier)extendedModifier;
						if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PUBLIC_KEYWORD)) {
							modifierFound = true;
						}
						else if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PRIVATE_KEYWORD) ||
								modifier.getKeyword().equals(Modifier.ModifierKeyword.PROTECTED_KEYWORD)) {
							modifierFound = true;
							modifierRewrite.replace(modifier, publicModifier, null);
							try {
								TextEdit sourceEdit = sourceRewriter.rewriteAST();
								sourceMultiTextEdit.addChild(sourceEdit);
								sourceCompilationUnitChange.addTextEditGroup(new TextEditGroup("Change access level to public", new TextEdit[] {sourceEdit}));
							}
							catch(JavaModelException javaModelException) {
								javaModelException.printStackTrace();
							}
						}
					}
				}
				if(!modifierFound) {
					modifierRewrite.insertFirst(publicModifier, null);
					try {
						TextEdit sourceEdit = sourceRewriter.rewriteAST();
						sourceMultiTextEdit.addChild(sourceEdit);
						sourceCompilationUnitChange.addTextEditGroup(new TextEditGroup("Set access level to public", new TextEdit[] {sourceEdit}));
					}
					catch(JavaModelException javaModelException) {
						javaModelException.printStackTrace();
					}
				}
			}
		}
	}

	private void addParameterToMovedMethod(MethodDeclaration newMethodDeclaration, SimpleName fieldName, ASTRewrite targetRewriter) {
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
		this.additionalTypeBindingsToBeImportedInTargetClass.add(fieldType.resolveBinding());
		addParamTagElementToJavadoc(newMethodDeclaration, targetRewriter, fieldName.getIdentifier());
	}

	private void addParameterToMovedMethod(MethodDeclaration newMethodDeclaration, IVariableBinding variableBinding, ASTRewrite targetRewriter) {
		AST ast = newMethodDeclaration.getAST();
		SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
		ITypeBinding typeBinding = variableBinding.getType();
		Type fieldType = RefactoringUtility.generateTypeFromTypeBinding(typeBinding, ast, targetRewriter);
		targetRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, fieldType, null);
		targetRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, ast.newSimpleName(variableBinding.getName()), null);
		ListRewrite parametersRewrite = targetRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		parametersRewrite.insertLast(parameter, null);
		this.additionalArgumentsAddedToMovedMethod.add(variableBinding.getName());
		this.additionalTypeBindingsToBeImportedInTargetClass.add(variableBinding.getType());
		addParamTagElementToJavadoc(newMethodDeclaration, targetRewriter, variableBinding.getName());
	}

	private void setPublicModifierToSourceMethod(IMethodBinding methodBinding, TypeDeclaration sourceTypeDeclaration) {
		MethodDeclaration[] methodDeclarations = sourceTypeDeclaration.getMethods();
		for(MethodDeclaration methodDeclaration : methodDeclarations) {
			if(methodDeclaration.resolveBinding().isEqualTo(methodBinding)) {
				CompilationUnit sourceCompilationUnit = RefactoringUtility.findCompilationUnit(methodDeclaration);
				ASTRewrite sourceRewriter = ASTRewrite.create(sourceCompilationUnit.getAST());
				ListRewrite modifierRewrite = sourceRewriter.getListRewrite(methodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
				Modifier publicModifier = methodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
				boolean modifierFound = false;
				List<IExtendedModifier> modifiers = methodDeclaration.modifiers();
				for(IExtendedModifier extendedModifier : modifiers) {
					if(extendedModifier.isModifier()) {
						Modifier modifier = (Modifier)extendedModifier;
						if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PUBLIC_KEYWORD)) {
							modifierFound = true;
						}
						else if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PRIVATE_KEYWORD)) {
							modifierFound = true;
							modifierRewrite.replace(modifier, publicModifier, null);
							updateAccessModifier(sourceRewriter, sourceCompilationUnit);
						}
						else if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PROTECTED_KEYWORD)) {
							modifierFound = true;
							IPackageBinding targetTypeDeclarationPackageBinding = this.targetTypeDeclaration.resolveBinding().getPackage();
							IPackageBinding typeDeclarationPackageBinding = sourceTypeDeclaration.resolveBinding().getPackage();
							if(targetTypeDeclarationPackageBinding != null && typeDeclarationPackageBinding != null &&
									!targetTypeDeclarationPackageBinding.isEqualTo(typeDeclarationPackageBinding)) {
								modifierRewrite.replace(modifier, publicModifier, null);
								updateAccessModifier(sourceRewriter, sourceCompilationUnit);
							}
						}
					}
				}
				if(!modifierFound) {
					modifierRewrite.insertFirst(publicModifier, null);
					updateAccessModifier(sourceRewriter, sourceCompilationUnit);
				}
			}
		}
	}

	private void updateAccessModifier(ASTRewrite sourceRewriter, CompilationUnit sourceCompilationUnit) {
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
			CompilationUnitChange change = fChanges.get(sourceICompilationUnit);
			if(change == null) {
				MultiTextEdit sourceMultiTextEdit = new MultiTextEdit();
				change = new CompilationUnitChange("", sourceICompilationUnit);
				change.setEdit(sourceMultiTextEdit);
				fChanges.put(sourceICompilationUnit, change);
			}
			change.getEdit().addChild(sourceEdit);
			change.addTextEditGroup(new TextEditGroup("Update access modifier to public", new TextEdit[] {sourceEdit}));
		}
		catch(JavaModelException javaModelException) {
			javaModelException.printStackTrace();
		}
	}
	
	private void modifyRecursiveMethodInvocationsOfTheMovedMethod(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
		ExpressionExtractor extractor = new ExpressionExtractor();
		List<Expression> sourceMethodInvocations = extractor.getMethodInvocations(sourceMethod.getBody());
		List<Expression> newMethodInvocations = extractor.getMethodInvocations(newMethodDeclaration.getBody());
		int i = 0;
		for(Expression invocation : newMethodInvocations) {
			if(invocation instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)invocation;
				MethodInvocation sourceMethodInvocation = (MethodInvocation)sourceMethodInvocations.get(i);
				AST ast = newMethodDeclaration.getAST();
				if(sourceMethod.resolveBinding().isEqualTo(sourceMethodInvocation.resolveMethodBinding())) {
					targetRewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(movedMethodName), null);
					List<Expression> arguments = methodInvocation.arguments();
					boolean argumentFound = false;
					for(Expression argument : arguments) {
						SimpleName argumentSimpleName = null;
						if(argument instanceof SimpleName) {
							argumentSimpleName = (SimpleName)argument;
						}
						else if(argument instanceof FieldAccess) {
							FieldAccess fieldAccess = (FieldAccess)argument;
							argumentSimpleName = fieldAccess.getName();
						}
						if(argumentSimpleName != null) {
							ListRewrite argumentRewrite = targetRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
							if(argumentSimpleName.getIdentifier().equals(targetClassVariableName)) {
								argumentRewrite.remove(argument, null);
								argumentFound = true;
								break;
							}
						}
					}
					if(!argumentFound && isTargetClassVariableParameter) {
						List<Expression> sourceMethodInvocationArguments = sourceMethodInvocation.arguments();
						int j = 0;
						for(Expression argument : sourceMethodInvocationArguments) {
							SimpleName argumentSimpleName = null;
							if(argument instanceof SimpleName) {
								argumentSimpleName = (SimpleName)argument;
							}
							else if(argument instanceof FieldAccess) {
								FieldAccess fieldAccess = (FieldAccess)argument;
								argumentSimpleName = fieldAccess.getName();
							}
							if(argumentSimpleName != null) {
								ListRewrite argumentRewrite = targetRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
								if(( argumentSimpleName.resolveTypeBinding().isEqualTo(targetTypeDeclaration.resolveBinding()) ||
										targetTypeDeclaration.resolveBinding().isEqualTo(argumentSimpleName.resolveTypeBinding().getSuperclass()) ) &&
										targetClassVariableParameterIndex == j) {
									argumentRewrite.remove(arguments.get(j), null);
									targetRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, arguments.get(j), null);
									argumentFound = true;
									break;
								}
							}
							j++;
						}
					}
					if(!argumentFound && isTargetClassVariableParameter) {
						List<Expression> sourceMethodInvocationArguments = sourceMethodInvocation.arguments();
						int j = 0;
						for(Expression argument : sourceMethodInvocationArguments) {
							if(argument instanceof MethodInvocation) {
								MethodInvocation argumentMethodInvocation = (MethodInvocation)argument;
								ITypeBinding returnTypeBinding = argumentMethodInvocation.resolveMethodBinding().getReturnType();
								ListRewrite argumentRewrite = targetRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
								if(( returnTypeBinding.isEqualTo(targetTypeDeclaration.resolveBinding()) ||
										targetTypeDeclaration.resolveBinding().isEqualTo(returnTypeBinding.getSuperclass()) ) &&
										targetClassVariableParameterIndex == j) {
									argumentRewrite.remove(arguments.get(j), null);
									targetRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, arguments.get(j), null);
									break;
								}
							}
							j++;
						}
					}
				}
			}
			i++;
		}
	}
	
	private void replaceTargetClassVariableNameWithThisExpressionInMethodInvocationArguments(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
		ExpressionExtractor extractor = new ExpressionExtractor();
		List<Expression> sourceMethodInvocations = extractor.getMethodInvocations(sourceMethod.getBody());
		List<Expression> newMethodInvocations = extractor.getMethodInvocations(newMethodDeclaration.getBody());
		int i = 0;
		for(Expression invocation : newMethodInvocations) {
			if(invocation instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)invocation;
				List<Expression> arguments = methodInvocation.arguments();
				for(Expression argument : arguments) {
					if(argument instanceof SimpleName) {
						SimpleName simpleNameArgument = (SimpleName)argument;
						if(simpleNameArgument.getIdentifier().equals(targetClassVariableName)) {
							ListRewrite argumentRewrite = targetRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
							MethodInvocation sourceMethodInvocation = (MethodInvocation)sourceMethodInvocations.get(i);
							AST ast = newMethodDeclaration.getAST();
							if(!sourceMethod.resolveBinding().isEqualTo(sourceMethodInvocation.resolveMethodBinding())) {
								argumentRewrite.replace(argument, ast.newThisExpression(), null);
							}
						}
					}
					else if(argument instanceof FieldAccess) {
						FieldAccess fieldAccess = (FieldAccess)argument;
						SimpleName simpleNameArgument = fieldAccess.getName();
						if(simpleNameArgument.getIdentifier().equals(targetClassVariableName)) {
							ListRewrite argumentRewrite = targetRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
							MethodInvocation sourceMethodInvocation = (MethodInvocation)sourceMethodInvocations.get(i);
							AST ast = newMethodDeclaration.getAST();
							if(!sourceMethod.resolveBinding().isEqualTo(sourceMethodInvocation.resolveMethodBinding())) {
								argumentRewrite.replace(argument, ast.newThisExpression(), null);
							}
						}
					}
				}
			}
			i++;
		}
	}
	
	private void replaceTargetClassVariableNameWithThisExpressionInClassInstanceCreationArguments(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
		ExpressionExtractor extractor = new ExpressionExtractor();
		List<Expression> classInstanceCreations = extractor.getClassInstanceCreations(newMethodDeclaration.getBody());
		for(Expression creation : classInstanceCreations) {
			ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)creation;
			List<Expression> arguments = classInstanceCreation.arguments();
			for(Expression argument : arguments) {
				if(argument instanceof SimpleName) {
					SimpleName simpleNameArgument = (SimpleName)argument;
					if(simpleNameArgument.getIdentifier().equals(targetClassVariableName)) {
						ListRewrite argumentRewrite = targetRewriter.getListRewrite(classInstanceCreation, ClassInstanceCreation.ARGUMENTS_PROPERTY);
						AST ast = newMethodDeclaration.getAST();
						argumentRewrite.replace(argument, ast.newThisExpression(), null);
					}
				}
				else if(argument instanceof FieldAccess) {
					FieldAccess fieldAccess = (FieldAccess)argument;
					SimpleName simpleNameArgument = fieldAccess.getName();
					if(simpleNameArgument.getIdentifier().equals(targetClassVariableName)) {
						ListRewrite argumentRewrite = targetRewriter.getListRewrite(classInstanceCreation, ClassInstanceCreation.ARGUMENTS_PROPERTY);
						AST ast = newMethodDeclaration.getAST();
						argumentRewrite.replace(argument, ast.newThisExpression(), null);
					}
				}
			}
		}
	}
	
	private void replaceTargetClassVariableNameWithThisExpressionInVariableDeclarationInitializers(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
		StatementExtractor statementExtractor = new StatementExtractor();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<VariableDeclarationFragment> variableDeclarationFragments = new ArrayList<VariableDeclarationFragment>();
		List<Statement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(newMethodDeclaration.getBody());
		for(Statement statement : variableDeclarationStatements) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
			List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
			variableDeclarationFragments.addAll(fragments);
		}
		List<Expression> variableDeclarationExpressions = expressionExtractor.getVariableDeclarationExpressions(newMethodDeclaration.getBody());
		for(Expression expression : variableDeclarationExpressions) {
			VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)expression;
			List<VariableDeclarationFragment> fragments = variableDeclarationExpression.fragments();
			variableDeclarationFragments.addAll(fragments);
		}
		for(VariableDeclarationFragment fragment : variableDeclarationFragments) {
			Expression initializer = fragment.getInitializer();
			if(initializer instanceof SimpleName) {
				SimpleName simpleNameInitializer = (SimpleName)initializer;
				if(simpleNameInitializer.getIdentifier().equals(targetClassVariableName)) {
					AST ast = newMethodDeclaration.getAST();
					targetRewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, ast.newThisExpression(), null);
				}
			}
			else if(initializer instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)initializer;
				SimpleName simpleNameInitializer = fieldAccess.getName();
				if(simpleNameInitializer.getIdentifier().equals(targetClassVariableName)) {
					AST ast = newMethodDeclaration.getAST();
					targetRewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, ast.newThisExpression(), null);
				}
			}
		}
	}
	
	private void replaceTargetClassVariableNameWithThisExpressionInInfixExpressions(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
		ExpressionExtractor extractor = new ExpressionExtractor();
		List<Expression> infixExpressions = extractor.getInfixExpressions(newMethodDeclaration.getBody());
		for(Expression expression : infixExpressions) {
			InfixExpression infixExpression = (InfixExpression)expression;
			if(infixExpression.getLeftOperand() instanceof SimpleName) {
				SimpleName leftOperand = (SimpleName)infixExpression.getLeftOperand();
				if(leftOperand.getIdentifier().equals(targetClassVariableName)) {
					AST ast = newMethodDeclaration.getAST();
					targetRewriter.set(infixExpression, InfixExpression.LEFT_OPERAND_PROPERTY, ast.newThisExpression(), null);
				}	
			}
			else if(infixExpression.getLeftOperand() instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)infixExpression.getLeftOperand();
				SimpleName leftOperand = fieldAccess.getName();
				if(leftOperand.getIdentifier().equals(targetClassVariableName)) {
					AST ast = newMethodDeclaration.getAST();
					targetRewriter.set(infixExpression, InfixExpression.LEFT_OPERAND_PROPERTY, ast.newThisExpression(), null);
				}
			}
			if(infixExpression.getRightOperand() instanceof SimpleName) {
				SimpleName rightOperand = (SimpleName)infixExpression.getRightOperand();
				if(rightOperand.getIdentifier().equals(targetClassVariableName)) {
					AST ast = newMethodDeclaration.getAST();
					targetRewriter.set(infixExpression, InfixExpression.RIGHT_OPERAND_PROPERTY, ast.newThisExpression(), null);
				}	
			}
			else if(infixExpression.getRightOperand() instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)infixExpression.getRightOperand();
				SimpleName rightOperand = fieldAccess.getName();
				if(rightOperand.getIdentifier().equals(targetClassVariableName)) {
					AST ast = newMethodDeclaration.getAST();
					targetRewriter.set(infixExpression, InfixExpression.RIGHT_OPERAND_PROPERTY, ast.newThisExpression(), null);
				}
			}
		}
	}
	
	private void replaceTargetClassVariableNameWithThisExpressionInCastExpressions(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
		ExpressionExtractor extractor = new ExpressionExtractor();
		List<Expression> castExpressions = extractor.getCastExpressions(newMethodDeclaration.getBody());
		for(Expression expression : castExpressions) {
			CastExpression castExpression = (CastExpression)expression;
			if(castExpression.getExpression() instanceof SimpleName) {
				SimpleName simpleName = (SimpleName)castExpression.getExpression();
				if(simpleName.getIdentifier().equals(targetClassVariableName)) {
					AST ast = newMethodDeclaration.getAST();
					targetRewriter.set(castExpression, CastExpression.EXPRESSION_PROPERTY, ast.newThisExpression(), null);
				}
			}
			else if(castExpression.getExpression() instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)castExpression.getExpression();
				SimpleName simpleName = fieldAccess.getName();
				if(simpleName.getIdentifier().equals(targetClassVariableName)) {
					AST ast = newMethodDeclaration.getAST();
					targetRewriter.set(castExpression, CastExpression.EXPRESSION_PROPERTY, ast.newThisExpression(), null);
				}
			}
		}
	}
	
	private void replaceTargetClassVariableNameWithThisExpressionInInstanceofExpressions(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
		ExpressionExtractor extractor = new ExpressionExtractor();
		List<Expression> instanceofExpressions = extractor.getInstanceofExpressions(newMethodDeclaration.getBody());
		for(Expression expression : instanceofExpressions) {
			InstanceofExpression instanceofExpression = (InstanceofExpression)expression;
			if(instanceofExpression.getLeftOperand() instanceof SimpleName) {
				SimpleName simpleName = (SimpleName)instanceofExpression.getLeftOperand();
				if(simpleName.getIdentifier().equals(targetClassVariableName)) {
					AST ast = newMethodDeclaration.getAST();
					targetRewriter.set(instanceofExpression, InstanceofExpression.LEFT_OPERAND_PROPERTY, ast.newThisExpression(), null);
				}
			}
			else if(instanceofExpression.getLeftOperand() instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)instanceofExpression.getLeftOperand();
				SimpleName simpleName = fieldAccess.getName();
				if(simpleName.getIdentifier().equals(targetClassVariableName)) {
					AST ast = newMethodDeclaration.getAST();
					targetRewriter.set(instanceofExpression, InstanceofExpression.LEFT_OPERAND_PROPERTY, ast.newThisExpression(), null);
				}
			}
		}
	}
	
	private void replaceTargetClassVariableNameWithThisExpressionInAssignments(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
		ExpressionExtractor extractor = new ExpressionExtractor();
		List<Expression> assignments = extractor.getAssignments(newMethodDeclaration.getBody());
		for(Expression expression : assignments) {
			Assignment assignment = (Assignment)expression;
			if(assignment.getLeftHandSide() instanceof SimpleName) {
				SimpleName leftHandSide = (SimpleName)assignment.getLeftHandSide();
				if(leftHandSide.getIdentifier().equals(targetClassVariableName)) {
					AST ast = newMethodDeclaration.getAST();
					targetRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, ast.newThisExpression(), null);
				}	
			}
			else if(assignment.getLeftHandSide() instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)assignment.getLeftHandSide();
				SimpleName leftHandSide = fieldAccess.getName();
				if(leftHandSide.getIdentifier().equals(targetClassVariableName)) {
					AST ast = newMethodDeclaration.getAST();
					targetRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, ast.newThisExpression(), null);
				}
			}
			if(assignment.getRightHandSide() instanceof SimpleName) {
				SimpleName rightHandSide = (SimpleName)assignment.getRightHandSide();
				if(rightHandSide.getIdentifier().equals(targetClassVariableName)) {
					AST ast = newMethodDeclaration.getAST();
					targetRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, ast.newThisExpression(), null);
				}	
			}
			else if(assignment.getRightHandSide() instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)assignment.getRightHandSide();
				SimpleName rightHandSide = fieldAccess.getName();
				if(rightHandSide.getIdentifier().equals(targetClassVariableName)) {
					AST ast = newMethodDeclaration.getAST();
					targetRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, ast.newThisExpression(), null);
				}
			}
		}
	}

	private void replaceTargetClassVariableNameWithThisExpressionInReturnStatements(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
		StatementExtractor extractor = new StatementExtractor();
		List<Statement> returnStatements = extractor.getReturnStatements(newMethodDeclaration.getBody());
		for(Statement statement : returnStatements) {
			ReturnStatement returnStatement = (ReturnStatement)statement;
			if(returnStatement.getExpression() instanceof SimpleName) {
				SimpleName simpleName = (SimpleName)returnStatement.getExpression();
				if(simpleName.getIdentifier().equals(targetClassVariableName)) {
					AST ast = newMethodDeclaration.getAST();
					targetRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, ast.newThisExpression(), null);
				}
			}
			else if(returnStatement.getExpression() instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)returnStatement.getExpression();
				SimpleName simpleName = fieldAccess.getName();
				if(simpleName.getIdentifier().equals(targetClassVariableName)) {
					AST ast = newMethodDeclaration.getAST();
					targetRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, ast.newThisExpression(), null);
				}
			}
		}
	}

	private void replaceThisExpressionWithSourceClassParameterInMethodInvocationArguments(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
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
							parameterName = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
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
	
	private void replaceThisExpressionWithSourceClassParameterInClassInstanceCreationArguments(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
		ExpressionExtractor extractor = new ExpressionExtractor();
		List<Expression> classInstanceCreations = extractor.getClassInstanceCreations(newMethodDeclaration.getBody());
		for(Expression creation : classInstanceCreations) {
			ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)creation;
			List<Expression> arguments = classInstanceCreation.arguments();
			for(Expression argument : arguments) {
				if(argument instanceof ThisExpression) {
					SimpleName parameterName = null;
					if(!additionalArgumentsAddedToMovedMethod.contains("this")) {
						parameterName = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
					}
					else {
						AST ast = newMethodDeclaration.getAST();
						String sourceTypeName = sourceTypeDeclaration.getName().getIdentifier();
						parameterName = ast.newSimpleName(sourceTypeName.replaceFirst(Character.toString(sourceTypeName.charAt(0)), Character.toString(Character.toLowerCase(sourceTypeName.charAt(0)))));
					}
					ListRewrite argumentRewrite = targetRewriter.getListRewrite(classInstanceCreation, ClassInstanceCreation.ARGUMENTS_PROPERTY);
					argumentRewrite.replace(argument, parameterName, null);
				}
			}
		}
	}
	
	private void replaceThisExpressionWithSourceClassParameterInVariableDeclarationInitializers(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
		StatementExtractor statementExtractor = new StatementExtractor();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<VariableDeclarationFragment> variableDeclarationFragments = new ArrayList<VariableDeclarationFragment>();
		List<Statement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(newMethodDeclaration.getBody());
		for(Statement statement : variableDeclarationStatements) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
			List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
			variableDeclarationFragments.addAll(fragments);
		}
		List<Expression> variableDeclarationExpressions = expressionExtractor.getVariableDeclarationExpressions(newMethodDeclaration.getBody());
		for(Expression expression : variableDeclarationExpressions) {
			VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)expression;
			List<VariableDeclarationFragment> fragments = variableDeclarationExpression.fragments();
			variableDeclarationFragments.addAll(fragments);
		}
		for(VariableDeclarationFragment fragment : variableDeclarationFragments) {
			Expression initializer = fragment.getInitializer();
			if(initializer instanceof ThisExpression) {
				SimpleName parameterName = null;
				if(!additionalArgumentsAddedToMovedMethod.contains("this")) {
					parameterName = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
				}
				else {
					AST ast = newMethodDeclaration.getAST();
					String sourceTypeName = sourceTypeDeclaration.getName().getIdentifier();
					parameterName = ast.newSimpleName(sourceTypeName.replaceFirst(Character.toString(sourceTypeName.charAt(0)), Character.toString(Character.toLowerCase(sourceTypeName.charAt(0)))));
				}
				targetRewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, parameterName, null);
			}
		}
	}

	private void addParamTagElementToJavadoc(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter, String parameterToBeAdded) {
		if(newMethodDeclaration.getJavadoc() != null) {
			AST ast = newMethodDeclaration.getAST();
			Javadoc javadoc = newMethodDeclaration.getJavadoc();
			List<TagElement> tags = javadoc.tags();
			TagElement returnTagElement = null;
			for(TagElement tag : tags) {
				if(tag.getTagName() != null && tag.getTagName().equals(TagElement.TAG_RETURN)) {
					returnTagElement = tag;
					break;
				}
			}
			
			TagElement tagElement = ast.newTagElement();
			targetRewriter.set(tagElement, TagElement.TAG_NAME_PROPERTY, TagElement.TAG_PARAM, null);
			ListRewrite fragmentsRewrite = targetRewriter.getListRewrite(tagElement, TagElement.FRAGMENTS_PROPERTY);
			SimpleName paramName = ast.newSimpleName(parameterToBeAdded);
			fragmentsRewrite.insertLast(paramName, null);
			
			ListRewrite tagsRewrite = targetRewriter.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
			if(returnTagElement != null)
				tagsRewrite.insertBefore(tagElement, returnTagElement, null);
			else
				tagsRewrite.insertLast(tagElement, null);
		}
	}

	private void removeParamTagElementFromJavadoc(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter, String parameterToBeRemoved) {
		if(newMethodDeclaration.getJavadoc() != null) {
			Javadoc javadoc = newMethodDeclaration.getJavadoc();
			List<TagElement> tags = javadoc.tags();
			for(TagElement tag : tags) {
				if(tag.getTagName() != null && tag.getTagName().equals(TagElement.TAG_PARAM)) {
					List<ASTNode> tagFragments = tag.fragments();
					boolean paramFound = false;
					for(ASTNode node : tagFragments) {
						if(node instanceof SimpleName) {
							SimpleName simpleName = (SimpleName)node;
							if(simpleName.getIdentifier().equals(parameterToBeRemoved)) {
								paramFound = true;
								break;
							}
						}
					}
					if(paramFound) {
						ListRewrite tagsRewrite = targetRewriter.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
						tagsRewrite.remove(tag, null);
						break;
					}
				}
			}
		}
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 2);
			apply();
		} finally {
			pm.done();
		}
		return status;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 1);
		} finally {
			pm.done();
		}
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		try {
			pm.beginTask("Creating change...", 1);
			final Collection<CompilationUnitChange> changes = fChanges.values();
			CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {
				@Override
				public ChangeDescriptor getDescriptor() {
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
					String project = sourceICompilationUnit.getJavaProject().getElementName();
					String description = MessageFormat.format("Move method ''{0}''", new Object[] { sourceMethod.getName().getIdentifier()});
					String comment = MessageFormat.format("Move method ''{0}'' to ''{1}''", new Object[] { sourceMethod.getName().getIdentifier(), targetTypeDeclaration.getName().getIdentifier()});
					return new RefactoringChangeDescriptor(new MoveMethodRefactoringDescriptor(project, description, comment,
							sourceCompilationUnit, targetCompilationUnit,
							sourceTypeDeclaration, targetTypeDeclaration, sourceMethod,
							additionalMethodsToBeMoved, leaveDelegate, movedMethodName));
				}
			};
			return change;
		} finally {
			pm.done();
		}
	}

	@Override
	public String getName() {
		return "Move Method";
	}
}
