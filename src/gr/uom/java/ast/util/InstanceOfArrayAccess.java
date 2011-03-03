package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Expression;

public class InstanceOfArrayAccess implements ExpressionInstanceChecker {

	public boolean instanceOf(Expression expression) {
		if(expression instanceof ArrayAccess)
			return true;
		else
			return false;
	}

}
