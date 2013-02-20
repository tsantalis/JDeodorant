package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.decomposition.cfg.mapping.PDGMapper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class ExtractCloneRefactoringDescriptor extends RefactoringDescriptor {

	public static final String REFACTORING_ID = "org.eclipse.extract.clone";
	private PDGMapper mapper;
	
	protected ExtractCloneRefactoringDescriptor(String project,
			String description, String comment, PDGMapper mapper) {
		super(REFACTORING_ID, project, description, comment, RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE);
		this.mapper = mapper;
	}

	@Override
	public Refactoring createRefactoring(RefactoringStatus status)
			throws CoreException {
		Refactoring refactoring = new ExtractCloneRefactoring(mapper);
		RefactoringStatus refStatus = new RefactoringStatus();
		status.merge(refStatus);
		return refactoring;
	}

}
