package gr.uom.java.ast;

import java.util.List;

import org.eclipse.jdt.core.dom.ConstructorInvocation;

public class ConstructorInvocationObject extends AbstractMethodInvocationObject {

	public ConstructorInvocationObject(TypeObject originClassType, String methodName, TypeObject returnType) {
		super(originClassType, methodName, returnType);
	}

	public ConstructorInvocationObject(TypeObject originClassType, String methodName, TypeObject returnType, List<TypeObject> parameterList) {
		super(originClassType, methodName, returnType, parameterList);
	}

    public void setConstructorInvocation(ConstructorInvocation constructorInvocation) {
    	this.methodInvocation = ASTInformationGenerator.generateASTInformation(constructorInvocation);
    }

    public ConstructorInvocation getConstructorInvocation() {
    	//return this.superMethodInvocation;
    	return (ConstructorInvocation)this.methodInvocation.recoverASTNode();
    }
}
