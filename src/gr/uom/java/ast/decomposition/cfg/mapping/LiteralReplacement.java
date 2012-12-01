package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.LiteralObject;

public class LiteralReplacement implements Replacement {
	private LiteralObject l1;
	private LiteralObject l2;
	
	public LiteralReplacement(LiteralObject l1, LiteralObject l2) {
		this.l1 = l1;
		this.l2 = l2;
	}

	public String getValue1() {
		return l1.getValue();
	}

	public String getValue2() {
		return l2.getValue();
	}

	public String toString() {
		return l1 + " -> " + l2;
	}
}
