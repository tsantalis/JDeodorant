package gr.uom.java.ast;

import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeLiteral;

public class LiteralObject {
	private LiteralType literalType;
	private String value;
	private TypeObject type;
	private ASTInformation literal;
	private volatile int hashCode = 0;
	
	public LiteralObject(Expression expression) {
		if(expression instanceof StringLiteral) {
			StringLiteral stringLiteral = (StringLiteral)expression;
			literalType = LiteralType.STRING;
			value = stringLiteral.getLiteralValue();
			type = TypeObject.extractTypeObject(stringLiteral.resolveTypeBinding().getQualifiedName());
		}
		else if(expression instanceof NullLiteral) {
			NullLiteral nullLiteral = (NullLiteral)expression;
			literalType = LiteralType.NULL;
			value = "null";
			if(nullLiteral.resolveTypeBinding() != null) {
				type = TypeObject.extractTypeObject(nullLiteral.resolveTypeBinding().getQualifiedName());
			}
		}
		else if(expression instanceof NumberLiteral) {
			NumberLiteral numberLiteral = (NumberLiteral)expression;
			literalType = LiteralType.NUMBER;
			value = numberLiteral.getToken();
			type = TypeObject.extractTypeObject(numberLiteral.resolveTypeBinding().getQualifiedName());
		}
		else if(expression instanceof BooleanLiteral) {
			BooleanLiteral booleanLiteral = (BooleanLiteral)expression;
			literalType = LiteralType.BOOLEAN;
			value = Boolean.toString(booleanLiteral.booleanValue());
			type = TypeObject.extractTypeObject(booleanLiteral.resolveTypeBinding().getQualifiedName());
		}
		else if(expression instanceof CharacterLiteral) {
			CharacterLiteral characterLiteral = (CharacterLiteral)expression;
			literalType = LiteralType.CHARACTER;
			value = Character.toString(characterLiteral.charValue());
			type = TypeObject.extractTypeObject(characterLiteral.resolveTypeBinding().getQualifiedName());
		}
		else if(expression instanceof TypeLiteral) {
			TypeLiteral typeLiteral = (TypeLiteral)expression;
			literalType = LiteralType.TYPE;
			value = typeLiteral.getType().toString();
			type = TypeObject.extractTypeObject(typeLiteral.resolveTypeBinding().getQualifiedName());
		}
		this.literal = ASTInformationGenerator.generateASTInformation(expression);
	}

	public LiteralType getLiteralType() {
		return literalType;
	}

	public String getValue() {
		return value;
	}

	public TypeObject getType() {
		return type;
	}

	public Expression getLiteral() {
		Expression expression = null;
		if(literalType.equals(LiteralType.BOOLEAN)) {
			expression = (BooleanLiteral)literal.recoverASTNode();
		}
		else if(literalType.equals(LiteralType.CHARACTER)) {
			expression = (CharacterLiteral)literal.recoverASTNode();
		}
		else if(literalType.equals(LiteralType.NULL)) {
			expression = (NullLiteral)literal.recoverASTNode();
		}
		else if(literalType.equals(LiteralType.NUMBER)) {
			expression = (NumberLiteral)literal.recoverASTNode();
		}
		else if(literalType.equals(LiteralType.STRING)) {
			expression = (StringLiteral)literal.recoverASTNode();
		}
		else if(literalType.equals(LiteralType.TYPE)) {
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
            return this.literalType.equals(literalObject.literalType) && this.value.equals(literalObject.value);
        }
        
        return false;
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 37*result + value.hashCode();
			result = 37*result + literalType.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	public String toString() {
		return value;
	}
}
