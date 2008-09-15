package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.ast.decomposition.AbstractStatement;

public class CFGBranchLoopNode extends CFGBranchNode {

	public CFGBranchLoopNode(AbstractStatement statement) {
		super(statement);
	}

	public CFGNode getJoinNode() {
		return this;
	}

	public List<BasicBlock> getNestedBasicBlocks() {
		List<BasicBlock> blocksBetween = new ArrayList<BasicBlock>();
		BasicBlock srcBlock = getBasicBlock();
		Flow falseControlFlow = getFalseControlFlow();
		if(falseControlFlow != null) {
			CFGNode dstNode = (CFGNode)falseControlFlow.dst;
			if(dstNode.getBasicBlock().getId() < srcBlock.getId() && dstNode instanceof CFGBranchLoopNode) {
				CFGBranchLoopNode loopNode = (CFGBranchLoopNode)dstNode;
				Flow falseControlFlow2 = loopNode.getFalseControlFlow();
				if(falseControlFlow2 != null)
					dstNode = (CFGNode)falseControlFlow2.dst;
				else
					return getNestedBasicBlocksToEnd();
			}
			BasicBlock dstBlock = dstNode.getBasicBlock();
			if(srcBlock.getId() < dstBlock.getId()) {
				BasicBlock nextBlock = srcBlock;
				while(!nextBlock.getNextBasicBlock().equals(dstBlock)) {
					nextBlock = nextBlock.getNextBasicBlock();
					blocksBetween.add(nextBlock);
				}
			}
			else if(srcBlock.getId() > dstBlock.getId()) {
				return getNestedBasicBlocksToEnd();
			}
		}
		else {
			return getNestedBasicBlocksToEnd();
		}
		return blocksBetween;
	}
}
