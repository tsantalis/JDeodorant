package gr.uom.java.ast.decomposition.matching.loop;

import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.StatementExtractor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

@SuppressWarnings("unchecked")
public class ConditionalLoopUtilities
{
	public static boolean isUpdatingVariable(Expression updater, SimpleName variable)
	{
		if (updater instanceof PrefixExpression)
		{
			PrefixExpression prefixExpression = (PrefixExpression) updater;
			return isSameVariable(prefixExpression.getOperand(), variable);
		}
		else if (updater instanceof PostfixExpression)
		{
			PostfixExpression postfixExpression = (PostfixExpression) updater;
			return isSameVariable(postfixExpression.getOperand(), variable);			
		}
		else if (updater instanceof Assignment)
		{
			Assignment assignment = (Assignment) updater;
			return isSameVariable(assignment.getLeftHandSide(), variable);
		}
		else if (updater instanceof MethodInvocation)
		{
			MethodInvocation methodInvocation = (MethodInvocation) updater;
			return isSameVariable(methodInvocation.getExpression(), variable);
		}
		return false;
	}
	
	private static boolean isSameVariable(Expression operand, SimpleName variable)
	{
		if (operand instanceof SimpleName)
		{
			SimpleName simpleNameOperand = (SimpleName) operand;
			return variable.resolveBinding().isEqualTo(simpleNameOperand.resolveBinding());
		}
		return false;
	}
	
	public static boolean isSizeInvocation(Expression expression)
	{
		if (expression instanceof MethodInvocation)
		{
			MethodInvocation methodInvocation = (MethodInvocation) expression;
			Expression callingExpression      = methodInvocation.getExpression();
			IMethodBinding methodBinding      = methodInvocation.resolveMethodBinding();
			if (methodBinding != null && callingExpression != null)
			{					
				return (methodBinding.getName().equals("size") && ConditionalLoopUtilities.isCollection(callingExpression));
			}
		}
		return false;
	}
	
	public static boolean isLengthFieldAccess(Expression expression)
	{
		if (expression instanceof QualifiedName)
		{
			QualifiedName qualifiedName = (QualifiedName) expression;
			SimpleName name             = qualifiedName.getName();
			Name qualifier              = qualifiedName.getQualifier();
			if (name != null && qualifier != null)
			{
				IBinding nameBinding = name.resolveBinding();
				ITypeBinding qualifierTypeBinding = qualifier.resolveTypeBinding();
				if (nameBinding != null && nameBinding.getKind() == IBinding.VARIABLE && qualifierTypeBinding != null)
				{
					IVariableBinding nameVariableBinding = (IVariableBinding) nameBinding;
					return (nameVariableBinding.getName().equals("length") && qualifierTypeBinding.isArray());
				}
			}
		}
		return false;
	}
	
	public static boolean isCollection(Expression variable)
	{
		return isSubclassOf(variable,"java.util.AbstractCollection");
	}
	
	public static boolean isSubclassOf(Expression expression, String qualifiedName)
	{
		ITypeBinding typeBinding = expression.resolveTypeBinding();
		do
		{
			if (typeBinding.getQualifiedName().startsWith(qualifiedName))
			{
				return true;
			}
			typeBinding = typeBinding.getSuperclass();
		} while (typeBinding != null);
		return false;
	}

	public static VariableDeclaration getVariableDeclaration(SimpleName variable)
	{
		VariableDeclaration variableDeclaration        = null;
		MethodDeclaration method                       = findParentMethodDeclaration(variable);
		List<VariableDeclaration> variableDeclarations = getAllVariableDeclarations(method);
		
		// find the variable's initializer
		IBinding binding = variable.resolveBinding();
		if (binding.getKind() == IBinding.VARIABLE)
		{
			IVariableBinding variableBinding = (IVariableBinding) binding;
			for (VariableDeclaration currentVariableDeclaration : variableDeclarations)
			{
				IVariableBinding currentVariableDeclarationBinding = currentVariableDeclaration.resolveBinding();
				if (currentVariableDeclarationBinding.isEqualTo(variableBinding))
				{
					variableDeclaration = currentVariableDeclaration;
					break;
				}
			}
		}
		return variableDeclaration;
	}
	
