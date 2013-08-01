package gr.uom.java.ast.decomposition;

import org.eclipse.jdt.core.dom.Expression;

public class DualExpressionPreconditionViolation extends PreconditionViolation {
	private AbstractExpression expression1;
	private AbstractExpression expression2;
	
	public DualExpressionPreconditionViolation(AbstractExpression expression1, AbstractExpression expression2, PreconditionViolationType type) {
		super(type);
		this.expression1 = expression1;
		this.expression2 = expression2;
	}

	public String getViolation() {
		if(type.equals(PreconditionViolationType.INFEASIBLE_UNIFICATION_DUE_TO_VARIABLE_TYPE_MISMATCH)) {
			Expression expression1 = this.expression1.getExpression();
			Expression expression2 = this.expression2.getExpression();
			StringBuilder sb = new StringBuilder();
			sb.append("Type ");
			sb.append(expression1.resolveTypeBinding().getQualifiedName());
			sb.append(" of variable ");
			sb.append(expression1.toString());
			sb.append(" does not match with ");
			sb.append("type ");
			sb.append(expression2.resolveTypeBinding().getQualifiedName());
			sb.append(" of variable ");
			sb.append(expression2.toString());
		}
		return "";
	}
}
