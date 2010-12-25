package gr.uom.java.ast.decomposition.cfg;

import org.eclipse.jdt.core.dom.BreakStatement;

import gr.uom.java.ast.decomposition.AbstractStatement;

public class CFGBreakNode extends CFGNode {
	private boolean isLabeled;
	
	public CFGBreakNode(AbstractStatement statement) {
		super(statement);
		BreakStatement breakStatement = (BreakStatement)statement.getStatement();
		if(breakStatement.getLabel() != null)
			isLabeled = true;
		else
			isLabeled = false;
	}

	public boolean isLabeled() {
		return isLabeled;
	}
}
