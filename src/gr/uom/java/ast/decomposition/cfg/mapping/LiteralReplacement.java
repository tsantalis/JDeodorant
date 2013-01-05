package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.LiteralObject;

public class LiteralReplacement extends Replacement {
	private LiteralObject l1;
	private LiteralObject l2;
	
	public LiteralReplacement(LiteralObject l1, LiteralObject l2,
			int statementStartPosition1, int statementStartPosition2) {
		super(statementStartPosition1, statementStartPosition2);
		this.l1 = l1;
		this.l2 = l2;
	}

	public String getValue1() {
		return l1.getValue();
	}

	public String getValue2() {
		return l2.getValue();
	}

	public int getLength1() {
		return l1.getValue().length();
	}

	public int getLength2() {
		return l2.getValue().length();
	}

	public String toString() {
		return l1 + " -> " + l2;
	}
}
