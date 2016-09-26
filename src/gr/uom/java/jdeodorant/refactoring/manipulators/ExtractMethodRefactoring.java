package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.decomposition.cfg.CFGBranchDoLoopNode;
import gr.uom.java.ast.decomposition.cfg.CFGNode;
import gr.uom.java.ast.decomposition.cfg.PDGControlPredicateNode;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PDGTryNode;
import gr.uom.java.ast.util.StatementExtractor;
import gr.uom.java.ast.util.TypeVisitor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

public class ExtractMethodRefactoring extends ExtractMethodFragmentRefactoring {
	private ASTSlice slice;
	private CompilationUnit sourceCompilationUnit;
	private TypeDeclaration sourceTypeDeclaration;
	private MethodDeclaration sourceMethodDeclaration;
	private CompilationUnitChange compilationUnitChange;
	private Set<ITypeBinding> exceptionTypesThatShouldBeThrownByExtractedMethod;
	
	public ExtractMethodRefactoring(CompilationUnit sourceCompilationUnit, ASTSlice slice) {
		super();
		this.slice = slice;
		this.sourceCompilationUnit = sourceCompilationUnit;
		this.sourceTypeDeclaration = slice.getSourceTypeDeclaration();
		this.sourceMethodDeclaration = slice.getSourceMethodDeclaration();
		ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
		this.compilationUnitChange = new CompilationUnitChange("", sourceICompilationUnit);
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
	}

