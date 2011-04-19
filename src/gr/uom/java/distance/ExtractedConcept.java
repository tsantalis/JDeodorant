package gr.uom.java.distance;

import gr.uom.java.ast.util.TopicFinder;
import gr.uom.java.ast.util.math.HumaniseCamelCase;
import gr.uom.java.ast.util.math.Stemmer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ExtractedConcept implements TopicFinder {

	private Set<ExtractClassCandidateRefactoring> conceptClusters;
	private Set<Entity> conceptEntities;
	private String sourceClass;
	private double minEP;
	private String topic;

	public ExtractedConcept(Set<Entity> conceptEntities, String sourceClass) {
		this.conceptEntities = conceptEntities;
		this.conceptClusters = new HashSet<ExtractClassCandidateRefactoring>();
		this.sourceClass = sourceClass;
		this.minEP = 0.0;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getSourceClass() {
		return sourceClass;
	}

	public Set<Entity> getConceptEntities() {
		return conceptEntities;
	}

	public Set<ExtractClassCandidateRefactoring> getConceptClusters() {
		return conceptClusters;
	}

	public void addConceptCluster(ExtractClassCandidateRefactoring candidate) {
		this.conceptClusters.add(candidate);
	}

	public double getMinEP() {
		if (minEP == 0.0) {
			double min = Double.MAX_VALUE;
			for (ExtractClassCandidateRefactoring candidate : conceptClusters) {
				if (candidate.getEntityPlacement() < min) {
					min = candidate.getEntityPlacement();
				}
			}
			return min;
		} else {
			return minEP;
		}
	}

	public void findTopic(Stemmer stemmer, HumaniseCamelCase humaniser,
			ArrayList<String> stopWords) {
		HashMap<String, Integer> vocabulary = new HashMap<String, Integer>();
		for (Entity entity : this.getConceptEntities()) {
			String[] tokens = null;
			if (entity instanceof MyAttribute) {
				tokens = humaniser.humanise(((MyAttribute) entity).getName())
						.split("\\s");
			} else {
				tokens = humaniser
						.humanise(((MyMethod) entity).getMethodName()).split(
								"\\s");
			}
			for (String token : tokens) {
				if (!token.toUpperCase().equals(token)) {
					stemmer.add(token.toLowerCase().toCharArray(),
							token.length());
					stemmer.stem();
					if (!stopWords.contains(token)
							&& !stopWords.contains(stemmer.toString()
									.toLowerCase())) {
						if (!vocabulary.containsKey(stemmer.toString()
								.toLowerCase())) {
							vocabulary.put(stemmer.toString().toLowerCase(), 1);
						} else {
							vocabulary.put(stemmer.toString().toLowerCase(),
									vocabulary.get(stemmer.toString()
											.toLowerCase()) + 1);
						}
					}
				} else {
					if (!vocabulary.containsKey(token)) {
						vocabulary.put(token, 1);
					} else {
						vocabulary.put(token, vocabulary.get(token) + 1);
					}
				}
			}
			int max = 0;
			ArrayList<String> frequentTermList = new ArrayList<String>();
			String frequentTerm = "";
			if (!vocabulary.isEmpty()) {
				for (String term : vocabulary.keySet()) {
					if (vocabulary.get(term) >= max) {
						max = vocabulary.get(term);
					}
				}
				for (String term : vocabulary.keySet()) {
					if (vocabulary.get(term) == max) {
						frequentTermList.add(term);
					}
				}
				for (int i = 0; i < frequentTermList.size() - 1; i++) {
					frequentTerm += frequentTermList.get(i) + " + ";
				}
				frequentTerm += frequentTermList
						.get(frequentTermList.size() - 1);
			}
			this.setTopic(frequentTerm);
		}
	}

}
