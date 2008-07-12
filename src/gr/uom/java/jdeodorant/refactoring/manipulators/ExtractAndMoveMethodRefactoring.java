package gr.uom.java.jdeodorant.refactoring.manipulators;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;

public class ExtractAndMoveMethodRefactoring extends Refactoring {
	private IFile sourceFile;
	private CompilationUnit sourceCompilationUnit;
	private CompilationUnit targetCompilationUnit;
	private TypeDeclaration sourceTypeDeclaration;
	private TypeDeclaration targetTypeDeclaration;
	private MethodDeclaration sourceMethod;
	private ASTExtractionBlock extractionBlock;
	private Map<ICompilationUnit, TextFileChange> fChanges;
	
	public ExtractAndMoveMethodRefactoring(IFile sourceFile, CompilationUnit sourceCompilationUnit, CompilationUnit targetCompilationUnit,
			TypeDeclaration sourceTypeDeclaration, TypeDeclaration targetTypeDeclaration, MethodDeclaration sourceMethod,
			ASTExtractionBlock extractionBlock) {
		this.sourceFile = sourceFile;
		this.sourceCompilationUnit = sourceCompilationUnit;
		this.targetCompilationUnit = targetCompilationUnit;
		this.sourceTypeDeclaration = sourceTypeDeclaration;
		this.targetTypeDeclaration = targetTypeDeclaration;
		this.sourceMethod = sourceMethod;
		this.extractionBlock = extractionBlock;
	}

	public void apply() {
		ExtractMethodRefactoring extractMethodRefactoring =
			new ExtractMethodRefactoring(sourceCompilationUnit, sourceTypeDeclaration, sourceMethod, extractionBlock);
		extractMethodRefactoring.apply();
		this.fChanges = extractMethodRefactoring.getChanges();
		IJavaElement iJavaElement = JavaCore.create(sourceFile);
        ICompilationUnit iCompilationUnit = (ICompilationUnit)iJavaElement;
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(iCompilationUnit);
        parser.setResolveBindings(true);
        CompilationUnit sourceCompilationUnit = (CompilationUnit)parser.createAST(null);
        List<AbstractTypeDeclaration> typeDeclarationList = sourceCompilationUnit.types();
        TypeDeclaration sourceTypeDeclaration = null;
        MethodDeclaration extractedMethodDeclaration = null;
        for(AbstractTypeDeclaration abstractTypeDeclaration : typeDeclarationList) {
        	if(abstractTypeDeclaration instanceof TypeDeclaration) {
        		TypeDeclaration typeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
        		if(typeDeclaration.resolveBinding().getQualifiedName().equals(this.sourceTypeDeclaration.resolveBinding().getQualifiedName())) {
        			sourceTypeDeclaration = typeDeclaration;
        			MethodDeclaration[] sourceClassMethodDeclarations = sourceTypeDeclaration.getMethods();
        			extractedMethodDeclaration = sourceClassMethodDeclarations[sourceClassMethodDeclarations.length-1];
        		}
        	}
        }
        CompilationUnit targetCompilationUnit = null;
        TypeDeclaration targetTypeDeclaration = null;
        if(this.sourceCompilationUnit.equals(this.targetCompilationUnit)) {
        	targetCompilationUnit = sourceCompilationUnit;
        	for(AbstractTypeDeclaration abstractTypeDeclaration : typeDeclarationList) {
            	if(abstractTypeDeclaration instanceof TypeDeclaration) {
            		TypeDeclaration typeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
            		if(typeDeclaration.resolveBinding().getQualifiedName().equals(this.targetTypeDeclaration.resolveBinding().getQualifiedName())) {
            			targetTypeDeclaration = typeDeclaration;
            		}
            	}
        	}
        }
        else {
        	targetCompilationUnit = this.targetCompilationUnit;
        	targetTypeDeclaration = this.targetTypeDeclaration;
        }
		MoveMethodRefactoring moveMethodRefactoring = new MoveMethodRefactoring(sourceCompilationUnit, targetCompilationUnit,
			sourceTypeDeclaration, targetTypeDeclaration, extractedMethodDeclaration, new LinkedHashMap<MethodInvocation, MethodDeclaration>(),
			false, extractedMethodDeclaration.getName().getIdentifier());
		moveMethodRefactoring.apply();
		Map<ICompilationUnit, TextFileChange> moveMethodChanges = moveMethodRefactoring.getChanges();
		for(ICompilationUnit key : moveMethodChanges.keySet()) {
			if(fChanges.containsKey(key)) {
				TextFileChange change = fChanges.get(key);
				change.getEdit().addChild(moveMethodChanges.get(key).getEdit());
			}
			else
				fChanges.put(key, moveMethodChanges.get(key));
		}
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
}
