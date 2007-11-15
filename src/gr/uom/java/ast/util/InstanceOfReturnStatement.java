package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;

public class InstanceOfReturnStatement implements StatementInstanceChecker {

	public boolean instanceOf(Statement statement) {
		if(statement instanceof ReturnStatement)
			return true;
		else
			return false;
	}

}
