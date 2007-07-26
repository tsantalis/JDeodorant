package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;

public class InstanceOfConstructorInvocation implements StatementInstanceChecker {

	public boolean instanceOf(Statement statement) {
		if(statement instanceof ConstructorInvocation || statement instanceof SuperConstructorInvocation)
			return true;
		else
			return false;
	}

}
