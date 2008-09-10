package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import gr.uom.java.ast.decomposition.AbstractStatement;

public class CFGBranchConditionalNode extends CFGBranchNode {
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

	public Set<CFGNode> getImmediatelyNestedNodes() {
		return null;
	}

	public List<BasicBlock> getNestedBasicBlocks() {
		List<BasicBlock> blocksBetween = new ArrayList<BasicBlock>();
		BasicBlock srcBlock = getBasicBlock();
		if(joinNode != null) {
			CFGNode dstNode = null;
			if(joinNode instanceof CFGBranchLoopNode) {
				CFGBranchLoopNode loopNode = (CFGBranchLoopNode)joinNode;
				Flow falseControlFlow = loopNode.getFalseControlFlow();
				if(falseControlFlow != null)
					dstNode = (CFGNode)falseControlFlow.dst;
				else
					return getNestedBasicBlocksToEnd();
			}
			else {
				dstNode = joinNode;
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
