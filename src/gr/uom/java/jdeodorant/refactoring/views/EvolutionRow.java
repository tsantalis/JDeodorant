package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.history.ProjectVersionPair;

public class EvolutionRow {
	private ProjectVersionPair projectVersionPair;
	private String percentage;
	
	public EvolutionRow(ProjectVersionPair projectVersionPair, String percentage) {
		this.projectVersionPair = projectVersionPair;
		this.percentage = percentage;
	}

	public ProjectVersionPair getProjectVersionPair() {
		return projectVersionPair;
	}

	public String getPercentage() {
		return percentage;
	}
}
