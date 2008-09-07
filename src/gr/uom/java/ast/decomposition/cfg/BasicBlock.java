package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.List;

public class BasicBlock {
	private static int blockNum = 0;
	private int id;
	private CFGNode leader;
	private List<CFGNode> nodes;
	
	public BasicBlock(CFGNode node) {
		blockNum++;
		this.id = blockNum;
		this.leader = node;
		node.setBasicBlock(this);
		this.nodes = new ArrayList<CFGNode>();
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

	public void add(CFGNode node) {
		nodes.add(node);
		node.setBasicBlock(this);
	}

	public static void resetBlockNum() {
		blockNum = 0;
	}

	public String toString() {
		return leader.toString() + nodes.toString();
	}
}
