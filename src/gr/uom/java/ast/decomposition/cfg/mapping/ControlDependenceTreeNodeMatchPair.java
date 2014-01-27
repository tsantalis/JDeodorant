package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;

import java.util.List;

public class ControlDependenceTreeNodeMatchPair implements Comparable<ControlDependenceTreeNodeMatchPair> {
	private ControlDependenceTreeNode node1;
	private ControlDependenceTreeNode node2;
	private List<ASTNodeDifference> nodeDifferences;
	private volatile int hashCode = 0;

	public ControlDependenceTreeNodeMatchPair(ControlDependenceTreeNode node1, ControlDependenceTreeNode node2,
			ASTNodeMatcher matcher) {
		this.node1 = node1;
		this.node2 = node2;
		this.nodeDifferences = matcher.getDifferences();
	}

	public ControlDependenceTreeNode getNode1() {
		return node1;
	}

	public ControlDependenceTreeNode getNode2() {
		return node2;
	}

	public List<ASTNodeDifference> getNodeDifferences() {
		return nodeDifferences;
	}

	public boolean ifStatementInsideElseIfChain() {
		return node1.ifStatementInsideElseIfChain() || node2.ifStatementInsideElseIfChain();
	}

	public boolean isElseIfChainSibling(ControlDependenceTreeNodeMatchPair otherPair) {
		List<ControlDependenceTreeNode> ifParents1 = node1.getIfParents();
		List<ControlDependenceTreeNode> ifParents2 = node2.getIfParents();
		List<ControlDependenceTreeNode> elseIfChildren1 = node1.getElseIfChildren();
		List<ControlDependenceTreeNode> elseIfChildren2 = node2.getElseIfChildren();
		return (ifParents1.contains(otherPair.node1) || elseIfChildren1.contains(otherPair.node1)) &&
				(ifParents2.contains(otherPair.node2) || elseIfChildren2.contains(otherPair.node2));
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 37*result + node1.hashCode();
			result = 37*result + node2.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof ControlDependenceTreeNodeMatchPair) {
			ControlDependenceTreeNodeMatchPair pair = (ControlDependenceTreeNodeMatchPair)o;
			return this.node1.equals(pair.node1) && this.node2.equals(pair.node2);
		}
		return false;
	}

	public String toString() {
		return node1.toString() + node2.toString();
	}

	public int compareTo(ControlDependenceTreeNodeMatchPair other) {
		int val1 = Double.compare(this.node1.getId(), other.node1.getId());
		int val2 = Double.compare(this.node2.getId(), other.node2.getId());
		if(val1 != 0)
			return val1;
		else if(val2 != 0)
			return val2;
		else
			return 0;
	}
}
