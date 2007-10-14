package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;

public class InstanceOfIfStatement implements StatementInstanceChecker {

	public boolean instanceOf(Statement statement) {
		if(statement instanceof IfStatement)
			return true;
		else
			return false;
	}

}
