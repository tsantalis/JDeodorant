package gr.uom.java.ast.decomposition.matching.loop;

import java.util.HashMap;

public class ConditionalLoopBindingInformation {
	
	private static ConditionalLoopBindingInformation instance;
	private static HashMap<String, Integer> iteratorInstantiationMethodBindingStartValues;		// the start values of different "iterators" when created by these instantiation methods
	private static HashMap<String, VariableValue> conditionalExpressionEndValues;				// the end values of different "iterators" being checked when these methods are called
	private static HashMap<String, Integer> updateMethodUpdateValues;							// the value by which different "iterators" are updated when these methods are called

	private ConditionalLoopBindingInformation()
	{
		iteratorInstantiationMethodBindingStartValues = new HashMap<String, Integer>();
		iteratorInstantiationMethodBindingStartValues.put(".iterator()Ljava/util/Iterator<TE;>;", 0);				// .iterator()
		iteratorInstantiationMethodBindingStartValues.put(".listIterator()Ljava/util/ListIterator<TE;>;", 0);		// .listIterator()
		iteratorInstantiationMethodBindingStartValues.put(".listIterator(I)Ljava/util/ListIterator<TE;>;", null);	// .listIterator(int index)
		iteratorInstantiationMethodBindingStartValues.put(".elements()Ljava/util/Enumeration<TV;>;", 0);			// .elements()
		
		conditionalExpressionEndValues = new HashMap<String, VariableValue>();
		conditionalExpressionEndValues.put("Ljava/util/Iterator;.hasNext()Z", new VariableValue(VariableValue.ValueType.COLLECTION_SIZE));				// .hasNext() (from Iterator)
		conditionalExpressionEndValues.put("Ljava/util/ListIterator;.hasNext()Z", new VariableValue(VariableValue.ValueType.COLLECTION_SIZE));			// .hasNext() (from ListIterator)
		conditionalExpressionEndValues.put("Ljava/util/ListIterator;.hasPrevious()Z", new VariableValue(0));											// .hasPrevious()
		conditionalExpressionEndValues.put("Ljava/util/Enumeration;.hasMoreElements()Z", new VariableValue(VariableValue.ValueType.COLLECTION_SIZE));	// .hasMoreElements()

		updateMethodUpdateValues = new HashMap<String, Integer>();
		updateMethodUpdateValues.put("Ljava/util/Iterator;.next()TE;", 1);					// .next() (from Iterator)
		updateMethodUpdateValues.put("Ljava/util/ListIterator;.next()TE;", 1);				// .next() (from ListIterator)
		updateMethodUpdateValues.put("Ljava/util/ListIterator;.previous()TE;", (-1));		// .previous()
		updateMethodUpdateValues.put("Ljava/util/Enumeration;.nextElement()TE;", 1);		// .nextElement()
	}
	
	public static ConditionalLoopBindingInformation getInstance()
	{
		if (instance == null)
		{
			instance = new ConditionalLoopBindingInformation();
		}
		return instance;
	}

	// checks if the instantiationMethodBindings field contains the ending of the specified MethodBinding key String
	public boolean iteratorInstantiationMethodBindingStartValuesContains(String methodBindingKey)
	{
		for (String methodBindingEnd : iteratorInstantiationMethodBindingStartValues.keySet())
		{
			if (methodBindingKey.endsWith(methodBindingEnd))
			{
				return true;
			}
		}
		return false;
	}
	
	// checks if the conditionalMethodBindingEndValues field contains the specified MethodBinding key
	public boolean conditionalMethodBindingEndValuesContains(String methodBindingKey)
	{
		return conditionalExpressionEndValues.keySet().contains(methodBindingKey);
	}

	// checks if the updateMethodBindingUpdateValues field contains the specified MethodBinding key
	public boolean updateMethodBindingUpdateValuesContains(String methodBindingKey)
	{
		return updateMethodUpdateValues.keySet().contains(methodBindingKey);
	}
	
	public Integer getIteratorInstantiationMethodBindingStartValue(String methodBindingKey)
	{
		for (String methodBindingEnd : iteratorInstantiationMethodBindingStartValues.keySet())
		{
			if (methodBindingKey.endsWith(methodBindingEnd))
			{
				return iteratorInstantiationMethodBindingStartValues.get(methodBindingEnd);
			}
		}
		return null;
	}
	
	public VariableValue getConditionalMethodBindingEndValue(String methodBindingKey)
	{
		return conditionalExpressionEndValues.get(methodBindingKey);
	}
	
	public Integer getUpdateMethodBindingUpdateValue(String methodBindingKey)
	{
		return updateMethodUpdateValues.get(methodBindingKey);
	}
}
