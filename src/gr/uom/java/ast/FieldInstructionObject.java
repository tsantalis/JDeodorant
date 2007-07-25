package gr.uom.java.ast;

import org.eclipse.jdt.core.dom.SimpleName;

public class FieldInstructionObject {

    private String ownerClass;
    private TypeObject type;
    private String name;
    private boolean _static;
    private SimpleName simpleName;

    public FieldInstructionObject(String ownerClass, TypeObject type, String name) {
        this.ownerClass = ownerClass;
        this.type = type;
        this.name = name;
        this._static = false;
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

    public boolean isStatic() {
        return _static;
    }

    public void setStatic(boolean s) {
        _static = s;
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

        if (o instanceof FieldInstructionObject) {
            FieldInstructionObject fio = (FieldInstructionObject)o;
            return this.ownerClass.equals(fio.ownerClass) && this.name.equals(fio.name) && this.type.equals(fio.type);
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ownerClass).append("::");
        //sb.append(type).append(" ");
        sb.append(name);
        return sb.toString();
    }
}
