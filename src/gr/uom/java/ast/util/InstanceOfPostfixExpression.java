package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.PostfixExpression;

public class InstanceOfPostfixExpression implements ExpressionInstanceChecker {

	public boolean instanceOf(Expression expression) {
		if(expression instanceof PostfixExpression)
			return true;
		else
			return false;
	}

}
