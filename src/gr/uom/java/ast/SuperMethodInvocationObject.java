package gr.uom.java.ast;

import java.util.List;

import org.eclipse.jdt.core.dom.SuperMethodInvocation;

public class SuperMethodInvocationObject extends AbstractMethodInvocationObject {

    public SuperMethodInvocationObject(TypeObject originClassType, String methodName, TypeObject returnType) {
        super(originClassType, methodName, returnType);
    }

    public SuperMethodInvocationObject(TypeObject originClassType, String methodName, TypeObject returnType, List<TypeObject> parameterList) {
        super(originClassType, methodName, returnType, parameterList);
    }

    public void setSuperMethodInvocation(SuperMethodInvocation superMethodInvocation) {
    	//this.superMethodInvocation = superMethodInvocation;
    	this.methodInvocation = ASTInformationGenerator.generateASTInformation(superMethodInvocation);
    }

    public SuperMethodInvocation getSuperMethodInvocation() {
    	//return this.superMethodInvocation;
    	return (SuperMethodInvocation)this.methodInvocation.recoverASTNode();
    }
}
