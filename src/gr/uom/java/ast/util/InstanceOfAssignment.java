package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;

public class InstanceOfAssignment implements ExpressionInstanceChecker {

	public boolean instanceOf(Expression expression) {
		if(expression instanceof Assignment)
			return true;
		else
			return false;
	}

}
