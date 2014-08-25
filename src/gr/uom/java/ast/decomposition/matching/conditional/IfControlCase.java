package gr.uom.java.ast.decomposition.matching.conditional;

import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;

public class IfControlCase extends AbstractControlCase
{
	public IfControlCase(Expression condition, Statement body)
	{
		this.caseCondition = condition;
		List<Statement> statements = new ArrayList<Statement>();
		statements.add(body);
		this.body = AbstractControlStructureUtilities.unBlock(statements);
	}

	@Override
	public boolean match(IfControlCase otherCase, ASTNodeMatcher matcher)
	{
		return matcher.safeSubtreeMatch(this.caseCondition, otherCase.caseCondition);
	}

	@Override
	public boolean match(SwitchControlCase otherCase, ASTNodeMatcher matcher)
	{
		return false;
	}
}
