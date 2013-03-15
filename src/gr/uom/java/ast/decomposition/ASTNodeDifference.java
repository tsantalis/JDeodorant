package gr.uom.java.ast.decomposition;

import java.util.ArrayList;
import java.util.List;

public class ASTNodeDifference {
	private AbstractExpression expression1;
	private AbstractExpression expression2;
	private List<Difference> differences;
	
	public ASTNodeDifference(AbstractExpression e1, AbstractExpression e2) {
		this.expression1=e1;
		this.expression2=e2;
		differences = new ArrayList<Difference>();
	}
	
	public List<Difference> getDifferences() {
		return differences;
	}
	
	public AbstractExpression getExpression1() {
		return expression1;
	}

	public AbstractExpression getExpression2() {
		return expression2;
	}

	public void addDifference(Difference diff)
	{
		differences.add(diff);
	}
	
	public boolean containsDifferenceType(DifferenceType type) {
		for(Difference difference : differences)
		{
			if(difference.getType().equals(type))
				return true;
		}
		return false;
	}

	public boolean isParameterizable()
	{
		for(Difference difference : differences)
		{
			if(typeMismatch(difference))
				return false;
		}
		return true;
	}
	
	private boolean typeMismatch(Difference diff)
	{
		if(diff.getType().equals(DifferenceType.AST_TYPE_MISMATCH)
			||diff.getType().equals(DifferenceType.VARIABLE_TYPE_MISMATCH))
					return true;
		return false;
	}

	public boolean isEmpty() {
		return differences.isEmpty();
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for(Difference difference : differences)
		{
			sb.append(difference.toString()).append("\n");
		}
		return sb.toString();
	}
}
