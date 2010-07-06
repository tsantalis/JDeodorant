package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.distance.Entity;
import gr.uom.java.distance.MyMethod;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class ExtractClassRefactoringDescriptor extends RefactoringDescriptor {
	
	public static final String REFACTORING_ID = "org.eclipse.extract.class";
	private CompilationUnit sourceCompilationUnit;
	private TypeDeclaration sourceTypeDeclaration;
	private IFile sourceFile;
	private List<Entity> extractedEntities;
	private Map<MyMethod, Boolean> leaveDelegate;
	private String targetTypeName;

	protected ExtractClassRefactoringDescriptor(String project,
			String description, String comment, CompilationUnit sourceCompilationUnit, 
			TypeDeclaration sourceTypeDeclaration, IFile sourceFile, List<Entity> extractedEntities, Map<MyMethod, Boolean> leaveDelegate, String targetTypeName) {
		super(REFACTORING_ID, project, description, comment, RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE);
		this.sourceCompilationUnit = sourceCompilationUnit;
		this.sourceTypeDeclaration = sourceTypeDeclaration;
		this.sourceFile = sourceFile;
		this.extractedEntities = extractedEntities;
		this.leaveDelegate = leaveDelegate;
		this.targetTypeName = targetTypeName;
	}

	@Override
	public Refactoring createRefactoring(RefactoringStatus status)
			throws CoreException {
		Refactoring refactoring = new ExtractClassRefactoring(sourceCompilationUnit, sourceTypeDeclaration, sourceFile, extractedEntities, leaveDelegate, targetTypeName);
		RefactoringStatus refStatus = new RefactoringStatus();
		status.merge(refStatus);
		return refactoring;
	}

}
