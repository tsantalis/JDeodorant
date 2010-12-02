package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.Statement;

public class InstanceOfForStatement implements StatementInstanceChecker {

	public boolean instanceOf(Statement statement) {
		if(statement instanceof ForStatement)
			return true;
		else
			return false;
	}

}
