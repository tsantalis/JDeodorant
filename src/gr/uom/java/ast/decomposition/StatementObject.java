package gr.uom.java.ast.decomposition;

import java.util.Set;

import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;

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
	
	public boolean isLocalVariableAssignment(LocalVariableDeclarationObject lvdo) {
		LocalVariableInstructionObject lvio = lvdo.generateLocalVariableInstruction();
		if( getLocalVariableInstructions().contains(lvio) ) {
			Statement statement = getStatement();
			if(statement instanceof ExpressionStatement) {
				ExpressionStatement expressionStatement = (ExpressionStatement)statement;
				Expression expression = expressionStatement.getExpression();
				if(expression instanceof Assignment) {
					Assignment assignment = (Assignment)expression;
					Expression leftHandSide = assignment.getLeftHandSide();
					if(leftHandSide instanceof SimpleName) {
						SimpleName leftHandSideSimpleName = (SimpleName)leftHandSide;
						IBinding leftHandSideBinding = leftHandSideSimpleName.resolveBinding();
						if(leftHandSideBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding leftHandSideVariableBinding = (IVariableBinding)leftHandSideBinding;
							if(!leftHandSideVariableBinding.isField() && !leftHandSideVariableBinding.isParameter()) {
								if(leftHandSideSimpleName.getIdentifier().equals(lvdo.getName()))
									return true;
							}
						}
					}
				}
				else if(expression instanceof PostfixExpression) {
					PostfixExpression postfixExpression = (PostfixExpression)expression;
					Expression operand = postfixExpression.getOperand();
					if(operand instanceof SimpleName) {
						SimpleName operandSimpleName = (SimpleName)operand;
						IBinding operandBinding = operandSimpleName.resolveBinding();
						if(operandBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding operandVariableBinding = (IVariableBinding)operandBinding;
							if(!operandVariableBinding.isField() && !operandVariableBinding.isParameter()) {
								if(operandSimpleName.getIdentifier().equals(lvdo.getName()))
									return true;
							}
						}
					}
				}
				else if(expression instanceof PrefixExpression) {
					PrefixExpression prefixExpression = (PrefixExpression)expression;
					Expression operand = prefixExpression.getOperand();
					if(operand instanceof SimpleName) {
						SimpleName operandSimpleName = (SimpleName)operand;
						IBinding operandBinding = operandSimpleName.resolveBinding();
						if(operandBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding operandVariableBinding = (IVariableBinding)operandBinding;
							if(!operandVariableBinding.isField() && !operandVariableBinding.isParameter()) {
								if(operandSimpleName.getIdentifier().equals(lvdo.getName()))
									return true;
							}
						}
					}
				}
			}
		}
		return false;
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
					SimpleName leftHandSideName = null;
					if(leftHandSide instanceof SimpleName) {
						leftHandSideName = (SimpleName)leftHandSide;
					}
					else if(leftHandSide instanceof QualifiedName) {
						QualifiedName leftHandSideQualifiedName = (QualifiedName)leftHandSide;
						leftHandSideName = leftHandSideQualifiedName.getName();
					}
					else if(leftHandSide instanceof FieldAccess) {
						FieldAccess leftHandSideFieldAccess = (FieldAccess)leftHandSide;
						leftHandSideName = leftHandSideFieldAccess.getName();
					}
					else if(leftHandSide instanceof ArrayAccess) {
						ArrayAccess leftHandSideArrayAccess = (ArrayAccess)leftHandSide;
						Expression array = leftHandSideArrayAccess.getArray();
						if(array instanceof SimpleName) {
							leftHandSideName = (SimpleName)array;
						}
						else if(array instanceof QualifiedName) {
							QualifiedName arrayQualifiedName = (QualifiedName)array;
							leftHandSideName = arrayQualifiedName.getName();
						}
						else if(array instanceof FieldAccess) {
							FieldAccess arrayFieldAccess = (FieldAccess)array;
							leftHandSideName = arrayFieldAccess.getName();
						}
					}
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
					SimpleName operandName = null;
					if(operand instanceof SimpleName) {
						operandName = (SimpleName)operand;
					}
					else if(operand instanceof QualifiedName) {
						QualifiedName operandQualifiedName = (QualifiedName)operand;
						operandName = operandQualifiedName.getName();
					}
					else if(operand instanceof FieldAccess) {
						FieldAccess operandFieldAccess = (FieldAccess)operand;
						operandName = operandFieldAccess.getName();
					}
					else if(operand instanceof ArrayAccess) {
						ArrayAccess operandArrayAccess = (ArrayAccess)operand;
						Expression array = operandArrayAccess.getArray();
						if(array instanceof SimpleName) {
							operandName = (SimpleName)array;
						}
						else if(array instanceof QualifiedName) {
							QualifiedName arrayQualifiedName = (QualifiedName)array;
							operandName = arrayQualifiedName.getName();
						}
						else if(array instanceof FieldAccess) {
							FieldAccess arrayFieldAccess = (FieldAccess)array;
							operandName = arrayFieldAccess.getName();
						}
					}
					if(operandName != null) {
						IBinding opernadBinding = operandName.resolveBinding();
						if(opernadBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding operandVariableBinding = (IVariableBinding)opernadBinding;
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
					SimpleName operandName = null;
					if(operand instanceof SimpleName) {
						operandName = (SimpleName)operand;
					}
					else if(operand instanceof QualifiedName) {
						QualifiedName operandQualifiedName = (QualifiedName)operand;
						operandName = operandQualifiedName.getName();
					}
					else if(operand instanceof FieldAccess) {
						FieldAccess operandFieldAccess = (FieldAccess)operand;
						operandName = operandFieldAccess.getName();
					}
					else if(operand instanceof ArrayAccess) {
						ArrayAccess operandArrayAccess = (ArrayAccess)operand;
						Expression array = operandArrayAccess.getArray();
						if(array instanceof SimpleName) {
							operandName = (SimpleName)array;
						}
						else if(array instanceof QualifiedName) {
							QualifiedName arrayQualifiedName = (QualifiedName)array;
							operandName = arrayQualifiedName.getName();
						}
						else if(array instanceof FieldAccess) {
							FieldAccess arrayFieldAccess = (FieldAccess)array;
							operandName = arrayFieldAccess.getName();
						}
					}
					if(operandName != null) {
						IBinding opernadBinding = operandName.resolveBinding();
						if(opernadBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding operandVariableBinding = (IVariableBinding)opernadBinding;
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

	public boolean isLocalVariableAssignment(Set<LocalVariableDeclarationObject> set) {
		for(LocalVariableDeclarationObject lvdo : set) {
			if(isLocalVariableAssignment(lvdo))
				return true;
		}
		return false;
	}
}
