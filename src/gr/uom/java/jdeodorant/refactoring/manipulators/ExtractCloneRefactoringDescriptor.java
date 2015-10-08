package gr.uom.java.jdeodorant.refactoring.manipulators;

import java.util.List;

import gr.uom.java.ast.decomposition.cfg.mapping.DivideAndConquerMatcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class ExtractCloneRefactoringDescriptor extends RefactoringDescriptor {

	public static final String REFACTORING_ID = "org.eclipse.extract.clone";
	private List<? extends DivideAndConquerMatcher> mappers;
	
	protected ExtractCloneRefactoringDescriptor(String project,
			String description, String comment, List<? extends DivideAndConquerMatcher> mappers) {
		super(REFACTORING_ID, project, description, comment, RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE);
		this.mappers = mappers;
	}

	@Override
	public Refactoring createRefactoring(RefactoringStatus status)
			throws CoreException {
		Refactoring refactoring = new ExtractCloneRefactoring(mappers);
		RefactoringStatus refStatus = new RefactoringStatus();
		status.merge(refStatus);
		return refactoring;
	}

}
