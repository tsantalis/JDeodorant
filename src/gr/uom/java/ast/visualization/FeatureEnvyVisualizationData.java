package gr.uom.java.ast.visualization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CompositeVariable;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;

public class FeatureEnvyVisualizationData {

	private Map<MethodInvocationObject, Integer> sourceMethodInvocationMap;
	private Map<MethodInvocationObject, Integer> targetMethodInvocationMap;
	private Map<FieldInstructionObject, Integer> sourceFieldReadMap;
	private Map<FieldInstructionObject, Integer> sourceFieldWriteMap;
	private Map<FieldInstructionObject, Integer> targetFieldReadMap;
	private Map<FieldInstructionObject, Integer> targetFieldWriteMap;
	
	public FeatureEnvyVisualizationData(ClassObject sourceClass, MethodObject methodToBeMoved, ClassObject targetClass) {
		this.sourceMethodInvocationMap = new LinkedHashMap<MethodInvocationObject, Integer>();
		List<MethodInvocationObject> sourceMethodInvocations = methodToBeMoved.getNonDistinctInvokedMethodsThroughThisReference();
		for(MethodInvocationObject methodInvocation : sourceMethodInvocations) {
			if(sourceMethodInvocationMap.containsKey(methodInvocation)) {
				sourceMethodInvocationMap.put(methodInvocation, sourceMethodInvocationMap.get(methodInvocation) + 1);
			}
			else {
				sourceMethodInvocationMap.put(methodInvocation, 1);
			}
		}
		
		this.targetMethodInvocationMap = new LinkedHashMap<MethodInvocationObject, Integer>();
		Map<AbstractVariable, ArrayList<MethodInvocationObject>> externalMethodInvocationsThroughFieldsMap = methodToBeMoved.getNonDistinctInvokedMethodsThroughFields();
		processExternalMethodInvocations(externalMethodInvocationsThroughFieldsMap, targetClass);
		Map<AbstractVariable, ArrayList<MethodInvocationObject>> externalMethodInvocationsThroughParametersMap = methodToBeMoved.getNonDistinctInvokedMethodsThroughParameters();
		processExternalMethodInvocations(externalMethodInvocationsThroughParametersMap, targetClass);
		
		List<FieldInstructionObject> fieldInstructions = methodToBeMoved.getFieldInstructions();
		this.sourceFieldReadMap = new LinkedHashMap<FieldInstructionObject, Integer>();
		List<PlainVariable> usedFieldsThroughThisReference = methodToBeMoved.getNonDistinctUsedFieldsThroughThisReference();
		for(PlainVariable variable : usedFieldsThroughThisReference) {
			FieldInstructionObject fieldInstruction = findFieldInstruction(variable, fieldInstructions);
			if(sourceFieldReadMap.containsKey(fieldInstruction)) {
				sourceFieldReadMap.put(fieldInstruction, sourceFieldReadMap.get(fieldInstruction) + 1);
			}
			else {
				sourceFieldReadMap.put(fieldInstruction, 1);
			}
		}
		
		this.sourceFieldWriteMap = new LinkedHashMap<FieldInstructionObject, Integer>();
		List<PlainVariable> definedFieldsThroughThisReference = methodToBeMoved.getNonDistinctDefinedFieldsThroughThisReference();
		for(PlainVariable variable : definedFieldsThroughThisReference) {
			FieldInstructionObject fieldInstruction = findFieldInstruction(variable, fieldInstructions);
			if(sourceFieldWriteMap.containsKey(fieldInstruction)) {
				sourceFieldWriteMap.put(fieldInstruction, sourceFieldWriteMap.get(fieldInstruction) + 1);
			}
			else {
				sourceFieldWriteMap.put(fieldInstruction, 1);
			}
		}
		
		this.targetFieldReadMap = new LinkedHashMap<FieldInstructionObject, Integer>();
		List<AbstractVariable> usedFieldsThroughFields = methodToBeMoved.getNonDistinctUsedFieldsThroughFields();
		handleUsedFields(usedFieldsThroughFields, fieldInstructions, targetClass);
		List<AbstractVariable> usedFieldsThroughParameters = methodToBeMoved.getNonDistinctUsedFieldsThroughParameters();
		handleUsedFields(usedFieldsThroughParameters, fieldInstructions, targetClass);
		
		this.targetFieldWriteMap = new LinkedHashMap<FieldInstructionObject, Integer>();
		List<AbstractVariable> definedFieldsThroughFields = methodToBeMoved.getNonDistinctDefinedFieldsThroughFields();
		handleDefinedFields(definedFieldsThroughFields, fieldInstructions, targetClass);
		List<AbstractVariable> definedFieldsThroughParameters = methodToBeMoved.getNonDistinctDefinedFieldsThroughParameters();
		handleDefinedFields(definedFieldsThroughParameters, fieldInstructions, targetClass);
	}

