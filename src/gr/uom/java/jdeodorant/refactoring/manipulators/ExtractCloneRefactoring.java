package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CFGBranchDoLoopNode;
import gr.uom.java.ast.decomposition.cfg.CFGNode;
import gr.uom.java.ast.decomposition.cfg.PDGControlPredicateNode;
import gr.uom.java.ast.decomposition.cfg.PDGExitNode;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PDGTryNode;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGMapper;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGNodeMapping;
import gr.uom.java.ast.util.StatementExtractor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
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
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

public class ExtractCloneRefactoring extends ExtractMethodFragmentRefactoring {
	private PDGMapper mapper;
	private List<CompilationUnit> sourceCompilationUnits;
	private List<TypeDeclaration> sourceTypeDeclarations;
	private List<MethodDeclaration> sourceMethodDeclarations;
	private Map<ICompilationUnit, CompilationUnitChange> compilationUnitChanges;
	private Set<PDGNodeMapping> sortedNodeMappings;
	
	public ExtractCloneRefactoring(PDGMapper mapper) {
		super();
		this.mapper = mapper;
		MethodObject methodObject1 = mapper.getPDG1().getMethod();
		MethodObject methodObject2 = mapper.getPDG2().getMethod();
		MethodDeclaration methodDeclaration1 = methodObject1.getMethodDeclaration();
		MethodDeclaration methodDeclaration2 = methodObject2.getMethodDeclaration();
		
		this.sourceCompilationUnits = new ArrayList<CompilationUnit>();
		this.sourceTypeDeclarations = new ArrayList<TypeDeclaration>();
		this.sourceMethodDeclarations = new ArrayList<MethodDeclaration>();
		this.compilationUnitChanges = new LinkedHashMap<ICompilationUnit, CompilationUnitChange>();
		
		this.sourceMethodDeclarations.add(methodDeclaration1);
		this.sourceMethodDeclarations.add(methodDeclaration2);
		this.sourceTypeDeclarations.add((TypeDeclaration)methodDeclaration1.getParent());
		this.sourceTypeDeclarations.add((TypeDeclaration)methodDeclaration2.getParent());
		this.sourceCompilationUnits.add((CompilationUnit)methodDeclaration1.getRoot());
		this.sourceCompilationUnits.add((CompilationUnit)methodDeclaration2.getRoot());
		this.sortedNodeMappings = new TreeSet<PDGNodeMapping>(mapper.getMaximumStateWithMinimumDifferences().getNodeMappings());
		for(PDGNodeMapping pdgNodeMapping : sortedNodeMappings) {
			PDGNode pdgNode = pdgNodeMapping.getNodeG1();
			CFGNode cfgNode = pdgNode.getCFGNode();
			if(cfgNode instanceof CFGBranchDoLoopNode) {
				CFGBranchDoLoopNode cfgDoLoopNode = (CFGBranchDoLoopNode)cfgNode;
				doLoopNodes.add(cfgDoLoopNode);
			}
		}
		StatementExtractor statementExtractor = new StatementExtractor();
		//examining the body of the first method declaration for try blocks
		List<Statement> tryStatements = statementExtractor.getTryStatements(methodDeclaration1.getBody());
		for(Statement tryStatement : tryStatements) {
			processTryStatement((TryStatement)tryStatement);
		}
		for(CompilationUnit sourceCompilationUnit : sourceCompilationUnits) {
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
			MultiTextEdit sourceMultiTextEdit = new MultiTextEdit();
			CompilationUnitChange sourceCompilationUnitChange = new CompilationUnitChange("", sourceICompilationUnit);
			sourceCompilationUnitChange.setEdit(sourceMultiTextEdit);
			compilationUnitChanges.put(sourceICompilationUnit, sourceCompilationUnitChange);
		}
	}

	public void apply() {
		extractClone();
	}

