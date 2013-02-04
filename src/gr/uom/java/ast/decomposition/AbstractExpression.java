package gr.uom.java.ast.decomposition;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.ast.ASTInformation;
import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.ArrayAccessObject;
import gr.uom.java.ast.ArrayCreationObject;
import gr.uom.java.ast.ClassInstanceCreationObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LiteralObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.SuperFieldInstructionObject;
import gr.uom.java.ast.SuperMethodInvocationObject;
import gr.uom.java.ast.TypeHolder;
import gr.uom.java.ast.util.ExpressionExtractor;

import org.eclipse.jdt.core.dom.Expression;

public class AbstractExpression extends AbstractMethodFragment {

	//private Expression expression;
	private ASTInformation expression;
	private ExpressionType type;

	public AbstractExpression(Expression expression, ExpressionType type, AbstractMethodFragment parent) {
		//this.expression = expression;
		super(parent);
		this.type = type;
		this.startPosition = expression.getStartPosition();
		this.length = expression.getLength();
		this.entireString = expression.toString();
		this.expression = ASTInformationGenerator.generateASTInformation(expression);

		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		if(type.equals(ExpressionType.FIELD_ACCESS) || type.equals(ExpressionType.SUPER_FIELD_ACCESS) ||
				type.equals(ExpressionType.SIMPLE_NAME) || type.equals(ExpressionType.QUALIFIED_NAME)) {
			Expression topExpression = getTopExpression(expression);
			List<Expression> assignments = expressionExtractor.getAssignments(topExpression);
			List<Expression> postfixExpressions = expressionExtractor.getPostfixExpressions(topExpression);
			List<Expression> prefixExpressions = expressionExtractor.getPrefixExpressions(topExpression);
			processVariables(expressionExtractor.getVariableInstructions(expression), assignments, postfixExpressions, prefixExpressions);
		}
		if(type.equals(ExpressionType.METHOD_INVOCATION) || type.equals(ExpressionType.SUPER_METHOD_INVOCATION)) {
			processMethodInvocations(expressionExtractor.getMethodInvocations(expression));
		}
		if(type.equals(ExpressionType.CLASS_INSTANCE_CREATION)) {
			processClassInstanceCreations(expressionExtractor.getClassInstanceCreations(expression));
		}
		if(type.equals(ExpressionType.ARRAY_CREATION)) {
			processArrayCreations(expressionExtractor.getArrayCreations(expression));
		}
		if(type.equals(ExpressionType.ARRAY_ACCESS)) {
			processArrayAccesses(expressionExtractor.getArrayAccesses(expression));
		}
		if(type.equals(ExpressionType.NUMBER_LITERAL) || type.equals(ExpressionType.STRING_LITERAL) || type.equals(ExpressionType.NULL_LITERAL) ||
				type.equals(ExpressionType.CHARACTER_LITERAL) || type.equals(ExpressionType.BOOLEAN_LITERAL) || type.equals(ExpressionType.TYPE_LITERAL)) {
			processLiterals(expressionExtractor.getLiterals(expression));
		}
	}

	public Expression getExpression() {
		//return expression;
		return (Expression)this.expression.recoverASTNode();
	}

	public ExpressionType getType() {
		return type;
	}

	public String toString() {
		//return getExpression().toString();
		return getEntireString();
	}

	private Expression getTopExpression(Expression expression) {
		Expression topExpression = expression;
		while(topExpression.getParent() instanceof Expression) {
			topExpression = (Expression)topExpression.getParent();
		}
		return topExpression;
	}

