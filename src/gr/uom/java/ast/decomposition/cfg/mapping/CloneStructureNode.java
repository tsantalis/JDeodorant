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

	public CloneStructureNode findNodeG1(PDGNode nodeG1) {
		if(mapping != null && nodeG1.equals(mapping.getNodeG1())) {
			return this;
		}
		else {
			for(CloneStructureNode child : children) {
				CloneStructureNode cloneStructureNode = child.findNodeG1(nodeG1);
				if(cloneStructureNode != null)
					return cloneStructureNode;
			}
		}
		return null;
	}

	public CloneStructureNode findNodeG2(PDGNode nodeG2) {
		if(mapping != null && nodeG2.equals(mapping.getNodeG2())) {
			return this;
		}
		else {
			for(CloneStructureNode child : children) {
				CloneStructureNode cloneStructureNode = child.findNodeG2(nodeG2);
				if(cloneStructureNode != null)
					return cloneStructureNode;
			}
		}
		return null;
	}

	public boolean isGapNodeG1InAdditionalMatches(PDGNode nodeG1) {
		if(mapping != null && mapping instanceof PDGNodeMapping && ((PDGNodeMapping)mapping).containsAdditionallyMatchedFragment1(nodeG1)) {
			return true;
		}
		else {
			for(CloneStructureNode child : children) {
				boolean gapNodeInAdditionalMatches = child.isGapNodeG1InAdditionalMatches(nodeG1);
				if(gapNodeInAdditionalMatches)
					return true;
			}
		}
		return false;
	}

	public boolean isGapNodeG2InAdditionalMatches(PDGNode nodeG2) {
		if(mapping != null && mapping instanceof PDGNodeMapping && ((PDGNodeMapping)mapping).containsAdditionallyMatchedFragment2(nodeG2)) {
			return true;
		}
		else {
			for(CloneStructureNode child : children) {
				boolean gapNodeInAdditionalMatches = child.isGapNodeG2InAdditionalMatches(nodeG2);
				if(gapNodeInAdditionalMatches)
					return true;
			}
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
			if(gap.isFalseControlDependent()) {
				//find the else node
				for(CloneStructureNode child : controlParent.children) {
					if(child.getMapping() instanceof PDGElseMapping || child.getMapping() instanceof PDGElseGap) {
						gapNode.setParent(child);
						break;
					}
				}
				if(controlParent.getMapping() instanceof PDGNodeMapping) {
					PDGNodeMapping pdgNodeMapping = (PDGNodeMapping)controlParent.getMapping();
					PDGNodeMapping symmetricalPDGNodeMapping = pdgNodeMapping.getSymmetricalIfNodePair();
					if(symmetricalPDGNodeMapping != null && gapNode.parent == null) {
						//find the child of controlParent corresponding to the symmetricalPDGNodeMapping
						CloneStructureNode symmetricalIfChild = null;
						for(CloneStructureNode child : controlParent.children) {
							if(child.getMapping().equals(symmetricalPDGNodeMapping)) {
								symmetricalIfChild = child;
								break;
							}
						}
						if(symmetricalIfChild != null) {
							for(CloneStructureNode child : symmetricalIfChild.children) {
								if(child.getMapping() instanceof PDGElseMapping || child.getMapping() instanceof PDGElseGap) {
									gapNode.setParent(child);
									break;
								}
							}
						}
					}
				}
				//else node is not found
				if(gapNode.parent == null) {
					//check whether gapNode is an if statement
					if((gap.getNodeG1() != null && gap.getNodeG1().getASTStatement() instanceof IfStatement) ||
							(gap.getNodeG2() != null && gap.getNodeG2().getASTStatement() instanceof IfStatement)) {
						gapNode.setParent(controlParent);
					}
					else {
						//create a new else gap and add gapNode under it
						PDGElseGap elseGap = null;
						if(gap.getNodeG1() != null) {
							double elseGapId1 = gap.getId1() - 0.5;
							elseGap = new PDGElseGap(elseGapId1, 0, gap.isAdvancedMatch());
						}
						if(gap.getNodeG2() != null) {
							double elseGapId2 = gap.getId2() - 0.5;
							elseGap = new PDGElseGap(0, elseGapId2, gap.isAdvancedMatch());
						}
						if(elseGap != null) {
							CloneStructureNode elseNode = new CloneStructureNode(elseGap);
							elseNode.setParent(controlParent);
							gapNode.setParent(elseNode);
						}
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
				PDGNode otherSymmetricalNodeG1ControlParent = null;
				PDGNode otherSymmetricalNodeG2ControlParent = null;
				if(otherNodeMapping.getSymmetricalIfNodePair() != null) {
					otherSymmetricalNodeG1ControlParent = otherNodeMapping.getSymmetricalIfNodePair().getNodeG1().getControlDependenceParent();
					otherSymmetricalNodeG2ControlParent = otherNodeMapping.getSymmetricalIfNodePair().getNodeG2().getControlDependenceParent();
				}
				CloneStructureNode nodeG1ControlParent = null;
				CloneStructureNode nodeG2ControlParent = null;
				CloneStructureNode lastControlParent = null;
				for(CloneStructureNode child : getDescendants()) {
					if(child.getMapping() instanceof PDGNodeMapping) {
						PDGNodeMapping childNodeMapping = (PDGNodeMapping)child.getMapping();
						if(childNodeMapping.getNodeG1().getCFGNode() instanceof CFGBranchIfNode &&
								childNodeMapping.getNodeG2().getCFGNode() instanceof CFGBranchIfNode) {
							if(childNodeMapping.getNodeG1().equals(otherNodeG1ControlParent) && 
									childNodeMapping.getNodeG2().equals(otherNodeG2ControlParent)) {
								return child;
							}
							if(childNodeMapping.getNodeG1().equals(otherNodeG1ControlParent)) {
								nodeG1ControlParent = child;
								lastControlParent = child;
							}
							if(childNodeMapping.getNodeG2().equals(otherNodeG2ControlParent)) {
								nodeG2ControlParent = child;
								lastControlParent = child;
							}
							if(otherSymmetricalNodeG1ControlParent != null && otherSymmetricalNodeG2ControlParent != null) {
								if(childNodeMapping.getNodeG1().equals(otherNodeG1ControlParent) && childNodeMapping.getNodeG2().equals(otherSymmetricalNodeG2ControlParent) ||
										childNodeMapping.getNodeG1().equals(otherSymmetricalNodeG1ControlParent) && childNodeMapping.getNodeG2().equals(otherNodeG2ControlParent)) {
									return child;
								}
							}
						}
					}
				}
				if(nodeG1ControlParent != null && nodeG2ControlParent != null) {
					return lastControlParent;
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
