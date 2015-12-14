package gr.uom.java.ast;

import org.eclipse.jdt.core.dom.SimpleName;

public class FieldInstructionObject {

    private String ownerClass;
    private TypeObject type;
    private String name;
    private boolean _static;
    //private SimpleName simpleName;
    private ASTInformation simpleName;
    private volatile int hashCode = 0;
    private String variableBindingKey;

    public FieldInstructionObject(String ownerClass, TypeObject type, String name) {
        this.ownerClass = ownerClass;
        this.type = type;
        this.name = name;
        this._static = false;
    }

    public FieldInstructionObject(String ownerClass, TypeObject type, String name, String variableBindingKey) {
        this(ownerClass, type, name);
        this.variableBindingKey = variableBindingKey;
    }

    public String getOwnerClass() {
        return ownerClass;
    }

    public TypeObject getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getVariableBindingKey() {
		return variableBindingKey;
	}

	public boolean isStatic() {
        return _static;
    }

    public void setStatic(boolean s) {
        _static = s;
    }

    public void setSimpleName(SimpleName simpleName) {
    	//this.simpleName = simpleName;
    	this.variableBindingKey = simpleName.resolveBinding().getKey();
    	this.simpleName = ASTInformationGenerator.generateASTInformation(simpleName);
    }

    public SimpleName getSimpleName() {
    	//return this.simpleName;
    	return (SimpleName)this.simpleName.recoverASTNode();
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if (o instanceof FieldInstructionObject) {
            FieldInstructionObject fio = (FieldInstructionObject)o;
            return this.ownerClass.equals(fio.ownerClass) && this.name.equals(fio.name) && this.type.equals(fio.type) &&
            		this.variableBindingKey.equals(fio.variableBindingKey);
        }
        return false;
    }

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + ownerClass.hashCode();
    		result = 37*result + name.hashCode();
    		result = 37*result + type.hashCode();
    		result = 37*result + variableBindingKey.hashCode();
    		hashCode = result;
    	}
    	return hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ownerClass).append("::");
        //sb.append(type).append(" ");
        sb.append(name);
        return sb.toString();
    }
}
