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
		this.children.add(node);
	}

	public PDGNodeMapping getMapping() {
		return mapping;
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
