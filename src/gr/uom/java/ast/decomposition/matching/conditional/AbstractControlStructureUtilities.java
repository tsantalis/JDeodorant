package gr.uom.java.ast.decomposition.matching.conditional;

import gr.uom.java.ast.util.ExpressionExtractor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;

@SuppressWarnings("unchecked")
public class AbstractControlStructureUtilities
{
	public static List<Pair<Expression>> getIfAndTernaryStructureMatchList(IfControlStructure ifStructure, TernaryControlStructure ternaryStructure)
	{
		List<Pair<Expression>> matchList  = new ArrayList<Pair<Expression>>();
		List<AbstractControlCase> ifCases = ifStructure.controlCases;
		// if the ifControlStructure is a basic if-else
		if (ifCases.size() == 2 && ifCases.get(1).caseCondition == null)
		{
			List<Statement> thenBody = ifCases.get(0).body;
			List<Statement> elseBody = ifCases.get(1).body;
			// if both then and else bodies have one statement
			if (thenBody.size() == 1 && elseBody.size() == 1)
			{
				Statement ifThenStatement  = thenBody.get(0);
				Statement ifElseStatement  = elseBody.get(0);
				Statement ternaryStatement = ternaryStructure.getNode();
				// if the then statement, the else statement and the statement containing the ConditionalExpression are ExpressionStatements
				if (ifThenStatement instanceof ExpressionStatement && ifElseStatement instanceof ExpressionStatement && ternaryStatement instanceof ExpressionStatement)
				{
					Expression thenExpression    = ((ExpressionStatement)ifThenStatement).getExpression();
					Expression elseExpression    = ((ExpressionStatement)ifElseStatement).getExpression();
					Expression ternaryExpression = ((ExpressionStatement)ternaryStatement).getExpression();
					matchExpressionStatementExpressions(thenExpression, elseExpression, ternaryExpression, ternaryStructure, matchList);
				}
				// if the then statement, the else statement and the statement containing the ConditionalExpression are ReturnStatements
				// AND the expression of the ternary ReturnStatement is the the ConditionalExpression we are comparing
				else if (ifThenStatement instanceof ReturnStatement && ifElseStatement instanceof ReturnStatement &&
						ternaryStatement instanceof ReturnStatement && ((ReturnStatement)ternaryStatement).getExpression().equals(ternaryStructure.getConditionalExpression()))
				{
					Expression ifThenExpression      = ((ReturnStatement)ifThenStatement).getExpression();
					Expression ifElseExpression      = ((ReturnStatement)ifElseStatement).getExpression();
					Expression ternaryThenExpression = ternaryStructure.getThenExpression();
					Expression ternaryElseExpression = ternaryStructure.getElseExpression();
					matchList.add(new Pair<Expression>(ifThenExpression, ternaryThenExpression));
					matchList.add(new Pair<Expression>(ifElseExpression, ternaryElseExpression));
				}
			}
		}
		if (!matchList.isEmpty())
		{
			matchList.add(0, new Pair<Expression>(ifCases.get(0).caseCondition, ternaryStructure.getCondition()));
		}
		return matchList;
	}

