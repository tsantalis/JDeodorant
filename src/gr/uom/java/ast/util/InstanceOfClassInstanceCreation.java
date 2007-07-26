package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;

public class InstanceOfClassInstanceCreation implements ExpressionInstanceChecker {

	public boolean instanceOf(Expression expression) {
		if(expression instanceof ClassInstanceCreation)
			return true;
		else
			return false;
	}

}
