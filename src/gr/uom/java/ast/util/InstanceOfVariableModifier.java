package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;

public class InstanceOfVariableModifier implements ExpressionInstanceChecker {

	public boolean instanceOf(Expression expression) {
		if(expression instanceof Assignment || expression instanceof PrefixExpression || expression instanceof PostfixExpression || expression instanceof MethodInvocation)
			return true;
		else
			return false;
	}
}
