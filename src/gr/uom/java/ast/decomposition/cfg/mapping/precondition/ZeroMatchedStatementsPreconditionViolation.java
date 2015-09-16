package gr.uom.java.ast.decomposition.cfg.mapping.precondition;

import org.eclipse.jface.viewers.StyledString;

public class ZeroMatchedStatementsPreconditionViolation extends PreconditionViolation {

	public ZeroMatchedStatementsPreconditionViolation() {
		super(PreconditionViolationType.INFEASIBLE_REFACTORING_DUE_TO_ZERO_MATCHED_STATEMENTS);
	}

	@Override
	public String getViolation() {
		StringBuilder sb = new StringBuilder();
		sb.append("The refactoring of the clones is infeasible, because the number of macthed statements is equal to zero");
		return sb.toString();
	}

	@Override
	public StyledString getStyledViolation() {
		StyledString styledString = new StyledString();
		NormalStyler normalStyler = new NormalStyler();
		styledString.append("The refactoring of the clones is infeasible, because the number of macthed statements is equal to zero", normalStyler);
		return styledString;
	}

}
