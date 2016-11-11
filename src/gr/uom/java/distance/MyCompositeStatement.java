package gr.uom.java.distance;

import gr.uom.java.ast.decomposition.AbstractStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class MyCompositeStatement extends MyAbstractStatement {
	
	private List<MyAbstractStatement> statementList;
	private List<MyAbstractExpression> expressionList;
	
	public MyCompositeStatement(AbstractStatement statement) {
		super(statement);
		this.statementList = new ArrayList<MyAbstractStatement>();
		this.expressionList = new ArrayList<MyAbstractExpression>();
	}

	public MyCompositeStatement(List<MyAbstractStatement> statementList) {
		super(statementList);
		this.statementList = statementList;
		this.expressionList = new ArrayList<MyAbstractExpression>();
	}

	public void addStatement(MyAbstractStatement statement) {
		statementList.add(statement);
		statement.setParent(this);
	}

	public ListIterator<MyAbstractStatement> getStatementIterator() {
		return this.statementList.listIterator();
	}

	public void addExpression(MyAbstractExpression expression) {
		expressionList.add(expression);
		expression.setOwner(this);
	}

	public ListIterator<MyAbstractExpression> getExpressionIterator() {
		return this.expressionList.listIterator();
	}

    public void setAttributeInstructionReference(MyAttributeInstruction myAttributeInstruction, boolean reference) {
    	super.setAttributeInstructionReference(myAttributeInstruction, reference);
    	for(MyAbstractStatement statement : statementList) {
			statement.setAttributeInstructionReference(myAttributeInstruction, reference);
		}
    	for(MyAbstractExpression expression : expressionList) {
    		expression.setAttributeInstructionReference(myAttributeInstruction, reference);
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

    private void update() {
    	List<MyMethodInvocation> methodInvocationList = new ArrayList<MyMethodInvocation>();
    	List<MyAttributeInstruction> attributeInstructionList = new ArrayList<MyAttributeInstruction>();
    	for(MyAbstractExpression myAbstractExpression : expressionList) {
    		ListIterator<MyMethodInvocation> methodInvocationIterator = myAbstractExpression.getMethodInvocationIterator();
    		while(methodInvocationIterator.hasNext()) {
    			MyMethodInvocation methodInvocation = methodInvocationIterator.next();
    			if(!methodInvocationList.contains(methodInvocation))
    				methodInvocationList.add(methodInvocation);
    		}
    		ListIterator<MyAttributeInstruction> attributeInstructionIterator = myAbstractExpression.getAttributeInstructionIterator();
    		while(attributeInstructionIterator.hasNext()) {
    			MyAttributeInstruction attributeInstruction = attributeInstructionIterator.next();
    			if(!attributeInstructionList.contains(attributeInstruction))
    				attributeInstructionList.add(attributeInstruction);
    		}
    	}
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

    public void addAttributeInstructionInStatementsOrExpressionsContainingMethodInvocation(MyAttributeInstruction attributeInstruction, MyMethodInvocation methodInvocation) {
    	for(MyAbstractExpression expression : expressionList) {
    		if(expression.containsMethodInvocation(methodInvocation)) {
    			expression.addAttributeInstruction(attributeInstruction);
    			update();
    		}
    	}
    	for(MyAbstractStatement abstractStatement : statementList) {
    		if(abstractStatement instanceof MyStatement) {
    			if(abstractStatement.containsMethodInvocation(methodInvocation)) {
    				abstractStatement.addAttributeInstruction(attributeInstruction);
    				update();
    			}
    		}
    		else if(abstractStatement instanceof MyCompositeStatement) {
    			MyCompositeStatement myCompositeStatement = (MyCompositeStatement)abstractStatement;
    			myCompositeStatement.addAttributeInstructionInStatementsOrExpressionsContainingMethodInvocation(attributeInstruction, methodInvocation);
    		}
    	}
    }

    public void insertMethodInvocationBeforeStatement(MyAbstractStatement parentStatement, MyStatement methodInvocation) {
    	if(statementList.contains(parentStatement)) {
    		int index = statementList.indexOf(parentStatement);
    		methodInvocation.setParent(this);
    		statementList.add(index, methodInvocation);
    		update();
    		return;
    	}
    	else {
    		for(MyAbstractStatement myAbstractStatement : statementList) {
    			if(myAbstractStatement instanceof MyCompositeStatement) {
    				MyCompositeStatement myCompositeStatement = (MyCompositeStatement)myAbstractStatement;
    				myCompositeStatement.insertMethodInvocationBeforeStatement(parentStatement, methodInvocation);
    			}
    		}
    	}
    }

    public void removeStatement(MyAbstractStatement statementToRemove) {
    	if(statementList.contains(statementToRemove)) {
    		statementList.remove(statementToRemove);
    		update();
    		return;
    	}
    	else {
    		for(MyAbstractStatement statement : statementList) {
    			if(statement instanceof MyCompositeStatement) {
    				MyCompositeStatement myCompositeStatement = (MyCompositeStatement)statement;
    				myCompositeStatement.removeStatement(statementToRemove);
    			}
    		}
    	}
    }

    public void replaceSiblingStatementsWithMethodInvocation(List<MyAbstractStatement> statementsToRemove, MyStatement methodInvocation) {
    	boolean found = false;
    	int lastIndexRemoved = -1;
    	for(MyAbstractStatement myAbstractStatement : statementsToRemove) {
    		lastIndexRemoved = statementList.indexOf(myAbstractStatement);
    		if(lastIndexRemoved != -1) {
    			statementList.remove(myAbstractStatement);
    			found = true;
    		}	
    	}
    	if(found) {
    		methodInvocation.setParent(this);
    		statementList.add(lastIndexRemoved, methodInvocation);
    		update();
    		return;
    	}
    	else {
    		for(MyAbstractStatement myAbstractStatement : statementList) {
    			if(myAbstractStatement instanceof MyCompositeStatement) {
    				MyCompositeStatement myCompositeStatement = (MyCompositeStatement)myAbstractStatement;
    				myCompositeStatement.replaceSiblingStatementsWithMethodInvocation(statementsToRemove, methodInvocation);
    			}
    		}
    	}
    }

    public void removeAllStatementsExceptFromSiblingStatements(List<MyAbstractStatement> statementsToKeep) {
    	if(statementList.contains(statementsToKeep.get(0))) {
    		List<MyAbstractStatement> statementsToRemove = new ArrayList<MyAbstractStatement>();
			for(MyAbstractStatement statement : statementList) {
				if(!statementsToKeep.contains(statement))
					statementsToRemove.add(statement);
			}
			statementList.removeAll(statementsToRemove);
			update();
			return;
    	}
    	else {
			for(MyAbstractStatement statement : statementList) {
				if(statement instanceof MyCompositeStatement) {
					MyCompositeStatement myCompositeStatement = (MyCompositeStatement)statement;
					myCompositeStatement.removeAllStatementsExceptFromSiblingStatements(statementsToKeep);
				}
			}
		}
    }

    public Set<String> getEntitySet() {
    	Set<String> entitySet = super.getEntitySet();
    	for(MyAbstractExpression expression : expressionList)
    		entitySet.addAll(expression.getEntitySet());
    	return entitySet;
    }
}
