package gr.uom.java.ast.decomposition;

import gr.uom.java.ast.util.ExpressionExtractor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;

public class BindingSignature {

	private List<String> bindingKeys;
	
	public BindingSignature(AbstractExpression expression) {
		this.bindingKeys = new ArrayList<String>();
		Expression expr = expression.getExpression();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		if(isMethodName(expr)) {
			expr = (Expression)expr.getParent();
		}
		List<Expression> variableInstructions = expressionExtractor.getVariableInstructions(expr);
		for(Expression variableInstruction : variableInstructions) {
			SimpleName simpleName = (SimpleName)variableInstruction;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null) {
				bindingKeys.add(binding.getKey());
			}
		}
		List<Expression> literals = expressionExtractor.getLiterals(expr);
		for(Expression literal : literals) {
			bindingKeys.add(literal.toString());
		}
		if(bindingKeys.isEmpty())
			bindingKeys.add(expr.toString());
	}

	public boolean containsBinding(String key) {
		return bindingKeys.contains(key);
	}

	private boolean isMethodName(Expression expression) {
		if(expression instanceof SimpleName) {
			SimpleName simpleName = (SimpleName)expression;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null && binding.getKind() == IBinding.METHOD) {
				if(expression.getParent() instanceof Expression) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof BindingSignature) {
			BindingSignature signature = (BindingSignature)o;
			return this.bindingKeys.equals(signature.bindingKeys);
		}
		return false;
	}

	public int hashCode() {
		return bindingKeys.hashCode();
	}

	public String toString() {
		return bindingKeys.toString();
	}
}
