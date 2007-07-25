package gr.uom.java.distance;

public class InitialSystem implements CandidateRefactoring {
	private double entityPlacement;
	
	public InitialSystem(DistanceMatrix distanceMatrix) {
		this.entityPlacement = distanceMatrix.getSystemEntityPlacementValue();
	}

	public String getSourceEntity() {
		return "initialSystem";
	}

	public String getTarget() {
		return "";
	}

	public double getEntityPlacement() {
		return entityPlacement;
	}
}
