package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.ast.decomposition.AbstractStatement;

public class CFGBranchDoLoopNode extends CFGBranchNode {

	public CFGBranchDoLoopNode(AbstractStatement statement) {
		super(statement);
	}

	public CFGNode getJoinNode() {
		Flow flow = getTrueControlFlow();
		return (CFGNode)flow.dst;
	}

	public List<BasicBlock> getNestedBasicBlocks() {
		List<BasicBlock> blocksBetween = new ArrayList<BasicBlock>();
		BasicBlock srcBlock = getBasicBlock();
		BasicBlock joinBlock = getJoinNode().getBasicBlock();
		//join node is always before do-loop node
		blocksBetween.add(joinBlock);
		BasicBlock nextBlock = joinBlock;
		if(!joinBlock.equals(srcBlock)) {
			while(!nextBlock.getNextBasicBlock().equals(srcBlock)) {
				nextBlock = nextBlock.getNextBasicBlock();
				blocksBetween.add(nextBlock);
			}
		}
		return blocksBetween;
	}
}
