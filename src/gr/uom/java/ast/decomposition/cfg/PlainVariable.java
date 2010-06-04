package gr.uom.java.ast.decomposition.cfg;

import org.eclipse.jdt.core.dom.VariableDeclaration;

public class PlainVariable extends AbstractVariable {
	private volatile int hashCode = 0;
	
	public PlainVariable(VariableDeclaration variableName) {
		super(variableName);
	}

	public PlainVariable(String variableBindingKey, String variableName, String variableType, boolean isField, boolean isParameter) {
		super(variableBindingKey, variableName, variableType, isField, isParameter);
	}

	public boolean containsPlainVariable(PlainVariable variable) {
		return this.variableBindingKey.equals(variable.variableBindingKey);
	}

	public PlainVariable getInitialVariable() {
		return this;
	}

	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(o instanceof PlainVariable) {
			PlainVariable plain = (PlainVariable)o;
			return this.variableBindingKey.equals(plain.variableBindingKey);
		}
		return false;
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 31*result + variableBindingKey.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(variableName);
		return sb.toString();
	}
}