	private void handleUsedFields(List<AbstractVariable> usedFields, List<FieldInstructionObject> fieldInstructions,
			ClassObject targetClass) {
		for(AbstractVariable abstractVariable : usedFields) {
			CompositeVariable compositeVariable = (CompositeVariable)abstractVariable;
			AbstractVariable leftPart = compositeVariable.getLeftPart();
			PlainVariable variable = null;
			if(leftPart instanceof CompositeVariable) {
				variable = ((CompositeVariable)leftPart).getFinalVariable();
			}
			else {
				variable = (PlainVariable)leftPart;
			}
			if(variable.getVariableType().equals(targetClass.getName())) {
				FieldInstructionObject fieldInstruction = findFieldInstruction(compositeVariable.getFinalVariable(), fieldInstructions);
				if(targetFieldReadMap.containsKey(fieldInstruction)) {
					targetFieldReadMap.put(fieldInstruction, targetFieldReadMap.get(fieldInstruction) + 1);
				}
				else {
					targetFieldReadMap.put(fieldInstruction, 1);
				}
			}
		}
	}

	private void handleDefinedFields(List<AbstractVariable> definedFields, List<FieldInstructionObject> fieldInstructions,
			ClassObject targetClass) {
		for(AbstractVariable abstractVariable : definedFields) {
			CompositeVariable compositeVariable = (CompositeVariable)abstractVariable;
			AbstractVariable leftPart = compositeVariable.getLeftPart();
			PlainVariable variable = null;
			if(leftPart instanceof CompositeVariable) {
				variable = ((CompositeVariable)leftPart).getFinalVariable();
			}
			else {
				variable = (PlainVariable)leftPart;
			}
			if(variable.getVariableType().equals(targetClass.getName())) {
				FieldInstructionObject fieldInstruction = findFieldInstruction(compositeVariable.getFinalVariable(), fieldInstructions);
				if(targetFieldWriteMap.containsKey(fieldInstruction)) {
					targetFieldWriteMap.put(fieldInstruction, targetFieldWriteMap.get(fieldInstruction) + 1);
				}
				else {
					targetFieldWriteMap.put(fieldInstruction, 1);
				}
			}
		}
	}

	private FieldInstructionObject findFieldInstruction(PlainVariable variable, List<FieldInstructionObject> fieldInstructions) {
		for(FieldInstructionObject fieldInstruction : fieldInstructions) {
			if(fieldInstruction.getSimpleName().resolveBinding().getKey().equals(variable.getVariableBindingKey()))
				return fieldInstruction;
		}
		return null;
	}

	private void processExternalMethodInvocations(Map<AbstractVariable, ArrayList<MethodInvocationObject>> externalMethodInvocationMap,
			ClassObject targetClass) {
		for(AbstractVariable abstractVariable : externalMethodInvocationMap.keySet()) {
			PlainVariable variable = null;
			if(abstractVariable instanceof CompositeVariable) {
				variable = ((CompositeVariable)abstractVariable).getFinalVariable();
			}
			else {
				variable = (PlainVariable)abstractVariable;
			}
			if(variable.getVariableType().equals(targetClass.getName())) {
				List<MethodInvocationObject> externalMethodInvocations = externalMethodInvocationMap.get(variable);
				for(MethodInvocationObject methodInvocation : externalMethodInvocations) {
					if(targetMethodInvocationMap.containsKey(methodInvocation)) {
						targetMethodInvocationMap.put(methodInvocation, targetMethodInvocationMap.get(methodInvocation) + 1);
					}
					else {
						targetMethodInvocationMap.put(methodInvocation, 1);
					}
				}
			}
		}
	}

	public Map<MethodInvocationObject, Integer> getSourceMethodInvocationMap() {
		return sourceMethodInvocationMap;
	}

	public Map<MethodInvocationObject, Integer> getTargetMethodInvocationMap() {
		return targetMethodInvocationMap;
	}

	public Map<FieldInstructionObject, Integer> getSourceFieldReadMap() {
		return sourceFieldReadMap;
	}

	public Map<FieldInstructionObject, Integer> getSourceFieldWriteMap() {
		return sourceFieldWriteMap;
	}

	public Map<FieldInstructionObject, Integer> getTargetFieldReadMap() {
		return targetFieldReadMap;
	}

	public Map<FieldInstructionObject, Integer> getTargetFieldWriteMap() {
		return targetFieldWriteMap;
	}
}
