package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.MethodDeclarationUtility;
import gr.uom.java.ast.util.StatementExtractor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class ReplaceConditionalWithPolymorphism extends Refactoring {
	private IFile sourceFile;
	private CompilationUnit sourceCompilationUnit;
	private TypeDeclaration sourceTypeDeclaration;
	private TypeCheckElimination typeCheckElimination;
	private ASTRewrite sourceRewriter;
	private VariableDeclaration returnedVariable;
	private Set<ITypeBinding> requiredImportDeclarationsBasedOnSignature;
	private Set<ITypeBinding> thrownExceptions;
	private VariableDeclaration typeVariable;
	private MethodInvocation typeMethodInvocation;
	private Map<ICompilationUnit, TextFileChange> fChanges;
	private Set<IJavaElement> javaElementsToOpenInEditor;
	
	public ReplaceConditionalWithPolymorphism(IFile sourceFile, CompilationUnit sourceCompilationUnit,
			TypeDeclaration sourceTypeDeclaration, TypeCheckElimination typeCheckElimination) {
		this.sourceFile = sourceFile;
		this.sourceCompilationUnit = sourceCompilationUnit;
		this.sourceTypeDeclaration = sourceTypeDeclaration;
		this.typeCheckElimination = typeCheckElimination;
		this.sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		this.returnedVariable = typeCheckElimination.getTypeCheckMethodReturnedVariable();
		this.requiredImportDeclarationsBasedOnSignature = new LinkedHashSet<ITypeBinding>();
		this.thrownExceptions = typeCheckElimination.getThrownExceptions();
		if(typeCheckElimination.getTypeField() != null) {
			this.typeVariable = typeCheckElimination.getTypeField();
		}
		else if(typeCheckElimination.getTypeLocalVariable() != null) {
			this.typeVariable = typeCheckElimination.getTypeLocalVariable();
		}
		this.typeMethodInvocation = typeCheckElimination.getTypeMethodInvocation();
		this.fChanges = new LinkedHashMap<ICompilationUnit, TextFileChange>();
		this.javaElementsToOpenInEditor = new LinkedHashSet<IJavaElement>();
	}

	public Set<IJavaElement> getJavaElementsToOpenInEditor() {
		return javaElementsToOpenInEditor;
	}

	public void apply() {
		modifyInheritanceHierarchy();
		modifyClient();
	}

	private void modifyClient() {
		AST clientAST = sourceTypeDeclaration.getAST();
		Block typeCheckCodeFragmentParentBlock = (Block)typeCheckElimination.getTypeCheckCodeFragment().getParent();
		ListRewrite typeCheckCodeFragmentParentBlockStatementsRewrite = sourceRewriter.getListRewrite(typeCheckCodeFragmentParentBlock, Block.STATEMENTS_PROPERTY);
		if(returnedVariable == null && !typeCheckElimination.typeCheckCodeFragmentContainsReturnStatement()) {
			MethodInvocation abstractMethodInvocation = clientAST.newMethodInvocation();
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.NAME_PROPERTY, clientAST.newSimpleName(typeCheckElimination.getAbstractMethodName()), null);
			if(typeVariable != null)
				sourceRewriter.set(abstractMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, typeVariable.getName(), null);
			else if(typeMethodInvocation != null)
				sourceRewriter.set(abstractMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, typeMethodInvocation, null);
			ListRewrite methodInvocationArgumentsRewrite = sourceRewriter.getListRewrite(abstractMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
			for(SingleVariableDeclaration abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
				if(!abstractMethodParameter.equals(returnedVariable) && !abstractMethodParameter.equals(typeVariable)) {
					methodInvocationArgumentsRewrite.insertLast(abstractMethodParameter.getName(), null);
				}
			}
			for(VariableDeclaration fragment : typeCheckElimination.getAccessedLocalVariables()) {
				if(!fragment.equals(returnedVariable) && !fragment.equals(typeVariable)) {
					methodInvocationArgumentsRewrite.insertLast(fragment.getName(), null);
				}
			}
			if(typeCheckElimination.getAccessedFields().size() > 0 || typeCheckElimination.getAssignedFields().size() > 0 ||
					typeCheckElimination.getAccessedMethods().size() > 0 || typeCheckElimination.getSuperAccessedMethods().size() > 0 ||
					typeCheckElimination.getSuperAccessedFieldBindings().size() > 0 || typeCheckElimination.getSuperAssignedFieldBindings().size() > 0) {
				methodInvocationArgumentsRewrite.insertLast(clientAST.newThisExpression(), null);
			}
			ExpressionStatement expressionStatement = clientAST.newExpressionStatement(abstractMethodInvocation);
			typeCheckCodeFragmentParentBlockStatementsRewrite.replace(typeCheckElimination.getTypeCheckCodeFragment(), expressionStatement, null);
		}
		else {
			MethodInvocation abstractMethodInvocation = clientAST.newMethodInvocation();
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.NAME_PROPERTY, clientAST.newSimpleName(typeCheckElimination.getAbstractMethodName()), null);
			if(typeVariable != null)
				sourceRewriter.set(abstractMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, typeVariable.getName(), null);
			else if(typeMethodInvocation != null)
				sourceRewriter.set(abstractMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, typeMethodInvocation, null);
			ListRewrite methodInvocationArgumentsRewrite = sourceRewriter.getListRewrite(abstractMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
			if(returnedVariable != null) {
				if(returnedVariable instanceof SingleVariableDeclaration) {
					SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)returnedVariable;
					methodInvocationArgumentsRewrite.insertLast(singleVariableDeclaration.getName(), null);
				}
				else if(returnedVariable instanceof VariableDeclarationFragment) {
					VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)returnedVariable;
					methodInvocationArgumentsRewrite.insertLast(variableDeclarationFragment.getName(), null);
				}
			}
			for(SingleVariableDeclaration abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
				if(!abstractMethodParameter.equals(returnedVariable) && !abstractMethodParameter.equals(typeVariable)) {
					methodInvocationArgumentsRewrite.insertLast(abstractMethodParameter.getName(), null);
				}
			}
			for(VariableDeclaration fragment : typeCheckElimination.getAccessedLocalVariables()) {
				if(!fragment.equals(returnedVariable) && !fragment.equals(typeVariable)) {
					methodInvocationArgumentsRewrite.insertLast(fragment.getName(), null);
				}
			}
			if(typeCheckElimination.getAccessedFields().size() > 0 || typeCheckElimination.getAssignedFields().size() > 0 ||
					typeCheckElimination.getAccessedMethods().size() > 0 || typeCheckElimination.getSuperAccessedMethods().size() > 0 ||
					typeCheckElimination.getSuperAccessedFieldBindings().size() > 0 || typeCheckElimination.getSuperAssignedFieldBindings().size() > 0) {
				methodInvocationArgumentsRewrite.insertLast(clientAST.newThisExpression(), null);
			}
			if(returnedVariable != null) {
				Assignment assignment = clientAST.newAssignment();
				sourceRewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.ASSIGN, null);
				sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, returnedVariable.getName(), null);
				sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, abstractMethodInvocation, null);
				ExpressionStatement expressionStatement = clientAST.newExpressionStatement(assignment);
				typeCheckCodeFragmentParentBlockStatementsRewrite.replace(typeCheckElimination.getTypeCheckCodeFragment(), expressionStatement, null);
			}
			else {
				ReturnStatement returnStatement = clientAST.newReturnStatement();
				sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, abstractMethodInvocation, null);
				typeCheckCodeFragmentParentBlockStatementsRewrite.replace(typeCheckElimination.getTypeCheckCodeFragment(), returnStatement, null);
			}
		}
		
		generateGettersForAccessedFields();
		generateSettersForAssignedFields();
		setPublicModifierToAccessedMethods();
		
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
			TextFileChange change = fChanges.get(sourceICompilationUnit);
			if (change == null) {
				change = new TextFileChange(sourceICompilationUnit.getElementName(), (IFile)sourceICompilationUnit.getResource());
				change.setTextType("java");
				change.setEdit(sourceEdit);
			} else
				change.getEdit().addChild(sourceEdit);
			fChanges.put(sourceICompilationUnit, change);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	private void modifyInheritanceHierarchy() {
		IContainer contextContainer = (IContainer)sourceFile.getParent();
		PackageDeclaration contextPackageDeclaration = sourceCompilationUnit.getPackage();
		IContainer rootContainer = contextContainer;
		if(contextPackageDeclaration != null) {
			String packageName = contextPackageDeclaration.getName().getFullyQualifiedName();
			String[] subPackages = packageName.split("\\.");
			for(int i = 0; i<subPackages.length; i++)
				rootContainer = (IContainer)rootContainer.getParent();
		}
		String abstractClassFullyQualifiedName = typeCheckElimination.getAbstractClassName();
		IFile abstractClassFile = getFile(rootContainer, abstractClassFullyQualifiedName);
		
		IJavaElement abstractJavaElement = JavaCore.create(abstractClassFile);
		javaElementsToOpenInEditor.add(abstractJavaElement);
		ICompilationUnit abstractICompilationUnit = (ICompilationUnit)abstractJavaElement;
        ASTParser abstractParser = ASTParser.newParser(AST.JLS3);
        abstractParser.setKind(ASTParser.K_COMPILATION_UNIT);
        abstractParser.setSource(abstractICompilationUnit);
        abstractParser.setResolveBindings(true); // we need bindings later on
        CompilationUnit abstractCompilationUnit = (CompilationUnit)abstractParser.createAST(null);
        
        AST abstractAST = abstractCompilationUnit.getAST();
        ASTRewrite abstractRewriter = ASTRewrite.create(abstractAST);
		
		TypeDeclaration abstractClassTypeDeclaration = null;
		List<AbstractTypeDeclaration> abstractTypeDeclarations = abstractCompilationUnit.types();
		for(AbstractTypeDeclaration abstractTypeDeclaration : abstractTypeDeclarations) {
			if(abstractTypeDeclaration instanceof TypeDeclaration) {
				TypeDeclaration typeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
				if(typeDeclaration.resolveBinding().getQualifiedName().equals(typeCheckElimination.getAbstractClassName())) {
					abstractClassTypeDeclaration = typeDeclaration;
					break;
				}
			}
		}
		int abstractClassModifiers = abstractClassTypeDeclaration.getModifiers();
		if((abstractClassModifiers & Modifier.ABSTRACT) == 0 && !abstractClassTypeDeclaration.isInterface()) {
			ListRewrite abstractModifiersRewrite = abstractRewriter.getListRewrite(abstractClassTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
			abstractModifiersRewrite.insertLast(abstractAST.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
		}
		
		ListRewrite abstractBodyRewrite = abstractRewriter.getListRewrite(abstractClassTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		
		MethodDeclaration abstractMethodDeclaration = abstractAST.newMethodDeclaration();
		abstractRewriter.set(abstractMethodDeclaration, MethodDeclaration.NAME_PROPERTY, abstractAST.newSimpleName(typeCheckElimination.getAbstractMethodName()), null);
		if(returnedVariable == null && !typeCheckElimination.typeCheckCodeFragmentContainsReturnStatement()) {
			abstractRewriter.set(abstractMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, abstractAST.newPrimitiveType(PrimitiveType.VOID), null);
		}
		else {
			if(returnedVariable != null) {
				Type returnType = null;
				if(returnedVariable instanceof SingleVariableDeclaration) {
					SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)returnedVariable;
					returnType = singleVariableDeclaration.getType();
				}
				else if(returnedVariable instanceof VariableDeclarationFragment) {
					VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)returnedVariable;
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
					returnType = variableDeclarationStatement.getType();
				}
				abstractRewriter.set(abstractMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnType, null);
			}
			else {
				abstractRewriter.set(abstractMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, typeCheckElimination.getTypeCheckMethodReturnType(), null);
			}
		}
		ListRewrite abstractMethodModifiersRewrite = abstractRewriter.getListRewrite(abstractMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		abstractMethodModifiersRewrite.insertLast(abstractAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
		abstractMethodModifiersRewrite.insertLast(abstractAST.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
		ListRewrite abstractMethodParametersRewrite = abstractRewriter.getListRewrite(abstractMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		if(returnedVariable != null) {
			if(returnedVariable instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)returnedVariable;
				abstractMethodParametersRewrite.insertLast(singleVariableDeclaration, null);
			}
			else if(returnedVariable instanceof VariableDeclarationFragment) {
				SingleVariableDeclaration parameter = abstractAST.newSingleVariableDeclaration();
				VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)returnedVariable;
				VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
				abstractRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, variableDeclarationStatement.getType(), null);
				abstractRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, variableDeclarationFragment.getName(), null);
				abstractMethodParametersRewrite.insertLast(parameter, null);
			}
		}
		for(SingleVariableDeclaration abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {		
			if(!abstractMethodParameter.equals(returnedVariable) && !abstractMethodParameter.equals(typeVariable)) {
				abstractMethodParametersRewrite.insertLast(abstractMethodParameter, null);
			}
		}
		for(VariableDeclaration fragment : typeCheckElimination.getAccessedLocalVariables()) {
			if(!fragment.equals(returnedVariable) && !fragment.equals(typeVariable)) {
				if(fragment instanceof SingleVariableDeclaration) {
					SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)fragment;
					abstractMethodParametersRewrite.insertLast(singleVariableDeclaration, null);
				}
				else if(fragment instanceof VariableDeclarationFragment) {
					SingleVariableDeclaration parameter = abstractAST.newSingleVariableDeclaration();
					VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)fragment;
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
					abstractRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, variableDeclarationStatement.getType(), null);
					abstractRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, variableDeclarationFragment.getName(), null);
					abstractMethodParametersRewrite.insertLast(parameter, null);
				}
			}
		}
		Set<VariableDeclarationFragment> accessedFields = typeCheckElimination.getAccessedFields();
		Set<VariableDeclarationFragment> assignedFields = typeCheckElimination.getAssignedFields();
		Set<MethodDeclaration> accessedMethods = typeCheckElimination.getAccessedMethods();
		Set<IMethodBinding> superAccessedMethods = typeCheckElimination.getSuperAccessedMethods();
		Set<IVariableBinding> superAccessedFields = typeCheckElimination.getSuperAccessedFieldBindings();
		Set<IVariableBinding> superAssignedFields = typeCheckElimination.getSuperAssignedFieldBindings();
		if(accessedFields.size() > 0 || assignedFields.size() > 0 || accessedMethods.size() > 0 || superAccessedMethods.size() > 0 ||
				superAccessedFields.size() > 0 || superAssignedFields.size() > 0) {
			SingleVariableDeclaration parameter = abstractAST.newSingleVariableDeclaration();
			SimpleName parameterType = abstractAST.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
			abstractRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, abstractAST.newSimpleType(parameterType), null);
			String parameterName = sourceTypeDeclaration.getName().getIdentifier();
			parameterName = parameterName.substring(0,1).toLowerCase() + parameterName.substring(1,parameterName.length());
			abstractRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, abstractAST.newSimpleName(parameterName), null);
			abstractMethodParametersRewrite.insertLast(parameter, null);
		}
		
		ListRewrite abstractMethodThrownExceptionsRewrite = abstractRewriter.getListRewrite(abstractMethodDeclaration, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
		for(ITypeBinding typeBinding : thrownExceptions) {
			abstractMethodThrownExceptionsRewrite.insertLast(abstractAST.newSimpleName(typeBinding.getName()), null);
		}
		
		abstractBodyRewrite.insertLast(abstractMethodDeclaration, null);
		
		generateRequiredImportDeclarationsBasedOnSignature();
		for(ITypeBinding typeBinding : requiredImportDeclarationsBasedOnSignature) {
			addImportDeclaration(typeBinding, abstractCompilationUnit, abstractRewriter);
		}
		
		try {
			TextEdit abstractEdit = abstractRewriter.rewriteAST();
			TextFileChange change = fChanges.get(abstractICompilationUnit);
			if (change == null) {
				change = new TextFileChange(abstractICompilationUnit.getElementName(), (IFile)abstractICompilationUnit.getResource());
				change.setTextType("java");
				change.setEdit(abstractEdit);
			} else
				change.getEdit().addChild(abstractEdit);
			fChanges.put(abstractICompilationUnit, change);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		
		List<ArrayList<Statement>> typeCheckStatements = typeCheckElimination.getTypeCheckStatements();
		List<String> subclassNames = typeCheckElimination.getSubclassNames();
		DefaultMutableTreeNode root = typeCheckElimination.getExistingInheritanceTree().getRootNode();
		Enumeration<DefaultMutableTreeNode> enumeration = root.children();
		while(enumeration.hasMoreElements()) {
			DefaultMutableTreeNode child = enumeration.nextElement();
			String childClassName = (String)child.getUserObject();
			if(!subclassNames.contains(childClassName))
				subclassNames.add(childClassName);
		}
		for(int i=0; i<subclassNames.size(); i++) {
			ArrayList<Statement> statements = null;
			DefaultMutableTreeNode remainingIfStatementExpression = null;
			if(i < typeCheckStatements.size()) {
				statements = typeCheckStatements.get(i);
				Expression expression = typeCheckElimination.getExpressionCorrespondingToTypeCheckStatementList(statements);
				remainingIfStatementExpression = typeCheckElimination.getRemainingIfStatementExpression(expression);
			}
			else {
				statements = typeCheckElimination.getDefaultCaseStatements();
			}
			IFile subclassFile = getFile(rootContainer, subclassNames.get(i));
			IJavaElement subclassJavaElement = JavaCore.create(subclassFile);
			javaElementsToOpenInEditor.add(subclassJavaElement);
			ICompilationUnit subclassICompilationUnit = (ICompilationUnit)subclassJavaElement;
	        ASTParser subclassParser = ASTParser.newParser(AST.JLS3);
	        subclassParser.setKind(ASTParser.K_COMPILATION_UNIT);
	        subclassParser.setSource(subclassICompilationUnit);
	        subclassParser.setResolveBindings(true); // we need bindings later on
	        CompilationUnit subclassCompilationUnit = (CompilationUnit)subclassParser.createAST(null);
	        
	        AST subclassAST = subclassCompilationUnit.getAST();
	        ASTRewrite subclassRewriter = ASTRewrite.create(subclassAST);
			
			TypeDeclaration subclassTypeDeclaration = null;
			List<AbstractTypeDeclaration> subclassAbstractTypeDeclarations = subclassCompilationUnit.types();
			for(AbstractTypeDeclaration abstractTypeDeclaration : subclassAbstractTypeDeclarations) {
				if(abstractTypeDeclaration instanceof TypeDeclaration) {
					TypeDeclaration typeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
					if(typeDeclaration.resolveBinding().getQualifiedName().equals(subclassNames.get(i))) {
						subclassTypeDeclaration = typeDeclaration;
						break;
					}
				}
			}
			
			ListRewrite subclassBodyRewrite = subclassRewriter.getListRewrite(subclassTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			
			MethodDeclaration concreteMethodDeclaration = subclassAST.newMethodDeclaration();
			subclassRewriter.set(concreteMethodDeclaration, MethodDeclaration.NAME_PROPERTY, subclassAST.newSimpleName(typeCheckElimination.getAbstractMethodName()), null);
			if(returnedVariable == null && !typeCheckElimination.typeCheckCodeFragmentContainsReturnStatement()) {
				subclassRewriter.set(concreteMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, subclassAST.newPrimitiveType(PrimitiveType.VOID), null);
			}
			else {
				if(returnedVariable != null) {
					Type returnType = null;
					if(returnedVariable instanceof SingleVariableDeclaration) {
						SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)returnedVariable;
						returnType = singleVariableDeclaration.getType();
					}
					else if(returnedVariable instanceof VariableDeclarationFragment) {
						VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)returnedVariable;
						VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
						returnType = variableDeclarationStatement.getType();
					}
					subclassRewriter.set(concreteMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnType, null);
				}
				else {
					subclassRewriter.set(concreteMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, typeCheckElimination.getTypeCheckMethodReturnType(), null);
				}
			}
			ListRewrite concreteMethodModifiersRewrite = subclassRewriter.getListRewrite(concreteMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
			concreteMethodModifiersRewrite.insertLast(subclassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
			ListRewrite concreteMethodParametersRewrite = subclassRewriter.getListRewrite(concreteMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
			if(returnedVariable != null) {
				if(returnedVariable instanceof SingleVariableDeclaration) {
					SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)returnedVariable;
					concreteMethodParametersRewrite.insertLast(singleVariableDeclaration, null);
				}
				else if(returnedVariable instanceof VariableDeclarationFragment) {
					SingleVariableDeclaration parameter = subclassAST.newSingleVariableDeclaration();
					VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)returnedVariable;
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
					subclassRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, variableDeclarationStatement.getType(), null);
					subclassRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, variableDeclarationFragment.getName(), null);
					concreteMethodParametersRewrite.insertLast(parameter, null);
				}
			}
			for(SingleVariableDeclaration abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
				if(!abstractMethodParameter.equals(returnedVariable) && !abstractMethodParameter.equals(typeVariable)) {
					concreteMethodParametersRewrite.insertLast(abstractMethodParameter, null);
				}
			}
			for(VariableDeclaration fragment : typeCheckElimination.getAccessedLocalVariables()) {
				if(!fragment.equals(returnedVariable) && !fragment.equals(typeVariable)) {
					if(fragment instanceof SingleVariableDeclaration) {
						SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)fragment;
						concreteMethodParametersRewrite.insertLast(singleVariableDeclaration, null);
					}
					else if(fragment instanceof VariableDeclarationFragment) {
						SingleVariableDeclaration parameter = subclassAST.newSingleVariableDeclaration();
						VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)fragment;
						VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
						subclassRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, variableDeclarationStatement.getType(), null);
						subclassRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, variableDeclarationFragment.getName(), null);
						concreteMethodParametersRewrite.insertLast(parameter, null);
					}
				}
			}
			if(accessedFields.size() > 0 || assignedFields.size() > 0 || accessedMethods.size() > 0 || superAccessedMethods.size() > 0 ||
					superAccessedFields.size() > 0 || superAssignedFields.size() > 0) {
				SingleVariableDeclaration parameter = subclassAST.newSingleVariableDeclaration();
				SimpleName parameterType = subclassAST.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
				subclassRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, subclassAST.newSimpleType(parameterType), null);
				String parameterName = sourceTypeDeclaration.getName().getIdentifier();
				parameterName = parameterName.substring(0,1).toLowerCase() + parameterName.substring(1,parameterName.length());
				subclassRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, subclassAST.newSimpleName(parameterName), null);
				concreteMethodParametersRewrite.insertLast(parameter, null);
			}
			
			ListRewrite concreteMethodThrownExceptionsRewrite = subclassRewriter.getListRewrite(concreteMethodDeclaration, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
			for(ITypeBinding typeBinding : thrownExceptions) {
				concreteMethodThrownExceptionsRewrite.insertLast(subclassAST.newSimpleName(typeBinding.getName()), null);
			}

			Block concreteMethodBody = subclassAST.newBlock();
			ListRewrite concreteMethodBodyRewrite = subclassRewriter.getListRewrite(concreteMethodBody, Block.STATEMENTS_PROPERTY);
			ExpressionExtractor expressionExtractor = new ExpressionExtractor();
			ListRewrite ifStatementBodyRewrite = null;
			if(remainingIfStatementExpression != null) {
				IfStatement enclosingIfStatement = subclassAST.newIfStatement();
				Expression enclosingIfStatementExpression = constructExpression(subclassAST, remainingIfStatementExpression);
				Expression newEnclosingIfStatementExpression = (Expression)ASTNode.copySubtree(subclassAST, enclosingIfStatementExpression);
				List<Expression> oldVariableInstructions = expressionExtractor.getVariableInstructions(enclosingIfStatementExpression);
				List<Expression> newVariableInstructions = expressionExtractor.getVariableInstructions(newEnclosingIfStatementExpression);
				modifySourceVariableInstructionsInSubclass(oldVariableInstructions, newVariableInstructions, subclassAST, subclassRewriter,
						accessedFields, assignedFields, superAccessedFields, superAssignedFields);
				List<Expression> oldMethodInvocations = expressionExtractor.getMethodInvocations(enclosingIfStatementExpression);
				List<Expression> newMethodInvocations = expressionExtractor.getMethodInvocations(newEnclosingIfStatementExpression);
				modifySourceMethodInvocationsInSubclass(oldMethodInvocations, newMethodInvocations, subclassAST, subclassRewriter, accessedMethods, superAccessedMethods);
				modifySubclassMethodInvocations(oldMethodInvocations, newMethodInvocations, subclassAST, subclassRewriter, subclassTypeDeclaration, null);
				replaceThisExpressionWithContextParameterInMethodInvocationArguments(newMethodInvocations, subclassAST, subclassRewriter);
				subclassRewriter.set(enclosingIfStatement, IfStatement.EXPRESSION_PROPERTY, newEnclosingIfStatementExpression, null);
				Block ifStatementBody = subclassAST.newBlock();
				ifStatementBodyRewrite = subclassRewriter.getListRewrite(ifStatementBody, Block.STATEMENTS_PROPERTY);
				subclassRewriter.set(enclosingIfStatement, IfStatement.THEN_STATEMENT_PROPERTY, ifStatementBody, null);
				concreteMethodBodyRewrite.insertLast(enclosingIfStatement, null);
			}
			SimpleName subclassCastInvoker = null;
			for(Statement statement : statements) {
				Statement newStatement = (Statement)ASTNode.copySubtree(subclassAST, statement);
				boolean insert = true;
				if(statement instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
					List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
					VariableDeclarationFragment fragment = fragments.get(0);
					if(fragment.getInitializer() instanceof CastExpression) {
						CastExpression castExpression = (CastExpression)fragment.getInitializer();
						if(castExpression.getType().resolveBinding().isEqualTo(subclassTypeDeclaration.resolveBinding())) {
							if(castExpression.getExpression() instanceof SimpleName) {
								SimpleName castSimpleName = (SimpleName)castExpression.getExpression();
								if(typeVariable != null && typeVariable.getName().resolveBinding().isEqualTo(castSimpleName.resolveBinding())) {
									subclassCastInvoker = fragment.getName();
									insert = false;
								}
							}
							else if(castExpression.getExpression() instanceof MethodInvocation) {
								MethodInvocation castMethodInvocation = (MethodInvocation)castExpression.getExpression();
								if(typeMethodInvocation != null && typeMethodInvocation.subtreeMatch(new ASTMatcher(), castMethodInvocation)) {
									subclassCastInvoker = fragment.getName();
									insert = false;
								}
							}
						}
					}
				}
				else {
					StatementExtractor statementExtractor = new StatementExtractor();
					List<Statement> oldVariableDeclarations = statementExtractor.getVariableDeclarations(statement);
					List<Statement> newVariableDeclarations = statementExtractor.getVariableDeclarations(newStatement);
					int j = 0;
					for(Statement oldVariableDeclaration : oldVariableDeclarations) {
						VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)oldVariableDeclaration;
						List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
						VariableDeclarationFragment fragment = fragments.get(0);
						if(fragment.getInitializer() instanceof CastExpression) {
							CastExpression castExpression = (CastExpression)fragment.getInitializer();
							if(castExpression.getType().resolveBinding().isEqualTo(subclassTypeDeclaration.resolveBinding())) {
								if(castExpression.getExpression() instanceof SimpleName) {
									SimpleName castSimpleName = (SimpleName)castExpression.getExpression();
									if(typeVariable != null && typeVariable.getName().resolveBinding().isEqualTo(castSimpleName.resolveBinding())) {
										subclassCastInvoker = fragment.getName();
										subclassRewriter.remove(newVariableDeclarations.get(j), null);
										break;
									}
								}
								else if(castExpression.getExpression() instanceof MethodInvocation) {
									MethodInvocation castMethodInvocation = (MethodInvocation)castExpression.getExpression();
									if(typeMethodInvocation != null && typeMethodInvocation.subtreeMatch(new ASTMatcher(), castMethodInvocation)) {
										subclassCastInvoker = fragment.getName();
										subclassRewriter.remove(newVariableDeclarations.get(j), null);
										break;
									}
								}
							}
						}
						j++;
					}
				}
				List<Expression> oldVariableInstructions = expressionExtractor.getVariableInstructions(statement);
				List<Expression> newVariableInstructions = expressionExtractor.getVariableInstructions(newStatement);
				modifySourceVariableInstructionsInSubclass(oldVariableInstructions, newVariableInstructions, subclassAST, subclassRewriter,
						accessedFields, assignedFields, superAccessedFields, superAssignedFields);
				List<Expression> oldMethodInvocations = expressionExtractor.getMethodInvocations(statement);
				List<Expression> newMethodInvocations = expressionExtractor.getMethodInvocations(newStatement);
				modifySourceMethodInvocationsInSubclass(oldMethodInvocations, newMethodInvocations, subclassAST, subclassRewriter, accessedMethods, superAccessedMethods);
				modifySubclassMethodInvocations(oldMethodInvocations, newMethodInvocations, subclassAST, subclassRewriter, subclassTypeDeclaration, subclassCastInvoker);
				replaceThisExpressionWithContextParameterInMethodInvocationArguments(newMethodInvocations, subclassAST, subclassRewriter);
				replaceThisExpressionWithContextParameterInClassInstanceCreationArguments(newStatement, subclassAST, subclassRewriter);
				if(insert) {
					if(ifStatementBodyRewrite != null)
						ifStatementBodyRewrite.insertLast(newStatement, null);
					else
						concreteMethodBodyRewrite.insertLast(newStatement, null);
				}
			}
			if(returnedVariable != null) {
				ReturnStatement returnStatement = subclassAST.newReturnStatement();
				subclassRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, returnedVariable.getName(), null);
				concreteMethodBodyRewrite.insertLast(returnStatement, null);
			}
			subclassRewriter.set(concreteMethodDeclaration, MethodDeclaration.BODY_PROPERTY, concreteMethodBody, null);
			
			subclassBodyRewrite.insertLast(concreteMethodDeclaration, null);
			
			for(ITypeBinding typeBinding : requiredImportDeclarationsBasedOnSignature) {
				addImportDeclaration(typeBinding, subclassCompilationUnit, subclassRewriter);
			}
			Set<ITypeBinding> requiredImportDeclarationsBasedOnBranch = generateRequiredImportDeclarationsBasedOnBranch(statements);
			for(ITypeBinding typeBinding : requiredImportDeclarationsBasedOnBranch) {
				if(!requiredImportDeclarationsBasedOnSignature.contains(typeBinding))
					addImportDeclaration(typeBinding, subclassCompilationUnit, subclassRewriter);
			}
			
			try {
				TextEdit subclassEdit = subclassRewriter.rewriteAST();
				TextFileChange change = fChanges.get(subclassICompilationUnit);
				if (change == null) {
					change = new TextFileChange(subclassICompilationUnit.getElementName(), (IFile)subclassICompilationUnit.getResource());
					change.setTextType("java");
					change.setEdit(subclassEdit);
				} else
					change.getEdit().addChild(subclassEdit);
				fChanges.put(subclassICompilationUnit, change);
			} catch (MalformedTreeException e) {
				e.printStackTrace();
			} catch (JavaModelException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
	}

	private void modifySubclassMethodInvocations(List<Expression> oldMethodInvocations, List<Expression> newMethodInvocations, AST subclassAST,
			ASTRewrite subclassRewriter, TypeDeclaration subclassTypeDeclaration, SimpleName subclassCastInvoker) {
		int j = 0;
		for(Expression expression : newMethodInvocations) {
			if(expression instanceof MethodInvocation) {
				MethodInvocation newMethodInvocation = (MethodInvocation)expression;
				MethodInvocation oldMethodInvocation = (MethodInvocation)oldMethodInvocations.get(j);
				List<Expression> newMethodInvocationExpressions = new ArrayList<Expression>();
				List<Expression> oldMethodInvocationExpressions = new ArrayList<Expression>();
				Expression newMethodInvocationExpression = newMethodInvocation.getExpression();
				if(newMethodInvocationExpression != null)
					newMethodInvocationExpressions.add(newMethodInvocationExpression);
				Expression oldMethodInvocationExpression = oldMethodInvocation.getExpression();
				if(oldMethodInvocationExpression != null)
					oldMethodInvocationExpressions.add(oldMethodInvocationExpression);
				newMethodInvocationExpressions.addAll(newMethodInvocation.arguments());
				oldMethodInvocationExpressions.addAll(oldMethodInvocation.arguments());
				
				int k = 0;
				for(Expression oldExpression : oldMethodInvocationExpressions) {
					if(oldExpression instanceof SimpleName) {
						SimpleName invoker = (SimpleName)oldExpression;
						if(typeVariable != null && typeVariable.resolveBinding().isEqualTo(invoker.resolveBinding())) {
							subclassRewriter.replace(newMethodInvocationExpressions.get(k), subclassAST.newThisExpression(), null);
						}
						if(subclassCastInvoker != null && subclassCastInvoker.resolveBinding().isEqualTo(invoker.resolveBinding())) {
							subclassRewriter.replace(newMethodInvocationExpressions.get(k), subclassAST.newThisExpression(), null);
						}
					}
					else if(oldExpression instanceof MethodInvocation) {
						MethodInvocation invoker = (MethodInvocation)oldExpression;
						if(typeMethodInvocation != null && typeMethodInvocation.subtreeMatch(new ASTMatcher(), invoker)) {
							subclassRewriter.replace(newMethodInvocationExpressions.get(k), subclassAST.newThisExpression(), null);
						}
					}
					else if(oldExpression instanceof ParenthesizedExpression) {
						ParenthesizedExpression oldParenthesizedExpression = (ParenthesizedExpression)oldExpression;
						if(oldParenthesizedExpression.getExpression() instanceof CastExpression) {
							CastExpression oldCastExpression = (CastExpression)oldParenthesizedExpression.getExpression();
							if(oldCastExpression.getType().resolveBinding().isEqualTo(subclassTypeDeclaration.resolveBinding())) {
								if(oldCastExpression.getExpression() instanceof SimpleName) {
									SimpleName castSimpleName = (SimpleName)oldCastExpression.getExpression();
									if(typeVariable != null && typeVariable.getName().resolveBinding().isEqualTo(castSimpleName.resolveBinding())) {
										subclassRewriter.replace(newMethodInvocationExpressions.get(k), subclassAST.newThisExpression(), null);
									}
								}
								else if(oldCastExpression.getExpression() instanceof MethodInvocation) {
									MethodInvocation castMethodInvocation = (MethodInvocation)oldCastExpression.getExpression();
									if(typeMethodInvocation != null && typeMethodInvocation.subtreeMatch(new ASTMatcher(), castMethodInvocation)) {
										subclassRewriter.replace(newMethodInvocationExpressions.get(k), subclassAST.newThisExpression(), null);
									}
								}
							}
						}
					}
					k++;
				}
			}
			j++;
		}
	}

	private void modifySourceMethodInvocationsInSubclass(List<Expression> oldMethodInvocations, List<Expression> newMethodInvocations, AST subclassAST,
			ASTRewrite subclassRewriter, Set<MethodDeclaration> accessedMethods, Set<IMethodBinding> superAccessedMethods) {
		int j = 0;
		for(Expression expression : newMethodInvocations) {
			if(expression instanceof MethodInvocation) {
				MethodInvocation newMethodInvocation = (MethodInvocation)expression;
				MethodInvocation oldMethodInvocation = (MethodInvocation)oldMethodInvocations.get(j);
				for(MethodDeclaration methodDeclaration : accessedMethods) {
					if(oldMethodInvocation.resolveMethodBinding().isEqualTo(methodDeclaration.resolveBinding())) {
						String invokerName = sourceTypeDeclaration.getName().getIdentifier();
						invokerName = invokerName.substring(0,1).toLowerCase() + invokerName.substring(1,invokerName.length());
						subclassRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
						break;
					}
				}
				for(IMethodBinding superMethodBinding : superAccessedMethods) {
					if(superMethodBinding.isEqualTo(oldMethodInvocation.resolveMethodBinding())) {
						if(oldMethodInvocation.getExpression() == null ||
								(oldMethodInvocation.getExpression() != null && oldMethodInvocation.getExpression() instanceof ThisExpression)) {
							String invokerName = sourceTypeDeclaration.getName().getIdentifier();
							invokerName = invokerName.substring(0,1).toLowerCase() + invokerName.substring(1,invokerName.length());
							subclassRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
							break;
						}
					}
				}
			}
			j++;
		}
	}

	private void replaceThisExpressionWithContextParameterInMethodInvocationArguments(List<Expression> newMethodInvocations, AST subclassAST, ASTRewrite subclassRewriter) {
		for(Expression expression : newMethodInvocations) {
			if(expression instanceof MethodInvocation) {
				MethodInvocation newMethodInvocation = (MethodInvocation)expression;
				List<Expression> arguments = newMethodInvocation.arguments();
				for(Expression argument : arguments) {
					if(argument instanceof ThisExpression) {
						String parameterName = sourceTypeDeclaration.getName().getIdentifier();
						parameterName = parameterName.substring(0,1).toLowerCase() + parameterName.substring(1,parameterName.length());
						ListRewrite argumentsRewrite = subclassRewriter.getListRewrite(newMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
						argumentsRewrite.replace(argument, subclassAST.newSimpleName(parameterName), null);
					}
				}
			}
		}
	}

	private void replaceThisExpressionWithContextParameterInClassInstanceCreationArguments(Statement newStatement, AST subclassAST, ASTRewrite subclassRewriter) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(newStatement);
		for(Expression creation : classInstanceCreations) {
			ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)creation;
			List<Expression> arguments = classInstanceCreation.arguments();
			for(Expression argument : arguments) {
				if(argument instanceof ThisExpression) {
					String parameterName = sourceTypeDeclaration.getName().getIdentifier();
					parameterName = parameterName.substring(0,1).toLowerCase() + parameterName.substring(1,parameterName.length());
					ListRewrite argumentsRewrite = subclassRewriter.getListRewrite(classInstanceCreation, ClassInstanceCreation.ARGUMENTS_PROPERTY);
					argumentsRewrite.replace(argument, subclassAST.newSimpleName(parameterName), null);
				}
			}
		}
	}

	private void modifySourceVariableInstructionsInSubclass(List<Expression> oldVariableInstructions, List<Expression> newVariableInstructions, AST subclassAST, ASTRewrite subclassRewriter,
			Set<VariableDeclarationFragment> accessedFields, Set<VariableDeclarationFragment> assignedFields, Set<IVariableBinding> superAccessedFields, Set<IVariableBinding> superAssignedFields) {
		int j = 0;
		Set<IVariableBinding> accessedFieldBindings = new LinkedHashSet<IVariableBinding>();
		for(VariableDeclarationFragment fragment : accessedFields) {
			accessedFieldBindings.add(fragment.resolveBinding());
		}
		accessedFieldBindings.addAll(superAccessedFields);
		Set<IVariableBinding> assignedFieldBindings = new LinkedHashSet<IVariableBinding>();
		for(VariableDeclarationFragment fragment : assignedFields) {
			assignedFieldBindings.add(fragment.resolveBinding());
		}
		assignedFieldBindings.addAll(superAccessedFields);
		
		for(Expression expression : newVariableInstructions) {
			SimpleName newSimpleName = (SimpleName)expression;
			SimpleName oldSimpleName = (SimpleName)oldVariableInstructions.get(j);
			Expression newParentExpression = null;
			Expression oldParentExpression = null;
			if(newSimpleName.getParent() instanceof QualifiedName) {
				newParentExpression = (QualifiedName)newSimpleName.getParent();
				oldParentExpression = (QualifiedName)oldSimpleName.getParent();
			}
			else if(newSimpleName.getParent() instanceof FieldAccess) {
				newParentExpression = (FieldAccess)newSimpleName.getParent();
				oldParentExpression = (FieldAccess)oldSimpleName.getParent();
			}
			else {
				newParentExpression = newSimpleName;
				oldParentExpression = oldSimpleName;
			}
			if(newParentExpression.getParent() instanceof Assignment) {
				Assignment newAssignment = (Assignment)newParentExpression.getParent();
				Assignment oldAssignment = (Assignment)oldParentExpression.getParent();
				Expression newLeftHandSide = newAssignment.getLeftHandSide();
				Expression oldLeftHandSide = oldAssignment.getLeftHandSide();
				SimpleName newLeftHandSideName = null;
				SimpleName oldLeftHandSideName = null;
				if(newLeftHandSide instanceof SimpleName) {
					newLeftHandSideName = (SimpleName)newLeftHandSide;
					oldLeftHandSideName = (SimpleName)oldLeftHandSide;
				}
				else if(newLeftHandSide instanceof QualifiedName) {
					QualifiedName newLeftHandSideQualifiedName = (QualifiedName)newLeftHandSide;
					newLeftHandSideName = newLeftHandSideQualifiedName.getName();
					QualifiedName oldLeftHandSideQualifiedName = (QualifiedName)oldLeftHandSide;
					oldLeftHandSideName = oldLeftHandSideQualifiedName.getName();
				}
				else if(newLeftHandSide instanceof FieldAccess) {
					FieldAccess newLeftHandSideFieldAccess = (FieldAccess)newLeftHandSide;
					newLeftHandSideName = newLeftHandSideFieldAccess.getName();
					FieldAccess oldLeftHandSideFieldAccess = (FieldAccess)oldLeftHandSide;
					oldLeftHandSideName = oldLeftHandSideFieldAccess.getName();
				}
				Expression newRightHandSide = newAssignment.getRightHandSide();
				Expression oldRightHandSide = oldAssignment.getRightHandSide();
				SimpleName newRightHandSideName = null;
				SimpleName oldRightHandSideName = null;
				if(newRightHandSide instanceof SimpleName) {
					newRightHandSideName = (SimpleName)newRightHandSide;
					oldRightHandSideName = (SimpleName)oldRightHandSide;
				}
				else if(newRightHandSide instanceof QualifiedName) {
					QualifiedName newRightHandSideQualifiedName = (QualifiedName)newRightHandSide;
					newRightHandSideName = newRightHandSideQualifiedName.getName();
					QualifiedName oldRightHandSideQualifiedName = (QualifiedName)oldRightHandSide;
					oldRightHandSideName = oldRightHandSideQualifiedName.getName();
				}
				else if(newRightHandSide instanceof FieldAccess) {
					FieldAccess newRightHandSideFieldAccess = (FieldAccess)newRightHandSide;
					newRightHandSideName = newRightHandSideFieldAccess.getName();
					FieldAccess oldRightHandSideFieldAccess = (FieldAccess)oldRightHandSide;
					oldRightHandSideName = oldRightHandSideFieldAccess.getName();
				}
				String invokerName = sourceTypeDeclaration.getName().getIdentifier();
				invokerName = invokerName.substring(0,1).toLowerCase() + invokerName.substring(1,invokerName.length());
				if(newLeftHandSideName != null && newLeftHandSideName.equals(newSimpleName)) {
					for(IVariableBinding assignedFieldBinding : assignedFieldBindings) {
						if(assignedFieldBinding.isEqualTo(oldLeftHandSideName.resolveBinding())) {
							IMethodBinding setterMethodBinding = null;
							if(superAssignedFields.contains(assignedFieldBinding)) {
								setterMethodBinding = typeCheckElimination.getSetterMethodBindingOfSuperAssignedField(assignedFieldBinding);
							}
							else {
								setterMethodBinding = findSetterMethodInContext(assignedFieldBinding);
							}
							String leftHandMethodName;
							if(setterMethodBinding != null) {
								leftHandMethodName = setterMethodBinding.getName();
							}
							else {
								leftHandMethodName = assignedFieldBinding.getName();
								leftHandMethodName = "set" + leftHandMethodName.substring(0,1).toUpperCase() + leftHandMethodName.substring(1,leftHandMethodName.length());
							}
							MethodInvocation leftHandMethodInvocation = subclassAST.newMethodInvocation();
							subclassRewriter.set(leftHandMethodInvocation, MethodInvocation.NAME_PROPERTY, subclassAST.newSimpleName(leftHandMethodName), null);
							subclassRewriter.set(leftHandMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
							ListRewrite methodInvocationArgumentsRewrite = subclassRewriter.getListRewrite(leftHandMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
							if(newRightHandSideName != null) {
								boolean accessedFieldFound = false;
								for(IVariableBinding accessedFieldBinding : accessedFieldBindings) {
									if(accessedFieldBinding.isEqualTo(oldRightHandSideName.resolveBinding())) {
										if((accessedFieldBinding.getModifiers() & Modifier.STATIC) != 0 &&
												(accessedFieldBinding.getModifiers() & Modifier.PUBLIC) != 0) {
											SimpleName qualifier = subclassAST.newSimpleName(accessedFieldBinding.getDeclaringClass().getName());
											if(newRightHandSideName.getParent() instanceof FieldAccess) {
												FieldAccess fieldAccess = (FieldAccess)newRightHandSideName.getParent();
												subclassRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, qualifier, null);
												methodInvocationArgumentsRewrite.insertLast(fieldAccess, null);
											}
											else if(newRightHandSideName.getParent() instanceof QualifiedName) {
												QualifiedName qualifiedName = (QualifiedName)newRightHandSideName.getParent();
												methodInvocationArgumentsRewrite.insertLast(qualifiedName, null);
											}
											else {
												SimpleName simpleName = subclassAST.newSimpleName(newRightHandSideName.getIdentifier());
												QualifiedName newQualifiedName = subclassAST.newQualifiedName(qualifier, simpleName);
												subclassRewriter.replace(newRightHandSideName, newQualifiedName, null);
												methodInvocationArgumentsRewrite.insertLast(newQualifiedName, null);
											}
										}
										else {
											IMethodBinding getterMethodBinding = null;
											if(superAccessedFields.contains(accessedFieldBinding)) {
												getterMethodBinding = typeCheckElimination.getGetterMethodBindingOfSuperAccessedField(accessedFieldBinding);
											}
											else {
												getterMethodBinding = findGetterMethodInContext(accessedFieldBinding);
											}
											String rightHandMethodName;
											if(getterMethodBinding != null) {
												rightHandMethodName = getterMethodBinding.getName();
											}
											else {
												rightHandMethodName = accessedFieldBinding.getName();
												rightHandMethodName = "get" + rightHandMethodName.substring(0,1).toUpperCase() + rightHandMethodName.substring(1,rightHandMethodName.length());
											}
											MethodInvocation rightHandMethodInvocation = subclassAST.newMethodInvocation();
											subclassRewriter.set(rightHandMethodInvocation, MethodInvocation.NAME_PROPERTY, subclassAST.newSimpleName(rightHandMethodName), null);
											subclassRewriter.set(rightHandMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
											methodInvocationArgumentsRewrite.insertLast(rightHandMethodInvocation, null);
										}
										accessedFieldFound = true;
										break;
									}
								}
								if(!accessedFieldFound)
									methodInvocationArgumentsRewrite.insertLast(newAssignment.getRightHandSide(), null);
							}
							else {
								methodInvocationArgumentsRewrite.insertLast(newAssignment.getRightHandSide(), null);
							}
							subclassRewriter.replace(newAssignment, leftHandMethodInvocation, null);
							break;
						}
					}
				}
			}
			else {
				for(IVariableBinding accessedFieldBinding : accessedFieldBindings) {
					if(accessedFieldBinding.isEqualTo(oldSimpleName.resolveBinding())) {
						if((accessedFieldBinding.getModifiers() & Modifier.STATIC) != 0 &&
								(accessedFieldBinding.getModifiers() & Modifier.PUBLIC) != 0) {
							SimpleName qualifier = subclassAST.newSimpleName(accessedFieldBinding.getDeclaringClass().getName());
							if(newSimpleName.getParent() instanceof FieldAccess) {
								FieldAccess fieldAccess = (FieldAccess)newSimpleName.getParent();
								subclassRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, qualifier, null);
							}
							else if(!(newSimpleName.getParent() instanceof QualifiedName)) {
								SimpleName simpleName = subclassAST.newSimpleName(newSimpleName.getIdentifier());
								QualifiedName newQualifiedName = subclassAST.newQualifiedName(qualifier, simpleName);
								subclassRewriter.replace(newSimpleName, newQualifiedName, null);
							}
						}
						else {
							IMethodBinding getterMethodBinding = null;
							if(superAccessedFields.contains(accessedFieldBinding)) {
								getterMethodBinding = typeCheckElimination.getGetterMethodBindingOfSuperAccessedField(accessedFieldBinding);
							}
							else {
								getterMethodBinding = findGetterMethodInContext(accessedFieldBinding);
							}
							String methodName;
							if(getterMethodBinding != null) {
								methodName = getterMethodBinding.getName();
							}
							else {
								methodName = accessedFieldBinding.getName();
								methodName = "get" + methodName.substring(0,1).toUpperCase() + methodName.substring(1,methodName.length());
							}
							MethodInvocation methodInvocation = subclassAST.newMethodInvocation();
							subclassRewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY, subclassAST.newSimpleName(methodName), null);
							String invokerName = sourceTypeDeclaration.getName().getIdentifier();
							invokerName = invokerName.substring(0,1).toLowerCase() + invokerName.substring(1,invokerName.length());
							subclassRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
							if(newSimpleName.getParent() instanceof FieldAccess) {
								FieldAccess fieldAccess = (FieldAccess)newSimpleName.getParent();
								subclassRewriter.replace(fieldAccess, methodInvocation, null);
							}
							else if(newSimpleName.getParent() instanceof QualifiedName) {
								QualifiedName qualifiedName = (QualifiedName)newSimpleName.getParent();
								subclassRewriter.replace(qualifiedName, methodInvocation, null);
							}
							else {
								subclassRewriter.replace(newSimpleName, methodInvocation, null);
							}
						}
						break;
					}
				}
			}
			j++;
		}
	}

	private IMethodBinding findSetterMethodInContext(IVariableBinding fieldBinding) {
		MethodDeclaration[] contextMethods = sourceTypeDeclaration.getMethods();
		for(MethodDeclaration methodDeclaration : contextMethods) {
			SimpleName simpleName = MethodDeclarationUtility.isSetter(methodDeclaration);
			if(simpleName != null && simpleName.resolveBinding().isEqualTo(fieldBinding)) {
				return methodDeclaration.resolveBinding();
			}
		}
		return null;
	}

	private IMethodBinding findGetterMethodInContext(IVariableBinding fieldBinding) {
		MethodDeclaration[] contextMethods = sourceTypeDeclaration.getMethods();
		for(MethodDeclaration methodDeclaration : contextMethods) {
			SimpleName simpleName = MethodDeclarationUtility.isGetter(methodDeclaration);
			if(simpleName != null && simpleName.resolveBinding().isEqualTo(fieldBinding)) {
				return methodDeclaration.resolveBinding();
			}
		}
		return null;
	}

	private IFile getFile(IContainer rootContainer, String fullyQualifiedClassName) {
		String[] subPackages = fullyQualifiedClassName.split("\\.");
		IContainer classContainer = rootContainer;
		IFile classFile = null;
		for(int i = 0; i<subPackages.length; i++) {
			try {
				if(i == subPackages.length-1) {
					IResource[] resources = classContainer.members();
					for(IResource resource : resources) {
						if(resource instanceof IFile) {
							IFile file = (IFile)resource;
							if(file.getName().equals(subPackages[i] + ".java")) {
								classFile = file;
								break;
							}
						}
					}
				}
				else {
					IResource[] resources = classContainer.members();
					for(IResource resource : resources) {
						if(resource instanceof IFolder) {
							IContainer container = (IContainer)resource;
							if(container.getName().equals(subPackages[i])) {
								classContainer = container;
								break;
							}
						}
					}
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return classFile;
	}

	private void generateGettersForAccessedFields() {
		AST contextAST = sourceTypeDeclaration.getAST();
		Set<VariableDeclarationFragment> accessedFields = new LinkedHashSet<VariableDeclarationFragment>();
		accessedFields.addAll(typeCheckElimination.getAccessedFields());
		accessedFields.addAll(typeCheckElimination.getSuperAccessedFields());
		for(VariableDeclarationFragment fragment : accessedFields) {
			IMethodBinding getterMethodBinding = null;
			if(typeCheckElimination.getSuperAccessedFields().contains(fragment)) {
				for(IVariableBinding fieldBinding : typeCheckElimination.getSuperAccessedFieldBindings()) {
					if(fieldBinding.isEqualTo(fragment.resolveBinding())) {
						getterMethodBinding = typeCheckElimination.getGetterMethodBindingOfSuperAccessedField(fieldBinding);
						break;
					}
				}
			}
			else {
				getterMethodBinding = findGetterMethodInContext(fragment.resolveBinding());
			}
			if(getterMethodBinding == null) {
				FieldDeclaration fieldDeclaration = (FieldDeclaration)fragment.getParent();
				int modifiers = fieldDeclaration.getModifiers();
				if(!fragment.equals(typeCheckElimination.getTypeField()) &&
						!((modifiers & Modifier.PUBLIC) != 0 && (modifiers & Modifier.STATIC) != 0)) {
					MethodDeclaration newMethodDeclaration = contextAST.newMethodDeclaration();
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, fieldDeclaration.getType(), null);
					ListRewrite methodDeclarationModifiersRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
					methodDeclarationModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
					String methodName = fragment.getName().getIdentifier();
					methodName = "get" + methodName.substring(0,1).toUpperCase() + methodName.substring(1,methodName.length());
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, contextAST.newSimpleName(methodName), null);
					Block methodDeclarationBody = contextAST.newBlock();
					ListRewrite methodDeclarationBodyStatementsRewrite = sourceRewriter.getListRewrite(methodDeclarationBody, Block.STATEMENTS_PROPERTY);
					ReturnStatement returnStatement = contextAST.newReturnStatement();
					sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, fragment.getName(), null);
					methodDeclarationBodyStatementsRewrite.insertLast(returnStatement, null);
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.BODY_PROPERTY, methodDeclarationBody, null);
					ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
					contextBodyRewrite.insertLast(newMethodDeclaration, null);
				}
			}
		}
	}

	private void generateSettersForAssignedFields() {
		AST contextAST = sourceTypeDeclaration.getAST();
		Set<VariableDeclarationFragment> assignedFields = new LinkedHashSet<VariableDeclarationFragment>();
		assignedFields.addAll(typeCheckElimination.getAssignedFields());
		assignedFields.addAll(typeCheckElimination.getSuperAssignedFields());
		for(VariableDeclarationFragment fragment : assignedFields) {
			IMethodBinding setterMethodBinding = null;
			if(typeCheckElimination.getSuperAssignedFields().contains(fragment)) {
				for(IVariableBinding fieldBinding : typeCheckElimination.getSuperAssignedFieldBindings()) {
					if(fieldBinding.isEqualTo(fragment.resolveBinding())) {
						setterMethodBinding = typeCheckElimination.getSetterMethodBindingOfSuperAssignedField(fieldBinding);
						break;
					}
				}
			}
			else {
				setterMethodBinding = findSetterMethodInContext(fragment.resolveBinding());
			}
			if(setterMethodBinding == null) {
				FieldDeclaration fieldDeclaration = (FieldDeclaration)fragment.getParent();
				if(!fragment.equals(typeCheckElimination.getTypeField())) {
					MethodDeclaration newMethodDeclaration = contextAST.newMethodDeclaration();
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, contextAST.newPrimitiveType(PrimitiveType.VOID), null);
					ListRewrite methodDeclarationModifiersRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
					methodDeclarationModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
					String methodName = fragment.getName().getIdentifier();
					methodName = "set" + methodName.substring(0,1).toUpperCase() + methodName.substring(1,methodName.length());
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, contextAST.newSimpleName(methodName), null);
					ListRewrite methodDeclarationParametersRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
					SingleVariableDeclaration parameter = contextAST.newSingleVariableDeclaration();
					sourceRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, fieldDeclaration.getType(), null);
					sourceRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, fragment.getName(), null);
					methodDeclarationParametersRewrite.insertLast(parameter, null);
					Block methodDeclarationBody = contextAST.newBlock();
					ListRewrite methodDeclarationBodyStatementsRewrite = sourceRewriter.getListRewrite(methodDeclarationBody, Block.STATEMENTS_PROPERTY);
					Assignment assignment = contextAST.newAssignment();
					sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, fragment.getName(), null);
					sourceRewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.ASSIGN, null);
					FieldAccess fieldAccess = contextAST.newFieldAccess();
					sourceRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, contextAST.newThisExpression(), null);
					sourceRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, fragment.getName(), null);
					sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, fieldAccess, null);
					ExpressionStatement expressionStatement = contextAST.newExpressionStatement(assignment);
					methodDeclarationBodyStatementsRewrite.insertLast(expressionStatement, null);
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.BODY_PROPERTY, methodDeclarationBody, null);
					ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
					contextBodyRewrite.insertLast(newMethodDeclaration, null);
				}
			}
		}
	}

	private void generateRequiredImportDeclarationsBasedOnSignature() {
		List<ITypeBinding> typeBindings = new ArrayList<ITypeBinding>();
		if(returnedVariable != null) {
			Type returnType = null;
			if(returnedVariable instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)returnedVariable;
				returnType = singleVariableDeclaration.getType();
			}
			else if(returnedVariable instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)returnedVariable;
				VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
				returnType = variableDeclarationStatement.getType();
			}
			ITypeBinding returnTypeBinding = returnType.resolveBinding();
			if(!typeBindings.contains(returnTypeBinding))
				typeBindings.add(returnTypeBinding);
		}
		
		Set<SingleVariableDeclaration> parameters = typeCheckElimination.getAccessedParameters();
		for(SingleVariableDeclaration parameter : parameters) {
			if(!parameter.equals(returnedVariable) && !parameter.equals(typeVariable)) {
				Type parameterType = parameter.getType();
				ITypeBinding parameterTypeBinding = parameterType.resolveBinding();
				if(!typeBindings.contains(parameterTypeBinding))
					typeBindings.add(parameterTypeBinding);
			}
		}
		
		Set<VariableDeclaration> accessedLocalVariables = typeCheckElimination.getAccessedLocalVariables();
		for(VariableDeclaration fragment : accessedLocalVariables) {
			if(!fragment.equals(returnedVariable) && !fragment.equals(typeVariable)) {
				Type variableType = null;
				if(fragment instanceof SingleVariableDeclaration) {
					SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)fragment;
					variableType = singleVariableDeclaration.getType();
				}
				else if(fragment instanceof VariableDeclarationFragment) {
					VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)fragment;
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
					variableType = variableDeclarationStatement.getType();
				}
				ITypeBinding variableTypeBinding = variableType.resolveBinding();
				if(!typeBindings.contains(variableTypeBinding))
					typeBindings.add(variableTypeBinding);
			}
		}
		
		if(typeCheckElimination.getAccessedFields().size() > 0 || typeCheckElimination.getAssignedFields().size() > 0 ||
				typeCheckElimination.getAccessedMethods().size() > 0 || typeCheckElimination.getSuperAccessedMethods().size() > 0) {
			if(!typeBindings.contains(sourceTypeDeclaration.resolveBinding()))
				typeBindings.add(sourceTypeDeclaration.resolveBinding());
		}
		
		for(ITypeBinding typeBinding : thrownExceptions) {
			if(!typeBindings.contains(typeBinding))
				typeBindings.add(typeBinding);
		}
		
		getSimpleTypeBindings(typeBindings, requiredImportDeclarationsBasedOnSignature);
	}

	private Set<ITypeBinding> generateRequiredImportDeclarationsBasedOnBranch(ArrayList<Statement> statements) {
		List<ITypeBinding> typeBindings = new ArrayList<ITypeBinding>();
		for(Statement statement : statements) {
			ExpressionExtractor expressionExtractor = new ExpressionExtractor();
			List<Expression> variableInstructions = expressionExtractor.getVariableInstructions(statement);
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
			
			List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(statement);
			for(Expression expression : methodInvocations) {
				if(expression instanceof MethodInvocation) {
					MethodInvocation methodInvocation = (MethodInvocation)expression;
					IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
					ITypeBinding declaringClassTypeBinding = methodBinding.getDeclaringClass();
					if(declaringClassTypeBinding != null && !typeBindings.contains(declaringClassTypeBinding))
						typeBindings.add(declaringClassTypeBinding);
				}
			}
			
			List<Expression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(statement);
			for(Expression expression : classInstanceCreations) {
				ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)expression;
				Type classInstanceCreationType = classInstanceCreation.getType();
				ITypeBinding classInstanceCreationTypeBinding = classInstanceCreationType.resolveBinding();
				if(!typeBindings.contains(classInstanceCreationTypeBinding))
					typeBindings.add(classInstanceCreationTypeBinding);
			}
			
			List<Expression> typeLiterals = expressionExtractor.getTypeLiterals(statement);
			for(Expression expression : typeLiterals) {
				TypeLiteral typeLiteral = (TypeLiteral)expression;
				Type typeLiteralType = typeLiteral.getType();
				ITypeBinding typeLiteralTypeBinding = typeLiteralType.resolveBinding();
				if(!typeBindings.contains(typeLiteralTypeBinding))
					typeBindings.add(typeLiteralTypeBinding);
			}
			
			List<Expression> castExpressions = expressionExtractor.getCastExpressions(statement);
			for(Expression expression : castExpressions) {
				CastExpression castExpression = (CastExpression)expression;
				Type castExpressionType = castExpression.getType();
				ITypeBinding typeLiteralTypeBinding = castExpressionType.resolveBinding();
				if(!typeBindings.contains(typeLiteralTypeBinding))
					typeBindings.add(typeLiteralTypeBinding);
			}
		}
		
		Set<ITypeBinding> finalTypeBindings = new LinkedHashSet<ITypeBinding>();
		getSimpleTypeBindings(typeBindings, finalTypeBindings);
		return finalTypeBindings;
	}

	private void getSimpleTypeBindings(List<ITypeBinding> typeBindings, Set<ITypeBinding> finalTypeBindings) {
		for(ITypeBinding typeBinding : typeBindings) {
			if(typeBinding.isPrimitive()) {
				
			}
			else if(typeBinding.isArray()) {
				ITypeBinding elementTypeBinding = typeBinding.getElementType();
				List<ITypeBinding> typeBindingList = new ArrayList<ITypeBinding>();
				typeBindingList.add(elementTypeBinding);
				getSimpleTypeBindings(typeBindingList, finalTypeBindings);
			}
			else if(typeBinding.isParameterizedType()) {
				List<ITypeBinding> typeBindingList = new ArrayList<ITypeBinding>();
				typeBindingList.add(typeBinding.getTypeDeclaration());
				ITypeBinding[] typeArgumentBindings = typeBinding.getTypeArguments();
				for(ITypeBinding typeArgumentBinding : typeArgumentBindings)
					typeBindingList.add(typeArgumentBinding);
				getSimpleTypeBindings(typeBindingList, finalTypeBindings);
			}
			else if(typeBinding.isWildcardType()) {
				List<ITypeBinding> typeBindingList = new ArrayList<ITypeBinding>();
				typeBindingList.add(typeBinding.getBound());
				getSimpleTypeBindings(typeBindingList, finalTypeBindings);
			}
			else {
				if(typeBinding.isNested()) {
					finalTypeBindings.add(typeBinding.getDeclaringClass());
				}
				finalTypeBindings.add(typeBinding);
			}
		}
	}

	private void addImportDeclaration(ITypeBinding typeBinding, CompilationUnit targetCompilationUnit, ASTRewrite targetRewriter) {
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

	private void setPublicModifierToAccessedMethods() {
		for(MethodDeclaration methodDeclaration : typeCheckElimination.getAccessedMethods()) {
			List<IExtendedModifier> modifiers = methodDeclaration.modifiers();
			ListRewrite modifierRewrite = sourceRewriter.getListRewrite(methodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
			Modifier publicModifier = methodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
			boolean modifierFound = false;
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
		}
	}

	private Expression constructExpression(AST ast, DefaultMutableTreeNode node) {
		Object object = node.getUserObject();
		if(object instanceof InfixExpression.Operator) {
			InfixExpression.Operator operator = (InfixExpression.Operator)object;
			InfixExpression infixExpression = ast.newInfixExpression();
			infixExpression.setOperator(operator);
			DefaultMutableTreeNode leftChild = (DefaultMutableTreeNode)node.getChildAt(0);
			DefaultMutableTreeNode rightChild = (DefaultMutableTreeNode)node.getChildAt(1);
			infixExpression.setLeftOperand(constructExpression(ast, leftChild));
			infixExpression.setRightOperand(constructExpression(ast, rightChild));
			return infixExpression;
		}
		else if(object instanceof Expression) {
			Expression expression = (Expression)object;
			return expression;
		}
		return null;
	}

	public TypeCheckElimination getTypeCheckElimination() {
		return typeCheckElimination;
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
			final Collection<TextFileChange> changes = fChanges.values();
			CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {
				@Override
				public ChangeDescriptor getDescriptor() {
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
					String project = sourceICompilationUnit.getJavaProject().getElementName();
					String description = MessageFormat.format("Replace Conditional with Polymorphism in method ''{0}''", new Object[] { typeCheckElimination.getTypeCheckMethod().getName().getIdentifier()});
					String comment = null;
					return new RefactoringChangeDescriptor(new ReplaceConditionalWithPolymorphismDescriptor(project, description, comment,
							sourceFile, sourceCompilationUnit, sourceTypeDeclaration, typeCheckElimination));
				}
			};
			return change;
		} finally {
			pm.done();
		}
	}

	@Override
	public String getName() {
		return "Replace Conditional with Polymorphism";
	}
}
