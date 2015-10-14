package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.inheritance.InheritanceTree;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.StatementExtractor;
import gr.uom.java.ast.util.TypeVisitor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
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
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

public class ReplaceConditionalWithPolymorphism extends PolymorphismRefactoring {
	private VariableDeclaration returnedVariable;
	private Set<ITypeBinding> thrownExceptions;
	private VariableDeclaration typeVariable;
	private MethodInvocation typeMethodInvocation;
	
	public ReplaceConditionalWithPolymorphism(IFile sourceFile, CompilationUnit sourceCompilationUnit,
			TypeDeclaration sourceTypeDeclaration, TypeCheckElimination typeCheckElimination) {
		super(sourceFile, sourceCompilationUnit, sourceTypeDeclaration, typeCheckElimination);
		this.returnedVariable = typeCheckElimination.getTypeCheckMethodReturnedVariable();
		this.thrownExceptions = typeCheckElimination.getThrownExceptions();
		if(typeCheckElimination.getTypeField() != null) {
			this.typeVariable = typeCheckElimination.getTypeField();
		}
		else if(typeCheckElimination.getTypeLocalVariable() != null) {
			this.typeVariable = typeCheckElimination.getTypeLocalVariable();
		}
		this.typeMethodInvocation = typeCheckElimination.getTypeMethodInvocation();
	}

	public void apply() {
		modifyInheritanceHierarchy();
		modifyClient();
	}

