package gr.uom.java.ast.visualization;

import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GodClassVisualizationData implements VisualizationData {
	//the MethodObject keys correspond to the methods suggested to be extracted in a new class
	private Map<MethodObject, Map<MethodInvocationObject, Integer>> internalMethodInvocationMap;
	private Map<MethodObject, Map<MethodInvocationObject, Integer>> externalMethodInvocationMap;
	private Map<MethodObject, Map<FieldInstructionObject, Integer>> internalFieldReadMap;
	private Map<MethodObject, Map<FieldInstructionObject, Integer>> internalFieldWriteMap;
	private Map<MethodObject, Map<FieldInstructionObject, Integer>> externalFieldReadMap;
	private Map<MethodObject, Map<FieldInstructionObject, Integer>> externalFieldWriteMap;
	private Set<MethodObject> extractedMethods;
	private Set<FieldObject> extractedFields;
	private ClassObject sourceClass;
	
	public GodClassVisualizationData(ClassObject sourceClass, Set<MethodObject> extractedMethods, Set<FieldObject> extractedFields) {
		this.sourceClass = sourceClass;
		this.extractedMethods = extractedMethods;
		this.extractedFields = extractedFields;
		this.internalMethodInvocationMap = new LinkedHashMap<MethodObject, Map<MethodInvocationObject, Integer>>();
		this.externalMethodInvocationMap = new LinkedHashMap<MethodObject, Map<MethodInvocationObject, Integer>>();
		this.internalFieldReadMap = new LinkedHashMap<MethodObject, Map<FieldInstructionObject, Integer>>();
		this.internalFieldWriteMap = new LinkedHashMap<MethodObject, Map<FieldInstructionObject, Integer>>();
		this.externalFieldReadMap = new LinkedHashMap<MethodObject, Map<FieldInstructionObject, Integer>>();
		this.externalFieldWriteMap = new LinkedHashMap<MethodObject, Map<FieldInstructionObject, Integer>>();
		
		for(MethodObject method : extractedMethods) {
			List<MethodInvocationObject> methodInvocations = method.getNonDistinctInvokedMethodsThroughThisReference();
			for(MethodInvocationObject invocation : methodInvocations) {
				if(isInvocationToExtractedMethod(invocation, extractedMethods)) {
					insertToMap(method, invocation, internalMethodInvocationMap);
				}
				else {
					insertToMap(method, invocation, externalMethodInvocationMap);
				}
			}
			List<FieldInstructionObject> fieldInstructions = method.getFieldInstructions();
			List<PlainVariable> fieldAccesses = method.getNonDistinctUsedFieldsThroughThisReference();
			for(PlainVariable fieldAccess : fieldAccesses) {
				FieldInstructionObject fieldInstruction = findFieldInstruction(fieldAccess, fieldInstructions);
				//exclude accesses to static fields
				if(!fieldInstruction.isStatic()) {
					if(isAccessToExtractedField(fieldInstruction, extractedFields)) {
						insertToMap(method, fieldInstruction, internalFieldReadMap, 1);
					}
					else {
						insertToMap(method, fieldInstruction, externalFieldReadMap, 1);
					}
				}
			}
			List<PlainVariable> fieldWrites = method.getNonDistinctDefinedFieldsThroughThisReference();
			for(PlainVariable fieldWrite : fieldWrites) {
				FieldInstructionObject fieldInstruction = findFieldInstruction(fieldWrite, fieldInstructions);
				//exclude accesses to static fields
				if(!fieldInstruction.isStatic()) {
					if(isAccessToExtractedField(fieldInstruction, extractedFields)) {
						insertToMap(method, fieldInstruction, internalFieldWriteMap, 1);
					}
					else {
						insertToMap(method, fieldInstruction, externalFieldWriteMap, 1);
					}
				}
			}
		}
		for(MethodObject method : extractedMethods) {
			//replace getter method calls in Source class with field accesses in Extracted class
			Map<MethodInvocationObject, Integer> methodInvocationMap = externalMethodInvocationMap.get(method);
			if(methodInvocationMap != null) {
				Set<MethodInvocationObject> invocationsToBeRemoved = new LinkedHashSet<MethodInvocationObject>();
				for(MethodInvocationObject invocation : methodInvocationMap.keySet()) {
					int count = methodInvocationMap.get(invocation);
					MethodObject methodDeclaration = sourceClass.getMethod(invocation);
					if(methodDeclaration != null) {
						FieldInstructionObject getterFieldInstruction = methodDeclaration.isGetter();
						if(getterFieldInstruction != null) {
							if(isAccessToExtractedField(getterFieldInstruction, extractedFields)) {
								//remove getter method calls in source class
								invocationsToBeRemoved.add(invocation);
								//add field reads in extracted class
								insertToMap(method, getterFieldInstruction, internalFieldReadMap, count);
							}
						}
						FieldInstructionObject setterFieldInstruction = methodDeclaration.isSetter();
						if(setterFieldInstruction != null) {
							if(isAccessToExtractedField(setterFieldInstruction, extractedFields)) {
								//remove setter method calls in source class
								invocationsToBeRemoved.add(invocation);
								//add field writes in extracted class
								insertToMap(method, setterFieldInstruction, internalFieldWriteMap, count);
							}
						}
					}
				}
				for(MethodInvocationObject invocation : invocationsToBeRemoved) {
					methodInvocationMap.remove(invocation);
				}
				if(methodInvocationMap.isEmpty()){
					externalMethodInvocationMap.remove(method);
				}
			}
		}
	}

	private void insertToMap(MethodObject method, MethodInvocationObject methodInvocation, 
			Map<MethodObject, Map<MethodInvocationObject, Integer>> map) {
		if(map.containsKey(method)) {
			Map<MethodInvocationObject, Integer> invocationMap = map.get(method);
			if(invocationMap.containsKey(methodInvocation)) {
				invocationMap.put(methodInvocation, invocationMap.get(methodInvocation) + 1);
			}
			else {
				invocationMap.put(methodInvocation, 1);
			}
		}
		else {
			Map<MethodInvocationObject, Integer> invocationMap = new LinkedHashMap<MethodInvocationObject, Integer>();
			invocationMap.put(methodInvocation, 1);
			map.put(method, invocationMap);
		}
	}

	private void insertToMap(MethodObject method, FieldInstructionObject fieldInstruction,
			Map<MethodObject, Map<FieldInstructionObject, Integer>> map, int count) {
		if(map.containsKey(method)) {
			Map<FieldInstructionObject, Integer> fieldAccessMap = map.get(method);
			if(fieldAccessMap.containsKey(fieldInstruction)) {
				fieldAccessMap.put(fieldInstruction, fieldAccessMap.get(fieldInstruction) + count);
			}
			else {
				fieldAccessMap.put(fieldInstruction, count);
			}
		}
		else {
			Map<FieldInstructionObject, Integer> fieldAccessMap = new LinkedHashMap<FieldInstructionObject, Integer>();
			fieldAccessMap.put(fieldInstruction, count);
			map.put(method, fieldAccessMap);
		}
	}

	private boolean isInvocationToExtractedMethod(MethodInvocationObject invocation, Set<MethodObject> extractedMethods) {
		for(MethodObject method : extractedMethods) {
			if(method.equals(invocation))
				return true;
		}
		return false;
	}

	private boolean isAccessToExtractedField(FieldInstructionObject instruction, Set<FieldObject> extractedFields) {
		for(FieldObject field : extractedFields) {
			if(field.equals(instruction))
				return true;
		}
		return false;
	}

	private FieldInstructionObject findFieldInstruction(PlainVariable variable, List<FieldInstructionObject> fieldInstructions) {
		for(FieldInstructionObject fieldInstruction : fieldInstructions) {
			if(fieldInstruction.getSimpleName().resolveBinding().getKey().equals(variable.getVariableBindingKey()))
				return fieldInstruction;
		}
		return null;
	}

	public Map<MethodObject, Map<MethodInvocationObject, Integer>> getInternalMethodInvocationMap() {
		return internalMethodInvocationMap;
	}

	public Map<MethodObject, Map<MethodInvocationObject, Integer>> getExternalMethodInvocationMap() {
		return externalMethodInvocationMap;
	}

	public Map<MethodObject, Map<FieldInstructionObject, Integer>> getInternalFieldReadMap() {
		return internalFieldReadMap;
	}

	public Map<MethodObject, Map<FieldInstructionObject, Integer>> getInternalFieldWriteMap() {
		return internalFieldWriteMap;
	}

	public Map<MethodObject, Map<FieldInstructionObject, Integer>> getExternalFieldReadMap() {
		return externalFieldReadMap;
	}

	public Map<MethodObject, Map<FieldInstructionObject, Integer>> getExternalFieldWriteMap() {
		return externalFieldWriteMap;
	}

	public Set<MethodObject> getExtractedMethods() {
		return extractedMethods;
	}

	public Set<FieldObject> getExtractedFields() {
		return extractedFields;
	}

	public ClassObject getSourceClass() {
		return sourceClass;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("--FIELD READS FROM EXTRACTED METHODS TO EXTRACTED FIELDS--").append("\n");
		sb.append(fieldAccessMapToString(getInternalFieldReadMap()));
		sb.append("--FIELD WRITES FROM EXTRACTED METHODS TO EXTRACTED FIELDS--").append("\n");
		sb.append(fieldAccessMapToString(getInternalFieldWriteMap()));
		sb.append("--METHOD CALLS BETWEEN EXTRACTED METHODS--").append("\n");
		sb.append(methodCallMapToString(getInternalMethodInvocationMap()));
		sb.append("\n");
		sb.append("--FIELD READS FROM EXTRACTED METHODS TO SOURCE CLASS FIELDS--").append("\n");
		sb.append(fieldAccessMapToString(getExternalFieldReadMap()));
		sb.append("--FIELD WRITES FROM EXTRACTED METHODS TO SOURCE CLASS FIELDS--").append("\n");
		sb.append(fieldAccessMapToString(getExternalFieldWriteMap()));
		sb.append("--METHOD CALLS FROM EXTRACTED METHODS TO SOURCE CLASS METHODS--").append("\n");
		sb.append(methodCallMapToString(getExternalMethodInvocationMap()));
		
		return sb.toString();
	}

	private String methodCallMapToString(Map<MethodObject, Map<MethodInvocationObject, Integer>> map) {
		StringBuilder sb = new StringBuilder();
		for(MethodObject method : map.keySet()) {
			sb.append(method).append("\n");
			Map<MethodInvocationObject, Integer> invocationMap = map.get(method);
			for(MethodInvocationObject invocation : invocationMap.keySet()) {
				sb.append("\t").append(invocation).append(" : ").append(invocationMap.get(invocation)).append("\n");
			}
		}
		return sb.toString();
	}

	private String fieldAccessMapToString(Map<MethodObject, Map<FieldInstructionObject, Integer>> map) {
		StringBuilder sb = new StringBuilder();
		for(MethodObject method : map.keySet()) {
			sb.append(method).append("\n");
			Map<FieldInstructionObject, Integer> fieldAccessMap = map.get(method);
			for(FieldInstructionObject instruction : fieldAccessMap.keySet()) {
				sb.append("\t").append(instruction).append(" : ").append(fieldAccessMap.get(instruction)).append("\n");
			}
		}
		return sb.toString();
	}

	public int getDistinctTargetDependencies() {
		Set<FieldInstructionObject> fields = new LinkedHashSet<FieldInstructionObject>();
		for(MethodObject key : internalFieldReadMap.keySet()) {
			Map<FieldInstructionObject, Integer> value = internalFieldReadMap.get(key);
			fields.addAll(value.keySet());
		}
		for(MethodObject key : internalFieldWriteMap.keySet()) {
			Map<FieldInstructionObject, Integer> value = internalFieldWriteMap.get(key);
			fields.addAll(value.keySet());
		}
		Set<MethodInvocationObject> methods = new LinkedHashSet<MethodInvocationObject>();
		for(MethodObject key : internalMethodInvocationMap.keySet()) {
			Map<MethodInvocationObject, Integer> value = internalMethodInvocationMap.get(key);
			methods.addAll(value.keySet());
		}
		return fields.size() + methods.size();
	}

	public int getDistinctSourceDependencies() {
		Set<FieldInstructionObject> fields = new LinkedHashSet<FieldInstructionObject>();
		for(MethodObject key : externalFieldReadMap.keySet()) {
			Map<FieldInstructionObject, Integer> value = externalFieldReadMap.get(key);
			fields.addAll(value.keySet());
		}
		for(MethodObject key : externalFieldWriteMap.keySet()) {
			Map<FieldInstructionObject, Integer> value = externalFieldWriteMap.get(key);
			fields.addAll(value.keySet());
		}
		Set<MethodInvocationObject> methods = new LinkedHashSet<MethodInvocationObject>();
		for(MethodObject key : externalMethodInvocationMap.keySet()) {
			Map<MethodInvocationObject, Integer> value = externalMethodInvocationMap.get(key);
			methods.addAll(value.keySet());
		}
		return fields.size() + methods.size();
	}

	public boolean containsNonAccessedFieldInExtractedClass() {
		Set<FieldInstructionObject> fields = new LinkedHashSet<FieldInstructionObject>();
		for(MethodObject key : internalFieldReadMap.keySet()) {
			Map<FieldInstructionObject, Integer> value = internalFieldReadMap.get(key);
			fields.addAll(value.keySet());
		}
		for(MethodObject key : internalFieldWriteMap.keySet()) {
			Map<FieldInstructionObject, Integer> value = internalFieldWriteMap.get(key);
			fields.addAll(value.keySet());
		}
		for(FieldObject field : extractedFields) {
			if(!fields.contains(field.generateFieldInstruction())) {
				return true;
			}
		}
		return false;
	}
}
