package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;

public class InstanceOfTryStatement implements StatementInstanceChecker {

	public boolean instanceOf(Statement statement) {
		if(statement instanceof TryStatement)
			return true;
		else
			return false;
	}

}
