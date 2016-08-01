package gr.uom.java.ast;

import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CompositeVariable;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

public class Indexer {
	//String key and value correspond to MethodDeclaration.resolveBinding.getKey()
	private Map<String, LinkedHashSet<String>> methodInvocationMap;
	//String key corresponds to MethodDeclaration.resolveBinding.getKey()
	private Map<String, LinkedHashSet<PlainVariable>> definedFieldMap;
	//String key corresponds to MethodDeclaration.resolveBinding.getKey()
	private Map<String, LinkedHashSet<PlainVariable>> usedFieldMap;
	private Map<IType, LinkedHashSet<IType>> subTypeMap;
	//String key and value correspond to MethodDeclaration.resolveBinding.getKey()
	private Map<String, LinkedHashSet<String>> overridingMethodMap;
	//String key and value correspond to MethodDeclaration.resolveBinding.getKey()
	private Map<String, HashMap<PlainVariable, LinkedHashSet<String>>> methodInvocationThroughReferenceMap;
	//String corresponds to MethodDeclaration.resolveBinding.getKey()
	private Set<String> abstractMethodSet;
	private Set<String> nativeMethodSet;
	//String key corresponds to MethodDeclaration.resolveBinding.getKey()
	private Map<String, LinkedHashSet<String>> thrownExceptionTypeMap;
	//String key corresponds to MethodDeclaration.resolveBinding.getKey()
	protected Map<String, HashMap<Integer, LinkedHashSet<AbstractVariable>>> usedFieldsForMethodArgumentsMap;
	//String key corresponds to MethodDeclaration.resolveBinding.getKey()
	protected Map<String, HashMap<Integer, LinkedHashSet<AbstractVariable>>> definedFieldsForMethodArgumentsMap;
	
	public Indexer() {
		this.methodInvocationMap = new HashMap<String, LinkedHashSet<String>>();
		this.definedFieldMap = new HashMap<String, LinkedHashSet<PlainVariable>>();
		this.usedFieldMap = new HashMap<String, LinkedHashSet<PlainVariable>>();
		this.subTypeMap = new HashMap<IType, LinkedHashSet<IType>>();
		this.overridingMethodMap = new HashMap<String, LinkedHashSet<String>>();
		this.methodInvocationThroughReferenceMap = new HashMap<String, HashMap<PlainVariable, LinkedHashSet<String>>>();
		this.abstractMethodSet = new LinkedHashSet<String>();
		this.nativeMethodSet = new LinkedHashSet<String>();
		this.thrownExceptionTypeMap = new HashMap<String, LinkedHashSet<String>>();
		this.usedFieldsForMethodArgumentsMap = new HashMap<String, HashMap<Integer, LinkedHashSet<AbstractVariable>>>();
		this.definedFieldsForMethodArgumentsMap = new HashMap<String, HashMap<Integer, LinkedHashSet<AbstractVariable>>>();
	}
	
