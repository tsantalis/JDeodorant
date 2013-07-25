package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.List;

import gr.uom.java.ast.decomposition.ASTNodeDifference;
import gr.uom.java.ast.decomposition.cfg.PDGNode;

public abstract class NodeMapping implements Comparable<NodeMapping> {
	
	public abstract PDGNode getNodeG1();
	public abstract PDGNode getNodeG2();
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
		if(this instanceof IdBasedMapping && other instanceof IdBasedMapping) {
			IdBasedMapping thisMapping = (IdBasedMapping)this;
			IdBasedMapping otherMapping = (IdBasedMapping)other;
			return thisMapping.compareTo(otherMapping);
		}
		if(this instanceof PDGNodeGap && other instanceof PDGNodeGap) {
			PDGNodeGap thisGap = (PDGNodeGap)this;
			PDGNodeGap otherGap = (PDGNodeGap)other;
			return thisGap.compareTo(otherGap);
		}
		if(this instanceof IdBasedMapping && !(other instanceof IdBasedMapping)) {
			IdBasedMapping thisMapping = (IdBasedMapping)this;
			if(other instanceof PDGNodeGap) {
				PDGNodeGap otherGap = (PDGNodeGap)other;
				double id1 = otherGap.getNodeG1() != null ? thisMapping.getId1() : thisMapping.getId2();
				double id2 = otherGap.getNodeG1() != null ? otherGap.getNodeG1().getId() : otherGap.getNodeG2().getId();
				if(id1 == id2)
					return -1;
				else
					return Double.compare(id1, id2);
			}
		}
		if(other instanceof IdBasedMapping && !(this instanceof IdBasedMapping)) {
			IdBasedMapping otherMapping = (IdBasedMapping)other;
			if(this instanceof PDGNodeGap) {
				PDGNodeGap thisGap = (PDGNodeGap)this;
				double id1 = thisGap.getNodeG1() != null ? thisGap.getNodeG1().getId() : thisGap.getNodeG2().getId();
				double id2 = thisGap.getNodeG1() != null ? otherMapping.getId1() : otherMapping.getId2();
				if(id1 == id2)
					return -1;
				else
					return Double.compare(id1, id2);
			}
		}
		return 0;
	}
}
