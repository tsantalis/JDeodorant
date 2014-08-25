package gr.uom.java.ast.decomposition.matching.conditional;

import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;

import java.util.List;

import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SwitchCase;

public class SwitchControlCase extends AbstractControlCase
{	
	public SwitchControlCase(Expression switchVariable, Expression caseValue, List<Statement> body)
	{
		this.caseCondition  = caseValue;
		this.body           = AbstractControlStructureUtilities.unBlock(body);
	}

	@Override
	public boolean match(IfControlCase otherCase, ASTNodeMatcher matcher)
	{
		return false;
	}

	@Override
	public boolean match(SwitchControlCase otherCase, ASTNodeMatcher matcher)
	{
		if (this.body.size() == otherCase.body.size())
		{
			boolean caseStatementsMatch = true;
			for (int i = 0; i < this.body.size(); i++)
			{
				Statement currentThisStatement = this.body.get(i);
				Statement currentOtherStatement = otherCase.body.get(i);
				boolean switchCaseStatementMatch = false;
				if (currentThisStatement instanceof SwitchCase && currentOtherStatement instanceof SwitchCase)
				{
					SwitchCase currentThisSwitchCase = (SwitchCase)currentThisStatement;
					SwitchCase currentOtherSwitchCase = (SwitchCase)currentOtherStatement;
					switchCaseStatementMatch = matchCaseCondition(currentThisSwitchCase.getExpression(), currentOtherSwitchCase.getExpression());
				}
				boolean endStatementMatch = ((currentThisStatement instanceof BreakStatement && currentOtherStatement instanceof BreakStatement) ||
						(currentThisStatement instanceof ContinueStatement && currentOtherStatement instanceof ContinueStatement) ||
						(currentThisStatement instanceof ReturnStatement && currentOtherStatement instanceof ReturnStatement));
				if (!switchCaseStatementMatch && !endStatementMatch)
				{
					caseStatementsMatch = false;
				}
			}
			return caseStatementsMatch;
		}
		return false;
	}

	private boolean matchCaseCondition(Expression caseCondition1, Expression caseCondition2)
	{
		if(caseCondition1.resolveTypeBinding().isEqualTo(caseCondition2.resolveTypeBinding()))
		{
			// int, byte
			if (caseCondition1 instanceof NumberLiteral && caseCondition2 instanceof NumberLiteral)
			{
				return ((NumberLiteral)caseCondition1).getToken().equals(((NumberLiteral)caseCondition2).getToken());
			}
			// char
			else if (caseCondition1 instanceof CharacterLiteral && caseCondition2 instanceof CharacterLiteral)
			{
				return ((CharacterLiteral)caseCondition1).charValue() == ((CharacterLiteral)caseCondition2).charValue();
			}
			// String
			else if (caseCondition1 instanceof StringLiteral && caseCondition2 instanceof StringLiteral)
			{
				return ((StringLiteral)caseCondition1).getLiteralValue().equals(((StringLiteral)caseCondition2).getLiteralValue());
			}
			// Enum
			else if (caseCondition1 instanceof Name && caseCondition2 instanceof Name)
			{
				return ((Name)caseCondition1).resolveBinding().isEqualTo(((Name)caseCondition2).resolveBinding());
			}
		}
		return false;
	}
}
