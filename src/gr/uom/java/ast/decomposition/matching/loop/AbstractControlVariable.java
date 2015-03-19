package gr.uom.java.ast.decomposition.matching.loop;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Expression;

public class AbstractControlVariable
{
	protected VariableValue startValue;
	protected VariableValue endValue;
	protected List<VariableUpdater> variableUpdaters;
	protected Expression dataStructureExpression;

	public AbstractControlVariable()
	{
		this.startValue       = new VariableValue(VariableValue.ValueType.VARIABLE);
		this.endValue         = new VariableValue(VariableValue.ValueType.VARIABLE);
		this.variableUpdaters = new ArrayList<VariableUpdater>();
	}
	
	public AbstractControlVariable(VariableValue startValue, VariableValue endValue, List<VariableUpdater> variableUpdaters,
			Expression dataStructureExpression)
	{
		this.startValue              = startValue;
		this.endValue                = endValue;
		this.variableUpdaters        = variableUpdaters;
		this.dataStructureExpression = dataStructureExpression;
	}

	public List<VariableUpdater> getVariableUpdaters()
	{
		return variableUpdaters;
	}

	public Expression getDataStructureExpression()
	{
		return dataStructureExpression;
	}

	public boolean match(AbstractControlVariable otherControlVariable)
	{
		return this.startValue.match(otherControlVariable.startValue) &&
				this.endValue.match(otherControlVariable.endValue) &&
				equalUpdaterLists(this.variableUpdaters, otherControlVariable.variableUpdaters);
	}
	
	private static boolean equalUpdaterLists(List<VariableUpdater> updaters1, List<VariableUpdater> updaters2)
	{
		if (updaters1.size() != updaters2.size())
		{
			return false;
		}
		
		// updaters must be in the same order and each pair must have the same update value
		for (int i = 0; i < updaters1.size(); i++)
		{
			VariableUpdater currentUpdater1 = updaters1.get(i);
			VariableUpdater currentUpdater2 = updaters2.get(i);
			
			if (currentUpdater1 != null && currentUpdater2 != null && !currentUpdater1.match(currentUpdater2))
			{
				return false;
			}
		}
		return true;
	}
}
