package gr.uom.java.ast;

public abstract class CreationObject {
	private TypeObject type;
	protected ASTInformation creation;
	
	public CreationObject(TypeObject type) {
		this.type = type;
	}

	public TypeObject getType() {
		return type;
	}
}
