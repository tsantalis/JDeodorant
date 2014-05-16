package gr.uom.java.ast.decomposition.cfg.mapping.precondition;

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
		if(type.equals(PreconditionViolationType.MULTIPLE_RETURNED_VARIABLES) || type.equals(PreconditionViolationType.UNEQUAL_NUMBER_OF_RETURNED_VARIABLES)) {
			StringBuilder sb = new StringBuilder();
			sb.append("Clone fragment #1 returns variables ");
			sb.append(returnedVariablesG1);
			sb.append(" , while Clone fragment #2 returns variables ");
			sb.append(returnedVariablesG2);
			return sb.toString();
		}
		if(type.equals(PreconditionViolationType.SINGLE_RETURNED_VARIABLE_WITH_DIFFERENT_TYPES)) {
			StringBuilder sb = new StringBuilder();
			sb.append("Clone fragment #1 returns variable ");
			PlainVariable v1 = returnedVariablesG1.iterator().next();
			sb.append(v1.getVariableName());
			sb.append(" with type ");
			sb.append(v1.getVariableType());
			sb.append(" , while Clone fragment #2 returns variable ");
			PlainVariable v2 = returnedVariablesG2.iterator().next();
			sb.append(v2.getVariableName());
			sb.append(" with type ");
			sb.append(v2.getVariableType());
			return sb.toString();
		}
		return null;
	}

	@Override
	public StyledString getStyledViolation() {
		StyledString styledString = new StyledString();
		NormalStyler normalStyler = new NormalStyler();
		BoldStyler boldStyler = new BoldStyler();
		if(type.equals(PreconditionViolationType.MULTIPLE_RETURNED_VARIABLES) || type.equals(PreconditionViolationType.UNEQUAL_NUMBER_OF_RETURNED_VARIABLES)) {
			styledString.append("Clone fragment #1 returns variables ", normalStyler);
			int counter = 0;
			for(PlainVariable variable : returnedVariablesG1) {
				styledString.append(variable.toString(), boldStyler);
				if(counter < returnedVariablesG1.size()-1)
					styledString.append(", ", normalStyler);
				counter++;
			}
			styledString.append(" , while Clone fragment #2 returns variables ", normalStyler);
			counter = 0;
			for(PlainVariable variable : returnedVariablesG2) {
				styledString.append(variable.toString(), boldStyler);
				if(counter < returnedVariablesG2.size()-1)
					styledString.append(", ", normalStyler);
				counter++;
			}
		}
		if(type.equals(PreconditionViolationType.SINGLE_RETURNED_VARIABLE_WITH_DIFFERENT_TYPES)) {
			styledString.append("Clone fragment #1 returns variable ", normalStyler);
			PlainVariable v1 = returnedVariablesG1.iterator().next();
			styledString.append(v1.getVariableName(), boldStyler);
			styledString.append(" with type ", normalStyler);
			styledString.append(v1.getVariableType(), boldStyler);
			styledString.append(" , while Clone fragment #2 returns variable ", normalStyler);
			PlainVariable v2 = returnedVariablesG2.iterator().next();
			styledString.append(v2.getVariableName(), boldStyler);
			styledString.append(" with type ", normalStyler);
			styledString.append(v2.getVariableType(), boldStyler);
		}
		return styledString;
	}
}
