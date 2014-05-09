package gr.uom.java.ast.decomposition.cfg.mapping.precondition;

public enum PreconditionViolationType {
	EXPRESSION_DIFFERENCE_CANNOT_BE_PARAMETERIZED,
	INFEASIBLE_UNIFICATION_DUE_TO_VARIABLE_TYPE_MISMATCH,
	UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_OR_AFTER_THE_EXTRACTED_CODE,
	UNMATCHED_BREAK_STATEMENT,
	UNMATCHED_CONTINUE_STATEMENT,
	UNMATCHED_RETURN_STATEMENT,
	MULTIPLE_RETURNED_VARIABLES,
	UNEQUAL_NUMBER_OF_RETURNED_VARIABLES,
	SINGLE_RETURNED_VARIABLE_WITH_DIFFERENT_TYPES,
	BREAK_STATEMENT_WITHOUT_LOOP,
	CONTINUE_STATEMENT_WITHOUT_LOOP,
	CONDITIONAL_RETURN_STATEMENT;
	
	public String toString() {
		if(name().equals(EXPRESSION_DIFFERENCE_CANNOT_BE_PARAMETERIZED.name())) {
			return "cannot be parameterized, because it accesses variables declared in statements that will be extracted";
		}
		else if(name().equals(UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_OR_AFTER_THE_EXTRACTED_CODE.name())) {
			return "cannot be moved before or after the extracted code, because it accesses variables declared in statements that will be extracted";
		}
		return "";
	}
}
