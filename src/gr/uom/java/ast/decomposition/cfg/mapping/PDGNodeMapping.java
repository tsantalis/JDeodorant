package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.List;

import org.eclipse.jdt.core.ITypeRoot;

import gr.uom.java.ast.decomposition.ASTNodeDifference;
import gr.uom.java.ast.decomposition.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.Difference;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CompositeVariable;
import gr.uom.java.ast.decomposition.cfg.PDGNode;

public class PDGNodeMapping implements Comparable<PDGNodeMapping> {
	private PDGNode nodeG1;
	private PDGNode nodeG2;
	private ASTNodeMatcher matcher;
	private List<ASTNodeDifference> nodeDifferences;
	private boolean symmetricalIfNodePair;
	private volatile int hashCode = 0;
	
	public PDGNodeMapping(PDGNode nodeG1, PDGNode nodeG2, ASTNodeMatcher matcher) {
		this.nodeG1 = nodeG1;
		this.nodeG2 = nodeG2;
		this.matcher = matcher;
		this.nodeDifferences = matcher.getDifferences();
	}
	
	public PDGNode getNodeG1() {
		return nodeG1;
	}

	public PDGNode getNodeG2() {
		return nodeG2;
	}

	public List<ASTNodeDifference> getNodeDifferences() {
		return nodeDifferences;
	}

	public ITypeRoot getTypeRoot1() {
		return matcher.getTypeRoot1();
	}

	public ITypeRoot getTypeRoot2() {
		return matcher.getTypeRoot2();
	}

	public int getDifferenceCount() {
		int count = 0;
		for(ASTNodeDifference nodeDifference : nodeDifferences) {
			count += nodeDifference.getDifferences().size();
		}
		return count;
	}

	public boolean isSymmetricalIfNodePair() {
		return symmetricalIfNodePair;
	}

	public void setSymmetricalIfNodePair(boolean symmetricalIfNodePair) {
		this.symmetricalIfNodePair = symmetricalIfNodePair;
	}

	public boolean matchingVariableDifference(AbstractVariable variable1, AbstractVariable variable2) {
		if(variable1.getClass() == variable2.getClass()) {
			String rightPartVariable1 = null;
			String rightPartVariable2 = null;
			if(variable1 instanceof CompositeVariable) {
				CompositeVariable comp1 = (CompositeVariable)variable1;
				CompositeVariable comp2 = (CompositeVariable)variable2;
				rightPartVariable1 = comp1.getRightPart().toString();
				rightPartVariable2 = comp2.getRightPart().toString();
			}
			boolean equalRightPart = false;
			if(rightPartVariable1 != null && rightPartVariable2 != null) {
				equalRightPart = rightPartVariable1.equals(rightPartVariable2);
			}
			else {
				equalRightPart = true;
			}
			for(ASTNodeDifference nodeDifference : nodeDifferences)
			{
				List<Difference> differences = nodeDifference.getDifferences();
				for(Difference difference : differences) {
					if(equalRightPart && difference.getFirstValue().equals(variable1.getVariableName()) &&
							difference.getSecondValue().equals(variable2.getVariableName())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof PDGNodeMapping) {
			PDGNodeMapping mapping = (PDGNodeMapping)o;
			return this.nodeG1.equals(mapping.nodeG1) &&
					this.nodeG2.equals(mapping.nodeG2);
		}
		return false;
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 37*result + nodeG1.hashCode();
			result = 37*result + nodeG2.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(nodeG1);
		sb.append(nodeG2);
		for(ASTNodeDifference nodeDifference : nodeDifferences)
		{
			sb.append(nodeDifference.toString());
		}
		return sb.toString();
	}

	public int compareTo(PDGNodeMapping other) {
		return Integer.compare(this.nodeG1.getId(), other.nodeG1.getId());
	}
}