	private static List<VariableDeclaration> getAllVariableDeclarations(MethodDeclaration method)
	{
		StatementExtractor statementExtractor            = new StatementExtractor();
		ExpressionExtractor expressionExtractor          = new ExpressionExtractor();
		List<SingleVariableDeclaration> methodParameters = method.parameters();
		Block methodBody                                 = method.getBody();
		List<Statement> variableDeclarationStatements    = statementExtractor.getVariableDeclarationStatements(methodBody);
		List<Expression> variableDeclarationExpressions  = expressionExtractor.getVariableDeclarationExpressions(methodBody);
		List<Statement> enhancedForStatements            = statementExtractor.getEnhancedForStatements(methodBody);
		List<VariableDeclaration> variableDeclarations   = new ArrayList<VariableDeclaration>(methodParameters);
		
		for (Statement currentStatement : variableDeclarationStatements)
		{
			if (currentStatement instanceof VariableDeclarationStatement)
			{
				VariableDeclarationStatement currentVariableDeclarationStatement = (VariableDeclarationStatement) currentStatement;
				variableDeclarations.addAll(currentVariableDeclarationStatement.fragments());
			}
		}
		for (Expression currentExpression : variableDeclarationExpressions)
		{
			if (currentExpression instanceof VariableDeclarationExpression)
			{
				VariableDeclarationExpression currentVariableDeclarationExpression = (VariableDeclarationExpression) currentExpression;
				variableDeclarations.addAll(currentVariableDeclarationExpression.fragments());
			}
		}
		for (Statement currentStatement : enhancedForStatements)
		{
			if (currentStatement instanceof EnhancedForStatement)
			{
				EnhancedForStatement currentEnhancedForStatement = (EnhancedForStatement) currentStatement;
				variableDeclarations.add(currentEnhancedForStatement.getParameter());
			}
		}
		
		return variableDeclarations;
	}

	public static MethodDeclaration findParentMethodDeclaration(ASTNode node)
	{
		MethodDeclaration parentMethodDeclaration = null;
		ASTNode parent = node.getParent();

		while (parent != null)
		{
			if (parent instanceof MethodDeclaration)
			{
				parentMethodDeclaration = (MethodDeclaration) parent;
				break;
			}
			parent = parent.getParent();
		}
		return parentMethodDeclaration;
	}
	
	// returns null if the main variable (LHS in an assignment, expression in postfix, prefix, or methodInvocation) is not being UPDATED
	public static Integer getUpdateValue(Expression updater)
	{
		if (updater instanceof PrefixExpression || updater instanceof PostfixExpression)
		{
			return ConditionalLoopUtilities.getIncrementValue(updater);
		}
		else if (updater instanceof Assignment)
		{
			Assignment assignment = (Assignment) updater;
			return assignmentUpdateValue(assignment);
		}
		else if (updater instanceof MethodInvocation)
		{
			MethodInvocation methodInvocation = (MethodInvocation) updater;
			ConditionalLoopBindingInformation bindingInformation = ConditionalLoopBindingInformation.getInstance();
			return bindingInformation.getupdateMethodValue(methodInvocation.resolveMethodBinding().getMethodDeclaration().getKey());
		}
		return null;
	}

	// returns null if specified expression is not a Prefix or PostfixExpression
	private static Integer getIncrementValue(Expression expression)
	{
		Integer incrementValue = null;
		String operator = null;
		if (expression instanceof PrefixExpression)
		{
			PrefixExpression prefixExpression  = (PrefixExpression) expression;
			operator = prefixExpression.getOperator().toString();
		}
		else if (expression instanceof PostfixExpression)
		{
			PostfixExpression postfixExpression = (PostfixExpression) expression;
			operator = postfixExpression.getOperator().toString();
		}
		if (operator != null && operator.equals("++"))
		{
			incrementValue = 1;
		}
		else if (operator != null && operator.equals("--"))
		{
			incrementValue = (-1);
		}
		return incrementValue;
	}

