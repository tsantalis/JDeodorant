package gr.uom.java.ast.decomposition.matching.loop;

import java.util.HashMap;

public class ConditionalLoopBindingInformation {
	
	private static ConditionalLoopBindingInformation instance;
	private static HashMap<String, Integer> iteratorInstantiationMethodBindingStartValues;		// the start values of different "iterators" when created by these instantiation methods
	private static HashMap<String, VariableValue> conditionalExpressionEndValues;				// the end values of different "iterators" being checked when these methods are called
	private static HashMap<String, Integer> updateMethodValues;									// the value by which different "iterators" are updated when these methods are called
	private static HashMap<String, VariableValue> dataStructureSizeMethodEndValues;				// the end values of different methods being used in an InfixExpression to check the size of a data structure

	private ConditionalLoopBindingInformation()
	{
		iteratorInstantiationMethodBindingStartValues = new HashMap<String, Integer>();
		iteratorInstantiationMethodBindingStartValues.put(".iterator()Ljava/util/Iterator<TE;>;", 0);									// .iterator()<E>
		iteratorInstantiationMethodBindingStartValues.put(".iterator()Ljava/util/Iterator;", 0);										// .iterator()
		iteratorInstantiationMethodBindingStartValues.put(".listIterator()Ljava/util/ListIterator<TE;>;", 0);							// .listIterator()<E>
		iteratorInstantiationMethodBindingStartValues.put(".listIterator(I)Ljava/util/ListIterator<TE;>;", null);						// .listIterator(int)<E>
		iteratorInstantiationMethodBindingStartValues.put(".listIterator()Ljava/util/ListIterator;", 0);								// .listIterator()
		iteratorInstantiationMethodBindingStartValues.put(".listIterator(I)Ljava/util/ListIterator;", null);							// .listIterator(int)
		iteratorInstantiationMethodBindingStartValues.put(".elements()Ljava/util/Enumeration<TV;>;", 0);								// .elements()<V>
		iteratorInstantiationMethodBindingStartValues.put(".elements()Ljava/util/Enumeration;", 0);										// .elements()
		iteratorInstantiationMethodBindingStartValues.put("Ljava/util/StringTokenizer;.(Ljava/lang/String;)V", 0);						// StringTokenizer(String)
		iteratorInstantiationMethodBindingStartValues.put("Ljava/util/StringTokenizer;.(Ljava/lang/String;Ljava/lang/String;)V", 0);	// StringTokenizer(String, String)
		iteratorInstantiationMethodBindingStartValues.put("Ljava/util/StringTokenizer;.(Ljava/lang/String;Ljava/lang/String;Z)V", 0);	// StringTokenizer(String, String, boolean)
		
		conditionalExpressionEndValues = new HashMap<String, VariableValue>();
		conditionalExpressionEndValues.put("Ljava/util/Iterator;.hasNext()Z", new VariableValue(VariableValue.ValueType.DATA_STRUCTURE_SIZE));					// .hasNext() (from Iterator)
		conditionalExpressionEndValues.put("Ljava/util/ListIterator;.hasNext()Z", new VariableValue(VariableValue.ValueType.DATA_STRUCTURE_SIZE));				// .hasNext() (from ListIterator)
		conditionalExpressionEndValues.put("Ljava/util/ListIterator;.hasPrevious()Z", new VariableValue(0));													// .hasPrevious()
		conditionalExpressionEndValues.put("Ljava/util/Enumeration;.hasMoreElements()Z", new VariableValue(VariableValue.ValueType.DATA_STRUCTURE_SIZE));		// .hasMoreElements() (from Enumeration)
		conditionalExpressionEndValues.put("Ljava/util/StringTokenizer;.hasMoreElements()Z", new VariableValue(VariableValue.ValueType.DATA_STRUCTURE_SIZE));	// .hasMoreElements() (from StringTokenizer)
		conditionalExpressionEndValues.put("Ljava/util/StringTokenizer;.hasMoreTokens()Z", new VariableValue(VariableValue.ValueType.DATA_STRUCTURE_SIZE));		// .hasMoreTokens()

		
		updateMethodValues = new HashMap<String, Integer>();
		updateMethodValues.put("Ljava/util/Iterator;.next()TE;", 1);									// .next() (from Iterator)
		updateMethodValues.put("Ljava/util/ListIterator;.next()TE;", 1);								// .next() (from ListIterator)
		updateMethodValues.put("Ljava/util/ListIterator;.previous()TE;", (-1));							// .previous()
		updateMethodValues.put("Ljava/util/Enumeration;.nextElement()TE;", 1);							// .nextElement() (from Enumeration)
		updateMethodValues.put("Ljava/util/StringTokenizer;.nextElement()Ljava/lang/Object;", 1);		// .nextElement() (from StringTokenizer)
		updateMethodValues.put("Ljava/util/StringTokenizer;.nextToken()Ljava/lang/String;", 1);			// .nextToken()
		
		
		dataStructureSizeMethodEndValues = new HashMap<String, VariableValue>();
		dataStructureSizeMethodEndValues.put(".size()I", new VariableValue(VariableValue.ValueType.DATA_STRUCTURE_SIZE));		// .size()
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
	public boolean updateMethodValuesContains(String methodBindingKey)
	{
		return updateMethodValues.keySet().contains(methodBindingKey);
	}

	// checks if the dataStructureSizeMethods field contains the specified MethodBinding key
	public boolean dataStructureSizeMethodEndValuesContains(String methodBindingKey)
	{
		return dataStructureSizeMethodEndValues.keySet().contains(methodBindingKey);
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
	
	public Integer getUpdateMethodValue(String methodBindingKey)
	{
		return updateMethodValues.get(methodBindingKey);
	}
	
	public VariableValue getDataStructureSizeMethodEndValue(String methodBindingKey)
	{
		for (String methodBindingEnd : dataStructureSizeMethodEndValues.keySet())
		{
			if (methodBindingKey.endsWith(methodBindingEnd))
			{
				return dataStructureSizeMethodEndValues.get(methodBindingEnd);
			}
		}
		return null;
	}
}
