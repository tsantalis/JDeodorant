package gr.uom.java.distance;

import java.util.Set;

public interface CandidateRefactoring {
	
	public double getEntityPlacement();
	public String getSourceEntity();
	public String getTarget();
	public Set<String> getEntitySet();
}
