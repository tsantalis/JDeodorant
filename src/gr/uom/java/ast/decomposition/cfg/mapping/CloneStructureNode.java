package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.cfg.CFGBranchIfNode;
import gr.uom.java.ast.decomposition.cfg.PDGNode;

import java.util.Set;
import java.util.TreeSet;

public class CloneStructureNode implements Comparable<CloneStructureNode> {
	private CloneStructureNode parent;
	private PDGNodeMapping mapping;
	private Set<CloneStructureNode> children;
	
	public CloneStructureNode(PDGNodeMapping mapping) {
		this.mapping = mapping;
		this.children = new TreeSet<CloneStructureNode>();
	}

	public CloneStructureNode(CloneStructureNode parent, PDGNodeMapping mapping) {
		this.parent = parent;
		this.mapping = mapping;
		this.children = new TreeSet<CloneStructureNode>();
	}

	private void setParent(CloneStructureNode parent) {
		this.parent = parent;
		parent.children.add(this);
	}

	public void addChild(CloneStructureNode node) {
		CloneStructureNode symmetricalChild = this.containsChildSymmetricalToNode(node);
		CloneStructureNode controlParent = this.containsControlParentOfNode(node);
		CloneStructureNode controlChild = this.containsControlChildOfNode(node);
		if(symmetricalChild != null) {
			node.setParent(symmetricalChild);
		}
		else if(controlParent != null) {
			node.setParent(controlParent);
		}
		else if(controlChild != null) {
			controlChild.parent.children.remove(controlChild);
			node.setParent(controlChild.parent);
			controlChild.setParent(node);
		}
		else {
			node.setParent(this);
		}
	}
	
	private CloneStructureNode containsChildSymmetricalToNode(CloneStructureNode other) {
		PDGNodeMapping otherNodeMapping = other.getMapping();
		if(otherNodeMapping.getSymmetricalIfNodePair() != null) {
			for(CloneStructureNode child : this.children) {
				PDGNodeMapping childNodeMapping = child.getMapping();
				if(childNodeMapping.getSymmetricalIfNodePair() != null) {
					if(otherNodeMapping.getSymmetricalIfNodePair().equals(childNodeMapping))
						return child;
				}
			}
		}
		return null;
	}
	
	private CloneStructureNode containsControlChildOfNode(CloneStructureNode other) {
		PDGNodeMapping otherNodeMapping = other.getMapping();
		if(otherNodeMapping.getNodeG1().getCFGNode() instanceof CFGBranchIfNode &&
				otherNodeMapping.getNodeG2().getCFGNode() instanceof CFGBranchIfNode) {
			for(CloneStructureNode child : this.children) {
				PDGNodeMapping childNodeMapping = child.getMapping();
				if(childNodeMapping.getNodeG1().getCFGNode() instanceof CFGBranchIfNode &&
						childNodeMapping.getNodeG2().getCFGNode() instanceof CFGBranchIfNode) {
					PDGNode nodeG1ControlParent = childNodeMapping.getNodeG1().getControlDependenceParent();
					PDGNode nodeG2ControlParent = childNodeMapping.getNodeG2().getControlDependenceParent();
					if(otherNodeMapping.getNodeG1().equals(nodeG1ControlParent) && 
							otherNodeMapping.getNodeG2().equals(nodeG2ControlParent))
						return child;
				}
			}
		}
		return null;
	}
	
	private CloneStructureNode containsControlParentOfNode(CloneStructureNode other) {
		PDGNodeMapping otherNodeMapping = other.getMapping();
		if(otherNodeMapping.getNodeG1().getCFGNode() instanceof CFGBranchIfNode &&
				otherNodeMapping.getNodeG2().getCFGNode() instanceof CFGBranchIfNode) {
			PDGNode otherNodeG1ControlParent = otherNodeMapping.getNodeG1().getControlDependenceParent();
			PDGNode otherNodeG2ControlParent = otherNodeMapping.getNodeG2().getControlDependenceParent();
			for(CloneStructureNode child : this.children) {
				PDGNodeMapping childNodeMapping = child.getMapping();
				if(childNodeMapping.getNodeG1().getCFGNode() instanceof CFGBranchIfNode &&
						childNodeMapping.getNodeG2().getCFGNode() instanceof CFGBranchIfNode) {
					if(childNodeMapping.getNodeG1().equals(otherNodeG1ControlParent) && 
							childNodeMapping.getNodeG2().equals(otherNodeG2ControlParent))
						return child;
				}
			}
		}
		return null;
	}

	public CloneStructureNode getParent() {
		return parent;
	}

	public PDGNodeMapping getMapping() {
		return mapping;
	}

	public Set<CloneStructureNode> getChildren() {
		return children;
	}

	public boolean isRoot() {
		return parent == null;
	}

	public String toString() {
		if(mapping != null)
			return mapping.toString();
		else
			return "root";
	}

	public int compareTo(CloneStructureNode other) {
		return this.mapping.compareTo(other.mapping);
	}
}
