package gr.uom.java.ast.decomposition.matching;

import gr.uom.java.ast.decomposition.AbstractExpression;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Expression;

public class BindingSignature {

	private List<String> bindingKeys;
	
	public BindingSignature(AbstractExpression expression) {
		if(expression != null) {
			Expression expr = expression.getExpression();
			expr = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expr);
			BindingSignatureVisitor visitor = new BindingSignatureVisitor();
			expr.accept(visitor);
			this.bindingKeys = visitor.getBindingKeys();
			if(bindingKeys.isEmpty())
				bindingKeys.add(expr.toString());
		}
		else {
			this.bindingKeys = new ArrayList<String>();
			bindingKeys.add("this");
		}
	}

	public int getOccurrences(String key) {
		int counter = 0;
		for(String bindingKey : bindingKeys) {
			if(bindingKey.equals(key)) {
				counter++;
			}
		}
		return counter;
	}

	public boolean containsBinding(String key) {
		return bindingKeys.contains(key);
	}

	public boolean containsOnlyBinding(String key) {
		return bindingKeys.size() == 1 && bindingKeys.contains(key);
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
