package gr.uom.java.ast.util;

import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;

public class InstanceOfTypeDeclarationStatement implements StatementInstanceChecker {

	public boolean instanceOf(Statement statement) {
		if(statement instanceof TypeDeclarationStatement)
			return true;
		else
			return false;
	}

}
