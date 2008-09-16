package gr.uom.java.ast.decomposition.cfg;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PDG extends Graph {
	private CFG cfg;
	private PDGMethodEntryNode entryNode;
	private Map<CFGBranchNode, Set<CFGNode>> nestingMap;
	
	public PDG(CFG cfg) {
		this.cfg = cfg;
		this.entryNode = new PDGMethodEntryNode(cfg.getMethod());
		this.nestingMap = new LinkedHashMap<CFGBranchNode, Set<CFGNode>>();
		nodes.add(entryNode);
		for(GraphNode node : cfg.nodes) {
			CFGNode cfgNode = (CFGNode)node;
			if(cfgNode instanceof CFGBranchNode) {
				CFGBranchNode branchNode = (CFGBranchNode)cfgNode;
				nestingMap.put(branchNode, branchNode.getImmediatelyNestedNodesFromAST());
			}
		}
		createControlDependenciesFromEntryNode();
		GraphNode.resetNodeNum();
	}

	private void createControlDependenciesFromEntryNode() {
		for(GraphNode node : cfg.nodes) {
			CFGNode cfgNode = (CFGNode)node;
			if(!isNested(cfgNode)) {
				processCFGNode(entryNode, cfgNode, true);
			}
		}
	}

	private void processCFGNode(PDGNode previousNode, CFGNode cfgNode, boolean controlType) {
		if(cfgNode instanceof CFGBranchNode) {
			PDGControlPredicateNode predicateNode = new PDGControlPredicateNode(cfgNode);
			nodes.add(predicateNode);
			PDGControlDependence controlDependence = new PDGControlDependence(previousNode, predicateNode);
			if(controlType)
				controlDependence.setTrueControlDependence(true);
			else
				controlDependence.setFalseControlDependence(true);
			edges.add(controlDependence);
			processControlPredicate(predicateNode);
		}
		else {
			PDGNode pdgNode = new PDGStatementNode(cfgNode);
			nodes.add(pdgNode);
			PDGControlDependence controlDependence = new PDGControlDependence(previousNode, pdgNode);
			if(controlType)
				controlDependence.setTrueControlDependence(true);
			else
				controlDependence.setFalseControlDependence(true);
			edges.add(controlDependence);
		}
	}

	private void processControlPredicate(PDGControlPredicateNode predicateNode) {
		CFGBranchNode branchNode = (CFGBranchNode)predicateNode.getCFGNode();
		if(branchNode instanceof CFGBranchConditionalNode) {
			CFGBranchConditionalNode conditionalNode = (CFGBranchConditionalNode)branchNode;
			Set<CFGNode> nestedNodesInTrueControlFlow = conditionalNode.getImmediatelyNestedNodesInTrueControlFlow();
			for(CFGNode nestedNode : nestedNodesInTrueControlFlow) {
				processCFGNode(predicateNode, nestedNode, true);
			}
			Set<CFGNode> nestedNodesInFalseControlFlow = conditionalNode.getImmediatelyNestedNodesInFalseControlFlow();
			for(CFGNode nestedNode : nestedNodesInFalseControlFlow) {
				processCFGNode(predicateNode, nestedNode, false);
			}
		}
		else {
			Set<CFGNode> nestedNodes = nestingMap.get(branchNode);
			for(CFGNode nestedNode : nestedNodes) {
				processCFGNode(predicateNode, nestedNode, true);
			}
		}
	}

	private boolean isNested(CFGNode node) {
		for(CFGBranchNode key : nestingMap.keySet()) {
			Set<CFGNode> nestedNodes = nestingMap.get(key);
			if(nestedNodes.contains(node))
				return true;
		}
		return false;
	}

	public List<BasicBlock> getBasicBlocks() {
		return cfg.getBasicBlocks();
	}

	public Set<BasicBlock> forwardReachableBlocks(BasicBlock basicBlock) {
		return cfg.getBasicBlockCFG().forwardReachableBlocks(basicBlock);
	}

	//returns the node (branch or method entry) that directly dominates the leader of the block
	private PDGNode directlyDominates(BasicBlock block) {
		CFGNode leaderCFGNode = block.getLeader();
		PDGNode leaderPDGNode = leaderCFGNode.getPDGNode();
		for(GraphEdge edge : leaderPDGNode.incomingEdges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(dependence instanceof PDGControlDependence) {
				PDGNode srcNode = (PDGNode)dependence.src;
				return srcNode;
			}
		}
		return null;
	}

	public Set<BasicBlock> dominatedBlocks(BasicBlock block) {
		PDGNode pdgNode = directlyDominates(block);
		return dominatedBlocks(pdgNode);
	}
	
	private Set<BasicBlock> dominatedBlocks(PDGNode branchNode) {
		Set<BasicBlock> dominatedBlocks = new LinkedHashSet<BasicBlock>();
		for(GraphEdge edge : branchNode.outgoingEdges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(dependence instanceof PDGControlDependence) {
				PDGNode dstNode = (PDGNode)dependence.dst;
				BasicBlock dstBlock = dstNode.getBasicBlock();
				dominatedBlocks.add(dstBlock);
				PDGNode dstBlockLastNode = dstBlock.getLastNode().getPDGNode();
				if(dstBlockLastNode instanceof PDGControlPredicateNode)
					dominatedBlocks.addAll(dominatedBlocks(dstBlockLastNode));
			}
		}
		return dominatedBlocks;
	}

	public Set<BasicBlock> boundaryBlocks(CFGNode node) {
		Set<BasicBlock> boundaryBlocks = new LinkedHashSet<BasicBlock>();
		BasicBlock srcBlock = node.getBasicBlock();
		for(BasicBlock block : getBasicBlocks()) {
			Set<BasicBlock> forwardReachableBlocks = forwardReachableBlocks(block);
			Set<BasicBlock> dominatedBlocks = dominatedBlocks(block);
			Set<BasicBlock> intersection = new LinkedHashSet<BasicBlock>();
			intersection.addAll(forwardReachableBlocks);
			intersection.retainAll(dominatedBlocks);
			if(intersection.contains(srcBlock))
				boundaryBlocks.add(block);
		}
		return boundaryBlocks;
	}
}
