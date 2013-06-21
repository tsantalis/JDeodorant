package gr.uom.java.ast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.MethodDeclarationUtility;
import gr.uom.java.ast.util.StatementExtractor;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckElimination;

public class TypeCheckCodeFragmentAnalyzer {
	private TypeCheckElimination typeCheckElimination;
	private TypeDeclaration typeDeclaration;
	private MethodDeclaration typeCheckMethod;
	private FieldDeclaration[] fields;
	private MethodDeclaration[] methods;
	private Map<SimpleName, Integer> typeVariableCounterMap;
	private Map<MethodInvocation, Integer> typeMethodInvocationCounterMap;
	private Map<Expression, IfStatementExpressionAnalyzer> complexExpressionMap;
	
	public TypeCheckCodeFragmentAnalyzer(TypeCheckElimination typeCheckElimination,
			TypeDeclaration typeDeclaration, MethodDeclaration typeCheckMethod, IFile iFile) {
		this.typeCheckElimination = typeCheckElimination;
		this.typeDeclaration = typeDeclaration;
		this.typeCheckMethod = typeCheckMethod;
		this.fields = typeDeclaration.getFields();
		this.methods = typeDeclaration.getMethods();
		this.typeVariableCounterMap = new LinkedHashMap<SimpleName, Integer>();
		this.typeMethodInvocationCounterMap = new LinkedHashMap<MethodInvocation, Integer>();
		this.complexExpressionMap = new LinkedHashMap<Expression, IfStatementExpressionAnalyzer>();
		typeCheckElimination.setTypeCheckClass(typeDeclaration);
		typeCheckElimination.setTypeCheckMethod(typeCheckMethod);
		typeCheckElimination.setTypeCheckIFile(iFile);
		processTypeCheckCodeFragment();
	}
	
	private void processTypeCheckCodeFragment() {
		if(typeCheckElimination.getTypeCheckCodeFragment() instanceof SwitchStatement) {
			SwitchStatement switchStatement = (SwitchStatement)typeCheckElimination.getTypeCheckCodeFragment();
			Expression switchStatementExpression = switchStatement.getExpression();
			Expression switchStatementExpressionName = extractOperand(switchStatementExpression);
			if(switchStatementExpressionName != null) {
				if(switchStatementExpressionName instanceof SimpleName) {
					SimpleName switchStatementExpressionSimpleName = (SimpleName)switchStatementExpressionName;
					IBinding switchStatementExpressionNameBinding = switchStatementExpressionSimpleName.resolveBinding();
					if(switchStatementExpressionNameBinding != null && switchStatementExpressionNameBinding.getKind() == IBinding.VARIABLE) {
						IVariableBinding switchStatementExpressionNameVariableBinding = (IVariableBinding)switchStatementExpressionNameBinding;
						if(switchStatementExpressionNameVariableBinding.isField()) {
							for(FieldDeclaration field : fields) {
								List<VariableDeclarationFragment> fragments = field.fragments();
								for(VariableDeclarationFragment fragment : fragments) {
									IVariableBinding fragmentVariableBinding = fragment.resolveBinding();
									if(fragmentVariableBinding.isEqualTo(switchStatementExpressionNameVariableBinding)) {
										typeCheckElimination.setTypeField(fragment);
										for(MethodDeclaration method : methods) {
											SimpleName fieldInstruction = MethodDeclarationUtility.isSetter(method);
											if(fieldInstruction != null && fragment.getName().getIdentifier().equals(fieldInstruction.getIdentifier())) {
												typeCheckElimination.setTypeFieldSetterMethod(method);
											}
											fieldInstruction = MethodDeclarationUtility.isGetter(method);
											if(fieldInstruction != null && fragment.getName().getIdentifier().equals(fieldInstruction.getIdentifier())) {
												typeCheckElimination.setTypeFieldGetterMethod(method);
											}
										}
										break;
									}
								}
							}
						}
						else if(switchStatementExpressionNameVariableBinding.isParameter()) {
							List<SingleVariableDeclaration> parameters = typeCheckMethod.parameters();
							for(SingleVariableDeclaration parameter : parameters) {
								IVariableBinding parameterVariableBinding = parameter.resolveBinding();
								if(parameterVariableBinding.isEqualTo(switchStatementExpressionNameVariableBinding)) {
									typeCheckElimination.setTypeLocalVariable(parameter);
									break;
								}
							}
						}
						else {
							StatementExtractor statementExtractor = new StatementExtractor();
							ExpressionExtractor expressionExtractor = new ExpressionExtractor();
							List<VariableDeclarationFragment> variableDeclarationFragments = new ArrayList<VariableDeclarationFragment>();
							List<Statement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(typeCheckMethod.getBody());
							for(Statement statement : variableDeclarationStatements) {
								VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
								List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
								variableDeclarationFragments.addAll(fragments);
							}
							List<Expression> variableDeclarationExpressions = expressionExtractor.getVariableDeclarationExpressions(typeCheckMethod.getBody());
							for(Expression expression : variableDeclarationExpressions) {
								VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)expression;
								List<VariableDeclarationFragment> fragments = variableDeclarationExpression.fragments();
								variableDeclarationFragments.addAll(fragments);
							}
							for(VariableDeclarationFragment fragment : variableDeclarationFragments) {
								IVariableBinding fragmentVariableBinding = fragment.resolveBinding();
								if(fragmentVariableBinding.isEqualTo(switchStatementExpressionNameVariableBinding)) {
									typeCheckElimination.setTypeLocalVariable(fragment);
									break;
								}
							}
							List<Statement> enhancedForStatements = statementExtractor.getEnhancedForStatements(typeCheckMethod.getBody());
							for(Statement eFStatement : enhancedForStatements) {
								EnhancedForStatement enhancedForStatement = (EnhancedForStatement)eFStatement;
								SingleVariableDeclaration formalParameter = enhancedForStatement.getParameter();
								IVariableBinding parameterVariableBinding = formalParameter.resolveBinding();
								if(parameterVariableBinding.isEqualTo(switchStatementExpressionNameVariableBinding)) {
									typeCheckElimination.setTypeLocalVariable(formalParameter);
									break;
								}
							}
						}
					}
				}
				else if(switchStatementExpressionName instanceof MethodInvocation) {
					MethodInvocation switchStatementExpressionMethodInvocation = (MethodInvocation)switchStatementExpressionName;
					Expression invoker = switchStatementExpressionMethodInvocation.getExpression();
					IMethodBinding switchStatementExpressionMethodBinding = switchStatementExpressionMethodInvocation.resolveMethodBinding();
					if(!switchStatementExpressionMethodBinding.getDeclaringClass().isEqualTo(typeDeclaration.resolveBinding()) &&
							invoker != null && !(invoker instanceof ThisExpression)) {
						typeCheckElimination.setTypeMethodInvocation(switchStatementExpressionMethodInvocation);
					}
				}
			}
		}
		
