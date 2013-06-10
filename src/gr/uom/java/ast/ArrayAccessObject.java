package gr.uom.java.ast;

import org.eclipse.jdt.core.dom.ArrayAccess;

public class ArrayAccessObject {
	private TypeObject type;
	private ASTInformation arrayAccess;
	
	public ArrayAccessObject(TypeObject type) {
		this.type = type;
	}
	
	public TypeObject getType() {
		return type;
	}

	public ArrayAccess getArrayAccess() {
		return (ArrayAccess)this.arrayAccess.recoverASTNode();
	}

	public void setArrayAccess(ArrayAccess arrayAccess) {
		this.arrayAccess = ASTInformationGenerator.generateASTInformation(arrayAccess);
	}
}
