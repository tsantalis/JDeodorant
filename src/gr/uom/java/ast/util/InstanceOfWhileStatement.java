package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class InstanceOfWhileStatement implements StatementInstanceChecker {

	public boolean instanceOf(Statement statement) {
		if(statement instanceof WhileStatement)
			return true;
		else
			return false;
	}

}
