package gr.uom.java.ast.decomposition;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.ast.ASTInformation;
import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LiteralObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.util.ExpressionExtractor;

import org.eclipse.jdt.core.dom.Expression;

public class AbstractExpression extends AbstractMethodFragment {

	//private Expression expression;
	private ASTInformation expression;
	private AbstractMethodFragment owner;
	private ExpressionType type;

	public AbstractExpression(Expression expression, ExpressionType type) {
		//this.expression = expression;
		super();
		this.type = type;
		this.startPosition = expression.getStartPosition();
		this.length = expression.getLength();
		this.entireString = expression.toString();
		this.expression = ASTInformationGenerator.generateASTInformation(expression);
		this.owner = null;

		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> assignments = expressionExtractor.getAssignments(expression);
		List<Expression> postfixExpressions = expressionExtractor.getPostfixExpressions(expression);
		List<Expression> prefixExpressions = expressionExtractor.getPrefixExpressions(expression);
		processVariables(expressionExtractor.getVariableInstructions(expression), assignments, postfixExpressions, prefixExpressions);
		processMethodInvocations(expressionExtractor.getMethodInvocations(expression));
		processClassInstanceCreations(expressionExtractor.getClassInstanceCreations(expression));
		processArrayCreations(expressionExtractor.getArrayCreations(expression));
		processLiterals(expressionExtractor.getLiterals(expression));
	}

	public void setOwner(AbstractMethodFragment owner) {
		this.owner = owner;
	}

	public AbstractMethodFragment getOwner() {
		return this.owner;
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

	public ASTNodeDifference checkEquivalence(AbstractExpression e) {
		ASTNodeDifference parentNodeDifference = new ASTNodeDifference(this,e);
		if(!this.getType().equals(e.getType()))
		{
			Difference difference = new Difference(this.toString(),e.toString(),DifferenceType.AST_TYPE_MISMATCH);
			parentNodeDifference.addDifference(difference);
			if(this.isTypeHolder() && e.isTypeHolder())
			{
				//TODO
			}
		}		
		else
		{
			List<AbstractExpression> srcExpressionList = this.getExpressions();
			List<AbstractExpression> tgtExpressionList = e.getExpressions();	
			//leaf nodes
			if (srcExpressionList.size() == 0 && tgtExpressionList.size() == 0)
			{
				if(!this.getType().equals(e.getType()))
				{	
					Difference difference = new Difference(this.toString(),e.toString(),DifferenceType.AST_TYPE_MISMATCH);
					parentNodeDifference.addDifference(difference);
				}
				else 
				{
					List<Difference> differences = this.extractDifferences(e);
					if(differences.isEmpty()) 
					{
						Difference difference = new Difference(this.toString(),e.toString(),DifferenceType.EXACT_MATCH);
						differences.add(difference);
					}
					parentNodeDifference.addDifferences(differences);
				}
			}
			else //two non-leaf nodes
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
	
	// method to check if an expression holds the type
	public boolean isTypeHolder()
	{
		ExpressionType expType = this.getType();
		if(expType.equals(ExpressionType.NUMBER_LITERAL)
				||expType.equals(ExpressionType.METHOD_INVOCATION)
					||expType.equals(ExpressionType.SUPER_METHOD_INVOCATION)
						||expType.equals(ExpressionType.SUPER_FIELD_ACCESS)
							||expType.equals(ExpressionType.STRING_LITERAL)
								||expType.equals(ExpressionType.SIMPLE_NAME)
									||expType.equals(ExpressionType.FIELD_ACCESS)
										||expType.equals(ExpressionType.BOOLEAN_LITERAL)
											||expType.equals(ExpressionType.CHARACTER_LITERAL))
												return true;
		return false;
	}
	
}
