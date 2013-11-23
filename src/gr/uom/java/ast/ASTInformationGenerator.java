package gr.uom.java.ast;

import java.io.File;

import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.ASTNode;

public class ASTInformationGenerator {

	private static ITypeRoot iTypeRoot;
	private static File file;
	
	public static void setCurrentITypeRoot(ITypeRoot typeRoot) {
		iTypeRoot = typeRoot;
	}
	
	public static void setCurrentFile(File file) {
		ASTInformationGenerator.file = file;
	}

	public static ASTInformation generateASTInformation(ASTNode astNode) {
		if(iTypeRoot != null)
			return new ASTInformation(iTypeRoot, astNode);
		else
			return new ASTInformation(file, astNode);
	}
}
