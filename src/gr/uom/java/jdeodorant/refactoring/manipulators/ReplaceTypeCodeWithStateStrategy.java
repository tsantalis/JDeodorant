package gr.uom.java.jdeodorant.refactoring.manipulators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;

public class ReplaceTypeCodeWithStateStrategy implements Refactoring {
	private IFile sourceFile;
	private CompilationUnit sourceCompilationUnit;
	private TypeDeclaration sourceTypeDeclaration;
	private TypeCheckElimination typeCheckElimination;
	private ASTRewrite sourceRewriter;
	private UndoRefactoring undoRefactoring;
	
	public ReplaceTypeCodeWithStateStrategy(IFile sourceFile, CompilationUnit sourceCompilationUnit,
			TypeDeclaration sourceTypeDeclaration,
			TypeCheckElimination typeCheckElimination) {
		this.sourceFile = sourceFile;
		this.sourceCompilationUnit = sourceCompilationUnit;
		this.sourceTypeDeclaration = sourceTypeDeclaration;
		this.typeCheckElimination = typeCheckElimination;
		this.sourceRewriter = ASTRewrite.create(sourceCompilationUnit.getAST());
		this.undoRefactoring = new UndoRefactoring();
	}

	public void apply() {
		createStateStrategyHierarchy();
		ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
		ITextFileBuffer sourceTextFileBuffer = bufferManager.getTextFileBuffer(sourceFile.getFullPath(), LocationKind.IFILE);
		IDocument sourceDocument = sourceTextFileBuffer.getDocument();
		TextEdit sourceEdit = sourceRewriter.rewriteAST(sourceDocument, null);
		try {
			UndoEdit sourceUndoEdit = sourceEdit.apply(sourceDocument, UndoEdit.CREATE_UNDO);
			undoRefactoring.put(sourceFile, sourceDocument, sourceUndoEdit);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	private void createStateStrategyHierarchy() {
		AST ast = sourceCompilationUnit.getAST();
		ListRewrite typesRewrite = sourceRewriter.getListRewrite(sourceCompilationUnit, CompilationUnit.TYPES_PROPERTY);
		
		TypeDeclaration stateStrategyTypeDeclaration = ast.newTypeDeclaration();
		SimpleName stateStrategyName = ast.newSimpleName(typeCheckElimination.getAbstractClassName());
		sourceRewriter.set(stateStrategyTypeDeclaration, TypeDeclaration.NAME_PROPERTY, stateStrategyName, null);
		ListRewrite stateStrategyModifiersRewrite = sourceRewriter.getListRewrite(stateStrategyTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
		//stateStrategyModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
		stateStrategyModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
		
		ListRewrite stateStrategyBodyRewrite = sourceRewriter.getListRewrite(stateStrategyTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		
		MethodDeclaration abstractMethodDeclaration = ast.newMethodDeclaration();
		String abstractMethodName = typeCheckElimination.getAbstractMethodName();
		sourceRewriter.set(abstractMethodDeclaration, MethodDeclaration.NAME_PROPERTY, ast.newSimpleName(abstractMethodName), null);
		sourceRewriter.set(abstractMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, typeCheckElimination.getAbstractMethodReturnType(), null);
		ListRewrite abstractMethodModifiersRewrite = sourceRewriter.getListRewrite(abstractMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		abstractMethodModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
		abstractMethodModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
		
		stateStrategyBodyRewrite.insertLast(abstractMethodDeclaration, null);
		
		typesRewrite.insertLast(stateStrategyTypeDeclaration, null);
		
		Collection<ArrayList<Statement>> typeCheckStatements = typeCheckElimination.getTypeCheckStatements();
		List<String> subclassNames = typeCheckElimination.getSubclassNames();
		int i = 0;
		for(ArrayList<Statement> statements : typeCheckStatements) {
			TypeDeclaration subclassTypeDeclaration = ast.newTypeDeclaration();
			SimpleName subclassName = ast.newSimpleName(subclassNames.get(i));
			sourceRewriter.set(subclassTypeDeclaration, TypeDeclaration.NAME_PROPERTY, subclassName, null);
			sourceRewriter.set(subclassTypeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, ast.newSimpleType(ast.newSimpleName(typeCheckElimination.getAbstractClassName())), null);
			ListRewrite subclassModifiersRewrite = sourceRewriter.getListRewrite(subclassTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
			//subclassModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
			
			ListRewrite subclassBodyRewrite = sourceRewriter.getListRewrite(subclassTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			
			MethodDeclaration concreteMethodDeclaration = ast.newMethodDeclaration();
			String concreteMethodName = typeCheckElimination.getAbstractMethodName();
			sourceRewriter.set(concreteMethodDeclaration, MethodDeclaration.NAME_PROPERTY, ast.newSimpleName(concreteMethodName), null);
			sourceRewriter.set(concreteMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, typeCheckElimination.getAbstractMethodReturnType(), null);
			ListRewrite concreteMethodModifiersRewrite = sourceRewriter.getListRewrite(concreteMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
			concreteMethodModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
			
			if(statements.size() == 1 && statements.get(0) instanceof Block) {
				sourceRewriter.set(concreteMethodDeclaration, MethodDeclaration.BODY_PROPERTY, statements.get(0), null);
			}
			else {
				Block concreteMethodBody = ast.newBlock();
				ListRewrite concreteMethodBodyRewrite = sourceRewriter.getListRewrite(concreteMethodBody, Block.STATEMENTS_PROPERTY);
				for(Statement statement : statements) {
					concreteMethodBodyRewrite.insertLast(statement, null);
				}
				sourceRewriter.set(concreteMethodDeclaration, MethodDeclaration.BODY_PROPERTY, concreteMethodBody, null);
			}
			
			subclassBodyRewrite.insertLast(concreteMethodDeclaration, null);
			
			typesRewrite.insertLast(subclassTypeDeclaration, null);
			i++;
		}
	}

	public UndoRefactoring getUndoRefactoring() {
		return undoRefactoring;
	}

	public IFile getSourceFile() {
		return sourceFile;
	}

	public Statement getTypeCheckCodeFragment() {
		return typeCheckElimination.getTypeCheckCodeFragment();
	}

	public String getTypeCheckMethodName() {
		return typeCheckElimination.getTypeCheckMethod().resolveBinding().toString();
	}
}
