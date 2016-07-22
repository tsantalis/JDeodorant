package ca.concordia.jdeodorant.clone.parsers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class CloneGroup {
	
	private int cloneGroupID;
	private Set<CloneInstance> cloneInstances = new LinkedHashSet<CloneInstance>();
	private CloneGroup subCloneOf;
	
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
		if (!cloneInstance.isClassLevelClone()) {
			cloneInstances.add(cloneInstance);
			cloneInstance.setBelongingCloneGroup(this);
		}
	}
	
	public List<CloneInstance> getCloneInstances() {
		List<CloneInstance> copyToReturn = new ArrayList<CloneInstance>();
		for (CloneInstance cloneInstance : cloneInstances)
			copyToReturn.add(cloneInstance);
		return copyToReturn;
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

	public boolean containsClassLevelClone() {
		for (CloneInstance cloneInstance : cloneInstances)
			if (cloneInstance.isClassLevelClone())
				return true;
		return false;
	}

	public CloneGroup getSubcloneOf() {
		return this.subCloneOf;
	}
	
	public void setSubCloneOf(CloneGroup subCloneOf) {
		this.subCloneOf = subCloneOf;
	}

	public boolean isSubClone() {
		return this.subCloneOf != null;
	}

	public boolean isSubCloneOf(CloneGroup otherCloneGroup) {
		for (CloneInstance cloneInstance : this.cloneInstances) {
			boolean isSubClone = false;
			for (CloneInstance otherCloneInstance : otherCloneGroup.cloneInstances) {
				if (cloneInstance.isSubcloneOf(otherCloneInstance)) {
					isSubClone = true;
					break;
				}
			}
			if (!isSubClone)
				return false;
		}
		return true;
	}

	public boolean removeClonesExistingInFile(String filePath) {
		return updateClonesExistingInFile(filePath, "");
	}

	public boolean updateClonesExistingInFile(String filePath, String newSource) {
		boolean changed = false;
		for (CloneInstance cloneInstance : cloneInstances) {
			if (cloneInstance.getLocationInfo().getContainingFilePath().equals(filePath)) {
				changed |= cloneInstance.validateIntegrity(newSource);
			}
		}
		return changed;
	}

	public boolean isUpdated() {
		for (CloneInstance cloneInstance : cloneInstances) {
			if (!cloneInstance.getStatus().equals(CloneInstanceStatus.ORIGINAL_LOCATION))
				return true;
		}
		return false;
	}
	
	public ClonesRelativeLocation getClonesRelativeLocation() {
		Set<String> uniqueCloneCodeFragmentsSourceFiles = new HashSet<String>();
		Set<String> uniqueCloneMethodIMethods = new HashSet<String>();
		for (CloneInstance instance : cloneInstances) {
			uniqueCloneCodeFragmentsSourceFiles.add(instance.getLocationInfo().getContainingFilePath());
			uniqueCloneMethodIMethods.add(instance.getContainingClassFullyQualifiedName() + "#" +
					instance.getMethodName() + ":" +
					instance.getIMethodSignature());
		}
		if (uniqueCloneCodeFragmentsSourceFiles.size() == 1) {
			if (uniqueCloneMethodIMethods.size() == 1) {
				return ClonesRelativeLocation.WITHIN_THE_SAME_METHOD;
			} else {
				return ClonesRelativeLocation.WITHIN_THE_SAME_FILE;
			}
		} else {
			return ClonesRelativeLocation.DIFFERENT_FILES;
		}
	}

}
