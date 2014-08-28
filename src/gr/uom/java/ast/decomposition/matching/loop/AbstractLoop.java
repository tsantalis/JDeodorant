package gr.uom.java.ast.decomposition.matching.loop;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.WhileStatement;

public abstract class AbstractLoop
{
	private Statement loopStatement;
	protected Expression condition;
	
	public AbstractLoop(Statement loopStatement)
	{
		this.loopStatement = loopStatement;
	}
	
	public Statement getLoopStatement()
	{
		return loopStatement;
	}
	
	public Statement getLoopBody()
	{
		if (loopStatement instanceof WhileStatement)
		{
			return ((WhileStatement)loopStatement).getBody();
		}
		else if (loopStatement instanceof ForStatement)
		{
			return ((ForStatement)loopStatement).getBody();
		}
		else if (loopStatement instanceof DoStatement)
		{
			return ((DoStatement)loopStatement).getBody();
		}
		else if (loopStatement instanceof EnhancedForStatement)
		{
			return ((EnhancedForStatement)loopStatement).getBody();
		}
		return null;
	}

	public boolean match(AbstractLoop otherLoop, ConditionalLoopASTNodeMatcher matcher)
	{
		if (otherLoop instanceof ConditionalLoop)
		{
			return this.match((ConditionalLoop)otherLoop, matcher);
		}
		if (otherLoop instanceof EnhancedForLoop)
		{
			return this.match((EnhancedForLoop)otherLoop, matcher);
		}
		return false;
	}
	
	public abstract boolean match(ConditionalLoop otherLoop, ConditionalLoopASTNodeMatcher matcher);
	public abstract boolean match(EnhancedForLoop otherLoop, ConditionalLoopASTNodeMatcher matcher);
	public abstract List<ASTNode> getAdditionalFragments();
}
