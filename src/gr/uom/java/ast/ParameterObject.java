package gr.uom.java.ast;

import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

public class ParameterObject {
	private TypeObject type;
	private String name;
	private SingleVariableDeclaration singleVariableDeclaration;

	public ParameterObject(TypeObject type, String name) {
		this.type = type;
		this.name = name;
	}

	public TypeObject getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public void setSingleVariableDeclaration(SingleVariableDeclaration singleVariableDeclaration) {
		this.singleVariableDeclaration = singleVariableDeclaration;
	}

	public SingleVariableDeclaration getSingleVariableDeclaration() {
		return this.singleVariableDeclaration;
	}

	public boolean equals(Object o) {
		if(this == o) {
            return true;
        }

        if (o instanceof ParameterObject) {
            ParameterObject parameterObject = (ParameterObject)o;
            return this.type.equals(parameterObject.type) && this.name.equals(parameterObject.name);
        }
        
        return false;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(type.toString()).append(" ");
		sb.append(name);
		return sb.toString();
	}
}
