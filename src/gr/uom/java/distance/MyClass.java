package gr.uom.java.distance;

import gr.uom.java.ast.ClassObject;

import java.util.*;

public class MyClass {

    private String name;
    private String superclass;
    private List<MyAttribute> attributeList;
    private List<MyMethod> methodList;
    private ClassObject classObject;

    public MyClass(String name) {
        this.name = name;
        this.attributeList = new ArrayList<MyAttribute>();
        this.methodList = new ArrayList<MyMethod>();
    }

    public String getName() {
        return name;
    }

    public String getSuperclass() {
        return superclass;
    }

    public void setSuperclass(String superclass) {
        this.superclass = superclass;
    }

    public ClassObject getClassObject() {
    	return this.classObject;
    }

    public void setClassObject(ClassObject classObject) {
    	this.classObject = classObject;
    }

    public void addAttribute(MyAttribute attribute) {
        if(!attributeList.contains(attribute))
            attributeList.add(attribute);
    }

    public MyAttribute getAttribute(MyAttributeInstruction attributeInstruction) {
        for(MyAttribute attribute : attributeList) {
            if(attribute.equals(attributeInstruction))
                return attribute;
        }
        return null;
    }

    public MyMethod getMethod(MyMethodInvocation methodInvocation) {
        for(MyMethod method : methodList) {
            if(method.equals(methodInvocation))
                return method;
        }
        return null;
    }

    public MyMethod getMethod(MyMethod myMethod) {
    	for(MyMethod method : methodList) {
            if(method.equals(myMethod))
                return method;
        }
        return null;
    }

    public void addMethod(MyMethod method) {
        methodList.add(method);
    }

    public void removeMethod(MyMethod method) {
        methodList.remove(method);
    }

    public void removeAttribute(MyAttribute attribute) {
        attributeList.remove(attribute);
    }

    public ListIterator<MyAttribute> getAttributeIterator() {
        return attributeList.listIterator();
    }

    public ListIterator<MyMethod> getMethodIterator() {
        return methodList.listIterator();
    }

    public Set<String> getEntitySet() {
        Set<String> set = new HashSet<String>();
        for(MyAttribute attribute : attributeList) {
            if(!attribute.isReference())
                set.add(attribute.toString());
        }
        for(MyMethod method : methodList) {
            set.add(method.toString());
        }
        return set;
    }

    public static MyClass newInstance(MyClass myClass) {
        MyClass newClass = new MyClass(myClass.name);
        newClass.setClassObject(myClass.classObject);
        newClass.setSuperclass(myClass.superclass);
        ListIterator<MyAttribute> attributeIterator = myClass.getAttributeIterator();
        while(attributeIterator.hasNext()) {
            MyAttribute attribute = attributeIterator.next();
            newClass.addAttribute(MyAttribute.newInstance(attribute));
        }
        ListIterator<MyMethod> methodIterator = myClass.getMethodIterator();
        while(methodIterator.hasNext()) {
            MyMethod method = methodIterator.next();
            newClass.addMethod(MyMethod.newInstance(method));
        }
        return newClass;
    }

    public String toString() {
        return name;
    }
}
