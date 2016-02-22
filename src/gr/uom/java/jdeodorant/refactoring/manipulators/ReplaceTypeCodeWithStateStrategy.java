package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.inheritance.InheritanceTree;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.TypeVisitor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
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
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NumberLiteral;
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
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreateCompilationUnitChange;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ReplaceTypeCodeWithStateStrategy extends PolymorphismRefactoring {
	private VariableDeclaration returnedVariable;
	private Set<ITypeBinding> requiredImportDeclarationsBasedOnSignature;
	private Set<ITypeBinding> requiredImportDeclarationsForContext;
	private Set<ITypeBinding> thrownExceptions;
	private Map<SimpleName, String> staticFieldMap;
	private Map<SimpleName, String> additionalStaticFieldMap;
	private String abstractClassName;
	private Map<ICompilationUnit, CreateCompilationUnitChange> createCompilationUnitChanges;
	
	public ReplaceTypeCodeWithStateStrategy(IFile sourceFile, CompilationUnit sourceCompilationUnit,
			TypeDeclaration sourceTypeDeclaration, TypeCheckElimination typeCheckElimination) {
		super(sourceFile, sourceCompilationUnit, sourceTypeDeclaration, typeCheckElimination);
		this.returnedVariable = typeCheckElimination.getTypeCheckMethodReturnedVariable();
		this.requiredImportDeclarationsBasedOnSignature = new LinkedHashSet<ITypeBinding>();
		this.requiredImportDeclarationsForContext = new LinkedHashSet<ITypeBinding>();
		this.thrownExceptions = typeCheckElimination.getThrownExceptions();
		this.staticFieldMap = new LinkedHashMap<SimpleName, String>();
		for(SimpleName simpleName : typeCheckElimination.getStaticFields()) {
			this.staticFieldMap.put(simpleName, generateSubclassName(simpleName));
		}
		this.additionalStaticFieldMap = new LinkedHashMap<SimpleName, String>();
		for(SimpleName simpleName : typeCheckElimination.getAdditionalStaticFields()) {
			this.additionalStaticFieldMap.put(simpleName, generateSubclassName(simpleName));
		}
		this.abstractClassName = typeCheckElimination.getAbstractClassName();
		this.createCompilationUnitChanges = new LinkedHashMap<ICompilationUnit, CreateCompilationUnitChange>();
	}

	public void apply() {
		if(typeCheckElimination.getTypeField() != null) {
			modifyTypeFieldAssignmentsInContextClass(true);
			modifyTypeFieldAccessesInContextClass(true);
		}
		createStateStrategyHierarchy();
		if(typeCheckElimination.getTypeField() != null)
			modifyContext();
		else if(typeCheckElimination.getTypeLocalVariable() != null || typeCheckElimination.getTypeMethodInvocation() != null)
			modifyTypeCheckMethod();
	}

	private void modifyContext() {
		boolean typeFieldInSingleFragment = false;
		VariableDeclarationFragment fragment = typeCheckElimination.getTypeField();
		if(fragment != null) {
			FieldDeclaration fieldDeclaration = (FieldDeclaration)fragment.getParent();
			if(fieldDeclaration.fragments().size() == 1) {
				typeFieldInSingleFragment = true;
			}
		}
		if(typeFieldInSingleFragment) {
			replacePrimitiveStateField();
		}
		else {
			createStateField();
			removePrimitiveStateField();
		}
		generateSetterMethodForStateField();
		generateGetterMethodForStateField();
		replaceConditionalStructureWithPolymorphicMethodInvocation();
		
		generateGettersForAccessedFields();
		generateSettersForAssignedFields();
		setPublicModifierToStaticFields();
		setPublicModifierToAccessedMethods();
		
		addRequiredImportDeclarationsToContext();
	}

	private void createStateField() {
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		AST contextAST = sourceTypeDeclaration.getAST();
		ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		VariableDeclarationFragment typeFragment = createStateFieldVariableDeclarationFragment(sourceRewriter, contextAST);
		
		FieldDeclaration typeFieldDeclaration = contextAST.newFieldDeclaration(typeFragment);
		sourceRewriter.set(typeFieldDeclaration, FieldDeclaration.TYPE_PROPERTY, contextAST.newSimpleName(abstractClassName), null);
		ListRewrite typeFieldDeclarationModifiersRewrite = sourceRewriter.getListRewrite(typeFieldDeclaration, FieldDeclaration.MODIFIERS2_PROPERTY);
		typeFieldDeclarationModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD), null);
		contextBodyRewrite.insertBefore(typeFieldDeclaration, typeCheckElimination.getTypeField().getParent(), null);
		
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
			change.getEdit().addChild(sourceEdit);
			change.addTextEditGroup(new TextEditGroup("Create field holding the current state", new TextEdit[] {sourceEdit}));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private VariableDeclarationFragment createStateFieldVariableDeclarationFragment(
			ASTRewrite sourceRewriter, AST contextAST) {
		VariableDeclarationFragment typeFragment = contextAST.newVariableDeclarationFragment();
		sourceRewriter.set(typeFragment, VariableDeclarationFragment.NAME_PROPERTY, typeCheckElimination.getTypeField().getName(), null);
		Expression typeFieldInitializer = typeCheckElimination.getTypeField().getInitializer();
		Set<SimpleName> allStaticFieldNames = new LinkedHashSet<SimpleName>(typeCheckElimination.getStaticFields());
		allStaticFieldNames.addAll(additionalStaticFieldMap.keySet());
		if(typeFieldInitializer != null) {
			SimpleName typeFieldInitializerSimpleName = null;
			if(typeFieldInitializer instanceof SimpleName) {
				typeFieldInitializerSimpleName = (SimpleName)typeFieldInitializer;
			}
			else if(typeFieldInitializer instanceof QualifiedName) {
				QualifiedName typeFieldInitializerQualifiedName = (QualifiedName)typeFieldInitializer;
				typeFieldInitializerSimpleName = typeFieldInitializerQualifiedName.getName();
			}
			else if(typeFieldInitializer instanceof FieldAccess) {
				FieldAccess typeFieldInitializerFieldAccess = (FieldAccess)typeFieldInitializer;
				typeFieldInitializerSimpleName = typeFieldInitializerFieldAccess.getName();
			}
			else if(typeFieldInitializer instanceof NumberLiteral) {
				NumberLiteral typeFieldInitializerNumberLiteral = (NumberLiteral)typeFieldInitializer;
				for(SimpleName staticFieldName : allStaticFieldNames) {
					Object constantValue = ((IVariableBinding)staticFieldName.resolveBinding()).getConstantValue();
					if(constantValue != null && constantValue instanceof Integer) {
						Integer constantIntegerValue = (Integer)constantValue;
						if(constantIntegerValue.toString().equals(typeFieldInitializerNumberLiteral.getToken())) {
							ClassInstanceCreation classInstanceCreation = contextAST.newClassInstanceCreation();
							String subclassName = getTypeNameForNamedConstant(staticFieldName);
							sourceRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY, contextAST.newSimpleName(subclassName), null);
							sourceRewriter.set(typeFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, classInstanceCreation, null);
							break;
						}
					}
				}
			}
			if(typeFieldInitializerSimpleName != null) {
				for(SimpleName staticFieldName : allStaticFieldNames) {
					if(staticFieldName.resolveBinding().isEqualTo(typeFieldInitializerSimpleName.resolveBinding())) {
						ClassInstanceCreation classInstanceCreation = contextAST.newClassInstanceCreation();
						String subclassName = getTypeNameForNamedConstant(staticFieldName);
						sourceRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY, contextAST.newSimpleName(subclassName), null);
						sourceRewriter.set(typeFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, classInstanceCreation, null);
						break;
					}
				}
			}
		}
		else {
			for(SimpleName staticFieldName : allStaticFieldNames) {
				Object constantValue = ((IVariableBinding)staticFieldName.resolveBinding()).getConstantValue();
				if(constantValue != null && constantValue instanceof Integer) {
					Integer constantIntegerValue = (Integer)constantValue;
					if(constantIntegerValue.toString().equals("0")) {
						ClassInstanceCreation classInstanceCreation = contextAST.newClassInstanceCreation();
						String subclassName = getTypeNameForNamedConstant(staticFieldName);
						sourceRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY, contextAST.newSimpleName(subclassName), null);
						sourceRewriter.set(typeFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, classInstanceCreation, null);
						break;
					}
				}
			}
		}
		return typeFragment;
	}

	private void replacePrimitiveStateField() {
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		AST contextAST = sourceTypeDeclaration.getAST();
		ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		FieldDeclaration[] fieldDeclarations = sourceTypeDeclaration.getFields();
		for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
			List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				if(fragment.equals(typeCheckElimination.getTypeField())) {
					if(fragments.size() == 1) {
						ListRewrite fragmentsRewriter = sourceRewriter.getListRewrite(fieldDeclaration, FieldDeclaration.FRAGMENTS_PROPERTY);
						fragmentsRewriter.remove(fragment, null);
						VariableDeclarationFragment typeFragment = createStateFieldVariableDeclarationFragment(sourceRewriter, contextAST);
						fragmentsRewriter.insertLast(typeFragment, null);
						sourceRewriter.set(fieldDeclaration, FieldDeclaration.TYPE_PROPERTY, contextAST.newSimpleName(abstractClassName), null);
					}
				}
			}
		}
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
			change.getEdit().addChild(sourceEdit);
			change.addTextEditGroup(new TextEditGroup("Replace primitive type with State type", new TextEdit[] {sourceEdit}));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void removePrimitiveStateField() {
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		AST contextAST = sourceTypeDeclaration.getAST();
		ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
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
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
			change.getEdit().addChild(sourceEdit);
			change.addTextEditGroup(new TextEditGroup("Remove primitive field holding the current state", new TextEdit[] {sourceEdit}));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void generateSetterMethodForStateField() {
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		AST contextAST = sourceTypeDeclaration.getAST();
		ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		MethodDeclaration setterMethod = typeCheckElimination.getTypeFieldSetterMethod();
		SwitchStatement switchStatement = contextAST.newSwitchStatement();
		List<SimpleName> staticFieldNames = new ArrayList<SimpleName>(staticFieldMap.keySet());
		List<String> subclassNames = new ArrayList<String>(staticFieldMap.values());
		ListRewrite switchStatementStatementsRewrite = sourceRewriter.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);
		int i = 0;
		for(SimpleName staticFieldName : staticFieldNames) {
			SwitchCase switchCase = contextAST.newSwitchCase();
			IBinding staticFieldNameBinding = staticFieldName.resolveBinding();
			String staticFieldNameDeclaringClass = null;
			boolean isEnumConstant = false;
			if(staticFieldNameBinding != null && staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
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
		for(SimpleName staticFieldName : additionalStaticFieldMap.keySet()) {
			SwitchCase switchCase = contextAST.newSwitchCase();
			IBinding staticFieldNameBinding = staticFieldName.resolveBinding();
			String staticFieldNameDeclaringClass = null;
			boolean isEnumConstant = false;
			if(staticFieldNameBinding != null && staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
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
			sourceRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY, contextAST.newSimpleName(additionalStaticFieldMap.get(staticFieldName)), null);
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
			try {
				TextEdit sourceEdit = sourceRewriter.rewriteAST();
				ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
				CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
				change.getEdit().addChild(sourceEdit);
				change.addTextEditGroup(new TextEditGroup("Modify setter method for the field holding the current state", new TextEdit[] {sourceEdit}));
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		else {
			MethodDeclaration setterMethodDeclaration = contextAST.newMethodDeclaration();
			sourceRewriter.set(setterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, contextAST.newSimpleName(setterMethodName()), null);
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
			try {
				TextEdit sourceEdit = sourceRewriter.rewriteAST();
				ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
				CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
				change.getEdit().addChild(sourceEdit);
				change.addTextEditGroup(new TextEditGroup("Create setter method for the field holding the current state", new TextEdit[] {sourceEdit}));
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
	}

	private String setterMethodName() {
		String defaultName = "set" + abstractClassName;
		for(MethodDeclaration method : sourceTypeDeclaration.getMethods()) {
			if(method.getName().getIdentifier().equals(defaultName))
				return defaultName + "2";
		}
		return defaultName;
	}

	private void generateGetterMethodForStateField() {
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		AST contextAST = sourceTypeDeclaration.getAST();
		ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
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
			try {
				TextEdit sourceEdit = sourceRewriter.rewriteAST();
				ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
				CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
				change.getEdit().addChild(sourceEdit);
				change.addTextEditGroup(new TextEditGroup("Modify getter method for the field holding the current state", new TextEdit[] {sourceEdit}));
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		else {
			MethodDeclaration getterMethodDeclaration = contextAST.newMethodDeclaration();
			sourceRewriter.set(getterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, contextAST.newSimpleName("get" + abstractClassName), null);
			VariableDeclarationFragment typeField = typeCheckElimination.getTypeField();
			Type returnType = ((FieldDeclaration)typeField.getParent()).getType();
			sourceRewriter.set(getterMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnType, null);
			ListRewrite getterMethodModifiersRewrite = sourceRewriter.getListRewrite(getterMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
			getterMethodModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
			
			ReturnStatement returnStatement = contextAST.newReturnStatement();
			MethodInvocation abstractGetterMethodInvocation = contextAST.newMethodInvocation();
			sourceRewriter.set(abstractGetterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + abstractClassName), null);
			sourceRewriter.set(abstractGetterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, typeCheckElimination.getTypeField().getName(), null);
			sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, abstractGetterMethodInvocation, null);
			Block getterMethodBody = contextAST.newBlock();
			ListRewrite getterMethodBodyRewrite = sourceRewriter.getListRewrite(getterMethodBody, Block.STATEMENTS_PROPERTY);
			getterMethodBodyRewrite.insertLast(returnStatement, null);
			sourceRewriter.set(getterMethodDeclaration, MethodDeclaration.BODY_PROPERTY, getterMethodBody, null);
			contextBodyRewrite.insertLast(getterMethodDeclaration, null);
			try {
				TextEdit sourceEdit = sourceRewriter.rewriteAST();
				ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
				CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
				change.getEdit().addChild(sourceEdit);
				change.addTextEditGroup(new TextEditGroup("Create getter method for the field holding the current state", new TextEdit[] {sourceEdit}));
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
	}

	private void replaceConditionalStructureWithPolymorphicMethodInvocation() {
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		AST contextAST = sourceTypeDeclaration.getAST();
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
			if(sourceTypeRequiredForExtraction()) {
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
					initializeReturnedVariableDeclaration();
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
			if(sourceTypeRequiredForExtraction()) {
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
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
			change.getEdit().addChild(sourceEdit);
			change.addTextEditGroup(new TextEditGroup("Replace conditional structure with polymorphic method invocation", new TextEdit[] {sourceEdit}));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void initializeReturnedVariableDeclaration() {
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		AST contextAST = sourceTypeDeclaration.getAST();
		if(returnedVariable != null) {
			IVariableBinding returnedVariableBinding = returnedVariable.resolveBinding();
			if(returnedVariable instanceof VariableDeclarationFragment && !returnedVariableBinding.isField()) {
				VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)returnedVariable;
				if(variableDeclarationFragment.getInitializer() == null) {
					Expression defaultValue = generateDefaultValue(sourceRewriter, contextAST, returnedVariableBinding.getType());
					sourceRewriter.set(variableDeclarationFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, defaultValue, null);
				}
			}
		}
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
			change.getEdit().addChild(sourceEdit);
			change.addTextEditGroup(new TextEditGroup("Initialize returned variable", new TextEdit[] {sourceEdit}));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void addRequiredImportDeclarationsToContext() {
		ImportRewrite sourceImportRewrite = ImportRewrite.create(sourceCompilationUnit, true);
		for(ITypeBinding typeBinding : requiredImportDeclarationsForContext) {
			if(!typeBinding.isNested())
				sourceImportRewrite.addImport(typeBinding);
		}

		try {
			TextEdit sourceImportEdit = sourceImportRewrite.rewriteImports(null);
			if(sourceImportRewrite.getCreatedImports().length > 0) {
				ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
				CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
				change.getEdit().addChild(sourceImportEdit);
				change.addTextEditGroup(new TextEditGroup("Add required import declarations", new TextEdit[] {sourceImportEdit}));
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private void modifyTypeCheckMethod() {
		createGetterMethodForStateObject();
		replaceConditionalStructureWithPolymorphicMethodInvocationThroughStateObject();
		
		generateGettersForAccessedFields();
		generateSettersForAssignedFields();
		setPublicModifierToStaticFields();
		setPublicModifierToAccessedMethods();
		
		addRequiredImportDeclarationsToContext();
	}

	private void createGetterMethodForStateObject() {
		if(!typeObjectGetterMethodAlreadyExists()) {
			ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
			AST contextAST = sourceTypeDeclaration.getAST();
			ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			SwitchStatement switchStatement = contextAST.newSwitchStatement();
			List<SimpleName> staticFieldNames = new ArrayList<SimpleName>(staticFieldMap.keySet());
			List<String> subclassNames = new ArrayList<String>(staticFieldMap.values());
			ListRewrite switchStatementStatementsRewrite = sourceRewriter.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);
			int i = 0;
			for(SimpleName staticFieldName : staticFieldNames) {
				SwitchCase switchCase = contextAST.newSwitchCase();
				IBinding staticFieldNameBinding = staticFieldName.resolveBinding();
				String staticFieldNameDeclaringClass = null;
				boolean isEnumConstant = false;
				if(staticFieldNameBinding != null && staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
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
			for(SimpleName staticFieldName : additionalStaticFieldMap.keySet()) {
				SwitchCase switchCase = contextAST.newSwitchCase();
				IBinding staticFieldNameBinding = staticFieldName.resolveBinding();
				String staticFieldNameDeclaringClass = null;
				boolean isEnumConstant = false;
				if(staticFieldNameBinding != null && staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
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
				sourceRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY, contextAST.newSimpleName(additionalStaticFieldMap.get(staticFieldName)), null);
				sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, classInstanceCreation, null);
				switchStatementStatementsRewrite.insertLast(returnStatement, null);
			}
			
			MethodDeclaration setterMethodDeclaration = contextAST.newMethodDeclaration();
			sourceRewriter.set(setterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, contextAST.newSimpleName("get" + abstractClassName + "Object"), null);
			sourceRewriter.set(setterMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, contextAST.newSimpleType(contextAST.newSimpleName(abstractClassName)), null);
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
					if(variableDeclarationFragment.getParent() instanceof VariableDeclarationStatement) {
						VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
						parameterType = variableDeclarationStatement.getType();
					}
					else if(variableDeclarationFragment.getParent() instanceof VariableDeclarationExpression) {
						VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)variableDeclarationFragment.getParent();
						parameterType = variableDeclarationExpression.getType();
					}
					else if(variableDeclarationFragment.getParent() instanceof FieldDeclaration) {
						FieldDeclaration fieldDeclaration = (FieldDeclaration)variableDeclarationFragment.getParent();
						parameterType = fieldDeclaration.getType();
					}
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
			
			try {
				TextEdit sourceEdit = sourceRewriter.rewriteAST();
				ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
				CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
				change.getEdit().addChild(sourceEdit);
				change.addTextEditGroup(new TextEditGroup("Create getter method for state object", new TextEdit[] {sourceEdit}));
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
	}

	private void replaceConditionalStructureWithPolymorphicMethodInvocationThroughStateObject() {
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		AST contextAST = sourceTypeDeclaration.getAST();
		Block typeCheckCodeFragmentParentBlock = (Block)typeCheckElimination.getTypeCheckCodeFragment().getParent();
		ListRewrite typeCheckCodeFragmentParentBlockStatementsRewrite = sourceRewriter.getListRewrite(typeCheckCodeFragmentParentBlock, Block.STATEMENTS_PROPERTY);
		if(returnedVariable == null && !typeCheckElimination.typeCheckCodeFragmentContainsReturnStatement()) {
			MethodInvocation abstractMethodInvocation = contextAST.newMethodInvocation();
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName(typeCheckElimination.getAbstractMethodName()), null);
			MethodInvocation invokerMethodInvocation = contextAST.newMethodInvocation();
			sourceRewriter.set(invokerMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + abstractClassName + "Object"), null);
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
			if(sourceTypeRequiredForExtraction()) {
				abstractMethodInvocationArgumentsRewrite.insertLast(contextAST.newThisExpression(), null);
			}
			ExpressionStatement expressionStatement = contextAST.newExpressionStatement(abstractMethodInvocation);
			typeCheckCodeFragmentParentBlockStatementsRewrite.replace(typeCheckElimination.getTypeCheckCodeFragment(), expressionStatement, null);
		}
		else {
			MethodInvocation abstractMethodInvocation = contextAST.newMethodInvocation();
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName(typeCheckElimination.getAbstractMethodName()), null);
			MethodInvocation invokerMethodInvocation = contextAST.newMethodInvocation();
			sourceRewriter.set(invokerMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + abstractClassName + "Object"), null);
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
					initializeReturnedVariableDeclaration();
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
			if(sourceTypeRequiredForExtraction()) {
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
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
			change.getEdit().addChild(sourceEdit);
			change.addTextEditGroup(new TextEditGroup("Replace conditional structure with polymorphic method invocation", new TextEdit[] {sourceEdit}));
		} catch (JavaModelException e) {
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
				stateStrategyFile = contextProject.getFile(abstractClassName + ".java");
			}
			else if(contextContainer instanceof IFolder) {
				IFolder contextFolder = (IFolder)contextContainer;
				stateStrategyFile = contextFolder.getFile(abstractClassName + ".java");
			}
		}
		boolean stateStrategyAlreadyExists = false;
		ICompilationUnit stateStrategyICompilationUnit = JavaCore.createCompilationUnitFrom(stateStrategyFile);
		javaElementsToOpenInEditor.add(stateStrategyICompilationUnit);
		ASTParser stateStrategyParser = ASTParser.newParser(ASTReader.JLS);
		stateStrategyParser.setKind(ASTParser.K_COMPILATION_UNIT);
		Document stateStrategyDocument = null;
		if(stateStrategyFile.exists()) {
			stateStrategyAlreadyExists = true;
	        stateStrategyParser.setSource(stateStrategyICompilationUnit);
	        stateStrategyParser.setResolveBindings(true); // we need bindings later on
		}
		else {
			stateStrategyDocument = new Document();
			stateStrategyParser.setSource(stateStrategyDocument.get().toCharArray());
		}
        
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
					if(typeDeclaration.getName().getIdentifier().equals(abstractClassName)) {
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
			SimpleName stateStrategyName = stateStrategyAST.newSimpleName(abstractClassName);
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
				stateStrategyRewriter.set(abstractGetterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, stateStrategyAST.newSimpleName("get" + abstractClassName), null);
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
					if(variableDeclarationFragment.getParent() instanceof VariableDeclarationStatement) {
						VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
						returnType = variableDeclarationStatement.getType();
					}
					else if(variableDeclarationFragment.getParent() instanceof VariableDeclarationExpression) {
						VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)variableDeclarationFragment.getParent();
						returnType = variableDeclarationExpression.getType();
					}
					else if(variableDeclarationFragment.getParent() instanceof FieldDeclaration) {
						FieldDeclaration fieldDeclaration = (FieldDeclaration)variableDeclarationFragment.getParent();
						returnType = fieldDeclaration.getType();
					}
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
				Type type = null;
				if(variableDeclarationFragment.getParent() instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
					type = variableDeclarationStatement.getType();
				}
				else if(variableDeclarationFragment.getParent() instanceof VariableDeclarationExpression) {
					VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)variableDeclarationFragment.getParent();
					type = variableDeclarationExpression.getType();
				}
				else if(variableDeclarationFragment.getParent() instanceof FieldDeclaration) {
					FieldDeclaration fieldDeclaration = (FieldDeclaration)variableDeclarationFragment.getParent();
					type = fieldDeclaration.getType();
				}
				stateStrategyRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, type, null);
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
					Type type = null;
					if(variableDeclarationFragment.getParent() instanceof VariableDeclarationStatement) {
						VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
						type = variableDeclarationStatement.getType();
					}
					else if(variableDeclarationFragment.getParent() instanceof VariableDeclarationExpression) {
						VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)variableDeclarationFragment.getParent();
						type = variableDeclarationExpression.getType();
					}
					else if(variableDeclarationFragment.getParent() instanceof FieldDeclaration) {
						FieldDeclaration fieldDeclaration = (FieldDeclaration)variableDeclarationFragment.getParent();
						type = fieldDeclaration.getType();
					}
					stateStrategyRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, type, null);
					stateStrategyRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, variableDeclarationFragment.getName(), null);
					abstractMethodParametersRewrite.insertLast(parameter, null);
				}
			}
		}
		if(sourceTypeRequiredForExtraction()) {
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
		
		if(!stateStrategyAlreadyExists)
			stateStrategyTypesRewrite.insertLast(stateStrategyTypeDeclaration, null);
		
		if(stateStrategyDocument != null) {
			try {
				for(ITypeBinding typeBinding : requiredImportDeclarationsBasedOnSignature) {
					addImportDeclaration(typeBinding, stateStrategyCompilationUnit, stateStrategyRewriter);
				}
				TextEdit stateStrategyEdit = stateStrategyRewriter.rewriteAST(stateStrategyDocument, null);
				stateStrategyEdit.apply(stateStrategyDocument);
				CreateCompilationUnitChange createCompilationUnitChange =
					new CreateCompilationUnitChange(stateStrategyICompilationUnit, stateStrategyDocument.get(), stateStrategyFile.getCharset());
				createCompilationUnitChanges.put(stateStrategyICompilationUnit, createCompilationUnitChange);
			} catch (CoreException e) {
				e.printStackTrace();
			} catch (MalformedTreeException e) {
				e.printStackTrace();
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				MultiTextEdit stateStrategyMultiTextEdit = new MultiTextEdit();
				CompilationUnitChange stateStrategyCompilationUnitChange = new CompilationUnitChange("", stateStrategyICompilationUnit);
				stateStrategyCompilationUnitChange.setEdit(stateStrategyMultiTextEdit);
				compilationUnitChanges.put(stateStrategyICompilationUnit, stateStrategyCompilationUnitChange);
				
				ImportRewrite stateStrategyImportRewrite = ImportRewrite.create(stateStrategyCompilationUnit, true);
				for(ITypeBinding typeBinding : requiredImportDeclarationsBasedOnSignature) {
					if(!typeBinding.isNested())
						stateStrategyImportRewrite.addImport(typeBinding);
				}
				
				TextEdit stateStrategyImportEdit = stateStrategyImportRewrite.rewriteImports(null);
				if(stateStrategyImportRewrite.getCreatedImports().length > 0) {
					stateStrategyMultiTextEdit.addChild(stateStrategyImportEdit);
					stateStrategyCompilationUnitChange.addTextEditGroup(new TextEditGroup("Add required import declarations", new TextEdit[] {stateStrategyImportEdit}));
				}
				
				TextEdit stateStrategyEdit = stateStrategyRewriter.rewriteAST();
				stateStrategyMultiTextEdit.addChild(stateStrategyEdit);
				stateStrategyCompilationUnitChange.addTextEditGroup(new TextEditGroup("Create State/Strategy", new TextEdit[] {stateStrategyEdit}));
			} catch (JavaModelException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		
		List<ArrayList<Statement>> typeCheckStatements = typeCheckElimination.getTypeCheckStatements();
		List<String> subclassNames = new ArrayList<String>(staticFieldMap.values());
		subclassNames.addAll(additionalStaticFieldMap.values());
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
		List<SimpleName> staticFields = new ArrayList<SimpleName>(staticFieldMap.keySet());
		for(SimpleName simpleName : additionalStaticFieldMap.keySet())
			staticFields.add(simpleName);
		
		for(Expression expression : typeCheckElimination.getTypeCheckExpressions()) {
			List<SimpleName> leafStaticFields = typeCheckElimination.getStaticFields(expression);
			if(leafStaticFields.size() > 1) {
				List<String> leafSubclassNames = new ArrayList<String>();
				for(SimpleName leafStaticField : leafStaticFields) {
					leafSubclassNames.add(getTypeNameForNamedConstant(leafStaticField));
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
			ICompilationUnit subclassICompilationUnit = JavaCore.createCompilationUnitFrom(subclassFile);
			javaElementsToOpenInEditor.add(subclassICompilationUnit);
			ASTParser subclassParser = ASTParser.newParser(ASTReader.JLS);
			subclassParser.setKind(ASTParser.K_COMPILATION_UNIT);
			Document subclassDocument = null;
			if(subclassFile.exists()) {
				subclassAlreadyExists = true;
				subclassParser.setSource(subclassICompilationUnit);
				subclassParser.setResolveBindings(true); // we need bindings later on
			}
			else {
				subclassDocument = new Document();
				subclassParser.setSource(subclassDocument.get().toCharArray());
			}
			
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
				if(staticFieldNameBinding != null && staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
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
				subclassRewriter.set(subclassTypeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, subclassAST.newSimpleType(subclassAST.newSimpleName(abstractClassName)), null);
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
					if(staticFieldNameBinding != null && staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
						IVariableBinding staticFieldNameVariableBinding = (IVariableBinding)staticFieldNameBinding;
						ITypeBinding staticFieldDeclaringClass = staticFieldNameVariableBinding.getDeclaringClass();
						String staticFieldDeclaringClassQualifiedName = staticFieldDeclaringClass.getQualifiedName();
						IPackageBinding packageBinding = staticFieldDeclaringClass.getPackage();
						if(packageBinding != null && !packageBinding.getName().equals("")) {
							String packageBindingQualifiedName = packageBinding.getName();
							staticFieldNameDeclaringClass = staticFieldDeclaringClassQualifiedName.substring(
									packageBindingQualifiedName.length() + 1, staticFieldDeclaringClassQualifiedName.length());
						}
						else {
							staticFieldNameDeclaringClass = staticFieldDeclaringClassQualifiedName;
						}
					}
					FieldAccess fieldAccess = subclassAST.newFieldAccess();
					subclassRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, staticFields.get(i), null);
					if(!staticFieldNameDeclaringClass.contains(".")) {
						subclassRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, subclassAST.newSimpleName(staticFieldNameDeclaringClass), null);
					}
					else {
						QualifiedName qualifiedName = subclassAST.newQualifiedName(
								subclassAST.newName(staticFieldNameDeclaringClass.substring(0, staticFieldNameDeclaringClass.lastIndexOf("."))),
								subclassAST.newSimpleName(staticFieldNameDeclaringClass.substring(staticFieldNameDeclaringClass.lastIndexOf(".") + 1,
								staticFieldNameDeclaringClass.length())));
						subclassRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, qualifiedName, null);
					}
					subclassRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, fieldAccess, null);
					concreteGetterMethodBodyRewrite.insertLast(returnStatement, null);
					subclassRewriter.set(concreteGetterMethodDeclaration, MethodDeclaration.BODY_PROPERTY, concreteGetterMethodBody, null);
					subclassBodyRewrite.insertLast(concreteGetterMethodDeclaration, null);
				}
				else {
					MethodDeclaration concreteGetterMethodDeclaration = subclassAST.newMethodDeclaration();
					subclassRewriter.set(concreteGetterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, subclassAST.newSimpleName("get" + abstractClassName), null);
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
					if(staticFieldNameBinding != null && staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
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
						if(variableDeclarationFragment.getParent() instanceof VariableDeclarationStatement) {
							VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
							returnType = variableDeclarationStatement.getType();
						}
						else if(variableDeclarationFragment.getParent() instanceof VariableDeclarationExpression) {
							VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)variableDeclarationFragment.getParent();
							returnType = variableDeclarationExpression.getType();
						}
						else if(variableDeclarationFragment.getParent() instanceof FieldDeclaration) {
							FieldDeclaration fieldDeclaration = (FieldDeclaration)variableDeclarationFragment.getParent();
							returnType = fieldDeclaration.getType();
						}
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
					Type type = null;
					if(variableDeclarationFragment.getParent() instanceof VariableDeclarationStatement) {
						VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
						type = variableDeclarationStatement.getType();
					}
					else if(variableDeclarationFragment.getParent() instanceof VariableDeclarationExpression) {
						VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)variableDeclarationFragment.getParent();
						type = variableDeclarationExpression.getType();
					}
					else if(variableDeclarationFragment.getParent() instanceof FieldDeclaration) {
						FieldDeclaration fieldDeclaration = (FieldDeclaration)variableDeclarationFragment.getParent();
						type = fieldDeclaration.getType();
					}
					subclassRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, type, null);
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
						Type type = null;
						if(variableDeclarationFragment.getParent() instanceof VariableDeclarationStatement) {
							VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
							type = variableDeclarationStatement.getType();
						}
						else if(variableDeclarationFragment.getParent() instanceof VariableDeclarationExpression) {
							VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)variableDeclarationFragment.getParent();
							type = variableDeclarationExpression.getType();
						}
						else if(variableDeclarationFragment.getParent() instanceof FieldDeclaration) {
							FieldDeclaration fieldDeclaration = (FieldDeclaration)variableDeclarationFragment.getParent();
							type = fieldDeclaration.getType();
						}
						subclassRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, type, null);
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
			if(sourceTypeRequiredForExtraction()) {
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
				modifySourceVariableInstructionsInSubclass(oldVariableInstructions, newVariableInstructions, subclassAST, subclassRewriter, accessedFields, assignedFields, superAccessedFields, superAssignedFields);
				List<Expression> oldMethodInvocations = expressionExtractor.getMethodInvocations(enclosingIfStatementExpression);
				List<Expression> newMethodInvocations = expressionExtractor.getMethodInvocations(newEnclosingIfStatementExpression);
				modifySourceMethodInvocationsInSubclass(oldMethodInvocations, newMethodInvocations, subclassAST, subclassRewriter, accessedMethods, superAccessedMethods);
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
				modifySourceVariableInstructionsInSubclass(oldVariableInstructions, newVariableInstructions, subclassAST, subclassRewriter, accessedFields, assignedFields, superAccessedFields, superAssignedFields);
				List<Expression> oldMethodInvocations = expressionExtractor.getMethodInvocations(statement);
				List<Expression> newMethodInvocations = expressionExtractor.getMethodInvocations(newStatement);
				modifySourceMethodInvocationsInSubclass(oldMethodInvocations, newMethodInvocations, subclassAST, subclassRewriter, accessedMethods, superAccessedMethods);
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
			
			if(!subclassAlreadyExists)
				subclassTypesRewrite.insertLast(subclassTypeDeclaration, null);
			
			if(subclassDocument != null) {
				try {
					for(ITypeBinding typeBinding : requiredImportDeclarationsBasedOnSignature) {
						addImportDeclaration(typeBinding, subclassCompilationUnit, subclassRewriter);
					}
					Set<ITypeBinding> requiredImportDeclarationsBasedOnBranch = getRequiredImportDeclarationsBasedOnBranch(statements);
					for(ITypeBinding typeBinding : requiredImportDeclarationsBasedOnBranch) {
						if(!requiredImportDeclarationsBasedOnSignature.contains(typeBinding))
							addImportDeclaration(typeBinding, subclassCompilationUnit, subclassRewriter);
					}
					TextEdit subclassEdit = subclassRewriter.rewriteAST(subclassDocument, null);
					subclassEdit.apply(subclassDocument);
					CreateCompilationUnitChange createCompilationUnitChange =
						new CreateCompilationUnitChange(subclassICompilationUnit, subclassDocument.get(), subclassFile.getCharset());
					createCompilationUnitChanges.put(subclassICompilationUnit, createCompilationUnitChange);
				} catch (CoreException e) {
					e.printStackTrace();
				} catch (MalformedTreeException e) {
					e.printStackTrace();
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			}
			else {
				try {
					MultiTextEdit subclassMultiTextEdit = new MultiTextEdit();
					CompilationUnitChange subclassCompilationUnitChange = new CompilationUnitChange("", subclassICompilationUnit);
					subclassCompilationUnitChange.setEdit(subclassMultiTextEdit);
					compilationUnitChanges.put(subclassICompilationUnit, subclassCompilationUnitChange);
					
					ImportRewrite subclassImportRewrite = ImportRewrite.create(subclassCompilationUnit, true);
					for(ITypeBinding typeBinding : requiredImportDeclarationsBasedOnSignature) {
						if(!typeBinding.isNested())
							subclassImportRewrite.addImport(typeBinding);
					}
					Set<ITypeBinding> requiredImportDeclarationsBasedOnBranch = getRequiredImportDeclarationsBasedOnBranch(statements);
					for(ITypeBinding typeBinding : requiredImportDeclarationsBasedOnBranch) {
						if(!typeBinding.isNested())
							subclassImportRewrite.addImport(typeBinding);
					}
					
					TextEdit subclassImportEdit = subclassImportRewrite.rewriteImports(null);
					if(subclassImportRewrite.getCreatedImports().length > 0) {
						subclassMultiTextEdit.addChild(subclassImportEdit);
						subclassCompilationUnitChange.addTextEditGroup(new TextEditGroup("Add required import declarations", new TextEdit[] {subclassImportEdit}));
					}
					
					TextEdit subclassEdit = subclassRewriter.rewriteAST();
					subclassMultiTextEdit.addChild(subclassEdit);
					subclassCompilationUnitChange.addTextEditGroup(new TextEditGroup("Create concrete State/Strategy", new TextEdit[] {subclassEdit}));
				} catch (JavaModelException e) {
					e.printStackTrace();
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
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
		ICompilationUnit intermediateClassICompilationUnit = JavaCore.createCompilationUnitFrom(intermediateClassFile);
		javaElementsToOpenInEditor.add(intermediateClassICompilationUnit);
		ASTParser intermediateClassParser = ASTParser.newParser(ASTReader.JLS);
		intermediateClassParser.setKind(ASTParser.K_COMPILATION_UNIT);
		Document intermediateClassDocument = null;
		if(intermediateClassFile.exists()) {
			intermediateClassAlreadyExists = true;
			intermediateClassParser.setSource(intermediateClassICompilationUnit);
			intermediateClassParser.setResolveBindings(true); // we need bindings later on
		}
		else {
			intermediateClassDocument = new Document();
			intermediateClassParser.setSource(intermediateClassDocument.get().toCharArray());
		}
        
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
			intermediateClassRewriter.set(intermediateClassTypeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, intermediateClassAST.newSimpleType(intermediateClassAST.newSimpleName(abstractClassName)), null);
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
					if(variableDeclarationFragment.getParent() instanceof VariableDeclarationStatement) {
						VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
						returnType = variableDeclarationStatement.getType();
					}
					else if(variableDeclarationFragment.getParent() instanceof VariableDeclarationExpression) {
						VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)variableDeclarationFragment.getParent();
						returnType = variableDeclarationExpression.getType();
					}
					else if(variableDeclarationFragment.getParent() instanceof FieldDeclaration) {
						FieldDeclaration fieldDeclaration = (FieldDeclaration)variableDeclarationFragment.getParent();
						returnType = fieldDeclaration.getType();
					}
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
				Type type = null;
				if(variableDeclarationFragment.getParent() instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
					type = variableDeclarationStatement.getType();
				}
				else if(variableDeclarationFragment.getParent() instanceof VariableDeclarationExpression) {
					VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)variableDeclarationFragment.getParent();
					type = variableDeclarationExpression.getType();
				}
				else if(variableDeclarationFragment.getParent() instanceof FieldDeclaration) {
					FieldDeclaration fieldDeclaration = (FieldDeclaration)variableDeclarationFragment.getParent();
					type = fieldDeclaration.getType();
				}
				intermediateClassRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, type, null);
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
					Type type = null;
					if(variableDeclarationFragment.getParent() instanceof VariableDeclarationStatement) {
						VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
						type = variableDeclarationStatement.getType();
					}
					else if(variableDeclarationFragment.getParent() instanceof VariableDeclarationExpression) {
						VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)variableDeclarationFragment.getParent();
						type = variableDeclarationExpression.getType();
					}
					else if(variableDeclarationFragment.getParent() instanceof FieldDeclaration) {
						FieldDeclaration fieldDeclaration = (FieldDeclaration)variableDeclarationFragment.getParent();
						type = fieldDeclaration.getType();
					}
					intermediateClassRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, type, null);
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
		if(sourceTypeRequiredForExtraction()) {
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
			modifySourceVariableInstructionsInSubclass(oldVariableInstructions, newVariableInstructions, intermediateClassAST, intermediateClassRewriter, accessedFields, assignedFields, superAccessedFields, superAssignedFields);
			List<Expression> oldMethodInvocations = expressionExtractor.getMethodInvocations(enclosingIfStatementExpression);
			List<Expression> newMethodInvocations = expressionExtractor.getMethodInvocations(newEnclosingIfStatementExpression);
			modifySourceMethodInvocationsInSubclass(oldMethodInvocations, newMethodInvocations, intermediateClassAST, intermediateClassRewriter, accessedMethods, superAccessedMethods);
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
			modifySourceVariableInstructionsInSubclass(oldVariableInstructions, newVariableInstructions, intermediateClassAST, intermediateClassRewriter, accessedFields, assignedFields, superAccessedFields, superAssignedFields);
			List<Expression> oldMethodInvocations = expressionExtractor.getMethodInvocations(statement);
			List<Expression> newMethodInvocations = expressionExtractor.getMethodInvocations(newStatement);
			modifySourceMethodInvocationsInSubclass(oldMethodInvocations, newMethodInvocations, intermediateClassAST, intermediateClassRewriter, accessedMethods, superAccessedMethods);
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
		
		if(!intermediateClassAlreadyExists)
			intermediateClassTypesRewrite.insertLast(intermediateClassTypeDeclaration, null);
		
		if(intermediateClassDocument != null) {
			try {
				for(ITypeBinding typeBinding : requiredImportDeclarationsBasedOnSignature) {
					addImportDeclaration(typeBinding, intermediateClassCompilationUnit, intermediateClassRewriter);
				}
				Set<ITypeBinding> requiredImportDeclarationsBasedOnBranch = getRequiredImportDeclarationsBasedOnBranch(typeCheckStatements);
				for(ITypeBinding typeBinding : requiredImportDeclarationsBasedOnBranch) {
					if(!requiredImportDeclarationsBasedOnSignature.contains(typeBinding))
						addImportDeclaration(typeBinding, intermediateClassCompilationUnit, intermediateClassRewriter);
				}
				TextEdit intermediateClassEdit = intermediateClassRewriter.rewriteAST(intermediateClassDocument, null);
				intermediateClassEdit.apply(intermediateClassDocument);
				CreateCompilationUnitChange createCompilationUnitChange =
					new CreateCompilationUnitChange(intermediateClassICompilationUnit, intermediateClassDocument.get(), intermediateClassFile.getCharset());
				createCompilationUnitChanges.put(intermediateClassICompilationUnit, createCompilationUnitChange);
			} catch (MalformedTreeException e) {
				e.printStackTrace();
			} catch (BadLocationException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				MultiTextEdit intermediateClassMultiTextEdit = new MultiTextEdit();
				CompilationUnitChange intermediateClassCompilationUnitChange = new CompilationUnitChange("", intermediateClassICompilationUnit);
				intermediateClassCompilationUnitChange.setEdit(intermediateClassMultiTextEdit);
				compilationUnitChanges.put(intermediateClassICompilationUnit, intermediateClassCompilationUnitChange);
				
				ImportRewrite intermediateClassImportRewrite = ImportRewrite.create(intermediateClassCompilationUnit, true);
				for(ITypeBinding typeBinding : requiredImportDeclarationsBasedOnSignature) {
					if(!typeBinding.isNested())
						intermediateClassImportRewrite.addImport(typeBinding);
				}
				Set<ITypeBinding> requiredImportDeclarationsBasedOnBranch = getRequiredImportDeclarationsBasedOnBranch(typeCheckStatements);
				for(ITypeBinding typeBinding : requiredImportDeclarationsBasedOnBranch) {
					if(!typeBinding.isNested())
						intermediateClassImportRewrite.addImport(typeBinding);
				}
				
				TextEdit intermediateClassImportEdit = intermediateClassImportRewrite.rewriteImports(null);
				if(intermediateClassImportRewrite.getCreatedImports().length > 0) {
					intermediateClassMultiTextEdit.addChild(intermediateClassImportEdit);
					intermediateClassCompilationUnitChange.addTextEditGroup(new TextEditGroup("Add required import declarations", new TextEdit[] {intermediateClassImportEdit}));
				}
				
				TextEdit intermediateClassEdit = intermediateClassRewriter.rewriteAST();
				intermediateClassMultiTextEdit.addChild(intermediateClassEdit);
				intermediateClassCompilationUnitChange.addTextEditGroup(new TextEditGroup("Create intermediate class", new TextEdit[] {intermediateClassEdit}));
			} catch (JavaModelException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		
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
			ICompilationUnit subclassICompilationUnit = JavaCore.createCompilationUnitFrom(subclassFile);
			javaElementsToOpenInEditor.add(subclassICompilationUnit);
			ASTParser subclassParser = ASTParser.newParser(ASTReader.JLS);
			subclassParser.setKind(ASTParser.K_COMPILATION_UNIT);
			Document subclassDocument = null;
			if(subclassFile.exists()) {
				subclassAlreadyExists = true;
				subclassParser.setSource(subclassICompilationUnit);
				subclassParser.setResolveBindings(true); // we need bindings later on
			}
			else {
				subclassDocument = new Document();
				subclassParser.setSource(subclassDocument.get().toCharArray());
			}
			
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
				if(staticFieldNameBinding != null && staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
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
					if(staticFieldNameBinding != null && staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
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
					subclassRewriter.set(concreteGetterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, subclassAST.newSimpleName("get" + abstractClassName), null);
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
					if(staticFieldNameBinding != null && staticFieldNameBinding.getKind() == IBinding.VARIABLE) {
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
			
			if(subclassDocument != null) {
				try {
					TextEdit subclassEdit = subclassRewriter.rewriteAST(subclassDocument, null);
					subclassEdit.apply(subclassDocument);
					CreateCompilationUnitChange createCompilationUnitChange =
						new CreateCompilationUnitChange(subclassICompilationUnit, subclassDocument.get(), subclassFile.getCharset());
					createCompilationUnitChanges.put(subclassICompilationUnit, createCompilationUnitChange);
				} catch (MalformedTreeException e) {
					e.printStackTrace();
				} catch (BadLocationException e) {
					e.printStackTrace();
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
			else {
				try {
					CompilationUnitChange subclassCompilationUnitChange = new CompilationUnitChange("", subclassICompilationUnit);
					TextEdit subclassEdit = subclassRewriter.rewriteAST();
					subclassCompilationUnitChange.setEdit(subclassEdit);
					compilationUnitChanges.put(subclassICompilationUnit, subclassCompilationUnitChange);
					subclassCompilationUnitChange.addTextEditGroup(new TextEditGroup("Create concrete State/Strategy", new TextEdit[] {subclassEdit}));
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void modifyTypeFieldAssignmentsInContextClass(boolean modify) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		Set<MethodDeclaration> contextMethods = getAllMethodDeclarationsInSourceClass();
		List<SimpleName> staticFields = typeCheckElimination.getStaticFields();
		MethodDeclaration typeFieldSetterMethod = typeCheckElimination.getTypeFieldSetterMethod();
		for(MethodDeclaration methodDeclaration : contextMethods) {
			if(!methodDeclaration.equals(typeFieldSetterMethod)) {
				Block methodBody = methodDeclaration.getBody();
				if(methodBody != null) {
					List<Statement> statements = methodBody.statements();
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
							List<Expression> accessedVariables = expressionExtractor.getVariableInstructions(rightHandSide);
							ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
							AST contextAST = sourceTypeDeclaration.getAST();
							boolean rewriteAST = false;
							if(assignedVariable != null) {
								IBinding leftHandBinding = assignedVariable.resolveBinding();
								if(leftHandBinding != null && leftHandBinding.getKind() == IBinding.VARIABLE) {
									IVariableBinding assignedVariableBinding = (IVariableBinding)leftHandBinding;
									if(assignedVariableBinding.isField() && typeCheckElimination.getTypeField().resolveBinding().isEqualTo(assignedVariableBinding)) {
										if(modify && !nodeExistsInsideTypeCheckCodeFragment(assignment)) {
											MethodInvocation setterMethodInvocation = contextAST.newMethodInvocation();
											if(typeCheckElimination.getTypeFieldSetterMethod() != null) {
												sourceRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckElimination.getTypeFieldSetterMethod().getName(), null);
											}
											else {
												sourceRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName(setterMethodName()), null);
											}
											ListRewrite setterMethodInvocationArgumentsRewrite = sourceRewriter.getListRewrite(setterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
											setterMethodInvocationArgumentsRewrite.insertLast(assignment.getRightHandSide(), null);
											if(invoker != null) {
												sourceRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, invoker, null);
											}
											sourceRewriter.replace(assignment, setterMethodInvocation, null);
											rewriteAST = true;
										}
										for(Expression expression2 : accessedVariables) {
											SimpleName accessedVariable = (SimpleName)expression2;
											IBinding rightHandBinding = accessedVariable.resolveBinding();
											if(rightHandBinding != null && rightHandBinding.getKind() == IBinding.VARIABLE) {
												IVariableBinding accessedVariableBinding = (IVariableBinding)rightHandBinding;
												if(accessedVariableBinding.isField() && (accessedVariableBinding.getModifiers() & Modifier.STATIC) != 0 &&
														!containsVariable(staticFields, accessedVariable) && accessedVariableBinding.getType().isEqualTo(assignedVariableBinding.getType())) {
													if(!containsStaticFieldKey(accessedVariable) && !modify)
														additionalStaticFieldMap.put(accessedVariable, generateSubclassName(accessedVariable));
												}
											}
										}
									}
								}
							}
							for(Expression expression2 : accessedVariables) {
								SimpleName accessedVariable = (SimpleName)expression2;
								IBinding rightHandBinding = accessedVariable.resolveBinding();
								if(rightHandBinding != null && rightHandBinding.getKind() == IBinding.VARIABLE) {
									IVariableBinding accessedVariableBinding = (IVariableBinding)rightHandBinding;
									if(accessedVariableBinding.isField() && typeCheckElimination.getTypeField().resolveBinding().isEqualTo(accessedVariableBinding)) {
										if(modify && !nodeExistsInsideTypeCheckCodeFragment(accessedVariable)) {
											MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
											if(typeCheckElimination.getTypeFieldGetterMethod() != null) {
												sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckElimination.getTypeFieldGetterMethod().getName(), null);
											}
											else {
												sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + abstractClassName), null);
											}
											sourceRewriter.replace(accessedVariable, getterMethodInvocation, null);
											rewriteAST = true;
										}
									}
								}
							}
							if(rewriteAST) {
								try {
									TextEdit sourceEdit = sourceRewriter.rewriteAST();
									ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
									CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
									change.getEdit().addChild(sourceEdit);
									change.addTextEditGroup(new TextEditGroup("Replace field assignment with invocation of setter method", new TextEdit[] {sourceEdit}));
								} catch (JavaModelException e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
			}
		}
	}

	private void modifyTypeFieldAccessesInContextClass(boolean modify) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		Set<MethodDeclaration> contextMethods = getAllMethodDeclarationsInSourceClass();
		List<SimpleName> staticFields = typeCheckElimination.getStaticFields();
		for(MethodDeclaration methodDeclaration : contextMethods) {
			Block methodBody = methodDeclaration.getBody();
			if(methodBody != null) {
				List<Statement> statements = methodBody.statements();
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
							if(switchStatementExpressionBinding != null && switchStatementExpressionBinding.getKind() == IBinding.VARIABLE) {
								IVariableBinding accessedVariableBinding = (IVariableBinding)switchStatementExpressionBinding;
								if(accessedVariableBinding.isField() && typeCheckElimination.getTypeField().resolveBinding().isEqualTo(accessedVariableBinding)) {
									if(modify && !nodeExistsInsideTypeCheckCodeFragment(switchStatementExpression)) {
										ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
										AST contextAST = sourceTypeDeclaration.getAST();
										MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
										if(typeCheckElimination.getTypeFieldGetterMethod() != null) {
											sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckElimination.getTypeFieldGetterMethod().getName(), null);
										}
										else {
											sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + abstractClassName), null);
										}
										sourceRewriter.replace(switchStatementExpression, getterMethodInvocation, null);
										try {
											TextEdit sourceEdit = sourceRewriter.rewriteAST();
											ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
											CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
											change.getEdit().addChild(sourceEdit);
											change.addTextEditGroup(new TextEditGroup("Replace field access with invocation of getter method", new TextEdit[] {sourceEdit}));
										} catch (JavaModelException e) {
											e.printStackTrace();
										}
									}
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
												if(switchCaseExpressionBinding != null && switchCaseExpressionBinding.getKind() == IBinding.VARIABLE) {
													IVariableBinding comparedVariableBinding = (IVariableBinding)switchCaseExpressionBinding;
													if(comparedVariableBinding.isField() && (comparedVariableBinding.getModifiers() & Modifier.STATIC) != 0 &&
															!containsVariable(staticFields, comparedVariable) && comparedVariableBinding.getType().isEqualTo(accessedVariableBinding.getType())) {
														if(!containsStaticFieldKey(comparedVariable) && !modify)
															additionalStaticFieldMap.put(comparedVariable, generateSubclassName(comparedVariable));
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
									if(argumentBinding != null && argumentBinding.getKind() == IBinding.VARIABLE) {
										IVariableBinding accessedVariableBinding = (IVariableBinding)argumentBinding;
										if(accessedVariableBinding.isField() && typeCheckElimination.getTypeField().resolveBinding().isEqualTo(accessedVariable.resolveBinding())) {
											if(modify && !nodeExistsInsideTypeCheckCodeFragment(argument)) {
												ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
												AST contextAST = sourceTypeDeclaration.getAST();
												MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
												if(typeCheckElimination.getTypeFieldGetterMethod() != null) {
													sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckElimination.getTypeFieldGetterMethod().getName(), null);
												}
												else {
													sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + abstractClassName), null);
												}
												ListRewrite argumentRewrite = sourceRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
												argumentRewrite.replace(argument, getterMethodInvocation, null);
												try {
													TextEdit sourceEdit = sourceRewriter.rewriteAST();
													ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
													CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
													change.getEdit().addChild(sourceEdit);
													change.addTextEditGroup(new TextEditGroup("Replace field access with invocation of getter method", new TextEdit[] {sourceEdit}));
												} catch (JavaModelException e) {
													e.printStackTrace();
												}
											}
										}
									}
								}
							}
						}
					}
					List<Expression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(statement);
					for(Expression expression : classInstanceCreations) {
						ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)expression;
						List<Expression> arguments = classInstanceCreation.arguments();
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
								if(argumentBinding != null && argumentBinding.getKind() == IBinding.VARIABLE) {
									IVariableBinding accessedVariableBinding = (IVariableBinding)argumentBinding;
									if(accessedVariableBinding.isField() && typeCheckElimination.getTypeField().resolveBinding().isEqualTo(accessedVariable.resolveBinding())) {
										if(modify && !nodeExistsInsideTypeCheckCodeFragment(argument)) {
											ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
											AST contextAST = sourceTypeDeclaration.getAST();
											MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
											if(typeCheckElimination.getTypeFieldGetterMethod() != null) {
												sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckElimination.getTypeFieldGetterMethod().getName(), null);
											}
											else {
												sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + abstractClassName), null);
											}
											ListRewrite argumentRewrite = sourceRewriter.getListRewrite(classInstanceCreation, ClassInstanceCreation.ARGUMENTS_PROPERTY);
											argumentRewrite.replace(argument, getterMethodInvocation, null);
											try {
												TextEdit sourceEdit = sourceRewriter.rewriteAST();
												ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
												CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
												change.getEdit().addChild(sourceEdit);
												change.addTextEditGroup(new TextEditGroup("Replace field access with invocation of getter method", new TextEdit[] {sourceEdit}));
											} catch (JavaModelException e) {
												e.printStackTrace();
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
							if(leftOperandBinding != null && leftOperandBinding.getKind() == IBinding.VARIABLE) {
								IVariableBinding accessedVariableBinding = (IVariableBinding)leftOperandBinding;
								if(accessedVariableBinding.isField() && typeCheckElimination.getTypeField().resolveBinding().isEqualTo(accessedVariableBinding)) {
									if(modify && !nodeExistsInsideTypeCheckCodeFragment(leftOperand) && !isAssignmentChild(infixExpression)) {
										ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
										AST contextAST = sourceTypeDeclaration.getAST();
										MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
										if(typeCheckElimination.getTypeFieldGetterMethod() != null) {
											sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckElimination.getTypeFieldGetterMethod().getName(), null);
										}
										else {
											sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + abstractClassName), null);
										}
										sourceRewriter.replace(leftOperand, getterMethodInvocation, null);
										try {
											TextEdit sourceEdit = sourceRewriter.rewriteAST();
											ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
											CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
											change.getEdit().addChild(sourceEdit);
											change.addTextEditGroup(new TextEditGroup("Replace field access with invocation of getter method", new TextEdit[] {sourceEdit}));
										} catch (JavaModelException e) {
											e.printStackTrace();
										}
									}
									typeFieldIsReplaced = true;
									if(comparedVariable != null) {
										IBinding rightOperandBinding = comparedVariable.resolveBinding();
										if(rightOperandBinding != null && rightOperandBinding.getKind() == IBinding.VARIABLE) {
											IVariableBinding comparedVariableBinding = (IVariableBinding)rightOperandBinding;
											if(comparedVariableBinding.isField() && (comparedVariableBinding.getModifiers() & Modifier.STATIC) != 0 &&
													!containsVariable(staticFields, comparedVariable) && comparedVariableBinding.getType().isEqualTo(accessedVariableBinding.getType())) {
												if(!containsStaticFieldKey(comparedVariable) && !modify)
													additionalStaticFieldMap.put(comparedVariable, generateSubclassName(comparedVariable));
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
								if(rightOperandBinding != null && rightOperandBinding.getKind() == IBinding.VARIABLE) {
									IVariableBinding accessedVariableBinding = (IVariableBinding)rightOperandBinding;
									if(accessedVariableBinding.isField() && typeCheckElimination.getTypeField().resolveBinding().isEqualTo(accessedVariableBinding)) {
										if(modify && !nodeExistsInsideTypeCheckCodeFragment(rightOperand) && !isAssignmentChild(infixExpression)) {
											ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
											AST contextAST = sourceTypeDeclaration.getAST();
											MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
											if(typeCheckElimination.getTypeFieldGetterMethod() != null) {
												sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckElimination.getTypeFieldGetterMethod().getName(), null);
											}
											else {
												sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("get" + abstractClassName), null);
											}
											sourceRewriter.replace(rightOperand, getterMethodInvocation, null);
											try {
												TextEdit sourceEdit = sourceRewriter.rewriteAST();
												ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
												CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
												change.getEdit().addChild(sourceEdit);
												change.addTextEditGroup(new TextEditGroup("Replace field access with invocation of getter method", new TextEdit[] {sourceEdit}));
											} catch (JavaModelException e) {
												e.printStackTrace();
											}
										}
										if(comparedVariable != null) {
											IBinding leftOperandBinding = comparedVariable.resolveBinding();
											if(leftOperandBinding != null && leftOperandBinding.getKind() == IBinding.VARIABLE) {
												IVariableBinding comparedVariableBinding = (IVariableBinding)leftOperandBinding;
												if(comparedVariableBinding.isField() && (comparedVariableBinding.getModifiers() & Modifier.STATIC) != 0 &&
														!containsVariable(staticFields, comparedVariable) && comparedVariableBinding.getType().isEqualTo(accessedVariableBinding.getType())) {
													if(!containsStaticFieldKey(comparedVariable) && !modify)
														additionalStaticFieldMap.put(comparedVariable, generateSubclassName(comparedVariable));
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

	private Set<MethodDeclaration> getAllMethodDeclarationsInSourceClass() {
		Set<MethodDeclaration> contextMethods = new LinkedHashSet<MethodDeclaration>();
		for(FieldDeclaration fieldDeclaration : sourceTypeDeclaration.getFields()) {
			contextMethods.addAll(getMethodDeclarationsWithinAnonymousClassDeclarations(fieldDeclaration));
		}
		List<MethodDeclaration> methodDeclarationList = Arrays.asList(sourceTypeDeclaration.getMethods());
		contextMethods.addAll(methodDeclarationList);
		/*for(MethodDeclaration methodDeclaration : methodDeclarationList) {
			contextMethods.addAll(getMethodDeclarationsWithinAnonymousClassDeclarations(methodDeclaration));
		}*/
		//get methods of inner classes
		TypeDeclaration[] types = sourceTypeDeclaration.getTypes();
		for(TypeDeclaration type : types) {
			for(FieldDeclaration fieldDeclaration : type.getFields()) {
				contextMethods.addAll(getMethodDeclarationsWithinAnonymousClassDeclarations(fieldDeclaration));
			}
			List<MethodDeclaration> innerMethodDeclarationList = Arrays.asList(type.getMethods());
			contextMethods.addAll(innerMethodDeclarationList);
			/*for(MethodDeclaration methodDeclaration : innerMethodDeclarationList) {
				contextMethods.addAll(getMethodDeclarationsWithinAnonymousClassDeclarations(methodDeclaration));
			}*/
		}
		return contextMethods;
	}

	private Set<MethodDeclaration> getMethodDeclarationsWithinAnonymousClassDeclarations(MethodDeclaration methodDeclaration) {
		Set<MethodDeclaration> methods = new LinkedHashSet<MethodDeclaration>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(methodDeclaration.getBody());
		for(Expression expression : classInstanceCreations) {
			ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)expression;
			AnonymousClassDeclaration anonymousClassDeclaration = classInstanceCreation.getAnonymousClassDeclaration();
			if(anonymousClassDeclaration != null) {
				List<BodyDeclaration> bodyDeclarations = anonymousClassDeclaration.bodyDeclarations();
				for(BodyDeclaration bodyDeclaration : bodyDeclarations) {
					if(bodyDeclaration instanceof MethodDeclaration)
						methods.add((MethodDeclaration)bodyDeclaration);
				}
			}
		}
		return methods;
	}

	private Set<MethodDeclaration> getMethodDeclarationsWithinAnonymousClassDeclarations(FieldDeclaration fieldDeclaration) {
		Set<MethodDeclaration> methods = new LinkedHashSet<MethodDeclaration>();
		List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
		for(VariableDeclarationFragment fragment : fragments) {
			Expression expression = fragment.getInitializer();
			if(expression != null && expression instanceof ClassInstanceCreation) {
				ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)expression;
				AnonymousClassDeclaration anonymousClassDeclaration = classInstanceCreation.getAnonymousClassDeclaration();
				if(anonymousClassDeclaration != null) {
					List<BodyDeclaration> bodyDeclarations = anonymousClassDeclaration.bodyDeclarations();
					for(BodyDeclaration bodyDeclaration : bodyDeclarations) {
						if(bodyDeclaration instanceof MethodDeclaration)
							methods.add((MethodDeclaration)bodyDeclaration);
					}
				}
			}
		}
		return methods;
	}

	private boolean isAssignmentChild(ASTNode node) {
		if(node instanceof Assignment)
			return true;
		else if(node instanceof Statement)
			return false;
		else
			return isAssignmentChild(node.getParent());
	}

	private boolean nodeExistsInsideTypeCheckCodeFragment(ASTNode node) {
		Statement statement = typeCheckElimination.getTypeCheckCodeFragment();
		int startPosition = statement.getStartPosition();
		int endPosition = startPosition + statement.getLength();
		if(node.getStartPosition() >= startPosition && node.getStartPosition() <= endPosition)
			return true;
		else
			return false;
	}

	private void generateRequiredImportDeclarationsBasedOnSignature() {
		Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
		if(returnedVariable != null) {
			Type returnType = null;
			if(returnedVariable instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)returnedVariable;
				returnType = singleVariableDeclaration.getType();
			}
			else if(returnedVariable instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)returnedVariable;
				if(variableDeclarationFragment.getParent() instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
					returnType = variableDeclarationStatement.getType();
				}
				else if(variableDeclarationFragment.getParent() instanceof VariableDeclarationExpression) {
					VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)variableDeclarationFragment.getParent();
					returnType = variableDeclarationExpression.getType();
				}
				else if(variableDeclarationFragment.getParent() instanceof FieldDeclaration) {
					FieldDeclaration fieldDeclaration = (FieldDeclaration)variableDeclarationFragment.getParent();
					returnType = fieldDeclaration.getType();
				}
			}
			ITypeBinding returnTypeBinding = returnType.resolveBinding();
			if(!typeBindings.contains(returnTypeBinding))
				typeBindings.add(returnTypeBinding);
		}
		else {
			Type returnType = typeCheckElimination.getTypeCheckMethodReturnType();
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
					if(variableDeclarationFragment.getParent() instanceof VariableDeclarationStatement) {
						VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
						variableType = variableDeclarationStatement.getType();
					}
					else if(variableDeclarationFragment.getParent() instanceof VariableDeclarationExpression) {
						VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)variableDeclarationFragment.getParent();
						variableType = variableDeclarationExpression.getType();
					}
					else if(variableDeclarationFragment.getParent() instanceof FieldDeclaration) {
						FieldDeclaration fieldDeclaration = (FieldDeclaration)variableDeclarationFragment.getParent();
						variableType = fieldDeclaration.getType();
					}
				}
				ITypeBinding variableTypeBinding = variableType.resolveBinding();
				if(!typeBindings.contains(variableTypeBinding))
					typeBindings.add(variableTypeBinding);
			}
		}
		
		if(typeCheckElimination.getAccessedFields().size() > 0 || typeCheckElimination.getAssignedFields().size() > 0 ||
				typeCheckElimination.getAccessedMethods().size() > 0 || typeCheckElimination.getSuperAccessedMethods().size() > 0 ||
				typeCheckElimination.getSuperAccessedFieldBindings().size() > 0 || typeCheckElimination.getSuperAssignedFieldBindings().size() > 0) {
			if(!typeBindings.contains(sourceTypeDeclaration.resolveBinding()))
				typeBindings.add(sourceTypeDeclaration.resolveBinding());
		}
		
		for(ITypeBinding typeBinding : thrownExceptions) {
			if(!typeBindings.contains(typeBinding))
				typeBindings.add(typeBinding);
		}
		
		RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportDeclarationsBasedOnSignature);
	}

	private Set<ITypeBinding> getRequiredImportDeclarationsBasedOnBranch(ArrayList<Statement> statements) {
		Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
		for(Statement statement : statements) {
			TypeVisitor typeVisitor = new TypeVisitor();
			statement.accept(typeVisitor);
			typeBindings.addAll(typeVisitor.getTypeBindings());
		}
		Set<ITypeBinding> finalTypeBindings = new LinkedHashSet<ITypeBinding>();
		RefactoringUtility.getSimpleTypeBindings(typeBindings, finalTypeBindings);
        return finalTypeBindings;
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
		if(!qualifiedPackageName.equals("") && !qualifiedPackageName.equals("java.lang") &&
				!qualifiedPackageName.equals(sourcePackageDeclarationName) && !typeBinding.isNested()) {
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
		for(SimpleName simpleName : additionalStaticFieldMap.keySet())
			staticFields.add(simpleName);
		for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
			List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				boolean modifierIsReplaced = false;
				for(SimpleName staticField : staticFields) {
					if(staticField.resolveBinding().isEqualTo(fragment.getName().resolveBinding())) {
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
									modifierFound = true;
									modifierRewrite.replace(modifier, publicModifier, null);
									modifierIsReplaced = true;
									try {
										TextEdit sourceEdit = sourceRewriter.rewriteAST();
										ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
										CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
										change.getEdit().addChild(sourceEdit);
										change.addTextEditGroup(new TextEditGroup("Change access level to public", new TextEdit[] {sourceEdit}));
									} catch (JavaModelException e) {
										e.printStackTrace();
									}
								}
							}
						}
						if(!modifierFound) {
							modifierRewrite.insertFirst(publicModifier, null);
							modifierIsReplaced = true;
							try {
								TextEdit sourceEdit = sourceRewriter.rewriteAST();
								ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
								CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
								change.getEdit().addChild(sourceEdit);
								change.addTextEditGroup(new TextEditGroup("Set access level to public", new TextEdit[] {sourceEdit}));
							} catch (JavaModelException e) {
								e.printStackTrace();
							}
						}
						break;
					}
				}
				if(modifierIsReplaced)
					break;
			}
		}
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
					List<Expression> accessedVariables = expressionExtractor.getVariableInstructions(rightHandSide);
					if(assignedVariable != null) {
						IBinding leftHandBinding = assignedVariable.resolveBinding();
						if(leftHandBinding != null && leftHandBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding assignedVariableBinding = (IVariableBinding)leftHandBinding;
							if(typeCheckElimination.getTypeLocalVariable().resolveBinding().isEqualTo(assignedVariableBinding)) {
								for(Expression expression2 : accessedVariables) {
									SimpleName accessedVariable = (SimpleName)expression2;
									IBinding rightHandBinding = accessedVariable.resolveBinding();
									if(rightHandBinding != null && rightHandBinding.getKind() == IBinding.VARIABLE) {
										IVariableBinding accessedVariableBinding = (IVariableBinding)rightHandBinding;
										if(accessedVariableBinding.isField() && (accessedVariableBinding.getModifiers() & Modifier.STATIC) != 0 &&
												!containsVariable(staticFields, accessedVariable) && accessedVariableBinding.getType().isEqualTo(assignedVariableBinding.getType())) {
											if(!containsStaticFieldKey(accessedVariable))
												additionalStaticFieldMap.put(accessedVariable, generateSubclassName(accessedVariable));
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
						IBinding switchStatementExpressionBinding = accessedVariable.resolveBinding();
						if(switchStatementExpressionBinding != null && switchStatementExpressionBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding accessedVariableBinding = (IVariableBinding)switchStatementExpressionBinding;
							if(typeCheckElimination.getTypeLocalVariable().resolveBinding().isEqualTo(accessedVariableBinding)) {
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
											if(switchCaseExpressionBinding != null && switchCaseExpressionBinding.getKind() == IBinding.VARIABLE) {
												IVariableBinding comparedVariableBinding = (IVariableBinding)switchCaseExpressionBinding;
												if(comparedVariableBinding.isField() && (comparedVariableBinding.getModifiers() & Modifier.STATIC) != 0 &&
														!containsVariable(staticFields, comparedVariable) && comparedVariableBinding.getType().isEqualTo(accessedVariableBinding.getType())) {
													if(!containsStaticFieldKey(comparedVariable))
														additionalStaticFieldMap.put(comparedVariable, generateSubclassName(comparedVariable));
												}
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
						IBinding leftOperandBinding = accessedVariable.resolveBinding();
						if(leftOperandBinding != null && leftOperandBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding accessedVariableBinding = (IVariableBinding)leftOperandBinding;
							if(typeCheckElimination.getTypeLocalVariable().resolveBinding().isEqualTo(accessedVariableBinding)) {
								typeLocalVariableIsFound = true;
								if(comparedVariable != null) {
									IBinding rightOperandBinding = comparedVariable.resolveBinding();
									if(rightOperandBinding != null && rightOperandBinding.getKind() == IBinding.VARIABLE) {
										IVariableBinding comparedVariableBinding = (IVariableBinding)rightOperandBinding;
										if(comparedVariableBinding.isField() && (comparedVariableBinding.getModifiers() & Modifier.STATIC) != 0 &&
												!containsVariable(staticFields, comparedVariable) && comparedVariableBinding.getType().isEqualTo(accessedVariableBinding.getType())) {
											if(!containsStaticFieldKey(comparedVariable))
												additionalStaticFieldMap.put(comparedVariable, generateSubclassName(comparedVariable));
										}
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
							IBinding rightOperandBinding = accessedVariable.resolveBinding();
							if(rightOperandBinding != null && rightOperandBinding.getKind() == IBinding.VARIABLE) {
								IVariableBinding accessedVariableBinding = (IVariableBinding)rightOperandBinding;
								if(typeCheckElimination.getTypeLocalVariable().resolveBinding().isEqualTo(accessedVariableBinding)) {
									if(comparedVariable != null) {
										IBinding leftOperandBinding = comparedVariable.resolveBinding();
										if(leftOperandBinding != null && leftOperandBinding.getKind() == IBinding.VARIABLE) {
											IVariableBinding comparedVariableBinding = (IVariableBinding)leftOperandBinding;
											if(comparedVariableBinding.isField() && (comparedVariableBinding.getModifiers() & Modifier.STATIC) != 0 &&
													!containsVariable(staticFields, comparedVariable) && comparedVariableBinding.getType().isEqualTo(accessedVariableBinding.getType())) {
												if(!containsStaticFieldKey(comparedVariable))
													additionalStaticFieldMap.put(comparedVariable, generateSubclassName(comparedVariable));
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

	private boolean containsStaticFieldKey(SimpleName simpleName) {
		for(SimpleName keySimpleName : additionalStaticFieldMap.keySet()) {
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

	public CompilationUnit getSourceCompilationUnit() {
		return sourceCompilationUnit;
	}

	public String getAbstractClassName() {
		return abstractClassName;
	}

	public SimpleName getTypeVariableSimpleName() {
		return typeCheckElimination.getTypeVariableSimpleName();
	}

	public Set<Map.Entry<SimpleName, String>> getStaticFieldMapEntrySet() {
		return staticFieldMap.entrySet();
	}

	public Set<Map.Entry<SimpleName, String>> getAdditionalStaticFieldMapEntrySet() {
		return additionalStaticFieldMap.entrySet();
	}

	public void setTypeNameForNamedConstant(SimpleName namedConstant, String typeName) {
		if(staticFieldMap.containsKey(namedConstant)) {
			staticFieldMap.put(namedConstant, typeName);
		}
		else if(additionalStaticFieldMap.containsKey(namedConstant)) {
			additionalStaticFieldMap.put(namedConstant, typeName);
		}
		else {
			abstractClassName = typeName;
		}
	}

	public String getTypeNameForNamedConstant(SimpleName namedConstant) {
		if(staticFieldMap.containsKey(namedConstant)) {
			return staticFieldMap.get(namedConstant);
		}
		else if(additionalStaticFieldMap.containsKey(namedConstant)) {
			return additionalStaticFieldMap.get(namedConstant);
		}
		return null;
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
			if(typeCheckElimination.getTypeField() != null) {
				modifyTypeFieldAssignmentsInContextClass(false);
				modifyTypeFieldAccessesInContextClass(false);
			}
			else if(typeCheckElimination.getTypeLocalVariable() != null) {
				identifyTypeLocalVariableAssignmentsInTypeCheckMethod();
				identifyTypeLocalVariableAccessesInTypeCheckMethod();
			}
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
			final Collection<Change> changes = new ArrayList<Change>();
			changes.addAll(compilationUnitChanges.values());
			changes.addAll(createCompilationUnitChanges.values());
			CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {
				@Override
				public ChangeDescriptor getDescriptor() {
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
					String project = sourceICompilationUnit.getJavaProject().getElementName();
					String description = MessageFormat.format("Replace Type Code with State/Strategy in method ''{0}''", new Object[] { typeCheckElimination.getTypeCheckMethod().getName().getIdentifier()});
					String comment = null;
					return new RefactoringChangeDescriptor(new ReplaceTypeCodeWithStateStrategyDescriptor(project, description, comment,
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
		return "Replace Type Code with State/Strategy";
	}
}
