package gr.uom.java.ast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import gr.uom.java.ast.inheritance.InheritanceTree;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.MethodDeclarationUtility;
import gr.uom.java.ast.util.StatementExtractor;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckElimination;

public class TypeCheckCodeFragmentAnalyzer {
	private TypeCheckElimination typeCheckElimination;
	private TypeDeclaration typeDeclaration;
	private MethodDeclaration typeCheckMethod;
	private List<InheritanceTree> inheritanceTreeList;
	private FieldDeclaration[] fields;
	private MethodDeclaration[] methods;
	private Map<VariableDeclarationFragment, Integer> fieldTypeCounterMap;
	private Map<MethodInvocation, Integer> typeMethodInvocationCounterMap;
	
	public TypeCheckCodeFragmentAnalyzer(TypeCheckElimination typeCheckElimination, TypeDeclaration typeDeclaration,
			MethodDeclaration typeCheckMethod, List<InheritanceTree> inheritanceTreeList) {
		this.typeCheckElimination = typeCheckElimination;
		this.typeDeclaration = typeDeclaration;
		this.typeCheckMethod = typeCheckMethod;
		this.fields = typeDeclaration.getFields();
		this.methods = typeDeclaration.getMethods();
		this.inheritanceTreeList = inheritanceTreeList;
		this.fieldTypeCounterMap = new LinkedHashMap<VariableDeclarationFragment, Integer>();
		this.typeMethodInvocationCounterMap = new LinkedHashMap<MethodInvocation, Integer>();
		typeCheckElimination.setTypeCheckMethod(typeCheckMethod);
		processTypeCheckCodeFragment();
	}
	
