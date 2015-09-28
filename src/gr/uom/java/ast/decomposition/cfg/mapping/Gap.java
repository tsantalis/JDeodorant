package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;

public abstract class Gap {
	private Set<VariableBindingPair> parameterBindings;
	private Set<VariableBindingPair> nonEffectivelyFinalLocalVariableBindings;
	
	public Gap() {
		this.parameterBindings = new LinkedHashSet<VariableBindingPair>();
		this.nonEffectivelyFinalLocalVariableBindings = new LinkedHashSet<VariableBindingPair>();
	}

	public Set<VariableBindingPair> getParameterBindings() {
		return parameterBindings;
	}

	public void addParameterBinding(VariableBindingPair parameterBinding) {
		this.parameterBindings.add(parameterBinding);
	}

	public Set<VariableBindingPair> getNonEffectivelyFinalLocalVariableBindings() {
		return nonEffectivelyFinalLocalVariableBindings;
	}

	public void addNonEffectivelyFinalLocalVariableBinding(VariableBindingPair localVariableBinding) {
		this.nonEffectivelyFinalLocalVariableBindings.add(localVariableBinding);
	}

	protected void addTypeBinding(ITypeBinding typeBinding, Set<ITypeBinding> thrownExceptionTypeBindings) {
		boolean found = false;
		for(ITypeBinding thrownExceptionTypeBinding : thrownExceptionTypeBindings) {
			if(typeBinding.isEqualTo(thrownExceptionTypeBinding)) {
				found = true;
				break;
			}
		}
		//unchecked exceptions are ignored
		if(!found && !typeBinding.getSuperclass().getQualifiedName().equals("java.lang.RuntimeException")) {
			thrownExceptionTypeBindings.add(typeBinding);
		}
	}

	public abstract ITypeBinding getReturnType();
	public abstract Set<ITypeBinding> getThrownExceptions();

	protected boolean variableDefinedInNodes(Set<PDGNode> nodes, IVariableBinding binding) {
		for(PDGNode node : nodes) {
			Iterator<AbstractVariable> declaredVariableIterator = node.getDefinedVariableIterator();
			while(declaredVariableIterator.hasNext()) {
				AbstractVariable variable = declaredVariableIterator.next();
				if(variable instanceof PlainVariable) {
					PlainVariable plainVariable = (PlainVariable)variable;
					if(plainVariable.getVariableBindingKey().equals(binding.getKey())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	protected boolean variableUsedInNodes(Set<PDGNode> nodes, IVariableBinding binding) {
		for(PDGNode node : nodes) {
			Iterator<AbstractVariable> declaredVariableIterator = node.getUsedVariableIterator();
			while(declaredVariableIterator.hasNext()) {
				AbstractVariable variable = declaredVariableIterator.next();
				if(variable instanceof PlainVariable) {
					PlainVariable plainVariable = (PlainVariable)variable;
					if(plainVariable.getVariableBindingKey().equals(binding.getKey())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	protected boolean variableDeclaredInNodes(Set<PDGNode> nodes, IVariableBinding binding) {
		for(PDGNode node : nodes) {
			Iterator<AbstractVariable> declaredVariableIterator = node.getDeclaredVariableIterator();
			while(declaredVariableIterator.hasNext()) {
				AbstractVariable variable = declaredVariableIterator.next();
				if(variable instanceof PlainVariable) {
					PlainVariable plainVariable = (PlainVariable)variable;
					if(plainVariable.getVariableBindingKey().equals(binding.getKey())) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
