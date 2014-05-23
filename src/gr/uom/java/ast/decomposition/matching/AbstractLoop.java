package gr.uom.java.ast.decomposition.matching;

import org.eclipse.jdt.core.dom.Statement;

public abstract class AbstractLoop {
	private Statement loopStatement;
	
	public AbstractLoop(Statement loopStatement)
	{
		this.loopStatement = loopStatement;
	}
}
