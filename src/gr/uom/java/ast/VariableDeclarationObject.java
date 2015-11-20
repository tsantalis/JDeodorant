package gr.uom.java.ast;

import org.eclipse.jdt.core.dom.VariableDeclaration;

public abstract class VariableDeclarationObject {
	
	protected String variableBindingKey;
	
	public String getVariableBindingKey() {
		return variableBindingKey;
	}
	public abstract VariableDeclaration getVariableDeclaration();
	public abstract String getName();
}
