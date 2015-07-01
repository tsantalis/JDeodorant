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
		else if(type.equals(PreconditionViolationType.INFEASIBLE_UNIFICATION_DUE_TO_PASSED_ARGUMENT_TYPE_MISMATCH)) {
			Expression expression1 = this.getExpression1().getExpression();
			expression1 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression1);
			Expression expression2 = this.getExpression2().getExpression();
			expression2 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression2);
			sb.append("Expression ");
			sb.append(expression1.toString());
			sb.append(" cannot be unified with ");
			sb.append("expression ");
			sb.append(expression2.toString());
			sb.append(" , because common superclass type ");
			sb.append(commonSuperType);
			sb.append(" cannot be passed as an argument to ");
			sb.append(commonSuperTypeMembers);
		}
		return sb.toString();
	}

	public StyledString getStyledViolation() {
		StyledString styledString = new StyledString();
		BoldStyler boldStyler = new BoldStyler();
		NormalStyler normalStyler = new NormalStyler();
		if(type.equals(PreconditionViolationType.INFEASIBLE_UNIFICATION_DUE_TO_MISSING_MEMBERS_IN_THE_COMMON_SUPERCLASS)) {
			Expression expression1 = this.getExpression1().getExpression();
			expression1 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression1);
			Expression expression2 = this.getExpression2().getExpression();
			expression2 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression2);
			styledString.append("Expression ", normalStyler);
			styledString.append(expression1.toString(), boldStyler);
			styledString.append(" cannot be unified with ", normalStyler);
			styledString.append("expression ", normalStyler);
			styledString.append(expression2.toString(), boldStyler);
			styledString.append(" , because common superclass ", normalStyler);
			styledString.append(commonSuperType, boldStyler);
			styledString.append(" does not declare member(s) ", normalStyler);
			int counter = 1;
			for(String commonSuperTypeMember : commonSuperTypeMembers) {
				styledString.append(commonSuperTypeMember, boldStyler);
				if(counter < commonSuperTypeMembers.size())
					styledString.append(", ", normalStyler);
				counter++;
			}
		}
		else if(type.equals(PreconditionViolationType.INFEASIBLE_UNIFICATION_DUE_TO_PASSED_ARGUMENT_TYPE_MISMATCH)) {
			Expression expression1 = this.getExpression1().getExpression();
			expression1 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression1);
			Expression expression2 = this.getExpression2().getExpression();
			expression2 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression2);
			styledString.append("Expression ", normalStyler);
			styledString.append(expression1.toString(), boldStyler);
			styledString.append(" cannot be unified with ", normalStyler);
			styledString.append("expression ", normalStyler);
			styledString.append(expression2.toString(), boldStyler);
			styledString.append(" , because common superclass type ", normalStyler);
			styledString.append(commonSuperType, boldStyler);
			styledString.append(" cannot be passed as an argument to ", normalStyler);
			int counter = 1;
			for(String commonSuperTypeMember : commonSuperTypeMembers) {
				styledString.append(commonSuperTypeMember, boldStyler);
				if(counter < commonSuperTypeMembers.size())
					styledString.append(", ", normalStyler);
				counter++;
			}
		}
		return styledString;
	}
}
