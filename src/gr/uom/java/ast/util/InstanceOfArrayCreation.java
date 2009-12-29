package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Expression;

public class InstanceOfArrayCreation implements ExpressionInstanceChecker {

	public boolean instanceOf(Expression expression) {
		if(expression instanceof ArrayCreation)
			return true;
		else
			return false;
	}

}
