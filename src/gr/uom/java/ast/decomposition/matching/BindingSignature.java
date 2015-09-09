package gr.uom.java.ast.decomposition.matching;

import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.cfg.PDGNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;

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

	public BindingSignature(Set<PDGNode> statements) {
		this.bindingKeys = new ArrayList<String>();
		for(PDGNode node : statements) {
			Statement statement = node.getASTStatement();
			BindingSignatureVisitor visitor = new BindingSignatureVisitor();
			statement.accept(visitor);
			this.bindingKeys.addAll(visitor.getBindingKeys());
		}
	}

	public BindingSignature(List<String> bindingKeys) {
		this.bindingKeys = bindingKeys;
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

	public List<String> signatureWithoutMethods() {
		List<String> keys = new ArrayList<String>();
		for(String key : bindingKeys) {
			if(key.contains("[L")) {
				keys.add(key.substring(key.indexOf("[L"), key.length()));
			}
			else {
				keys.add(key);
			}
		}
		return keys;
	}

	public int getSize() {
		return bindingKeys.size();
	}

	public int getLength() {
		int length = 0;
		for(String key : bindingKeys) {
			length += key.length();
		}
		return length;
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
