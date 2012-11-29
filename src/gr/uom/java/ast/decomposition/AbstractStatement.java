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
    
    public AbstractStatement(Statement statement) {
    	//this.statement = statement;
    	super();
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
    
    public abstract List<String> stringRepresentation();

	public boolean isEquivalent(AbstractStatement s) {
		if(this instanceof CompositeStatementObject && s instanceof CompositeStatementObject)
			return ((CompositeStatementObject)this).isEquivalent((CompositeStatementObject)s);
		else if(this instanceof StatementObject && s instanceof StatementObject)
			return ((StatementObject)this).isEquivalent((StatementObject)s);
		return false;
	}
}
