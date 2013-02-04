package gr.uom.java.ast.decomposition;

public enum StatementType {
	ASSERT,
	BLOCK {
		public String toString() {
			return "{";
		}
	},
	BREAK,
	CONSTRUCTOR_INVOCATION,
	CONTINUE,
	DO,
	EMPTY,
	ENHANCED_FOR {
		public String toString() {
			return "for";
		}
	},
	EXPRESSION,
	FOR,
	IF,
	LABELED,
	RETURN,
	SUPER_CONSTRUCTOR_INVOCATION,
	SWITCH_CASE,
	SWITCH,
	SYNCHRONIZED,
	THROW,
	TRY,
	TYPE_DECLARATION,
	VARIABLE_DECLARATION,
	WHILE;
	
	public String toString() {
		return name().toLowerCase();
	}
}
