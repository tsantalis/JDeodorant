package gr.uom.java.ast.decomposition.cfg.mapping.precondition;

import gr.uom.java.ast.decomposition.AbstractExpression;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jface.viewers.StyledString;

public class ExpressionPreconditionViolation extends PreconditionViolation {
	private AbstractExpression expression;
	
	public ExpressionPreconditionViolation(AbstractExpression expression, PreconditionViolationType type) {
		super(type);
		this.expression = expression;
	}

	public AbstractExpression getExpression() {
		return expression;
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

	public StyledString getStyledViolation() {
		StyledString styledString = new StyledString();
		styledString.append("Expression ");
		Expression expression = this.expression.getExpression();
		String expressionToString = isMethodName(expression) ? expression.getParent().toString() : expression.toString();
		BoldStyler styler = new BoldStyler();
		styledString.append(expressionToString, styler);
		styledString.append(" ");
		styledString.append(type.toString());
		return styledString;
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
