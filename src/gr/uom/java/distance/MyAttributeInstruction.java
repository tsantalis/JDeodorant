package gr.uom.java.distance;

public class MyAttributeInstruction {

    private String classOrigin;
    private String classType;
    private String name;
    private boolean reference;

    public MyAttributeInstruction(String classOrigin, String classType, String name) {
        this.classOrigin = classOrigin;
        this.classType = classType;
        this.name = name;
        this.reference = false;
    }

    public boolean isReference() {
        return reference;
    }

    public void setReference(boolean reference) {
        this.reference = reference;
    }
    
    public String getClassOrigin() {
        return classOrigin;
    }

    public void setClassOrigin(String classOrigin) {
        this.classOrigin = classOrigin;
    }

    public String getClassType() {
        return classType;
    }

    public String getName() {
        return name;
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if (o instanceof MyAttributeInstruction) {
            MyAttributeInstruction attribute = (MyAttributeInstruction)o;
            return this.classOrigin.equals(attribute.classOrigin) && this.classType.equals(attribute.classType) &&
                this.name.equals(attribute.name);
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(classOrigin).append("::");
        sb.append(classType).append(" ");
        sb.append(name);
        return sb.toString();
    }
}
