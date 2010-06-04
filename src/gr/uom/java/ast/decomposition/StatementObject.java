package gr.uom.java.ast.decomposition;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.MethodDeclarationUtility;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;

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
	
	public StatementObject(Statement statement) {
		super(statement);
	}

	public List<Assignment> getFieldAssignments(FieldInstructionObject fio) {
		List<Assignment> fieldAssignments = new ArrayList<Assignment>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> assignments = expressionExtractor.getAssignments(getStatement());
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
		List<Expression> postfixExpressions = expressionExtractor.getPostfixExpressions(getStatement());
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
		List<Expression> prefixExpressions = expressionExtractor.getPrefixExpressions(getStatement());
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
}
