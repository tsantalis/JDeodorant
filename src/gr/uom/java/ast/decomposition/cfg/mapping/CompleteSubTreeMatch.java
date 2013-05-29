package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CompleteSubTreeMatch {
	private List<ControlDependenceTreeNodeMatchPair> matchPairs;

	public CompleteSubTreeMatch(List<ControlDependenceTreeNodeMatchPair> matches) {
		this.matchPairs = matches;
	}

	public void addStartPoint(ControlDependenceTreeNodeMatchPair pair) {
		this.matchPairs.add(0, pair);
	}

	public boolean subsumes(CompleteSubTreeMatch subTree) {
		return this.matchPairs.containsAll(subTree.matchPairs);
	}

	public boolean overlaps(CompleteSubTreeMatch subTree) {
		//an overlap takes places when two subtrees contain matches for the same sets of nodes in different combinations
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
		if(thisNodes1.size() == otherNodes1.size() && thisNodes1.containsAll(otherNodes1) &&
				thisNodes2.size() == otherNodes2.size() && thisNodes2.containsAll(otherNodes2))
			return true;
		return false;
	}

	public String toString() {
		return matchPairs.toString();
	}
}
