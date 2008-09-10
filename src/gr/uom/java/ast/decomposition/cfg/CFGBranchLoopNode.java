package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gr.uom.java.ast.decomposition.AbstractStatement;

public class CFGBranchLoopNode extends CFGBranchNode {

	public CFGBranchLoopNode(AbstractStatement statement) {
		super(statement);
	}

	public CFGNode getJoinNode() {
		return this;
	}

	public Set<CFGNode> getImmediatelyNestedNodes() {
		Set<CFGNode> nestedNodes = new LinkedHashSet<CFGNode>();
		BasicBlock srcBlock = getBasicBlock();
		//special handling for the loops without false control flow
		if(!srcBlock.getNodes().isEmpty()) {
			nestedNodes.addAll(srcBlock.getNodes());
		}
		List<BasicBlock> blocksBetween = getNestedBasicBlocks();
		if(!blocksBetween.isEmpty()) {
			nestedNodes.addAll(blocksBetween.get(0).getAllNodes());
			//remove the basic blocks which are between branch and join nodes
			Map<CFGNode, Set<BasicBlock>> joinBlockMap = getJoinBlocks(blocksBetween);
			for(CFGNode node : joinBlockMap.keySet()) {
				if(node instanceof CFGBranchLoopNode && !node.equals(this)) {
					CFGBranchLoopNode loopNode = (CFGBranchLoopNode)node;
					List<BasicBlock> blocksBetween2 = loopNode.getNestedBasicBlocks();
					blocksBetween.removeAll(blocksBetween2);
				}
				else {
					//this is the join node of a conditional node
					Set<BasicBlock> joinBlocks = joinBlockMap.get(node);
					List<Set<CFGBranchConditionalNode>> backwardReachableConditionalNodes = new ArrayList<Set<CFGBranchConditionalNode>>();
					for(BasicBlock block : joinBlocks) {
						backwardReachableConditionalNodes.add(block.getLastNode().getBackwardReachableConditionalNodes());
					}
					Set<CFGBranchConditionalNode> intersection = new LinkedHashSet<CFGBranchConditionalNode>(backwardReachableConditionalNodes.get(0));
					for(int i=1; i<backwardReachableConditionalNodes.size(); i++) {
						intersection.retainAll(backwardReachableConditionalNodes.get(i));
					}
					CFGBranchConditionalNode conditionalNode = new ArrayList<CFGBranchConditionalNode>(intersection).get(0);
					//check whether the conditional node has the same join node
					if(conditionalNode.getJoinNode().equals(node)) {
						List<BasicBlock> blocksBetween2 = conditionalNode.getNestedBasicBlocks();
						blocksBetween.removeAll(blocksBetween2);
					}
				}
			}
			for(BasicBlock block : blocksBetween) {
				nestedNodes.addAll(block.getAllNodes());
			}
		}
		return nestedNodes;
	}

	public List<BasicBlock> getNestedBasicBlocks() {
		List<BasicBlock> blocksBetween = new ArrayList<BasicBlock>();
		BasicBlock srcBlock = getBasicBlock();
		Flow falseControlFlow = getFalseControlFlow();
		if(falseControlFlow != null) {
			CFGNode dstNode = (CFGNode)falseControlFlow.dst;
			if(dstNode instanceof CFGBranchLoopNode) {
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

	private Map<CFGNode, Set<BasicBlock>> getJoinBlocks(List<BasicBlock> blocks) {
		Map<CFGNode, Set<BasicBlock>> joinBlockMapWithIncomingFlows = getJoinBlocksWithIncomingFlows(blocks);
		Map<CFGNode, Set<BasicBlock>> joinBlockMapWithOutgoingFlows = getJoinBlocksWithOutgoingFlows(blocks);
		for(CFGNode key : joinBlockMapWithOutgoingFlows.keySet()) {
			if(!joinBlockMapWithIncomingFlows.containsKey(key)) {
				joinBlockMapWithIncomingFlows.put(key, joinBlockMapWithOutgoingFlows.get(key));
			}
		}
		return joinBlockMapWithIncomingFlows;
	}

	private Map<CFGNode, Set<BasicBlock>> getJoinBlocksWithOutgoingFlows(List<BasicBlock> blocks) {
		Map<CFGNode, Set<BasicBlock>> joinBlockMap = new LinkedHashMap<CFGNode, Set<BasicBlock>>();
		for(BasicBlock block : blocks) {
			CFGNode lastNode = block.getLastNode();
			for(GraphEdge edge : lastNode.outgoingEdges) {
				Flow flow = (Flow)edge;
				CFGNode dstNode = (CFGNode)flow.dst;
				BasicBlock dstBlock = dstNode.getBasicBlock();
				if(blocks.contains(dstBlock) || dstBlock.equals(this.getBasicBlock())) {
					if(joinBlockMap.containsKey(dstNode)) {
						Set<BasicBlock> set = joinBlockMap.get(dstNode);
						set.add(block);
					}
					else {
						Set<BasicBlock> set = new LinkedHashSet<BasicBlock>();
						set.add(block);
						joinBlockMap.put(dstNode, set);
					}
				}
			}
		}
		Set<CFGNode> keysToBeRemoved = new LinkedHashSet<CFGNode>();
		for(CFGNode key : joinBlockMap.keySet()) {
			Set<BasicBlock> set = joinBlockMap.get(key);
			if(set.size() == 1)
				keysToBeRemoved.add(key);
		}
		for(CFGNode key : keysToBeRemoved)
			joinBlockMap.remove(key);
		return joinBlockMap;
	}

	private Map<CFGNode, Set<BasicBlock>> getJoinBlocksWithIncomingFlows(List<BasicBlock> blocks) {
		Map<CFGNode, Set<BasicBlock>> joinBlockMap = new LinkedHashMap<CFGNode, Set<BasicBlock>>();
		for(BasicBlock block : blocks) {
			CFGNode leaderNode = block.getLeader();
			for(GraphEdge edge : leaderNode.incomingEdges) {
				Flow flow = (Flow)edge;
				CFGNode srcNode = (CFGNode)flow.src;
				CFGNode dstNode = (CFGNode)flow.dst;
				BasicBlock srcBlock = srcNode.getBasicBlock();
				if(blocks.contains(srcBlock) || srcBlock.equals(this.getBasicBlock())) {
					if(joinBlockMap.containsKey(dstNode)) {
						Set<BasicBlock> set = joinBlockMap.get(dstNode);
						set.add(srcBlock);
					}
					else {
						Set<BasicBlock> set = new LinkedHashSet<BasicBlock>();
						set.add(srcBlock);
						joinBlockMap.put(dstNode, set);
					}
				}
			}
		}
		Set<CFGNode> keysToBeRemoved = new LinkedHashSet<CFGNode>();
		for(CFGNode key : joinBlockMap.keySet()) {
			Set<BasicBlock> set = joinBlockMap.get(key);
			if(set.size() == 1)
				keysToBeRemoved.add(key);
		}
		for(CFGNode key : keysToBeRemoved)
			joinBlockMap.remove(key);
		return joinBlockMap;
	}
}
