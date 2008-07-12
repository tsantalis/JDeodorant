package gr.uom.java.jdeodorant.refactoring.manipulators;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class MoveMethodRefactoringDescriptor extends RefactoringDescriptor {
	
	public static final String REFACTORING_ID = "org.eclipse.move.method";
	private CompilationUnit sourceCompilationUnit;
	private CompilationUnit targetCompilationUnit;
	private TypeDeclaration sourceTypeDeclaration;
	private TypeDeclaration targetTypeDeclaration;
	private MethodDeclaration sourceMethod;
	private Map<MethodInvocation, MethodDeclaration> additionalMethodsToBeMoved;
	private boolean leaveDelegate;
	private String movedMethodName;
	
	public MoveMethodRefactoringDescriptor(String project, String description, String comment,
			CompilationUnit sourceCompilationUnit, CompilationUnit targetCompilationUnit, 
			TypeDeclaration sourceTypeDeclaration, TypeDeclaration targetTypeDeclaration, MethodDeclaration sourceMethod,
			Map<MethodInvocation, MethodDeclaration> additionalMethodsToBeMoved, boolean leaveDelegate, String movedMethodName) {
		super(REFACTORING_ID, project, description, comment, RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE);
		this.sourceCompilationUnit = sourceCompilationUnit;
		this.targetCompilationUnit = targetCompilationUnit;
		this.sourceTypeDeclaration = sourceTypeDeclaration;
		this.targetTypeDeclaration = targetTypeDeclaration;
		this.sourceMethod = sourceMethod;
		this.additionalMethodsToBeMoved = additionalMethodsToBeMoved;
		this.leaveDelegate = leaveDelegate;
		this.movedMethodName = movedMethodName;
	}

	@Override
	public Refactoring createRefactoring(RefactoringStatus status)
			throws CoreException {
		Refactoring refactoring = new MoveMethodRefactoring(sourceCompilationUnit, targetCompilationUnit,
				sourceTypeDeclaration, targetTypeDeclaration, sourceMethod,
				additionalMethodsToBeMoved, leaveDelegate, movedMethodName);
		RefactoringStatus refStatus = new RefactoringStatus();
		status.merge(refStatus);
		return refactoring;
	}

}
