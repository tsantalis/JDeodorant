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

	protected void addTypeBinding(ITypeBinding typeBinding, Set<ITypeBinding> thrownExceptionTypeBindings) {
		boolean found = false;
		for(ITypeBinding thrownExceptionTypeBinding : thrownExceptionTypeBindings) {
			if(typeBinding.isEqualTo(thrownExceptionTypeBinding)) {
				found = true;
				break;
			}
		}
		if(!found) {
			thrownExceptionTypeBindings.add(typeBinding);
		}
	}

	public abstract ITypeBinding getReturnType();
	public abstract Set<ITypeBinding> getThrownExceptions();
}
