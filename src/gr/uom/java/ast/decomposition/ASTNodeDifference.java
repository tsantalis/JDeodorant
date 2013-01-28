package gr.uom.java.ast.decomposition;

import java.util.ArrayList;
import java.util.List;

public class ASTNodeDifference {
	private AbstractMethodFragment fragment1;
	private AbstractMethodFragment fragment2;
	private List<Difference> differences;
	private List<ASTNodeDifference> children;
	
	public ASTNodeDifference(AbstractMethodFragment f1,AbstractMethodFragment f2) {
		this.fragment1=f1;
		this.fragment2=f2;
		differences = new ArrayList<Difference>();
		children = new ArrayList<ASTNodeDifference>();
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
	
	public void addChild(ASTNodeDifference diff)
	{
		children.add(diff);
	}
	
	public void addDifferences(List<Difference> differences)
	{
		this.differences.addAll(differences);
	}
	
	public boolean isParameterizable()
	{
		for(Difference difference : differences)
		{
			if(typeMismatch(difference))
				return false;
		}
		for(ASTNodeDifference child : children)
		{
			boolean isParameterizable = child.isParameterizable();
			if(!isParameterizable) return false;
		}
		return true;
	}
	
	private boolean typeMismatch(Difference diff)
	{
		if(diff.getType().equals(DifferenceType.AST_TYPE_MISMATCH)
				||diff.getType().equals(DifferenceType.VARIABLE_TYPE_MISMATCH)
					||diff.getType().equals(DifferenceType.LITERAL_TYPE_MISMATCH))
						return true;
		return false;
	}
	
	public List<Difference> getLeafDifferences() {
		List<Difference> diffs = new ArrayList<Difference>();
		for(Difference difference : differences)
		{
			if(isLeafDifference(difference))
				diffs.add(difference);
		}
		for(ASTNodeDifference child : children)
		{
			diffs.addAll(child.getLeafDifferences());
		}
		return diffs;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for(Difference difference : differences)
		{
			if(isLeafDifference(difference))
				sb.append(difference.toString()).append("\n");
		}
		for(ASTNodeDifference child : children)
		{
			sb.append(child.toString());
		}
		return sb.toString();
	}
	
	private boolean isLeafDifference(Difference diff)
	{
		if(diff.getType().equals(DifferenceType.VARIABLE_NAME_MISMATCH)
				||diff.getType().equals(DifferenceType.VARIABLE_TYPE_MISMATCH)
					||diff.getType().equals(DifferenceType.LITERAL_TYPE_MISMATCH)
						||diff.getType().equals(DifferenceType.LITERAL_VALUE_MISMATCH))
							return true;
		return false;
	}
}
