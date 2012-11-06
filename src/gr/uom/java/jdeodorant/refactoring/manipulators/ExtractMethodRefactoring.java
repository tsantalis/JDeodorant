package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.decomposition.cfg.CFGBranchDoLoopNode;
import gr.uom.java.ast.decomposition.cfg.CFGNode;
import gr.uom.java.ast.decomposition.cfg.GraphEdge;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGControlPredicateNode;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PDGTryNode;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.StatementExtractor;
import gr.uom.java.ast.util.TypeVisitor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

public class ExtractMethodRefactoring extends Refactoring {
	private ASTSlice slice;
	private CompilationUnit sourceCompilationUnit;
	private TypeDeclaration sourceTypeDeclaration;
	private MethodDeclaration sourceMethodDeclaration;
	private CompilationUnitChange compilationUnitChange;
	private Set<TryStatement> tryStatementsToBeRemoved;
	private Set<TryStatement> tryStatementsToBeCopied;
	private Map<TryStatement, ListRewrite> tryStatementBodyRewriteMap;
	private List<CFGBranchDoLoopNode> doLoopNodes;
	private Set<ITypeBinding> exceptionTypesThatShouldBeThrownByExtractedMethod;
	private boolean variableCriterionDeclarationStatementIsDeeperNestedThanExtractedMethodInvocationInsertionStatement;
	
	public ExtractMethodRefactoring(CompilationUnit sourceCompilationUnit, ASTSlice slice) {
		this.slice = slice;
		this.sourceCompilationUnit = sourceCompilationUnit;
		this.sourceTypeDeclaration = slice.getSourceTypeDeclaration();
		this.sourceMethodDeclaration = slice.getSourceMethodDeclaration();
		ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
		this.compilationUnitChange = new CompilationUnitChange("", sourceICompilationUnit);
		this.tryStatementsToBeRemoved = new LinkedHashSet<TryStatement>();
		this.tryStatementsToBeCopied = new LinkedHashSet<TryStatement>();
		this.tryStatementBodyRewriteMap = new LinkedHashMap<TryStatement, ListRewrite>();
		this.doLoopNodes = new ArrayList<CFGBranchDoLoopNode>();
		this.exceptionTypesThatShouldBeThrownByExtractedMethod = new LinkedHashSet<ITypeBinding>();
		for(PDGNode pdgNode : slice.getSliceNodes()) {
			CFGNode cfgNode = pdgNode.getCFGNode();
			if(cfgNode instanceof CFGBranchDoLoopNode) {
				CFGBranchDoLoopNode cfgDoLoopNode = (CFGBranchDoLoopNode)cfgNode;
				doLoopNodes.add(cfgDoLoopNode);
			}
		}
		StatementExtractor statementExtractor = new StatementExtractor();
		List<Statement> tryStatements = statementExtractor.getTryStatements(sourceMethodDeclaration.getBody());
		for(Statement tryStatement : tryStatements) {
			processTryStatement((TryStatement)tryStatement);
		}
		for(Statement sliceStatement : slice.getSliceStatements()) {
			Set<ITypeBinding> thrownExceptionTypes = getThrownExceptionTypes(sliceStatement);
			if(thrownExceptionTypes.size() > 0) {
				TryStatement surroundingTryBlock = surroundingTryBlock(sliceStatement);
				if(surroundingTryBlock == null || !slice.getSliceStatements().contains(surroundingTryBlock)) {
					exceptionTypesThatShouldBeThrownByExtractedMethod.addAll(thrownExceptionTypes);
				}
				if(surroundingTryBlock != null && slice.getSliceStatements().contains(surroundingTryBlock)) {
					for(ITypeBinding thrownExceptionType : thrownExceptionTypes) {
						if(!tryBlockCatchesExceptionType(surroundingTryBlock, thrownExceptionType))
							exceptionTypesThatShouldBeThrownByExtractedMethod.add(thrownExceptionType);
					}
				}
			}
		}
		this.variableCriterionDeclarationStatementIsDeeperNestedThanExtractedMethodInvocationInsertionStatement = false;
		Statement variableCriterionDeclarationStatement = slice.getVariableCriterionDeclarationStatement();
		if(variableCriterionDeclarationStatement != null) {
			int depthOfNestingForVariableCriterionDeclarationStatement = depthOfNesting(variableCriterionDeclarationStatement);
			Statement extractedMethodInvocationInsertionStatement = slice.getExtractedMethodInvocationInsertionStatement();
			int depthOfNestingForExtractedMethodInvocationInsertionStatement = depthOfNesting(extractedMethodInvocationInsertionStatement);
			if(depthOfNestingForVariableCriterionDeclarationStatement > depthOfNestingForExtractedMethodInvocationInsertionStatement)
				this.variableCriterionDeclarationStatementIsDeeperNestedThanExtractedMethodInvocationInsertionStatement = true;
			if(depthOfNestingForVariableCriterionDeclarationStatement == depthOfNestingForExtractedMethodInvocationInsertionStatement &&
					variableCriterionDeclarationStatement instanceof TryStatement)
				this.variableCriterionDeclarationStatementIsDeeperNestedThanExtractedMethodInvocationInsertionStatement = true;
		}
	}

