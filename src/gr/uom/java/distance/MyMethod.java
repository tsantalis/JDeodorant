package gr.uom.java.distance;

import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.decomposition.AbstractStatement;

import java.util.*;

public class MyMethod extends Entity {

    private String classOrigin;
    private String methodName;
    private String returnType;
    private List<String> parameterList;
    private MyMethodBody methodBody;
    private boolean isAbstract;
    private String access;
    private MethodObject methodObject;

    public MyMethod(String classOrigin, String methodName, String returnType, List<String> parameterList) {
        this.classOrigin = classOrigin;
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameterList = parameterList;
        this.isAbstract = false;
    }

    public void setMethodObject(MethodObject methodObject) {
    	this.methodObject = methodObject;
    }

    public MethodObject getMethodObject() {
    	return this.methodObject;
    }

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public void setMethodBody(MyMethodBody methodBody) {
    	this.methodBody = methodBody;
    }

    public MyMethodInvocation generateMethodInvocation() {
        return new MyMethodInvocation(this.classOrigin,this.methodName,this.returnType,this.parameterList);
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean anAbstract) {
        isAbstract = anAbstract;
    }

    public boolean containsParameter(String p) {
        for(String parameter : parameterList) {
            if(parameter.equals(p))
                return true;
        }
        return false;
    }

    public void setClassOrigin(String className) {
        this.classOrigin = className;
    }

    public void removeParameter(String className) {
        this.parameterList.remove(className);
    }

    public void addParameter(String parameter) {
        if(!parameterList.contains(parameter))
            parameterList.add(parameter);
    }

    public String getClassOrigin() {
        return classOrigin;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<String> getParameterList() {
        return parameterList;
    }

    public void replaceMethodInvocationsWithAttributeInstructions(Map<MyMethodInvocation, MyAttributeInstruction> map) {
        if(this.methodBody != null)
        	this.methodBody.replaceMethodInvocationsWithAttributeInstructions(map);
    }

    public void replaceMethodInvocation(MyMethodInvocation oldMethodInvocation, MyMethodInvocation newMethodInvocation) {
    	if(this.methodBody != null)
    		this.methodBody.replaceMethodInvocation(oldMethodInvocation, newMethodInvocation);
    }

    public void replaceAttributeInstruction(MyAttributeInstruction oldInstruction, MyAttributeInstruction newInstruction) {
    	if(this.methodBody != null)
    		this.methodBody.replaceAttributeInstruction(oldInstruction, newInstruction);
    }

    public void removeAttributeInstruction(MyAttributeInstruction attributeInstruction) {
    	if(this.methodBody != null)
    		this.methodBody.removeAttributeInstruction(attributeInstruction);
    }

    public void setAttributeInstructionReference(MyAttributeInstruction myAttributeInstruction, boolean reference) {
    	if(this.methodBody != null)
    		this.methodBody.setAttributeInstructionReference(myAttributeInstruction, reference);
    }

    public void replaceStatementsWithMethodInvocation(List<AbstractStatement> statementsToRemove, MyStatement methodInvocation) {
		this.methodBody.replaceStatementsWithMethodInvocation(statementsToRemove, methodInvocation);
	}

    public MyAbstractStatement getAbstractStatement(AbstractStatement statement) {
    	return this.methodBody.getAbstractStatement(statement);
    }

    public Set<String> getEntitySet(AbstractStatement statement) {
    	return this.methodBody.getEntitySet(statement);
    }

    public ListIterator<MyMethodInvocation> getMethodInvocationIterator() {
    	if(this.methodBody != null)
    		return this.methodBody.getMethodInvocationIterator();
    	else
    		return new ArrayList<MyMethodInvocation>().listIterator();
    }

    public ListIterator<MyAttributeInstruction> getAttributeInstructionIterator() {
    	if(this.methodBody != null)
    		return this.methodBody.getAttributeInstructionIterator();
    	else
    		return new ArrayList<MyAttributeInstruction>().listIterator();
    }

	public boolean containsAttributeInstruction(MyAttributeInstruction instruction) {
		return this.methodBody.containsAttributeInstruction(instruction);
	}

	public boolean containsMethodInvocation(MyMethodInvocation invocation) {
		return this.methodBody.containsMethodInvocation(invocation);
	}

    public MyMethodInvocation getMethodInvocation(int pos) {
    	if(this.methodBody != null)
    		return this.methodBody.getMethodInvocation(pos);
    	else
    		return null;
    }

    public MyAttributeInstruction getAttributeInstruction(int pos) {
    	if(this.methodBody != null)
    		return this.methodBody.getAttributeInstruction(pos);
    	else
    		return null;
    }

    public int getNumberOfAttributeInstructions() {
    	if(this.methodBody != null)
    		return this.methodBody.getNumberOfAttributeInstructions();
    	else
    		return 0;
    }

    public int getNumberOfMethodInvocations() {
    	if(this.methodBody != null)
    		return this.methodBody.getNumberOfMethodInvocations();
    	else return 0;
    }

    public int getNumberOfParameters() {
        return this.parameterList.size();
    }

    public boolean equals(MyMethodInvocation methodInvocation) {
        return this.classOrigin.equals(methodInvocation.getClassOrigin()) &&
            this.methodName.equals(methodInvocation.getMethodName()) &&
            this.returnType.equals(methodInvocation.getReturnType()) &&
            this.parameterList.equals(methodInvocation.getParameterList());
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if (o instanceof MyMethod) {
            MyMethod method = (MyMethod)o;
            return this.classOrigin.equals(method.classOrigin) &&
                this.methodName.equals(method.methodName) &&
                this.returnType.equals(method.returnType) &&
                this.parameterList.equals(method.parameterList);
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(!classOrigin.equals(methodName))
            sb.append(classOrigin).append("::");
        sb.append(methodName);
        sb.append("(");
        if(!parameterList.isEmpty()) {
            for(int i=0; i<parameterList.size()-1; i++)
                sb.append(parameterList.get(i)).append(", ");
            sb.append(parameterList.get(parameterList.size()-1));
        }
        sb.append(")");
        if(returnType != null)
            sb.append(":").append(returnType);
        return sb.toString();
    }

    public Set<String> getEntitySet() {
        Set<String> set = new HashSet<String>();
        ListIterator<MyAttributeInstruction> attributeInstructionIterator = getAttributeInstructionIterator();
        while(attributeInstructionIterator.hasNext()) {
        	MyAttributeInstruction attributeInstruction = attributeInstructionIterator.next();
            if(!attributeInstruction.isReference())
                set.add(attributeInstruction.toString());
        }
        ListIterator<MyMethodInvocation> methodInvocationIterator = getMethodInvocationIterator();
        while(methodInvocationIterator.hasNext()) {
        	MyMethodInvocation methodInvocation = methodInvocationIterator.next();
            set.add(methodInvocation.toString());
        }
        return set;
    }

    public static MyMethod newInstance(MyMethod method) {
        List<String> newParameterList = new ArrayList<String>();
        for(String parameter : method.parameterList)
            newParameterList.add(parameter);
        MyMethod newMethod = new MyMethod(method.classOrigin,method.methodName,method.returnType,newParameterList);
        newMethod.setMethodObject(method.methodObject);
        if(method.isAbstract)
            newMethod.setAbstract(true);
        if(method.methodBody != null) {
        	MyMethodBody newMethodBody = MyMethodBody.newInstance(method.methodBody);
        	newMethod.setMethodBody(newMethodBody);
        }
        return newMethod;
    }
}
