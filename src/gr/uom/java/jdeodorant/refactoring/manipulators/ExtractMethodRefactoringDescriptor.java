package gr.uom.java.jdeodorant.refactoring.manipulators;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class ExtractMethodRefactoringDescriptor extends RefactoringDescriptor {

	public static final String REFACTORING_ID = "org.eclipse.extract.method";
	private CompilationUnit sourceCompilationUnit;
	private ASTSlice slice;
	
	protected ExtractMethodRefactoringDescriptor(String project, String description, String comment,
			CompilationUnit sourceCompilationUnit, ASTSlice slice) {
		super(REFACTORING_ID, project, description, comment, RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE);
		this.sourceCompilationUnit = sourceCompilationUnit;
		this.slice = slice;
	}

	@Override
	public Refactoring createRefactoring(RefactoringStatus status)
			throws CoreException {
		Refactoring refactoring = new ExtractMethodRefactoring(sourceCompilationUnit, slice);
		RefactoringStatus refStatus = new RefactoringStatus();
		status.merge(refStatus);
		return refactoring;
	}

}
