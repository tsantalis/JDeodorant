package gr.uom.java.ast;

import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeLiteral;

public class LiteralObject {
	private LiteralType type;
	private String value;
	private ASTInformation literal;
	private volatile int hashCode = 0;
	
	public LiteralObject(Expression expression) {
		if(expression instanceof StringLiteral) {
			StringLiteral stringLiteral = (StringLiteral)expression;
			type = LiteralType.STRING;
			value = stringLiteral.getLiteralValue();
		}
		else if(expression instanceof NullLiteral) {
			type = LiteralType.NULL;
			value = "null";
		}
		else if(expression instanceof NumberLiteral) {
			NumberLiteral numberLiteral = (NumberLiteral)expression;
			type = LiteralType.NUMBER;
			value = numberLiteral.getToken();
		}
		else if(expression instanceof BooleanLiteral) {
			BooleanLiteral booleanLiteral = (BooleanLiteral)expression;
			type = LiteralType.BOOLEAN;
			value = Boolean.toString(booleanLiteral.booleanValue());
		}
		else if(expression instanceof CharacterLiteral) {
			CharacterLiteral characterLiteral = (CharacterLiteral)expression;
			type = LiteralType.CHARACTER;
			value = Character.toString(characterLiteral.charValue());
		}
		else if(expression instanceof TypeLiteral) {
			TypeLiteral typeLiteral = (TypeLiteral)expression;
			type = LiteralType.TYPE;
			value = typeLiteral.getType().toString();
		}
		this.literal = ASTInformationGenerator.generateASTInformation(expression);
	}

	public LiteralType getType() {
		return type;
	}

	public String getValue() {
		return value;
	}
	
	public Expression getLiteral() {
		Expression expression = null;
		if(type.equals(LiteralType.BOOLEAN)) {
			expression = (BooleanLiteral)literal.recoverASTNode();
		}
		else if(type.equals(LiteralType.CHARACTER)) {
			expression = (CharacterLiteral)literal.recoverASTNode();
		}
		else if(type.equals(LiteralType.NULL)) {
			expression = (NullLiteral)literal.recoverASTNode();
		}
		else if(type.equals(LiteralType.NUMBER)) {
			expression = (NumberLiteral)literal.recoverASTNode();
		}
		else if(type.equals(LiteralType.STRING)) {
			expression = (StringLiteral)literal.recoverASTNode();
		}
		else if(type.equals(LiteralType.TYPE)) {
			expression = (TypeLiteral)literal.recoverASTNode();
		}
		return expression;
	}

	public boolean equals(Object o) {
		if(this == o) {
            return true;
        }

        if (o instanceof LiteralObject) {
        	LiteralObject literalObject = (LiteralObject)o;
            return this.type.equals(literalObject.type) && this.value.equals(literalObject.value);
        }
        
        return false;
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 37*result + value.hashCode();
			result = 37*result + type.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	public String toString() {
		return value;
	}
}
