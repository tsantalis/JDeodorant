package gr.uom.java.ast.decomposition;

import java.util.List;

import gr.uom.java.ast.ASTInformation;
import gr.uom.java.ast.ASTInformationGenerator;
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
}
