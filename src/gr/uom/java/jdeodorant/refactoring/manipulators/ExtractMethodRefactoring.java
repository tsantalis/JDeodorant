package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.util.ExpressionExtractor;

import java.util.ArrayList;
import java.util.List;

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

public class ExtractMethodRefactoring {
	private IFile sourceFile;
	private TypeDeclaration sourceTypeDeclaration;
	private MethodDeclaration sourceMethodDeclaration;
	private VariableDeclarationStatement variableDeclarationStatement;
	private VariableDeclarationFragment variableDeclarationFragment;
	private List<Statement> extractStatementList;
	//includes all variable declaration statements which are related with the extracted method
	private List<VariableDeclarationStatement> variableDeclarationStatementList;
	
	public ExtractMethodRefactoring(IFile sourceFile, TypeDeclaration sourceTypeDeclaration, MethodDeclaration sourceMethodDeclaration, 
			VariableDeclarationStatement variableDeclarationStatement, VariableDeclarationFragment variableDeclarationFragment, List<Statement> extractStatementList, List<VariableDeclarationStatement> variableDeclarationStatementList) {
		this.sourceFile = sourceFile;
		this.sourceTypeDeclaration = sourceTypeDeclaration;
		this.sourceMethodDeclaration = sourceMethodDeclaration;
		this.variableDeclarationStatement = variableDeclarationStatement;
		this.variableDeclarationFragment = variableDeclarationFragment;
		this.extractStatementList = extractStatementList;
		this.variableDeclarationStatementList = variableDeclarationStatementList;
	}

	public void extractMethod() {
		AST ast = sourceTypeDeclaration.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		MethodDeclaration newMethodDeclaration = ast.newMethodDeclaration();
		
		SimpleName returnVariableSimpleName = variableDeclarationFragment.getName();
		rewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, returnVariableSimpleName, null);

		rewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, variableDeclarationStatement.getType(), null);
		
		ListRewrite modifierRewrite = rewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		Modifier modifier = newMethodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
		modifierRewrite.insertLast(modifier, null);
		
		ExpressionExtractor extractor = new ExpressionExtractor();
		List<SimpleName> discreteLocalVariableInstructions = new ArrayList<SimpleName>();
		List<String> discreteLocalVariableInstructionIdentifiers = new ArrayList<String>();
		for(Statement statement : extractStatementList) {
			List<Expression> list = extractor.getVariableInstructions(statement);
			for(Expression expression : list) {
				SimpleName simpleName = (SimpleName)expression;
				IBinding binding = simpleName.resolveBinding();
				if(binding.getKind() == IBinding.VARIABLE) {
					IVariableBinding variableBinding = (IVariableBinding)binding;
					if(!variableBinding.isField() && !simpleName.isDeclaration()) {
						if(!discreteLocalVariableInstructionIdentifiers.contains(simpleName.getIdentifier())) {
							discreteLocalVariableInstructionIdentifiers.add(simpleName.getIdentifier());
							discreteLocalVariableInstructions.add(simpleName);
						}
					}
				}
			}
		}
		
		ListRewrite paramRewrite = rewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		for(SimpleName instruction : discreteLocalVariableInstructions) {
			if(!returnVariableSimpleName.getIdentifier().equals(instruction.getIdentifier())) {
				Type paramVariableDeclarationType = null;
				for(VariableDeclarationStatement statement : variableDeclarationStatementList) {
					List<VariableDeclarationFragment> fragmentList = statement.fragments();
					for(VariableDeclarationFragment fragment : fragmentList) {
						if(fragment.getName().getIdentifier().equals(instruction.getIdentifier()))
							paramVariableDeclarationType = statement.getType();
					}
				}
				if(paramVariableDeclarationType != null) {
					SingleVariableDeclaration newParam = newMethodDeclaration.getAST().newSingleVariableDeclaration();
					rewriter.set(newParam, SingleVariableDeclaration.NAME_PROPERTY, instruction, null);
					rewriter.set(newParam, SingleVariableDeclaration.TYPE_PROPERTY, paramVariableDeclarationType, null);
					paramRewrite.insertLast(newParam, null);
				}
			}
		}
		
		Block newMethodBody = newMethodDeclaration.getAST().newBlock();
		ListRewrite bodyRewrite = rewriter.getListRewrite(newMethodBody, Block.STATEMENTS_PROPERTY);
		VariableDeclarationFragment localVariableDeclarationFragment = newMethodBody.getAST().newVariableDeclarationFragment();
		rewriter.set(localVariableDeclarationFragment, VariableDeclarationFragment.NAME_PROPERTY, variableDeclarationFragment.getName(), null);
		rewriter.set(localVariableDeclarationFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, variableDeclarationFragment.getInitializer(), null);
		
		VariableDeclarationStatement localVariableDeclarationStatement = 
			newMethodBody.getAST().newVariableDeclarationStatement(localVariableDeclarationFragment);
		rewriter.set(localVariableDeclarationStatement, VariableDeclarationStatement.TYPE_PROPERTY, variableDeclarationStatement.getType(), null);
		bodyRewrite.insertLast(localVariableDeclarationStatement, null);
		for(Statement statement : extractStatementList) {
			bodyRewrite.insertLast(statement, null);
		}
		ReturnStatement returnStatement = newMethodBody.getAST().newReturnStatement();
		rewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, returnVariableSimpleName, null);

		bodyRewrite.insertLast(returnStatement, null);
		rewriter.set(newMethodDeclaration, MethodDeclaration.BODY_PROPERTY, newMethodBody, null);
		
		ListRewrite methodDeclarationRewrite = rewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		methodDeclarationRewrite.insertLast(newMethodDeclaration, null);
		
		replaceExtractedCodeWithMethodInvocation(rewriter);
		
		ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
		ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(sourceFile.getFullPath(), LocationKind.IFILE);
		IDocument document = textFileBuffer.getDocument();
		TextEdit edit = rewriter.rewriteAST(document, null);
		try {
			edit.apply(document);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
	
	private void replaceExtractedCodeWithMethodInvocation(ASTRewrite rewriter) {
		ASTNode parent = extractStatementList.get(0).getParent();
		Assignment assignment = parent.getAST().newAssignment();
		rewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, variableDeclarationFragment.getName(), null);
		if(variableDeclarationStatement.getType().isPrimitiveType() && parent != variableDeclarationStatement.getParent() && isLoop(parent))
			rewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.PLUS_ASSIGN, null);
		else
			rewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.ASSIGN, null);
		
		MethodInvocation methodInvocation = assignment.getAST().newMethodInvocation();
		rewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY, variableDeclarationFragment.getName(), null);
		ListRewrite argumentRewrite = rewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
		
		for(VariableDeclarationStatement statement : variableDeclarationStatementList) {
			List<VariableDeclarationFragment> fragmentList = statement.fragments();
			for(VariableDeclarationFragment fragment : fragmentList) {
				if(!fragment.getName().getIdentifier().equals(variableDeclarationFragment.getName().getIdentifier()))
					argumentRewrite.insertLast(fragment.getName(), null);
			}
		}
		rewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, methodInvocation, null);
		
		ExpressionStatement expressionStatement = parent.getAST().newExpressionStatement(assignment);
		
		for(int i=0; i<extractStatementList.size(); i++) {
			Statement statement = extractStatementList.get(i);
			ListRewrite bodyRewrite = rewriter.getListRewrite(statement.getParent(), Block.STATEMENTS_PROPERTY);
			if(i == extractStatementList.size()-1)
				bodyRewrite.replace(statement, expressionStatement, null);
			else
				bodyRewrite.remove(statement, null);
		}
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
