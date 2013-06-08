package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import gr.uom.java.ast.decomposition.cfg.PDGNode;

public class ControlDependenceTreeNode {
	private ControlDependenceTreeNode parent;
	private PDGNode node;
	private int level;
	private List<ControlDependenceTreeNode> children;
	private ControlDependenceTreeNode ifParent;
	private ControlDependenceTreeNode elseIfChild;

	private ControlDependenceTreeNode(ControlDependenceTreeNode treeNode) {
		this.parent = treeNode.parent;
		this.node = treeNode.node;
		this.level = treeNode.level;
		this.children = new ArrayList<ControlDependenceTreeNode>();
		this.children.addAll(treeNode.children);
	}

	public ControlDependenceTreeNode(ControlDependenceTreeNode parent, PDGNode node) {
		this.parent = parent;
		if(parent == null) {
			level = 0;
		}
		else {
			level = parent.level + 1;
		}
		this.node = node;
		this.children = new ArrayList<ControlDependenceTreeNode>();
		if(parent != null)
			parent.children.add(this);
	}

	public boolean parentChildRelationship(PDGNode parent, PDGNode child) {
		ControlDependenceTreeNode root = getRoot();
		ControlDependenceTreeNode treeParent = root.getNode(parent);
		if(treeParent != null) {
			for(ControlDependenceTreeNode treeChild : treeParent.children) {
				if(treeChild.node.equals(child))
					return true;
			}
		}
		return false;
	}

	public ControlDependenceTreeNode getNode(PDGNode node) {
		if(this.node.equals(node)) {
			return this;
		}
		else if(this.isLeaf()) {
			return null;
		}
		else {
			for(ControlDependenceTreeNode child : this.children) {
				ControlDependenceTreeNode treeNode = child.getNode(node);
				if(treeNode != null)
					return treeNode;
			}
		}
		return null;
	}

	public Set<PDGNode> getControlPredicateNodesInLevel(int level) {
		ControlDependenceTreeNode root = getRoot();
		List<ControlDependenceTreeNode> levelNodes = root.getControlDependenceTreeNodesInLevel(level);
		Set<PDGNode> predicateNodes = new LinkedHashSet<PDGNode>();
		for(ControlDependenceTreeNode levelNode : levelNodes) {
			PDGNode pdgNode = levelNode.node;
			predicateNodes.add(pdgNode);
		}
		return predicateNodes;
	}

	public int getMaxLevel() {
		ControlDependenceTreeNode root = getRoot();
		List<ControlDependenceTreeNode> leaves = root.getLeaves();
		int max = 0;
		for(ControlDependenceTreeNode leaf : leaves) {
			if(leaf.level > max) {
				max = leaf.level;
			}
		}
		return max;
	}

	private ControlDependenceTreeNode getRoot() {
		if(parent == null)
			return this;
		else 
			return parent.getRoot();
	}
	
	private boolean isLeaf() {
		return this.children.isEmpty();
	}

	public List<ControlDependenceTreeNode> getLeaves() {
		List<ControlDependenceTreeNode> leaves = new ArrayList<ControlDependenceTreeNode>();
		if(this.isLeaf()) {
			leaves.add(this);
		}
		else {
			for(ControlDependenceTreeNode child : this.children) {
				leaves.addAll(child.getLeaves());
			}
		}
		return leaves;
	}
	
	public boolean ifStatementInsideElseIfChain() {
		return ifParent != null || elseIfChild != null;
	}

	public int getNumberOfIfParents() {
		if(ifParent == null)
			return 0;
		else 
			return ifParent.getNumberOfIfParents() + 1;
	}
	
	public int getNumberOfElseIfChildren() {
		if(elseIfChild == null)
			return 0;
		else 
			return elseIfChild.getNumberOfElseIfChildren() + 1;
	}
	
	public int getLengthOfElseIfChain() {
		return getNumberOfIfParents() + getNumberOfElseIfChildren();
	}

	public List<ControlDependenceTreeNode> getSiblings() {
		List<ControlDependenceTreeNode> siblings = new ArrayList<ControlDependenceTreeNode>();
		if(this.parent != null) {
			for(ControlDependenceTreeNode sibling : parent.children) {
				if(!sibling.equals(this))
					siblings.add(sibling);
			}
		}
		return siblings;
	}

	public boolean areAllSiblingsLeaves() {
		if(this.isLeaf()) {
			ControlDependenceTreeNode parent = this.getParent();
			if(parent != null) {
				for(ControlDependenceTreeNode sibling : parent.children) {
					if(!sibling.isLeaf())
						return false;
				}
				return true;
			}
		}
		return false;
	}

	private List<ControlDependenceTreeNode> getControlDependenceTreeNodesInLevel(int level) {
		List<ControlDependenceTreeNode> levelNodes = new ArrayList<ControlDependenceTreeNode>();
		if(this.level == level) {
			levelNodes.add(this);
		}
		else {
			for(ControlDependenceTreeNode child : this.children) {
				levelNodes.addAll(child.getControlDependenceTreeNodesInLevel(level));
			}
		}
		return levelNodes;
	}

	public ControlDependenceTreeNode shallowCopy() {
		return new ControlDependenceTreeNode(this);
	}

	public ControlDependenceTreeNode getParent() {
		return parent;
	}

	public PDGNode getNode() {
		return node;
	}

	public int getLevel() {
		return level;
	}

	public List<ControlDependenceTreeNode> getChildren() {
		return children;
	}

	public int getNodeCount() {
		// count 1 for "this" node
		int count = 1;
		for(ControlDependenceTreeNode child : children) {
			count += child.getNodeCount();
		}
		return count;
	}

	public List<ControlDependenceTreeNode> getNodesInBreadthFirstOrder() {
		List<ControlDependenceTreeNode> nodes = new ArrayList<ControlDependenceTreeNode>();
		List<ControlDependenceTreeNode> queue = new LinkedList<ControlDependenceTreeNode>();
		nodes.add(this);
		queue.add(this);
		while(!queue.isEmpty()) {
			ControlDependenceTreeNode node = queue.remove(0);
			nodes.addAll(node.children);
			queue.addAll(node.children);
		}
		return nodes;
	}

	public ControlDependenceTreeNode getIfParent() {
		return ifParent;
	}

	public void setIfParent(ControlDependenceTreeNode ifParent) {
		this.ifParent = ifParent;
	}

	public ControlDependenceTreeNode getElseIfChild() {
		return elseIfChild;
	}

	public void setElseIfChild(ControlDependenceTreeNode elseIfChild) {
		this.elseIfChild = elseIfChild;
	}

	public int hashCode() {
		return this.getNode().hashCode();
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof ControlDependenceTreeNode) {
			ControlDependenceTreeNode node = (ControlDependenceTreeNode)o;
			return this.getNode().equals(node.getNode());
		}
		return false;
	}

	public String toString() {
		return node.toString();
	}
}
