package gr.uom.java.ast.decomposition;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.ast.ASTInformation;
import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.MethodDeclarationUtility;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;

public class AbstractExpression extends AbstractMethodFragment {
	
	//private Expression expression;
	private ASTInformation expression;
	private CompositeStatementObject owner;
    
    public AbstractExpression(Expression expression) {
    	//this.expression = expression;
    	super();
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
    }

    public void setOwner(CompositeStatementObject owner) {
    	this.owner = owner;
    }

    public CompositeStatementObject getOwner() {
    	return this.owner;
    }

    public Expression getExpression() {
    	//return expression;
    	return (Expression)this.expression.recoverASTNode();
    }

    public List<Assignment> getFieldAssignments(FieldInstructionObject fio) {
    	List<Assignment> fieldAssignments = new ArrayList<Assignment>();
    	ExpressionExtractor expressionExtractor = new ExpressionExtractor();
    	List<Expression> assignments = expressionExtractor.getAssignments(getExpression());
    	for(Expression expression : assignments) {
    		Assignment assignment = (Assignment)expression;
    		Expression leftHandSide = assignment.getLeftHandSide();
    		SimpleName leftHandSideName = MethodDeclarationUtility.getRightMostSimpleName(leftHandSide);
    		if(leftHandSideName != null && leftHandSideName.equals(fio.getSimpleName())) {
    			fieldAssignments.add(assignment);
    		}
    	}
    	return fieldAssignments;
    }

    public List<PostfixExpression> getFieldPostfixAssignments(FieldInstructionObject fio) {
    	List<PostfixExpression> fieldPostfixAssignments = new ArrayList<PostfixExpression>();
    	ExpressionExtractor expressionExtractor = new ExpressionExtractor();
    	List<Expression> postfixExpressions = expressionExtractor.getPostfixExpressions(getExpression());
    	for(Expression expression : postfixExpressions) {
    		PostfixExpression postfixExpression = (PostfixExpression)expression;
    		Expression operand = postfixExpression.getOperand();
    		SimpleName operandName = MethodDeclarationUtility.getRightMostSimpleName(operand);
    		if(operandName != null && operandName.equals(fio.getSimpleName())) {
    			fieldPostfixAssignments.add(postfixExpression);
    		}
    	}
    	return fieldPostfixAssignments;
    }

    public List<PrefixExpression> getFieldPrefixAssignments(FieldInstructionObject fio) {
    	List<PrefixExpression> fieldPrefixAssignments = new ArrayList<PrefixExpression>();
    	ExpressionExtractor expressionExtractor = new ExpressionExtractor();
    	List<Expression> prefixExpressions = expressionExtractor.getPrefixExpressions(getExpression());
    	for(Expression expression : prefixExpressions) {
    		PrefixExpression prefixExpression = (PrefixExpression)expression;
    		Expression operand = prefixExpression.getOperand();
    		PrefixExpression.Operator operator = prefixExpression.getOperator();
    		SimpleName operandName = MethodDeclarationUtility.getRightMostSimpleName(operand);
    		if(operandName != null && operandName.equals(fio.getSimpleName()) && (operator.equals(PrefixExpression.Operator.INCREMENT) ||
    				operator.equals(PrefixExpression.Operator.DECREMENT))) {
    			fieldPrefixAssignments.add(prefixExpression);
    		}
    	}
    	return fieldPrefixAssignments;
    }

	/*public boolean equals(Object o) {
		if(this == o)
			return true;

		if(o instanceof AbstractExpression) {
			AbstractExpression abstractExpression = (AbstractExpression)o;
			return this.expression.equals(abstractExpression.expression);
		}
		return false;
	}

	public int hashCode() {
		return expression.hashCode();
	}*/

	public String toString() {
		return expression.toString();
	}
}
