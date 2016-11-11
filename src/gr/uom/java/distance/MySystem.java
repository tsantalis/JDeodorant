package gr.uom.java.distance;

import gr.uom.java.ast.*;
import gr.uom.java.ast.association.Association;
import gr.uom.java.ast.association.AssociationDetection;
import gr.uom.java.ast.decomposition.MethodBodyObject;

import java.util.*;

public class MySystem {

    private Map<String,MyClass> classMap;
    private AssociationDetection associationDetection;
    private SystemObject systemObject;

    public MySystem(SystemObject systemObject, boolean includeStaticMembers) {
    	this.systemObject = systemObject;
        this.classMap = new HashMap<String,MyClass>();
        this.associationDetection = new AssociationDetection(systemObject);
        if(includeStaticMembers)
        	generateSystemWithStaticMembers();
        else
        	generateSystem();
    }

    private void generateSystem() {
        ListIterator<ClassObject> classIterator1 = systemObject.getClassListIterator();
        while(classIterator1.hasNext()) {
            ClassObject co = classIterator1.next();
            MyClass myClass = new MyClass(co.getName());
            myClass.setClassObject(co);
            TypeObject superclassType = co.getSuperclass();
            if(superclassType != null) {
            	String superclass = superclassType.getClassType();
            	if(systemObject.getClassObject(superclass) != null) {
            		myClass.setSuperclass(superclass);
            	}
            }

            ListIterator<FieldObject> fieldIt = co.getFieldIterator();
            while(fieldIt.hasNext()) {
            	FieldObject fo = fieldIt.next();
            	if(!fo.isStatic()) {
            		MyAttribute myAttribute = new MyAttribute(co.getName(),fo.getType().toString(),fo.getName());
            		myAttribute.setAccess(fo.getAccess().toString());
            		myAttribute.setFieldObject(fo);
            		if(associationDetection.containsFieldObject(fo))
            			myAttribute.setReference(true);
            		myClass.addAttribute(myAttribute);
            	}
            }
            classMap.put(co.getName(),myClass);
        }
        
        ListIterator<ClassObject> classIterator2 = systemObject.getClassListIterator();
        while(classIterator2.hasNext()) {
            ClassObject co = classIterator2.next();
            MyClass myClass = classMap.get(co.getName());
            ListIterator<MethodObject> methodIt = co.getMethodIterator();
            while(methodIt.hasNext()) {
            	MethodObject mo = methodIt.next();
            	if(!mo.isStatic() && systemObject.containsGetter(mo.generateMethodInvocation()) == null &&
            			systemObject.containsSetter(mo.generateMethodInvocation()) == null && systemObject.containsCollectionAdder(mo.generateMethodInvocation()) == null) {
            		MethodInvocationObject delegation = systemObject.containsDelegate(mo.generateMethodInvocation());
            		if(delegation == null || (delegation != null && systemObject.getClassObject(delegation.getOriginClassName()) == null)) {
            			MyMethod myMethod = new MyMethod(mo.getClassName(),mo.getName(),
            					mo.getReturnType().toString(),mo.getParameterList());
            			if(mo.isAbstract())
            				myMethod.setAbstract(true);
            			myMethod.setAccess(mo.getAccess().toString());
            			myMethod.setMethodObject(mo);
            			MethodBodyObject methodBodyObject = mo.getMethodBody();
            			if(methodBodyObject != null) {
            				MyMethodBody myMethodBody = new MyMethodBody(methodBodyObject);
            				myMethod.setMethodBody(myMethodBody);
            			}
            			myClass.addMethod(myMethod);
            			ListIterator<MyAttributeInstruction> attributeInstructionIterator = myMethod.getAttributeInstructionIterator();
            			while(attributeInstructionIterator.hasNext()) {
            				MyAttributeInstruction myInstruction = attributeInstructionIterator.next();
            				MyClass ownerClass = classMap.get(myInstruction.getClassOrigin());
            				MyAttribute accessedAttribute = ownerClass.getAttribute(myInstruction);
            				if(accessedAttribute != null) {
            					if(accessedAttribute.isReference())
            						myMethod.setAttributeInstructionReference(myInstruction, true);
            					accessedAttribute.addMethod(myMethod);
            				}
            			}
            		}
            	}
            }
        }
    }

