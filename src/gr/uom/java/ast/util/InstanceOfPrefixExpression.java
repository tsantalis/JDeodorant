package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.PrefixExpression;

public class InstanceOfPrefixExpression implements ExpressionInstanceChecker {

	public boolean instanceOf(Expression expression) {
		if(expression instanceof PrefixExpression)
			return true;
		else
			return false;
	}

}
