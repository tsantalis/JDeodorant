package gr.uom.java.ast;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Type;

public class ClassInstanceCreationObject implements CreationObject {

	private TypeObject type;
	private ASTInformation classInstanceCreation;
	
	public ClassInstanceCreationObject(TypeObject type) {
		this.type = type;
	}

	public ClassInstanceCreation getClassInstanceCreation() {
		return (ClassInstanceCreation)this.classInstanceCreation.recoverASTNode();
	}

	public void setClassInstanceCreation(ClassInstanceCreation creation) {
		this.classInstanceCreation = ASTInformationGenerator.generateASTInformation(creation);
	}

	public TypeObject getType() {
		return type;
	}

	public Type getASTType() {
		return getClassInstanceCreation().getType();
	}
}
