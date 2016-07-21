package ca.concordia.jdeodorant.clone.parsers;

public class CloneInstance {

	private CloneGroup belongingCloneGroup;
	private final CloneInstanceLocationInfo locationInfo;
	private int cloneID;
	private String sourceFolder = "";
	private String packageName = "";
	private String className = "";
	private String iMethodSignature = "";
	private String methodSignature = "";
	private String methodName = "";
	private String containingClassFullyQualifiedName = "";
	private final String originalCodeFragment;
	
	public CloneInstance(CloneInstanceLocationInfo locationInfo, int cloneID) {
		this(locationInfo, null, cloneID);
	}
	
	public CloneInstance(CloneInstanceLocationInfo locationInfo, CloneGroup parentGroup, int cloneID) {
		this.locationInfo = locationInfo;
		this.setBelongingCloneGroup(parentGroup);
		this.setCloneID(cloneID);
		this.originalCodeFragment = getActualCodeFragment();
	}

	private String getActualCodeFragment() {
		String code = locationInfo.getContainingFileContents();
		/* From substring documentation:
		 * The substring begins at the specified beginIndex and extends to the character at **index endIndex - 1**
		 */
		return code.substring(locationInfo.getStartOffset(), locationInfo.getStartOffset() + locationInfo.getLength());
	}

	public CloneGroup getBelongingCloneGroup() {
		return belongingCloneGroup;
	}

	public void setBelongingCloneGroup(CloneGroup belongingCloneGroup) {
		this.belongingCloneGroup = belongingCloneGroup;
	}

	public CloneInstanceLocationInfo getLocationInfo() {
		return locationInfo;
	}
	
	public boolean updateOffsets(int startOffset, int endOffset) {
		return this.locationInfo.updateOffsets(startOffset, endOffset);
	}
	
	public CloneInstanceStatus getStatus() {
		return this.locationInfo.getCloneLocationStatus();
	}
	
	public String getOriginalCodeFragment() {
		return this.originalCodeFragment;
	}

	public int getCloneID() {
		return cloneID;
	}

	public void setCloneID(int cloneID) {
		this.cloneID = cloneID;
	}
	
	public String getSourceFolder() {
		return sourceFolder;
	}

	public void setSourceFolder(String sourceFolder) {
		this.sourceFolder = sourceFolder;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethodName() {
		return this.methodName;
	}
	
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getMethodSignature() {
		return this.methodSignature;
	}
	
	public void setMethodSignature(String methodSignature) {
		this.methodSignature = methodSignature;
	}
	
	public String getIMethodSignature() {
		return iMethodSignature;
	}

	public void setIMethodSignature(String iMethodSignature) {
		this.iMethodSignature = iMethodSignature;
	}

	public boolean isSubcloneOf(CloneInstance other) {
		if (!locationInfo.getContainingFilePath().equals(other.locationInfo.getContainingFilePath()))
			return false;
		int startOffset = locationInfo.getStartOffset();
		int endOffset = locationInfo.getEndOffset();
		int otherStartOffset = other.locationInfo.getStartOffset();
		int otherEndOffset = other.locationInfo.getEndOffset();
		if (startOffset >= otherStartOffset && endOffset <= otherEndOffset)
			return true;
		return false;
	}
	
	@Override
	public boolean equals(Object other) {
	
		if (other == null)
			return false;
		
		if (other.getClass() != CloneInstance.class)
			return false;
		
		CloneInstance otherInstance =(CloneInstance)other;
		
		return locationInfo.equals(otherInstance.locationInfo);
	}
	
	@Override
	public int hashCode() {
		return locationInfo.hashCode();
	}
	
	@Override
	public String toString() {
		return locationInfo.toString();
	}

	public boolean isClassLevelClone() {
		return this.methodName == null;
	}
	
	public boolean validateIntegrity(String newSource) { 
		int newStartOffset = newSource.indexOf(originalCodeFragment);
		int newEndOffset = -1;
		if (newStartOffset >= 0)
			newEndOffset = newStartOffset + originalCodeFragment.length() - 1;
		return updateOffsets(newStartOffset, newEndOffset);
	}

	public void setContainingClassFullyQualifiedName(String parentFullyQualifiedName) {
		this.containingClassFullyQualifiedName = parentFullyQualifiedName;
	}
	
	public String getContainingClassFullyQualifiedName() {
		return containingClassFullyQualifiedName;
	}
}
