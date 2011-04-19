package gr.uom.java.distance;

import java.util.Comparator;

public class ClusterSizeComparator implements Comparator<ExtractClassCandidateRefactoring> {

	public int compare(ExtractClassCandidateRefactoring o1,
			ExtractClassCandidateRefactoring o2) {
		if(o1.getExtractedEntities().size() > o2.getExtractedEntities().size()) {
			return -1;
		}
		else if(o1.getExtractedEntities().size() < o2.getExtractedEntities().size()) {
			return 1;
		}
		else {
			return 0;
		}
	}

}
