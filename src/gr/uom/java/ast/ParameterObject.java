package gr.uom.java.ast;

import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class ParameterObject extends VariableDeclarationObject {
	private TypeObject type;
	private String name;
	private boolean varargs;
	//private SingleVariableDeclaration singleVariableDeclaration;
	private ASTInformation singleVariableDeclaration;
	private volatile int hashCode = 0;

	public ParameterObject(TypeObject type, String name, boolean varargs) {
		this.type = type;
		this.name = name;
		this.varargs = varargs;
	}

	public TypeObject getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public boolean isVarargs() {
		return varargs;
	}

	public void setSingleVariableDeclaration(SingleVariableDeclaration singleVariableDeclaration) {
		//this.singleVariableDeclaration = singleVariableDeclaration;
		this.variableBindingKey = singleVariableDeclaration.resolveBinding().getKey();
		this.singleVariableDeclaration = ASTInformationGenerator.generateASTInformation(singleVariableDeclaration);
	}

	public SingleVariableDeclaration getSingleVariableDeclaration() {
		//return this.singleVariableDeclaration;
		return (SingleVariableDeclaration)this.singleVariableDeclaration.recoverASTNode();
	}

	public boolean equals(Object o) {
		if(this == o) {
            return true;
        }

        if (o instanceof ParameterObject) {
            ParameterObject parameterObject = (ParameterObject)o;
            return this.type.equals(parameterObject.type) && this.name.equals(parameterObject.name) &&
            		this.varargs == parameterObject.varargs && this.variableBindingKey.equals(parameterObject.variableBindingKey);
        }
        
        return false;
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 37*result + name.hashCode();
			result = 37*result + type.hashCode();
			result = 37*result + (varargs ? 1 : 0);
			result = 37*result + variableBindingKey.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(type.toString()).append(" ");
		sb.append(name);
		return sb.toString();
	}

	public VariableDeclaration getVariableDeclaration() {
		return getSingleVariableDeclaration();
	}
}