	private static void matchExpressionStatementExpressions(Expression thenExpression, Expression elseExpression, Expression ternaryExpression,
			TernaryControlStructure ternaryStructure, List<Pair<Expression>> matchList)
	{
		// if all three expressions are Assignments
		if (thenExpression instanceof Assignment && elseExpression instanceof Assignment && ternaryExpression instanceof Assignment)
		{
			Assignment thenAssignment    = (Assignment) thenExpression;
			Assignment elseAssignment    = (Assignment) elseExpression;
			Assignment ternaryAssignment = (Assignment) ternaryExpression;
			if (isSameAssignee(thenAssignment, elseAssignment) && ternaryAssignment.getRightHandSide().equals(ternaryStructure.getConditionalExpression()))
			{
				matchList.add(new Pair<Expression>(thenAssignment.getLeftHandSide(), ternaryAssignment.getLeftHandSide()));
				matchList.add(new Pair<Expression>(elseAssignment.getLeftHandSide(), ternaryAssignment.getLeftHandSide()));
				matchList.add(new Pair<Expression>(thenAssignment.getRightHandSide(), ternaryStructure.getThenExpression()));
				matchList.add(new Pair<Expression>(elseAssignment.getRightHandSide(), ternaryStructure.getElseExpression()));
			}
		}
		// if all three expressions are MethodInvocations
		else if (thenExpression instanceof MethodInvocation && elseExpression instanceof MethodInvocation && ternaryExpression instanceof MethodInvocation)
		{
			MethodInvocation thenMethodInvocation    = (MethodInvocation) thenExpression;
			MethodInvocation elseMethodInvocation    = (MethodInvocation) elseExpression;
			MethodInvocation ternaryMethodInvocation = (MethodInvocation) ternaryExpression;
			Integer ternaryArgumentIndex             = getTernaryArgumentIndex(ternaryMethodInvocation, ternaryStructure.getConditionalExpression());
			List<Expression> thenArguments           = thenMethodInvocation.arguments();
			List<Expression> elseArguments           = elseMethodInvocation.arguments();
			List<Expression> ternaryArguments        = ternaryMethodInvocation.arguments();
			// if all three methods have the same method binding
			if (thenMethodInvocation.resolveMethodBinding().isEqualTo(elseMethodInvocation.resolveMethodBinding()) &&
				thenMethodInvocation.resolveMethodBinding().isEqualTo(ternaryMethodInvocation.resolveMethodBinding()))
			{
				// if the ConditionalExpression is in the arguments
				if (ternaryArgumentIndex != null)
				{
					// match the expressions
					matchList.add(new Pair<Expression>(thenMethodInvocation.getExpression(), ternaryMethodInvocation.getExpression()));
					matchList.add(new Pair<Expression>(elseMethodInvocation.getExpression(), ternaryMethodInvocation.getExpression()));
					for (int i = 0; i < ternaryArguments.size(); i++)
					{
						// match the arguments
						if (i == ternaryArgumentIndex)
						{
							matchList.add(new Pair<Expression>(thenArguments.get(i), ternaryStructure.getThenExpression()));
							matchList.add(new Pair<Expression>(elseArguments.get(i), ternaryStructure.getElseExpression()));
						}
						else
						{
							matchList.add(new Pair<Expression>(thenArguments.get(i), ternaryArguments.get(i)));
							matchList.add(new Pair<Expression>(elseArguments.get(i), ternaryArguments.get(i)));
						}
					}
				}
				// if the ConditionalExpression is the method's expression
				else if (ternaryMethodInvocation.getExpression() != null &&
						(unparenthesize(ternaryMethodInvocation.getExpression()).equals(ternaryStructure.getConditionalExpression())))
				{
					// match the expressions
					Expression thenMethodExpression = thenMethodInvocation.getExpression();
					Expression elseMethodExpression = elseMethodInvocation.getExpression();
					matchList.add(new Pair<Expression>(thenMethodExpression, ternaryStructure.getThenExpression()));
					matchList.add(new Pair<Expression>(elseMethodExpression, ternaryStructure.getElseExpression()));
					// match the arguments
					for (int i = 0; i < ternaryArguments.size(); i++)
					{
						matchList.add(new Pair<Expression>(thenArguments.get(i), ternaryArguments.get(i)));
						matchList.add(new Pair<Expression>(elseArguments.get(i), ternaryArguments.get(i)));
					}
				}
			}
		}
	}

	private static Integer getTernaryArgumentIndex(MethodInvocation ternaryMethodInvocation, ConditionalExpression conditionalExpression)
	{
		List<Expression> arguments = ternaryMethodInvocation.arguments();
		for (int i = 0; i < arguments.size(); i++)
		{
			if (arguments.get(i).equals(conditionalExpression))
			{
				return i;
			}
		}
		return null;
	}

	private static boolean isSameAssignee(Assignment assignment1, Assignment assignment2)
	{
		Expression assignee1 = assignment1.getLeftHandSide();
		Expression assignee2 = assignment2.getLeftHandSide();
		IBinding binding1 = null;
		IBinding binding2 = null;
		if (assignee1 instanceof Name && assignee2 instanceof Name)
		{
			binding1 = ((Name)assignee1).resolveBinding();
			binding2 = ((Name)assignee2).resolveBinding();
		}
		else if (assignee1 instanceof FieldAccess && assignee2 instanceof FieldAccess)
		{
			binding1 = ((FieldAccess)assignee1).resolveFieldBinding();
			binding2 = ((FieldAccess)assignee2).resolveFieldBinding();
		}
		return (binding1 != null && binding2 != null &&
				binding1.getKind() == IBinding.VARIABLE && binding2.getKind() == IBinding.VARIABLE &&
				binding1.isEqualTo(binding2));
	}
	
	public static ConditionalExpression hasOneConditionalExpression(Statement statement)
	{
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> conditionalExpressions = expressionExtractor.getConditionalExpressions(statement);
		if (conditionalExpressions.size() == 1)
		{
			return (ConditionalExpression)conditionalExpressions.get(0);
		}
		return null;
	}
	
	public static List<Statement> unBlock(List<Statement> statements)
	{
		List<Statement> returnList = new ArrayList<Statement>();
		for (Statement currentStatement : statements)
		{
			if (currentStatement instanceof Block)
			{
				List<Statement> subList = ((Block)currentStatement).statements();
				returnList.addAll(unBlock(subList));
			}
			else
			{
				returnList.add(currentStatement);
			}
		}
		return returnList;
	}
	
	public static Expression unparenthesize(Expression expression)
	{
		if (expression instanceof ParenthesizedExpression)
		{
			ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression)expression;
			Expression innerExpression = parenthesizedExpression.getExpression();
			return unparenthesize(innerExpression);
		}
		return expression;
	}

}
