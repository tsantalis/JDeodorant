package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Statement;

public class InstanceOfEnhancedForStatement implements StatementInstanceChecker {

	public boolean instanceOf(Statement statement) {
		if(statement instanceof EnhancedForStatement)
			return true;
		else
			return false;
	}

}
