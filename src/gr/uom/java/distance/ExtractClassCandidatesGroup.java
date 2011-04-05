package gr.uom.java.distance;

import java.util.ArrayList;
import java.util.Collections;

public class ExtractClassCandidatesGroup {
	
	private String source;
	private ArrayList<ExtractClassCandidateRefactoring> candidates;
	private double minEP;
	
	public ExtractClassCandidatesGroup(String source) {
		this.source = source;
		this.candidates = new ArrayList<ExtractClassCandidateRefactoring>();
	}

	public String getSource() {
		return source;
	}
	
	public void addCandidate(ExtractClassCandidateRefactoring candidate) {
		this.candidates.add(candidate);
	}

	public ArrayList<ExtractClassCandidateRefactoring> getCandidates() {
		Collections.sort(candidates);
		return candidates;
	}
	
	public double getMinEP() {
		if(minEP == 0.0) {
			double min = Double.MAX_VALUE;
			for(ExtractClassCandidateRefactoring candidate : candidates) {
				if(candidate.getEntityPlacement() < min) {
					min = candidate.getEntityPlacement();
				}
			}
			return min;
		}
		else {
			return minEP;
		}
	}
	
	public void setMinEP(double ep) {
		this.minEP = ep;
	}
}