	public String getExtractedMethodName() {
		return slice.getExtractedMethodName();
	}

	public void setExtractedMethodName(String extractedMethodName) {
		slice.setExtractedMethodName(extractedMethodName);
	}

	private void processTryStatement(TryStatement tryStatement) {
		List<Statement> nestedStatements = getStatements(tryStatement);
		Set<Statement> removableStatements = slice.getRemovableStatements();
		Set<Statement> sliceStatements = slice.getSliceStatements();
		boolean allNestedStatementsAreRemovable = true;
		boolean sliceStatementThrowsException = false;
		for(Statement nestedStatement : nestedStatements) {
			if(!removableStatements.contains(nestedStatement)) {
				allNestedStatementsAreRemovable = false;
			}
			if(sliceStatements.contains(nestedStatement)) {
				Set<ITypeBinding> thrownExceptionTypes = getThrownExceptionTypes(nestedStatement);
				if(thrownExceptionTypes.size() > 0)
					sliceStatementThrowsException = true;
			}
		}
		if(slice.getSliceStatements().contains(tryStatement)) {
			if(allNestedStatementsAreRemovable)
				tryStatementsToBeRemoved.add(tryStatement);
			else if(sliceStatementThrowsException)
				tryStatementsToBeCopied.add(tryStatement);
		}
	}

	private TryStatement surroundingTryBlock(Statement statement) {
		ASTNode parent = statement.getParent();
		while(!(parent instanceof MethodDeclaration)) {
			if(parent instanceof TryStatement)
				return (TryStatement)parent;
			parent = parent.getParent();
		}
		return null;
	}

	private int depthOfNesting(Statement statement) {
		int depthOfNesting = 0;
		ASTNode parent = statement;
		while(!(parent instanceof MethodDeclaration)) {
			depthOfNesting++;
			parent = parent.getParent();
		}
		return depthOfNesting;
	}

	private boolean tryBlockCatchesExceptionType(TryStatement tryStatement, ITypeBinding exceptionType) {
		List<CatchClause> catchClauses = tryStatement.catchClauses();
		for(CatchClause catchClause : catchClauses) {
			SingleVariableDeclaration exceptionDeclaration = catchClause.getException();
			Type exceptionDeclarationType = exceptionDeclaration.getType();
			if(exceptionDeclarationType instanceof UnionType) {
				UnionType unionType = (UnionType)exceptionDeclarationType;
				List<Type> types = unionType.types();
				for(Type type : types) {
					if(type.resolveBinding().isEqualTo(exceptionType))
						return true;
				}
			}
			else {
				if(exceptionDeclarationType.resolveBinding().isEqualTo(exceptionType))
					return true;
			}
		}
		return false;
	}

	public void apply() {
		MultiTextEdit root = new MultiTextEdit();
		compilationUnitChange.setEdit(root);
		extractMethod(root);
		modifySourceMethod(root);
	}

