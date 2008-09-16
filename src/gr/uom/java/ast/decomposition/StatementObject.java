package gr.uom.java.ast.decomposition;

import java.util.List;

import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.util.ExpressionExtractor;

import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
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
	
	public boolean isLocalVariableAssignment(LocalVariableInstructionObject lvio) {
		if( getLocalVariableInstructions().contains(lvio) ) {
			Statement statement = getStatement();
			if(statement instanceof ExpressionStatement) {
				ExpressionStatement expressionStatement = (ExpressionStatement)statement;
				Expression expression = expressionStatement.getExpression();
				if(expression instanceof Assignment) {
					Assignment assignment = (Assignment)expression;
					Expression leftHandSide = assignment.getLeftHandSide();
					SimpleName leftHandSideName = processExpression(leftHandSide);
					if(leftHandSideName != null) {
						IBinding leftHandSideBinding = leftHandSideName.resolveBinding();
						if(leftHandSideBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding leftHandSideVariableBinding = (IVariableBinding)leftHandSideBinding;
							if(!leftHandSideVariableBinding.isField() && !leftHandSideVariableBinding.isParameter()) {
								if(leftHandSideName.getIdentifier().equals(lvio.getName()))
									return true;
							}
						}
					}
				}
				else if(expression instanceof PostfixExpression) {
					PostfixExpression postfixExpression = (PostfixExpression)expression;
					Expression operand = postfixExpression.getOperand();
					SimpleName operandName = processExpression(operand);
					if(operandName != null) {
						IBinding operandBinding = operandName.resolveBinding();
						if(operandBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding operandVariableBinding = (IVariableBinding)operandBinding;
							if(!operandVariableBinding.isField() && !operandVariableBinding.isParameter()) {
								if(operandName.getIdentifier().equals(lvio.getName()))
									return true;
							}
						}
					}
				}
				else if(expression instanceof PrefixExpression) {
					PrefixExpression prefixExpression = (PrefixExpression)expression;
					Expression operand = prefixExpression.getOperand();
					PrefixExpression.Operator operator = prefixExpression.getOperator();
					SimpleName operandName = processExpression(operand);
					if(operandName != null && (operator.equals(PrefixExpression.Operator.INCREMENT) ||
							operator.equals(PrefixExpression.Operator.DECREMENT))) {
						IBinding operandBinding = operandName.resolveBinding();
						if(operandBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding operandVariableBinding = (IVariableBinding)operandBinding;
							if(!operandVariableBinding.isField() && !operandVariableBinding.isParameter()) {
								if(operandName.getIdentifier().equals(lvio.getName()))
									return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	public Assignment containsLocalVariableAssignment(LocalVariableInstructionObject lvio) {
		Statement statement = getStatement();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> assignments = expressionExtractor.getAssignments(statement);
		for(Expression expression : assignments) {
			Assignment assignment = (Assignment)expression;
			Expression leftHandSide = assignment.getLeftHandSide();
			SimpleName leftHandSideName = processExpression(leftHandSide);
			if(leftHandSideName != null) {
				IBinding leftHandSideBinding = leftHandSideName.resolveBinding();
				if(leftHandSideBinding.getKind() == IBinding.VARIABLE) {
					IVariableBinding leftHandSideVariableBinding = (IVariableBinding)leftHandSideBinding;
					if(!leftHandSideVariableBinding.isField() && !leftHandSideVariableBinding.isParameter()) {
						if(leftHandSideName.getIdentifier().equals(lvio.getName()))
							return assignment;
					}
				}
			}
		}
		return null;
	}
	
	public PostfixExpression containsLocalVariablePostfixAssignment(LocalVariableInstructionObject lvio) {
		Statement statement = getStatement();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> postfixExpressions = expressionExtractor.getPostfixExpressions(statement);
		for(Expression expression : postfixExpressions) {
			PostfixExpression postfixExpression = (PostfixExpression)expression;
			Expression operand = postfixExpression.getOperand();
			SimpleName operandName = processExpression(operand);
			if(operandName != null) {
				IBinding operandBinding = operandName.resolveBinding();
				if(operandBinding.getKind() == IBinding.VARIABLE) {
					IVariableBinding operandVariableBinding = (IVariableBinding)operandBinding;
					if(!operandVariableBinding.isField() && !operandVariableBinding.isParameter()) {
						if(operandName.getIdentifier().equals(lvio.getName()))
							return postfixExpression;
					}
				}
			}
		}
		return null;
	}
	
	public PrefixExpression containsLocalVariablePrefixAssignment(LocalVariableInstructionObject lvio) {
		Statement statement = getStatement();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> prefixExpressions = expressionExtractor.getPrefixExpressions(statement);
		for(Expression expression : prefixExpressions) {
			PrefixExpression prefixExpression = (PrefixExpression)expression;
			Expression operand = prefixExpression.getOperand();
			PrefixExpression.Operator operator = prefixExpression.getOperator();
			SimpleName operandName = processExpression(operand);
			if(operandName != null && (operator.equals(PrefixExpression.Operator.INCREMENT) ||
					operator.equals(PrefixExpression.Operator.DECREMENT))) {
				IBinding operandBinding = operandName.resolveBinding();
				if(operandBinding.getKind() == IBinding.VARIABLE) {
					IVariableBinding operandVariableBinding = (IVariableBinding)operandBinding;
					if(!operandVariableBinding.isField() && !operandVariableBinding.isParameter()) {
						if(operandName.getIdentifier().equals(lvio.getName()))
							return prefixExpression;
					}
				}
			}
		}
		return null;
	}
	
	public boolean isFieldAssignment(FieldInstructionObject fio) {
		if( getFieldInstructions().contains(fio) ) {
			Statement statement = getStatement();
			if(statement instanceof ExpressionStatement) {
				ExpressionStatement expressionStatement = (ExpressionStatement)statement;
				Expression expression = expressionStatement.getExpression();
				if(expression instanceof Assignment) {
					Assignment assignment = (Assignment)expression;
					Expression leftHandSide = assignment.getLeftHandSide();
					SimpleName leftHandSideName = processExpression(leftHandSide);
					if(leftHandSideName != null) {
						IBinding leftHandSideBinding = leftHandSideName.resolveBinding();
						if(leftHandSideBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding leftHandSideVariableBinding = (IVariableBinding)leftHandSideBinding;
							if(leftHandSideVariableBinding.isField()) {
								if(leftHandSideName.getIdentifier().equals(fio.getName()))
									return true;
							}
						}
					}
				}
				else if(expression instanceof PostfixExpression) {
					PostfixExpression postfixExpression = (PostfixExpression)expression;
					Expression operand = postfixExpression.getOperand();
					SimpleName operandName = processExpression(operand);
					if(operandName != null) {
						IBinding operandBinding = operandName.resolveBinding();
						if(operandBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding operandVariableBinding = (IVariableBinding)operandBinding;
							if(operandVariableBinding.isField()) {
								if(operandName.getIdentifier().equals(fio.getName()))
									return true;
							}
						}
					}
				}
				else if(expression instanceof PrefixExpression) {
					PrefixExpression prefixExpression = (PrefixExpression)expression;
					Expression operand = prefixExpression.getOperand();
					PrefixExpression.Operator operator = prefixExpression.getOperator();
					SimpleName operandName = processExpression(operand);
					if(operandName != null && (operator.equals(PrefixExpression.Operator.INCREMENT) ||
							operator.equals(PrefixExpression.Operator.DECREMENT))) {
						IBinding operandBinding = operandName.resolveBinding();
						if(operandBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding operandVariableBinding = (IVariableBinding)operandBinding;
							if(operandVariableBinding.isField()) {
								if(operandName.getIdentifier().equals(fio.getName()))
									return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private SimpleName processExpression(Expression expression) {
		SimpleName simpleName = null;
		if(expression instanceof SimpleName) {
			simpleName = (SimpleName)expression;
		}
		else if(expression instanceof QualifiedName) {
			QualifiedName leftHandSideQualifiedName = (QualifiedName)expression;
			simpleName = leftHandSideQualifiedName.getName();
		}
		else if(expression instanceof FieldAccess) {
			FieldAccess leftHandSideFieldAccess = (FieldAccess)expression;
			simpleName = leftHandSideFieldAccess.getName();
		}
		else if(expression instanceof ArrayAccess) {
			ArrayAccess leftHandSideArrayAccess = (ArrayAccess)expression;
			Expression array = leftHandSideArrayAccess.getArray();
			if(array instanceof SimpleName) {
				simpleName = (SimpleName)array;
			}
			else if(array instanceof QualifiedName) {
				QualifiedName arrayQualifiedName = (QualifiedName)array;
				simpleName = arrayQualifiedName.getName();
			}
			else if(array instanceof FieldAccess) {
				FieldAccess arrayFieldAccess = (FieldAccess)array;
				simpleName = arrayFieldAccess.getName();
			}
		}
		return simpleName;
	}
}
