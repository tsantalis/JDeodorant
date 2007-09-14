package gr.uom.java.distance;

import java.util.HashSet;
import java.util.Set;

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

	public Set<String> getEntitySet() {
		return new HashSet<String>();
	}
}
