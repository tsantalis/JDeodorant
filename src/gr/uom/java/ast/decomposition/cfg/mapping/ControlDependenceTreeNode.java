package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.IfStatement;

import gr.uom.java.ast.decomposition.cfg.CFGBranchIfNode;
import gr.uom.java.ast.decomposition.cfg.GraphEdge;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGControlPredicateNode;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PDGTryNode;

public class ControlDependenceTreeNode {
	private ControlDependenceTreeNode parent;
	private PDGNode node;
	private int level;
	private List<ControlDependenceTreeNode> children;

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
		this.processControlDependences();
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

	private ControlDependenceTreeNode getNode(PDGNode node) {
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

	private int numberOfOutgoingFalseControlDependences(PDGNode pdgNode) {
		int count = 0;
		Iterator<GraphEdge> edgeIterator = pdgNode.getOutgoingDependenceIterator();
		while(edgeIterator.hasNext()) {
			PDGDependence dependence = (PDGDependence)edgeIterator.next();
			if(dependence instanceof PDGControlDependence) {
				PDGControlDependence controlDependence = (PDGControlDependence)dependence;
				if(controlDependence.isFalseControlDependence())
					count++;
			}
		}
		return count;
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

	private List<ControlDependenceTreeNode> getLeaves() {
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

	private void processControlDependences() {
		Iterator<GraphEdge> edgeIterator = node.getOutgoingDependenceIterator();
		while(edgeIterator.hasNext()) {
			PDGDependence dependence = (PDGDependence)edgeIterator.next();
			if(dependence instanceof PDGControlDependence) {
				PDGControlDependence controlDependence = (PDGControlDependence)dependence;
				PDGNode dstNode = (PDGNode)controlDependence.getDst();
				if(dstNode instanceof PDGControlPredicateNode) {
					//special handling for symmetrical if statements
					PDGNode nodeControlParent = dstNode.getControlDependenceParent();
					PDGControlDependence nodeIncomingControlDependence = dstNode.getIncomingControlDependence();
					if(nodeControlParent.getCFGNode() instanceof CFGBranchIfNode && nodeIncomingControlDependence.isFalseControlDependence() &&
							numberOfOutgoingFalseControlDependences(nodeControlParent) == 1 &&
							dstNode.getASTStatement().getParent() instanceof IfStatement) {
						//a case of "if/else if" -> add as a sibling, not as a child
						ControlDependenceTreeNode treeNode = new ControlDependenceTreeNode(this.parent, dstNode);
						this.parent.children.add(treeNode);
					}
					else {
						ControlDependenceTreeNode treeNode = new ControlDependenceTreeNode(this, dstNode);
						this.children.add(treeNode);
					}
				}
				else if(dstNode instanceof PDGTryNode) {
					ControlDependenceTreeNode treeNode = new ControlDependenceTreeNode(this, dstNode);
					this.children.add(treeNode);
				}
			}
		}
	}

	public String toString() {
		return node.toString();
	}
}
