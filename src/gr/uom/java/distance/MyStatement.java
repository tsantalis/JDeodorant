package gr.uom.java.distance;

import java.util.ListIterator;

import gr.uom.java.ast.decomposition.AbstractStatement;

public class MyStatement extends MyAbstractStatement {

	public MyStatement(AbstractStatement statement) {
		super(statement);
	}

	public MyStatement(MyMethodInvocation methodInvocation) {
		super(methodInvocation);
	}

	private MyStatement() {
		super();
	}

	public static MyStatement newInstance(MyStatement statement) {
		MyStatement newStatement = new MyStatement();
		newStatement.setStatement(statement.getStatement());
		newStatement.setParent(statement.getParent());
		ListIterator<MyMethodInvocation> methodInvocationIterator = statement.getMethodInvocationIterator();
		while(methodInvocationIterator.hasNext()) {
			MyMethodInvocation myMethodInvocation = methodInvocationIterator.next();
			newStatement.addMethodInvocation(myMethodInvocation);
		}
		ListIterator<MyAttributeInstruction> attributeInstructionIterator = statement.getAttributeInstructionIterator();
		while(attributeInstructionIterator.hasNext()) {
			MyAttributeInstruction myAttributeInstruction = attributeInstructionIterator.next();
			newStatement.addAttributeInstruction(myAttributeInstruction);
		}
		return newStatement;
	}
}
