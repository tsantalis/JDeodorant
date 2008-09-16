package gr.uom.java.ast.decomposition.cfg;

import java.util.List;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;

import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.CompositeStatementObject;

public class PDGControlPredicateNode extends PDGNode {

	public PDGControlPredicateNode(CFGNode cfgNode) {
		super(cfgNode);
		determineDefinedAndUsedVariables();
	}

	private void determineDefinedAndUsedVariables() {
		CFGNode cfgNode = getCFGNode();
		if(cfgNode.getStatement() instanceof CompositeStatementObject) {
			CompositeStatementObject composite = (CompositeStatementObject)cfgNode.getStatement();
			List<AbstractExpression> expressions = composite.getExpressions();
			for(AbstractExpression expression : expressions) {
				List<LocalVariableDeclarationObject> variableDeclarations = expression.getLocalVariableDeclarations();
				for(LocalVariableDeclarationObject variableDeclaration : variableDeclarations)
					definedVariables.add(variableDeclaration.generateLocalVariableInstruction());
				List<LocalVariableInstructionObject> variableInstructions = expression.getLocalVariableInstructions();
				for(LocalVariableInstructionObject variableInstruction : variableInstructions) {
					Assignment assignment = null;
					PostfixExpression postfixExpression = null;
					PrefixExpression prefixExpression = null;
					if((assignment = expression.containsLocalVariableAssignment(variableInstruction)) != null) {
						definedVariables.add(variableInstruction);
						Assignment.Operator operator = assignment.getOperator();
						if(!operator.equals(Assignment.Operator.ASSIGN))
							usedVariables.add(variableInstruction);
					}
					else if((postfixExpression = expression.containsLocalVariablePostfixAssignment(variableInstruction)) != null) {
						definedVariables.add(variableInstruction);
						usedVariables.add(variableInstruction);
					}
					else if((prefixExpression = expression.containsLocalVariablePrefixAssignment(variableInstruction)) != null) {
						definedVariables.add(variableInstruction);
						usedVariables.add(variableInstruction);
					}
					else
						usedVariables.add(variableInstruction);
				}
			}
		}
	}
}
