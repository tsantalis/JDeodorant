package gr.uom.java.jdeodorant.refactoring.manipulators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.decomposition.cfg.mapping.CloneRefactoringType;
import gr.uom.java.ast.decomposition.cfg.mapping.CloneStructureNode;
import gr.uom.java.ast.decomposition.cfg.mapping.DivideAndConquerMatcher;
import gr.uom.java.ast.decomposition.cfg.mapping.NodeMapping;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGExpressionGap;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGNodeBlockGap;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGNodeGap;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGNodeMapping;
import gr.uom.java.ast.decomposition.cfg.mapping.VariableBindingKeyPair;
import gr.uom.java.ast.decomposition.cfg.mapping.VariableBindingPair;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.ExpressionPreconditionViolation;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.matching.BindingSignaturePair;
import gr.uom.java.ast.decomposition.matching.DifferenceType;
import gr.uom.java.ast.util.ExpressionExtractor;

public class ExtractCloneRefactoringGapHandler {
	
	private static final String FUNCTIONAL_INTERFACE_METHOD_NAME = "apply";
	private DivideAndConquerMatcher mapper;
	private Set<PDGNodeMapping> sortedNodeMappings;
	private Set<VariableBindingPair> nonEffectivelyFinalLocalVariables;
	private List<TypeDeclaration> sourceTypeDeclarations;
	private List<MethodDeclaration> sourceMethodDeclarations;
	
	public ExtractCloneRefactoringGapHandler(DivideAndConquerMatcher mapper, MethodDeclaration methodDeclaration1, MethodDeclaration methodDeclaration2) {
		this.mapper = mapper;
		this.sortedNodeMappings = mapper.getMaximumStateWithMinimumDifferences().getSortedNodeMappings();
		this.nonEffectivelyFinalLocalVariables = new LinkedHashSet<VariableBindingPair>();
		this.sourceTypeDeclarations = new ArrayList<TypeDeclaration>();
		this.sourceMethodDeclarations = new ArrayList<MethodDeclaration>();
		
		this.sourceMethodDeclarations.add(methodDeclaration1);
		this.sourceMethodDeclarations.add(methodDeclaration2);
		if(methodDeclaration1.getParent() instanceof TypeDeclaration && methodDeclaration2.getParent() instanceof TypeDeclaration) { 
			this.sourceTypeDeclarations.add((TypeDeclaration)methodDeclaration1.getParent());
			this.sourceTypeDeclarations.add((TypeDeclaration)methodDeclaration2.getParent());
		}
		
		for(PDGExpressionGap expressionGap : mapper.getRefactorableExpressionGaps()) {
			nonEffectivelyFinalLocalVariables.addAll(expressionGap.getNonEffectivelyFinalLocalVariableBindings());
		}
		for(PDGNodeBlockGap blockGap : mapper.getRefactorableBlockGaps()) {
			nonEffectivelyFinalLocalVariables.addAll(blockGap.getNonEffectivelyFinalLocalVariableBindings());
		}
	}

	public Type createParameterForFunctionalInterface(PDGNodeBlockGap blockGap, ASTRewrite sourceRewriter, AST ast, ListRewrite bodyDeclarationsRewrite, Set<ITypeBinding> requiredImportTypeBindings, int i) {
		ITypeBinding returnTypeBinding = blockGap.getReturnType();
		Set<VariableBindingPair> parameterTypeBindings = blockGap.getParameterBindings();
		Set<ITypeBinding> thrownExceptionTypeBindings = blockGap.getThrownExceptions();
		Type interfaceType = null;
		if(returnTypeBinding != null) {
			//introduce java.util.function.Function or a custom FunctionalInterface
			Type returnType = RefactoringUtility.generateTypeFromTypeBinding(returnTypeBinding, ast, sourceRewriter);
			Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
			typeBindings.add(returnTypeBinding);
			RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
			if(parameterTypeBindings.size() == 1 && thrownExceptionTypeBindings.isEmpty()) {
				interfaceType = createFunction(sourceRewriter, ast, parameterTypeBindings, returnType, returnTypeBinding, requiredImportTypeBindings);
			}
			else if(parameterTypeBindings.size() == 0 && thrownExceptionTypeBindings.isEmpty()) {
				interfaceType = createSupplier(sourceRewriter, ast, returnType, returnTypeBinding, requiredImportTypeBindings);
			}
			else {
				interfaceType = createFunctionalInterface(sourceRewriter, ast, parameterTypeBindings, returnType, thrownExceptionTypeBindings, bodyDeclarationsRewrite, requiredImportTypeBindings, i);
			}
		}
		else {
			//introduce a java.util.function.Consumer or a custom FunctionalInterface
			if(parameterTypeBindings.size() == 1 && thrownExceptionTypeBindings.isEmpty()) {
				interfaceType = createConsumer(sourceRewriter, ast, parameterTypeBindings, requiredImportTypeBindings);
			}
			else {
				interfaceType = createFunctionalInterface(sourceRewriter, ast, parameterTypeBindings, null, thrownExceptionTypeBindings, bodyDeclarationsRewrite, requiredImportTypeBindings, i);
			}
		}
		return interfaceType;
	}

	private Type createFunction(ASTRewrite sourceRewriter, AST ast, Set<VariableBindingPair> parameterTypeBindings, Type type,
			ITypeBinding typeBinding, Set<ITypeBinding> requiredImportTypeBindings) {
		//introduce java.util.function.Function
		SimpleName interfaceName = ast.newSimpleName("Function");
		ParameterizedType parameterizedType = ast.newParameterizedType(ast.newSimpleType(interfaceName));
		ListRewrite typeArgumentsRewrite = sourceRewriter.getListRewrite(parameterizedType, ParameterizedType.TYPE_ARGUMENTS_PROPERTY);
		//add first the type of the input to the function
		processTypeArgumentsOfParameterizedType(typeArgumentsRewrite, sourceRewriter, ast, parameterTypeBindings, requiredImportTypeBindings);
		//add second the type of the result of the function
		if(typeBinding.isPrimitive()) {
			typeArgumentsRewrite.insertLast(RefactoringUtility.generateWrapperTypeForPrimitiveTypeBinding(typeBinding, ast), null);
		}
		else {
			typeArgumentsRewrite.insertLast(type, null);
		}
		return parameterizedType;
	}

	private Type createSupplier(ASTRewrite sourceRewriter, AST ast, Type type, ITypeBinding typeBinding, Set<ITypeBinding> requiredImportTypeBindings) {
		//introduce java.util.function.Supplier
		SimpleName interfaceName = ast.newSimpleName("Supplier");
		ParameterizedType parameterizedType = ast.newParameterizedType(ast.newSimpleType(interfaceName));
		ListRewrite typeArgumentsRewrite = sourceRewriter.getListRewrite(parameterizedType, ParameterizedType.TYPE_ARGUMENTS_PROPERTY);
		//add the type of the result of the function
		if(typeBinding.isPrimitive()) {
			typeArgumentsRewrite.insertLast(RefactoringUtility.generateWrapperTypeForPrimitiveTypeBinding(typeBinding, ast), null);
		}
		else {
			typeArgumentsRewrite.insertLast(type, null);
		}
		return parameterizedType;
	}

	private Type createConsumer(ASTRewrite sourceRewriter, AST ast, Set<VariableBindingPair> parameterTypeBindings, Set<ITypeBinding> requiredImportTypeBindings) {
		//introduce java.util.function.Consumer
		SimpleName interfaceName = ast.newSimpleName("Consumer");
		ParameterizedType parameterizedType = ast.newParameterizedType(ast.newSimpleType(interfaceName));
		ListRewrite typeArgumentsRewrite = sourceRewriter.getListRewrite(parameterizedType, ParameterizedType.TYPE_ARGUMENTS_PROPERTY);
		//add first the type of the input to the function
		processTypeArgumentsOfParameterizedType(typeArgumentsRewrite, sourceRewriter, ast, parameterTypeBindings, requiredImportTypeBindings);
		return parameterizedType;
	}

