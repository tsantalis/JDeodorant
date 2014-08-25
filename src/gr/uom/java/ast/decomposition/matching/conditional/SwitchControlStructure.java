package gr.uom.java.ast.decomposition.matching.conditional;

import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SwitchCase;

@SuppressWarnings("unchecked")
public class SwitchControlStructure extends AbstractControlStructure
{
	private Expression variable;
	
	public SwitchControlStructure(SwitchStatement node)
	{
		super(node);
		this.variable      = node.getExpression();
		this.controlCases  = createSwitchCases(node);
	}
	
	// this version of createSwitchCases makes SwitchCases with only the SwitchCase Statements and case ending statement of each case
	private List<AbstractControlCase> createSwitchCases(SwitchStatement switchStatement)
	{
		List<AbstractControlCase> returnList  = new ArrayList<AbstractControlCase>();
		List<AbstractControlCase> tempList    = new ArrayList<AbstractControlCase>();
		List<Statement> switchGroupStatements = switchStatement.statements();
		for (Statement currentStatement : switchGroupStatements)
		{
			if (currentStatement instanceof SwitchCase)
			{
				Expression caseValue = ((SwitchCase)currentStatement).getExpression();
				SwitchControlCase newCase = new SwitchControlCase(this.variable, caseValue, new ArrayList<Statement>());
				tempList.add(newCase);
				addToAll((SwitchCase)currentStatement, tempList);
			}
			else if (currentStatement instanceof BreakStatement || currentStatement instanceof ReturnStatement || currentStatement instanceof ContinueStatement)
			{
				addToAll(currentStatement, tempList);
				returnList.addAll(tempList);
				tempList = new ArrayList<AbstractControlCase>();
			}
		}
		return returnList;
	}

	// this version of createSwitchCases makes SwitchCases with no SwitchCase Statements but with all other statements of each case
//	private List<AbstractControlCase> createSwitchCases(SwitchStatement switchStatement)
//	{
//		List<AbstractControlCase> returnList  = new ArrayList<AbstractControlCase>();
//		List<AbstractControlCase> tempList    = new ArrayList<AbstractControlCase>();
//		List<Statement> switchGroupStatements = switchStatement.statements();
//		for (Statement currentStatement : switchGroupStatements)
//		{
//			if (currentStatement instanceof SwitchCase)
//			{
//				Expression caseValue = ((SwitchCase)currentStatement).getExpression();
//				SwitchControlCase newCase = new SwitchControlCase(this.variable, caseValue, new ArrayList<Statement>());
//				tempList.add(newCase);
//			}
//			else if (currentStatement instanceof BreakStatement || currentStatement instanceof ReturnStatement || currentStatement instanceof ContinueStatement)
//			{
//				addToAll(currentStatement, tempList);
//				returnList.addAll(tempList);
//				tempList = new ArrayList<AbstractControlCase>();
//			}
//			else
//			{
//				addToAll(currentStatement, tempList);
//			}
//		}
//		return returnList;
//	}
	
	private static List<AbstractControlCase> addToAll(Statement statement, List<AbstractControlCase> caseList)
	{
		for (AbstractControlCase currentCase : caseList)
		{
			currentCase.addBodyStatement(statement);
		}
		return caseList;
	}

	@Override
	public boolean match(IfControlStructure otherStructure, ASTNodeMatcher matcher)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean match(SwitchControlStructure otherStructure, ASTNodeMatcher matcher)
	{
		if (matcher.safeSubtreeMatch(this.variable, otherStructure.variable) && this.controlCases.size() == otherStructure.controlCases.size())
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
	public boolean match(TernaryControlStructure otherStructure, ASTNodeMatcher matcher)
	{
		return false;
	}

	@Override
	public List<ASTNode> getAdditionalFragments()
	{
		return new ArrayList<ASTNode>(((SwitchStatement)this.getNode()).statements());
	}
}
