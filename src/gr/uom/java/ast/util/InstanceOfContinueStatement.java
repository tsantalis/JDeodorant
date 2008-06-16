package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.Statement;

public class InstanceOfContinueStatement implements StatementInstanceChecker {

	public boolean instanceOf(Statement statement) {
		if(statement instanceof ContinueStatement)
			return true;
		else
			return false;
	}

}
