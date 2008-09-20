package gr.uom.java.ast;

import org.eclipse.jdt.core.dom.SimpleName;

public class LocalVariableInstructionObject {
	private TypeObject type;
    private String name;
    private SimpleName simpleName;
    private volatile int hashCode = 0;

    public LocalVariableInstructionObject(TypeObject type, String name) {
        this.type = type;
        this.name = name;
    }

    public TypeObject getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setSimpleName(SimpleName simpleName) {
    	this.simpleName = simpleName;
    }

    public SimpleName getSimpleName() {
    	return this.simpleName;
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if (o instanceof LocalVariableInstructionObject) {
        	LocalVariableInstructionObject lvio = (LocalVariableInstructionObject)o;
            return this.name.equals(lvio.name) && this.type.equals(lvio.type);
        }
        else if(o instanceof LocalVariableDeclarationObject) {
        	LocalVariableDeclarationObject lvdo = (LocalVariableDeclarationObject)o;
            return this.name.equals(lvdo.getName()) && this.type.equals(lvdo.getType());
        }
        return false;
    }

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + type.hashCode();
    		result = 37*result + name.hashCode();
    		hashCode = result;
    	}
    	return hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(" ");
        sb.append(name);
        return sb.toString();
    }
}
