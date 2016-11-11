package gr.uom.java.distance;

import java.util.List;

public class MyMethodInvocation {
    private String classOrigin;
    private String methodName;
    private String returnType;
    private List<String> parameterList;

    public MyMethodInvocation(String classOrigin, String methodName, String returnType, List<String> parameterList) {
        this.classOrigin = classOrigin;
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameterList = parameterList;
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

    public int getNumberOfParameters() {
        return this.parameterList.size();
    }

    public void setClassOrigin(String classOrigin) {
        this.classOrigin = classOrigin;    
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if (o instanceof MyMethodInvocation) {
            MyMethodInvocation invocation = (MyMethodInvocation)o;
            return this.classOrigin.equals(invocation.classOrigin) &&
                this.methodName.equals(invocation.methodName) &&
                this.returnType.equals(invocation.returnType) &&
                this.parameterList.equals(invocation.parameterList);
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
}
