package gr.uom.java.ast.decomposition;

import gr.uom.java.ast.LocalVariableDeclarationObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/*
 * CompositeStatementObject represents the following AST Statement subclasses:
 * 1.	Block
 * 2.	DoStatement
 * 3.	EnhancedForStatement
 * 4.	ForStatement
 * 5.	IfStatement
 * 6.	LabeledStatement
 * 7.	SwitchStatement
 * 8.	SynchronizedStatement
 * 9.	TryStatement
 * 10.	WhileStatement
 */

public class CompositeStatementObject extends AbstractStatement {
	
	private List<AbstractStatement> statementList;
	private List<AbstractExpression> expressionList;

	public CompositeStatementObject(Statement statement) {
		super(statement);
		this.statementList = new ArrayList<AbstractStatement>();
		this.expressionList = new ArrayList<AbstractExpression>();
	}

	public CompositeStatementObject(List<AbstractStatement> statementList) {
		super(statementList);
		this.statementList = new ArrayList<AbstractStatement>();
		this.expressionList = new ArrayList<AbstractExpression>();
		for(AbstractStatement statement : statementList) {
			processAbstractStatement(this, statement);
		}
	}

	private void processAbstractStatement(CompositeStatementObject parent, AbstractStatement statement) {
		if(statement instanceof StatementObject) {
			StatementObject child = new StatementObject(statement.getStatement());
			parent.addStatement(child);
		}
		else if(statement instanceof CompositeStatementObject) {
			CompositeStatementObject compositeStatement = (CompositeStatementObject)statement;
			CompositeStatementObject child = new CompositeStatementObject(statement.getStatement());
			List<AbstractExpression> expressionList = compositeStatement.getExpressions();
			for(AbstractExpression abstractExpression : expressionList)
				child.addExpression(new AbstractExpression(abstractExpression.getExpression()));
			parent.addStatement(child);
			List<AbstractStatement> statementList = compositeStatement.getStatements();
			for(AbstractStatement abstractStatement : statementList)
				processAbstractStatement(child, abstractStatement);
		}
	}

	public void addStatement(AbstractStatement statement) {
		statementList.add(statement);
		statement.setParent(this);
	}

	public List<AbstractStatement> getStatements() {
		return statementList;
	}

	public void addExpression(AbstractExpression expression) {
		expressionList.add(expression);
		expression.setOwner(this);
	}

	public List<AbstractExpression> getExpressions() {
		return expressionList;
	}

	public VariableDeclarationStatement getVariableDeclarationStatement(LocalVariableDeclarationObject lvdo) {
		for(AbstractStatement statement : statementList) {
			if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				if(statementObject.containsLocalVariableDeclaration(lvdo))
					return (VariableDeclarationStatement)statementObject.getStatement();
			}
			else if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject compositeStatementObject = (CompositeStatementObject)statement;
				if(compositeStatementObject.containsLocalVariableDeclaration(lvdo))
					return compositeStatementObject.getVariableDeclarationStatement(lvdo);
			}
		}
		return null;
	}

	public List<AbstractStatement> getLocalVariableAssignments(LocalVariableDeclarationObject lvdo) {
		List<AbstractStatement> localVariableAssignments = new ArrayList<AbstractStatement>();
		for(AbstractStatement statement : statementList) {
			if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				if(statementObject.isLocalVariableAssignment(lvdo))
					localVariableAssignments.add(statementObject);
			}
			else if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject compositeStatementObject = (CompositeStatementObject)statement;
				localVariableAssignments.addAll(compositeStatementObject.getLocalVariableAssignments(lvdo));
			}
		}
		return localVariableAssignments;
	}

	public List<AbstractStatement> getLocalVariableAssignments(Set<LocalVariableDeclarationObject> set) {
		List<AbstractStatement> localVariableAssignments = new ArrayList<AbstractStatement>();
		for(LocalVariableDeclarationObject lvdo : set)
			localVariableAssignments.addAll(getLocalVariableAssignments(lvdo));
		return localVariableAssignments;
	}
}
