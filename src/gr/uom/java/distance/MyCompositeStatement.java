package gr.uom.java.distance;

import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.decomposition.AbstractStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class MyCompositeStatement extends MyAbstractStatement {
	
	private List<MyAbstractStatement> statementList;
	
	public MyCompositeStatement(AbstractStatement statement, SystemObject system) {
		super(statement, system);
		this.statementList = new ArrayList<MyAbstractStatement>();
	}

	public MyCompositeStatement(List<MyAbstractStatement> statementList) {
		super(statementList);
		this.statementList = statementList;
	}

	private MyCompositeStatement(AbstractStatement statement) {
		super(statement);
		this.statementList = new ArrayList<MyAbstractStatement>();
	}

	public void addStatement(MyAbstractStatement statement) {
		statementList.add(statement);
		statement.setParent(this);
	}

	public ListIterator<MyAbstractStatement> getStatementIterator() {
		return this.statementList.listIterator();
	}

	public void replaceMethodInvocationWithAttributeInstruction(MyMethodInvocation methodInvocation, MyAttributeInstruction attributeInstruction) {
		super.replaceMethodInvocationWithAttributeInstruction(methodInvocation, attributeInstruction);
		for(MyAbstractStatement statement : statementList) {
			statement.replaceMethodInvocationWithAttributeInstruction(methodInvocation, attributeInstruction);
		}
	}

	public void replaceMethodInvocation(MyMethodInvocation oldMethodInvocation, MyMethodInvocation newMethodInvocation) {
        super.replaceMethodInvocation(oldMethodInvocation, newMethodInvocation);
        for(MyAbstractStatement statement : statementList) {
			statement.replaceMethodInvocation(oldMethodInvocation, newMethodInvocation);
		}
    }

    public void replaceAttributeInstruction(MyAttributeInstruction oldInstruction, MyAttributeInstruction newInstruction) {
        super.replaceAttributeInstruction(oldInstruction, newInstruction);
        for(MyAbstractStatement statement : statementList) {
			statement.replaceAttributeInstruction(oldInstruction, newInstruction);
		}
    }


    public void removeAttributeInstruction(MyAttributeInstruction attributeInstruction) {
    	super.removeAttributeInstruction(attributeInstruction);
    	for(MyAbstractStatement statement : statementList) {
			statement.removeAttributeInstruction(attributeInstruction);
		}
    }

    public void setAttributeInstructionReference(MyAttributeInstruction myAttributeInstruction, boolean reference) {
    	super.setAttributeInstructionReference(myAttributeInstruction, reference);
    	for(MyAbstractStatement statement : statementList) {
			statement.setAttributeInstructionReference(myAttributeInstruction, reference);
		}
    }

    public MyAbstractStatement getAbstractStatement(AbstractStatement statement) {
    	MyAbstractStatement abstractStatement = super.getAbstractStatement(statement);
    	if(abstractStatement != null) {
    		return abstractStatement;
    	}
    	else {
    		for(MyAbstractStatement myStatement : statementList) {
    			abstractStatement = myStatement.getAbstractStatement(statement);
    			if(abstractStatement != null) {
    	    		return abstractStatement;
    	    	}
    		}
    	}
    	return null;
    }

    public Set<String> getEntitySet(AbstractStatement statement) {
    	Set<String> entitySet = super.getEntitySet(statement);
    	if(entitySet != null) {
    		return entitySet;
    	}
    	else {
    		for(MyAbstractStatement myStatement : statementList) {
    			entitySet = myStatement.getEntitySet(statement);
    			if(entitySet != null)
    				return entitySet;
    		}
    	}
    	return null;
    }
    
    private MyAbstractStatement getStatementPosition(AbstractStatement abstractStatement) {
    	for(MyAbstractStatement myAbstractStatement : statementList) {
    		if(myAbstractStatement.getStatement().equals(abstractStatement))
    			return myAbstractStatement;
    	}
    	return null;
    }
    
    private void update() {
    	List<MyMethodInvocation> methodInvocationList = new ArrayList<MyMethodInvocation>();
    	List<MyAttributeInstruction> attributeInstructionList = new ArrayList<MyAttributeInstruction>();
    	for(MyAbstractStatement myAbstractStatement : statementList) {
    		ListIterator<MyMethodInvocation> methodInvocationIterator = myAbstractStatement.getMethodInvocationIterator();
    		while(methodInvocationIterator.hasNext()) {
    			MyMethodInvocation methodInvocation = methodInvocationIterator.next();
    			if(!methodInvocationList.contains(methodInvocation))
    				methodInvocationList.add(methodInvocation);
    		}
    		ListIterator<MyAttributeInstruction> attributeInstructionIterator = myAbstractStatement.getAttributeInstructionIterator();
    		while(attributeInstructionIterator.hasNext()) {
    			MyAttributeInstruction attributeInstruction = attributeInstructionIterator.next();
    			if(!attributeInstructionList.contains(attributeInstruction))
    				attributeInstructionList.add(attributeInstruction);
    		}
    	}
    	setMethodInvocationList(methodInvocationList);
    	setAttributeInstructionList(attributeInstructionList);
    	MyCompositeStatement parent = getParent();
    	if(parent != null)
    		parent.update();
    }

    public void replaceStatementsWithMethodInvocation(List<AbstractStatement> statementsToRemove, MyStatement methodInvocation) {
    	boolean found = true;
    	int lastIndexRemoved = -1;
    	for(AbstractStatement abstractStatement : statementsToRemove) {
    		MyAbstractStatement myAbstractStatement = getStatementPosition(abstractStatement);
    		lastIndexRemoved = statementList.indexOf(myAbstractStatement);
    		if(lastIndexRemoved == -1)
    			found = false;
    		else
    			statementList.remove(myAbstractStatement);
    	}
    	if(found) {
    		methodInvocation.setParent(this);
    		statementList.add(lastIndexRemoved, methodInvocation);
    		update();
    	}
    	else {
    		for(MyAbstractStatement myAbstractStatement : statementList) {
    			if(myAbstractStatement instanceof MyCompositeStatement) {
    				MyCompositeStatement myCompositeStatement = (MyCompositeStatement)myAbstractStatement;
    				myCompositeStatement.replaceStatementsWithMethodInvocation(statementsToRemove, methodInvocation);
    			}
    		}
    	}
    }

    public static MyCompositeStatement newInstance(MyCompositeStatement statement) {
		MyCompositeStatement newStatement = new MyCompositeStatement(statement.getStatement());
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
		ListIterator<MyAbstractStatement> statementIterator = statement.getStatementIterator();
		while(statementIterator.hasNext()) {
			MyAbstractStatement myAbstractStatement = statementIterator.next();
			if(myAbstractStatement instanceof MyStatement) {
				MyStatement myStatement = (MyStatement)myAbstractStatement;
				MyStatement newMyStatement = MyStatement.newInstance(myStatement);
				newStatement.addStatement(newMyStatement);
			}
			else if(myAbstractStatement instanceof MyCompositeStatement) {
				MyCompositeStatement myCompositeStatement = (MyCompositeStatement)myAbstractStatement;
				MyCompositeStatement newMyCompositeStatement = MyCompositeStatement.newInstance(myCompositeStatement);
				newStatement.addStatement(newMyCompositeStatement);
			}
		}
		return newStatement;
	}
}
