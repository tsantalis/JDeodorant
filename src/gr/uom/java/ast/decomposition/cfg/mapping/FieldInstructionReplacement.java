package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.FieldInstructionObject;

public class FieldInstructionReplacement implements Replacement {
	private FieldInstructionObject f1;
	private FieldInstructionObject f2;
	
	public FieldInstructionReplacement(FieldInstructionObject f1, FieldInstructionObject f2) {
		this.f1 = f1;
		this.f2 = f2;
	}

	public String getValue1() {
		return f1.getName();
	}

	public String getValue2() {
		return f2.getName();
	}

	public String toString() {
		return f1 + " -> " + f2;
	}
}
