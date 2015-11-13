package gr.uom.java.ast.decomposition.matching.loop;

import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.util.ExpressionExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;

@SuppressWarnings("unchecked")
public class ConditionalLoop extends AbstractLoop
{
	private HashMap<SimpleName, AbstractControlVariable> conditionControlVariables;
	
	public ConditionalLoop(ForStatement forStatement) {
		super(forStatement);
		this.condition                 = forStatement.getExpression();
		Statement loopBody             = forStatement.getBody();
		List<Expression> forUpdaters   = forStatement.updaters();
		this.conditionControlVariables = generateConditionControlVariables(this.condition, loopBody, forUpdaters);
	}
	
	public ConditionalLoop(WhileStatement whileStatement) {
		super(whileStatement);
		this.condition                 = whileStatement.getExpression();
		Statement loopBody             = whileStatement.getBody();
		List<Expression> forUpdaters   = new ArrayList<Expression>();
		this.conditionControlVariables = generateConditionControlVariables(this.condition, loopBody, forUpdaters);
	}
	
	public ConditionalLoop(DoStatement doStatement) {
		super(doStatement);
		this.condition                 = doStatement.getExpression();
		Statement loopBody             = doStatement.getBody();
		List<Expression> forUpdaters   = new ArrayList<Expression>();
		this.conditionControlVariables = generateConditionControlVariables(this.condition, loopBody, forUpdaters);
	}
	
	public HashMap<SimpleName, AbstractControlVariable> getConditionControlVariables()
	{
		return this.conditionControlVariables;
	}
	
	private HashMap<SimpleName, AbstractControlVariable> generateConditionControlVariables(Expression condition, Statement loopBody, List<Expression> forUpdaters)
	{
		HashMap<SimpleName, AbstractControlVariable> conditionControlVariables = new HashMap<SimpleName, AbstractControlVariable>();
		// get all simpleNames in the condition
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> simpleNamesInCondition = expressionExtractor.getVariableInstructions(condition);
		// for each simpleName, create a ControlVariable object and add it to the map if it is indeed a ControlVariable (i.e. it has variable updaters)
		for (Expression currentExpression : simpleNamesInCondition)
		{
			if (currentExpression instanceof SimpleName)
			{
				SimpleName currentSimpleName           = (SimpleName) currentExpression;
				ControlVariable currentControlVariable = new ControlVariable(currentSimpleName, loopBody, forUpdaters);
				if (currentControlVariable.getVariableUpdaters().size() > 0)
				{
					conditionControlVariables.put(currentSimpleName, currentControlVariable);
				}
			}
		}
		return conditionControlVariables;
	}
	
	// returns all modifiers of the specified variable occurring before it in its containing method
	private static List<ASTNode> getAllVariableModifiersInParentBlock(SimpleName variable, Block block)
	{
		List<ASTNode> bodyVariableModifiers = new ArrayList<ASTNode>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		bodyVariableModifiers.addAll(expressionExtractor.getAssignments(block));
		// remove all variable updaters that are not modifying the specified variable or are after the position of the variable in use
		Iterator<ASTNode> it = bodyVariableModifiers.iterator();
		while (it.hasNext())
		{
			ASTNode currentNode = it.next();
			if (currentNode instanceof Expression)
			{
				Expression currentExpression = (Expression) currentNode;
				if (!AbstractLoopUtilities.isUpdatingVariable(currentExpression, variable) || currentExpression.getStartPosition() >= variable.getStartPosition())
				{
					it.remove();
				}
			}
		}
		return bodyVariableModifiers;
	}

	// TODO add initializer fragments when VariableDeclarations are supported
	public List<ASTNode> getAdditionalFragments()
	{
		List<ASTNode> additionalFragments = new ArrayList<ASTNode>();
		for(AbstractControlVariable currentControlVariable : conditionControlVariables.values())
		{
			List<VariableUpdater> updaters = currentControlVariable.getVariableUpdaters();
			for (VariableUpdater currentVariableUpdater : updaters)
			{
				additionalFragments.add(currentVariableUpdater.getUpdater());
			}
			if (currentControlVariable instanceof ControlVariable)
			{
				ControlVariable controlVariable = (ControlVariable) currentControlVariable;
				VariableDeclaration variableDeclaration = AbstractLoopUtilities.getVariableDeclaration(controlVariable.getVariable());
				if(variableDeclaration != null)
				{
					additionalFragments.add(variableDeclaration);
					if (variableDeclaration.getParent() instanceof FieldDeclaration && getLoopStatement().getParent() instanceof Block)
					{
						List<ASTNode> nodes = getAllVariableModifiersInParentBlock(controlVariable.getVariable(), (Block) getLoopStatement().getParent());
						if(!nodes.isEmpty())
						{
							additionalFragments.add(nodes.get(nodes.size() - 1));
						}
					}
				}
				ASTNode dataStructureAccessExpression = controlVariable.getDataStructureAccessExpression();
				if(dataStructureAccessExpression != null)
				{
					additionalFragments.add(dataStructureAccessExpression);
				}
			}
		}
		/*// if the current ConditionalLoop is a for loop, remove all for the updaters found in the for declaration
		if (this.getLoopStatement() instanceof ForStatement)
		{
			ForStatement forStatement = (ForStatement) this.getLoopStatement();
			List<Expression> updaters = forStatement.updaters();
			for (Expression currentUpdater : updaters)
			{
				if (additionalFragments.contains(currentUpdater))
				{
					additionalFragments.remove(currentUpdater);
				}
			}
		}*/
		return additionalFragments;
	}

