package gr.uom.java.distance;

import gr.uom.java.ast.util.math.HumaniseCamelCase;
import gr.uom.java.ast.util.math.Stemmer;
import gr.uom.java.jdeodorant.refactoring.Activator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class ExtractClassCandidateGroup implements Comparable<ExtractClassCandidateGroup> {

	private String source;
	private ArrayList<ExtractClassCandidateRefactoring> candidates;
	private ArrayList<ExtractedConcept> extractedConcepts;
	private double minEP;

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

	public double getMinEP() {
		if (minEP == 0.0) {
			double min = Double.MAX_VALUE;
			for (ExtractedConcept concept : extractedConcepts) {
				if (concept.getMinEP() < min) {
					min = concept.getMinEP();
				}
			}
			return min;
		} else {
			return minEP;
		}
	}

	public void setMinEP(double ep) {
		this.minEP = ep;
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
		Stemmer stemmer = new Stemmer();
		HumaniseCamelCase humaniser = new HumaniseCamelCase();
		ArrayList<String> stopWords = getStopWords();
		for (ExtractedConcept concept : extractedConcepts) {
			concept.findTopic(stemmer, humaniser, stopWords);
			for (ExtractClassCandidateRefactoring conceptCluster : concept.getConceptClusters()) {
				conceptCluster.findTopic(stemmer, humaniser, stopWords);
			}
		}
	}

	private ArrayList<String> getStopWords() {
		ArrayList<String> stopWords = new ArrayList<String>();
		try {

			BufferedReader in = new BufferedReader(
					new InputStreamReader(Activator.getDefault().getBundle()
							.getEntry("icons/glasgowstoplist.txt").openStream()));
			String next = in.readLine();
			while (next != null) {
				stopWords.add(next);
				next = in.readLine();
			}
			in.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return stopWords;
	}

	public int compareTo(ExtractClassCandidateGroup other) {
		if(this.getMinEP() < other.getMinEP())
			return -1;
		else if(this.getMinEP() > other.getMinEP())
			return 1;
		return this.source.compareTo(other.source);
	}
}
