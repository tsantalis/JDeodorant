package gr.uom.java.ast.decomposition.matching;

public enum DifferenceType {
	AST_TYPE_MISMATCH,
	ARGUMENT_NUMBER_MISMATCH,
	ARRAY_DIMENSION_MISMATCH,
	MISSING_ARRAY_INITIALIZER,
	ARRAY_INITIALIZER_EXPRESSION_NUMBER_MISMATCH,
	ARRAY_INITIALIZER_MISMATCH,
	VARIABLE_TYPE_MISMATCH,
	VARIABLE_NAME_MISMATCH,
	METHOD_INVOCATION_NAME_MISMATCH,
	LITERAL_VALUE_MISMATCH,
	SUBCLASS_TYPE_MISMATCH,
	TYPE_COMPATIBLE_REPLACEMENT,
	MISSING_METHOD_INVOCATION_EXPRESSION,
	OPERATOR_MISMATCH,
	INFIX_EXTENDED_OPERAND_NUMBER_MISMATCH,
	INFIX_LEFT_OPERAND_MISMATCH,
	INFIX_RIGHT_OPERAND_MISMATCH,
	FIELD_ASSIGNMENT_REPLACED_WITH_SETTER,
	FIELD_ACCESS_REPLACED_WITH_GETTER,
	ANONYMOUS_CLASS_DECLARATION_MISMATCH,
	IF_ELSE_SYMMETRICAL_MATCH;

	public String toString(){
		if (name().equals(AST_TYPE_MISMATCH.name())){
			return "The expressions have a different structure and type";
		}
		else if (name().equals(ARGUMENT_NUMBER_MISMATCH.name())){
			return "The number of arguments is different";
		}
		else if (name().equals(ARRAY_DIMENSION_MISMATCH.name())){
			return "The dimensions of the arrays are different";
		}
		else if (name().equals(MISSING_ARRAY_INITIALIZER.name())){
			return "One of the array creations does not have an initializer";
		}
		else if (name().equals(ARRAY_INITIALIZER_EXPRESSION_NUMBER_MISMATCH.name())){
			return "The initializers of the array creations have a different number of expressions";
		}
		else if (name().equals(ARRAY_INITIALIZER_MISMATCH.name())){
			return "The initializers of the array creations are different";
		}
		else if (name().equals(VARIABLE_TYPE_MISMATCH.name())){
			return "The types of the variables are different";
		}
		else if (name().equals(VARIABLE_NAME_MISMATCH.name())){
			return "The names of the variables are different";
		}
		else if (name().equals(METHOD_INVOCATION_NAME_MISMATCH.name())){
			return "The names of the invoked methods are different";
		}
		else if (name().equals(LITERAL_VALUE_MISMATCH.name())){
			return "The values of the literals are different";
		}
		else if (name().equals(SUBCLASS_TYPE_MISMATCH.name())){
			return "The types are different subclasses of the same superclass"; 	
		}
		else if (name().equals(TYPE_COMPATIBLE_REPLACEMENT.name())){
			return "The expressions have a different structure, but the same type";
		}
		else if (name().equals(MISSING_METHOD_INVOCATION_EXPRESSION.name())){
			return "One of the method invocations is not called through an object reference";
		}
		else if (name().equals(OPERATOR_MISMATCH.name())){
			return "The operators of the expressions are different";
		}
		else if (name().equals(INFIX_EXTENDED_OPERAND_NUMBER_MISMATCH.name())){
			return "The infix expressions have a different number of operands";
		}
		else if (name().equals(INFIX_LEFT_OPERAND_MISMATCH.name())){
			return "The infix epxressions have different left operands";
		}
		else if (name().equals(INFIX_RIGHT_OPERAND_MISMATCH.name())){
			return "The infix epxressions have different right operands";
		}
		else if (name().equals(FIELD_ASSIGNMENT_REPLACED_WITH_SETTER.name())){
			return "Field assignment has been replaced with a setter call";
		}
		else if (name().equals(FIELD_ACCESS_REPLACED_WITH_GETTER.name())){
			return "Field access has been replaced with a getter call";
		}
		else if (name().equals(ANONYMOUS_CLASS_DECLARATION_MISMATCH.name())){
			return "The anonymous class declarations are different";
		}
		else if (name().equals(IF_ELSE_SYMMETRICAL_MATCH.name())){
			return "If clause matched with else clause";
		}
		return "";
	}
}
