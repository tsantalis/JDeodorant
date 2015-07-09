package gr.uom.java.ast.decomposition.cfg.mapping;

public enum CloneRefactoringType {
	EXTRACT_LOCAL_METHOD,
	PULL_UP_TO_EXISTING_SUPERCLASS,
	PULL_UP_TO_NEW_SUPERCLASS,
	EXTRACT_STATIC_METHOD_TO_NEW_UTILITY_CLASS,
	INFEASIBLE;
}
