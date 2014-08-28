package gr.uom.java.ast.decomposition.matching.conditional;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;

public class IfControlStructure extends AbstractControlStructure
{
	public IfControlStructure(IfStatement node)
	{
		super(node);
		this.controlCases = new ArrayList<AbstractControlCase>();
		initializeFields(node);
	}

	private void initializeFields(IfStatement node)
	{
		Statement elseStatement;
		do
		{
			Expression condition = node.getExpression();
			Statement body       = node.getThenStatement();
			elseStatement        = node.getElseStatement();
			controlCases.add(new IfControlCase(condition, body));
			if (elseStatement instanceof IfStatement)
			{
				node = (IfStatement) elseStatement;
			}
			else if (elseStatement instanceof Block && ((Block)elseStatement).statements().size() == 1 && ((Block)elseStatement).statements().get(0) instanceof IfStatement)
			{
				node = (IfStatement) ((Block)elseStatement).statements().get(0);
			}
			else if (elseStatement != null)
			{
				controlCases.add(new IfControlCase(null, elseStatement));
			}
		} while ((elseStatement instanceof IfStatement) ||
				(elseStatement instanceof Block && ((Block)elseStatement).statements().size() == 1 && ((Block)elseStatement).statements().get(0) instanceof IfStatement));
		
	}

	@Override
	public boolean match(IfControlStructure otherStructure, ASTNodeMatcher matcher)
	{
		if (this.controlCases.size() == otherStructure.controlCases.size())
		{
			boolean caseMatch = true;
			for (int i = 0; i < this.controlCases.size(); i++)
			{
				if (!this.controlCases.get(i).match(otherStructure.controlCases.get(i), matcher))
				{
					caseMatch = false;
				}
			}
			return caseMatch;
		}
		return false;
	}

	@Override
	public boolean match(SwitchControlStructure otherStructure, ASTNodeMatcher matcher)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean match(TernaryControlStructure otherStructure, ASTNodeMatcher matcher)
	{
		if (otherStructure.getConditionalExpression() == null)
		{
			return false;
		}
		List<Pair<Expression>> matchList = AbstractControlStructureUtilities.getIfAndTernaryStructureMatchList(this, otherStructure);
		if (!matchList.isEmpty())
		{
			boolean allPairsMatch = true;
			for (Pair<Expression> currentPair : matchList)
			{
				if (!matcher.safeSubtreeMatch(currentPair.getFirst(), currentPair.getSecond()))		// here the match order is done (first, second) because the this object is the IfControlStructure
				{
					allPairsMatch = false;
				}
			}
			return allPairsMatch;
		}
		return false;
	}

	@Override
	public List<ASTNode> getAdditionalFragments()
	{
		List<ASTNode> additionalFragments = new ArrayList<ASTNode>();
		for (AbstractControlCase currentCase : this.controlCases)
		{
			if (currentCase instanceof IfControlCase)
			{
				IfControlCase currentIfCase = (IfControlCase)currentCase;
				additionalFragments.addAll(currentIfCase.body);
			}
		}
		return additionalFragments;
	}
}
