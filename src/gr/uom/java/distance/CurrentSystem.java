package gr.uom.java.distance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.Position;

public class CurrentSystem extends CandidateRefactoring {
	private double entityPlacement;
	
	public CurrentSystem(DistanceMatrix distanceMatrix) {
		this.entityPlacement = distanceMatrix.getSystemEntityPlacementValue();
	}

	public String getSourceEntity() {
		return "current system";
	}

	public String getSource() {
		return "";
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

	public TypeDeclaration getSourceClassTypeDeclaration() {
		return null;
	}

	public TypeDeclaration getTargetClassTypeDeclaration() {
		return null;
	}

	public IFile getSourceIFile() {
		return null;
	}

	public IFile getTargetIFile() {
		return null;
	}
	public List<Position> getPositions() {
		return new ArrayList<Position>();
	}
}