	private void processTypeCheckCodeFragment() {
		List<ArrayList<Statement>> typeCheckStatements = typeCheckElimination.getTypeCheckStatements();
		ArrayList<Statement> firstBlockOfStatements = typeCheckStatements.get(0);
		Statement firstStatementOfBlock = firstBlockOfStatements.get(0);
		if(firstStatementOfBlock.getParent() instanceof SwitchStatement) {
			SwitchStatement switchStatement = (SwitchStatement)firstStatementOfBlock.getParent();
			Expression switchStatementExpression = switchStatement.getExpression();
			SimpleName switchStatementExpressionName = null;
			if(switchStatementExpression instanceof SimpleName) {
				SimpleName simpleName = (SimpleName)switchStatementExpression;
				switchStatementExpressionName = simpleName;
			}
			else if(switchStatementExpression instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)switchStatementExpression;
				switchStatementExpressionName = fieldAccess.getName();
			}
			else if(switchStatementExpression instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)switchStatementExpression;
				for(MethodDeclaration method : methods) {
					SimpleName fieldInstruction = MethodDeclarationUtility.isGetter(method);
					if(fieldInstruction != null && method.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding()))
						switchStatementExpressionName = fieldInstruction;
				}
				if(switchStatementExpressionName == null) {
					IMethodBinding methodInvocationBinding = methodInvocation.resolveMethodBinding();
					if((methodInvocationBinding.getModifiers() & Modifier.ABSTRACT) != 0) {
						for(InheritanceTree tree : inheritanceTreeList) {
							DefaultMutableTreeNode root = tree.getRootNode();
							String rootClassName = (String)root.getUserObject();
							ITypeBinding declaringClassTypeBinding = methodInvocationBinding.getDeclaringClass();
							if(rootClassName.equals(declaringClassTypeBinding.getQualifiedName())) {
								typeCheckElimination.setTypeMethodInvocation(methodInvocation);
								typeCheckElimination.setExistingInheritanceTree(tree);
								break;
							}
						}
					}
				}
			}
			IBinding switchStatementExpressionNameBinding = switchStatementExpressionName.resolveBinding();
			IVariableBinding switchStatementExpressionNameVariableBinding = null;
			if(switchStatementExpressionNameBinding.getKind() == IBinding.VARIABLE)
				switchStatementExpressionNameVariableBinding = (IVariableBinding)switchStatementExpressionNameBinding;
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
		
		Set<Expression> typeCheckExpressions = typeCheckElimination.getTypeCheckExpressions();
		for(Expression typeCheckExpression : typeCheckExpressions) {
			if(typeCheckExpression.getParent() instanceof SwitchCase) {
				if(typeCheckExpression instanceof SimpleName) {
					SimpleName simpleName = (SimpleName)typeCheckExpression;
					IBinding binding = simpleName.resolveBinding();
					if(binding.getKind() == IBinding.VARIABLE) {
						IVariableBinding variableBinding = (IVariableBinding)binding;
						if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) != 0) {
							typeCheckElimination.addStaticType(typeCheckExpression, simpleName);
						}
					}
				}
				else if(typeCheckExpression instanceof QualifiedName) {
					QualifiedName qualifiedName = (QualifiedName)typeCheckExpression;
					IBinding binding = qualifiedName.resolveBinding();
					if(binding.getKind() == IBinding.VARIABLE) {
						IVariableBinding variableBinding = (IVariableBinding)binding;
						if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) != 0) {
							typeCheckElimination.addStaticType(typeCheckExpression, qualifiedName.getName());
						}
					}
				}
				else if(typeCheckExpression instanceof FieldAccess) {
					FieldAccess fieldAccess = (FieldAccess)typeCheckExpression;
					IVariableBinding variableBinding = fieldAccess.resolveFieldBinding();
					if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) != 0) {
						typeCheckElimination.addStaticType(typeCheckExpression, fieldAccess.getName());
					}
				}
			}
			else if(typeCheckExpression instanceof InfixExpression) {
				InfixExpression infixExpression = (InfixExpression)typeCheckExpression;
				IfStatementExpressionAnalyzer analyzer = new IfStatementExpressionAnalyzer(infixExpression);
				for(InfixExpression leafInfixExpression : analyzer.getInfixExpressionsWithEqualsOperator()) {
					analyzer.getRemainingExpression(leafInfixExpression);
					Expression leftOperand = leafInfixExpression.getLeftOperand();
					Expression rightOperand = leafInfixExpression.getRightOperand();
					infixExpressionHandler(leftOperand, leafInfixExpression, analyzer);
					infixExpressionHandler(rightOperand, leafInfixExpression, analyzer);
				}
			}
		}
		for(VariableDeclarationFragment fragment : fieldTypeCounterMap.keySet()) {
			if(fieldTypeCounterMap.get(fragment) == typeCheckExpressions.size()) {
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
			}
		}
		for(MethodInvocation methodInvocation : typeMethodInvocationCounterMap.keySet()) {
			if(typeMethodInvocationCounterMap.get(methodInvocation) == typeCheckExpressions.size()) {
				typeCheckElimination.setTypeMethodInvocation(methodInvocation);
				IMethodBinding methodInvocationBinding = methodInvocation.resolveMethodBinding();
				for(InheritanceTree tree : inheritanceTreeList) {
					DefaultMutableTreeNode root = tree.getRootNode();
					String rootClassName = (String)root.getUserObject();
					ITypeBinding declaringClassTypeBinding = methodInvocationBinding.getDeclaringClass();
					if(rootClassName.equals(declaringClassTypeBinding.getQualifiedName())) {
						typeCheckElimination.setExistingInheritanceTree(tree);
						break;
					}
				}
			}
		}
	}
	
	public void processTypeCheckCodeFragmentBranches() {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<ArrayList<Statement>> allTypeCheckStatements = typeCheckElimination.getTypeCheckStatements();
		if(!typeCheckElimination.getDefaultCaseStatements().isEmpty()) {
			allTypeCheckStatements.add(typeCheckElimination.getDefaultCaseStatements());
		}
		StatementExtractor statementExtractor = new StatementExtractor();
		List<Statement> variableDeclarationStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment = statementExtractor.getVariableDeclarations(typeCheckMethod.getBody());
		for(ArrayList<Statement> typeCheckStatementList : allTypeCheckStatements) {
			for(Statement statement : typeCheckStatementList) {
				variableDeclarationStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment.removeAll(statementExtractor.getVariableDeclarations(statement));
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
					}
				}
				//checking for Source class fields or parameters of the type-checking method accessed inside the type-checking branches
				List<Expression> variableInstructions = expressionExtractor.getVariableInstructions(statement);
				for(Expression variableInstruction : variableInstructions) {
					SimpleName simpleName = (SimpleName)variableInstruction;
					IBinding variableInstructionBinding = simpleName.resolveBinding();
					if(variableInstructionBinding.getKind() == IBinding.VARIABLE) {
						IVariableBinding variableInstructionVariableBinding = (IVariableBinding)variableInstructionBinding;
						if(variableInstructionVariableBinding.isField()) {
							if(variableInstructionVariableBinding.getDeclaringClass() != null && variableInstructionVariableBinding.getDeclaringClass().isEqualTo(typeDeclaration.resolveBinding())) {
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
											boolean isLeftHandOfAssignment = false;
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
													isLeftHandOfAssignment = true;
													typeCheckElimination.addAssignedField(fragment);
												}
											}
											if(!isLeftHandOfAssignment)
												typeCheckElimination.addAccessedField(fragment);
										}
									}
								}
							}
						}
						else if(variableInstructionVariableBinding.isParameter()) {
							List<SingleVariableDeclaration> parameters = typeCheckMethod.parameters();
							for(SingleVariableDeclaration parameter : parameters) {
								IVariableBinding parameterVariableBinding = parameter.resolveBinding();
								if(parameterVariableBinding.isEqualTo(variableInstructionVariableBinding))
									typeCheckElimination.addAccessedParameter(parameter);
							}
						}
						//checking for local variables accessed inside the type-checking code branches, but declared outside them
						else {
							for(Statement vDStatement : variableDeclarationStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment) {
								VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)vDStatement;
								List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
								for(VariableDeclarationFragment fragment : fragments) {
									IVariableBinding fragmentVariableBinding = fragment.resolveBinding();
									if(fragmentVariableBinding.isEqualTo(variableInstructionVariableBinding)) {
										typeCheckElimination.addAccessedLocalVariable(fragment);
										break;
									}
								}    											
							}
						}
					}
				}
			}
		}
	}
	
	private void infixExpressionHandler(Expression operand, Expression typeCheckExpression, IfStatementExpressionAnalyzer analyzer) {
		SimpleName leftOperandName = null;
		if(operand instanceof SimpleName) {
			SimpleName leftOperandSimpleName = (SimpleName)operand;
			leftOperandName = leftOperandSimpleName;
		}
		else if(operand instanceof QualifiedName) {
			QualifiedName leftOperandQualifiedName = (QualifiedName)operand;
			leftOperandName = leftOperandQualifiedName.getName();
		}
		else if(operand instanceof FieldAccess) {
			FieldAccess leftOperandFieldAccess = (FieldAccess)operand;
			leftOperandName = leftOperandFieldAccess.getName();
		}
		else if(operand instanceof MethodInvocation) {
			MethodInvocation methodInvocation = (MethodInvocation)operand;
			for(MethodDeclaration method : methods) {
				SimpleName fieldInstruction = MethodDeclarationUtility.isGetter(method);
				if(fieldInstruction != null && method.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
					leftOperandName = fieldInstruction;
					break;
				}
				MethodInvocation delegateMethodInvocation = MethodDeclarationUtility.isDelegate(method);
				if(delegateMethodInvocation != null && method.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
					methodInvocation = delegateMethodInvocation;
					break;
				}
			}
			if(leftOperandName == null) {
				IMethodBinding methodInvocationBinding = methodInvocation.resolveMethodBinding();
				for(InheritanceTree tree : inheritanceTreeList) {
					DefaultMutableTreeNode root = tree.getRootNode();
					String rootClassName = (String)root.getUserObject();
					ITypeBinding declaringClassTypeBinding = methodInvocationBinding.getDeclaringClass();
					if(rootClassName.equals(declaringClassTypeBinding.getQualifiedName())) {
						boolean found = false;
						for(MethodInvocation key : typeMethodInvocationCounterMap.keySet()) {
							if(key.toString().equals(methodInvocation.toString())) {
								typeMethodInvocationCounterMap.put(key, typeMethodInvocationCounterMap.get(key)+1);
								found = true;
							}
						}
						if(!found)
							typeMethodInvocationCounterMap.put(methodInvocation, 1);
						break;
					}
				}
			}
		}
		if(leftOperandName != null) {
			IBinding leftOperandNameBinding = leftOperandName.resolveBinding();
			if(leftOperandNameBinding.getKind() == IBinding.VARIABLE) {
				IVariableBinding leftOperandNameVariableBinding = (IVariableBinding)leftOperandNameBinding;
				if(leftOperandNameVariableBinding.isField() && (leftOperandNameVariableBinding.getModifiers() & Modifier.STATIC) != 0) {
					typeCheckElimination.addStaticType(analyzer.getCompleteExpression(), leftOperandName);
					typeCheckElimination.addRemainingIfStatementExpression(analyzer.getCompleteExpression(), analyzer.getRemainingExpression(typeCheckExpression));
				}
				else {
					for(FieldDeclaration field : fields) {
						List<VariableDeclarationFragment> fragments = field.fragments();
						for(VariableDeclarationFragment fragment : fragments) {
							IVariableBinding fragmentVariableBinding = fragment.resolveBinding();
							if(fragmentVariableBinding.isEqualTo(leftOperandNameVariableBinding)) {
								if(fieldTypeCounterMap.containsKey(fragment)) {
									fieldTypeCounterMap.put(fragment, fieldTypeCounterMap.get(fragment)+1);
								}
								else {
									fieldTypeCounterMap.put(fragment, 1);
								}
							}
						}
					}
				}
			}
		}
	}
}
