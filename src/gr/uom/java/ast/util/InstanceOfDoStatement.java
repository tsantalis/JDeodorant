package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Statement;

public class InstanceOfDoStatement implements StatementInstanceChecker {

	public boolean instanceOf(Statement statement) {
		if(statement instanceof DoStatement)
			return true;
		else
			return false;
	}

}
