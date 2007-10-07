package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.util.ExpressionExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;

public class ExtractMethodRefactoring implements Refactoring {
	private IFile sourceFile;
	private TypeDeclaration sourceTypeDeclaration;
	private MethodDeclaration sourceMethodDeclaration;
	private ASTExtractionBlock extractionBlock;
	private ASTRewrite sourceRewriter;
	private UndoRefactoring undoRefactoring;
	
	public ExtractMethodRefactoring(IFile sourceFile, TypeDeclaration sourceTypeDeclaration, MethodDeclaration sourceMethodDeclaration, 
			ASTExtractionBlock extractionBlock) {
		this.sourceFile = sourceFile;
		this.sourceTypeDeclaration = sourceTypeDeclaration;
		this.sourceMethodDeclaration = sourceMethodDeclaration;
		this.extractionBlock = extractionBlock;
		this.sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		this.undoRefactoring = new UndoRefactoring();
	}

	public UndoRefactoring getUndoRefactoring() {
		return undoRefactoring;
	}

	public void apply() {
		extractMethod();
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

	private void extractMethod() {
		AST ast = sourceTypeDeclaration.getAST();
		MethodDeclaration newMethodDeclaration = ast.newMethodDeclaration();
		
		SimpleName returnVariableSimpleName = extractionBlock.getReturnVariableDeclarationFragment().getName();
		sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, ast.newSimpleName(extractionBlock.getExtractedMethodName()), null);

		sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, extractionBlock.getReturnVariableDeclarationStatement().getType(), null);
		
		ListRewrite modifierRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		Modifier modifier = newMethodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
		modifierRewrite.insertLast(modifier, null);
		
		ExpressionExtractor extractor = new ExpressionExtractor();
		List<SimpleName> extractedMethodArguments = new ArrayList<SimpleName>();
		List<String> extractedMethodArgumentIdentifiers = new ArrayList<String>();
		List<String> variableDeclarationIdentifiers = new ArrayList<String>();
		for(VariableDeclarationFragment fragment : extractionBlock.getAdditionalRequiredVariableDeclarationFragments())
			variableDeclarationIdentifiers.add(fragment.getName().getIdentifier());
		if(extractionBlock.getParentStatementForCopy() != null) {
			Statement parentStatementForCopy = extractionBlock.getParentStatementForCopy();
			if(parentStatementForCopy.getNodeType() == Statement.IF_STATEMENT) {
				IfStatement ifStatement = (IfStatement)parentStatementForCopy;
				List<Expression> list = extractor.getVariableInstructions(ifStatement.getExpression());
				for(Expression expression : list) {
					SimpleName simpleName = (SimpleName)expression;
					IBinding binding = simpleName.resolveBinding();
					if(binding.getKind() == IBinding.VARIABLE) {
						IVariableBinding variableBinding = (IVariableBinding)binding;
						String simpleNameIdentifier = simpleName.getIdentifier();
						if(!variableBinding.isField() && !simpleName.isDeclaration() && !returnVariableSimpleName.getIdentifier().equals(simpleNameIdentifier)) {
							if(!extractedMethodArgumentIdentifiers.contains(simpleNameIdentifier) && !variableDeclarationIdentifiers.contains(simpleNameIdentifier)) {
								extractedMethodArgumentIdentifiers.add(simpleNameIdentifier);
								extractedMethodArguments.add(simpleName);
							}
						}
						else {
							if(!variableDeclarationIdentifiers.contains(simpleNameIdentifier))
								variableDeclarationIdentifiers.add(simpleNameIdentifier);
						}
					}
				}
			}
		}
		for(Statement statement : extractionBlock.getStatementsForExtraction()) {
			List<Expression> list = extractor.getVariableInstructions(statement);
			for(Expression expression : list) {
				SimpleName simpleName = (SimpleName)expression;
				IBinding binding = simpleName.resolveBinding();
				if(binding.getKind() == IBinding.VARIABLE) {
					IVariableBinding variableBinding = (IVariableBinding)binding;
					String simpleNameIdentifier = simpleName.getIdentifier();
					if(!variableBinding.isField() && !simpleName.isDeclaration() && !returnVariableSimpleName.getIdentifier().equals(simpleNameIdentifier)) {
						if(!extractedMethodArgumentIdentifiers.contains(simpleNameIdentifier) && !variableDeclarationIdentifiers.contains(simpleNameIdentifier)) {
							extractedMethodArgumentIdentifiers.add(simpleNameIdentifier);
							extractedMethodArguments.add(simpleName);
						}
					}
					else {
						if(!variableDeclarationIdentifiers.contains(simpleNameIdentifier))
							variableDeclarationIdentifiers.add(simpleNameIdentifier);
					}
				}
			}
		}
		
