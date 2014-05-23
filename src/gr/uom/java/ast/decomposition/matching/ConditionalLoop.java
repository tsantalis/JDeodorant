package gr.uom.java.ast.decomposition.matching;

import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.StatementExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class ConditionalLoop extends AbstractLoop {
	private Expression condition;
	private Map<SimpleName,List<Expression>> conditionVariableUpdatersMap;
	private Map<SimpleName,VariableDeclaration> conditionVariableInitializersMap;
	
	public ConditionalLoop(ForStatement forStatement) {
		super(forStatement);
		Expression forCondition          = forStatement.getExpression();
		Statement forBody                = forStatement.getBody();
		List<Expression> forUpdaters     = forStatement.updaters();
		condition                        = forCondition;
		conditionVariableUpdatersMap     = getConditionVariableUpdaters(forCondition, forBody, forUpdaters);
		conditionVariableInitializersMap = getConditionVariableDeclarations(forCondition);
	}
	
	public ConditionalLoop(WhileStatement whileStatement) {
		super(whileStatement);
		Expression whileCondition        = whileStatement.getExpression();
		Statement whileBody              = whileStatement.getBody();
		condition                        = whileCondition;
		conditionVariableUpdatersMap     = getConditionVariableUpdaters(whileCondition, whileBody, new ArrayList<Expression>());
		conditionVariableInitializersMap = getConditionVariableDeclarations(whileCondition);
	}
	
	public ConditionalLoop(DoStatement doStatement) {
		super(doStatement);
		Expression doCondition           = doStatement.getExpression();
		Statement doBody                 = doStatement.getBody();
		condition                        = doCondition;
		conditionVariableUpdatersMap     = getConditionVariableUpdaters(doCondition, doBody, new ArrayList<Expression>());
		conditionVariableInitializersMap = getConditionVariableDeclarations(doCondition);
	}
	
	public Expression getCondition()
	{
		return condition;
	}
	
	public Map<SimpleName,List<Expression>> getConditionVariableUpdatersMap()
	{
		return conditionVariableUpdatersMap;
	}

	public Map<SimpleName,VariableDeclaration> conditionVariableInitializersMap()
	{
		return conditionVariableInitializersMap;
	}
	
	private static Map<SimpleName,List<Expression>> getConditionVariableUpdaters(Expression condition, Statement body, List<Expression> forUpdaters)
	{
		HashMap<SimpleName,List<Expression>> variableUpdaters = new HashMap<SimpleName,List<Expression>>();
		ExpressionExtractor expressionExtractor               = new ExpressionExtractor();
		List<Expression> simpleNamesInCondition               = expressionExtractor.getVariableInstructions(condition);
		List<Expression> possibleUpdatersInBody               = getAllFirstLevelUpdaters(body);
		
		// find all updaters for each simple name 
		for (Expression currentExpression : simpleNamesInCondition)
		{
			SimpleName currentSimpleName = (SimpleName) currentExpression;
			IBinding currentSimpleNameBinding = currentSimpleName.resolveBinding();
			
			if (currentSimpleNameBinding != null && currentSimpleNameBinding.getKind() == IBinding.VARIABLE)
			{
				List<Expression> currentVariableUpdaters = new ArrayList<Expression>();
				for (Expression currentUpdater : possibleUpdatersInBody)
				{
					if (isUpdatingVariable(currentUpdater, currentSimpleName))
					{
						currentVariableUpdaters.add(currentUpdater);
					}
				}
				for (Expression currentUpdater : forUpdaters)
				{
					if (isUpdatingVariable(currentUpdater, currentSimpleName))
					{
						currentVariableUpdaters.add(currentUpdater);
					}
				}
				if (currentVariableUpdaters.size() > 0)
				{
					variableUpdaters.put(currentSimpleName, currentVariableUpdaters);
				}
			}
		}
		return variableUpdaters;
	}
	
	public static boolean isSubclassOf(ITypeBinding typeBinding, String qualifiedName)
	{
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
	
	private static List<Expression> getAllFirstLevelUpdaters(Statement statement)
	{
		List<Expression> updaters               = new ArrayList<Expression>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();

		List<Statement> innerStatements = new ArrayList<Statement>();
		if (statement instanceof Block)
		{
			Block statementBlock = (Block) statement;
			innerStatements.addAll(statementBlock.statements());
		}
		else
		{
			innerStatements.add(statement);
		}
		// get all PrefixExpressions, PostfixExpressions, Assignments, and next() MethodInvocations from each inner statement
		for (Statement currentStatement : innerStatements)
		{
			if (currentStatement instanceof ExpressionStatement || currentStatement instanceof VariableDeclarationStatement)
			{
				updaters.addAll(expressionExtractor.getPrefixExpressions(currentStatement));
				updaters.addAll(expressionExtractor.getPostfixExpressions(currentStatement));
				updaters.addAll(expressionExtractor.getAssignments(currentStatement));
				
				List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(currentStatement);
				for (Expression currentExpression : methodInvocations)
				{
					if (currentExpression instanceof MethodInvocation)
					{
						MethodInvocation currentMethodInvocation      = (MethodInvocation) currentExpression;
						IMethodBinding currentMethodInvocationBinding = currentMethodInvocation.resolveMethodBinding();
						String currentMethodName                      = currentMethodInvocationBinding.getName();
						Expression callingExpression                  = currentMethodInvocation.getExpression();
						ITypeBinding callingExpressionTypeBinding     = callingExpression.resolveTypeBinding();
						if (((currentMethodName.equals("next") || currentMethodName.equals("previous")) && isSubclassOf(callingExpressionTypeBinding,"java.util.ListIterator")) ||
							(currentMethodName.equals("next") && isSubclassOf(callingExpressionTypeBinding,"java.util.Iterator")) ||
							(currentMethodName.equals("nextElement") && isSubclassOf(callingExpressionTypeBinding,"java.util.Enumeration")))
						{
							updaters.add(currentMethodInvocation);
						}
					}
				}
			}
		}
		// remove any expressions in a ConditionalExpression
		ListIterator<Expression> it = updaters.listIterator();
		while (it.hasNext())
		{
			Expression currentUpdater = it.next();
			ASTNode parent = currentUpdater.getParent();
			while (parent != null && !parent.equals(statement))
			{
				if (parent instanceof ConditionalExpression)
				{
					it.remove();
					break;
				}
				parent = parent.getParent();
			}
		}
		return updaters;
	}
	
	private static boolean isUpdatingVariable(Expression updater, SimpleName variable)
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

	private static Map<SimpleName,VariableDeclaration> getConditionVariableDeclarations(Expression condition)
	{
		MethodDeclaration method                                     = findParentMethodDeclaration(condition);
		ExpressionExtractor expressionExtractor                      = new ExpressionExtractor();
		HashMap<SimpleName,VariableDeclaration> variableInitializers = new HashMap<SimpleName,VariableDeclaration>();
		List<Expression> simpleNamesInCondition                      = expressionExtractor.getVariableInstructions(condition);
		List<VariableDeclaration> variableDeclarations               = getAllVariableDeclarations(method);
		
		// for each variable in the condition, find the initializer
		for (Expression currentExpression : simpleNamesInCondition)
		{
			SimpleName currentSimpleName      = (SimpleName) currentExpression;
			IBinding currentSimpleNameBinding = currentSimpleName.resolveBinding();
			if (currentSimpleNameBinding.getKind() == IBinding.VARIABLE)
			{
				IVariableBinding currentSimpleNameVariableBinding = (IVariableBinding) currentSimpleNameBinding;
				for (VariableDeclaration currentVariableDeclaration : variableDeclarations)
				{
					IVariableBinding currentVariableDeclarationVariableBinding = currentVariableDeclaration.resolveBinding();
					if (currentVariableDeclarationVariableBinding.isEqualTo(currentSimpleNameVariableBinding))
					{
						variableInitializers.put(currentSimpleName, currentVariableDeclaration);
						break;
					}
				}
			}
		}
		return variableInitializers;
	}

	private static MethodDeclaration findParentMethodDeclaration(ASTNode node)
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
	
	public boolean match(AbstractLoop otherLoop, List<ASTNodeDifference> differences)
	{
		if (otherLoop instanceof ConditionalLoop)
		{
			ConditionalLoop otherConditionalLoop = (ConditionalLoop) otherLoop;
			return conditionalForLoopMatch(otherConditionalLoop, differences);
		}
		return false;
	}
	
	private boolean conditionalForLoopMatch(ConditionalLoop otherConditionalLoop, List<ASTNodeDifference> differences)
	{
		boolean updaterMatch = false;
		if (this.conditionVariableUpdatersMap.size() == otherConditionalLoop.conditionVariableUpdatersMap.size())
		{
			updaterMatch = true;
			for(Entry<SimpleName,List<Expression>> currentEntry : this.conditionVariableUpdatersMap.entrySet())
			{
				SimpleName currentKey                     = currentEntry.getKey();
				Expression currentKeyEquivalentExpression = findEquivalentExpression(currentKey, otherConditionalLoop.conditionVariableUpdatersMap.keySet(), differences);
				
				if (currentKeyEquivalentExpression instanceof SimpleName)
				{
					SimpleName currentKeyEquivalentSimpleName = (SimpleName) currentKeyEquivalentExpression;
					if (!equalUpdaterLists(this.conditionVariableUpdatersMap.get(currentKey), otherConditionalLoop.conditionVariableUpdatersMap.get(currentKeyEquivalentSimpleName)))
					{
						updaterMatch = false;
					}
				}
				else
				{
					updaterMatch = false;
				}
			}
		}
		return updaterMatch;
	}
	
	private static Expression findEquivalentExpression(SimpleName simpleName, Set<SimpleName> equivalentExpressions, List<ASTNodeDifference> differences)
	{
		Expression equivalentExpression = null;
		for (ASTNodeDifference currentDifference : differences)
		{
			if (currentDifference.getExpression1().getExpression() instanceof SimpleName)
			{
				IBinding simpleNameBinding      = simpleName.resolveBinding();
				SimpleName differenceExpression1      = (SimpleName) currentDifference.getExpression1().getExpression();
				IBinding differenceExpression1Binding = differenceExpression1.resolveBinding();
				
				if(differenceExpression1Binding.getKind() == IBinding.VARIABLE && simpleNameBinding.getKind() == IBinding.VARIABLE)
				{
					IVariableBinding differenceExpression1VariableBinding = (IVariableBinding)differenceExpression1Binding;
					IVariableBinding simpleNameVariableBinding            = (IVariableBinding)simpleNameBinding;
					
					if (differenceExpression1VariableBinding.isEqualTo(simpleNameVariableBinding))
					{
						equivalentExpression = currentDifference.getExpression2().getExpression();
						break;
					}
				}
			}
		}
		if (equivalentExpression == null)
		{
			for (SimpleName currentEquivalentExpression : equivalentExpressions)
			{					
				if (currentEquivalentExpression.getIdentifier().equals(simpleName.getIdentifier()))
				{
					equivalentExpression = currentEquivalentExpression;
					break;
				}
			}
		}
		return equivalentExpression;
	}
	
	private static boolean equalUpdaterLists(List<Expression> updaters1, List<Expression> updaters2)
	{
		if (updaters1.size() != updaters2.size())
		{
			return false;
		}
		
		for (int i = 0; i < updaters1.size(); i++)
		{
			Expression currentUpdater1 = updaters1.get(i);
			Expression currentUpdater2 = updaters2.get(i);
			
			if (!equalUpdaters(currentUpdater1, currentUpdater2))
			{
				return false;
			}
		}
		return true;
	}
	
	private static boolean equalUpdaters(Expression updater1, Expression updater2)
	{
		if (updater1 instanceof PrefixExpression && updater2 instanceof PrefixExpression)
		{
			PrefixExpression currentPrefixExpression1 = (PrefixExpression) updater1;
			PrefixExpression currentPrefixExpression2 = (PrefixExpression) updater2;
			PrefixExpression.Operator operator1       = currentPrefixExpression1.getOperator();
			PrefixExpression.Operator operator2       = currentPrefixExpression2.getOperator();
			
			if (operator1.toString().equals(operator2.toString()))
			{
				return true;
			}
		}
		if (updater1 instanceof PostfixExpression && updater2 instanceof PostfixExpression)
		{
			PostfixExpression currentPostfixExpression1 = (PostfixExpression) updater1;
			PostfixExpression currentPostfixExpression2 = (PostfixExpression) updater2;
			PostfixExpression.Operator operator1        = currentPostfixExpression1.getOperator();
			PostfixExpression.Operator operator2        = currentPostfixExpression2.getOperator();
			
			if (operator1.toString().equals(operator2.toString()))
			{
				return true;
			}
		}
		if (updater1 instanceof Assignment && updater2 instanceof Assignment)
		{
			Assignment currentAssignment1 = (Assignment) updater1;
			Assignment currentAssignment2 = (Assignment) updater2;
			
			if (equalAssignmentOperatorsAndLHS(currentAssignment1, currentAssignment2))
			{
				return true;
			}
		}
		return false;
	}
	
	private static boolean equalAssignmentOperatorsAndLHS(Assignment assignment1, Assignment assignment2)
	{
		Assignment.Operator operator1 = assignment1.getOperator();
		Assignment.Operator operator2 = assignment2.getOperator();
		Expression leftHandSide1      = assignment1.getLeftHandSide();
		Expression leftHandSide2      = assignment2.getLeftHandSide();
		NumberLiteral numberLiteral1  = null;
		NumberLiteral numberLiteral2  = null;
		
		if (leftHandSide1 instanceof NumberLiteral && leftHandSide2 instanceof NumberLiteral)
		{
			numberLiteral1 = (NumberLiteral) leftHandSide1;
			numberLiteral2 = (NumberLiteral) leftHandSide2;
		}
		
		if (numberLiteral1 != null &&
				numberLiteral2 != null &&
				numberLiteral1.getToken().equals(numberLiteral2.getToken()) &&
				operator1.toString().equals(operator2.toString()))
		{
			return true;
		}
		return false;
	}
}
