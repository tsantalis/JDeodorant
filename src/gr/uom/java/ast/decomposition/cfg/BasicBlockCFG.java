package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class BasicBlockCFG {
	private List<BasicBlock> basicBlocks;
	private Map<BasicBlock, Set<BasicBlock>> forwardReachableBlocks;
	
	public BasicBlockCFG(CFG cfg) {
		this.basicBlocks = new ArrayList<BasicBlock>();
		this.forwardReachableBlocks = new LinkedHashMap<BasicBlock, Set<BasicBlock>>();
		TreeSet<GraphNode> allNodes = new TreeSet<GraphNode>(cfg.nodes);
		Map<CFGBlockNode, List<CFGNode>> directlyNestedNodesInBlocks = cfg.getDirectlyNestedNodesInBlocks();
		for(CFGBlockNode blockNode : directlyNestedNodesInBlocks.keySet()) {
			if(blockNode instanceof CFGTryNode) {
				CFGTryNode tryNode = (CFGTryNode)blockNode;
				if(!tryNode.hasResources())
					allNodes.add(tryNode);
			}
		}
		for(GraphNode node : allNodes) {
			CFGNode cfgNode = (CFGNode)node;
			if(cfgNode instanceof CFGTryNode && !((CFGTryNode)cfgNode).hasResources()) {
				CFGTryNode tryNode = (CFGTryNode)cfgNode;
				if(!basicBlocks.isEmpty()) {
					BasicBlock basicBlock = basicBlocks.get(basicBlocks.size()-1);
					basicBlock.addTryNode(tryNode);
				}
			}
			else if(cfgNode.isLeader()) {
				BasicBlock basicBlock = new BasicBlock(cfgNode);
				if(!basicBlocks.isEmpty()) {
					BasicBlock previousBlock = basicBlocks.get(basicBlocks.size()-1);
					previousBlock.setNextBasicBlock(basicBlock);
					basicBlock.setPreviousBasicBlock(previousBlock);
				}
				basicBlocks.add(basicBlock);
			}
			else {
				BasicBlock basicBlock = basicBlocks.get(basicBlocks.size()-1);
				basicBlock.add(cfgNode);
			}
		}
		//special handling for the try statement that is first node
		for(CFGBlockNode blockNode : directlyNestedNodesInBlocks.keySet()) {
			if(blockNode instanceof CFGTryNode) {
				CFGTryNode tryNode = (CFGTryNode)blockNode;
				if(tryNode.id == 1 && !basicBlocks.isEmpty() && !tryNode.hasResources()) {
					BasicBlock basicBlock = basicBlocks.get(0);
					basicBlock.addTryNode(tryNode);
				}
			}
		}
		BasicBlock.resetBlockNum();
	}

	public List<BasicBlock> getBasicBlocks() {
		return basicBlocks;
	}

	public Set<BasicBlock> forwardReachableBlocks(BasicBlock basicBlock) {
		if(forwardReachableBlocks.containsKey(basicBlock))
			return forwardReachableBlocks.get(basicBlock);
		Set<BasicBlock> reachableBlocks = new LinkedHashSet<BasicBlock>();
		reachableBlocks.add(basicBlock);
		CFGNode lastNode = basicBlock.getLastNode();
		for(GraphEdge edge : lastNode.outgoingEdges) {
			Flow flow = (Flow)edge;
			if(!flow.isLoopbackFlow()) {
				CFGNode dstNode = (CFGNode)flow.dst;
				BasicBlock dstBasicBlock = dstNode.getBasicBlock();
				reachableBlocks.add(dstBasicBlock);
				reachableBlocks.addAll(forwardReachableBlocks(dstBasicBlock));
			}
		}
		forwardReachableBlocks.put(basicBlock, reachableBlocks);
		return reachableBlocks;
	}
}
