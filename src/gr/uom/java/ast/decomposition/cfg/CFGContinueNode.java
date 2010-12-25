package gr.uom.java.ast.decomposition.cfg;

import org.eclipse.jdt.core.dom.ContinueStatement;

import gr.uom.java.ast.decomposition.AbstractStatement;

public class CFGContinueNode extends CFGNode {
	private boolean isLabeled;
	
	public CFGContinueNode(AbstractStatement statement) {
		super(statement);
		ContinueStatement continueStatement = (ContinueStatement)statement.getStatement();
		if(continueStatement.getLabel() != null)
			isLabeled = true;
		else
			isLabeled = false;
	}

	public boolean isLabeled() {
		return isLabeled;
	}
}
