package gr.uom.java.ast;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;

public class ClassInstanceCreationObject extends CreationObject {

	private List<TypeObject> parameterList;
	private Set<String> thrownExceptions;
	
	public ClassInstanceCreationObject(TypeObject type) {
		super(type);
		this.parameterList = new ArrayList<TypeObject>();
		this.thrownExceptions = new LinkedHashSet<String>();
	}

	public ClassInstanceCreation getClassInstanceCreation() {
		return (ClassInstanceCreation)this.creation.recoverASTNode();
	}

	public void setClassInstanceCreation(ClassInstanceCreation creation) {
		this.creation = ASTInformationGenerator.generateASTInformation(creation);
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

    public void addThrownException(String type) {
    	thrownExceptions.add(type);
    }

    public Set<String> getThrownExceptions() {
    	return this.thrownExceptions;
    }

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("new ");
		sb.append(getType().toString());
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
