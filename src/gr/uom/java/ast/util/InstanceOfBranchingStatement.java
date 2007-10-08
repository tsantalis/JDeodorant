package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;

public class InstanceOfBranchingStatement implements StatementInstanceChecker {

	public boolean instanceOf(Statement statement) {
		if(statement instanceof BreakStatement || statement instanceof ContinueStatement || statement instanceof ReturnStatement)
			return true;
		else
			return false;
	}

}
