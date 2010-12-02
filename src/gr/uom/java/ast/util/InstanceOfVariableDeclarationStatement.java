package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class InstanceOfVariableDeclarationStatement implements StatementInstanceChecker {

	public boolean instanceOf(Statement statement) {
		if(statement instanceof VariableDeclarationStatement)
			return true;
		else
			return false;
	}

}
