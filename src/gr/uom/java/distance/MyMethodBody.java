package gr.uom.java.distance;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.CompositeStatementObject;
import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.ast.decomposition.StatementObject;

public class MyMethodBody {
	
	private MyCompositeStatement compositeStatement;
	private SystemObject system;
	private MethodBodyObject methodBodyObject;

	public MyMethodBody(MethodBodyObject methodBody, SystemObject system) {
		this.system = system;
		this.methodBodyObject = methodBody;
		CompositeStatementObject compositeStatementObject = methodBody.getCompositeStatement();
		this.compositeStatement = new MyCompositeStatement(compositeStatementObject, system);
		
		List<AbstractStatement> statements = compositeStatementObject.getStatements();
		for(AbstractStatement statement : statements) {
			processStatement(compositeStatement, statement);
		}
	}

	public MyMethodBody(List<MyAbstractStatement> statementList) {
		this.compositeStatement = new MyCompositeStatement(statementList);
	}

	private MyMethodBody(MethodBodyObject methodBodyObject, MyCompositeStatement compositeStatement) {
		this.methodBodyObject = methodBodyObject;
		this.compositeStatement = compositeStatement;
	}

	private void processStatement(MyCompositeStatement parent, AbstractStatement statement) {
		if(statement instanceof StatementObject) {
			MyStatement child = new MyStatement(statement, system);
			parent.addStatement(child);
		}
		else if(statement instanceof CompositeStatementObject) {
			MyCompositeStatement child = new MyCompositeStatement(statement, system);
			parent.addStatement(child);
			CompositeStatementObject compositeStatementObject = (CompositeStatementObject)statement;
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

    public MyMethodInvocation getMethodInvocation(int pos) {
        return this.compositeStatement.getMethodInvocation(pos);
    }

    public MyAttributeInstruction getAttributeInstruction(int pos) {
        return this.compositeStatement.getAttributeInstruction(pos);
    }

    public ListIterator<MyMethodInvocation> getMethodInvocationIterator() {
        return this.compositeStatement.getMethodInvocationIterator();
    }

    public ListIterator<MyAttributeInstruction> getAttributeInstructionIterator() {
        return this.compositeStatement.getAttributeInstructionIterator();
    }

	public void replaceMethodInvocationsWithAttributeInstructions(Map<MyMethodInvocation, MyAttributeInstruction> map) {
		for(MyMethodInvocation key : map.keySet()) {
			this.compositeStatement.replaceMethodInvocationWithAttributeInstruction(key, map.get(key));
		}
	}

	public void replaceMethodInvocation(MyMethodInvocation oldMethodInvocation, MyMethodInvocation newMethodInvocation) {
		this.compositeStatement.replaceMethodInvocation(oldMethodInvocation, newMethodInvocation);
	}

	public void replaceAttributeInstruction(MyAttributeInstruction oldInstruction, MyAttributeInstruction newInstruction) {
		this.compositeStatement.replaceAttributeInstruction(oldInstruction, newInstruction);
	}

	public void setAttributeInstructionReference(MyAttributeInstruction myAttributeInstruction, boolean reference) {
    	this.compositeStatement.setAttributeInstructionReference(myAttributeInstruction, reference);
    }

	public MyAbstractStatement getAbstractStatement(AbstractStatement statement) {
		return this.compositeStatement.getAbstractStatement(statement);
	}

	public Set<String> getEntitySet(AbstractStatement statement) {
		return this.compositeStatement.getEntitySet(statement);
	}

	public void replaceStatementsWithMethodInvocation(List<AbstractStatement> statementsToRemove, MyStatement methodInvocation) {
		this.compositeStatement.replaceStatementsWithMethodInvocation(statementsToRemove, methodInvocation);
	}

	public static MyMethodBody newInstance(MyMethodBody methodBody) {
		MyCompositeStatement myCompositeStatement = methodBody.compositeStatement;
		MyCompositeStatement newMyCompositeStatement = MyCompositeStatement.newInstance(myCompositeStatement);
		return new MyMethodBody(methodBody.methodBodyObject, newMyCompositeStatement);
	}
}
