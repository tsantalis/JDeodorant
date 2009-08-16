package gr.uom.java.ast.decomposition.cfg;

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

import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.VariableDeclarationObject;
import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.CompositeStatementObject;

public class PDGControlPredicateNode extends PDGNode {
	
	public PDGControlPredicateNode(CFGNode cfgNode, Set<VariableDeclarationObject> variableDeclarationsInMethod) {
		super(cfgNode, variableDeclarationsInMethod);
		determineDefinedAndUsedVariables();
	}

	private void determineDefinedAndUsedVariables() {
		CFGNode cfgNode = getCFGNode();
		if(cfgNode.getStatement() instanceof CompositeStatementObject) {
			CompositeStatementObject composite = (CompositeStatementObject)cfgNode.getStatement();
			List<AbstractExpression> expressions = composite.getExpressions();
			for(AbstractExpression expression : expressions) {
				List<LocalVariableDeclarationObject> variableDeclarations = expression.getLocalVariableDeclarations();
				for(LocalVariableDeclarationObject variableDeclaration : variableDeclarations) {
					PlainVariable variable = new PlainVariable(variableDeclaration);
					declaredVariables.add(variable);
					definedVariables.add(variable);
				}
				List<LocalVariableInstructionObject> variableInstructions = expression.getLocalVariableInstructions();
				for(LocalVariableInstructionObject variableInstruction : variableInstructions) {
					VariableDeclarationObject variableDeclarationObject = null;
					for(VariableDeclarationObject declarationObject : variableDeclarationsInMethod) {
						VariableDeclaration declaration = declarationObject.getVariableDeclaration();
						if(declaration.resolveBinding().isEqualTo(variableInstruction.getSimpleName().resolveBinding())) {
							variableDeclarationObject = declarationObject;
							break;
						}
					}
					if(variableDeclarationObject != null) {
						PlainVariable variable = new PlainVariable(variableDeclarationObject);
						List<Assignment> assignments = expression.getLocalVariableAssignments(variableInstruction);
						List<PostfixExpression> postfixExpressions = expression.getLocalVariablePostfixAssignments(variableInstruction);
						List<PrefixExpression> prefixExpressions = expression.getLocalVariablePrefixAssignments(variableInstruction);
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
									List<MethodInvocationObject> methodInvocations = expression.getMethodInvocations();
									MethodInvocationObject methodInvocationObject = null;
									for(MethodInvocationObject mio : methodInvocations) {
										if(mio.getMethodInvocation().equals(methodInvocation)) {
											methodInvocationObject = mio;
											break;
										}
									}
									processArgumentsOfInternalMethodInvocation(methodInvocationObject, methodInvocation, variable);
								}
							}
							usedVariables.add(variable);
						}
					}
				}
				List<FieldInstructionObject> fieldInstructions = expression.getFieldInstructions();
				for(FieldInstructionObject fieldInstruction : fieldInstructions) {
					SimpleName fieldInstructionName = fieldInstruction.getSimpleName();
					AbstractVariable field = processFieldInstruction(fieldInstructionName, null, null);
					if(field != null) {
						List<Assignment> fieldAssignments = expression.getFieldAssignments(fieldInstruction);
						List<PostfixExpression> fieldPostfixAssignments = expression.getFieldPostfixAssignments(fieldInstruction);
						List<PrefixExpression> fieldPrefixAssignments = expression.getFieldPrefixAssignments(fieldInstruction);
						if(!fieldAssignments.isEmpty()) {
							definedVariables.add(field);
							for(Assignment assignment : fieldAssignments) {
								Assignment.Operator operator = assignment.getOperator();
								if(!operator.equals(Assignment.Operator.ASSIGN))
									usedVariables.add(field);
							}
							if(field instanceof CompositeVariable) {
								putInStateChangingFieldModificationMap(((CompositeVariable)field).getLeftPart(), field);
							}
						}
						else if(!fieldPostfixAssignments.isEmpty()) {
							definedVariables.add(field);
							usedVariables.add(field);
							if(field instanceof CompositeVariable) {
								putInStateChangingFieldModificationMap(((CompositeVariable)field).getLeftPart(), field);
							}
						}
						else if(!fieldPrefixAssignments.isEmpty()) {
							definedVariables.add(field);
							usedVariables.add(field);
							if(field instanceof CompositeVariable) {
								putInStateChangingFieldModificationMap(((CompositeVariable)field).getLeftPart(), field);
							}
						}
						else {
							List<MethodInvocationObject> methodInvocations = expression.getMethodInvocations();
							for(MethodInvocationObject methodInvocationObject : methodInvocations) {
								MethodInvocation methodInvocation = methodInvocationObject.getMethodInvocation();
								Expression methodInvocationExpression = methodInvocation.getExpression();
								AbstractVariable variable = processMethodInvocationExpression(methodInvocationExpression, null);
								if(variable != null && variable.equals(field)) {
									processArgumentsOfInternalMethodInvocation(methodInvocationObject, methodInvocation, field);
								}
							}
							usedVariables.add(field);
						}
					}
				}
				List<MethodInvocationObject> methodInvocations = expression.getMethodInvocations();
				for(MethodInvocationObject methodInvocationObject : methodInvocations) {
					MethodInvocation methodInvocation = methodInvocationObject.getMethodInvocation();
					if(methodInvocation.getExpression() == null || methodInvocation.getExpression() instanceof ThisExpression) {
						processArgumentsOfInternalMethodInvocation(methodInvocationObject, methodInvocation, null);
					}
				}
			}
		}
	}
}
