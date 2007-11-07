package gr.uom.java.jdeodorant.refactoring.manipulators;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;

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
		this.sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		this.undoRefactoring = new UndoRefactoring();
	}

	public void apply() {
		createStateStrategyHierarchy();
		modifyContext();
	}

	private void modifyContext() {
		AST contextAST = sourceTypeDeclaration.getAST();
		ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		VariableDeclarationFragment typeFragment = contextAST.newVariableDeclarationFragment();
		sourceRewriter.set(typeFragment, VariableDeclarationFragment.NAME_PROPERTY, typeCheckElimination.getTypeField().getName(), null);
		FieldDeclaration typeFieldDeclaration = contextAST.newFieldDeclaration(typeFragment);
		String type = typeCheckElimination.getTypeField().getName().getIdentifier();
		type = type.substring(0,1).toUpperCase() + type.substring(1, type.length());
		sourceRewriter.set(typeFieldDeclaration, FieldDeclaration.TYPE_PROPERTY, contextAST.newSimpleName(type), null);
		ListRewrite typeFieldDeclrationModifiersRewrite = sourceRewriter.getListRewrite(typeFieldDeclaration, FieldDeclaration.MODIFIERS2_PROPERTY);
		typeFieldDeclrationModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD), null);
		contextBodyRewrite.insertFirst(typeFieldDeclaration, null);
		
		FieldDeclaration[] fieldDeclarations = sourceTypeDeclaration.getFields();
		for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
			List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				if(fragment.equals(typeCheckElimination.getTypeField())) {
					if(fragments.size() == 1) {
						contextBodyRewrite.remove(fragment.getParent(), null);
					}
					else {
						ListRewrite fragmentRewrite = sourceRewriter.getListRewrite(fragment.getParent(), FieldDeclaration.FRAGMENTS_PROPERTY);
						fragmentRewrite.remove(fragment, null);
					}
				}
			}
		}
		
		MethodDeclaration setterMethod = typeCheckElimination.getTypeFieldSetterMethod();
		List<SingleVariableDeclaration> setterMethodParameters = setterMethod.parameters();
		Statement setterMethodStatement = (Statement)ASTNode.copySubtree(contextAST, typeCheckElimination.getTypeCheckCodeFragment());
		if(setterMethodStatement instanceof SwitchStatement) {
			SwitchStatement switchStatement = (SwitchStatement)setterMethodStatement;
			List<Statement> switchStatementStatements = switchStatement.statements();
			if(setterMethodParameters.size() == 1) {
				sourceRewriter.set(switchStatement, SwitchStatement.EXPRESSION_PROPERTY, setterMethodParameters.get(0).getName(), null);
			}
			ListRewrite switchStatementStatementsRewrite = sourceRewriter.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);
			List<String> subclassNames = typeCheckElimination.getSubclassNames();
			Collection<ArrayList<Statement>> allTypeCheckStatements = typeCheckElimination.getTypeCheckStatements();
			int i = 0;
			for(ArrayList<Statement> typeCheckStatements : allTypeCheckStatements) {
				int matches = 0;
				for(Statement oldStatement : typeCheckStatements) {
					for(Statement newStatement : switchStatementStatements) {
						if(oldStatement.toString().equals(newStatement.toString())) {
							matches++;
							if(matches == typeCheckStatements.size()) {
								Assignment assignment = contextAST.newAssignment();
								sourceRewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.ASSIGN, null);
								FieldAccess typeFieldAccess = contextAST.newFieldAccess();
								sourceRewriter.set(typeFieldAccess, FieldAccess.EXPRESSION_PROPERTY, contextAST.newThisExpression(), null);
								sourceRewriter.set(typeFieldAccess, FieldAccess.NAME_PROPERTY, typeCheckElimination.getTypeField().getName(), null);
								sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, typeFieldAccess, null);
								ClassInstanceCreation classInstanceCreation = contextAST.newClassInstanceCreation();
								sourceRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY, contextAST.newSimpleName(subclassNames.get(i)), null);
								sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, classInstanceCreation, null);
								switchStatementStatementsRewrite.replace(newStatement, contextAST.newExpressionStatement(assignment), null);
							}
							else {
								switchStatementStatementsRewrite.remove(newStatement, null);
							}
						}
					}
				}
				i++;
			}
		}
		else if(setterMethodStatement instanceof IfStatement) {
			IfStatement typeCheckIfStatement = (IfStatement)setterMethodStatement;
			
			List<String> subclassNames = typeCheckElimination.getSubclassNames();
			Collection<ArrayList<Statement>> allTypeCheckStatements = typeCheckElimination.getTypeCheckStatements();
			int i = 0;
			for(ArrayList<Statement> typeCheckStatements : allTypeCheckStatements) {
				if(typeCheckStatements.size() == 1) {
					Statement oldStatement = typeCheckStatements.get(0);
					IfStatement ifStatement = typeCheckIfStatement;
					Statement newStatement = typeCheckIfStatement.getThenStatement();
					while(!oldStatement.toString().equals(newStatement.toString())) {
						Statement elseStatement = ifStatement.getElseStatement();
						if(elseStatement instanceof IfStatement) {
							ifStatement = (IfStatement)elseStatement;
							newStatement = ifStatement.getThenStatement();
						}
					}
					IfStatement parentIfStatement = (IfStatement)newStatement.getParent();
					Assignment assignment = contextAST.newAssignment();
					sourceRewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.ASSIGN, null);
					FieldAccess typeFieldAccess = contextAST.newFieldAccess();
					sourceRewriter.set(typeFieldAccess, FieldAccess.EXPRESSION_PROPERTY, contextAST.newThisExpression(), null);
					sourceRewriter.set(typeFieldAccess, FieldAccess.NAME_PROPERTY, typeCheckElimination.getTypeField().getName(), null);
					sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, typeFieldAccess, null);
					ClassInstanceCreation classInstanceCreation = contextAST.newClassInstanceCreation();
					sourceRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY, contextAST.newSimpleName(subclassNames.get(i)), null);
					sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, classInstanceCreation, null);
					sourceRewriter.set(parentIfStatement, IfStatement.THEN_STATEMENT_PROPERTY, contextAST.newExpressionStatement(assignment), null);
					
					InfixExpression infixExpression = (InfixExpression)parentIfStatement.getExpression();
					Expression leftOperand = infixExpression.getLeftOperand();
					if(leftOperand instanceof SimpleName) {
						SimpleName simpleName = (SimpleName)leftOperand;
						if(simpleName.getIdentifier().equals(typeCheckElimination.getTypeField().getName().getIdentifier())) {
							sourceRewriter.set(infixExpression, InfixExpression.LEFT_OPERAND_PROPERTY, setterMethodParameters.get(0).getName(), null);
						}
					}
					else if(leftOperand instanceof FieldAccess) {
						FieldAccess fieldAccess = (FieldAccess)leftOperand;
						if(fieldAccess.getName().getIdentifier().equals(typeCheckElimination.getTypeField().getName().getIdentifier())) {
							sourceRewriter.set(infixExpression, InfixExpression.LEFT_OPERAND_PROPERTY, setterMethodParameters.get(0).getName(), null);
						}
					}
					Expression rightOperand = infixExpression.getRightOperand();
					if(rightOperand instanceof SimpleName) {
						SimpleName simpleName = (SimpleName)rightOperand;
						if(simpleName.getIdentifier().equals(typeCheckElimination.getTypeField().getName().getIdentifier())) {
							sourceRewriter.set(infixExpression, InfixExpression.RIGHT_OPERAND_PROPERTY, setterMethodParameters.get(0).getName(), null);
						}
					}
					else if(rightOperand instanceof FieldAccess) {
						FieldAccess fieldAccess = (FieldAccess)rightOperand;
						if(fieldAccess.getName().getIdentifier().equals(typeCheckElimination.getTypeField().getName().getIdentifier())) {
							sourceRewriter.set(infixExpression, InfixExpression.RIGHT_OPERAND_PROPERTY, setterMethodParameters.get(0).getName(), null);
						}
					}
				}
				i++;
			}
		}
		Block setterMethodBody = setterMethod.getBody();
		List<Statement> setterMethodBodyStatements = setterMethodBody.statements();
		ListRewrite setterMethodBodyRewrite = sourceRewriter.getListRewrite(setterMethodBody, Block.STATEMENTS_PROPERTY);
		if(setterMethodBodyStatements.size() == 1) {
			setterMethodBodyRewrite.replace(setterMethodBodyStatements.get(0), setterMethodStatement, null);
		}
		
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
		IFolder contextFolder = (IFolder)sourceFile.getParent();
		IFile stateStrategyFile = contextFolder.getFile(typeCheckElimination.getAbstractClassName() + ".java");
		boolean stateStrategyAlreadyExists = false;
		try {
			stateStrategyFile.create(new ByteArrayInputStream("".getBytes()), true, null);
			undoRefactoring.addNewlyCreatedFile(stateStrategyFile);
		} catch (CoreException e) {
			stateStrategyAlreadyExists = true;
		}
		IJavaElement stateStrategyJavaElement = JavaCore.create(stateStrategyFile);
		ITextEditor stateStrategyEditor = null;
		try {
			stateStrategyEditor = (ITextEditor)JavaUI.openInEditor(stateStrategyJavaElement);
		} catch (PartInitException e) {
			e.printStackTrace();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
        ICompilationUnit stateStrategyICompilationUnit = (ICompilationUnit)stateStrategyJavaElement;
        ASTParser stateStrategyParser = ASTParser.newParser(AST.JLS3);
        stateStrategyParser.setKind(ASTParser.K_COMPILATION_UNIT);
        stateStrategyParser.setSource(stateStrategyICompilationUnit);
        stateStrategyParser.setResolveBindings(true); // we need bindings later on
        CompilationUnit stateStrategyCompilationUnit = (CompilationUnit)stateStrategyParser.createAST(null);
        
        AST stateStrategyAST = stateStrategyCompilationUnit.getAST();
        ASTRewrite stateStrategyRewriter = ASTRewrite.create(stateStrategyAST);
        ListRewrite stateStrategyTypesRewrite = stateStrategyRewriter.getListRewrite(stateStrategyCompilationUnit, CompilationUnit.TYPES_PROPERTY);
		
		TypeDeclaration stateStrategyTypeDeclaration = null;
		if(stateStrategyAlreadyExists) {
			List<AbstractTypeDeclaration> abstractTypeDeclarations = stateStrategyCompilationUnit.types();
			for(AbstractTypeDeclaration abstractTypeDeclaration : abstractTypeDeclarations) {
				if(abstractTypeDeclaration instanceof TypeDeclaration) {
					TypeDeclaration typeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
					if(typeDeclaration.getName().getIdentifier().equals(typeCheckElimination.getAbstractClassName())) {
						stateStrategyTypeDeclaration = typeDeclaration;
						break;
					}
				}
			}
		}
		else {
			if(sourceCompilationUnit.getPackage() != null) {
				stateStrategyRewriter.set(stateStrategyCompilationUnit, CompilationUnit.PACKAGE_PROPERTY, sourceCompilationUnit.getPackage(), null);
			}
			stateStrategyTypeDeclaration = stateStrategyAST.newTypeDeclaration();
			SimpleName stateStrategyName = stateStrategyAST.newSimpleName(typeCheckElimination.getAbstractClassName());
			stateStrategyRewriter.set(stateStrategyTypeDeclaration, TypeDeclaration.NAME_PROPERTY, stateStrategyName, null);
			ListRewrite stateStrategyModifiersRewrite = stateStrategyRewriter.getListRewrite(stateStrategyTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
			stateStrategyModifiersRewrite.insertLast(stateStrategyAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
			stateStrategyModifiersRewrite.insertLast(stateStrategyAST.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
		}
		
		ListRewrite stateStrategyBodyRewrite = stateStrategyRewriter.getListRewrite(stateStrategyTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		
		MethodDeclaration abstractMethodDeclaration = stateStrategyAST.newMethodDeclaration();
		String abstractMethodName = typeCheckElimination.getAbstractMethodName();
		stateStrategyRewriter.set(abstractMethodDeclaration, MethodDeclaration.NAME_PROPERTY, stateStrategyAST.newSimpleName(abstractMethodName), null);
		stateStrategyRewriter.set(abstractMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, typeCheckElimination.getAbstractMethodReturnType(), null);
		ListRewrite abstractMethodModifiersRewrite = stateStrategyRewriter.getListRewrite(abstractMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		abstractMethodModifiersRewrite.insertLast(stateStrategyAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
		abstractMethodModifiersRewrite.insertLast(stateStrategyAST.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
		
		stateStrategyBodyRewrite.insertLast(abstractMethodDeclaration, null);
		
		if(!stateStrategyAlreadyExists)
			stateStrategyTypesRewrite.insertLast(stateStrategyTypeDeclaration, null);
		
		ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
		ITextFileBuffer stateStrategyTextFileBuffer = bufferManager.getTextFileBuffer(stateStrategyFile.getFullPath(), LocationKind.IFILE);
		IDocument stateStrategyDocument = stateStrategyTextFileBuffer.getDocument();
		TextEdit stateStrategyEdit = stateStrategyRewriter.rewriteAST(stateStrategyDocument, null);
		try {
			UndoEdit stateStrategyUndoEdit = stateStrategyEdit.apply(stateStrategyDocument, UndoEdit.CREATE_UNDO);
			undoRefactoring.put(stateStrategyFile, stateStrategyDocument, stateStrategyUndoEdit);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		stateStrategyEditor.doSave(null);
		
		
		Collection<ArrayList<Statement>> typeCheckStatements = typeCheckElimination.getTypeCheckStatements();
		List<String> subclassNames = typeCheckElimination.getSubclassNames();
		int i = 0;
		for(ArrayList<Statement> statements : typeCheckStatements) {
			IFile subclassFile = contextFolder.getFile(subclassNames.get(i) + ".java");
			boolean subclassAlreadyExists = false;
			try {
				subclassFile.create(new ByteArrayInputStream("".getBytes()), true, null);
				undoRefactoring.addNewlyCreatedFile(subclassFile);
			} catch (CoreException e) {
				subclassAlreadyExists = true;
			}
			IJavaElement subclassJavaElement = JavaCore.create(subclassFile);
			ITextEditor subclassEditor = null;
			try {
				subclassEditor = (ITextEditor)JavaUI.openInEditor(subclassJavaElement);
			} catch (PartInitException e) {
				e.printStackTrace();
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
			ICompilationUnit subclassICompilationUnit = (ICompilationUnit)subclassJavaElement;
	        ASTParser subclassParser = ASTParser.newParser(AST.JLS3);
	        subclassParser.setKind(ASTParser.K_COMPILATION_UNIT);
	        subclassParser.setSource(subclassICompilationUnit);
	        subclassParser.setResolveBindings(true); // we need bindings later on
	        CompilationUnit subclassCompilationUnit = (CompilationUnit)subclassParser.createAST(null);
	        
	        AST subclassAST = subclassCompilationUnit.getAST();
	        ASTRewrite subclassRewriter = ASTRewrite.create(subclassAST);
	        ListRewrite subclassTypesRewrite = subclassRewriter.getListRewrite(subclassCompilationUnit, CompilationUnit.TYPES_PROPERTY);
			
			TypeDeclaration subclassTypeDeclaration = null;
			if(subclassAlreadyExists) {
				List<AbstractTypeDeclaration> abstractTypeDeclarations = subclassCompilationUnit.types();
				for(AbstractTypeDeclaration abstractTypeDeclaration : abstractTypeDeclarations) {
					if(abstractTypeDeclaration instanceof TypeDeclaration) {
						TypeDeclaration typeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
						if(typeDeclaration.getName().getIdentifier().equals(subclassNames.get(i))) {
							subclassTypeDeclaration = typeDeclaration;
							break;
						}
					}
				}
			}
			else {
				if(sourceCompilationUnit.getPackage() != null) {
					subclassRewriter.set(subclassCompilationUnit, CompilationUnit.PACKAGE_PROPERTY, sourceCompilationUnit.getPackage(), null);
				}
				subclassTypeDeclaration = subclassAST.newTypeDeclaration();
				SimpleName subclassName = subclassAST.newSimpleName(subclassNames.get(i));
				subclassRewriter.set(subclassTypeDeclaration, TypeDeclaration.NAME_PROPERTY, subclassName, null);
				subclassRewriter.set(subclassTypeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, subclassAST.newSimpleType(subclassAST.newSimpleName(typeCheckElimination.getAbstractClassName())), null);
				ListRewrite subclassModifiersRewrite = subclassRewriter.getListRewrite(subclassTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
				subclassModifiersRewrite.insertLast(subclassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
			}
			
			ListRewrite subclassBodyRewrite = subclassRewriter.getListRewrite(subclassTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			
			MethodDeclaration concreteMethodDeclaration = subclassAST.newMethodDeclaration();
			String concreteMethodName = typeCheckElimination.getAbstractMethodName();
			subclassRewriter.set(concreteMethodDeclaration, MethodDeclaration.NAME_PROPERTY, subclassAST.newSimpleName(concreteMethodName), null);
			subclassRewriter.set(concreteMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, typeCheckElimination.getAbstractMethodReturnType(), null);
			ListRewrite concreteMethodModifiersRewrite = subclassRewriter.getListRewrite(concreteMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
			concreteMethodModifiersRewrite.insertLast(subclassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
			
			if(statements.size() == 1 && statements.get(0) instanceof Block) {
				subclassRewriter.set(concreteMethodDeclaration, MethodDeclaration.BODY_PROPERTY, statements.get(0), null);
			}
			else {
				Block concreteMethodBody = subclassAST.newBlock();
				ListRewrite concreteMethodBodyRewrite = subclassRewriter.getListRewrite(concreteMethodBody, Block.STATEMENTS_PROPERTY);
				for(Statement statement : statements) {
					concreteMethodBodyRewrite.insertLast(statement, null);
				}
				subclassRewriter.set(concreteMethodDeclaration, MethodDeclaration.BODY_PROPERTY, concreteMethodBody, null);
			}
			
			subclassBodyRewrite.insertLast(concreteMethodDeclaration, null);
			
			if(!subclassAlreadyExists)
				subclassTypesRewrite.insertLast(subclassTypeDeclaration, null);
			
			ITextFileBuffer subclassTextFileBuffer = bufferManager.getTextFileBuffer(subclassFile.getFullPath(), LocationKind.IFILE);
			IDocument subclassDocument = subclassTextFileBuffer.getDocument();
			TextEdit subclassEdit = subclassRewriter.rewriteAST(subclassDocument, null);
			try {
				UndoEdit subclassUndoEdit = subclassEdit.apply(subclassDocument, UndoEdit.CREATE_UNDO);
				undoRefactoring.put(subclassFile, subclassDocument, subclassUndoEdit);
			} catch (MalformedTreeException e) {
				e.printStackTrace();
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
			subclassEditor.doSave(null);
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
