package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.Statement;

public interface StatementInstanceChecker {
	public boolean instanceOf(Statement statement);
}
