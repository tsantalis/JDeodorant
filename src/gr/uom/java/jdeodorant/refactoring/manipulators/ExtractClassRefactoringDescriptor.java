package gr.uom.java.jdeodorant.refactoring.manipulators;

import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class ExtractClassRefactoringDescriptor extends RefactoringDescriptor {
	
	public static final String REFACTORING_ID = "org.eclipse.extract.class";
	private CompilationUnit sourceCompilationUnit;
	private TypeDeclaration sourceTypeDeclaration;
	private IFile sourceFile;
	private Set<VariableDeclaration> extractedFieldFragments;
	private Set<MethodDeclaration> extractedMethods;
	private Set<MethodDeclaration> delegateMethods;
	private String extractedTypeName;

	protected ExtractClassRefactoringDescriptor(String project, String description, String comment,
			CompilationUnit sourceCompilationUnit, TypeDeclaration sourceTypeDeclaration, IFile sourceFile,
			Set<VariableDeclaration> extractedFieldFragments, Set<MethodDeclaration> extractedMethods,
			Set<MethodDeclaration> delegateMethods, String extractedTypeName) {
		super(REFACTORING_ID, project, description, comment, RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE);
		this.sourceCompilationUnit = sourceCompilationUnit;
		this.sourceTypeDeclaration = sourceTypeDeclaration;
		this.sourceFile = sourceFile;
		this.extractedFieldFragments = extractedFieldFragments;
		this.extractedMethods = extractedMethods;
		this.delegateMethods = delegateMethods;
		this.extractedTypeName = extractedTypeName;
	}

	@Override
	public Refactoring createRefactoring(RefactoringStatus status)
			throws CoreException {
		Refactoring refactoring = new ExtractClassRefactoring(sourceFile, sourceCompilationUnit, sourceTypeDeclaration,
				extractedFieldFragments, extractedMethods, delegateMethods, extractedTypeName);
		RefactoringStatus refStatus = new RefactoringStatus();
		status.merge(refStatus);
		return refactoring;
	}

}