		Set<Expression> typeCheckExpressions = typeCheckElimination.getTypeCheckExpressions();
		for(Expression typeCheckExpression : typeCheckExpressions) {
			if(typeCheckExpression.getParent() instanceof SwitchCase) {
				if(typeCheckExpression instanceof SimpleName) {
					SimpleName simpleName = (SimpleName)typeCheckExpression;
					IBinding binding = simpleName.resolveBinding();
					if(binding != null && binding.getKind() == IBinding.VARIABLE) {
						IVariableBinding variableBinding = (IVariableBinding)binding;
						if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) != 0) {
							ArrayList<SimpleName> staticTypes = new ArrayList<SimpleName>();
							staticTypes.add(simpleName);
							typeCheckElimination.addStaticType(typeCheckExpression, staticTypes);
						}
					}
				}
				else if(typeCheckExpression instanceof QualifiedName) {
					QualifiedName qualifiedName = (QualifiedName)typeCheckExpression;
					IBinding binding = qualifiedName.resolveBinding();
					if(binding != null && binding.getKind() == IBinding.VARIABLE) {
						IVariableBinding variableBinding = (IVariableBinding)binding;
						if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) != 0) {
							ArrayList<SimpleName> staticTypes = new ArrayList<SimpleName>();
							staticTypes.add(qualifiedName.getName());
							typeCheckElimination.addStaticType(typeCheckExpression, staticTypes);
						}
					}
				}
				else if(typeCheckExpression instanceof FieldAccess) {
					FieldAccess fieldAccess = (FieldAccess)typeCheckExpression;
					IVariableBinding variableBinding = fieldAccess.resolveFieldBinding();
					if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) != 0) {
						ArrayList<SimpleName> staticTypes = new ArrayList<SimpleName>();
						staticTypes.add(fieldAccess.getName());
						typeCheckElimination.addStaticType(typeCheckExpression, staticTypes);
					}
				}
			}
			else if(typeCheckExpression instanceof InstanceofExpression) {
				InstanceofExpression instanceofExpression = (InstanceofExpression)typeCheckExpression;
				IfStatementExpressionAnalyzer analyzer = new IfStatementExpressionAnalyzer(instanceofExpression);
				Expression operandExpression = extractOperand(instanceofExpression.getLeftOperand());
				if(operandExpression != null) {
					if(operandExpression instanceof SimpleName) {
						SimpleName operandName = (SimpleName)operandExpression;
						SimpleName keySimpleName = containsTypeVariableKey(operandName);
						if(keySimpleName != null) {
							typeVariableCounterMap.put(keySimpleName, typeVariableCounterMap.get(keySimpleName)+1);
						}
						else {
							typeVariableCounterMap.put(operandName, 1);
						}
						analyzer.putTypeVariableExpression(operandName, instanceofExpression);
						analyzer.putTypeVariableSubclass(operandName, instanceofExpression.getRightOperand());
					}
					else if(operandExpression instanceof MethodInvocation) {
						MethodInvocation operandMethodInvocation = (MethodInvocation)operandExpression;
						MethodInvocation keyMethodInvocation = containsTypeMethodInvocationKey(operandMethodInvocation);
						if(keyMethodInvocation != null) {
							typeMethodInvocationCounterMap.put(keyMethodInvocation, typeMethodInvocationCounterMap.get(keyMethodInvocation)+1);
						}
						else {
							typeMethodInvocationCounterMap.put(operandMethodInvocation, 1);
						}
						analyzer.putTypeMethodInvocationExpression(operandMethodInvocation, instanceofExpression);
						analyzer.putTypeMethodInvocationSubclass(operandMethodInvocation, instanceofExpression.getRightOperand());
					}
					//typeCheckElimination.addSubclassType(typeCheckExpression, instanceofExpression.getRightOperand());
					complexExpressionMap.put(typeCheckExpression, analyzer);
				}
			}
			else if(typeCheckExpression instanceof InfixExpression) {
				InfixExpression infixExpression = (InfixExpression)typeCheckExpression;
				IfStatementExpressionAnalyzer analyzer = new IfStatementExpressionAnalyzer(infixExpression);
				for(InfixExpression leafInfixExpression : analyzer.getInfixExpressionsWithEqualsOperator()) {
					Expression leftOperand = leafInfixExpression.getLeftOperand();
					Expression rightOperand = leafInfixExpression.getRightOperand();
					Expression leftOperandExpression = extractOperand(leftOperand);
					Expression rightOperandExpression = extractOperand(rightOperand);
					SimpleName typeVariableName = null;
					SimpleName staticFieldName = null;
					MethodInvocation typeMethodInvocation = null;
					Type subclassType = null;
					if(leftOperandExpression != null && rightOperandExpression != null) {
						if(leftOperandExpression instanceof SimpleName) {
							SimpleName leftOperandName = (SimpleName)leftOperandExpression;
							IBinding leftOperandNameBinding = leftOperandName.resolveBinding();
							if(leftOperandNameBinding != null && leftOperandNameBinding.getKind() == IBinding.VARIABLE) {
								IVariableBinding leftOperandNameVariableBinding = (IVariableBinding)leftOperandNameBinding;
								if(leftOperandNameVariableBinding.isField() && (leftOperandNameVariableBinding.getModifiers() & Modifier.STATIC) != 0)
									staticFieldName = leftOperandName;
							}
						}
						if(rightOperandExpression instanceof SimpleName) {
							SimpleName rightOperandName = (SimpleName)rightOperandExpression;
							IBinding rightOperandNameBinding = rightOperandName.resolveBinding();
							if(rightOperandNameBinding != null && rightOperandNameBinding.getKind() == IBinding.VARIABLE) {
								IVariableBinding rightOperandNameVariableBinding = (IVariableBinding)rightOperandNameBinding;
								if(rightOperandNameVariableBinding.isField() && (rightOperandNameVariableBinding.getModifiers() & Modifier.STATIC) != 0)
									staticFieldName = rightOperandName;
							}
						}
						if(staticFieldName != null && staticFieldName.equals(leftOperandExpression)) {
							if(rightOperandExpression instanceof SimpleName) {
								SimpleName rightOperandName = (SimpleName)rightOperandExpression;
								typeVariableName = rightOperandName;
							}
							else if(rightOperandExpression instanceof MethodInvocation) {
								MethodInvocation rightOperandMethodInvocation = (MethodInvocation)rightOperandExpression;
								typeMethodInvocation = rightOperandMethodInvocation;
							}
						}
						else if(staticFieldName != null && staticFieldName.equals(rightOperandExpression)) {
							if(leftOperandExpression instanceof SimpleName) {
								SimpleName leftOperandName = (SimpleName)leftOperandExpression;
								typeVariableName = leftOperandName;
							}
							else if(leftOperandExpression instanceof MethodInvocation) {
								MethodInvocation leftOperandMethodInvocation = (MethodInvocation)leftOperandExpression;
								typeMethodInvocation = leftOperandMethodInvocation;
							}
						}
					}
					else if(leftOperandExpression != null && rightOperandExpression == null) {
						if(rightOperand instanceof TypeLiteral) {
							TypeLiteral typeLiteral = (TypeLiteral)rightOperand;
							subclassType = typeLiteral.getType();
							if(leftOperandExpression instanceof SimpleName) {
								SimpleName leftOperandName = (SimpleName)leftOperandExpression;
								typeVariableName = leftOperandName;
							}
							else if(leftOperandExpression instanceof MethodInvocation) {
								MethodInvocation leftOperandMethodInvocation = (MethodInvocation)leftOperandExpression;
								typeMethodInvocation = leftOperandMethodInvocation;
							}
						}
					}
					else if(leftOperandExpression == null && rightOperandExpression != null) {
						if(leftOperand instanceof TypeLiteral) {
							TypeLiteral typeLiteral = (TypeLiteral)leftOperand;
							subclassType = typeLiteral.getType();
							if(rightOperandExpression instanceof SimpleName) {
								SimpleName rightOperandName = (SimpleName)rightOperandExpression;
								typeVariableName = rightOperandName;
							}
							else if(rightOperandExpression instanceof MethodInvocation) {
								MethodInvocation rightOperandMethodInvocation = (MethodInvocation)rightOperandExpression;
								typeMethodInvocation = rightOperandMethodInvocation;
							}
						}
					}
					if(typeVariableName != null && staticFieldName != null) {
						SimpleName keySimpleName = containsTypeVariableKey(typeVariableName);
						if(keySimpleName != null) {
							typeVariableCounterMap.put(keySimpleName, typeVariableCounterMap.get(keySimpleName)+1);
						}
						else {
							typeVariableCounterMap.put(typeVariableName, 1);
						}
						analyzer.putTypeVariableExpression(typeVariableName, leafInfixExpression);
						analyzer.putTypeVariableStaticField(typeVariableName, staticFieldName);
					}
					if(typeMethodInvocation != null && staticFieldName != null) {
						MethodInvocation keyMethodInvocation = containsTypeMethodInvocationKey(typeMethodInvocation);
						if(keyMethodInvocation != null) {
							typeMethodInvocationCounterMap.put(keyMethodInvocation, typeMethodInvocationCounterMap.get(keyMethodInvocation)+1);
						}
						else {
							typeMethodInvocationCounterMap.put(typeMethodInvocation, 1);
						}
						analyzer.putTypeMethodInvocationExpression(typeMethodInvocation, leafInfixExpression);
						analyzer.putTypeMethodInvocationStaticField(typeMethodInvocation, staticFieldName);
					}
					if(typeVariableName != null && subclassType != null) {
						SimpleName keySimpleName = containsTypeVariableKey(typeVariableName);
						if(keySimpleName != null) {
							typeVariableCounterMap.put(keySimpleName, typeVariableCounterMap.get(keySimpleName)+1);
						}
						else {
							typeVariableCounterMap.put(typeVariableName, 1);
						}
						analyzer.putTypeVariableExpression(typeVariableName, leafInfixExpression);
						analyzer.putTypeVariableSubclass(typeVariableName, subclassType);
					}
					if(typeMethodInvocation != null && subclassType != null) {
						MethodInvocation keyMethodInvocation = containsTypeMethodInvocationKey(typeMethodInvocation);
						if(keyMethodInvocation != null) {
							typeMethodInvocationCounterMap.put(keyMethodInvocation, typeMethodInvocationCounterMap.get(keyMethodInvocation)+1);
						}
						else {
							typeMethodInvocationCounterMap.put(typeMethodInvocation, 1);
						}
						analyzer.putTypeMethodInvocationExpression(typeMethodInvocation, leafInfixExpression);
						analyzer.putTypeMethodInvocationSubclass(typeMethodInvocation, subclassType);
					}
				}
				for(InstanceofExpression leafInstanceofExpression : analyzer.getInstanceofExpressions()) {
					Expression operandExpression = extractOperand(leafInstanceofExpression.getLeftOperand());
					if(operandExpression != null) {
						if(operandExpression instanceof SimpleName) {
							SimpleName operandName = (SimpleName)operandExpression;
							SimpleName keySimpleName = containsTypeVariableKey(operandName);
							if(keySimpleName != null) {
								typeVariableCounterMap.put(keySimpleName, typeVariableCounterMap.get(keySimpleName)+1);
							}
							else {
								typeVariableCounterMap.put(operandName, 1);
							}
							analyzer.putTypeVariableExpression(operandName, leafInstanceofExpression);
							analyzer.putTypeVariableSubclass(operandName, leafInstanceofExpression.getRightOperand());
						}
						else if(operandExpression instanceof MethodInvocation) {
							MethodInvocation operandMethodInvocation = (MethodInvocation)operandExpression;
							MethodInvocation keyMethodInvocation = containsTypeMethodInvocationKey(operandMethodInvocation);
							if(keyMethodInvocation != null) {
								typeMethodInvocationCounterMap.put(keyMethodInvocation, typeMethodInvocationCounterMap.get(keyMethodInvocation)+1);
							}
							else {
								typeMethodInvocationCounterMap.put(operandMethodInvocation, 1);
							}
							analyzer.putTypeMethodInvocationExpression(operandMethodInvocation, leafInstanceofExpression);
							analyzer.putTypeMethodInvocationSubclass(operandMethodInvocation, leafInstanceofExpression.getRightOperand());
						}
					}
				}
				complexExpressionMap.put(typeCheckExpression, analyzer);
			}
		}
		for(SimpleName typeVariable : typeVariableCounterMap.keySet()) {
			if(isValidTypeVariable(typeVariable, typeCheckExpressions)) {
				for(Expression complexExpression : complexExpressionMap.keySet()) {
					IfStatementExpressionAnalyzer analyzer = complexExpressionMap.get(complexExpression);
					for(SimpleName analyzerTypeVariable : analyzer.getTargetVariables()) {
						if(analyzerTypeVariable.resolveBinding().isEqualTo(typeVariable.resolveBinding())) {
							if(typeVariableCounterMap.get(typeVariable) == typeCheckExpressions.size()) {
								typeCheckElimination.addRemainingIfStatementExpression(analyzer.getCompleteExpression(),
								analyzer.getRemainingExpression(analyzer.getTypeVariableExpression(analyzerTypeVariable)));
							}
							List<SimpleName> staticFields = analyzer.getTypeVariableStaticField(analyzerTypeVariable);
							if(staticFields != null) {
								typeCheckElimination.addStaticType(analyzer.getCompleteExpression(), staticFields);
							}
							List<Type> subclassTypes = analyzer.getTypeVariableSubclass(analyzerTypeVariable);
							if(subclassTypes != null) {
								typeCheckElimination.addSubclassType(analyzer.getCompleteExpression(), subclassTypes);
							}
						}
					}
				}
				IBinding binding = typeVariable.resolveBinding();
				if(binding != null && binding.getKind() == IBinding.VARIABLE) {
					IVariableBinding variableBinding = (IVariableBinding)binding;
					if(variableBinding.isField()) {
						for(FieldDeclaration field : fields) {
							List<VariableDeclarationFragment> fragments = field.fragments();
							for(VariableDeclarationFragment fragment : fragments) {
								IVariableBinding fragmentVariableBinding = fragment.resolveBinding();
								if(fragmentVariableBinding.isEqualTo(variableBinding)) {
									typeCheckElimination.setTypeField(fragment);
									for(MethodDeclaration method : methods) {
										SimpleName fieldInstruction = MethodDeclarationUtility.isSetter(method);
										if(fieldInstruction != null && fragment.getName().getIdentifier().equals(fieldInstruction.getIdentifier())) {
											typeCheckElimination.setTypeFieldSetterMethod(method);
										}
										fieldInstruction = MethodDeclarationUtility.isGetter(method);
										if(fieldInstruction != null && fragment.getName().getIdentifier().equals(fieldInstruction.getIdentifier())) {
											typeCheckElimination.setTypeFieldGetterMethod(method);
										}
									}
									break;
								}
							}
						}
					}
					else if(variableBinding.isParameter()) {
						List<SingleVariableDeclaration> parameters = typeCheckMethod.parameters();
						for(SingleVariableDeclaration parameter : parameters) {
							IVariableBinding parameterVariableBinding = parameter.resolveBinding();
							if(parameterVariableBinding.isEqualTo(variableBinding)) {
								typeCheckElimination.setTypeLocalVariable(parameter);
								break;
							}
						}
					}
					else {
						StatementExtractor statementExtractor = new StatementExtractor();
						ExpressionExtractor expressionExtractor = new ExpressionExtractor();
						List<VariableDeclarationFragment> variableDeclarationFragments = new ArrayList<VariableDeclarationFragment>();
						List<Statement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(typeCheckMethod.getBody());
						for(Statement statement : variableDeclarationStatements) {
							VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
							List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
							variableDeclarationFragments.addAll(fragments);
						}
						List<Expression> variableDeclarationExpressions = expressionExtractor.getVariableDeclarationExpressions(typeCheckMethod.getBody());
						for(Expression expression : variableDeclarationExpressions) {
							VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)expression;
							List<VariableDeclarationFragment> fragments = variableDeclarationExpression.fragments();
							variableDeclarationFragments.addAll(fragments);
						}
						for(VariableDeclarationFragment fragment : variableDeclarationFragments) {
							IVariableBinding fragmentVariableBinding = fragment.resolveBinding();
							if(fragmentVariableBinding.isEqualTo(variableBinding)) {
								typeCheckElimination.setTypeLocalVariable(fragment);
								break;
							}
						}
						List<Statement> enhancedForStatements = statementExtractor.getEnhancedForStatements(typeCheckMethod.getBody());
						for(Statement eFStatement : enhancedForStatements) {
							EnhancedForStatement enhancedForStatement = (EnhancedForStatement)eFStatement;
							SingleVariableDeclaration formalParameter = enhancedForStatement.getParameter();
							IVariableBinding parameterVariableBinding = formalParameter.resolveBinding();
							if(parameterVariableBinding.isEqualTo(variableBinding)) {
								typeCheckElimination.setTypeLocalVariable(formalParameter);
								break;
							}
						}
					}
				}
			}
		}
		for(MethodInvocation typeMethodInvocation : typeMethodInvocationCounterMap.keySet()) {
			if(isValidTypeMethodInvocation(typeMethodInvocation, typeCheckExpressions)) {
				for(Expression complexExpression : complexExpressionMap.keySet()) {
					IfStatementExpressionAnalyzer analyzer = complexExpressionMap.get(complexExpression);
					for(MethodInvocation analyzerTypeMethodInvocation : analyzer.getTargetMethodInvocations()) {
						if(analyzerTypeMethodInvocation.resolveMethodBinding().isEqualTo(typeMethodInvocation.resolveMethodBinding())) {
							if(typeMethodInvocationCounterMap.get(typeMethodInvocation) == typeCheckExpressions.size()) {
								typeCheckElimination.addRemainingIfStatementExpression(analyzer.getCompleteExpression(),
								analyzer.getRemainingExpression(analyzer.getTypeMethodInvocationExpression(analyzerTypeMethodInvocation)));
							}
							List<SimpleName> staticFields = analyzer.getTypeMethodInvocationStaticField(analyzerTypeMethodInvocation);
							if(staticFields != null) {
								typeCheckElimination.addStaticType(analyzer.getCompleteExpression(), staticFields);
							}
							List<Type> subclassTypes = analyzer.getTypeMethodInvocationSubclass(analyzerTypeMethodInvocation);
							if(subclassTypes != null) {
								typeCheckElimination.addSubclassType(analyzer.getCompleteExpression(), subclassTypes);
							}
						}
					}
				}
				Expression invoker = typeMethodInvocation.getExpression();
				IMethodBinding typeMethodInvocationBinding = typeMethodInvocation.resolveMethodBinding();
				if(!typeMethodInvocationBinding.getDeclaringClass().isEqualTo(typeDeclaration.resolveBinding()) &&
						invoker != null && !(invoker instanceof ThisExpression)) {
					typeCheckElimination.setTypeMethodInvocation(typeMethodInvocation);
				}
			}
		}
		processTypeCheckCodeFragmentBranches();
	}
	
	private void processTypeCheckCodeFragmentBranches() {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<ArrayList<Statement>> allTypeCheckStatements = typeCheckElimination.getTypeCheckStatements();
		if(!typeCheckElimination.getDefaultCaseStatements().isEmpty()) {
			allTypeCheckStatements.add(typeCheckElimination.getDefaultCaseStatements());
		}
		StatementExtractor statementExtractor = new StatementExtractor();
		List<VariableDeclarationFragment> variableDeclarationFragmentsInsideTypeCheckMethodApartFromTypeCheckCodeFragment = new ArrayList<VariableDeclarationFragment>();
		List<Statement> variableDeclarationStatementsInsideTypeCheckMethod = statementExtractor.getVariableDeclarationStatements(typeCheckMethod.getBody());
		for(Statement statement : variableDeclarationStatementsInsideTypeCheckMethod) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
			List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
			variableDeclarationFragmentsInsideTypeCheckMethodApartFromTypeCheckCodeFragment.addAll(fragments);
		}
		List<Expression> variableDeclarationExpressionsInsideTypeCheckMethod = expressionExtractor.getVariableDeclarationExpressions(typeCheckMethod.getBody());
		for(Expression expression : variableDeclarationExpressionsInsideTypeCheckMethod) {
			VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)expression;
			List<VariableDeclarationFragment> fragments = variableDeclarationExpression.fragments();
			variableDeclarationFragmentsInsideTypeCheckMethodApartFromTypeCheckCodeFragment.addAll(fragments);
		}
		for(ArrayList<Statement> typeCheckStatementList : allTypeCheckStatements) {
			for(Statement statement : typeCheckStatementList) {
				List<Statement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(statement);
				for(Statement statement2 : variableDeclarationStatements) {
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement2;
					List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
					variableDeclarationFragmentsInsideTypeCheckMethodApartFromTypeCheckCodeFragment.removeAll(fragments);
				}
				List<Expression> variableDeclarationExpressions = expressionExtractor.getVariableDeclarationExpressions(statement);
				for(Expression expression : variableDeclarationExpressions) {
					VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)expression;
					List<VariableDeclarationFragment> fragments = variableDeclarationExpression.fragments();
					variableDeclarationFragmentsInsideTypeCheckMethodApartFromTypeCheckCodeFragment.removeAll(fragments);
				}
			}
		}
		List<Statement> enhancedForStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment = statementExtractor.getEnhancedForStatements(typeCheckMethod.getBody());
		for(ArrayList<Statement> typeCheckStatementList : allTypeCheckStatements) {
			for(Statement statement : typeCheckStatementList) {
				enhancedForStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment.removeAll(statementExtractor.getEnhancedForStatements(statement));
			}
		}
		for(ArrayList<Statement> typeCheckStatementList : allTypeCheckStatements) {
			for(Statement statement : typeCheckStatementList) {
				//checking for methods of the Source class invoked inside the type-checking branches
				List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(statement);
				for(Expression expression : methodInvocations) {
					if(expression instanceof MethodInvocation) {
						MethodInvocation methodInvocation = (MethodInvocation)expression;
						IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
						if(methodBinding.getDeclaringClass().isEqualTo(typeDeclaration.resolveBinding())) {
							for(MethodDeclaration method : methods) {
								if(method.resolveBinding().isEqualTo(methodBinding)) {
									typeCheckElimination.addAccessedMethod(method);
								}
							}
						}
						else if(methodInvocation.getExpression() == null || (methodInvocation.getExpression() != null && methodInvocation.getExpression() instanceof ThisExpression)) {
							ITypeBinding superclassTypeBinding = typeDeclaration.resolveBinding().getSuperclass();
							while(superclassTypeBinding != null && !superclassTypeBinding.isEqualTo(methodBinding.getDeclaringClass())) {
								superclassTypeBinding = superclassTypeBinding.getSuperclass();
							}
							if(methodBinding.getDeclaringClass().isEqualTo(superclassTypeBinding))
								typeCheckElimination.addSuperAccessedMethod(methodBinding);
						}
					}
				}
				//checking for Source class fields or parameters of the type-checking method accessed inside the type-checking branches
				List<Expression> variableInstructions = expressionExtractor.getVariableInstructions(statement);
				for(Expression variableInstruction : variableInstructions) {
					SimpleName simpleName = (SimpleName)variableInstruction;
					IBinding variableInstructionBinding = simpleName.resolveBinding();
					if(variableInstructionBinding != null && variableInstructionBinding.getKind() == IBinding.VARIABLE) {
						IVariableBinding variableInstructionVariableBinding = (IVariableBinding)variableInstructionBinding;
						if(variableInstructionVariableBinding.isField()) {
							if(variableInstructionVariableBinding.getDeclaringClass() != null) {
								if(variableInstructionVariableBinding.getDeclaringClass().isEqualTo(typeDeclaration.resolveBinding())) {
									for(FieldDeclaration field : fields) {
										List<VariableDeclarationFragment> fragments = field.fragments();
										for(VariableDeclarationFragment fragment : fragments) {
											IVariableBinding fragmentVariableBinding = fragment.resolveBinding();
											if(fragmentVariableBinding.isEqualTo(variableInstructionVariableBinding)) {
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
												boolean isAssigned = false;
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
														isAssigned = true;
														typeCheckElimination.addAssignedField(fragment);
														if(!assignment.getOperator().equals(Assignment.Operator.ASSIGN))
															typeCheckElimination.addAccessedField(fragment);
													}
												}
												else if(parentExpression.getParent() instanceof PostfixExpression) {
													//PostfixExpression postfixExpression = (PostfixExpression)parentExpression.getParent();
													isAssigned = true;
													typeCheckElimination.addAssignedField(fragment);
													typeCheckElimination.addAccessedField(fragment);
												}
												else if(parentExpression.getParent() instanceof PrefixExpression) {
													PrefixExpression prefixExpression = (PrefixExpression)parentExpression.getParent();
													PrefixExpression.Operator operator = prefixExpression.getOperator();
													if(operator.equals(PrefixExpression.Operator.INCREMENT) || operator.equals(PrefixExpression.Operator.DECREMENT)) {
														isAssigned = true;
														typeCheckElimination.addAssignedField(fragment);
														typeCheckElimination.addAccessedField(fragment);
													}
												}
												if(!isAssigned)
													typeCheckElimination.addAccessedField(fragment);
											}
										}
									}
								}
								else {
									ITypeBinding superclassTypeBinding = typeDeclaration.resolveBinding().getSuperclass();
									while(superclassTypeBinding != null && !superclassTypeBinding.isEqualTo(variableInstructionVariableBinding.getDeclaringClass())) {
										superclassTypeBinding = superclassTypeBinding.getSuperclass();
									}
									if(variableInstructionVariableBinding.getDeclaringClass().isEqualTo(superclassTypeBinding)) {
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
										boolean isAssigned = false;
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
												isAssigned = true;
												typeCheckElimination.addSuperAssignedFieldBinding(variableInstructionVariableBinding, null);
												if(!assignment.getOperator().equals(Assignment.Operator.ASSIGN))
													typeCheckElimination.addSuperAccessedFieldBinding(variableInstructionVariableBinding, null);
											}
										}
										else if(parentExpression.getParent() instanceof PostfixExpression) {
											//PostfixExpression postfixExpression = (PostfixExpression)parentExpression.getParent();
											isAssigned = true;
											typeCheckElimination.addSuperAssignedFieldBinding(variableInstructionVariableBinding, null);
											typeCheckElimination.addSuperAccessedFieldBinding(variableInstructionVariableBinding, null);
										}
										else if(parentExpression.getParent() instanceof PrefixExpression) {
											PrefixExpression prefixExpression = (PrefixExpression)parentExpression.getParent();
											PrefixExpression.Operator operator = prefixExpression.getOperator();
											if(operator.equals(PrefixExpression.Operator.INCREMENT) || operator.equals(PrefixExpression.Operator.DECREMENT)) {
												isAssigned = true;
												typeCheckElimination.addSuperAssignedFieldBinding(variableInstructionVariableBinding, null);
												typeCheckElimination.addSuperAccessedFieldBinding(variableInstructionVariableBinding, null);
											}
										}
										if(!isAssigned)
											typeCheckElimination.addSuperAccessedFieldBinding(variableInstructionVariableBinding, null);
									}
								}
							}
						}
						else if(variableInstructionVariableBinding.isParameter()) {
							List<SingleVariableDeclaration> parameters = typeCheckMethod.parameters();
							for(SingleVariableDeclaration parameter : parameters) {
								IVariableBinding parameterVariableBinding = parameter.resolveBinding();
								if(parameterVariableBinding.isEqualTo(variableInstructionVariableBinding)) {
									boolean isAssigned = false;
									if(simpleName.getParent() instanceof Assignment) {
										Assignment assignment = (Assignment)simpleName.getParent();
										Expression leftHandSide = assignment.getLeftHandSide();
										if(leftHandSide instanceof SimpleName) {
											SimpleName leftHandSideName = (SimpleName)leftHandSide;
											if(leftHandSideName.equals(simpleName)) {
												isAssigned = true;
												typeCheckElimination.addAssignedParameter(parameter);
											}
										}
									}
									else if(simpleName.getParent() instanceof PostfixExpression) {
										//PostfixExpression postfixExpression = (PostfixExpression)simpleName.getParent();
										isAssigned = true;
										typeCheckElimination.addAssignedParameter(parameter);
									}
									else if(simpleName.getParent() instanceof PrefixExpression) {
										PrefixExpression prefixExpression = (PrefixExpression)simpleName.getParent();
										PrefixExpression.Operator operator = prefixExpression.getOperator();
										if(operator.equals(PrefixExpression.Operator.INCREMENT) || operator.equals(PrefixExpression.Operator.DECREMENT)) {
											isAssigned = true;
											typeCheckElimination.addAssignedParameter(parameter);
										}
									}
									if(!isAssigned)
										typeCheckElimination.addAccessedParameter(parameter);
									break;
								}
							}
						}
						//checking for local variables accessed inside the type-checking code branches, but declared outside them
						else {
							for(VariableDeclarationFragment fragment : variableDeclarationFragmentsInsideTypeCheckMethodApartFromTypeCheckCodeFragment) {
								IVariableBinding fragmentVariableBinding = fragment.resolveBinding();
								if(fragmentVariableBinding.isEqualTo(variableInstructionVariableBinding)) {
									boolean isAssigned = false;
									if(simpleName.getParent() instanceof Assignment) {
										Assignment assignment = (Assignment)simpleName.getParent();
										Expression leftHandSide = assignment.getLeftHandSide();
										if(leftHandSide instanceof SimpleName) {
											SimpleName leftHandSideName = (SimpleName)leftHandSide;
											if(leftHandSideName.equals(simpleName)) {
												isAssigned = true;
												typeCheckElimination.addAssignedLocalVariable(fragment);
											}
										}
									}
									else if(simpleName.getParent() instanceof PostfixExpression) {
										//PostfixExpression postfixExpression = (PostfixExpression)simpleName.getParent();
										isAssigned = true;
										typeCheckElimination.addAssignedLocalVariable(fragment);
									}
									else if(simpleName.getParent() instanceof PrefixExpression) {
										PrefixExpression prefixExpression = (PrefixExpression)simpleName.getParent();
										PrefixExpression.Operator operator = prefixExpression.getOperator();
										if(operator.equals(PrefixExpression.Operator.INCREMENT) || operator.equals(PrefixExpression.Operator.DECREMENT)) {
											isAssigned = true;
											typeCheckElimination.addAssignedLocalVariable(fragment);
										}
									}
									if(!isAssigned)
										typeCheckElimination.addAccessedLocalVariable(fragment);
									break;
								}
							}
							for(Statement eFStatement : enhancedForStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment) {
								EnhancedForStatement enhancedForStatement = (EnhancedForStatement)eFStatement;
								SingleVariableDeclaration formalParameter = enhancedForStatement.getParameter();
								IVariableBinding parameterVariableBinding = formalParameter.resolveBinding();
								if(parameterVariableBinding.isEqualTo(variableInstructionVariableBinding)) {
									boolean isAssigned = false;
									if(simpleName.getParent() instanceof Assignment) {
										Assignment assignment = (Assignment)simpleName.getParent();
										Expression leftHandSide = assignment.getLeftHandSide();
										if(leftHandSide instanceof SimpleName) {
											SimpleName leftHandSideName = (SimpleName)leftHandSide;
											if(leftHandSideName.equals(simpleName)) {
												isAssigned = true;
												typeCheckElimination.addAssignedLocalVariable(formalParameter);
											}
										}
									}
									else if(simpleName.getParent() instanceof PostfixExpression) {
										//PostfixExpression postfixExpression = (PostfixExpression)simpleName.getParent();
										isAssigned = true;
										typeCheckElimination.addAssignedLocalVariable(formalParameter);
									}
									else if(simpleName.getParent() instanceof PrefixExpression) {
										PrefixExpression prefixExpression = (PrefixExpression)simpleName.getParent();
										PrefixExpression.Operator operator = prefixExpression.getOperator();
										if(operator.equals(PrefixExpression.Operator.INCREMENT) || operator.equals(PrefixExpression.Operator.DECREMENT)) {
											isAssigned = true;
											typeCheckElimination.addAssignedLocalVariable(formalParameter);
										}
									}
									if(!isAssigned)
										typeCheckElimination.addAccessedLocalVariable(formalParameter);
									break;
								}
							}
						}
					}
				}
			}
		}
		processRemainingIfStatementExpressions(variableDeclarationFragmentsInsideTypeCheckMethodApartFromTypeCheckCodeFragment,
				enhancedForStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment);
	}

	private void processRemainingIfStatementExpressions(List<VariableDeclarationFragment> variableDeclarationFragmentsInsideTypeCheckMethodApartFromTypeCheckCodeFragment,
			List<Statement> enhancedForStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		for(Expression complexExpression : complexExpressionMap.keySet()) {
			DefaultMutableTreeNode root = typeCheckElimination.getRemainingIfStatementExpression(complexExpression);
			if(root != null) {
				DefaultMutableTreeNode leaf = root.getFirstLeaf();
				while(leaf != null) {
					Expression leafExpression = (Expression)leaf.getUserObject();
					//checking for methods of the Source class invoked inside the type-checking branches
					List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(leafExpression);
					for(Expression expression : methodInvocations) {
						if(expression instanceof MethodInvocation) {
							MethodInvocation methodInvocation = (MethodInvocation)expression;
							IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
							if(methodBinding.getDeclaringClass().isEqualTo(typeDeclaration.resolveBinding())) {
								for(MethodDeclaration method : methods) {
									if(method.resolveBinding().isEqualTo(methodBinding)) {
										typeCheckElimination.addAccessedMethod(method);
									}
								}
							}
							else if(methodInvocation.getExpression() == null || (methodInvocation.getExpression() != null && methodInvocation.getExpression() instanceof ThisExpression)) {
								ITypeBinding superclassTypeBinding = typeDeclaration.resolveBinding().getSuperclass();
								while(superclassTypeBinding != null && !superclassTypeBinding.isEqualTo(methodBinding.getDeclaringClass())) {
									superclassTypeBinding = superclassTypeBinding.getSuperclass();
								}
								if(methodBinding.getDeclaringClass().isEqualTo(superclassTypeBinding))
									typeCheckElimination.addSuperAccessedMethod(methodBinding);
							}
						}
					}
					//checking for Source class fields or parameters of the type-checking method accessed inside the type-checking branches
					List<Expression> variableInstructions = expressionExtractor.getVariableInstructions(leafExpression);
					for(Expression variableInstruction : variableInstructions) {
						SimpleName simpleName = (SimpleName)variableInstruction;
						IBinding variableInstructionBinding = simpleName.resolveBinding();
						if(variableInstructionBinding != null && variableInstructionBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding variableInstructionVariableBinding = (IVariableBinding)variableInstructionBinding;
							if(variableInstructionVariableBinding.isField()) {
								if(variableInstructionVariableBinding.getDeclaringClass() != null) {
									if(variableInstructionVariableBinding.getDeclaringClass().isEqualTo(typeDeclaration.resolveBinding())) {
										for(FieldDeclaration field : fields) {
											List<VariableDeclarationFragment> fragments = field.fragments();
											for(VariableDeclarationFragment fragment : fragments) {
												IVariableBinding fragmentVariableBinding = fragment.resolveBinding();
												if(fragmentVariableBinding.isEqualTo(variableInstructionVariableBinding)) {
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
													boolean isAssigned = false;
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
															isAssigned = true;
															typeCheckElimination.addAssignedField(fragment);
															if(!assignment.getOperator().equals(Assignment.Operator.ASSIGN))
																typeCheckElimination.addAccessedField(fragment);
														}
													}
													else if(parentExpression.getParent() instanceof PostfixExpression) {
														//PostfixExpression postfixExpression = (PostfixExpression)parentExpression.getParent();
														isAssigned = true;
														typeCheckElimination.addAssignedField(fragment);
														typeCheckElimination.addAccessedField(fragment);
													}
													else if(parentExpression.getParent() instanceof PrefixExpression) {
														PrefixExpression prefixExpression = (PrefixExpression)parentExpression.getParent();
														PrefixExpression.Operator operator = prefixExpression.getOperator();
														if(operator.equals(PrefixExpression.Operator.INCREMENT) || operator.equals(PrefixExpression.Operator.DECREMENT)) {
															isAssigned = true;
															typeCheckElimination.addAssignedField(fragment);
															typeCheckElimination.addAccessedField(fragment);
														}
													}
													if(!isAssigned)
														typeCheckElimination.addAccessedField(fragment);
												}
											}
										}
									}
									else {
										ITypeBinding superclassTypeBinding = typeDeclaration.resolveBinding().getSuperclass();
										while(superclassTypeBinding != null && !superclassTypeBinding.isEqualTo(variableInstructionVariableBinding.getDeclaringClass())) {
											superclassTypeBinding = superclassTypeBinding.getSuperclass();
										}
										if(variableInstructionVariableBinding.getDeclaringClass().isEqualTo(superclassTypeBinding)) {
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
											boolean isAssigned = false;
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
													isAssigned = true;
													typeCheckElimination.addSuperAssignedFieldBinding(variableInstructionVariableBinding, null);
													if(!assignment.getOperator().equals(Assignment.Operator.ASSIGN))
														typeCheckElimination.addSuperAccessedFieldBinding(variableInstructionVariableBinding, null);
												}
											}
											else if(parentExpression.getParent() instanceof PostfixExpression) {
												//PostfixExpression postfixExpression = (PostfixExpression)parentExpression.getParent();
												isAssigned = true;
												typeCheckElimination.addSuperAssignedFieldBinding(variableInstructionVariableBinding, null);
												typeCheckElimination.addSuperAccessedFieldBinding(variableInstructionVariableBinding, null);
											}
											else if(parentExpression.getParent() instanceof PrefixExpression) {
												PrefixExpression prefixExpression = (PrefixExpression)parentExpression.getParent();
												PrefixExpression.Operator operator = prefixExpression.getOperator();
												if(operator.equals(PrefixExpression.Operator.INCREMENT) || operator.equals(PrefixExpression.Operator.DECREMENT)) {
													isAssigned = true;
													typeCheckElimination.addSuperAssignedFieldBinding(variableInstructionVariableBinding, null);
													typeCheckElimination.addSuperAccessedFieldBinding(variableInstructionVariableBinding, null);
												}
											}
											if(!isAssigned)
												typeCheckElimination.addSuperAccessedFieldBinding(variableInstructionVariableBinding, null);
										}
									}
								}
							}
							else if(variableInstructionVariableBinding.isParameter()) {
								List<SingleVariableDeclaration> parameters = typeCheckMethod.parameters();
								for(SingleVariableDeclaration parameter : parameters) {
									IVariableBinding parameterVariableBinding = parameter.resolveBinding();
									if(parameterVariableBinding.isEqualTo(variableInstructionVariableBinding)) {
										boolean isAssigned = false;
										if(simpleName.getParent() instanceof Assignment) {
											Assignment assignment = (Assignment)simpleName.getParent();
											Expression leftHandSide = assignment.getLeftHandSide();
											if(leftHandSide instanceof SimpleName) {
												SimpleName leftHandSideName = (SimpleName)leftHandSide;
												if(leftHandSideName.equals(simpleName)) {
													isAssigned = true;
													typeCheckElimination.addAssignedParameter(parameter);
												}
											}
										}
										else if(simpleName.getParent() instanceof PostfixExpression) {
											//PostfixExpression postfixExpression = (PostfixExpression)simpleName.getParent();
											isAssigned = true;
											typeCheckElimination.addAssignedParameter(parameter);
										}
										else if(simpleName.getParent() instanceof PrefixExpression) {
											PrefixExpression prefixExpression = (PrefixExpression)simpleName.getParent();
											PrefixExpression.Operator operator = prefixExpression.getOperator();
											if(operator.equals(PrefixExpression.Operator.INCREMENT) || operator.equals(PrefixExpression.Operator.DECREMENT)) {
												isAssigned = true;
												typeCheckElimination.addAssignedParameter(parameter);
											}
										}
										if(!isAssigned)
											typeCheckElimination.addAccessedParameter(parameter);
										break;
									}
								}
							}
							//checking for local variables accessed inside the type-checking code branches, but declared outside them
							else {
								for(VariableDeclarationFragment fragment : variableDeclarationFragmentsInsideTypeCheckMethodApartFromTypeCheckCodeFragment) {
									IVariableBinding fragmentVariableBinding = fragment.resolveBinding();
									if(fragmentVariableBinding.isEqualTo(variableInstructionVariableBinding)) {
										boolean isAssigned = false;
										if(simpleName.getParent() instanceof Assignment) {
											Assignment assignment = (Assignment)simpleName.getParent();
											Expression leftHandSide = assignment.getLeftHandSide();
											if(leftHandSide instanceof SimpleName) {
												SimpleName leftHandSideName = (SimpleName)leftHandSide;
												if(leftHandSideName.equals(simpleName)) {
													isAssigned = true;
													typeCheckElimination.addAssignedLocalVariable(fragment);
												}
											}
										}
										else if(simpleName.getParent() instanceof PostfixExpression) {
											//PostfixExpression postfixExpression = (PostfixExpression)simpleName.getParent();
											isAssigned = true;
											typeCheckElimination.addAssignedLocalVariable(fragment);
										}
										else if(simpleName.getParent() instanceof PrefixExpression) {
											PrefixExpression prefixExpression = (PrefixExpression)simpleName.getParent();
											PrefixExpression.Operator operator = prefixExpression.getOperator();
											if(operator.equals(PrefixExpression.Operator.INCREMENT) || operator.equals(PrefixExpression.Operator.DECREMENT)) {
												isAssigned = true;
												typeCheckElimination.addAssignedLocalVariable(fragment);
											}
										}
										if(!isAssigned)
											typeCheckElimination.addAccessedLocalVariable(fragment);
										break;
									}
								}
								for(Statement eFStatement : enhancedForStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment) {
									EnhancedForStatement enhancedForStatement = (EnhancedForStatement)eFStatement;
									SingleVariableDeclaration formalParameter = enhancedForStatement.getParameter();
									IVariableBinding parameterVariableBinding = formalParameter.resolveBinding();
									if(parameterVariableBinding.isEqualTo(variableInstructionVariableBinding)) {
										boolean isAssigned = false;
										if(simpleName.getParent() instanceof Assignment) {
											Assignment assignment = (Assignment)simpleName.getParent();
											Expression leftHandSide = assignment.getLeftHandSide();
											if(leftHandSide instanceof SimpleName) {
												SimpleName leftHandSideName = (SimpleName)leftHandSide;
												if(leftHandSideName.equals(simpleName)) {
													isAssigned = true;
													typeCheckElimination.addAssignedLocalVariable(formalParameter);
												}
											}
										}
										else if(simpleName.getParent() instanceof PostfixExpression) {
											//PostfixExpression postfixExpression = (PostfixExpression)simpleName.getParent();
											isAssigned = true;
											typeCheckElimination.addAssignedLocalVariable(formalParameter);
										}
										else if(simpleName.getParent() instanceof PrefixExpression) {
											PrefixExpression prefixExpression = (PrefixExpression)simpleName.getParent();
											PrefixExpression.Operator operator = prefixExpression.getOperator();
											if(operator.equals(PrefixExpression.Operator.INCREMENT) || operator.equals(PrefixExpression.Operator.DECREMENT)) {
												isAssigned = true;
												typeCheckElimination.addAssignedLocalVariable(formalParameter);
											}
										}
										if(!isAssigned)
											typeCheckElimination.addAccessedLocalVariable(formalParameter);
										break;
									}
								}
							}
						}
					}
					leaf = leaf.getNextLeaf();
				}
			}
		}
	}

	private Expression extractOperand(Expression operand) {
		if(operand instanceof SimpleName) {
			SimpleName operandSimpleName = (SimpleName)operand;
			return operandSimpleName;
		}
		else if(operand instanceof QualifiedName) {
			QualifiedName operandQualifiedName = (QualifiedName)operand;
			return operandQualifiedName.getName();
		}
		else if(operand instanceof FieldAccess) {
			FieldAccess operandFieldAccess = (FieldAccess)operand;
			return operandFieldAccess.getName();
		}
		else if(operand instanceof MethodInvocation) {
			MethodInvocation methodInvocation = (MethodInvocation)operand;
			for(MethodDeclaration method : methods) {
				SimpleName fieldInstruction = MethodDeclarationUtility.isGetter(method);
				if(fieldInstruction != null && method.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
					return fieldInstruction;
				}
				MethodInvocation delegateMethodInvocation = MethodDeclarationUtility.isDelegate(method);
				if(delegateMethodInvocation != null && method.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
					return delegateMethodInvocation;
				}
			}
			return methodInvocation;
		}
		return null;
	}

	private boolean isValidTypeVariable(SimpleName typeVariable, Set<Expression> typeCheckExpressions) {
		int validTypeCheckExpressions = 0;
		int typeVariableCounter = 0;
		for(Expression complexExpression : complexExpressionMap.keySet()) {
			IfStatementExpressionAnalyzer analyzer = complexExpressionMap.get(complexExpression);
			for(SimpleName analyzerTypeVariable : analyzer.getTargetVariables()) {
				if(analyzerTypeVariable.resolveBinding().isEqualTo(typeVariable.resolveBinding())) {
					List<SimpleName> staticFields = analyzer.getTypeVariableStaticField(analyzerTypeVariable);
					if(staticFields != null && staticFields.size() == 1 && analyzer.allParentNodesAreConditionalAndOperators()) {
						validTypeCheckExpressions++;
						typeVariableCounter++;
					}
					if(staticFields != null && analyzer.getNumberOfConditionalOperatorNodes() == staticFields.size()-1 &&
							staticFields.size() > 1 && analyzer.allParentNodesAreConditionalOrOperators()) {
						validTypeCheckExpressions++;
						typeVariableCounter += staticFields.size();
					}
					List<Type> subclasses = analyzer.getTypeVariableSubclass(analyzerTypeVariable);
					if(subclasses != null && subclasses.size() == 1 && analyzer.allParentNodesAreConditionalAndOperators()) {
						validTypeCheckExpressions++;
						typeVariableCounter++;
					}
					if(subclasses != null && analyzer.getNumberOfConditionalOperatorNodes() == subclasses.size()-1 &&
							subclasses.size() > 1 && analyzer.allParentNodesAreConditionalOrOperators()) {
						validTypeCheckExpressions++;
						typeVariableCounter += subclasses.size();
					}
				}
			}
		}
		if(validTypeCheckExpressions == typeCheckExpressions.size() &&
				typeVariableCounter == typeVariableCounterMap.get(typeVariable))
			return true;
		else
			return false;
	}

	private boolean isValidTypeMethodInvocation(MethodInvocation typeMethodInvocation, Set<Expression> typeCheckExpressions) {
		int validTypeCheckExpressions = 0;
		int typeMethodInvocationCounter = 0;
		for(Expression complexExpression : complexExpressionMap.keySet()) {
			IfStatementExpressionAnalyzer analyzer = complexExpressionMap.get(complexExpression);
			for(MethodInvocation analyzerTypeMethodInvocation : analyzer.getTargetMethodInvocations()) {
				if(analyzerTypeMethodInvocation.resolveMethodBinding().isEqualTo(typeMethodInvocation.resolveMethodBinding())) {
					List<SimpleName> staticFields = analyzer.getTypeMethodInvocationStaticField(analyzerTypeMethodInvocation);
					if(staticFields != null && staticFields.size() == 1 && analyzer.allParentNodesAreConditionalAndOperators()) {
						validTypeCheckExpressions++;
						typeMethodInvocationCounter++;
					}
					if(staticFields != null && analyzer.getNumberOfConditionalOperatorNodes() == staticFields.size()-1 &&
							staticFields.size() > 1 && analyzer.allParentNodesAreConditionalOrOperators()) {
						validTypeCheckExpressions++;
						typeMethodInvocationCounter += staticFields.size();
					}
					List<Type> subclasses = analyzer.getTypeMethodInvocationSubclass(analyzerTypeMethodInvocation);
					if(subclasses != null && subclasses.size() == 1 && analyzer.allParentNodesAreConditionalAndOperators()) {
						validTypeCheckExpressions++;
						typeMethodInvocationCounter++;
					}
					if(subclasses != null && analyzer.getNumberOfConditionalOperatorNodes() == subclasses.size()-1 &&
							subclasses.size() > 1 && analyzer.allParentNodesAreConditionalOrOperators()) {
						validTypeCheckExpressions++;
						typeMethodInvocationCounter += subclasses.size();
					}
				}
			}
		}
		if(validTypeCheckExpressions == typeCheckExpressions.size() &&
				typeMethodInvocationCounter == typeMethodInvocationCounterMap.get(typeMethodInvocation))
			return true;
		else
			return false;
	}

	private SimpleName containsTypeVariableKey(SimpleName simpleName) {
		for(SimpleName keySimpleName : typeVariableCounterMap.keySet()) {
			if(keySimpleName.resolveBinding().isEqualTo(simpleName.resolveBinding()))
				return keySimpleName;
		}
		return null;
	}

	private MethodInvocation containsTypeMethodInvocationKey(MethodInvocation methodInvocation) {
		for(MethodInvocation keyMethodInvocation : typeMethodInvocationCounterMap.keySet()) {
			if(keyMethodInvocation.resolveMethodBinding().isEqualTo(methodInvocation.resolveMethodBinding()))
				return keyMethodInvocation;
		}
		return null;
	}
}
