package gr.uom.java.ast.decomposition.cfg;

import org.eclipse.jdt.core.dom.BreakStatement;

import gr.uom.java.ast.decomposition.AbstractStatement;

public class CFGBreakNode extends CFGNode {
	private String label;
	private CFGNode innerMostLoopNode;
	
	public CFGBreakNode(AbstractStatement statement) {
		super(statement);
		BreakStatement breakStatement = (BreakStatement)statement.getStatement();
		if(breakStatement.getLabel() != null)
			label = breakStatement.getLabel().getIdentifier();
	}

	public String getLabel() {
		return label;
	}

	public boolean isLabeled() {
		return label != null;
	}

	public CFGNode getInnerMostLoopNode() {
		return innerMostLoopNode;
	}

	public void setInnerMostLoopNode(CFGNode innerMostLoopNode) {
		this.innerMostLoopNode = innerMostLoopNode;
	}
}
