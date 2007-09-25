package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.TypeLiteral;

public class InstanceOfTypeLiteral implements ExpressionInstanceChecker {

	public boolean instanceOf(Expression expression) {
		if(expression instanceof TypeLiteral)
			return true;
		else
			return false;
	}

}