	// returns null if the assignment is not UPDATING the variable on the left hand side or if the updateValue cannot be evaluated to an integer
	private static Integer assignmentUpdateValue(Assignment assignment)
	{
		Integer updateValue          = null;
		Expression leftHandSide      = assignment.getLeftHandSide();
		Assignment.Operator operator = assignment.getOperator();
		Expression rightHandSide     = assignment.getRightHandSide();
		
		if (operator == Assignment.Operator.PLUS_ASSIGN)
		{			
			updateValue = ConditionalLoopUtilities.getIntegerValue(rightHandSide);
		}
		else if (operator == Assignment.Operator.MINUS_ASSIGN)
		{
			Integer rightHandSideIntegerValue = ConditionalLoopUtilities.getIntegerValue(rightHandSide);
			if (rightHandSideIntegerValue != null)
			{
				updateValue = (-1) * rightHandSideIntegerValue;
			}
		}
		else if (leftHandSide instanceof SimpleName && operator == Assignment.Operator.ASSIGN && rightHandSide instanceof InfixExpression)
		{
			SimpleName leftHandSideSimpleName      = (SimpleName) leftHandSide;
			IBinding leftHandSideBinding           = leftHandSideSimpleName.resolveBinding();
			InfixExpression infixExpression        = (InfixExpression) rightHandSide;
			InfixExpression.Operator infixOperator = infixExpression.getOperator();
			Expression rightOperand                = infixExpression.getRightOperand();
			Expression leftOperand                 = infixExpression.getLeftOperand();
			
			if (infixOperator.toString().equals("+") || infixOperator.toString().equals("-"))
			{
				if (leftOperand instanceof SimpleName)
				{
					SimpleName leftOperandSimpleName = (SimpleName) leftOperand;
					IBinding leftOperandBinding      = leftOperandSimpleName.resolveBinding();
					if (leftOperandBinding.isEqualTo(leftHandSideBinding))
					{
						Integer rightOperandIntegerValue = ConditionalLoopUtilities.getIntegerValue(rightOperand);
						if (infixOperator.toString().equals("+") && rightOperandIntegerValue != null)
						{
							updateValue = rightOperandIntegerValue;
						}
						else if (infixOperator.toString().equals("-") && rightOperandIntegerValue != null)
						{
							updateValue = (-1) * rightOperandIntegerValue;
						}
					}
				}
				else if (rightOperand instanceof SimpleName)
				{
					SimpleName rightOperandSimpleName = (SimpleName) rightOperand;
					IBinding rightOperandBinding      = rightOperandSimpleName.resolveBinding();
					if (rightOperandBinding.isEqualTo(leftHandSideBinding))
					{
						Integer leftOperandIntegerValue = ConditionalLoopUtilities.getIntegerValue(leftOperand);
						if (infixOperator.toString().equals("+") && leftOperandIntegerValue != null)
						{
							updateValue = leftOperandIntegerValue;
						}
					}
				}
			}
		}
		return updateValue;
	}
	
	// returns null if specified expression cannot be evaluated to an Integer
	public static Integer getIntegerValue(Expression expression)
	{
		Integer numberLiteralValue = null;
		if (expression instanceof NumberLiteral)
		{
			NumberLiteral numberLiteral = (NumberLiteral) expression;
			try
			{
				numberLiteralValue = Integer.parseInt(numberLiteral.getToken());
			}
			catch (NumberFormatException e) {}
		}
		else if (expression instanceof PrefixExpression)
		{
			PrefixExpression prefixExpression        = (PrefixExpression) expression;
			PrefixExpression.Operator prefixOperator = prefixExpression.getOperator();
			Expression operand                       = prefixExpression.getOperand();
			if (operand instanceof NumberLiteral && (prefixOperator.toString().equals("+") || prefixOperator.toString().equals("-")))
			{
				NumberLiteral numberLiteral = (NumberLiteral) operand;
				try
				{
					numberLiteralValue = Integer.parseInt(prefixOperator.toString() + numberLiteral.getToken());
				}
				catch (NumberFormatException e) {}
			}
		}
		return numberLiteralValue;
	}
	
	// returns a boolean indicating which side of the specified infixExpression the specified variable is on
	// returns null if it is on neither side
	public static Boolean isVariableLeftOperand(SimpleName variable, InfixExpression infixExpression)
	{
		Boolean leftOperandIsVariable     = null;
		Expression leftOperand            = infixExpression.getLeftOperand();
		Expression rightOperand           = infixExpression.getRightOperand();
		IBinding infixVariableBinding     = variable.resolveBinding();
		if (leftOperand instanceof SimpleName)
		{
			SimpleName leftOperandSimpleName = (SimpleName) leftOperand;
			IBinding leftOperandBinding      = leftOperandSimpleName.resolveBinding();
			if (leftOperandBinding.isEqualTo(infixVariableBinding))
			{
				leftOperandIsVariable = true;
			}
		}
		if (rightOperand instanceof SimpleName)
		{
			SimpleName rightOperandSimpleName = (SimpleName) rightOperand;
			IBinding rightOperandBinding      = rightOperandSimpleName.resolveBinding();
			if (rightOperandBinding.isEqualTo(infixVariableBinding))
			{
				leftOperandIsVariable = false;
			}
		}
		return leftOperandIsVariable;
	}
}
