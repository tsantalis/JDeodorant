package gr.uom.java.ast.decomposition;

import java.util.List;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;

public class BindingSignature {

	private List<String> bindingKeys;
	
	public BindingSignature(AbstractExpression expression) {
		Expression expr = expression.getExpression();
		if(isMethodName(expr)) {
			expr = (Expression)expr.getParent();
		}
		BindingSignatureVisitor visitor = new BindingSignatureVisitor();
		expr.accept(visitor);
		this.bindingKeys = visitor.getBindingKeys();
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
