package gr.uom.java.ast.decomposition;

public enum PreconditionViolationType {
	EXPRESSION_DIFFERENCE_CANNOT_BE_PARAMETERIZED,
	INFEASIBLE_UNIFICATION_DUE_TO_VARIABLE_TYPE_MISMATCH;
	
	public String toString() {
		if(name().equals(EXPRESSION_DIFFERENCE_CANNOT_BE_PARAMETERIZED.name())) {
			return "cannot be parameterized, because it accesses variables declared in statements that will be extracted";
		}
		return "";
	}
}
