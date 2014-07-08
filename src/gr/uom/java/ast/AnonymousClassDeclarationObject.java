package gr.uom.java.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;

public class AnonymousClassDeclarationObject extends ClassDeclarationObject {
	private ASTInformation anonymousClassDeclaration;
	private ClassObject classObject;
	
	public void setAnonymousClassDeclaration(AnonymousClassDeclaration anonymous) {
		this.anonymousClassDeclaration = ASTInformationGenerator.generateASTInformation(anonymous);
	}
	
	public AnonymousClassDeclaration getAnonymousClassDeclaration() {
		return (AnonymousClassDeclaration)anonymousClassDeclaration.recoverASTNode();
	}

	public ClassObject getClassObject() {
		return classObject;
	}

	public void setClassObject(ClassObject classObject) {
		this.classObject = classObject;
	}

	public ITypeRoot getITypeRoot() {
		return anonymousClassDeclaration.getITypeRoot();
	}

	public IFile getIFile() {
		if(classObject != null) {
			return classObject.getIFile();
		}
		return null;
	}

	public TypeObject getSuperclass() {
		return null;
	}

	protected void accessedFieldFromThisClass(Set<FieldObject> fields, FieldInstructionObject fieldInstruction) {
		List<FieldObject> allFields = new ArrayList<FieldObject>(fieldList);
		if(classObject != null) {
			//add the fields of the class in which the anonymous class is declared
			allFields.addAll(classObject.fieldList);
		}
		for(FieldObject field : allFields) {
			if(field.equals(fieldInstruction)) {
				if(!fields.contains(field))
					fields.add(field);
				break;
			}
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append("\n\n").append("Fields:");
        for(FieldObject field : fieldList)
            sb.append("\n").append(field.toString());

        sb.append("\n\n").append("Methods:");
        for(MethodObject method : methodList)
            sb.append("\n").append(method.toString());

        return sb.toString();
	}
}
