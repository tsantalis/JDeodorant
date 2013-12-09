package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.Comparator;

public class ControlDependenceTreeNodeMatchPairComparator implements
		Comparator<ControlDependenceTreeNodeMatchPair> {

	public int compare(ControlDependenceTreeNodeMatchPair o1,
			ControlDependenceTreeNodeMatchPair o2) {
		return Integer.valueOf(o1.getNodeDifferences().size()).compareTo(
				Integer.valueOf(o2.getNodeDifferences().size()));
	}

}
