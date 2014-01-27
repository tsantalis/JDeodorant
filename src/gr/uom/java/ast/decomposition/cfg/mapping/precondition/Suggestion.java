package gr.uom.java.ast.decomposition.cfg.mapping.precondition;

public class Suggestion {

	private String suggestion;
	private PreconditionViolation preconditionViolation;
	
	public Suggestion(String suggestion, PreconditionViolation preconditionViolation){
		this.suggestion = suggestion;
		this.preconditionViolation = preconditionViolation;
	}
	public PreconditionViolation getPreconditionViolation(){
		return preconditionViolation;
	}
	public String getSuggestion(){
		return suggestion;
	}
	public void setSuggestion(String suggestion){
		this.suggestion = suggestion;
	}
	
}