	public ASTSlice getSlice() {
		return slice;
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
		if(extractedMethodInvocationInsertionStatement.getParent() instanceof LabeledStatement) {
			extractedMethodInvocationInsertionStatement = (LabeledStatement)extractedMethodInvocationInsertionStatement.getParent();
		}
		ASTNode statementParent = extractedMethodInvocationInsertionStatement.getParent();
		if(statementParent != null && statementParent instanceof Block)
			statementParent = statementParent.getParent();
		if(statementParent != null && statementParent instanceof TryStatement) {
			TryStatement tryStatementParent = (TryStatement)statementParent;
			if(tryStatementsToBeRemoved.contains(tryStatementParent))
				extractedMethodInvocationInsertionStatement = tryStatementParent;
		}
		
		VariableDeclaration returnedVariableDeclaration = slice.getLocalVariableCriterion();
		if(slice.declarationOfVariableCriterionBelongsToSliceNodes() && slice.declarationOfVariableCriterionBelongsToRemovableNodes()) {
			VariableDeclarationFragment initializationFragment = ast.newVariableDeclarationFragment();
			sourceRewriter.set(initializationFragment, VariableDeclarationFragment.NAME_PROPERTY, returnedVariableDeclaration.getName(), null);
			sourceRewriter.set(initializationFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, extractedMethodInvocation, null);
			VariableDeclarationStatement initializationVariableDeclarationStatement = ast.newVariableDeclarationStatement(initializationFragment);
			ITypeBinding returnedVariableTypeBinding = extractTypeBinding(returnedVariableDeclaration);
			Type returnedVariableType = RefactoringUtility.generateTypeFromTypeBinding(returnedVariableTypeBinding, ast, sourceRewriter);
			sourceRewriter.set(initializationVariableDeclarationStatement, VariableDeclarationStatement.TYPE_PROPERTY, returnedVariableType, null);
			ListRewrite blockRewrite = getBlockRewrite(extractedMethodInvocationInsertionStatement, sourceRewriter);
			blockRewrite.insertBefore(initializationVariableDeclarationStatement, extractedMethodInvocationInsertionStatement, null);
		}
		else if(slice.declarationOfVariableCriterionBelongsToSliceNodes() && !slice.declarationOfVariableCriterionBelongsToRemovableNodes()) {
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
			if(!slice.isObjectSlice()) {
				Assignment assignment = ast.newAssignment();
				sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, returnedVariableDeclaration.getName(), null);
				sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, extractedMethodInvocation, null);
				ExpressionStatement expressionStatement = ast.newExpressionStatement(assignment);
				ListRewrite blockRewrite = getBlockRewrite(extractedMethodInvocationInsertionStatement, sourceRewriter);
				blockRewrite.insertBefore(expressionStatement, extractedMethodInvocationInsertionStatement, null);
			}
			else {
				ExpressionStatement expressionStatement = ast.newExpressionStatement(extractedMethodInvocation);
				ListRewrite blockRewrite = getBlockRewrite(extractedMethodInvocationInsertionStatement, sourceRewriter);
				blockRewrite.insertBefore(expressionStatement, extractedMethodInvocationInsertionStatement, null);
			}
		}
		
		for(Statement removableStatement : slice.getRemovableStatements()) {
			sourceRewriter.remove(removableStatement, null);
		}
		for(TryStatement tryStatement : tryStatementsToBeRemoved) {
			sourceRewriter.remove(tryStatement, null);
		}
		for(LabeledStatement labeledStatement : labeledStatementsToBeRemoved) {
			sourceRewriter.remove(labeledStatement, null);
		}
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			root.addChild(sourceEdit);
			compilationUnitChange.addTextEditGroup(new TextEditGroup("Modify original method", new TextEdit[] {sourceEdit}));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	public ListRewrite getBlockRewrite(Statement extractedMethodInvocationInsertionStatement, ASTRewrite sourceRewriter) {
		ListRewrite blockRewrite = null;
		if(extractedMethodInvocationInsertionStatement.getParent() instanceof Block) {
			Block parentStatement = (Block)extractedMethodInvocationInsertionStatement.getParent();
			blockRewrite = sourceRewriter.getListRewrite(parentStatement, Block.STATEMENTS_PROPERTY);
		}
		else if(extractedMethodInvocationInsertionStatement.getParent() instanceof SwitchStatement) {
			SwitchStatement parentStatement = (SwitchStatement)extractedMethodInvocationInsertionStatement.getParent();
			blockRewrite = sourceRewriter.getListRewrite(parentStatement, Block.STATEMENTS_PROPERTY);
		}
		return blockRewrite;
	}

	private void extractMethod(MultiTextEdit root) {
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		AST ast = sourceTypeDeclaration.getAST();
		MethodDeclaration newMethodDeclaration = ast.newMethodDeclaration();
		
		VariableDeclaration returnedVariableDeclaration = slice.getLocalVariableCriterion();
		SimpleName returnedVariableSimpleName = returnedVariableDeclaration.getName();
		ITypeBinding returnedVariableTypeBinding = extractTypeBinding(returnedVariableDeclaration);
		Type returnedVariableType = RefactoringUtility.generateTypeFromTypeBinding(returnedVariableTypeBinding, ast, sourceRewriter);
		
		sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, ast.newSimpleName(slice.getExtractedMethodName()), null);
		IVariableBinding returnedVariableBinding = returnedVariableDeclaration.resolveBinding();
		if(slice.isObjectSlice() && (returnedVariableBinding.isField() || returnedVariableBinding.isParameter() || !slice.declarationOfVariableCriterionBelongsToSliceNodes()))
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
		Set<ITypeBinding> requiredTypeParameterBindings = new LinkedHashSet<ITypeBinding>();
		for(VariableDeclaration variableDeclaration : slice.getPassedParameters()) {
			ITypeBinding variableTypeBinding = extractTypeBinding(variableDeclaration);
			TypeVisitor typeVisitor = new TypeVisitor();
			variableDeclaration.accept(typeVisitor);
			Set<ITypeBinding> typeBindings = typeVisitor.getTypeBindings();
			for(ITypeBinding typeBinding : typeBindings) {
				if(typeBinding.isTypeVariable()) {
					requiredTypeParameterBindings.add(typeBinding);
				}
			}
			Type variableType = RefactoringUtility.generateTypeFromTypeBinding(variableTypeBinding, ast, sourceRewriter);
			if(!variableDeclaration.resolveBinding().isField()) {
				SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
				sourceRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, variableDeclaration.getName(), null);
				sourceRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, variableType, null);
				if((variableDeclaration.resolveBinding().getModifiers() & Modifier.FINAL) != 0) {
					Modifier finalModifier = newMethodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD);
					ListRewrite parameterModifierRewrite = sourceRewriter.getListRewrite(parameter, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
					parameterModifierRewrite.insertLast(finalModifier, null);
				}
				parameterRewrite.insertLast(parameter, null);
			}
		}
		ListRewrite typeParameterRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.TYPE_PARAMETERS_PROPERTY);
		for(ITypeBinding typeParameterBinding : requiredTypeParameterBindings) {
			TypeParameter typeParameter = ast.newTypeParameter();
			sourceRewriter.set(typeParameter, TypeParameter.NAME_PROPERTY, ast.newSimpleName(typeParameterBinding.getName()), null);
			ITypeBinding[] typeBounds = typeParameterBinding.getTypeBounds();
			if(typeBounds.length > 0) {
				ListRewrite typeBoundsRewrite = sourceRewriter.getListRewrite(typeParameter, TypeParameter.TYPE_BOUNDS_PROPERTY);
				for(ITypeBinding typeBoundBinding : typeBounds) {
					Type typeBound = RefactoringUtility.generateTypeFromTypeBinding(typeBoundBinding, ast, sourceRewriter);
					typeBoundsRewrite.insertLast(typeBound, null);
				}
			}
			typeParameterRewrite.insertLast(typeParameter, null);
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
		
		if(!slice.isObjectSlice() || (!returnedVariableBinding.isField() && !returnedVariableBinding.isParameter() && slice.declarationOfVariableCriterionBelongsToSliceNodes())) {
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

	protected void processStatementNode(ListRewrite bodyRewrite, PDGNode dstPDGNode, AST ast, ASTRewrite sourceRewriter) {
		bodyRewrite.insertLast(dstPDGNode.getASTStatement(), null);
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
