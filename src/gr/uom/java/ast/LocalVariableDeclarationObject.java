package gr.uom.java.ast;

public class LocalVariableDeclarationObject {
	private TypeObject type;
    private String name;

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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(" ");
        sb.append(name);
        return sb.toString();
    }
}
