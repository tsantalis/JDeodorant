package gr.uom.java.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Type;

public class ClassInstanceCreationObject implements CreationObject {

	private TypeObject type;
	private List<TypeObject> parameterList;
	private ASTInformation classInstanceCreation;
	
	public ClassInstanceCreationObject(TypeObject type) {
		this.type = type;
		this.parameterList = new ArrayList<TypeObject>();
	}

	public ClassInstanceCreation getClassInstanceCreation() {
		return (ClassInstanceCreation)this.classInstanceCreation.recoverASTNode();
	}

	public void setClassInstanceCreation(ClassInstanceCreation creation) {
		this.classInstanceCreation = ASTInformationGenerator.generateASTInformation(creation);
	}

    public boolean addParameter(TypeObject parameterType) {
        return parameterList.add(parameterType);
    }

    public ListIterator<TypeObject> getParameterListIterator() {
        return parameterList.listIterator();
    }
    
    public List<TypeObject> getParameterTypeList() {
    	return this.parameterList;
    }

    public List<String> getParameterList() {
    	List<String> list = new ArrayList<String>();
    	for(TypeObject typeObject : parameterList)
    		list.add(typeObject.toString());
    	return list;
    }

	public TypeObject getType() {
		return type;
	}

	public Type getASTType() {
		return getClassInstanceCreation().getType();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("new ");
		sb.append(type.toString());
		sb.append("(");
		if(!parameterList.isEmpty()) {
			for(int i=0; i<parameterList.size()-1; i++)
				sb.append(parameterList.get(i)).append(", ");
			sb.append(parameterList.get(parameterList.size()-1));
		}
		sb.append(")");
		return sb.toString();
	}
}