	private void modifyClient() {
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
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
			if(sourceTypeRequiredForExtraction()) {
				methodInvocationArgumentsRewrite.insertLast(clientAST.newThisExpression(), null);
				setPublicModifierToSourceTypeDeclaration();
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
			if(sourceTypeRequiredForExtraction()) {
				methodInvocationArgumentsRewrite.insertLast(clientAST.newThisExpression(), null);
				setPublicModifierToSourceTypeDeclaration();
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
			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
			change.getEdit().addChild(sourceEdit);
			change.addTextEditGroup(new TextEditGroup("Replace conditional structure with polymorphic method invocation", new TextEdit[] {sourceEdit}));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void setPublicModifierToSourceTypeDeclaration() {
		PackageDeclaration sourcePackageDeclaration = sourceCompilationUnit.getPackage();
		InheritanceTree tree = null;
		if(typeCheckElimination.getExistingInheritanceTree() != null)
			tree = typeCheckElimination.getExistingInheritanceTree();
		else if(typeCheckElimination.getInheritanceTreeMatchingWithStaticTypes() != null)
			tree = typeCheckElimination.getInheritanceTreeMatchingWithStaticTypes();
		String abstractClassName = null;
		if(tree != null) {
			DefaultMutableTreeNode root = tree.getRootNode();
			abstractClassName = (String)root.getUserObject();
		}
		if(sourcePackageDeclaration != null && abstractClassName != null && abstractClassName.contains(".")) {
			String sourcePackageName = sourcePackageDeclaration.getName().getFullyQualifiedName();
			String targetPackageName = abstractClassName.substring(0, abstractClassName.lastIndexOf("."));
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
								ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
								CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
								change.getEdit().addChild(sourceEdit);
								change.addTextEditGroup(new TextEditGroup("Change access level to public", new TextEdit[] {sourceEdit}));
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
						ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
						CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
						change.getEdit().addChild(sourceEdit);
						change.addTextEditGroup(new TextEditGroup("Set access level to public", new TextEdit[] {sourceEdit}));
					}
					catch(JavaModelException javaModelException) {
						javaModelException.printStackTrace();
					}
				}
			}
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
		
		ICompilationUnit abstractICompilationUnit = null;
		CompilationUnit abstractCompilationUnit = null;
		if(abstractClassFile != null) {
			IJavaElement abstractJavaElement = JavaCore.create(abstractClassFile);
			javaElementsToOpenInEditor.add(abstractJavaElement);
			abstractICompilationUnit = (ICompilationUnit)abstractJavaElement;
			ASTParser abstractParser = ASTParser.newParser(ASTReader.JLS);
			abstractParser.setKind(ASTParser.K_COMPILATION_UNIT);
			abstractParser.setSource(abstractICompilationUnit);
			abstractParser.setResolveBindings(true); // we need bindings later on
			abstractCompilationUnit = (CompilationUnit)abstractParser.createAST(null);
		}
		else {
			IJavaElement javaElement = JavaCore.create(contextContainer);
			if(javaElement != null && javaElement instanceof IPackageFragment) {
				IPackageFragment packageFragment = (IPackageFragment)javaElement;
				try {
					IJavaElement[] children = packageFragment.getChildren();
					for(IJavaElement child : children) {
						if(child instanceof ICompilationUnit) {
							ICompilationUnit childCompilationUnit = (ICompilationUnit)child;
							IType[] types = childCompilationUnit.getTypes();
							for(IType type : types) {
								String qualifiedName = packageFragment.getElementName() + "." + type.getElementName();
								if(qualifiedName.equals(abstractClassFullyQualifiedName)) {
									abstractICompilationUnit = childCompilationUnit;
									break;
								}
							}
							if(abstractICompilationUnit != null) {
								if(abstractICompilationUnit.equals(sourceCompilationUnit.getJavaElement())) {
									abstractCompilationUnit = sourceCompilationUnit;
								}
								else {
									ASTParser abstractParser = ASTParser.newParser(ASTReader.JLS);
									abstractParser.setKind(ASTParser.K_COMPILATION_UNIT);
									abstractParser.setSource(abstractICompilationUnit);
									abstractParser.setResolveBindings(true); // we need bindings later on
									abstractCompilationUnit = (CompilationUnit)abstractParser.createAST(null);
								}
								break;
							}
						}
					}
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
        
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
		
		Set<VariableDeclarationFragment> accessedFields = typeCheckElimination.getAccessedFields();
		Set<VariableDeclarationFragment> assignedFields = typeCheckElimination.getAssignedFields();
		Set<MethodDeclaration> accessedMethods = typeCheckElimination.getAccessedMethods();
		Set<IMethodBinding> superAccessedMethods = typeCheckElimination.getSuperAccessedMethods();
		Set<IVariableBinding> superAccessedFields = typeCheckElimination.getSuperAccessedFieldBindings();
		Set<IVariableBinding> superAssignedFields = typeCheckElimination.getSuperAssignedFieldBindings();
		Set<ITypeBinding> requiredImportDeclarationsBasedOnSignature = getRequiredImportDeclarationsBasedOnSignature();
		MultiTextEdit abstractMultiTextEdit = new MultiTextEdit();
		CompilationUnitChange abstractCompilationUnitChange = new CompilationUnitChange("", abstractICompilationUnit);
		abstractCompilationUnitChange.setEdit(abstractMultiTextEdit);
		compilationUnitChanges.put(abstractICompilationUnit, abstractCompilationUnitChange);
		
		if(!typeCheckElimination.getSubclassNames().contains(abstractClassFullyQualifiedName)) {
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
					abstractRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, type, null);
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
						abstractRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, type, null);
						abstractRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, variableDeclarationFragment.getName(), null);
						abstractMethodParametersRewrite.insertLast(parameter, null);
					}
				}
			}
			
