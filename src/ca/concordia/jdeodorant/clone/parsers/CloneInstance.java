package ca.concordia.jdeodorant.clone.parsers;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

public class CloneInstance {
	
	private CloneGroup belongingCloneGroup;
	private final CloneInstanceLocationInfo locationInfo;
	private final String actualCodeFragment;
	private int cloneID;
	private String sourceFolder;
	private String packageName;
	private String className;
	private IMethod iMethod;
	
	public CloneInstance(CloneInstanceLocationInfo locationInfo) {
		this(locationInfo, null);
	}
	
	public CloneInstance(CloneInstanceLocationInfo cloneLocationInfo, CloneGroup parentGroup) {
		this(cloneLocationInfo, parentGroup, 0);
	}
	
	public CloneInstance(CloneInstanceLocationInfo locationInfo, CloneGroup parentGroup, int cloneID) {
		this.locationInfo = locationInfo;
		this.actualCodeFragment = getActualCodeFragment(locationInfo);
		this.setBelongingCloneGroup(parentGroup);
		this.setCloneID(cloneID);
	}

	private String getActualCodeFragment(CloneInstanceLocationInfo locationInfo) {
		String code = locationInfo.getContainingFileContents();
		return code.substring(locationInfo.getStartOffset(), locationInfo.getStartOffset() + locationInfo.getLength() - 1);
	}

	public String getActualCodeFragment() {
		return actualCodeFragment;
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
		return getIMethod().getElementName();
	}

	public String getMethodSignature() {
		try {
			return getIMethod().getSignature();
		} catch (JavaModelException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public IMethod getIMethod() {
		return this.iMethod;
	}
	
	public void setIMethod(IMethod iMethod) {
		this.iMethod = iMethod;
	}
	
	public boolean isSubcloneOf(CloneInstance other) {
		int startOffset = locationInfo.getStartOffset();
		int endOffset = startOffset + locationInfo.getLength() - 1;
		int otherStartOffset = other.locationInfo.getStartOffset();
		int otherEndOffset = otherStartOffset + other.locationInfo.getLength() - 1;
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
		StringBuilder builder = new StringBuilder();
		builder.append(getLocationInfo()).append(System.lineSeparator());
		builder.append(getActualCodeFragment());
		return builder.toString();
	}

	
	
}
