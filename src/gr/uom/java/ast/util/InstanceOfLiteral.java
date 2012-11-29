package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeLiteral;

public class InstanceOfLiteral implements ExpressionInstanceChecker {

	public boolean instanceOf(Expression expression) {
		if(expression instanceof BooleanLiteral || expression instanceof CharacterLiteral || expression instanceof StringLiteral ||
				expression instanceof NullLiteral || expression instanceof NumberLiteral || expression instanceof TypeLiteral)
			return true;
		else
			return false;
	}

}
