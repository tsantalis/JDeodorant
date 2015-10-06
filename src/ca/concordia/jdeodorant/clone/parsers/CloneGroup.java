package ca.concordia.jdeodorant.clone.parsers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.IMethod;


public class CloneGroup {
	
	private int cloneGroupID;
	private Set<CloneInstance> cloneInstances = new LinkedHashSet<CloneInstance>();
	private CloneGroup repeatedOf;
	
	public CloneGroup(int groupID) {
		setCloneGroupID(groupID);
	}

	public int getCloneGroupID() {
		return cloneGroupID;
	}

	public void setCloneGroupID(int cloneGroupID) {
		this.cloneGroupID = cloneGroupID;
	}

	public void addClone(CloneInstance cloneInstance) {
		cloneInstances.add(cloneInstance);
	}
	
	public List<CloneInstance> getCloneInstances() {
		List<CloneInstance> copyToReturn = new ArrayList<CloneInstance>();
		for (CloneInstance cloneInstance : cloneInstances)
			copyToReturn.add(cloneInstance);
		return copyToReturn;
	}
	
	public ClonesRelativeLocation getClonesRelativeLocation() {

		IMethod previousIMethod = null;
		String previousFilePath = null;
		boolean sameFile = true;
		boolean sameMethod = true;
		for (CloneInstance cloneInstance : cloneInstances) {
			if (sameFile && previousFilePath != null && !cloneInstance.getLocationInfo().getContainingFilePath().equals(previousFilePath)) {
				sameFile = false;
			}
			previousFilePath = cloneInstance.getLocationInfo().getContainingFilePath(); 
					
			if (sameMethod && previousIMethod != null && cloneInstance.getIMethod() != previousIMethod) {
				sameMethod = false;
			}
			previousIMethod = cloneInstance.getIMethod();
				
		}
		if (sameFile) {
			if (sameMethod)
				return ClonesRelativeLocation.WITHIN_THE_SAME_METHOD;
			else
				return ClonesRelativeLocation.WITHIN_THE_SAME_FILE;
		} else {
			return ClonesRelativeLocation.DIFFERENT_FILES;
		}
		
	}
	
	@Override
	public boolean equals(Object obj) {

		if (obj == null)
			return false;
		
		if (obj.getClass() != CloneGroup.class)
			return false;
		
		CloneGroup otherCloneGroup = (CloneGroup)obj;
		
		if (getCloneGroupSize() != otherCloneGroup.getCloneGroupSize())
			return false;
		
		return this.cloneInstances.equals(otherCloneGroup.cloneInstances);
		
	}
	
	@Override
	public int hashCode() {
		return cloneInstances.hashCode();
	}
	
	
	public int getCloneGroupSize() {
		return cloneInstances.size();
	}
	
	@Override
	public String toString() {
		return String.format("Clone group ID: %s%sNumber of clone instances: %s", cloneGroupID, System.lineSeparator(), getCloneGroupSize());
	}

	public CloneGroup getRepeatedOf() {
		return repeatedOf;
	}

	public void setRepeatedOf(CloneGroup repeatedOf) {
		this.repeatedOf = repeatedOf;
	}
	
	public boolean isRepeated() {
		return repeatedOf != null;
	}
	
}
