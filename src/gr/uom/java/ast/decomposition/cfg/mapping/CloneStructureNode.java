package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.cfg.CFGBranchIfNode;
import gr.uom.java.ast.decomposition.cfg.PDGNode;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.IfStatement;

public class CloneStructureNode implements Comparable<CloneStructureNode> {
	private CloneStructureNode parent;
	private NodeMapping mapping;
	private Set<CloneStructureNode> children;
	
	public CloneStructureNode(NodeMapping mapping) {
		this.mapping = mapping;
		this.children = new TreeSet<CloneStructureNode>();
	}

	public void setParent(CloneStructureNode parent) {
		this.parent = parent;
		parent.children.add(this);
	}

	public boolean isElseIf() {
		if(this.getMapping() instanceof PDGNodeMapping) {
			PDGNodeMapping thisMapping = (PDGNodeMapping)this.getMapping();
			PDGNodeMapping symmetrical = thisMapping.getSymmetricalIfNodePair();
			if(symmetrical != null && symmetrical.equals(parent.getMapping()))
				return true;
			PDGNode childNodeG1 = thisMapping.getNodeG1();
			PDGNode childNodeG2 = thisMapping.getNodeG2();
			if(thisMapping.isFalseControlDependent() &&
					(childNodeG1.getASTStatement() instanceof IfStatement && childNodeG1.getASTStatement().getParent() instanceof IfStatement) &&
					(childNodeG2.getASTStatement() instanceof IfStatement && childNodeG2.getASTStatement().getParent() instanceof IfStatement))
				return true;
		}
		else if(this.getMapping() instanceof PDGNodeGap) {
			PDGNodeGap nodeGap = (PDGNodeGap)this.getMapping();
			PDGNode nodeG1 = nodeGap.getNodeG1();
			PDGNode nodeG2 = nodeGap.getNodeG2();
			if(nodeGap.isFalseControlDependent() &&
					((nodeG1 != null && nodeG1.getASTStatement() instanceof IfStatement && nodeG1.getASTStatement().getParent() instanceof IfStatement) ||
					(nodeG2 != null && nodeG2.getASTStatement() instanceof IfStatement && nodeG2.getASTStatement().getParent() instanceof IfStatement)))
				return true;
		}
		return false;
	}

	public CloneStructureNode findNode(PDGNode node) {
		if(mapping != null && (node.equals(mapping.getNodeG1()) || node.equals(mapping.getNodeG2()))) {
			return this;
		}
		else {
			for(CloneStructureNode child : children) {
				CloneStructureNode cloneStructureNode = child.findNode(node);
				if(cloneStructureNode != null)
					return cloneStructureNode;
			}
		}
		return null;
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
			if(gap.isFalseControlDependent()) {
				//find the else node
				for(CloneStructureNode child : controlParent.children) {
					if(child.getMapping() instanceof PDGElseMapping) {
						gapNode.setParent(child);
						break;
					}
				}
			}
			else {
				gapNode.setParent(controlParent);
			}
		}
		else {
			gapNode.setParent(this);
		}
	}

	public void addChild(CloneStructureNode node) {
		if(this.getMapping() instanceof PDGElseMapping) {
			node.setParent(this);
		}
		else {
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
	}
	
	private CloneStructureNode containsChildSymmetricalToNode(CloneStructureNode other) {
		if(other.getMapping() instanceof PDGElseMapping)
			return null;
		PDGNodeMapping otherNodeMapping = (PDGNodeMapping)other.getMapping();
		if(otherNodeMapping.getSymmetricalIfNodePair() != null) {
			for(CloneStructureNode child : getDescendants()) {
				if(child.getMapping() instanceof PDGNodeMapping) {
					PDGNodeMapping childNodeMapping = (PDGNodeMapping)child.getMapping();
					if(childNodeMapping.getSymmetricalIfNodePair() != null) {
						if(otherNodeMapping.getSymmetricalIfNodePair().equals(childNodeMapping))
							return child;
					}
				}
			}
		}
		return null;
	}
	
	private CloneStructureNode containsControlChildOfNode(CloneStructureNode other) {
		if(other.getMapping() instanceof PDGElseMapping) {
			for(CloneStructureNode otherChild : other.children) {
				CloneStructureNode controlChild = this.containsControlChildOfNode(otherChild);
				if(controlChild != null)
					return controlChild;
			}
		}
		else {
			PDGNodeMapping otherNodeMapping = (PDGNodeMapping)other.getMapping();
			if(otherNodeMapping.getNodeG1().getCFGNode() instanceof CFGBranchIfNode &&
					otherNodeMapping.getNodeG2().getCFGNode() instanceof CFGBranchIfNode) {
				for(CloneStructureNode child : getDescendants()) {
					if(child.getMapping() instanceof PDGNodeMapping) {
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
			}
		}
		return null;
	}
	
	private CloneStructureNode containsControlParentOfNode(CloneStructureNode other) {
		if(other.getMapping() instanceof PDGElseMapping) {
			for(CloneStructureNode otherChild : other.children) {
				CloneStructureNode controlParent = this.containsControlParentOfNode(otherChild);
				if(controlParent != null)
					return controlParent;
			}
		}
		else {
			PDGNodeMapping otherNodeMapping = (PDGNodeMapping)other.getMapping();
			if(otherNodeMapping.getNodeG1().getCFGNode() instanceof CFGBranchIfNode &&
					otherNodeMapping.getNodeG2().getCFGNode() instanceof CFGBranchIfNode) {
				PDGNode otherNodeG1ControlParent = otherNodeMapping.getNodeG1().getControlDependenceParent();
				PDGNode otherNodeG2ControlParent = otherNodeMapping.getNodeG2().getControlDependenceParent();
				for(CloneStructureNode child : getDescendants()) {
					if(child.getMapping() instanceof PDGNodeMapping) {
						PDGNodeMapping childNodeMapping = (PDGNodeMapping)child.getMapping();
						if(childNodeMapping.getNodeG1().getCFGNode() instanceof CFGBranchIfNode &&
								childNodeMapping.getNodeG2().getCFGNode() instanceof CFGBranchIfNode) {
							if(childNodeMapping.getNodeG1().equals(otherNodeG1ControlParent) && 
									childNodeMapping.getNodeG2().equals(otherNodeG2ControlParent))
								return child;
						}
					}
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

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof CloneStructureNode) {
			CloneStructureNode other = (CloneStructureNode)o;
			if(this.getMapping() != null && other.getMapping() != null) {
				return this.getMapping().equals(other.getMapping());
			}
			if(this.getMapping() == null && other.getMapping() == null) {
				return true;
			}
		}
		return false;
	}
	
	public int compareTo(CloneStructureNode other) {
		return this.mapping.compareTo(other.mapping);
	}
}
