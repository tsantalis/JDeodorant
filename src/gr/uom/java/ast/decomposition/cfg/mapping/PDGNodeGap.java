package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.ast.decomposition.ASTNodeDifference;
import gr.uom.java.ast.decomposition.cfg.PDGNode;

public class PDGNodeGap extends NodeMapping {
	private volatile int hashCode = 0;
	
	public PDGNodeGap(PDGNode nodeG1, PDGNode nodeG2) {
		super(nodeG1, nodeG2);
	}

	public List<ASTNodeDifference> getNodeDifferences() {
		return new ArrayList<ASTNodeDifference>();
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof PDGNodeGap) {
			PDGNodeGap gap = (PDGNodeGap)o;
			if(this.nodeG1 == null && gap.nodeG1 == null)
				return this.nodeG2.equals(gap.nodeG2);
			if(this.nodeG2 == null && gap.nodeG2 == null)
				return this.nodeG1.equals(gap.nodeG1);
		}
		return false;
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 37*result + (nodeG1 == null ? 0 : nodeG1.hashCode());
			result = 37*result + (nodeG2 == null ? 0 : nodeG2.hashCode());
			hashCode = result;
		}
		return hashCode;
	}

	public int compareTo(PDGNodeGap other) {
		if(this.nodeG1 != null && other.nodeG1 != null)
			return Integer.compare(this.nodeG1.getId(), other.nodeG1.getId());
		if(this.nodeG2 != null && other.nodeG2 != null)
			return Integer.compare(this.nodeG2.getId(), other.nodeG2.getId());
		
		if(this.nodeG1 != null && other.nodeG2 != null) {
			int id1 = this.nodeG1.getId();
			int id2 = other.nodeG2.getId();
			if(id1 == id2)
				return -1;
			else
				return Integer.compare(id1, id2);
		}
		if(other.nodeG1 != null && this.nodeG2 != null) {
			int id1 = other.nodeG1.getId();
			int id2 = this.nodeG2.getId();
			if(id1 == id2)
				return -1;
			else
				return Integer.compare(id1, id2);
		}
		return 0;
	}
}
