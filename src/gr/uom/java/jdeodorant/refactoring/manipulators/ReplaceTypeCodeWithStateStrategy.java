package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.MethodDeclarationUtility;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
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
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;

public class ReplaceTypeCodeWithStateStrategy implements Refactoring {
	private IFile sourceFile;
	private CompilationUnit sourceCompilationUnit;
	private TypeDeclaration sourceTypeDeclaration;
	private TypeCheckElimination typeCheckElimination;
	private ASTRewrite sourceRewriter;
	private UndoRefactoring undoRefactoring;
	private VariableDeclarationFragment returnedVariable;
	private Set<ITypeBinding> requiredImportDeclarationsBasedOnSignature;
	private Set<ITypeBinding> thrownExceptions;
	private Map<String, String> additionalStaticFields;
	
	public ReplaceTypeCodeWithStateStrategy(IFile sourceFile, CompilationUnit sourceCompilationUnit,
			TypeDeclaration sourceTypeDeclaration, TypeCheckElimination typeCheckElimination) {
		this.sourceFile = sourceFile;
		this.sourceCompilationUnit = sourceCompilationUnit;
		this.sourceTypeDeclaration = sourceTypeDeclaration;
		this.typeCheckElimination = typeCheckElimination;
		this.sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		this.undoRefactoring = new UndoRefactoring();
		this.returnedVariable = typeCheckElimination.getTypeCheckMethodReturnedVariable();
		this.requiredImportDeclarationsBasedOnSignature = new LinkedHashSet<ITypeBinding>();
		this.thrownExceptions = typeCheckElimination.getThrownExceptions();
		this.additionalStaticFields = new LinkedHashMap<String, String>();
	}

	public void apply() {
		modifyContext();
		createStateStrategyHierarchy();
	}

