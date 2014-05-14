package gr.uom.java.ast.decomposition.cfg.mapping.precondition;

import java.util.Set;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jface.viewers.StyledString;

import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;

public class DualExpressionWithCommonSuperTypePreconditionViolation extends DualExpressionPreconditionViolation {

	private String commonSuperType;
	private Set<String> commonSuperTypeMembers;
	public DualExpressionWithCommonSuperTypePreconditionViolation(
			AbstractExpression expression1, AbstractExpression expression2, PreconditionViolationType type,
			String commonSuperType, Set<String> commonSuperTypeMembers) {
		super(expression1, expression2, type);
		this.commonSuperType = commonSuperType;
		this.commonSuperTypeMembers = commonSuperTypeMembers;
	}

	public String getViolation() {
		StringBuilder sb = new StringBuilder();
		if(type.equals(PreconditionViolationType.INFEASIBLE_UNIFICATION_DUE_TO_MISSING_MEMBERS_IN_THE_COMMON_SUPERCLASS)) {
			Expression expression1 = this.getExpression1().getExpression();
			expression1 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression1);
			Expression expression2 = this.getExpression2().getExpression();
			expression2 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression2);
			sb.append("Expression ");
			sb.append(expression1.toString());
			sb.append(" cannot be unified with ");
			sb.append("expression ");
			sb.append(expression2.toString());
			sb.append(" , because common superclass ");
			sb.append(commonSuperType);
			sb.append(" does not declare member(s) ");
			sb.append(commonSuperTypeMembers);
		}
		return sb.toString();
	}

	public StyledString getStyledViolation() {
		StyledString styledString = new StyledString();
		BoldStyler styler = new BoldStyler();
		if(type.equals(PreconditionViolationType.INFEASIBLE_UNIFICATION_DUE_TO_MISSING_MEMBERS_IN_THE_COMMON_SUPERCLASS)) {
			Expression expression1 = this.getExpression1().getExpression();
			expression1 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression1);
			Expression expression2 = this.getExpression2().getExpression();
			expression2 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression2);
			styledString.append("Expression ");
			styledString.append(expression1.toString(), styler);
			styledString.append(" cannot be unified with ");
			styledString.append("expression ");
			styledString.append(expression2.toString(), styler);
			styledString.append(" , because common superclass ");
			styledString.append(commonSuperType, styler);
			styledString.append(" does not declare member(s) ");
			int counter = 1;
			for(String commonSuperTypeMember : commonSuperTypeMembers) {
				styledString.append(commonSuperTypeMember, styler);
				if(counter < commonSuperTypeMembers.size())
					styledString.append(", ");
				counter++;
			}
		}
		return styledString;
	}
}
