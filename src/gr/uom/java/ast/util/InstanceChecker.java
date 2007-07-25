package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.Expression;

public interface InstanceChecker {
	public boolean instanceOf(Expression expression);
}
