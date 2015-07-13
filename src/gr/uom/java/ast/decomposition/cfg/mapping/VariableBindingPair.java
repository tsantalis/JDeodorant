package gr.uom.java.ast.decomposition.cfg.mapping;

import org.eclipse.jdt.core.dom.IVariableBinding;

public class VariableBindingPair {
	private IVariableBinding binding1;
	private IVariableBinding binding2;
	
	public VariableBindingPair(IVariableBinding binding1, IVariableBinding binding2) {
		this.binding1 = binding1;
		this.binding2 = binding2;
	}

	public IVariableBinding getBinding1() {
		return binding1;
	}

	public IVariableBinding getBinding2() {
		return binding2;
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof VariableBindingPair) {
			VariableBindingPair keyPair = (VariableBindingPair)o;
			return this.binding1.equals(keyPair.binding1) &&
					this.binding2.equals(keyPair.binding2);
		}
		return false;
	}

	public int hashCode() {
		int result = 17;
		result = 37*result + binding1.hashCode();
		result = 37*result + binding2.hashCode();
		return result;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(binding1);
		sb.append("\n");
		sb.append(binding2);
		return sb.toString();
	}
}
