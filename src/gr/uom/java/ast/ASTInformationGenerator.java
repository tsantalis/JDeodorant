package gr.uom.java.ast;

import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.ASTNode;

public class ASTInformationGenerator {

	private static ITypeRoot iTypeRoot;
	
	public static void setCurrentITypeRoot(ITypeRoot typeRoot) {
		iTypeRoot = typeRoot;
	}

	public static ASTInformation generateASTInformation(ASTNode astNode) {
		return new ASTInformation(iTypeRoot, astNode);
	}
}