		ListRewrite paramRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		for(SimpleName argument : extractedMethodArguments) {
			Type argumentVariableDeclarationType = null;
			for(VariableDeclarationStatement statement : extractionBlock.getAllVariableDeclarationStatements()) {
				List<VariableDeclarationFragment> fragmentList = statement.fragments();
				for(VariableDeclarationFragment fragment : fragmentList) {
					if(fragment.getName().getIdentifier().equals(argument.getIdentifier())) {
						argumentVariableDeclarationType = statement.getType();
						break;
					}
				}
			}
			if(argumentVariableDeclarationType == null) {
				List<SingleVariableDeclaration> sourceMethodParameters = sourceMethodDeclaration.parameters();
				for(SingleVariableDeclaration variableDeclaration : sourceMethodParameters) {
					if(variableDeclaration.getName().getIdentifier().equals(argument.getIdentifier())) {
						argumentVariableDeclarationType = variableDeclaration.getType();
						break;
					}
				}
			}
			if(argumentVariableDeclarationType != null) {
				SingleVariableDeclaration newParam = newMethodDeclaration.getAST().newSingleVariableDeclaration();
				sourceRewriter.set(newParam, SingleVariableDeclaration.NAME_PROPERTY, argument, null);
				sourceRewriter.set(newParam, SingleVariableDeclaration.TYPE_PROPERTY, argumentVariableDeclarationType, null);
				paramRewrite.insertLast(newParam, null);
			}
		}
		
		Set<String> thrownExceptions = extractionBlock.getThrownExceptions();
		ListRewrite thrownExceptionRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
		for(String thrownException : thrownExceptions) {
			SimpleName simpleName = ast.newSimpleName(thrownException);
			thrownExceptionRewrite.insertLast(simpleName, null);
		}
		
		Block newMethodBody = newMethodDeclaration.getAST().newBlock();
		ListRewrite bodyRewrite = sourceRewriter.getListRewrite(newMethodBody, Block.STATEMENTS_PROPERTY);
		VariableDeclarationFragment returnVariableDeclarationFragment = newMethodBody.getAST().newVariableDeclarationFragment();
		sourceRewriter.set(returnVariableDeclarationFragment, VariableDeclarationFragment.NAME_PROPERTY, extractionBlock.getReturnVariableDeclarationFragment().getName(), null);
		sourceRewriter.set(returnVariableDeclarationFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, extractionBlock.getReturnVariableDeclarationFragment().getInitializer(), null);
		VariableDeclarationStatement returnVariableDeclarationStatement = 
			newMethodBody.getAST().newVariableDeclarationStatement(returnVariableDeclarationFragment);
		sourceRewriter.set(returnVariableDeclarationStatement, VariableDeclarationStatement.TYPE_PROPERTY, extractionBlock.getReturnVariableDeclarationStatement().getType(), null);
		bodyRewrite.insertLast(returnVariableDeclarationStatement, null);
		
