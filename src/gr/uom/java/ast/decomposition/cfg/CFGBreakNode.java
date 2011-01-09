package gr.uom.java.ast.decomposition.cfg;

import org.eclipse.jdt.core.dom.BreakStatement;

import gr.uom.java.ast.decomposition.AbstractStatement;

public class CFGBreakNode extends CFGNode {
	private boolean isLabeled;
	private CFGNode innerMostLoopNode;
	
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

	public CFGNode getInnerMostLoopNode() {
		return innerMostLoopNode;
	}

	public void setInnerMostLoopNode(CFGNode innerMostLoopNode) {
		this.innerMostLoopNode = innerMostLoopNode;
	}
}
