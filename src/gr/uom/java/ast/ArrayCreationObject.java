package gr.uom.java.ast;

import org.eclipse.jdt.core.dom.ArrayCreation;

public class ArrayCreationObject extends CreationObject {

	public ArrayCreationObject(TypeObject type) {
		super(type);
	}

	public ArrayCreation getArrayCreation() {
		return (ArrayCreation)this.creation.recoverASTNode();
	}

	public void setArrayCreation(ArrayCreation creation) {
		this.creation = ASTInformationGenerator.generateASTInformation(creation);
	}
}
