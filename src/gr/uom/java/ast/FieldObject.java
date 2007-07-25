package gr.uom.java.ast;

import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class FieldObject {

    private String name;
    private TypeObject type;
    private boolean _static;
    private Access access;
    private VariableDeclarationFragment fragment;

    public FieldObject(TypeObject type, String name) {
        this.type = type;
        this.name = name;
        this._static = false;
        this.access = Access.NONE;
    }

    public void setVariableDeclarationFragment(VariableDeclarationFragment fragment) {
    	this.fragment = fragment;
    }

    public VariableDeclarationFragment getVariableDeclarationFragment() {
    	return this.fragment;
    }

    public void setAccess(Access access) {
        this.access = access;
    }

    public Access getAccess() {
        return access;
    }

    public String getName() {
        return name;
    }

    public TypeObject getType() {
        return type;
    }

    public boolean isStatic() {
        return _static;
    }

    public void setStatic(boolean s) {
        _static = s;
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if (o instanceof FieldObject) {
            FieldObject fieldObject = (FieldObject)o;
            return this.name.equals(fieldObject.name) && this.type.equals(fieldObject.type);
        }
        return false;
    }

    public boolean equals(FieldInstructionObject fio) {
        return this.name.equals(fio.getName()) && this.type.equals(fio.getType());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(!access.equals(Access.NONE))
            sb.append(access.toString()).append(" ");
        if(_static)
            sb.append("static").append(" ");
        sb.append(type.toString()).append(" ");
        sb.append(name);
        return sb.toString();
    }
}