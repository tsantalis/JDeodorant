package gr.uom.java.distance;

import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.CompositeStatementObject;
import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.ast.decomposition.StatementObject;

public class MyMethodBody {
	
	private MyCompositeStatement compositeStatement;
	private MethodBodyObject methodBodyObject;

	public MyMethodBody(MethodBodyObject methodBody) {
		this.methodBodyObject = methodBody;
		CompositeStatementObject compositeStatementObject = methodBody.getCompositeStatement();
		this.compositeStatement = new MyCompositeStatement(compositeStatementObject);
		
		List<AbstractStatement> statements = compositeStatementObject.getStatements();
		for(AbstractStatement statement : statements) {
			processStatement(compositeStatement, statement);
		}
	}

	private void processStatement(MyCompositeStatement parent, AbstractStatement statement) {
		if(statement instanceof StatementObject) {
			MyStatement child = new MyStatement(statement);
			parent.addStatement(child);
		}
		else if(statement instanceof CompositeStatementObject) {
			MyCompositeStatement child = new MyCompositeStatement(statement);
			parent.addStatement(child);
			CompositeStatementObject compositeStatementObject = (CompositeStatementObject)statement;
			List<AbstractExpression> expressions = compositeStatementObject.getExpressions();
			for(AbstractExpression expression : expressions) {
				MyAbstractExpression myAbstractExpression = new MyAbstractExpression(expression);
				child.addExpression(myAbstractExpression);
			}
			List<AbstractStatement> statements = compositeStatementObject.getStatements();
			for(AbstractStatement statement2 : statements) {
				processStatement(child, statement2);
			}
		}
	}

	public MethodBodyObject getMethodBodyObject() {
		return this.methodBodyObject;
	}

	public boolean containsAttributeInstruction(MyAttributeInstruction instruction) {
		return this.compositeStatement.containsAttributeInstruction(instruction);
	}

	public boolean containsMethodInvocation(MyMethodInvocation invocation) {
		return this.compositeStatement.containsMethodInvocation(invocation);
	}

	public int getNumberOfAttributeInstructions() {
        return this.compositeStatement.getNumberOfAttributeInstructions();
    }

    public int getNumberOfMethodInvocations() {
        return this.compositeStatement.getNumberOfMethodInvocations();
    }

    public ListIterator<MyMethodInvocation> getMethodInvocationIterator() {
        return this.compositeStatement.getMethodInvocationIterator();
    }

    public ListIterator<MyAttributeInstruction> getAttributeInstructionIterator() {
        return this.compositeStatement.getAttributeInstructionIterator();
    }

	public void setAttributeInstructionReference(MyAttributeInstruction myAttributeInstruction, boolean reference) {
    	this.compositeStatement.setAttributeInstructionReference(myAttributeInstruction, reference);
    }

	public MyAbstractStatement getAbstractStatement(AbstractStatement statement) {
		return this.compositeStatement.getAbstractStatement(statement);
	}

	public void addAttributeInstructionInStatementsOrExpressionsContainingMethodInvocation(MyAttributeInstruction attributeInstruction, MyMethodInvocation methodInvocation) {
		this.compositeStatement.addAttributeInstructionInStatementsOrExpressionsContainingMethodInvocation(attributeInstruction, methodInvocation);
	}

	public void insertMethodInvocationBeforeStatement(MyAbstractStatement parentStatement, MyStatement methodInvocation) {
		this.compositeStatement.insertMethodInvocationBeforeStatement(parentStatement, methodInvocation);
	}

	public void removeStatement(MyAbstractStatement statementToRemove) {
		this.compositeStatement.removeStatement(statementToRemove);
	}

	public void replaceSiblingStatementsWithMethodInvocation(List<MyAbstractStatement> statementsToRemove, MyStatement methodInvocation) {
		this.compositeStatement.replaceSiblingStatementsWithMethodInvocation(statementsToRemove, methodInvocation);
	}

	public Set<String> getEntitySet() {
		return this.compositeStatement.getEntitySet();
	}
}
