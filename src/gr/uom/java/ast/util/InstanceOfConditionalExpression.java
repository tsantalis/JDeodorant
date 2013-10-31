package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;

public class InstanceOfConditionalExpression implements ExpressionInstanceChecker {

	public boolean instanceOf(Expression expression) {
		if(expression instanceof ConditionalExpression)
			return true;
		else
			return false;
	}

}