	private Type findReturnType() {
		Set<AbstractVariable> returnedVariables = new LinkedHashSet<AbstractVariable>();
		for(PDGNodeMapping pdgNodeMapping : sortedNodeMappings) {
			PDGNode pdgNode = pdgNodeMapping.getNodeG1();
			if(pdgNode instanceof PDGExitNode) {
				PDGExitNode exitNode = (PDGExitNode)pdgNode;
				AbstractVariable returnedVariable = exitNode.getReturnedVariable();
				if(returnedVariable != null)
					returnedVariables.add(returnedVariable);
			}
		}
		Set<VariableDeclaration> variableDeclarationsAndAccessedFields = mapper.getPDG1().getVariableDeclarationsAndAccessedFieldsInMethod();
		for(AbstractVariable returnedVariable : returnedVariables) {
			for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFields) {
				if(variableDeclaration.resolveBinding().getKey().equals(returnedVariable.getVariableBindingKey())) {
					Type returnedVariableType = null;
					if(variableDeclaration instanceof SingleVariableDeclaration) {
						SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)variableDeclaration;
						returnedVariableType = singleVariableDeclaration.getType();
					}
					else if(variableDeclaration instanceof VariableDeclarationFragment) {
						VariableDeclarationFragment fragment = (VariableDeclarationFragment)variableDeclaration;
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
					return returnedVariableType;
				}
			}
		}
		return null;
	}

	private void extractClone() {
		//we need some logic to select the class where the clone will be extracted
		CompilationUnit sourceCompilationUnit = sourceCompilationUnits.get(0);
		TypeDeclaration sourceTypeDeclaration = sourceTypeDeclarations.get(0);
		MethodDeclaration sourceMethodDeclaration = sourceMethodDeclarations.get(0);
		//
		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
		AST ast = sourceTypeDeclaration.getAST();
		
		MethodDeclaration newMethodDeclaration = ast.newMethodDeclaration();
		sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, ast.newSimpleName(sourceMethodDeclaration.getName().getIdentifier()), null);
		Type returnType = findReturnType();
		if(returnType != null)
			sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnType, null);
		else
			sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, ast.newPrimitiveType(PrimitiveType.VOID), null);
		
		ListRewrite parameterRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		Map<String, ArrayList<VariableDeclaration>> commonPassedParameters = mapper.getCommonPassedParameters();
		Set<VariableDeclaration> totalPassedParameters = new LinkedHashSet<VariableDeclaration>();
		for(String parameterName : commonPassedParameters.keySet()) {
			List<VariableDeclaration> variableDeclarations = commonPassedParameters.get(parameterName);
			totalPassedParameters.add(variableDeclarations.get(0));
		}
		totalPassedParameters.addAll(mapper.getPassedParametersG1());
		totalPassedParameters.addAll(mapper.getPassedParametersG2());
		for(VariableDeclaration variableDeclaration : totalPassedParameters) {
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
		
		Block newMethodBody = newMethodDeclaration.getAST().newBlock();
		ListRewrite methodBodyRewrite = sourceRewriter.getListRewrite(newMethodBody, Block.STATEMENTS_PROPERTY);
		List<PDGNode> sliceNodes = new ArrayList<PDGNode>();
		for(PDGNodeMapping pdgNodeMapping : sortedNodeMappings) {
			PDGNode pdgNode = pdgNodeMapping.getNodeG1();
			sliceNodes.add(pdgNode);
		}
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
		
		sourceRewriter.set(newMethodDeclaration, MethodDeclaration.BODY_PROPERTY, newMethodBody, null);

		ListRewrite methodDeclarationRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		methodDeclarationRewrite.insertAfter(newMethodDeclaration, sourceMethodDeclaration, null);
		
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
			change.getEdit().addChild(sourceEdit);
			change.addTextEditGroup(new TextEditGroup("Create method for the extracted duplicated code", new TextEdit[] {sourceEdit}));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void processTryStatement(TryStatement tryStatement) {
		List<Statement> nestedStatements = getStatements(tryStatement);
		List<Statement> cloneStatements = new ArrayList<Statement>();
		for(PDGNodeMapping pdgNodeMapping : sortedNodeMappings) {
			PDGNode pdgNode = pdgNodeMapping.getNodeG1();
			cloneStatements.add(pdgNode.getASTStatement());
		}
		boolean allNestedStatementsAreRemovable = true;
		boolean sliceStatementThrowsException = false;
		for(Statement nestedStatement : nestedStatements) {
			if(!cloneStatements.contains(nestedStatement)) {
				allNestedStatementsAreRemovable = false;
			}
			if(cloneStatements.contains(nestedStatement)) {
				Set<ITypeBinding> thrownExceptionTypes = getThrownExceptionTypes(nestedStatement);
				if(thrownExceptionTypes.size() > 0)
					sliceStatementThrowsException = true;
			}
		}
		if(cloneStatements.contains(tryStatement)) {
			if(allNestedStatementsAreRemovable)
				tryStatementsToBeRemoved.add(tryStatement);
			else if(sliceStatementThrowsException)
				tryStatementsToBeCopied.add(tryStatement);
		}
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
			final Collection<CompilationUnitChange> changes = compilationUnitChanges.values();
			CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {
				@Override
				public ChangeDescriptor getDescriptor() {
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnits.get(0).getJavaElement();
					String project = sourceICompilationUnit.getJavaProject().getElementName();
					String description = MessageFormat.format("Extract Clone in class ''{0}''", new Object[] { sourceICompilationUnit.getElementName() });
					String comment = null;
					return new RefactoringChangeDescriptor(new ExtractCloneRefactoringDescriptor(project, description, comment,
							mapper));
				}
			};
			return change;
		} finally {
			pm.done();
		}
	}

	@Override
	public String getName() {
		return "Extract Clone";
	}

}
