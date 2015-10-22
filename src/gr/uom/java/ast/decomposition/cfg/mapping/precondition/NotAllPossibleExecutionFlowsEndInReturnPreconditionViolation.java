package gr.uom.java.ast.decomposition.cfg.mapping.precondition;

import org.eclipse.jface.viewers.StyledString;

public class NotAllPossibleExecutionFlowsEndInReturnPreconditionViolation extends PreconditionViolation {

	public NotAllPossibleExecutionFlowsEndInReturnPreconditionViolation() {
		super(PreconditionViolationType.NOT_ALL_POSSIBLE_EXECUTION_FLOWS_END_IN_RETURN);
	}

	@Override
	public String getViolation() {
		StringBuilder sb = new StringBuilder();
		sb.append("Not all possible execution flows end in a return statement");
		return sb.toString();
	}

	@Override
	public StyledString getStyledViolation() {
		StyledString styledString = new StyledString();
		NormalStyler normalStyler = new NormalStyler();
		styledString.append("Not all possible execution flows end in a return statement", normalStyler);
		return styledString;
	}

}
