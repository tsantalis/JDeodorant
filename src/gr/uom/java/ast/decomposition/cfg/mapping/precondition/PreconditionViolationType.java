package gr.uom.java.ast.decomposition.cfg.mapping.precondition;

public enum PreconditionViolationType {
	EXPRESSION_DIFFERENCE_CANNOT_BE_PARAMETERIZED,
	EXPRESSION_DIFFERENCE_IS_FIELD_UPDATE,
	EXPRESSION_DIFFERENCE_IS_VOID_METHOD_CALL,
	EXPRESSION_DIFFERENCE_IS_METHOD_CALL_THROWING_EXCEPTION_WITHIN_MATCHED_TRY_BLOCK,
	INFEASIBLE_UNIFICATION_DUE_TO_VARIABLE_TYPE_MISMATCH,
	INFEASIBLE_UNIFICATION_DUE_TO_MISSING_MEMBERS_IN_THE_COMMON_SUPERCLASS,
	INFEASIBLE_UNIFICATION_DUE_TO_PASSED_ARGUMENT_TYPE_MISMATCH,
	UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_OR_AFTER_THE_EXTRACTED_CODE,
	UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_THE_EXTRACTED_CODE_DUE_TO_CONTROL_DEPENDENCE,
	UNMATCHED_BREAK_STATEMENT,
	UNMATCHED_CONTINUE_STATEMENT,
	UNMATCHED_RETURN_STATEMENT,
	UNMATCHED_THROW_STATEMENT,
	UNMATCHED_EXCEPTION_THROWING_STATEMENT_NESTED_WITHIN_MATCHED_TRY_BLOCK,
	MULTIPLE_RETURNED_VARIABLES,
	UNEQUAL_NUMBER_OF_RETURNED_VARIABLES,
	SINGLE_RETURNED_VARIABLE_WITH_DIFFERENT_TYPES,
	BREAK_STATEMENT_WITHOUT_LOOP,
	CONTINUE_STATEMENT_WITHOUT_LOOP,
	CONDITIONAL_RETURN_STATEMENT,
	SWITCH_CASE_STATEMENT_WITHOUT_SWITCH,
	SUPER_CONSTRUCTOR_INVOCATION_STATEMENT,
	SUPER_METHOD_INVOCATION_STATEMENT,
	MULTIPLE_UNMATCHED_STATEMENTS_UPDATE_THE_SAME_VARIABLE,
	INFEASIBLE_REFACTORING_DUE_TO_UNCOMMON_SUPERCLASS,
	INFEASIBLE_REFACTORING_DUE_TO_ZERO_MATCHED_STATEMENTS,
	NOT_ALL_POSSIBLE_EXECUTION_FLOWS_END_IN_RETURN,
	THIS_CONSTRUCTOR_INVOCATION_STATEMENT;
	
	public String toString() {
		if(name().equals(EXPRESSION_DIFFERENCE_CANNOT_BE_PARAMETERIZED.name())) {
			return "cannot be parameterized, because it has dependencies to/from statements that will be extracted";
		}
		else if(name().equals(EXPRESSION_DIFFERENCE_IS_FIELD_UPDATE.name())) {
			return "is a field being modified, and thus it cannot be parameterized";
		}
		else if(name().equals(EXPRESSION_DIFFERENCE_IS_VOID_METHOD_CALL.name())) {
			return "is a void method call, and thus it cannot be parameterized";
		}
		else if(name().equals(EXPRESSION_DIFFERENCE_IS_METHOD_CALL_THROWING_EXCEPTION_WITHIN_MATCHED_TRY_BLOCK.name())) {
			return "is a method call throwing exception(s) that should be caught by a try block that will be extracted";
		}
		else if(name().equals(UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_OR_AFTER_THE_EXTRACTED_CODE.name())) {
			return "cannot be moved before or after the extracted code, because it has dependencies to/from statements that will be extracted";
		}
		else if(name().equals(UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_THE_EXTRACTED_CODE_DUE_TO_CONTROL_DEPENDENCE.name())) {
			return "cannot be moved before the extracted code, because it has control dependencies from statements that will be extracted";
		}
		else if(name().equals(MULTIPLE_UNMATCHED_STATEMENTS_UPDATE_THE_SAME_VARIABLE.name())) {
			return "cannot be moved, because it updates a variable modified in other unmapped statements";
		}
		else if(name().equals(UNMATCHED_EXCEPTION_THROWING_STATEMENT_NESTED_WITHIN_MATCHED_TRY_BLOCK.name())) {
			return "cannot be moved before or after the extracted code, because it throws exception(s) that should be caught by a try block that will be extracted";
		}
		else if(name().equals(SUPER_CONSTRUCTOR_INVOCATION_STATEMENT.name())) {
			return "cannot be extracted from constructor";
		}
		else if(name().equals(THIS_CONSTRUCTOR_INVOCATION_STATEMENT.name())) {
			return "cannot be extracted from constructor";
		}
		else if(name().equals(SUPER_METHOD_INVOCATION_STATEMENT.name())) {
			return "cannot be extracted from method";
		}
		return "";
	}
}
