package gr.uom.java.ast;

import java.util.ArrayList;
import java.util.List;

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

    public boolean equalsClassType(TypeObject typeObject) {
    	//this case covers type parameter names, such as E, K, N, T, V, S, U
    	if(this.classType.length() == 1 || typeObject.classType.length() == 1)
    		return true;
    	else
    		return this.classType.equals(typeObject.classType);
    }

    public boolean equalsGenericType(TypeObject typeObject) {
    	if(this.genericType == null && typeObject.genericType == null)
    		return true;
    	else if(this.genericType != null && typeObject.genericType != null) {
    		//remove < > , and whitespace 
    		String[] thisTokens = this.genericType.split("<|>|,|\\s");
    		String[] otherTokens = typeObject.genericType.split("<|>|,|\\s");
    		List<String> singleLetters1 = new ArrayList<String>();
    		List<String> words1 = new ArrayList<String>();
    		for(String token : thisTokens) {
    			if(token.length() == 1)
    				singleLetters1.add(token);
    			else if(token.length() > 1)
    				words1.add(token);
    		}
    		List<String> singleLetters2 = new ArrayList<String>();
    		List<String> words2 = new ArrayList<String>();
    		for(String token : otherTokens) {
    			if(token.length() == 1)
    				singleLetters2.add(token);
    			else if(token.length() > 1)
    				words2.add(token);
    		}
    		boolean allLetters1 = singleLetters1.size() > 0 && words1.size() == 0;
    		boolean allLetters2 = singleLetters2.size() > 0 && words2.size() == 0;
    		if(allLetters1 || allLetters2)
    			return true;
    		else
    			return this.genericType.equals(typeObject.genericType);
    	}
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

    public static TypeObject extractTypeObject(String qualifiedName) {
		int arrayDimension = 0;
		String generic = null;
		if(qualifiedName.endsWith("[]")) {
			while(qualifiedName.endsWith("[]")) {
				qualifiedName = qualifiedName.substring(0, qualifiedName.lastIndexOf("[]"));
				arrayDimension++;
			}
		}
		if(qualifiedName.contains("<") && qualifiedName.contains(">")) {
			generic = qualifiedName.substring(qualifiedName.indexOf("<"), qualifiedName.lastIndexOf(">")+1);
			qualifiedName = qualifiedName.substring(0, qualifiedName.indexOf("<"));
		}
		TypeObject typeObject = new TypeObject(qualifiedName);
		typeObject.setGeneric(generic);
		typeObject.setArrayDimension(arrayDimension);
		return typeObject;
	}
}
