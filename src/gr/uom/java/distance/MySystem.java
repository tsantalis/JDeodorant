package gr.uom.java.distance;

import gr.uom.java.ast.*;
import gr.uom.java.ast.association.AssociationDetection;
import gr.uom.java.ast.decomposition.MethodBodyObject;

import java.util.*;

public class MySystem {

    private Map<String,MyClass> classMap;
    private AssociationDetection associationDetection;

    public MySystem(SystemObject system) {
        this.classMap = new HashMap<String,MyClass>();
        this.associationDetection = new AssociationDetection(system);
        generate(system);
        //replaceGetInvocationWithAttribute();
        //removeExistingGetMethods();
    }

    private MySystem(Map<String,MyClass> classMap) {
        this.classMap = classMap;
    }

    private void generate(SystemObject so) {
        ListIterator<ClassObject> classIterator1 = so.getClassListIterator();
        while(classIterator1.hasNext()) {
            ClassObject co = classIterator1.next();
            MyClass myClass = new MyClass(co.getName());
            myClass.setClassObject(co);
            if(so.getClassObject(co.getSuperclass()) != null) {
                myClass.setSuperclass(co.getSuperclass());
            }

            ListIterator<FieldObject> fieldIt = co.getFieldIterator();
            while(fieldIt.hasNext()) {
                FieldObject fo = fieldIt.next();
                if(!fo.isStatic()) {
                    MyAttribute myAttribute = new MyAttribute(co.getName(),fo.getType().toString(),fo.getName());
                    myAttribute.setAccess(fo.getAccess().toString());
                    if(associationDetection.containsFieldObject(fo))
                    	myAttribute.setReference(true);
                    myClass.addAttribute(myAttribute);
                }
            }
            classMap.put(co.getName(),myClass);
        }
        
        ListIterator<ClassObject> classIterator2 = so.getClassListIterator();
        while(classIterator2.hasNext()) {
            ClassObject co = classIterator2.next();
            MyClass myClass = classMap.get(co.getName());
            ListIterator<MethodObject> methodIt = co.getMethodIterator();
            while(methodIt.hasNext()) {
                MethodObject mo = methodIt.next();
                if(!mo.isStatic() && so.containsGetter(mo.generateMethodInvocation()) == null &&
                		so.containsSetter(mo.generateMethodInvocation()) == null && so.containsCollectionAdder(mo.generateMethodInvocation()) == null) {
                    MethodInvocationObject delegation = so.containsDelegate(mo.generateMethodInvocation());
                    if(delegation == null || (delegation != null && so.getClassObject(delegation.getOriginClassName()) == null)) {
	                	MyMethod myMethod = new MyMethod(mo.getClassName(),mo.getName(),
	                        mo.getReturnType().toString(),mo.getParameterList());
	                    if(mo.isAbstract())
	                        myMethod.setAbstract(true);
	                    myMethod.setAccess(mo.getAccess().toString());
	                    myMethod.setMethodObject(mo);
	                    MethodBodyObject methodBodyObject = mo.getMethodBody();
	                    if(methodBodyObject != null) {
	                    	MyMethodBody myMethodBody = new MyMethodBody(methodBodyObject, so);
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

    public static MySystem newInstance(MySystem system) {
        Map<String,MyClass> classMap = new HashMap<String,MyClass>();
        Iterator<MyClass> classIterator = system.getClassIterator();
        while(classIterator.hasNext()) {
            MyClass myClass = classIterator.next();
            classMap.put(myClass.getName(),MyClass.newInstance(myClass));
        }
        return new MySystem(classMap);
    }
}
