package gr.uom.java.ast.decomposition.matching.conditional;

import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;

import java.util.List;

import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
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
					if(currentThisSwitchCase.isDefault() && currentOtherSwitchCase.isDefault())
					{
						switchCaseStatementMatch = true;
					}
					else
					{
						switchCaseStatementMatch = matchCaseCondition(currentThisSwitchCase.getExpression(), currentOtherSwitchCase.getExpression());
					}
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
		if(caseCondition1 != null && caseCondition2 != null && caseCondition1.resolveTypeBinding().isEqualTo(caseCondition2.resolveTypeBinding()))
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
			// Enum, constant variables with different bindings but same values
			else if (caseCondition1 instanceof Name && caseCondition2 instanceof Name)
			{
				String nameValue1 = getConstantValue((Name)caseCondition1);
				String nameValue2 = getConstantValue((Name)caseCondition2);
				return (((Name)caseCondition1).resolveBinding().isEqualTo(((Name)caseCondition2).resolveBinding()) ||
						(nameValue1 != null && nameValue2 != null && nameValue1.equals(nameValue2)));
			}
			// constant variable and a literal value
			else if (caseCondition1 instanceof Name)
			{
				String nameValue = getConstantValue((Name)caseCondition1);
				String otherValue = getLiteralValue(caseCondition2);
				return (nameValue != null && otherValue != null && nameValue.equals(otherValue));
			}
			// literal value and a constant variable
			else if (caseCondition2 instanceof Name)
			{
				String nameValue = getConstantValue((Name)caseCondition2);
				String otherValue = getLiteralValue(caseCondition2);
				return (nameValue != null && otherValue != null && nameValue.equals(otherValue));
			}
		}
		return false;
	}
	
	private String getConstantValue(Name name)
	{
		IBinding nameBinding = name.resolveBinding();
		if (nameBinding.getKind() == IBinding.VARIABLE)
		{
			IVariableBinding nameVariableBinding = (IVariableBinding)nameBinding;
			Object valueObject = nameVariableBinding.getConstantValue();
			if (valueObject instanceof Integer || valueObject instanceof Byte || valueObject instanceof Character || valueObject instanceof String)
			{
				return valueObject.toString();
			}
		}
		return null;
	}
	
	private String getLiteralValue(Expression expression)
	{
		if (expression instanceof NumberLiteral)
		{
			return ((NumberLiteral)expression).getToken();
		}
		else if (expression instanceof CharacterLiteral)
		{
			return String.valueOf(((CharacterLiteral)expression).charValue());
		}
		else if (expression instanceof StringLiteral)
		{
			return ((StringLiteral)expression).getLiteralValue();
		}
		else
		{
			return null;
		}
	}
}
