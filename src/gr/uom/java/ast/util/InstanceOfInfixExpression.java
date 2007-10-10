package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;

public class InstanceOfInfixExpression implements ExpressionInstanceChecker {

	public boolean instanceOf(Expression expression) {
		if(expression instanceof InfixExpression)
			return true;
		else
			return false;
	}

}
