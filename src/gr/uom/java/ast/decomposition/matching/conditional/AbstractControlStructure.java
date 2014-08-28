package gr.uom.java.ast.decomposition.matching.conditional;

import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Statement;

public abstract class AbstractControlStructure
{
	private Statement node;
	protected List<AbstractControlCase> controlCases;
	
	public AbstractControlStructure(Statement node)
	{
		this.node = node;
	}
	
	@Override
	public String toString()
	{
		return node.toString();
	}
	
	public Statement getNode()
	{
		return node;
	}
	
	public boolean match(AbstractControlStructure otherControlStructure, ASTNodeMatcher matcher)
	{
		if (otherControlStructure instanceof IfControlStructure)
		{
			return this.match((IfControlStructure)otherControlStructure, matcher);
		}
		else if (otherControlStructure instanceof SwitchControlStructure)
		{
			return this.match((SwitchControlStructure)otherControlStructure, matcher);
		}
		else if (otherControlStructure instanceof TernaryControlStructure)
		{
			return this.match((TernaryControlStructure)otherControlStructure, matcher);
		}
		return false;
	}
	
	public abstract boolean match(IfControlStructure otherStructure, ASTNodeMatcher matcher);
	public abstract boolean match(SwitchControlStructure otherStructure, ASTNodeMatcher matcher);
	public abstract boolean match(TernaryControlStructure otherStructure, ASTNodeMatcher matcher);
	public abstract List<ASTNode> getAdditionalFragments();
}
