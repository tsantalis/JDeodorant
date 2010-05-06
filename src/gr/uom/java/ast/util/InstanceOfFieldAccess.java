package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;

public class InstanceOfFieldAccess implements ExpressionInstanceChecker {

	public boolean instanceOf(Expression expression) {
		if(expression instanceof FieldAccess)
			return true;
		else
			return false;
	}

}