		for(VariableDeclarationFragment fragment : extractionBlock.getAdditionalRequiredVariableDeclarationFragments()) {
			VariableDeclarationFragment variableDeclarationFragment = newMethodBody.getAST().newVariableDeclarationFragment();
			sourceRewriter.set(variableDeclarationFragment, VariableDeclarationFragment.NAME_PROPERTY, fragment.getName(), null);
			sourceRewriter.set(variableDeclarationFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, fragment.getInitializer(), null);
			VariableDeclarationStatement variableDeclarationStatement = 
				newMethodBody.getAST().newVariableDeclarationStatement(variableDeclarationFragment);
			sourceRewriter.set(variableDeclarationStatement, VariableDeclarationStatement.TYPE_PROPERTY, extractionBlock.getAdditionalRequiredVariableDeclarationStatement(fragment).getType(), null);
			bodyRewrite.insertLast(variableDeclarationStatement, null);
		}
		
		if(extractionBlock.getParentStatementForCopy() == null) {
			for(Statement statement : extractionBlock.getStatementsForExtraction()) {
				bodyRewrite.insertLast(statement, null);
			}
		}
		else {
			Statement parentStatement = extractionBlock.getParentStatementForCopy();
			Statement copiedParentStatement = (Statement)ASTNode.copySubtree(ast, parentStatement);
			if(copiedParentStatement.getNodeType() == ASTNode.IF_STATEMENT) {
				IfStatement oldIfStatement = (IfStatement)parentStatement;
				IfStatement newIfStatement = (IfStatement)copiedParentStatement;
				modifyExtractionBlock(oldIfStatement, newIfStatement);
			}
			bodyRewrite.insertLast(copiedParentStatement, null);
		}
		
