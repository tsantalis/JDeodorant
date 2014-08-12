package gr.uom.java.ast.decomposition.matching.loop;

public class VariableValue
{
	public enum ValueType
	{
		INTEGER,
		VARIABLE,
		DATA_STRUCTURE_SIZE
	}
	
	private ValueType type;
	private Integer value;
	
	public VariableValue()
	{
		this.type  = ValueType.VARIABLE;
		this.value = null;
	}
	
	public VariableValue(Integer value)
	{
		this.type  = ValueType.INTEGER;
		this.value = value;
	}
	
	public VariableValue(ValueType type)
	{
		this.type  = type;
		this.value = null;
	}

	public ValueType getType()
	{
		return type;
	}

	public void setType(ValueType type)
	{
		this.type = type;
	}

	public Integer getValue()
	{
		return value;
	}

	public void setValue(Integer value)
	{
		this.value = value;
	}
	
	public boolean match(VariableValue otherVariableValue)
	{
		return this.type == otherVariableValue.type && this.value == otherVariableValue.value;
	}
}
