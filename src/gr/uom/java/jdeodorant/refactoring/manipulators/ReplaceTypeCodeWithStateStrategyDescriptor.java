package gr.uom.java.jdeodorant.refactoring.manipulators;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class ReplaceTypeCodeWithStateStrategyDescriptor extends RefactoringDescriptor {
	
	public static final String REFACTORING_ID = "org.eclipse.replace.type.code.with.state.strategy";
	private IFile sourceFile;
	private CompilationUnit sourceCompilationUnit;
	private TypeDeclaration sourceTypeDeclaration;
	private TypeCheckElimination typeCheckElimination;
	
	protected ReplaceTypeCodeWithStateStrategyDescriptor(String project, String description, String comment,
			IFile sourceFile, CompilationUnit sourceCompilationUnit,
			TypeDeclaration sourceTypeDeclaration, TypeCheckElimination typeCheckElimination) {
		super(REFACTORING_ID, project, description, comment, RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE);
		this.sourceFile = sourceFile;
		this.sourceCompilationUnit = sourceCompilationUnit;
		this.sourceTypeDeclaration = sourceTypeDeclaration;
		this.typeCheckElimination = typeCheckElimination;
	}

	@Override
	public Refactoring createRefactoring(RefactoringStatus status)
			throws CoreException {
		Refactoring refactoring = new ReplaceTypeCodeWithStateStrategy(sourceFile, sourceCompilationUnit, sourceTypeDeclaration, typeCheckElimination);
		RefactoringStatus refStatus = new RefactoringStatus();
		status.merge(refStatus);
		return refactoring;
	}

}
