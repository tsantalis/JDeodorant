package gr.uom.java.ast.decomposition.cfg;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.ast.decomposition.StatementObject;

public class PDGStatementNode extends PDGNode {
	
	public PDGStatementNode(CFGNode cfgNode, Set<VariableDeclaration> variableDeclarationsInMethod) {
		super(cfgNode);
		determineDefinedAndUsedVariables(variableDeclarationsInMethod);
	}

	private void determineDefinedAndUsedVariables(Set<VariableDeclaration> variableDeclarationsInMethod) {
		CFGNode cfgNode = getCFGNode();
		if(cfgNode.getStatement() instanceof StatementObject) {
			StatementObject statement = (StatementObject)cfgNode.getStatement();
			List<LocalVariableDeclarationObject> variableDeclarations = statement.getLocalVariableDeclarations();
			for(LocalVariableDeclarationObject variableDeclaration : variableDeclarations) {
				declaredVariables.add(variableDeclaration.getVariableDeclaration());
				definedVariables.add(variableDeclaration.getVariableDeclaration());
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
				List<Assignment> assignments = statement.getLocalVariableAssignments(variableInstruction);
				List<PostfixExpression> postfixExpressions = statement.getLocalVariablePostfixAssignments(variableInstruction);
				List<PrefixExpression> prefixExpressions = statement.getLocalVariablePrefixAssignments(variableInstruction);
				if(!assignments.isEmpty()) {
					definedVariables.add(variableDeclaration);
					for(Assignment assignment : assignments) {
						Assignment.Operator operator = assignment.getOperator();
						if(!operator.equals(Assignment.Operator.ASSIGN))
							usedVariables.add(variableDeclaration);
					}
				}
				else if(!postfixExpressions.isEmpty()) {
					definedVariables.add(variableDeclaration);
					usedVariables.add(variableDeclaration);
				}
				else if(!prefixExpressions.isEmpty()) {
					definedVariables.add(variableDeclaration);
					usedVariables.add(variableDeclaration);
				}
				else {
					SimpleName variableInstructionName = variableInstruction.getSimpleName();
					if(variableInstructionName.getParent() instanceof MethodInvocation) {
						MethodInvocation methodInvocation = (MethodInvocation)variableInstructionName.getParent();
						if(methodInvocation.getExpression() != null && methodInvocation.getExpression().equals(variableInstructionName)) {
							TypeObject type = variableInstruction.getType();
							String methodInvocationName = methodInvocation.getName().getIdentifier();
							if(type.getClassType().equals("java.util.Iterator") && methodInvocationName.equals("next"))
								stateChangingMethodInvocationMap.put(variableDeclaration, methodInvocation);
							else if(type.getClassType().equals("java.util.Enumeration") && methodInvocationName.equals("nextElement"))
								stateChangingMethodInvocationMap.put(variableDeclaration, methodInvocation);
							else if(type.getClassType().equals("java.util.ListIterator") &&
									(methodInvocationName.equals("next") || methodInvocationName.equals("previous")))
								stateChangingMethodInvocationMap.put(variableDeclaration, methodInvocation);
						}
					}
					usedVariables.add(variableDeclaration);
				}
			}
		}
	}
}