			if(sourceTypeRequiredForExtraction()) {
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

			try {
				TextEdit abstractEdit = abstractRewriter.rewriteAST();
				abstractMultiTextEdit.addChild(abstractEdit);
				abstractCompilationUnitChange.addTextEditGroup(new TextEditGroup("Add abstract method", new TextEdit[] {abstractEdit}));
			} catch(JavaModelException e) {
				e.printStackTrace();
			}
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
			ICompilationUnit subclassICompilationUnit = null;
			CompilationUnit subclassCompilationUnit = null;
			AST subclassAST = null;
			ASTRewrite subclassRewriter = null;
			if(subclassFile != null) {
				if(subclassFile.equals(abstractClassFile)) {
					subclassICompilationUnit = abstractICompilationUnit;
					subclassCompilationUnit = abstractCompilationUnit;
				}
				else {
					IJavaElement subclassJavaElement = JavaCore.create(subclassFile);
					javaElementsToOpenInEditor.add(subclassJavaElement);
					subclassICompilationUnit = (ICompilationUnit)subclassJavaElement;
					ASTParser subclassParser = ASTParser.newParser(ASTReader.JLS);
					subclassParser.setKind(ASTParser.K_COMPILATION_UNIT);
					subclassParser.setSource(subclassICompilationUnit);
					subclassParser.setResolveBindings(true); // we need bindings later on
					subclassCompilationUnit = (CompilationUnit)subclassParser.createAST(null);
				}
			}
			else {
				IJavaElement javaElement = JavaCore.create(contextContainer);
				if(javaElement != null && javaElement instanceof IPackageFragment) {
					IPackageFragment packageFragment = (IPackageFragment)javaElement;
					try {
						IJavaElement[] children = packageFragment.getChildren();
						for(IJavaElement child : children) {
							if(child instanceof ICompilationUnit) {
								ICompilationUnit childCompilationUnit = (ICompilationUnit)child;
								IType[] types = childCompilationUnit.getTypes();
								for(IType type : types) {
									String qualifiedName = packageFragment.getElementName() + "." + type.getElementName();
									if(qualifiedName.equals(subclassNames.get(i))) {
										subclassICompilationUnit = childCompilationUnit;
										break;
									}
								}
								if(subclassICompilationUnit != null) {
									if(subclassICompilationUnit.equals(sourceCompilationUnit.getJavaElement())) {
										subclassCompilationUnit = sourceCompilationUnit;
									}
									else {
										ASTParser subclassParser = ASTParser.newParser(ASTReader.JLS);
										subclassParser.setKind(ASTParser.K_COMPILATION_UNIT);
										subclassParser.setSource(subclassICompilationUnit);
										subclassParser.setResolveBindings(true); // we need bindings later on
										subclassCompilationUnit = (CompilationUnit)subclassParser.createAST(null);
									}
									break;
								}
							}
						}
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
			}
			subclassAST = subclassCompilationUnit.getAST();
			subclassRewriter = ASTRewrite.create(subclassAST);
			TypeDeclaration subclassTypeDeclaration = null;
			List<AbstractTypeDeclaration> subclassAbstractTypeDeclarations = subclassCompilationUnit.types();
			for(AbstractTypeDeclaration abstractTypeDeclaration : subclassAbstractTypeDeclarations) {
				if(abstractTypeDeclaration instanceof TypeDeclaration) {
					TypeDeclaration topLevelTypeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
					List<TypeDeclaration> typeDeclarations = new ArrayList<TypeDeclaration>();
	        		typeDeclarations.add(topLevelTypeDeclaration);
	        		typeDeclarations.addAll(getRecursivelyInnerTypes(topLevelTypeDeclaration));
	        		for(TypeDeclaration typeDeclaration : typeDeclarations) {
	        			if(typeDeclaration.resolveBinding().getQualifiedName().equals(subclassNames.get(i))) {
	        				subclassTypeDeclaration = typeDeclaration;
	        				break;
	        			}
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
					List<Statement> oldVariableDeclarations = statementExtractor.getVariableDeclarationStatements(statement);
					List<Statement> newVariableDeclarations = statementExtractor.getVariableDeclarationStatements(newStatement);
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
				List<Expression> oldCastExpressions = expressionExtractor.getCastExpressions(statement);
				List<Expression> newCastExpressions = expressionExtractor.getCastExpressions(newStatement);
				replaceCastExpressionWithThisExpression(oldCastExpressions, newCastExpressions, subclassTypeDeclaration, subclassAST, subclassRewriter);
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
			
			//special handling if the inner classes are nested within the superclass
			MultiTextEdit subclassMultiTextEdit = null;
			CompilationUnitChange subclassCompilationUnitChange = null;
			if(subclassICompilationUnit.equals(abstractICompilationUnit)) {
				subclassCompilationUnitChange = compilationUnitChanges.get(subclassICompilationUnit);
				subclassMultiTextEdit = (MultiTextEdit)subclassCompilationUnitChange.getEdit();
			}
			else {
				subclassMultiTextEdit = new MultiTextEdit();
				subclassCompilationUnitChange = new CompilationUnitChange("", subclassICompilationUnit);
				subclassCompilationUnitChange.setEdit(subclassMultiTextEdit);
				compilationUnitChanges.put(subclassICompilationUnit, subclassCompilationUnitChange);
			}
			
			try {
				if(!subclassICompilationUnit.equals(abstractICompilationUnit)) {
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
				}
				else {
					Set<ITypeBinding> requiredImportDeclarationsBasedOnBranch = getRequiredImportDeclarationsBasedOnBranch(statements);
					requiredImportDeclarationsBasedOnSignature.addAll(requiredImportDeclarationsBasedOnBranch);
				}

				TextEdit subclassEdit = subclassRewriter.rewriteAST();
				subclassMultiTextEdit.addChild(subclassEdit);
				subclassCompilationUnitChange.addTextEditGroup(new TextEditGroup("Add concrete method", new TextEdit[] {subclassEdit}));
			} catch(JavaModelException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		try {
			ImportRewrite abstractImportRewrite = ImportRewrite.create(abstractCompilationUnit, true);
			for(ITypeBinding typeBinding : requiredImportDeclarationsBasedOnSignature) {
				if(!typeBinding.isNested())
					abstractImportRewrite.addImport(typeBinding);
			}
			TextEdit abstractImportEdit = abstractImportRewrite.rewriteImports(null);
			if(abstractImportRewrite.getCreatedImports().length > 0) {
				abstractMultiTextEdit.addChild(abstractImportEdit);
				abstractCompilationUnitChange.addTextEditGroup(new TextEditGroup("Add required import declarations", new TextEdit[] {abstractImportEdit}));
			}
		} catch(JavaModelException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private void replaceCastExpressionWithThisExpression(List<Expression> oldCastExpressions, List<Expression> newCastExpressions, TypeDeclaration subclassTypeDeclaration, AST subclassAST, ASTRewrite subclassRewriter) {
		int j = 0;
		for(Expression expression : oldCastExpressions) {
			CastExpression castExpression = (CastExpression)expression;
			if(castExpression.getType().resolveBinding().isEqualTo(subclassTypeDeclaration.resolveBinding())) {
				if(castExpression.getExpression() instanceof SimpleName) {
					SimpleName castSimpleName = (SimpleName)castExpression.getExpression();
					if(typeVariable != null && typeVariable.getName().resolveBinding().isEqualTo(castSimpleName.resolveBinding())) {
						subclassRewriter.replace(newCastExpressions.get(j), subclassAST.newThisExpression(), null);
					}
				}
				else if(castExpression.getExpression() instanceof MethodInvocation) {
					MethodInvocation castMethodInvocation = (MethodInvocation)castExpression.getExpression();
					if(typeMethodInvocation != null && typeMethodInvocation.subtreeMatch(new ASTMatcher(), castMethodInvocation)) {
						subclassRewriter.replace(newCastExpressions.get(j), subclassAST.newThisExpression(), null);
					}
				}
			}
			j++;
		}
	}

	private List<TypeDeclaration> getRecursivelyInnerTypes(TypeDeclaration typeDeclaration) {
		List<TypeDeclaration> innerTypeDeclarations = new ArrayList<TypeDeclaration>();
		TypeDeclaration[] types = typeDeclaration.getTypes();
		for(TypeDeclaration type : types) {
			innerTypeDeclarations.add(type);
			innerTypeDeclarations.addAll(getRecursivelyInnerTypes(type));
		}
		return innerTypeDeclarations;
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

	private Set<ITypeBinding> getRequiredImportDeclarationsBasedOnSignature() {
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
		
		return typeBindings;
	}

	private Set<ITypeBinding> getRequiredImportDeclarationsBasedOnBranch(ArrayList<Statement> statements) {
		Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
		for(Statement statement : statements) {
			TypeVisitor typeVisitor = new TypeVisitor();
			statement.accept(typeVisitor);
			typeBindings.addAll(typeVisitor.getTypeBindings());
		}
		return typeBindings;
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
			final Collection<CompilationUnitChange> changes = compilationUnitChanges.values();
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
