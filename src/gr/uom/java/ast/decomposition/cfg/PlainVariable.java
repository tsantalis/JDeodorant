package gr.uom.java.ast.decomposition.cfg;

import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class PlainVariable extends AbstractVariable {
	private volatile int hashCode = 0;
	
	public PlainVariable(VariableDeclaration variableName) {
		super(variableName);
	}

	public boolean isLocalVariable() {
		IVariableBinding variableBinding = name.resolveBinding();
		if(variableBinding.isField())
			return false;
		else
			return true;
	}

	public boolean containsPlainVariable(PlainVariable variable) {
		if(this.name.equals(variable.name) ||
				this.name.resolveBinding().isEqualTo(variable.name.resolveBinding()))
			return true;
		return false;
		/*return this.equals(variable);*/
	}

	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(o instanceof PlainVariable) {
			PlainVariable plain = (PlainVariable)o;
			return this.name.equals(plain.name);
			/*if(this.name.equals(plain.name) ||
					this.name.resolveBinding().isEqualTo(plain.name.resolveBinding()))
			return true;*/
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
		sb.append(name.getName().getIdentifier());
		return sb.toString();
	}
}