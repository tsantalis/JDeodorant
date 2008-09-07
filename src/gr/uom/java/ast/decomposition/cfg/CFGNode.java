package gr.uom.java.ast.decomposition.cfg;

import org.eclipse.jdt.core.dom.Statement;

import gr.uom.java.ast.decomposition.AbstractStatement;

public class CFGNode extends GraphNode {
	private AbstractStatement statement;
	private BasicBlock basicBlock;

	public CFGNode(AbstractStatement statement) {
		super();
		this.statement = statement;
	}

	public AbstractStatement getStatement() {
		return statement;
	}

	public Statement getASTStatement() {
		return statement.getStatement();
	}

	public boolean isLeader() {
		if(isFirst() || isJoin() || immediatelyFollowsBranchNode())
			return true;
		return false;
	}

	public boolean immediatelyFollowsBranchNode() {
		for(GraphEdge edge : incomingEdges) {
			CFGNode srcNode = (CFGNode)edge.src;
			if(srcNode.isBranch())
				return true;
		}
		return false;
	}

	public boolean isFirst() {
		if(incomingEdges.size() == 0)
			return true;
		return false;
	}

	public boolean isBranch() {
		if(outgoingEdges.size() > 1)
			return true;
		return false;
	}

	public boolean isJoin() {
		if(incomingEdges.size() > 1)
			return true;
		return false;
	}

	public void setBasicBlock(BasicBlock basicBlock) {
		this.basicBlock = basicBlock;
	}

	public BasicBlock getBasicBlock() {
		return basicBlock;
	}

	public boolean equals(Object o) {
		if(this == o)
    		return true;
    	
    	if(o instanceof CFGNode) {
    		CFGNode node = (CFGNode)o;
    		return this.statement.equals(node.statement);
    	}
    	return false;
	}

	public int hashCode() {
		return statement.hashCode();
	}

	public String toString() {
		return id + "\t" + statement.toString();
	}
}
