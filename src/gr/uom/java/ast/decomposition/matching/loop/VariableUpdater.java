package gr.uom.java.ast.decomposition.matching.loop;

import org.eclipse.jdt.core.dom.Expression;

public class VariableUpdater
{
	private Expression updater;
	private Integer updateValue;
	
	public VariableUpdater(Expression updater)
	{
		this.updater     = updater;
		this.updateValue = AbstractLoopUtilities.getUpdateValue(updater);
	}
	
	public VariableUpdater(Integer updateValue)
	{
		this.updater     = null;
		this.updateValue = updateValue;
	}
	
	public Expression getUpdater()
	{
		return updater;
	}

	public Integer getUpdateValue()
	{
		return updateValue;
	}
	
	public boolean match(VariableUpdater otherUpdater)
	{
		return this.updateValue == otherUpdater.updateValue;
	}
}
