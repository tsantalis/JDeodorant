package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.Comparator;

public class ControlDependenceTreeNodeMatchPairComparator implements
		Comparator<ControlDependenceTreeNodeMatchPair> {

	public int compare(ControlDependenceTreeNodeMatchPair o1,
			ControlDependenceTreeNodeMatchPair o2) {
		
		int count1 = o1.getDistinctDifferenceCount();
		int count2 = o2.getDistinctDifferenceCount();
		if(count1 != count2) {
			return Integer.valueOf(count1).compareTo(Integer.valueOf(count2));
		}
		else {
			count1 = o1.getNonDistinctDifferenceCount();
			count2 = o2.getNonDistinctDifferenceCount();
			if(count1 != count2) {
				return Integer.valueOf(count1).compareTo(Integer.valueOf(count2));
			}
			else {
				count1 = o1.getNonDistinctDifferenceCountIncludingTypeMismatches();
				count2 = o2.getNonDistinctDifferenceCountIncludingTypeMismatches();
				if(count1 != count2) {
					return Integer.valueOf(count1).compareTo(Integer.valueOf(count2));
				}
			}
		}
		return 0;
	}

}