	private TypeHolder getTopLevelTypeHolder() {
		if(type.equals(ExpressionType.METHOD_INVOCATION)) {
			return getMethodInvocations().get(0);
		}
		else if(type.equals(ExpressionType.SUPER_METHOD_INVOCATION)) {
			return getSuperMethodInvocations().get(0);
		}
		else if(type.equals(ExpressionType.NUMBER_LITERAL) || type.equals(ExpressionType.STRING_LITERAL) ||
				type.equals(ExpressionType.CHARACTER_LITERAL) || type.equals(ExpressionType.BOOLEAN_LITERAL) || type.equals(ExpressionType.TYPE_LITERAL)) {
			return getLiterals().get(0);
		}
		else if(type.equals(ExpressionType.ARRAY_CREATION)) {
			return getArrayCreations().get(0);
		}
		else if(type.equals(ExpressionType.CLASS_INSTANCE_CREATION)) {
			return getClassInstanceCreations().get(0);
		}
		else if(type.equals(ExpressionType.ARRAY_ACCESS)) {
			return getArrayAccesses().get(0);
		}
		else if(type.equals(ExpressionType.FIELD_ACCESS)) {
			return getFieldInstructions().get(0);
		}
		else if(type.equals(ExpressionType.SUPER_FIELD_ACCESS)) {
			return getSuperFieldInstructions().get(0);
		}
		else if(type.equals(ExpressionType.SIMPLE_NAME)) {
			List<FieldInstructionObject> fieldInstructions = getFieldInstructions();
			List<LocalVariableInstructionObject> localVariableInstructions = getLocalVariableInstructions();
			if(fieldInstructions.isEmpty() && !localVariableInstructions.isEmpty()) {
				return localVariableInstructions.get(0);
			}
			if(!fieldInstructions.isEmpty() && localVariableInstructions.isEmpty()) {
				return fieldInstructions.get(0);
			}
		}
		else if(type.equals(ExpressionType.QUALIFIED_NAME)) {
			List<FieldInstructionObject> fieldInstructions = getFieldInstructions();
			if(!fieldInstructions.isEmpty()) {
				return fieldInstructions.get(0);
			}
		}
		return null;
	}

