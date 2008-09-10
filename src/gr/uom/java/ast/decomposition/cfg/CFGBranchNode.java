package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import gr.uom.java.ast.decomposition.AbstractStatement;

public abstract class CFGBranchNode extends CFGNode {

	public CFGBranchNode(AbstractStatement statement) {
		super(statement);
	}

	public Flow getTrueControlFlow() {
		for(GraphEdge edge : outgoingEdges) {
			Flow flow = (Flow)edge;
			if(flow.isTrueControlFlow())
				return flow;
		}
		return null;
	}

	public Flow getFalseControlFlow() {
		for(GraphEdge edge : outgoingEdges) {
			Flow flow = (Flow)edge;
			if(flow.isFalseControlFlow())
				return flow;
		}
		return null;
	}

	protected List<BasicBlock> getNestedBasicBlocksToEnd() {
		List<BasicBlock> blocksBetween = new ArrayList<BasicBlock>();
		BasicBlock srcBlock = getBasicBlock();
		BasicBlock nextBlock = srcBlock;
		while(nextBlock.getNextBasicBlock() != null) {
			nextBlock = nextBlock.getNextBasicBlock();
			blocksBetween.add(nextBlock);
		}
		return blocksBetween;
	}

	public abstract CFGNode getJoinNode();
	
	public abstract Set<CFGNode> getImmediatelyNestedNodes();
}
