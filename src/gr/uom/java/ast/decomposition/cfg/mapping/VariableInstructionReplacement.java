package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.LocalVariableInstructionObject;

public class VariableInstructionReplacement implements Replacement {
	private LocalVariableInstructionObject v1;
	private LocalVariableInstructionObject v2;
	
	public VariableInstructionReplacement(LocalVariableInstructionObject v1, LocalVariableInstructionObject v2) {
		this.v1 = v1;
		this.v2 = v2;
	}

	public String getValue1() {
		return v1.getName();
	}

	public String getValue2() {
		return v2.getName();
	}

	public String toString() {
		return v1 + " -> " + v2;
	}
}
