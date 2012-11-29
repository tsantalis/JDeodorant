package gr.uom.java.ast.decomposition;

import gr.uom.java.ast.CreationObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LiteralObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.SuperFieldInstructionObject;
import gr.uom.java.ast.SuperMethodInvocationObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

	public String getType() {
		return type;
	}

	public List<FieldInstructionObject> getFieldInstructionsInExpressions() {
		List<FieldInstructionObject> fieldInstructions = new ArrayList<FieldInstructionObject>();
		for(AbstractExpression expression : expressionList) {
			fieldInstructions.addAll(expression.getFieldInstructions());
		}
		return fieldInstructions;
	}

	public List<SuperFieldInstructionObject> getSuperFieldInstructionsInExpressions() {
		List<SuperFieldInstructionObject> superFieldInstructions = new ArrayList<SuperFieldInstructionObject>();
		for(AbstractExpression expression : expressionList) {
			superFieldInstructions.addAll(expression.getSuperFieldInstructions());
		}
		return superFieldInstructions;
	}

	public List<LocalVariableDeclarationObject> getLocalVariableDeclarationsInExpressions() {
		List<LocalVariableDeclarationObject> localVariableDeclarations = new ArrayList<LocalVariableDeclarationObject>();
		for(AbstractExpression expression : expressionList) {
			localVariableDeclarations.addAll(expression.getLocalVariableDeclarations());
		}
		return localVariableDeclarations;
	}

	public List<LocalVariableInstructionObject> getLocalVariableInstructionsInExpressions() {
		List<LocalVariableInstructionObject> localVariableInstructions = new ArrayList<LocalVariableInstructionObject>();
		for(AbstractExpression expression : expressionList) {
			localVariableInstructions.addAll(expression.getLocalVariableInstructions());
		}
		return localVariableInstructions;
	}

	public List<MethodInvocationObject> getMethodInvocationsInExpressions() {
		List<MethodInvocationObject> methodInvocations = new ArrayList<MethodInvocationObject>();
		for(AbstractExpression expression : expressionList) {
			methodInvocations.addAll(expression.getMethodInvocations());
		}
		return methodInvocations;
	}

	public List<SuperMethodInvocationObject> getSuperMethodInvocationsInExpressions() {
		List<SuperMethodInvocationObject> superMethodInvocations = new ArrayList<SuperMethodInvocationObject>();
		for(AbstractExpression expression : expressionList) {
			superMethodInvocations.addAll(expression.getSuperMethodInvocations());
		}
		return superMethodInvocations;
	}

	public List<CreationObject> getCreationsInExpressions() {
		List<CreationObject> creations = new ArrayList<CreationObject>();
		for(AbstractExpression expression : expressionList) {
			creations.addAll(expression.getCreations());
		}
		return creations;
	}

	public List<LiteralObject> getLiteralsInExpressions() {
		List<LiteralObject> literals = new ArrayList<LiteralObject>();
		for(AbstractExpression expression : expressionList) {
			literals.addAll(expression.getLiterals());
		}
		return literals;
	}

	public Set<MethodInvocationObject> getInvokedStaticMethodsInExpressions() {
		Set<MethodInvocationObject> staticMethodInvocations = new LinkedHashSet<MethodInvocationObject>();
		for(AbstractExpression expression : expressionList) {
			staticMethodInvocations.addAll(expression.getInvokedStaticMethods());
		}
		return staticMethodInvocations;
	}

	public List<String> stringRepresentation() {
		List<String> stringRepresentation = new ArrayList<String>();
		stringRepresentation.add(this.toString());
		for(AbstractStatement statement : statementList) {
			stringRepresentation.addAll(statement.stringRepresentation());
		}
		return stringRepresentation;
	}

	public List<CompositeStatementObject> getIfStatements() {
		List<CompositeStatementObject> ifStatements = new ArrayList<CompositeStatementObject>();
		if(this.type.equals("if"))
			ifStatements.add(this);
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				ifStatements.addAll(composite.getIfStatements());
			}
		}
		return ifStatements;
	}

	public List<CompositeStatementObject> getSwitchStatements() {
		List<CompositeStatementObject> switchStatements = new ArrayList<CompositeStatementObject>();
		if(this.type.equals("switch"))
			switchStatements.add(this);
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				switchStatements.addAll(composite.getSwitchStatements());
			}
		}
		return switchStatements;
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
	
	public boolean isEquivalent(CompositeStatementObject comp) {
		return this.getType().equals(comp.getType()) &&
		this.getCreationsInExpressions().size() == comp.getCreationsInExpressions().size() &&
		this.getFieldInstructionsInExpressions().size() == comp.getFieldInstructionsInExpressions().size() &&
		this.getSuperFieldInstructionsInExpressions().size() == comp.getSuperFieldInstructionsInExpressions().size() &&
		this.getSuperMethodInvocationsInExpressions().size() == comp.getSuperMethodInvocationsInExpressions().size() &&
		this.getLocalVariableDeclarationsInExpressions().size() == comp.getLocalVariableDeclarationsInExpressions().size() &&
		this.getLocalVariableInstructionsInExpressions().size() == comp.getLocalVariableInstructionsInExpressions().size() &&
		this.getMethodInvocationsInExpressions().size() == comp.getMethodInvocationsInExpressions().size() &&
		this.getLiteralsInExpressions().size() == comp.getLiteralsInExpressions().size() &&
		this.getInvokedStaticMethodsInExpressions().size() == comp.getInvokedStaticMethodsInExpressions().size();
	}
}
