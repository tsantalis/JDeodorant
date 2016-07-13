package gr.uom.java.ast.decomposition.cfg.mapping.precondition;

import gr.uom.java.ast.decomposition.AbstractStatement;

import org.eclipse.jface.viewers.StyledString;

public class StatementPreconditionViolation extends PreconditionViolation {
	private AbstractStatement statement;
	
	public StatementPreconditionViolation(AbstractStatement statement, PreconditionViolationType type) {
		super(type);
		this.statement = statement;
	}

	public AbstractStatement getStatement() {
		return statement;
	}

	public String getViolation() {
		StringBuilder sb = new StringBuilder();
		if(type.equals(PreconditionViolationType.UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_OR_AFTER_THE_EXTRACTED_CODE) ||
				type.equals(PreconditionViolationType.UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_THE_EXTRACTED_CODE_DUE_TO_CONTROL_DEPENDENCE) ||
				type.equals(PreconditionViolationType.MULTIPLE_UNMATCHED_STATEMENTS_UPDATE_THE_SAME_VARIABLE) ||
				type.equals(PreconditionViolationType.UNMATCHED_EXCEPTION_THROWING_STATEMENT_NESTED_WITHIN_MATCHED_TRY_BLOCK)) {
			sb.append("Unmatched statement ");
			String str = statement.toString();
			sb.append(str.substring(0, str.lastIndexOf("\n")));
			sb.append(" ");
			sb.append(type.toString());
		}
		else if(type.equals(PreconditionViolationType.UNMATCHED_BREAK_STATEMENT) ||
				type.equals(PreconditionViolationType.UNMATCHED_CONTINUE_STATEMENT) ||
				type.equals(PreconditionViolationType.UNMATCHED_RETURN_STATEMENT) ||
				type.equals(PreconditionViolationType.UNMATCHED_THROW_STATEMENT)) {
			sb.append("Unmatched ");
			String str = statement.toString();
			sb.append(str.substring(0, str.lastIndexOf("\n")));
		}
		else if(type.equals(PreconditionViolationType.BREAK_STATEMENT_WITHOUT_LOOP) ||
				type.equals(PreconditionViolationType.CONTINUE_STATEMENT_WITHOUT_LOOP)) {
			sb.append("Statement ");
			String str = statement.toString();
			sb.append(str.substring(0, str.lastIndexOf("\n")));
			sb.append(" without innermost loop");
		}
		else if(type.equals(PreconditionViolationType.SWITCH_CASE_STATEMENT_WITHOUT_SWITCH)) {
			sb.append("Switch ");
			String str = statement.toString();
			sb.append(str.substring(0, str.lastIndexOf("\n")));
			sb.append(" without corresponding switch");
		}
		else if(type.equals(PreconditionViolationType.CONDITIONAL_RETURN_STATEMENT)) {
			sb.append("Conditional ");
			String str = statement.toString();
			sb.append(str.substring(0, str.lastIndexOf("\n")));
		}
		else if(type.equals(PreconditionViolationType.SUPER_CONSTRUCTOR_INVOCATION_STATEMENT)) {
			sb.append("Super constructor call ");
			String str = statement.toString();
			sb.append(str.substring(0, str.lastIndexOf("\n")));
			sb.append(" ");
			sb.append(type.toString());
		}
		else if(type.equals(PreconditionViolationType.THIS_CONSTRUCTOR_INVOCATION_STATEMENT)) {
			sb.append("Constructor call ");
			String str = statement.toString();
			sb.append(str.substring(0, str.lastIndexOf("\n")));
			sb.append(" ");
			sb.append(type.toString());
		}
		else if(type.equals(PreconditionViolationType.SUPER_METHOD_INVOCATION_STATEMENT)) {
			sb.append("Super method call ");
			String str = statement.toString();
			sb.append(str.substring(0, str.lastIndexOf("\n")));
			sb.append(" ");
			sb.append(type.toString());
		}
		return sb.toString();
	}

