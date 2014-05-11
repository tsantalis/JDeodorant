package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;

import java.util.ArrayList;
import java.util.List;

public class PDGElseGap extends IdBasedGap {
	private volatile int hashCode = 0;
	
	public PDGElseGap(double id1, double id2, boolean advancedMatch) {
		super(id1, id2, advancedMatch);
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

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof PDGElseGap) {
			PDGElseGap gap = (PDGElseGap)o;
			if(this.id1 == 0 && gap.id1 == 0)
				return this.id2 == gap.id2;
			if(this.id2 == 0 && gap.id2 == 0)
				return this.id1 == gap.id1;
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
		if(id1 != 0)
			sb.append(id1 + "\telse\n");
		else
			sb.append("\n");
		if(id2 != 0)
			sb.append(id2 + "\telse\n");
		else
			sb.append("\n");
		return sb.toString();
	}

}
