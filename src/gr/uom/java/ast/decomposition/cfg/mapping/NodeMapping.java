package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;

import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.CompositeStatementObject;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.PreconditionViolation;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.decomposition.matching.DifferenceType;

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

	public List<ASTNodeDifference> getNonOverlappingNodeDifferences() {
		List<ASTNodeDifference> nodeDifferences = getNodeDifferences();
		List<ASTNodeDifference> nonOverlappingDifferences = new ArrayList<ASTNodeDifference>(nodeDifferences);
		for(int i=0; i<nodeDifferences.size(); i++) {
			ASTNodeDifference nodeDifferenceI = nodeDifferences.get(i);
			for(int j=i+1; j<nodeDifferences.size(); j++) {
				ASTNodeDifference nodeDifferenceJ = nodeDifferences.get(j);
				if(nodeDifferenceI.isParentNodeDifferenceOf(nodeDifferenceJ)) {
					nonOverlappingDifferences.remove(nodeDifferenceJ);
				}
				else if(nodeDifferenceJ.isParentNodeDifferenceOf(nodeDifferenceI)) {
					nonOverlappingDifferences.remove(nodeDifferenceI);
				}
			}
		}
		return nonOverlappingDifferences;
	}

	public boolean isDifferenceInConditionalExpressionOfAdvancedLoopMatch(ASTNodeDifference difference) {
		if(isAdvancedMatch() && getNodeG1() != null && getNodeG2() != null && !difference.containsDifferenceType(DifferenceType.IF_ELSE_SYMMETRICAL_MATCH)) {
			AbstractStatement statement1 = getNodeG1().getStatement();
			AbstractStatement statement2 = getNodeG2().getStatement();
			if(statement1 instanceof CompositeStatementObject && statement2 instanceof CompositeStatementObject) {
				CompositeStatementObject composite1 = (CompositeStatementObject)statement1;	
				boolean foundInComposite1 = false;
				for(AbstractExpression expression1 : composite1.getExpressions()) {
					AbstractExpression differenceExpression1 = difference.getExpression1();
					if(isExpressionWithinExpression(differenceExpression1.getExpression(), expression1.getExpression())) {
						foundInComposite1 = true;
						break;
					}
				}
				CompositeStatementObject composite2 = (CompositeStatementObject)statement2;
				boolean foundInComposite2 = false;
				for(AbstractExpression expression2 : composite2.getExpressions()) {
					AbstractExpression differenceExpression2 = difference.getExpression2();
					if(isExpressionWithinExpression(differenceExpression2.getExpression(), expression2.getExpression())) {
						foundInComposite2 = true;
						break;
					}
				}
				if(foundInComposite1 && foundInComposite2)
					return true;
			}
		}
		return false;
	}
	
	private boolean isExpressionWithinExpression(ASTNode expression, Expression parentExpression) {
		if(expression.equals(parentExpression))
			return true;
		ASTNode parent = expression.getParent();
		if(!(parent instanceof Statement))
			return isExpressionWithinExpression(parent, parentExpression);
		else
			return false;
	}

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