	private void modifySourceMethod(MultiTextEdit root) {
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		AST ast = sourceMethodDeclaration.getAST();
		MethodInvocation extractedMethodInvocation = ast.newMethodInvocation();
		sourceRewriter.set(extractedMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(slice.getExtractedMethodName()), null);
		ListRewrite argumentRewrite = sourceRewriter.getListRewrite(extractedMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
		for(VariableDeclaration variableDeclaration : slice.getPassedParameters()) {
			if(!variableDeclaration.resolveBinding().isField())
				argumentRewrite.insertLast(variableDeclaration.getName(), null);
		}
		
		Statement extractedMethodInvocationInsertionStatement = slice.getExtractedMethodInvocationInsertionStatement();
		ASTNode statementParent = extractedMethodInvocationInsertionStatement.getParent();
		if(statementParent != null && statementParent instanceof Block)
			statementParent = statementParent.getParent();
		if(statementParent != null && statementParent instanceof TryStatement) {
			TryStatement tryStatementParent = (TryStatement)statementParent;
			if(tryStatementsToBeRemoved.contains(tryStatementParent))
				extractedMethodInvocationInsertionStatement = tryStatementParent;
		}
		
		VariableDeclaration returnedVariableDeclaration = slice.getLocalVariableCriterion();
		if(slice.declarationOfVariableCriterionBelongsToSliceNodes() && slice.declarationOfVariableCriterionBelongsToRemovableNodes() &&
				!variableCriterionDeclarationStatementIsDeeperNestedThanExtractedMethodInvocationInsertionStatement) {
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
				if(fragment.getParent() instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)fragment.getParent();
					returnedVariableType = variableDeclarationStatement.getType();
				}
				else if(fragment.getParent() instanceof VariableDeclarationExpression) {
					VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)fragment.getParent();
					returnedVariableType = variableDeclarationExpression.getType();
				}
				else if(fragment.getParent() instanceof FieldDeclaration) {
					FieldDeclaration fieldDeclaration = (FieldDeclaration)fragment.getParent();
					returnedVariableType = fieldDeclaration.getType();
				}
			}
			sourceRewriter.set(initializationVariableDeclarationStatement, VariableDeclarationStatement.TYPE_PROPERTY, returnedVariableType, null);
			Block parentStatement = (Block)extractedMethodInvocationInsertionStatement.getParent();
			ListRewrite blockRewrite = sourceRewriter.getListRewrite(parentStatement, Block.STATEMENTS_PROPERTY);
			blockRewrite.insertBefore(initializationVariableDeclarationStatement, extractedMethodInvocationInsertionStatement, null);
		}
		else if(slice.declarationOfVariableCriterionBelongsToSliceNodes() && !slice.declarationOfVariableCriterionBelongsToRemovableNodes() &&
				!variableCriterionDeclarationStatementIsDeeperNestedThanExtractedMethodInvocationInsertionStatement) {
			if(returnedVariableDeclaration instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment oldInitializationFragment = (VariableDeclarationFragment)returnedVariableDeclaration;
				VariableDeclarationFragment newInitializationFragment = ast.newVariableDeclarationFragment();
				sourceRewriter.set(newInitializationFragment, VariableDeclarationFragment.NAME_PROPERTY, returnedVariableDeclaration.getName(), null);
				sourceRewriter.set(newInitializationFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, extractedMethodInvocation, null);
				if(oldInitializationFragment.getParent() instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement oldVariableDeclarationStatement = (VariableDeclarationStatement)oldInitializationFragment.getParent();
					List<VariableDeclarationFragment> oldFragments = oldVariableDeclarationStatement.fragments();
					ListRewrite fragmentRewrite = sourceRewriter.getListRewrite(oldVariableDeclarationStatement, VariableDeclarationStatement.FRAGMENTS_PROPERTY);
					for(int i=0; i<oldFragments.size(); i++) {
						if(oldInitializationFragment.equals(oldFragments.get(i)))
							fragmentRewrite.replace(oldFragments.get(i), newInitializationFragment, null);
					}
				}
				else if(oldInitializationFragment.getParent() instanceof VariableDeclarationExpression) {
					VariableDeclarationExpression oldVariableDeclarationExpression = (VariableDeclarationExpression)oldInitializationFragment.getParent();
					List<VariableDeclarationFragment> oldFragments = oldVariableDeclarationExpression.fragments();
					ListRewrite fragmentRewrite = sourceRewriter.getListRewrite(oldVariableDeclarationExpression, VariableDeclarationExpression.FRAGMENTS_PROPERTY);
					for(int i=0; i<oldFragments.size(); i++) {
						if(oldInitializationFragment.equals(oldFragments.get(i)))
							fragmentRewrite.replace(oldFragments.get(i), newInitializationFragment, null);
					}
				}
			}
		}
		else {
			//variable criterion is field, parameter, or local variable whose declaration does not belong to slice nodes
			//or is nested in deeper level compared to the insertion point of the extracted method invocation
			if(!slice.isObjectSlice()) {
				Assignment assignment = ast.newAssignment();
				sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, returnedVariableDeclaration.getName(), null);
				sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, extractedMethodInvocation, null);
				ExpressionStatement expressionStatement = ast.newExpressionStatement(assignment);
				Block parentStatement = (Block)extractedMethodInvocationInsertionStatement.getParent();
				ListRewrite blockRewrite = sourceRewriter.getListRewrite(parentStatement, Block.STATEMENTS_PROPERTY);
				blockRewrite.insertBefore(expressionStatement, extractedMethodInvocationInsertionStatement, null);
			}
			else {
				ExpressionStatement expressionStatement = ast.newExpressionStatement(extractedMethodInvocation);
				Block parentStatement = (Block)extractedMethodInvocationInsertionStatement.getParent();
				ListRewrite blockRewrite = sourceRewriter.getListRewrite(parentStatement, Block.STATEMENTS_PROPERTY);
				blockRewrite.insertBefore(expressionStatement, extractedMethodInvocationInsertionStatement, null);
			}
		}
		
		for(Statement removableStatement : slice.getRemovableStatements()) {
			sourceRewriter.remove(removableStatement, null);
		}
		for(TryStatement tryStatement : tryStatementsToBeRemoved) {
			sourceRewriter.remove(tryStatement, null);
		}
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			root.addChild(sourceEdit);
			compilationUnitChange.addTextEditGroup(new TextEditGroup("Modify original method", new TextEdit[] {sourceEdit}));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void extractMethod(MultiTextEdit root) {
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
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
			if(fragment.getParent() instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)fragment.getParent();
				returnedVariableType = variableDeclarationStatement.getType();
			}
			else if(fragment.getParent() instanceof VariableDeclarationExpression) {
				VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)fragment.getParent();
				returnedVariableType = variableDeclarationExpression.getType();
			}
			else if(fragment.getParent() instanceof FieldDeclaration) {
				FieldDeclaration fieldDeclaration = (FieldDeclaration)fragment.getParent();
				returnedVariableType = fieldDeclaration.getType();
			}
		}
		
		sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, ast.newSimpleName(slice.getExtractedMethodName()), null);
		IVariableBinding returnedVariableBinding = returnedVariableDeclaration.resolveBinding();
		if((slice.isObjectSlice() && (returnedVariableBinding.isField() || returnedVariableBinding.isParameter() || !slice.declarationOfVariableCriterionBelongsToSliceNodes())) ||
				variableCriterionDeclarationStatementIsDeeperNestedThanExtractedMethodInvocationInsertionStatement)
			sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, ast.newPrimitiveType(PrimitiveType.VOID), null);
		else
			sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnedVariableType, null);
		
		ListRewrite modifierRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		Modifier accessModifier = newMethodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD);
		modifierRewrite.insertLast(accessModifier, null);
		
		if((sourceMethodDeclaration.getModifiers() & Modifier.STATIC) != 0) {
			Modifier staticModifier = newMethodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD);
			modifierRewrite.insertLast(staticModifier, null);
		}
		
		ListRewrite parameterRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		for(VariableDeclaration variableDeclaration : slice.getPassedParameters()) {
			Type variableType = null;
			if(variableDeclaration instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)variableDeclaration;
				variableType = singleVariableDeclaration.getType();
			}
			else if(variableDeclaration instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment)variableDeclaration;
				if(fragment.getParent() instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)fragment.getParent();
					variableType = variableDeclarationStatement.getType();
				}
				else if(fragment.getParent() instanceof VariableDeclarationExpression) {
					VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)fragment.getParent();
					variableType = variableDeclarationExpression.getType();
				}
				else if(fragment.getParent() instanceof FieldDeclaration) {
					FieldDeclaration fieldDeclaration = (FieldDeclaration)fragment.getParent();
					variableType = fieldDeclaration.getType();
				}
			}
			if(!variableDeclaration.resolveBinding().isField()) {
				SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
				sourceRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, variableDeclaration.getName(), null);
				sourceRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, variableType, null);
				parameterRewrite.insertLast(parameter, null);
			}
		}
		ListRewrite thrownExceptionRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
		for(ITypeBinding thrownExceptionType : exceptionTypesThatShouldBeThrownByExtractedMethod) {
			thrownExceptionRewrite.insertLast(ast.newName(thrownExceptionType.getQualifiedName()), null);
		}
		Block newMethodBody = newMethodDeclaration.getAST().newBlock();
		ListRewrite methodBodyRewrite = sourceRewriter.getListRewrite(newMethodBody, Block.STATEMENTS_PROPERTY);

		List<PDGNode> sliceNodes = new ArrayList<PDGNode>(slice.getSliceNodes());
		while(!sliceNodes.isEmpty()) {
			ListRewrite bodyRewrite = methodBodyRewrite;
			PDGNode node = sliceNodes.get(0);
			PDGControlPredicateNode doLoopPredicateNode = isInsideDoLoop(node);
			if(doLoopPredicateNode != null) {
				bodyRewrite = createTryStatementIfNeeded(sourceRewriter, ast, bodyRewrite, doLoopPredicateNode);
				if(sliceNodes.contains(doLoopPredicateNode)) {
					bodyRewrite.insertLast(processPredicateNode(doLoopPredicateNode, ast, sourceRewriter, sliceNodes), null);
				}
			}
			else {
				bodyRewrite = createTryStatementIfNeeded(sourceRewriter, ast, bodyRewrite, node);
				if(node instanceof PDGControlPredicateNode) {
					PDGControlPredicateNode predicateNode = (PDGControlPredicateNode)node;
					bodyRewrite.insertLast(processPredicateNode(predicateNode, ast, sourceRewriter, sliceNodes), null);
				}
				else if(node instanceof PDGTryNode) {
					sliceNodes.remove(node);
				}
				else {
					bodyRewrite.insertLast(node.getASTStatement(), null);
					sliceNodes.remove(node);
				}
			}
		}
		
		if((!slice.isObjectSlice() || (!returnedVariableBinding.isField() && !returnedVariableBinding.isParameter() && slice.declarationOfVariableCriterionBelongsToSliceNodes())) &&
				!variableCriterionDeclarationStatementIsDeeperNestedThanExtractedMethodInvocationInsertionStatement) {
			ReturnStatement returnStatement = newMethodBody.getAST().newReturnStatement();
			sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, returnedVariableSimpleName, null);
			methodBodyRewrite.insertLast(returnStatement, null);
		}
		
		sourceRewriter.set(newMethodDeclaration, MethodDeclaration.BODY_PROPERTY, newMethodBody, null);
		
		ListRewrite methodDeclarationRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		methodDeclarationRewrite.insertAfter(newMethodDeclaration, sourceMethodDeclaration, null);
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			root.addChild(sourceEdit);
			compilationUnitChange.addTextEditGroup(new TextEditGroup("Create extracted method", new TextEdit[] {sourceEdit}));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private ListRewrite createTryStatementIfNeeded(ASTRewrite sourceRewriter, AST ast, ListRewrite bodyRewrite, PDGNode node) {
		Statement statement = node.getASTStatement();
		ASTNode statementParent = statement.getParent();
		if(statementParent != null && statementParent instanceof Block)
			statementParent = statementParent.getParent();
		if(statementParent != null && statementParent instanceof TryStatement) {
			TryStatement tryStatementParent = (TryStatement)statementParent;
			if(tryStatementsToBeRemoved.contains(tryStatementParent) || tryStatementsToBeCopied.contains(tryStatementParent)) {
				if(tryStatementBodyRewriteMap.containsKey(tryStatementParent)) {
					bodyRewrite = tryStatementBodyRewriteMap.get(tryStatementParent);
				}
				else {
					TryStatement newTryStatement = ast.newTryStatement();
					ListRewrite resourceRewrite = sourceRewriter.getListRewrite(newTryStatement, TryStatement.RESOURCES_PROPERTY);
					List<VariableDeclarationExpression> resources = tryStatementParent.resources();
					for(VariableDeclarationExpression expression : resources) {
						resourceRewrite.insertLast(expression, null);
					}
					ListRewrite catchClauseRewrite = sourceRewriter.getListRewrite(newTryStatement, TryStatement.CATCH_CLAUSES_PROPERTY);
					List<CatchClause> catchClauses = tryStatementParent.catchClauses();
					for(CatchClause catchClause : catchClauses) {
						catchClauseRewrite.insertLast(catchClause, null);
					}
					if(tryStatementParent.getFinally() != null) {
						sourceRewriter.set(newTryStatement, TryStatement.FINALLY_PROPERTY, tryStatementParent.getFinally(), null);
					}
					Block tryMethodBody = ast.newBlock();
					sourceRewriter.set(newTryStatement, TryStatement.BODY_PROPERTY, tryMethodBody, null);
					ListRewrite tryBodyRewrite = sourceRewriter.getListRewrite(tryMethodBody, Block.STATEMENTS_PROPERTY);
					tryStatementBodyRewriteMap.put(tryStatementParent, tryBodyRewrite);
					bodyRewrite.insertLast(newTryStatement, null);
					bodyRewrite = tryBodyRewrite;
				}
			}
		}
		return bodyRewrite;
	}

	private PDGControlPredicateNode isInsideDoLoop(PDGNode node) {
		for(CFGBranchDoLoopNode doLoopNode : doLoopNodes) {
			if(node.getId() >= doLoopNode.getJoinNode().getId() && node.getId() < doLoopNode.getId()) {
				PDGControlPredicateNode predicateNode = (PDGControlPredicateNode)doLoopNode.getPDGNode();
				return predicateNode;
			}
		}
		return null;
	}

	private Statement processPredicateNode(PDGControlPredicateNode predicateNode, AST ast, ASTRewrite sourceRewriter, List<PDGNode> sliceNodes) {
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
						listRewrite = createTryStatementIfNeeded(sourceRewriter, ast, listRewrite, dstPDGNode);
						if(dstPDGNode instanceof PDGControlPredicateNode) {
							PDGControlPredicateNode dstPredicateNode = (PDGControlPredicateNode)dstPDGNode;
							listRewrite.insertLast(processPredicateNode(dstPredicateNode, ast, sourceRewriter, sliceNodes), null);
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
		else if(oldPredicateStatement instanceof SwitchStatement) {
			SwitchStatement oldSwitchStatement = (SwitchStatement)oldPredicateStatement;
			SwitchStatement newSwitchStatement = ast.newSwitchStatement();
			newPredicateStatement = newSwitchStatement;
			sourceRewriter.set(newSwitchStatement, SwitchStatement.EXPRESSION_PROPERTY, oldSwitchStatement.getExpression(), null);
			ListRewrite switchStatementsRewrite = sourceRewriter.getListRewrite(newSwitchStatement, SwitchStatement.STATEMENTS_PROPERTY);
			Iterator<GraphEdge> outgoingDependenceIterator = predicateNode.getOutgoingDependenceIterator();
			while(outgoingDependenceIterator.hasNext()) {
				ListRewrite bodyRewrite = switchStatementsRewrite;
				PDGDependence dependence = (PDGDependence)outgoingDependenceIterator.next();
				if(dependence instanceof PDGControlDependence) {
					PDGControlDependence controlDependence = (PDGControlDependence)dependence;
					PDGNode dstPDGNode = (PDGNode)controlDependence.getDst();
					if(sliceNodes.contains(dstPDGNode)) {
						bodyRewrite = createTryStatementIfNeeded(sourceRewriter, ast, bodyRewrite, dstPDGNode);
						if(dstPDGNode instanceof PDGControlPredicateNode) {
							PDGControlPredicateNode dstPredicateNode = (PDGControlPredicateNode)dstPDGNode;
							bodyRewrite.insertLast(processPredicateNode(dstPredicateNode, ast, sourceRewriter, sliceNodes), null);
						}
						else {
							bodyRewrite.insertLast(dstPDGNode.getASTStatement(), null);
							sliceNodes.remove(dstPDGNode);
						}
					}
				}
			}
		}
		else {
			Block loopBlock = ast.newBlock();
			ListRewrite loopBodyRewrite = sourceRewriter.getListRewrite(loopBlock, Block.STATEMENTS_PROPERTY);
			Iterator<GraphEdge> outgoingDependenceIterator = predicateNode.getOutgoingDependenceIterator();
			while(outgoingDependenceIterator.hasNext()) {
				ListRewrite bodyRewrite = loopBodyRewrite;
				PDGDependence dependence = (PDGDependence)outgoingDependenceIterator.next();
				if(dependence instanceof PDGControlDependence) {
					PDGControlDependence controlDependence = (PDGControlDependence)dependence;
					PDGNode dstPDGNode = (PDGNode)controlDependence.getDst();
					if(sliceNodes.contains(dstPDGNode)) {
						bodyRewrite = createTryStatementIfNeeded(sourceRewriter, ast, bodyRewrite, dstPDGNode);
						if(dstPDGNode instanceof PDGControlPredicateNode) {
							PDGControlPredicateNode dstPredicateNode = (PDGControlPredicateNode)dstPDGNode;
							bodyRewrite.insertLast(processPredicateNode(dstPredicateNode, ast, sourceRewriter, sliceNodes), null);
						}
						else {
							bodyRewrite.insertLast(dstPDGNode.getASTStatement(), null);
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

	private Set<ITypeBinding> getThrownExceptionTypes(Statement statement) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> methodInvocations = new ArrayList<Expression>();
		if(statement instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement)statement;
			Expression ifExpression = ifStatement.getExpression();
			methodInvocations.addAll(expressionExtractor.getMethodInvocations(ifExpression));
		}
		else if(statement instanceof WhileStatement) {
			WhileStatement whileStatement = (WhileStatement)statement;
			Expression whileExpression = whileStatement.getExpression();
			methodInvocations.addAll(expressionExtractor.getMethodInvocations(whileExpression));
		}
		else if(statement instanceof ForStatement) {
			ForStatement forStatement = (ForStatement)statement;
			List<Expression> initializers = forStatement.initializers();
			for(Expression expression : initializers) {
				methodInvocations.addAll(expressionExtractor.getMethodInvocations(expression));
			}
			Expression forExpression = forStatement.getExpression();
			if(forExpression != null) {
				methodInvocations.addAll(expressionExtractor.getMethodInvocations(forExpression));
			}
			List<Expression> updaters = forStatement.updaters();
			for(Expression expression : updaters) {
				methodInvocations.addAll(expressionExtractor.getMethodInvocations(expression));
			}
		}
		else if(statement instanceof EnhancedForStatement) {
			EnhancedForStatement enhancedForStatement = (EnhancedForStatement)statement;
			Expression expression = enhancedForStatement.getExpression();
			methodInvocations.addAll(expressionExtractor.getMethodInvocations(expression));
		}
		else if(statement instanceof DoStatement) {
			DoStatement doStatement = (DoStatement)statement;
			Expression doExpression = doStatement.getExpression();
			methodInvocations.addAll(expressionExtractor.getMethodInvocations(doExpression));
		}
		else if(statement instanceof SwitchStatement) {
			SwitchStatement switchStatement = (SwitchStatement)statement;
			Expression switchExpression = switchStatement.getExpression();
			methodInvocations.addAll(expressionExtractor.getMethodInvocations(switchExpression));
		}
		else if(statement instanceof TryStatement) {
			
		}
		else {
			methodInvocations.addAll(expressionExtractor.getMethodInvocations(statement));
		}
		Set<ITypeBinding> thrownExceptionTypes = new LinkedHashSet<ITypeBinding>();
		for(Expression expression : methodInvocations) {
			if(expression instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)expression;
				IMethodBinding methodInvocationBinding = methodInvocation.resolveMethodBinding();
				ITypeBinding[] exceptionTypes = methodInvocationBinding.getExceptionTypes();
				for(ITypeBinding typeBinding : exceptionTypes)
					thrownExceptionTypes.add(typeBinding);
			}
			else if(expression instanceof SuperMethodInvocation) {
				SuperMethodInvocation superMethodInvocation = (SuperMethodInvocation)expression;
				IMethodBinding methodInvocationBinding = superMethodInvocation.resolveMethodBinding();
				ITypeBinding[] exceptionTypes = methodInvocationBinding.getExceptionTypes();
				for(ITypeBinding typeBinding : exceptionTypes)
					thrownExceptionTypes.add(typeBinding);
			}
		}
		if(statement instanceof ThrowStatement) {
			ThrowStatement throwStatement = (ThrowStatement)statement;
			TypeVisitor typeVisitor = new TypeVisitor();
			throwStatement.accept(typeVisitor);
			thrownExceptionTypes.addAll(typeVisitor.getTypeBindings());
		}
		return thrownExceptionTypes;
	}

	private List<Statement> getStatements(Statement statement) {
		List<Statement> statementList = new ArrayList<Statement>();
		if(statement instanceof Block) {
			Block block = (Block)statement;
			List<Statement> blockStatements = block.statements();
			for(Statement blockStatement : blockStatements)
				statementList.addAll(getStatements(blockStatement));
		}
		else if(statement instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement)statement;
			statementList.add(ifStatement);
			statementList.addAll(getStatements(ifStatement.getThenStatement()));
			if(ifStatement.getElseStatement() != null) {
				statementList.addAll(getStatements(ifStatement.getElseStatement()));
			}
		}
		else if(statement instanceof ForStatement) {
			ForStatement forStatement = (ForStatement)statement;
			statementList.add(forStatement);
			statementList.addAll(getStatements(forStatement.getBody()));
		}
		else if(statement instanceof EnhancedForStatement) {
			EnhancedForStatement enhancedForStatement = (EnhancedForStatement)statement;
			statementList.add(enhancedForStatement);
			statementList.addAll(getStatements(enhancedForStatement.getBody()));
		}
		else if(statement instanceof WhileStatement) {
			WhileStatement whileStatement = (WhileStatement)statement;
			statementList.add(whileStatement);
			statementList.addAll(getStatements(whileStatement.getBody()));
		}
		else if(statement instanceof DoStatement) {
			DoStatement doStatement = (DoStatement)statement;
			statementList.add(doStatement);
			statementList.addAll(getStatements(doStatement.getBody()));
		}
		else if(statement instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement = (ExpressionStatement)statement;
			statementList.add(expressionStatement);
		}
		else if(statement instanceof SwitchStatement) {
			SwitchStatement switchStatement = (SwitchStatement)statement;
			statementList.add(switchStatement);
			List<Statement> statements = switchStatement.statements();
			for(Statement statement2 : statements)
				statementList.addAll(getStatements(statement2));
		}
		else if(statement instanceof SwitchCase) {
			SwitchCase switchCase = (SwitchCase)statement;
			statementList.add(switchCase);
		}
		else if(statement instanceof AssertStatement) {
			AssertStatement assertStatement = (AssertStatement)statement;
			statementList.add(assertStatement);
		}
		else if(statement instanceof LabeledStatement) {
			LabeledStatement labeledStatement = (LabeledStatement)statement;
			//handling of LabeledStatement
			statementList.addAll(getStatements(labeledStatement.getBody()));
		}
		else if(statement instanceof ReturnStatement) {
			ReturnStatement returnStatement = (ReturnStatement)statement;
			statementList.add(returnStatement);
		}
		else if(statement instanceof SynchronizedStatement) {
			SynchronizedStatement synchronizedStatement = (SynchronizedStatement)statement;
			//handling of SynchronizedStatement
			statementList.addAll(getStatements(synchronizedStatement.getBody()));
		}
		else if(statement instanceof ThrowStatement) {
			ThrowStatement throwStatement = (ThrowStatement)statement;
			statementList.add(throwStatement);
		}
		else if(statement instanceof TryStatement) {
			TryStatement tryStatement = (TryStatement)statement;
			statementList.addAll(getStatements(tryStatement.getBody()));
			/*List<CatchClause> catchClauses = tryStatement.catchClauses();
			for(CatchClause catchClause : catchClauses) {
				statementList.addAll(getStatements(catchClause.getBody()));
			}
			Block finallyBlock = tryStatement.getFinally();
			if(finallyBlock != null)
				statementList.addAll(getStatements(finallyBlock));*/
		}
		else if(statement instanceof VariableDeclarationStatement) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
			statementList.add(variableDeclarationStatement);
		}
		else if(statement instanceof ConstructorInvocation) {
			ConstructorInvocation constructorInvocation = (ConstructorInvocation)statement;
			statementList.add(constructorInvocation);
		}
		else if(statement instanceof SuperConstructorInvocation) {
			SuperConstructorInvocation superConstructorInvocation = (SuperConstructorInvocation)statement;
			statementList.add(superConstructorInvocation);
		}
		else if(statement instanceof BreakStatement) {
			BreakStatement breakStatement = (BreakStatement)statement;
			statementList.add(breakStatement);
		}
		else if(statement instanceof ContinueStatement) {
			ContinueStatement continueStatement = (ContinueStatement)statement;
			statementList.add(continueStatement);
		}
		
		return statementList;
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
			final Collection<TextFileChange> changes = new ArrayList<TextFileChange>();
			changes.add(compilationUnitChange);
			CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {
				@Override
				public ChangeDescriptor getDescriptor() {
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
					String project = sourceICompilationUnit.getJavaProject().getElementName();
					String description = MessageFormat.format("Extract from method ''{0}''", new Object[] { sourceMethodDeclaration.getName().getIdentifier()});
					String comment = MessageFormat.format("Extract from method ''{0}'' variable ''{1}''",
							new Object[] { sourceMethodDeclaration.getName().getIdentifier(), slice.getLocalVariableCriterion().toString()});
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
