package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.VariableDeclarationObject;

import org.eclipse.jdt.core.dom.IVariableBinding;

public class PlainVariable extends AbstractVariable {
	private volatile int hashCode = 0;
	
	public PlainVariable(VariableDeclarationObject variableName) {
		super(variableName);
	}

	public boolean isLocalVariable() {
		IVariableBinding variableBinding = name.getVariableDeclaration().resolveBinding();
		if(variableBinding.isField())
			return false;
		else
			return true;
	}

	public boolean containsPlainVariable(PlainVariable variable) {
		return this.equals(variable);
	}

	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(o instanceof PlainVariable) {
			PlainVariable plain = (PlainVariable)o;
			return this.name.equals(plain.name);
		}
		return false;
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 31*result + name.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name.getName());
		return sb.toString();
	}
}
