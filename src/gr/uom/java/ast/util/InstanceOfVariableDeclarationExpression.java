package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;

public class InstanceOfVariableDeclarationExpression implements ExpressionInstanceChecker {

	public boolean instanceOf(Expression expression) {
		if(expression instanceof VariableDeclarationExpression)
			return true;
		else
			return false;
	}

}
