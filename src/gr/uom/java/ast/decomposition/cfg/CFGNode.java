package gr.uom.java.ast.decomposition.cfg;

import org.eclipse.jdt.core.dom.Statement;

import gr.uom.java.ast.decomposition.AbstractStatement;

public class CFGNode extends GraphNode implements Comparable<CFGNode> {
	private AbstractStatement statement;
	private BasicBlock basicBlock;
	private PDGNode pdgNode;
	private volatile int hashCode = 0;

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
		int numberOfNonLoopbackFlows = 0;
		for(GraphEdge edge : incomingEdges) {
			Flow flow = (Flow)edge;
			if(!flow.isLoopbackFlow())
				numberOfNonLoopbackFlows++;
		}
		if(numberOfNonLoopbackFlows == 0)
			return true;
		return false;
	}

	public boolean isBranch() {
		if(outgoingEdges.size() > 1 || this instanceof CFGBranchNode)
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

	public PDGNode getPDGNode() {
		return pdgNode;
	}

	public void setPDGNode(PDGNode pdgNode) {
		this.pdgNode = pdgNode;
	}

	public boolean equals(Object o) {
		if(this == o)
    		return true;
    	
    	if(o instanceof CFGNode) {
    		CFGNode node = (CFGNode)o;
    		return this.getId() == node.getId();
    	}
    	return false;
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 37*result + getId();
			hashCode = result;
		}
		return hashCode;
	}

	public String toString() {
		return id + "\t" + statement.toString();
	}

	public int compareTo(CFGNode node) {
		if(this.getId() > node.getId())
			return 1;
		else if(this.getId() < node.getId())
			return -1;
		else
			return 0;
	}
}
