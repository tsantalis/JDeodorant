package gr.uom.java.ast.decomposition;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;

public class PreconditionViolation {
	private AbstractExpression expression;
	private PreconditionViolationType type;
	
	public PreconditionViolation(AbstractExpression expression, PreconditionViolationType type) {
		this.expression = expression;
		this.type = type;
	}
	
	public String getViolation() {
		StringBuilder sb = new StringBuilder();
		sb.append("Expression ");
		Expression expression = this.expression.getExpression();
		String expressionToString = isMethodName(expression) ? expression.getParent().toString() : expression.toString();
		sb.append(expressionToString);
		sb.append(" ");
		sb.append(type.toString());
		return sb.toString();
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
}
