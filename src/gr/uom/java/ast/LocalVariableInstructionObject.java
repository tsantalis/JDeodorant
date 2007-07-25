package gr.uom.java.ast;

public class LocalVariableInstructionObject {
	private TypeObject type;
    private String name;

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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(" ");
        sb.append(name);
        return sb.toString();
    }
}
