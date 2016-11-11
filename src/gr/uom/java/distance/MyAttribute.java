package gr.uom.java.distance;

import gr.uom.java.ast.FieldObject;

import java.util.*;

public class MyAttribute extends Entity {

    private String classOrigin;
    private String classType;
    private String name;
    private List<MyMethod> methodList;
    private boolean reference;
    private String access;
    private FieldObject fieldObject;
    private volatile int hashCode = 0;

    public MyAttribute(String classOrigin, String classType, String name) {
        this.classOrigin = classOrigin;
        this.classType = classType;
        this.name = name;
        this.methodList = new ArrayList<MyMethod>();
        this.reference = false;
    }

    public FieldObject getFieldObject() {
		return fieldObject;
	}

	public void setFieldObject(FieldObject fieldObject) {
		this.fieldObject = fieldObject;
	}

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public MyAttributeInstruction generateAttributeInstruction() {
        MyAttributeInstruction instruction = new MyAttributeInstruction(this.classOrigin,this.classType,this.name);
        instruction.setReference(this.reference);
        return instruction;
    }

    public void setClassOrigin(String className) {
        this.classOrigin = className;
    }

    public String getClassOrigin() {
        return classOrigin;
    }

    public String getName() {
        return name;
    }

    public void setClassType(String type) {
        classType = type;
    }

    public String getClassType() {
        return classType;
    }

    public boolean isReference() {
        return reference;
    }

    public void setReference(boolean reference) {
        this.reference = reference;
    }

    public void addMethod(MyMethod method) {
    	if(!methodList.contains(method))
    		methodList.add(method);
    }

    public ListIterator<MyMethod> getMethodIterator() {
        return methodList.listIterator();
    }

    public boolean equals(MyAttributeInstruction attributeInstruction) {
        return this.classOrigin.equals(attributeInstruction.getClassOrigin()) &&
            this.classType.equals(attributeInstruction.getClassType()) &&
            this.name.equals(attributeInstruction.getName());
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if(o instanceof MyAttribute) {
            MyAttribute attribute = (MyAttribute)o;
            return this.classOrigin.equals(attribute.classOrigin) && this.classType.equals(attribute.classType) &&
                this.name.equals(attribute.name);
        }
        return false;
    }

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + classOrigin.hashCode();
    		result = 37*result + classType.hashCode();
    		result = 37*result + name.hashCode();
    		hashCode = result;
    	}
    	return hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(classOrigin).append("::");
        sb.append(classType).append(" ");
        sb.append(name);
        return sb.toString();
    }

    public Set<String> getEntitySet() {
        Set<String> set = new HashSet<String>();
        if(!this.isReference()) {
            for(MyMethod method : methodList) {
                set.add(method.toString());
            }
        }
        return set;
    }

	public Set<String> getFullEntitySet() {
		Set<String> set = new HashSet<String>();
		set.add(this.toString());
            for(MyMethod method : methodList) {
                set.add(method.toString());
            }
        return set;
	}
}
