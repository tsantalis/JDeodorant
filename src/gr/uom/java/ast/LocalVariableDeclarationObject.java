package gr.uom.java.ast;

import org.eclipse.jdt.core.dom.VariableDeclaration;

public class LocalVariableDeclarationObject {
	private TypeObject type;
    private String name;
    private VariableDeclaration variableDeclaration;
    private volatile int hashCode = 0;

    public LocalVariableDeclarationObject(TypeObject type, String name) {
        this.type = type;
        this.name = name;
    }

    public TypeObject getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public VariableDeclaration getVariableDeclaration() {
		return variableDeclaration;
	}

	public void setVariableDeclaration(VariableDeclaration variableDeclaration) {
		this.variableDeclaration = variableDeclaration;
	}

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if (o instanceof LocalVariableDeclarationObject) {
        	LocalVariableDeclarationObject lvdo = (LocalVariableDeclarationObject)o;
            return this.name.equals(lvdo.name) && this.type.equals(lvdo.type);
        }
        else if(o instanceof LocalVariableInstructionObject) {
        	LocalVariableInstructionObject lvio = (LocalVariableInstructionObject)o;
            return this.name.equals(lvio.getName()) && this.type.equals(lvio.getType());
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
