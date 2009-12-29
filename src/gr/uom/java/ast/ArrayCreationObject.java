package gr.uom.java.ast;

import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Type;

public class ArrayCreationObject implements CreationObject {

	private TypeObject type;
	private ASTInformation arrayCreation;
	
	public ArrayCreationObject(TypeObject type) {
		this.type = type;
	}

	public ArrayCreation getArrayCreation() {
		return (ArrayCreation)this.arrayCreation.recoverASTNode();
	}

	public void setArrayCreation(ArrayCreation creation) {
		this.arrayCreation = ASTInformationGenerator.generateASTInformation(creation);
	}

	public TypeObject getType() {
		return type;
	}

	public Type getASTType() {
		return getArrayCreation().getType();
	}
}
