package ca.concordia.jdeodorant.clone.parsers;

import gr.uom.java.ast.ASTReader;

import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class ResourceInfo {
	
	public static class ICompilationUnitNotFoundException extends Exception {
		private static final long serialVersionUID = 1L;
		public ICompilationUnitNotFoundException(String message) {
			super(message);
		}
	}

	private final String sourceFolder;
	private final ICompilationUnit iCompilationUnit;
	private final CompilationUnit compilationUnit;
	private final String packageName;
	private final String className;
	private final String givenPath;

	public ResourceInfo(String sourceFolder, ICompilationUnit iCompilationUnit, String givenPath) {
		this.sourceFolder = sourceFolder;
		this.iCompilationUnit = iCompilationUnit;
		this.givenPath = givenPath;
		this.compilationUnit = getCompilationUnitFromICompilationUnit(iCompilationUnit);
		if (this.compilationUnit.getPackage() != null)
			packageName = this.compilationUnit.getPackage().getName().toString();
		else 
			packageName = "";
		this.className = iCompilationUnit.getResource().getName().substring(0, iCompilationUnit.getResource().getName().lastIndexOf("."));
	}
	
	public CompilationUnit getCompilationUnitFromICompilationUnit(ICompilationUnit iCompilationUnit) {
		CompilationUnit cunit = null;
		ASTParser parser = ASTParser.newParser(ASTReader.JLS);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(iCompilationUnit);
		//parser.setResolveBindings(true);
		cunit = (CompilationUnit) parser.createAST(null);
		return cunit;
	}

	public String getSourceFolder() {
		return sourceFolder;
	}

	public ICompilationUnit getICompilationUnit() {
		return iCompilationUnit;
	}

	public CompilationUnit getCompilationUnit() {
		return compilationUnit;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getClassName() {
		return className;
	}

	public static ResourceInfo getResourceInfo(IJavaProject jProject, String fullResourceName, boolean isAbsoluteFilePath) throws JavaModelException, ICompilationUnitNotFoundException {

		// First try the given path, if not found, prepend src dir
		ICompilationUnit iCompilationUnit = (ICompilationUnit) JavaCore.create(jProject.getProject().getFile(fullResourceName));
		Set<String> allSrcDirectories = JavaModelUtility.getAllSourceDirectories(jProject);

		if (iCompilationUnit != null && iCompilationUnit.exists()) {
			for (String srcDirectory : allSrcDirectories) {
				if (fullResourceName.startsWith(srcDirectory)) {
					return new ResourceInfo(srcDirectory, iCompilationUnit, fullResourceName);
				}
			}
		}

		for (String srcDirectory : allSrcDirectories) {
			String fullPath = "";
			if (isAbsoluteFilePath) {
				int indexOfSrcDirectorInTheAbsolutePath = fullResourceName.indexOf(srcDirectory);
				if (indexOfSrcDirectorInTheAbsolutePath >= 0) {
					fullPath = fullResourceName.substring(indexOfSrcDirectorInTheAbsolutePath);
				} else {
					continue;
				}
			} else {
				fullPath = srcDirectory + "/" + fullResourceName;
			}
			iCompilationUnit = (ICompilationUnit) JavaCore.create(jProject.getProject().getFile(fullPath));
			if (iCompilationUnit != null && iCompilationUnit.exists()) {
				return new ResourceInfo(srcDirectory, iCompilationUnit, fullResourceName);
			}
		}
		
		throw new ICompilationUnitNotFoundException(String.format("ICompilationUnit not found for %s", fullResourceName));
	}

	public String getFullPath() {
		if (this.iCompilationUnit != null)
			return this.iCompilationUnit.getResource().getLocation().toPortableString();
		return this.givenPath;
	}
}