package gr.uom.java.ast;

import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;

public class AnonymousClassDeclarationObject extends ClassDeclarationObject {
	private ASTInformation anonymousClassDeclaration;
	
	public void setAnonymousClassDeclaration(AnonymousClassDeclaration anonymous) {
		this.anonymousClassDeclaration = ASTInformationGenerator.generateASTInformation(anonymous);
	}
	
	public AnonymousClassDeclaration getAnonymousClassDeclaration() {
		return (AnonymousClassDeclaration)anonymousClassDeclaration.recoverASTNode();
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
