package gr.uom.java.ast.decomposition;

import gr.uom.java.ast.FieldInstructionObject;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.Statement;

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

	public List<Assignment> getFieldAssignments(FieldInstructionObject fio) {
		List<Assignment> fieldAssignments = new ArrayList<Assignment>();
		for(AbstractExpression expression : expressionList) {
			fieldAssignments.addAll(expression.getFieldAssignments(fio));
		}
		for(AbstractStatement statement : statementList) {
			if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				fieldAssignments.addAll(statementObject.getFieldAssignments(fio));
			}
			else if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject compositeStatementObject = (CompositeStatementObject)statement;
				fieldAssignments.addAll(compositeStatementObject.getFieldAssignments(fio));
			}
		}
		return fieldAssignments;
	}

	public List<PostfixExpression> getFieldPostfixAssignments(FieldInstructionObject fio) {
		List<PostfixExpression> fieldPostfixAssignments = new ArrayList<PostfixExpression>();
		for(AbstractExpression expression : expressionList) {
			fieldPostfixAssignments.addAll(expression.getFieldPostfixAssignments(fio));
		}
		for(AbstractStatement statement : statementList) {
			if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				fieldPostfixAssignments.addAll(statementObject.getFieldPostfixAssignments(fio));
			}
			else if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject compositeStatementObject = (CompositeStatementObject)statement;
				fieldPostfixAssignments.addAll(compositeStatementObject.getFieldPostfixAssignments(fio));
			}
		}
		return fieldPostfixAssignments;
	}

	public List<PrefixExpression> getFieldPrefixAssignments(FieldInstructionObject fio) {
		List<PrefixExpression> fieldPrefixAssignments = new ArrayList<PrefixExpression>();
		for(AbstractExpression expression : expressionList) {
			fieldPrefixAssignments.addAll(expression.getFieldPrefixAssignments(fio));
		}
		for(AbstractStatement statement : statementList) {
			if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				fieldPrefixAssignments.addAll(statementObject.getFieldPrefixAssignments(fio));
			}
			else if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject compositeStatementObject = (CompositeStatementObject)statement;
				fieldPrefixAssignments.addAll(compositeStatementObject.getFieldPrefixAssignments(fio));
			}
		}
		return fieldPrefixAssignments;
	}
}
