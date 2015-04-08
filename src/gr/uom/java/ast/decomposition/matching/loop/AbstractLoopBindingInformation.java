package gr.uom.java.ast.decomposition.matching.loop;

import java.util.ArrayList;
import java.util.HashMap;

public class AbstractLoopBindingInformation
{
	private static AbstractLoopBindingInformation instance;
	private static HashMap<String, Integer> iteratorInstantiationMethodBindingStartValues;		// the start values of different "iterators" when created by these instantiation methods
	private static HashMap<String, VariableValue> conditionalExpressionEndValues;				// the end values of different "iterators" being checked when these methods are called
	private static HashMap<String, Integer> updateMethodValues;									// the value by which different "iterators" are updated when these methods are called
	private static ArrayList<String> dataStructureSizeMethods;									// the end values of different methods being used in an InfixExpression to check the size of a data structure
	private static ArrayList<String> dataStructureAccessMethods;

	private AbstractLoopBindingInformation()
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
		iteratorInstantiationMethodBindingStartValues.put("Ljava/util/Properties;.propertyNames()Ljava/util/Enumeration<*>;", 0);		// Properies.propertyNames()
		
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
		
		dataStructureSizeMethods = new ArrayList<String>();
		// .size method of a collection is handled by the method isCollectionSizeInvocation(Expression) in the AbstractLoopUtilities class
		// .length of an array is handled by the method isLengthFieldAccess(Expression) in the AbstractLoopUtilities class
		dataStructureSizeMethods.add("Ljava/lang/String;.length()I");			// .length() (from String)
		dataStructureSizeMethods.add(".getSize()I");							// .getSize() (from any class)
		dataStructureSizeMethods.add(".getLength()I");							// .getLength() (from any class)
		dataStructureSizeMethods.add(".size()I");								// .size() (from any class)
		dataStructureSizeMethods.add(".length()I");								// .length() (from any class)
		
		dataStructureAccessMethods = new ArrayList<String>();
		dataStructureAccessMethods.add("Ljava/util/ArrayList;.get(I)TE;");						// .get(int) (from ArrayList)
		dataStructureAccessMethods.add("Ljava/util/LinkedList;.get(I)TE;");						// .get(int) (from LinkedList)
		dataStructureAccessMethods.add("Ljava/util/Vector;.get(I)TE;");							// .get(int) (from Vector)
		dataStructureAccessMethods.add("Ljava/util/AbstractList;.get(I)TE;");					// .get(int) (from AbstractList)
		dataStructureAccessMethods.add("Ljava/util/List;.get(I)TE;");							// .get(int) (from List)
		dataStructureAccessMethods.add("Ljava/util/AbstractSequentialList;.get(I)TE;");			// .get(int) (from AbstractSequentialList)
		dataStructureAccessMethods.add("Ljava/util/Stack;.get(I)TE;");							// .get(int) (from Stack)
		dataStructureAccessMethods.add("Ljava/util/Vector;.elementAt(I)TE;");					// .elementAt(int) (from Vector)
		dataStructureAccessMethods.add("Ljava/util/Stack;.elementAt(I)TE;");					// .elementAt(int) (from Stack)
		dataStructureAccessMethods.add("Ljava/lang/String;.charAt(I)C");						// .charAt(int) (from String)
	}
	
	public static AbstractLoopBindingInformation getInstance()
	{
		if (instance == null)
		{
			instance = new AbstractLoopBindingInformation();
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
	public boolean dataStructureSizeMethodContains(String methodBindingKey)
	{
		for (String methodBindingEnd : dataStructureSizeMethods)
		{
			if (methodBindingKey.endsWith(methodBindingEnd))
			{
				return true;
			}
		}
		return false;
	}

	// checks if the dataStructureSizeMethods field contains the specified MethodBinding key
	public boolean dataStructureAccessMethodsContains(String methodBindingKey)
	{
		return dataStructureAccessMethods.contains(methodBindingKey);
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
}