	public Set<IType> getSubTypes(IType superType) {
		if(subTypeMap.containsKey(superType)) {
			Set<IType> subTypes = subTypeMap.get(superType);
			LinkedHashSet<IType> subTypesOfAbstractSubTypes = new LinkedHashSet<IType>();
			try {
				for(IType subType: subTypes) {
					if(Flags.isAbstract(subType.getFlags()) && !subType.equals(superType)) {
						subTypesOfAbstractSubTypes.addAll(getSubTypes(subType));
					}
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
			LinkedHashSet<IType> finalSubTypes = new LinkedHashSet<IType>();
			finalSubTypes.addAll(subTypes);
			finalSubTypes.addAll(subTypesOfAbstractSubTypes);
			return finalSubTypes;
		}
		else {
			IPackageFragment packageFragment = superType.getPackageFragment();
			final LinkedHashSet<IType> subTypes = new LinkedHashSet<IType>();
			final LinkedHashSet<IType> subTypesOfAbstractSubTypes = new LinkedHashSet<IType>();
			try {
				SearchPattern searchPattern = SearchPattern.createPattern(superType, IJavaSearchConstants.IMPLEMENTORS);
				SearchEngine searchEngine = new SearchEngine();
				IJavaSearchScope scope = null;
				if(ASTReader.getSystemObject().getClassObject(superType.getFullyQualifiedName()) == null)
					scope = SearchEngine.createJavaSearchScope(new IJavaElement[] {packageFragment}, false);
				else
					scope = SearchEngine.createStrictHierarchyScope(ASTReader.getExaminedProject(), superType, 
							true, false, null);
				SearchRequestor requestor = new TypeSearchRequestor(subTypes);
				searchEngine.search(searchPattern, new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
						scope, requestor, null);
				subTypeMap.put(superType, subTypes);
				for(IType subType: subTypes) {
					if(Flags.isAbstract(subType.getFlags()) && !subType.equals(superType)) {
						subTypesOfAbstractSubTypes.addAll(getSubTypes(subType));
					}
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
			LinkedHashSet<IType> finalSubTypes = new LinkedHashSet<IType>();
			finalSubTypes.addAll(subTypes);
			finalSubTypes.addAll(subTypesOfAbstractSubTypes);
			return finalSubTypes;
		}
	}
	
	public void addInvokedMethod(MethodDeclaration originalMethod, MethodDeclaration invokedMethod) {
		String originalMethodBindingKey = originalMethod.resolveBinding().getKey();
		String invokedMethodBindingKey = invokedMethod.resolveBinding().getKey();
		//check if the invoked method is abstract or native
		if(invokedMethod.getBody() == null)
			abstractMethodSet.add(invokedMethodBindingKey);
		if((invokedMethod.getModifiers() & Modifier.NATIVE) != 0)
			nativeMethodSet.add(invokedMethodBindingKey);
		if(methodInvocationMap.containsKey(originalMethodBindingKey)) {
			LinkedHashSet<String> invokedMethods = methodInvocationMap.get(originalMethodBindingKey);
			invokedMethods.add(invokedMethodBindingKey);
		}
		else {
			LinkedHashSet<String> invokedMethods = new LinkedHashSet<String>();
			invokedMethods.add(invokedMethodBindingKey);
			methodInvocationMap.put(originalMethodBindingKey, invokedMethods);
		}
	}
	
	public void addInvokedMethodThroughReference(MethodDeclaration originalMethod, MethodDeclaration invokedMethod, PlainVariable fieldReference) {
		String originalMethodBindingKey = originalMethod.resolveBinding().getKey();
		String invokedMethodBindingKey = invokedMethod.resolveBinding().getKey();
		//check if the invoked method is abstract or native
		if(invokedMethod.getBody() == null)
			abstractMethodSet.add(invokedMethodBindingKey);
		if((invokedMethod.getModifiers() & Modifier.NATIVE) != 0)
			nativeMethodSet.add(invokedMethodBindingKey);
		if(methodInvocationThroughReferenceMap.containsKey(originalMethodBindingKey)) {
			HashMap<PlainVariable, LinkedHashSet<String>> invokedMethodsThroughReference = methodInvocationThroughReferenceMap.get(originalMethodBindingKey);
			if(invokedMethodsThroughReference.containsKey(fieldReference)) {
				LinkedHashSet<String> invokedMethods = invokedMethodsThroughReference.get(fieldReference);
				invokedMethods.add(invokedMethodBindingKey);
			}
			else {
				LinkedHashSet<String> invokedMethods = new LinkedHashSet<String>();
				invokedMethods.add(invokedMethodBindingKey);
				invokedMethodsThroughReference.put(fieldReference, invokedMethods);
			}
		}
		else {
			LinkedHashSet<String> invokedMethods = new LinkedHashSet<String>();
			invokedMethods.add(invokedMethodBindingKey);
			HashMap<PlainVariable, LinkedHashSet<String>> invokedMethodsThroughReference = new HashMap<PlainVariable, LinkedHashSet<String>>();
			invokedMethodsThroughReference.put(fieldReference, invokedMethods);
			methodInvocationThroughReferenceMap.put(originalMethodBindingKey, invokedMethodsThroughReference);
		}
	}
	
	public Map<String, Set<PlainVariable>> getRecursivelyInvocationReferences(String methodBindingKey, Set<String> processedMethods) {
		//String originalMethodBindingKey = originalMethod.resolveBinding().getKey();
		Map<String, Set<PlainVariable>> invocationReferenceMap = new LinkedHashMap<String, Set<PlainVariable>>();
		if(methodInvocationThroughReferenceMap.containsKey(methodBindingKey)) {
			HashMap<PlainVariable, LinkedHashSet<String>> invokedMethodsThroughReference = methodInvocationThroughReferenceMap.get(methodBindingKey);
			invocationReferenceMap.put(methodBindingKey, invokedMethodsThroughReference.keySet());
		}
		processedMethods.add(methodBindingKey);
		LinkedHashSet<String> invokedMethods = methodInvocationMap.get(methodBindingKey);
		if(invokedMethods != null) {
			for(String invokedMethodBindingKey : invokedMethods) {
				if(!processedMethods.contains(invokedMethodBindingKey)) {
					if(!abstractMethodSet.contains(invokedMethodBindingKey) && !overridingMethodMap.containsKey(invokedMethodBindingKey)) {
						if(nativeMethodSet.contains(invokedMethodBindingKey)) {
							//method is native
						}
						else {
							invocationReferenceMap.putAll(getRecursivelyInvocationReferences(invokedMethodBindingKey, processedMethods));
						}
					}
					else {
						LinkedHashSet<String> overridingMethods = overridingMethodMap.get(invokedMethodBindingKey);
						processedMethods.add(invokedMethodBindingKey);
						if(overridingMethods != null) {
							for(String overridingMethodBindingKey : overridingMethods) {
								if(nativeMethodSet.contains(overridingMethodBindingKey)) {
									//method is native
								}
								else {
									invocationReferenceMap.putAll(getRecursivelyInvocationReferences(overridingMethodBindingKey, processedMethods));
								}
							}
						}
					}
				}
			}
		}
		return invocationReferenceMap;
	}
	
	public void addOverridingMethod(MethodDeclaration abstractMethod, MethodDeclaration overridingMethod) {
		String abstractMethodBindingKey = abstractMethod.resolveBinding().getKey();
		String overridingMethodBindingKey = overridingMethod.resolveBinding().getKey();
		//check if the overriding method is abstract or native
		if(overridingMethod.getBody() == null)
			abstractMethodSet.add(overridingMethodBindingKey);
		if((overridingMethod.getModifiers() & Modifier.NATIVE) != 0)
			nativeMethodSet.add(overridingMethodBindingKey);
		if(overridingMethodMap.containsKey(abstractMethodBindingKey)) {
			LinkedHashSet<String> overridingMethods = overridingMethodMap.get(abstractMethodBindingKey);
			overridingMethods.add(overridingMethodBindingKey);
		}
		else {
			LinkedHashSet<String> overridingMethods = new LinkedHashSet<String>();
			overridingMethods.add(overridingMethodBindingKey);
			overridingMethodMap.put(abstractMethodBindingKey, overridingMethods);
		}
	}
	
	public void setDefinedFields(MethodDeclaration method, LinkedHashSet<PlainVariable> fields) {
		String methodBindingKey = method.resolveBinding().getKey();
		definedFieldMap.put(methodBindingKey, fields);
	}
	
	public void setUsedFields(MethodDeclaration method, LinkedHashSet<PlainVariable> fields) {
		String methodBindingKey = method.resolveBinding().getKey();
		usedFieldMap.put(methodBindingKey, fields);
	}
	
	public void setThrownExceptionTypes(MethodDeclaration method, LinkedHashSet<String> exceptionTypes) {
		String methodBindingKey = method.resolveBinding().getKey();
		thrownExceptionTypeMap.put(methodBindingKey, exceptionTypes);
	}
	
	public boolean isAnalyzed(String methodBindingKey) {
		//String methodBindingKey = method.resolveBinding().getKey();
		if(definedFieldMap.containsKey(methodBindingKey) && usedFieldMap.containsKey(methodBindingKey))
			return true;
		else
			return false;
	}
	
	public LinkedHashSet<PlainVariable> getRecursivelyDefinedFields(String methodBindingKey,
			Set<String> processedMethods) {
		LinkedHashSet<PlainVariable> definedFields = new LinkedHashSet<PlainVariable>();
		if(definedFieldMap.containsKey(methodBindingKey))
			definedFields.addAll(definedFieldMap.get(methodBindingKey));
		processedMethods.add(methodBindingKey);
		LinkedHashSet<String> invokedMethods = methodInvocationMap.get(methodBindingKey);
		if(invokedMethods != null) {
			for(String invokedMethodBindingKey : invokedMethods) {
				if(!processedMethods.contains(invokedMethodBindingKey)) {
					if(!abstractMethodSet.contains(invokedMethodBindingKey) && !overridingMethodMap.containsKey(invokedMethodBindingKey)) {
						if(nativeMethodSet.contains(invokedMethodBindingKey)) {
							//method is native
						}
						else {
							definedFields.addAll(getRecursivelyDefinedFields(invokedMethodBindingKey, processedMethods));
						}
					}
					else {
						LinkedHashSet<String> overridingMethods = overridingMethodMap.get(invokedMethodBindingKey);
						processedMethods.add(invokedMethodBindingKey);
						if(overridingMethods != null) {
							for(String overridingMethodBindingKey : overridingMethods) {
								if(nativeMethodSet.contains(overridingMethodBindingKey)) {
									//method is native
								}
								else {
									definedFields.addAll(getRecursivelyDefinedFields(overridingMethodBindingKey, processedMethods));
								}
							}
						}
					}
				}
			}
		}
		return definedFields;
	}
	
	public LinkedHashSet<PlainVariable> getRecursivelyUsedFields(String methodBindingKey,
			Set<String> processedMethods) {
		LinkedHashSet<PlainVariable> usedFields = new LinkedHashSet<PlainVariable>();
		if(usedFieldMap.containsKey(methodBindingKey))
			usedFields.addAll(usedFieldMap.get(methodBindingKey));
		processedMethods.add(methodBindingKey);
		LinkedHashSet<String> invokedMethods = methodInvocationMap.get(methodBindingKey);
		if(invokedMethods != null) {
			for(String invokedMethodBindingKey : invokedMethods) {
				if(!processedMethods.contains(invokedMethodBindingKey)) {
					if(!abstractMethodSet.contains(invokedMethodBindingKey) && !overridingMethodMap.containsKey(invokedMethodBindingKey)) {
						if(nativeMethodSet.contains(invokedMethodBindingKey)) {
							//method is native
						}
						else {
							usedFields.addAll(getRecursivelyUsedFields(invokedMethodBindingKey, processedMethods));
						}
					}
					else {
						LinkedHashSet<String> overridingMethods = overridingMethodMap.get(invokedMethodBindingKey);
						processedMethods.add(invokedMethodBindingKey);
						if(overridingMethods != null) {
							for(String overridingMethodBindingKey : overridingMethods) {
								if(nativeMethodSet.contains(overridingMethodBindingKey)) {
									//method is native
								}
								else {
									usedFields.addAll(getRecursivelyUsedFields(overridingMethodBindingKey, processedMethods));
								}
							}
						}
					}
				}
			}
		}
		return usedFields;
	}
	
	public LinkedHashSet<String> getRecursivelyThrownExceptionTypes(String methodBindingKey,
			Set<String> processedMethods) {
		LinkedHashSet<String> thrownExceptionTypes = new LinkedHashSet<String>();
		if(thrownExceptionTypeMap.containsKey(methodBindingKey))
			thrownExceptionTypes.addAll(thrownExceptionTypeMap.get(methodBindingKey));
		processedMethods.add(methodBindingKey);
		LinkedHashSet<String> invokedMethods = methodInvocationMap.get(methodBindingKey);
		if(invokedMethods != null) {
			for(String invokedMethodBindingKey : invokedMethods) {
				if(!processedMethods.contains(invokedMethodBindingKey)) {
					if(!abstractMethodSet.contains(invokedMethodBindingKey) && !overridingMethodMap.containsKey(invokedMethodBindingKey)) {
						if(nativeMethodSet.contains(invokedMethodBindingKey)) {
							//method is native
						}
						else {
							thrownExceptionTypes.addAll(getRecursivelyThrownExceptionTypes(invokedMethodBindingKey, processedMethods));
						}
					}
					else {
						LinkedHashSet<String> overridingMethods = overridingMethodMap.get(invokedMethodBindingKey);
						processedMethods.add(invokedMethodBindingKey);
						if(overridingMethods != null) {
							for(String overridingMethodBindingKey : overridingMethods) {
								if(nativeMethodSet.contains(overridingMethodBindingKey)) {
									//method is native
								}
								else {
									thrownExceptionTypes.addAll(getRecursivelyThrownExceptionTypes(overridingMethodBindingKey, processedMethods));
								}
							}
						}
					}
				}
			}
		}
		return thrownExceptionTypes;
	}
	
	public LinkedHashSet<String> getThrownExceptionTypes(String methodBindingKey) {
		LinkedHashSet<String> thrownExceptionTypes = new LinkedHashSet<String>();
		if(thrownExceptionTypeMap.containsKey(methodBindingKey))
			thrownExceptionTypes.addAll(thrownExceptionTypeMap.get(methodBindingKey));
		return thrownExceptionTypes;
	}
	
	public LinkedHashSet<AbstractVariable> getRecursivelyDefinedFieldsThroughReference(String methodBindingKey,
			AbstractVariable fieldReference, Set<String> processedMethods) {
		LinkedHashSet<AbstractVariable> definedFields = new LinkedHashSet<AbstractVariable>();
		processedMethods.add(methodBindingKey);
		HashMap<PlainVariable, LinkedHashSet<String>> invokedMethodsThroughReference = methodInvocationThroughReferenceMap.get(methodBindingKey);
		if(invokedMethodsThroughReference != null) {
			PlainVariable reference = null;
			if(fieldReference instanceof PlainVariable) {
				reference = (PlainVariable)fieldReference;
			}
			else if(fieldReference instanceof CompositeVariable) {
				CompositeVariable composite = (CompositeVariable)fieldReference;
				reference = composite.getFinalVariable();
			}
			LinkedHashSet<String> invokedMethods = invokedMethodsThroughReference.get(reference);
			if(invokedMethods != null) {
				for(String invokedMethodBindingKey : invokedMethods) {
					if(!processedMethods.contains(invokedMethodBindingKey)) {
						if(nativeMethodSet.contains(invokedMethodBindingKey)) {
							//method is native
						}
						else {
							LinkedHashSet<PlainVariable> definedFieldsInInvokedMethod = getRecursivelyDefinedFields(invokedMethodBindingKey, new LinkedHashSet<String>());
							if(definedFieldsInInvokedMethod != null) {
								for(PlainVariable rightSide : definedFieldsInInvokedMethod) {
									AbstractVariable definedField = composeVariable(fieldReference, rightSide);
									definedFields.add(definedField);
								}
							}
							LinkedHashSet<PlainVariable> usedFieldsInInvokedMethod = getRecursivelyUsedFields(invokedMethodBindingKey, new LinkedHashSet<String>());
							if(usedFieldsInInvokedMethod != null) {
								for(PlainVariable rightSide : usedFieldsInInvokedMethod) {
									AbstractVariable usedField = composeVariable(fieldReference, rightSide);
									definedFields.addAll(getRecursivelyDefinedFieldsThroughReference(invokedMethodBindingKey, usedField, processedMethods));
								}
							}
						}
					}
				}
			}
		}
		return definedFields;
	}
	
	public LinkedHashSet<AbstractVariable> getRecursivelyUsedFieldsThroughReference(String methodBindingKey,
			AbstractVariable fieldReference, Set<String> processedMethods) {
		LinkedHashSet<AbstractVariable> usedFields = new LinkedHashSet<AbstractVariable>();
		processedMethods.add(methodBindingKey);
		HashMap<PlainVariable, LinkedHashSet<String>> invokedMethodsThroughReference = methodInvocationThroughReferenceMap.get(methodBindingKey);
		if(invokedMethodsThroughReference != null) {
			PlainVariable reference = null;
			if(fieldReference instanceof PlainVariable) {
				reference = (PlainVariable)fieldReference;
			}
			else if(fieldReference instanceof CompositeVariable) {
				CompositeVariable composite = (CompositeVariable)fieldReference;
				reference = composite.getFinalVariable();
			}
			LinkedHashSet<String> invokedMethods = invokedMethodsThroughReference.get(reference);
			if(invokedMethods != null) {
				for(String invokedMethod : invokedMethods) {
					if(!processedMethods.contains(invokedMethod)) {
						if(nativeMethodSet.contains(invokedMethod)) {
							//method is native
						}
						else {
							LinkedHashSet<PlainVariable> usedFieldsInInvokedMethod = getRecursivelyUsedFields(invokedMethod, new LinkedHashSet<String>());
							if(usedFieldsInInvokedMethod != null) {
								for(PlainVariable rightSide : usedFieldsInInvokedMethod) {
									AbstractVariable usedField = composeVariable(fieldReference, rightSide);
									usedFields.add(usedField);
									usedFields.addAll(getRecursivelyUsedFieldsThroughReference(invokedMethod, usedField, processedMethods));
								}
							}
						}
					}
				}
			}
		}
		return usedFields;
	}
	
	private AbstractVariable composeVariable(AbstractVariable leftSide, AbstractVariable rightSide) {
		if(leftSide instanceof CompositeVariable) {
			CompositeVariable leftSideCompositeVariable = (CompositeVariable)leftSide;
			PlainVariable finalVariable = leftSideCompositeVariable.getFinalVariable();
			CompositeVariable newRightSide = new CompositeVariable(finalVariable, rightSide);
			AbstractVariable newLeftSide = leftSideCompositeVariable.getLeftPart();
			return composeVariable(newLeftSide, newRightSide);
		}
		else {
			return new CompositeVariable(leftSide, rightSide);
		}
	}

	public void addUsedFieldForMethodArgument(AbstractVariable field, MethodDeclaration mo, int argPosition) {
		String methodBindingKey = mo.resolveBinding().getKey();
		if(usedFieldsForMethodArgumentsMap.containsKey(methodBindingKey)) {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = usedFieldsForMethodArgumentsMap.get(methodBindingKey);
			if(argumentMap.containsKey(argPosition)) {
				LinkedHashSet<AbstractVariable> fieldSet = argumentMap.get(argPosition);
				fieldSet.add(field);
			}
			else {
				LinkedHashSet<AbstractVariable> fieldSet = new LinkedHashSet<AbstractVariable>();
				fieldSet.add(field);
				argumentMap.put(argPosition, fieldSet);
			}
		}
		else {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = new HashMap<Integer, LinkedHashSet<AbstractVariable>>();
			LinkedHashSet<AbstractVariable> fieldSet = new LinkedHashSet<AbstractVariable>();
			fieldSet.add(field);
			argumentMap.put(argPosition, fieldSet);
			usedFieldsForMethodArgumentsMap.put(methodBindingKey, argumentMap);
		}
	}

	public void setEmptyUsedFieldsForMethodArgument(MethodDeclaration mo, int argPosition) {
		String methodBindingKey = mo.resolveBinding().getKey();
		if(usedFieldsForMethodArgumentsMap.containsKey(methodBindingKey)) {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = usedFieldsForMethodArgumentsMap.get(methodBindingKey);
			argumentMap.put(argPosition, new LinkedHashSet<AbstractVariable>());
		}
		else {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = new HashMap<Integer, LinkedHashSet<AbstractVariable>>();
			LinkedHashSet<AbstractVariable> fieldSet = new LinkedHashSet<AbstractVariable>();
			argumentMap.put(argPosition, fieldSet);
			usedFieldsForMethodArgumentsMap.put(methodBindingKey, argumentMap);
		}
	}

	public void addDefinedFieldForMethodArgument(AbstractVariable field, MethodDeclaration mo, int argPosition) {
		String methodBindingKey = mo.resolveBinding().getKey();
		if(definedFieldsForMethodArgumentsMap.containsKey(methodBindingKey)) {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = definedFieldsForMethodArgumentsMap.get(methodBindingKey);
			if(argumentMap.containsKey(argPosition)) {
				LinkedHashSet<AbstractVariable> fieldSet = argumentMap.get(argPosition);
				fieldSet.add(field);
			}
			else {
				LinkedHashSet<AbstractVariable> fieldSet = new LinkedHashSet<AbstractVariable>();
				fieldSet.add(field);
				argumentMap.put(argPosition, fieldSet);
			}
		}
		else {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = new HashMap<Integer, LinkedHashSet<AbstractVariable>>();
			LinkedHashSet<AbstractVariable> fieldSet = new LinkedHashSet<AbstractVariable>();
			fieldSet.add(field);
			argumentMap.put(argPosition, fieldSet);
			definedFieldsForMethodArgumentsMap.put(methodBindingKey, argumentMap);
		}
	}

	public void setEmptyDefinedFieldsForMethodArgument(MethodDeclaration mo, int argPosition) {
		String methodBindingKey = mo.resolveBinding().getKey();
		if(definedFieldsForMethodArgumentsMap.containsKey(methodBindingKey)) {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = definedFieldsForMethodArgumentsMap.get(methodBindingKey);
			argumentMap.put(argPosition, new LinkedHashSet<AbstractVariable>());
		}
		else {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = new HashMap<Integer, LinkedHashSet<AbstractVariable>>();
			LinkedHashSet<AbstractVariable> fieldSet = new LinkedHashSet<AbstractVariable>();
			argumentMap.put(argPosition, fieldSet);
			definedFieldsForMethodArgumentsMap.put(methodBindingKey, argumentMap);
		}
	}

	public boolean containsMethodArgument(String methodBindingKey, int argPosition) {
		//String methodBindingKey = mo.resolveBinding().getKey();
		if(usedFieldsForMethodArgumentsMap.containsKey(methodBindingKey)) {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = usedFieldsForMethodArgumentsMap.get(methodBindingKey);
			if(argumentMap.containsKey(argPosition))
				return true;
		}
		if(definedFieldsForMethodArgumentsMap.containsKey(methodBindingKey)) {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = definedFieldsForMethodArgumentsMap.get(methodBindingKey);
			if(argumentMap.containsKey(argPosition))
				return true;
		}
		return false;
	}

	public Set<AbstractVariable> getUsedFieldsForMethodArgument(String methodBindingKey, int argPosition) {
		//String methodBindingKey = mo.resolveBinding().getKey();
		if(usedFieldsForMethodArgumentsMap.containsKey(methodBindingKey)) {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = usedFieldsForMethodArgumentsMap.get(methodBindingKey);
			if(argumentMap.containsKey(argPosition))
				return argumentMap.get(argPosition);
		}
		return new LinkedHashSet<AbstractVariable>();
	}

	public Set<AbstractVariable> getDefinedFieldsForMethodArgument(String methodBindingKey, int argPosition) {
		//String methodBindingKey = mo.resolveBinding().getKey();
		if(definedFieldsForMethodArgumentsMap.containsKey(methodBindingKey)) {
			HashMap<Integer, LinkedHashSet<AbstractVariable>> argumentMap = definedFieldsForMethodArgumentsMap.get(methodBindingKey);
			if(argumentMap.containsKey(argPosition))
				return argumentMap.get(argPosition);
		}
		return new LinkedHashSet<AbstractVariable>();
	}
}
