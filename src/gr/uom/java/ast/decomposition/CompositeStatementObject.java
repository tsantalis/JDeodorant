package gr.uom.java.ast.decomposition;

import gr.uom.java.ast.CreationObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LiteralObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.SuperFieldInstructionObject;
import gr.uom.java.ast.SuperMethodInvocationObject;
import gr.uom.java.ast.decomposition.cfg.mapping.FieldInstructionReplacement;
import gr.uom.java.ast.decomposition.cfg.mapping.LiteralReplacement;
import gr.uom.java.ast.decomposition.cfg.mapping.Replacement;
import gr.uom.java.ast.decomposition.cfg.mapping.VariableDeclarationReplacement;
import gr.uom.java.ast.decomposition.cfg.mapping.VariableInstructionReplacement;

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
		if(getEntireString().contains("\n")) {
			String compositeString = getEntireString().substring(0, getEntireString().indexOf("\n"));
			//find all occurrences of ")"
			List<Integer> indices = new ArrayList<Integer>();
			int index = compositeString.indexOf(")");
			while (index >= 0) {
			    indices.add(index);
			    index = compositeString.indexOf(")", index + 1);
			}
			int searchIndex = compositeString.length();
			if(compositeString.contains("{")) {
				searchIndex = compositeString.indexOf("{");
			}
			else if(expressionList.size() > 0) {
				AbstractExpression expression = expressionList.get(expressionList.size()-1);
				int expressionStartPosition = expression.getStartPosition();
				int expressionLength = expression.getLength();
				searchIndex = expressionStartPosition - getStartPosition() + expressionLength;
			}
			int minimumDistance = Integer.MAX_VALUE;
			int closestClosingParenthesis = -1;
			for(Integer closingParenthesisIndex : indices) {
				int distance = Math.abs(searchIndex - closingParenthesisIndex);
				if(distance < minimumDistance) {
					closestClosingParenthesis = closingParenthesisIndex;
					minimumDistance = distance;
				}
			}
			if(closestClosingParenthesis != -1) {
				return getEntireString().substring(0, closestClosingParenthesis + 1) + "\n";
			}
			else {
				return compositeString + "\n";
			}
		}
		return getEntireString() + "\n";
	}
	
	public boolean isEquivalent(CompositeStatementObject comp) {
		return this.getType().equals(comp.getType()) &&
		this.getExpressions().size() == comp.getExpressions().size() &&
		this.getCreationsInExpressions().size() == comp.getCreationsInExpressions().size() &&
		this.getFieldInstructionsInExpressions().size() == comp.getFieldInstructionsInExpressions().size() &&
		this.getSuperFieldInstructionsInExpressions().size() == comp.getSuperFieldInstructionsInExpressions().size() &&
		this.getSuperMethodInvocationsInExpressions().size() == comp.getSuperMethodInvocationsInExpressions().size() &&
		this.getLocalVariableDeclarationsInExpressions().size() == comp.getLocalVariableDeclarationsInExpressions().size() &&
		this.getLocalVariableInstructionsInExpressions().size() == comp.getLocalVariableInstructionsInExpressions().size() &&
		this.getMethodInvocationsInExpressions().size() == comp.getMethodInvocationsInExpressions().size() &&
		this.getLiteralsInExpressions().size() == comp.getLiteralsInExpressions().size() &&
		this.getInvokedStaticMethodsInExpressions().size() == comp.getInvokedStaticMethodsInExpressions().size() &&
		this.equivalentTypes(comp);
	}
	
	private boolean equivalentTypes(CompositeStatementObject comp) {
		List<LocalVariableDeclarationObject> variableDeclarations1 = this.getLocalVariableDeclarationsInExpressions();
		List<LocalVariableDeclarationObject> variableDeclarations2 = comp.getLocalVariableDeclarationsInExpressions();
		for(int i=0; i<variableDeclarations1.size(); i++) {
			LocalVariableDeclarationObject variableDeclaration1 = variableDeclarations1.get(i);
			LocalVariableDeclarationObject variableDeclaration2 = variableDeclarations2.get(i);
			if(!variableDeclaration1.getType().equals(variableDeclaration2.getType())) {
				return false;
			}
		}
		
		List<LocalVariableInstructionObject> variableInstructions1 = this.getLocalVariableInstructionsInExpressions();
		List<LocalVariableInstructionObject> variableInstructions2 = comp.getLocalVariableInstructionsInExpressions();
		for(int i=0; i<variableInstructions1.size(); i++) {
			LocalVariableInstructionObject variableInstruction1 = variableInstructions1.get(i);
			LocalVariableInstructionObject variableInstruction2 = variableInstructions2.get(i);
			if(!variableInstruction1.getType().equals(variableInstruction2.getType())) {
				return false;
			}
		}

		List<FieldInstructionObject> fieldInstructions1 = this.getFieldInstructionsInExpressions();
		List<FieldInstructionObject> fieldInstructions2 = comp.getFieldInstructionsInExpressions();
		for(int i=0; i<fieldInstructions1.size(); i++) {
			FieldInstructionObject fieldInstruction1 = fieldInstructions1.get(i);
			FieldInstructionObject fieldInstruction2 = fieldInstructions2.get(i);
			if(!fieldInstruction1.getType().equals(fieldInstruction2.getType())) {
				return false;
			}
		}
		
		List<LiteralObject> literals1 = this.getLiteralsInExpressions();
		List<LiteralObject> literals2 = comp.getLiteralsInExpressions();
		for(int i=0; i<literals1.size(); i++) {
			LiteralObject literal1 = literals1.get(i);
			LiteralObject literal2 = literals2.get(i);
			if(!literal1.getType().equals(literal2.getType())) {
				return false;
			}
		}
		return true;
	}
	
	public List<Replacement> findReplacements(CompositeStatementObject comp) {
		List<Replacement> replacements = new ArrayList<Replacement>();
		for(int j=0; j<this.expressionList.size(); j++) {
			AbstractExpression expression1 = this.expressionList.get(j);
			AbstractExpression expression2 = comp.expressionList.get(j);
			
			List<LocalVariableDeclarationObject> variableDeclarations1 = expression1.getLocalVariableDeclarations();
			List<LocalVariableDeclarationObject> variableDeclarations2 = expression2.getLocalVariableDeclarations();
			for(int i=0; i<variableDeclarations1.size(); i++) {
				LocalVariableDeclarationObject variableDeclaration1 = variableDeclarations1.get(i);
				LocalVariableDeclarationObject variableDeclaration2 = variableDeclarations2.get(i);
				if(!variableDeclaration1.getName().equals(variableDeclaration2.getName())) {
					int startPosition1 = fixIndex(variableDeclaration1.getVariableDeclaration().getName().getStartPosition() - this.getStartPosition(), this.toString(), variableDeclaration1.getName(), expression1.toString());
					int startPosition2 = fixIndex(variableDeclaration2.getVariableDeclaration().getName().getStartPosition() - comp.getStartPosition(), comp.toString(), variableDeclaration2.getName(), expression2.toString());
					VariableDeclarationReplacement replacement = new VariableDeclarationReplacement(variableDeclaration1, variableDeclaration2, startPosition1, startPosition2);
					replacements.add(replacement);
				}
			}

			List<LocalVariableInstructionObject> variableInstructions1 = expression1.getLocalVariableInstructions();
			List<LocalVariableInstructionObject> variableInstructions2 = expression2.getLocalVariableInstructions();
			for(int i=0; i<variableInstructions1.size(); i++) {
				LocalVariableInstructionObject variableInstruction1 = variableInstructions1.get(i);
				LocalVariableInstructionObject variableInstruction2 = variableInstructions2.get(i);
				if(!variableInstruction1.getName().equals(variableInstruction2.getName())) {
					int startPosition1 = fixIndex(variableInstruction1.getSimpleName().getStartPosition() - this.getStartPosition(), this.toString(), variableInstruction1.getName(), expression1.toString());
					int startPosition2 = fixIndex(variableInstruction2.getSimpleName().getStartPosition() - comp.getStartPosition(), comp.toString(), variableInstruction2.getName(), expression2.toString());
					VariableInstructionReplacement replacement = new VariableInstructionReplacement(variableInstruction1, variableInstruction2, startPosition1, startPosition2);
					replacements.add(replacement);
				}
			}

			List<FieldInstructionObject> fieldInstructions1 = expression1.getFieldInstructions();
			List<FieldInstructionObject> fieldInstructions2 = expression2.getFieldInstructions();
			for(int i=0; i<fieldInstructions1.size(); i++) {
				FieldInstructionObject fieldInstruction1 = fieldInstructions1.get(i);
				FieldInstructionObject fieldInstruction2 = fieldInstructions2.get(i);
				if(!fieldInstruction1.getName().equals(fieldInstruction2.getName())) {
					int startPosition1 = fixIndex(fieldInstruction1.getSimpleName().getStartPosition() - this.getStartPosition(), this.toString(), fieldInstruction1.getName(), expression1.toString());
					int startPosition2 = fixIndex(fieldInstruction2.getSimpleName().getStartPosition() - comp.getStartPosition(), comp.toString(), fieldInstruction2.getName(), expression2.toString());
					FieldInstructionReplacement replacement = new FieldInstructionReplacement(fieldInstruction1, fieldInstruction2, startPosition1, startPosition2);
					replacements.add(replacement);
				}
			}

			List<LiteralObject> literals1 = expression1.getLiterals();
			List<LiteralObject> literals2 = expression2.getLiterals();
			for(int i=0; i< literals1.size(); i++) {
				LiteralObject literal1 = literals1.get(i);
				LiteralObject literal2 = literals2.get(i);
				if(!literal1.getValue().equals(literal2.getValue())) {
					int startPosition1 = fixIndex(literal1.getLiteral().getStartPosition() - this.getStartPosition(), this.toString(), literal1.getValue(), expression1.toString());
					int startPosition2 = fixIndex(literal2.getLiteral().getStartPosition() - comp.getStartPosition(), comp.toString(), literal2.getValue(), expression2.toString());
					LiteralReplacement replacement = new LiteralReplacement(literal1, literal2, startPosition1, startPosition2);
					replacements.add(replacement);
				}
			}
		}
		return replacements;
	}
	
	private int fixIndex(int startPosition, String statement, String lookFor, String expression) {
		//searching left
		int indexOfExpression = statement.indexOf(expression);
		int decrement = 0;
		boolean decrementFound = false;
		while(!statement.substring(startPosition - decrement, startPosition - decrement + lookFor.length()).equals(lookFor) &&
				(startPosition - decrement) >= 0) {
			decrement++;
		}
		if(statement.substring(startPosition - decrement, startPosition - decrement + lookFor.length()).equals(lookFor) &&
				isWithinExpression(startPosition - decrement, indexOfExpression, expression.length()))
			decrementFound = true;
		
		//searching right
		int increment = 0;
		boolean incrementFound = false;
		while(!statement.substring(startPosition + increment, startPosition + increment + lookFor.length()).equals(lookFor) &&
				(startPosition + increment) < (statement.length() - lookFor.length()) ) {
			increment++;
		}
		if(statement.substring(startPosition + increment, startPosition + increment + lookFor.length()).equals(lookFor) &&
				isWithinExpression(startPosition + increment, indexOfExpression, expression.length()))
			incrementFound = true;
		
		//update indices
		if(decrementFound && !incrementFound) {
			startPosition -= decrement;
		}
		else if(!decrementFound && incrementFound) {
			startPosition += increment;
		}
		else if(decrementFound && incrementFound) {
			if(decrement < increment) {
				startPosition -= decrement;
			}
			else if(increment < decrement) {
				startPosition += increment;
			}
			else {
				//increment1 = decrement1
			}
		}
		return startPosition;
	}
	
	private boolean isWithinExpression(int startPosition, int indexOfExpression, int lengthOfExpression) {
		return startPosition >= indexOfExpression && startPosition < indexOfExpression + lengthOfExpression;
	}
}
