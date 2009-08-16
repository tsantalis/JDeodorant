package gr.uom.java.ast;

import org.eclipse.jdt.core.dom.VariableDeclaration;

public interface VariableDeclarationObject {
	
	public VariableDeclaration getVariableDeclaration();
	public String getName();
}
