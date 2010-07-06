package gr.uom.java.distance;

import java.util.ArrayList;

public class ExtractClassCandidatesGroup {
	
	private String source;
	private ArrayList<CandidateRefactoring> candidates;
	private double minEP;
	
	public ExtractClassCandidatesGroup(String source) {
		this.source = source;
		this.candidates = new ArrayList<CandidateRefactoring>();
	}

	public String getSource() {
		return source;
	}
	
	public void addCandidate(CandidateRefactoring candidate) {
		this.candidates.add(candidate);
	}

	public ArrayList<CandidateRefactoring> getCandidates() {
		return candidates;
	}
	
	public double getMinEP() {
		if (minEP == 0.0) {
			double min = Double.MAX_VALUE;
			for (CandidateRefactoring candidate : candidates) {
				if (candidate.getEntityPlacement() < min) {
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
