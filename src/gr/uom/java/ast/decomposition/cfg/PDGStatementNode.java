package gr.uom.java.ast.decomposition.cfg;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.ParameterObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.decomposition.StatementObject;

public class PDGStatementNode extends PDGNode {
	
	public PDGStatementNode(CFGNode cfgNode, Set<VariableDeclaration> variableDeclarationsInMethod) {
		super(cfgNode, variableDeclarationsInMethod);
		determineDefinedAndUsedVariables();
	}

	private void determineDefinedAndUsedVariables() {
		CFGNode cfgNode = getCFGNode();
		if(cfgNode.getStatement() instanceof StatementObject) {
			StatementObject statement = (StatementObject)cfgNode.getStatement();
			List<LocalVariableDeclarationObject> variableDeclarations = statement.getLocalVariableDeclarations();
			for(LocalVariableDeclarationObject variableDeclaration : variableDeclarations) {
				PlainVariable variable = new PlainVariable(variableDeclaration.getVariableDeclaration());
				declaredVariables.add(variable);
				definedVariables.add(variable);
			}
			List<LocalVariableInstructionObject> variableInstructions = statement.getLocalVariableInstructions();
			for(LocalVariableInstructionObject variableInstruction : variableInstructions) {
				VariableDeclaration variableDeclaration = null;
				for(VariableDeclaration declaration : variableDeclarationsInMethod) {
					if(declaration.resolveBinding().isEqualTo(variableInstruction.getSimpleName().resolveBinding())) {
						variableDeclaration = declaration;
						break;
					}
				}
				if(variableDeclaration != null) {
					PlainVariable variable = new PlainVariable(variableDeclaration);
					List<Assignment> assignments = statement.getLocalVariableAssignments(variableInstruction);
					List<PostfixExpression> postfixExpressions = statement.getLocalVariablePostfixAssignments(variableInstruction);
					List<PrefixExpression> prefixExpressions = statement.getLocalVariablePrefixAssignments(variableInstruction);
					if(!assignments.isEmpty()) {
						definedVariables.add(variable);
						for(Assignment assignment : assignments) {
							Assignment.Operator operator = assignment.getOperator();
							if(!operator.equals(Assignment.Operator.ASSIGN))
								usedVariables.add(variable);
						}
					}
					else if(!postfixExpressions.isEmpty()) {
						definedVariables.add(variable);
						usedVariables.add(variable);
					}
					else if(!prefixExpressions.isEmpty()) {
						definedVariables.add(variable);
						usedVariables.add(variable);
					}
					else {
						SimpleName variableInstructionName = variableInstruction.getSimpleName();
						if(variableInstructionName.getParent() instanceof MethodInvocation) {
							MethodInvocation methodInvocation = (MethodInvocation)variableInstructionName.getParent();
							if(methodInvocation.getExpression() != null && methodInvocation.getExpression().equals(variableInstructionName)) {
								List<MethodInvocationObject> methodInvocations = statement.getMethodInvocations();
								MethodInvocationObject methodInvocationObject = null;
								for(MethodInvocationObject mio : methodInvocations) {
									if(mio.getMethodInvocation().equals(methodInvocation)) {
										methodInvocationObject = mio;
										break;
									}
								}
								SystemObject systemObject = ASTReader.getSystemObject();
								ClassObject classObject = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
								if(classObject != null) {
									MethodObject methodObject = classObject.getMethod(methodInvocationObject);
									if(methodObject != null) {
										processInternalMethodInvocation(classObject, methodObject, methodInvocation, variable,
												new LinkedHashSet<MethodInvocation>());
										List<Expression> arguments = methodInvocation.arguments();
										int argumentPosition = 0;
										for(Expression argument : arguments) {
											if(argument instanceof SimpleName) {
												SimpleName argumentName = (SimpleName)argument;
												VariableDeclaration argumentDeclaration = null;
												for(VariableDeclaration variableDeclaration2 : variableDeclarationsInMethod) {
													if(variableDeclaration2.resolveBinding().isEqualTo(argumentName.resolveBinding())) {
														argumentDeclaration = variableDeclaration2;
														break;
													}
												}
												if(argumentDeclaration != null) {
													ParameterObject parameter = methodObject.getParameter(argumentPosition);
													VariableDeclaration parameterDeclaration = parameter.getSingleVariableDeclaration();
													ClassObject classObject2 = systemObject.getClassObject(parameter.getType().getClassType());
													if(classObject2 != null) {
														PlainVariable argumentVariable = new PlainVariable(argumentDeclaration);
														processArgumentOfInternalMethodInvocation(methodObject, methodInvocation,
																argumentVariable, parameterDeclaration, new LinkedHashSet<MethodInvocation>());
													}
												}
											}
											argumentPosition++;
										}
									}
								}
								else {
									processExternalMethodInvocation(methodInvocation, variable);
								}
							}
						}
						usedVariables.add(variable);
					}
				}
			}
			List<FieldInstructionObject> fieldInstructions = statement.getFieldInstructions();
			for(FieldInstructionObject fieldInstruction : fieldInstructions) {
				SimpleName fieldInstructionName = fieldInstruction.getSimpleName();
				AbstractVariable field = processFieldInstruction(fieldInstructionName, null, null);
				if(field != null) {
					List<Assignment> fieldAssignments = statement.getFieldAssignments(fieldInstruction);
					List<PostfixExpression> fieldPostfixAssignments = statement.getFieldPostfixAssignments(fieldInstruction);
					List<PrefixExpression> fieldPrefixAssignments = statement.getFieldPrefixAssignments(fieldInstruction);
					if(!fieldAssignments.isEmpty()) {
						definedVariables.add(field);
						for(Assignment assignment : fieldAssignments) {
							Assignment.Operator operator = assignment.getOperator();
							if(!operator.equals(Assignment.Operator.ASSIGN))
								usedVariables.add(field);
						}
						if(field instanceof CompositeVariable) {
							definedVariables.add(((CompositeVariable)field).getLeftPart());
						}
					}
					else if(!fieldPostfixAssignments.isEmpty()) {
						definedVariables.add(field);
						usedVariables.add(field);
						if(field instanceof CompositeVariable) {
							definedVariables.add(((CompositeVariable)field).getLeftPart());
						}
					}
					else if(!fieldPrefixAssignments.isEmpty()) {
						definedVariables.add(field);
						usedVariables.add(field);
						if(field instanceof CompositeVariable) {
							definedVariables.add(((CompositeVariable)field).getLeftPart());
						}
					}
					else {
						List<MethodInvocationObject> methodInvocations = statement.getMethodInvocations();
						for(MethodInvocationObject methodInvocationObject : methodInvocations) {
							MethodInvocation methodInvocation = methodInvocationObject.getMethodInvocation();
							Expression methodInvocationExpression = methodInvocation.getExpression();
							AbstractVariable variable = processMethodInvocationExpression(methodInvocationExpression, null);
							if(variable != null && variable.equals(field)) {
								SystemObject systemObject = ASTReader.getSystemObject();
								ClassObject classObject = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
								if(classObject != null) {
									MethodObject methodObject = classObject.getMethod(methodInvocationObject);
									if(methodObject != null) {
										processInternalMethodInvocation(classObject, methodObject, methodInvocation, field,
												new LinkedHashSet<MethodInvocation>());
										List<Expression> arguments = methodInvocation.arguments();
										int argumentPosition = 0;
										for(Expression argument : arguments) {
											if(argument instanceof SimpleName) {
												SimpleName argumentName = (SimpleName)argument;
												VariableDeclaration argumentDeclaration = null;
												for(VariableDeclaration variableDeclaration : variableDeclarationsInMethod) {
													if(variableDeclaration.resolveBinding().isEqualTo(argumentName.resolveBinding())) {
														argumentDeclaration = variableDeclaration;
														break;
													}
												}
												if(argumentDeclaration != null) {
													ParameterObject parameter = methodObject.getParameter(argumentPosition);
													VariableDeclaration parameterDeclaration = parameter.getSingleVariableDeclaration();
													ClassObject classObject2 = systemObject.getClassObject(parameter.getType().getClassType());
													if(classObject2 != null) {
														PlainVariable argumentVariable = new PlainVariable(argumentDeclaration);
														processArgumentOfInternalMethodInvocation(methodObject, methodInvocation,
																argumentVariable, parameterDeclaration, new LinkedHashSet<MethodInvocation>());
													}
												}
											}
											argumentPosition++;
										}
									}
								}
								else {
									processExternalMethodInvocation(methodInvocation, field);
								}
							}
						}
						usedVariables.add(field);
					}
				}
			}
			List<MethodInvocationObject> methodInvocations = statement.getMethodInvocations();
			for(MethodInvocationObject methodInvocationObject : methodInvocations) {
				MethodInvocation methodInvocation = methodInvocationObject.getMethodInvocation();
				if(methodInvocation.getExpression() == null || methodInvocation.getExpression() instanceof ThisExpression) {
					SystemObject systemObject = ASTReader.getSystemObject();
					ClassObject classObject = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
					if(classObject != null) {
						MethodObject methodObject = classObject.getMethod(methodInvocationObject);
						if(methodObject != null) {
							processInternalMethodInvocation(classObject, methodObject, methodInvocation, null,
									new LinkedHashSet<MethodInvocation>());
							List<Expression> arguments = methodInvocation.arguments();
							int argumentPosition = 0;
							for(Expression argument : arguments) {
								if(argument instanceof SimpleName) {
									SimpleName argumentName = (SimpleName)argument;
									VariableDeclaration argumentDeclaration = null;
									for(VariableDeclaration variableDeclaration : variableDeclarationsInMethod) {
										if(variableDeclaration.resolveBinding().isEqualTo(argumentName.resolveBinding())) {
											argumentDeclaration = variableDeclaration;
											break;
										}
									}
									if(argumentDeclaration != null) {
										ParameterObject parameter = methodObject.getParameter(argumentPosition);
										VariableDeclaration parameterDeclaration = parameter.getSingleVariableDeclaration();
										ClassObject classObject2 = systemObject.getClassObject(parameter.getType().getClassType());
										if(classObject2 != null) {
											PlainVariable argumentVariable = new PlainVariable(argumentDeclaration);
											processArgumentOfInternalMethodInvocation(methodObject, methodInvocation,
													argumentVariable, parameterDeclaration, new LinkedHashSet<MethodInvocation>());
										}
									}
								}
								argumentPosition++;
							}
						}
					}
				}
			}
		}
	}
}
