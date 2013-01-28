package gr.uom.java.ast.decomposition;

import java.util.List;

import gr.uom.java.ast.ASTInformation;
import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.util.ExpressionExtractor;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;

public abstract class AbstractStatement extends AbstractMethodFragment {

	//private Statement statement;
	private ASTInformation statement;
	private CompositeStatementObject parent;
	private StatementType type;
    
    public AbstractStatement(Statement statement, StatementType type) {
    	//this.statement = statement;
    	super();
    	this.type = type;
    	this.startPosition = statement.getStartPosition();
    	this.length = statement.getLength();
    	this.entireString = statement.toString();
    	this.statement = ASTInformationGenerator.generateASTInformation(statement);
    	this.parent = null;
        
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        List<Expression> assignments = expressionExtractor.getAssignments(statement);
        List<Expression> postfixExpressions = expressionExtractor.getPostfixExpressions(statement);
        List<Expression> prefixExpressions = expressionExtractor.getPrefixExpressions(statement);
        processVariables(expressionExtractor.getVariableInstructions(statement), assignments, postfixExpressions, prefixExpressions);
		processMethodInvocations(expressionExtractor.getMethodInvocations(statement));
		processClassInstanceCreations(expressionExtractor.getClassInstanceCreations(statement));
		processArrayCreations(expressionExtractor.getArrayCreations(statement));
		processLiterals(expressionExtractor.getLiterals(statement));
    }

    public void setParent(CompositeStatementObject parent) {
    	this.parent = parent;
    }

    public CompositeStatementObject getParent() {
    	return this.parent;
    }

    public Statement getStatement() {
    	//return statement;
    	return (Statement)this.statement.recoverASTNode();
    }

	public StatementType getType() {
		return type;
	}

	public abstract List<String> stringRepresentation();

	public ASTNodeDifference checkEquivalence(AbstractStatement s) {
		ASTNodeDifference parentNodeDifference = new ASTNodeDifference(this,s);
		if(!this.getType().equals(s.getType()))
		{
			Difference difference = new Difference(this.toString(),s.toString(),DifferenceType.AST_TYPE_MISMATCH);
			parentNodeDifference.addDifference(difference);
			return parentNodeDifference;
		}
		else
		{	
			List<AbstractExpression> srcExpressionList = this.getExpressions();
			List<AbstractExpression> tgtExpressionList = s.getExpressions();	
			if (srcExpressionList.size() != tgtExpressionList.size())
			{
				Difference difference = new Difference(this.toString(),s.toString(),DifferenceType.EXPRESSION_NUMBER_MISMATCH);
				parentNodeDifference.addDifference(difference);
				return parentNodeDifference;
			}
			else
			{
				Difference difference = new Difference(this.toString(),s.toString(),DifferenceType.AST_TYPE_MATCH);
				parentNodeDifference.addDifference(difference);			
				for(int i=0;i<srcExpressionList.size();i++)
				{
					ASTNodeDifference childDifference = srcExpressionList.get(i).checkEquivalence(tgtExpressionList.get(i));
					parentNodeDifference.addChild(childDifference);
				}
				return parentNodeDifference;
			}				
		}
	}
}
