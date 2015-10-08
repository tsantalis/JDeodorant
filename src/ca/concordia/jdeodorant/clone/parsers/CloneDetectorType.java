package ca.concordia.jdeodorant.clone.parsers;

public enum CloneDetectorType {
	
	CCFINDER("CCFinder"),
	CONQAT("ConQAT"),
	DECKARD("Deckard"),
	NICAD("NiCad"),
	CLONEDR("CloneDR");
	
	private final String detectorName;
	
	private CloneDetectorType(String detectorName) {
		this.detectorName = detectorName;
	}
	
	@Override
	public String toString() {
		return this.detectorName;
	}
}
