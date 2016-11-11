package gr.uom.java.ast.visualization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ITypeBinding;

import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CompositeVariable;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;

public class FeatureEnvyVisualizationData implements VisualizationData {

	private Map<MethodInvocationObject, Integer> sourceMethodInvocationMap;
	private Map<MethodInvocationObject, Integer> targetMethodInvocationMap;
	private Map<FieldInstructionObject, Integer> sourceFieldReadMap;
	private Map<FieldInstructionObject, Integer> sourceFieldWriteMap;
	private Map<FieldInstructionObject, Integer> targetFieldReadMap;
	private Map<FieldInstructionObject, Integer> targetFieldWriteMap;
	private ClassObject sourceClass;
	private MethodObject methodToBeMoved;
	private ClassObject targetClass;
	
	public FeatureEnvyVisualizationData(ClassObject sourceClass, MethodObject methodToBeMoved, ClassObject targetClass) {
		this.sourceClass = sourceClass;
		this.methodToBeMoved = methodToBeMoved;
		this.targetClass = targetClass;
		this.sourceMethodInvocationMap = new LinkedHashMap<MethodInvocationObject, Integer>();
		List<MethodInvocationObject> sourceMethodInvocations = new ArrayList<MethodInvocationObject>(methodToBeMoved.getNonDistinctInvokedMethodsThroughThisReference());
		
		this.targetMethodInvocationMap = new LinkedHashMap<MethodInvocationObject, Integer>();
		List<FieldInstructionObject> fieldInstructions = new ArrayList<FieldInstructionObject>(methodToBeMoved.getFieldInstructions());
		List<LocalVariableInstructionObject> localVariableInstructions = new ArrayList<LocalVariableInstructionObject>(methodToBeMoved.getLocalVariableInstructions());
		Map<AbstractVariable, ArrayList<MethodInvocationObject>> externalMethodInvocationsThroughFieldsMap = new LinkedHashMap<AbstractVariable, ArrayList<MethodInvocationObject>>();
		Map<AbstractVariable, ArrayList<MethodInvocationObject>> copyFromNonDistinctInvokedMethodsThroughFields = methodToBeMoved.getNonDistinctInvokedMethodsThroughFields();
		for(AbstractVariable key : copyFromNonDistinctInvokedMethodsThroughFields.keySet()) {
			ArrayList<MethodInvocationObject> value = new ArrayList<MethodInvocationObject>(copyFromNonDistinctInvokedMethodsThroughFields.get(key));
			externalMethodInvocationsThroughFieldsMap.put(key, value);
		}
		Map<AbstractVariable, ArrayList<MethodInvocationObject>> externalMethodInvocationsThroughParametersMap = new LinkedHashMap<AbstractVariable, ArrayList<MethodInvocationObject>>();
		Map<AbstractVariable, ArrayList<MethodInvocationObject>> copyFromNonDistinctInvokedMethodsThroughParameters = methodToBeMoved.getNonDistinctInvokedMethodsThroughParameters();
		for(AbstractVariable key : copyFromNonDistinctInvokedMethodsThroughParameters.keySet()) {
			ArrayList<MethodInvocationObject> value = new ArrayList<MethodInvocationObject>(copyFromNonDistinctInvokedMethodsThroughParameters.get(key));
			externalMethodInvocationsThroughParametersMap.put(key, value);
		}
		
		this.sourceFieldReadMap = new LinkedHashMap<FieldInstructionObject, Integer>();
		List<PlainVariable> usedFieldsThroughThisReference = new ArrayList<PlainVariable>(methodToBeMoved.getNonDistinctUsedFieldsThroughThisReference());
		
		this.sourceFieldWriteMap = new LinkedHashMap<FieldInstructionObject, Integer>();
		List<PlainVariable> definedFieldsThroughThisReference = new ArrayList<PlainVariable>(methodToBeMoved.getNonDistinctDefinedFieldsThroughThisReference());
		
		this.targetFieldReadMap = new LinkedHashMap<FieldInstructionObject, Integer>();
		List<AbstractVariable> usedFieldsThroughFields = new ArrayList<AbstractVariable>(methodToBeMoved.getNonDistinctUsedFieldsThroughFields());
		List<AbstractVariable> usedFieldsThroughParameters = new ArrayList<AbstractVariable>(methodToBeMoved.getNonDistinctUsedFieldsThroughParameters());
		
		this.targetFieldWriteMap = new LinkedHashMap<FieldInstructionObject, Integer>();
		List<AbstractVariable> definedFieldsThroughFields = new ArrayList<AbstractVariable>(methodToBeMoved.getNonDistinctDefinedFieldsThroughFields());
		List<AbstractVariable> definedFieldsThroughParameters = new ArrayList<AbstractVariable>(methodToBeMoved.getNonDistinctDefinedFieldsThroughParameters());
		
		for(MethodInvocationObject methodInvocation : sourceMethodInvocations) {
			boolean delegatesToTarget = false;
			MethodObject delegateMethod = sourceClass.getMethod(methodInvocation);
			if(delegateMethod != null) {
				MethodInvocationObject delegateMethodInvocation = delegateMethod.isDelegate();
				if(delegateMethodInvocation != null && delegateMethodInvocation.getOriginClassName().equals(targetClass.getName())) {
					delegatesToTarget = true;
					//include delegate method in the analysis
					fieldInstructions.addAll(new ArrayList<FieldInstructionObject>(delegateMethod.getFieldInstructions()));
					localVariableInstructions.addAll(new ArrayList<LocalVariableInstructionObject>(delegateMethod.getLocalVariableInstructions()));
					Map<AbstractVariable, ArrayList<MethodInvocationObject>> externalMethodInvocationsThroughFieldsMapDelegate = new LinkedHashMap<AbstractVariable, ArrayList<MethodInvocationObject>>();
					Map<AbstractVariable, ArrayList<MethodInvocationObject>> copyFromNonDistinctInvokedMethodsThroughFieldsDelegate = delegateMethod.getNonDistinctInvokedMethodsThroughFields();
					for(AbstractVariable key : copyFromNonDistinctInvokedMethodsThroughFieldsDelegate.keySet()) {
						ArrayList<MethodInvocationObject> value = new ArrayList<MethodInvocationObject>(copyFromNonDistinctInvokedMethodsThroughFieldsDelegate.get(key));
						externalMethodInvocationsThroughFieldsMapDelegate.put(key, value);
					}
					for(AbstractVariable variable : externalMethodInvocationsThroughFieldsMapDelegate.keySet()) {
						if(externalMethodInvocationsThroughFieldsMap.containsKey(variable)) {
							externalMethodInvocationsThroughFieldsMap.get(variable).addAll(externalMethodInvocationsThroughFieldsMapDelegate.get(variable));
						}
						else {
							externalMethodInvocationsThroughFieldsMap.put(variable, externalMethodInvocationsThroughFieldsMapDelegate.get(variable));
						}
					}
					Map<AbstractVariable, ArrayList<MethodInvocationObject>> externalMethodInvocationsThroughParametersMapDelegate = new LinkedHashMap<AbstractVariable, ArrayList<MethodInvocationObject>>();
					Map<AbstractVariable, ArrayList<MethodInvocationObject>> copyFromNonDistinctInvokedMethodsThroughParametersDelegate = delegateMethod.getNonDistinctInvokedMethodsThroughParameters();
					for(AbstractVariable key : copyFromNonDistinctInvokedMethodsThroughParametersDelegate.keySet()) {
						ArrayList<MethodInvocationObject> value = new ArrayList<MethodInvocationObject>(copyFromNonDistinctInvokedMethodsThroughParametersDelegate.get(key));
						externalMethodInvocationsThroughParametersMapDelegate.put(key, value);
					}
					for(AbstractVariable variable : externalMethodInvocationsThroughParametersMapDelegate.keySet()) {
						if(externalMethodInvocationsThroughParametersMap.containsKey(variable)) {
							externalMethodInvocationsThroughParametersMap.get(variable).addAll(externalMethodInvocationsThroughParametersMapDelegate.get(variable));
						}
						else {
							externalMethodInvocationsThroughParametersMap.put(variable, externalMethodInvocationsThroughParametersMapDelegate.get(variable));
						}
					}
					usedFieldsThroughThisReference.addAll(new ArrayList<PlainVariable>(delegateMethod.getNonDistinctUsedFieldsThroughThisReference()));
					definedFieldsThroughThisReference.addAll(new ArrayList<PlainVariable>(delegateMethod.getNonDistinctDefinedFieldsThroughThisReference()));
					usedFieldsThroughFields.addAll(new ArrayList<AbstractVariable>(delegateMethod.getNonDistinctUsedFieldsThroughFields()));
					usedFieldsThroughParameters.addAll(new ArrayList<AbstractVariable>(delegateMethod.getNonDistinctUsedFieldsThroughParameters()));
					definedFieldsThroughFields.addAll(new ArrayList<AbstractVariable>(delegateMethod.getNonDistinctDefinedFieldsThroughFields()));
					definedFieldsThroughParameters.addAll(new ArrayList<AbstractVariable>(delegateMethod.getNonDistinctDefinedFieldsThroughParameters()));
				}
			}
			if(!delegatesToTarget) {
				if(sourceMethodInvocationMap.containsKey(methodInvocation)) {
					sourceMethodInvocationMap.put(methodInvocation, sourceMethodInvocationMap.get(methodInvocation) + 1);
				}
				else {
					sourceMethodInvocationMap.put(methodInvocation, 1);
				}
			}
		}
		
		processExternalMethodInvocations(externalMethodInvocationsThroughFieldsMap, fieldInstructions, localVariableInstructions, targetClass);
		processExternalMethodInvocations(externalMethodInvocationsThroughParametersMap, fieldInstructions, localVariableInstructions, targetClass);
		
		for(PlainVariable variable : usedFieldsThroughThisReference) {
			FieldInstructionObject fieldInstruction = findFieldInstruction(variable, fieldInstructions);
			if(fieldInstruction.getOwnerClass().equals(targetClass.getName())) {
				//the used field in inherited from a superclass which is the target
				if(targetFieldReadMap.containsKey(fieldInstruction)) {
					targetFieldReadMap.put(fieldInstruction, targetFieldReadMap.get(fieldInstruction) + 1);
				}
				else {
					targetFieldReadMap.put(fieldInstruction, 1);
				}
			}
			else {
				if(sourceFieldReadMap.containsKey(fieldInstruction)) {
					sourceFieldReadMap.put(fieldInstruction, sourceFieldReadMap.get(fieldInstruction) + 1);
				}
				else {
					sourceFieldReadMap.put(fieldInstruction, 1);
				}
			}
		}
		
		for(PlainVariable variable : definedFieldsThroughThisReference) {
			FieldInstructionObject fieldInstruction = findFieldInstruction(variable, fieldInstructions);
			if(fieldInstruction.getOwnerClass().equals(targetClass.getName())) {
				//the defined field in inherited from a superclass which is the target
				if(targetFieldWriteMap.containsKey(fieldInstruction)) {
					targetFieldWriteMap.put(fieldInstruction, targetFieldWriteMap.get(fieldInstruction) + 1);
				}
				else {
					targetFieldWriteMap.put(fieldInstruction, 1);
				}
			}
			else {
				if(sourceFieldWriteMap.containsKey(fieldInstruction)) {
					sourceFieldWriteMap.put(fieldInstruction, sourceFieldWriteMap.get(fieldInstruction) + 1);
				}
				else {
					sourceFieldWriteMap.put(fieldInstruction, 1);
				}
			}
		}
		
		handleUsedFields(usedFieldsThroughFields, fieldInstructions, localVariableInstructions, targetClass);
		handleUsedFields(usedFieldsThroughParameters, fieldInstructions, localVariableInstructions, targetClass);
		
		handleDefinedFields(definedFieldsThroughFields, fieldInstructions, localVariableInstructions, targetClass);
		handleDefinedFields(definedFieldsThroughParameters, fieldInstructions, localVariableInstructions, targetClass);
	}

