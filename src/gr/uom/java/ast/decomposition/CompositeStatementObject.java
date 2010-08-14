package gr.uom.java.ast.decomposition;

import java.util.ArrayList;
import java.util.List;

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
	private String type;

	public CompositeStatementObject(Statement statement, String type) {
		super(statement);
		this.type = type;
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
	
	public List<String> stringRepresentation() {
		List<String> stringRepresentation = new ArrayList<String>();
		stringRepresentation.add(this.toString());
		for(AbstractStatement statement : statementList) {
			stringRepresentation.addAll(statement.stringRepresentation());
		}
		return stringRepresentation;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(type);
		if(expressionList.size() > 0) {
			sb.append("(");
			for(AbstractExpression expression : expressionList)
				sb.append(expression.toString());
			sb.append(")");
		}
		return sb.toString();
	}
}
