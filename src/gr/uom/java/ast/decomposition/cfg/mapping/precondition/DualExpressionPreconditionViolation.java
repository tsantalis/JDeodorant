package gr.uom.java.ast.decomposition.cfg.mapping.precondition;

import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jface.viewers.StyledString;

public class DualExpressionPreconditionViolation extends PreconditionViolation {
	private AbstractExpression expression1;
	private AbstractExpression expression2;
	
	public DualExpressionPreconditionViolation(AbstractExpression expression1, AbstractExpression expression2, PreconditionViolationType type) {
		super(type);
		this.expression1 = expression1;
		this.expression2 = expression2;
	}

	public AbstractExpression getExpression1() {
		return expression1;
	}

	public AbstractExpression getExpression2() {
		return expression2;
	}

	public String getViolation() {
		if(type.equals(PreconditionViolationType.INFEASIBLE_UNIFICATION_DUE_TO_VARIABLE_TYPE_MISMATCH)) {
			Expression expression1 = this.expression1.getExpression();
			Expression expression2 = this.expression2.getExpression();
			if(expression1 instanceof Name && ((Name)expression1).resolveBinding().getKind() == IBinding.TYPE &&
					expression2 instanceof Name && ((Name)expression2).resolveBinding().getKind() == IBinding.TYPE) {
				StringBuilder sb = new StringBuilder();
				sb.append("Type ");
				sb.append(expression1.resolveTypeBinding().getQualifiedName());
				sb.append(" does not match with ");
				sb.append("type ");
				sb.append(expression2.resolveTypeBinding().getQualifiedName());
				return sb.toString();
			}
			else {
				expression1 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression1);
				expression2 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression2);
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
				return sb.toString();
			}
		}
		return "";
	}

	public StyledString getStyledViolation() {
		StyledString styledString = new StyledString();
		BoldStyler boldStyler = new BoldStyler();
		NormalStyler normalStyler = new NormalStyler();
		if(type.equals(PreconditionViolationType.INFEASIBLE_UNIFICATION_DUE_TO_VARIABLE_TYPE_MISMATCH)) {
			Expression expression1 = this.expression1.getExpression();
			Expression expression2 = this.expression2.getExpression();
			if(expression1 instanceof Name && ((Name)expression1).resolveBinding().getKind() == IBinding.TYPE &&
					expression2 instanceof Name && ((Name)expression2).resolveBinding().getKind() == IBinding.TYPE) {
				styledString.append("Type ", normalStyler);
				styledString.append(expression1.resolveTypeBinding().getQualifiedName(), boldStyler);
				styledString.append(" does not match with ", normalStyler);
				styledString.append("type ", normalStyler);
				styledString.append(expression2.resolveTypeBinding().getQualifiedName(), boldStyler);
			}
			else {
				expression1 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression1);
				expression2 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression2);
				styledString.append("Type ", normalStyler);
				styledString.append(expression1.resolveTypeBinding().getQualifiedName(), boldStyler);
				styledString.append(" of variable ", normalStyler);
				styledString.append(expression1.toString(), boldStyler);
				styledString.append(" does not match with ", normalStyler);
				styledString.append("type ", normalStyler);
				styledString.append(expression2.resolveTypeBinding().getQualifiedName(), boldStyler);
				styledString.append(" of variable ", normalStyler);
				styledString.append(expression2.toString(), boldStyler);
			}
		}
		return styledString;
	}
}
