package gr.uom.java.ast.decomposition;

public abstract class PreconditionViolation {
	protected PreconditionViolationType type;
	protected String suggestion;
	
	public PreconditionViolation(PreconditionViolationType type) {
		this.type = type;
	}
	
	public abstract String getViolation();

	public String getSuggestion() {
		return suggestion;
	}

	public void setSuggestion(String suggestion) {
		this.suggestion = suggestion;
	}
	
}
