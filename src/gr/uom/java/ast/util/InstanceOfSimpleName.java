package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.SimpleName;

public class InstanceOfSimpleName implements ExpressionInstanceChecker {

	public boolean instanceOf(Expression expression) {
		if(expression instanceof SimpleName)
			return true;
		else
			return false;
	}

}
