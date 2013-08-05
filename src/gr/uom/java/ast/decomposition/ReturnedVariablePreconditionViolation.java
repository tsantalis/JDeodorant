package gr.uom.java.ast.decomposition;

import gr.uom.java.ast.decomposition.cfg.PlainVariable;

import java.util.Set;

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
}
