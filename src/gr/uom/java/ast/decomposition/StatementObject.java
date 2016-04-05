package gr.uom.java.ast.decomposition;

import gr.uom.java.ast.util.ExpressionExtractor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;

/*
 * StatementObject represents the following AST Statement subclasses:
 * 1.	ExpressionStatement
 * 2.	VariableDeclarationStatement
 * 3.	ConstructorInvocation
 * 4.	SuperConstructorInvocation
 * 5.	ReturnStatement
 * 6.	AssertStatement
 * 7.	BreakStatement
 * 8.	ContinueStatement
 * 9.	SwitchCase
 * 10.	EmptyStatement
 * 11.	ThrowStatement
 */

public class StatementObject extends AbstractStatement {
	
	public StatementObject(Statement statement, StatementType type, AbstractMethodFragment parent) {
		super(statement, type, parent);
		
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        List<Expression> assignments = expressionExtractor.getAssignments(statement);
        List<Expression> postfixExpressions = expressionExtractor.getPostfixExpressions(statement);
        List<Expression> prefixExpressions = expressionExtractor.getPrefixExpressions(statement);
        processVariables(expressionExtractor.getVariableInstructions(statement), assignments, postfixExpressions, prefixExpressions);
		processMethodInvocations(expressionExtractor.getMethodInvocations(statement));
		processClassInstanceCreations(expressionExtractor.getClassInstanceCreations(statement));
		processArrayCreations(expressionExtractor.getArrayCreations(statement));
		//processArrayAccesses(expressionExtractor.getArrayAccesses(statement));
		processLiterals(expressionExtractor.getLiterals(statement));
		if(statement instanceof ThrowStatement) {
			processThrowStatement((ThrowStatement)statement);
		}
		if(statement instanceof ConstructorInvocation) {
			processConstructorInvocation((ConstructorInvocation)statement);
		}
	}

	public String toString() {
		return getStatement().toString();
	}

	public List<String> stringRepresentation() {
		List<String> stringRepresentation = new ArrayList<String>();
		stringRepresentation.add(this.toString());
		return stringRepresentation;
	}
}
