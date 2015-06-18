package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ITypeBinding;

public abstract class Gap {
	private Set<VariableBindingPair> parameterBindings;
	
	public Gap() {
		this.parameterBindings = new LinkedHashSet<VariableBindingPair>();
	}

	public Set<VariableBindingPair> getParameterBindings() {
		return parameterBindings;
	}

	public void addParameterBinding(VariableBindingPair parameterBinding) {
		this.parameterBindings.add(parameterBinding);
	}

	public abstract ITypeBinding getReturnType();
}
