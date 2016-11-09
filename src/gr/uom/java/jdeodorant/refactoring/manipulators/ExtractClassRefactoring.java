package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.MethodDeclarationUtility;
import gr.uom.java.ast.util.StatementExtractor;
import gr.uom.java.ast.util.ThrownExceptionVisitor;
import gr.uom.java.ast.util.TypeVisitor;
import gr.uom.java.ast.util.math.AdjacencyList;
import gr.uom.java.ast.util.math.Edge;
import gr.uom.java.ast.util.math.Node;
import gr.uom.java.ast.util.math.TarjanAlgorithm;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
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
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression.Operator;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreateCompilationUnitChange;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ExtractClassRefactoring extends Refactoring {
	private static final String GETTER_PREFIX = "get";
	private static final String SETTER_PREFIX = "set";
	private static final String ACCESSOR_SUFFIX = "2";
	private IFile sourceFile;
	private CompilationUnit sourceCompilationUnit;
	private TypeDeclaration sourceTypeDeclaration;
	private Map<ICompilationUnit, CompilationUnitChange> compilationUnitChanges;
	private Map<ICompilationUnit, CreateCompilationUnitChange> createCompilationUnitChanges;
	private Set<IJavaElement> javaElementsToOpenInEditor;
	private Set<ITypeBinding> requiredImportDeclarationsInExtractedClass;
	private Map<MethodDeclaration, Set<PlainVariable>> additionalArgumentsAddedToExtractedMethods;
	private Map<MethodDeclaration, Set<SingleVariableDeclaration>> additionalParametersAddedToExtractedMethods;
	private Set<String> sourceMethodBindingsChangedWithPublicModifier;
	private Set<String> sourceFieldBindingsWithCreatedSetterMethod;
	private Set<String> sourceFieldBindingsWithCreatedGetterMethod;
	private Set<FieldDeclaration> fieldDeclarationsChangedWithPublicModifier;
	private Set<BodyDeclaration> memberTypeDeclarationsChangedWithPublicModifier;
	private Map<MethodDeclaration, Set<MethodInvocation>> oldMethodInvocationsWithinExtractedMethods;
	private Map<MethodDeclaration, Set<MethodInvocation>> newMethodInvocationsWithinExtractedMethods;
	private Map<MethodDeclaration, MethodDeclaration> oldToNewExtractedMethodDeclarationMap;
	private Set<VariableDeclaration> extractedFieldFragments;
	private Set<MethodDeclaration> extractedMethods;
	private Set<MethodDeclaration> delegateMethods;
	private String extractedTypeName;
	private boolean leaveDelegateForPublicMethods;
	private Map<Statement, ASTRewrite> statementRewriteMap;
	//this map holds for each constructor the assignment statements that initialize final extracted fields
	private Map<MethodDeclaration, Map<VariableDeclaration, Assignment>> constructorFinalFieldAssignmentMap;
	//this map hold the parameters that should be passed in each constructor of the extracted class
	private Map<MethodDeclaration, Set<VariableDeclaration>> extractedClassConstructorParameterMap;
	private Set<VariableDeclaration> extractedFieldsWithThisExpressionInTheirInitializer;
	private Set<IMethodBinding> staticallyImportedMethods;
	
	public ExtractClassRefactoring(IFile sourceFile, CompilationUnit sourceCompilationUnit, TypeDeclaration sourceTypeDeclaration,
			Set<VariableDeclaration> extractedFieldFragments, Set<MethodDeclaration> extractedMethods, Set<MethodDeclaration> delegateMethods, String extractedTypeName) {
		this.sourceFile = sourceFile;
		this.sourceCompilationUnit = sourceCompilationUnit;
		this.sourceTypeDeclaration = sourceTypeDeclaration;
		this.compilationUnitChanges = new LinkedHashMap<ICompilationUnit, CompilationUnitChange>();
		ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
		MultiTextEdit sourceMultiTextEdit = new MultiTextEdit();
		CompilationUnitChange sourceCompilationUnitChange = new CompilationUnitChange("", sourceICompilationUnit);
		sourceCompilationUnitChange.setEdit(sourceMultiTextEdit);
		this.compilationUnitChanges.put(sourceICompilationUnit, sourceCompilationUnitChange);
		this.createCompilationUnitChanges = new LinkedHashMap<ICompilationUnit, CreateCompilationUnitChange>();
		this.javaElementsToOpenInEditor = new LinkedHashSet<IJavaElement>();
		this.requiredImportDeclarationsInExtractedClass = new LinkedHashSet<ITypeBinding>();
		this.additionalArgumentsAddedToExtractedMethods = new LinkedHashMap<MethodDeclaration, Set<PlainVariable>>();
		this.additionalParametersAddedToExtractedMethods = new LinkedHashMap<MethodDeclaration, Set<SingleVariableDeclaration>>();
		this.sourceMethodBindingsChangedWithPublicModifier = new LinkedHashSet<String>();
		this.sourceFieldBindingsWithCreatedSetterMethod = new LinkedHashSet<String>();
		this.sourceFieldBindingsWithCreatedGetterMethod = new LinkedHashSet<String>();
		this.fieldDeclarationsChangedWithPublicModifier = new LinkedHashSet<FieldDeclaration>();
		this.memberTypeDeclarationsChangedWithPublicModifier = new LinkedHashSet<BodyDeclaration>();
		this.oldMethodInvocationsWithinExtractedMethods = new LinkedHashMap<MethodDeclaration, Set<MethodInvocation>>();
		this.newMethodInvocationsWithinExtractedMethods = new LinkedHashMap<MethodDeclaration, Set<MethodInvocation>>();
		this.oldToNewExtractedMethodDeclarationMap = new LinkedHashMap<MethodDeclaration, MethodDeclaration>();
		this.extractedFieldFragments = extractedFieldFragments;
		this.extractedMethods = extractedMethods;
		this.delegateMethods = delegateMethods;
		this.extractedTypeName = extractedTypeName;
		this.leaveDelegateForPublicMethods = false;
		this.statementRewriteMap = new LinkedHashMap<Statement, ASTRewrite>();
		this.constructorFinalFieldAssignmentMap = new LinkedHashMap<MethodDeclaration, Map<VariableDeclaration, Assignment>>();
		this.extractedClassConstructorParameterMap = new LinkedHashMap<MethodDeclaration, Set<VariableDeclaration>>();
		this.extractedFieldsWithThisExpressionInTheirInitializer = new LinkedHashSet<VariableDeclaration>();
		this.staticallyImportedMethods = new LinkedHashSet<IMethodBinding>();
		for(MethodDeclaration extractedMethod : extractedMethods) {
			additionalArgumentsAddedToExtractedMethods.put(extractedMethod, new LinkedHashSet<PlainVariable>());
			additionalParametersAddedToExtractedMethods.put(extractedMethod, new LinkedHashSet<SingleVariableDeclaration>());
		}
	}

	public String getExtractedTypeName() {
		return extractedTypeName;
	}

	public void setExtractedTypeName(String targetTypeName) {
		this.extractedTypeName = targetTypeName;
	}

	public void setLeaveDelegateForPublicMethods(boolean leaveDelegateForPublicMethods) {
		this.leaveDelegateForPublicMethods = leaveDelegateForPublicMethods;
	}

	public CompilationUnit getSourceCompilationUnit() {
		return sourceCompilationUnit;
	}

	public Set<IJavaElement> getJavaElementsToOpenInEditor() {
		return javaElementsToOpenInEditor;
	}

	private String appendAccessorMethodSuffix(String accessorMethodName, MethodDeclaration[] methodDeclarations) {
		return appendAccessorMethodSuffix(accessorMethodName, new LinkedHashSet<MethodDeclaration>(Arrays.asList(methodDeclarations)));
	}

	private String appendAccessorMethodSuffix(String accessorMethodName, Set<MethodDeclaration> methodDeclarations) {
		boolean sourceTypeContainsMethodWithAccessorNameThatIsNotPureAccessor = false;
		for(MethodDeclaration methodDeclaration : methodDeclarations) {
			if(methodDeclaration.getName().getIdentifier().equals(accessorMethodName)) {
				if(accessorMethodName.startsWith(GETTER_PREFIX)) {
					SimpleName simpleName = MethodDeclarationUtility.isGetter(methodDeclaration);
					if(simpleName == null) {
						sourceTypeContainsMethodWithAccessorNameThatIsNotPureAccessor = true;
						break;
					}
				}
				else if(accessorMethodName.startsWith(SETTER_PREFIX)) {
					SimpleName simpleName = MethodDeclarationUtility.isSetter(methodDeclaration);
					if(simpleName == null) {
						sourceTypeContainsMethodWithAccessorNameThatIsNotPureAccessor = true;
						break;
					}
				}
			}
		}
		if(sourceTypeContainsMethodWithAccessorNameThatIsNotPureAccessor) {
			accessorMethodName += ACCESSOR_SUFFIX;
		}
		return accessorMethodName;
	}

	public void apply() {
		for(MethodDeclaration method : extractedMethods) {
			int modifiers = method.getModifiers();
			if((modifiers & Modifier.PRIVATE) == 0)
				delegateMethods.add(method);
		}
		removeFieldFragmentsInSourceClass(extractedFieldFragments);
		Set<VariableDeclaration> modifiedFieldsInNonExtractedMethods = new LinkedHashSet<VariableDeclaration>();
		Set<VariableDeclaration> accessedFieldsInNonExtractedMethods = new LinkedHashSet<VariableDeclaration>();
		modifyExtractedFieldAssignmentsInSourceClass(extractedFieldFragments, modifiedFieldsInNonExtractedMethods, accessedFieldsInNonExtractedMethods);
		modifyExtractedFieldAccessesInSourceClass(extractedFieldFragments, accessedFieldsInNonExtractedMethods);
		createExtractedTypeFieldReferenceInSourceClass();
		
		Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
		TypeVisitor typeVisitor = new TypeVisitor();
		for(VariableDeclaration fieldFragment : extractedFieldFragments) {
			fieldFragment.getParent().accept(typeVisitor);
			for(ITypeBinding typeBinding : typeVisitor.getTypeBindings()) {
				typeBindings.add(typeBinding);
			}
		}
		for(MethodDeclaration method : extractedMethods) {
			method.accept(typeVisitor);
			for(ITypeBinding typeBinding : typeVisitor.getTypeBindings()) {
				typeBindings.add(typeBinding);
			}
		}
		RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportDeclarationsInExtractedClass);
		
		createExtractedClass(modifiedFieldsInNonExtractedMethods, accessedFieldsInNonExtractedMethods);
		modifyExtractedMethodInvocationsInSourceClass();
		handleInitializationOfExtractedFieldsWithThisExpressionInTheirInitializer();
		for(Statement statement : statementRewriteMap.keySet()) {
			ASTRewrite sourceRewriter = statementRewriteMap.get(statement);
			try {
				TextEdit sourceEdit = sourceRewriter.rewriteAST();
				ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
				CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
				change.getEdit().addChild(sourceEdit);
				change.addTextEditGroup(new TextEditGroup("Change access of extracted member", new TextEdit[] {sourceEdit}));
			}
			catch(JavaModelException javaModelException) {
				javaModelException.printStackTrace();
			}
		}
		Set<MethodDeclaration> methodsToBeRemoved = new LinkedHashSet<MethodDeclaration>();
		for(MethodDeclaration method : extractedMethods) {
			if(delegateMethods.contains(method))
				addDelegationInExtractedMethod(method);
			else
				methodsToBeRemoved.add(method);
		}
		if(methodsToBeRemoved.size() > 0)
			removeSourceMethods(methodsToBeRemoved);
	}

	private void handleInitializationOfExtractedFieldsWithThisExpressionInTheirInitializer() {
		String modifiedExtractedTypeName = extractedTypeName.substring(0,1).toLowerCase() + extractedTypeName.substring(1,extractedTypeName.length());
		for(VariableDeclaration fieldFragment : extractedFieldsWithThisExpressionInTheirInitializer) {
			String originalFieldName = fieldFragment.getName().getIdentifier();
			String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
			for(MethodDeclaration methodDeclaration : sourceTypeDeclaration.getMethods()) {
				if(methodDeclaration.isConstructor()) {
					ASTRewrite sourceRewriter = ASTRewrite.create(sourceCompilationUnit.getAST());
					ListRewrite constructorBodyRewrite = sourceRewriter.getListRewrite(methodDeclaration.getBody(), Block.STATEMENTS_PROPERTY);
					AST contextAST = sourceTypeDeclaration.getAST();
					MethodInvocation setterMethodInvocation = contextAST.newMethodInvocation();
					sourceRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName(SETTER_PREFIX + modifiedFieldName), null);
					ListRewrite setterMethodInvocationArgumentsRewrite = sourceRewriter.getListRewrite(setterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
					setterMethodInvocationArgumentsRewrite.insertLast(fieldFragment.getInitializer(), null);
					if((fieldFragment.resolveBinding().getModifiers() & Modifier.STATIC) != 0) {
						sourceRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(extractedTypeName), null);
					}
					else {
						sourceRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(modifiedExtractedTypeName), null);
					}
					ExpressionStatement expressionStatement = contextAST.newExpressionStatement(setterMethodInvocation);
					constructorBodyRewrite.insertLast(expressionStatement, null);
					try {
						TextEdit sourceEdit = sourceRewriter.rewriteAST();
						ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
						CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
						change.getEdit().addChild(sourceEdit);
						change.addTextEditGroup(new TextEditGroup("Initialize extracted field " + fieldFragment.getName().getIdentifier(), new TextEdit[] {sourceEdit}));
					}
					catch(JavaModelException javaModelException) {
						javaModelException.printStackTrace();
					}
				}
			}
		}
	}

	private void addDelegationInExtractedMethod(MethodDeclaration sourceMethod) {
		List<SingleVariableDeclaration> sourceMethodParameters = sourceMethod.parameters();
		
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
		sourceRewriter.set(delegation, MethodInvocation.NAME_PROPERTY, sourceMethod.getName(), null);
		if((sourceMethod.getModifiers() & Modifier.STATIC) != 0) {
			sourceRewriter.set(delegation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(extractedTypeName), null);
		}
		else {
			String modifiedExtractedTypeName = extractedTypeName.substring(0,1).toLowerCase() + extractedTypeName.substring(1,extractedTypeName.length());
			SimpleName expressionName = ast.newSimpleName(modifiedExtractedTypeName);
			sourceRewriter.set(delegation, MethodInvocation.EXPRESSION_PROPERTY, expressionName, null);
		}
		
		ListRewrite argumentRewrite = sourceRewriter.getListRewrite(delegation, MethodInvocation.ARGUMENTS_PROPERTY);
		for(SingleVariableDeclaration parameter : sourceMethodParameters) {
			SimpleName argumentName = ast.newSimpleName(parameter.getName().getIdentifier());
			argumentRewrite.insertLast(argumentName, null);
		}
		Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
		for(PlainVariable argument : additionalArgumentsAddedToMovedMethod) {
			if(isThisVariable(argument)) {
				argumentRewrite.insertLast(ast.newThisExpression(), null);
			}
			else {
				if(argument.isField()) {
					FieldAccess fieldAccess = ast.newFieldAccess();
					sourceRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, ast.newSimpleName(argument.getVariableName()), null);
					sourceRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, ast.newThisExpression(), null);
					argumentRewrite.insertLast(fieldAccess, null);
				}
				else {
					argumentRewrite.insertLast(ast.newSimpleName(argument.getVariableName()), null);
				}
			}
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
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
			change.getEdit().addChild(sourceEdit);
			change.addTextEditGroup(new TextEditGroup("Leave delegate to extracted method " + sourceMethod.getName().getIdentifier(), new TextEdit[] {sourceEdit}));
		}
		catch(JavaModelException javaModelException) {
			javaModelException.printStackTrace();
		}
	}

	private void removeSourceMethods(Set<MethodDeclaration> methods) {
		//Eclipse bug workaround: Overlapping TextEdits Exception when the x last methods of a type declaration are being removed
		List<Integer> removedMethodPositions = new ArrayList<Integer>();
		MethodDeclaration[] sourceMethods = sourceTypeDeclaration.getMethods();
		int numberOfMethods = sourceMethods.length;
		int i = 0;
		for(MethodDeclaration sourceMethod : sourceMethods) {
			if(methods.contains(sourceMethod)) {
				removedMethodPositions.add(i);
			}
			i++;
		}
		Set<MethodDeclaration> methodsToBeRemovedTogether = new LinkedHashSet<MethodDeclaration>();
		int lastMethodPosition = numberOfMethods - 1;
		if(removedMethodPositions.get(removedMethodPositions.size() - 1) == lastMethodPosition) {
			int validIndex = lastMethodPosition;
			for(int j=removedMethodPositions.size()-1; j>=0; j--) {
				if(removedMethodPositions.get(j) == validIndex)
					methodsToBeRemovedTogether.add(sourceMethods[removedMethodPositions.get(j)]);
				else
					break;
				validIndex--;
			}
		}
		for(MethodDeclaration method : methods) {
			if(!methodsToBeRemovedTogether.contains(method)) {
				ASTRewrite sourceRewriter = ASTRewrite.create(sourceCompilationUnit.getAST());
				ListRewrite classBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
				classBodyRewrite.remove(method, null);
				try {
					TextEdit sourceEdit = sourceRewriter.rewriteAST();
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
					CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
					change.getEdit().addChild(sourceEdit);
					change.addTextEditGroup(new TextEditGroup("Remove extracted method " + method.getName().getIdentifier(), new TextEdit[] {sourceEdit}));
				}
				catch(JavaModelException javaModelException) {
					javaModelException.printStackTrace();
				}
			}
		}
		if(!methodsToBeRemovedTogether.isEmpty()) {
			ASTRewrite sourceRewriter = ASTRewrite.create(sourceCompilationUnit.getAST());
			ListRewrite classBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			for(MethodDeclaration method : methodsToBeRemovedTogether) {
				classBodyRewrite.remove(method, null);
			}
			try {
				TextEdit sourceEdit = sourceRewriter.rewriteAST();
				ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
				CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
				change.getEdit().addChild(sourceEdit);
				change.addTextEditGroup(new TextEditGroup("Remove extracted method", new TextEdit[] {sourceEdit}));
			}
			catch(JavaModelException javaModelException) {
				javaModelException.printStackTrace();
			}
		}
	}

	private void modifyExtractedMethodInvocationsInSourceClass() {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		Set<MethodDeclaration> contextMethods = getAllMethodDeclarationsInSourceClass();
		for(MethodDeclaration methodDeclaration : contextMethods) {
			if(!extractedMethods.contains(methodDeclaration)) {
				Block methodBody = methodDeclaration.getBody();
				if(methodBody != null) {
					List<Statement> statements = methodBody.statements();
					for(Statement statement : statements) {
						ASTRewrite sourceRewriter = null;
						if(statementRewriteMap.containsKey(statement)) {
							sourceRewriter = statementRewriteMap.get(statement);
						}
						else {
							sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
						}
						AST ast = sourceTypeDeclaration.getAST();
						boolean rewriteAST = false;
						List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(statement);
						for(Expression expression : methodInvocations) {
							if(expression instanceof MethodInvocation) {
								MethodInvocation methodInvocation = (MethodInvocation)expression;
								MethodDeclaration extractedMethod = getExtractedMethod(methodInvocation.resolveMethodBinding());
								if(extractedMethod != null) {
									if(methodInvocation.getExpression() == null || methodInvocation.getExpression() instanceof ThisExpression) {
										ListRewrite argumentRewrite = sourceRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
										Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(extractedMethod);
										for(PlainVariable argument : additionalArgumentsAddedToMovedMethod) {
											if(isThisVariable(argument)) {
												if(isParentAnonymousClassDeclaration(methodInvocation)) {
													ThisExpression thisExpression = ast.newThisExpression();
													sourceRewriter.set(thisExpression, ThisExpression.QUALIFIER_PROPERTY, sourceTypeDeclaration.getName(), null);
													argumentRewrite.insertLast(thisExpression, null);
												}
												else {
													argumentRewrite.insertLast(ast.newThisExpression(), null);
												}
											}
											else {
												if(argument.isField()) {
													FieldAccess fieldAccess = ast.newFieldAccess();
													sourceRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, ast.newSimpleName(argument.getVariableName()), null);
													sourceRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, ast.newThisExpression(), null);
													argumentRewrite.insertLast(fieldAccess, null);
												}
												else {
													argumentRewrite.insertLast(ast.newSimpleName(argument.getVariableName()), null);
												}
											}
										}
										if((extractedMethod.getModifiers() & Modifier.STATIC) != 0) {
											sourceRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(extractedTypeName), null);
										}
										else {
											String modifiedExtractedTypeName = extractedTypeName.substring(0,1).toLowerCase() + extractedTypeName.substring(1,extractedTypeName.length());
											sourceRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedExtractedTypeName), null);
										}
										rewriteAST = true;
									}
									else {
										delegateMethods.add(extractedMethod);
									}
								}
							}
						}
						if(rewriteAST) {
							if(!statementRewriteMap.containsKey(statement))
								statementRewriteMap.put(statement, sourceRewriter);
							/*try {
								TextEdit sourceEdit = sourceRewriter.rewriteAST();
								ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
								CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
								change.getEdit().addChild(sourceEdit);
								change.addTextEditGroup(new TextEditGroup("Change invocation of extracted method", new TextEdit[] {sourceEdit}));
							}
							catch(JavaModelException javaModelException) {
								javaModelException.printStackTrace();
							}*/
						}
					}
				}
			}
		}
	}

	private boolean existsNonTransientExtractedFieldFragment() {
		for(VariableDeclaration fieldFragment : extractedFieldFragments) {
			FieldDeclaration originalFieldDeclaration = (FieldDeclaration)fieldFragment.getParent();
			List<IExtendedModifier> originalModifiers = originalFieldDeclaration.modifiers();
			boolean transientFound = false;
    		for(IExtendedModifier extendedModifier : originalModifiers) {
    			if(extendedModifier.isModifier()) {
    				Modifier modifier = (Modifier)extendedModifier;
    				if(modifier.isTransient()) {
    					transientFound = true;
    					break;
    				}
    			}
    		}
    		if(!transientFound) {
    			return true;
    		}
		}
		return false;
	}

	private ITypeBinding implementsSerializableInterface(ITypeBinding typeBinding) {
		ITypeBinding[] implementedInterfaces = typeBinding.getInterfaces();
		for(ITypeBinding implementedInterface : implementedInterfaces) {
			if(implementedInterface.getQualifiedName().equals("java.io.Serializable")) {
				return implementedInterface;
			}
		}
		ITypeBinding superclassTypeBinding = typeBinding.getSuperclass();
		if(superclassTypeBinding != null) {
			return implementsSerializableInterface(superclassTypeBinding);
		}
		return null;
	}

	private ITypeBinding implementsCloneableInterface(ITypeBinding typeBinding) {
		ITypeBinding[] implementedInterfaces = typeBinding.getInterfaces();
		for(ITypeBinding implementedInterface : implementedInterfaces) {
			if(implementedInterface.getQualifiedName().equals("java.lang.Cloneable")) {
				return implementedInterface;
			}
			ITypeBinding[] superInterfaces = implementedInterface.getInterfaces();
			for(ITypeBinding superInterface : superInterfaces) {
				if(superInterface.getQualifiedName().equals("java.lang.Cloneable")) {
					return implementedInterface;
				}
			}
		}
		return null;
	}

	private IVariableBinding staticFieldInitializer(VariableDeclaration fieldFragment) {
		if(fieldFragment.getInitializer() instanceof SimpleName) {
			SimpleName simpleName = (SimpleName)fieldFragment.getInitializer();
			IBinding binding = simpleName.resolveBinding();
			if(binding != null && binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				if((variableBinding.getModifiers() & Modifier.STATIC) != 0) {
					return variableBinding;
				}
			}
		}
		return null;
	}

	private boolean variableBindingInExtractedFields(IVariableBinding variableBinding) {
		for(VariableDeclaration variableDeclaration : extractedFieldFragments) {
			if(variableDeclaration.resolveBinding().isEqualTo(variableBinding)) {
				return true;
			}
		}
		return false;
	}

	private void createExtractedClass(Set<VariableDeclaration> modifiedFieldsInNonExtractedMethods, Set<VariableDeclaration> accessedFieldsInNonExtractedMethods) {
		IContainer contextContainer = (IContainer)sourceFile.getParent();
		IFile extractedClassFile = null;
		if(contextContainer instanceof IProject) {
			IProject contextProject = (IProject)contextContainer;
			extractedClassFile = contextProject.getFile(extractedTypeName + ".java");
		}
		else if(contextContainer instanceof IFolder) {
			IFolder contextFolder = (IFolder)contextContainer;
			extractedClassFile = contextFolder.getFile(extractedTypeName + ".java");
		}
		ICompilationUnit extractedClassICompilationUnit = JavaCore.createCompilationUnitFrom(extractedClassFile);
		javaElementsToOpenInEditor.add(extractedClassICompilationUnit);
		ASTParser extractedClassParser = ASTParser.newParser(ASTReader.JLS);
		extractedClassParser.setKind(ASTParser.K_COMPILATION_UNIT);
		Document extractedClassDocument = new Document();
		extractedClassParser.setSource(extractedClassDocument.get().toCharArray());
		
		CompilationUnit extractedClassCompilationUnit = (CompilationUnit)extractedClassParser.createAST(null);
        AST extractedClassAST = extractedClassCompilationUnit.getAST();
        ASTRewrite extractedClassRewriter = ASTRewrite.create(extractedClassAST);
        ListRewrite extractedClassTypesRewrite = extractedClassRewriter.getListRewrite(extractedClassCompilationUnit, CompilationUnit.TYPES_PROPERTY);

        if(sourceCompilationUnit.getPackage() != null) {
        	extractedClassRewriter.set(extractedClassCompilationUnit, CompilationUnit.PACKAGE_PROPERTY, sourceCompilationUnit.getPackage(), null);
        }
        TypeDeclaration extractedClassTypeDeclaration = extractedClassAST.newTypeDeclaration();
        SimpleName extractedClassName = extractedClassAST.newSimpleName(extractedTypeName);
        extractedClassRewriter.set(extractedClassTypeDeclaration, TypeDeclaration.NAME_PROPERTY, extractedClassName, null);
        ListRewrite extractedClassModifiersRewrite = extractedClassRewriter.getListRewrite(extractedClassTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
        extractedClassModifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
        
        ListRewrite extractedClassImplementedInterfacesRewrite = extractedClassRewriter.getListRewrite(extractedClassTypeDeclaration, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY);
        ITypeBinding serializableTypeBinding = implementsSerializableInterface(sourceTypeDeclaration.resolveBinding());
        if(serializableTypeBinding != null) {
        	Type serializableType = extractedClassAST.newSimpleType(extractedClassAST.newName(serializableTypeBinding.getName()));
        	extractedClassImplementedInterfacesRewrite.insertLast(serializableType, null);
        	requiredImportDeclarationsInExtractedClass.add(serializableTypeBinding);
        }

        ListRewrite extractedClassBodyRewrite = extractedClassRewriter.getListRewrite(extractedClassTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        Set<VariableDeclaration> finalFieldFragments = new LinkedHashSet<VariableDeclaration>();
        Set<VariableDeclaration> finalFieldFragmentsWithoutInitializer = new LinkedHashSet<VariableDeclaration>();
        for(VariableDeclaration fieldFragment : extractedFieldFragments) {
        	List<Expression> initializerThisExpressions = expressionExtractor.getThisExpressions(fieldFragment.getInitializer());
        	FieldDeclaration extractedFieldDeclaration = null;
        	if(initializerThisExpressions.isEmpty()) {
        		IVariableBinding staticFieldInitializer = staticFieldInitializer(fieldFragment);
				if(staticFieldInitializer != null && !variableBindingInExtractedFields(staticFieldInitializer)) {
					VariableDeclarationFragment fragment = extractedClassAST.newVariableDeclarationFragment();
	        		extractedClassRewriter.set(fragment, VariableDeclarationFragment.NAME_PROPERTY, extractedClassAST.newSimpleName(fieldFragment.getName().getIdentifier()), null);
	        		ITypeBinding staticFieldDeclaringClassTypeBinding = staticFieldInitializer.getDeclaringClass();
	        		QualifiedName qualifiedName = extractedClassAST.newQualifiedName(extractedClassAST.newSimpleName(staticFieldDeclaringClassTypeBinding.getName()), extractedClassAST.newSimpleName(staticFieldInitializer.getName()));
	        		extractedClassRewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, qualifiedName, null);
	        		extractedFieldDeclaration = extractedClassAST.newFieldDeclaration(fragment);
        		}
        		else {
        			extractedFieldDeclaration = extractedClassAST.newFieldDeclaration((VariableDeclarationFragment)ASTNode.copySubtree(extractedClassAST, fieldFragment));
        		}
        	}
        	else {
        		this.extractedFieldsWithThisExpressionInTheirInitializer.add(fieldFragment);
        		VariableDeclarationFragment fragment = extractedClassAST.newVariableDeclarationFragment();
        		extractedClassRewriter.set(fragment, VariableDeclarationFragment.NAME_PROPERTY, extractedClassAST.newSimpleName(fieldFragment.getName().getIdentifier()), null);
        		extractedFieldDeclaration = extractedClassAST.newFieldDeclaration(fragment);
        	}
        	FieldDeclaration originalFieldDeclaration = (FieldDeclaration)fieldFragment.getParent();
        	extractedClassRewriter.set(extractedFieldDeclaration, FieldDeclaration.TYPE_PROPERTY, originalFieldDeclaration.getType(), null);
    		ListRewrite extractedFieldDeclarationModifiersRewrite = extractedClassRewriter.getListRewrite(extractedFieldDeclaration, FieldDeclaration.MODIFIERS2_PROPERTY);
    		extractedFieldDeclarationModifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD), null);
    		List<IExtendedModifier> originalModifiers = originalFieldDeclaration.modifiers();
    		for(IExtendedModifier extendedModifier : originalModifiers) {
    			if(extendedModifier.isModifier()) {
    				Modifier modifier = (Modifier)extendedModifier;
    				if(modifier.isFinal()) {
    					extractedFieldDeclarationModifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
    					finalFieldFragments.add(fieldFragment);
    					if(fieldFragment.getInitializer() == null)
    						finalFieldFragmentsWithoutInitializer.add(fieldFragment);
    				}
    				else if(modifier.isStatic()) {
    					extractedFieldDeclarationModifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD), null);
    				}
    				else if(modifier.isTransient()) {
    					extractedFieldDeclarationModifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.TRANSIENT_KEYWORD), null);
    				}
    				else if(modifier.isVolatile()) {
    					extractedFieldDeclarationModifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.VOLATILE_KEYWORD), null);
    				}
    			}
    		}
    		extractedClassBodyRewrite.insertLast(extractedFieldDeclaration, null);
        }
        for(MethodDeclaration constructor : constructorFinalFieldAssignmentMap.keySet()) {
        	Map<VariableDeclaration, Assignment> finalFieldAssignmentMap = constructorFinalFieldAssignmentMap.get(constructor);
        	MethodDeclaration extractedClassConstructor = extractedClassAST.newMethodDeclaration();
        	extractedClassConstructor.setConstructor(true);
        	extractedClassRewriter.set(extractedClassConstructor, MethodDeclaration.NAME_PROPERTY, extractedClassName, null);
        	ListRewrite extractedClassConstructorModifiersRewrite = extractedClassRewriter.getListRewrite(extractedClassConstructor, MethodDeclaration.MODIFIERS2_PROPERTY);
        	extractedClassConstructorModifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
        	
        	Block extractedClassConstructorBody = extractedClassAST.newBlock();
        	extractedClassRewriter.set(extractedClassConstructor, MethodDeclaration.BODY_PROPERTY, extractedClassConstructorBody, null);
        	ListRewrite extractedClassConstructorBodyStatementsRewrite = extractedClassRewriter.getListRewrite(extractedClassConstructorBody, Block.STATEMENTS_PROPERTY);
        	ListRewrite extractedClassConstructorParametersRewrite = extractedClassRewriter.getListRewrite(extractedClassConstructor, MethodDeclaration.PARAMETERS_PROPERTY);
        	Set<VariableDeclaration> extractedClassConstructorParameters = extractedClassConstructorParameterMap.get(constructor);
        	for(VariableDeclaration variableDeclaration : extractedClassConstructorParameters) {
        		Type parameterType = null;
        		if(variableDeclaration instanceof SingleVariableDeclaration) {
        			parameterType = ((SingleVariableDeclaration)variableDeclaration).getType();
        		}
        		else if(variableDeclaration instanceof VariableDeclarationFragment) {
        			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclaration.getParent();
        			parameterType = variableDeclarationStatement.getType();
        		}
        		SingleVariableDeclaration constructorParameter = extractedClassAST.newSingleVariableDeclaration();
        		extractedClassRewriter.set(constructorParameter, SingleVariableDeclaration.TYPE_PROPERTY, parameterType, null);
        		extractedClassRewriter.set(constructorParameter, SingleVariableDeclaration.NAME_PROPERTY, variableDeclaration.getName(), null);
        		extractedClassConstructorParametersRewrite.insertLast(constructorParameter, null);
        	}
        	Set<ITypeBinding> thrownExceptionTypeBindings = new LinkedHashSet<ITypeBinding>();
        	for(VariableDeclaration fieldFragment : finalFieldAssignmentMap.keySet()) {
        		Assignment fieldAssignment = finalFieldAssignmentMap.get(fieldFragment);
        		Assignment newFieldAssignment = extractedClassAST.newAssignment();
        		extractedClassRewriter.set(newFieldAssignment, Assignment.LEFT_HAND_SIDE_PROPERTY, fieldAssignment.getLeftHandSide(), null);
        		extractedClassRewriter.set(newFieldAssignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, fieldAssignment.getRightHandSide(), null);
        		extractedClassRewriter.set(newFieldAssignment, Assignment.OPERATOR_PROPERTY, fieldAssignment.getOperator(), null);
        		ExpressionStatement assignmentStatement = extractedClassAST.newExpressionStatement(newFieldAssignment);
        		extractedClassConstructorBodyStatementsRewrite.insertLast(assignmentStatement, null);
        		ThrownExceptionVisitor thrownExceptionVisitor = new ThrownExceptionVisitor();
        		fieldAssignment.getRightHandSide().accept(thrownExceptionVisitor);
        		thrownExceptionTypeBindings.addAll(thrownExceptionVisitor.getTypeBindings());
        	}
        	RefactoringUtility.getSimpleTypeBindings(thrownExceptionTypeBindings, requiredImportDeclarationsInExtractedClass);
        	ListRewrite constructorThrownExceptionsRewrite = extractedClassRewriter.getListRewrite(extractedClassConstructor, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
        	for(ITypeBinding thrownExceptionTypeBinding : thrownExceptionTypeBindings) {
        		constructorThrownExceptionsRewrite.insertLast(extractedClassAST.newSimpleName(thrownExceptionTypeBinding.getName()), null);
        	}
        	extractedClassBodyRewrite.insertLast(extractedClassConstructor, null);
        }
        for(VariableDeclaration fieldFragment : extractedFieldFragments) {
        	if(accessedFieldsInNonExtractedMethods.contains(fieldFragment)) {
        		MethodDeclaration getterMethodDeclaration = createGetterMethodDeclaration(fieldFragment, extractedClassAST, extractedClassRewriter);
        		extractedClassBodyRewrite.insertLast(getterMethodDeclaration, null);
        	}
        	if(modifiedFieldsInNonExtractedMethods.contains(fieldFragment) && !finalFieldFragments.contains(fieldFragment)) {
        		MethodDeclaration setterMethodDeclaration = createSetterMethodDeclaration(fieldFragment, extractedClassAST, extractedClassRewriter);
        		extractedClassBodyRewrite.insertLast(setterMethodDeclaration, null);
        	}
        }
        for(MethodDeclaration method : extractedMethods) {
        	MethodDeclaration extractedMethodDeclaration = createExtractedMethodDeclaration(method, extractedClassAST, extractedClassRewriter);
        	extractedClassBodyRewrite.insertLast(extractedMethodDeclaration, null);
        }
        Map<MethodDeclaration, Integer> levelMap = new LinkedHashMap<MethodDeclaration, Integer>();
        //create adjacency list
        AdjacencyList adjacencyList = new AdjacencyList();
        for(MethodDeclaration method : extractedMethods) {
        	if(oldMethodInvocationsWithinExtractedMethods.containsKey(method)) {
        		levelMap.put(method, -1);
        		for(MethodInvocation methodInvocation : oldMethodInvocationsWithinExtractedMethods.get(method)) {
        			//exclude recursive invocations
    				if(!method.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
    					Node source = new Node(method.resolveBinding().getKey());
    					Node target = new Node(methodInvocation.resolveMethodBinding().getKey());
    					adjacencyList.addEdge(source, target, 0);
    				}
            	}
        	}
        	else
        		levelMap.put(method, 0);
        }
        TarjanAlgorithm tarjan = new TarjanAlgorithm(adjacencyList);
        while(!allExtractedMethodsObtainedLevel(levelMap)) {
        	for(MethodDeclaration method : extractedMethods) {
        		if(levelMap.get(method) == -1) {
        			Set<MethodInvocation> methodInvocations = oldMethodInvocationsWithinExtractedMethods.get(method);
        			int maxLevel = 0;
        			boolean dependsOnMethodWithoutLevel = false;
        			for(MethodInvocation methodInvocation : methodInvocations) {
        				//exclude recursive invocations
        				if(!method.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
        					MethodDeclaration invokedMethod = getExtractedMethod(methodInvocation.resolveMethodBinding());
        					int invokedMethodLevel = levelMap.get(invokedMethod);
        					if(invokedMethodLevel == -1) {
        						boolean belongToTheSameStronglyConnectedComponent = tarjan.belongToTheSameStronglyConnectedComponent(
        								method.resolveBinding().getKey(), methodInvocation.resolveMethodBinding().getKey());
        						if(belongToTheSameStronglyConnectedComponent) {
        							double sourceAverageLevelOfTargets = getAverageLevelOfTargets(method.resolveBinding().getKey(), levelMap, adjacencyList);
        							double targetAverageLevelOfTargets = getAverageLevelOfTargets(methodInvocation.resolveMethodBinding().getKey(), levelMap, adjacencyList);
        							if(sourceAverageLevelOfTargets > targetAverageLevelOfTargets) {
        								dependsOnMethodWithoutLevel = true;
            							break;
        							}
        						}
        						else {
        							dependsOnMethodWithoutLevel = true;
        							break;
        						}
        					}
        					else {
        						if(invokedMethodLevel > maxLevel)
        							maxLevel = invokedMethodLevel;
        					}
        				}
        			}
        			if(!dependsOnMethodWithoutLevel) {
        				levelMap.put(method, maxLevel + 1);
        			}
        		}
        	}
        }
        Set<MethodDeclaration> sortedMethods = new LinkedHashSet<MethodDeclaration>();
        int min = 0;
        while(!levelMap.isEmpty()) {
        	for(MethodDeclaration method : extractedMethods) {
        		if(levelMap.containsKey(method)) {
        			int level = levelMap.get(method);
        			if(level == min) {
        				levelMap.remove(method);
        				if(level > 0)
        					sortedMethods.add(method);
        			}
        		}
        	}
        	min++;
        }
        for(MethodDeclaration oldMethod : sortedMethods) {
        	Map<PlainVariable, SingleVariableDeclaration> fieldParameterMap = new LinkedHashMap<PlainVariable, SingleVariableDeclaration>();
    		Map<PlainVariable, Boolean> fieldParameterFinalMap = new LinkedHashMap<PlainVariable, Boolean>();
    		SingleVariableDeclaration sourceClassParameter = null;
    		boolean sourceClassParameterShouldBeFinal = false;
        	
        	Set<MethodInvocation> oldMethodInvocations = oldMethodInvocationsWithinExtractedMethods.get(oldMethod);
        	MethodDeclaration newMethod = oldToNewExtractedMethodDeclarationMap.get(oldMethod);
        	List<MethodInvocation> newMethodInvocations = new ArrayList<MethodInvocation>(newMethodInvocationsWithinExtractedMethods.get(newMethod));
        	Set<PlainVariable> additionalArgumentsForInvokerMethod = additionalArgumentsAddedToExtractedMethods.get(oldMethod);
        	Set<SingleVariableDeclaration> additionalParametersForInvokerMethod = additionalParametersAddedToExtractedMethods.get(oldMethod);
        	int i = 0;
        	for(MethodInvocation oldMethodInvocation : oldMethodInvocations) {
        		if(oldMethodInvocation.getExpression() == null || oldMethodInvocation.getExpression() instanceof ThisExpression) {
        			//invocation without expression
        			if(!oldMethod.resolveBinding().isEqualTo(oldMethodInvocation.resolveMethodBinding())) {
        				//non-recursive
        				MethodDeclaration oldExtractedInvokedMethod = getExtractedMethod(oldMethodInvocation.resolveMethodBinding());
        				Set<PlainVariable> additionalArgumentsForExtractedInvokedMethod = additionalArgumentsAddedToExtractedMethods.get(oldExtractedInvokedMethod);
        				for(PlainVariable additionalArgument : additionalArgumentsForExtractedInvokedMethod) {
        					if(isParentAnonymousClassDeclaration(oldMethodInvocation)) {
        						if(isThisVariable(additionalArgument))
        							sourceClassParameterShouldBeFinal = true;
        						else
        							fieldParameterFinalMap.put(additionalArgument, true);
        					}
        					if(!additionalArgumentsForInvokerMethod.contains(additionalArgument)) {
        						if(isThisVariable(additionalArgument)) {
        							sourceClassParameter = addSourceClassParameterToMovedMethod(newMethod, extractedClassRewriter);
        							addThisVariable(additionalArgumentsForInvokerMethod);
        							additionalParametersForInvokerMethod.add(sourceClassParameter);
        						}
        						else {
        							SingleVariableDeclaration fieldParameter = addParameterToMovedMethod(newMethod, additionalArgument, extractedClassRewriter);
    								additionalArgumentsForInvokerMethod.add(additionalArgument);
    								additionalParametersForInvokerMethod.add(fieldParameter);
    								fieldParameterMap.put(additionalArgument, fieldParameter);
        						}
        					}
        					MethodInvocation newMethodInvocation = newMethodInvocations.get(i);
        					ListRewrite argumentsRewrite = extractedClassRewriter.getListRewrite(newMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
        					if(isThisVariable(additionalArgument)) {
        						String sourceTypeName = sourceTypeDeclaration.getName().getIdentifier();
        						String modifiedSourceTypeName = sourceTypeName.substring(0,1).toLowerCase() + sourceTypeName.substring(1,sourceTypeName.length());
        						SimpleName parameterName = extractedClassAST.newSimpleName(modifiedSourceTypeName);
        						argumentsRewrite.insertLast(parameterName, null);
        					}
        					else {
        						if(additionalArgument.isField()) {
        							//adding "this" prefix to avoid collisions with other parameter names
        							String parameterName = createNameForParameterizedFieldAccess(additionalArgument.getVariableName());
        							argumentsRewrite.insertLast(extractedClassAST.newSimpleName(parameterName), null);
        						}
        						else {
        							argumentsRewrite.insertLast(extractedClassAST.newSimpleName(additionalArgument.getVariableName()), null);
        						}
        					}
        				}
        			}
        		}
        		else {
        			//invocation with expression
        			MethodDeclaration oldExtractedInvokedMethod = getExtractedMethod(oldMethodInvocation.resolveMethodBinding());
        			delegateMethods.add(oldExtractedInvokedMethod);
        		}
        		i++;
        	}
        	if(sourceClassParameterShouldBeFinal) {
        		if(sourceClassParameter != null) {
        			ListRewrite modifiersRewrite = extractedClassRewriter.getListRewrite(sourceClassParameter, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
        			modifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
        		}
        		else {
        			int j = 0;
        			List<SingleVariableDeclaration> additionalParametersForInvokerMethodList = new ArrayList<SingleVariableDeclaration>(additionalParametersForInvokerMethod);
        			SingleVariableDeclaration additionalParameter = null;
        			for(PlainVariable additionalArgument : additionalArgumentsForInvokerMethod) {
        				if(isThisVariable(additionalArgument)) {
        					additionalParameter = additionalParametersForInvokerMethodList.get(j);
        					break;
        				}
        				j++;
        			}
        			ListRewrite modifiersRewrite = extractedClassRewriter.getListRewrite(additionalParameter, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
        			modifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
        		}
    		}
    		for(PlainVariable fieldName : fieldParameterFinalMap.keySet()) {
    			if(fieldParameterFinalMap.get(fieldName) == true) {
    				SingleVariableDeclaration fieldParameter = fieldParameterMap.get(fieldName);
    				if(fieldParameter != null) {
    					ListRewrite modifiersRewrite = extractedClassRewriter.getListRewrite(fieldParameter, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
    					modifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
    				}
    				else {
    					int j = 0;
            			List<SingleVariableDeclaration> additionalParametersForInvokerMethodList = new ArrayList<SingleVariableDeclaration>(additionalParametersForInvokerMethod);
            			SingleVariableDeclaration additionalParameter = null;
            			for(PlainVariable additionalArgument : additionalArgumentsForInvokerMethod) {
            				if(additionalArgument.equals(fieldName)) {
            					additionalParameter = additionalParametersForInvokerMethodList.get(j);
            					break;
            				}
            				j++;
            			}
            			ListRewrite modifiersRewrite = extractedClassRewriter.getListRewrite(additionalParameter, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
            			modifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
    				}
    			}
    		}
        }
        //handle recursive extracted method invocations
        for(MethodDeclaration oldMethod : oldMethodInvocationsWithinExtractedMethods.keySet()) {
        	Set<MethodInvocation> oldMethodInvocations = oldMethodInvocationsWithinExtractedMethods.get(oldMethod);
        	MethodDeclaration newMethod = oldToNewExtractedMethodDeclarationMap.get(oldMethod);
        	List<MethodInvocation> newMethodInvocations = new ArrayList<MethodInvocation>(newMethodInvocationsWithinExtractedMethods.get(newMethod));
        	Set<PlainVariable> additionalArgumentsForInvokerMethod = additionalArgumentsAddedToExtractedMethods.get(oldMethod);
        	int i = 0;
        	for(MethodInvocation oldMethodInvocation : oldMethodInvocations) {
        		if(oldMethodInvocation.getExpression() == null || oldMethodInvocation.getExpression() instanceof ThisExpression) {
        			//invocation without expression
        			if(oldMethod.resolveBinding().isEqualTo(oldMethodInvocation.resolveMethodBinding())) {
        				//recursive invocation
        				for(PlainVariable additionalArgument : additionalArgumentsForInvokerMethod) {
        					MethodInvocation newMethodInvocation = newMethodInvocations.get(i);
        					ListRewrite argumentsRewrite = extractedClassRewriter.getListRewrite(newMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
        					if(isThisVariable(additionalArgument)) {
        						String sourceTypeName = sourceTypeDeclaration.getName().getIdentifier();
        						String modifiedSourceTypeName = sourceTypeName.substring(0,1).toLowerCase() + sourceTypeName.substring(1,sourceTypeName.length());
        						SimpleName parameterName = extractedClassAST.newSimpleName(modifiedSourceTypeName);
        						argumentsRewrite.insertLast(parameterName, null);
        					}
        					else {
        						if(additionalArgument.isField()) {
        							//adding "this" prefix to avoid collisions with other parameter names
        							String parameterName = createNameForParameterizedFieldAccess(additionalArgument.getVariableName());
        							argumentsRewrite.insertLast(extractedClassAST.newSimpleName(parameterName), null);
        						}
        						else {
        							argumentsRewrite.insertLast(extractedClassAST.newSimpleName(additionalArgument.getVariableName()), null);
        						}
        					}
        				}
        			}
        		}
        		i++;
        	}
        }
        MethodDeclaration cloneMethod = createCloneMethod(extractedClassRewriter, extractedClassAST);
        if(cloneMethod != null) {
        	extractedClassBodyRewrite.insertLast(cloneMethod, null);
        	ITypeBinding cloneableInterfaceTypeBinding = implementsCloneableInterface(sourceTypeDeclaration.resolveBinding());
        	if(cloneableInterfaceTypeBinding != null) {
        		Type cloneableInterfaceType = RefactoringUtility.generateTypeFromTypeBinding(cloneableInterfaceTypeBinding, extractedClassAST, extractedClassRewriter);
            	extractedClassImplementedInterfacesRewrite.insertLast(cloneableInterfaceType, null);
            	Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
            	typeBindings.add(cloneableInterfaceTypeBinding);
            	RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportDeclarationsInExtractedClass);
        	}
        	else {
        		SimpleType cloneableType = extractedClassAST.newSimpleType(extractedClassAST.newSimpleName("Cloneable"));
        		extractedClassImplementedInterfacesRewrite.insertLast(cloneableType, null);
        	}
        }
        extractedClassTypesRewrite.insertLast(extractedClassTypeDeclaration, null);

        try {
        	for(ITypeBinding typeBinding : requiredImportDeclarationsInExtractedClass) {
				addImportDeclaration(typeBinding, extractedClassCompilationUnit, extractedClassRewriter);
			}
        	for(IMethodBinding methodBinding : staticallyImportedMethods) {
        		addStaticImportDeclaration(methodBinding, extractedClassCompilationUnit, extractedClassRewriter);
        	}
        	TextEdit extractedClassEdit = extractedClassRewriter.rewriteAST(extractedClassDocument, null);
        	extractedClassEdit.apply(extractedClassDocument);
        	CreateCompilationUnitChange createCompilationUnitChange =
        		new CreateCompilationUnitChange(extractedClassICompilationUnit, extractedClassDocument.get(), extractedClassFile.getCharset());
        	createCompilationUnitChanges.put(extractedClassICompilationUnit, createCompilationUnitChange);
        } catch (CoreException e) {
        	e.printStackTrace();
        } catch (MalformedTreeException e) {
        	e.printStackTrace();
        } catch (BadLocationException e) {
        	e.printStackTrace();
        }
	}

	private MethodDeclaration createCloneMethod(ASTRewrite extractedClassRewriter, AST extractedClassAST) {
		// check if source class contains clone method
		IMethodBinding cloneMethodBinding = null;
		MethodDeclaration[] methodDeclarations = sourceTypeDeclaration.getMethods();
		for(MethodDeclaration method : methodDeclarations) {
			if(isClone(method)) {
				cloneMethodBinding = method.resolveBinding();
				break;
			}
		}
		if(cloneMethodBinding == null) {
			cloneMethodBinding = findCloneMethod(sourceTypeDeclaration.resolveBinding().getSuperclass());
		}
		if(cloneMethodBinding != null) {
			MethodDeclaration cloneMethodDeclaration = extractedClassAST.newMethodDeclaration();
			extractedClassRewriter.set(cloneMethodDeclaration, MethodDeclaration.NAME_PROPERTY,
					extractedClassAST.newSimpleName(cloneMethodBinding.getName()), null);
			extractedClassRewriter.set(cloneMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY,
					RefactoringUtility.generateTypeFromTypeBinding(cloneMethodBinding.getReturnType(), extractedClassAST, extractedClassRewriter), null);
			
			ListRewrite modifiersRewrite = extractedClassRewriter.getListRewrite(cloneMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
			modifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
			
			ListRewrite thrownExceptionTypesRewrite = extractedClassRewriter.getListRewrite(cloneMethodDeclaration, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
			ITypeBinding[] thrownExceptionTypes = cloneMethodBinding.getExceptionTypes();
			Set<ITypeBinding> thrownExceptionTypeBindings = new LinkedHashSet<ITypeBinding>();
			for(ITypeBinding typeBinding : thrownExceptionTypes) {
				Name type = extractedClassAST.newSimpleName(typeBinding.getName());
				thrownExceptionTypesRewrite.insertLast(type, null);
				thrownExceptionTypeBindings.add(typeBinding);
			}
			RefactoringUtility.getSimpleTypeBindings(thrownExceptionTypeBindings, requiredImportDeclarationsInExtractedClass);
			Block body = extractedClassAST.newBlock();
			extractedClassRewriter.set(cloneMethodDeclaration, MethodDeclaration.BODY_PROPERTY, body, null);
			
			ListRewrite statementsRewrite = extractedClassRewriter.getListRewrite(body, Block.STATEMENTS_PROPERTY);
			ReturnStatement returnStatement = extractedClassAST.newReturnStatement();
			SuperMethodInvocation superCloneMethodInvocation = extractedClassAST.newSuperMethodInvocation();
			extractedClassRewriter.set(superCloneMethodInvocation, SuperMethodInvocation.NAME_PROPERTY,
					extractedClassAST.newSimpleName(cloneMethodBinding.getName()), null);
			CastExpression castExpression = extractedClassAST.newCastExpression();
			extractedClassRewriter.set(castExpression, CastExpression.EXPRESSION_PROPERTY, superCloneMethodInvocation, null);
			extractedClassRewriter.set(castExpression, CastExpression.TYPE_PROPERTY, extractedClassAST.newSimpleName(extractedTypeName), null);
			extractedClassRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, castExpression, null);
			if(thrownExceptionTypes.length == 0) {
				TryStatement tryStatement = extractedClassAST.newTryStatement();
				Block tryBody = extractedClassAST.newBlock();
				extractedClassRewriter.set(tryStatement, TryStatement.BODY_PROPERTY, tryBody, null);
				ListRewrite tryStatementsRewrite = extractedClassRewriter.getListRewrite(tryBody, Block.STATEMENTS_PROPERTY);
				tryStatementsRewrite.insertLast(returnStatement, null);
				ListRewrite catchClausesRewrite = extractedClassRewriter.getListRewrite(tryStatement, TryStatement.CATCH_CLAUSES_PROPERTY);
				CatchClause catchClause = extractedClassAST.newCatchClause();
				Type catchClauseExceptionType = extractedClassAST.newSimpleType(extractedClassAST.newSimpleName("CloneNotSupportedException"));
				SingleVariableDeclaration singleVariableDeclaration = extractedClassAST.newSingleVariableDeclaration();
				extractedClassRewriter.set(singleVariableDeclaration, SingleVariableDeclaration.TYPE_PROPERTY, catchClauseExceptionType, null);
				SimpleName exceptionName = extractedClassAST.newSimpleName("e");
				extractedClassRewriter.set(singleVariableDeclaration, SingleVariableDeclaration.NAME_PROPERTY, exceptionName, null);
				extractedClassRewriter.set(catchClause, CatchClause.EXCEPTION_PROPERTY, singleVariableDeclaration, null);
				Block catchBody = extractedClassAST.newBlock();
				extractedClassRewriter.set(catchClause, CatchClause.BODY_PROPERTY, catchBody, null);
				ListRewrite catchStatementsRewrite = extractedClassRewriter.getListRewrite(catchBody, Block.STATEMENTS_PROPERTY);
				ThrowStatement throwStatement = extractedClassAST.newThrowStatement();
				ClassInstanceCreation classInstanceCreation = extractedClassAST.newClassInstanceCreation();
				Type throwExceptionType = extractedClassAST.newSimpleType(extractedClassAST.newSimpleName("InternalError"));
				extractedClassRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY, throwExceptionType, null);
				ListRewrite argumentsRewrite = extractedClassRewriter.getListRewrite(classInstanceCreation, ClassInstanceCreation.ARGUMENTS_PROPERTY);
				argumentsRewrite.insertLast(exceptionName, null);
				extractedClassRewriter.set(throwStatement, ThrowStatement.EXPRESSION_PROPERTY, classInstanceCreation, null);
				catchStatementsRewrite.insertLast(throwStatement, null);
				catchClausesRewrite.insertLast(catchClause, null);
				statementsRewrite.insertLast(tryStatement, null);
			}
			else {
				statementsRewrite.insertLast(returnStatement, null);
			}
			return cloneMethodDeclaration;
		}
		return null;
	}

	private double getAverageLevelOfTargets(String methodBindingKey, Map<MethodDeclaration, Integer> levelMap, AdjacencyList adjacency) {
		Node n = new Node(methodBindingKey);
		LinkedHashSet<Edge> edges = adjacency.getAdjacent(n);
		int levelSum = 0;
		int targetSum = 0;
		for(Edge edge : edges) {
			Node target = edge.getTarget();
			for(MethodDeclaration methodDeclaration : levelMap.keySet()) {
				int level = levelMap.get(methodDeclaration);
				if(methodDeclaration.resolveBinding().getKey().equals(target.getName())) {
					if(level != -1) {
						levelSum += level;
						targetSum++;
					}
					break;
				}
			}
		}
		if(targetSum == 0)
			return Double.MAX_VALUE;
		else
			return (double)levelSum/(double)targetSum;
	}

	private boolean allExtractedMethodsObtainedLevel(Map<MethodDeclaration, Integer> levelMap) {
		for(MethodDeclaration method : levelMap.keySet()) {
			if(levelMap.get(method) == -1)
				return false;
		}
		return true;
	}

	private MethodDeclaration getExtractedMethod(IMethodBinding methodBinding) {
		for(MethodDeclaration extractedMethod : extractedMethods) {
			if(extractedMethod.resolveBinding().isEqualTo(methodBinding))
				return extractedMethod;
		}
		return null;
	}

	private MethodDeclaration createExtractedMethodDeclaration(MethodDeclaration extractedMethod, AST extractedClassAST, ASTRewrite extractedClassRewriter) {
		MethodDeclaration newMethodDeclaration = (MethodDeclaration)ASTNode.copySubtree(extractedClassAST, extractedMethod);
		
		extractedClassRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, 
				extractedClassAST.newSimpleName(extractedMethod.getName().getIdentifier()), null);
		ListRewrite modifierRewrite = extractedClassRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
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
			else if(extendedModifier.isAnnotation()) {
				Annotation annotation = (Annotation)extendedModifier;
				if(annotation instanceof MarkerAnnotation && annotation.getTypeName().getFullyQualifiedName().equals("Test")) {
					modifierRewrite.remove(annotation, null);
				}
			}
		}
		if(!modifierFound) {
			modifierRewrite.insertFirst(publicModifier, null);
		}
		
		modifySourceMemberAccessesInTargetClass(extractedMethod, newMethodDeclaration, extractedClassRewriter);
		modifySourceStaticFieldInstructionsInTargetClass(extractedMethod, newMethodDeclaration, extractedClassRewriter);
		//check if there is a parameter type collision
		List<SingleVariableDeclaration> originalParameters = extractedMethod.parameters();
		List<SingleVariableDeclaration> newParameters = newMethodDeclaration.parameters();
		int i = 0;
		Set<ITypeBinding> typeBindingsToBeRemoved = new LinkedHashSet<ITypeBinding>();
		for(SingleVariableDeclaration originalParameter : originalParameters) {
			ITypeBinding originalParameterTypeBinding = originalParameter.getType().resolveBinding();
			for(ITypeBinding typeBinding : requiredImportDeclarationsInExtractedClass) {
				if(!originalParameterTypeBinding.isEqualTo(typeBinding) && originalParameterTypeBinding.getName().equals(typeBinding.getName())) {
					Type qualifiedType = RefactoringUtility.generateQualifiedTypeFromTypeBinding(originalParameterTypeBinding, extractedClassAST, extractedClassRewriter);
					extractedClassRewriter.set(newParameters.get(i), SingleVariableDeclaration.TYPE_PROPERTY, qualifiedType, null);
					typeBindingsToBeRemoved.add(originalParameterTypeBinding);
					break;
				}
			}
			i++;
		}
		requiredImportDeclarationsInExtractedClass.removeAll(typeBindingsToBeRemoved);
		return newMethodDeclaration;
	}

	private boolean variableBindingCorrespondsToExtractedField(IVariableBinding variableBinding) {
		for(VariableDeclaration extractedFieldFragment : extractedFieldFragments) {
			if(extractedFieldFragment.resolveBinding().isEqualTo(variableBinding))
				return true;
		}
		return false;
	}

	private boolean methodBindingCorrespondsToExtractedMethod(IMethodBinding methodBinding) {
		for(MethodDeclaration extractedMethod : extractedMethods) {
			if(extractedMethod.resolveBinding().isEqualTo(methodBinding))
				return true;
		}
		return false;
	}

	private boolean isParentAnonymousClassDeclaration(ASTNode node) {
		if(node.getParent() instanceof AnonymousClassDeclaration) {
			return true;
		}
		else if(node.getParent() instanceof CompilationUnit) {
			return false;
		}
		else {
			return isParentAnonymousClassDeclaration(node.getParent());
		}
	}

	private IMethodBinding findSetterMethodInSourceClass(IVariableBinding fieldBinding) {
		TypeDeclaration typeDeclaration = RefactoringUtility.findDeclaringTypeDeclaration(fieldBinding, sourceTypeDeclaration);
		if(typeDeclaration != null) {
			MethodDeclaration[] contextMethods = typeDeclaration.getMethods();
			for(MethodDeclaration methodDeclaration : contextMethods) {
				SimpleName simpleName = MethodDeclarationUtility.isSetter(methodDeclaration);
				if(simpleName != null && simpleName.resolveBinding().isEqualTo(fieldBinding)) {
					return methodDeclaration.resolveBinding();
				}
			}
		}
		return null;
	}

	private IMethodBinding findGetterMethodInSourceClass(IVariableBinding fieldBinding) {
		TypeDeclaration typeDeclaration = RefactoringUtility.findDeclaringTypeDeclaration(fieldBinding, sourceTypeDeclaration);
		if(typeDeclaration != null) {
			MethodDeclaration[] contextMethods = typeDeclaration.getMethods();
			for(MethodDeclaration methodDeclaration : contextMethods) {
				SimpleName simpleName = MethodDeclarationUtility.isGetter(methodDeclaration);
				if(simpleName != null && simpleName.resolveBinding().isEqualTo(fieldBinding)) {
					return methodDeclaration.resolveBinding();
				}
			}
		}
		return null;
	}

	private void createSetterMethodInSourceClass(IVariableBinding variableBinding) {
		FieldDeclaration[] fieldDeclarations = sourceTypeDeclaration.getFields();
		for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
			List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				if(variableBinding.isEqualTo(fragment.resolveBinding())) {
					ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
					AST contextAST = sourceTypeDeclaration.getAST();
					MethodDeclaration newMethodDeclaration = contextAST.newMethodDeclaration();
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, contextAST.newPrimitiveType(PrimitiveType.VOID), null);
					ListRewrite methodDeclarationModifiersRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
					methodDeclarationModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
					String methodName = fragment.getName().getIdentifier();
					methodName = SETTER_PREFIX + methodName.substring(0,1).toUpperCase() + methodName.substring(1,methodName.length());
					methodName = appendAccessorMethodSuffix(methodName, sourceTypeDeclaration.getMethods());
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
					try {
						TextEdit sourceEdit = sourceRewriter.rewriteAST();
						ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
						CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
						change.getEdit().addChild(sourceEdit);
						change.addTextEditGroup(new TextEditGroup("Create setter method for field " + variableBinding.getName(), new TextEdit[] {sourceEdit}));
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void createGetterMethodInSourceClass(IVariableBinding variableBinding) {
		FieldDeclaration[] fieldDeclarations = sourceTypeDeclaration.getFields();
		for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
			List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				if(variableBinding.isEqualTo(fragment.resolveBinding())) {
					ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
					AST contextAST = sourceTypeDeclaration.getAST();
					MethodDeclaration newMethodDeclaration = contextAST.newMethodDeclaration();
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, fieldDeclaration.getType(), null);
					ListRewrite methodDeclarationModifiersRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
					methodDeclarationModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
					String methodName = fragment.getName().getIdentifier();
					methodName = GETTER_PREFIX + methodName.substring(0,1).toUpperCase() + methodName.substring(1,methodName.length());
					methodName = appendAccessorMethodSuffix(methodName, sourceTypeDeclaration.getMethods());
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, contextAST.newSimpleName(methodName), null);
					Block methodDeclarationBody = contextAST.newBlock();
					ListRewrite methodDeclarationBodyStatementsRewrite = sourceRewriter.getListRewrite(methodDeclarationBody, Block.STATEMENTS_PROPERTY);
					ReturnStatement returnStatement = contextAST.newReturnStatement();
					sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, fragment.getName(), null);
					methodDeclarationBodyStatementsRewrite.insertLast(returnStatement, null);
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.BODY_PROPERTY, methodDeclarationBody, null);
					ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
					contextBodyRewrite.insertLast(newMethodDeclaration, null);
					try {
						TextEdit sourceEdit = sourceRewriter.rewriteAST();
						ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
						CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
						change.getEdit().addChild(sourceEdit);
						change.addTextEditGroup(new TextEditGroup("Create getter method for field " + variableBinding.getName(), new TextEdit[] {sourceEdit}));
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void modifySourceMemberAccessesInTargetClass(MethodDeclaration sourceMethod, MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		AST ast = newMethodDeclaration.getAST();
		oldToNewExtractedMethodDeclarationMap.put(sourceMethod, newMethodDeclaration);
		List<Expression> sourceMethodInvocations = expressionExtractor.getMethodInvocations(sourceMethod.getBody());
		List<Expression> newMethodInvocations = expressionExtractor.getMethodInvocations(newMethodDeclaration.getBody());
		
		List<Expression> sourceFieldInstructions = expressionExtractor.getVariableInstructions(sourceMethod.getBody());
		List<Expression> newFieldInstructions = expressionExtractor.getVariableInstructions(newMethodDeclaration.getBody());
		
		List<Expression> sourceAssignments = expressionExtractor.getAssignments(sourceMethod.getBody());
		List<Expression> newAssignments = expressionExtractor.getAssignments(newMethodDeclaration.getBody());
		
		SingleVariableDeclaration sourceClassParameter = null;
		boolean sourceClassParameterShouldBeFinal = false;
		Map<PlainVariable, SingleVariableDeclaration> fieldParameterMap = new LinkedHashMap<PlainVariable, SingleVariableDeclaration>();
		Map<PlainVariable, Boolean> fieldParameterFinalMap = new LinkedHashMap<PlainVariable, Boolean>();
		String sourceTypeName = sourceTypeDeclaration.getName().getIdentifier();
		String modifiedSourceTypeName = sourceTypeName.substring(0,1).toLowerCase() + sourceTypeName.substring(1,sourceTypeName.length());
		SimpleName parameterName = ast.newSimpleName(modifiedSourceTypeName);
		
		int i = 0;
		for(Expression expression : sourceAssignments) {
			Assignment oldAssignment = (Assignment)expression;
			Assignment newAssignment = (Assignment)newAssignments.get(i);
			Expression oldLeftHandSide = oldAssignment.getLeftHandSide();
			Expression newLeftHandSide = newAssignment.getLeftHandSide();
			SimpleName oldAssignedVariable = null;
			SimpleName newAssignedVariable = null;
			if(oldLeftHandSide instanceof SimpleName) {
				oldAssignedVariable = (SimpleName)oldLeftHandSide;
				newAssignedVariable = (SimpleName)newLeftHandSide;
			}
			else if(oldLeftHandSide instanceof FieldAccess) {
				FieldAccess oldFieldAccess = (FieldAccess)oldLeftHandSide;
				oldAssignedVariable = oldFieldAccess.getName();
				FieldAccess newFieldAccess = (FieldAccess)newLeftHandSide;
				newAssignedVariable = newFieldAccess.getName();
			}
			Expression oldRightHandSide = oldAssignment.getRightHandSide();
			Expression newRightHandSide = newAssignment.getRightHandSide();
			if(oldAssignedVariable != null) {
				IBinding binding = oldAssignedVariable.resolveBinding();
				if(binding != null && binding.getKind() == IBinding.VARIABLE) {
					IVariableBinding variableBinding = (IVariableBinding)binding;
					if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) == 0) {
						if(declaredInSourceTypeDeclarationOrSuperclass(variableBinding)) {
							if(!variableBindingCorrespondsToExtractedField(variableBinding)) {
								IMethodBinding setterMethodBinding = findSetterMethodInSourceClass(variableBinding);
								Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
								Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
								if(isParentAnonymousClassDeclaration(oldAssignment))
									sourceClassParameterShouldBeFinal = true;
								if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
									sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
									addThisVariable(additionalArgumentsAddedToMovedMethod);
									additionalParametersAddedToMovedMethod.add(sourceClassParameter);
								}
								MethodInvocation setterMethodInvocation = ast.newMethodInvocation();
								if(setterMethodBinding != null) {
									targetRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(setterMethodBinding.getName()), null);
								}
								else {
									if(!sourceFieldBindingsWithCreatedSetterMethod.contains(variableBinding.getKey())) {
										createSetterMethodInSourceClass(variableBinding);
										sourceFieldBindingsWithCreatedSetterMethod.add(variableBinding.getKey());
									}
									String originalFieldName = variableBinding.getName();
									String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
									String setterMethodName = SETTER_PREFIX + modifiedFieldName;
									setterMethodName = appendAccessorMethodSuffix(setterMethodName, sourceTypeDeclaration.getMethods());
									targetRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(setterMethodName), null);
								}
								ListRewrite setterMethodInvocationArgumentsRewrite = targetRewriter.getListRewrite(setterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
								if(!newAssignment.getOperator().equals(Assignment.Operator.ASSIGN)) {
									IMethodBinding getterMethodBinding = findGetterMethodInSourceClass(variableBinding);
									MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
									if(getterMethodBinding != null) {
										targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodBinding.getName()), null);
									}
									else {
										if(!sourceFieldBindingsWithCreatedGetterMethod.contains(variableBinding.getKey())) {
											createGetterMethodInSourceClass(variableBinding);
											sourceFieldBindingsWithCreatedGetterMethod.add(variableBinding.getKey());
										}
										String originalFieldName = variableBinding.getName();
										String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
										String getterMethodName = GETTER_PREFIX + modifiedFieldName;
										getterMethodName = appendAccessorMethodSuffix(getterMethodName, sourceTypeDeclaration.getMethods());
										targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodName), null);
									}
									targetRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
									InfixExpression infixExpression = ast.newInfixExpression();
									targetRewriter.set(infixExpression, InfixExpression.LEFT_OPERAND_PROPERTY, getterMethodInvocation, null);
									targetRewriter.set(infixExpression, InfixExpression.RIGHT_OPERAND_PROPERTY, newAssignment.getRightHandSide(), null);
									if(newAssignment.getOperator().equals(Assignment.Operator.PLUS_ASSIGN)) {
										targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.PLUS, null);
									}
									else if(newAssignment.getOperator().equals(Assignment.Operator.MINUS_ASSIGN)) {
										targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.MINUS, null);
									}
									else if(newAssignment.getOperator().equals(Assignment.Operator.TIMES_ASSIGN)) {
										targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.TIMES, null);
									}
									else if(newAssignment.getOperator().equals(Assignment.Operator.DIVIDE_ASSIGN)) {
										targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.DIVIDE, null);
									}
									else if(newAssignment.getOperator().equals(Assignment.Operator.REMAINDER_ASSIGN)) {
										targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.REMAINDER, null);
									}
									else if(newAssignment.getOperator().equals(Assignment.Operator.BIT_AND_ASSIGN)) {
										targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.AND, null);
									}
									else if(newAssignment.getOperator().equals(Assignment.Operator.BIT_OR_ASSIGN)) {
										targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.OR, null);
									}
									else if(newAssignment.getOperator().equals(Assignment.Operator.BIT_XOR_ASSIGN)) {
										targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.XOR, null);
									}
									else if(newAssignment.getOperator().equals(Assignment.Operator.LEFT_SHIFT_ASSIGN)) {
										targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.LEFT_SHIFT, null);
									}
									else if(newAssignment.getOperator().equals(Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN)) {
										targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.RIGHT_SHIFT_SIGNED, null);
									}
									else if(newAssignment.getOperator().equals(Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN)) {
										targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED, null);
									}
									setterMethodInvocationArgumentsRewrite.insertLast(infixExpression, null);
								}
								else {
									setterMethodInvocationArgumentsRewrite.insertLast(newAssignment.getRightHandSide(), null);
								}
								targetRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
								targetRewriter.replace(newAssignment, setterMethodInvocation, null);
							}
						}
					}
				}
			}
			else {
				//if an assigned field is not found in left hand side, then replace all accessed fields in left hand side
				int j = 0;
				List<Expression> oldAccessedVariables = expressionExtractor.getVariableInstructions(oldLeftHandSide);
				List<Expression> newAccessedVariables = expressionExtractor.getVariableInstructions(newLeftHandSide);
				for(Expression expression2 : oldAccessedVariables) {
					SimpleName oldAccessedVariable = (SimpleName)expression2;
					SimpleName newAccessedVariable = (SimpleName)newAccessedVariables.get(j);
					IBinding rightHandBinding = oldAccessedVariable.resolveBinding();
					if(rightHandBinding != null && rightHandBinding.getKind() == IBinding.VARIABLE) {
						IVariableBinding accessedVariableBinding = (IVariableBinding)rightHandBinding;
						if(accessedVariableBinding.isField() && (accessedVariableBinding.getModifiers() & Modifier.STATIC) == 0) {
							if(declaredInSourceTypeDeclarationOrSuperclass(accessedVariableBinding)) {
								if(!variableBindingCorrespondsToExtractedField(accessedVariableBinding)) {
									if(sourceFieldBindingsWithCreatedSetterMethod.contains(accessedVariableBinding.getKey())) {
										if(isParentAnonymousClassDeclaration(oldAssignment))
											sourceClassParameterShouldBeFinal = true;
										sourceClassParameter = handleAccessedFieldHavingSetterMethod(
												sourceMethod,
												newMethodDeclaration,
												targetRewriter, ast,
												sourceClassParameter,
												modifiedSourceTypeName,
												newAccessedVariable,
												accessedVariableBinding);
									}
									else {
										if(isParentAnonymousClassDeclaration(oldAccessedVariable))
											fieldParameterFinalMap.put(new PlainVariable(accessedVariableBinding), true);
										handleAccessedFieldNotHavingSetterMethod(
												sourceMethod,
												newMethodDeclaration,
												targetRewriter,
												ast, fieldParameterMap,
												newAccessedVariable,
												accessedVariableBinding);
									}
								}
							}
						}
					}
					j++;
				}
			}
			int j = 0;
			List<Expression> oldAccessedVariables = expressionExtractor.getVariableInstructions(oldRightHandSide);
			List<Expression> newAccessedVariables = expressionExtractor.getVariableInstructions(newRightHandSide);
			for(Expression expression2 : oldAccessedVariables) {
				SimpleName oldAccessedVariable = (SimpleName)expression2;
				SimpleName newAccessedVariable = (SimpleName)newAccessedVariables.get(j);
				IBinding rightHandBinding = oldAccessedVariable.resolveBinding();
				if(rightHandBinding != null && rightHandBinding.getKind() == IBinding.VARIABLE) {
					IVariableBinding accessedVariableBinding = (IVariableBinding)rightHandBinding;
					if(accessedVariableBinding.isField() && (accessedVariableBinding.getModifiers() & Modifier.STATIC) == 0) {
						if(declaredInSourceTypeDeclarationOrSuperclass(accessedVariableBinding)) {
							if(!variableBindingCorrespondsToExtractedField(accessedVariableBinding)) {
								if(sourceFieldBindingsWithCreatedSetterMethod.contains(accessedVariableBinding.getKey())) {
									if(isParentAnonymousClassDeclaration(oldAssignment))
										sourceClassParameterShouldBeFinal = true;
									sourceClassParameter = handleAccessedFieldHavingSetterMethod(
											sourceMethod, newMethodDeclaration,
											targetRewriter, ast,
											sourceClassParameter,
											modifiedSourceTypeName,
											newAccessedVariable,
											accessedVariableBinding);
								}
								else {
									if(isParentAnonymousClassDeclaration(oldAccessedVariable))
										fieldParameterFinalMap.put(new PlainVariable(accessedVariableBinding), true);
									handleAccessedFieldNotHavingSetterMethod(
											sourceMethod, newMethodDeclaration,
											targetRewriter, ast, fieldParameterMap,
											newAccessedVariable, accessedVariableBinding);
								}
							}
						}
					}
				}
				j++;
			}
			i++;
		}
		
		List<Expression> sourcePostfixExpressions = expressionExtractor.getPostfixExpressions(sourceMethod.getBody());
		List<Expression> newPostfixExpressions = expressionExtractor.getPostfixExpressions(newMethodDeclaration.getBody());
		i = 0;
		for(Expression expression : sourcePostfixExpressions) {
			PostfixExpression oldPostfixExpression = (PostfixExpression)expression;
			PostfixExpression newPostfixExpression = (PostfixExpression)newPostfixExpressions.get(i);
			Expression oldOperand = oldPostfixExpression.getOperand();
			Expression newOperand = newPostfixExpression.getOperand();
			SimpleName oldAssignedVariable = null;
			SimpleName newAssignedVariable = null;
			if(oldOperand instanceof SimpleName) {
				oldAssignedVariable = (SimpleName)oldOperand;
				newAssignedVariable = (SimpleName)newOperand;
			}
			else if(oldOperand instanceof FieldAccess) {
				FieldAccess oldFieldAccess = (FieldAccess)oldOperand;
				oldAssignedVariable = oldFieldAccess.getName();
				FieldAccess newFieldAccess = (FieldAccess)newOperand;
				newAssignedVariable = newFieldAccess.getName();
			}
			if(oldAssignedVariable != null) {
				IBinding binding = oldAssignedVariable.resolveBinding();
				if(binding != null && binding.getKind() == IBinding.VARIABLE) {
					IVariableBinding variableBinding = (IVariableBinding)binding;
					if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) == 0) {
						if(declaredInSourceTypeDeclarationOrSuperclass(variableBinding)) {
							if(!variableBindingCorrespondsToExtractedField(variableBinding)) {
								IMethodBinding setterMethodBinding = findSetterMethodInSourceClass(variableBinding);
								Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
								Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
								if(isParentAnonymousClassDeclaration(oldPostfixExpression))
									sourceClassParameterShouldBeFinal = true;
								if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
									sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
									addThisVariable(additionalArgumentsAddedToMovedMethod);
									additionalParametersAddedToMovedMethod.add(sourceClassParameter);
								}
								MethodInvocation setterMethodInvocation = ast.newMethodInvocation();
								if(setterMethodBinding != null) {
									targetRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(setterMethodBinding.getName()), null);
								}
								else {
									if(!sourceFieldBindingsWithCreatedSetterMethod.contains(variableBinding.getKey())) {
										createSetterMethodInSourceClass(variableBinding);
										sourceFieldBindingsWithCreatedSetterMethod.add(variableBinding.getKey());
									}
									String originalFieldName = variableBinding.getName();
									String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
									String setterMethodName = SETTER_PREFIX + modifiedFieldName;
									setterMethodName = appendAccessorMethodSuffix(setterMethodName, sourceTypeDeclaration.getMethods());
									targetRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(setterMethodName), null);
								}
								ListRewrite setterMethodInvocationArgumentsRewrite = targetRewriter.getListRewrite(setterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
								IMethodBinding getterMethodBinding = findGetterMethodInSourceClass(variableBinding);
								MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
								if(getterMethodBinding != null) {
									targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodBinding.getName()), null);
								}
								else {
									if(!sourceFieldBindingsWithCreatedGetterMethod.contains(variableBinding.getKey())) {
										createGetterMethodInSourceClass(variableBinding);
										sourceFieldBindingsWithCreatedGetterMethod.add(variableBinding.getKey());
									}
									String originalFieldName = variableBinding.getName();
									String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
									String getterMethodName = GETTER_PREFIX + modifiedFieldName;
									getterMethodName = appendAccessorMethodSuffix(getterMethodName, sourceTypeDeclaration.getMethods());
									targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodName), null);
								}
								targetRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
								InfixExpression infixExpression = ast.newInfixExpression();
								targetRewriter.set(infixExpression, InfixExpression.LEFT_OPERAND_PROPERTY, getterMethodInvocation, null);
								targetRewriter.set(infixExpression, InfixExpression.RIGHT_OPERAND_PROPERTY, ast.newNumberLiteral("1"), null);
								if(newPostfixExpression.getOperator().equals(PostfixExpression.Operator.INCREMENT)) {
									targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.PLUS, null);
								}
								else if(newPostfixExpression.getOperator().equals(PostfixExpression.Operator.DECREMENT)) {
									targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.MINUS, null);
								}
								setterMethodInvocationArgumentsRewrite.insertLast(infixExpression, null);
								targetRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
								targetRewriter.replace(newPostfixExpression, setterMethodInvocation, null);
							}
						}
					}
				}
			}
			else {
				//if an assigned field is not found in operand, then replace all accessed fields in operand
				int j = 0;
				List<Expression> oldAccessedVariables = expressionExtractor.getVariableInstructions(oldOperand);
				List<Expression> newAccessedVariables = expressionExtractor.getVariableInstructions(newOperand);
				for(Expression expression2 : oldAccessedVariables) {
					SimpleName oldAccessedVariable = (SimpleName)expression2;
					SimpleName newAccessedVariable = (SimpleName)newAccessedVariables.get(j);
					IBinding rightHandBinding = oldAccessedVariable.resolveBinding();
					if(rightHandBinding != null && rightHandBinding.getKind() == IBinding.VARIABLE) {
						IVariableBinding accessedVariableBinding = (IVariableBinding)rightHandBinding;
						if(accessedVariableBinding.isField() && (accessedVariableBinding.getModifiers() & Modifier.STATIC) == 0) {
							if(declaredInSourceTypeDeclarationOrSuperclass(accessedVariableBinding)) {
								if(!variableBindingCorrespondsToExtractedField(accessedVariableBinding)) {
									if(sourceFieldBindingsWithCreatedSetterMethod.contains(accessedVariableBinding.getKey())) {
										if(isParentAnonymousClassDeclaration(oldPostfixExpression))
											sourceClassParameterShouldBeFinal = true;
										sourceClassParameter = handleAccessedFieldHavingSetterMethod(
												sourceMethod,
												newMethodDeclaration,
												targetRewriter, ast,
												sourceClassParameter,
												modifiedSourceTypeName,
												newAccessedVariable,
												accessedVariableBinding);
									}
									else {
										if(isParentAnonymousClassDeclaration(oldAccessedVariable))
											fieldParameterFinalMap.put(new PlainVariable(accessedVariableBinding), true);
										handleAccessedFieldNotHavingSetterMethod(
												sourceMethod,
												newMethodDeclaration,
												targetRewriter,
												ast, fieldParameterMap,
												newAccessedVariable,
												accessedVariableBinding);
									}
								}
							}
						}
					}
					j++;
				}
			}
			i++;
		}
		
		List<Expression> sourcePrefixExpressions = expressionExtractor.getPrefixExpressions(sourceMethod.getBody());
		List<Expression> newPrefixExpressions = expressionExtractor.getPrefixExpressions(newMethodDeclaration.getBody());
		i = 0;
		for(Expression expression : sourcePrefixExpressions) {
			PrefixExpression oldPrefixExpression = (PrefixExpression)expression;
			PrefixExpression newPrefixExpression = (PrefixExpression)newPrefixExpressions.get(i);
			Expression oldOperand = oldPrefixExpression.getOperand();
			Expression newOperand = newPrefixExpression.getOperand();
			Operator oldOperator = oldPrefixExpression.getOperator();
			Operator newOperator = newPrefixExpression.getOperator();
			SimpleName oldAssignedVariable = null;
			SimpleName newAssignedVariable = null;
			if(oldOperand instanceof SimpleName) {
				oldAssignedVariable = (SimpleName)oldOperand;
				newAssignedVariable = (SimpleName)newOperand;
			}
			else if(oldOperand instanceof FieldAccess) {
				FieldAccess oldFieldAccess = (FieldAccess)oldOperand;
				oldAssignedVariable = oldFieldAccess.getName();
				FieldAccess newFieldAccess = (FieldAccess)newOperand;
				newAssignedVariable = newFieldAccess.getName();
			}
			if(oldAssignedVariable != null && (oldOperator.equals(PrefixExpression.Operator.INCREMENT) ||
					oldOperator.equals(PrefixExpression.Operator.DECREMENT))) {
				IBinding binding = oldAssignedVariable.resolveBinding();
				if(binding != null && binding.getKind() == IBinding.VARIABLE) {
					IVariableBinding variableBinding = (IVariableBinding)binding;
					if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) == 0) {
						if(declaredInSourceTypeDeclarationOrSuperclass(variableBinding)) {
							if(!variableBindingCorrespondsToExtractedField(variableBinding)) {
								IMethodBinding setterMethodBinding = findSetterMethodInSourceClass(variableBinding);
								Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
								Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
								if(isParentAnonymousClassDeclaration(oldPrefixExpression))
									sourceClassParameterShouldBeFinal = true;
								if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
									sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
									addThisVariable(additionalArgumentsAddedToMovedMethod);
									additionalParametersAddedToMovedMethod.add(sourceClassParameter);
								}
								MethodInvocation setterMethodInvocation = ast.newMethodInvocation();
								if(setterMethodBinding != null) {
									targetRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(setterMethodBinding.getName()), null);
								}
								else {
									if(!sourceFieldBindingsWithCreatedSetterMethod.contains(variableBinding.getKey())) {
										createSetterMethodInSourceClass(variableBinding);
										sourceFieldBindingsWithCreatedSetterMethod.add(variableBinding.getKey());
									}
									String originalFieldName = variableBinding.getName();
									String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
									String setterMethodName = SETTER_PREFIX + modifiedFieldName;
									setterMethodName = appendAccessorMethodSuffix(setterMethodName, sourceTypeDeclaration.getMethods());
									targetRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(setterMethodName), null);
								}
								ListRewrite setterMethodInvocationArgumentsRewrite = targetRewriter.getListRewrite(setterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
								IMethodBinding getterMethodBinding = findGetterMethodInSourceClass(variableBinding);
								MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
								if(getterMethodBinding != null) {
									targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodBinding.getName()), null);
								}
								else {
									if(!sourceFieldBindingsWithCreatedGetterMethod.contains(variableBinding.getKey())) {
										createGetterMethodInSourceClass(variableBinding);
										sourceFieldBindingsWithCreatedGetterMethod.add(variableBinding.getKey());
									}
									String originalFieldName = variableBinding.getName();
									String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
									String getterMethodName = GETTER_PREFIX + modifiedFieldName;
									getterMethodName = appendAccessorMethodSuffix(getterMethodName, sourceTypeDeclaration.getMethods());
									targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodName), null);
								}
								targetRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
								InfixExpression infixExpression = ast.newInfixExpression();
								targetRewriter.set(infixExpression, InfixExpression.LEFT_OPERAND_PROPERTY, getterMethodInvocation, null);
								targetRewriter.set(infixExpression, InfixExpression.RIGHT_OPERAND_PROPERTY, ast.newNumberLiteral("1"), null);
								if(newOperator.equals(PrefixExpression.Operator.INCREMENT)) {
									targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.PLUS, null);
								}
								else if(newOperator.equals(PrefixExpression.Operator.DECREMENT)) {
									targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.MINUS, null);
								}
								setterMethodInvocationArgumentsRewrite.insertLast(infixExpression, null);
								targetRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
								targetRewriter.replace(newPrefixExpression, setterMethodInvocation, null);
							}
						}
					}
				}
			}
			else {
				//if an assigned field is not found in operand, then replace all accessed fields in operand
				int j = 0;
				List<Expression> oldAccessedVariables = expressionExtractor.getVariableInstructions(oldOperand);
				List<Expression> newAccessedVariables = expressionExtractor.getVariableInstructions(newOperand);
				for(Expression expression2 : oldAccessedVariables) {
					SimpleName oldAccessedVariable = (SimpleName)expression2;
					SimpleName newAccessedVariable = (SimpleName)newAccessedVariables.get(j);
					IBinding rightHandBinding = oldAccessedVariable.resolveBinding();
					if(rightHandBinding != null && rightHandBinding.getKind() == IBinding.VARIABLE) {
						IVariableBinding accessedVariableBinding = (IVariableBinding)rightHandBinding;
						if(accessedVariableBinding.isField() && (accessedVariableBinding.getModifiers() & Modifier.STATIC) == 0) {
							if(declaredInSourceTypeDeclarationOrSuperclass(accessedVariableBinding)) {
								if(!variableBindingCorrespondsToExtractedField(accessedVariableBinding)) {
									if(sourceFieldBindingsWithCreatedSetterMethod.contains(accessedVariableBinding.getKey())) {
										if(isParentAnonymousClassDeclaration(oldPrefixExpression))
											sourceClassParameterShouldBeFinal = true;
										sourceClassParameter = handleAccessedFieldHavingSetterMethod(
												sourceMethod,
												newMethodDeclaration,
												targetRewriter, ast,
												sourceClassParameter,
												modifiedSourceTypeName,
												newAccessedVariable,
												accessedVariableBinding);
									}
									else {
										if(isParentAnonymousClassDeclaration(oldAccessedVariable))
											fieldParameterFinalMap.put(new PlainVariable(accessedVariableBinding), true);
										handleAccessedFieldNotHavingSetterMethod(
												sourceMethod,
												newMethodDeclaration,
												targetRewriter,
												ast, fieldParameterMap,
												newAccessedVariable,
												accessedVariableBinding);
									}
								}
							}
						}
					}
					j++;
				}
			}
			i++;
		}
		
		i = 0;
		for(Expression expression : sourceFieldInstructions) {
			SimpleName simpleName = (SimpleName)expression;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null && binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) == 0) {
					if(declaredInSourceTypeDeclarationOrSuperclass(variableBinding)) {
						if(!variableBindingCorrespondsToExtractedField(variableBinding)) {
							if(!isAssignmentChild(expression)) {
								SimpleName expressionName = (SimpleName)newFieldInstructions.get(i);
								if(sourceFieldBindingsWithCreatedSetterMethod.contains(variableBinding.getKey())) {
									if(isParentAnonymousClassDeclaration(simpleName))
										sourceClassParameterShouldBeFinal = true;
									sourceClassParameter = handleAccessedFieldHavingSetterMethod(
											sourceMethod,
											newMethodDeclaration,
											targetRewriter, ast,
											sourceClassParameter,
											modifiedSourceTypeName,
											expressionName,
											variableBinding);
								}
								else {
									if(isParentAnonymousClassDeclaration(simpleName))
										fieldParameterFinalMap.put(new PlainVariable(variableBinding), true);
									handleAccessedFieldNotHavingSetterMethod(
											sourceMethod, newMethodDeclaration,
											targetRewriter, ast, fieldParameterMap,
											expressionName, variableBinding);
								}
							}
						}
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
									if(!isAssignmentChild(expression)) {
										Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
										Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
										SimpleName expressionName = (SimpleName)newFieldInstructions.get(i);
										if(isParentAnonymousClassDeclaration(simpleName))
											fieldParameterFinalMap.put(new PlainVariable(variableBinding), true);
										if(!containsVariable(variableBinding, additionalArgumentsAddedToMovedMethod)) {
											SingleVariableDeclaration fieldParameter = addParameterToMovedMethod(newMethodDeclaration, variableBinding, targetRewriter);
											addVariable(variableBinding, additionalArgumentsAddedToMovedMethod);
											additionalParametersAddedToMovedMethod.add(fieldParameter);
											fieldParameterMap.put(new PlainVariable(variableBinding), fieldParameter);
										}
									}
								}
							}
						}
					}
				}
			}
			i++;
		}
		
		int j = 0;
		for(Expression expression : sourceMethodInvocations) {
			if(expression instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)expression;
				if(methodInvocation.getExpression() == null || methodInvocation.getExpression() instanceof ThisExpression) {
					IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
					if(methodBinding.getDeclaringClass().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
						MethodDeclaration[] sourceMethodDeclarations = sourceTypeDeclaration.getMethods();
						for(MethodDeclaration sourceMethodDeclaration : sourceMethodDeclarations) {
							if(sourceMethodDeclaration.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
								if(!methodBindingCorrespondsToExtractedMethod(methodInvocation.resolveMethodBinding()) &&
										!sourceMethod.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
									SimpleName fieldName = MethodDeclarationUtility.isGetter(sourceMethodDeclaration);
									Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
									Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
									int modifiers = sourceMethodDeclaration.getModifiers();
									MethodInvocation newMethodInvocation = (MethodInvocation)newMethodInvocations.get(j);
									if((modifiers & Modifier.STATIC) != 0) {
										targetRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier()), null);
										if(!sourceMethodBindingsChangedWithPublicModifier.contains(methodInvocation.resolveMethodBinding().getKey())) {
											setPublicModifierToSourceMethod(methodInvocation.resolveMethodBinding(), sourceTypeDeclaration);
											sourceMethodBindingsChangedWithPublicModifier.add(methodInvocation.resolveMethodBinding().getKey());
										}
									}
									else if(fieldName != null) {
										IVariableBinding fieldBinding = (IVariableBinding)fieldName.resolveBinding();
										if(!variableBindingCorrespondsToExtractedField(fieldBinding)) {
											if((fieldBinding.getModifiers() & Modifier.STATIC) != 0) {
												SimpleName qualifier = ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
												QualifiedName qualifiedName = ast.newQualifiedName(qualifier, ast.newSimpleName(fieldName.getIdentifier()));
												targetRewriter.replace(newMethodInvocation, qualifiedName, null);
												setPublicModifierToSourceField(fieldBinding);
											}
											else {
												String parameterNameString = createNameForParameterizedFieldAccess(fieldName.getIdentifier());
												targetRewriter.replace(newMethodInvocation, ast.newSimpleName(parameterNameString), null);
												//targetRewriter.replace(newMethodInvocation, ast.newSimpleName(fieldName.getIdentifier()), null);
												if(isParentAnonymousClassDeclaration(methodInvocation))
													fieldParameterFinalMap.put(new PlainVariable(fieldBinding), true);
												if(!containsVariable(fieldBinding, additionalArgumentsAddedToMovedMethod)) {
													SingleVariableDeclaration fieldParameter = addParameterToMovedMethod(newMethodDeclaration, fieldBinding, targetRewriter);
													addVariable(fieldBinding, additionalArgumentsAddedToMovedMethod);
													additionalParametersAddedToMovedMethod.add(fieldParameter);
													fieldParameterMap.put(new PlainVariable(fieldBinding), fieldParameter);
												}
											}
										}
										else {
											targetRewriter.replace(newMethodInvocation, ast.newSimpleName(fieldName.getIdentifier()), null);
										}
									}
									else {
										if(isParentAnonymousClassDeclaration(methodInvocation))
											sourceClassParameterShouldBeFinal = true;
										if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
											sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
											addThisVariable(additionalArgumentsAddedToMovedMethod);
											additionalParametersAddedToMovedMethod.add(sourceClassParameter);
										}
										targetRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, parameterName, null);
										if(!sourceMethodBindingsChangedWithPublicModifier.contains(methodInvocation.resolveMethodBinding().getKey())) {
											setPublicModifierToSourceMethod(methodInvocation.resolveMethodBinding(), sourceTypeDeclaration);
											sourceMethodBindingsChangedWithPublicModifier.add(methodInvocation.resolveMethodBinding().getKey());
										}
									}
								}
								else {
									if(!oldMethodInvocationsWithinExtractedMethods.containsKey(sourceMethod)) {
										LinkedHashSet<MethodInvocation> methodInvocations = new LinkedHashSet<MethodInvocation>();
										methodInvocations.add(methodInvocation);
										oldMethodInvocationsWithinExtractedMethods.put(sourceMethod, methodInvocations);
									}
									else {
										Set<MethodInvocation> methodInvocations = oldMethodInvocationsWithinExtractedMethods.get(sourceMethod);
										methodInvocations.add(methodInvocation);
									}
									if(!newMethodInvocationsWithinExtractedMethods.containsKey(newMethodDeclaration)) {
										LinkedHashSet<MethodInvocation> methodInvocations = new LinkedHashSet<MethodInvocation>();
										methodInvocations.add((MethodInvocation)newMethodInvocations.get(j));
										newMethodInvocationsWithinExtractedMethods.put(newMethodDeclaration, methodInvocations);
									}
									else {
										Set<MethodInvocation> methodInvocations = newMethodInvocationsWithinExtractedMethods.get(newMethodDeclaration);
										methodInvocations.add((MethodInvocation)newMethodInvocations.get(j));
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
									Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
									Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
									MethodInvocation newMethodInvocation = (MethodInvocation)newMethodInvocations.get(j);
									if((superclassMethodBinding.getModifiers() & Modifier.STATIC) != 0) {
										SimpleName qualifier = ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
										targetRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, qualifier, null);
									}
									else {
										if(isParentAnonymousClassDeclaration(methodInvocation))
											sourceClassParameterShouldBeFinal = true;
										if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
											sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
											addThisVariable(additionalArgumentsAddedToMovedMethod);
											additionalParametersAddedToMovedMethod.add(sourceClassParameter);
										}
										targetRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, parameterName, null);
										if(!sourceMethodBindingsChangedWithPublicModifier.contains(methodBinding.getKey())) {
											TypeDeclaration superclassTypeDeclaration = RefactoringUtility.findDeclaringTypeDeclaration(superclassMethodBinding, sourceTypeDeclaration);
											if(superclassTypeDeclaration != null) {
												setPublicModifierToSourceMethod(methodInvocation.resolveMethodBinding(), superclassTypeDeclaration);
											}
											sourceMethodBindingsChangedWithPublicModifier.add(methodBinding.getKey());
										}
									}
								}
							}
						}
						else {
							//check if it is a statically imported method
							List<ImportDeclaration> sourceImportDeclarations = sourceCompilationUnit.imports();
							for(ImportDeclaration importDeclaration : sourceImportDeclarations) {
								if(importDeclaration.isStatic()) {
									IBinding binding = importDeclaration.resolveBinding();
									//A single-static-import declaration imports all accessible static members with a given simple name from a type.
									//binding.isEqualTo(methodBinding) will not work when the static import actually imports multiple overloaded methods
									if(binding != null && binding.getKind() == IBinding.METHOD) {
										IMethodBinding importedMethodBinding = (IMethodBinding)binding;
										if(importedMethodBinding.getName().equals(methodBinding.getName()) &&
												importedMethodBinding.getDeclaringClass().getQualifiedName().equals(methodBinding.getDeclaringClass().getQualifiedName())) {
											this.staticallyImportedMethods.add(importedMethodBinding);
										}
									}
								}
							}
						}
					}
				}
				else if(methodBindingCorrespondsToExtractedMethod(methodInvocation.resolveMethodBinding())) {
					if(!oldMethodInvocationsWithinExtractedMethods.containsKey(sourceMethod)) {
						LinkedHashSet<MethodInvocation> methodInvocations = new LinkedHashSet<MethodInvocation>();
						methodInvocations.add(methodInvocation);
						oldMethodInvocationsWithinExtractedMethods.put(sourceMethod, methodInvocations);
					}
					else {
						Set<MethodInvocation> methodInvocations = oldMethodInvocationsWithinExtractedMethods.get(sourceMethod);
						methodInvocations.add(methodInvocation);
					}
					if(!newMethodInvocationsWithinExtractedMethods.containsKey(newMethodDeclaration)) {
						LinkedHashSet<MethodInvocation> methodInvocations = new LinkedHashSet<MethodInvocation>();
						methodInvocations.add((MethodInvocation)newMethodInvocations.get(j));
						newMethodInvocationsWithinExtractedMethods.put(newMethodDeclaration, methodInvocations);
					}
					else {
						Set<MethodInvocation> methodInvocations = newMethodInvocationsWithinExtractedMethods.get(newMethodDeclaration);
						methodInvocations.add((MethodInvocation)newMethodInvocations.get(j));
					}
					if(methodInvocation.getExpression() != null && methodInvocation.getExpression().resolveTypeBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
						if(!sourceMethodBindingsChangedWithPublicModifier.contains(methodInvocation.resolveMethodBinding().getKey())) {
							setPublicModifierToSourceMethod(methodInvocation.resolveMethodBinding(), sourceTypeDeclaration);
							sourceMethodBindingsChangedWithPublicModifier.add(methodInvocation.resolveMethodBinding().getKey());
						}
					}
				}
			}
			j++;
		}
		//replaceThisExpressionWithSourceClassParameterInMethodInvocationArguments
		int k=0;
		for(Expression invocation : newMethodInvocations) {
			if(invocation instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)invocation;
				List<Expression> arguments = methodInvocation.arguments();
				for(Expression argument : arguments) {
					if(argument instanceof ThisExpression) {
						if(isParentAnonymousClassDeclaration(sourceMethodInvocations.get(k)))
							sourceClassParameterShouldBeFinal = true;
						Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
						Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
						if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
							sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
							addThisVariable(additionalArgumentsAddedToMovedMethod);
							additionalParametersAddedToMovedMethod.add(sourceClassParameter);
						}
						ListRewrite argumentRewrite = targetRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
						argumentRewrite.replace(argument, parameterName, null);
					}
				}
			}
			k++;
		}
		//replaceThisExpressionWithSourceClassParameterInClassInstanceCreationArguments
		List<Expression> sourceClassInstanceCreations = expressionExtractor.getClassInstanceCreations(sourceMethod.getBody());
		List<Expression> newClassInstanceCreations = expressionExtractor.getClassInstanceCreations(newMethodDeclaration.getBody());
		k = 0;
		for(Expression creation : newClassInstanceCreations) {
			ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)creation;
			List<Expression> arguments = classInstanceCreation.arguments();
			for(Expression argument : arguments) {
				if(argument instanceof ThisExpression) {
					if(isParentAnonymousClassDeclaration(sourceClassInstanceCreations.get(k)))
						sourceClassParameterShouldBeFinal = true;
					Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
					Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
					if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
						sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
						addThisVariable(additionalArgumentsAddedToMovedMethod);
						additionalParametersAddedToMovedMethod.add(sourceClassParameter);
					}
					ListRewrite argumentRewrite = targetRewriter.getListRewrite(classInstanceCreation, ClassInstanceCreation.ARGUMENTS_PROPERTY);
					argumentRewrite.replace(argument, parameterName, null);
				}
			}
			ClassInstanceCreation oldClassInstanceCreation = (ClassInstanceCreation)sourceClassInstanceCreations.get(k);
			ITypeBinding classInstanceCreationTypeBinding = oldClassInstanceCreation.resolveTypeBinding();
			if(classInstanceCreationTypeBinding.isNested() && oldClassInstanceCreation.getAnonymousClassDeclaration() == null &&
					sourceTypeDeclaration.resolveBinding().isEqualTo(classInstanceCreationTypeBinding.getDeclaringClass())) {
				if(isParentAnonymousClassDeclaration(sourceClassInstanceCreations.get(k)))
					sourceClassParameterShouldBeFinal = true;
				if((classInstanceCreationTypeBinding.getModifiers() & Modifier.STATIC) != 0) {
					Type qualifierType = ast.newSimpleType(ast.newSimpleName(sourceTypeDeclaration.resolveBinding().getName()));
					QualifiedType qualifiedType = ast.newQualifiedType(qualifierType, ast.newSimpleName(classInstanceCreationTypeBinding.getName()));
					targetRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY, qualifiedType, null);
					requiredImportDeclarationsInExtractedClass.add(classInstanceCreationTypeBinding);
					setPublicModifierToSourceMemberType(classInstanceCreationTypeBinding);
				}
				else {
					Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
					Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
					if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
						sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
						addThisVariable(additionalArgumentsAddedToMovedMethod);
						additionalParametersAddedToMovedMethod.add(sourceClassParameter);
					}
					targetRewriter.set(classInstanceCreation, ClassInstanceCreation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
					Type oldClassInstanceCreationType = oldClassInstanceCreation.getType();
					SimpleName simpleNameType = null;
					if(oldClassInstanceCreationType instanceof QualifiedType) {
						QualifiedType qualifiedType = (QualifiedType)oldClassInstanceCreationType;
						simpleNameType = qualifiedType.getName();
					}
					else if(oldClassInstanceCreationType instanceof SimpleType) { 
						SimpleType simpleType = (SimpleType)oldClassInstanceCreationType;
						if(simpleType.getName() instanceof QualifiedName) {
							QualifiedName qualifiedName = (QualifiedName)simpleType.getName();
							simpleNameType = qualifiedName.getName();
						}
					}
					if(simpleNameType != null) {
						targetRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY,
								ast.newSimpleType(ast.newSimpleName(simpleNameType.getIdentifier())), null);
					}
				}
			}
			k++;
		}
		//replaceThisExpressionWithSourceClassParameterInVariableDeclarationInitializers
		StatementExtractor statementExtractor = new StatementExtractor();
		List<VariableDeclarationFragment> sourceVariableDeclarationFragments = new ArrayList<VariableDeclarationFragment>();
		List<VariableDeclarationFragment> newVariableDeclarationFragments = new ArrayList<VariableDeclarationFragment>();
		List<Statement> sourceVariableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(sourceMethod.getBody());
		for(Statement statement : sourceVariableDeclarationStatements) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
			List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
			sourceVariableDeclarationFragments.addAll(fragments);
		}
		List<Statement> newVariableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(newMethodDeclaration.getBody());
		for(Statement statement : newVariableDeclarationStatements) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
			List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
			newVariableDeclarationFragments.addAll(fragments);
		}
		List<Expression> sourceVariableDeclarationExpressions = expressionExtractor.getVariableDeclarationExpressions(sourceMethod.getBody());
		for(Expression expression : sourceVariableDeclarationExpressions) {
			VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)expression;
			List<VariableDeclarationFragment> fragments = variableDeclarationExpression.fragments();
			sourceVariableDeclarationFragments.addAll(fragments);
		}
		List<Expression> newVariableDeclarationExpressions = expressionExtractor.getVariableDeclarationExpressions(newMethodDeclaration.getBody());
		for(Expression expression : newVariableDeclarationExpressions) {
			VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)expression;
			List<VariableDeclarationFragment> fragments = variableDeclarationExpression.fragments();
			newVariableDeclarationFragments.addAll(fragments);
		}
		k = 0;
		for(VariableDeclarationFragment fragment : newVariableDeclarationFragments) {
			Expression initializer = fragment.getInitializer();
			if(initializer instanceof ThisExpression) {
				if(isParentAnonymousClassDeclaration(sourceVariableDeclarationFragments.get(k)))
					sourceClassParameterShouldBeFinal = true;
				Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
				Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
				if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
					sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
					addThisVariable(additionalArgumentsAddedToMovedMethod);
					additionalParametersAddedToMovedMethod.add(sourceClassParameter);
				}
				targetRewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, parameterName, null);
			}
			k++;
		}
		//replaceThisExpressionWithSourceClassParameterInReturnStatementExpressions
		List<Statement> sourceReturnStatements = statementExtractor.getReturnStatements(sourceMethod.getBody());
		List<Statement> newReturnStatements = statementExtractor.getReturnStatements(newMethodDeclaration.getBody());
		k = 0;
		for(Statement statement : newReturnStatements) {
			ReturnStatement newReturnStatement = (ReturnStatement)statement;
			if(newReturnStatement.getExpression() instanceof ThisExpression) {
				if(isParentAnonymousClassDeclaration(sourceReturnStatements.get(k)))
					sourceClassParameterShouldBeFinal = true;
				Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
				Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
				if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
					sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
					addThisVariable(additionalArgumentsAddedToMovedMethod);
					additionalParametersAddedToMovedMethod.add(sourceClassParameter);
				}
				targetRewriter.set(newReturnStatement, ReturnStatement.EXPRESSION_PROPERTY, parameterName, null);
			}
			k++;
		}
		if(sourceClassParameter != null && sourceClassParameterShouldBeFinal) {
			ListRewrite modifiersRewrite = targetRewriter.getListRewrite(sourceClassParameter, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
			modifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
		}
		for(PlainVariable fieldName : fieldParameterFinalMap.keySet()) {
			if(fieldParameterFinalMap.get(fieldName) == true) {
				SingleVariableDeclaration fieldParameter = fieldParameterMap.get(fieldName);
				if(fieldParameter != null) {
					ListRewrite modifiersRewrite = targetRewriter.getListRewrite(fieldParameter, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
					modifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
				}
			}
		}
	}

	private void handleAccessedFieldNotHavingSetterMethod(MethodDeclaration sourceMethod,
			MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter, AST ast,
			Map<PlainVariable, SingleVariableDeclaration> fieldParameterMap, SimpleName newAccessedVariable, IVariableBinding accessedVariableBinding) {
		Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
		Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
		if(!containsVariable(accessedVariableBinding, additionalArgumentsAddedToMovedMethod)) {
			SingleVariableDeclaration fieldParameter = addParameterToMovedMethod(newMethodDeclaration, accessedVariableBinding, targetRewriter);
			addVariable(accessedVariableBinding, additionalArgumentsAddedToMovedMethod);
			additionalParametersAddedToMovedMethod.add(fieldParameter);
			fieldParameterMap.put(new PlainVariable(accessedVariableBinding), fieldParameter);
		}
		if(newAccessedVariable.getParent() instanceof FieldAccess) {
			FieldAccess fieldAccess = (FieldAccess)newAccessedVariable.getParent();
			if(fieldAccess.getExpression() instanceof ThisExpression) {
				String parameterName = createNameForParameterizedFieldAccess(fieldAccess.getName().getIdentifier());
				targetRewriter.replace(newAccessedVariable.getParent(), ast.newSimpleName(parameterName), null);
			}
		}
		else if(newAccessedVariable.getParent() instanceof QualifiedName) {
			
		}
		else {
			String parameterName = createNameForParameterizedFieldAccess(accessedVariableBinding.getName());
			targetRewriter.replace(newAccessedVariable, ast.newSimpleName(parameterName), null);
		}
	}

	private SingleVariableDeclaration handleAccessedFieldHavingSetterMethod(MethodDeclaration sourceMethod,
			MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter,
			AST ast, SingleVariableDeclaration sourceClassParameter,
			String modifiedSourceTypeName, SimpleName newAccessedVariable, IVariableBinding accessedVariableBinding) {
		IMethodBinding getterMethodBinding = findGetterMethodInSourceClass(accessedVariableBinding);
		Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
		Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
		if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
			sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
			addThisVariable(additionalArgumentsAddedToMovedMethod);
			additionalParametersAddedToMovedMethod.add(sourceClassParameter);
		}
		MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
		if(getterMethodBinding != null) {
			targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodBinding.getName()), null);
		}
		else {
			if(!sourceFieldBindingsWithCreatedGetterMethod.contains(accessedVariableBinding.getKey())) {
				createGetterMethodInSourceClass(accessedVariableBinding);
				sourceFieldBindingsWithCreatedGetterMethod.add(accessedVariableBinding.getKey());
			}
			String originalFieldName = accessedVariableBinding.getName();
			String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
			String getterMethodName = GETTER_PREFIX + modifiedFieldName;
			getterMethodName = appendAccessorMethodSuffix(getterMethodName, sourceTypeDeclaration.getMethods());
			targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodName), null);
		}
		if(newAccessedVariable.getParent() instanceof FieldAccess) {
			FieldAccess newFieldAccess = (FieldAccess)newAccessedVariable.getParent();
			if(newFieldAccess.getExpression() instanceof ThisExpression) {
				targetRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
				targetRewriter.replace(newFieldAccess, getterMethodInvocation, null);
			}
		}
		else if(newAccessedVariable.getParent() instanceof QualifiedName) {
			targetRewriter.replace(newAccessedVariable, getterMethodInvocation, null);
		}
		else {
			targetRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
			targetRewriter.replace(newAccessedVariable, getterMethodInvocation, null);
		}
		return sourceClassParameter;
	}

	private void addVariable(IVariableBinding variableBinding, Set<PlainVariable> additionalArgumentsAddedToMovedMethod) {
		PlainVariable variable = new PlainVariable(variableBinding);
		additionalArgumentsAddedToMovedMethod.add(variable);
	}

	private void addThisVariable(Set<PlainVariable> additionalArgumentsAddedToMovedMethod) {
		PlainVariable variable = new PlainVariable("this", "this", "this", false, false, false);
		additionalArgumentsAddedToMovedMethod.add(variable);
	}

	private boolean isThisVariable(PlainVariable argument) {
		return argument.getVariableBindingKey().equals("this");
	}

	private boolean containsThisVariable(Set<PlainVariable> additionalArgumentsAddedToMovedMethod) {
		for(PlainVariable argument : additionalArgumentsAddedToMovedMethod) {
			if(isThisVariable(argument)) {
				return true;
			}
		}
		return false;
	}

	private boolean containsVariable(IVariableBinding variableBinding, Set<PlainVariable> additionalArgumentsAddedToMovedMethod) {
		for(PlainVariable argument : additionalArgumentsAddedToMovedMethod) {
			if(argument.getVariableBindingKey().equals(variableBinding.getKey())) {
				return true;
			}
		}
		return false;
	}

	private boolean declaredInSourceTypeDeclarationOrSuperclass(IVariableBinding variableBinding) {
		return RefactoringUtility.findDeclaringTypeDeclaration(variableBinding, sourceTypeDeclaration) != null;
	}

	private SingleVariableDeclaration addSourceClassParameterToMovedMethod(MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
		AST ast = newMethodDeclaration.getAST();
		SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
		SimpleName typeName = ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
		Type parameterType = ast.newSimpleType(typeName);
		targetRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, parameterType, null);
		String sourceTypeName = sourceTypeDeclaration.getName().getIdentifier();
		String modifiedSourceTypeName = sourceTypeName.substring(0,1).toLowerCase() + sourceTypeName.substring(1,sourceTypeName.length());
		SimpleName parameterName = ast.newSimpleName(modifiedSourceTypeName);
		targetRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, parameterName, null);
		ListRewrite parametersRewrite = targetRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		parametersRewrite.insertLast(parameter, null);
		Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
		typeBindings.add(sourceTypeDeclaration.resolveBinding());
		RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportDeclarationsInExtractedClass);
		return parameter;
	}

	private SingleVariableDeclaration addParameterToMovedMethod(MethodDeclaration newMethodDeclaration, PlainVariable additionalArgument, ASTRewrite targetRewriter) {
		AST ast = newMethodDeclaration.getAST();
		SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
		VariableDeclaration field = RefactoringUtility.findFieldDeclaration(additionalArgument, sourceTypeDeclaration);
		FieldDeclaration fieldDeclaration = (FieldDeclaration)field.getParent();
		Type fieldType = fieldDeclaration.getType();
		targetRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, fieldType, null);
		if(additionalArgument.isField()) {
			//adding "this" prefix to avoid collisions with other parameter names
			String parameterName = createNameForParameterizedFieldAccess(field.getName().getIdentifier());
			targetRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, ast.newSimpleName(parameterName), null);
		}
		else {
			targetRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, field.getName(), null);
		}
		ListRewrite parametersRewrite = targetRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		parametersRewrite.insertLast(parameter, null);
		Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
		typeBindings.add(fieldType.resolveBinding());
		RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportDeclarationsInExtractedClass);
		return parameter;
	}

	private SingleVariableDeclaration addParameterToMovedMethod(MethodDeclaration newMethodDeclaration, IVariableBinding variableBinding, ASTRewrite targetRewriter) {
		AST ast = newMethodDeclaration.getAST();
		SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
		ITypeBinding typeBinding = variableBinding.getType();
		Type fieldType = RefactoringUtility.generateTypeFromTypeBinding(typeBinding, ast, targetRewriter);
		targetRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, fieldType, null);
		if(variableBinding.isField()) {
			//adding "this" prefix to avoid collisions with other parameter names
			String parameterName = createNameForParameterizedFieldAccess(variableBinding.getName());
			targetRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, ast.newSimpleName(parameterName), null);
		}
		else {
			targetRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, ast.newSimpleName(variableBinding.getName()), null);
		}
		ListRewrite parametersRewrite = targetRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		parametersRewrite.insertLast(parameter, null);
		Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
		typeBindings.add(variableBinding.getType());
		RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportDeclarationsInExtractedClass);
		return parameter;
	}

	private String createNameForParameterizedFieldAccess(String fieldName) {
		return "this" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1, fieldName.length());
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
							IPackageBinding sourceTypeDeclarationPackageBinding = this.sourceTypeDeclaration.resolveBinding().getPackage();
							IPackageBinding typeDeclarationPackageBinding = sourceTypeDeclaration.resolveBinding().getPackage();
							if(sourceTypeDeclarationPackageBinding != null && typeDeclarationPackageBinding != null &&
									!sourceTypeDeclarationPackageBinding.isEqualTo(typeDeclarationPackageBinding)) {
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
			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
			if(change == null) {
				MultiTextEdit sourceMultiTextEdit = new MultiTextEdit();
				change = new CompilationUnitChange("", sourceICompilationUnit);
				change.setEdit(sourceMultiTextEdit);
				compilationUnitChanges.put(sourceICompilationUnit, change);
			}
			change.getEdit().addChild(sourceEdit);
			change.addTextEditGroup(new TextEditGroup("Update access modifier to public", new TextEdit[] {sourceEdit}));
		}
		catch(JavaModelException javaModelException) {
			javaModelException.printStackTrace();
		}
	}

	private void modifySourceStaticFieldInstructionsInTargetClass(MethodDeclaration sourceMethod,
			MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter) {
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
					if(declaredInSourceTypeDeclarationOrSuperclass(variableBinding)) {
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
						setPublicModifierToSourceField(variableBinding);
					}
					else {
						AST ast = newMethodDeclaration.getAST();
						SimpleName qualifier = null;
						if((variableBinding.getModifiers() & Modifier.PUBLIC) != 0) {
							qualifier = ast.newSimpleName(variableBinding.getDeclaringClass().getName());
							Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
							typeBindings.add(variableBinding.getDeclaringClass());
							RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportDeclarationsInExtractedClass);
						}
						else {
							qualifier = ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
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
							ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
							CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
							change.getEdit().addChild(sourceEdit);
							change.addTextEditGroup(new TextEditGroup("Change access level to public", new TextEdit[] {sourceEdit}));
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
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
					CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
					change.getEdit().addChild(sourceEdit);
					change.addTextEditGroup(new TextEditGroup("Set access level to public", new TextEdit[] {sourceEdit}));
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
							else if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PRIVATE_KEYWORD)) {
								if(!fieldDeclarationsChangedWithPublicModifier.contains(fieldDeclaration)) {
									fieldDeclarationsChangedWithPublicModifier.add(fieldDeclaration);
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
							else if(modifier.getKeyword().equals(Modifier.ModifierKeyword.PROTECTED_KEYWORD)) {
								modifierFound = true;
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
								ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
								CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
								change.getEdit().addChild(sourceEdit);
								change.addTextEditGroup(new TextEditGroup("Set access level to public", new TextEdit[] {sourceEdit}));
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

	private MethodDeclaration createSetterMethodDeclaration(VariableDeclaration fieldFragment, AST extractedClassAST, ASTRewrite extractedClassRewriter) {
		String originalFieldName = fieldFragment.getName().getIdentifier();
		String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
		MethodDeclaration setterMethodDeclaration = extractedClassAST.newMethodDeclaration();
		String setterMethodName = SETTER_PREFIX + modifiedFieldName;
		setterMethodName = appendAccessorMethodSuffix(setterMethodName, this.extractedMethods);
		extractedClassRewriter.set(setterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, extractedClassAST.newSimpleName(setterMethodName), null);
		PrimitiveType type = extractedClassAST.newPrimitiveType(PrimitiveType.VOID);
		extractedClassRewriter.set(setterMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, type, null);
		ListRewrite setterMethodModifiersRewrite = extractedClassRewriter.getListRewrite(setterMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		setterMethodModifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
		SingleVariableDeclaration parameter = extractedClassAST.newSingleVariableDeclaration();
		extractedClassRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, fieldFragment.getName(), null);
		FieldDeclaration originalFieldDeclaration = (FieldDeclaration)fieldFragment.getParent();
		extractedClassRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, originalFieldDeclaration.getType(), null);
		ListRewrite setterMethodParametersRewrite = extractedClassRewriter.getListRewrite(setterMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		setterMethodParametersRewrite.insertLast(parameter, null);
		if((originalFieldDeclaration.getModifiers() & Modifier.STATIC) != 0) {
			setterMethodModifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD), null);
		}
		
		Assignment assignment = extractedClassAST.newAssignment();
		FieldAccess fieldAccess = extractedClassAST.newFieldAccess();
		if((originalFieldDeclaration.getModifiers() & Modifier.STATIC) != 0) {
			extractedClassRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, extractedClassAST.newSimpleName(extractedTypeName), null);
		}
		else {
			ThisExpression thisExpression = extractedClassAST.newThisExpression();
			extractedClassRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, thisExpression, null);
		}
		extractedClassRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, fieldFragment.getName(), null);
		extractedClassRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, fieldAccess, null);
		extractedClassRewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.ASSIGN, null);
		extractedClassRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, fieldFragment.getName(), null);
		ExpressionStatement expressionStatement = extractedClassAST.newExpressionStatement(assignment);
		Block setterMethodBody = extractedClassAST.newBlock();
		ListRewrite setterMethodBodyRewrite = extractedClassRewriter.getListRewrite(setterMethodBody, Block.STATEMENTS_PROPERTY);
		setterMethodBodyRewrite.insertLast(expressionStatement, null);
		extractedClassRewriter.set(setterMethodDeclaration, MethodDeclaration.BODY_PROPERTY, setterMethodBody, null);
		return setterMethodDeclaration;
	}

	private MethodDeclaration createGetterMethodDeclaration(VariableDeclaration fieldFragment, AST extractedClassAST, ASTRewrite extractedClassRewriter) {
		String originalFieldName = fieldFragment.getName().getIdentifier();
		String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
		MethodDeclaration getterMethodDeclaration = extractedClassAST.newMethodDeclaration();
		String getterMethodName = GETTER_PREFIX + modifiedFieldName;
		getterMethodName = appendAccessorMethodSuffix(getterMethodName, this.extractedMethods);
		extractedClassRewriter.set(getterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, extractedClassAST.newSimpleName(getterMethodName), null);
		FieldDeclaration originalFieldDeclaration = (FieldDeclaration)fieldFragment.getParent();
		extractedClassRewriter.set(getterMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, originalFieldDeclaration.getType(), null);
		ListRewrite getterMethodModifiersRewrite = extractedClassRewriter.getListRewrite(getterMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		getterMethodModifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
		if((originalFieldDeclaration.getModifiers() & Modifier.STATIC) != 0) {
			getterMethodModifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD), null);
		}
		ReturnStatement returnStatement = extractedClassAST.newReturnStatement();
		extractedClassRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, fieldFragment.getName(), null);
		Block getterMethodBody = extractedClassAST.newBlock();
		ListRewrite getterMethodBodyRewrite = extractedClassRewriter.getListRewrite(getterMethodBody, Block.STATEMENTS_PROPERTY);
		getterMethodBodyRewrite.insertLast(returnStatement, null);
		extractedClassRewriter.set(getterMethodDeclaration, MethodDeclaration.BODY_PROPERTY, getterMethodBody, null);
		return getterMethodDeclaration;
	}

	private void addStaticImportDeclaration(IMethodBinding methodBinding, CompilationUnit targetCompilationUnit, ASTRewrite targetRewriter) {
		AST ast = targetCompilationUnit.getAST();
		ImportDeclaration importDeclaration = ast.newImportDeclaration();
		QualifiedName qualifiedName = ast.newQualifiedName(ast.newName(methodBinding.getDeclaringClass().getQualifiedName()), ast.newSimpleName(methodBinding.getName()));
		targetRewriter.set(importDeclaration, ImportDeclaration.NAME_PROPERTY, qualifiedName, null);
		targetRewriter.set(importDeclaration, ImportDeclaration.STATIC_PROPERTY, true, null);
		ListRewrite importRewrite = targetRewriter.getListRewrite(targetCompilationUnit, CompilationUnit.IMPORTS_PROPERTY);
		importRewrite.insertLast(importDeclaration, null);
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
				((!qualifiedPackageName.equals(sourcePackageDeclarationName) && !typeBinding.isNested()) ||
				(typeBinding.isNested() && sourceTypeDeclaration.resolveBinding().isEqualTo(typeBinding)) ||
				(qualifiedPackageName.equals(sourceTypeDeclaration.resolveBinding().getQualifiedName()) && typeBinding.isMember()))) {
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

	private void createExtractedTypeFieldReferenceInSourceClass() {
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		AST contextAST = sourceTypeDeclaration.getAST();
		ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		VariableDeclarationFragment extractedReferenceFragment = contextAST.newVariableDeclarationFragment();
		String modifiedExtractedTypeName = extractedTypeName.substring(0,1).toLowerCase() + extractedTypeName.substring(1,extractedTypeName.length());
		sourceRewriter.set(extractedReferenceFragment, VariableDeclarationFragment.NAME_PROPERTY, contextAST.newSimpleName(modifiedExtractedTypeName), null);
		if(constructorFinalFieldAssignmentMap.isEmpty()) {
			ClassInstanceCreation initializer = contextAST.newClassInstanceCreation();
			Type targetType = contextAST.newSimpleType(contextAST.newName(extractedTypeName));
			sourceRewriter.set(initializer, ClassInstanceCreation.TYPE_PROPERTY, targetType, null);
			sourceRewriter.set(extractedReferenceFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, initializer, null);
		}
		else {
			ExpressionExtractor expressionExtractor = new ExpressionExtractor();
			for(MethodDeclaration constructor : constructorFinalFieldAssignmentMap.keySet()) {
				ASTRewrite constructorRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
				ListRewrite constructorBodyStatementsRewrite = constructorRewriter.getListRewrite(constructor.getBody(), Block.STATEMENTS_PROPERTY);
				
				Assignment extractedTypeFieldReferenceAssignment = contextAST.newAssignment();
				FieldAccess extractedTypeFieldAccess = contextAST.newFieldAccess();
				constructorRewriter.set(extractedTypeFieldAccess, FieldAccess.NAME_PROPERTY, contextAST.newSimpleName(modifiedExtractedTypeName), null);
				constructorRewriter.set(extractedTypeFieldAccess, FieldAccess.EXPRESSION_PROPERTY, contextAST.newThisExpression(), null);
				constructorRewriter.set(extractedTypeFieldReferenceAssignment, Assignment.LEFT_HAND_SIDE_PROPERTY, extractedTypeFieldAccess, null);
				constructorRewriter.set(extractedTypeFieldReferenceAssignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.ASSIGN, null);
				ClassInstanceCreation classInstanceCreation = contextAST.newClassInstanceCreation();
				Type targetType = contextAST.newSimpleType(contextAST.newName(extractedTypeName));
				constructorRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY, targetType, null);
				constructorRewriter.set(extractedTypeFieldReferenceAssignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, classInstanceCreation, null);
				ExpressionStatement assignmentStatement = contextAST.newExpressionStatement(extractedTypeFieldReferenceAssignment);
				
				Map<VariableDeclaration, Assignment> finalFieldAssignmentMap = constructorFinalFieldAssignmentMap.get(constructor);
				ListRewrite classInstanceCreationArgumentsRewrite = constructorRewriter.getListRewrite(classInstanceCreation, ClassInstanceCreation.ARGUMENTS_PROPERTY);
				Set<VariableDeclaration> extractedClassConstructorParameters = new LinkedHashSet<VariableDeclaration>();
				
				StatementExtractor statementExtractor = new StatementExtractor();
				List<Statement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(constructor.getBody());
				List<Statement> insertAfterStatements = new ArrayList<Statement>();
				for(VariableDeclaration fieldFragment : finalFieldAssignmentMap.keySet()) {
	        		Assignment fieldAssignment = finalFieldAssignmentMap.get(fieldFragment);
	        		List<Expression> variableInstructions = expressionExtractor.getVariableInstructions(fieldAssignment.getRightHandSide());
	        		TypeVisitor typeVisitor = new TypeVisitor();
	        		fieldAssignment.getRightHandSide().accept(typeVisitor);
	        		RefactoringUtility.getSimpleTypeBindings(typeVisitor.getTypeBindings(), requiredImportDeclarationsInExtractedClass);
	        		for(Expression expression : variableInstructions) {
	        			SimpleName simpleName = (SimpleName)expression;
	        			boolean foundInOriginalConstructorParameters = false;
	        			List<SingleVariableDeclaration> originalConstructorParameters = constructor.parameters();
	        			for(SingleVariableDeclaration originalConstructorParameter : originalConstructorParameters) {
	        				if(originalConstructorParameter.resolveBinding().isEqualTo(simpleName.resolveBinding())) {
	        					if(!extractedClassConstructorParameters.contains(originalConstructorParameter)) {
	        						classInstanceCreationArgumentsRewrite.insertLast(simpleName, null);
	        						extractedClassConstructorParameters.add(originalConstructorParameter);
	        						Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
	        						typeBindings.add(originalConstructorParameter.getType().resolveBinding());
	        						RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportDeclarationsInExtractedClass);
	        						foundInOriginalConstructorParameters = true;
	        						break;
	        					}
	        				}
	        			}
	        			if(!foundInOriginalConstructorParameters) {
	        				boolean foundInVariableDeclarationStatement = false;
	        				for(Statement statement : variableDeclarationStatements) {
	        					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
	        					List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
	        					for(VariableDeclarationFragment fragment : fragments) {
	        						if(fragment.resolveBinding().isEqualTo(simpleName.resolveBinding())) {
	        							if(!extractedClassConstructorParameters.contains(fragment)) {
	        								classInstanceCreationArgumentsRewrite.insertLast(simpleName, null);
	    	        						extractedClassConstructorParameters.add(fragment);
	    	        						Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
	    	        						typeBindings.add(variableDeclarationStatement.getType().resolveBinding());
	    	        						RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportDeclarationsInExtractedClass);
	    	        						if(!insertAfterStatements.contains(variableDeclarationStatement)) {
	    	        							insertAfterStatements.add(variableDeclarationStatement);
	    	        						}
	    	        						foundInVariableDeclarationStatement = true;
	    	        						break;
	        							}
	        						}
	        					}
	        					if(foundInVariableDeclarationStatement) {
	        						break;
	        					}
	        				}
	        			}
	        		}
	        	}
				if(!insertAfterStatements.isEmpty()) {
					Statement lastStatement = insertAfterStatements.get(0);
					int maxStartPosition = lastStatement.getStartPosition();
					for(int i=1; i<insertAfterStatements.size(); i++) {
						Statement currentStatement = insertAfterStatements.get(i);
						if(currentStatement.getStartPosition() > maxStartPosition) {
							maxStartPosition = currentStatement.getStartPosition();
							lastStatement = currentStatement;
						}
					}
					constructorBodyStatementsRewrite.insertAfter(assignmentStatement, lastStatement, null);
				}
				else {
					constructorBodyStatementsRewrite.insertFirst(assignmentStatement, null);
				}
				extractedClassConstructorParameterMap.put(constructor, extractedClassConstructorParameters);
	        	try {
	    			TextEdit sourceEdit = constructorRewriter.rewriteAST();
	    			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
	    			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
	    			change.getEdit().addChild(sourceEdit);
	    			change.addTextEditGroup(new TextEditGroup("Initialize field holding a reference to the extracted class", new TextEdit[] {sourceEdit}));
	    		} catch (JavaModelException e) {
	    			e.printStackTrace();
	    		}
			}
		}
		FieldDeclaration extractedReferenceFieldDeclaration = contextAST.newFieldDeclaration(extractedReferenceFragment);
		sourceRewriter.set(extractedReferenceFieldDeclaration, FieldDeclaration.TYPE_PROPERTY, contextAST.newSimpleName(extractedTypeName), null);
		ListRewrite typeFieldDeclarationModifiersRewrite = sourceRewriter.getListRewrite(extractedReferenceFieldDeclaration, FieldDeclaration.MODIFIERS2_PROPERTY);
		typeFieldDeclarationModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD), null);
		ITypeBinding serializableTypeBinding = implementsSerializableInterface(sourceTypeDeclaration.resolveBinding());
		if(serializableTypeBinding != null && !existsNonTransientExtractedFieldFragment()) {
			typeFieldDeclarationModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.TRANSIENT_KEYWORD), null);
			updateReadObjectInSourceClass(modifiedExtractedTypeName);
			updateWriteObjectInSourceClass(modifiedExtractedTypeName);
        }
		updateCloneInSourceClass(modifiedExtractedTypeName);
		contextBodyRewrite.insertFirst(extractedReferenceFieldDeclaration, null);
		
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
			change.getEdit().addChild(sourceEdit);
			change.addTextEditGroup(new TextEditGroup("Create field holding a reference to the extracted class", new TextEdit[] {sourceEdit}));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void updateWriteObjectInSourceClass(String modifiedExtractedTypeName) {
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		AST contextAST = sourceTypeDeclaration.getAST();
		MethodDeclaration[] methodDeclarations = sourceTypeDeclaration.getMethods();
		boolean methodFound = false;
		for(MethodDeclaration method : methodDeclarations) {
			if(isWriteObject(method)) {
				methodFound = true;
				if(!extractedMethods.contains(method)) {
					Block methodBody = method.getBody();
					if(methodBody != null) {
						List<SingleVariableDeclaration> parameters = method.parameters();
						SimpleName parameterSimpleName = parameters.get(0).getName();
						ListRewrite statementRewrite = sourceRewriter.getListRewrite(methodBody, Block.STATEMENTS_PROPERTY);
						Statement methodInvocationStatement = createMethodInvocationStatementForWriteObject(
								sourceRewriter, contextAST, modifiedExtractedTypeName, parameterSimpleName);
						Statement firstStatement = isFirstStatementMethodInvocationExpressionStatementWithName(method, "defaultWriteObject");
						if(firstStatement != null) {
							statementRewrite.insertAfter(methodInvocationStatement, firstStatement, null);
						}
						else {
							statementRewrite.insertFirst(methodInvocationStatement, null);
						}
						try {
							TextEdit sourceEdit = sourceRewriter.rewriteAST();
							ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
							CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
							change.getEdit().addChild(sourceEdit);
							change.addTextEditGroup(new TextEditGroup("Update writeObject()", new TextEdit[] {sourceEdit}));
						} catch (JavaModelException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		if(!methodFound) {
			MethodDeclaration writeObjectMethod = contextAST.newMethodDeclaration();
			sourceRewriter.set(writeObjectMethod, MethodDeclaration.NAME_PROPERTY, contextAST.newSimpleName("writeObject"), null);
			ListRewrite parametersRewrite = sourceRewriter.getListRewrite(writeObjectMethod, MethodDeclaration.PARAMETERS_PROPERTY);
			SingleVariableDeclaration parameter = contextAST.newSingleVariableDeclaration();
			Type parameterType = contextAST.newSimpleType(contextAST.newName("java.io.ObjectOutputStream"));
			sourceRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, parameterType, null);
			SimpleName parameterSimpleName = contextAST.newSimpleName("stream");
			sourceRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, parameterSimpleName, null);
			parametersRewrite.insertLast(parameter, null);
			ListRewrite modifiersRewrite = sourceRewriter.getListRewrite(writeObjectMethod, MethodDeclaration.MODIFIERS2_PROPERTY);
			modifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD), null);
			sourceRewriter.set(writeObjectMethod, MethodDeclaration.RETURN_TYPE2_PROPERTY, contextAST.newPrimitiveType(PrimitiveType.VOID), null);
			ListRewrite thrownExceptionTypesRewrite = sourceRewriter.getListRewrite(writeObjectMethod, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
			thrownExceptionTypesRewrite.insertLast(contextAST.newName("java.io.IOException"), null);
			
			Block methodBody = contextAST.newBlock();
			sourceRewriter.set(writeObjectMethod, MethodDeclaration.BODY_PROPERTY, methodBody, null);
			ListRewrite statementRewrite = sourceRewriter.getListRewrite(methodBody, Block.STATEMENTS_PROPERTY);
			
			MethodInvocation methodInvocation = contextAST.newMethodInvocation();
			sourceRewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("defaultWriteObject"), null);
			sourceRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, parameterSimpleName, null);
			ExpressionStatement methodInvocationStatement = contextAST.newExpressionStatement(methodInvocation);
			statementRewrite.insertLast(methodInvocationStatement, null);
			
			Statement methodInvocationStatement2 = createMethodInvocationStatementForWriteObject(
					sourceRewriter, contextAST, modifiedExtractedTypeName, parameterSimpleName);
			statementRewrite.insertLast(methodInvocationStatement2, null);
			
			ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			contextBodyRewrite.insertLast(writeObjectMethod, null);
			try {
				TextEdit sourceEdit = sourceRewriter.rewriteAST();
				ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
				CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
				change.getEdit().addChild(sourceEdit);
				change.addTextEditGroup(new TextEditGroup("Create writeObject()", new TextEdit[] {sourceEdit}));
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
	}

	private Statement createMethodInvocationStatementForWriteObject(ASTRewrite sourceRewriter, AST contextAST,
			String modifiedExtractedTypeName, SimpleName parameterSimpleName) {
		FieldAccess fieldAccess = contextAST.newFieldAccess();
		sourceRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, contextAST.newSimpleName(modifiedExtractedTypeName), null);
		sourceRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, contextAST.newThisExpression(), null);

		MethodInvocation writeObjectMethodInvocation = contextAST.newMethodInvocation();
		sourceRewriter.set(writeObjectMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, parameterSimpleName, null);
		sourceRewriter.set(writeObjectMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("writeObject"), null);
		ListRewrite argumentRewrite = sourceRewriter.getListRewrite(writeObjectMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
		argumentRewrite.insertLast(fieldAccess, null);

		Statement methodInvocationStatement = contextAST.newExpressionStatement(writeObjectMethodInvocation);
		return methodInvocationStatement;
	}

	private void updateReadObjectInSourceClass(String modifiedExtractedTypeName) {
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		AST contextAST = sourceTypeDeclaration.getAST();
		MethodDeclaration[] methodDeclarations = sourceTypeDeclaration.getMethods();
		boolean methodFound = false;
		for(MethodDeclaration method : methodDeclarations) {
			if(isReadObject(method)) {
				methodFound = true;
				if(!extractedMethods.contains(method)) {
					Block methodBody = method.getBody();
					if(methodBody != null) {
						List<SingleVariableDeclaration> parameters = method.parameters();
						SimpleName parameterSimpleName = parameters.get(0).getName();
						ListRewrite statementRewrite = sourceRewriter.getListRewrite(methodBody, Block.STATEMENTS_PROPERTY);
						Statement assignmentStatement = createAssignmentStatementForReadObject(sourceRewriter,
								contextAST, modifiedExtractedTypeName, parameterSimpleName);
						Statement firstStatement = isFirstStatementMethodInvocationExpressionStatementWithName(method, "defaultReadObject");
						if(firstStatement != null) {
							statementRewrite.insertAfter(assignmentStatement, firstStatement, null);
						}
						else {
							statementRewrite.insertFirst(assignmentStatement, null);
						}
						try {
							TextEdit sourceEdit = sourceRewriter.rewriteAST();
							ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
							CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
							change.getEdit().addChild(sourceEdit);
							change.addTextEditGroup(new TextEditGroup("Update readObject()", new TextEdit[] {sourceEdit}));
						} catch (JavaModelException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		if(!methodFound) {
			MethodDeclaration readObjectMethod = contextAST.newMethodDeclaration();
			sourceRewriter.set(readObjectMethod, MethodDeclaration.NAME_PROPERTY, contextAST.newSimpleName("readObject"), null);
			ListRewrite parametersRewrite = sourceRewriter.getListRewrite(readObjectMethod, MethodDeclaration.PARAMETERS_PROPERTY);
			SingleVariableDeclaration parameter = contextAST.newSingleVariableDeclaration();
			Type parameterType = contextAST.newSimpleType(contextAST.newName("java.io.ObjectInputStream"));
			sourceRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, parameterType, null);
			SimpleName parameterSimpleName = contextAST.newSimpleName("stream");
			sourceRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, parameterSimpleName, null);
			parametersRewrite.insertLast(parameter, null);
			ListRewrite modifiersRewrite = sourceRewriter.getListRewrite(readObjectMethod, MethodDeclaration.MODIFIERS2_PROPERTY);
			modifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD), null);
			sourceRewriter.set(readObjectMethod, MethodDeclaration.RETURN_TYPE2_PROPERTY, contextAST.newPrimitiveType(PrimitiveType.VOID), null);
			ListRewrite thrownExceptionTypesRewrite = sourceRewriter.getListRewrite(readObjectMethod, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
			thrownExceptionTypesRewrite.insertLast(contextAST.newName("java.io.IOException"), null);
			thrownExceptionTypesRewrite.insertLast(contextAST.newName("java.lang.ClassNotFoundException"), null);
			
			Block methodBody = contextAST.newBlock();
			sourceRewriter.set(readObjectMethod, MethodDeclaration.BODY_PROPERTY, methodBody, null);
			ListRewrite statementRewrite = sourceRewriter.getListRewrite(methodBody, Block.STATEMENTS_PROPERTY);
			
			MethodInvocation methodInvocation = contextAST.newMethodInvocation();
			sourceRewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("defaultReadObject"), null);
			sourceRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, parameterSimpleName, null);
			ExpressionStatement methodInvocationStatement = contextAST.newExpressionStatement(methodInvocation);
			statementRewrite.insertLast(methodInvocationStatement, null);
			Statement assignmentStatement = createAssignmentStatementForReadObject(sourceRewriter,
					contextAST, modifiedExtractedTypeName, parameterSimpleName);
			statementRewrite.insertLast(assignmentStatement, null);
			
			ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			contextBodyRewrite.insertLast(readObjectMethod, null);
			try {
				TextEdit sourceEdit = sourceRewriter.rewriteAST();
				ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
				CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
				change.getEdit().addChild(sourceEdit);
				change.addTextEditGroup(new TextEditGroup("Create readObject()", new TextEdit[] {sourceEdit}));
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
	}

	private Statement createAssignmentStatementForReadObject(ASTRewrite sourceRewriter, AST contextAST,
			String modifiedExtractedTypeName, SimpleName parameterSimpleName) {
		Assignment assignment = contextAST.newAssignment();
		FieldAccess fieldAccess = contextAST.newFieldAccess();
		sourceRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, contextAST.newSimpleName(modifiedExtractedTypeName), null);
		sourceRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, contextAST.newThisExpression(), null);
		sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, fieldAccess, null);

		MethodInvocation readObjectMethodInvocation = contextAST.newMethodInvocation();
		sourceRewriter.set(readObjectMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, parameterSimpleName, null);
		sourceRewriter.set(readObjectMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("readObject"), null);

		CastExpression castExpression = contextAST.newCastExpression();
		sourceRewriter.set(castExpression, CastExpression.EXPRESSION_PROPERTY, readObjectMethodInvocation, null);
		sourceRewriter.set(castExpression, CastExpression.TYPE_PROPERTY, contextAST.newSimpleName(extractedTypeName), null);
		sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, castExpression, null);

		Statement assignmentStatement = contextAST.newExpressionStatement(assignment);
		return assignmentStatement;
	}

	private void updateCloneInSourceClass(String modifiedExtractedTypeName) {
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		AST contextAST = sourceTypeDeclaration.getAST();
		MethodDeclaration[] methodDeclarations = sourceTypeDeclaration.getMethods();
		boolean methodFound = false;
		for(MethodDeclaration method : methodDeclarations) {
			if(isClone(method)) {
				methodFound = true;
				if(!extractedMethods.contains(method)) {
					Block methodBody = method.getBody();
					if(methodBody != null) {
						ListRewrite statementRewrite = sourceRewriter.getListRewrite(methodBody, Block.STATEMENTS_PROPERTY);
						VariableDeclarationStatement firstStatement = isFirstStatementVariableDeclarationStatementWithSuperCloneInitializer(method);
						if(firstStatement != null) {
							VariableDeclarationFragment fragment = (VariableDeclarationFragment)firstStatement.fragments().get(0);

							Statement assignmentStatement = createAssignmentStatementForClone(sourceRewriter, contextAST,
									fragment.getName().getIdentifier(), modifiedExtractedTypeName);
							statementRewrite.insertAfter(assignmentStatement, firstStatement, null);

							try {
								TextEdit sourceEdit = sourceRewriter.rewriteAST();
								ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
								CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
								change.getEdit().addChild(sourceEdit);
								change.addTextEditGroup(new TextEditGroup("Update clone()", new TextEdit[] {sourceEdit}));
							} catch (JavaModelException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		if(!methodFound) {
			IMethodBinding cloneMethodBinding = findCloneMethod(sourceTypeDeclaration.resolveBinding().getSuperclass());
			if(cloneMethodBinding != null) {
				MethodDeclaration cloneMethodDeclaration = contextAST.newMethodDeclaration();
				sourceRewriter.set(cloneMethodDeclaration, MethodDeclaration.NAME_PROPERTY,
						contextAST.newSimpleName(cloneMethodBinding.getName()), null);
				sourceRewriter.set(cloneMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY,
						RefactoringUtility.generateTypeFromTypeBinding(cloneMethodBinding.getReturnType(), contextAST, sourceRewriter), null);
				
				ListRewrite modifiersRewrite = sourceRewriter.getListRewrite(cloneMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
				modifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
				
				ListRewrite thrownExceptionTypesRewrite = sourceRewriter.getListRewrite(cloneMethodDeclaration, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
				ITypeBinding[] thrownExceptionTypeBindings = cloneMethodBinding.getExceptionTypes();
				for(ITypeBinding typeBinding : thrownExceptionTypeBindings) {
					Name type = contextAST.newName(typeBinding.getQualifiedName());
					thrownExceptionTypesRewrite.insertLast(type, null);
				}
				
				Block body = contextAST.newBlock();
				sourceRewriter.set(cloneMethodDeclaration, MethodDeclaration.BODY_PROPERTY, body, null);
				
				ListRewrite statementsRewrite = sourceRewriter.getListRewrite(body, Block.STATEMENTS_PROPERTY);
				
				VariableDeclarationFragment fragment = contextAST.newVariableDeclarationFragment();
				SimpleName cloneSimpleName = contextAST.newSimpleName("clone");
				Type sourceClassType = RefactoringUtility.generateTypeFromTypeBinding(sourceTypeDeclaration.resolveBinding(), contextAST, sourceRewriter);
				sourceRewriter.set(fragment, VariableDeclarationFragment.NAME_PROPERTY, cloneSimpleName, null);
				
				SuperMethodInvocation superCloneInvocation = contextAST.newSuperMethodInvocation();
				sourceRewriter.set(superCloneInvocation, SuperMethodInvocation.NAME_PROPERTY, cloneSimpleName, null);
				CastExpression castExpression = contextAST.newCastExpression();
				sourceRewriter.set(castExpression, CastExpression.EXPRESSION_PROPERTY, superCloneInvocation, null);
				sourceRewriter.set(castExpression, CastExpression.TYPE_PROPERTY, sourceClassType, null);
				sourceRewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, castExpression, null);
				VariableDeclarationStatement variableDeclarationStatement = contextAST.newVariableDeclarationStatement(fragment);
				sourceRewriter.set(variableDeclarationStatement, VariableDeclarationStatement.TYPE_PROPERTY, sourceClassType, null);
				statementsRewrite.insertLast(variableDeclarationStatement, null);
				
				Statement assignmentStatement = createAssignmentStatementForClone(sourceRewriter, contextAST, "clone", modifiedExtractedTypeName);
				statementsRewrite.insertLast(assignmentStatement, null);
				
				ReturnStatement returnStatement = contextAST.newReturnStatement();
				sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, contextAST.newSimpleName("clone"), null);
				statementsRewrite.insertLast(returnStatement, null);
				
				ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
				contextBodyRewrite.insertLast(cloneMethodDeclaration, null);
				
				try {
					TextEdit sourceEdit = sourceRewriter.rewriteAST();
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
					CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
					change.getEdit().addChild(sourceEdit);
					change.addTextEditGroup(new TextEditGroup("Create clone()", new TextEdit[] {sourceEdit}));
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private Statement createAssignmentStatementForClone(ASTRewrite sourceRewriter, AST contextAST,
			String cloneVariableName, String modifiedExtractedTypeName) {
		Assignment assignment = contextAST.newAssignment();
		FieldAccess cloneFieldAccess = contextAST.newFieldAccess();
		sourceRewriter.set(cloneFieldAccess, FieldAccess.NAME_PROPERTY, contextAST.newSimpleName(modifiedExtractedTypeName), null);
		sourceRewriter.set(cloneFieldAccess, FieldAccess.EXPRESSION_PROPERTY, contextAST.newSimpleName(cloneVariableName), null);
		sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, cloneFieldAccess, null);

		FieldAccess thisFieldAccess = contextAST.newFieldAccess();
		sourceRewriter.set(thisFieldAccess, FieldAccess.NAME_PROPERTY, contextAST.newSimpleName(modifiedExtractedTypeName), null);
		sourceRewriter.set(thisFieldAccess, FieldAccess.EXPRESSION_PROPERTY, contextAST.newThisExpression(), null);

		MethodInvocation cloneInvocation = contextAST.newMethodInvocation();
		sourceRewriter.set(cloneInvocation, MethodInvocation.EXPRESSION_PROPERTY, thisFieldAccess, null);
		sourceRewriter.set(cloneInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("clone"), null);

		CastExpression castExpression = contextAST.newCastExpression();
		sourceRewriter.set(castExpression, CastExpression.EXPRESSION_PROPERTY, cloneInvocation, null);
		sourceRewriter.set(castExpression, CastExpression.TYPE_PROPERTY, contextAST.newSimpleName(extractedTypeName), null);
		sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, castExpression, null);

		Statement assignmentStatement = contextAST.newExpressionStatement(assignment);
		return assignmentStatement;
	}

	private boolean isWriteObject(MethodDeclaration method) {
		return method.getName().getIdentifier().equals("writeObject") && method.parameters().size() == 1 &&
				((SingleVariableDeclaration)method.parameters().get(0)).getType().resolveBinding().getQualifiedName().equals("java.io.ObjectOutputStream");
	}

	private boolean isReadObject(MethodDeclaration method) {
		return method.getName().getIdentifier().equals("readObject") && method.parameters().size() == 1 &&
				((SingleVariableDeclaration)method.parameters().get(0)).getType().resolveBinding().getQualifiedName().equals("java.io.ObjectInputStream");
	}

	private boolean isClone(MethodDeclaration method) {
		return method.getName().getIdentifier().equals("clone") && method.parameters().size() == 0 &&
				method.getReturnType2().resolveBinding().getQualifiedName().equals("java.lang.Object");
	}

	private IMethodBinding findCloneMethod(ITypeBinding typeBinding) {
		if(typeBinding != null && !typeBinding.getQualifiedName().equals("java.lang.Object")) {
			for(IMethodBinding methodBinding : typeBinding.getDeclaredMethods()) {
				if(isClone(methodBinding)) {
					return methodBinding;
				}
			}
			return findCloneMethod(typeBinding.getSuperclass());
		}
		return null;
	}

	private boolean isClone(IMethodBinding methodBinding) {
		return methodBinding.getName().equals("clone") && methodBinding.getParameterTypes().length == 0 &&
				methodBinding.getReturnType().getQualifiedName().equals("java.lang.Object");
	}

	private Statement isFirstStatementMethodInvocationExpressionStatementWithName(MethodDeclaration method, String methodName) {
		Block methodBody = method.getBody();
		if(methodBody != null) {
			List<Statement> statements = methodBody.statements();
			if(!statements.isEmpty()) {
				Statement firstStatement = statements.get(0);
				if(firstStatement instanceof ExpressionStatement) {
					ExpressionStatement expressionStatement = (ExpressionStatement)firstStatement;
					Expression expression = expressionStatement.getExpression();
					if(expression instanceof MethodInvocation) {
						MethodInvocation methodInvocation = (MethodInvocation)expression;
						if(methodInvocation.getName().getIdentifier().equals(methodName)) {
							return firstStatement;
						}
					}
				}
			}
		}
		return null;
	}

	private VariableDeclarationStatement isFirstStatementVariableDeclarationStatementWithSuperCloneInitializer(MethodDeclaration method) {
		Block methodBody = method.getBody();
		if(methodBody != null) {
			List<Statement> statements = methodBody.statements();
			if(!statements.isEmpty()) {
				Statement firstStatement = statements.get(0);
				if(firstStatement instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)firstStatement;
					List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
					if(fragments.size() == 1) {
						for(VariableDeclarationFragment fragment : fragments) {
							if(fragment.getInitializer() != null) {
								Expression expression = fragment.getInitializer();
								if(expression instanceof CastExpression) {
									CastExpression castExpression = (CastExpression)expression;
									if(castExpression.getExpression() instanceof SuperMethodInvocation) {
										SuperMethodInvocation superMethodInvocation = (SuperMethodInvocation)castExpression.getExpression();
										if(superMethodInvocation.getName().getIdentifier().equals("clone") &&
												superMethodInvocation.arguments().size() == 0) {
											return variableDeclarationStatement;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	private void removeFieldFragmentsInSourceClass(Set<VariableDeclaration> fieldFragments) {
		FieldDeclaration[] fieldDeclarations = sourceTypeDeclaration.getFields();
		for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
			List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
			int actualNumberOfFragments = fragments.size();
			Set<VariableDeclaration> fragmentsToBeRemoved = new LinkedHashSet<VariableDeclaration>();
			for(VariableDeclarationFragment fragment : fragments) {
				if(fieldFragments.contains(fragment)) {
					fragmentsToBeRemoved.add(fragment);
				}
			}
			if(fragmentsToBeRemoved.size() > 0) {
				ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
				ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
				if(actualNumberOfFragments == fragmentsToBeRemoved.size()) {
					contextBodyRewrite.remove(fieldDeclaration, null);
				}
				else if(fragmentsToBeRemoved.size() < actualNumberOfFragments) {
					ListRewrite fragmentRewrite = sourceRewriter.getListRewrite(fieldDeclaration, FieldDeclaration.FRAGMENTS_PROPERTY);
					for(VariableDeclaration fragment : fragmentsToBeRemoved) {
						fragmentRewrite.remove(fragment, null);
					}
				}
				try {
					TextEdit sourceEdit = sourceRewriter.rewriteAST();
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
					CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
					change.getEdit().addChild(sourceEdit);
					change.addTextEditGroup(new TextEditGroup("Remove extracted field", new TextEdit[] {sourceEdit}));
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void modifyExtractedFieldAssignmentsInSourceClass(Set<VariableDeclaration> fieldFragments, Set<VariableDeclaration> modifiedFields, Set<VariableDeclaration> accessedFields) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		Set<MethodDeclaration> contextMethods = getAllMethodDeclarationsInSourceClass();
		String modifiedExtractedTypeName = extractedTypeName.substring(0,1).toLowerCase() + extractedTypeName.substring(1,extractedTypeName.length());
		for(MethodDeclaration methodDeclaration : contextMethods) {
			if(!extractedMethods.contains(methodDeclaration)) {
				Block methodBody = methodDeclaration.getBody();
				if(methodBody != null) {
					List<Statement> statements = methodBody.statements();
					for(Statement statement : statements) {
						ASTRewrite sourceRewriter = null;
						if(statementRewriteMap.containsKey(statement)) {
							sourceRewriter = statementRewriteMap.get(statement);
						}
						else {
							sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
						}
						AST contextAST = sourceTypeDeclaration.getAST();
						boolean rewriteAST = false;
						List<Expression> assignments = expressionExtractor.getAssignments(statement);
						for(Expression expression : assignments) {
							Assignment assignment = (Assignment)expression;
							Expression leftHandSide = assignment.getLeftHandSide();
							SimpleName assignedVariable = null;
							if(leftHandSide instanceof SimpleName) {
								assignedVariable = (SimpleName)leftHandSide;
							}
							else if(leftHandSide instanceof FieldAccess) {
								FieldAccess fieldAccess = (FieldAccess)leftHandSide;
								assignedVariable = fieldAccess.getName();
							}
							else if(leftHandSide instanceof QualifiedName) {
								QualifiedName qualifiedName = (QualifiedName)leftHandSide;
								assignedVariable = qualifiedName.getName();
							}
							Expression rightHandSide = assignment.getRightHandSide();
							List<Expression> accessedVariables = expressionExtractor.getVariableInstructions(rightHandSide);
							List<Expression> arrayAccesses = expressionExtractor.getArrayAccesses(leftHandSide);
							for(VariableDeclaration fieldFragment : fieldFragments) {
								String originalFieldName = fieldFragment.getName().getIdentifier();
								String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
								String getterMethodName = GETTER_PREFIX + modifiedFieldName;
								getterMethodName = appendAccessorMethodSuffix(getterMethodName, this.extractedMethods);
								if(assignedVariable != null) {
									IBinding leftHandBinding = assignedVariable.resolveBinding();
									if(leftHandBinding != null && leftHandBinding.getKind() == IBinding.VARIABLE) {
										IVariableBinding assignedVariableBinding = (IVariableBinding)leftHandBinding;
										if(assignedVariableBinding.isField() && fieldFragment.resolveBinding().isEqualTo(assignedVariableBinding)) {
											if(methodDeclaration.isConstructor() && (assignedVariableBinding.getModifiers() & Modifier.FINAL) != 0) {
												if(assignment.getParent() instanceof ExpressionStatement) {
													ExpressionStatement assignmentStatement = (ExpressionStatement)assignment.getParent();
													ListRewrite constructorStatementsRewrite = sourceRewriter.getListRewrite(methodDeclaration.getBody(), Block.STATEMENTS_PROPERTY);
													constructorStatementsRewrite.remove(assignmentStatement, null);
													if(constructorFinalFieldAssignmentMap.containsKey(methodDeclaration)) {
														Map<VariableDeclaration, Assignment> finalFieldAssignmentMap = constructorFinalFieldAssignmentMap.get(methodDeclaration);
														finalFieldAssignmentMap.put(fieldFragment, assignment);
													}
													else {
														Map<VariableDeclaration, Assignment> finalFieldAssignmentMap = new LinkedHashMap<VariableDeclaration, Assignment>();
														finalFieldAssignmentMap.put(fieldFragment, assignment);
														constructorFinalFieldAssignmentMap.put(methodDeclaration, finalFieldAssignmentMap);
													}
												}
											}
											else {
												MethodInvocation setterMethodInvocation = contextAST.newMethodInvocation();
												String setterMethodName = SETTER_PREFIX + modifiedFieldName;
												setterMethodName = appendAccessorMethodSuffix(setterMethodName, this.extractedMethods);
												sourceRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName(setterMethodName), null);
												ListRewrite setterMethodInvocationArgumentsRewrite = sourceRewriter.getListRewrite(setterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
												if(!assignment.getOperator().equals(Assignment.Operator.ASSIGN)) {
													accessedFields.add(fieldFragment);
													InfixExpression infixExpression = contextAST.newInfixExpression();
													MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
													sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName(getterMethodName), null);
													if((assignedVariableBinding.getModifiers() & Modifier.STATIC) != 0) {
														sourceRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(extractedTypeName), null);
													}
													else {
														sourceRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(modifiedExtractedTypeName), null);
													}
													sourceRewriter.set(infixExpression, InfixExpression.LEFT_OPERAND_PROPERTY, getterMethodInvocation, null);
													sourceRewriter.set(infixExpression, InfixExpression.RIGHT_OPERAND_PROPERTY, assignment.getRightHandSide(), null);
													if(assignment.getOperator().equals(Assignment.Operator.PLUS_ASSIGN)) {
														sourceRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.PLUS, null);
													}
													else if(assignment.getOperator().equals(Assignment.Operator.MINUS_ASSIGN)) {
														sourceRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.MINUS, null);
													}
													else if(assignment.getOperator().equals(Assignment.Operator.TIMES_ASSIGN)) {
														sourceRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.TIMES, null);
													}
													else if(assignment.getOperator().equals(Assignment.Operator.DIVIDE_ASSIGN)) {
														sourceRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.DIVIDE, null);
													}
													else if(assignment.getOperator().equals(Assignment.Operator.REMAINDER_ASSIGN)) {
														sourceRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.REMAINDER, null);
													}
													else if(assignment.getOperator().equals(Assignment.Operator.BIT_AND_ASSIGN)) {
														sourceRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.AND, null);
													}
													else if(assignment.getOperator().equals(Assignment.Operator.BIT_OR_ASSIGN)) {
														sourceRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.OR, null);
													}
													else if(assignment.getOperator().equals(Assignment.Operator.BIT_XOR_ASSIGN)) {
														sourceRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.XOR, null);
													}
													else if(assignment.getOperator().equals(Assignment.Operator.LEFT_SHIFT_ASSIGN)) {
														sourceRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.LEFT_SHIFT, null);
													}
													else if(assignment.getOperator().equals(Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN)) {
														sourceRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.RIGHT_SHIFT_SIGNED, null);
													}
													else if(assignment.getOperator().equals(Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN)) {
														sourceRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED, null);
													}
													setterMethodInvocationArgumentsRewrite.insertLast(infixExpression, null);
												}
												else {
													setterMethodInvocationArgumentsRewrite.insertLast(assignment.getRightHandSide(), null);
												}
												if(leftHandSide instanceof QualifiedName) {
													Name qualifier = contextAST.newName(((QualifiedName)leftHandSide).getQualifier().getFullyQualifiedName());
													QualifiedName qualifiedName = contextAST.newQualifiedName(qualifier, contextAST.newSimpleName(modifiedExtractedTypeName));
													sourceRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, qualifiedName, null);
												}
												else if((assignedVariableBinding.getModifiers() & Modifier.STATIC) != 0) {
													sourceRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(extractedTypeName), null);
												}
												else {
													sourceRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(modifiedExtractedTypeName), null);
												}
												sourceRewriter.replace(assignment, setterMethodInvocation, null);
											}
											rewriteAST = true;
											modifiedFields.add(fieldFragment);
										}
									}
								}
								for(Expression expression2 : arrayAccesses) {
									ArrayAccess arrayAccess = (ArrayAccess)expression2;
									Expression arrayExpression = arrayAccess.getArray();
									SimpleName arrayVariable = null;
									if(arrayExpression instanceof SimpleName) {
										arrayVariable = (SimpleName)arrayExpression;
									}
									else if(arrayExpression instanceof FieldAccess) {
										FieldAccess fieldAccess = (FieldAccess)arrayExpression;
										arrayVariable = fieldAccess.getName();
									}
									else if(arrayExpression instanceof QualifiedName) {
										QualifiedName qualifiedName = (QualifiedName)arrayExpression;
										arrayVariable = qualifiedName.getName();
									}
									if(arrayVariable != null) {
										IBinding arrayBinding = arrayVariable.resolveBinding();
										if(arrayBinding != null && arrayBinding.getKind() == IBinding.VARIABLE) {
											IVariableBinding arrayVariableBinding = (IVariableBinding)arrayBinding;
											if(arrayVariableBinding.isField() && fieldFragment.resolveBinding().isEqualTo(arrayVariableBinding)) {
												MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
												sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName(getterMethodName), null);
												if((arrayVariableBinding.getModifiers() & Modifier.STATIC) != 0) {
													sourceRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(extractedTypeName), null);
												}
												else {
													sourceRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(modifiedExtractedTypeName), null);
												}
												sourceRewriter.replace(arrayVariable, getterMethodInvocation, null);
												rewriteAST = true;
												accessedFields.add(fieldFragment);
											}
										}
									}
								}
								for(Expression expression2 : accessedVariables) {
									SimpleName accessedVariable = (SimpleName)expression2;
									IBinding rightHandBinding = accessedVariable.resolveBinding();
									if(rightHandBinding != null && rightHandBinding.getKind() == IBinding.VARIABLE) {
										IVariableBinding accessedVariableBinding = (IVariableBinding)rightHandBinding;
										if(accessedVariableBinding.isField() && fieldFragment.resolveBinding().isEqualTo(accessedVariableBinding)) {
											MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
											sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName(getterMethodName), null);
											if((accessedVariableBinding.getModifiers() & Modifier.STATIC) != 0) {
												sourceRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(extractedTypeName), null);
											}
											else {
												sourceRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(modifiedExtractedTypeName), null);
											}
											sourceRewriter.replace(accessedVariable, getterMethodInvocation, null);
											rewriteAST = true;
											accessedFields.add(fieldFragment);
										}
									}
								}
							}
						}
						List<Expression> postfixExpressions = expressionExtractor.getPostfixExpressions(statement);
						for(Expression expression : postfixExpressions) {
							PostfixExpression postfix = (PostfixExpression)expression;
							Expression operand = postfix.getOperand();
							SimpleName assignedVariable = null;
							if(operand instanceof SimpleName) {
								assignedVariable = (SimpleName)operand;
							}
							else if(operand instanceof FieldAccess) {
								FieldAccess fieldAccess = (FieldAccess)operand;
								assignedVariable = fieldAccess.getName();
							}
							else if(operand instanceof QualifiedName) {
								QualifiedName qualifiedName = (QualifiedName)operand;
								assignedVariable = qualifiedName.getName();
							}
							List<Expression> arrayAccesses = expressionExtractor.getArrayAccesses(operand);
							for(VariableDeclaration fieldFragment : fieldFragments) {
								String originalFieldName = fieldFragment.getName().getIdentifier();
								String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
								String getterMethodName = GETTER_PREFIX + modifiedFieldName;
								getterMethodName = appendAccessorMethodSuffix(getterMethodName, this.extractedMethods);
								if(assignedVariable != null) {
									IBinding operandBinding = assignedVariable.resolveBinding();
									if(operandBinding != null && operandBinding.getKind() == IBinding.VARIABLE) {
										IVariableBinding assignedVariableBinding = (IVariableBinding)operandBinding;
										if(assignedVariableBinding.isField() && fieldFragment.resolveBinding().isEqualTo(assignedVariableBinding)) {
											MethodInvocation setterMethodInvocation = contextAST.newMethodInvocation();
											String setterMethodName = SETTER_PREFIX + modifiedFieldName;
											setterMethodName = appendAccessorMethodSuffix(setterMethodName, this.extractedMethods);
											sourceRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName(setterMethodName), null);
											ListRewrite setterMethodInvocationArgumentsRewrite = sourceRewriter.getListRewrite(setterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
											accessedFields.add(fieldFragment);
											InfixExpression infixExpression = contextAST.newInfixExpression();
											MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
											sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName(getterMethodName), null);
											if((assignedVariableBinding.getModifiers() & Modifier.STATIC) != 0) {
												sourceRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(extractedTypeName), null);
											}
											else {
												sourceRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(modifiedExtractedTypeName), null);
											}
											sourceRewriter.set(infixExpression, InfixExpression.LEFT_OPERAND_PROPERTY, getterMethodInvocation, null);
											sourceRewriter.set(infixExpression, InfixExpression.RIGHT_OPERAND_PROPERTY, contextAST.newNumberLiteral("1"), null);
											if(postfix.getOperator().equals(PostfixExpression.Operator.INCREMENT)) {
												sourceRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.PLUS, null);
											}
											else if(postfix.getOperator().equals(PostfixExpression.Operator.DECREMENT)) {
												sourceRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.MINUS, null);
											}
											setterMethodInvocationArgumentsRewrite.insertLast(infixExpression, null);
											if(operand instanceof QualifiedName) {
												Name qualifier = contextAST.newName(((QualifiedName)operand).getQualifier().getFullyQualifiedName());
												QualifiedName qualifiedName = contextAST.newQualifiedName(qualifier, contextAST.newSimpleName(modifiedExtractedTypeName));
												sourceRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, qualifiedName, null);
											}
											else if((assignedVariableBinding.getModifiers() & Modifier.STATIC) != 0) {
												sourceRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(extractedTypeName), null);
											}
											else {
												sourceRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(modifiedExtractedTypeName), null);
											}
											sourceRewriter.replace(postfix, setterMethodInvocation, null);
											rewriteAST = true;
											modifiedFields.add(fieldFragment);
										}
									}
								}
								for(Expression expression2 : arrayAccesses) {
									ArrayAccess arrayAccess = (ArrayAccess)expression2;
									Expression arrayExpression = arrayAccess.getArray();
									SimpleName arrayVariable = null;
									if(arrayExpression instanceof SimpleName) {
										arrayVariable = (SimpleName)arrayExpression;
									}
									else if(arrayExpression instanceof FieldAccess) {
										FieldAccess fieldAccess = (FieldAccess)arrayExpression;
										arrayVariable = fieldAccess.getName();
									}
									else if(arrayExpression instanceof QualifiedName) {
										QualifiedName qualifiedName = (QualifiedName)arrayExpression;
										arrayVariable = qualifiedName.getName();
									}
									if(arrayVariable != null) {
										IBinding arrayBinding = arrayVariable.resolveBinding();
										if(arrayBinding != null && arrayBinding.getKind() == IBinding.VARIABLE) {
											IVariableBinding arrayVariableBinding = (IVariableBinding)arrayBinding;
											if(arrayVariableBinding.isField() && fieldFragment.resolveBinding().isEqualTo(arrayVariableBinding)) {
												MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
												sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName(getterMethodName), null);
												if((arrayVariableBinding.getModifiers() & Modifier.STATIC) != 0) {
													sourceRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(extractedTypeName), null);
												}
												else {
													sourceRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(modifiedExtractedTypeName), null);
												}
												sourceRewriter.replace(arrayVariable, getterMethodInvocation, null);
												rewriteAST = true;
												accessedFields.add(fieldFragment);
											}
										}
									}
								}
							}
						}
						if(rewriteAST) {
							if(!statementRewriteMap.containsKey(statement))
								statementRewriteMap.put(statement, sourceRewriter);
							/*try {
								TextEdit sourceEdit = sourceRewriter.rewriteAST();
								ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
								CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
								change.getEdit().addChild(sourceEdit);
								change.addTextEditGroup(new TextEditGroup("Replace field assignment with invocation of setter method", new TextEdit[] {sourceEdit}));
							} catch (JavaModelException e) {
								e.printStackTrace();
							}*/
						}
					}
				}
			}
		}
	}

	private void modifyExtractedFieldAccessesInSourceClass(Set<VariableDeclaration> fieldFragments, Set<VariableDeclaration> accessedFields) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		Set<MethodDeclaration> contextMethods = getAllMethodDeclarationsInSourceClass();
		String modifiedExtractedTypeName = extractedTypeName.substring(0,1).toLowerCase() + extractedTypeName.substring(1,extractedTypeName.length());
		for(MethodDeclaration methodDeclaration : contextMethods) {
			if(!extractedMethods.contains(methodDeclaration)) {
				Block methodBody = methodDeclaration.getBody();
				if(methodBody != null) {
					List<Statement> statements = methodBody.statements();
					for(Statement statement : statements) {
						ASTRewrite sourceRewriter = null;
						if(statementRewriteMap.containsKey(statement)) {
							sourceRewriter = statementRewriteMap.get(statement);
						}
						else {
							sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
						}
						AST contextAST = sourceTypeDeclaration.getAST();
						boolean rewriteAST = false;
						List<Expression> accessedVariables = expressionExtractor.getVariableInstructions(statement);
						List<Expression> arrayAccesses = expressionExtractor.getArrayAccesses(statement);
						for(VariableDeclaration fieldFragment : fieldFragments) {
							String originalFieldName = fieldFragment.getName().getIdentifier();
							String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
							String getterMethodName = GETTER_PREFIX + modifiedFieldName;
							getterMethodName = appendAccessorMethodSuffix(getterMethodName, this.extractedMethods);
							for(Expression expression : accessedVariables) {
								SimpleName accessedVariable = (SimpleName)expression;
								IBinding binding = accessedVariable.resolveBinding();
								if(binding != null && binding.getKind() == IBinding.VARIABLE) {
									IVariableBinding accessedVariableBinding = (IVariableBinding)binding;
									if(accessedVariableBinding.isField() && fieldFragment.resolveBinding().isEqualTo(accessedVariableBinding)) {
										if(!isAssignmentChild(expression)) {
											MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
											sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName(getterMethodName), null);
											if((accessedVariableBinding.getModifiers() & Modifier.STATIC) != 0) {
												sourceRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(extractedTypeName), null);
											}
											else {
												sourceRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(modifiedExtractedTypeName), null);
											}
											sourceRewriter.replace(accessedVariable, getterMethodInvocation, null);
											rewriteAST = true;
											accessedFields.add(fieldFragment);
										}
									}
								}
							}
							for(Expression expression : arrayAccesses) {
								ArrayAccess arrayAccess = (ArrayAccess)expression;
								Expression arrayExpression = arrayAccess.getArray();
								SimpleName arrayVariable = null;
								if(arrayExpression instanceof SimpleName) {
									arrayVariable = (SimpleName)arrayExpression;
								}
								else if(arrayExpression instanceof FieldAccess) {
									FieldAccess fieldAccess = (FieldAccess)arrayExpression;
									arrayVariable = fieldAccess.getName();
								}
								if(arrayVariable != null) {
									IBinding arrayBinding = arrayVariable.resolveBinding();
									if(arrayBinding != null && arrayBinding.getKind() == IBinding.VARIABLE) {
										IVariableBinding arrayVariableBinding = (IVariableBinding)arrayBinding;
										if(arrayVariableBinding.isField() && fieldFragment.resolveBinding().isEqualTo(arrayVariableBinding)) {
											if(!isAssignmentChild(expression)) {
												MethodInvocation getterMethodInvocation = contextAST.newMethodInvocation();
												sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName(getterMethodName), null);
												if((arrayVariableBinding.getModifiers() & Modifier.STATIC) != 0) {
													sourceRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(extractedTypeName), null);
												}
												else {
													sourceRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName(modifiedExtractedTypeName), null);
												}
												sourceRewriter.replace(arrayVariable, getterMethodInvocation, null);
												rewriteAST = true;
												accessedFields.add(fieldFragment);
											}
										}
									}
								}
							}
						}
						if(rewriteAST) {
							if(!statementRewriteMap.containsKey(statement))
								statementRewriteMap.put(statement, sourceRewriter);
							/*try {
								TextEdit sourceEdit = sourceRewriter.rewriteAST();
								ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
								CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
								change.getEdit().addChild(sourceEdit);
								change.addTextEditGroup(new TextEditGroup("Replace field access with invocation of getter method", new TextEdit[] {sourceEdit}));
							} catch (JavaModelException e) {
								e.printStackTrace();
							}*/
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
		else if(node instanceof PrefixExpression) {
			PrefixExpression prefixExpression = (PrefixExpression)node;
			if(prefixExpression.getOperator().equals(PrefixExpression.Operator.INCREMENT) ||
					prefixExpression.getOperator().equals(PrefixExpression.Operator.DECREMENT))
				return true;
			else
				return isAssignmentChild(node.getParent());
		}
		else if(node instanceof PostfixExpression) {
			PostfixExpression postfixExpression = (PostfixExpression)node;
			if(postfixExpression.getOperator().equals(PostfixExpression.Operator.INCREMENT) ||
					postfixExpression.getOperator().equals(PostfixExpression.Operator.DECREMENT))
				return true;
			else
				return isAssignmentChild(node.getParent());
		}
		else if(node instanceof Statement)
			return false;
		else
			return isAssignmentChild(node.getParent());
	}

	@Override
	public String getName() {
		return "Extract Class";
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
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
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
					String description = MessageFormat.format("Extracting class from ''{0}''", new Object[] { sourceTypeDeclaration.getName().getIdentifier()});
					String comment = null;
					return new RefactoringChangeDescriptor(new ExtractClassRefactoringDescriptor(project, description, comment,
							sourceCompilationUnit, sourceTypeDeclaration, sourceFile, extractedFieldFragments, extractedMethods, delegateMethods, extractedTypeName));
				}
			};
			return change;
		} finally {
			pm.done();
		}
	}
}
