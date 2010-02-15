package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.ast.decomposition.AbstractStatement;

public abstract class CFGBranchConditionalNode extends CFGBranchNode {
	private CFGNode joinNode;
	
	public CFGBranchConditionalNode(AbstractStatement statement) {
		super(statement);
	}

	public void setJoinNode(CFGNode joinNode) {
		this.joinNode = joinNode;
	}

	public CFGNode getJoinNode() {
		return joinNode;
	}

	public List<BasicBlock> getNestedBasicBlocks() {
		List<BasicBlock> blocksBetween = new ArrayList<BasicBlock>();
		BasicBlock srcBlock = getBasicBlock();
		if(joinNode != null) {
			CFGNode dstNode = joinNode;
			if(dstNode.getBasicBlock().getId() < srcBlock.getId() && joinNode instanceof CFGBranchLoopNode) {
				CFGBranchLoopNode loopNode = (CFGBranchLoopNode)joinNode;
				Flow falseControlFlow = loopNode.getFalseControlFlow();
				if(falseControlFlow != null)
					dstNode = (CFGNode)falseControlFlow.dst;
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
			else if(srcBlock.getId() >= dstBlock.getId()) {
				return getNestedBasicBlocksToEnd();
			}
		}
		else {
			return getNestedBasicBlocksToEnd();
		}
		return blocksBetween;
	}
}
