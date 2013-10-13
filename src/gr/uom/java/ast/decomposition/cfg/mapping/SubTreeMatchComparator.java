package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.Comparator;

public class SubTreeMatchComparator implements Comparator<CompleteSubTreeMatch> {

	public int compare(CompleteSubTreeMatch o1, CompleteSubTreeMatch o2) {
		int size1 = o1.getMatchPairs().size();
		int size2 = o2.getMatchPairs().size();
		return -Integer.valueOf(size1).compareTo(Integer.valueOf(size2));
	}

}
