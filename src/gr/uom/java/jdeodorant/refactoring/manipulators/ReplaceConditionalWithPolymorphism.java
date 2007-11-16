package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.util.ExpressionExtractor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
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
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
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

public class ReplaceConditionalWithPolymorphism implements Refactoring {
	private IFile sourceFile;
	private CompilationUnit sourceCompilationUnit;
	private TypeDeclaration sourceTypeDeclaration;
	private TypeCheckElimination typeCheckElimination;
	private ASTRewrite sourceRewriter;
	private UndoRefactoring undoRefactoring;
	
	public ReplaceConditionalWithPolymorphism(IFile sourceFile, CompilationUnit sourceCompilationUnit,
			TypeDeclaration sourceTypeDeclaration, TypeCheckElimination typeCheckElimination) {
		this.sourceFile = sourceFile;
		this.sourceCompilationUnit = sourceCompilationUnit;
		this.sourceTypeDeclaration = sourceTypeDeclaration;
		this.typeCheckElimination = typeCheckElimination;
		this.sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		this.undoRefactoring = new UndoRefactoring();
	}

	public void apply() {
		modifyInheritanceHierarchy();
		modifyClient();
	}

	private void modifyClient() {
		AST clientAST = sourceTypeDeclaration.getAST();
		MethodDeclaration typeCheckMethod = typeCheckElimination.getTypeCheckMethod();
		Block typeCheckCodeFragmentParentBlock = (Block)typeCheckElimination.getTypeCheckCodeFragment().getParent();
		ListRewrite typeCheckCodeFragmentParentBlockStatementsRewrite = sourceRewriter.getListRewrite(typeCheckCodeFragmentParentBlock, Block.STATEMENTS_PROPERTY);
		Type typeCheckMethodReturnType = typeCheckMethod.getReturnType2();
		if(typeCheckMethodReturnType.isPrimitiveType() && ((PrimitiveType)typeCheckMethodReturnType).getPrimitiveTypeCode().equals(PrimitiveType.VOID)) {
			MethodInvocation abstractMethodInvocation = clientAST.newMethodInvocation();
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckMethod.getName(), null);
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, typeCheckElimination.getTypeMethodInvocation().getExpression(), null);
			for(SingleVariableDeclaration abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
				ListRewrite methodInvocationArgumentsRewrite = sourceRewriter.getListRewrite(abstractMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
				methodInvocationArgumentsRewrite.insertLast(abstractMethodParameter.getName(), null);
			}
			ExpressionStatement expressionStatement = clientAST.newExpressionStatement(abstractMethodInvocation);
			typeCheckCodeFragmentParentBlockStatementsRewrite.replace(typeCheckElimination.getTypeCheckCodeFragment(), expressionStatement, null);
		}
		else {
			Assignment assignment = clientAST.newAssignment();
			MethodInvocation abstractMethodInvocation = clientAST.newMethodInvocation();
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.NAME_PROPERTY, typeCheckMethod.getName(), null);
			sourceRewriter.set(abstractMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, typeCheckElimination.getTypeMethodInvocation().getExpression(), null);
			for(SingleVariableDeclaration abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
				ListRewrite methodInvocationArgumentsRewrite = sourceRewriter.getListRewrite(abstractMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
				methodInvocationArgumentsRewrite.insertLast(abstractMethodParameter.getName(), null);
			}
			VariableDeclarationFragment returnedVariable = typeCheckElimination.getTypeCheckMethodReturnedVariable();
			sourceRewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.ASSIGN, null);
			sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, returnedVariable.getName(), null);
			sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, abstractMethodInvocation, null);
			ExpressionStatement expressionStatement = clientAST.newExpressionStatement(assignment);
			typeCheckCodeFragmentParentBlockStatementsRewrite.replace(typeCheckElimination.getTypeCheckCodeFragment(), expressionStatement, null);
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

	private void modifyInheritanceHierarchy() {
		IContainer contextContainer = (IContainer)sourceFile.getParent();
		PackageDeclaration contextPackageDeclaration = sourceCompilationUnit.getPackage();
		IContainer rootContainer = contextContainer;
		if(contextPackageDeclaration != null) {
			String packageName = contextPackageDeclaration.getName().getFullyQualifiedName();
			String[] subPackages = packageName.split("\\.");
			for(int i = 0; i<subPackages.length; i++)
				rootContainer = (IContainer)rootContainer.getParent();
		}
		String abstractClassFullyQualifiedName = typeCheckElimination.getAbstractClassName();
		IFile abstractClassFile = getFile(rootContainer, abstractClassFullyQualifiedName);
		
		IJavaElement abstractJavaElement = JavaCore.create(abstractClassFile);
		ITextEditor abstractEditor = null;
		try {
			abstractEditor = (ITextEditor)JavaUI.openInEditor(abstractJavaElement);
		} catch (PartInitException e) {
			e.printStackTrace();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
		ICompilationUnit abstractICompilationUnit = (ICompilationUnit)abstractJavaElement;
        ASTParser abstractParser = ASTParser.newParser(AST.JLS3);
        abstractParser.setKind(ASTParser.K_COMPILATION_UNIT);
        abstractParser.setSource(abstractICompilationUnit);
        abstractParser.setResolveBindings(true); // we need bindings later on
        CompilationUnit abstractCompilationUnit = (CompilationUnit)abstractParser.createAST(null);
        
        AST abstractAST = abstractCompilationUnit.getAST();
        ASTRewrite abstractRewriter = ASTRewrite.create(abstractAST);
		
		TypeDeclaration abstractClassTypeDeclaration = null;
		List<AbstractTypeDeclaration> abstractTypeDeclarations = abstractCompilationUnit.types();
		for(AbstractTypeDeclaration abstractTypeDeclaration : abstractTypeDeclarations) {
			if(abstractTypeDeclaration instanceof TypeDeclaration) {
				TypeDeclaration typeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
				if(typeDeclaration.resolveBinding().getQualifiedName().equals(typeCheckElimination.getAbstractClassName())) {
					abstractClassTypeDeclaration = typeDeclaration;
					break;
				}
			}
		}
		int abstractClassModifiers = abstractClassTypeDeclaration.getModifiers();
		if((abstractClassModifiers & Modifier.ABSTRACT) == 0) {
			ListRewrite abstractModifiersRewrite = abstractRewriter.getListRewrite(abstractClassTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
			abstractModifiersRewrite.insertLast(abstractAST.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
		}
		
		ListRewrite abstractBodyRewrite = abstractRewriter.getListRewrite(abstractClassTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		
		MethodDeclaration abstractMethodDeclaration = abstractAST.newMethodDeclaration();
		String abstractMethodName = typeCheckElimination.getTypeCheckMethodName();
		abstractRewriter.set(abstractMethodDeclaration, MethodDeclaration.NAME_PROPERTY, abstractAST.newSimpleName(abstractMethodName), null);
		abstractRewriter.set(abstractMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, typeCheckElimination.getTypeCheckMethodReturnType(), null);
		ListRewrite abstractMethodModifiersRewrite = abstractRewriter.getListRewrite(abstractMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		abstractMethodModifiersRewrite.insertLast(abstractAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
		abstractMethodModifiersRewrite.insertLast(abstractAST.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
		for(SingleVariableDeclaration abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
			ListRewrite abstractMethodParametersRewrite = abstractRewriter.getListRewrite(abstractMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
			abstractMethodParametersRewrite.insertLast(abstractMethodParameter, null);
		}
		if(typeCheckElimination.getAccessedFields().size() > 0) {
			ListRewrite abstractMethodParametersRewrite = abstractRewriter.getListRewrite(abstractMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
			SingleVariableDeclaration parameter = abstractAST.newSingleVariableDeclaration();
			SimpleName parameterType = abstractAST.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
			abstractRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, abstractAST.newSimpleType(parameterType), null);
			String parameterName = sourceTypeDeclaration.getName().getIdentifier();
			parameterName = parameterName.substring(0,1).toLowerCase() + parameterName.substring(1,parameterName.length());
			abstractRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, abstractAST.newSimpleName(parameterName), null);
			abstractMethodParametersRewrite.insertLast(parameter, null);
		}
		
		abstractBodyRewrite.insertLast(abstractMethodDeclaration, null);
		
		ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
		ITextFileBuffer abstractTextFileBuffer = bufferManager.getTextFileBuffer(abstractClassFile.getFullPath(), LocationKind.IFILE);
		IDocument abstractDocument = abstractTextFileBuffer.getDocument();
		TextEdit abstractEdit = abstractRewriter.rewriteAST(abstractDocument, null);
		try {
			UndoEdit abstractUndoEdit = abstractEdit.apply(abstractDocument, UndoEdit.CREATE_UNDO);
			undoRefactoring.put(abstractClassFile, abstractDocument, abstractUndoEdit);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		abstractEditor.doSave(null);
		
		
		Collection<ArrayList<Statement>> typeCheckStatements = typeCheckElimination.getTypeCheckStatements();
		List<String> subclassNames = typeCheckElimination.getSubclassNames();
		int i = 0;
		for(ArrayList<Statement> statements : typeCheckStatements) {
			IFile subclassFile = getFile(rootContainer, subclassNames.get(i));
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
			
			TypeDeclaration subclassTypeDeclaration = null;
			List<AbstractTypeDeclaration> subclassAbstractTypeDeclarations = subclassCompilationUnit.types();
			for(AbstractTypeDeclaration abstractTypeDeclaration : subclassAbstractTypeDeclarations) {
				if(abstractTypeDeclaration instanceof TypeDeclaration) {
					TypeDeclaration typeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
					if(typeDeclaration.resolveBinding().getQualifiedName().equals(subclassNames.get(i))) {
						subclassTypeDeclaration = typeDeclaration;
						break;
					}
				}
			}
			
			ListRewrite subclassBodyRewrite = subclassRewriter.getListRewrite(subclassTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			
			MethodDeclaration concreteMethodDeclaration = subclassAST.newMethodDeclaration();
			String concreteMethodName = typeCheckElimination.getTypeCheckMethodName();
			subclassRewriter.set(concreteMethodDeclaration, MethodDeclaration.NAME_PROPERTY, subclassAST.newSimpleName(concreteMethodName), null);
			subclassRewriter.set(concreteMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, typeCheckElimination.getTypeCheckMethodReturnType(), null);
			ListRewrite concreteMethodModifiersRewrite = subclassRewriter.getListRewrite(concreteMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
			concreteMethodModifiersRewrite.insertLast(subclassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
			for(SingleVariableDeclaration abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
				ListRewrite concreteMethodParametersRewrite = subclassRewriter.getListRewrite(concreteMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
				concreteMethodParametersRewrite.insertLast(abstractMethodParameter, null);
			}
			Set<VariableDeclarationFragment> accessedFields = typeCheckElimination.getAccessedFields();
			if(accessedFields.size() > 0) {
				ListRewrite concreteMethodParametersRewrite = subclassRewriter.getListRewrite(concreteMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
				SingleVariableDeclaration parameter = subclassAST.newSingleVariableDeclaration();
				SimpleName parameterType = subclassAST.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
				subclassRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, subclassAST.newSimpleType(parameterType), null);
				String parameterName = sourceTypeDeclaration.getName().getIdentifier();
				parameterName = parameterName.substring(0,1).toLowerCase() + parameterName.substring(1,parameterName.length());
				subclassRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, subclassAST.newSimpleName(parameterName), null);
				concreteMethodParametersRewrite.insertLast(parameter, null);
			}
			
			VariableDeclarationFragment returnedVariable = typeCheckElimination.getTypeCheckMethodReturnedVariable();
			if(statements.size() == 1 && statements.get(0) instanceof Block) {
				Block newBlock = (Block)ASTNode.copySubtree(subclassAST, statements.get(0));
				if(returnedVariable != null) {
					ListRewrite concreteMethodBodyRewrite = subclassRewriter.getListRewrite(newBlock, Block.STATEMENTS_PROPERTY);
					VariableDeclarationFragment variableDeclarationFragment = subclassAST.newVariableDeclarationFragment();
					subclassRewriter.set(variableDeclarationFragment, VariableDeclarationFragment.NAME_PROPERTY, returnedVariable.getName(), null);
					subclassRewriter.set(variableDeclarationFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, returnedVariable.getInitializer(), null);
					VariableDeclarationStatement variableDeclarationStatement = subclassAST.newVariableDeclarationStatement(variableDeclarationFragment);
					subclassRewriter.set(variableDeclarationStatement, VariableDeclarationStatement.TYPE_PROPERTY, typeCheckElimination.getTypeCheckMethodReturnType(), null);
					concreteMethodBodyRewrite.insertFirst(variableDeclarationStatement, null);
					ReturnStatement returnStatement = subclassAST.newReturnStatement();
					subclassRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, returnedVariable.getName(), null);
					concreteMethodBodyRewrite.insertLast(returnStatement, null);
				}
				List<Statement> blockStatements = newBlock.statements();
				SimpleName invokerSimpleName = null;
				for(Statement statement : blockStatements) {
					if(statement instanceof VariableDeclarationStatement) {
						VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
						List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
						VariableDeclarationFragment fragment = fragments.get(0);
						if(fragment.getInitializer() instanceof CastExpression) {
							invokerSimpleName = fragment.getName();
							ListRewrite concreteMethodBodyRewrite = subclassRewriter.getListRewrite(newBlock, Block.STATEMENTS_PROPERTY);
							concreteMethodBodyRewrite.remove(variableDeclarationStatement, null);
						}
					}
					if(invokerSimpleName != null) {
						ExpressionExtractor expressionExtractor = new ExpressionExtractor();
						List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(statement);
						for(Expression expression : methodInvocations) {
							if(expression instanceof MethodInvocation) {
								MethodInvocation methodInvocation = (MethodInvocation)expression;
								Expression methodInvocationExpression = methodInvocation.getExpression();
								if(methodInvocationExpression instanceof SimpleName) {
									SimpleName simpleName = (SimpleName)methodInvocationExpression;
									if(simpleName.getIdentifier().equals(invokerSimpleName.getIdentifier())) {
										subclassRewriter.remove(simpleName, null);
									}
								}
							}
						}
					}
				}
				subclassRewriter.set(concreteMethodDeclaration, MethodDeclaration.BODY_PROPERTY, newBlock, null);
			}
			else {
				Block concreteMethodBody = subclassAST.newBlock();
				ListRewrite concreteMethodBodyRewrite = subclassRewriter.getListRewrite(concreteMethodBody, Block.STATEMENTS_PROPERTY);
				if(returnedVariable != null) {
					VariableDeclarationFragment variableDeclarationFragment = subclassAST.newVariableDeclarationFragment();
					subclassRewriter.set(variableDeclarationFragment, VariableDeclarationFragment.NAME_PROPERTY, returnedVariable.getName(), null);
					subclassRewriter.set(variableDeclarationFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, returnedVariable.getInitializer(), null);
					VariableDeclarationStatement variableDeclarationStatement = subclassAST.newVariableDeclarationStatement(variableDeclarationFragment);
					subclassRewriter.set(variableDeclarationStatement, VariableDeclarationStatement.TYPE_PROPERTY, typeCheckElimination.getTypeCheckMethodReturnType(), null);
					concreteMethodBodyRewrite.insertFirst(variableDeclarationStatement, null);
				}
				SimpleName invokerSimpleName = null;
				for(Statement statement : statements) {
					Statement newStatement = (Statement)ASTNode.copySubtree(subclassAST, statement);
					boolean insert = true;
					if(newStatement instanceof VariableDeclarationStatement) {
						VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)newStatement;
						List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
						VariableDeclarationFragment fragment = fragments.get(0);
						if(fragment.getInitializer() instanceof CastExpression) {
							invokerSimpleName = fragment.getName();
							insert = false;
						}
					}
					if(invokerSimpleName != null) {
						ExpressionExtractor expressionExtractor = new ExpressionExtractor();
						List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(newStatement);
						for(Expression expression : methodInvocations) {
							if(expression instanceof MethodInvocation) {
								MethodInvocation methodInvocation = (MethodInvocation)expression;
								Expression methodInvocationExpression = methodInvocation.getExpression();
								if(methodInvocationExpression instanceof SimpleName) {
									SimpleName simpleName = (SimpleName)methodInvocationExpression;
									if(simpleName.getIdentifier().equals(invokerSimpleName.getIdentifier())) {
										subclassRewriter.remove(simpleName, null);
									}
								}
							}
						}
					}
					if(insert)
						concreteMethodBodyRewrite.insertLast(newStatement, null);
				}
				if(returnedVariable != null) {
					ReturnStatement returnStatement = subclassAST.newReturnStatement();
					subclassRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, returnedVariable.getName(), null);
					concreteMethodBodyRewrite.insertLast(returnStatement, null);
				}
				subclassRewriter.set(concreteMethodDeclaration, MethodDeclaration.BODY_PROPERTY, concreteMethodBody, null);
			}
			
			subclassBodyRewrite.insertLast(concreteMethodDeclaration, null);
			
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

	private IFile getFile(IContainer rootContainer, String fullyQualifiedClassName) {
		String[] subPackages = fullyQualifiedClassName.split("\\.");
		IContainer classContainer = rootContainer;
		IFile classFile = null;
		for(int i = 0; i<subPackages.length; i++) {
			try {
				if(i == subPackages.length-1) {
					IResource[] resources = classContainer.members();
					for(IResource resource : resources) {
						if(resource instanceof IFile) {
							IFile file = (IFile)resource;
							if(file.getName().equals(subPackages[i] + ".java")) {
								classFile = file;
								break;
							}
						}
					}
				}
				else {
					IResource[] resources = classContainer.members();
					for(IResource resource : resources) {
						if(resource instanceof IFolder) {
							IContainer container = (IContainer)resource;
							if(container.getName().equals(subPackages[i])) {
								classContainer = container;
								break;
							}
						}
					}
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return classFile;
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
