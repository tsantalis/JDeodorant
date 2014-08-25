package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.List;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;

import gr.uom.java.ast.decomposition.cfg.PDGBlockNode;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;

public class PDGNodeGap extends IdBasedGap {
	private PDGNode nodeG1;
	private PDGNode nodeG2;
	private List<ASTNodeDifference> nodeDifferences;
	private volatile int hashCode = 0;
	
	public PDGNodeGap(PDGNode nodeG1, PDGNode nodeG2, boolean advancedMatch, List<ASTNodeDifference> nodeDifferences) {
		super(nodeG1 != null ? nodeG1.getId() : 0, nodeG2 != null ? nodeG2.getId() : 0,
				advancedMatch);
		this.nodeG1 = nodeG1;
		this.nodeG2 = nodeG2;
		this.nodeDifferences = nodeDifferences;
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

	public boolean isFalseControlDependent() {
		if(nodeG1 != null) {
			PDGControlDependence controlDependence1 = nodeG1.getIncomingControlDependence();
			if(controlDependence1 != null)
				return controlDependence1.isFalseControlDependence();
			if(nodeG1 instanceof PDGBlockNode)
				return isNestedUnderElse((PDGBlockNode)nodeG1);
		}
		if(nodeG2 != null) {
			PDGControlDependence controlDependence2 = nodeG2.getIncomingControlDependence();
			if(controlDependence2 != null)
				return controlDependence2.isFalseControlDependence();
			if(nodeG2 instanceof PDGBlockNode)
				return isNestedUnderElse((PDGBlockNode)nodeG2);
		}
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

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(nodeG1 != null)
			sb.append(nodeG1);
		else
			sb.append("\n");
		if(nodeG2 != null)
			sb.append(nodeG2);
		else
			sb.append("\n");
		return sb.toString();
	}

	/*public int compareTo(PDGNodeGap other) {
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
			int id2 = other.nodeG1.getId();
			int id1 = this.nodeG2.getId();
			if(id1 == id2)
				return -1;
			else
				return Integer.compare(id1, id2);
		}
		return 0;
	}*/
}
