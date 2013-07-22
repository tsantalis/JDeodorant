package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.List;

import gr.uom.java.ast.decomposition.ASTNodeDifference;
import gr.uom.java.ast.decomposition.cfg.PDGNode;

public abstract class NodeMapping implements Comparable<NodeMapping> {
	protected PDGNode nodeG1;
	protected PDGNode nodeG2;
	
	public NodeMapping(PDGNode nodeG1, PDGNode nodeG2) {
		this.nodeG1 = nodeG1;
		this.nodeG2 = nodeG2;
	}
	
	public PDGNode getNodeG1() {
		return nodeG1;
	}

	public PDGNode getNodeG2() {
		return nodeG2;
	}

	public abstract List<ASTNodeDifference> getNodeDifferences();

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(this instanceof PDGNodeMapping && o instanceof PDGNodeMapping) {
			PDGNodeMapping thisMapping = (PDGNodeMapping)this;
			PDGNodeMapping otherMapping = (PDGNodeMapping)o;
			return thisMapping.equals(otherMapping);
		}
		if(this instanceof PDGElseMapping && o instanceof PDGElseMapping) {
			PDGElseMapping thisMapping = (PDGElseMapping)this;
			PDGElseMapping otherMapping = (PDGElseMapping)o;
			return thisMapping.equals(otherMapping);
		}
		if(this instanceof PDGNodeGap && o instanceof PDGNodeGap) {
			PDGNodeGap thisMapping = (PDGNodeGap)this;
			PDGNodeGap otherMapping = (PDGNodeGap)o;
			return thisMapping.equals(otherMapping);
		}
		return false;
	}

	public int compareTo(NodeMapping other) {
		if(this instanceof PDGNodeMapping && other instanceof PDGNodeMapping) {
			PDGNodeMapping thisMapping = (PDGNodeMapping)this;
			PDGNodeMapping otherMapping = (PDGNodeMapping)other;
			return thisMapping.compareTo(otherMapping);
		}
		if(this instanceof PDGNodeGap && other instanceof PDGNodeGap) {
			PDGNodeGap thisGap = (PDGNodeGap)this;
			PDGNodeGap otherGap = (PDGNodeGap)other;
			return thisGap.compareTo(otherGap);
		}
		if(this instanceof PDGElseMapping && other instanceof PDGElseMapping) {
			PDGElseMapping thisElse = (PDGElseMapping)this;
			PDGElseMapping otherElse = (PDGElseMapping)other;
			return thisElse.compareTo(otherElse);
		}
		if(this instanceof PDGNodeMapping && !(other instanceof PDGNodeMapping)) {
			PDGNodeMapping thisMapping = (PDGNodeMapping)this;
			if(other instanceof PDGNodeGap) {
				PDGNodeGap otherGap = (PDGNodeGap)other;
				int id1 = otherGap.getNodeG1() != null ? thisMapping.getNodeG1().getId() : thisMapping.getNodeG2().getId();
				int id2 = otherGap.getNodeG1() != null ? otherGap.getNodeG1().getId() : otherGap.getNodeG2().getId();
				if(id1 == id2)
					return -1;
				else
					return Integer.compare(id1, id2);
			}
			if(other instanceof PDGElseMapping) {
				PDGElseMapping otherElseMapping = (PDGElseMapping)other;
				return Double.compare(thisMapping.getId1(), otherElseMapping.getId1());
			}
		}
		if(other instanceof PDGNodeMapping && !(this instanceof PDGNodeMapping)) {
			PDGNodeMapping otherMapping = (PDGNodeMapping)other;
			if(this instanceof PDGNodeGap) {
				PDGNodeGap thisGap = (PDGNodeGap)this;
				int id1 = thisGap.getNodeG1() != null ? thisGap.getNodeG1().getId() : thisGap.getNodeG2().getId();
				int id2 = thisGap.getNodeG1() != null ? otherMapping.getNodeG1().getId() : otherMapping.getNodeG2().getId();
				if(id1 == id2)
					return -1;
				else
					return Integer.compare(id1, id2);
			}
			if(this instanceof PDGElseMapping) {
				PDGElseMapping thisElseMapping = (PDGElseMapping)this;
				return Double.compare(thisElseMapping.getId1(), otherMapping.getId1());
			}
		}
		return 0;
	}
}