	private void modifyContext() {
		AST contextAST = sourceTypeDeclaration.getAST();
		ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		VariableDeclarationFragment typeFragment = contextAST.newVariableDeclarationFragment();
		sourceRewriter.set(typeFragment, VariableDeclarationFragment.NAME_PROPERTY, typeCheckElimination.getTypeField().getName(), null);
		FieldDeclaration typeFieldDeclaration = contextAST.newFieldDeclaration(typeFragment);
		sourceRewriter.set(typeFieldDeclaration, FieldDeclaration.TYPE_PROPERTY, contextAST.newSimpleName(typeCheckElimination.getAbstractClassName()), null);
		ListRewrite typeFieldDeclrationModifiersRewrite = sourceRewriter.getListRewrite(typeFieldDeclaration, FieldDeclaration.MODIFIERS2_PROPERTY);
		typeFieldDeclrationModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD), null);
		contextBodyRewrite.insertFirst(typeFieldDeclaration, null);
		
		FieldDeclaration[] fieldDeclarations = sourceTypeDeclaration.getFields();
		for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
			List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				if(fragment.equals(typeCheckElimination.getTypeField())) {
					if(fragments.size() == 1) {
						contextBodyRewrite.remove(fragment.getParent(), null);
					}
					else {
						ListRewrite fragmentRewrite = sourceRewriter.getListRewrite(fragment.getParent(), FieldDeclaration.FRAGMENTS_PROPERTY);
						fragmentRewrite.remove(fragment, null);
					}
				}
			}
		}
		
		modifyTypeFieldAssignmentsInContextClass();
		MethodDeclaration setterMethod = typeCheckElimination.getTypeFieldSetterMethod();
		SwitchStatement switchStatement = contextAST.newSwitchStatement();
		List<SimpleName> staticFieldNames = typeCheckElimination.getStaticFields();
		List<String> subclassNames = typeCheckElimination.getSubclassNames();
		ListRewrite switchStatementStatementsRewrite = sourceRewriter.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);
		int i = 0;
		for(SimpleName staticFieldName : staticFieldNames) {
			SwitchCase switchCase = contextAST.newSwitchCase();
			sourceRewriter.set(switchCase, SwitchCase.EXPRESSION_PROPERTY, staticFieldName, null);
			switchStatementStatementsRewrite.insertLast(switchCase, null);
			Assignment assignment = contextAST.newAssignment();
			sourceRewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.ASSIGN, null);
			FieldAccess typeFieldAccess = contextAST.newFieldAccess();
			sourceRewriter.set(typeFieldAccess, FieldAccess.EXPRESSION_PROPERTY, contextAST.newThisExpression(), null);
			sourceRewriter.set(typeFieldAccess, FieldAccess.NAME_PROPERTY, typeCheckElimination.getTypeField().getName(), null);
			sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, typeFieldAccess, null);
			ClassInstanceCreation classInstanceCreation = contextAST.newClassInstanceCreation();
			sourceRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY, contextAST.newSimpleName(subclassNames.get(i)), null);
			sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, classInstanceCreation, null);
			switchStatementStatementsRewrite.insertLast(contextAST.newExpressionStatement(assignment), null);
			switchStatementStatementsRewrite.insertLast(contextAST.newBreakStatement(), null);
			i++;
		}
		for(String staticFieldName : additionalStaticFields.keySet()) {
			SwitchCase switchCase = contextAST.newSwitchCase();
			sourceRewriter.set(switchCase, SwitchCase.EXPRESSION_PROPERTY, contextAST.newSimpleName(staticFieldName), null);
			switchStatementStatementsRewrite.insertLast(switchCase, null);
			Assignment assignment = contextAST.newAssignment();
			sourceRewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.ASSIGN, null);
			FieldAccess typeFieldAccess = contextAST.newFieldAccess();
			sourceRewriter.set(typeFieldAccess, FieldAccess.EXPRESSION_PROPERTY, contextAST.newThisExpression(), null);
			sourceRewriter.set(typeFieldAccess, FieldAccess.NAME_PROPERTY, typeCheckElimination.getTypeField().getName(), null);
			sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, typeFieldAccess, null);
			ClassInstanceCreation classInstanceCreation = contextAST.newClassInstanceCreation();
			sourceRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY, contextAST.newSimpleName(additionalStaticFields.get(staticFieldName)), null);
			sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, classInstanceCreation, null);
			switchStatementStatementsRewrite.insertLast(contextAST.newExpressionStatement(assignment), null);
			switchStatementStatementsRewrite.insertLast(contextAST.newBreakStatement(), null);
		}
		if(setterMethod != null) {
			List<SingleVariableDeclaration> setterMethodParameters = setterMethod.parameters();
			if(setterMethodParameters.size() == 1) {
				sourceRewriter.set(switchStatement, SwitchStatement.EXPRESSION_PROPERTY, setterMethodParameters.get(0).getName(), null);
			}
			Block setterMethodBody = setterMethod.getBody();
			List<Statement> setterMethodBodyStatements = setterMethodBody.statements();
			ListRewrite setterMethodBodyRewrite = sourceRewriter.getListRewrite(setterMethodBody, Block.STATEMENTS_PROPERTY);
			if(setterMethodBodyStatements.size() == 1) {
				setterMethodBodyRewrite.replace(setterMethodBodyStatements.get(0), switchStatement, null);
			}
		}
		else {
			MethodDeclaration setterMethodDeclaration = contextAST.newMethodDeclaration();
			sourceRewriter.set(setterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, contextAST.newSimpleName("set" + typeCheckElimination.getAbstractClassName()), null);
			sourceRewriter.set(setterMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, contextAST.newPrimitiveType(PrimitiveType.VOID), null);
			ListRewrite setterMethodModifiersRewrite = sourceRewriter.getListRewrite(setterMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
			setterMethodModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
			ListRewrite setterMethodParameterRewrite = sourceRewriter.getListRewrite(setterMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
			SingleVariableDeclaration parameter = contextAST.newSingleVariableDeclaration();
			VariableDeclarationFragment typeField = typeCheckElimination.getTypeField();
			Type parameterType = ((FieldDeclaration)typeField.getParent()).getType();
			sourceRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, parameterType, null);
			sourceRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, typeField.getName(), null);
			setterMethodParameterRewrite.insertLast(parameter, null);
			
			sourceRewriter.set(switchStatement, SwitchStatement.EXPRESSION_PROPERTY, typeField.getName(), null);
			Block setterMethodBody = contextAST.newBlock();
			ListRewrite setterMethodBodyRewrite = sourceRewriter.getListRewrite(setterMethodBody, Block.STATEMENTS_PROPERTY);
			setterMethodBodyRewrite.insertLast(switchStatement, null);
			sourceRewriter.set(setterMethodDeclaration, MethodDeclaration.BODY_PROPERTY, setterMethodBody, null);
			contextBodyRewrite.insertLast(setterMethodDeclaration, null);
		}
		
		MethodDeclaration getterMethod = typeCheckElimination.getTypeFieldGetterMethod();
		if(getterMethod != null) {
			Block getterMethodBody = getterMethod.getBody();
			List<Statement> getterMethodBodyStatements = getterMethodBody.statements();
			ListRewrite getterMethodBodyRewrite = sourceRewriter.getListRewrite(getterMethodBody, Block.STATEMENTS_PROPERTY);
			if(getterMethodBodyStatements.size() == 1) {
				ReturnStatement returnStatement = contextAST.newReturnStatement();
				MethodInvocation abstractGetterMethodInvocation = contextAST.newMethodInvocation();
				sourceRewriter.set(abstractGetterMethodInvocation, MethodInvocation.NAME_PROPERTY, getterMethod.getName(), null);
				sourceRewriter.set(abstractGetterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, typeCheckElimination.getTypeField().getName(), null);
				sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, abstractGetterMethodInvocation, null);
				getterMethodBodyRewrite.replace(getterMethodBodyStatements.get(0), returnStatement, null);
			}
		}
		else {
			MethodDeclaration getterMethodDeclaration = contextAST.newMethodDeclaration();
			sourceRewriter.set(getterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, contextAST.newSimpleName("get" + typeCheckElimination.getAbstractClassName()), null);
			VariableDeclarationFragment typeField = typeCheckElimination.getTypeField();
			Type returnType = ((FieldDeclaration)typeField.getParent()).getType();
			sourceRewriter.set(getterMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnType, null);
			ListRewrite getterMethodModifiersRewrite = sourceRewriter.getListRewrite(getterMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
			getterMethodModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
			
			ReturnStatement returnStatement = contextAST.newReturnStatement();
			MethodInvocation abstractGetterMethodInvocation = contextAST.newMethodInvocation();
			sourceRewriter.set(abstractGetterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + typeCheckElimination.getAbstractClassName()), null);
			sourceRewriter.set(abstractGetterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, typeCheckElimination.getTypeField().getName(), null);
			sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, abstractGetterMethodInvocation, null);
			Block getterMethodBody = contextAST.newBlock();
			ListRewrite getterMethodBodyRewrite = sourceRewriter.getListRewrite(getterMethodBody, Block.STATEMENTS_PROPERTY);
			getterMethodBodyRewrite.insertLast(returnStatement, null);
			sourceRewriter.set(getterMethodDeclaration, MethodDeclaration.BODY_PROPERTY, getterMethodBody, null);
			contextBodyRewrite.insertLast(getterMethodDeclaration, null);
		}
		
		MethodDeclaration typeCheckMethod = typeCheckElimination.getTypeCheckMethod();
		Block typeCheckCodeFragmentParentBlock = (Block)typeCheckElimination.getTypeCheckCodeFragment().getParent();
		ListRewrite typeCheckCodeFragmentParentBlockStatementsRewrite = sourceRewriter.getListRewrite(typeCheckCodeFragmentParentBlock, Block.STATEMENTS_PROPERTY);
		Type typeCheckMethodReturnType = typeCheckMethod.getReturnType2();
		if(typeCheckMethodReturnType.isPrimitiveType() && ((PrimitiveType)typeCheckMethodReturnType).getPrimitiveTypeCode().equals(PrimitiveType.VOID)) {
			MethodInvocation abstractMethodInvocation = contextAST.newMethodInvocation();
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckMethod.getName(), null);
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(typeCheckElimination.getTypeField().getName().getIdentifier()), null);
			ListRewrite methodInvocationArgumentsRewrite = sourceRewriter.getListRewrite(abstractMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
			for(SingleVariableDeclaration abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
				methodInvocationArgumentsRewrite.insertLast(abstractMethodParameter.getName(), null);
			}
			for(VariableDeclarationFragment fragment : typeCheckElimination.getAccessedLocalVariables()) {
				if(!fragment.equals(returnedVariable)) {
					methodInvocationArgumentsRewrite.insertLast(fragment.getName(), null);
				}
			}
			if(typeCheckElimination.getAccessedFields().size() > 0 || typeCheckElimination.getAssignedFields().size() > 0 || typeCheckElimination.getAccessedMethods().size() > 0) {
				methodInvocationArgumentsRewrite.insertLast(contextAST.newThisExpression(), null);
			}
			ExpressionStatement expressionStatement = contextAST.newExpressionStatement(abstractMethodInvocation);
			typeCheckCodeFragmentParentBlockStatementsRewrite.replace(typeCheckElimination.getTypeCheckCodeFragment(), expressionStatement, null);
		}
		else {
			MethodInvocation abstractMethodInvocation = contextAST.newMethodInvocation();
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckMethod.getName(), null);
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(typeCheckElimination.getTypeField().getName().getIdentifier()), null);
			ListRewrite methodInvocationArgumentsRewrite = sourceRewriter.getListRewrite(abstractMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
			for(SingleVariableDeclaration abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
				methodInvocationArgumentsRewrite.insertLast(abstractMethodParameter.getName(), null);
			}
			for(VariableDeclarationFragment fragment : typeCheckElimination.getAccessedLocalVariables()) {
				if(!fragment.equals(returnedVariable)) {
					methodInvocationArgumentsRewrite.insertLast(fragment.getName(), null);
				}
			}
			if(typeCheckElimination.getAccessedFields().size() > 0 || typeCheckElimination.getAssignedFields().size() > 0 || typeCheckElimination.getAccessedMethods().size() > 0) {
				methodInvocationArgumentsRewrite.insertLast(contextAST.newThisExpression(), null);
			}
			if(returnedVariable != null) {
				Assignment assignment = contextAST.newAssignment();
				sourceRewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.ASSIGN, null);
				sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, returnedVariable.getName(), null);
				sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, abstractMethodInvocation, null);
				ExpressionStatement expressionStatement = contextAST.newExpressionStatement(assignment);
				typeCheckCodeFragmentParentBlockStatementsRewrite.replace(typeCheckElimination.getTypeCheckCodeFragment(), expressionStatement, null);
			}
			else {
				ReturnStatement returnStatement = contextAST.newReturnStatement();
				sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, abstractMethodInvocation, null);
				typeCheckCodeFragmentParentBlockStatementsRewrite.replace(typeCheckElimination.getTypeCheckCodeFragment(), returnStatement, null);
			}
		}
		
		generateGettersForAccessedFields();
		generateSettersForAssignedFields();
		setPublicModifierToStaticFields();
		setPublicModifierToAccessedMethods();
		modifyTypeFieldAccessesInContextClass();
		
		ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
		ITextFileBuffer sourceTextFileBuffer = bufferManager.getTextFileBuffer(sourceFile.getFullPath(), LocationKind.IFILE);
		IDocument sourceDocument = sourceTextFileBuffer.getDocument();
		TextEdit sourceEdit = sourceRewriter.rewriteAST(sourceDocument, null);
		try {
			UndoEdit sourceUndoEdit = sourceEdit.apply(sourceDocument, UndoEdit.CREATE_UNDO);
			undoRefactoring.put(sourceFile, sourceDocument, sourceUndoEdit);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	private void createStateStrategyHierarchy() {
		IContainer contextContainer = (IContainer)sourceFile.getParent();
		IFile stateStrategyFile = null;
		if(contextContainer instanceof IProject) {
			IProject contextProject = (IProject)contextContainer;
			stateStrategyFile = contextProject.getFile(typeCheckElimination.getAbstractClassName() + ".java");
		}
		else if(contextContainer instanceof IFolder) {
			IFolder contextFolder = (IFolder)contextContainer;
			stateStrategyFile = contextFolder.getFile(typeCheckElimination.getAbstractClassName() + ".java");
		}
		boolean stateStrategyAlreadyExists = false;
		try {
			stateStrategyFile.create(new ByteArrayInputStream("".getBytes()), true, null);
			undoRefactoring.addNewlyCreatedFile(stateStrategyFile);
		} catch (CoreException e) {
			stateStrategyAlreadyExists = true;
		}
		IJavaElement stateStrategyJavaElement = JavaCore.create(stateStrategyFile);
		ITextEditor stateStrategyEditor = null;
		try {
			stateStrategyEditor = (ITextEditor)JavaUI.openInEditor(stateStrategyJavaElement);
		} catch (PartInitException e) {
			e.printStackTrace();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
        ICompilationUnit stateStrategyICompilationUnit = (ICompilationUnit)stateStrategyJavaElement;
        ASTParser stateStrategyParser = ASTParser.newParser(AST.JLS3);
        stateStrategyParser.setKind(ASTParser.K_COMPILATION_UNIT);
        stateStrategyParser.setSource(stateStrategyICompilationUnit);
        stateStrategyParser.setResolveBindings(true); // we need bindings later on
        CompilationUnit stateStrategyCompilationUnit = (CompilationUnit)stateStrategyParser.createAST(null);
        
        AST stateStrategyAST = stateStrategyCompilationUnit.getAST();
        ASTRewrite stateStrategyRewriter = ASTRewrite.create(stateStrategyAST);
        ListRewrite stateStrategyTypesRewrite = stateStrategyRewriter.getListRewrite(stateStrategyCompilationUnit, CompilationUnit.TYPES_PROPERTY);
		
		TypeDeclaration stateStrategyTypeDeclaration = null;
		if(stateStrategyAlreadyExists) {
			List<AbstractTypeDeclaration> abstractTypeDeclarations = stateStrategyCompilationUnit.types();
			for(AbstractTypeDeclaration abstractTypeDeclaration : abstractTypeDeclarations) {
				if(abstractTypeDeclaration instanceof TypeDeclaration) {
					TypeDeclaration typeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
					if(typeDeclaration.getName().getIdentifier().equals(typeCheckElimination.getAbstractClassName())) {
						stateStrategyTypeDeclaration = typeDeclaration;
						int stateStrategyModifiers = stateStrategyTypeDeclaration.getModifiers();
						if((stateStrategyModifiers & Modifier.ABSTRACT) == 0) {
							ListRewrite stateStrategyModifiersRewrite = stateStrategyRewriter.getListRewrite(stateStrategyTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
							stateStrategyModifiersRewrite.insertLast(stateStrategyAST.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
						}
						break;
					}
				}
			}
		}
		else {
			if(sourceCompilationUnit.getPackage() != null) {
				stateStrategyRewriter.set(stateStrategyCompilationUnit, CompilationUnit.PACKAGE_PROPERTY, sourceCompilationUnit.getPackage(), null);
			}
			stateStrategyTypeDeclaration = stateStrategyAST.newTypeDeclaration();
			SimpleName stateStrategyName = stateStrategyAST.newSimpleName(typeCheckElimination.getAbstractClassName());
			stateStrategyRewriter.set(stateStrategyTypeDeclaration, TypeDeclaration.NAME_PROPERTY, stateStrategyName, null);
			ListRewrite stateStrategyModifiersRewrite = stateStrategyRewriter.getListRewrite(stateStrategyTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
			stateStrategyModifiersRewrite.insertLast(stateStrategyAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
			stateStrategyModifiersRewrite.insertLast(stateStrategyAST.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
		}
		
		ListRewrite stateStrategyBodyRewrite = stateStrategyRewriter.getListRewrite(stateStrategyTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		
		MethodDeclaration getterMethod = typeCheckElimination.getTypeFieldGetterMethod();
		if(getterMethod != null) {
			MethodDeclaration abstractGetterMethodDeclaration = stateStrategyAST.newMethodDeclaration();
			stateStrategyRewriter.set(abstractGetterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, getterMethod.getName(), null);
			stateStrategyRewriter.set(abstractGetterMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, getterMethod.getReturnType2(), null);
			ListRewrite abstractGetterMethodModifiersRewrite = stateStrategyRewriter.getListRewrite(abstractGetterMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
			abstractGetterMethodModifiersRewrite.insertLast(stateStrategyAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
			abstractGetterMethodModifiersRewrite.insertLast(stateStrategyAST.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
			stateStrategyBodyRewrite.insertLast(abstractGetterMethodDeclaration, null);
		}
		else {
			MethodDeclaration abstractGetterMethodDeclaration = stateStrategyAST.newMethodDeclaration();
			stateStrategyRewriter.set(abstractGetterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, stateStrategyAST.newSimpleName("get" + typeCheckElimination.getAbstractClassName()), null);
			VariableDeclarationFragment typeField = typeCheckElimination.getTypeField();
			Type returnType = ((FieldDeclaration)typeField.getParent()).getType();
			stateStrategyRewriter.set(abstractGetterMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnType, null);
			ListRewrite abstractGetterMethodModifiersRewrite = stateStrategyRewriter.getListRewrite(abstractGetterMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
			abstractGetterMethodModifiersRewrite.insertLast(stateStrategyAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
			abstractGetterMethodModifiersRewrite.insertLast(stateStrategyAST.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
			stateStrategyBodyRewrite.insertLast(abstractGetterMethodDeclaration, null);
		}
		
		MethodDeclaration abstractMethodDeclaration = stateStrategyAST.newMethodDeclaration();
		String abstractMethodName = typeCheckElimination.getTypeCheckMethodName();
		stateStrategyRewriter.set(abstractMethodDeclaration, MethodDeclaration.NAME_PROPERTY, stateStrategyAST.newSimpleName(abstractMethodName), null);
		stateStrategyRewriter.set(abstractMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, typeCheckElimination.getTypeCheckMethodReturnType(), null);
		ListRewrite abstractMethodModifiersRewrite = stateStrategyRewriter.getListRewrite(abstractMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		abstractMethodModifiersRewrite.insertLast(stateStrategyAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
		abstractMethodModifiersRewrite.insertLast(stateStrategyAST.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
		ListRewrite abstractMethodParametersRewrite = stateStrategyRewriter.getListRewrite(abstractMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		for(SingleVariableDeclaration abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
			abstractMethodParametersRewrite.insertLast(abstractMethodParameter, null);
		}
		for(VariableDeclarationFragment fragment : typeCheckElimination.getAccessedLocalVariables()) {
			if(!fragment.equals(returnedVariable)) {
				SingleVariableDeclaration parameter = stateStrategyAST.newSingleVariableDeclaration();
				VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)fragment.getParent();
				stateStrategyRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, variableDeclarationStatement.getType(), null);
				stateStrategyRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, fragment.getName(), null);
				abstractMethodParametersRewrite.insertLast(parameter, null);
			}
		}
		if(typeCheckElimination.getAccessedFields().size() > 0 || typeCheckElimination.getAssignedFields().size() > 0 || typeCheckElimination.getAccessedMethods().size() > 0) {
			SingleVariableDeclaration parameter = stateStrategyAST.newSingleVariableDeclaration();
			SimpleName parameterType = stateStrategyAST.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
			stateStrategyRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, stateStrategyAST.newSimpleType(parameterType), null);
			String parameterName = sourceTypeDeclaration.getName().getIdentifier();
			parameterName = parameterName.substring(0,1).toLowerCase() + parameterName.substring(1,parameterName.length());
			stateStrategyRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, stateStrategyAST.newSimpleName(parameterName), null);
			abstractMethodParametersRewrite.insertLast(parameter, null);
		}
		
		ListRewrite abstractMethodThrownExceptionsRewrite = stateStrategyRewriter.getListRewrite(abstractMethodDeclaration, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
		for(ITypeBinding typeBinding : thrownExceptions) {
			abstractMethodThrownExceptionsRewrite.insertLast(stateStrategyAST.newSimpleName(typeBinding.getName()), null);
		}
		
		stateStrategyBodyRewrite.insertLast(abstractMethodDeclaration, null);
		
		generateRequiredImportDeclarationsBasedOnSignature();
		for(ITypeBinding typeBinding : requiredImportDeclarationsBasedOnSignature) {
			addImportDeclaration(typeBinding, stateStrategyCompilationUnit, stateStrategyRewriter);
		}
		
		if(!stateStrategyAlreadyExists)
			stateStrategyTypesRewrite.insertLast(stateStrategyTypeDeclaration, null);
		
		ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
		ITextFileBuffer stateStrategyTextFileBuffer = bufferManager.getTextFileBuffer(stateStrategyFile.getFullPath(), LocationKind.IFILE);
		IDocument stateStrategyDocument = stateStrategyTextFileBuffer.getDocument();
		TextEdit stateStrategyEdit = stateStrategyRewriter.rewriteAST(stateStrategyDocument, null);
		try {
			UndoEdit stateStrategyUndoEdit = stateStrategyEdit.apply(stateStrategyDocument, UndoEdit.CREATE_UNDO);
			undoRefactoring.put(stateStrategyFile, stateStrategyDocument, stateStrategyUndoEdit);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		stateStrategyEditor.doSave(null);
		
		
		List<ArrayList<Statement>> typeCheckStatements = typeCheckElimination.getTypeCheckStatements();
		List<String> subclassNames = typeCheckElimination.getSubclassNames();
		subclassNames.addAll(additionalStaticFields.values());
		List<String> staticFieldNames = typeCheckElimination.getStaticFieldNames();
		staticFieldNames.addAll(additionalStaticFields.keySet());
		for(int i=0; i<staticFieldNames.size(); i++) {
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
			IFile subclassFile = null;
			if(contextContainer instanceof IProject) {
				IProject contextProject = (IProject)contextContainer;
				subclassFile = contextProject.getFile(subclassNames.get(i) + ".java");
			}
			else if(contextContainer instanceof IFolder) {
				IFolder contextFolder = (IFolder)contextContainer;
				subclassFile = contextFolder.getFile(subclassNames.get(i) + ".java");
			}
			boolean subclassAlreadyExists = false;
			try {
				subclassFile.create(new ByteArrayInputStream("".getBytes()), true, null);
				undoRefactoring.addNewlyCreatedFile(subclassFile);
			} catch (CoreException e) {
				subclassAlreadyExists = true;
			}
			IJavaElement subclassJavaElement = JavaCore.create(subclassFile);
			ITextEditor subclassEditor = null;
			try {
				subclassEditor = (ITextEditor)JavaUI.openInEditor(subclassJavaElement);
			} catch (PartInitException e) {
				e.printStackTrace();
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
			ICompilationUnit subclassICompilationUnit = (ICompilationUnit)subclassJavaElement;
	        ASTParser subclassParser = ASTParser.newParser(AST.JLS3);
	        subclassParser.setKind(ASTParser.K_COMPILATION_UNIT);
	        subclassParser.setSource(subclassICompilationUnit);
	        subclassParser.setResolveBindings(true); // we need bindings later on
	        CompilationUnit subclassCompilationUnit = (CompilationUnit)subclassParser.createAST(null);
	        
	        AST subclassAST = subclassCompilationUnit.getAST();
	        ASTRewrite subclassRewriter = ASTRewrite.create(subclassAST);
	        ListRewrite subclassTypesRewrite = subclassRewriter.getListRewrite(subclassCompilationUnit, CompilationUnit.TYPES_PROPERTY);
			
			TypeDeclaration subclassTypeDeclaration = null;
			if(subclassAlreadyExists) {
				List<AbstractTypeDeclaration> abstractTypeDeclarations = subclassCompilationUnit.types();
				for(AbstractTypeDeclaration abstractTypeDeclaration : abstractTypeDeclarations) {
					if(abstractTypeDeclaration instanceof TypeDeclaration) {
						TypeDeclaration typeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
						if(typeDeclaration.getName().getIdentifier().equals(subclassNames.get(i))) {
							subclassTypeDeclaration = typeDeclaration;
							break;
						}
					}
				}
			}
			else {
				if(sourceCompilationUnit.getPackage() != null) {
					subclassRewriter.set(subclassCompilationUnit, CompilationUnit.PACKAGE_PROPERTY, sourceCompilationUnit.getPackage(), null);
				}
				subclassTypeDeclaration = subclassAST.newTypeDeclaration();
				SimpleName subclassName = subclassAST.newSimpleName(subclassNames.get(i));
				subclassRewriter.set(subclassTypeDeclaration, TypeDeclaration.NAME_PROPERTY, subclassName, null);
				subclassRewriter.set(subclassTypeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, subclassAST.newSimpleType(subclassAST.newSimpleName(typeCheckElimination.getAbstractClassName())), null);
				ListRewrite subclassModifiersRewrite = subclassRewriter.getListRewrite(subclassTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
				subclassModifiersRewrite.insertLast(subclassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
			}
			
			ListRewrite subclassBodyRewrite = subclassRewriter.getListRewrite(subclassTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			
			if(getterMethod != null) {
				MethodDeclaration concreteGetterMethodDeclaration = subclassAST.newMethodDeclaration();
				subclassRewriter.set(concreteGetterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, getterMethod.getName(), null);
				subclassRewriter.set(concreteGetterMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, getterMethod.getReturnType2(), null);
				ListRewrite concreteGetterMethodModifiersRewrite = subclassRewriter.getListRewrite(concreteGetterMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
				concreteGetterMethodModifiersRewrite.insertLast(subclassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
				Block concreteGetterMethodBody = subclassAST.newBlock();
				ListRewrite concreteGetterMethodBodyRewrite = subclassRewriter.getListRewrite(concreteGetterMethodBody, Block.STATEMENTS_PROPERTY);
				ReturnStatement returnStatement = subclassAST.newReturnStatement();
				FieldAccess fieldAccess = subclassAST.newFieldAccess();
				subclassRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, subclassAST.newSimpleName(staticFieldNames.get(i)), null);
				subclassRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, sourceTypeDeclaration.getName(), null);
				subclassRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, fieldAccess, null);
				concreteGetterMethodBodyRewrite.insertLast(returnStatement, null);
				subclassRewriter.set(concreteGetterMethodDeclaration, MethodDeclaration.BODY_PROPERTY, concreteGetterMethodBody, null);
				subclassBodyRewrite.insertLast(concreteGetterMethodDeclaration, null);
			}
			else {
				MethodDeclaration concreteGetterMethodDeclaration = subclassAST.newMethodDeclaration();
				subclassRewriter.set(concreteGetterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, subclassAST.newSimpleName("get" + typeCheckElimination.getAbstractClassName()), null);
				VariableDeclarationFragment typeField = typeCheckElimination.getTypeField();
				Type returnType = ((FieldDeclaration)typeField.getParent()).getType();
				subclassRewriter.set(concreteGetterMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnType, null);
				ListRewrite concreteGetterMethodModifiersRewrite = subclassRewriter.getListRewrite(concreteGetterMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
				concreteGetterMethodModifiersRewrite.insertLast(subclassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
				Block concreteGetterMethodBody = subclassAST.newBlock();
				ListRewrite concreteGetterMethodBodyRewrite = subclassRewriter.getListRewrite(concreteGetterMethodBody, Block.STATEMENTS_PROPERTY);
				ReturnStatement returnStatement = subclassAST.newReturnStatement();
				FieldAccess fieldAccess = subclassAST.newFieldAccess();
				subclassRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, subclassAST.newSimpleName(staticFieldNames.get(i)), null);
				subclassRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, sourceTypeDeclaration.getName(), null);
				subclassRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, fieldAccess, null);
				concreteGetterMethodBodyRewrite.insertLast(returnStatement, null);
				subclassRewriter.set(concreteGetterMethodDeclaration, MethodDeclaration.BODY_PROPERTY, concreteGetterMethodBody, null);
				subclassBodyRewrite.insertLast(concreteGetterMethodDeclaration, null);
			}
			
			MethodDeclaration concreteMethodDeclaration = subclassAST.newMethodDeclaration();
			String concreteMethodName = typeCheckElimination.getTypeCheckMethodName();
			subclassRewriter.set(concreteMethodDeclaration, MethodDeclaration.NAME_PROPERTY, subclassAST.newSimpleName(concreteMethodName), null);
			subclassRewriter.set(concreteMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, typeCheckElimination.getTypeCheckMethodReturnType(), null);
			ListRewrite concreteMethodModifiersRewrite = subclassRewriter.getListRewrite(concreteMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
			concreteMethodModifiersRewrite.insertLast(subclassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
			ListRewrite concreteMethodParametersRewrite = subclassRewriter.getListRewrite(concreteMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
			for(SingleVariableDeclaration abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
				concreteMethodParametersRewrite.insertLast(abstractMethodParameter, null);
			}
			for(VariableDeclarationFragment fragment : typeCheckElimination.getAccessedLocalVariables()) {
				if(!fragment.equals(returnedVariable)) {
					SingleVariableDeclaration parameter = subclassAST.newSingleVariableDeclaration();
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)fragment.getParent();
					subclassRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, variableDeclarationStatement.getType(), null);
					subclassRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, fragment.getName(), null);
					concreteMethodParametersRewrite.insertLast(parameter, null);
				}
			}
			Set<VariableDeclarationFragment> accessedFields = typeCheckElimination.getAccessedFields();
			Set<VariableDeclarationFragment> assignedFields = typeCheckElimination.getAssignedFields();
			Set<MethodDeclaration> accessedMethods = typeCheckElimination.getAccessedMethods();
			if(accessedFields.size() > 0 || assignedFields.size() > 0 || accessedMethods.size() > 0) {
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
				List<Expression> variableInstructions = expressionExtractor.getVariableInstructions(newEnclosingIfStatementExpression);
				modifyVariableInstructionsInSubclass(variableInstructions, subclassAST, subclassRewriter, accessedFields, assignedFields);
				List<Expression> oldMethodInvocations = expressionExtractor.getMethodInvocations(enclosingIfStatementExpression);
				List<Expression> newMethodInvocations = expressionExtractor.getMethodInvocations(newEnclosingIfStatementExpression);
				modifyMethodInvocationsInSubclass(oldMethodInvocations, newMethodInvocations, subclassAST, subclassRewriter, accessedMethods);
				subclassRewriter.set(enclosingIfStatement, IfStatement.EXPRESSION_PROPERTY, newEnclosingIfStatementExpression, null);
				Block ifStatementBody = subclassAST.newBlock();
				ifStatementBodyRewrite = subclassRewriter.getListRewrite(ifStatementBody, Block.STATEMENTS_PROPERTY);
				subclassRewriter.set(enclosingIfStatement, IfStatement.THEN_STATEMENT_PROPERTY, ifStatementBody, null);
				concreteMethodBodyRewrite.insertLast(enclosingIfStatement, null);
			}
			if(returnedVariable != null) {
				VariableDeclarationFragment variableDeclarationFragment = subclassAST.newVariableDeclarationFragment();
				subclassRewriter.set(variableDeclarationFragment, VariableDeclarationFragment.NAME_PROPERTY, returnedVariable.getName(), null);
				subclassRewriter.set(variableDeclarationFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, returnedVariable.getInitializer(), null);
				VariableDeclarationStatement variableDeclarationStatement = subclassAST.newVariableDeclarationStatement(variableDeclarationFragment);
				subclassRewriter.set(variableDeclarationStatement, VariableDeclarationStatement.TYPE_PROPERTY, typeCheckElimination.getTypeCheckMethodReturnType(), null);
				if(ifStatementBodyRewrite != null)
					ifStatementBodyRewrite.insertFirst(variableDeclarationStatement, null);
				else
					concreteMethodBodyRewrite.insertFirst(variableDeclarationStatement, null);
			}
			for(Statement statement : statements) {
				Statement newStatement = (Statement)ASTNode.copySubtree(subclassAST, statement);
				List<Expression> variableInstructions = expressionExtractor.getVariableInstructions(newStatement);
				modifyVariableInstructionsInSubclass(variableInstructions, subclassAST, subclassRewriter, accessedFields, assignedFields);
				List<Expression> oldMethodInvocations = expressionExtractor.getMethodInvocations(statement);
				List<Expression> newMethodInvocations = expressionExtractor.getMethodInvocations(newStatement);
				modifyMethodInvocationsInSubclass(oldMethodInvocations, newMethodInvocations, subclassAST, subclassRewriter, accessedMethods);
				if(ifStatementBodyRewrite != null)
					ifStatementBodyRewrite.insertLast(newStatement, null);
				else
					concreteMethodBodyRewrite.insertLast(newStatement, null);
			}
			if(returnedVariable != null) {
				ReturnStatement returnStatement = subclassAST.newReturnStatement();
				subclassRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, returnedVariable.getName(), null);
				if(ifStatementBodyRewrite != null)
					ifStatementBodyRewrite.insertLast(returnStatement, null);
				else
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
			
			if(!subclassAlreadyExists)
				subclassTypesRewrite.insertLast(subclassTypeDeclaration, null);
			
			ITextFileBuffer subclassTextFileBuffer = bufferManager.getTextFileBuffer(subclassFile.getFullPath(), LocationKind.IFILE);
			IDocument subclassDocument = subclassTextFileBuffer.getDocument();
			TextEdit subclassEdit = subclassRewriter.rewriteAST(subclassDocument, null);
			try {
				UndoEdit subclassUndoEdit = subclassEdit.apply(subclassDocument, UndoEdit.CREATE_UNDO);
				undoRefactoring.put(subclassFile, subclassDocument, subclassUndoEdit);
			} catch (MalformedTreeException e) {
				e.printStackTrace();
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
			subclassEditor.doSave(null);
		}
	}

	private void modifyMethodInvocationsInSubclass(List<Expression> oldMethodInvocations, List<Expression> newMethodInvocations, AST subclassAST,
			ASTRewrite subclassRewriter, Set<MethodDeclaration> accessedMethods) {
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
			}
			j++;
		}
	}

	private void modifyVariableInstructionsInSubclass(List<Expression> variableInstructions, AST subclassAST, ASTRewrite subclassRewriter,
			Set<VariableDeclarationFragment> accessedFields, Set<VariableDeclarationFragment> assignedFields) {
		for(Expression expression : variableInstructions) {
			SimpleName simpleName = (SimpleName)expression;
			Expression parentExpression = null;
			if(simpleName.getParent() instanceof QualifiedName) {
				parentExpression = (QualifiedName)simpleName.getParent();
			}
			else if(simpleName.getParent() instanceof FieldAccess) {
				parentExpression = (FieldAccess)simpleName.getParent();
			}
			else {
				parentExpression = simpleName;
			}
			if(parentExpression.getParent() instanceof Assignment) {
				Assignment assignment = (Assignment)parentExpression.getParent();
				Expression leftHandSide = assignment.getLeftHandSide();
				SimpleName leftHandSideName = null;
				if(leftHandSide instanceof SimpleName) {
					leftHandSideName = (SimpleName)leftHandSide;
				}
				else if(leftHandSide instanceof QualifiedName) {
					QualifiedName leftHandSideQualifiedName = (QualifiedName)leftHandSide;
					leftHandSideName = leftHandSideQualifiedName.getName();
				}
				else if(leftHandSide instanceof FieldAccess) {
					FieldAccess leftHandSideFieldAccess = (FieldAccess)leftHandSide;
					leftHandSideName = leftHandSideFieldAccess.getName();
				}
				if(leftHandSideName != null && leftHandSideName.equals(simpleName)) {
					for(VariableDeclarationFragment assignedFragment : assignedFields) {
						if(assignedFragment.getName().getIdentifier().equals(simpleName.getIdentifier())) {
							MethodInvocation leftHandMethodInvocation = subclassAST.newMethodInvocation();
							String leftHandMethodName = assignedFragment.getName().getIdentifier();
							leftHandMethodName = "set" + leftHandMethodName.substring(0,1).toUpperCase() + leftHandMethodName.substring(1,leftHandMethodName.length());
							subclassRewriter.set(leftHandMethodInvocation, MethodInvocation.NAME_PROPERTY, subclassAST.newSimpleName(leftHandMethodName), null);
							String invokerName = sourceTypeDeclaration.getName().getIdentifier();
							invokerName = invokerName.substring(0,1).toLowerCase() + invokerName.substring(1,invokerName.length());
							subclassRewriter.set(leftHandMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
							ListRewrite methodInvocationArgumentsRewrite = subclassRewriter.getListRewrite(leftHandMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
							Expression rightHandSide = assignment.getRightHandSide();
							SimpleName rightHandSideSimpleName = null;
							if(rightHandSide instanceof SimpleName) {
								rightHandSideSimpleName = (SimpleName)rightHandSide;
							}
							else if(rightHandSide instanceof QualifiedName) {
								QualifiedName qualifiedName = (QualifiedName)rightHandSide;
								rightHandSideSimpleName = qualifiedName.getName();
							}
							else if(rightHandSide instanceof FieldAccess) {
								FieldAccess fieldAccess = (FieldAccess)rightHandSide;
								rightHandSideSimpleName = fieldAccess.getName();
							}
							if(rightHandSideSimpleName != null) {
								for(VariableDeclarationFragment accessedFragment : accessedFields) {
									if(accessedFragment.getName().getIdentifier().equals(rightHandSideSimpleName.getIdentifier())) {
										MethodInvocation rightHandMethodInvocation = subclassAST.newMethodInvocation();
										String rightHandMethodName = accessedFragment.getName().getIdentifier();
										rightHandMethodName = "get" + rightHandMethodName.substring(0,1).toUpperCase() + rightHandMethodName.substring(1,rightHandMethodName.length());
										subclassRewriter.set(rightHandMethodInvocation, MethodInvocation.NAME_PROPERTY, subclassAST.newSimpleName(rightHandMethodName), null);
										subclassRewriter.set(rightHandMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
										methodInvocationArgumentsRewrite.insertLast(rightHandMethodInvocation, null);
										break;
									}
								}
							}
							else {
								methodInvocationArgumentsRewrite.insertLast(assignment.getRightHandSide(), null);
							}
							subclassRewriter.replace(assignment, leftHandMethodInvocation, null);
						}
					}
				}
			}
			else {
				for(VariableDeclarationFragment fragment : accessedFields) {
					if(fragment.getName().getIdentifier().equals(simpleName.getIdentifier())) {
						MethodInvocation methodInvocation = subclassAST.newMethodInvocation();
						String methodName = fragment.getName().getIdentifier();
						methodName = "get" + methodName.substring(0,1).toUpperCase() + methodName.substring(1,methodName.length());
						subclassRewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY, subclassAST.newSimpleName(methodName), null);
						String invokerName = sourceTypeDeclaration.getName().getIdentifier();
						invokerName = invokerName.substring(0,1).toLowerCase() + invokerName.substring(1,invokerName.length());
						subclassRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, subclassAST.newSimpleName(invokerName), null);
						subclassRewriter.replace(simpleName, methodInvocation, null);
						break;
					}
				}
			}
		}
	}

	private void modifyTypeFieldAssignmentsInContextClass() {
		AST contextAST = sourceTypeDeclaration.getAST();
		MethodDeclaration[] contextMethods = sourceTypeDeclaration.getMethods();
		List<String> staticFieldNames = typeCheckElimination.getStaticFieldNames();
		for(MethodDeclaration methodDeclaration : contextMethods) {
			Block methodBody = methodDeclaration.getBody();
			if(methodBody != null) {
				List<Statement> statements = methodBody.statements();
				ExpressionExtractor expressionExtractor = new ExpressionExtractor();
				for(Statement statement : statements) {
					List<Expression> assignments = expressionExtractor.getAssignments(statement);
					for(Expression expression : assignments) {
						Assignment assignment = (Assignment)expression;
						Expression leftHandSide = assignment.getLeftHandSide();
						SimpleName assignedVariable = null;
						Expression invoker = null;
						if(leftHandSide instanceof SimpleName) {
							assignedVariable = (SimpleName)leftHandSide;
						}
						else if(leftHandSide instanceof QualifiedName) {
							QualifiedName qualifiedName = (QualifiedName)leftHandSide;
							assignedVariable = qualifiedName.getName();
							invoker = qualifiedName.getQualifier();
						}
						else if(leftHandSide instanceof FieldAccess) {
							FieldAccess fieldAccess = (FieldAccess)leftHandSide;
							assignedVariable = fieldAccess.getName();
							invoker = fieldAccess.getExpression();
						}
						Expression rightHandSide = assignment.getRightHandSide();
						SimpleName accessedVariable = decomposeRightHandSide(rightHandSide);
						if(assignedVariable != null) {
							IBinding leftHandBinding = assignedVariable.resolveBinding();
							if(leftHandBinding.getKind() == IBinding.VARIABLE) {
								IVariableBinding assignedVariableBinding = (IVariableBinding)leftHandBinding;
								if(assignedVariableBinding.isField() && typeCheckElimination.getTypeField().resolveBinding().isEqualTo(assignedVariableBinding)) {
									MethodInvocation setterMethodInvocation = contextAST.newMethodInvocation();
									if(typeCheckElimination.getTypeFieldSetterMethod() != null) {
										sourceRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckElimination.getTypeFieldSetterMethod().getName(), null);
									}
									else {
										sourceRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("set" + typeCheckElimination.getAbstractClassName()), null);
									}
									ListRewrite setterMethodInvocationArgumentsRewrite = sourceRewriter.getListRewrite(setterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
									setterMethodInvocationArgumentsRewrite.insertLast(assignment.getRightHandSide(), null);
									if(invoker != null) {
										sourceRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, invoker, null);
									}
									sourceRewriter.replace(assignment, setterMethodInvocation, null);
									if(accessedVariable != null) {
										IBinding rightHandBinding = accessedVariable.resolveBinding();
										if(rightHandBinding.getKind() == IBinding.VARIABLE) {
											IVariableBinding accessedVariableBinding = (IVariableBinding)rightHandBinding;
											if(accessedVariableBinding.isField() && (accessedVariableBinding.getModifiers() & Modifier.STATIC) != 0 &&
													!staticFieldNames.contains(accessedVariable.getIdentifier())) {
												String subclassName = "";
												StringTokenizer tokenizer = new StringTokenizer(accessedVariable.getIdentifier(),"_");
												while(tokenizer.hasMoreTokens()) {
													String tempName = tokenizer.nextToken().toLowerCase().toString();
													subclassName += tempName.subSequence(0, 1).toString().toUpperCase() + 
													tempName.subSequence(1, tempName.length()).toString();
												}
												additionalStaticFields.put(accessedVariable.getIdentifier(), subclassName);
											}
										}
									}
								}
							}
						}
						if(accessedVariable != null) {
							IBinding rightHandBinding = accessedVariable.resolveBinding();
							if(rightHandBinding.getKind() == IBinding.VARIABLE) {
								IVariableBinding accessedVariableBinding = (IVariableBinding)rightHandBinding;
								if(accessedVariableBinding.isField() && typeCheckElimination.getTypeField().resolveBinding().isEqualTo(accessedVariableBinding)) {
									MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
									if(typeCheckElimination.getTypeFieldGetterMethod() != null) {
										sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckElimination.getTypeFieldGetterMethod().getName(), null);
									}
									else {
										sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + typeCheckElimination.getAbstractClassName()), null);
									}
									sourceRewriter.replace(accessedVariable, getterMethodInvocation, null);
								}
							}
						}
					}
				}
			}
		}
	}

	private SimpleName decomposeRightHandSide(Expression rightHandSide) {
		if(rightHandSide instanceof SimpleName) {
			return (SimpleName)rightHandSide;
		}
		else if(rightHandSide instanceof QualifiedName) {
			QualifiedName qualifiedName = (QualifiedName)rightHandSide;
			return qualifiedName.getName();
		}
		else if(rightHandSide instanceof FieldAccess) {
			FieldAccess fieldAccess = (FieldAccess)rightHandSide;
			return fieldAccess.getName();
		}
		else if(rightHandSide instanceof Assignment) {
			Assignment assignment = (Assignment)rightHandSide;
			return decomposeRightHandSide(assignment.getRightHandSide());
		}
		return null;
	}

	private void modifyTypeFieldAccessesInContextClass() {
		AST contextAST = sourceTypeDeclaration.getAST();
		MethodDeclaration[] contextMethods = sourceTypeDeclaration.getMethods();
		List<String> staticFieldNames = typeCheckElimination.getStaticFieldNames();
		for(MethodDeclaration methodDeclaration : contextMethods) {
			Block methodBody = methodDeclaration.getBody();
			if(methodBody != null) {
				List<Statement> statements = methodBody.statements();
				ExpressionExtractor expressionExtractor = new ExpressionExtractor();
				for(Statement statement : statements) {
					List<Expression> infixExpressions = expressionExtractor.getInfixExpressions(statement);
					for(Expression expression : infixExpressions) {
						InfixExpression infixExpression = (InfixExpression)expression;
						Expression leftOperand = infixExpression.getLeftOperand();
						Expression rightOperand = infixExpression.getRightOperand();
						SimpleName accessedVariable = null;
						boolean typeFieldIsReplaced = false;
						if(leftOperand instanceof SimpleName) {
							accessedVariable = (SimpleName)leftOperand;
						}
						else if(leftOperand instanceof FieldAccess) {
							FieldAccess fieldAccess = (FieldAccess)leftOperand;
							accessedVariable = fieldAccess.getName();
						}
						if(accessedVariable != null) {
							IBinding leftOperandBinding = accessedVariable.resolveBinding();
							if(leftOperandBinding.getKind() == IBinding.VARIABLE) {
								IVariableBinding accessedVariableBinding = (IVariableBinding)leftOperandBinding;
								if(accessedVariableBinding.isField() && typeCheckElimination.getTypeField().getName().getIdentifier().equals(accessedVariable.getIdentifier())) {
									MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
									if(typeCheckElimination.getTypeFieldGetterMethod() != null) {
										sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckElimination.getTypeFieldGetterMethod().getName(), null);
									}
									else {
										sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + typeCheckElimination.getAbstractClassName()), null);
									}
									sourceRewriter.replace(leftOperand, getterMethodInvocation, null);
									typeFieldIsReplaced = true;
								}
							}
						}
						if(!typeFieldIsReplaced) {
							if(rightOperand instanceof SimpleName) {
								accessedVariable = (SimpleName)rightOperand;
							}
							else if(rightOperand instanceof FieldAccess) {
								FieldAccess fieldAccess = (FieldAccess)rightOperand;
								accessedVariable = fieldAccess.getName();
							}
							if(accessedVariable != null) {
								IBinding rightOperandBinding = accessedVariable.resolveBinding();
								if(rightOperandBinding.getKind() == IBinding.VARIABLE) {
									IVariableBinding accessedVariableBinding = (IVariableBinding)rightOperandBinding;
									if(accessedVariableBinding.isField() && typeCheckElimination.getTypeField().getName().getIdentifier().equals(accessedVariable.getIdentifier())) {
										MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
										if(typeCheckElimination.getTypeFieldGetterMethod() != null) {
											sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckElimination.getTypeFieldGetterMethod().getName(), null);
										}
										else {
											sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + typeCheckElimination.getAbstractClassName()), null);
										}
										sourceRewriter.replace(rightOperand, getterMethodInvocation, null);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void generateGettersForAccessedFields() {
		AST contextAST = sourceTypeDeclaration.getAST();
		MethodDeclaration[] contextMethods = sourceTypeDeclaration.getMethods();
		for(VariableDeclarationFragment fragment : typeCheckElimination.getAccessedFields()) {
			if(!fragment.equals(typeCheckElimination.getTypeField())) {
				boolean getterFound = false;
				for(MethodDeclaration methodDeclaration : contextMethods) {
					SimpleName simpleName = MethodDeclarationUtility.isGetter(methodDeclaration);
					if(simpleName != null && simpleName.getIdentifier().equals(fragment.getName().getIdentifier())) {
						getterFound = true;
						break;
					}
				}
				if(!getterFound) {
					FieldDeclaration fieldDeclaration = (FieldDeclaration)fragment.getParent();
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
		MethodDeclaration[] contextMethods = sourceTypeDeclaration.getMethods();
		for(VariableDeclarationFragment fragment : typeCheckElimination.getAssignedFields()) {
			if(!fragment.equals(typeCheckElimination.getTypeField())) {
				boolean setterFound = false;
				for(MethodDeclaration methodDeclaration : contextMethods) {
					SimpleName simpleName = MethodDeclarationUtility.isSetter(methodDeclaration);
					if(simpleName != null && simpleName.getIdentifier().equals(fragment.getName().getIdentifier())) {
						setterFound = true;
						break;
					}
				}
				if(!setterFound) {
					FieldDeclaration fieldDeclaration = (FieldDeclaration)fragment.getParent();
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
		Type returnType = typeCheckElimination.getTypeCheckMethodReturnType();
		ITypeBinding returnTypeBinding = returnType.resolveBinding();
		if(!typeBindings.contains(returnTypeBinding))
			typeBindings.add(returnTypeBinding);
		
		Set<SingleVariableDeclaration> parameters = typeCheckElimination.getAccessedParameters();
		for(SingleVariableDeclaration parameter : parameters) {
			Type parameterType = parameter.getType();
			ITypeBinding parameterTypeBinding = parameterType.resolveBinding();
			if(!typeBindings.contains(parameterTypeBinding))
				typeBindings.add(parameterTypeBinding);			
		}
		
		Set<VariableDeclarationFragment> accessedLocalVariables = typeCheckElimination.getAccessedLocalVariables();
		for(VariableDeclarationFragment fragment : accessedLocalVariables) {
			if(!fragment.equals(returnedVariable)) {
				VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)fragment.getParent();
				Type variableType = variableDeclarationStatement.getType();
				ITypeBinding variableTypeBinding = variableType.resolveBinding();
				if(!typeBindings.contains(variableTypeBinding))
					typeBindings.add(variableTypeBinding);
			}
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
		PackageDeclaration sourcePackageDeclaration = sourceCompilationUnit.getPackage();
		String sourcePackageDeclarationName = "";
		if(sourcePackageDeclaration != null)
			sourcePackageDeclarationName = sourcePackageDeclaration.getName().getFullyQualifiedName();	
		if(!qualifiedPackageName.equals("") && !qualifiedPackageName.equals("java.lang") && !qualifiedPackageName.equals(sourcePackageDeclarationName)) {
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

	private void setPublicModifierToStaticFields() {
		FieldDeclaration[] fieldDeclarations = sourceTypeDeclaration.getFields();
		List<String> staticFieldNames = typeCheckElimination.getStaticFieldNames();
		staticFieldNames.addAll(additionalStaticFields.keySet());
		for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
			List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				boolean modifierIsReplaced = false;
				for(String staticFieldName : staticFieldNames) {
					if(staticFieldName.equals(fragment.getName().getIdentifier())) {
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
								modifierIsReplaced = true;
							}
						}
						if(!modifierFound){
							modifierRewrite.insertFirst(publicModifier, null);
							modifierIsReplaced = true;
						}
						break;
					}
				}
				if(modifierIsReplaced)
					break;
			}
		}
	}

	private void setPublicModifierToAccessedMethods() {
		for(MethodDeclaration methodDeclaration : typeCheckElimination.getAccessedMethods()) {
			List<Modifier> modifiers = methodDeclaration.modifiers();
			ListRewrite modifierRewrite = sourceRewriter.getListRewrite(methodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
			Modifier publicModifier = methodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
			boolean modifierFound = false;
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
			return (Expression)ASTNode.copySubtree(ast, expression);
		}
		return null;
	}

	public UndoRefactoring getUndoRefactoring() {
		return undoRefactoring;
	}

	public IFile getSourceFile() {
		return sourceFile;
	}

	public Statement getTypeCheckCodeFragment() {
		return typeCheckElimination.getTypeCheckCodeFragment();
	}

	public String getTypeCheckMethodName() {
		IMethodBinding typeCheckMethodBinding = typeCheckElimination.getTypeCheckMethod().resolveBinding();
		ITypeBinding declaringClassTypeBinding = typeCheckMethodBinding.getDeclaringClass();
		return declaringClassTypeBinding.getQualifiedName() + "::" + typeCheckMethodBinding.toString();
	}
}
