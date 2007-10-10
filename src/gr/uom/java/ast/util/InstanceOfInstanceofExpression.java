package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InstanceofExpression;

public class InstanceOfInstanceofExpression implements
		ExpressionInstanceChecker {

	public boolean instanceOf(Expression expression) {
		if(expression instanceof InstanceofExpression)
			return true;
		else
			return false;
	}

}
