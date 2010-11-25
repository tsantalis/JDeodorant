package gr.uom.java.ast.metrics;

import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.SystemObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class ConnectivityMetric {
	private Map<String, Double> classCohesionMap;
	
	public ConnectivityMetric(SystemObject system) {
		this.classCohesionMap = new LinkedHashMap<String, Double>();
		ListIterator<ClassObject> classIterator = system.getClassListIterator();
		while(classIterator.hasNext()) {
			ClassObject classObject = classIterator.next();
			classCohesion(classObject);
		}
	}

	private void classCohesion(ClassObject classObject) {
		List<FieldObject> fields = new ArrayList<FieldObject>();
		ListIterator<FieldObject> fieldIterator = classObject.getFieldIterator();
		while(fieldIterator.hasNext()) {
			FieldObject field = fieldIterator.next();
			if(!field.isStatic())
				fields.add(field);
		}
		
		List<MethodObject> methods = new ArrayList<MethodObject>();
		List<MethodObject> accessors = new ArrayList<MethodObject>();
		ListIterator<MethodObject> methodIterator = classObject.getMethodIterator();
		while(methodIterator.hasNext()) {
			MethodObject method = methodIterator.next();
			if(!method.isStatic() && method.getMethodBody() != null) {
				FieldInstructionObject fieldInstruction = method.isGetter();
				if(fieldInstruction != null) {
					accessors.add(method);
				}
				if(fieldInstruction == null) {
					fieldInstruction = method.isSetter();
					if(fieldInstruction != null) {
						accessors.add(method);
					}
				}
				if(fieldInstruction == null) {
					fieldInstruction = method.isCollectionAdder();
					if(fieldInstruction != null) {
						accessors.add(method);
					}
				}
				if(fieldInstruction == null)
					methods.add(method);
			}
		}
		int cohesivePairs = 0;
		for(int i=0; i<methods.size(); i++) {
			MethodObject methodI = methods.get(i);
			for(int j=i+1; j<methods.size(); j++) {
				MethodObject methodJ = methods.get(j);
				if(useCommonFieldOrOneInvokesTheOther(methodI, methodJ, classObject.getName(), accessors))
					cohesivePairs++;
			}
		}
		
		int m = methods.size();
		if(m > 1) {
			double connectivity = 2.0 * (double)cohesivePairs/(double)(m * (m-1));
			classCohesionMap.put(classObject.getName(), connectivity);
		}
		else
			classCohesionMap.put(classObject.getName(), null);
	}
	
	private boolean useCommonFieldOrOneInvokesTheOther(MethodObject methodI, MethodObject methodJ, String className, List<MethodObject> accessors) {
		List<FieldInstructionObject> fieldInstructionsI = methodI.getFieldInstructions();
		List<FieldInstructionObject> fieldInstructionsJ = methodJ.getFieldInstructions();
		for(FieldInstructionObject instruction : fieldInstructionsI) {
			//methods access the same field of the class that they belong to
			if(instruction.getOwnerClass().equals(className) && fieldInstructionsJ.contains(instruction))
				return true;
		}
		List<MethodInvocationObject> methodInvocationsI = methodI.getMethodInvocations();
		List<MethodInvocationObject> methodInvocationsJ = methodJ.getMethodInvocations();
		for(MethodInvocationObject invocation : methodInvocationsI) {
			//methodI invokes methodJ
			if(methodJ.equals(invocation))
				return true;
			for(MethodObject accessor : accessors) {
				//methodI invokes an accessor method
				if(accessor.equals(invocation)) {
					FieldInstructionObject accessorFieldInstruction = accessor.getFieldInstructions().get(0);
					if(accessorFieldInstruction.getOwnerClass().equals(className)) {
						//methods invoke the same accessor method
						if(methodInvocationsJ.contains(invocation))
							return true;
						//methodJ accesses the field of the accessor that methodI invokes
						if(fieldInstructionsJ.contains(accessorFieldInstruction))
							return true;
					}
				}
			}
		}
		for(MethodInvocationObject invocation: methodInvocationsJ) {
			//methodJ invokes methodI
			if(methodI.equals(invocation))
				return true;
			for(MethodObject accessor : accessors) {
				//methodJ invokes an accessor method
				if(accessor.equals(invocation)) {
					FieldInstructionObject accessorFieldInstruction = accessor.getFieldInstructions().get(0);
					if(accessorFieldInstruction.getOwnerClass().equals(className)) {
						//methods invoke the same accessor method
						if(methodInvocationsI.contains(invocation))
							return true;
						//methodI accesses the field of the accessor that methodJ invokes
						if(fieldInstructionsI.contains(accessorFieldInstruction))
							return true;
					}
				}
			}
		}
		
		return false;
	}
	
	public double getSystemAverageConnectivity() {
		Set<String> keySet = classCohesionMap.keySet();
		double sum = 0;
		int notDefined = 0;
		for(String key : keySet) {
			Double value = classCohesionMap.get(key);
			if(value != null)
				sum += value;
			else
				notDefined++;
		}
		if(keySet.size() == notDefined)
			return 0;
		else
			return sum/(double)(keySet.size()-notDefined);
	}
}
