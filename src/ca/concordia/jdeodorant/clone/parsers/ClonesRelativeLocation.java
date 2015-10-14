package ca.concordia.jdeodorant.clone.parsers;

public enum ClonesRelativeLocation {
	
	WITHIN_THE_SAME_METHOD("within the same method"),
	WITHIN_THE_SAME_FILE("within the same file"),
	DIFFERENT_FILES("in different files");
	
	private final String location;
	
	private ClonesRelativeLocation(String location) {
		this.location = location;
	}
	
	@Override
	public String toString() {
		return "Clones are " + this.location;
	}
	
}
