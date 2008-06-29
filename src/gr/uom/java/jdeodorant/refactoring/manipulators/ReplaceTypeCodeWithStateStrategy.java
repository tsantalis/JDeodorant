package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.inheritance.InheritanceTree;
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
import org.eclipse.core.resources.IResource;
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
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MemberRef;
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
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclaration;
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
	private VariableDeclaration returnedVariable;
	private Set<ITypeBinding> requiredImportDeclarationsBasedOnSignature;
	private Set<ITypeBinding> requiredImportDeclarationsForContext;
	private Set<ITypeBinding> thrownExceptions;
	private Map<SimpleName, String> additionalStaticFields;
	
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
		this.requiredImportDeclarationsForContext = new LinkedHashSet<ITypeBinding>();
		this.thrownExceptions = typeCheckElimination.getThrownExceptions();
		this.additionalStaticFields = new LinkedHashMap<SimpleName, String>();
		for(SimpleName simpleName : typeCheckElimination.getAdditionalStaticFields()) {
			this.additionalStaticFields.put(simpleName, generateSubclassName(simpleName));
		}
	}

	public void apply() {
		if(typeCheckElimination.getTypeField() != null) {
			modifyTypeFieldAssignmentsInContextClass();
			modifyTypeFieldAccessesInContextClass();
		}
		else if(typeCheckElimination.getTypeLocalVariable() != null) {
			identifyTypeLocalVariableAssignmentsInTypeCheckMethod();
			identifyTypeLocalVariableAccessesInTypeCheckMethod();
		}
		createStateStrategyHierarchy();
		if(typeCheckElimination.getTypeField() != null)
			modifyContext();
		else if(typeCheckElimination.getTypeLocalVariable() != null || typeCheckElimination.getTypeMethodInvocation() != null)
			modifyTypeCheckMethod();
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
		
		MethodDeclaration setterMethod = typeCheckElimination.getTypeFieldSetterMethod();
		SwitchStatement switchStatement = contextAST.newSwitchStatement();
		List<SimpleName> staticFieldNames = typeCheckElimination.getStaticFields();
		List<String> subclassNames = typeCheckElimination.getSubclassNames();
		ListRewrite switchStatementStatementsRewrite = sourceRewriter.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);
		int i = 0;
		for(SimpleName staticFieldName : staticFieldNames) {
			SwitchCase switchCase = contextAST.newSwitchCase();
			IBinding staticFieldNameBinding = staticFieldName.resolveBinding();
			String staticFieldNameDeclaringClass = null;
			boolean isEnumConstant = false;
			if(staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
				IVariableBinding staticFieldNameVariableBinding = (IVariableBinding)staticFieldNameBinding;
				isEnumConstant = staticFieldNameVariableBinding.isEnumConstant();
				if(!sourceTypeDeclaration.resolveBinding().isEqualTo(staticFieldNameVariableBinding.getDeclaringClass())) {
					staticFieldNameDeclaringClass = staticFieldNameVariableBinding.getDeclaringClass().getName();
				}
			}
			if(staticFieldNameDeclaringClass == null || isEnumConstant) {
				sourceRewriter.set(switchCase, SwitchCase.EXPRESSION_PROPERTY, staticFieldName, null);
			}
			else {
				FieldAccess fieldAccess = contextAST.newFieldAccess();
				sourceRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, contextAST.newSimpleName(staticFieldNameDeclaringClass), null);
				sourceRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, staticFieldName, null);
				sourceRewriter.set(switchCase, SwitchCase.EXPRESSION_PROPERTY, fieldAccess, null);
			}
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
		for(SimpleName staticFieldName : additionalStaticFields.keySet()) {
			SwitchCase switchCase = contextAST.newSwitchCase();
			IBinding staticFieldNameBinding = staticFieldName.resolveBinding();
			String staticFieldNameDeclaringClass = null;
			boolean isEnumConstant = false;
			if(staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
				IVariableBinding staticFieldNameVariableBinding = (IVariableBinding)staticFieldNameBinding;
				isEnumConstant = staticFieldNameVariableBinding.isEnumConstant();
				if(!sourceTypeDeclaration.resolveBinding().isEqualTo(staticFieldNameVariableBinding.getDeclaringClass())) {
					staticFieldNameDeclaringClass = staticFieldNameVariableBinding.getDeclaringClass().getName();
				}
			}
			if(staticFieldNameDeclaringClass == null || isEnumConstant) {
				sourceRewriter.set(switchCase, SwitchCase.EXPRESSION_PROPERTY, staticFieldName, null);
			}
			else {
				FieldAccess fieldAccess = contextAST.newFieldAccess();
				sourceRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, contextAST.newSimpleName(staticFieldNameDeclaringClass), null);
				sourceRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, staticFieldName, null);
				sourceRewriter.set(switchCase, SwitchCase.EXPRESSION_PROPERTY, fieldAccess, null);
			}
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
		SwitchCase switchCase = contextAST.newSwitchCase();
		sourceRewriter.set(switchCase, SwitchCase.EXPRESSION_PROPERTY, null, null);
		switchStatementStatementsRewrite.insertLast(switchCase, null);
		Assignment nullAssignment = contextAST.newAssignment();
		sourceRewriter.set(nullAssignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.ASSIGN, null);
		FieldAccess typeFieldAccess = contextAST.newFieldAccess();
		sourceRewriter.set(typeFieldAccess, FieldAccess.EXPRESSION_PROPERTY, contextAST.newThisExpression(), null);
		sourceRewriter.set(typeFieldAccess, FieldAccess.NAME_PROPERTY, typeCheckElimination.getTypeField().getName(), null);
		sourceRewriter.set(nullAssignment, Assignment.LEFT_HAND_SIDE_PROPERTY, typeFieldAccess, null);
		sourceRewriter.set(nullAssignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, contextAST.newNullLiteral(), null);
		switchStatementStatementsRewrite.insertLast(contextAST.newExpressionStatement(nullAssignment), null);
		switchStatementStatementsRewrite.insertLast(contextAST.newBreakStatement(), null);
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
		
		Block typeCheckCodeFragmentParentBlock = (Block)typeCheckElimination.getTypeCheckCodeFragment().getParent();
		ListRewrite typeCheckCodeFragmentParentBlockStatementsRewrite = sourceRewriter.getListRewrite(typeCheckCodeFragmentParentBlock, Block.STATEMENTS_PROPERTY);
		if(returnedVariable == null && !typeCheckElimination.typeCheckCodeFragmentContainsReturnStatement()) {
			MethodInvocation abstractMethodInvocation = contextAST.newMethodInvocation();
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName(typeCheckElimination.getAbstractMethodName()), null);
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(typeCheckElimination.getTypeField().getName().getIdentifier()), null);
			ListRewrite methodInvocationArgumentsRewrite = sourceRewriter.getListRewrite(abstractMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
			for(SingleVariableDeclaration abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
				if(!abstractMethodParameter.equals(returnedVariable)) {
					methodInvocationArgumentsRewrite.insertLast(abstractMethodParameter.getName(), null);
				}
			}
			for(VariableDeclaration fragment : typeCheckElimination.getAccessedLocalVariables()) {
				if(!fragment.equals(returnedVariable)) {
					methodInvocationArgumentsRewrite.insertLast(fragment.getName(), null);
				}
			}
			if(typeCheckElimination.getAccessedFields().size() > 0 || typeCheckElimination.getAssignedFields().size() > 0 ||
					typeCheckElimination.getAccessedMethods().size() > 0  || typeCheckElimination.getSuperAccessedMethods().size() > 0 ||
					typeCheckElimination.getSuperAccessedFieldBindings().size() > 0 || typeCheckElimination.getSuperAssignedFieldBindings().size() > 0) {
				methodInvocationArgumentsRewrite.insertLast(contextAST.newThisExpression(), null);
			}
			ExpressionStatement expressionStatement = contextAST.newExpressionStatement(abstractMethodInvocation);
			typeCheckCodeFragmentParentBlockStatementsRewrite.replace(typeCheckElimination.getTypeCheckCodeFragment(), expressionStatement, null);
		}
		else {
			MethodInvocation abstractMethodInvocation = contextAST.newMethodInvocation();
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName(typeCheckElimination.getAbstractMethodName()), null);
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(typeCheckElimination.getTypeField().getName().getIdentifier()), null);
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
				if(!abstractMethodParameter.equals(returnedVariable)) {
					methodInvocationArgumentsRewrite.insertLast(abstractMethodParameter.getName(), null);
				}
			}
			for(VariableDeclaration fragment : typeCheckElimination.getAccessedLocalVariables()) {
				if(!fragment.equals(returnedVariable)) {
					methodInvocationArgumentsRewrite.insertLast(fragment.getName(), null);
				}
			}
			if(typeCheckElimination.getAccessedFields().size() > 0 || typeCheckElimination.getAssignedFields().size() > 0 ||
					typeCheckElimination.getAccessedMethods().size() > 0 || typeCheckElimination.getSuperAccessedMethods().size() > 0 ||
					typeCheckElimination.getSuperAccessedFieldBindings().size() > 0 || typeCheckElimination.getSuperAssignedFieldBindings().size() > 0) {
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
		
		for(ITypeBinding typeBinding : requiredImportDeclarationsForContext) {
			addImportDeclaration(typeBinding, sourceCompilationUnit, sourceRewriter);
		}
		
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

	private void modifyTypeCheckMethod() {
		AST contextAST = sourceTypeDeclaration.getAST();
		ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		
		if(!typeObjectGetterMethodAlreadyExists()) {
			SwitchStatement switchStatement = contextAST.newSwitchStatement();
			List<SimpleName> staticFieldNames = typeCheckElimination.getStaticFields();
			List<String> subclassNames = typeCheckElimination.getSubclassNames();
			ListRewrite switchStatementStatementsRewrite = sourceRewriter.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);
			int i = 0;
			for(SimpleName staticFieldName : staticFieldNames) {
				SwitchCase switchCase = contextAST.newSwitchCase();
				IBinding staticFieldNameBinding = staticFieldName.resolveBinding();
				String staticFieldNameDeclaringClass = null;
				boolean isEnumConstant = false;
				if(staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
					IVariableBinding staticFieldNameVariableBinding = (IVariableBinding)staticFieldNameBinding;
					isEnumConstant = staticFieldNameVariableBinding.isEnumConstant();
					if(!sourceTypeDeclaration.resolveBinding().isEqualTo(staticFieldNameVariableBinding.getDeclaringClass())) {
						staticFieldNameDeclaringClass = staticFieldNameVariableBinding.getDeclaringClass().getName();
					}
				}
				if(staticFieldNameDeclaringClass == null || isEnumConstant) {
					sourceRewriter.set(switchCase, SwitchCase.EXPRESSION_PROPERTY, staticFieldName, null);
				}
				else {
					FieldAccess fieldAccess = contextAST.newFieldAccess();
					sourceRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, contextAST.newSimpleName(staticFieldNameDeclaringClass), null);
					sourceRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, staticFieldName, null);
					sourceRewriter.set(switchCase, SwitchCase.EXPRESSION_PROPERTY, fieldAccess, null);
				}
				switchStatementStatementsRewrite.insertLast(switchCase, null);
				ReturnStatement returnStatement = contextAST.newReturnStatement();
				ClassInstanceCreation classInstanceCreation = contextAST.newClassInstanceCreation();
				sourceRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY, contextAST.newSimpleName(subclassNames.get(i)), null);
				sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, classInstanceCreation, null);
				switchStatementStatementsRewrite.insertLast(returnStatement, null);
				i++;
			}
			for(SimpleName staticFieldName : additionalStaticFields.keySet()) {
				SwitchCase switchCase = contextAST.newSwitchCase();
				IBinding staticFieldNameBinding = staticFieldName.resolveBinding();
				String staticFieldNameDeclaringClass = null;
				boolean isEnumConstant = false;
				if(staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
					IVariableBinding staticFieldNameVariableBinding = (IVariableBinding)staticFieldNameBinding;
					isEnumConstant = staticFieldNameVariableBinding.isEnumConstant();
					if(!sourceTypeDeclaration.resolveBinding().isEqualTo(staticFieldNameVariableBinding.getDeclaringClass())) {
						staticFieldNameDeclaringClass = staticFieldNameVariableBinding.getDeclaringClass().getName();
					}
				}
				if(staticFieldNameDeclaringClass == null || isEnumConstant) {
					sourceRewriter.set(switchCase, SwitchCase.EXPRESSION_PROPERTY, staticFieldName, null);
				}
				else {
					FieldAccess fieldAccess = contextAST.newFieldAccess();
					sourceRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, contextAST.newSimpleName(staticFieldNameDeclaringClass), null);
					sourceRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, staticFieldName, null);
					sourceRewriter.set(switchCase, SwitchCase.EXPRESSION_PROPERTY, fieldAccess, null);
				}
				switchStatementStatementsRewrite.insertLast(switchCase, null);
				ReturnStatement returnStatement = contextAST.newReturnStatement();
				ClassInstanceCreation classInstanceCreation = contextAST.newClassInstanceCreation();
				sourceRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY, contextAST.newSimpleName(additionalStaticFields.get(staticFieldName)), null);
				sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, classInstanceCreation, null);
				switchStatementStatementsRewrite.insertLast(returnStatement, null);
			}
			
			MethodDeclaration setterMethodDeclaration = contextAST.newMethodDeclaration();
			sourceRewriter.set(setterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, contextAST.newSimpleName("get" + typeCheckElimination.getAbstractClassName() + "Object"), null);
			sourceRewriter.set(setterMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, contextAST.newSimpleType(contextAST.newSimpleName(typeCheckElimination.getAbstractClassName())), null);
			ListRewrite setterMethodModifiersRewrite = sourceRewriter.getListRewrite(setterMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
			setterMethodModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD), null);
			if((typeCheckElimination.getTypeCheckMethod().resolveBinding().getModifiers() & Modifier.STATIC) != 0)
				setterMethodModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD), null);
			ListRewrite setterMethodParameterRewrite = sourceRewriter.getListRewrite(setterMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
			SingleVariableDeclaration parameter = contextAST.newSingleVariableDeclaration();
			Type parameterType = null;
			SimpleName parameterName = null;
			if(typeCheckElimination.getTypeLocalVariable() != null) {
				VariableDeclaration typeLocalVariable = typeCheckElimination.getTypeLocalVariable();
				if(typeLocalVariable instanceof SingleVariableDeclaration) {
					SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)typeLocalVariable;
					parameterType = singleVariableDeclaration.getType();
					parameterName = singleVariableDeclaration.getName();
				}
				else if(typeLocalVariable instanceof VariableDeclarationFragment) {
					VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)typeLocalVariable;
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
					parameterType = variableDeclarationStatement.getType();
					parameterName = variableDeclarationFragment.getName();
				}
			}
			else if(typeCheckElimination.getForeignTypeField() != null) {
				VariableDeclarationFragment foreignTypeField = typeCheckElimination.getForeignTypeField();
				FieldDeclaration fieldDeclaration = (FieldDeclaration)foreignTypeField.getParent();
				parameterType = fieldDeclaration.getType();
				parameterName = foreignTypeField.getName();
				
			}
			sourceRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, parameterType, null);
			sourceRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, parameterName, null);
			setterMethodParameterRewrite.insertLast(parameter, null);
			
			sourceRewriter.set(switchStatement, SwitchStatement.EXPRESSION_PROPERTY, parameterName, null);
			Block setterMethodBody = contextAST.newBlock();
			ListRewrite setterMethodBodyRewrite = sourceRewriter.getListRewrite(setterMethodBody, Block.STATEMENTS_PROPERTY);
			setterMethodBodyRewrite.insertLast(switchStatement, null);
			ReturnStatement defaultReturnStatement = contextAST.newReturnStatement();
			sourceRewriter.set(defaultReturnStatement, ReturnStatement.EXPRESSION_PROPERTY, contextAST.newNullLiteral(), null);
			setterMethodBodyRewrite.insertLast(defaultReturnStatement, null);
			sourceRewriter.set(setterMethodDeclaration, MethodDeclaration.BODY_PROPERTY, setterMethodBody, null);
			contextBodyRewrite.insertLast(setterMethodDeclaration, null);
		}
		
		Block typeCheckCodeFragmentParentBlock = (Block)typeCheckElimination.getTypeCheckCodeFragment().getParent();
		ListRewrite typeCheckCodeFragmentParentBlockStatementsRewrite = sourceRewriter.getListRewrite(typeCheckCodeFragmentParentBlock, Block.STATEMENTS_PROPERTY);
		if(returnedVariable == null && !typeCheckElimination.typeCheckCodeFragmentContainsReturnStatement()) {
			MethodInvocation abstractMethodInvocation = contextAST.newMethodInvocation();
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName(typeCheckElimination.getAbstractMethodName()), null);
			MethodInvocation invokerMethodInvocation = contextAST.newMethodInvocation();
			sourceRewriter.set(invokerMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + typeCheckElimination.getAbstractClassName() + "Object"), null);
			ListRewrite invokerMethodInvocationArgumentsRewrite = sourceRewriter.getListRewrite(invokerMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
			if(typeCheckElimination.getTypeLocalVariable() != null)
				invokerMethodInvocationArgumentsRewrite.insertLast(typeCheckElimination.getTypeLocalVariable().getName(), null);
			else if(typeCheckElimination.getTypeMethodInvocation() != null)
				invokerMethodInvocationArgumentsRewrite.insertLast(typeCheckElimination.getTypeMethodInvocation(), null);
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, invokerMethodInvocation, null);
			ListRewrite abstractMethodInvocationArgumentsRewrite = sourceRewriter.getListRewrite(abstractMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
			for(SingleVariableDeclaration abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
				if(!abstractMethodParameter.equals(returnedVariable)) {
					abstractMethodInvocationArgumentsRewrite.insertLast(abstractMethodParameter.getName(), null);
				}
			}
			for(VariableDeclaration fragment : typeCheckElimination.getAccessedLocalVariables()) {
				if(!fragment.equals(returnedVariable)) {
					abstractMethodInvocationArgumentsRewrite.insertLast(fragment.getName(), null);
				}
			}
			if(typeCheckElimination.getAccessedFields().size() > 0 || typeCheckElimination.getAssignedFields().size() > 0 ||
					typeCheckElimination.getAccessedMethods().size() > 0 || typeCheckElimination.getSuperAccessedMethods().size() > 0 ||
					typeCheckElimination.getSuperAccessedFieldBindings().size() > 0 || typeCheckElimination.getSuperAssignedFieldBindings().size() > 0) {
				abstractMethodInvocationArgumentsRewrite.insertLast(contextAST.newThisExpression(), null);
			}
			ExpressionStatement expressionStatement = contextAST.newExpressionStatement(abstractMethodInvocation);
			typeCheckCodeFragmentParentBlockStatementsRewrite.replace(typeCheckElimination.getTypeCheckCodeFragment(), expressionStatement, null);
		}
		else {
			MethodInvocation abstractMethodInvocation = contextAST.newMethodInvocation();
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName(typeCheckElimination.getAbstractMethodName()), null);
			MethodInvocation invokerMethodInvocation = contextAST.newMethodInvocation();
			sourceRewriter.set(invokerMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + typeCheckElimination.getAbstractClassName() + "Object"), null);
			ListRewrite invokerMethodInvocationArgumentsRewrite = sourceRewriter.getListRewrite(invokerMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
			if(typeCheckElimination.getTypeLocalVariable() != null)
				invokerMethodInvocationArgumentsRewrite.insertLast(typeCheckElimination.getTypeLocalVariable().getName(), null);
			else if(typeCheckElimination.getTypeMethodInvocation() != null)
				invokerMethodInvocationArgumentsRewrite.insertLast(typeCheckElimination.getTypeMethodInvocation(), null);
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, invokerMethodInvocation, null);
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
				if(!abstractMethodParameter.equals(returnedVariable)) {
					methodInvocationArgumentsRewrite.insertLast(abstractMethodParameter.getName(), null);
				}
			}
			for(VariableDeclaration fragment : typeCheckElimination.getAccessedLocalVariables()) {
				if(!fragment.equals(returnedVariable)) {
					methodInvocationArgumentsRewrite.insertLast(fragment.getName(), null);
				}
			}
			if(typeCheckElimination.getAccessedFields().size() > 0 || typeCheckElimination.getAssignedFields().size() > 0 ||
					typeCheckElimination.getAccessedMethods().size() > 0 || typeCheckElimination.getSuperAccessedMethods().size() > 0 ||
					typeCheckElimination.getSuperAccessedFieldBindings().size() > 0 || typeCheckElimination.getSuperAssignedFieldBindings().size() > 0) {
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
		
		for(ITypeBinding typeBinding : requiredImportDeclarationsForContext) {
			addImportDeclaration(typeBinding, sourceCompilationUnit, sourceRewriter);
		}
		
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

	private boolean typeObjectGetterMethodAlreadyExists() {
		InheritanceTree tree = typeCheckElimination.getInheritanceTreeMatchingWithStaticTypes();
		if(tree != null) {
			MethodDeclaration[] contextMethods = sourceTypeDeclaration.getMethods();
			DefaultMutableTreeNode rootNode = tree.getRootNode();
			String rootClassName = (String)rootNode.getUserObject();
			DefaultMutableTreeNode leaf = rootNode.getFirstLeaf();
			List<String> subclassNames = new ArrayList<String>();
			while(leaf != null) {
				subclassNames.add((String)leaf.getUserObject());
				leaf = leaf.getNextLeaf();
			}
			for(MethodDeclaration contextMethod : contextMethods) {
				Type returnType = contextMethod.getReturnType2();
				if(returnType != null) {
					if(returnType.resolveBinding().getQualifiedName().equals(rootClassName)) {
						Block contextMethodBody = contextMethod.getBody();
						if(contextMethodBody != null) {
							List<Statement> statements = contextMethodBody.statements();
							if(statements.size() > 0 && statements.get(0) instanceof SwitchStatement) {
								SwitchStatement switchStatement = (SwitchStatement)statements.get(0);
								List<Statement> statements2 = switchStatement.statements();
								int matchCounter = 0;
								for(Statement statement2 : statements2) {
									if(statement2 instanceof ReturnStatement) {
										ReturnStatement returnStatement = (ReturnStatement)statement2;
										Expression returnStatementExpression = returnStatement.getExpression();
										if(returnStatementExpression instanceof ClassInstanceCreation) {
											ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)returnStatementExpression;
											Type classInstanceCreationType = classInstanceCreation.getType();
											if(subclassNames.contains(classInstanceCreationType.resolveBinding().getQualifiedName())) {
												matchCounter++;
											}
										}
									}
								}
								if(matchCounter == subclassNames.size())
									return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private void createStateStrategyHierarchy() {
		IContainer contextContainer = (IContainer)sourceFile.getParent();
		PackageDeclaration contextPackageDeclaration = sourceCompilationUnit.getPackage();
		IContainer rootContainer = contextContainer;
		if(contextPackageDeclaration != null) {
			String packageName = contextPackageDeclaration.getName().getFullyQualifiedName();
			String[] subPackages = packageName.split("\\.");
			for(int i = 0; i<subPackages.length; i++)
				rootContainer = (IContainer)rootContainer.getParent();
		}
		InheritanceTree tree = typeCheckElimination.getInheritanceTreeMatchingWithStaticTypes();
		IFile stateStrategyFile = null;
		if(tree != null) {
			DefaultMutableTreeNode rootNode = tree.getRootNode();
			stateStrategyFile = getFile(rootContainer, (String)rootNode.getUserObject());
		}
		else {
			if(contextContainer instanceof IProject) {
				IProject contextProject = (IProject)contextContainer;
				stateStrategyFile = contextProject.getFile(typeCheckElimination.getAbstractClassName() + ".java");
			}
			else if(contextContainer instanceof IFolder) {
				IFolder contextFolder = (IFolder)contextContainer;
				stateStrategyFile = contextFolder.getFile(typeCheckElimination.getAbstractClassName() + ".java");
			}
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
						requiredImportDeclarationsForContext.add(stateStrategyTypeDeclaration.resolveBinding());
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
		if(typeCheckElimination.getTypeField() != null) {
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
		}
		
		MethodDeclaration abstractMethodDeclaration = stateStrategyAST.newMethodDeclaration();
		stateStrategyRewriter.set(abstractMethodDeclaration, MethodDeclaration.NAME_PROPERTY, stateStrategyAST.newSimpleName(typeCheckElimination.getAbstractMethodName()), null);
		if(returnedVariable == null && !typeCheckElimination.typeCheckCodeFragmentContainsReturnStatement()) {
			stateStrategyRewriter.set(abstractMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, stateStrategyAST.newPrimitiveType(PrimitiveType.VOID), null);
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
				stateStrategyRewriter.set(abstractMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnType, null);
			}
			else {
				stateStrategyRewriter.set(abstractMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, typeCheckElimination.getTypeCheckMethodReturnType(), null);
			}
		}
		ListRewrite abstractMethodModifiersRewrite = stateStrategyRewriter.getListRewrite(abstractMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		abstractMethodModifiersRewrite.insertLast(stateStrategyAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
		abstractMethodModifiersRewrite.insertLast(stateStrategyAST.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
		ListRewrite abstractMethodParametersRewrite = stateStrategyRewriter.getListRewrite(abstractMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		if(returnedVariable != null) {
			if(returnedVariable instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)returnedVariable;
				abstractMethodParametersRewrite.insertLast(singleVariableDeclaration, null);
			}
			else if(returnedVariable instanceof VariableDeclarationFragment) {
				SingleVariableDeclaration parameter = stateStrategyAST.newSingleVariableDeclaration();
				VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)returnedVariable;
				VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
				stateStrategyRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, variableDeclarationStatement.getType(), null);
				stateStrategyRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, variableDeclarationFragment.getName(), null);
				abstractMethodParametersRewrite.insertLast(parameter, null);
			}
		}
		for(SingleVariableDeclaration abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
			if(!abstractMethodParameter.equals(returnedVariable)) {
				abstractMethodParametersRewrite.insertLast(abstractMethodParameter, null);
			}
		}
		for(VariableDeclaration fragment : typeCheckElimination.getAccessedLocalVariables()) {
			if(!fragment.equals(returnedVariable)) {
				if(fragment instanceof SingleVariableDeclaration) {
					SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)fragment;
					abstractMethodParametersRewrite.insertLast(singleVariableDeclaration, null);
				}
				else if(fragment instanceof VariableDeclarationFragment) {
					SingleVariableDeclaration parameter = stateStrategyAST.newSingleVariableDeclaration();
					VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)fragment;
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
					stateStrategyRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, variableDeclarationStatement.getType(), null);
					stateStrategyRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, variableDeclarationFragment.getName(), null);
					abstractMethodParametersRewrite.insertLast(parameter, null);
				}
			}
		}
		if(typeCheckElimination.getAccessedFields().size() > 0 || typeCheckElimination.getAssignedFields().size() > 0 ||
				typeCheckElimination.getAccessedMethods().size() > 0 || typeCheckElimination.getSuperAccessedMethods().size() > 0 ||
				typeCheckElimination.getSuperAccessedFieldBindings().size() > 0 || typeCheckElimination.getSuperAssignedFieldBindings().size() > 0) {
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
		if(tree != null) {
			DefaultMutableTreeNode rootNode = tree.getRootNode();
			DefaultMutableTreeNode leaf = rootNode.getFirstLeaf();
			while(leaf != null) {
				String qualifiedSubclassName = (String)leaf.getUserObject();
				String subclassName = null;
				if(qualifiedSubclassName.contains("."))
					subclassName = qualifiedSubclassName.substring(qualifiedSubclassName.lastIndexOf(".")+1,qualifiedSubclassName.length());
				else
					subclassName = qualifiedSubclassName;
				if(!subclassNames.contains(subclassName))
					subclassNames.add(subclassName);
				leaf = leaf.getNextLeaf();
			}
		}
		List<SimpleName> staticFields = typeCheckElimination.getStaticFields();
		for(SimpleName simpleName : additionalStaticFields.keySet())
			staticFields.add(simpleName);
		
		for(Expression expression : typeCheckElimination.getTypeCheckExpressions()) {
			List<SimpleName> leafStaticFields = typeCheckElimination.getStaticFields(expression);
			if(leafStaticFields.size() > 1) {
				List<String> leafSubclassNames = new ArrayList<String>();
				for(SimpleName leafStaticField : leafStaticFields) {
					leafSubclassNames.add(generateSubclassName(leafStaticField));
				}
				ArrayList<Statement> typeCheckStatements2 = typeCheckElimination.getTypeCheckStatements(expression);
				createIntermediateClassAndItsSubclasses(leafStaticFields, leafSubclassNames, typeCheckStatements2,
						tree, rootContainer, contextContainer);
				staticFields.removeAll(leafStaticFields);
				subclassNames.removeAll(leafSubclassNames);
				typeCheckStatements.remove(typeCheckStatements2);
			}
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
			IFile subclassFile = null;
			if(tree != null) {
				DefaultMutableTreeNode rootNode = tree.getRootNode();
				DefaultMutableTreeNode leaf = rootNode.getFirstLeaf();
				while(leaf != null) {
					String qualifiedSubclassName = (String)leaf.getUserObject();
					if((qualifiedSubclassName.contains(".") && qualifiedSubclassName.endsWith("." + subclassNames.get(i))) || qualifiedSubclassName.equals(subclassNames.get(i))) {
						subclassFile = getFile(rootContainer, qualifiedSubclassName);
						break;
					}
					leaf = leaf.getNextLeaf();
				}
			}
			else {
				if(contextContainer instanceof IProject) {
					IProject contextProject = (IProject)contextContainer;
					subclassFile = contextProject.getFile(subclassNames.get(i) + ".java");
				}
				else if(contextContainer instanceof IFolder) {
					IFolder contextFolder = (IFolder)contextContainer;
					subclassFile = contextFolder.getFile(subclassNames.get(i) + ".java");
				}
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
							requiredImportDeclarationsForContext.add(subclassTypeDeclaration.resolveBinding());
							break;
						}
					}
				}
			}
			else {
				if(sourceCompilationUnit.getPackage() != null) {
					subclassRewriter.set(subclassCompilationUnit, CompilationUnit.PACKAGE_PROPERTY, sourceCompilationUnit.getPackage(), null);
				}
				Javadoc subclassJavaDoc = subclassAST.newJavadoc();
				TagElement subclassTagElement = subclassAST.newTagElement();
				subclassRewriter.set(subclassTagElement, TagElement.TAG_NAME_PROPERTY, TagElement.TAG_SEE, null);
				
				MemberRef subclassMemberRef = subclassAST.newMemberRef();
				IBinding staticFieldNameBinding = staticFields.get(i).resolveBinding();
				ITypeBinding staticFieldNameDeclaringClass = null;
				if(staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
					IVariableBinding staticFieldNameVariableBinding = (IVariableBinding)staticFieldNameBinding;
					staticFieldNameDeclaringClass = staticFieldNameVariableBinding.getDeclaringClass();
				}
				subclassRewriter.set(subclassMemberRef, MemberRef.NAME_PROPERTY, subclassAST.newSimpleName(staticFieldNameBinding.getName()), null);
				subclassRewriter.set(subclassMemberRef, MemberRef.QUALIFIER_PROPERTY, subclassAST.newName(staticFieldNameDeclaringClass.getQualifiedName()), null);
				
				ListRewrite subclassTagElementFragmentsRewrite = subclassRewriter.getListRewrite(subclassTagElement, TagElement.FRAGMENTS_PROPERTY);
				subclassTagElementFragmentsRewrite.insertLast(subclassMemberRef, null);
				
				ListRewrite subclassJavaDocTagsRewrite = subclassRewriter.getListRewrite(subclassJavaDoc, Javadoc.TAGS_PROPERTY);
				subclassJavaDocTagsRewrite.insertLast(subclassTagElement, null);
				
				subclassTypeDeclaration = subclassAST.newTypeDeclaration();
				SimpleName subclassName = subclassAST.newSimpleName(subclassNames.get(i));
				subclassRewriter.set(subclassTypeDeclaration, TypeDeclaration.NAME_PROPERTY, subclassName, null);
				subclassRewriter.set(subclassTypeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, subclassAST.newSimpleType(subclassAST.newSimpleName(typeCheckElimination.getAbstractClassName())), null);
				ListRewrite subclassModifiersRewrite = subclassRewriter.getListRewrite(subclassTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
				subclassModifiersRewrite.insertLast(subclassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
				subclassRewriter.set(subclassTypeDeclaration, TypeDeclaration.JAVADOC_PROPERTY, subclassJavaDoc, null);
			}
			
			ListRewrite subclassBodyRewrite = subclassRewriter.getListRewrite(subclassTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			
			if(typeCheckElimination.getTypeField() != null) {
				if(getterMethod != null) {
					MethodDeclaration concreteGetterMethodDeclaration = subclassAST.newMethodDeclaration();
					subclassRewriter.set(concreteGetterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, getterMethod.getName(), null);
					subclassRewriter.set(concreteGetterMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, getterMethod.getReturnType2(), null);
					ListRewrite concreteGetterMethodModifiersRewrite = subclassRewriter.getListRewrite(concreteGetterMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
					concreteGetterMethodModifiersRewrite.insertLast(subclassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
					Block concreteGetterMethodBody = subclassAST.newBlock();
					ListRewrite concreteGetterMethodBodyRewrite = subclassRewriter.getListRewrite(concreteGetterMethodBody, Block.STATEMENTS_PROPERTY);
					ReturnStatement returnStatement = subclassAST.newReturnStatement();
					IBinding staticFieldNameBinding = staticFields.get(i).resolveBinding();
					String staticFieldNameDeclaringClass = null;
					if(staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
						IVariableBinding staticFieldNameVariableBinding = (IVariableBinding)staticFieldNameBinding;
						staticFieldNameDeclaringClass = staticFieldNameVariableBinding.getDeclaringClass().getName();
					}
					FieldAccess fieldAccess = subclassAST.newFieldAccess();
					subclassRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, staticFields.get(i), null);
					subclassRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, subclassAST.newSimpleName(staticFieldNameDeclaringClass), null);
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
					IBinding staticFieldNameBinding = staticFields.get(i).resolveBinding();
					String staticFieldNameDeclaringClass = null;
					if(staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
						IVariableBinding staticFieldNameVariableBinding = (IVariableBinding)staticFieldNameBinding;
						staticFieldNameDeclaringClass = staticFieldNameVariableBinding.getDeclaringClass().getName();
					}
					FieldAccess fieldAccess = subclassAST.newFieldAccess();
					subclassRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, staticFields.get(i), null);
					subclassRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, subclassAST.newSimpleName(staticFieldNameDeclaringClass), null);
					subclassRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, fieldAccess, null);
					concreteGetterMethodBodyRewrite.insertLast(returnStatement, null);
					subclassRewriter.set(concreteGetterMethodDeclaration, MethodDeclaration.BODY_PROPERTY, concreteGetterMethodBody, null);
					subclassBodyRewrite.insertLast(concreteGetterMethodDeclaration, null);
				}
			}
			
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
				if(!abstractMethodParameter.equals(returnedVariable)) {
					concreteMethodParametersRewrite.insertLast(abstractMethodParameter, null);
				}
			}
			for(VariableDeclaration fragment : typeCheckElimination.getAccessedLocalVariables()) {
				if(!fragment.equals(returnedVariable)) {
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
			Set<VariableDeclarationFragment> accessedFields = typeCheckElimination.getAccessedFields();
			Set<VariableDeclarationFragment> assignedFields = typeCheckElimination.getAssignedFields();
			Set<MethodDeclaration> accessedMethods = typeCheckElimination.getAccessedMethods();
			Set<IMethodBinding> superAccessedMethods = typeCheckElimination.getSuperAccessedMethods();
			Set<IVariableBinding> superAccessedFields = typeCheckElimination.getSuperAccessedFieldBindings();
			Set<IVariableBinding> superAssignedFields = typeCheckElimination.getSuperAssignedFieldBindings();
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
				modifyVariableInstructionsInSubclass(oldVariableInstructions, newVariableInstructions, subclassAST, subclassRewriter, accessedFields, assignedFields, superAccessedFields, superAssignedFields);
				List<Expression> oldMethodInvocations = expressionExtractor.getMethodInvocations(enclosingIfStatementExpression);
				List<Expression> newMethodInvocations = expressionExtractor.getMethodInvocations(newEnclosingIfStatementExpression);
				modifyMethodInvocationsInSubclass(oldMethodInvocations, newMethodInvocations, subclassAST, subclassRewriter, accessedMethods, superAccessedMethods);
				replaceThisExpressionWithContextParameterInMethodInvocationArguments(newMethodInvocations, subclassAST, subclassRewriter);
				subclassRewriter.set(enclosingIfStatement, IfStatement.EXPRESSION_PROPERTY, newEnclosingIfStatementExpression, null);
				Block ifStatementBody = subclassAST.newBlock();
				ifStatementBodyRewrite = subclassRewriter.getListRewrite(ifStatementBody, Block.STATEMENTS_PROPERTY);
				subclassRewriter.set(enclosingIfStatement, IfStatement.THEN_STATEMENT_PROPERTY, ifStatementBody, null);
				concreteMethodBodyRewrite.insertLast(enclosingIfStatement, null);
			}
			for(Statement statement : statements) {
				Statement newStatement = (Statement)ASTNode.copySubtree(subclassAST, statement);
				List<Expression> oldVariableInstructions = expressionExtractor.getVariableInstructions(statement);
				List<Expression> newVariableInstructions = expressionExtractor.getVariableInstructions(newStatement);
				modifyVariableInstructionsInSubclass(oldVariableInstructions, newVariableInstructions, subclassAST, subclassRewriter, accessedFields, assignedFields, superAccessedFields, superAssignedFields);
				List<Expression> oldMethodInvocations = expressionExtractor.getMethodInvocations(statement);
				List<Expression> newMethodInvocations = expressionExtractor.getMethodInvocations(newStatement);
				modifyMethodInvocationsInSubclass(oldMethodInvocations, newMethodInvocations, subclassAST, subclassRewriter, accessedMethods, superAccessedMethods);
				replaceThisExpressionWithContextParameterInMethodInvocationArguments(newMethodInvocations, subclassAST, subclassRewriter);
				replaceThisExpressionWithContextParameterInClassInstanceCreationArguments(newStatement, subclassAST, subclassRewriter);
				if(ifStatementBodyRewrite != null)
					ifStatementBodyRewrite.insertLast(newStatement, null);
				else
					concreteMethodBodyRewrite.insertLast(newStatement, null);
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

	private void createIntermediateClassAndItsSubclasses(List<SimpleName> staticFields, List<String> subclassNames, ArrayList<Statement> typeCheckStatements,
			InheritanceTree tree, IContainer rootContainer, IContainer contextContainer) {
		String intermediateClassName = commonSubstring(subclassNames);
		Expression expression = typeCheckElimination.getExpressionCorrespondingToTypeCheckStatementList(typeCheckStatements);
		DefaultMutableTreeNode remainingIfStatementExpression = typeCheckElimination.getRemainingIfStatementExpression(expression);
		IFile intermediateClassFile = null;
		if(tree != null) {
			DefaultMutableTreeNode rootNode = tree.getRootNode();
			DefaultMutableTreeNode leaf = rootNode.getFirstLeaf();
			while(leaf != null) {
				String qualifiedSubclassName = (String)leaf.getUserObject();
				if((qualifiedSubclassName.contains(".") && qualifiedSubclassName.endsWith("." + intermediateClassName)) || qualifiedSubclassName.equals(intermediateClassName)) {
					intermediateClassFile = getFile(rootContainer, qualifiedSubclassName);
					break;
				}
				leaf = leaf.getNextLeaf();
			}
		}
		else {
			if(contextContainer instanceof IProject) {
				IProject contextProject = (IProject)contextContainer;
				intermediateClassFile = contextProject.getFile(intermediateClassName + ".java");
			}
			else if(contextContainer instanceof IFolder) {
				IFolder contextFolder = (IFolder)contextContainer;
				intermediateClassFile = contextFolder.getFile(intermediateClassName + ".java");
			}
		}
		boolean intermediateClassAlreadyExists = false;
		try {
			intermediateClassFile.create(new ByteArrayInputStream("".getBytes()), true, null);
			undoRefactoring.addNewlyCreatedFile(intermediateClassFile);
		} catch (CoreException e) {
			intermediateClassAlreadyExists = true;
		}
		IJavaElement intermediateClassJavaElement = JavaCore.create(intermediateClassFile);
		ITextEditor intermediateClassEditor = null;
		try {
			intermediateClassEditor = (ITextEditor)JavaUI.openInEditor(intermediateClassJavaElement);
		} catch (PartInitException e) {
			e.printStackTrace();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		ICompilationUnit intermediateClassICompilationUnit = (ICompilationUnit)intermediateClassJavaElement;
        ASTParser intermediateClassParser = ASTParser.newParser(AST.JLS3);
        intermediateClassParser.setKind(ASTParser.K_COMPILATION_UNIT);
        intermediateClassParser.setSource(intermediateClassICompilationUnit);
        intermediateClassParser.setResolveBindings(true); // we need bindings later on
        CompilationUnit intermediateClassCompilationUnit = (CompilationUnit)intermediateClassParser.createAST(null);
        
        AST intermediateClassAST = intermediateClassCompilationUnit.getAST();
        ASTRewrite intermediateClassRewriter = ASTRewrite.create(intermediateClassAST);
        ListRewrite intermediateClassTypesRewrite = intermediateClassRewriter.getListRewrite(intermediateClassCompilationUnit, CompilationUnit.TYPES_PROPERTY);
        
        TypeDeclaration intermediateClassTypeDeclaration = null;
		if(intermediateClassAlreadyExists) {
			List<AbstractTypeDeclaration> abstractTypeDeclarations = intermediateClassCompilationUnit.types();
			for(AbstractTypeDeclaration abstractTypeDeclaration : abstractTypeDeclarations) {
				if(abstractTypeDeclaration instanceof TypeDeclaration) {
					TypeDeclaration typeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
					if(typeDeclaration.getName().getIdentifier().equals(intermediateClassName)) {
						intermediateClassTypeDeclaration = typeDeclaration;
						requiredImportDeclarationsForContext.add(intermediateClassTypeDeclaration.resolveBinding());
						break;
					}
				}
			}
		}
		else {
			if(sourceCompilationUnit.getPackage() != null) {
				intermediateClassRewriter.set(intermediateClassCompilationUnit, CompilationUnit.PACKAGE_PROPERTY, sourceCompilationUnit.getPackage(), null);
			}
			intermediateClassTypeDeclaration = intermediateClassAST.newTypeDeclaration();
			intermediateClassRewriter.set(intermediateClassTypeDeclaration, TypeDeclaration.NAME_PROPERTY, intermediateClassAST.newSimpleName(intermediateClassName), null);
			intermediateClassRewriter.set(intermediateClassTypeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, intermediateClassAST.newSimpleType(intermediateClassAST.newSimpleName(typeCheckElimination.getAbstractClassName())), null);
			ListRewrite intermediateClassModifiersRewrite = intermediateClassRewriter.getListRewrite(intermediateClassTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
			intermediateClassModifiersRewrite.insertLast(intermediateClassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
			intermediateClassModifiersRewrite.insertLast(intermediateClassAST.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
		}
		
		ListRewrite intermediateClassBodyRewrite = intermediateClassRewriter.getListRewrite(intermediateClassTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		MethodDeclaration concreteMethodDeclaration = intermediateClassAST.newMethodDeclaration();
		intermediateClassRewriter.set(concreteMethodDeclaration, MethodDeclaration.NAME_PROPERTY, intermediateClassAST.newSimpleName(typeCheckElimination.getAbstractMethodName()), null);
		if(returnedVariable == null && !typeCheckElimination.typeCheckCodeFragmentContainsReturnStatement()) {
			intermediateClassRewriter.set(concreteMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, intermediateClassAST.newPrimitiveType(PrimitiveType.VOID), null);
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
				intermediateClassRewriter.set(concreteMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnType, null);
			}
			else {
				intermediateClassRewriter.set(concreteMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, typeCheckElimination.getTypeCheckMethodReturnType(), null);
			}
		}
		ListRewrite concreteMethodModifiersRewrite = intermediateClassRewriter.getListRewrite(concreteMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		concreteMethodModifiersRewrite.insertLast(intermediateClassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
		ListRewrite concreteMethodParametersRewrite = intermediateClassRewriter.getListRewrite(concreteMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		if(returnedVariable != null) {
			if(returnedVariable instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)returnedVariable;
				concreteMethodParametersRewrite.insertLast(singleVariableDeclaration, null);
			}
			else if(returnedVariable instanceof VariableDeclarationFragment) {
				SingleVariableDeclaration parameter = intermediateClassAST.newSingleVariableDeclaration();
				VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)returnedVariable;
				VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
				intermediateClassRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, variableDeclarationStatement.getType(), null);
				intermediateClassRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, variableDeclarationFragment.getName(), null);
				concreteMethodParametersRewrite.insertLast(parameter, null);
			}
		}
		for(SingleVariableDeclaration abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
			if(!abstractMethodParameter.equals(returnedVariable)) {
				concreteMethodParametersRewrite.insertLast(abstractMethodParameter, null);
			}
		}
		for(VariableDeclaration fragment : typeCheckElimination.getAccessedLocalVariables()) {
			if(!fragment.equals(returnedVariable)) {
				if(fragment instanceof SingleVariableDeclaration) {
					SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)fragment;
					concreteMethodParametersRewrite.insertLast(singleVariableDeclaration, null);
				}
				else if(fragment instanceof VariableDeclarationFragment) {
					SingleVariableDeclaration parameter = intermediateClassAST.newSingleVariableDeclaration();
					VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)fragment;
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
					intermediateClassRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, variableDeclarationStatement.getType(), null);
					intermediateClassRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, variableDeclarationFragment.getName(), null);
					concreteMethodParametersRewrite.insertLast(parameter, null);
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
			SingleVariableDeclaration parameter = intermediateClassAST.newSingleVariableDeclaration();
			SimpleName parameterType = intermediateClassAST.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
			intermediateClassRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, intermediateClassAST.newSimpleType(parameterType), null);
			String parameterName = sourceTypeDeclaration.getName().getIdentifier();
			parameterName = parameterName.substring(0,1).toLowerCase() + parameterName.substring(1,parameterName.length());
			intermediateClassRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, intermediateClassAST.newSimpleName(parameterName), null);
			concreteMethodParametersRewrite.insertLast(parameter, null);
		}
		
		ListRewrite concreteMethodThrownExceptionsRewrite = intermediateClassRewriter.getListRewrite(concreteMethodDeclaration, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
		for(ITypeBinding typeBinding : thrownExceptions) {
			concreteMethodThrownExceptionsRewrite.insertLast(intermediateClassAST.newSimpleName(typeBinding.getName()), null);
		}
		
		Block concreteMethodBody = intermediateClassAST.newBlock();
		ListRewrite concreteMethodBodyRewrite = intermediateClassRewriter.getListRewrite(concreteMethodBody, Block.STATEMENTS_PROPERTY);
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		ListRewrite ifStatementBodyRewrite = null;
		if(remainingIfStatementExpression != null) {
			IfStatement enclosingIfStatement = intermediateClassAST.newIfStatement();
			Expression enclosingIfStatementExpression = constructExpression(intermediateClassAST, remainingIfStatementExpression);
			Expression newEnclosingIfStatementExpression = (Expression)ASTNode.copySubtree(intermediateClassAST, enclosingIfStatementExpression);
			List<Expression> oldVariableInstructions = expressionExtractor.getVariableInstructions(enclosingIfStatementExpression);
			List<Expression> newVariableInstructions = expressionExtractor.getVariableInstructions(newEnclosingIfStatementExpression);
			modifyVariableInstructionsInSubclass(oldVariableInstructions, newVariableInstructions, intermediateClassAST, intermediateClassRewriter, accessedFields, assignedFields, superAccessedFields, superAssignedFields);
			List<Expression> oldMethodInvocations = expressionExtractor.getMethodInvocations(enclosingIfStatementExpression);
			List<Expression> newMethodInvocations = expressionExtractor.getMethodInvocations(newEnclosingIfStatementExpression);
			modifyMethodInvocationsInSubclass(oldMethodInvocations, newMethodInvocations, intermediateClassAST, intermediateClassRewriter, accessedMethods, superAccessedMethods);
			replaceThisExpressionWithContextParameterInMethodInvocationArguments(newMethodInvocations, intermediateClassAST, intermediateClassRewriter);
			intermediateClassRewriter.set(enclosingIfStatement, IfStatement.EXPRESSION_PROPERTY, newEnclosingIfStatementExpression, null);
			Block ifStatementBody = intermediateClassAST.newBlock();
			ifStatementBodyRewrite = intermediateClassRewriter.getListRewrite(ifStatementBody, Block.STATEMENTS_PROPERTY);
			intermediateClassRewriter.set(enclosingIfStatement, IfStatement.THEN_STATEMENT_PROPERTY, ifStatementBody, null);
			concreteMethodBodyRewrite.insertLast(enclosingIfStatement, null);
		}
		for(Statement statement : typeCheckStatements) {
			Statement newStatement = (Statement)ASTNode.copySubtree(intermediateClassAST, statement);
			List<Expression> oldVariableInstructions = expressionExtractor.getVariableInstructions(statement);
			List<Expression> newVariableInstructions = expressionExtractor.getVariableInstructions(newStatement);
			modifyVariableInstructionsInSubclass(oldVariableInstructions, newVariableInstructions, intermediateClassAST, intermediateClassRewriter, accessedFields, assignedFields, superAccessedFields, superAssignedFields);
			List<Expression> oldMethodInvocations = expressionExtractor.getMethodInvocations(statement);
			List<Expression> newMethodInvocations = expressionExtractor.getMethodInvocations(newStatement);
			modifyMethodInvocationsInSubclass(oldMethodInvocations, newMethodInvocations, intermediateClassAST, intermediateClassRewriter, accessedMethods, superAccessedMethods);
			replaceThisExpressionWithContextParameterInMethodInvocationArguments(newMethodInvocations, intermediateClassAST, intermediateClassRewriter);
			replaceThisExpressionWithContextParameterInClassInstanceCreationArguments(newStatement, intermediateClassAST, intermediateClassRewriter);
			if(ifStatementBodyRewrite != null)
				ifStatementBodyRewrite.insertLast(newStatement, null);
			else
				concreteMethodBodyRewrite.insertLast(newStatement, null);
		}
		if(returnedVariable != null) {
			ReturnStatement returnStatement = intermediateClassAST.newReturnStatement();
			intermediateClassRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, returnedVariable.getName(), null);
			concreteMethodBodyRewrite.insertLast(returnStatement, null);
		}
		intermediateClassRewriter.set(concreteMethodDeclaration, MethodDeclaration.BODY_PROPERTY, concreteMethodBody, null);
		
		intermediateClassBodyRewrite.insertLast(concreteMethodDeclaration, null);
		
		for(ITypeBinding typeBinding : requiredImportDeclarationsBasedOnSignature) {
			addImportDeclaration(typeBinding, intermediateClassCompilationUnit, intermediateClassRewriter);
		}
		Set<ITypeBinding> requiredImportDeclarationsBasedOnBranch = generateRequiredImportDeclarationsBasedOnBranch(typeCheckStatements);
		for(ITypeBinding typeBinding : requiredImportDeclarationsBasedOnBranch) {
			if(!requiredImportDeclarationsBasedOnSignature.contains(typeBinding))
				addImportDeclaration(typeBinding, intermediateClassCompilationUnit, intermediateClassRewriter);
		}
		
		if(!intermediateClassAlreadyExists)
			intermediateClassTypesRewrite.insertLast(intermediateClassTypeDeclaration, null);
		
		ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
		ITextFileBuffer intermediateClassTextFileBuffer = bufferManager.getTextFileBuffer(intermediateClassFile.getFullPath(), LocationKind.IFILE);
		IDocument intermediateClassDocument = intermediateClassTextFileBuffer.getDocument();
		TextEdit intermediateClassEdit = intermediateClassRewriter.rewriteAST(intermediateClassDocument, null);
		try {
			UndoEdit intermediateClassUndoEdit = intermediateClassEdit.apply(intermediateClassDocument, UndoEdit.CREATE_UNDO);
			undoRefactoring.put(intermediateClassFile, intermediateClassDocument, intermediateClassUndoEdit);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		intermediateClassEditor.doSave(null);
		
		for(int i=0; i<subclassNames.size(); i++) {
			IFile subclassFile = null;
			if(tree != null) {
				DefaultMutableTreeNode rootNode = tree.getRootNode();
				DefaultMutableTreeNode leaf = rootNode.getFirstLeaf();
				while(leaf != null) {
					String qualifiedSubclassName = (String)leaf.getUserObject();
					if((qualifiedSubclassName.contains(".") && qualifiedSubclassName.endsWith("." + subclassNames.get(i))) || qualifiedSubclassName.equals(subclassNames.get(i))) {
						subclassFile = getFile(rootContainer, qualifiedSubclassName);
						break;
					}
					leaf = leaf.getNextLeaf();
				}
			}
			else {
				if(contextContainer instanceof IProject) {
					IProject contextProject = (IProject)contextContainer;
					subclassFile = contextProject.getFile(subclassNames.get(i) + ".java");
				}
				else if(contextContainer instanceof IFolder) {
					IFolder contextFolder = (IFolder)contextContainer;
					subclassFile = contextFolder.getFile(subclassNames.get(i) + ".java");
				}
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
							requiredImportDeclarationsForContext.add(subclassTypeDeclaration.resolveBinding());
							break;
						}
					}
				}
			}
			else {
				if(sourceCompilationUnit.getPackage() != null) {
					subclassRewriter.set(subclassCompilationUnit, CompilationUnit.PACKAGE_PROPERTY, sourceCompilationUnit.getPackage(), null);
				}
				Javadoc subclassJavaDoc = subclassAST.newJavadoc();
				TagElement subclassTagElement = subclassAST.newTagElement();
				subclassRewriter.set(subclassTagElement, TagElement.TAG_NAME_PROPERTY, TagElement.TAG_SEE, null);
				
				MemberRef subclassMemberRef = subclassAST.newMemberRef();
				IBinding staticFieldNameBinding = staticFields.get(i).resolveBinding();
				ITypeBinding staticFieldNameDeclaringClass = null;
				if(staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
					IVariableBinding staticFieldNameVariableBinding = (IVariableBinding)staticFieldNameBinding;
					staticFieldNameDeclaringClass = staticFieldNameVariableBinding.getDeclaringClass();
				}
				subclassRewriter.set(subclassMemberRef, MemberRef.NAME_PROPERTY, subclassAST.newSimpleName(staticFieldNameBinding.getName()), null);
				subclassRewriter.set(subclassMemberRef, MemberRef.QUALIFIER_PROPERTY, subclassAST.newName(staticFieldNameDeclaringClass.getQualifiedName()), null);
				
				ListRewrite subclassTagElementFragmentsRewrite = subclassRewriter.getListRewrite(subclassTagElement, TagElement.FRAGMENTS_PROPERTY);
				subclassTagElementFragmentsRewrite.insertLast(subclassMemberRef, null);
				
				ListRewrite subclassJavaDocTagsRewrite = subclassRewriter.getListRewrite(subclassJavaDoc, Javadoc.TAGS_PROPERTY);
				subclassJavaDocTagsRewrite.insertLast(subclassTagElement, null);
				
				subclassTypeDeclaration = subclassAST.newTypeDeclaration();
				SimpleName subclassName = subclassAST.newSimpleName(subclassNames.get(i));
				subclassRewriter.set(subclassTypeDeclaration, TypeDeclaration.NAME_PROPERTY, subclassName, null);
				subclassRewriter.set(subclassTypeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, subclassAST.newSimpleType(subclassAST.newSimpleName(intermediateClassName)), null);
				ListRewrite subclassModifiersRewrite = subclassRewriter.getListRewrite(subclassTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
				subclassModifiersRewrite.insertLast(subclassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
				subclassRewriter.set(subclassTypeDeclaration, TypeDeclaration.JAVADOC_PROPERTY, subclassJavaDoc, null);
			}
			
			ListRewrite subclassBodyRewrite = subclassRewriter.getListRewrite(subclassTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			MethodDeclaration getterMethod = typeCheckElimination.getTypeFieldGetterMethod();
			if(typeCheckElimination.getTypeField() != null) {
				if(getterMethod != null) {
					MethodDeclaration concreteGetterMethodDeclaration = subclassAST.newMethodDeclaration();
					subclassRewriter.set(concreteGetterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, getterMethod.getName(), null);
					subclassRewriter.set(concreteGetterMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, getterMethod.getReturnType2(), null);
					ListRewrite concreteGetterMethodModifiersRewrite = subclassRewriter.getListRewrite(concreteGetterMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
					concreteGetterMethodModifiersRewrite.insertLast(subclassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
					Block concreteGetterMethodBody = subclassAST.newBlock();
					ListRewrite concreteGetterMethodBodyRewrite = subclassRewriter.getListRewrite(concreteGetterMethodBody, Block.STATEMENTS_PROPERTY);
					ReturnStatement returnStatement = subclassAST.newReturnStatement();
					IBinding staticFieldNameBinding = staticFields.get(i).resolveBinding();
					String staticFieldNameDeclaringClass = null;
					if(staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
						IVariableBinding staticFieldNameVariableBinding = (IVariableBinding)staticFieldNameBinding;
						staticFieldNameDeclaringClass = staticFieldNameVariableBinding.getDeclaringClass().getName();
					}
					FieldAccess fieldAccess = subclassAST.newFieldAccess();
					subclassRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, staticFields.get(i), null);
					subclassRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, subclassAST.newSimpleName(staticFieldNameDeclaringClass), null);
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
					IBinding staticFieldNameBinding = staticFields.get(i).resolveBinding();
					String staticFieldNameDeclaringClass = null;
					if(staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
						IVariableBinding staticFieldNameVariableBinding = (IVariableBinding)staticFieldNameBinding;
						staticFieldNameDeclaringClass = staticFieldNameVariableBinding.getDeclaringClass().getName();
					}
					FieldAccess fieldAccess = subclassAST.newFieldAccess();
					subclassRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, staticFields.get(i), null);
					subclassRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, subclassAST.newSimpleName(staticFieldNameDeclaringClass), null);
					subclassRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, fieldAccess, null);
					concreteGetterMethodBodyRewrite.insertLast(returnStatement, null);
					subclassRewriter.set(concreteGetterMethodDeclaration, MethodDeclaration.BODY_PROPERTY, concreteGetterMethodBody, null);
					subclassBodyRewrite.insertLast(concreteGetterMethodDeclaration, null);
				}
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

	private void modifyVariableInstructionsInSubclass(List<Expression> oldVariableInstructions, List<Expression> newVariableInstructions, AST subclassAST, ASTRewrite subclassRewriter,
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
				if(newRightHandSideName != null && newRightHandSideName.equals(newSimpleName)) {
					for(IVariableBinding accessedFieldBinding : accessedFieldBindings) {
						if(accessedFieldBinding.isEqualTo(oldRightHandSideName.resolveBinding())) {
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
							subclassRewriter.set(newAssignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, rightHandMethodInvocation, null);
							break;
						}
					}
					IBinding oldRightHandSideNameBinding = oldRightHandSideName.resolveBinding();
					if(oldRightHandSideNameBinding.getKind() == IBinding.VARIABLE) {
						IVariableBinding oldRightHandSideNameVariableBinding = (IVariableBinding)oldRightHandSideNameBinding;
						if((oldRightHandSideNameVariableBinding.getModifiers() & Modifier.STATIC) != 0 &&
								(oldRightHandSideNameVariableBinding.getModifiers() & Modifier.PUBLIC) != 0) {
							SimpleName qualifier = subclassAST.newSimpleName(oldRightHandSideNameVariableBinding.getDeclaringClass().getName());
							if(newRightHandSideName.getParent() instanceof FieldAccess) {
								FieldAccess fieldAccess = (FieldAccess)newRightHandSideName.getParent();
								subclassRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, qualifier, null);
							}
							else if(!(newRightHandSideName.getParent() instanceof QualifiedName)) {
								SimpleName simpleName = subclassAST.newSimpleName(newRightHandSideName.getIdentifier());
								QualifiedName newQualifiedName = subclassAST.newQualifiedName(qualifier, simpleName);
								subclassRewriter.replace(newRightHandSideName, newQualifiedName, null);
							}
						}
					}
				}
			}
			else {
				for(IVariableBinding accessedFieldBinding : accessedFieldBindings) {
					if(accessedFieldBinding.isEqualTo(oldSimpleName.resolveBinding())) {
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
						subclassRewriter.replace(newSimpleName, methodInvocation, null);
						break;
					}
				}
				IBinding oldSimpleNameBinding = oldSimpleName.resolveBinding();
				if(oldSimpleNameBinding.getKind() == IBinding.VARIABLE) {
					IVariableBinding oldSimpleNameVariableBinding = (IVariableBinding)oldSimpleNameBinding;
					if((oldSimpleNameVariableBinding.getModifiers() & Modifier.STATIC) != 0 &&
							(oldSimpleNameVariableBinding.getModifiers() & Modifier.PUBLIC) != 0) {
						SimpleName qualifier = subclassAST.newSimpleName(oldSimpleNameVariableBinding.getDeclaringClass().getName());
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

	private void modifyTypeFieldAssignmentsInContextClass() {
		AST contextAST = sourceTypeDeclaration.getAST();
		MethodDeclaration[] contextMethods = sourceTypeDeclaration.getMethods();
		List<SimpleName> staticFields = typeCheckElimination.getStaticFields();
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
													!containsVariable(staticFields, accessedVariable)) {
												if(!containsStaticFieldKey(accessedVariable))
													additionalStaticFields.put(accessedVariable, generateSubclassName(accessedVariable));
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
		List<SimpleName> staticFields = typeCheckElimination.getStaticFields();
		for(MethodDeclaration methodDeclaration : contextMethods) {
			Block methodBody = methodDeclaration.getBody();
			if(methodBody != null) {
				List<Statement> statements = methodBody.statements();
				ExpressionExtractor expressionExtractor = new ExpressionExtractor();
				for(Statement statement : statements) {
					if(statement instanceof SwitchStatement) {
						SwitchStatement switchStatement = (SwitchStatement)statement;
						Expression switchStatementExpression = switchStatement.getExpression();
						SimpleName accessedVariable = null;
						if(switchStatementExpression instanceof SimpleName) {
							accessedVariable = (SimpleName)switchStatementExpression;
						}
						else if(switchStatementExpression instanceof FieldAccess) {
							FieldAccess fieldAccess = (FieldAccess)switchStatementExpression;
							accessedVariable = fieldAccess.getName();
						}
						if(accessedVariable != null) {
							IBinding switchStatementExpressionBinding = accessedVariable.resolveBinding();
							if(switchStatementExpressionBinding.getKind() == IBinding.VARIABLE) {
								IVariableBinding accessedVariableBinding = (IVariableBinding)switchStatementExpressionBinding;
								if(accessedVariableBinding.isField() && typeCheckElimination.getTypeField().resolveBinding().isEqualTo(accessedVariable.resolveBinding())) {
									MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
									if(typeCheckElimination.getTypeFieldGetterMethod() != null) {
										sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckElimination.getTypeFieldGetterMethod().getName(), null);
									}
									else {
										sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + typeCheckElimination.getAbstractClassName()), null);
									}
									sourceRewriter.replace(switchStatementExpression, getterMethodInvocation, null);
									List<Statement> statements2 = switchStatement.statements();
									for(Statement statement2 : statements2) {
										if(statement2 instanceof SwitchCase) {
											SwitchCase switchCase = (SwitchCase)statement2;
											Expression switchCaseExpression = switchCase.getExpression();
											SimpleName comparedVariable = null;
											if(switchCaseExpression instanceof SimpleName) {
												comparedVariable = (SimpleName)switchCaseExpression;
											}
											else if(switchCaseExpression instanceof QualifiedName) {
												QualifiedName qualifiedName = (QualifiedName)switchCaseExpression;
												comparedVariable = qualifiedName.getName();
											}
											else if(switchCaseExpression instanceof FieldAccess) {
												FieldAccess fieldAccess = (FieldAccess)switchCaseExpression;
												comparedVariable = fieldAccess.getName();
											}
											if(comparedVariable != null) {
												IBinding switchCaseExpressionBinding = comparedVariable.resolveBinding();
												if(switchCaseExpressionBinding.getKind() == IBinding.VARIABLE) {
													IVariableBinding comparedVariableBinding = (IVariableBinding)switchCaseExpressionBinding;
													if(comparedVariableBinding.isField() && (comparedVariableBinding.getModifiers() & Modifier.STATIC) != 0 &&
															!containsVariable(staticFields, comparedVariable)) {
														if(!containsStaticFieldKey(comparedVariable))
															additionalStaticFields.put(comparedVariable, generateSubclassName(comparedVariable));
													}
												}
											}
										}
									}
								}
							}
						}
					}
					List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(statement);
					for(Expression expression : methodInvocations) {
						if(expression instanceof MethodInvocation) {
							MethodInvocation methodInvocation = (MethodInvocation)expression;
							List<Expression> arguments = methodInvocation.arguments();
							for(Expression argument : arguments) {
								SimpleName accessedVariable = null;
								if(argument instanceof SimpleName) {
									accessedVariable = (SimpleName)argument;
								}
								else if(argument instanceof FieldAccess) {
									FieldAccess fieldAccess = (FieldAccess)argument;
									accessedVariable = fieldAccess.getName();
								}
								if(accessedVariable != null) {
									IBinding argumentBinding = accessedVariable.resolveBinding();
									if(argumentBinding.getKind() == IBinding.VARIABLE) {
										IVariableBinding accessedVariableBinding = (IVariableBinding)argumentBinding;
										if(accessedVariableBinding.isField() && typeCheckElimination.getTypeField().resolveBinding().isEqualTo(accessedVariable.resolveBinding())) {
											MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
											if(typeCheckElimination.getTypeFieldGetterMethod() != null) {
												sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckElimination.getTypeFieldGetterMethod().getName(), null);
											}
											else {
												sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + typeCheckElimination.getAbstractClassName()), null);
											}
											ListRewrite argumentRewrite = sourceRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
											argumentRewrite.replace(argument, getterMethodInvocation, null);
										}
									}
								}
							}
						}
					}
					List<Expression> infixExpressions = expressionExtractor.getInfixExpressions(statement);
					for(Expression expression : infixExpressions) {
						InfixExpression infixExpression = (InfixExpression)expression;
						Expression leftOperand = infixExpression.getLeftOperand();
						Expression rightOperand = infixExpression.getRightOperand();
						SimpleName accessedVariable = null;
						SimpleName comparedVariable = null;
						boolean typeFieldIsReplaced = false;
						if(leftOperand instanceof SimpleName) {
							accessedVariable = (SimpleName)leftOperand;
						}
						else if(leftOperand instanceof FieldAccess) {
							FieldAccess fieldAccess = (FieldAccess)leftOperand;
							accessedVariable = fieldAccess.getName();
						}
						if(rightOperand instanceof SimpleName) {
							comparedVariable = (SimpleName)rightOperand;
						}
						else if(rightOperand instanceof QualifiedName) {
							QualifiedName qualifiedName = (QualifiedName)rightOperand;
							comparedVariable = qualifiedName.getName();
						}
						else if(rightOperand instanceof FieldAccess) {
							FieldAccess fieldAccess = (FieldAccess)rightOperand;
							comparedVariable = fieldAccess.getName();
						}
						if(accessedVariable != null) {
							IBinding leftOperandBinding = accessedVariable.resolveBinding();
							if(leftOperandBinding.getKind() == IBinding.VARIABLE) {
								IVariableBinding accessedVariableBinding = (IVariableBinding)leftOperandBinding;
								if(accessedVariableBinding.isField() && typeCheckElimination.getTypeField().resolveBinding().isEqualTo(accessedVariable.resolveBinding())) {
									MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
									if(typeCheckElimination.getTypeFieldGetterMethod() != null) {
										sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckElimination.getTypeFieldGetterMethod().getName(), null);
									}
									else {
										sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + typeCheckElimination.getAbstractClassName()), null);
									}
									sourceRewriter.replace(leftOperand, getterMethodInvocation, null);
									typeFieldIsReplaced = true;
									if(comparedVariable != null) {
										IBinding rightOperandBinding = comparedVariable.resolveBinding();
										if(rightOperandBinding.getKind() == IBinding.VARIABLE) {
											IVariableBinding comparedVariableBinding = (IVariableBinding)rightOperandBinding;
											if(comparedVariableBinding.isField() && (comparedVariableBinding.getModifiers() & Modifier.STATIC) != 0 &&
													!containsVariable(staticFields, comparedVariable)) {
												if(!containsStaticFieldKey(comparedVariable))
													additionalStaticFields.put(comparedVariable, generateSubclassName(comparedVariable));
											}
										}
									}
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
							if(leftOperand instanceof SimpleName) {
								comparedVariable = (SimpleName)leftOperand;
							}
							else if(leftOperand instanceof QualifiedName) {
								QualifiedName qualifiedName = (QualifiedName)leftOperand;
								comparedVariable = qualifiedName.getName();
							}
							else if(leftOperand instanceof FieldAccess) {
								FieldAccess fieldAccess = (FieldAccess)leftOperand;
								comparedVariable = fieldAccess.getName();
							}
							if(accessedVariable != null) {
								IBinding rightOperandBinding = accessedVariable.resolveBinding();
								if(rightOperandBinding.getKind() == IBinding.VARIABLE) {
									IVariableBinding accessedVariableBinding = (IVariableBinding)rightOperandBinding;
									if(accessedVariableBinding.isField() && typeCheckElimination.getTypeField().resolveBinding().isEqualTo(accessedVariable.resolveBinding())) {
										MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
										if(typeCheckElimination.getTypeFieldGetterMethod() != null) {
											sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckElimination.getTypeFieldGetterMethod().getName(), null);
										}
										else {
											sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + typeCheckElimination.getAbstractClassName()), null);
										}
										sourceRewriter.replace(rightOperand, getterMethodInvocation, null);
										if(comparedVariable != null) {
											IBinding leftOperandBinding = comparedVariable.resolveBinding();
											if(leftOperandBinding.getKind() == IBinding.VARIABLE) {
												IVariableBinding comparedVariableBinding = (IVariableBinding)leftOperandBinding;
												if(comparedVariableBinding.isField() && (comparedVariableBinding.getModifiers() & Modifier.STATIC) != 0 &&
														!containsVariable(staticFields, comparedVariable)) {
													if(!containsStaticFieldKey(comparedVariable))
														additionalStaticFields.put(comparedVariable, generateSubclassName(comparedVariable));
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
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
			Type parameterType = parameter.getType();
			ITypeBinding parameterTypeBinding = parameterType.resolveBinding();
			if(!typeBindings.contains(parameterTypeBinding))
				typeBindings.add(parameterTypeBinding);			
		}
		
		Set<VariableDeclaration> accessedLocalVariables = typeCheckElimination.getAccessedLocalVariables();
		for(VariableDeclaration fragment : accessedLocalVariables) {
			if(!fragment.equals(returnedVariable)) {
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
		List<SimpleName> staticFields = typeCheckElimination.getStaticFields();
		for(SimpleName simpleName : additionalStaticFields.keySet())
			staticFields.add(simpleName);
		for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
			List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				boolean modifierIsReplaced = false;
				for(SimpleName staticField : staticFields) {
					if(staticField.resolveBinding().isEqualTo(fragment.getName().resolveBinding())) {
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
									modifierFound = true;
									modifierRewrite.replace(modifier, publicModifier, null);
									modifierIsReplaced = true;
								}
							}
						}
						if(!modifierFound) {
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

	private void identifyTypeLocalVariableAssignmentsInTypeCheckMethod() {
		List<SimpleName> staticFields = typeCheckElimination.getStaticFields();
		Block methodBody = typeCheckElimination.getTypeCheckMethod().getBody();
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
							if(typeCheckElimination.getTypeLocalVariable().resolveBinding().isEqualTo(assignedVariableBinding)) {
								if(accessedVariable != null) {
									IBinding rightHandBinding = accessedVariable.resolveBinding();
									if(rightHandBinding.getKind() == IBinding.VARIABLE) {
										IVariableBinding accessedVariableBinding = (IVariableBinding)rightHandBinding;
										if(accessedVariableBinding.isField() && (accessedVariableBinding.getModifiers() & Modifier.STATIC) != 0 &&
												!containsVariable(staticFields, accessedVariable)) {
											if(!containsStaticFieldKey(accessedVariable))
												additionalStaticFields.put(accessedVariable, generateSubclassName(accessedVariable));
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void identifyTypeLocalVariableAccessesInTypeCheckMethod() {
		List<SimpleName> staticFields = typeCheckElimination.getStaticFields();
		Block methodBody = typeCheckElimination.getTypeCheckMethod().getBody();
		if(methodBody != null) {
			List<Statement> statements = methodBody.statements();
			ExpressionExtractor expressionExtractor = new ExpressionExtractor();
			for(Statement statement : statements) {
				if(statement instanceof SwitchStatement) {
					SwitchStatement switchStatement = (SwitchStatement)statement;
					Expression switchStatementExpression = switchStatement.getExpression();
					SimpleName accessedVariable = null;
					if(switchStatementExpression instanceof SimpleName) {
						accessedVariable = (SimpleName)switchStatementExpression;
					}
					else if(switchStatementExpression instanceof FieldAccess) {
						FieldAccess fieldAccess = (FieldAccess)switchStatementExpression;
						accessedVariable = fieldAccess.getName();
					}
					if(accessedVariable != null) {
						if(typeCheckElimination.getTypeLocalVariable().resolveBinding().isEqualTo(accessedVariable.resolveBinding())) {
							List<Statement> statements2 = switchStatement.statements();
							for(Statement statement2 : statements2) {
								if(statement2 instanceof SwitchCase) {
									SwitchCase switchCase = (SwitchCase)statement2;
									Expression switchCaseExpression = switchCase.getExpression();
									SimpleName comparedVariable = null;
									if(switchCaseExpression instanceof SimpleName) {
										comparedVariable = (SimpleName)switchCaseExpression;
									}
									else if(switchCaseExpression instanceof QualifiedName) {
										QualifiedName qualifiedName = (QualifiedName)switchCaseExpression;
										comparedVariable = qualifiedName.getName();
									}
									else if(switchCaseExpression instanceof FieldAccess) {
										FieldAccess fieldAccess = (FieldAccess)switchCaseExpression;
										comparedVariable = fieldAccess.getName();
									}
									if(comparedVariable != null) {
										IBinding switchCaseExpressionBinding = comparedVariable.resolveBinding();
										if(switchCaseExpressionBinding.getKind() == IBinding.VARIABLE) {
											IVariableBinding comparedVariableBinding = (IVariableBinding)switchCaseExpressionBinding;
											if(comparedVariableBinding.isField() && (comparedVariableBinding.getModifiers() & Modifier.STATIC) != 0 &&
													!containsVariable(staticFields, comparedVariable)) {
												if(!containsStaticFieldKey(comparedVariable))
													additionalStaticFields.put(comparedVariable, generateSubclassName(comparedVariable));
											}
										}
									}
								}
							}
						}
					}
				}
				List<Expression> infixExpressions = expressionExtractor.getInfixExpressions(statement);
				for(Expression expression : infixExpressions) {
					InfixExpression infixExpression = (InfixExpression)expression;
					Expression leftOperand = infixExpression.getLeftOperand();
					Expression rightOperand = infixExpression.getRightOperand();
					SimpleName accessedVariable = null;
					SimpleName comparedVariable = null;
					boolean typeLocalVariableIsFound = false;
					if(leftOperand instanceof SimpleName) {
						accessedVariable = (SimpleName)leftOperand;
					}
					else if(leftOperand instanceof FieldAccess) {
						FieldAccess fieldAccess = (FieldAccess)leftOperand;
						accessedVariable = fieldAccess.getName();
					}
					if(rightOperand instanceof SimpleName) {
						comparedVariable = (SimpleName)rightOperand;
					}
					else if(rightOperand instanceof QualifiedName) {
						QualifiedName qualifiedName = (QualifiedName)rightOperand;
						comparedVariable = qualifiedName.getName();
					}
					else if(rightOperand instanceof FieldAccess) {
						FieldAccess fieldAccess = (FieldAccess)rightOperand;
						comparedVariable = fieldAccess.getName();
					}
					if(accessedVariable != null) {
						if(typeCheckElimination.getTypeLocalVariable().resolveBinding().isEqualTo(accessedVariable.resolveBinding())) {
							typeLocalVariableIsFound = true;
							if(comparedVariable != null) {
								IBinding rightOperandBinding = comparedVariable.resolveBinding();
								if(rightOperandBinding.getKind() == IBinding.VARIABLE) {
									IVariableBinding comparedVariableBinding = (IVariableBinding)rightOperandBinding;
									if(comparedVariableBinding.isField() && (comparedVariableBinding.getModifiers() & Modifier.STATIC) != 0 &&
											!containsVariable(staticFields, comparedVariable)) {
										if(!containsStaticFieldKey(comparedVariable))
											additionalStaticFields.put(comparedVariable, generateSubclassName(comparedVariable));
									}
								}
							}
						}
					}
					if(!typeLocalVariableIsFound) {
						if(rightOperand instanceof SimpleName) {
							accessedVariable = (SimpleName)rightOperand;
						}
						else if(rightOperand instanceof FieldAccess) {
							FieldAccess fieldAccess = (FieldAccess)rightOperand;
							accessedVariable = fieldAccess.getName();
						}
						if(leftOperand instanceof SimpleName) {
							comparedVariable = (SimpleName)leftOperand;
						}
						else if(leftOperand instanceof QualifiedName) {
							QualifiedName qualifiedName = (QualifiedName)leftOperand;
							comparedVariable = qualifiedName.getName();
						}
						else if(leftOperand instanceof FieldAccess) {
							FieldAccess fieldAccess = (FieldAccess)leftOperand;
							comparedVariable = fieldAccess.getName();
						}
						if(accessedVariable != null) {
							if(typeCheckElimination.getTypeLocalVariable().resolveBinding().isEqualTo(accessedVariable.resolveBinding())) {
								if(comparedVariable != null) {
									IBinding leftOperandBinding = comparedVariable.resolveBinding();
									if(leftOperandBinding.getKind() == IBinding.VARIABLE) {
										IVariableBinding comparedVariableBinding = (IVariableBinding)leftOperandBinding;
										if(comparedVariableBinding.isField() && (comparedVariableBinding.getModifiers() & Modifier.STATIC) != 0 &&
												!containsVariable(staticFields, comparedVariable)) {
											if(!containsStaticFieldKey(comparedVariable))
												additionalStaticFields.put(comparedVariable, generateSubclassName(comparedVariable));
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean containsStaticFieldKey(SimpleName simpleName) {
		for(SimpleName keySimpleName : additionalStaticFields.keySet()) {
			if(keySimpleName.resolveBinding().isEqualTo(simpleName.resolveBinding()))
				return true;
		}
		return false;
	}

	private boolean containsVariable(List<SimpleName> staticFields, SimpleName variable) {
		for(SimpleName staticField : staticFields) {
			if(staticField.resolveBinding().isEqualTo(variable.resolveBinding()))
				return true;
		}
		return false;
	}

	private String generateSubclassName(SimpleName variable) {
		String subclassName = "";
		StringTokenizer tokenizer = new StringTokenizer(variable.getIdentifier(),"_");
		while(tokenizer.hasMoreTokens()) {
			String tempName = tokenizer.nextToken().toLowerCase().toString();
			subclassName += tempName.subSequence(0, 1).toString().toUpperCase() + 
			tempName.subSequence(1, tempName.length()).toString();
		}
		return subclassName;
	}

	private String commonSubstring(List<String> subclassNames) {
		Map<String, Integer> rankMap = new LinkedHashMap<String, Integer>();
		for(int i=0; i<subclassNames.size(); i++) {
			for(int j=i+1; j<subclassNames.size(); j++) {
				List<String> commonSubstrings = commonSubstrings(subclassNames.get(i), subclassNames.get(j));
				for(String s : commonSubstrings) {
					if(rankMap.containsKey(s)) {
						rankMap.put(s, rankMap.get(s)+1);
					}
					else {
						rankMap.put(s, 1);
					}
				}
			}
		}
		int maxRank = 0;
		String mostCommonSubstring = null;
		for(String s : rankMap.keySet()) {
			int rank = rankMap.get(s);
			if(rank > maxRank) {
				maxRank = rank;
				mostCommonSubstring = s;
			}
		}
		return mostCommonSubstring;
	}

	private List<String> commonSubstrings(String s1, String s2) {
		List<String> commonSubstrings = new ArrayList<String>();
		int m = s1.length();
		int n = s2.length();
		int[][] num = new int[m][n];
		int maxlen = 0;
		int lastSubsBegin = 0;
		StringBuilder sequence = new StringBuilder();
		for(int i=0; i<m; i++) {
			for(int j=0; j<n; j++) {
				if(s1.charAt(i) != s2.charAt(j))
					num[i][j] = 0;
				else {
					if ((i == 0) || (j == 0))
						num[i][j] = 1;
					else
						num[i][j] = 1 + num[i-1][j-1];
					if(num[i][j] > maxlen) {
						maxlen = num[i][j];
						int thisSubsBegin = i - num[i][j] + 1;
						if (lastSubsBegin == thisSubsBegin)
							sequence.append(s1.charAt(i));
						else {
							lastSubsBegin = thisSubsBegin;
							commonSubstrings.add(sequence.toString());
							sequence = new StringBuilder();
							sequence.append(s1.substring(lastSubsBegin, i+1));
						}
					}
				}
			}
		}
		commonSubstrings.add(sequence.toString());
		return commonSubstrings;
	}

	public UndoRefactoring getUndoRefactoring() {
		return undoRefactoring;
	}

	public TypeCheckElimination getTypeCheckElimination() {
		return typeCheckElimination;
	}
}
