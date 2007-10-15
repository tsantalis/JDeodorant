package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

public class InstanceOfSuperMethodInvocation implements ExpressionInstanceChecker {

	public boolean instanceOf(Expression expression) {
		if(expression instanceof SuperMethodInvocation)
			return true;
		else
			return false;
	}

}
