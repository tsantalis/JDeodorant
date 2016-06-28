package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGNode;

public class ControlDependenceTreeNode {
	private ControlDependenceTreeNode parent;
	private PDGNode node;
	private int level;
	private List<ControlDependenceTreeNode> children;
	private ControlDependenceTreeNode ifParent;
	private ControlDependenceTreeNode elseIfChild;
	private boolean isElseNode = false;
	private boolean isTernary = false;

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

	public ControlDependenceTreeNode(ControlDependenceTreeNode parent, ControlDependenceTreeNode previousSibling, PDGNode node) {
		this.parent = parent;
		if(parent == null) {
			level = 0;
		}
		else {
			level = parent.level + 1;
		}
		this.node = node;
		this.children = new ArrayList<ControlDependenceTreeNode>();
		if(parent != null) {
			int indexOfPreviousSibling = parent.children.indexOf(previousSibling);
			parent.children.add(indexOfPreviousSibling+1, this);
		}
	}

	public boolean parentChildRelationship(double parentId, double childId) {
		ControlDependenceTreeNode root = getRoot();
		ControlDependenceTreeNode treeParent = root.getNode(parentId);
		if(treeParent != null) {
			for(ControlDependenceTreeNode treeChild : treeParent.children) {
				if(treeChild.getId() == childId)
					return true;
			}
		}
		return false;
	}

	public boolean ifElseRelationship(double ifParentId, double elseChildId) {
		ControlDependenceTreeNode root = getRoot();
		ControlDependenceTreeNode ifParent = root.getNode(ifParentId);
		ControlDependenceTreeNode elseChild = root.getNode(elseChildId);
		if(ifParent != null && elseChild != null) {
			if(elseChild.isElseNode && elseChild.ifParent.getId() == ifParent.getId())
				return true;
		}
		return false;
	}

	public ControlDependenceTreeNode getNode(double id) {
		if(this.getId() == id) {
			return this;
		}
		else if(this.isLeaf()) {
			return null;
		}
		else {
			for(ControlDependenceTreeNode child : this.children) {
				ControlDependenceTreeNode treeNode = child.getNode(id);
				if(treeNode != null)
					return treeNode;
			}
		}
		return null;
	}

	public ControlDependenceTreeNode getNode(PDGNode node) {
		if(this.node != null && this.node.equals(node)) {
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

	public ControlDependenceTreeNode getElseNode(PDGNode ifParent) {
		if(this.isElseNode && this.getIfParent().getNode().equals(ifParent)) {
			return this;
		}
		else if(this.isLeaf()) {
			return null;
		}
		else {
			for(ControlDependenceTreeNode child : this.children) {
				ControlDependenceTreeNode treeNode = child.getElseNode(ifParent);
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
			if(levelNode.isElseNode)
				pdgNode = levelNode.ifParent.node;
			if(pdgNode != null)
				predicateNodes.add(pdgNode);
		}
		return predicateNodes;
	}

	public Set<ControlDependenceTreeNode> getElseNodesInLevel(int level) {
		ControlDependenceTreeNode root = getRoot();
		List<ControlDependenceTreeNode> levelNodes = root.getControlDependenceTreeNodesInLevel(level);
		Set<ControlDependenceTreeNode> elseNodes = new LinkedHashSet<ControlDependenceTreeNode>();
		for(ControlDependenceTreeNode levelNode : levelNodes) {
			if(levelNode.isElseNode) {
				elseNodes.add(levelNode);
			}
		}
		return elseNodes;
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

	public List<ControlDependenceTreeNode> getIfParents() {
		List<ControlDependenceTreeNode> ifParents = new ArrayList<ControlDependenceTreeNode>();
		if(ifParent != null) {
			ifParents.addAll(ifParent.getIfParents());
			ifParents.add(ifParent);
		}
		return ifParents;
	}

	public List<ControlDependenceTreeNode> getElseIfChildren() {
		List<ControlDependenceTreeNode> elseIfChildren = new ArrayList<ControlDependenceTreeNode>();
		if(elseIfChild != null) {
			elseIfChildren.add(elseIfChild);
			elseIfChildren.addAll(elseIfChild.getElseIfChildren());
		}
		return elseIfChildren;
	}

	public boolean isElseNode() {
		return isElseNode;
	}

	public void setElseNode(boolean isElseNode) {
		this.isElseNode = isElseNode;
	}

	public boolean isTernary() {
		return isTernary;
	}

	public void setTernary(boolean isTernary) {
		this.isTernary = isTernary;
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

	public ControlDependenceTreeNode getPreviousSibling() {
		List<ControlDependenceTreeNode> siblings = this.getSiblings();
		ControlDependenceTreeNode previousSibling = null;
		for(ControlDependenceTreeNode sibling : siblings) {
			if(sibling.getId() < this.getId()) {
				previousSibling = sibling;
			}
		}
		return previousSibling;
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

	public Set<ControlDependenceTreeNode> getDescendants() {
		Set<ControlDependenceTreeNode> descendants = new LinkedHashSet<ControlDependenceTreeNode>();
		descendants.addAll(children);
		for(ControlDependenceTreeNode child : this.children) {
			descendants.addAll(child.getDescendants());
		}
		return descendants;
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

	public void setIfParentAndElseIfChild(ControlDependenceTreeNode ifParent) {
		this.ifParent = ifParent;
		ifParent.elseIfChild = this;
	}

	public int hashCode() {
		if(this.getNode() != null) {
			return this.getNode().hashCode();
		}
		else {
			//else node
			int result = 17;
			result = 37*result + (isElseNode ? 1 : 0);
			result = 37*result + ifParent.getNode().hashCode();
			return result;
		}
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof ControlDependenceTreeNode) {
			ControlDependenceTreeNode node = (ControlDependenceTreeNode)o;
			if(this.getNode() != null && node.getNode() != null) {
				return this.getNode().equals(node.getNode());
			}
			if(this.getNode() == null && node.getNode() == null) {
				return this.isElseNode == node.isElseNode && this.getIfParent().equals(node.getIfParent());
			}
		}
		return false;
	}

	public String toString() {
		if(node != null)
			return node.toString();
		else
			return getId() + "\telse\n";
	}

	public double getId() {
		if(isElseNode) {
			Set<PDGNode> controlDependentNodes = ifParent.getNode().getControlDependentNodes();
			int id = 0;
			for(PDGNode controlDependentNode : controlDependentNodes) {
				PDGControlDependence controlDependence = controlDependentNode.getIncomingControlDependence();
				if(controlDependence.isFalseControlDependence()) {
					id = controlDependentNode.getId();
					break;
				}
			}
			//special handling if a try block follows after else clause
			for(ControlDependenceTreeNode child : children) {
				if(child.getId() < id)
					id = (int) child.getId();
			}
			return id - 0.5;
		}
		else {
			return node.getId();
		}
	}
	
	public ControlDependenceTreeNode getElseChild() {
		for(ControlDependenceTreeNode cdtNode : getSiblings()) {
			if(cdtNode.isElseNode && this.equals(cdtNode.getIfParent())) {
				return cdtNode;
			}
		}
		return null;
	}
}
