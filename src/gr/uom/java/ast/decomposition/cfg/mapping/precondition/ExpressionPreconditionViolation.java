package gr.uom.java.ast.decomposition.cfg.mapping.precondition;

import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;

import org.eclipse.jdt.core.dom.Expression;
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
		expression = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression);
		sb.append(expression.toString());
		sb.append(" ");
		sb.append(type.toString());
		return sb.toString();
	}

	public StyledString getStyledViolation() {
		BoldStyler boldStyler = new BoldStyler();
		NormalStyler normalStyler = new NormalStyler();
		StyledString styledString = new StyledString();
		styledString.append("Expression ", normalStyler);
		Expression expression = this.expression.getExpression();
		expression = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression);
		styledString.append(expression.toString(), boldStyler);
		styledString.append(" ", normalStyler);
		styledString.append(type.toString(), normalStyler);
		return styledString;
	}
}
