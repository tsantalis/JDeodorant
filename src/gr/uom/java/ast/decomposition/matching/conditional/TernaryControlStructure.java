package gr.uom.java.ast.decomposition.matching.conditional;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;

public class TernaryControlStructure extends AbstractControlStructure
{
	private ConditionalExpression conditionalExpression;
	private Expression condition;
	private Expression thenExpression;
	private Expression elseExpression;
	
	public TernaryControlStructure(ExpressionStatement node)
	{
		super(node);
		initializeFields(node);
	}
	
	public TernaryControlStructure(ReturnStatement node)
	{
		super(node);
		initializeFields(node);
	}
	
	private void initializeFields(Statement node)
	{
		ConditionalExpression conditionalExpression = AbstractControlStructureUtilities.hasOneConditionalExpression(node);
		if (conditionalExpression != null)
		{
			this.conditionalExpression = conditionalExpression;
			this.condition             = conditionalExpression.getExpression();
			this.thenExpression        = conditionalExpression.getThenExpression();
			this.elseExpression        = conditionalExpression.getElseExpression();
		}
		else
		{
			this.conditionalExpression = null;
			this.condition             = null;
			this.thenExpression        = null;
			this.elseExpression        = null;
		}
	}

	public ConditionalExpression getConditionalExpression()
	{
		return conditionalExpression;
	}

	public Expression getCondition()
	{
		return condition;
	}

	public Expression getThenExpression()
	{
		return thenExpression;
	}
	
	public Expression getElseExpression()
	{
		return elseExpression;
	}

	@Override
	public boolean match(IfControlStructure otherStructure, ASTNodeMatcher matcher)
	{
		if (this.conditionalExpression == null)
		{
			return false;
		}
		List<Pair<Expression>> matchList = AbstractControlStructureUtilities.getIfAndTernaryStructureMatchList(otherStructure, this);
		if (!matchList.isEmpty())
		{
			boolean allPairsMatch = true;
			for (Pair<Expression> currentPair : matchList)
			{
				if (!matcher.safeSubtreeMatch(currentPair.getSecond(), currentPair.getFirst()))		// here the match order is done (second, first) because the this object is the TernaryControlStructure
				{
					allPairsMatch = false;
				}
			}
			return allPairsMatch;
		}
		return false;
	}

	@Override
	public boolean match(SwitchControlStructure otherStructure, ASTNodeMatcher matcher)
	{
		return false;
	}

	@Override
	public boolean match(TernaryControlStructure otherStructure, ASTNodeMatcher matcher)
	{
		return false;
	}

	@Override
	public List<ASTNode> getAdditionalFragments()
	{
		return new ArrayList<ASTNode>();
	}
}
