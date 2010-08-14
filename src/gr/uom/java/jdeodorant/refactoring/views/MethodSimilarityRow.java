package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.history.ProjectVersionPair;

public class MethodSimilarityRow {
	private ProjectVersionPair projectVersionPair;
	private String similarity;
	
	public MethodSimilarityRow(ProjectVersionPair projectVersionPair, String similarity) {
		this.projectVersionPair = projectVersionPair;
		this.similarity = similarity;
	}

	public ProjectVersionPair getProjectVersionPair() {
		return projectVersionPair;
	}

	public String getSimilarity() {
		return similarity;
	}
}
