package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.VariableDeclarationObject;

public abstract class AbstractVariable {
	protected VariableDeclarationObject name;
	
	public AbstractVariable(VariableDeclarationObject name) {
		this.name = name;
	}

	public VariableDeclarationObject getName() {
		return name;
	}

	public abstract boolean isLocalVariable();
	
	public abstract boolean containsPlainVariable(PlainVariable variable);
}
