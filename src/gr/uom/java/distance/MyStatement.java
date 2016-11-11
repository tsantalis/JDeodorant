package gr.uom.java.distance;

import gr.uom.java.ast.decomposition.AbstractStatement;

public class MyStatement extends MyAbstractStatement {

	public MyStatement(AbstractStatement statement) {
		super(statement);
	}

	public MyStatement(MyMethodInvocation methodInvocation) {
		super(methodInvocation);
	}
}