	public ASTNodeDifference checkEquivalence(AbstractExpression e) {
		ASTNodeDifference parentNodeDifference = new ASTNodeDifference(this,e);
		if(!this.getType().equals(e.getType()))
		{
			List<AbstractExpression> srcExpressionList = this.getExpressions();
			List<AbstractExpression> tgtExpressionList = e.getExpressions();
			//two leaf nodes of different types
			if (srcExpressionList.size() == 0 && tgtExpressionList.size() == 0)
			{
				TypeHolder thisTypeHolder = this.getTopLevelTypeHolder();
				TypeHolder expTypeHolder = e.getTopLevelTypeHolder();
				if(thisTypeHolder != null && expTypeHolder != null)
				{
					if(thisTypeHolder.getType().equals(expTypeHolder.getType()))
					{
						Difference difference = new Difference(this.toString(),e.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
						parentNodeDifference.addDifference(difference);
					}
					else
					{
						Difference difference = new Difference(this.toString(),e.toString(),DifferenceType.AST_TYPE_MISMATCH);
						parentNodeDifference.addDifference(difference);
					}
				}
				else
				{
					Difference difference = new Difference(this.toString(),e.toString(),DifferenceType.AST_TYPE_MISMATCH);
					parentNodeDifference.addDifference(difference);
				}
			}
			else //two non-leaf nodes of different types or a non-leaf node vs. leaf node 
			{
				TypeHolder thisTypeHolder = this.getTopLevelTypeHolder();
				TypeHolder expTypeHolder = e.getTopLevelTypeHolder();
				if(thisTypeHolder != null && expTypeHolder != null)
				{
					if(thisTypeHolder.getType().equals(expTypeHolder.getType()))
					{
						Difference difference = new Difference(this.toString(),e.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
						parentNodeDifference.addDifference(difference);
					}	
					else
					{
						Difference difference = new Difference(this.toString(),e.toString(),DifferenceType.AST_TYPE_MISMATCH);
						parentNodeDifference.addDifference(difference);
					}
				}
				else
				{
					Difference difference = new Difference(this.toString(),e.toString(),DifferenceType.AST_TYPE_MISMATCH);
					parentNodeDifference.addDifference(difference);
				}	
			}
		}		
		else
		{
			List<AbstractExpression> srcExpressionList = this.getExpressions();
			List<AbstractExpression> tgtExpressionList = e.getExpressions();
			//two leaf nodes of the same type
			if (srcExpressionList.size() == 0 && tgtExpressionList.size() == 0)
			{
				List<Difference> differences = this.extractDifferences(e);
				if(differences.isEmpty()) 
				{
					Difference difference = new Difference(this.toString(),e.toString(),DifferenceType.EXACT_MATCH);
					differences.add(difference);
				}
				parentNodeDifference.addDifferences(differences);
			}
			else //two non-leaf nodes of the same type
			{
				if(srcExpressionList.size()!=tgtExpressionList.size())
				{
					Difference difference = new Difference(this.toString(),e.toString(),DifferenceType.EXPRESSION_NUMBER_MISMATCH);
					parentNodeDifference.addDifference(difference);
				}
				else
				{
					for(int i=0;i<srcExpressionList.size();i++)
					{
						parentNodeDifference.addChild(srcExpressionList.get(i).checkEquivalence(tgtExpressionList.get(i)));
					}
				}			
			}				
		}
		return parentNodeDifference;
	}

	private List<Difference> extractDifferences(AbstractExpression e) {
		List<Difference> differences = new ArrayList<Difference>();
		List<LocalVariableDeclarationObject> variableDeclarations1 = this.getLocalVariableDeclarations();
		List<LocalVariableDeclarationObject> variableDeclarations2 = e.getLocalVariableDeclarations();
		for(int i=0; i<variableDeclarations1.size(); i++) {
			LocalVariableDeclarationObject variableDeclaration1 = variableDeclarations1.get(i);
			LocalVariableDeclarationObject variableDeclaration2 = variableDeclarations2.get(i);
			if(!variableDeclaration1.getName().equals(variableDeclaration2.getName())) {
				Difference diff = new Difference(variableDeclaration1.getName(), variableDeclaration2.getName(),DifferenceType.VARIABLE_NAME_MISMATCH);
				differences.add(diff);
			}
			if(!variableDeclaration1.getType().equals(variableDeclaration2.getType())) {
				Difference diff = new Difference(variableDeclaration1.getType().toString(), variableDeclaration2.getType().toString(),DifferenceType.VARIABLE_TYPE_MISMATCH);
				differences.add(diff);
			}
		}

		List<LocalVariableInstructionObject> variableInstructions1 = this.getLocalVariableInstructions();
		List<LocalVariableInstructionObject> variableInstructions2 = e.getLocalVariableInstructions();
		for(int i=0; i<variableInstructions1.size(); i++) {
			LocalVariableInstructionObject variableInstruction1 = variableInstructions1.get(i);
			LocalVariableInstructionObject variableInstruction2 = variableInstructions2.get(i);
			if(!variableInstruction1.getName().equals(variableInstruction2.getName())) {
				Difference diff = new Difference(variableInstruction1.getName(), variableInstruction2.getName(),DifferenceType.VARIABLE_NAME_MISMATCH);
				differences.add(diff);
			}
			if(!variableInstruction1.getType().equals(variableInstruction2.getType())) {
				Difference diff = new Difference(variableInstruction1.getType().toString(), variableInstruction2.getType().toString(),DifferenceType.VARIABLE_TYPE_MISMATCH);
				differences.add(diff);
			}
		}

		List<FieldInstructionObject> fieldInstructions1 = this.getFieldInstructions();
		List<FieldInstructionObject> fieldInstructions2 = e.getFieldInstructions();
		for(int i=0; i<fieldInstructions1.size(); i++) {
			FieldInstructionObject fieldInstruction1 = fieldInstructions1.get(i);
			FieldInstructionObject fieldInstruction2 = fieldInstructions2.get(i);
			if(!fieldInstruction1.getName().equals(fieldInstruction2.getName())) {
				Difference diff = new Difference(fieldInstruction1.getName(), fieldInstruction2.getName(),DifferenceType.VARIABLE_NAME_MISMATCH);
				differences.add(diff);
			}
			if(!fieldInstruction1.getType().equals(fieldInstruction2.getType())) {
				Difference diff = new Difference(fieldInstruction1.getType().toString(), fieldInstruction2.getType().toString(),DifferenceType.VARIABLE_TYPE_MISMATCH);
				differences.add(diff);
			}
		}

		List<LiteralObject> literals1 = this.getLiterals();
		List<LiteralObject> literals2 = e.getLiterals();
		for(int i=0; i< literals1.size(); i++) {
			LiteralObject literal1 = literals1.get(i);
			LiteralObject literal2 = literals2.get(i);
			if(!literal1.getValue().equals(literal2.getValue())) {
				Difference diff = new Difference(literal1.getValue(), literal2.getValue(),DifferenceType.LITERAL_VALUE_MISMATCH);
				differences.add(diff);
			}
			if(!literal1.getType().equals(literal2.getType())) {
				Difference diff = new Difference(literal1.getType().toString(), literal2.getType().toString(),DifferenceType.LITERAL_TYPE_MISMATCH);
				differences.add(diff);
			}
		}
		return differences;
	}
}
