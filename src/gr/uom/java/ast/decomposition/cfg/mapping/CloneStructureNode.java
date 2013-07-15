package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.cfg.CFGBranchIfNode;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PDGTryNode;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;

public class CloneStructureNode implements Comparable<CloneStructureNode> {
	private CloneStructureNode parent;
	private NodeMapping mapping;
	private Set<CloneStructureNode> children;
	
	public CloneStructureNode(NodeMapping mapping) {
		this.mapping = mapping;
		this.children = new TreeSet<CloneStructureNode>();
	}

	public CloneStructureNode(CloneStructureNode parent, NodeMapping mapping) {
		this.parent = parent;
		this.mapping = mapping;
		this.children = new TreeSet<CloneStructureNode>();
	}

	private void setParent(CloneStructureNode parent) {
		this.parent = parent;
		parent.children.add(this);
	}

	public boolean isNestedUnderElse() {
		if(this.getMapping() instanceof PDGNodeMapping) {
			PDGNodeMapping thisMapping = (PDGNodeMapping)this.getMapping();
			PDGNodeMapping symmetrical = thisMapping.getSymmetricalIfNodePair();
			if(symmetrical != null && symmetrical.equals(parent.getMapping()))
				return true;
			PDGNode childNodeG1 = thisMapping.getNodeG1();
			PDGNode childNodeG2 = thisMapping.getNodeG2();
			if(childNodeG1 instanceof PDGTryNode && childNodeG2 instanceof PDGTryNode) {
				return isNestedUnderElse((PDGTryNode)childNodeG1) && isNestedUnderElse((PDGTryNode)childNodeG2);
			}
			else {
				PDGControlDependence controlDependence1 = childNodeG1.getIncomingControlDependence();
				PDGControlDependence controlDependence2 = childNodeG2.getIncomingControlDependence();
				return controlDependence1.isFalseControlDependence() && controlDependence2.isFalseControlDependence();
			}
		}
		else {
			PDGNode childNodeG1 = this.getMapping().getNodeG1();
			PDGNode childNodeG2 = this.getMapping().getNodeG2();
			if(childNodeG1 != null) {
				if(childNodeG1 instanceof PDGTryNode) {
					return isNestedUnderElse((PDGTryNode)childNodeG1);
				}
				else {
					PDGControlDependence controlDependence1 = childNodeG1.getIncomingControlDependence();
					return controlDependence1.isFalseControlDependence();
				}
			}
			if(childNodeG2 != null) {
				if(childNodeG2 instanceof PDGTryNode) {
					return isNestedUnderElse((PDGTryNode)childNodeG2);
				}
				else {
					PDGControlDependence controlDependence2 = childNodeG2.getIncomingControlDependence();
					return controlDependence2.isFalseControlDependence();
				}
			}
			return false;
		}
	}

	private boolean isNestedUnderElse(PDGTryNode tryNode) {
		Statement statement = tryNode.getASTStatement();
		if(statement.getParent() instanceof Block) {
			Block block = (Block)statement.getParent();
			if(block.getParent() instanceof IfStatement) {
				IfStatement ifStatement = (IfStatement)block.getParent();
				if(ifStatement.getElseStatement() != null && ifStatement.getElseStatement().equals(block))
					return true;
			}
		}
		else if(statement.getParent() instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement)statement.getParent();
			if(ifStatement.getElseStatement() != null && ifStatement.getElseStatement().equals(statement))
				return true;
		}
		return false;
	}

	public void addGapChild(CloneStructureNode gapNode) {
		PDGNodeGap gap = (PDGNodeGap)gapNode.getMapping();
		PDGNode nodeG1ControlParent = gap.getNodeG1() != null ? gap.getNodeG1().getControlDependenceParent() : null;
		PDGNode nodeG2ControlParent = gap.getNodeG2() != null ? gap.getNodeG2().getControlDependenceParent() : null;
		CloneStructureNode controlParent = null;
		for(CloneStructureNode node : getDescendants()) {
			NodeMapping nodeMapping = node.getMapping();
			if(nodeG1ControlParent != null && nodeMapping.getNodeG1() != null && nodeMapping.getNodeG1().equals(nodeG1ControlParent)) {
				controlParent = node;
				break;
			}
			if(nodeG2ControlParent != null && nodeMapping.getNodeG2() != null && nodeMapping.getNodeG2().equals(nodeG2ControlParent)) {
				controlParent = node;
				break;
			}
		}
		if(controlParent != null) {
			gapNode.setParent(controlParent);
		}
		else {
			gapNode.setParent(this);
		}
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
		PDGNodeMapping otherNodeMapping = (PDGNodeMapping)other.getMapping();
		if(otherNodeMapping.getSymmetricalIfNodePair() != null) {
			for(CloneStructureNode child : getDescendants()) {
				PDGNodeMapping childNodeMapping = (PDGNodeMapping)child.getMapping();
				if(childNodeMapping.getSymmetricalIfNodePair() != null) {
					if(otherNodeMapping.getSymmetricalIfNodePair().equals(childNodeMapping))
						return child;
				}
			}
		}
		return null;
	}
	
	private CloneStructureNode containsControlChildOfNode(CloneStructureNode other) {
		PDGNodeMapping otherNodeMapping = (PDGNodeMapping)other.getMapping();
		if(otherNodeMapping.getNodeG1().getCFGNode() instanceof CFGBranchIfNode &&
				otherNodeMapping.getNodeG2().getCFGNode() instanceof CFGBranchIfNode) {
			for(CloneStructureNode child : getDescendants()) {
				PDGNodeMapping childNodeMapping = (PDGNodeMapping)child.getMapping();
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
		PDGNodeMapping otherNodeMapping = (PDGNodeMapping)other.getMapping();
		if(otherNodeMapping.getNodeG1().getCFGNode() instanceof CFGBranchIfNode &&
				otherNodeMapping.getNodeG2().getCFGNode() instanceof CFGBranchIfNode) {
			PDGNode otherNodeG1ControlParent = otherNodeMapping.getNodeG1().getControlDependenceParent();
			PDGNode otherNodeG2ControlParent = otherNodeMapping.getNodeG2().getControlDependenceParent();
			for(CloneStructureNode child : getDescendants()) {
				PDGNodeMapping childNodeMapping = (PDGNodeMapping)child.getMapping();
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

	public NodeMapping getMapping() {
		return mapping;
	}

	public Set<CloneStructureNode> getDescendants() {
		Set<CloneStructureNode> descendants = new LinkedHashSet<CloneStructureNode>();
		descendants.addAll(children);
		for(CloneStructureNode child : this.children) {
			descendants.addAll(child.getDescendants());
		}
		return descendants;
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
