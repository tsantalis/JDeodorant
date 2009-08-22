package gr.uom.java.ast.decomposition.cfg;

import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;

import gr.uom.java.ast.decomposition.AbstractStatement;

public class CFGExitNode extends CFGNode {
	private SimpleName returnedVariable;
	
	public CFGExitNode(AbstractStatement statement) {
		super(statement);
		ReturnStatement returnStatement = (ReturnStatement)statement.getStatement();
		if(returnStatement.getExpression() != null) {
			if(returnStatement.getExpression() instanceof SimpleName)
				returnedVariable = (SimpleName)returnStatement.getExpression();
		}
	}

	public SimpleName getReturnedVariable() {
		return returnedVariable;
	}
}
