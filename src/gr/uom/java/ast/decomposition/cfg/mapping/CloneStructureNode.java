package gr.uom.java.ast.decomposition.cfg.mapping;

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

	public void setParent(CloneStructureNode parent) {
		this.parent = parent;
	}

	public void addChild(CloneStructureNode node) {
		CloneStructureNode symmetricalChild = this.containsChildSymmetricalToNode(node);
		if(symmetricalChild != null) {
			symmetricalChild.children.add(node);
			node.setParent(symmetricalChild);
		}
		else {
			this.children.add(node);
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
