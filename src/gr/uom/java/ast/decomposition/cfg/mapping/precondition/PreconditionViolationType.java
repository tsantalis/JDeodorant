package gr.uom.java.ast.decomposition.cfg.mapping.precondition;

public enum PreconditionViolationType {
	EXPRESSION_DIFFERENCE_CANNOT_BE_PARAMETERIZED,
	EXPRESSION_DIFFERENCE_IS_FIELD_UPDATE,
	INFEASIBLE_UNIFICATION_DUE_TO_VARIABLE_TYPE_MISMATCH,
	INFEASIBLE_UNIFICATION_DUE_TO_MISSING_MEMBERS_IN_THE_COMMON_SUPERCLASS,
	UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_OR_AFTER_THE_EXTRACTED_CODE,
	UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_THE_EXTRACTED_CODE_DUE_TO_CONTROL_DEPENDENCE,
	UNMATCHED_BREAK_STATEMENT,
	UNMATCHED_CONTINUE_STATEMENT,
	UNMATCHED_RETURN_STATEMENT,
	UNMATCHED_THROW_STATEMENT,
	MULTIPLE_RETURNED_VARIABLES,
	UNEQUAL_NUMBER_OF_RETURNED_VARIABLES,
	SINGLE_RETURNED_VARIABLE_WITH_DIFFERENT_TYPES,
	BREAK_STATEMENT_WITHOUT_LOOP,
	CONTINUE_STATEMENT_WITHOUT_LOOP,
	CONDITIONAL_RETURN_STATEMENT;
	
	public String toString() {
		if(name().equals(EXPRESSION_DIFFERENCE_CANNOT_BE_PARAMETERIZED.name())) {
			return "cannot be parameterized, because it has dependencies to/from statements that will be extracted";
		}
		else if(name().equals(EXPRESSION_DIFFERENCE_IS_FIELD_UPDATE.name())) {
			return "is a field being modified, and thus it cannot be parameterized";
		}
		else if(name().equals(UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_OR_AFTER_THE_EXTRACTED_CODE.name())) {
			return "cannot be moved before or after the extracted code, because it has dependencies to/from statements that will be extracted";
		}
		else if(name().equals(UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_THE_EXTRACTED_CODE_DUE_TO_CONTROL_DEPENDENCE.name())) {
			return "cannot be moved before the extracted code, because it has control dependencies from statements that will be extracted";
		}
		return "";
	}
}
