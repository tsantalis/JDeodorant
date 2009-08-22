package gr.uom.java.ast.decomposition.cfg;

import org.eclipse.jdt.core.dom.VariableDeclaration;

public abstract class AbstractVariable {
	protected VariableDeclaration name;
	
	public AbstractVariable(VariableDeclaration name) {
		this.name = name;
	}

	public VariableDeclaration getName() {
		return name;
	}

	public abstract boolean isLocalVariable();
	
	public abstract boolean containsPlainVariable(PlainVariable variable);
}
