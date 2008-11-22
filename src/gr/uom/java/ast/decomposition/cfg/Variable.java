package gr.uom.java.ast.decomposition.cfg;

import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class Variable {
	private VariableDeclaration reference;
	private VariableDeclaration variable;
	private volatile int hashCode = 0;
	
	public Variable(VariableDeclaration reference, VariableDeclaration variable) {
		this.reference = reference;
		this.variable = variable;
	}

	public Variable(VariableDeclaration variable) {
		this.variable = variable;
	}

	public VariableDeclaration getReference() {
		return reference;
	}

	public VariableDeclaration getVariable() {
		return variable;
	}

	public boolean isLocalVariable() {
		IVariableBinding variableBinding = variable.resolveBinding();
		if(reference == null && !variableBinding.isField())
			return true;
		return false;
	}

	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(o instanceof Variable) {
			Variable v = (Variable)o;
			if(this.reference == null && v.reference == null)
				return this.variable.equals(v.variable);
			else if(this.reference != null && v.reference == null)
				return false;
			else if(this.reference == null && v.reference != null)
				return false;
			else if(this.reference != null && v.reference != null)
				return this.reference.equals(v.reference) && this.variable.equals(v.variable);
		}
		return false;
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			if(reference != null)
				result = 31*result + reference.hashCode();
			result = 31*result + variable.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(reference != null) {
			sb.append(reference.getName().getIdentifier());
			sb.append(".");
		}
		sb.append(variable.getName().getIdentifier());
		return sb.toString();
	}
}
