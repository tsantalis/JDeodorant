package gr.uom.java.ast.metrics;

import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.SystemObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class MMImportCoupling {
	
	private SystemObject system;
	private Map<String, LinkedHashMap<String, Integer>> importCouplingMap;
	
	public MMImportCoupling(SystemObject system) {
		this.system = system;
		this.importCouplingMap = new LinkedHashMap<String, LinkedHashMap<String, Integer>>();
		List<String> classNames = system.getClassNames();
		for(String className : classNames) {
			LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
			for(String className2 : classNames) {
				map.put(className2, 0);
			}
			importCouplingMap.put(className, map);
		}
		calculateCoupling();
	}
	
	private void calculateCoupling() {
		ListIterator<ClassObject> classIterator = system.getClassListIterator();
		while(classIterator.hasNext()) {
			ClassObject classObject = classIterator.next();
			LinkedHashMap<String, Integer> map = importCouplingMap.get(classObject.getName());
			ListIterator<MethodObject> methodIterator = classObject.getMethodIterator();
			while(methodIterator.hasNext()) {
				MethodObject method = methodIterator.next();
				if(method.getMethodBody() != null) {
					List<MethodInvocationObject> methodInvocations = method.getMethodInvocations();
					for(MethodInvocationObject methodInvocation : methodInvocations) {
						String methodInvocationOrigin = methodInvocation.getOriginClassName();
						if(map.keySet().contains(methodInvocationOrigin)) {
							ClassObject originClass = system.getClassObject(methodInvocationOrigin);
							MethodObject originMethod = originClass.getMethod(methodInvocation);
							if(!originMethod.isStatic())
								map.put(methodInvocationOrigin, map.get(methodInvocationOrigin)+1);
						}
					}
					List<FieldInstructionObject> fieldInstructions = method.getFieldInstructions();
					for(FieldInstructionObject fieldInstruction : fieldInstructions) {
						String fieldInstructionOrigin = fieldInstruction.getOwnerClass();
						if(map.keySet().contains(fieldInstructionOrigin) && !fieldInstruction.isStatic()) {
							map.put(fieldInstructionOrigin, map.get(fieldInstructionOrigin)+1);
						}
					}
				}
			}
		}
	}
	
	private double getClassAverageCoupling(String className) {
		LinkedHashMap<String, Integer> map = importCouplingMap.get(className);
		int sum = 0;
		Set<String> keySet = map.keySet();
		for(String key : keySet) {
			if(!key.equals(className))
				sum += map.get(key);
		}
		return (double)sum/(double)(keySet.size()-1);
	}
	
	public double getSystemAverageCoupling() {
		Set<String> keySet = importCouplingMap.keySet();
		double sum = 0;
		for(String key : keySet) {
			sum += getClassAverageCoupling(key);
		}
		return sum/(double)keySet.size();
	}
}
