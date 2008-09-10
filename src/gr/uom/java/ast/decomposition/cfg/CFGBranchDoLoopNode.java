package gr.uom.java.ast.decomposition.cfg;

import java.util.Set;

import gr.uom.java.ast.decomposition.AbstractStatement;

public class CFGBranchDoLoopNode extends CFGBranchNode {

	public CFGBranchDoLoopNode(AbstractStatement statement) {
		super(statement);
	}

	public CFGNode getJoinNode() {
		Flow flow = getTrueControlFlow();
		return (CFGNode)flow.dst;
	}

	public Set<CFGNode> getImmediatelyNestedNodes() {
		return null;
	}
}
