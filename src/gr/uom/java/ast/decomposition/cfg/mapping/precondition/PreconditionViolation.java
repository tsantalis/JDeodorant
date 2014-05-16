package gr.uom.java.ast.decomposition.cfg.mapping.precondition;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.StyledString;

public abstract class PreconditionViolation {
	protected PreconditionViolationType type;
	protected List<Suggestion> suggestions;
	
	public PreconditionViolation(PreconditionViolationType type) {
		this.type = type;
		suggestions = new ArrayList<Suggestion>();
	}
	
	public abstract String getViolation();
	public abstract StyledString getStyledViolation();

	public List<Suggestion> getSuggestions() {
		return suggestions;
	}

	public void addSuggestion(String suggestionString) {
		this.suggestions.add(new Suggestion(suggestionString, this));
	}

	public String toString() {
		return getViolation() + "\n";
	}
	
	public PreconditionViolationType getType() {
		return this.type;
	}
}
