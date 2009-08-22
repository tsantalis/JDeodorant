package gr.uom.java.ast;

import java.util.LinkedList;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class CompilationUnitCache {

	private static CompilationUnitCache instance;
	private static final int MAXIMUM_CACHE_SIZE = 5;
	private LinkedList<ITypeRoot> iTypeRootList;
	private LinkedList<CompilationUnit> compilationUnitList;

	private CompilationUnitCache() {
		this.iTypeRootList = new LinkedList<ITypeRoot>();
		this.compilationUnitList = new LinkedList<CompilationUnit>();
	}

	public static CompilationUnitCache getInstance() {
		if(instance == null) {
			instance = new CompilationUnitCache();
		}
		return instance;
	}

	public CompilationUnit getCompilationUnit(ITypeRoot iTypeRoot) {
		if(iTypeRoot instanceof IClassFile) {
			IClassFile classFile = (IClassFile)iTypeRoot;
			return LibraryClassStorage.getInstance().getCompilationUnit(classFile);
		}
		else {
			if(iTypeRootList.contains(iTypeRoot)) {
				int position = iTypeRootList.indexOf(iTypeRoot);
				return compilationUnitList.get(position);
			}
			else {
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				parser.setSource(iTypeRoot);
				parser.setResolveBindings(true);
				CompilationUnit compilationUnit = (CompilationUnit)parser.createAST(null);

				if(iTypeRootList.size() < MAXIMUM_CACHE_SIZE) {
					iTypeRootList.add(iTypeRoot);
					compilationUnitList.add(compilationUnit);
				}
				else {
					iTypeRootList.removeFirst();
					compilationUnitList.removeFirst();
					iTypeRootList.add(iTypeRoot);
					compilationUnitList.add(compilationUnit);
				}
				return compilationUnit;
			}
		}
	}
}