	private void handleUsedFields(List<AbstractVariable> usedFields, List<FieldInstructionObject> fieldInstructions,
			List<LocalVariableInstructionObject> localVariableInstructions, ClassObject targetClass) {
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
			ITypeBinding variableTypeBinding = null;
			if(variable.isField()) {
				FieldInstructionObject fieldInstruction = findFieldInstruction(variable, fieldInstructions);
				if(fieldInstruction != null)
					variableTypeBinding = fieldInstruction.getSimpleName().resolveTypeBinding();
			}
			else if(variable.isParameter()) {
				LocalVariableInstructionObject localVariableInstruction = findLocalVariableInstruction(variable, localVariableInstructions);
				if(localVariableInstruction != null)
					variableTypeBinding = localVariableInstruction.getSimpleName().resolveTypeBinding();
			}
			ITypeBinding targetClassBinding = targetClass.getAbstractTypeDeclaration().resolveBinding();
			if(variable.getVariableType().equals(targetClass.getName()) ||
					(variableTypeBinding != null && targetClassBinding.isEqualTo(variableTypeBinding.getSuperclass()))) {
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
			List<LocalVariableInstructionObject> localVariableInstructions, ClassObject targetClass) {
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
			ITypeBinding variableTypeBinding = null;
			if(variable.isField()) {
				FieldInstructionObject fieldInstruction = findFieldInstruction(variable, fieldInstructions);
				if(fieldInstruction != null)
					variableTypeBinding = fieldInstruction.getSimpleName().resolveTypeBinding();
			}
			else if(variable.isParameter()) {
				LocalVariableInstructionObject localVariableInstruction = findLocalVariableInstruction(variable, localVariableInstructions);
				if(localVariableInstruction != null)
					variableTypeBinding = localVariableInstruction.getSimpleName().resolveTypeBinding();
			}
			ITypeBinding targetClassBinding = targetClass.getAbstractTypeDeclaration().resolveBinding();
			if(variable.getVariableType().equals(targetClass.getName()) ||
					(variableTypeBinding != null && targetClassBinding.isEqualTo(variableTypeBinding.getSuperclass()))) {
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

	private LocalVariableInstructionObject findLocalVariableInstruction(PlainVariable variable, List<LocalVariableInstructionObject> localVariableInstructions) {
		for(LocalVariableInstructionObject localVariableInstruction : localVariableInstructions) {
			if(localVariableInstruction.getSimpleName().resolveBinding().getKey().equals(variable.getVariableBindingKey()))
				return localVariableInstruction;
		}
		return null;
	}

	private void processExternalMethodInvocations(Map<AbstractVariable, ArrayList<MethodInvocationObject>> externalMethodInvocationMap,
			List<FieldInstructionObject> fieldInstructions, List<LocalVariableInstructionObject> localVariableInstructions, ClassObject targetClass) {
		for(AbstractVariable abstractVariable : externalMethodInvocationMap.keySet()) {
			PlainVariable variable = null;
			if(abstractVariable instanceof CompositeVariable) {
				variable = ((CompositeVariable)abstractVariable).getFinalVariable();
			}
			else {
				variable = (PlainVariable)abstractVariable;
			}
			ITypeBinding variableTypeBinding = null;
			if(variable.isField()) {
				FieldInstructionObject fieldInstruction = findFieldInstruction(variable, fieldInstructions);
				if(fieldInstruction != null)
					variableTypeBinding = fieldInstruction.getSimpleName().resolveTypeBinding();
			}
			else if(variable.isParameter()) {
				LocalVariableInstructionObject localVariableInstruction = findLocalVariableInstruction(variable, localVariableInstructions);
				if(localVariableInstruction != null)
					variableTypeBinding = localVariableInstruction.getSimpleName().resolveTypeBinding();
			}
			ITypeBinding targetClassBinding = targetClass.getAbstractTypeDeclaration().resolveBinding();
			if(variable.getVariableType().equals(targetClass.getName()) ||
					(variableTypeBinding != null && targetClassBinding.isEqualTo(variableTypeBinding.getSuperclass()))) {
				List<MethodInvocationObject> externalMethodInvocations = externalMethodInvocationMap.get(abstractVariable);
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

	public ClassObject getSourceClass() {
		return sourceClass;
	}

	public MethodObject getMethodToBeMoved() {
		return methodToBeMoved;
	}

	public ClassObject getTargetClass() {
		return targetClass;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("--SOURCE FIELD READS--").append("\n");
		Map<FieldInstructionObject, Integer> sourceFieldReadMap = getSourceFieldReadMap();
		for(FieldInstructionObject fieldInstruction : sourceFieldReadMap.keySet()) {
			sb.append(fieldInstruction).append("\t").append(sourceFieldReadMap.get(fieldInstruction)).append("\n");
		}
		sb.append("--SOURCE FIELD WRITES--").append("\n");
		Map<FieldInstructionObject, Integer> sourceFieldWriteMap = getSourceFieldWriteMap();
		for(FieldInstructionObject fieldInstruction : sourceFieldWriteMap.keySet()) {
			sb.append(fieldInstruction).append("\t").append(sourceFieldWriteMap.get(fieldInstruction)).append("\n");
		}
		sb.append("--SOURCE METHOD CALLS--").append("\n");
		Map<MethodInvocationObject, Integer> sourceMethodInvocationMap = getSourceMethodInvocationMap();
		for(MethodInvocationObject methodInvocation : sourceMethodInvocationMap.keySet()) {
			sb.append(methodInvocation).append("\t").append(sourceMethodInvocationMap.get(methodInvocation)).append("\n");
		}
		sb.append("\n");
		sb.append("--TARGET FIELD READS--").append("\n");
		Map<FieldInstructionObject, Integer> targetFieldReadMap = getTargetFieldReadMap();
		for(FieldInstructionObject fieldInstruction : targetFieldReadMap.keySet()) {
			sb.append(fieldInstruction).append("\t").append(targetFieldReadMap.get(fieldInstruction)).append("\n");
		}
		sb.append("--TARGET FIELD WRITES--").append("\n");
		Map<FieldInstructionObject, Integer> targetFieldWriteMap = getTargetFieldWriteMap();
		for(FieldInstructionObject fieldInstruction : targetFieldWriteMap.keySet()) {
			sb.append(fieldInstruction).append("\t").append(targetFieldWriteMap.get(fieldInstruction)).append("\n");
		}
		sb.append("--TARGET METHOD CALLS--").append("\n");
		Map<MethodInvocationObject, Integer> targetMethodInvocationMap = getTargetMethodInvocationMap();
		for(MethodInvocationObject methodInvocation : targetMethodInvocationMap.keySet()) {
			sb.append(methodInvocation).append("\t").append(targetMethodInvocationMap.get(methodInvocation)).append("\n");
		}
		return sb.toString();
	}

	public int getDistinctSourceDependencies() {
		Set<FieldInstructionObject> fields = new LinkedHashSet<FieldInstructionObject>();
		fields.addAll(sourceFieldReadMap.keySet());
		fields.addAll(sourceFieldWriteMap.keySet());
		return fields.size() + sourceMethodInvocationMap.size();
	}

	public int getDistinctTargetDependencies() {
		Set<FieldInstructionObject> fields = new LinkedHashSet<FieldInstructionObject>();
		fields.addAll(targetFieldReadMap.keySet());
		fields.addAll(targetFieldWriteMap.keySet());
		return fields.size() + targetMethodInvocationMap.size();
	}
}
