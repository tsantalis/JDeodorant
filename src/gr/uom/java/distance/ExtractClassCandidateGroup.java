package gr.uom.java.distance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

public class ExtractClassCandidateGroup implements Comparable<ExtractClassCandidateGroup> {

	private String source;
	private ArrayList<ExtractClassCandidateRefactoring> candidates;
	private ArrayList<ExtractedConcept> extractedConcepts;

	public ExtractClassCandidateGroup(String source) {
		this.source = source;
		this.candidates = new ArrayList<ExtractClassCandidateRefactoring>();
		this.extractedConcepts = new ArrayList<ExtractedConcept>();
	}

	public ArrayList<ExtractedConcept> getExtractedConcepts() {
		return extractedConcepts;
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

	public void groupConcepts() {
		ArrayList<ExtractClassCandidateRefactoring> tempCandidates = new ArrayList<ExtractClassCandidateRefactoring>(candidates);
		Collections.sort(tempCandidates, new ClusterSizeComparator());
		while (!tempCandidates.isEmpty()) {
			Set<Entity> conceptEntities = new HashSet<Entity>(tempCandidates.get(0).getExtractedEntities());
			Set<Integer> indexSet = new LinkedHashSet<Integer>();
			indexSet.add(0);
			int previousSize = 0;
			do {
				previousSize = conceptEntities.size();
				for (int i = 1; i < tempCandidates.size(); i++) {
					HashSet<Entity> copiedConceptEntities = new HashSet<Entity>(conceptEntities);
					copiedConceptEntities.retainAll(tempCandidates.get(i).getExtractedEntities());
					if (!copiedConceptEntities.isEmpty()) {
						conceptEntities.addAll(tempCandidates.get(i).getExtractedEntities());
						indexSet.add(i);
					}
				}
			} while (previousSize < conceptEntities.size());
			Set<ExtractClassCandidateRefactoring> candidatesToBeRemoved = new HashSet<ExtractClassCandidateRefactoring>();
			ExtractedConcept newConcept = new ExtractedConcept(conceptEntities, source);
			for (Integer j : indexSet) {
				newConcept.addConceptCluster(tempCandidates.get(j));
				candidatesToBeRemoved.add(tempCandidates.get(j));
			}
			tempCandidates.removeAll(candidatesToBeRemoved);
			extractedConcepts.add(newConcept);
		}
		findConceptTerms();
	}

	private void findConceptTerms() {
		for (ExtractedConcept concept : extractedConcepts) {
			concept.findTopics();
			for (ExtractClassCandidateRefactoring conceptCluster : concept.getConceptClusters()) {
				conceptCluster.findTopics();
			}
		}
	}

	public int compareTo(ExtractClassCandidateGroup other) {
		TreeSet<ExtractClassCandidateRefactoring> thisSet = new TreeSet<ExtractClassCandidateRefactoring>(this.candidates);
		TreeSet<ExtractClassCandidateRefactoring> otherSet = new TreeSet<ExtractClassCandidateRefactoring>(other.candidates);
		ExtractClassCandidateRefactoring thisFirst = thisSet.first();
		ExtractClassCandidateRefactoring otherFirst = otherSet.first();
		int comparison = thisFirst.compareTo(otherFirst);
		if(comparison != 0) {
			return comparison;
		}
		else {
			return this.source.compareTo(other.source);
		}
	}
}