	private void processTypeArgumentsOfParameterizedType(ListRewrite typeArgumentsRewrite, ASTRewrite sourceRewriter,
			AST ast, Set<VariableBindingPair> parameterTypeBindings, Set<ITypeBinding> requiredImportTypeBindings) {
		for(VariableBindingPair variableBindingPair : parameterTypeBindings) {
			IVariableBinding variableBinding1 = variableBindingPair.getBinding1();
			IVariableBinding variableBinding2 = variableBindingPair.getBinding2();
			ITypeBinding typeBinding1 = variableBinding1.getType();
			ITypeBinding typeBinding2 = variableBinding2.getType();
			ITypeBinding variableBindingType = determineType(typeBinding1, typeBinding2);
			Type parameterType = null;
			if(variableBindingType.isPrimitive()) {
				parameterType = RefactoringUtility.generateWrapperTypeForPrimitiveTypeBinding(variableBindingType, ast);
			}
			else {
				parameterType = variableBindingPair.hasQualifiedType() ? RefactoringUtility.generateQualifiedTypeFromTypeBinding(variableBindingType, ast, sourceRewriter) :
					RefactoringUtility.generateTypeFromTypeBinding(variableBindingType, ast, sourceRewriter);
			}
			Set<ITypeBinding> typeBindings2 = new LinkedHashSet<ITypeBinding>();
			typeBindings2.add(variableBindingType);
			RefactoringUtility.getSimpleTypeBindings(typeBindings2, requiredImportTypeBindings);	
			typeArgumentsRewrite.insertLast(parameterType, null);
		}
	}

	private ITypeBinding determineType(ITypeBinding typeBinding1, ITypeBinding typeBinding2) {
		if(typeBinding1 != null && typeBinding2 != null) {
			if(typeBinding1.getQualifiedName().equals("null") && !typeBinding2.getQualifiedName().equals("null")) {
				return typeBinding2;
			}
			else if(typeBinding2.getQualifiedName().equals("null") && !typeBinding1.getQualifiedName().equals("null")) {
				return typeBinding1;
			}
			else if(typeBinding1.isEqualTo(typeBinding2)) {
				return typeBinding1;
			}
			else {
				ITypeBinding typeBinding = ASTNodeMatcher.commonSuperType(typeBinding1, typeBinding2);
				return typeBinding;
			}
		}
		else if(typeBinding1 == null && typeBinding2 != null) {
			return typeBinding2;
		}
		else if(typeBinding2 == null && typeBinding1 != null) {
			return typeBinding1;
		}
		return null;
	}