	@Override
	public StyledString getStyledViolation() {
		StyledString styledString = new StyledString();
		BoldStyler boldStyler = new BoldStyler();
		NormalStyler normalStyler = new NormalStyler();
		if(type.equals(PreconditionViolationType.UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_OR_AFTER_THE_EXTRACTED_CODE) ||
				type.equals(PreconditionViolationType.UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_THE_EXTRACTED_CODE_DUE_TO_CONTROL_DEPENDENCE) ||
				type.equals(PreconditionViolationType.MULTIPLE_UNMATCHED_STATEMENTS_UPDATE_THE_SAME_VARIABLE) ||
				type.equals(PreconditionViolationType.UNMATCHED_EXCEPTION_THROWING_STATEMENT_NESTED_WITHIN_MATCHED_TRY_BLOCK)) {
			styledString.append("Unmatched statement ", normalStyler);
			String str = statement.toString();
			styledString.append(str.substring(0, str.lastIndexOf("\n")), boldStyler);
			styledString.append(" ", normalStyler);
			styledString.append(type.toString(), normalStyler);
		}
		else if(type.equals(PreconditionViolationType.UNMATCHED_BREAK_STATEMENT) ||
				type.equals(PreconditionViolationType.UNMATCHED_CONTINUE_STATEMENT) ||
				type.equals(PreconditionViolationType.UNMATCHED_RETURN_STATEMENT) ||
				type.equals(PreconditionViolationType.UNMATCHED_THROW_STATEMENT)) {
			styledString.append("Unmatched ", normalStyler);
			String str = statement.toString();
			styledString.append(str.substring(0, str.lastIndexOf("\n")), boldStyler);
		}
		else if(type.equals(PreconditionViolationType.BREAK_STATEMENT_WITHOUT_LOOP) ||
				type.equals(PreconditionViolationType.CONTINUE_STATEMENT_WITHOUT_LOOP)) {
			styledString.append("Statement ", normalStyler);
			String str = statement.toString();
			styledString.append(str.substring(0, str.lastIndexOf("\n")), boldStyler);
			styledString.append(" without innermost loop", normalStyler);
		}
		else if(type.equals(PreconditionViolationType.SWITCH_CASE_STATEMENT_WITHOUT_SWITCH)) {
			styledString.append("Switch ", normalStyler);
			String str = statement.toString();
			styledString.append(str.substring(0, str.lastIndexOf("\n")), boldStyler);
			styledString.append(" without corresponding switch", normalStyler);
		}
		else if(type.equals(PreconditionViolationType.CONDITIONAL_RETURN_STATEMENT)) {
			styledString.append("Conditional ", normalStyler);
			String str = statement.toString();
			styledString.append(str.substring(0, str.lastIndexOf("\n")), boldStyler);
		}
		else if(type.equals(PreconditionViolationType.SUPER_CONSTRUCTOR_INVOCATION_STATEMENT)) {
			styledString.append("Super constructor call ", normalStyler);
			String str = statement.toString();
			styledString.append(str.substring(0, str.lastIndexOf("\n")), boldStyler);
			styledString.append(" ", normalStyler);
			styledString.append(type.toString(), normalStyler);
		}
		else if(type.equals(PreconditionViolationType.THIS_CONSTRUCTOR_INVOCATION_STATEMENT)) {
			styledString.append("Constructor call ", normalStyler);
			String str = statement.toString();
			styledString.append(str.substring(0, str.lastIndexOf("\n")), boldStyler);
			styledString.append(" ", normalStyler);
			styledString.append(type.toString(), normalStyler);
		}
		else if(type.equals(PreconditionViolationType.SUPER_METHOD_INVOCATION_STATEMENT)) {
			styledString.append("Super method call ", normalStyler);
			String str = statement.toString();
			styledString.append(str.substring(0, str.lastIndexOf("\n")), boldStyler);
			styledString.append(" ", normalStyler);
			styledString.append(type.toString(), normalStyler);
		}
		return styledString;
	}
}
