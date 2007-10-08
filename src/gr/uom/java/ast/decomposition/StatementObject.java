package gr.uom.java.ast.decomposition;

import java.util.List;
import java.util.Set;

import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.util.ExpressionExtractor;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
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
	
	public boolean isLocalVariableAssignment(LocalVariableDeclarationObject lvdo) {
		LocalVariableInstructionObject lvio = lvdo.generateLocalVariableInstruction();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		if( getLocalVariableInstructions().contains(lvio) ) {
			Statement statement = getStatement();
			if(statement instanceof ExpressionStatement) {
				ExpressionStatement expressionStatement = (ExpressionStatement)statement;
				Expression expression = expressionStatement.getExpression();
				if(expression instanceof Assignment) {
					Assignment assignment = (Assignment)expression;
					List<Expression> expressionList = expressionExtractor.getVariableInstructions(assignment.getLeftHandSide());
					for(Expression e : expressionList) {
						if( ((SimpleName)e).getIdentifier().equals(lvdo.getName()) )
							return true;
					}
				}
				else if(expression instanceof PostfixExpression) {
					PostfixExpression postfixExpression = (PostfixExpression)expression;
					List<Expression> expressionList = expressionExtractor.getVariableInstructions(postfixExpression.getOperand());
					for(Expression e : expressionList) {
						if( ((SimpleName)e).getIdentifier().equals(lvdo.getName()) )
							return true;
					}
				}
				else if(expression instanceof PrefixExpression) {
					PrefixExpression prefixExpression = (PrefixExpression)expression;
					List<Expression> expressionList = expressionExtractor.getVariableInstructions(prefixExpression.getOperand());
					for(Expression e : expressionList) {
						if( ((SimpleName)e).getIdentifier().equals(lvdo.getName()) )
							return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean isFieldAssignment(FieldInstructionObject fio) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		if( getFieldInstructions().contains(fio) ) {
			Statement statement = getStatement();
			if(statement instanceof ExpressionStatement) {
				ExpressionStatement expressionStatement = (ExpressionStatement)statement;
				Expression expression = expressionStatement.getExpression();
				if(expression instanceof Assignment) {
					Assignment assignment = (Assignment)expression;
					List<Expression> expressionList = expressionExtractor.getVariableInstructions(assignment.getLeftHandSide());
					for(Expression e : expressionList) {
						if( ((SimpleName)e).getIdentifier().equals(fio.getName()) )
							return true;
					}
				}
				else if(expression instanceof PostfixExpression) {
					PostfixExpression postfixExpression = (PostfixExpression)expression;
					List<Expression> expressionList = expressionExtractor.getVariableInstructions(postfixExpression.getOperand());
					for(Expression e : expressionList) {
						if( ((SimpleName)e).getIdentifier().equals(fio.getName()) )
							return true;
					}
				}
				else if(expression instanceof PrefixExpression) {
					PrefixExpression prefixExpression = (PrefixExpression)expression;
					List<Expression> expressionList = expressionExtractor.getVariableInstructions(prefixExpression.getOperand());
					for(Expression e : expressionList) {
						if( ((SimpleName)e).getIdentifier().equals(fio.getName()) )
							return true;
					}
				}
			}
		}
		return false;
	}

	public boolean isLocalVariableAssignment(Set<LocalVariableDeclarationObject> set) {
		for(LocalVariableDeclarationObject lvdo : set) {
			if(isLocalVariableAssignment(lvdo))
				return true;
		}
		return false;
	}
}