		ReturnStatement returnStatement = newMethodBody.getAST().newReturnStatement();
		sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, returnVariableSimpleName, null);

		bodyRewrite.insertLast(returnStatement, null);
		sourceRewriter.set(newMethodDeclaration, MethodDeclaration.BODY_PROPERTY, newMethodBody, null);
		
		ListRewrite methodDeclarationRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		methodDeclarationRewrite.insertLast(newMethodDeclaration, null);
		
		if(extractionBlock.getParentStatementForCopy() == null) {
			replaceExtractedCodeWithMethodInvocation(extractedMethodArguments);
		}
		else {
			insertMethodInvocationBeforeParentStatement(extractedMethodArguments);
		}
	}
	
	private void modifyExtractionBlock(IfStatement oldIfStatement, IfStatement newIfStatement) {
		Statement newThenStatement = newIfStatement.getThenStatement();
		if(newThenStatement.getNodeType() == ASTNode.BLOCK) {
			Block oldThenBlock = (Block)oldIfStatement.getThenStatement();
			Block newThenBlock = (Block)newThenStatement;
			ListRewrite oldBlockRewrite = sourceRewriter.getListRewrite(oldThenBlock, Block.STATEMENTS_PROPERTY);
			ListRewrite newBlockRewrite = sourceRewriter.getListRewrite(newThenBlock, Block.STATEMENTS_PROPERTY);
			List<Statement> oldStatementList = oldThenBlock.statements();
			List<Statement> newStatementList = newThenBlock.statements();
			int i = 0;
			for(Statement statement : oldStatementList) {
				if(extractionBlock.getStatementsForExtraction().contains(statement))
					oldBlockRewrite.remove(statement, null);
				else
					newBlockRewrite.remove(newStatementList.get(i), null);
				i++;
			}
		}
		Statement newElseStatement = newIfStatement.getElseStatement();
		if(newElseStatement.getNodeType() == ASTNode.BLOCK) {
			Block oldElseBlock = (Block)oldIfStatement.getElseStatement();
			Block newElseBlock = (Block)newElseStatement;
			ListRewrite oldBlockRewrite = sourceRewriter.getListRewrite(oldElseBlock, Block.STATEMENTS_PROPERTY);
			ListRewrite newBlockRewrite = sourceRewriter.getListRewrite(newElseBlock, Block.STATEMENTS_PROPERTY);
			List<Statement> oldStatementList = oldElseBlock.statements();
			List<Statement> newStatementList = newElseBlock.statements();
			int i = 0;
			for(Statement statement : oldStatementList) {
				if(extractionBlock.getStatementsForExtraction().contains(statement))
					oldBlockRewrite.remove(statement, null);
				else
					newBlockRewrite.remove(newStatementList.get(i), null);
				i++;
			}
		}
		else if(newElseStatement.getNodeType() == ASTNode.IF_STATEMENT) {
			IfStatement oldIfStatement2 = (IfStatement)oldIfStatement.getElseStatement();
			IfStatement newIfStatement2 = (IfStatement)newElseStatement;
			modifyExtractionBlock(oldIfStatement2, newIfStatement2);
		}
	}

	private void replaceExtractedCodeWithMethodInvocation(List<SimpleName> extractedMethodArguments) {
		ASTNode parentBlock = extractionBlock.getStatementsForExtraction().get(0).getParent();
		Assignment assignment = parentBlock.getAST().newAssignment();
		sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, extractionBlock.getReturnVariableDeclarationFragment().getName(), null);
		if(extractionBlock.getReturnVariableDeclarationStatement().getType().isPrimitiveType() && parentBlock != extractionBlock.getReturnVariableDeclarationStatement().getParent() &&
				isLoop(parentBlock) && extractionBlock.getReturnVariableDeclarationFragment().getInitializer() != null) {
			if(extractionBlock.allAssignmentOperatorsContainPlus())
				sourceRewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.PLUS_ASSIGN, null);
			else if(extractionBlock.allAssignmentOperatorsContainMinus())
				sourceRewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.MINUS_ASSIGN, null);
			else
				sourceRewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.ASSIGN, null);
		}
		else {
			sourceRewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.ASSIGN, null);
		}
		
		MethodInvocation methodInvocation = assignment.getAST().newMethodInvocation();
		sourceRewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY, parentBlock.getAST().newSimpleName(extractionBlock.getExtractedMethodName()), null);
		ListRewrite argumentRewrite = sourceRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
		
		for(SimpleName argument : extractedMethodArguments) {
			argumentRewrite.insertLast(argument, null);
		}
		sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, methodInvocation, null);
		
		ExpressionStatement expressionStatement = parentBlock.getAST().newExpressionStatement(assignment);
		
		for(int i=0; i<extractionBlock.getStatementsForExtraction().size(); i++) {
			Statement statement = extractionBlock.getStatementsForExtraction().get(i);
			ListRewrite bodyRewrite = sourceRewriter.getListRewrite(statement.getParent(), Block.STATEMENTS_PROPERTY);
			if(i == extractionBlock.getStatementsForExtraction().size()-1)
				bodyRewrite.replace(statement, expressionStatement, null);
			else
				bodyRewrite.remove(statement, null);
		}
	}
	
	private void insertMethodInvocationBeforeParentStatement(List<SimpleName> extractedMethodArguments) {
		ASTNode parentBlock = extractionBlock.getParentStatementForCopy().getParent();
		Assignment assignment = parentBlock.getAST().newAssignment();
		sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, extractionBlock.getReturnVariableDeclarationFragment().getName(), null);
		sourceRewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.ASSIGN, null);
		MethodInvocation methodInvocation = assignment.getAST().newMethodInvocation();
		sourceRewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY, parentBlock.getAST().newSimpleName(extractionBlock.getExtractedMethodName()), null);
		ListRewrite argumentRewrite = sourceRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
		
		for(SimpleName argument : extractedMethodArguments) {
			argumentRewrite.insertLast(argument, null);
		}
		sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, methodInvocation, null);
		
		ExpressionStatement expressionStatement = parentBlock.getAST().newExpressionStatement(assignment);
		ListRewrite bodyRewrite = sourceRewriter.getListRewrite(parentBlock, Block.STATEMENTS_PROPERTY);
		bodyRewrite.insertBefore(expressionStatement, extractionBlock.getParentStatementForCopy(), null);
	}
	
	private boolean isLoop(ASTNode node) {
		if(node instanceof Block)
			return isLoop(node.getParent());
		else if(node instanceof ForStatement)
			return true;
		else if(node instanceof EnhancedForStatement)
			return true;
		else if(node instanceof WhileStatement)
			return true;
		else if(node instanceof DoStatement)
			return true;
		return false;
	}
}
