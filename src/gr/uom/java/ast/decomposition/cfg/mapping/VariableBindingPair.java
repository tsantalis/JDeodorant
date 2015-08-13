package gr.uom.java.ast.decomposition.cfg.mapping;

import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import gr.uom.java.jdeodorant.refactoring.manipulators.RefactoringUtility;

public class VariableBindingPair {
	private IVariableBinding binding1;
	private IVariableBinding binding2;
	private boolean hasQualifiedType = false;
	
	public VariableBindingPair(IVariableBinding binding1, IVariableBinding binding2) {
		this.binding1 = binding1;
		this.binding2 = binding2;
	}
	
	public VariableBindingPair(IVariableBinding binding1, IVariableBinding binding2, VariableDeclaration variableDeclaration) {
		this(binding1, binding2);
		this.hasQualifiedType = RefactoringUtility.hasQualifiedType(variableDeclaration);
	}

	public IVariableBinding getBinding1() {
		return binding1;
	}

	public IVariableBinding getBinding2() {
		return binding2;
	}

	public boolean hasQualifiedType() {
		return hasQualifiedType;
	}

	public VariableBindingKeyPair getVariableBindingKeyPair() {
		return new VariableBindingKeyPair(binding1.getKey(), binding2.getKey());
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof VariableBindingPair) {
			VariableBindingPair keyPair = (VariableBindingPair)o;
			return this.binding1.getKey().equals(keyPair.binding1.getKey()) &&
					this.binding2.getKey().equals(keyPair.binding2.getKey());
		}
		return false;
	}

	public int hashCode() {
		int result = 17;
		result = 37*result + binding1.getKey().hashCode();
		result = 37*result + binding2.getKey().hashCode();
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
