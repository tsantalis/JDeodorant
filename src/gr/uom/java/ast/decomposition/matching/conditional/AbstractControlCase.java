package gr.uom.java.ast.decomposition.matching.conditional;

import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;

import java.util.List;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;

public abstract class AbstractControlCase
{
	protected Expression caseCondition;
	protected List<Statement> body;

	@Override
	public String toString()
	{
		StringBuilder returnString = new StringBuilder();
		returnString.append("caseCondition = " + this.caseCondition + "\n");
		for (Statement currentStatement : this.body)
		{
			returnString.append("  " + currentStatement);
		}
		return returnString.toString();
	}
	
	public void addBodyStatement(Statement statement)
	{
		this.body.add(statement);
	}
	
	public List<Statement> getBody()
	{
		return this.body;
	}

	public boolean isDefaultCase()
	{
		return this.caseCondition == null;
	}

	public boolean match(AbstractControlCase otherCase, ASTNodeMatcher matcher)
	{
		if (otherCase instanceof IfControlCase)
		{
			return this.match((IfControlCase)otherCase, matcher);
		}
		else if (otherCase instanceof SwitchControlCase)
		{
			return this.match((SwitchControlCase)otherCase, matcher);
		}
		return false;
	}
	
	public abstract boolean match(IfControlCase otherCase, ASTNodeMatcher matcher);
	public abstract boolean match(SwitchControlCase otherCase, ASTNodeMatcher matcher);
}
