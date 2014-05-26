package gr.uom.java.ast.decomposition.cfg.mapping;

public enum CloneType {
	TYPE_1("Type 1"), 
	TYPE_2("Type 2"),
	TYPE_3("Type 3"),
	UNKNOWN("Unknown type");
	
	private String typeString;
	private CloneType(String typeString) {
		this.typeString = typeString;
	}
	
	@Override
	public String toString() {
		return this.typeString;
	}
}
