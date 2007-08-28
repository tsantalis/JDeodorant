package gr.uom.java.distance;

import java.util.ListIterator;

import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.decomposition.AbstractStatement;

public class MyStatement extends MyAbstractStatement {

	public MyStatement(AbstractStatement statement, SystemObject system) {
		super(statement, system);
	}

	public MyStatement(MyMethodInvocation methodInvocation) {
		super(methodInvocation);
	}

	private MyStatement(AbstractStatement statement) {
		super(statement);
	}

	public static MyStatement newInstance(MyStatement statement) {
		MyStatement newStatement = new MyStatement(statement.getStatement());
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
