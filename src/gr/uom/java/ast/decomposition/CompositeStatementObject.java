package gr.uom.java.ast.decomposition;

import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.SuperMethodInvocationObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
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

	public void addStatement(AbstractStatement statement) {
		statementList.add(statement);
		statement.setParent(this);
	}

	public List<AbstractStatement> getStatements() {
		return statementList;
	}

	public int getStatementPosition(AbstractStatement statement) {
		if(statementList.contains(statement)) {
			return statementList.indexOf(statement);
		}
		else {
			for(AbstractStatement abstractStatement : statementList) {
				if(abstractStatement instanceof CompositeStatementObject) {
					CompositeStatementObject compositeStatementObject = (CompositeStatementObject)abstractStatement;
					int statementPosition = compositeStatementObject.getStatementPosition(statement);
					if(statementPosition != -1)
						return statementPosition;
				}
			}
		}
		return -1;
	}

	public AbstractStatement getPreviousStatement(AbstractStatement statement) {
		if(statementList.contains(statement)) {
			int index = statementList.indexOf(statement);
			if(index > 0)
				return statementList.get(index-1);
			else
				return null;
		}
		else {
			for(AbstractStatement abstractStatement : statementList) {
				if(abstractStatement instanceof CompositeStatementObject) {
					CompositeStatementObject compositeStatementObject = (CompositeStatementObject)abstractStatement;
					AbstractStatement previousStatement = compositeStatementObject.getPreviousStatement(statement);
					if(previousStatement != null)
						return previousStatement;
				}
			}
		}
		return null;
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

	public VariableDeclarationExpression getVariableDeclarationExpression(LocalVariableDeclarationObject lvdo) {
		for(AbstractExpression expression : expressionList) {
			if(expression.containsLocalVariableDeclaration(lvdo)) {
				return (VariableDeclarationExpression)expression.getExpression();
			}
		}
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject compositeStatementObject = (CompositeStatementObject)statement;
				return compositeStatementObject.getVariableDeclarationExpression(lvdo);
			}
		}
		return null;
	}

	public List<AbstractStatement> getMethodInvocationStatements(MethodInvocationObject methodInvocation) {
		List<AbstractStatement> methodInvocationStatements = new ArrayList<AbstractStatement>();
		for(AbstractExpression expression : expressionList) {
			if(expression.containsMethodInvocation(methodInvocation))
				methodInvocationStatements.add(this);
		}
		for(AbstractStatement statement : statementList) {
			if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				if(statementObject.containsMethodInvocation(methodInvocation))
					methodInvocationStatements.add(statementObject);
			}
			else if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject compositeStatementObject = (CompositeStatementObject)statement;
				methodInvocationStatements.addAll(compositeStatementObject.getMethodInvocationStatements(methodInvocation));
			}
		}
		return methodInvocationStatements;
	}

	public List<AbstractStatement> getSuperMethodInvocationStatements(SuperMethodInvocationObject superMethodInvocation) {
		List<AbstractStatement> superMethodInvocationStatements = new ArrayList<AbstractStatement>();
		for(AbstractExpression expression : expressionList) {
			if(expression.containsSuperMethodInvocation(superMethodInvocation))
				superMethodInvocationStatements.add(this);
		}
		for(AbstractStatement statement : statementList) {
			if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				if(statementObject.containsSuperMethodInvocation(superMethodInvocation))
					superMethodInvocationStatements.add(statementObject);
			}
			else if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject compositeStatementObject = (CompositeStatementObject)statement;
				superMethodInvocationStatements.addAll(compositeStatementObject.getSuperMethodInvocationStatements(superMethodInvocation));
			}
		}
		return superMethodInvocationStatements;
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

	public List<AbstractStatement> getFieldAssignments(FieldInstructionObject fio) {
		List<AbstractStatement> fieldAssignments = new ArrayList<AbstractStatement>();
		for(AbstractStatement statement : statementList) {
			if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				if(statementObject.isFieldAssignment(fio))
					fieldAssignments.add(statementObject);
			}
			else if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject compositeStatementObject = (CompositeStatementObject)statement;
				fieldAssignments.addAll(compositeStatementObject.getFieldAssignments(fio));
			}
		}
		return fieldAssignments;
	}

}
