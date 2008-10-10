package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.decomposition.cfg.GraphEdge;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGControlPredicateNode;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PDGStatementNode;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
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
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class ExtractMethodRefactoring extends Refactoring {
	private ASTSlice slice;
	private CompilationUnit sourceCompilationUnit;
	private TypeDeclaration sourceTypeDeclaration;
	private MethodDeclaration sourceMethodDeclaration;
	private ASTRewrite sourceRewriter;
	private Map<ICompilationUnit, TextFileChange> fChanges;
	
	public ExtractMethodRefactoring(CompilationUnit sourceCompilationUnit, ASTSlice slice) {
		this.slice = slice;
		this.sourceCompilationUnit = sourceCompilationUnit;
		this.sourceTypeDeclaration = slice.getSourceTypeDeclaration();
		this.sourceMethodDeclaration = slice.getSourceMethodDeclaration();
		this.sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		this.fChanges = new LinkedHashMap<ICompilationUnit, TextFileChange>();
	}

	public void apply() {
		extractMethod();
		modifySourceMethod();
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
			TextFileChange change = fChanges.get(sourceICompilationUnit);
			if (change == null) {
				change = new TextFileChange(sourceICompilationUnit.getElementName(), (IFile)sourceICompilationUnit.getResource());
				change.setTextType("java");
				change.setEdit(sourceEdit);
			} else
				change.getEdit().addChild(sourceEdit);
			fChanges.put(sourceICompilationUnit, change);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	private void modifySourceMethod() {
		AST ast = sourceMethodDeclaration.getAST();
		MethodInvocation extractedMethodInvocation = ast.newMethodInvocation();
		sourceRewriter.set(extractedMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(slice.getExtractedMethodName()), null);
		ListRewrite argumentRewrite = sourceRewriter.getListRewrite(extractedMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
		for(VariableDeclaration variableDeclaration : slice.getPassedParameters()) {
			argumentRewrite.insertLast(variableDeclaration.getName(), null);
		}
		
		if(slice.declarationOfVariableCriterionBelongsToSliceNodes() && slice.declarationOfVariableCriterionBelongsToRemovableNodes()) {
			VariableDeclaration returnedVariableDeclaration = slice.getLocalVariableCriterion();
			VariableDeclarationFragment initializationFragment = ast.newVariableDeclarationFragment();
			sourceRewriter.set(initializationFragment, VariableDeclarationFragment.NAME_PROPERTY, returnedVariableDeclaration.getName(), null);
			sourceRewriter.set(initializationFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, extractedMethodInvocation, null);
			VariableDeclarationStatement initializationVariableDeclarationStatement = ast.newVariableDeclarationStatement(initializationFragment);
			Type returnedVariableType = null;
			if(returnedVariableDeclaration instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)returnedVariableDeclaration;
				returnedVariableType = singleVariableDeclaration.getType();
			}
			else if(returnedVariableDeclaration instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment)returnedVariableDeclaration;
				VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)fragment.getParent();
				returnedVariableType = variableDeclarationStatement.getType();
			}
			sourceRewriter.set(initializationVariableDeclarationStatement, VariableDeclarationStatement.TYPE_PROPERTY, returnedVariableType, null);
			Statement extractedMethodInvocationInsertionStatement = slice.getExtractedMethodInvocationInsertionStatement();
			Block parentStatement = (Block)extractedMethodInvocationInsertionStatement.getParent();
			ListRewrite blockRewrite = sourceRewriter.getListRewrite(parentStatement, Block.STATEMENTS_PROPERTY);
			blockRewrite.insertBefore(initializationVariableDeclarationStatement, extractedMethodInvocationInsertionStatement, null);
		}
		else if(slice.declarationOfVariableCriterionBelongsToSliceNodes() && !slice.declarationOfVariableCriterionBelongsToRemovableNodes()) {
			VariableDeclaration returnedVariableDeclaration = slice.getLocalVariableCriterion();
			if(returnedVariableDeclaration instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment initializationFragment = (VariableDeclarationFragment)returnedVariableDeclaration;
				sourceRewriter.set(initializationFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, extractedMethodInvocation, null);
			}
		}
		else {
			Assignment assignment = ast.newAssignment();
			sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, slice.getLocalVariableCriterion().getName(), null);
			sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, extractedMethodInvocation, null);
			ExpressionStatement expressionStatement = ast.newExpressionStatement(assignment);
			Statement extractedMethodInvocationInsertionStatement = slice.getExtractedMethodInvocationInsertionStatement();
			Block parentStatement = (Block)extractedMethodInvocationInsertionStatement.getParent();
			ListRewrite blockRewrite = sourceRewriter.getListRewrite(parentStatement, Block.STATEMENTS_PROPERTY);
			blockRewrite.insertBefore(expressionStatement, extractedMethodInvocationInsertionStatement, null);
		}
		
		for(Statement removableStatement : slice.getRemovableStatements()) {
			sourceRewriter.remove(removableStatement, null);
		}
	}

	private void extractMethod() {
		AST ast = sourceTypeDeclaration.getAST();
		MethodDeclaration newMethodDeclaration = ast.newMethodDeclaration();
		
		VariableDeclaration returnedVariableDeclaration = slice.getLocalVariableCriterion();
		SimpleName returnedVariableSimpleName = returnedVariableDeclaration.getName();
		Type returnedVariableType = null;
		if(returnedVariableDeclaration instanceof SingleVariableDeclaration) {
			SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)returnedVariableDeclaration;
			returnedVariableType = singleVariableDeclaration.getType();
		}
		else if(returnedVariableDeclaration instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment)returnedVariableDeclaration;
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)fragment.getParent();
			returnedVariableType = variableDeclarationStatement.getType();
		}
		
		sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, ast.newSimpleName(slice.getExtractedMethodName()), null);
		sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnedVariableType, null);
		
		ListRewrite modifierRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		Modifier modifier = newMethodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD);
		modifierRewrite.insertLast(modifier, null);
		
		ListRewrite parameterRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		for(VariableDeclaration variableDeclaration : slice.getPassedParameters()) {
			Type variableType = null;
			if(variableDeclaration instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)variableDeclaration;
				variableType = singleVariableDeclaration.getType();
			}
			else if(variableDeclaration instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment)variableDeclaration;
				VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)fragment.getParent();
				variableType = variableDeclarationStatement.getType();
			}
			SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
			sourceRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, variableDeclaration.getName(), null);
			sourceRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, variableType, null);
			parameterRewrite.insertLast(parameter, null);
		}
		Block newMethodBody = newMethodDeclaration.getAST().newBlock();
		ListRewrite bodyRewrite = sourceRewriter.getListRewrite(newMethodBody, Block.STATEMENTS_PROPERTY);

		List<PDGNode> sliceNodes = new ArrayList<PDGNode>(slice.getSliceNodes());
		while(!sliceNodes.isEmpty()) {
			PDGNode node = sliceNodes.get(0);
			if(node instanceof PDGStatementNode) {
				bodyRewrite.insertLast(node.getASTStatement(), null);
				sliceNodes.remove(node);
			}
			else if(node instanceof PDGControlPredicateNode) {
				PDGControlPredicateNode predicateNode = (PDGControlPredicateNode)node;
				bodyRewrite.insertLast(processPredicateNode(predicateNode, ast, sliceNodes), null);
			}
		}
		
		ReturnStatement returnStatement = newMethodBody.getAST().newReturnStatement();
		sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, returnedVariableSimpleName, null);
		bodyRewrite.insertLast(returnStatement, null);
		
		sourceRewriter.set(newMethodDeclaration, MethodDeclaration.BODY_PROPERTY, newMethodBody, null);
		
		ListRewrite methodDeclarationRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		methodDeclarationRewrite.insertAfter(newMethodDeclaration, sourceMethodDeclaration, null);
	}

	private Statement processPredicateNode(PDGControlPredicateNode predicateNode, AST ast, List<PDGNode> sliceNodes) {
		Statement oldPredicateStatement = predicateNode.getASTStatement();
		sliceNodes.remove(predicateNode);
		Statement newPredicateStatement = null;
		if(oldPredicateStatement instanceof IfStatement) {
			IfStatement oldIfStatement = (IfStatement)oldPredicateStatement;
			IfStatement newIfStatement = ast.newIfStatement();
			newPredicateStatement = newIfStatement;
			sourceRewriter.set(newIfStatement, IfStatement.EXPRESSION_PROPERTY, oldIfStatement.getExpression(), null);
			Block thenBlock = ast.newBlock();
			ListRewrite thenBodyRewrite = sourceRewriter.getListRewrite(thenBlock, Block.STATEMENTS_PROPERTY);
			Block elseBlock = ast.newBlock();
			ListRewrite elseBodyRewrite = sourceRewriter.getListRewrite(elseBlock, Block.STATEMENTS_PROPERTY);
			Iterator<GraphEdge> outgoingDependenceIterator = predicateNode.getOutgoingDependenceIterator();
			int numberOfFalseControlDependencies = 0;
			while(outgoingDependenceIterator.hasNext()) {
				PDGDependence dependence = (PDGDependence)outgoingDependenceIterator.next();
				if(dependence instanceof PDGControlDependence) {
					PDGControlDependence controlDependence = (PDGControlDependence)dependence;
					PDGNode dstPDGNode = (PDGNode)controlDependence.getDst();
					if(sliceNodes.contains(dstPDGNode)) {
						ListRewrite listRewrite = null;
						if(controlDependence.isTrueControlDependence()) {
							listRewrite = thenBodyRewrite;
						}
						else {
							listRewrite = elseBodyRewrite;
							numberOfFalseControlDependencies++;
						}
						if(dstPDGNode instanceof PDGControlPredicateNode) {
							PDGControlPredicateNode dstPredicateNode = (PDGControlPredicateNode)dstPDGNode;
							listRewrite.insertLast(processPredicateNode(dstPredicateNode, ast, sliceNodes), null);
						}
						else {
							listRewrite.insertLast(dstPDGNode.getASTStatement(), null);
							sliceNodes.remove(dstPDGNode);
						}
					}
				}
			}
			sourceRewriter.set(newIfStatement, IfStatement.THEN_STATEMENT_PROPERTY, thenBlock, null);
			if(numberOfFalseControlDependencies > 0)
				sourceRewriter.set(newIfStatement, IfStatement.ELSE_STATEMENT_PROPERTY, elseBlock, null);
		}
		else {
			Block loopBlock = ast.newBlock();
			ListRewrite loopBodyRewrite = sourceRewriter.getListRewrite(loopBlock, Block.STATEMENTS_PROPERTY);
			Iterator<GraphEdge> outgoingDependenceIterator = predicateNode.getOutgoingDependenceIterator();
			while(outgoingDependenceIterator.hasNext()) {
				PDGDependence dependence = (PDGDependence)outgoingDependenceIterator.next();
				if(dependence instanceof PDGControlDependence) {
					PDGControlDependence controlDependence = (PDGControlDependence)dependence;
					PDGNode dstPDGNode = (PDGNode)controlDependence.getDst();
					if(sliceNodes.contains(dstPDGNode)) {
						if(dstPDGNode instanceof PDGControlPredicateNode) {
							PDGControlPredicateNode dstPredicateNode = (PDGControlPredicateNode)dstPDGNode;
							loopBodyRewrite.insertLast(processPredicateNode(dstPredicateNode, ast, sliceNodes), null);
						}
						else {
							loopBodyRewrite.insertLast(dstPDGNode.getASTStatement(), null);
							sliceNodes.remove(dstPDGNode);
						}
					}
				}
			}
			if(oldPredicateStatement instanceof WhileStatement) {
				WhileStatement oldWhileStatement = (WhileStatement)oldPredicateStatement;
				WhileStatement newWhileStatement = ast.newWhileStatement();
				newPredicateStatement = newWhileStatement;
				sourceRewriter.set(newWhileStatement, WhileStatement.EXPRESSION_PROPERTY, oldWhileStatement.getExpression(), null);
				sourceRewriter.set(newWhileStatement, WhileStatement.BODY_PROPERTY, loopBlock, null);
			}
			else if(oldPredicateStatement instanceof ForStatement) {
				ForStatement oldForStatement = (ForStatement)oldPredicateStatement;
				ForStatement newForStatement = ast.newForStatement();
				newPredicateStatement = newForStatement;
				sourceRewriter.set(newForStatement, ForStatement.EXPRESSION_PROPERTY, oldForStatement.getExpression(), null);
				ListRewrite initializerRewrite = sourceRewriter.getListRewrite(newForStatement, ForStatement.INITIALIZERS_PROPERTY);
				List<Expression> initializers = oldForStatement.initializers();
				for(Expression expression : initializers)
					initializerRewrite.insertLast(expression, null);
				ListRewrite updaterRewrite = sourceRewriter.getListRewrite(newForStatement, ForStatement.UPDATERS_PROPERTY);
				List<Expression> updaters = oldForStatement.updaters();
				for(Expression expression : updaters)
					updaterRewrite.insertLast(expression, null);
				sourceRewriter.set(newForStatement, ForStatement.BODY_PROPERTY, loopBlock, null);
			}
			else if(oldPredicateStatement instanceof EnhancedForStatement) {
				EnhancedForStatement oldEnhancedForStatement = (EnhancedForStatement)oldPredicateStatement;
				EnhancedForStatement newEnhancedForStatement = ast.newEnhancedForStatement();
				newPredicateStatement = newEnhancedForStatement;
				sourceRewriter.set(newEnhancedForStatement, EnhancedForStatement.PARAMETER_PROPERTY, oldEnhancedForStatement.getParameter(), null);
				sourceRewriter.set(newEnhancedForStatement, EnhancedForStatement.EXPRESSION_PROPERTY, oldEnhancedForStatement.getExpression(), null);
				sourceRewriter.set(newEnhancedForStatement, EnhancedForStatement.BODY_PROPERTY, loopBlock, null);
			}
			else if(oldPredicateStatement instanceof DoStatement) {
				DoStatement oldDoStatement = (DoStatement)oldPredicateStatement;
				DoStatement newDoStatement = ast.newDoStatement();
				newPredicateStatement = newDoStatement;
				sourceRewriter.set(newDoStatement, DoStatement.EXPRESSION_PROPERTY, oldDoStatement.getExpression(), null);
				sourceRewriter.set(newDoStatement, DoStatement.BODY_PROPERTY, loopBlock, null);
			}
		}
		return newPredicateStatement;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 2);
			apply();
		} finally {
			pm.done();
		}
		return status;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 1);
		} finally {
			pm.done();
		}
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		try {
			pm.beginTask("Creating change...", 1);
			final Collection<TextFileChange> changes = fChanges.values();
			CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {
				@Override
				public ChangeDescriptor getDescriptor() {
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
					String project = sourceICompilationUnit.getJavaProject().getElementName();
					String description = MessageFormat.format("Extract from method ''{0}''", new Object[] { sourceMethodDeclaration.getName().getIdentifier()});
					String comment = MessageFormat.format("Extract from method ''{0}'' variable ''{1}''",
							new Object[] { sourceMethodDeclaration.getName().getIdentifier(), slice.getLocalVariableCriterion().getName().getIdentifier()});
					return new RefactoringChangeDescriptor(new ExtractMethodRefactoringDescriptor(project, description, comment,
							sourceCompilationUnit, slice));
				}
			};
			return change;
		} finally {
			pm.done();
		}
	}

	@Override
	public String getName() {
		return "Extract Method";
	}
}
