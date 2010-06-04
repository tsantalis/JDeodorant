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

	/*public boolean equals(Object o) {
		if(this == o)
    		return true;
    	
    	if(o instanceof AbstractStatement) {
    		AbstractStatement abstractStatement = (AbstractStatement)o;
    		return this.statement.equals(abstractStatement.statement);
    	}
    	return false;
	}

	public int hashCode() {
		return statement.hashCode();
	}*/

	public String toString() {
		return statement.toString();
	}
}
