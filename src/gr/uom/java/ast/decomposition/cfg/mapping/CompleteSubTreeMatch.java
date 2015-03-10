package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.AbstractMethodFragment;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class CompleteSubTreeMatch {
	private TreeSet<ControlDependenceTreeNodeMatchPair> matchPairs;

	public CompleteSubTreeMatch(TreeSet<ControlDependenceTreeNodeMatchPair> matchPairs) {
		this.matchPairs = matchPairs;
	}

	public TreeSet<ControlDependenceTreeNodeMatchPair> getMatchPairs() {
		return matchPairs;
	}

	public List<AbstractMethodFragment> getAdditionalFragments1() {
		List<AbstractMethodFragment> additionalFragments = new ArrayList<AbstractMethodFragment>();
		for(ControlDependenceTreeNodeMatchPair pair : matchPairs) {
			additionalFragments.addAll(pair.getAdditionalFragments1());
		}
		return additionalFragments;
	}

	public List<AbstractMethodFragment> getAdditionalFragments2() {
		List<AbstractMethodFragment> additionalFragments = new ArrayList<AbstractMethodFragment>();
		for(ControlDependenceTreeNodeMatchPair pair : matchPairs) {
			additionalFragments.addAll(pair.getAdditionalFragments2());
		}
		return additionalFragments;
	}

	public boolean isAdvancedMatch() {
		return !getAdditionalFragments1().isEmpty() || !getAdditionalFragments2().isEmpty();
	}

	public List<ControlDependenceTreeNode> getControlDependenceTreeNodes1() {
		List<ControlDependenceTreeNode> nodes1 = new ArrayList<ControlDependenceTreeNode>();
		for(ControlDependenceTreeNodeMatchPair matchPair : this.matchPairs) {
			nodes1.add(matchPair.getNode1());
		}
		return nodes1;
	}

	public List<ControlDependenceTreeNode> getControlDependenceTreeNodes2() {
		List<ControlDependenceTreeNode> nodes2 = new ArrayList<ControlDependenceTreeNode>();
		for(ControlDependenceTreeNodeMatchPair matchPair : this.matchPairs) {
			nodes2.add(matchPair.getNode2());
		}
		return nodes2;
	}

	public void addStartPoint(ControlDependenceTreeNodeMatchPair pair) {
		this.matchPairs.add(pair);
	}

	public boolean subsumes(CompleteSubTreeMatch subTree) {
		//return this.matchPairs.containsAll(subTree.matchPairs);
		Set<ControlDependenceTreeNode> thisNodes1 = new LinkedHashSet<ControlDependenceTreeNode>();
		Set<ControlDependenceTreeNode> thisNodes2 = new LinkedHashSet<ControlDependenceTreeNode>();
		for(ControlDependenceTreeNodeMatchPair matchPair : this.matchPairs) {
			thisNodes1.add(matchPair.getNode1());
			thisNodes2.add(matchPair.getNode2());
		}
		Set<ControlDependenceTreeNode> otherNodes1 = new LinkedHashSet<ControlDependenceTreeNode>();
		Set<ControlDependenceTreeNode> otherNodes2 = new LinkedHashSet<ControlDependenceTreeNode>();
		for(ControlDependenceTreeNodeMatchPair matchPair : subTree.matchPairs) {
			otherNodes1.add(matchPair.getNode1());
			otherNodes2.add(matchPair.getNode2());
		}
		if(thisNodes1.containsAll(otherNodes1) && thisNodes2.containsAll(otherNodes2))
			return true;
		return false;
	}

	public String toString() {
		return matchPairs.toString();
	}
}
