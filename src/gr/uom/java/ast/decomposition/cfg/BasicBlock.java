package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.List;

public class BasicBlock {
	private static int blockNum = 0;
	private int id;
	private CFGNode leader;
	private List<CFGNode> nodes;
	private List<CFGTryNode> tryNodes;
	private BasicBlock previousBasicBlock;
	private BasicBlock nextBasicBlock;
	
	public BasicBlock(CFGNode node) {
		blockNum++;
		this.id = blockNum;
		this.leader = node;
		node.setBasicBlock(this);
		this.nodes = new ArrayList<CFGNode>();
		this.tryNodes = new ArrayList<CFGTryNode>();
	}

	public int getId() {
		return id;
	}

	public CFGNode getLeader() {
		return leader;
	}

	public List<CFGNode> getNodes() {
		return nodes;
	}

	public List<CFGNode> getAllNodes() {
		List<CFGNode> allNodes = new ArrayList<CFGNode>();
		allNodes.add(leader);
		allNodes.addAll(nodes);
		return allNodes;
	}

	public List<CFGNode> getAllNodesIncludingTry() {
		List<CFGNode> allNodes = new ArrayList<CFGNode>();
		allNodes.add(leader);
		allNodes.addAll(nodes);
		allNodes.addAll(tryNodes);
		return allNodes;
	}

	public CFGNode getLastNode() {
		if(!nodes.isEmpty())
			return nodes.get(nodes.size()-1);
		else
			return leader;
	}

	public void add(CFGNode node) {
		nodes.add(node);
		node.setBasicBlock(this);
	}

	public void addTryNode(CFGTryNode tryNode) {
		tryNodes.add(tryNode);
		tryNode.setBasicBlock(this);
	}

	public BasicBlock getPreviousBasicBlock() {
		return previousBasicBlock;
	}

	public void setPreviousBasicBlock(BasicBlock previousBasicBlock) {
		this.previousBasicBlock = previousBasicBlock;
	}

	public BasicBlock getNextBasicBlock() {
		return nextBasicBlock;
	}

	public void setNextBasicBlock(BasicBlock nextBasicBlock) {
		this.nextBasicBlock = nextBasicBlock;
	}

	public static void resetBlockNum() {
		blockNum = 0;
	}

	public String toString() {
		return leader.toString() + nodes.toString();
	}
}
