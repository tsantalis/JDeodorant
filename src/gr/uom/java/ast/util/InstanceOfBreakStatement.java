package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.Statement;

public class InstanceOfBreakStatement implements StatementInstanceChecker {

	public boolean instanceOf(Statement statement) {
		if(statement instanceof BreakStatement)
			return true;
		else
			return false;
	}

}
