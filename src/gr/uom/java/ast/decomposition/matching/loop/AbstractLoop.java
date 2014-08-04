package gr.uom.java.ast.decomposition.matching.loop;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;

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