	private Type createFunctionalInterface(ASTRewrite sourceRewriter, AST ast, Set<VariableBindingPair> parameterTypeBindings, Type returnType, Set<ITypeBinding> thrownExceptionTypeBindings,
			ListRewrite bodyDeclarationsRewrite, Set<ITypeBinding> requiredImportTypeBindings, int i) {
		//introduce a new custom functional interface
		TypeDeclaration interfaceTypeDeclaration = ast.newTypeDeclaration();
		sourceRewriter.set(interfaceTypeDeclaration, TypeDeclaration.INTERFACE_PROPERTY, true, null);
		SimpleName interfaceName = ast.newSimpleName("Interface" + i);
		sourceRewriter.set(interfaceTypeDeclaration, TypeDeclaration.NAME_PROPERTY, interfaceName, null);
		ListRewrite interfaceTypeDeclarationModifiersRewrite = sourceRewriter.getListRewrite(interfaceTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
		MarkerAnnotation markerAnnotation = ast.newMarkerAnnotation();
		sourceRewriter.set(markerAnnotation, MarkerAnnotation.TYPE_NAME_PROPERTY, ast.newSimpleName("FunctionalInterface"), null);
		interfaceTypeDeclarationModifiersRewrite.insertLast(markerAnnotation, null);
		if(sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclarations.get(1).resolveBinding()) &&
				sourceTypeDeclarations.get(0).resolveBinding().getQualifiedName().equals(sourceTypeDeclarations.get(1).resolveBinding().getQualifiedName())) {
			interfaceTypeDeclarationModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD), null);
		}
		else if(mapper.getCloneRefactoringType().equals(CloneRefactoringType.EXTRACT_STATIC_METHOD_TO_NEW_UTILITY_CLASS)) {
			interfaceTypeDeclarationModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
		}
		else {
			interfaceTypeDeclarationModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD), null);
		}
		ListRewrite interfaceBodyDeclarationRewrite = sourceRewriter.getListRewrite(interfaceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		MethodDeclaration interfaceMethodDeclaration = ast.newMethodDeclaration();
		sourceRewriter.set(interfaceMethodDeclaration, MethodDeclaration.NAME_PROPERTY, ast.newSimpleName(FUNCTIONAL_INTERFACE_METHOD_NAME), null);
		if(returnType != null) {
			sourceRewriter.set(interfaceMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnType, null);
		}
		else {
			sourceRewriter.set(interfaceMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, ast.newPrimitiveType(PrimitiveType.VOID), null);
		}
		ListRewrite interfaceMethodDeclarationParameterRewrite = sourceRewriter.getListRewrite(interfaceMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		for(VariableBindingPair variableBindingPair : parameterTypeBindings) {
			IVariableBinding variableBinding1 = variableBindingPair.getBinding1();
			IVariableBinding variableBinding2 = variableBindingPair.getBinding2();
			ITypeBinding typeBinding1 = variableBinding1.getType();
			ITypeBinding typeBinding2 = variableBinding2.getType();
			ITypeBinding variableBindingType = determineType(typeBinding1, typeBinding2);
			Type parameterType = variableBindingPair.hasQualifiedType() ? RefactoringUtility.generateQualifiedTypeFromTypeBinding(variableBindingType, ast, sourceRewriter) :
				RefactoringUtility.generateTypeFromTypeBinding(variableBindingType, ast, sourceRewriter);
			Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
			typeBindings.add(variableBindingType);
			RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);	
			SingleVariableDeclaration parameterDeclaration = ast.newSingleVariableDeclaration();
			sourceRewriter.set(parameterDeclaration, SingleVariableDeclaration.TYPE_PROPERTY, parameterType, null);
			sourceRewriter.set(parameterDeclaration, SingleVariableDeclaration.NAME_PROPERTY, ast.newSimpleName(variableBinding1.getName()), null);
			interfaceMethodDeclarationParameterRewrite.insertLast(parameterDeclaration, null);
		}
		ListRewrite interfaceMethodThrownExceptionRewrite = sourceRewriter.getListRewrite(interfaceMethodDeclaration, MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY);
		for(ITypeBinding typeBinding : thrownExceptionTypeBindings) {
			Type exceptionType = RefactoringUtility.generateTypeFromTypeBinding(typeBinding, ast, sourceRewriter);
			Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
			typeBindings.add(typeBinding);
			RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
			interfaceMethodThrownExceptionRewrite.insertLast(exceptionType, null);
		}
		interfaceBodyDeclarationRewrite.insertLast(interfaceMethodDeclaration, null);
		bodyDeclarationsRewrite.insertLast(interfaceTypeDeclaration, null);
		return ast.newSimpleType(interfaceName);
	}
	
	private Set<VariableBindingPair> findParametersForLambdaExpression(ASTNodeDifference difference) {
		List<PDGExpressionGap> expressionGaps = mapper.getRefactorableExpressionGaps();
		for(PDGExpressionGap expressionGap : expressionGaps) {
			if(expressionGap.getASTNodeDifference().equals(difference)) {
				return expressionGap.getParameterBindings();
			}
		}
		return null;
	}

	public boolean statementBelongsToBlockGaps(AbstractStatement statement) {
		for(PDGNodeBlockGap blockGap : mapper.getRefactorableBlockGaps()) {
			for(PDGNode node : blockGap.getNodesG1()) {
				if(node.getStatement().equals(statement)) {
					return true;
				}
			}
			for(PDGNode node : blockGap.getNodesG2()) {
				if(node.getStatement().equals(statement)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean differenceBelongsToBlockGaps(ASTNodeDifference difference) {
		AbstractExpression expr1 = difference.getExpression1();
		AbstractExpression expr2 = difference.getExpression2();
		if(expr1 != null && expr2 != null) {
			Statement statement1 = findParentStatement(expr1.getExpression());
			Statement statement2 = findParentStatement(expr2.getExpression());
			for(PDGNodeBlockGap blockGap : mapper.getRefactorableBlockGaps()) {
				boolean statement1Found = false;
				boolean statement2Found = false;
				for(PDGNode node : blockGap.getNodesG1()) {
					if(node.getASTStatement().equals(statement1)) {
						statement1Found = true;
					}
				}
				for(PDGNode node : blockGap.getNodesG2()) {
					if(node.getASTStatement().equals(statement2)) {
						statement2Found = true;
					}
				}
				if(statement1Found && statement2Found) {
					return true;
				}
			}
		}
		return false;
	}

	private PDGNodeBlockGap findBlockGapContainingDifference(ASTNodeDifference difference) {
		AbstractExpression expr1 = difference.getExpression1();
		AbstractExpression expr2 = difference.getExpression2();
		if(expr1 != null && expr2 != null) {
			Statement statement1 = findParentStatement(expr1.getExpression());
			Statement statement2 = findParentStatement(expr2.getExpression());
			for(PDGNodeBlockGap blockGap : mapper.getRefactorableBlockGaps()) {
				boolean statement1Found = false;
				boolean statement2Found = false;
				for(PDGNode node : blockGap.getNodesG1()) {
					if(node.getASTStatement().equals(statement1)) {
						statement1Found = true;
					}
				}
				for(PDGNode node : blockGap.getNodesG2()) {
					if(node.getASTStatement().equals(statement2)) {
						statement2Found = true;
					}
				}
				if(statement1Found && statement2Found) {
					return blockGap;
				}
			}
		}
		return null;
	}

	public PDGNodeBlockGap findBlockGapCorrespondingToBindingSignaturePair(BindingSignaturePair pair) {
		for(PDGNodeBlockGap blockGap : mapper.getRefactorableBlockGaps()) {
			BindingSignaturePair newPair = new BindingSignaturePair(blockGap.getNodesG1(), blockGap.getNodesG2());
			if(newPair.equals(pair)) {
				return blockGap;
			}
		}
		return null;
	}

	public boolean differenceBelongsToExpressionGaps(ASTNodeDifference difference) {
		for(PDGExpressionGap expressionGap : mapper.getRefactorableExpressionGaps()) {
			if(expressionGap.getASTNodeDifference().equals(difference)) {
				return true;
			}
		}
		return false;
	}

	private PDGExpressionGap findExpressionGapContainingDifference(ASTNodeDifference difference) {
		for(PDGExpressionGap expressionGap : mapper.getRefactorableExpressionGaps()) {
			if(expressionGap.getASTNodeDifference().equals(difference)) {
				return expressionGap;
			}
		}
		return null;
	}

	public ASTNodeDifference findDifferenceCorrespondingToPreconditionViolation(ExpressionPreconditionViolation expressionViolation) {
		for(ASTNodeDifference difference : mapper.getNodeDifferences()) {
			if(expressionViolation.getExpression().equals(difference.getExpression1()) ||
					expressionViolation.getExpression().equals(difference.getExpression2())) {
				return difference;
			}
		}
		return null;
	}

	private Statement findParentStatement(Expression expression) {
		ASTNode parent = expression.getParent();
		while(parent != null) {
			if(parent instanceof Statement) {
				return (Statement)parent;
			}
			parent = parent.getParent();
		}
		return null;
	}

	public boolean parameterIsDeclaredInBlockGap(VariableDeclaration variableDeclaration1, VariableDeclaration variableDeclaration2) {
		PlainVariable variable1 = new PlainVariable(variableDeclaration1);
		PlainVariable variable2 = new PlainVariable(variableDeclaration2);
		boolean variable1DeclaredInBlockGap = false;
		boolean variable2DeclaredInBlockGap = false;
		for(PDGNodeBlockGap blockGap : mapper.getRefactorableBlockGaps()) {
			Set<PDGNode> nodesG1 = blockGap.getNodesG1();
			for(PDGNode nodeG1 : nodesG1) {
				if(nodeG1.declaresLocalVariable(variable1)) {
					variable1DeclaredInBlockGap = true;
					break;
				}
			}
			Set<PDGNode> nodesG2 = blockGap.getNodesG2();
			for(PDGNode nodeG2 : nodesG2) {
				if(nodeG2.declaresLocalVariable(variable2)) {
					variable2DeclaredInBlockGap = true;
					break;
				}
			}
		}
		return variable1DeclaredInBlockGap && variable2DeclaredInBlockGap;
	}

	public Statement createStatementReplacingBlockGap(ASTRewrite sourceRewriter, AST ast, PDGNodeBlockGap blockGap, Expression argument) {
		Set<VariableBindingPair> parameterTypeBindings = blockGap.getParameterBindings();
		Set<ITypeBinding> thrownExceptionTypeBindings = blockGap.getThrownExceptions();
		String methodName = null;
		if(parameterTypeBindings.size() == 1 && thrownExceptionTypeBindings.isEmpty()) {
			if(blockGap.getReturnType() != null) {
				//a return type exists, and thus a Function will be created with 1 parameter
				methodName = "apply";
			}
			else {
				//a return type does not exist, and thus a Consumer will be created with 1 parameter
				methodName = "accept";
			}
		}
		else if(parameterTypeBindings.size() == 0 && blockGap.getReturnType() != null && thrownExceptionTypeBindings.isEmpty()) {
			//a return type exists, and thus a Supplier will be created with 0 parameters
			methodName = "get";
		}
		else {
			methodName = FUNCTIONAL_INTERFACE_METHOD_NAME;
		}
		MethodInvocation interfaceMethodInvocation = ast.newMethodInvocation();
		sourceRewriter.set(interfaceMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(methodName), null);
		ListRewrite argumentRewrite = sourceRewriter.getListRewrite(interfaceMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
		for(VariableBindingPair variableBindingPair : parameterTypeBindings) {
			IVariableBinding variableBinding = variableBindingPair.getBinding1();
			argumentRewrite.insertLast(ast.newSimpleName(variableBinding.getName()), null);
		}
		sourceRewriter.set(interfaceMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, argument, null);
		if(blockGap.getReturnedVariableBinding() != null) {
			IVariableBinding variableBinding1 = blockGap.getReturnedVariableBinding().getBinding1();
			IVariableBinding variableBinding2 = blockGap.getReturnedVariableBinding().getBinding2();
			Set<VariableDeclaration> declaredVariablesInRemainingNodesDefinedByMappedNodesG1 = mapper.getDeclaredVariablesInRemainingNodesDefinedByMappedNodesG1();
			Set<VariableDeclaration> declaredVariablesInRemainingNodesDefinedByMappedNodesG2 = mapper.getDeclaredVariablesInRemainingNodesDefinedByMappedNodesG2();
			boolean variableBinding1IsDeclaredInRemainingNodes = false;
			for(VariableDeclaration declaration1 : declaredVariablesInRemainingNodesDefinedByMappedNodesG1) {
				if(declaration1.resolveBinding().isEqualTo(variableBinding1)) {
					variableBinding1IsDeclaredInRemainingNodes = true;
					break;
				}
			}
			boolean variableBinding2IsDeclaredInRemainingNodes = false;
			for(VariableDeclaration declaration2 : declaredVariablesInRemainingNodesDefinedByMappedNodesG2) {
				if(declaration2.resolveBinding().isEqualTo(variableBinding2)) {
					variableBinding2IsDeclaredInRemainingNodes = true;
					break;
				}
			}
			boolean variablesPassedAsCommonParameter = false;
			for(VariableBindingKeyPair pair : mapper.getCommonPassedParameters().keySet()) {
				if(variableBinding1.getKey().equals(pair.getKey1()) && variableBinding2.getKey().equals(pair.getKey2())) {
					variablesPassedAsCommonParameter = true;
					break;
				}
			}
			if((variableBinding1IsDeclaredInRemainingNodes && variableBinding2IsDeclaredInRemainingNodes && variablesPassedAsCommonParameter) ||
					parameterTypeBindings.contains(blockGap.getReturnedVariableBinding()) ||
					(mappedNodesContainStatementDeclaringVariable(variableBinding1, variableBinding2) && !blockGap.variableIsDeclaredInBlockGap(blockGap.getReturnedVariableBinding()))) {
				Assignment assignment = ast.newAssignment();
				sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, ast.newSimpleName(variableBinding1.getName()), null);
				sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, interfaceMethodInvocation, null);
				return ast.newExpressionStatement(assignment);
			}
			else {
				Type returnType = RefactoringUtility.generateTypeFromTypeBinding(blockGap.getReturnType(), ast, sourceRewriter);
				VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
				sourceRewriter.set(fragment, VariableDeclarationFragment.NAME_PROPERTY, ast.newSimpleName(variableBinding1.getName()), null);
				sourceRewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, interfaceMethodInvocation, null);
				VariableDeclarationStatement variableDeclarationStatement = ast.newVariableDeclarationStatement(fragment);
				sourceRewriter.set(variableDeclarationStatement, VariableDeclarationStatement.TYPE_PROPERTY, returnType, null);
				return variableDeclarationStatement;
			}
		}
		else if(blockGap.getReturnType() != null) {
			ReturnStatement returnStatement = ast.newReturnStatement();
			sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, interfaceMethodInvocation, null);
			return returnStatement;
		}
		else {
			return ast.newExpressionStatement(interfaceMethodInvocation);
		}
	}

	private boolean mappedNodesContainStatementDeclaringVariable(IVariableBinding variableBinding1, IVariableBinding variableBinding2) {
		for(PDGNodeMapping pdgNodeMapping : sortedNodeMappings) {
			PDGNode pdgNode1 = pdgNodeMapping.getNodeG1();
			PDGNode pdgNode2 = pdgNodeMapping.getNodeG2();
			boolean node1DeclaresVariable = false;
			for(Iterator<AbstractVariable> declaredVariableIterator = pdgNode1.getDeclaredVariableIterator(); declaredVariableIterator.hasNext();) {
				AbstractVariable declaredVariable = declaredVariableIterator.next();
				if(declaredVariable.getVariableBindingKey().equals(variableBinding1.getKey())) {
					node1DeclaresVariable = true;
					break;
				}
			}
			boolean node2DeclaresVariable = false;
			for(Iterator<AbstractVariable> declaredVariableIterator = pdgNode2.getDeclaredVariableIterator(); declaredVariableIterator.hasNext();) {
				AbstractVariable declaredVariable = declaredVariableIterator.next();
				if(declaredVariable.getVariableBindingKey().equals(variableBinding2.getKey())) {
					node2DeclaresVariable = true;
					break;
				}
			}
			if(node1DeclaresVariable && node2DeclaresVariable) {
				return true;
			}
		}
		return false;
	}

	public void createLambdaExpressionForBlockGap(PDGNodeBlockGap blockGap, ASTRewrite methodBodyRewriter, AST ast,
			ListRewrite argumentsRewrite, List<VariableDeclaration> returnedVariables, int index) {
		Set<PDGNode> statements = index == 0 ? blockGap.getNodesG1() : blockGap.getNodesG2();
		LambdaExpression lambdaExpression = ast.newLambdaExpression();
		ListRewrite lambdaParameterRewrite = methodBodyRewriter.getListRewrite(lambdaExpression, LambdaExpression.PARAMETERS_PROPERTY);
		Set<VariableBindingPair> parameterTypeBindings = blockGap.getParameterBindings();
		Set<ITypeBinding> thrownExceptionTypeBindings = blockGap.getThrownExceptions();
		processLambdaExpressionParameters(parameterTypeBindings, lambdaParameterRewrite, methodBodyRewriter, ast, thrownExceptionTypeBindings, returnedVariables, index);
		Block lambdaBody = ast.newBlock();
		ListRewrite lambdaBodyRewrite = methodBodyRewriter.getListRewrite(lambdaBody, Block.STATEMENTS_PROPERTY);
		int statementIndex = 0;
		boolean returnStatementAdded = false;
		for(PDGNode node : statements) {
			Statement statement = node.getASTStatement();
			boolean statementChanged = false;
			if(statementIndex == statements.size() - 1) {
				ASTNodeDifference difference = findDifferenceCorrespondingToStatement(statement, blockGap.getNodeDifferences());
				if(difference != null) {
					Expression expression = index == 0 ? difference.getExpression1().getExpression() : difference.getExpression2().getExpression();
					if(statement instanceof ExpressionStatement) {
						ExpressionStatement expressionStatement = (ExpressionStatement)statement;
						if(expressionStatement.getExpression() instanceof Assignment) {
							Assignment assignment = (Assignment)expressionStatement.getExpression();
							if(assignment.getRightHandSide().equals(expression) && assignment.getLeftHandSide() instanceof SimpleName) {
								SimpleName leftHandSide = (SimpleName)assignment.getLeftHandSide();
								IBinding binding = leftHandSide.resolveBinding();
								if(binding.getKind() == IBinding.VARIABLE) {
									IVariableBinding variableBinding = (IVariableBinding)binding;
									VariableBindingPair returnedVariableBindingPair = blockGap.getReturnedVariableBinding();
									IVariableBinding returnedVariableBinding = index == 0 ? returnedVariableBindingPair.getBinding1() : returnedVariableBindingPair.getBinding2();
									if(variableBinding.isEqualTo(returnedVariableBinding)) {
										//introduce a return statement in the body of the lambda expression
										ReturnStatement returnStatement = ast.newReturnStatement();
										methodBodyRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, expression, null);
										lambdaBodyRewrite.insertLast(returnStatement, null);
										statementChanged = true;
										returnStatementAdded = true;
									}
								}
							}
						}
					}
				}
				else {
					PDGNode lastNode = index == 0 ? blockGap.getLastNodeG1() : blockGap.getLastNodeG2();
					if(statement instanceof ExpressionStatement && statement.equals(lastNode.getASTStatement())) {
						ExpressionStatement expressionStatement = (ExpressionStatement)statement;
						if(expressionStatement.getExpression() instanceof Assignment) {
							Assignment assignment = (Assignment)expressionStatement.getExpression();
							if(assignment.getLeftHandSide() instanceof SimpleName) {
								SimpleName leftHandSide = (SimpleName)assignment.getLeftHandSide();
								IBinding binding = leftHandSide.resolveBinding();
								if(binding.getKind() == IBinding.VARIABLE) {
									IVariableBinding variableBinding = (IVariableBinding)binding;
									VariableBindingPair returnedVariableBindingPair = blockGap.getReturnedVariableBinding();
									IVariableBinding returnedVariableBinding = null;
									if(returnedVariableBindingPair != null) {
										returnedVariableBinding = index == 0 ? returnedVariableBindingPair.getBinding1() : returnedVariableBindingPair.getBinding2();
									}
									if(variableBinding.isEqualTo(returnedVariableBinding)) {
										//introduce a return statement in the body of the lambda expression
										ReturnStatement returnStatement = ast.newReturnStatement();
										Expression newRightHandSide = (Expression)ASTNode.copySubtree(ast, assignment.getRightHandSide());
										//replace the non-effectively final variables in the lambda expression body
										for(VariableBindingPair variableBindingPair : nonEffectivelyFinalLocalVariables) {
											IVariableBinding variableBinding1 = index == 0 ? variableBindingPair.getBinding1() : variableBindingPair.getBinding2();
											ExpressionExtractor expressionExtractor = new ExpressionExtractor();
											List<Expression> oldSimpleNames = expressionExtractor.getVariableInstructions(assignment.getRightHandSide());
											List<Expression> newSimpleNames = expressionExtractor.getVariableInstructions(newRightHandSide);
											int j = 0;
											for(Expression oldExpression : oldSimpleNames) {
												SimpleName oldSimpleName = (SimpleName)oldExpression;
												SimpleName newSimpleName = (SimpleName)newSimpleNames.get(j);
												if(oldSimpleName.resolveBinding().isEqualTo(variableBinding1)) {
													String identifier = variableBinding1.getName() + "Final";
													if(sourceMethodDeclarations.get(0).equals(sourceMethodDeclarations.get(1))) {
														identifier = identifier + index;
													}
													methodBodyRewriter.replace(newSimpleName, ast.newSimpleName(identifier), null);
												}
												j++;
											}
										}
										methodBodyRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, newRightHandSide, null);
										lambdaBodyRewrite.insertLast(returnStatement, null);
										statementChanged = true;
										returnStatementAdded = true;
									}
								}
							}
						}
					}
				}
			}
			if(!statementChanged) {
				PDGNode controlParent = node.getControlDependenceParent();
				//special handling for statements nested under a try block, and are not control dependent on the try block
				CloneStructureNode cloneStructureNode = index == 0 ? mapper.getCloneStructureRoot().findNodeG1(node) : mapper.getCloneStructureRoot().findNodeG2(node);
				NodeMapping cloneStructureNodeParentMapping = cloneStructureNode.getParent().getMapping();
				if(cloneStructureNodeParentMapping != null) {
					PDGNode cloneStructureNodeParent = index == 0 ? cloneStructureNodeParentMapping.getNodeG1() : cloneStructureNodeParentMapping.getNodeG2();
					if(cloneStructureNodeParent != null) {
						controlParent = cloneStructureNodeParent;
					}
				}
				if(!statements.contains(controlParent)) {
					Statement newStatement = (Statement)ASTNode.copySubtree(ast, statement);
					//replace the non-effectively final variables in the lambda expression body
					for(VariableBindingPair variableBindingPair : nonEffectivelyFinalLocalVariables) {
						IVariableBinding variableBinding = index == 0 ? variableBindingPair.getBinding1() : variableBindingPair.getBinding2();
						ExpressionExtractor expressionExtractor = new ExpressionExtractor();
						List<Expression> oldSimpleNames = expressionExtractor.getVariableInstructions(statement);
						List<Expression> newSimpleNames = expressionExtractor.getVariableInstructions(newStatement);
						int j = 0;
						for(Expression oldExpression : oldSimpleNames) {
							SimpleName oldSimpleName = (SimpleName)oldExpression;
							SimpleName newSimpleName = (SimpleName)newSimpleNames.get(j);
							if(oldSimpleName.resolveBinding().isEqualTo(variableBinding)) {
								String identifier = variableBinding.getName() + "Final";
								if(sourceMethodDeclarations.get(0).equals(sourceMethodDeclarations.get(1))) {
									identifier = identifier + index;
								}
								methodBodyRewriter.replace(newSimpleName, ast.newSimpleName(identifier), null);
							}
							j++;
						}
					}
					//replace accesses to the returned variable in the lambda expression body
					ExpressionExtractor expressionExtractor = new ExpressionExtractor();
					List<Expression> oldSimpleNames = expressionExtractor.getVariableInstructions(statement);
					List<Expression> newSimpleNames = expressionExtractor.getVariableInstructions(newStatement);
					int j = 0;
					for(Expression oldExpression : oldSimpleNames) {
						SimpleName oldSimpleName = (SimpleName)oldExpression;
						SimpleName newSimpleName = (SimpleName)newSimpleNames.get(j);
						if(oldSimpleName.resolveBinding().getKind() == IBinding.VARIABLE) {
							IVariableBinding variableBinding = (IVariableBinding)oldSimpleName.resolveBinding();
							if(isReturnedVariableAndNotPassedAsCommonParameter(variableBinding, returnedVariables)) {
								String identifier = variableBinding.getName() + index;
								methodBodyRewriter.replace(newSimpleName, ast.newSimpleName(identifier), null);
							}
							else if(isLambdaExpressionParameter(variableBinding, parameterTypeBindings, index) &&
									parameterTypeBindings.size() == 1 && variableBinding.getType().isPrimitive() && thrownExceptionTypeBindings.isEmpty() && isMethodCallArgument(oldSimpleName)) {
								//Lambda expression parameter should be casted back to the original primitive type
								CastExpression castExpression = ast.newCastExpression();
								Type primitiveType = RefactoringUtility.generateTypeFromTypeBinding(variableBinding.getType(), ast, methodBodyRewriter);
								methodBodyRewriter.set(castExpression, CastExpression.TYPE_PROPERTY, primitiveType, null);
								methodBodyRewriter.set(castExpression, CastExpression.EXPRESSION_PROPERTY, ast.newSimpleName(variableBinding.getName()), null);
								methodBodyRewriter.replace(newSimpleName, castExpression, null);
							}
						}
						j++;
					}
					lambdaBodyRewrite.insertLast(newStatement, null);
				}
			}
			methodBodyRewriter.remove(statement, null);
			statementIndex++;
		}
		if(!returnStatementAdded && blockGap.getReturnedVariableBinding() != null) {
			ReturnStatement returnStatement = ast.newReturnStatement();
			IVariableBinding variableBinding1 = blockGap.getReturnedVariableBinding().getBinding1();
			IVariableBinding variableBinding2 = blockGap.getReturnedVariableBinding().getBinding2();
			String returnedVariableName = index == 0 ? variableBinding1.getName() : variableBinding2.getName();
			methodBodyRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, ast.newSimpleName(returnedVariableName), null);
			lambdaBodyRewrite.insertLast(returnStatement, null);
		}
		methodBodyRewriter.set(lambdaExpression, LambdaExpression.BODY_PROPERTY, lambdaBody, null);
		argumentsRewrite.insertLast(lambdaExpression, null);
	}

	private boolean isLambdaExpressionParameter(IVariableBinding variableBinding, Set<VariableBindingPair> parameterTypeBindings, int index) {
		for(VariableBindingPair variableBindingPair : parameterTypeBindings) {
			IVariableBinding parameterVariableBinding = index == 0 ? variableBindingPair.getBinding1() : variableBindingPair.getBinding2();
			if(variableBinding.isEqualTo(parameterVariableBinding)) {
				return true;
			}
		}
		return false;
	}

	private boolean isMethodCallArgument(SimpleName simpleName) {
		ASTNode parent = simpleName.getParent();
		List<Expression> arguments = null;
		if(parent instanceof MethodInvocation) {
			MethodInvocation methodInvocation = (MethodInvocation)parent;
			arguments = methodInvocation.arguments();
		}
		else if(parent instanceof ClassInstanceCreation) {
			ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)parent;
			arguments = classInstanceCreation.arguments();
		}
		if(arguments != null && arguments.contains(simpleName)) {
			return true;
		}
		return false;
	}

	private ASTNodeDifference findDifferenceCorrespondingToStatement(Statement statement, List<ASTNodeDifference> differences) {
		for(ASTNodeDifference difference : differences) {
			Expression expression1 = difference.getExpression1().getExpression();
			if(statement.equals(findParentStatement(expression1))) {
				return difference;
			}
			Expression expression2 = difference.getExpression2().getExpression();
			if(statement.equals(findParentStatement(expression2))) {
				return difference;
			}
		}
		return null;
	}

	private boolean isReturnedVariableAndNotPassedAsCommonParameter(IVariableBinding variableBinding, List<VariableDeclaration> returnedVariables) {
		for(VariableDeclaration variableDeclaration : returnedVariables) {
			if(variableDeclaration.resolveBinding().isEqualTo(variableBinding) && !variableIsPassedAsCommonParameter(variableDeclaration))
				return true;
		}
		return false;
	}

	private boolean variableIsPassedAsCommonParameter(VariableDeclaration variableDeclaration) {
		for(VariableBindingKeyPair pair : mapper.getCommonPassedParameters().keySet()) {
			if(pair.getKey1().equals(variableDeclaration.resolveBinding().getKey()) ||
					pair.getKey2().equals(variableDeclaration.resolveBinding().getKey())) {
				return true;
			}
		}
		return false;
	}

	public boolean requiresFunctionImport() {
		for(PDGExpressionGap expressionGap : mapper.getRefactorableExpressionGaps()) {
			Set<VariableBindingPair> parameters = expressionGap.getParameterBindings();
			ITypeBinding returnTypeBinding = expressionGap.getReturnType();
			if(parameters.size() == 1 && !returnTypeBinding.getName().equals("void") && expressionGap.getThrownExceptions().isEmpty()) {
				return true;
			}
		}
		for(PDGNodeBlockGap blockGap : mapper.getRefactorableBlockGaps()) {
			Set<VariableBindingPair> parameters = blockGap.getParameterBindings();
			ITypeBinding returnType = blockGap.getReturnType();
			if(parameters.size() == 1 && returnType != null && blockGap.getThrownExceptions().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public boolean requiresSupplierImport() {
		for(PDGExpressionGap expressionGap : mapper.getRefactorableExpressionGaps()) {
			Set<VariableBindingPair> parameters = expressionGap.getParameterBindings();
			ITypeBinding returnTypeBinding = expressionGap.getReturnType();
			if(parameters.size() == 0 && !returnTypeBinding.getName().equals("void") && expressionGap.getThrownExceptions().isEmpty()) {
				return true;
			}
		}
		for(PDGNodeBlockGap blockGap : mapper.getRefactorableBlockGaps()) {
			Set<VariableBindingPair> parameters = blockGap.getParameterBindings();
			ITypeBinding returnType = blockGap.getReturnType();
			if(parameters.size() == 0 && returnType != null && blockGap.getThrownExceptions().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public boolean requiresConsumerImport() {
		for(PDGExpressionGap expressionGap : mapper.getRefactorableExpressionGaps()) {
			Set<VariableBindingPair> parameters = expressionGap.getParameterBindings();
			ITypeBinding returnTypeBinding = expressionGap.getReturnType();
			if(parameters.size() == 1 && returnTypeBinding.getName().equals("void") && expressionGap.getThrownExceptions().isEmpty()) {
				return true;
			}
		}
		for(PDGNodeBlockGap blockGap : mapper.getRefactorableBlockGaps()) {
			Set<VariableBindingPair> parameters = blockGap.getParameterBindings();
			ITypeBinding returnType = blockGap.getReturnType();
			if(parameters.size() == 1 && returnType == null && blockGap.getThrownExceptions().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public PDGNodeBlockGap nodeGapIsLastInsideBlockGap(CloneStructureNode child) {
		if(child.getMapping() instanceof PDGNodeGap && !child.getMapping().isAdvancedMatch()) {
			PDGNode nodeG1 = child.getMapping().getNodeG1();
			PDGNode nodeG2 = child.getMapping().getNodeG2();
			for(PDGNodeBlockGap blockGap : mapper.getRefactorableBlockGaps()) {
				TreeSet<PDGNode> nodesG1 = blockGap.getNodesG1();
				if(nodesG1.size() > 0 && nodeG1 != null) {
					PDGNode lastNode = blockGap.getLastNodeG1();
					if(lastNode.getStatement().equals(nodeG1.getStatement())) {
						return blockGap;
					}
				}
				TreeSet<PDGNode> nodesG2 = blockGap.getNodesG2();
				if(nodesG1.size() == 0 && nodesG2.size() > 0 && nodeG2 != null) {
					PDGNode lastNode = blockGap.getLastNodeG2();
					if(lastNode.getStatement().equals(nodeG2.getStatement())) {
						return blockGap;
					}
				}
			}
		}
		return null;
	}

	public PDGNodeBlockGap nodeMappingIsLastInsideBlockGap(CloneStructureNode child) {
		if(child.getMapping() instanceof PDGNodeMapping) {
			PDGNode nodeG1 = child.getMapping().getNodeG1();
			for(PDGNodeBlockGap blockGap : mapper.getRefactorableBlockGaps()) {
				TreeSet<PDGNode> nodesG1 = blockGap.getNodesG1();
				if(nodesG1.size() > 0) {
					PDGNode lastNode = blockGap.getLastNodeG1();
					if(lastNode.getStatement().equals(nodeG1.getStatement())) {
						return blockGap;
					}
				}
			}
		}
		return null;
	}

	public Expression createArgument(ASTRewrite sourceRewriter, AST ast, ASTNodeDifference argumentDifference, Expression argument) {
		if(differenceBelongsToExpressionGaps(argumentDifference) && !differenceBelongsToBlockGaps(argumentDifference)) {
			Set<VariableBindingPair> parameterTypeBindings = findParametersForLambdaExpression(argumentDifference);
			PDGExpressionGap expressionGap = findExpressionGapContainingDifference(argumentDifference);
			Set<ITypeBinding> thrownExceptionTypeBindings = expressionGap.getThrownExceptions();
			String methodName = null;
			ITypeBinding returnTypeBinding = expressionGap.getReturnType();
			if(parameterTypeBindings.size() == 1 && thrownExceptionTypeBindings.isEmpty()) {
				if(!returnTypeBinding.getName().equals("void")) {
					//the expression has a type that is not void, and thus a Function will be created with 1 parameter
					methodName = "apply";
				}
				else {
					//the expression has a void type, and thus a Consumer will be created with 1 parameter
					methodName = "accept";
				}
			}
			else if(parameterTypeBindings.size() == 0 && !returnTypeBinding.getName().equals("void") && thrownExceptionTypeBindings.isEmpty()) {
				//the expression has a type that is not void, and thus a Supplier will be created with 0 parameters
				methodName = "get";
			}
			else {
				methodName = FUNCTIONAL_INTERFACE_METHOD_NAME;
			}
			MethodInvocation interfaceMethodInvocation = ast.newMethodInvocation();
			sourceRewriter.set(interfaceMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(methodName), null);
			ListRewrite argumentRewrite = sourceRewriter.getListRewrite(interfaceMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
			
			for(VariableBindingPair variableBindingPair : parameterTypeBindings) {
				IVariableBinding variableBinding = variableBindingPair.getBinding1();
				argumentRewrite.insertLast(ast.newSimpleName(variableBinding.getName()), null);
			}
			sourceRewriter.set(interfaceMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, argument, null);
			boolean castToPrimitive = returnTypeBinding.isPrimitive() && !returnTypeBinding.getName().equals("void") && parameterTypeBindings.size() <= 1;
			if(castToPrimitive) {
				CastExpression castExpression = ast.newCastExpression();
				sourceRewriter.set(castExpression, CastExpression.EXPRESSION_PROPERTY, interfaceMethodInvocation, null);
				Type primitiveType = RefactoringUtility.generateTypeFromTypeBinding(returnTypeBinding, ast, sourceRewriter);
				sourceRewriter.set(castExpression, CastExpression.TYPE_PROPERTY, primitiveType, null);
				return castExpression;
			}
			return interfaceMethodInvocation;
		}
		else if(differenceBelongsToBlockGaps(argumentDifference)) {
			PDGNodeBlockGap blockGap = findBlockGapContainingDifference(argumentDifference);
			Set<VariableBindingPair> parameterTypeBindings = blockGap.getParameterBindings();
			Set<ITypeBinding> thrownExceptionTypeBindings = blockGap.getThrownExceptions();
			String methodName = null;
			if(parameterTypeBindings.size() == 1 && thrownExceptionTypeBindings.isEmpty()) {
				if(blockGap.getReturnType() != null) {
					//a return type exists, and thus a Function will be created with 1 parameter
					methodName = "apply";
				}
				else {
					//a return type does not exist, and thus a Consumer will be created with 1 parameter
					methodName = "accept";
				}
			}
			else if(parameterTypeBindings.size() == 0 && blockGap.getReturnType() != null && thrownExceptionTypeBindings.isEmpty()) {
				//a return type exists, and thus a Supplier will be created with 0 parameters
				methodName = "get";
			}
			else {
				methodName = FUNCTIONAL_INTERFACE_METHOD_NAME;
			}
			MethodInvocation interfaceMethodInvocation = ast.newMethodInvocation();
			sourceRewriter.set(interfaceMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(methodName), null);
			ListRewrite argumentRewrite = sourceRewriter.getListRewrite(interfaceMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
			for(VariableBindingPair variableBindingPair : parameterTypeBindings) {
				IVariableBinding variableBinding = variableBindingPair.getBinding1();
				argumentRewrite.insertLast(ast.newSimpleName(variableBinding.getName()), null);
			}
			sourceRewriter.set(interfaceMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, argument, null);
			boolean castToPrimitive = blockGap.getReturnType() != null && blockGap.getReturnType().isPrimitive() && parameterTypeBindings.size() <= 1;
			if(castToPrimitive) {
				CastExpression castExpression = ast.newCastExpression();
				sourceRewriter.set(castExpression, CastExpression.EXPRESSION_PROPERTY, interfaceMethodInvocation, null);
				Type primitiveType = RefactoringUtility.generateTypeFromTypeBinding(blockGap.getReturnType(), ast, sourceRewriter);
				sourceRewriter.set(castExpression, CastExpression.TYPE_PROPERTY, primitiveType, null);
				return castExpression;
			}
			return interfaceMethodInvocation;
		}
		else {
			return argument;
		}
	}

	public void createFinalVariablesForTheNonEffectivelyFinalVariables(ASTRewrite methodBodyRewriter, AST ast,
			ListRewrite blockRewrite, Statement extractedMethodInvocationStatement, int index) {
		//create final variables for the non-effectively final variables used in the lambda expressions
		for(VariableBindingPair pair : nonEffectivelyFinalLocalVariables) {
			IVariableBinding variableBinding = index == 0 ? pair.getBinding1() : pair.getBinding2();
			VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
			String identifier = variableBinding.getName() + "Final";
			if(sourceMethodDeclarations.get(0).equals(sourceMethodDeclarations.get(1))) {
				identifier = identifier + index;
			}
			methodBodyRewriter.set(fragment, VariableDeclarationFragment.NAME_PROPERTY, ast.newSimpleName(identifier), null);
			methodBodyRewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, ast.newSimpleName(variableBinding.getName()), null);
			VariableDeclarationStatement variableDeclarationStatement = ast.newVariableDeclarationStatement(fragment);
			Type finalVariableType = RefactoringUtility.generateTypeFromTypeBinding(variableBinding.getType(), ast, methodBodyRewriter);
			methodBodyRewriter.set(variableDeclarationStatement, VariableDeclarationStatement.TYPE_PROPERTY, finalVariableType, null);
			ListRewrite modifierRewrite = methodBodyRewriter.getListRewrite(variableDeclarationStatement, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
			modifierRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
			blockRewrite.insertBefore(variableDeclarationStatement, extractedMethodInvocationStatement, null);
		}
	}

	public void insertArgumentInCallSite(ASTRewrite methodBodyRewriter, AST ast, ListRewrite argumentsRewrite,
			List<VariableDeclaration> returnedVariables, ASTNodeDifference difference, Expression expression, int index) {
		if(differenceBelongsToExpressionGaps(difference) && !differenceBelongsToBlockGaps(difference)) {
			LambdaExpression lambdaExpression = ast.newLambdaExpression();
			ListRewrite lambdaParameterRewrite = methodBodyRewriter.getListRewrite(lambdaExpression, LambdaExpression.PARAMETERS_PROPERTY);
			Set<VariableBindingPair> parameterTypeBindings = findParametersForLambdaExpression(difference);
			PDGExpressionGap expressionGap = findExpressionGapContainingDifference(difference);
			Set<ITypeBinding> thrownExceptionTypeBindings = expressionGap.getThrownExceptions();
			processLambdaExpressionParameters(parameterTypeBindings, lambdaParameterRewrite, methodBodyRewriter, ast, thrownExceptionTypeBindings, returnedVariables, index);
			//replace the non-effectively final variables in the lambda expression body
			SimpleName nonEffectivelyFinalVariableSimpleName = null;
			for(VariableBindingPair variableBindingPair : nonEffectivelyFinalLocalVariables) {
				IVariableBinding variableBinding = index == 0 ? variableBindingPair.getBinding1() : variableBindingPair.getBinding2();
				if(expression instanceof SimpleName) {
					SimpleName simpleName = (SimpleName)expression;
					if(simpleName.resolveBinding().isEqualTo(variableBinding)) {
						String identifier = variableBinding.getName() + "Final";
						if(sourceMethodDeclarations.get(0).equals(sourceMethodDeclarations.get(1))) {
							identifier = identifier + index;
						}
						nonEffectivelyFinalVariableSimpleName = ast.newSimpleName(identifier);
						break;
					}
				}
				else {
					ExpressionExtractor expressionExtractor = new ExpressionExtractor();
					List<Expression> simpleNames = expressionExtractor.getVariableInstructions(expression);
					for(Expression expr : simpleNames) {
						SimpleName simpleName = (SimpleName)expr;
						if(simpleName.resolveBinding().isEqualTo(variableBinding)) {
							String identifier = variableBinding.getName() + "Final";
							if(sourceMethodDeclarations.get(0).equals(sourceMethodDeclarations.get(1))) {
								identifier = identifier + index;
							}
							methodBodyRewriter.replace(simpleName, ast.newSimpleName(identifier), null);
						}
					}
				}
			}
			//replace accesses to the returned variable in the lambda expression body
			ExpressionExtractor expressionExtractor = new ExpressionExtractor();
			List<Expression> simpleNames = expressionExtractor.getVariableInstructions(expression);
			for(Expression expr : simpleNames) {
				SimpleName simpleName = (SimpleName)expr;
				if(simpleName.resolveBinding().getKind() == IBinding.VARIABLE) {
					IVariableBinding variableBinding = (IVariableBinding)simpleName.resolveBinding();
					if(isReturnedVariableAndNotPassedAsCommonParameter(variableBinding, returnedVariables)) {
						String identifier = variableBinding.getName() + index;
						methodBodyRewriter.replace(simpleName, ast.newSimpleName(identifier), null);
					}
					else if(isLambdaExpressionParameter(variableBinding, parameterTypeBindings, index) &&
							parameterTypeBindings.size() == 1 && variableBinding.getType().isPrimitive() && thrownExceptionTypeBindings.isEmpty() && isMethodCallArgument(simpleName)) {
						//Lambda expression parameter should be casted back to the original primitive type
						CastExpression castExpression = ast.newCastExpression();
						Type primitiveType = RefactoringUtility.generateTypeFromTypeBinding(variableBinding.getType(), ast, methodBodyRewriter);
						methodBodyRewriter.set(castExpression, CastExpression.TYPE_PROPERTY, primitiveType, null);
						methodBodyRewriter.set(castExpression, CastExpression.EXPRESSION_PROPERTY, ast.newSimpleName(variableBinding.getName()), null);
						methodBodyRewriter.replace(simpleName, castExpression, null);
					}
				}
			}
			if(difference.containsDifferenceType(DifferenceType.IF_ELSE_SYMMETRICAL_MATCH) && index == 1) {
				ParenthesizedExpression parenthesizedExpression = ast.newParenthesizedExpression();
				methodBodyRewriter.set(parenthesizedExpression, ParenthesizedExpression.EXPRESSION_PROPERTY,
						nonEffectivelyFinalVariableSimpleName != null ? nonEffectivelyFinalVariableSimpleName : expression, null);
				PrefixExpression prefixExpression = ast.newPrefixExpression();
				methodBodyRewriter.set(prefixExpression, PrefixExpression.OPERAND_PROPERTY, parenthesizedExpression, null);
				methodBodyRewriter.set(prefixExpression, PrefixExpression.OPERATOR_PROPERTY, PrefixExpression.Operator.NOT, null);
				methodBodyRewriter.set(lambdaExpression, LambdaExpression.BODY_PROPERTY, prefixExpression, null);
			}
			else {
				methodBodyRewriter.set(lambdaExpression, LambdaExpression.BODY_PROPERTY,
						nonEffectivelyFinalVariableSimpleName != null ? nonEffectivelyFinalVariableSimpleName : expression, null);
			}
			argumentsRewrite.insertLast(lambdaExpression, null);
		}
		else if(differenceBelongsToBlockGaps(difference)) {
			PDGNodeBlockGap blockGap = findBlockGapContainingDifference(difference);
			createLambdaExpressionForBlockGap(blockGap, methodBodyRewriter, ast, argumentsRewrite, returnedVariables, index);
		}
		else {
			argumentsRewrite.insertLast(expression, null);
		}
	}

	private void processLambdaExpressionParameters(Set<VariableBindingPair> parameterTypeBindings,
			ListRewrite lambdaParameterRewrite, ASTRewrite methodBodyRewriter, AST ast,
			Set<ITypeBinding> thrownExceptionTypeBindings, List<VariableDeclaration> returnedVariables, int index) {
		for(VariableBindingPair variableBindingPair : parameterTypeBindings) {
			IVariableBinding variableBinding = index == 0 ? variableBindingPair.getBinding1() : variableBindingPair.getBinding2();
			IVariableBinding variableBinding1 = variableBindingPair.getBinding1();
			IVariableBinding variableBinding2 = variableBindingPair.getBinding2();
			ITypeBinding typeBinding1 = variableBinding1.getType();
			ITypeBinding typeBinding2 = variableBinding2.getType();
			ITypeBinding variableBindingType = determineType(typeBinding1, typeBinding2);
			Type parameterType = null;
			if(parameterTypeBindings.size() == 1 && variableBindingType.isPrimitive() && thrownExceptionTypeBindings.isEmpty()) {
				parameterType = RefactoringUtility.generateWrapperTypeForPrimitiveTypeBinding(variableBindingType, ast);
			}
			else {
				parameterType = variableBindingPair.hasQualifiedType() ? RefactoringUtility.generateQualifiedTypeFromTypeBinding(variableBindingType, ast, methodBodyRewriter) :
					RefactoringUtility.generateTypeFromTypeBinding(variableBindingType, ast, methodBodyRewriter);
			}
			SingleVariableDeclaration lambdaParameterDeclaration = ast.newSingleVariableDeclaration();
			String parameterName = null;
			if(isReturnedVariableAndNotPassedAsCommonParameter(variableBinding, returnedVariables)) {
				parameterName = variableBinding.getName() + index;
			}
			else {
				parameterName = variableBinding.getName();
			}
			methodBodyRewriter.set(lambdaParameterDeclaration, SingleVariableDeclaration.NAME_PROPERTY, ast.newSimpleName(parameterName), null);
			methodBodyRewriter.set(lambdaParameterDeclaration, SingleVariableDeclaration.TYPE_PROPERTY, parameterType, null);
			lambdaParameterRewrite.insertLast(lambdaParameterDeclaration, null);
		}
	}

	private Type createInterfaceType(ASTRewrite sourceRewriter, AST ast, ListRewrite bodyDeclarationsRewrite, ASTNodeDifference difference, ITypeBinding typeBinding, Type type,
			Set<ITypeBinding> requiredImportTypeBindings, int i) {
		Set<VariableBindingPair> parameterTypeBindings = findParametersForLambdaExpression(difference);
		PDGExpressionGap expressionGap = findExpressionGapContainingDifference(difference);
		Set<ITypeBinding> thrownExceptionTypeBindingsByDifference = expressionGap.getThrownExceptions();
		Type interfaceType = null;
		if(parameterTypeBindings.size() == 1 && !typeBinding.getName().equals("void") && thrownExceptionTypeBindingsByDifference.isEmpty()) {
			interfaceType = createFunction(sourceRewriter, ast, parameterTypeBindings, type, typeBinding, requiredImportTypeBindings);
		}
		else if(parameterTypeBindings.size() == 1 && typeBinding.getName().equals("void") && thrownExceptionTypeBindingsByDifference.isEmpty()) {
			interfaceType = createConsumer(sourceRewriter, ast, parameterTypeBindings, requiredImportTypeBindings);
		}
		else if(parameterTypeBindings.size() == 0 && !typeBinding.getName().equals("void") && thrownExceptionTypeBindingsByDifference.isEmpty()) {
			interfaceType = createSupplier(sourceRewriter, ast, type, typeBinding, requiredImportTypeBindings);
		}
		else {
			interfaceType = createFunctionalInterface(sourceRewriter, ast, parameterTypeBindings, type, thrownExceptionTypeBindingsByDifference, bodyDeclarationsRewrite, requiredImportTypeBindings, i);
		}
		return interfaceType;
	}

	public Type createParameterType(ASTRewrite sourceRewriter, AST ast, ListRewrite bodyDeclarationsRewrite, ASTNodeDifference difference, ITypeBinding typeBinding, Type type,
			Set<ITypeBinding> requiredImportTypeBindings, int i) {
		if(differenceBelongsToExpressionGaps(difference) && !differenceBelongsToBlockGaps(difference)) {
			//find required parameters
			return createInterfaceType(sourceRewriter, ast, bodyDeclarationsRewrite, difference, typeBinding, type, requiredImportTypeBindings, i);
		}
		else if(differenceBelongsToBlockGaps(difference)) {
			PDGNodeBlockGap blockGap = findBlockGapContainingDifference(difference);
			return createParameterForFunctionalInterface(blockGap, sourceRewriter, ast, bodyDeclarationsRewrite, requiredImportTypeBindings, i);
		}
		else {
			return type;
		}
	}

}