	// ********************************************************************************************************************************************************************************
	// matching methods
	// ********************************************************************************************************************************************************************************

	public boolean match(EnhancedForLoop otherLoop, ConditionalLoopASTNodeMatcher matcher)
	{
		if (this.conditionControlVariables.size() == 1)
		{
			for (AbstractControlVariable currentControlVariable : this.conditionControlVariables.values())
			{
				return currentControlVariable.match(otherLoop.getControlVariable());
			}
		}
		return false;
	}
	
	// takes a matcher so that all differences in that matcher are accessible once the method terminates
	public boolean match(ConditionalLoop otherLoop, ConditionalLoopASTNodeMatcher matcher)
	{
		// match the conditions
		boolean isConditionMatch      = matcher.safeSubtreeMatch(this.condition, otherLoop.condition);
		boolean isParameterizable     = matcher.isParameterizable();
		boolean controlVariablesMatch = false;
		if (isConditionMatch && isParameterizable)
		{
			// check if each pair of corresponding control variables match
			if (this.conditionControlVariables.size() == otherLoop.conditionControlVariables.size())
			{
				controlVariablesMatch = true;
				for(Entry<SimpleName, AbstractControlVariable> currentControlVariableEntry : this.conditionControlVariables.entrySet())
				{
					SimpleName equivalentSimpleName = findEquivalentExpression(currentControlVariableEntry.getKey(), otherLoop.conditionControlVariables.keySet(),
																				matcher.getDifferences());	// conditions match, so we may have recorded differences
					AbstractControlVariable currentControlVariableEquivalent = otherLoop.conditionControlVariables.get(equivalentSimpleName);
					// currentControlVariableEquivalent is the control variable in the other loop that was matched with the control variable in this loop
					if (currentControlVariableEquivalent != null)
					{
						if (!currentControlVariableEntry.getValue().match(currentControlVariableEquivalent))
						{
							controlVariablesMatch = false;
							break;
						}
					}
					else
					{
						controlVariablesMatch = false;
						break;
					}
				}
			}
		}
		return controlVariablesMatch;
	}

	private static SimpleName findEquivalentExpression(SimpleName simpleName, Set<SimpleName> simpleNameSet, List<ASTNodeDifference> differences)
	{
		SimpleName equivalentExpression = null;
		Expression difference = getDifference(simpleName, differences);
		if (difference != null)
		{
			if (difference instanceof SimpleName)
			{
				if (simpleNameSet.contains((SimpleName)difference))
				{
					equivalentExpression = (SimpleName) difference;
				}
			}
			else if (difference instanceof InfixExpression)
			{
				InfixExpression infixExpression = (InfixExpression) difference;
				Expression leftOperand = infixExpression.getLeftOperand();
				Expression rightOperand = infixExpression.getRightOperand();
				if (leftOperand instanceof SimpleName && simpleNameSet.contains((SimpleName)leftOperand))
				{
					equivalentExpression = (SimpleName) leftOperand;
				}
				else if (rightOperand instanceof SimpleName && simpleNameSet.contains((SimpleName)rightOperand))
				{
					equivalentExpression = (SimpleName) rightOperand;
				}
			}
			else if (difference instanceof MethodInvocation)
			{
				MethodInvocation methodInvocation = (MethodInvocation) difference;
				Expression callingExpression = methodInvocation.getExpression();
				if (callingExpression instanceof SimpleName && simpleNameSet.contains((SimpleName)callingExpression))
				{
					equivalentExpression = (SimpleName) callingExpression;
				}
			}
		}
		else
		{
			for (SimpleName currentEquivalentExpression : simpleNameSet)
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
	
	// searches the differences for the equivalent of the specified expression or that of its parent (if the parent is an expression)
	private static Expression getDifference(Expression expression, List<ASTNodeDifference> differences)
	{
		Expression difference = null;
		ASTNode parent = expression.getParent();
		for (ASTNodeDifference currentDifference : differences)
		{
			if (currentDifference.getExpression1().getExpression() == expression || (parent instanceof Expression && currentDifference.getExpression1().getExpression() == (Expression)parent))
			{
				difference = currentDifference.getExpression2().getExpression();
				break;
			}
		}
		return difference;
	}
}
