package gr.uom.java.distance;

public class CurrentSystem implements CandidateRefactoring {
	private double entityPlacement;
	
	public CurrentSystem(DistanceMatrix distanceMatrix) {
		this.entityPlacement = distanceMatrix.getSystemEntityPlacementValue();
	}

	public String getSourceEntity() {
		return "current system";
	}

	public String getTarget() {
		return "";
	}

	public double getEntityPlacement() {
		return entityPlacement;
	}
}
