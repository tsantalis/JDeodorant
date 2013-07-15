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
		if(this instanceof PDGNodeMapping && other instanceof PDGNodeGap) {
			PDGNodeMapping thisMapping = (PDGNodeMapping)this;
			PDGNodeGap otherGap = (PDGNodeGap)other;
			int id1 = otherGap.getNodeG1() != null ? thisMapping.getNodeG1().getId() : thisMapping.getNodeG2().getId();
			int id2 = otherGap.getNodeG1() != null ? otherGap.getNodeG1().getId() : otherGap.getNodeG2().getId();
			if(id1 == id2)
				return -1;
			else
				return Integer.compare(id1, id2);
		}
		if(other instanceof PDGNodeMapping && this instanceof PDGNodeGap) {
			PDGNodeGap thisGap = (PDGNodeGap)this;
			PDGNodeMapping otherMapping = (PDGNodeMapping)other;
			int id1 = thisGap.getNodeG1() != null ? thisGap.getNodeG1().getId() : thisGap.getNodeG2().getId();
			int id2 = thisGap.getNodeG1() != null ? otherMapping.getNodeG1().getId() : otherMapping.getNodeG2().getId();
			if(id1 == id2)
				return -1;
			else
				return Integer.compare(id1, id2);
		}
		return 0;
	}
}
