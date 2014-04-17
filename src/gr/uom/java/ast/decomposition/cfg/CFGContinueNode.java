package gr.uom.java.ast.decomposition.cfg;

import org.eclipse.jdt.core.dom.ContinueStatement;

import gr.uom.java.ast.decomposition.AbstractStatement;

public class CFGContinueNode extends CFGNode {
	private String label;
	private CFGNode innerMostLoopNode;
	
	public CFGContinueNode(AbstractStatement statement) {
		super(statement);
		ContinueStatement continueStatement = (ContinueStatement)statement.getStatement();
		if(continueStatement.getLabel() != null)
			label = continueStatement.getLabel().getIdentifier();
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
