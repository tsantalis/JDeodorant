package gr.uom.java.ast;

import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

import org.eclipse.jdt.core.dom.MethodInvocation;

public class MethodInvocationObject {

    private String originClassName;
    private String methodName;
    private TypeObject returnType;
    private List<TypeObject> parameterList;
    private List<String> thrownExceptions;
    private boolean _static;
    //private MethodInvocation methodInvocation;
    private ASTInformation methodInvocation;
    private volatile int hashCode = 0;

    public MethodInvocationObject(String originClassName, String methodName, TypeObject returnType) {
        this.originClassName = originClassName;
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameterList = new ArrayList<TypeObject>();
        this.thrownExceptions = new ArrayList<String>();
        this._static = false;
    }

    public MethodInvocationObject(String originClassName, String methodName, TypeObject returnType, List<TypeObject> parameterList) {
        this.originClassName = originClassName;
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameterList = parameterList;
        this._static = false;
    }

    public boolean addParameter(TypeObject parameterType) {
        return parameterList.add(parameterType);
    }

    public ListIterator<TypeObject> getParameterListIterator() {
        return parameterList.listIterator();
    }
    
    public List<TypeObject> getParameterTypeList() {
    	return this.parameterList;
    }

    public TypeObject getReturnType() {
		return returnType;
	}

    public String getOriginClassName() {
        return this.originClassName;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public List<String> getParameterList() {
    	List<String> list = new ArrayList<String>();
    	for(TypeObject typeObject : parameterList)
    		list.add(typeObject.toString());
    	return list;
    }

    public boolean isStatic() {
        return _static;
    }

    public void setStatic(boolean s) {
        _static = s;
    }

    public void addThrownException(String type) {
    	thrownExceptions.add(type);
    }

    public List<String> getThrownExceptions() {
    	return this.thrownExceptions;
    }

    public void setMethodInvocation(MethodInvocation methodInvocation) {
    	//this.methodInvocation = methodInvocation;
    	this.methodInvocation = ASTInformationGenerator.generateASTInformation(methodInvocation);
    }

    public MethodInvocation getMethodInvocation() {
    	//return this.methodInvocation;
    	return (MethodInvocation)this.methodInvocation.recoverASTNode();
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if (o instanceof MethodInvocationObject) {
            MethodInvocationObject methodInvocationObject = (MethodInvocationObject)o;

            return originClassName.equals(methodInvocationObject.originClassName) &&
                methodName.equals(methodInvocationObject.methodName) &&
                returnType.equals(methodInvocationObject.returnType) &&
                parameterList.equals(methodInvocationObject.parameterList);
        }
        return false;
    }

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + originClassName.hashCode();
    		result = 37*result + methodName.hashCode();
    		result = 37*result + returnType.hashCode();
    		for(TypeObject parameter : parameterList)
    			result = 37*result + parameter.hashCode();
    		hashCode = result;
    	}
    	return hashCode;
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(originClassName).append("::");
        sb.append(methodName);
        sb.append("(");
        if(!parameterList.isEmpty()) {
            for(int i=0; i<parameterList.size()-1; i++)
                sb.append(parameterList.get(i)).append(", ");
            sb.append(parameterList.get(parameterList.size()-1));
        }
        sb.append(")");
        sb.append(":").append(returnType);
        return sb.toString();
    }
}