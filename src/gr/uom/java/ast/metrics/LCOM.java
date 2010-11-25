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

public class LCOM {

	private Map<String, Double> lcom2Map;
	private Map<String, Double> lcom3Map;
	
	public LCOM(SystemObject system) {
		this.lcom2Map = new LinkedHashMap<String, Double>();
		this.lcom3Map = new LinkedHashMap<String, Double>();
		ListIterator<ClassObject> classIterator = system.getClassListIterator();
		while(classIterator.hasNext()) {
			ClassObject classObject = classIterator.next();
			classLCOM(classObject);
		}
	}
	
	private void classLCOM(ClassObject classObject) {
		List<FieldObject> fields = new ArrayList<FieldObject>();
		ListIterator<FieldObject> fieldIterator = classObject.getFieldIterator();
		while(fieldIterator.hasNext()) {
			FieldObject field = fieldIterator.next();
			if(!field.isStatic())
				fields.add(field);
		}
		if(fields.size() == 0) {
			lcom2Map.put(classObject.getName(), null);
			lcom3Map.put(classObject.getName(), null);
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
		if(methods.size() == 0)
			lcom2Map.put(classObject.getName(), null);
		if(methods.size() <= 1)
			lcom3Map.put(classObject.getName(), null);
		
		int sum = 0;
		for(FieldObject field : fields) {
			int mA = 0;
			for(MethodObject method : methods) {
				boolean found = false;
				List<FieldInstructionObject> fieldInstructions = method.getFieldInstructions();
				for(FieldInstructionObject fieldInstruction : fieldInstructions) {
					if(field.equals(fieldInstruction)) {
						found = true;
						break;
					}
				}
				if(found) {
					mA++;
				}
				else {
					List<MethodInvocationObject> methodInvocations = method.getMethodInvocations();
					for(MethodInvocationObject methodInvocation : methodInvocations) {
						if(methodInvocation.getOriginClassName().equals(classObject.getName())) {
							MethodObject methodObject = classObject.getMethod(methodInvocation);
							if(accessors.contains(methodObject)) {
								List<FieldInstructionObject> accessorFieldInstructions = methodObject.getFieldInstructions();
								FieldInstructionObject accessorFieldInstruction = accessorFieldInstructions.get(0);
								if(field.equals(accessorFieldInstruction)) {
									mA++;
									break;
								}
							}
						}
					}
				}
			}
			sum += mA;
		}
		int a = fields.size();
		int m = methods.size();
		if(a > 0 && m > 0) {
			double lcom2 = 1.0 - (double)sum / (double)(m*a);
			lcom2Map.put(classObject.getName(), lcom2);
		}
		if(a > 0 && m > 1) {
			double lcom3 = ((double)m - ((double)sum/(double)a)) / ((double)m - 1.0);
			lcom3Map.put(classObject.getName(), lcom3);
		}
	}
	
	public double getSystemAverageLCOM2() {
		Set<String> keySet = lcom2Map.keySet();
		double sum = 0;
		int notDefined = 0;
		for(String key : keySet) {
			Double value = lcom2Map.get(key);
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
	
	public double getSystemAverageLCOM3() {
		Set<String> keySet = lcom3Map.keySet();
		double sum = 0;
		int notDefined = 0;
		for(String key : keySet) {
			Double value = lcom3Map.get(key);
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
