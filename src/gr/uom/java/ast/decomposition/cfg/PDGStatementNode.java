package gr.uom.java.ast.decomposition.cfg;

import java.util.List;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;

import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.decomposition.StatementObject;

public class PDGStatementNode extends PDGNode {

	public PDGStatementNode(CFGNode cfgNode) {
		super(cfgNode);
		determineDefinedAndUsedVariables();
	}

	private void determineDefinedAndUsedVariables() {
		CFGNode cfgNode = getCFGNode();
		if(cfgNode.getStatement() instanceof StatementObject) {
			StatementObject statement = (StatementObject)cfgNode.getStatement();
			List<LocalVariableDeclarationObject> variableDeclarations = statement.getLocalVariableDeclarations();
			for(LocalVariableDeclarationObject variableDeclaration : variableDeclarations)
				definedVariables.add(variableDeclaration.generateLocalVariableInstruction());
			List<LocalVariableInstructionObject> variableInstructions = statement.getLocalVariableInstructions();
			for(LocalVariableInstructionObject variableInstruction : variableInstructions) {
				Assignment assignment = null;
				PostfixExpression postfixExpression = null;
				PrefixExpression prefixExpression = null;
				if((assignment = statement.containsLocalVariableAssignment(variableInstruction)) != null) {
					definedVariables.add(variableInstruction);
					Assignment.Operator operator = assignment.getOperator();
					if(!operator.equals(Assignment.Operator.ASSIGN))
						usedVariables.add(variableInstruction);
				}
				else if((postfixExpression = statement.containsLocalVariablePostfixAssignment(variableInstruction)) != null) {
					definedVariables.add(variableInstruction);
					usedVariables.add(variableInstruction);
				}
				else if((prefixExpression = statement.containsLocalVariablePrefixAssignment(variableInstruction)) != null) {
					definedVariables.add(variableInstruction);
					usedVariables.add(variableInstruction);
				}
				else
					usedVariables.add(variableInstruction);
			}
		}
	}
}
