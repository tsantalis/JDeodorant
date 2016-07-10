package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.util.ExpressionExtractor;

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
	public abstract Set<IMethodBinding> getAllMethodsInvokedThroughVariable(VariableBindingPair variableBindingPair);

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

	protected Set<IMethodBinding> getAllMethodsInvokedThroughVariable(AbstractExpression expression, IVariableBinding variableBinding) {
		Set<IMethodBinding> methods = new LinkedHashSet<IMethodBinding>();
		Expression expr = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression.getExpression());
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(expr);
		for(Expression e : methodInvocations) {
			if(e instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)e;
				Expression methodInvocationExpression = methodInvocation.getExpression();
				if(methodInvocationExpression != null && methodInvocationExpression instanceof SimpleName) {
					SimpleName simpleName = (SimpleName)methodInvocationExpression;
					if(simpleName.resolveBinding().isEqualTo(variableBinding)) {
						methods.add(methodInvocation.resolveMethodBinding());
					}
				}
			}
		}
		return methods;
	}

	protected Set<IMethodBinding> getAllMethodsInvokedThroughVariable(AbstractStatement statement, IVariableBinding variableBinding) {
		Set<IMethodBinding> methods = new LinkedHashSet<IMethodBinding>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(statement.getStatement());
		for(Expression e : methodInvocations) {
			if(e instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)e;
				Expression methodInvocationExpression = methodInvocation.getExpression();
				if(methodInvocationExpression != null && methodInvocationExpression instanceof SimpleName) {
					SimpleName simpleName = (SimpleName)methodInvocationExpression;
					if(simpleName.resolveBinding().isEqualTo(variableBinding)) {
						methods.add(methodInvocation.resolveMethodBinding());
					}
				}
			}
		}
		return methods;
	}
}
