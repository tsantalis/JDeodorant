package gr.uom.java.ast.decomposition;

import java.util.ArrayList;
import java.util.List;

public class ASTNodeDifference {
	private AbstractMethodFragment fragment1;
	private AbstractMethodFragment fragment2;
	private List<Difference> differences;
	
	public ASTNodeDifference(AbstractMethodFragment f1,AbstractMethodFragment f2) {
		this.fragment1=f1;
		this.fragment2=f2;
		differences = new ArrayList<Difference>();
	}
	
	public List<Difference> getDifferences() {
		return differences;
	}
	
	public AbstractMethodFragment getFragment1() {
		return fragment1;
	}

	public AbstractMethodFragment getFragment2() {
		return fragment2;
	}

	public void addDifference(Difference diff)
	{
		differences.add(diff);
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
