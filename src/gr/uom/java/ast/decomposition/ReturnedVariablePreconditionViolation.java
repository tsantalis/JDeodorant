package gr.uom.java.ast.decomposition;

import gr.uom.java.ast.decomposition.cfg.PlainVariable;

import java.util.Set;

import org.eclipse.jface.viewers.StyledString;

public class ReturnedVariablePreconditionViolation extends PreconditionViolation {
	private Set<PlainVariable> returnedVariablesG1;
	private Set<PlainVariable> returnedVariablesG2;
	
	public ReturnedVariablePreconditionViolation(Set<PlainVariable> returnedVariablesG1,
			Set<PlainVariable> returnedVariablesG2, PreconditionViolationType type) {
		super(type);
		this.returnedVariablesG1 = returnedVariablesG1;
		this.returnedVariablesG2 = returnedVariablesG2;
	}

	@Override
	public String getViolation() {
		if(type.equals(PreconditionViolationType.MULTIPLE_RETURNED_VARIABLES)) {
			StringBuilder sb = new StringBuilder();
			sb.append("Clone fragment #1 returns variables ");
			sb.append(returnedVariablesG1);
			sb.append(" , while Clone fragment #2 returns variables ");
			sb.append(returnedVariablesG2);
			return sb.toString();
		}
		if(type.equals(PreconditionViolationType.DIFFERENT_RETURNED_VARIABLE)) {
			StringBuilder sb = new StringBuilder();
			sb.append("Clone fragment #1 returns variable ");
			sb.append(returnedVariablesG1.iterator().next());
			sb.append(" , while Clone fragment #2 returns variable ");
			sb.append(returnedVariablesG2.iterator().next());
			return sb.toString();
		}
		return null;
	}

	@Override
	public StyledString getStyledViolation() {
		StyledString styledString = new StyledString();
		BoldStyler styler = new BoldStyler();
		if(type.equals(PreconditionViolationType.MULTIPLE_RETURNED_VARIABLES)) {
			styledString.append("Clone fragment #1 returns variables ");
			int counter = 0;
			for(PlainVariable variable : returnedVariablesG1) {
				styledString.append(variable.toString(), styler);
				if(counter < returnedVariablesG1.size()-1)
					styledString.append(", ");
				counter++;
			}
			styledString.append(" , while Clone fragment #2 returns variables ");
			counter = 0;
			for(PlainVariable variable : returnedVariablesG2) {
				styledString.append(variable.toString(), styler);
				if(counter < returnedVariablesG2.size()-1)
					styledString.append(", ");
				counter++;
			}
		}
		if(type.equals(PreconditionViolationType.DIFFERENT_RETURNED_VARIABLE)) {
			styledString.append("Clone fragment #1 returns variable ");
			styledString.append(returnedVariablesG1.iterator().next().toString(), styler);
			styledString.append(" , while Clone fragment #2 returns variable ");
			styledString.append(returnedVariablesG2.iterator().next().toString(), styler);
		}
		return styledString;
	}
}
