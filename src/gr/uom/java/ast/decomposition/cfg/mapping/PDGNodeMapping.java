package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;

import gr.uom.java.ast.decomposition.AbstractMethodFragment;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CompositeVariable;
import gr.uom.java.ast.decomposition.cfg.PDGBlockNode;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.matching.Difference;

public class PDGNodeMapping extends IdBasedMapping {
	private PDGNode nodeG1;
	private PDGNode nodeG2;
	private ASTNodeMatcher matcher;
	private List<ASTNodeDifference> nodeDifferences;
	private List<AbstractMethodFragment> additionallyMatchedFragments1;
	private List<AbstractMethodFragment> additionallyMatchedFragments2;
	private PDGNodeMapping symmetricalIfNodePair;
	private boolean symmetricalIfElse;
	private volatile int hashCode = 0;
	
	public PDGNodeMapping(PDGNode nodeG1, PDGNode nodeG2, ASTNodeMatcher matcher) {
		super(nodeG1.getId(), nodeG2.getId());
		this.nodeG1 = nodeG1;
		this.nodeG2 = nodeG2;
		this.matcher = matcher;
		this.nodeDifferences = matcher.getDifferences();
		this.additionallyMatchedFragments1 = matcher.getAdditionallyMatchedFragments1();
		this.additionallyMatchedFragments2 = matcher.getAdditionallyMatchedFragments2();
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

	public boolean isAdvancedMatch() {
		return additionallyMatchedFragments1.size() > 0 || additionallyMatchedFragments2.size() > 0 || symmetricalIfElse;
	}

	public List<AbstractMethodFragment> getAdditionallyMatchedFragments1() {
		return additionallyMatchedFragments1;
	}

	public List<AbstractMethodFragment> getAdditionallyMatchedFragments2() {
		return additionallyMatchedFragments2;
	}

	public boolean containsAdditionallyMatchedFragment1(PDGNode node) {
		for(AbstractMethodFragment fragment : additionallyMatchedFragments1) {
			if(fragment instanceof AbstractStatement) {
				AbstractStatement statement = (AbstractStatement)fragment;
				if(statement.getStatement().equals(node.getASTStatement())) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean containsAdditionallyMatchedFragment2(PDGNode node) {
		for(AbstractMethodFragment fragment : additionallyMatchedFragments2) {
			if(fragment instanceof AbstractStatement) {
				AbstractStatement statement = (AbstractStatement)fragment;
				if(statement.getStatement().equals(node.getASTStatement())) {
					return true;
				}
			}
		}
		return false;
	}

	public List<ASTNodeDifference> getNonOverlappingNodeDifferences() {
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

	public ITypeRoot getTypeRoot1() {
		return matcher.getTypeRoot1();
	}

	public ITypeRoot getTypeRoot2() {
		return matcher.getTypeRoot2();
	}

	public PDGNodeMapping getSymmetricalIfNodePair() {
		return symmetricalIfNodePair;
	}

	public void setSymmetricalIfNodePair(PDGNodeMapping symmetricalIfNodePair) {
		this.symmetricalIfNodePair = symmetricalIfNodePair;
	}

	public boolean isSymmetricalIfElse() {
		return symmetricalIfElse;
	}

	public void setSymmetricalIfElse(boolean symmetricalIfElse) {
		this.symmetricalIfElse = symmetricalIfElse;
	}

	public boolean isFalseControlDependent() {
		PDGControlDependence controlDependence1 = nodeG1.getIncomingControlDependence();
		PDGControlDependence controlDependence2 = nodeG2.getIncomingControlDependence();
		if(controlDependence1 != null && controlDependence2 != null)
			return controlDependence1.isFalseControlDependence() && controlDependence2.isFalseControlDependence();
		if(nodeG1 instanceof PDGBlockNode && nodeG2 instanceof PDGBlockNode)
			return isNestedUnderElse((PDGBlockNode)nodeG1) && isNestedUnderElse((PDGBlockNode)nodeG2);
		return false;
	}

	public boolean isNode1FalseControlDependent() {
		PDGControlDependence controlDependence1 = nodeG1.getIncomingControlDependence();
		if(controlDependence1 != null)
			return controlDependence1.isFalseControlDependence();
		if(nodeG1 instanceof PDGBlockNode)
			return isNestedUnderElse((PDGBlockNode)nodeG1);
		return false;
	}

	public boolean isNode2FalseControlDependent() {
		PDGControlDependence controlDependence2 = nodeG2.getIncomingControlDependence();
		if(controlDependence2 != null)
			return controlDependence2.isFalseControlDependence();
		if(nodeG2 instanceof PDGBlockNode)
			return isNestedUnderElse((PDGBlockNode)nodeG2);
		return false;
	}

	private boolean isNestedUnderElse(PDGBlockNode blockNode) {
		Statement statement = blockNode.getASTStatement();
		if(statement.getParent() instanceof Block) {
			Block block = (Block)statement.getParent();
			if(block.getParent() instanceof IfStatement) {
				IfStatement ifStatement = (IfStatement)block.getParent();
				if(ifStatement.getElseStatement() != null && ifStatement.getElseStatement().equals(block))
					return true;
			}
		}
		else if(statement.getParent() instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement)statement.getParent();
			if(ifStatement.getElseStatement() != null && ifStatement.getElseStatement().equals(statement))
				return true;
		}
		return false;
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
}
