package gr.uom.java.ast;

import gr.uom.java.ast.decomposition.MethodBodyObject;

import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;

public class ConstructorObject {

    protected String name;
	protected List<ParameterObject> parameterList;
    protected Access access;
    protected MethodBodyObject methodBody;
    //protected MethodDeclaration methodDeclaration;
    protected ASTInformation methodDeclaration;
    private volatile int hashCode = 0;

    public ConstructorObject() {
		this.parameterList = new ArrayList<ParameterObject>();
        this.access = Access.NONE;
    }

    public void setMethodDeclaration(MethodDeclaration methodDeclaration) {
    	//this.methodDeclaration = methodDeclaration;
    	this.methodDeclaration = ASTInformationGenerator.generateASTInformation(methodDeclaration);
    }

    public MethodDeclaration getMethodDeclaration() {
    	//return this.methodDeclaration;
    	return (MethodDeclaration)this.methodDeclaration.recoverASTNode();
    }

    public void setMethodBody(MethodBodyObject methodBody) {
    	this.methodBody = methodBody;
    }

    public MethodBodyObject getMethodBody() {
    	return this.methodBody;
    }

    public void setAccess(Access access) {
        this.access = access;
    }

    public Access getAccess() {
        return access;
    }

    public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean addParameter(ParameterObject parameter) {
		return parameterList.add(parameter);
	}

    public ListIterator<ParameterObject> getParameterListIterator() {
		return parameterList.listIterator();
	}

    public ParameterObject getParameter(int position) {
    	if(position >= 0 && position < parameterList.size())
    		return parameterList.get(position);
    	else
    		return null;
    }

	public List<MethodInvocationObject> getMethodInvocations() {
		if(methodBody != null)
			return methodBody.getMethodInvocations();
		else
			return new ArrayList<MethodInvocationObject>();
	}

	public List<SuperMethodInvocationObject> getSuperMethodInvocations() {
		if(methodBody != null)
			return methodBody.getSuperMethodInvocations();
		else
			return new ArrayList<SuperMethodInvocationObject>();
	}

    public List<FieldInstructionObject> getFieldInstructions() {
    	if(methodBody != null)
    		return methodBody.getFieldInstructions();
    	else
    		return new ArrayList<FieldInstructionObject>();
    }

    public List<LocalVariableDeclarationObject> getLocalVariableDeclarations() {
    	if(methodBody != null)
    		return methodBody.getLocalVariableDeclarations();
    	else
    		return new ArrayList<LocalVariableDeclarationObject>();
    }

    public List<LocalVariableInstructionObject> getLocalVariableInstructions() {
    	if(methodBody != null)
    		return methodBody.getLocalVariableInstructions();
    	else
    		return new ArrayList<LocalVariableInstructionObject>();
    }

    public boolean containsMethodInvocation(MethodInvocationObject methodInvocation) {
    	if(methodBody != null)
    		return methodBody.containsMethodInvocation(methodInvocation);
    	else
    		return false;
    }

    public boolean containsSuperMethodInvocation(SuperMethodInvocationObject superMethodInvocation) {
    	if(methodBody != null)
    		return methodBody.containsSuperMethodInvocation(superMethodInvocation);
    	else
    		return false;
    }

    public List<Assignment> getFieldAssignments(FieldInstructionObject fio) {
    	if(methodBody != null)
    		return methodBody.getFieldAssignments(fio);
    	else
    		return new ArrayList<Assignment>();
    }

    public List<PostfixExpression> getFieldPostfixAssignments(FieldInstructionObject fio) {
    	if(methodBody != null)
    		return methodBody.getFieldPostfixAssignments(fio);
    	else
    		return new ArrayList<PostfixExpression>();
    }

    public List<PrefixExpression> getFieldPrefixAssignments(FieldInstructionObject fio) {
    	if(methodBody != null)
    		return methodBody.getFieldPrefixAssignments(fio);
    	else
    		return new ArrayList<PrefixExpression>();
    }

    public boolean containsSuperMethodInvocation() {
    	if(methodBody != null)
    		return methodBody.containsSuperMethodInvocation();
    	else
    		return false;
    }

    public List<TypeObject> getParameterTypeList() {
    	List<TypeObject> list = new ArrayList<TypeObject>();
    	for(ParameterObject parameterObject : parameterList)
    		list.add(parameterObject.getType());
    	return list;
    }

    public List<String> getParameterList() {
    	List<String> list = new ArrayList<String>();
    	for(ParameterObject parameterObject : parameterList)
    		list.add(parameterObject.getType().toString());
    	return list;
    }

    public boolean equals(Object o) {
        if(this == o) {
			return true;
		}

		if (o instanceof ConstructorObject) {
			ConstructorObject constructorObject = (ConstructorObject)o;

			return this.name.equals(constructorObject.name) &&
				this.parameterList.equals(constructorObject.parameterList);
		}
		return false;
    }

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + name.hashCode();
    		result = 37*result + parameterList.hashCode();
    		hashCode = result;
    	}
    	return hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(!access.equals(Access.NONE))
            sb.append(access.toString()).append(" ");
        sb.append(name);
        sb.append("(");
        if(!parameterList.isEmpty()) {
            for(int i=0; i<parameterList.size()-1; i++)
                sb.append(parameterList.get(i).toString()).append(", ");
            sb.append(parameterList.get(parameterList.size()-1).toString());
        }
        sb.append(")");
        sb.append("\n").append(methodBody.toString());
        return sb.toString();
    }
}