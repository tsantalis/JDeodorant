package gr.uom.java.ast;

public class TypeObject {
    private String classType;
    private String genericType;
    private int arrayDimension;
    private volatile int hashCode = 0;

    public TypeObject(String type) {
        this.classType = type;
    }

    public String getClassType() {
        return classType;
    }

    public String getGenericType() {
        return genericType;
    }

    public void setGeneric(String g) {
        this.genericType = g;
    }
    
    public void setArrayDimension(int dimension) {
    	this.arrayDimension = dimension;
    }
    
    public int getArrayDimension() {
    	return this.arrayDimension;
    }
    
    public boolean containsGeneticType(String s) {
        if(genericType != null)
            return genericType.contains(s);
        else
            return false;
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if (o instanceof TypeObject) {
            TypeObject typeObject = (TypeObject)o;

            if(this.classType.equals(typeObject.classType)) {
                if(this.genericType == null && typeObject.genericType == null)
                    return this.arrayDimension == typeObject.arrayDimension;
                else if(this.genericType != null && typeObject.genericType != null)
                    return this.genericType.equals(typeObject.genericType) && this.arrayDimension == typeObject.arrayDimension;
            }
        }
        return false;
    }

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + classType.hashCode();
    		if(genericType != null)
    			result = 37*result + genericType.hashCode();
    		result = 37*result + arrayDimension;
    		hashCode = result;
    	}
    	return hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(classType);
        if(genericType != null)
            sb.append(genericType);
        for(int i=0; i<arrayDimension; i++)
        	sb.append("[]");
        return sb.toString();
    }
}