    private void generateSystemWithStaticMembers() {
        ListIterator<ClassObject> classIterator1 = systemObject.getClassListIterator();
        while(classIterator1.hasNext()) {
            ClassObject co = classIterator1.next();
            MyClass myClass = new MyClass(co.getName());
            myClass.setClassObject(co);
            TypeObject superclassType = co.getSuperclass();
            if(superclassType != null) {
            	String superclass = superclassType.getClassType();
            	if(systemObject.getClassObject(superclass) != null) {
            		myClass.setSuperclass(superclass);
            	}
            }

            ListIterator<FieldObject> fieldIt = co.getFieldIterator();
            while(fieldIt.hasNext()) {
            	FieldObject fo = fieldIt.next();
            	MyAttribute myAttribute = new MyAttribute(co.getName(),fo.getType().toString(),fo.getName());
            	myAttribute.setAccess(fo.getAccess().toString());
            	myAttribute.setFieldObject(fo);
            	if(associationDetection.containsFieldObject(fo))
            		myAttribute.setReference(true);
            	myClass.addAttribute(myAttribute);
            }
            classMap.put(co.getName(),myClass);
        }
        
        ListIterator<ClassObject> classIterator2 = systemObject.getClassListIterator();
        while(classIterator2.hasNext()) {
            ClassObject co = classIterator2.next();
            MyClass myClass = classMap.get(co.getName());
            ListIterator<MethodObject> methodIt = co.getMethodIterator();
            while(methodIt.hasNext()) {
            	MethodObject mo = methodIt.next();
            	if(systemObject.containsGetter(mo.generateMethodInvocation()) == null &&
            			systemObject.containsSetter(mo.generateMethodInvocation()) == null && systemObject.containsCollectionAdder(mo.generateMethodInvocation()) == null) {
            		MethodInvocationObject delegation = systemObject.containsDelegate(mo.generateMethodInvocation());
            		if(delegation == null || (delegation != null && systemObject.getClassObject(delegation.getOriginClassName()) == null)) {
            			MyMethod myMethod = new MyMethod(mo.getClassName(),mo.getName(),
            					mo.getReturnType().toString(),mo.getParameterList());
            			if(mo.isAbstract())
            				myMethod.setAbstract(true);
            			myMethod.setAccess(mo.getAccess().toString());
            			myMethod.setMethodObject(mo);
            			MethodBodyObject methodBodyObject = mo.getMethodBody();
            			if(methodBodyObject != null) {
            				MyMethodBody myMethodBody = new MyMethodBody(methodBodyObject);
            				myMethod.setMethodBody(myMethodBody);
            			}
            			myClass.addMethod(myMethod);
            			ListIterator<MyAttributeInstruction> attributeInstructionIterator = myMethod.getAttributeInstructionIterator();
            			while(attributeInstructionIterator.hasNext()) {
            				MyAttributeInstruction myInstruction = attributeInstructionIterator.next();
            				MyClass ownerClass = classMap.get(myInstruction.getClassOrigin());
            				MyAttribute accessedAttribute = ownerClass.getAttribute(myInstruction);
            				if(accessedAttribute != null) {
            					if(accessedAttribute.isReference())
            						myMethod.setAttributeInstructionReference(myInstruction, true);
            					accessedAttribute.addMethod(myMethod);
            				}
            			}
            		}
            	}
            }
        }
    }

    public Iterator<MyClass> getClassIterator() {
        return classMap.values().iterator();
    }

    public MyClass getClass(String className) {
    	return classMap.get(className);
    }

    public void addClass(MyClass newClass) {
    	if(!classMap.containsKey(newClass.getName())) {
    		classMap.put(newClass.getName(), newClass);
    	}
    }
    
    public void removeClass(MyClass oldClass) {
    	if(classMap.containsKey(oldClass.getName())) {
    		classMap.remove(oldClass.getName());
    	}
    }

    public SystemObject getSystemObject() {
		return systemObject;
	}

    public List<Association> getAssociationsOfClass(ClassObject classObject) {
    	return associationDetection.getAssociationsOfClass(classObject);
    }

    public Association containsAssociationWithMultiplicityBetweenClasses(String from, String to) {
    	Association association = associationDetection.getAssociation(from, to);
    	if(association != null && association.isContainer())
    		return association;
    	return null;
    }
}
