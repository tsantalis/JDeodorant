package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.Expression;

public class InstanceOfCastExpression implements ExpressionInstanceChecker {

	public boolean instanceOf(Expression expression) {
		if(expression instanceof CastExpression)
			return true;
		else
			return false;
	}

}
