package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.PreconditionViolation;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;

public abstract class NodeMapping implements Comparable<NodeMapping> {
	private List<PreconditionViolation> preconditionViolations = new ArrayList<PreconditionViolation>();

	public List<PreconditionViolation> getPreconditionViolations() {
		return preconditionViolations;
	}

	public void addPreconditionViolation(PreconditionViolation violation) {
		preconditionViolations.add(violation);
	}
	
	public abstract PDGNode getNodeG1();
	public abstract PDGNode getNodeG2();
	public abstract List<ASTNodeDifference> getNodeDifferences();
	public abstract boolean isAdvancedMatch();

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
		if(this instanceof PDGElseGap && o instanceof PDGElseGap) {
			PDGElseGap thisMapping = (PDGElseGap)this;
			PDGElseGap otherMapping = (PDGElseGap)o;
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
		if(this instanceof IdBasedGap && other instanceof IdBasedGap) {
			IdBasedGap thisGap = (IdBasedGap)this;
			IdBasedGap otherGap = (IdBasedGap)other;
			return thisGap.compareTo(otherGap);
		}
		if(this instanceof IdBasedMapping && !(other instanceof IdBasedMapping)) {
			IdBasedMapping thisMapping = (IdBasedMapping)this;
			if(other instanceof IdBasedGap) {
				IdBasedGap otherGap = (IdBasedGap)other;
				double id1 = otherGap.getId1() != 0 ? thisMapping.getId1() : thisMapping.getId2();
				double id2 = otherGap.getId1() != 0 ? otherGap.getId1() : otherGap.getId2();
				if(id1 == id2)
					return -1;
				else
					return Double.compare(id1, id2);
			}
		}
		if(other instanceof IdBasedMapping && !(this instanceof IdBasedMapping)) {
			IdBasedMapping otherMapping = (IdBasedMapping)other;
			if(this instanceof IdBasedGap) {
				IdBasedGap thisGap = (IdBasedGap)this;
				double id1 = thisGap.getId1() != 0 ? thisGap.getId1() : thisGap.getId2();
				double id2 = thisGap.getId1() != 0 ? otherMapping.getId1() : otherMapping.getId2();
				if(id1 == id2)
					return -1;
				else
					return Double.compare(id1, id2);
			}
		}
		return 0;
	}
}
