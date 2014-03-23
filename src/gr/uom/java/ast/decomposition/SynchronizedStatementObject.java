package gr.uom.java.ast.decomposition;

import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;

public class SynchronizedStatementObject extends CompositeStatementObject {

	public SynchronizedStatementObject(Statement statement, AbstractMethodFragment parent) {
		super(statement, StatementType.SYNCHRONIZED, parent);
		AbstractExpression abstractExpression = new AbstractExpression(
				((SynchronizedStatement)statement).getExpression(), this);
		this.addExpression(abstractExpression);
	}

}
