package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;

import java.util.ArrayList;
import java.util.List;

public class PDGElseMapping extends IdBasedMapping {
	private PDGNodeMapping symmetricalIfNodePair;
	private volatile int hashCode = 0;
	
	public PDGElseMapping(double id1, double id2) {
		super(id1, id2);
	}
	
	public PDGNode getNodeG1() {
		return null;
	}

	public PDGNode getNodeG2() {
		return null;
	}

	public List<ASTNodeDifference> getNodeDifferences() {
		return new ArrayList<ASTNodeDifference>();
	}

	public boolean isAdvancedMatch() {
		return false;
	}

	public PDGNodeMapping getSymmetricalIfNodePair() {
		return symmetricalIfNodePair;
	}

	public void setSymmetricalIfNodePair(PDGNodeMapping symmetricalIfNodePair) {
		this.symmetricalIfNodePair = symmetricalIfNodePair;
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof PDGElseMapping) {
			PDGElseMapping mapping = (PDGElseMapping)o;
			return this.id1 == mapping.id1 &&
					this.id2 == mapping.id2;
		}
		return false;
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			long id1 = Double.doubleToLongBits(this.id1);
			result = 37*result + (int) (id1 ^ (id1 >>> 32));
			long id2 = Double.doubleToLongBits(this.id2);
			result = 37*result + (int) (id2 ^ (id2 >>> 32));
			hashCode = result;
		}
		return hashCode;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(id1 + "\telse\n");
		sb.append(id2 + "\telse\n");
		return sb.toString();
	}
}
