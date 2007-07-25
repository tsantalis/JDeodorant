package gr.uom.java.ast.association;

import gr.uom.java.ast.FieldObject;

public class Association {

    private String from;
    private String to;
    private boolean container;
    private FieldObject fieldObject;

    public Association(FieldObject fieldObject, String from, String to) {
        this.fieldObject = fieldObject;
    	this.from = from;
        this.to = to;
        this.container = false;
    }
    
    public FieldObject getFieldObject() {
    	return fieldObject;
    }
    
    public String getTo() {
        return to;
    }

    public String getFrom() {
        return from;
    }

    public boolean isContainer() {
        return container;
    }

    public void setContainer(boolean container) {
        this.container = container;
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if (o instanceof Association) {
            Association association = (Association)o;
            return this.from.equals(association.from) && this.to.equals(association.to) &&
            	this.fieldObject.equals(association.fieldObject) && this.container == association.container;
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(from).append(" -->");
        if(container)
            sb.append("(*) ");
        else
            sb.append("(1) ");
        sb.append(to);
        return sb.toString();
    }
}
