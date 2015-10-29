package gr.uom.java.ast;

import java.util.List;

import org.eclipse.jdt.core.dom.MethodInvocation;

public class MethodInvocationObject extends AbstractMethodInvocationObject {

    public MethodInvocationObject(TypeObject originClassType, String methodName, TypeObject returnType) {
        super(originClassType, methodName, returnType);
    }

    public MethodInvocationObject(TypeObject originClassType, String methodName, TypeObject returnType, List<TypeObject> parameterList) {
        super(originClassType, methodName, returnType, parameterList);
    }

    public void setMethodInvocation(MethodInvocation methodInvocation) {
    	//this.methodInvocation = methodInvocation;
    	this.methodInvocation = ASTInformationGenerator.generateASTInformation(methodInvocation);
    }

    public MethodInvocation getMethodInvocation() {
    	//return this.methodInvocation;
    	return (MethodInvocation)this.methodInvocation.recoverASTNode();
    }
}