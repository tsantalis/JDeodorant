package gr.uom.java.ast;

import gr.uom.java.ast.decomposition.MethodBodyObject;

import java.util.List;
import java.util.ListIterator;

import org.eclipse.jdt.core.dom.MethodDeclaration;

public class MethodObject {

    private TypeObject returnType;
    private boolean _abstract;
    private boolean _static;
    private String className;
    private ConstructorObject constructorObject;

    public MethodObject(ConstructorObject co) {
        this.constructorObject = co;
        this._abstract = false;
        this._static = false;
    }

    public void setReturnType(TypeObject returnType) {
        this.returnType = returnType;
    }

    public TypeObject getReturnType() {
        return returnType;
    }

    public void setAbstract(boolean abstr) {
        this._abstract = abstr;
    }

    public boolean isAbstract() {
        return this._abstract;
    }

    public boolean isStatic() {
        return _static;
    }

    public void setStatic(boolean s) {
        _static = s;
    }

    public void setName(String name) {
        this.constructorObject.name = name;
    }

    public String getName() {
        return constructorObject.getName();
    }

    public Access getAccess() {
        return constructorObject.getAccess();
    }

    public MethodDeclaration getMethodDeclaration() {
    	return constructorObject.getMethodDeclaration();
    }
    public MethodBodyObject getMethodBody() {
    	return constructorObject.getMethodBody();
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return this.className;
    }

    public ListIterator<ParameterObject> getParameterListIterator() {
        return constructorObject.getParameterListIterator();
    }

    public ListIterator<MethodInvocationObject> getMethodInvocationIterator() {
        return constructorObject.getMethodInvocationIterator();
    }

    public ListIterator<FieldInstructionObject> getFieldInstructionIterator() {
        return constructorObject.getFieldInstructionIterator();
    }

    public ListIterator<LocalVariableDeclarationObject> getLocalVariableDeclarationIterator() {
        return constructorObject.getLocalVariableDeclarationIterator();
    }
    
    public ListIterator<LocalVariableInstructionObject> getLocalVariableInstructionIterator() {
        return constructorObject.getLocalVariableInstructionIterator();
    }

    public List<String> getParameterList() {
    	return constructorObject.getParameterList();
    }

    public boolean equals(MethodInvocationObject mio) {
    	return this.className.equals(mio.getOriginClassName()) && this.getName().equals(mio.getMethodName()) &&
    		this.returnType.equals(mio.getReturnType()) && this.constructorObject.getParameterTypeList().equals(mio.getParameterTypeList());
    }
    
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if (o instanceof MethodObject) {
            MethodObject methodObject = (MethodObject)o;

            return this.className.equals(methodObject.className) && this.returnType.equals(methodObject.returnType) &&
                this.constructorObject.equals(methodObject.constructorObject);
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(!constructorObject.access.equals(Access.NONE))
            sb.append(constructorObject.access.toString()).append(" ");
        if(_abstract)
            sb.append("abstract").append(" ");
        if(_static)
            sb.append("static").append(" ");
        sb.append(returnType.toString()).append(" ");
        sb.append(constructorObject.name);
        sb.append("(");
        if(!constructorObject.parameterList.isEmpty()) {
            for(int i=0; i<constructorObject.parameterList.size()-1; i++)
                sb.append(constructorObject.parameterList.get(i).toString()).append(", ");
            sb.append(constructorObject.parameterList.get(constructorObject.parameterList.size()-1).toString());
        }
        sb.append(")");
        sb.append("\n").append(constructorObject.methodBody.toString());
        return sb.toString();
    }
}