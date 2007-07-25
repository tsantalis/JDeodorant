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
        replaceGetInvocationWithAttribute();
        removeExistingGetMethods();
    }

    private MySystem(Map<String,MyClass> classMap) {
        this.classMap = classMap;
    }

    public boolean checkInheritanceRelationship(String subClass, String superClass) {
        String s;
        String sub = subClass;
        while((s = classMap.get(sub).getSuperclass()) != null) {
            if(s.equals(superClass))
                return true;
            sub = s;
        }
        return false;
    }
/*
    public void moveOperation(Entity entity, String toClass) {
        if(entity instanceof MyMethod)
            moveMethod((MyMethod)entity,toClass);
        else if(entity instanceof MyAttribute)
            moveAttribute((MyAttribute)entity,toClass);
    }

    public void moveAttribute(MyAttribute oldAttribute, String toClass) {
        MyAttribute newAttribute = MyAttribute.newInstance(oldAttribute);
        newAttribute.setClassOrigin(toClass);
        if(associationDetection.checkForContainerAssociation(oldAttribute.getClassOrigin(),toClass)) {
            newAttribute.setClassType("java.util.Vector");
        }

        MyAttributeInstruction oldAttributeInstruction = oldAttribute.generateAttributeInstruction();
        MyAttributeInstruction newAttributeInstruction = newAttribute.generateAttributeInstruction();
        classMap.get(oldAttribute.getClassOrigin()).removeAttribute(oldAttribute);
        classMap.get(toClass).addAttribute(newAttribute);
        Iterator<MyClass> classIterator = getClassIterator();
        while(classIterator.hasNext()) {
            MyClass myClass = classIterator.next();
            ListIterator<MyMethod> methodIterator = myClass.getMethodIterator();
            while(methodIterator.hasNext()) {
                MyMethod myMethod = methodIterator.next();
                myMethod.replaceAttributeInstruction(oldAttributeInstruction,newAttributeInstruction);
            }
        }
    }

    public void moveMethod(MyMethod oldMethod, String toClass) {
        MyMethod newMethod = MyMethod.newInstance(oldMethod);
        newMethod.setClassOrigin(toClass);
        newMethod.removeParameter(toClass);

        ListIterator<MyAttributeInstruction> instructionIterator = newMethod.getAttributeInstructionIterator();
        while(instructionIterator.hasNext()) {
            MyAttributeInstruction instruction = instructionIterator.next();
            if(instruction.getClassOrigin().equals(oldMethod.getClassOrigin()))
                newMethod.addParameter(instruction.getClassOrigin());
        }
        ListIterator<MyMethodInvocation> invocationIterator = newMethod.getMethodInvocationIterator();
        while(invocationIterator.hasNext()) {
            MyMethodInvocation invocation = invocationIterator.next();
            if(invocation.getClassOrigin().equals(oldMethod.getClassOrigin()))
                newMethod.addParameter(invocation.getClassOrigin());
        }

        MyMethodInvocation oldMethodInvocation = oldMethod.generateMethodInvocation();
        MyMethodInvocation newMethodInvocation = newMethod.generateMethodInvocation();
        classMap.get(oldMethod.getClassOrigin()).removeMethod(oldMethod);
        classMap.get(toClass).addMethod(newMethod);
        Iterator<MyClass> classIterator = getClassIterator();
        while(classIterator.hasNext()) {
            MyClass myClass = classIterator.next();
            ListIterator<MyAttribute> attributeIterator = myClass.getAttributeIterator();
            while(attributeIterator.hasNext()) {
                MyAttribute attribute = attributeIterator.next();
                attribute.replaceMethod(oldMethod,newMethod);
            }
            ListIterator<MyMethod> methodIterator = myClass.getMethodIterator();
            while(methodIterator.hasNext()) {
                MyMethod myMethod = methodIterator.next();
                myMethod.replaceMethodInvocation(oldMethodInvocation,newMethodInvocation);
            }
        }
    }
*/
    private void generate(SystemObject so) {
        ListIterator<ClassObject> classIt = so.getClassListIterator();
        while(classIt.hasNext()) {
            ClassObject co = classIt.next();
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

            ListIterator<MethodObject> methodIt = co.getMethodIterator();
            while(methodIt.hasNext()) {
                MethodObject mo = methodIt.next();
                if(!mo.isStatic()) {
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
                }
            }
            classMap.put(co.getName(),myClass);
        }
        
        for(MyClass myClass : classMap.values()) {
            ListIterator<MyMethod> methodIterator = myClass.getMethodIterator();
            while(methodIterator.hasNext()) {
                MyMethod myMethod = methodIterator.next();
                ListIterator<MyAttributeInstruction> attributeInstructionIterator = myMethod.getAttributeInstructionIterator();
                while(attributeInstructionIterator.hasNext()) {
                    MyAttributeInstruction myInstruction = attributeInstructionIterator.next();
                    MyClass ownerClass = classMap.get(myInstruction.getClassOrigin());
                    MyAttribute accessedAttribute = ownerClass.getAttribute(myInstruction);
                    /*while(accessedAttribute == null && ownerClass.getSuperclass() != null) {
                        myInstruction.setClassOrigin(ownerClass.getSuperclass());
                        ownerClass = classMap.get(ownerClass.getSuperclass());
                        accessedAttribute = ownerClass.getAttribute(myInstruction);
                    }*/
                    if(accessedAttribute != null) {
                        if(accessedAttribute.isReference())
                            myMethod.setAttributeInstructionReference(myInstruction, true);
                        accessedAttribute.addMethod(myMethod);
                    }
                }
            }
        }
    }
    //replaces get, set and add method invocations with the corresponding attributes
    private void replaceGetInvocationWithAttribute() {
        List<MyMethod> methodsToBeRemoved = new ArrayList<MyMethod>();
        for(MyClass myClass : classMap.values()) {
            ListIterator<MyMethod> methodIterator = myClass.getMethodIterator();
            while(methodIterator.hasNext()) {
                MyMethod myMethod = methodIterator.next();
                ListIterator<MyMethodInvocation> methodInvocationIterator = myMethod.getMethodInvocationIterator();
                Map<MyMethodInvocation, MyAttributeInstruction> methodInvocationsToBeReplaced = new LinkedHashMap<MyMethodInvocation, MyAttributeInstruction>();
                while(methodInvocationIterator.hasNext()) {
                    MyMethodInvocation myInvocation = methodInvocationIterator.next();
                    if(myInvocation.getMethodName().startsWith("get") || myInvocation.getMethodName().startsWith("set") || myInvocation.getMethodName().startsWith("add")) {
                        MyClass invokedClass = classMap.get(myInvocation.getClassOrigin());
                        MyMethod invokedMethod = invokedClass.getMethod(myInvocation);
                        /*while(invokedMethod == null && invokedClass.getSuperclass() != null) {
                            myInvocation.setClassOrigin(invokedClass.getSuperclass());
                            invokedClass = classMap.get(invokedClass.getSuperclass());
                            invokedMethod = invokedClass.getMethod(myInvocation);
                        }*/
                        MyAttributeInstruction attributeInstruction = null;
                        if(invokedMethod != null && invokedMethod.getNumberOfAttributeInstructions() == 1 && invokedMethod.getNumberOfMethodInvocations() == 0) {
                            if(myInvocation.getMethodName().startsWith("get") && myInvocation.getNumberOfParameters() == 0 && myInvocation.getReturnType().equals(invokedMethod.getAttributeInstruction(0).getClassType()))
                                attributeInstruction = invokedMethod.getAttributeInstruction(0);
                            else if(myInvocation.getMethodName().startsWith("set") && myInvocation.getNumberOfParameters() == 1 && myInvocation.getParameterList().get(0).equals(invokedMethod.getAttributeInstruction(0).getClassType()))
                                attributeInstruction = invokedMethod.getAttributeInstruction(0);
                            else if(myInvocation.getMethodName().startsWith("add") && myInvocation.getNumberOfParameters() == 1)
                                attributeInstruction = invokedMethod.getAttributeInstruction(0);
                        }
                        if(attributeInstruction != null) {
                            MyAttribute myAttribute = invokedClass.getAttribute(attributeInstruction);
                            /*while(myAttribute == null && invokedClass.getSuperclass() != null) {
                                invokedClass = classMap.get(invokedClass.getSuperclass());
                                myAttribute = invokedClass.getAttribute(attributeInstruction);
                            }*/
                            if(myAttribute != null) {
                                myAttribute.addMethod(myMethod);
                                myAttribute.removeMethod(invokedMethod);
                                if(!methodInvocationsToBeReplaced.containsKey(myInvocation))
                                    methodInvocationsToBeReplaced.put(myInvocation, attributeInstruction);
                                //myMethod.addAttributeInstruction(attributeInstruction);
                                if(!methodsToBeRemoved.contains(invokedMethod))
                                    methodsToBeRemoved.add(invokedMethod);
                            }
                        }
                    }
                }
                myMethod.replaceMethodInvocationsWithAttributeInstructions(methodInvocationsToBeReplaced);
            }
        }
        for(MyMethod myMethod : methodsToBeRemoved) {
            MyClass myClass = classMap.get(myMethod.getClassOrigin());
            myClass.removeMethod(myMethod);
        }
    }
    //removes remaining get, set and add methods
    private void removeExistingGetMethods() {
        List<MyMethod> methodsToBeRemoved = new ArrayList<MyMethod>();
        for(MyClass myClass : classMap.values()) {
            ListIterator<MyMethod> methodIterator = myClass.getMethodIterator();
            while(methodIterator.hasNext()) {
                MyMethod myMethod = methodIterator.next();
                if(myMethod.getMethodName().startsWith("get") && myMethod.getNumberOfParameters() == 0) {
                    if(myMethod.getNumberOfAttributeInstructions() == 1 && myMethod.getNumberOfMethodInvocations() == 0) {
                        MyAttributeInstruction attributeInstruction = myMethod.getAttributeInstruction(0);
                        MyAttribute attribute = myClass.getAttribute(attributeInstruction);
                        if(attribute != null && attribute.getClassType().equals(myMethod.getReturnType())) {
                            attribute.removeMethod(myMethod);
                            methodsToBeRemoved.add(myMethod);
                        }
                    }
                }
                else if(myMethod.getMethodName().startsWith("set") && myMethod.getNumberOfParameters() == 1) {
                    if(myMethod.getNumberOfAttributeInstructions() == 1 && myMethod.getNumberOfMethodInvocations() == 0) {
                        MyAttributeInstruction attributeInstruction = myMethod.getAttributeInstruction(0);
                        MyAttribute attribute = myClass.getAttribute(attributeInstruction);
                        if(attribute != null && attribute.getClassType().equals(myMethod.getParameterList().get(0))) {
                            attribute.removeMethod(myMethod);
                            methodsToBeRemoved.add(myMethod);
                        }
                    }
                }
                else if(myMethod.getMethodName().startsWith("add") && myMethod.getNumberOfParameters() == 1) {
                    if(myMethod.getNumberOfAttributeInstructions() == 1 && myMethod.getNumberOfMethodInvocations() == 0) {
                        MyAttributeInstruction attributeInstruction = myMethod.getAttributeInstruction(0);
                        MyAttribute attribute = myClass.getAttribute(attributeInstruction);
                        if(attribute != null) {
                            attribute.removeMethod(myMethod);
                            methodsToBeRemoved.add(myMethod);
                        }
                    }
                }
            }
        }
        for(MyMethod myMethod : methodsToBeRemoved) {
            MyClass myClass = classMap.get(myMethod.getClassOrigin());
            myClass.removeMethod(myMethod);
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
