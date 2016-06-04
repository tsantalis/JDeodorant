package gr.uom.java.distance;

import gr.uom.java.ast.util.TopicFinder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExtractedConcept {

	private Set<ExtractClassCandidateRefactoring> conceptClusters;
	private Set<Entity> conceptEntities;
	private String sourceClass;
	private double minEP;
	private List<String> topics;

	public ExtractedConcept(Set<Entity> conceptEntities, String sourceClass) {
		this.conceptEntities = conceptEntities;
		this.conceptClusters = new HashSet<ExtractClassCandidateRefactoring>();
		this.sourceClass = sourceClass;
		this.minEP = 0.0;
		this.topics = new ArrayList<String>();
	}

	public List<String> getTopics() {
		return topics;
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

	public void findTopics() {
		List<String> codeElements = new ArrayList<String>();
		for (Entity entity : this.getConceptEntities()) {
			if (entity instanceof MyAttribute) {
				MyAttribute attribute = (MyAttribute) entity;
				codeElements.add(attribute.getName());
			}
			else if (entity instanceof MyMethod) {
				MyMethod method = (MyMethod) entity;
				codeElements.add(method.getMethodName());
			}
		}
		this.topics = TopicFinder.findTopics(codeElements);
	}
}
