package gr.uom.java.ast.decomposition.cfg.mapping.precondition;

import org.eclipse.jface.viewers.StyledString;

public class UncommonSuperclassPreconditionViolation extends PreconditionViolation {

	private String qualifiedType1;
	private String qualifiedType2;
	public UncommonSuperclassPreconditionViolation(String type1, String type2) {
		super(PreconditionViolationType.INFEASIBLE_REFACTORING_DUE_TO_UNCOMMON_SUPERCLASS);
		this.qualifiedType1 = type1;
		this.qualifiedType2 = type2;
	}

	@Override
	public String getViolation() {
		StringBuilder sb = new StringBuilder();
		sb.append("The refactoring of the clones is infeasible, because classes ");
		sb.append(qualifiedType1);
		sb.append(" and ");
		sb.append(qualifiedType2);
		sb.append(" do not have a common superclass");
		return sb.toString();
	}

	@Override
	public StyledString getStyledViolation() {
		StyledString styledString = new StyledString();
		NormalStyler normalStyler = new NormalStyler();
		BoldStyler boldStyler = new BoldStyler();
		styledString.append("The refactoring of the clones is infeasible, because classes ", normalStyler);
		styledString.append(qualifiedType1, boldStyler);
		styledString.append(" and ", normalStyler);
		styledString.append(qualifiedType2, boldStyler);
		styledString.append(" do not have a common superclass", normalStyler);
		return styledString;
	}
}
