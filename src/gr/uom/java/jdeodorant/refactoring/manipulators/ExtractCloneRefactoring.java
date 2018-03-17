package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.AbstractMethodDeclaration;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.AbstractMethodFragment;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.CatchClauseObject;
import gr.uom.java.ast.decomposition.CompositeStatementObject;
import gr.uom.java.ast.decomposition.StatementObject;
import gr.uom.java.ast.decomposition.TryStatementObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CFGBranchDoLoopNode;
import gr.uom.java.ast.decomposition.cfg.CFGNode;
import gr.uom.java.ast.decomposition.cfg.MethodCallAnalyzer;
import gr.uom.java.ast.decomposition.cfg.PDGBlockNode;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGExitNode;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PDGTryNode;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.decomposition.cfg.mapping.CloneRefactoringType;
import gr.uom.java.ast.decomposition.cfg.mapping.CloneStructureNode;
import gr.uom.java.ast.decomposition.cfg.mapping.DivideAndConquerMatcher;
import gr.uom.java.ast.decomposition.cfg.mapping.NodeMapping;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGElseGap;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGElseMapping;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGNodeGap;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGNodeMapping;
import gr.uom.java.ast.decomposition.cfg.mapping.PreconditionExaminer;
import gr.uom.java.ast.decomposition.cfg.mapping.StatementCollector;
import gr.uom.java.ast.decomposition.cfg.mapping.VariableBindingKeyPair;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.DualExpressionPreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.ExpressionPreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.NotAllPossibleExecutionFlowsEndInReturnPreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.PreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.PreconditionViolationType;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.ReturnedVariablePreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.StatementPreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.UncommonSuperclassPreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.ZeroMatchedStatementsPreconditionViolation;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.matching.BindingSignature;
import gr.uom.java.ast.decomposition.matching.BindingSignaturePair;
import gr.uom.java.ast.decomposition.matching.Difference;
import gr.uom.java.ast.decomposition.matching.DifferenceType;
import gr.uom.java.ast.decomposition.matching.FieldAccessReplacedWithGetterInvocationDifference;
import gr.uom.java.ast.decomposition.matching.FieldAssignmentReplacedWithSetterInvocationDifference;
import gr.uom.java.ast.decomposition.matching.loop.EarliestStartPositionComparator;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.MethodDeclarationUtility;
import gr.uom.java.ast.util.SuperMethodInvocationVisitor;
import gr.uom.java.ast.util.ThrownExceptionVisitor;
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
import java.util.TreeSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreateCompilationUnitChange;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ExtractCloneRefactoring extends ExtractMethodFragmentRefactoring {
	private static final String GETTER_PREFIX = "get";
	private static final String SETTER_PREFIX = "set";
	private static final String ACCESSOR_SUFFIX = "2";
	private List<? extends DivideAndConquerMatcher> mappers;
	private DivideAndConquerMatcher mapper;
	private List<CompilationUnit> sourceCompilationUnits;
	private List<TypeDeclaration> sourceTypeDeclarations;
	private List<MethodDeclaration> sourceMethodDeclarations;
	private List<Set<VariableDeclaration>> fieldDeclarationsToBePulledUp;
	private List<Set<VariableDeclaration>> fieldDeclarationsToBeParameterized;
	private List<Set<VariableDeclaration>> accessedFieldDeclarationsToBeReplacedWithGetter;
	private List<Set<VariableDeclaration>> assignedFieldDeclarationsToBeReplacedWithSetter;
	private List<Set<MethodDeclaration>> methodDeclarationsToBePulledUp;
	private List<Set<LabeledStatement>> labeledStatementsToBeRemoved;
	private Map<ICompilationUnit, CompilationUnitChange> compilationUnitChanges;
	private Map<ICompilationUnit, CreateCompilationUnitChange> createCompilationUnitChanges;
	private Set<IJavaElement> javaElementsToOpenInEditor;
	private Set<PDGNodeMapping> sortedNodeMappings;
	private List<TreeSet<PDGNode>> removableStatements;
	private List<TreeSet<PDGNode>> remainingStatementsMovableBefore;
	private List<TreeSet<PDGNode>> remainingStatementsMovableAfter;
	private Map<VariableBindingKeyPair, ArrayList<VariableDeclaration>> originalPassedParameters;
	private Map<BindingSignaturePair, ASTNodeDifference> parameterizedDifferenceMap;
	private List<ArrayList<VariableDeclaration>> returnedVariables;
	private String extractedMethodName;
	private List<Set<MethodDeclaration>> constructorsToBeCopiedInSubclasses;
	private CloneInformation cloneInfo;
	private RefactoringStatus status = new RefactoringStatus();
	
	private class CloneInformation {
		private ICompilationUnit sourceICompilationUnit;
		private CompilationUnit sourceCompilationUnit;
		private TypeDeclaration sourceTypeDeclaration;
		private AST ast;
		private IFile file;
		private ASTRewrite sourceRewriter;
		private Document document;
		private Set<ITypeBinding> requiredImportTypeBindings;
		private ImportRewrite importRewrite;
		private ListRewrite methodBodyRewrite;
		private ListRewrite parameterRewrite;
		private List<ListRewrite> argumentRewriteList = new ArrayList<ListRewrite>();
		private List<ASTRewrite> originalMethodBodyRewriteList = new ArrayList<ASTRewrite>();
		private boolean superclassNotDirectlyInheritedFromRefactoredSubclasses;
		private boolean extractUtilityClass;
		private String intermediateClassName;
		private ITypeBinding[] intermediateClassTypeParameters;
		private IPackageBinding intermediateClassPackageBinding;
	}
	
	public ExtractCloneRefactoring(List<? extends DivideAndConquerMatcher> mappers) {
		super();
		this.mappers = mappers;
		this.mapper = mappers.get(0);
		if(this.mapper.getMethodName1().equals(this.mapper.getMethodName2())) {
			this.extractedMethodName = this.mapper.getMethodName1() + "Extracted";
		}
		else {
			this.extractedMethodName = this.mapper.getMethodName1();
		}
	}

	public List<? extends DivideAndConquerMatcher> getMappers() {
		return mappers;
	}

	private void initialize() {
		AbstractMethodDeclaration methodObject1 = this.mapper.getPDG1().getMethod();
		AbstractMethodDeclaration methodObject2 = this.mapper.getPDG2().getMethod();
		MethodDeclaration methodDeclaration1 = methodObject1.getMethodDeclaration();
		MethodDeclaration methodDeclaration2 = methodObject2.getMethodDeclaration();
		
		this.sourceCompilationUnits = new ArrayList<CompilationUnit>();
		this.sourceTypeDeclarations = new ArrayList<TypeDeclaration>();
		this.sourceMethodDeclarations = new ArrayList<MethodDeclaration>();
		this.removableStatements = new ArrayList<TreeSet<PDGNode>>();
		removableStatements.add(this.mapper.getRemovableNodesG1());
		removableStatements.add(this.mapper.getRemovableNodesG2());
		this.remainingStatementsMovableBefore = new ArrayList<TreeSet<PDGNode>>();
		remainingStatementsMovableBefore.add(this.mapper.getNonMappedPDGNodesG1MovableBefore());
		remainingStatementsMovableBefore.add(this.mapper.getNonMappedPDGNodesG2MovableBefore());
		this.remainingStatementsMovableAfter = new ArrayList<TreeSet<PDGNode>>();
		remainingStatementsMovableAfter.add(this.mapper.getNonMappedPDGNodesG1MovableAfter());
		remainingStatementsMovableAfter.add(this.mapper.getNonMappedPDGNodesG2MovableAfter());
		this.returnedVariables = new ArrayList<ArrayList<VariableDeclaration>>();
		returnedVariables.add(new ArrayList<VariableDeclaration>(this.mapper.getVariablesToBeReturnedG1()));
		returnedVariables.add(new ArrayList<VariableDeclaration>(this.mapper.getVariablesToBeReturnedG2()));
		this.fieldDeclarationsToBePulledUp = new ArrayList<Set<VariableDeclaration>>();
		this.fieldDeclarationsToBeParameterized = new ArrayList<Set<VariableDeclaration>>();
		this.accessedFieldDeclarationsToBeReplacedWithGetter = new ArrayList<Set<VariableDeclaration>>();
		this.assignedFieldDeclarationsToBeReplacedWithSetter = new ArrayList<Set<VariableDeclaration>>();
		this.methodDeclarationsToBePulledUp = new ArrayList<Set<MethodDeclaration>>();
		this.labeledStatementsToBeRemoved = new ArrayList<Set<LabeledStatement>>();
		//this.nodesToBePreservedInTheOriginalMethod = new ArrayList<TreeSet<PDGNode>>();
		for(int i=0; i<2; i++) {
			fieldDeclarationsToBePulledUp.add(new LinkedHashSet<VariableDeclaration>());
			fieldDeclarationsToBeParameterized.add(new LinkedHashSet<VariableDeclaration>());
			accessedFieldDeclarationsToBeReplacedWithGetter.add(new LinkedHashSet<VariableDeclaration>());
			assignedFieldDeclarationsToBeReplacedWithSetter.add(new LinkedHashSet<VariableDeclaration>());
			methodDeclarationsToBePulledUp.add(new LinkedHashSet<MethodDeclaration>());
			labeledStatementsToBeRemoved.add(new LinkedHashSet<LabeledStatement>());
			//nodesToBePreservedInTheOriginalMethod.add(new TreeSet<PDGNode>());
		}
		this.compilationUnitChanges = new LinkedHashMap<ICompilationUnit, CompilationUnitChange>();
		this.createCompilationUnitChanges = new LinkedHashMap<ICompilationUnit, CreateCompilationUnitChange>();
		this.javaElementsToOpenInEditor = new LinkedHashSet<IJavaElement>();
		
		this.sourceMethodDeclarations.add(methodDeclaration1);
		this.sourceMethodDeclarations.add(methodDeclaration2);
		if(methodDeclaration1.getParent() instanceof TypeDeclaration && methodDeclaration2.getParent() instanceof TypeDeclaration) { 
			this.sourceTypeDeclarations.add((TypeDeclaration)methodDeclaration1.getParent());
			this.sourceTypeDeclarations.add((TypeDeclaration)methodDeclaration2.getParent());
		}
		this.sourceCompilationUnits.add((CompilationUnit)methodDeclaration1.getRoot());
		this.sourceCompilationUnits.add((CompilationUnit)methodDeclaration2.getRoot());
		this.originalPassedParameters = new LinkedHashMap<VariableBindingKeyPair, ArrayList<VariableDeclaration>>();
		this.parameterizedDifferenceMap = new LinkedHashMap<BindingSignaturePair, ASTNodeDifference>();
		this.sortedNodeMappings = this.mapper.getMaximumStateWithMinimumDifferences().getSortedNodeMappings();
		for(PDGNodeMapping pdgNodeMapping : sortedNodeMappings) {
			PDGNode pdgNode = pdgNodeMapping.getNodeG1();
			CFGNode cfgNode = pdgNode.getCFGNode();
			if(cfgNode instanceof CFGBranchDoLoopNode) {
				CFGBranchDoLoopNode cfgDoLoopNode = (CFGBranchDoLoopNode)cfgNode;
				doLoopNodes.add(cfgDoLoopNode);
			}
		}
		/*StatementExtractor statementExtractor = new StatementExtractor();
		//examining the body of the first method declaration for try blocks
		List<Statement> tryStatements = statementExtractor.getTryStatements(methodDeclaration1.getBody());
		for(Statement tryStatement : tryStatements) {
			processTryStatement((TryStatement)tryStatement);
		}*/
		for(CompilationUnit sourceCompilationUnit : sourceCompilationUnits) {
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
			MultiTextEdit sourceMultiTextEdit = new MultiTextEdit();
			CompilationUnitChange sourceCompilationUnitChange = new CompilationUnitChange("", sourceICompilationUnit);
			sourceCompilationUnitChange.setEdit(sourceMultiTextEdit);
			compilationUnitChanges.put(sourceICompilationUnit, sourceCompilationUnitChange);
		}
		this.cloneInfo = null;
		this.constructorsToBeCopiedInSubclasses = new ArrayList<Set<MethodDeclaration>>();
		for(int i=0; i<2; i++) {
			constructorsToBeCopiedInSubclasses.add(new LinkedHashSet<MethodDeclaration>());
		}
	}

	public Set<IJavaElement> getJavaElementsToOpenInEditor() {
		return javaElementsToOpenInEditor;
	}

	public DivideAndConquerMatcher getMapper() {
		return mapper;
	}

	public void setMapper(DivideAndConquerMatcher mapper) {
		this.mapper = mapper;
	}

	public String getExtractedMethodName() {
		return extractedMethodName;
	}

	public void setExtractedMethodName(String extractedMethodName) {
		this.extractedMethodName = extractedMethodName;
	}

	public void apply() {
		initialize();
		extractClone();
		if(status.getEntries().length == 0) {
			boolean bothClonesInTheSameCompilationUnit = sourceCompilationUnits.get(0).equals(sourceCompilationUnits.get(1));
			if(bothClonesInTheSameCompilationUnit) {
				modifySourceCompilationUnitImportDeclarations(sourceCompilationUnits.get(0), true);
			}
			for(int i=0; i<sourceCompilationUnits.size(); i++) {
				modifySourceClass(sourceCompilationUnits.get(i), sourceTypeDeclarations.get(i), fieldDeclarationsToBePulledUp.get(i), methodDeclarationsToBePulledUp.get(i),
						constructorsToBeCopiedInSubclasses.get(i), accessedFieldDeclarationsToBeReplacedWithGetter.get(i), assignedFieldDeclarationsToBeReplacedWithSetter.get(i));
				if(!bothClonesInTheSameCompilationUnit) {
					boolean cloneBelongsToTheCommonSuperclass = sourceCompilationUnits.get(i).getJavaElement().equals(cloneInfo.sourceCompilationUnit.getJavaElement());
					modifySourceCompilationUnitImportDeclarations(sourceCompilationUnits.get(i), cloneBelongsToTheCommonSuperclass);
				}
				modifySourceMethod(sourceCompilationUnits.get(i), sourceMethodDeclarations.get(i), removableStatements.get(i),
						remainingStatementsMovableBefore.get(i), remainingStatementsMovableAfter.get(i), returnedVariables.get(i), fieldDeclarationsToBeParameterized.get(i), i);
			}
			finalizeCloneExtraction();
		}
	}

	private boolean mappedNodesContainStatementReturningVariable(VariableDeclaration variableDeclaration1, VariableDeclaration variableDeclaration2) {
		for(PDGNodeMapping pdgNodeMapping : sortedNodeMappings) {
			PDGNode pdgNode1 = pdgNodeMapping.getNodeG1();
			PDGNode pdgNode2 = pdgNodeMapping.getNodeG2();
			if(pdgNode1 instanceof PDGExitNode && pdgNode2 instanceof PDGExitNode) {
				PDGExitNode exitNode1 = (PDGExitNode)pdgNode1;
				PDGExitNode exitNode2 = (PDGExitNode)pdgNode2;
				ReturnStatement returnStatement1 = (ReturnStatement)exitNode1.getASTStatement();
				ReturnStatement returnStatement2 = (ReturnStatement)exitNode2.getASTStatement();
				Expression returnedExpression1 = returnStatement1.getExpression();
				Expression returnedExpression2 = returnStatement2.getExpression();
				if(returnedExpression1 != null && returnedExpression2 != null) {
					if(returnedExpression1 instanceof SimpleName && returnedExpression2 instanceof SimpleName) {
						SimpleName simpleName1 = (SimpleName)returnedExpression1;
						SimpleName simpleName2 = (SimpleName)returnedExpression2;
						if(simpleName1.resolveBinding().isEqualTo(variableDeclaration1.getName().resolveBinding()) &&
								simpleName2.resolveBinding().isEqualTo(variableDeclaration2.getName().resolveBinding())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean mappedNodesContainStatementDeclaringVariable(VariableDeclaration variableDeclaration1, VariableDeclaration variableDeclaration2) {
		for(PDGNodeMapping pdgNodeMapping : sortedNodeMappings) {
			PDGNode pdgNode1 = pdgNodeMapping.getNodeG1();
			PDGNode pdgNode2 = pdgNodeMapping.getNodeG2();
			boolean node1DeclaresVariable = false;
			for(Iterator<AbstractVariable> declaredVariableIterator = pdgNode1.getDeclaredVariableIterator(); declaredVariableIterator.hasNext();) {
				AbstractVariable declaredVariable = declaredVariableIterator.next();
				if(declaredVariable.getVariableBindingKey().equals(variableDeclaration1.resolveBinding().getKey())) {
					node1DeclaresVariable = true;
					break;
				}
			}
			boolean node2DeclaresVariable = false;
			for(Iterator<AbstractVariable> declaredVariableIterator = pdgNode2.getDeclaredVariableIterator(); declaredVariableIterator.hasNext();) {
				AbstractVariable declaredVariable = declaredVariableIterator.next();
				if(declaredVariable.getVariableBindingKey().equals(variableDeclaration2.resolveBinding().getKey())) {
					node2DeclaresVariable = true;
					break;
				}
			}
			if(node1DeclaresVariable && node2DeclaresVariable) {
				return true;
			}
		}
		return false;
	}

	private boolean mappedNodesContainDifferentStatementsDeclaringVariables(VariableDeclaration variableDeclaration1, VariableDeclaration variableDeclaration2) {
		boolean variable1IsDeclared = false;
		boolean variable2IsDeclared = false;
		for(PDGNodeMapping pdgNodeMapping : sortedNodeMappings) {
			PDGNode pdgNode1 = pdgNodeMapping.getNodeG1();
			PDGNode pdgNode2 = pdgNodeMapping.getNodeG2();
			for(Iterator<AbstractVariable> declaredVariableIterator = pdgNode1.getDeclaredVariableIterator(); declaredVariableIterator.hasNext();) {
				AbstractVariable declaredVariable = declaredVariableIterator.next();
				if(declaredVariable.getVariableBindingKey().equals(variableDeclaration1.resolveBinding().getKey())) {
					variable1IsDeclared = true;
					break;
				}
			}
			for(Iterator<AbstractVariable> declaredVariableIterator = pdgNode2.getDeclaredVariableIterator(); declaredVariableIterator.hasNext();) {
				AbstractVariable declaredVariable = declaredVariableIterator.next();
				if(declaredVariable.getVariableBindingKey().equals(variableDeclaration2.resolveBinding().getKey())) {
					variable2IsDeclared = true;
					break;
				}
			}
		}
		if(variable1IsDeclared && variable2IsDeclared) {
			return true;
		}
		return false;
	}

	private BindingSignaturePair variableBelongsToParameterizedDifferences(VariableDeclaration variableDeclaration1, VariableDeclaration variableDeclaration2) {
		for(BindingSignaturePair pair : parameterizedDifferenceMap.keySet()) {
			if(pair.getSignature1().containsOnlyBinding(variableDeclaration1.resolveBinding().getKey()) &&
					pair.getSignature2().containsOnlyBinding(variableDeclaration2.resolveBinding().getKey())) {
				return pair;
			}
		}
		return null;
	}

	private boolean variableIsPassedAsCommonParameter(VariableDeclaration variableDeclaration1, VariableDeclaration variableDeclaration2) {
		for(VariableBindingKeyPair pair : mapper.getCommonPassedParameters().keySet()) {
			if(pair.getKey1().equals(variableDeclaration1.resolveBinding().getKey()) &&
					pair.getKey2().equals(variableDeclaration2.resolveBinding().getKey())) {
				return true;
			}
		}
		return false;
	}

	private boolean variableIsPassedAsCommonParameter(VariableDeclaration variableDeclaration) {
		for(VariableBindingKeyPair pair : mapper.getCommonPassedParameters().keySet()) {
			if(pair.getKey1().equals(variableDeclaration.resolveBinding().getKey()) ||
					pair.getKey2().equals(variableDeclaration.resolveBinding().getKey())) {
				return true;
			}
		}
		return false;
	}

	private boolean variableIsDeclaredInMappedNodes(VariableDeclaration variableDeclaration, Set<PDGNode> mappedNodes) {
		PlainVariable plainVariable = new PlainVariable(variableDeclaration);
		for(PDGNode mappedNode : mappedNodes) {
			if(mappedNode.declaresLocalVariable(plainVariable)) {
				return true;
			}
		}
		return false;
	}

	private Set<VariableDeclaration> getLocallyAccessedFields(Set<AbstractVariable> accessedFields, TypeDeclaration typeDeclaration) {
		Set<VariableDeclaration> accessedLocalFields = new LinkedHashSet<VariableDeclaration>();
		for(AbstractVariable variable : accessedFields) {
			VariableDeclaration fieldDeclaration = RefactoringUtility.findFieldDeclaration(variable, typeDeclaration);
			if(fieldDeclaration != null) {
				accessedLocalFields.add(fieldDeclaration);
			}
		}
		return accessedLocalFields;
	}

	private boolean containsImportWithNameClash(CompilationUnit cunit, ITypeBinding typeBinding) {
		List<ImportDeclaration> imports = cunit.imports();
		for(ImportDeclaration importDeclaration : imports) {
			IBinding importBinding = importDeclaration.resolveBinding();
			if(importBinding.getKind() == IBinding.TYPE) {
				ITypeBinding importTypeBinding = (ITypeBinding)importBinding;
				if(importTypeBinding.getName().equals(typeBinding.getName()) && !importTypeBinding.isEqualTo(typeBinding)) {
					return true;
				}
			}
		}
		return false;
	}

	private void extractClone() {
		this.cloneInfo = new CloneInformation();
		Set<ITypeBinding> requiredImportTypeBindings = new LinkedHashSet<ITypeBinding>();
		ITypeBinding commonSuperTypeOfSourceTypeDeclarations = null;
		ITypeBinding declaringClass1 = sourceTypeDeclarations.get(0).resolveBinding().getDeclaringClass();
		ITypeBinding declaringClass2 = sourceTypeDeclarations.get(1).resolveBinding().getDeclaringClass();
		if(sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclarations.get(1).resolveBinding()) &&
				sourceTypeDeclarations.get(0).resolveBinding().getQualifiedName().equals(sourceTypeDeclarations.get(1).resolveBinding().getQualifiedName())) {
			cloneInfo.sourceCompilationUnit = sourceCompilationUnits.get(0);
			cloneInfo.sourceICompilationUnit = (ICompilationUnit)cloneInfo.sourceCompilationUnit.getJavaElement();
			cloneInfo.sourceTypeDeclaration = sourceTypeDeclarations.get(0);
			cloneInfo.sourceRewriter = ASTRewrite.create(cloneInfo.sourceTypeDeclaration.getAST());
			cloneInfo.ast = cloneInfo.sourceTypeDeclaration.getAST();
		}
		else if(declaringClass1 != null && declaringClass2 != null && declaringClass1.isEqualTo(declaringClass2) && declaringClass1.getQualifiedName().equals(declaringClass2.getQualifiedName())) {
			cloneInfo.sourceCompilationUnit = sourceCompilationUnits.get(0);
			cloneInfo.sourceICompilationUnit = (ICompilationUnit)cloneInfo.sourceCompilationUnit.getJavaElement();
			List<AbstractTypeDeclaration> topLevelTypeDeclarations = cloneInfo.sourceCompilationUnit.types();
			List<AbstractTypeDeclaration> allTypeDeclarations = new ArrayList<AbstractTypeDeclaration>();
			for(AbstractTypeDeclaration abstractTypeDeclaration : topLevelTypeDeclarations) {
				if(abstractTypeDeclaration instanceof TypeDeclaration) {
					TypeDeclaration topLevelTypeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
					allTypeDeclarations.add(topLevelTypeDeclaration);
					allTypeDeclarations.addAll(ASTReader.getRecursivelyInnerTypes(topLevelTypeDeclaration));
				}
			}
			for(AbstractTypeDeclaration abstractTypeDeclaration : allTypeDeclarations) {
				if(abstractTypeDeclaration instanceof TypeDeclaration) {
					TypeDeclaration typeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
					cloneInfo.sourceTypeDeclaration = typeDeclaration;
					cloneInfo.sourceRewriter = ASTRewrite.create(cloneInfo.sourceTypeDeclaration.getAST());
					cloneInfo.ast = cloneInfo.sourceTypeDeclaration.getAST();
					break;
				}
			}
		}
		else {
			//check if they have a common superclass
			ITypeBinding typeBinding1 = sourceTypeDeclarations.get(0).resolveBinding();
			ITypeBinding typeBinding2 = sourceTypeDeclarations.get(1).resolveBinding();
			commonSuperTypeOfSourceTypeDeclarations = ASTNodeMatcher.commonSuperType(typeBinding1, typeBinding2);
			if(commonSuperTypeOfSourceTypeDeclarations != null) {
				if(mapper.getCloneRefactoringType().equals(CloneRefactoringType.PULL_UP_TO_EXISTING_SUPERCLASS)) {
					IJavaElement javaElement = commonSuperTypeOfSourceTypeDeclarations.getJavaElement();
					javaElementsToOpenInEditor.add(javaElement);
					//special handling for the case the common superclass is an inner class
					IJavaElement parent = javaElement.getParent();
					while(!(parent instanceof ICompilationUnit)) {
						parent = parent.getParent();
					}
					ICompilationUnit iCompilationUnit = (ICompilationUnit)parent;
					ASTParser parser = ASTParser.newParser(ASTReader.JLS);
					parser.setKind(ASTParser.K_COMPILATION_UNIT);
					parser.setSource(iCompilationUnit);
					parser.setResolveBindings(true); // we need bindings later on
					CompilationUnit compilationUnit = (CompilationUnit)parser.createAST(null);
					List<AbstractTypeDeclaration> topLevelTypeDeclarations = compilationUnit.types();
					List<AbstractTypeDeclaration> allTypeDeclarations = new ArrayList<AbstractTypeDeclaration>();
					for(AbstractTypeDeclaration abstractTypeDeclaration : topLevelTypeDeclarations) {
						if(abstractTypeDeclaration instanceof TypeDeclaration) {
							TypeDeclaration topLevelTypeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
							allTypeDeclarations.add(topLevelTypeDeclaration);
							allTypeDeclarations.addAll(ASTReader.getRecursivelyInnerTypes(topLevelTypeDeclaration));
						}
					}
					for(AbstractTypeDeclaration abstractTypeDeclaration : allTypeDeclarations) {
						if(abstractTypeDeclaration instanceof TypeDeclaration) {
							TypeDeclaration typeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
							if(typeDeclaration.resolveBinding().isEqualTo(commonSuperTypeOfSourceTypeDeclarations)) {
								cloneInfo.sourceCompilationUnit = compilationUnit;
								cloneInfo.sourceICompilationUnit = iCompilationUnit;
								cloneInfo.sourceTypeDeclaration = typeDeclaration;
								cloneInfo.sourceRewriter = ASTRewrite.create(cloneInfo.sourceTypeDeclaration.getAST());
								cloneInfo.ast = cloneInfo.sourceTypeDeclaration.getAST();
								cloneInfo.superclassNotDirectlyInheritedFromRefactoredSubclasses =
										!superclassDirectlyInheritedFromRefactoredSubclasses(commonSuperTypeOfSourceTypeDeclarations, typeBinding1, typeBinding2);
								break;
							}
						}
					}
					MultiTextEdit multiTextEdit = new MultiTextEdit();
					CompilationUnitChange compilationUnitChange = new CompilationUnitChange("", iCompilationUnit);
					compilationUnitChange.setEdit(multiTextEdit);
					compilationUnitChanges.put(iCompilationUnit, compilationUnitChange);
				}
				else {
					//create an intermediate superclass or a utility class
					if(mapper.getCloneRefactoringType().equals(CloneRefactoringType.EXTRACT_STATIC_METHOD_TO_NEW_UTILITY_CLASS)) {
						cloneInfo.extractUtilityClass = true;
					}
					if(cloneInfo.extractUtilityClass) {
						cloneInfo.intermediateClassName = "Utility";
					}
					else {
						cloneInfo.intermediateClassName = "Intermediate" + commonSuperTypeOfSourceTypeDeclarations.getTypeDeclaration().getName();
					}
					ClassObject commonSuperType = ASTReader.getSystemObject().getClassObject(commonSuperTypeOfSourceTypeDeclarations.getQualifiedName());
					CompilationUnit compilationUnit = null;
					PackageDeclaration package1 = sourceCompilationUnits.get(0).getPackage();
					PackageDeclaration package2 = sourceCompilationUnits.get(1).getPackage();
					if(package1 != null && package2 != null && package1.resolveBinding().isEqualTo(package2.resolveBinding())) {
						compilationUnit = sourceCompilationUnits.get(0);
					}
					else if(package1 == null && package2 == null) {
						compilationUnit = sourceCompilationUnits.get(0);
					}
					else if(commonSuperType != null) {
						compilationUnit = RefactoringUtility.findCompilationUnit(commonSuperType.getAbstractTypeDeclaration());
					}
					else {
						compilationUnit = sourceCompilationUnits.get(0);
					}
					if(compilationUnit.getPackage() != null) {
						cloneInfo.intermediateClassPackageBinding = compilationUnit.getPackage().resolveBinding();
					}
					ICompilationUnit iCompilationUnit = (ICompilationUnit)compilationUnit.getJavaElement();
					IContainer container = (IContainer)iCompilationUnit.getResource().getParent();
					if(container instanceof IProject) {
						IProject contextProject = (IProject)container;
						cloneInfo.file = contextProject.getFile(cloneInfo.intermediateClassName + ".java");
					}
					else if(container instanceof IFolder) {
						IFolder contextFolder = (IFolder)container;
						cloneInfo.file = contextFolder.getFile(cloneInfo.intermediateClassName + ".java");
					}
					boolean intermediateAlreadyExists = false;
					ICompilationUnit intermediateICompilationUnit = JavaCore.createCompilationUnitFrom(cloneInfo.file);
					javaElementsToOpenInEditor.add(intermediateICompilationUnit);
					ASTParser intermediateParser = ASTParser.newParser(ASTReader.JLS);
					intermediateParser.setKind(ASTParser.K_COMPILATION_UNIT);
					if(cloneInfo.file.exists()) {
						intermediateAlreadyExists = true;
				        intermediateParser.setSource(intermediateICompilationUnit);
				        intermediateParser.setResolveBindings(true); // we need bindings later on
					}
					else {
						cloneInfo.document = new Document();
						intermediateParser.setSource(cloneInfo.document.get().toCharArray());
					}
					CompilationUnit intermediateCompilationUnit = (CompilationUnit)intermediateParser.createAST(null);
			        AST intermediateAST = intermediateCompilationUnit.getAST();
			        ASTRewrite intermediateRewriter = ASTRewrite.create(intermediateAST);
			        ListRewrite intermediateTypesRewrite = intermediateRewriter.getListRewrite(intermediateCompilationUnit, CompilationUnit.TYPES_PROPERTY);
			        TypeDeclaration intermediateTypeDeclaration = null;
					if(intermediateAlreadyExists) {
						List<AbstractTypeDeclaration> abstractTypeDeclarations = intermediateCompilationUnit.types();
						for(AbstractTypeDeclaration abstractTypeDeclaration : abstractTypeDeclarations) {
							if(abstractTypeDeclaration instanceof TypeDeclaration) {
								TypeDeclaration typeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
								if(typeDeclaration.getName().getIdentifier().equals(cloneInfo.intermediateClassName)) {
									intermediateTypeDeclaration = typeDeclaration;
									int intermediateModifiers = intermediateTypeDeclaration.getModifiers();
									if((intermediateModifiers & Modifier.ABSTRACT) == 0 && !cloneInfo.extractUtilityClass) {
										ListRewrite intermediateModifiersRewrite = intermediateRewriter.getListRewrite(intermediateTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
										intermediateModifiersRewrite.insertLast(intermediateAST.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
									}
									break;
								}
							}
						}
						MultiTextEdit intermediateMultiTextEdit = new MultiTextEdit();
						CompilationUnitChange intermediateCompilationUnitChange = new CompilationUnitChange("", intermediateICompilationUnit);
						intermediateCompilationUnitChange.setEdit(intermediateMultiTextEdit);
						compilationUnitChanges.put(intermediateICompilationUnit, intermediateCompilationUnitChange);
					}
					else {
						if(compilationUnit.getPackage() != null) {
							intermediateRewriter.set(intermediateCompilationUnit, CompilationUnit.PACKAGE_PROPERTY, compilationUnit.getPackage(), null);
						}
						intermediateTypeDeclaration = intermediateAST.newTypeDeclaration();
						SimpleName intermediateName = intermediateAST.newSimpleName(cloneInfo.intermediateClassName);
						intermediateRewriter.set(intermediateTypeDeclaration, TypeDeclaration.NAME_PROPERTY, intermediateName, null);
						ListRewrite typeParametersRewrite = intermediateRewriter.getListRewrite(intermediateTypeDeclaration, TypeDeclaration.TYPE_PARAMETERS_PROPERTY);
						ITypeBinding[] typeArguments = commonSuperTypeOfSourceTypeDeclarations.getTypeArguments();
						cloneInfo.intermediateClassTypeParameters = typeArguments;
						for(ITypeBinding typeParameterBinding : typeArguments) {
							typeParametersRewrite.insertLast(RefactoringUtility.generateTypeFromTypeBinding(typeParameterBinding, intermediateAST, intermediateRewriter), null);
						}
						ListRewrite intermediateModifiersRewrite = intermediateRewriter.getListRewrite(intermediateTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
						intermediateModifiersRewrite.insertLast(intermediateAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
						if(!cloneInfo.extractUtilityClass) {
							intermediateModifiersRewrite.insertLast(intermediateAST.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
							Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
							if(commonSuperTypeOfSourceTypeDeclarations.isClass()) {
								intermediateRewriter.set(intermediateTypeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY,
										RefactoringUtility.generateTypeFromTypeBinding(commonSuperTypeOfSourceTypeDeclarations, intermediateAST, intermediateRewriter), null);
								typeBindings.add(commonSuperTypeOfSourceTypeDeclarations);
							}
							ListRewrite interfaceRewrite = intermediateRewriter.getListRewrite(intermediateTypeDeclaration, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY);
							if(commonSuperTypeOfSourceTypeDeclarations.isInterface()) {
								ITypeBinding[] superInterfaces = commonSuperTypeOfSourceTypeDeclarations.getInterfaces();
								int taggingSuperInterfaceCount = 0;
								for(ITypeBinding superInterface : superInterfaces) {
									if(ASTNodeMatcher.isTaggingInterface(superInterface)) {
										taggingSuperInterfaceCount++;
									}
								}
								boolean allSuperInterfacesAreTaggingInterfaces = superInterfaces.length > 0 && taggingSuperInterfaceCount == superInterfaces.length;
								boolean bothSubClassesImplementCommonSuperType = ASTNodeMatcher.implementsInterface(typeBinding1, commonSuperTypeOfSourceTypeDeclarations) &&
										ASTNodeMatcher.implementsInterface(typeBinding2, commonSuperTypeOfSourceTypeDeclarations);
								if(!allSuperInterfacesAreTaggingInterfaces && !bothSubClassesImplementCommonSuperType) {
									Type interfaceType = RefactoringUtility.generateTypeFromTypeBinding(commonSuperTypeOfSourceTypeDeclarations, intermediateAST, intermediateRewriter);
									interfaceRewrite.insertLast(interfaceType, null);
									typeBindings.add(commonSuperTypeOfSourceTypeDeclarations);
								}
							}
							//add the implemented interfaces being common in both subclasses
							List<Type> superInterfaceTypes1 = sourceTypeDeclarations.get(0).superInterfaceTypes();
							List<Type> superInterfaceTypes2 = sourceTypeDeclarations.get(1).superInterfaceTypes();
							for(Type interfaceType1 : superInterfaceTypes1) {
								ITypeBinding interfaceTypeBinding1 = interfaceType1.resolveBinding();
								for(Type interfaceType2 : superInterfaceTypes2) {
									ITypeBinding interfaceTypeBinding2 = interfaceType2.resolveBinding();
									if(interfaceTypeBinding1.isEqualTo(interfaceTypeBinding2) && interfaceTypeBinding1.getQualifiedName().equals(interfaceTypeBinding2.getQualifiedName()) &&
											checkIfThisReferenceIsPassedAsArgumentToMethodInvocation(interfaceTypeBinding1)) {
										interfaceRewrite.insertLast(interfaceType1, null);
										typeBindings.add(interfaceTypeBinding1);
										break;
									}
								}
							}
							//copy the constructors declared in the subclasses that contain a super-constructor call
							ListRewrite bodyDeclarationsRewrite = intermediateRewriter.getListRewrite(intermediateTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
							Set<String> processedSuperConstructorBindingKeys = new LinkedHashSet<String>();
							for(MethodDeclaration methodDeclaration1 : sourceTypeDeclarations.get(0).getMethods()) {
								if(methodDeclaration1.isConstructor()) {
									boolean matchingSuperConstructorCallFound = false;
									SuperConstructorInvocation superConstructorInvocation1 = firstStatementIsSuperConstructorInvocation(methodDeclaration1);
									String superConstructorBindingKey1 = superConstructorInvocation1 != null ? superConstructorInvocation1.resolveConstructorBinding().getKey() : null;
									for(MethodDeclaration methodDeclaration2 : sourceTypeDeclarations.get(1).getMethods()) {
										if(methodDeclaration2.isConstructor()) {
											SuperConstructorInvocation superConstructorInvocation2 = firstStatementIsSuperConstructorInvocation(methodDeclaration2);
											if(superConstructorInvocation1 != null && superConstructorInvocation2 != null) {
												List<Expression> superConstructorArguments1 = superConstructorInvocation1.arguments();
												List<Expression> superConstructorArguments2 = superConstructorInvocation2.arguments();
												if(matchingArgumentTypes(superConstructorArguments1, superConstructorArguments2) ||
														superConstructorInvocation1.resolveConstructorBinding().isEqualTo(superConstructorInvocation2.resolveConstructorBinding())) {
													matchingSuperConstructorCallFound = true;
													if(!processedSuperConstructorBindingKeys.contains(superConstructorBindingKey1)) {
														processedSuperConstructorBindingKeys.add(superConstructorBindingKey1);
														if(compareStatements(sourceCompilationUnits.get(0).getTypeRoot(), sourceCompilationUnits.get(1).getTypeRoot(),
																superConstructorInvocation1, superConstructorInvocation2)) {
															MethodDeclaration constructor = copyConstructor(methodDeclaration1, intermediateAST, intermediateRewriter, intermediateName, requiredImportTypeBindings);
															bodyDeclarationsRewrite.insertLast(constructor, null);
														}
														else {
															MethodDeclaration constructor = intermediateAST.newMethodDeclaration();
															intermediateRewriter.set(constructor, MethodDeclaration.NAME_PROPERTY, intermediateName, null);
															intermediateRewriter.set(constructor, MethodDeclaration.CONSTRUCTOR_PROPERTY, true, null);
															ListRewrite constructorModifierRewriter = intermediateRewriter.getListRewrite(constructor, MethodDeclaration.MODIFIERS2_PROPERTY);
															List<IExtendedModifier> modifiers = methodDeclaration1.modifiers();
															for(IExtendedModifier modifier : modifiers) {
																if(modifier instanceof Modifier) {
																	constructorModifierRewriter.insertLast((Modifier)modifier, null);
																}
															}
															ListRewrite parameterRewriter = intermediateRewriter.getListRewrite(constructor, MethodDeclaration.PARAMETERS_PROPERTY);
															SuperConstructorInvocation superConstructorInvocation = intermediateAST.newSuperConstructorInvocation();
															ListRewrite argumentRewriter = intermediateRewriter.getListRewrite(superConstructorInvocation, SuperConstructorInvocation.ARGUMENTS_PROPERTY);
															Map<String, Integer> parameterTypeCounterMap = new LinkedHashMap<String, Integer>();
															for(ITypeBinding argumentTypeBinding : superConstructorInvocation1.resolveConstructorBinding().getParameterTypes()) {
																SingleVariableDeclaration parameter = intermediateAST.newSingleVariableDeclaration();
																typeBindings.add(argumentTypeBinding);
																Type parameterType = RefactoringUtility.generateTypeFromTypeBinding(argumentTypeBinding, intermediateAST, intermediateRewriter);
																intermediateRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, parameterType, null);
																String typeName = null;
																if(argumentTypeBinding.isArray()) {
																	typeName = argumentTypeBinding.getElementType().getName();
																}
																else if(argumentTypeBinding.isParameterizedType()) {
																	typeName = argumentTypeBinding.getErasure().getName();
																}
																else {
																	typeName = argumentTypeBinding.getName();
																}
																String parameterName = null;
																if(argumentTypeBinding.isPrimitive()) {
																	parameterName = Character.toString(typeName.charAt(0));
																}
																else if(typeName.equals("Class")) {
																	parameterName = "clazz";
																}
																else {
																	parameterName = typeName.replaceFirst(Character.toString(typeName.charAt(0)), Character.toString(Character.toLowerCase(typeName.charAt(0))));
																}
																if(parameterTypeCounterMap.containsKey(argumentTypeBinding.getKey())) {
																	int previousCounter = parameterTypeCounterMap.get(argumentTypeBinding.getKey());
																	parameterName += previousCounter;
																	int currentCounter = previousCounter + 1;
																	parameterTypeCounterMap.put(argumentTypeBinding.getKey(), currentCounter);
																}
																else {
																	parameterTypeCounterMap.put(argumentTypeBinding.getKey(), 1);
																}
																SimpleName parameterSimpleName = intermediateAST.newSimpleName(parameterName);
																intermediateRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, parameterSimpleName, null);
																parameterRewriter.insertLast(parameter, null);
																argumentRewriter.insertLast(parameterSimpleName, null);
															}
															Block constructorBody = intermediateAST.newBlock();
															ListRewrite constructorBodyRewriter = intermediateRewriter.getListRewrite(constructorBody, Block.STATEMENTS_PROPERTY);
															constructorBodyRewriter.insertLast(superConstructorInvocation, null);
															intermediateRewriter.set(constructor, MethodDeclaration.BODY_PROPERTY, constructorBody, null);
															bodyDeclarationsRewrite.insertLast(constructor, null);
														}
													}
												}
											}
										}
									}
									if(!matchingSuperConstructorCallFound && superConstructorInvocation1 != null) {
										if(!processedSuperConstructorBindingKeys.contains(superConstructorBindingKey1)) {
											processedSuperConstructorBindingKeys.add(superConstructorBindingKey1);
											MethodDeclaration constructor = copyConstructor(methodDeclaration1, intermediateAST, intermediateRewriter, intermediateName, requiredImportTypeBindings);
											bodyDeclarationsRewrite.insertLast(constructor, null);
											constructorsToBeCopiedInSubclasses.get(1).add(methodDeclaration1);
										}
									}
								}
							}
							//handle constructors existing only in the second subclass
							for(MethodDeclaration methodDeclaration2 : sourceTypeDeclarations.get(1).getMethods()) {
								if(methodDeclaration2.isConstructor()) {
									boolean matchingSuperConstructorCallFound = false;
									SuperConstructorInvocation superConstructorInvocation2 = firstStatementIsSuperConstructorInvocation(methodDeclaration2);
									String superConstructorBindingKey2 = superConstructorInvocation2 != null ? superConstructorInvocation2.resolveConstructorBinding().getKey() : null;
									for(MethodDeclaration methodDeclaration1 : sourceTypeDeclarations.get(0).getMethods()) {
										if(methodDeclaration1.isConstructor()) {
											SuperConstructorInvocation superConstructorInvocation1 = firstStatementIsSuperConstructorInvocation(methodDeclaration1);
											if(superConstructorInvocation1 != null && superConstructorInvocation2 != null) {
												List<Expression> superConstructorArguments1 = superConstructorInvocation1.arguments();
												List<Expression> superConstructorArguments2 = superConstructorInvocation2.arguments();
												if(matchingArgumentTypes(superConstructorArguments1, superConstructorArguments2) ||
														superConstructorInvocation1.resolveConstructorBinding().isEqualTo(superConstructorInvocation2.resolveConstructorBinding())) {
													matchingSuperConstructorCallFound = true;
												}
											}
										}
									}
									if(!matchingSuperConstructorCallFound && superConstructorInvocation2 != null) {
										if(!processedSuperConstructorBindingKeys.contains(superConstructorBindingKey2)) {
											processedSuperConstructorBindingKeys.add(superConstructorBindingKey2);
											MethodDeclaration constructor = copyConstructor(methodDeclaration2, intermediateAST, intermediateRewriter, intermediateName, requiredImportTypeBindings);
											bodyDeclarationsRewrite.insertLast(constructor, null);
											constructorsToBeCopiedInSubclasses.get(0).add(methodDeclaration2);
										}
									}
								}
							}
							if((noneOfTheConstructorsContainsSuperConstructorCall(sourceTypeDeclarations.get(0)) && !containsConstructorCallingSuperConstructorWithoutArguments(sourceTypeDeclarations.get(1))) ||
									(noneOfTheConstructorsContainsSuperConstructorCall(sourceTypeDeclarations.get(1)) && !containsConstructorCallingSuperConstructorWithoutArguments(sourceTypeDeclarations.get(0)))) {
								boolean commonSuperTypeDeclaresConstructorWithoutParameters = false;
								for(IMethodBinding methodBinding : commonSuperTypeOfSourceTypeDeclarations.getDeclaredMethods()) {
									if(methodBinding.isConstructor() && methodBinding.getParameterTypes().length == 0) {
										commonSuperTypeDeclaresConstructorWithoutParameters = true;
										break;
									}
								}
								if(commonSuperTypeDeclaresConstructorWithoutParameters) {
									MethodDeclaration constructor = createDefaultConstructor(intermediateAST, intermediateRewriter, intermediateName);
									bodyDeclarationsRewrite.insertLast(constructor, null);
								}
							}
							RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
						}
						intermediateTypesRewrite.insertLast(intermediateTypeDeclaration, null);
					}
					cloneInfo.sourceCompilationUnit = intermediateCompilationUnit;
					cloneInfo.sourceICompilationUnit = intermediateICompilationUnit;
					cloneInfo.sourceTypeDeclaration = intermediateTypeDeclaration;
					cloneInfo.sourceRewriter = intermediateRewriter;
					cloneInfo.ast = intermediateAST;
				}
			}
		}

		ASTRewrite sourceRewriter = cloneInfo.sourceRewriter;
		AST ast = cloneInfo.ast;
		TypeDeclaration sourceTypeDeclaration = cloneInfo.sourceTypeDeclaration;
		MethodDeclaration sourceMethodDeclaration = sourceMethodDeclarations.get(0);
		ListRewrite bodyDeclarationsRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		if(commonSuperTypeOfSourceTypeDeclarations != null) {
			Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
			typeBindings.add(commonSuperTypeOfSourceTypeDeclarations);
			RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
		}
		Set<VariableDeclaration> accessedLocalFieldsG1 = getLocallyAccessedFields(mapper.getDirectlyAccessedLocalFieldsG1(), sourceTypeDeclarations.get(0));
		Set<VariableDeclaration> accessedLocalFieldsG2 = getLocallyAccessedFields(mapper.getDirectlyAccessedLocalFieldsG2(), sourceTypeDeclarations.get(1));
		Set<VariableDeclaration> modifiedLocalFieldsG1 = getLocallyAccessedFields(mapper.getDirectlyModifiedLocalFieldsG1(), sourceTypeDeclarations.get(0));
		Set<VariableDeclaration> modifiedLocalFieldsG2 = getLocallyAccessedFields(mapper.getDirectlyModifiedLocalFieldsG2(), sourceTypeDeclarations.get(1));
		if(!sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclarations.get(1).resolveBinding()) ||
				!sourceTypeDeclarations.get(0).resolveBinding().getQualifiedName().equals(sourceTypeDeclarations.get(1).resolveBinding().getQualifiedName())) {
			pullUpLocallyAccessedFields(accessedLocalFieldsG1, accessedLocalFieldsG2, modifiedLocalFieldsG1, modifiedLocalFieldsG2, bodyDeclarationsRewrite, requiredImportTypeBindings);

			Set<VariableDeclaration> indirectlyAccessedLocalFieldsG1 = getLocallyAccessedFields(mapper.getIndirectlyAccessedLocalFieldsG1(), sourceTypeDeclarations.get(0));
			Set<VariableDeclaration> indirectlyAccessedLocalFieldsG2 = getLocallyAccessedFields(mapper.getIndirectlyAccessedLocalFieldsG2(), sourceTypeDeclarations.get(1));
			Set<VariableDeclaration> indirectlyModifiedLocalFieldsG1 = getLocallyAccessedFields(mapper.getIndirectlyModifiedLocalFieldsG1(), sourceTypeDeclarations.get(0));
			Set<VariableDeclaration> indirectlyModifiedLocalFieldsG2 = getLocallyAccessedFields(mapper.getIndirectlyModifiedLocalFieldsG2(), sourceTypeDeclarations.get(1));
			Set<MethodObject> accessedLocalMethodsG1 = mapper.getAccessedLocalMethodsG1();
			Set<MethodObject> accessedLocalMethodsG2 = mapper.getAccessedLocalMethodsG2();
			for(MethodObject localMethodG1 : accessedLocalMethodsG1) {
				MethodDeclaration methodDeclaration1 = localMethodG1.getMethodDeclaration();
				for(MethodObject localMethodG2 : accessedLocalMethodsG2) {
					MethodDeclaration methodDeclaration2 = localMethodG2.getMethodDeclaration();
					ITypeBinding returnTypesCommonSuperType = ASTNodeMatcher.commonSuperType(localMethodG1.getMethodDeclaration().getReturnType2().resolveBinding(), localMethodG2.getMethodDeclaration().getReturnType2().resolveBinding());
					if(localMethodG1.getName().equals(localMethodG2.getName()) &&
							(localMethodG1.getReturnType().equals(localMethodG2.getReturnType()) || ASTNodeMatcher.validCommonSuperType(returnTypesCommonSuperType)) &&
							(localMethodG1.getParameterTypeList().equals(localMethodG2.getParameterTypeList()) ||
							//only for direct method calls, we allow them having parameter types with subclass type differences
							(MethodCallAnalyzer.equalSignatureIgnoringSubclassTypeDifferences(methodDeclaration1.resolveBinding(), methodDeclaration2.resolveBinding()) &&
							mapper.getDirectlyAccessedLocalMethodsG1().contains(localMethodG1) &&
							mapper.getDirectlyAccessedLocalMethodsG2().contains(localMethodG2))) ) {
						Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
						boolean clones = type2Clones(methodDeclaration1, methodDeclaration2);
						Type returnType = methodDeclaration1.getReturnType2();
						TypeDeclaration typeDeclaration1 = RefactoringUtility.findTypeDeclaration(methodDeclaration1);
						TypeDeclaration typeDeclaration2 = RefactoringUtility.findTypeDeclaration(methodDeclaration2);
						Set<VariableDeclaration> fieldsAccessedInMethod1 = getFieldsAccessedInMethod(indirectlyAccessedLocalFieldsG1, methodDeclaration1);
						Set<VariableDeclaration> fieldsAccessedInMethod2 = getFieldsAccessedInMethod(indirectlyAccessedLocalFieldsG2, methodDeclaration2);
						Set<VariableDeclaration> fieldsModifiedInMethod1 = getFieldsAccessedInMethod(indirectlyModifiedLocalFieldsG1, methodDeclaration1);
						Set<VariableDeclaration> fieldsModifiedInMethod2 = getFieldsAccessedInMethod(indirectlyModifiedLocalFieldsG2, methodDeclaration2);
						if(!typeDeclaration1.resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding()) &&
								!typeDeclaration2.resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding()) &&
								!methodDeclaration1.resolveBinding().getDeclaringClass().isEqualTo(commonSuperTypeOfSourceTypeDeclarations) &&
								!methodDeclaration2.resolveBinding().getDeclaringClass().isEqualTo(commonSuperTypeOfSourceTypeDeclarations) &&
								!commonSuperTypeDeclaresOrInheritsMethodWithIdenticalSignature(methodDeclaration1.resolveBinding(), commonSuperTypeOfSourceTypeDeclarations) &&
								!commonSuperTypeDeclaresOrInheritsMethodWithIdenticalSignature(methodDeclaration2.resolveBinding(), commonSuperTypeOfSourceTypeDeclarations)) {
							boolean avoidPullUpDueToSerialization1 = avoidPullUpMethodDueToSerialization(sourceTypeDeclarations.get(0), fieldsAccessedInMethod1, fieldsModifiedInMethod1);
							boolean avoidPullUpDueToSerialization2 = avoidPullUpMethodDueToSerialization(sourceTypeDeclarations.get(1), fieldsAccessedInMethod2, fieldsModifiedInMethod2);
							if(clones && !avoidPullUpDueToSerialization1 && !avoidPullUpDueToSerialization2 &&
									typeContainsMethodWithSignature(sourceTypeDeclaration, methodDeclaration1) == null && typeContainsMethodWithSignature(sourceTypeDeclaration, methodDeclaration2) == null) {
								MethodDeclaration copiedMethodDeclaration = (MethodDeclaration) ASTNode.copySubtree(ast, methodDeclaration1);
								ListRewrite modifiersRewrite = sourceRewriter.getListRewrite(copiedMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
								List<IExtendedModifier> originalModifiers = copiedMethodDeclaration.modifiers();
								for(IExtendedModifier extendedModifier : originalModifiers) {
									if(extendedModifier.isModifier()) {
										Modifier modifier = (Modifier)extendedModifier;
										if(modifier.isPrivate()) {
											modifiersRewrite.replace(modifier, ast.newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD), null);
										}
									}
								}
								if(!localMethodG1.getReturnType().equals(localMethodG2.getReturnType()) && ASTNodeMatcher.validCommonSuperType(returnTypesCommonSuperType)) {
									Type newReturnType = RefactoringUtility.generateTypeFromTypeBinding(returnTypesCommonSuperType, ast, sourceRewriter);
									sourceRewriter.set(copiedMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, newReturnType, null);
									typeBindings.add(returnTypesCommonSuperType);
								}
								if(!localMethodG1.isStatic() && localMethodG2.isStatic()) {
									modifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD), null);
								}
								bodyDeclarationsRewrite.insertLast(copiedMethodDeclaration, null);
								/*typeBindings.add(returnType.resolveBinding());
								List<SingleVariableDeclaration> parameters = methodDeclaration1.parameters();
								for(SingleVariableDeclaration parameter : parameters) {
									typeBindings.add(parameter.getType().resolveBinding());
								}
								List<Name> thrownExceptions = methodDeclaration1.thrownExceptions();
								for(Name thrownException : thrownExceptions) {
									typeBindings.add(thrownException.resolveTypeBinding());
								}*/
								TypeVisitor typeVisitor = new TypeVisitor();
								methodDeclaration1.accept(typeVisitor);
								typeBindings.addAll(typeVisitor.getTypeBindings());
								//check if the pulled up method is using fields that should be also pulled up, remove fields that have been already pulled up
								fieldsAccessedInMethod1.removeAll(accessedLocalFieldsG1);
								fieldsAccessedInMethod2.removeAll(accessedLocalFieldsG2);
								fieldsModifiedInMethod1.removeAll(modifiedLocalFieldsG1);
								fieldsModifiedInMethod2.removeAll(modifiedLocalFieldsG2);
								pullUpLocallyAccessedFields(fieldsAccessedInMethod1, fieldsAccessedInMethod2, fieldsModifiedInMethod1, fieldsModifiedInMethod2, bodyDeclarationsRewrite, requiredImportTypeBindings);
								if(!typeDeclaration1.resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding()) && typeContainsMethodWithSignature(sourceTypeDeclaration, methodDeclaration1) == null) {
									methodDeclarationsToBePulledUp.get(0).add(methodDeclaration1);
								}
								if(!typeDeclaration2.resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding()) && typeContainsMethodWithSignature(sourceTypeDeclaration, methodDeclaration2) == null) {
									methodDeclarationsToBePulledUp.get(1).add(methodDeclaration2);
								}
							}
							else {
								if(mapper.getCloneRefactoringType().equals(CloneRefactoringType.EXTRACT_STATIC_METHOD_TO_NEW_UTILITY_CLASS)) {
									//static methods with the same signature, but different bodies are called. A parameter should be introduced in the extracted method
									createDifferencesForStaticMethodCalls(methodDeclaration1.resolveBinding(), methodDeclaration2.resolveBinding());
								}
								else if(!containsSuperMethodCall(typeDeclaration1, methodDeclaration1.resolveBinding()) && !containsSuperMethodCall(typeDeclaration2, methodDeclaration2.resolveBinding()) &&
										typeContainsMethodWithSignature(sourceTypeDeclaration, methodDeclaration1) == null && typeContainsMethodWithSignature(sourceTypeDeclaration, methodDeclaration2) == null) {
									MethodDeclaration newMethodDeclaration = ast.newMethodDeclaration();
									sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, ast.newSimpleName(methodDeclaration1.getName().getIdentifier()), null);
									if(localMethodG1.getReturnType().equals(localMethodG2.getReturnType())) {
										sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnType, null);
										typeBindings.add(returnType.resolveBinding());
									}
									else if(ASTNodeMatcher.validCommonSuperType(returnTypesCommonSuperType)) {
										Type newReturnType = RefactoringUtility.generateTypeFromTypeBinding(returnTypesCommonSuperType, ast, sourceRewriter);
										sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, newReturnType, null);
										typeBindings.add(returnTypesCommonSuperType);
									}
									ListRewrite modifiersRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
									List<IExtendedModifier> originalModifiers = methodDeclaration1.modifiers();
									for(IExtendedModifier extendedModifier : originalModifiers) {
										if(extendedModifier.isModifier()) {
											Modifier modifier = (Modifier)extendedModifier;
											if(modifier.isProtected()) {
												modifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD), null);
												if((methodDeclaration2.getModifiers() & Modifier.PROTECTED) == 0) {
													updateAccessModifier(methodDeclaration2, Modifier.ModifierKeyword.PROTECTED_KEYWORD);
												}
											}
											else if(modifier.isPublic()) {
												if((methodDeclaration2.getModifiers() & Modifier.PUBLIC) != 0) {
													modifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
												}
												else if((methodDeclaration2.getModifiers() & Modifier.PROTECTED) != 0) {
													modifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD), null);
												}
												if((methodDeclaration2.getModifiers() & Modifier.PUBLIC) == 0) {
													updateAccessModifier(methodDeclaration2, Modifier.ModifierKeyword.PUBLIC_KEYWORD);
												}
											}
											else if(modifier.isPrivate()) {
												modifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD), null);
												//change modifiers to protected in the subclasses
												updateAccessModifier(methodDeclaration1, Modifier.ModifierKeyword.PROTECTED_KEYWORD);
												if((methodDeclaration2.getModifiers() & Modifier.PROTECTED) == 0) {
													updateAccessModifier(methodDeclaration2, Modifier.ModifierKeyword.PROTECTED_KEYWORD);
												}
											}
										}
									}
									if(cloneInfo.superclassNotDirectlyInheritedFromRefactoredSubclasses) {
										Block methodBody = ast.newBlock();
										sourceRewriter.set(newMethodDeclaration, MethodDeclaration.BODY_PROPERTY, methodBody, null);
										//create a default return statement
										Expression returnedExpression = generateDefaultValue(sourceRewriter, ast, returnType.resolveBinding());
										if(returnedExpression != null) {
											ReturnStatement returnStatement = ast.newReturnStatement();
											sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, returnedExpression, null);
											ListRewrite statementsRewrite = sourceRewriter.getListRewrite(methodBody, Block.STATEMENTS_PROPERTY);
											statementsRewrite.insertLast(returnStatement, null);
										}
									}
									else {
										modifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
									}
									ListRewrite parametersRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
									List<SingleVariableDeclaration> parameters1 = methodDeclaration1.parameters();
									List<SingleVariableDeclaration> parameters2 = methodDeclaration2.parameters();
									int parameterIndex = 0;
									for(SingleVariableDeclaration parameter1 : parameters1) {
										SingleVariableDeclaration parameter2 = parameters2.get(parameterIndex);
										ITypeBinding parameterTypeBinding1 = parameter1.getType().resolveBinding();
										ITypeBinding parameterTypeBinding2 = parameter2.getType().resolveBinding();
										if(parameterTypeBinding1.isEqualTo(parameterTypeBinding2) && parameterTypeBinding1.getQualifiedName().equals(parameterTypeBinding2.getQualifiedName())) {
											parametersRewrite.insertLast(parameter1, null);
											typeBindings.add(parameterTypeBinding1);
										}
										else {
											ITypeBinding parameterCommonSuperTypeBinding = ASTNodeMatcher.commonSuperType(parameterTypeBinding1, parameterTypeBinding2);
											Type parameterType = RefactoringUtility.generateTypeFromTypeBinding(parameterCommonSuperTypeBinding, ast, sourceRewriter);
											SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
											sourceRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, parameterType, null);
											sourceRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, ast.newSimpleName(parameter1.getName().getIdentifier()), null);
											parametersRewrite.insertLast(parameter, null);
											typeBindings.add(parameterCommonSuperTypeBinding);
										}
										parameterIndex++;
									}
									ListRewrite thrownExceptionsRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
									List<Name> thrownExceptions = methodDeclaration1.thrownExceptions();
									for(Name thrownException : thrownExceptions) {
										thrownExceptionsRewrite.insertLast(thrownException, null);
										typeBindings.add(thrownException.resolveTypeBinding());
									}
									bodyDeclarationsRewrite.insertLast(newMethodDeclaration, null);
								}
							}
						}
						RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
						break;
					}
				}
			}
		}
		
		MethodDeclaration newMethodDeclaration = ast.newMethodDeclaration();
		//extractedMethodName = sourceMethodDeclaration.getName().getIdentifier();
		sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, ast.newSimpleName(extractedMethodName), null);
		List<VariableDeclaration> returnedVariables1 = this.returnedVariables.get(0);
		List<VariableDeclaration> returnedVariables2 = this.returnedVariables.get(1);
		ITypeBinding returnTypeBinding = mapper.getReturnTypeBinding();
		if(returnTypeBinding != null) {
			Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
			typeBindings.add(returnTypeBinding);
			RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
			Type returnType = containsImportWithNameClash(cloneInfo.sourceCompilationUnit, returnTypeBinding) ?
					RefactoringUtility.generateQualifiedTypeFromTypeBinding(returnTypeBinding, ast, sourceRewriter) :
					RefactoringUtility.generateTypeFromTypeBinding(returnTypeBinding, ast, sourceRewriter);
			sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnType, null);
		}
		else {
			sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, ast.newPrimitiveType(PrimitiveType.VOID), null);
		}
		
		ListRewrite modifierRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		if(sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding()) &&
				sourceTypeDeclarations.get(1).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
			Modifier accessModifier = newMethodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD);
			modifierRewrite.insertLast(accessModifier, null);
		}
		else if(cloneInfo.extractUtilityClass) {
			Modifier accessModifier = newMethodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
			modifierRewrite.insertLast(accessModifier, null);
		}
		else {
			Modifier accessModifier = newMethodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD);
			modifierRewrite.insertLast(accessModifier, null);
		}
		
		boolean isSourceMethodDeclaration1Static = (sourceMethodDeclarations.get(0).getModifiers() & Modifier.STATIC) != 0;
		boolean isSourceMethodDeclaration2Static = (sourceMethodDeclarations.get(1).getModifiers() & Modifier.STATIC) != 0;
		if(isSourceMethodDeclaration1Static ||	isSourceMethodDeclaration2Static || cloneInfo.extractUtilityClass) {
			Modifier staticModifier = newMethodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD);
			modifierRewrite.insertLast(staticModifier, null);
		}
	
		ListRewrite parameterRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		Map<VariableBindingKeyPair, ArrayList<VariableDeclaration>> commonPassedParameters = mapper.getCommonPassedParameters();
		for(VariableBindingKeyPair parameterName : commonPassedParameters.keySet()) {
			ArrayList<VariableDeclaration> variableDeclarations = commonPassedParameters.get(parameterName);
			VariableDeclaration variableDeclaration1 = variableDeclarations.get(0);
			VariableDeclaration variableDeclaration2 = variableDeclarations.get(1);
			if(parameterIsUsedByNodesWithoutDifferences(variableDeclaration1, variableDeclaration2)) {
				if(!variableDeclaration1.resolveBinding().isField() || !variableDeclaration2.resolveBinding().isField()) {
					ITypeBinding typeBinding1 = extractTypeBinding(variableDeclaration1);
					ITypeBinding typeBinding2 = extractTypeBinding(variableDeclaration2);
					ITypeBinding typeBinding = PreconditionExaminer.determineType(typeBinding1, typeBinding2); 
					boolean makeQualifiedType = RefactoringUtility.hasQualifiedType(variableDeclaration1) && RefactoringUtility.hasQualifiedType(variableDeclaration2);
					Type variableType = makeQualifiedType ? RefactoringUtility.generateQualifiedTypeFromTypeBinding(typeBinding, ast, sourceRewriter) :
						RefactoringUtility.generateTypeFromTypeBinding(typeBinding, ast, sourceRewriter);
					Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
					typeBindings.add(typeBinding);
					RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
					SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
					sourceRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, variableDeclaration1.getName(), null);
					sourceRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, variableType, null);
					parameterRewrite.insertLast(parameter, null);
					originalPassedParameters.put(parameterName, variableDeclarations);
				}
			}
		}
		
		Block newMethodBody = newMethodDeclaration.getAST().newBlock();
		ListRewrite methodBodyRewrite = sourceRewriter.getListRewrite(newMethodBody, Block.STATEMENTS_PROPERTY);
		Set<ITypeBinding> thrownExceptionTypeBindings = new LinkedHashSet<ITypeBinding>();
		for(PDGNodeMapping pdgNodeMapping : sortedNodeMappings) {
			PDGNode pdgNode1 = pdgNodeMapping.getNodeG1();
			AbstractStatement statement1 = pdgNode1.getStatement();
			PDGBlockNode blockNode1 = mapper.getPDG1().isNestedWithinBlockNode(pdgNode1);
			if(blockNode1 != null && blockNode1 instanceof PDGTryNode && ((PDGTryNode)blockNode1).hasCatchClause() && mapper.getRemovableNodesG1().contains(blockNode1)) {
				//do nothing
			}
			else {
				ThrownExceptionVisitor thrownExceptionVisitor = new ThrownExceptionVisitor();
				statement1.getStatement().accept(thrownExceptionVisitor);
				for(ITypeBinding thrownException : thrownExceptionVisitor.getTypeBindings()) {
					if(pdgNode1.getThrownExceptionTypes().contains(thrownException.getQualifiedName())) {
						addTypeBinding(thrownException, thrownExceptionTypeBindings);
					}
				}
			}
			RefactoringUtility.getSimpleTypeBindings(extractTypeBindings(statement1), requiredImportTypeBindings);
			
			PDGNode pdgNode2 = pdgNodeMapping.getNodeG2();
			AbstractStatement statement2 = pdgNode2.getStatement();
			PDGBlockNode blockNode2 = mapper.getPDG2().isNestedWithinBlockNode(pdgNode2);
			if(blockNode2 != null && blockNode2 instanceof PDGTryNode && ((PDGTryNode)blockNode2).hasCatchClause() && mapper.getRemovableNodesG2().contains(blockNode2)) {
				//do nothing
			}
			else {
				ThrownExceptionVisitor thrownExceptionVisitor = new ThrownExceptionVisitor();
				statement2.getStatement().accept(thrownExceptionVisitor);
				for(ITypeBinding thrownException : thrownExceptionVisitor.getTypeBindings()) {
					if(pdgNode2.getThrownExceptionTypes().contains(thrownException.getQualifiedName())) {
						addTypeBinding(thrownException, thrownExceptionTypeBindings);
					}
				}
			}
			RefactoringUtility.getSimpleTypeBindings(extractTypeBindings(statement2), requiredImportTypeBindings);
		}
		
		ListRewrite thrownExceptionRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
		List<Name> thrownExceptions1 = sourceMethodDeclarations.get(0).thrownExceptions();
		List<Name> thrownExceptions2 = sourceMethodDeclarations.get(1).thrownExceptions();
		for(Name thrownException1 : thrownExceptions1) {
			for(Name thrownException2 : thrownExceptions2) {
				if(thrownException1.resolveTypeBinding().isEqualTo(thrownException2.resolveTypeBinding()) && thrownExceptionTypeBindings.contains(thrownException1.resolveTypeBinding())) {
					thrownExceptionRewrite.insertLast(thrownException1, null);
					Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
					typeBindings.add(thrownException1.resolveTypeBinding());
					RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
					thrownExceptionTypeBindings.remove(thrownException1.resolveTypeBinding());
					break;
				}
			}
		}
		//add remaining thrown exception types that have not been found in the signatures of the method declarations
		for(ITypeBinding thrownExceptionTypeBinding : thrownExceptionTypeBindings) {
			thrownExceptionRewrite.insertLast(ast.newSimpleName(thrownExceptionTypeBinding.getName()), null);
			Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
			typeBindings.add(thrownExceptionTypeBinding);
			RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
		}
		
		CloneStructureNode root = mapper.getCloneStructureRoot();
		for(CloneStructureNode child : root.getChildren()) {
			if(processableNode(child)) {
				Statement statement = processCloneStructureNode(child, ast, sourceRewriter);
				if(processableMappedNode(child) && ((PDGNodeMapping)child.getMapping()).declaresInconsistentlyRenamedVariable(mapper.getRenamedVariableBindings()) &&
						mapper.movableBeforeFirstMappedNode((PDGNodeMapping)child.getMapping())) {
					methodBodyRewrite.insertFirst(statement, null);
				}
				else {
					methodBodyRewrite.insertLast(statement, null);
				}
			}
		}
		if(returnedVariables1.size() == 1 && returnedVariables2.size() == 1 &&
				!mappedNodesContainStatementReturningVariable(returnedVariables1.get(0), returnedVariables2.get(0))) {
			ReturnStatement returnStatement = ast.newReturnStatement();
			BindingSignaturePair variableBelongingToParameterizedDifferences = variableBelongsToParameterizedDifferences(returnedVariables1.get(0), returnedVariables2.get(0));
			if(variableBelongingToParameterizedDifferences != null) {
				int existingArgValue = findExistingParametersWithArgName();
				int i = 0;
				if(existingArgValue > 0) {
					i = existingArgValue + 1;
				}
				for(BindingSignaturePair pair : parameterizedDifferenceMap.keySet()) {
					if(pair.equals(variableBelongingToParameterizedDifferences)) {
						break;
					}
					i++;
				}
				sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, ast.newSimpleName("arg" + i), null);
			}
			else {
				sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, returnedVariables1.get(0).getName(), null);
			}
			methodBodyRewrite.insertLast(returnStatement, null);
			if(!mappedNodesContainStatementDeclaringVariable(returnedVariables1.get(0), returnedVariables2.get(0)) &&
					!mappedNodesContainDifferentStatementsDeclaringVariables(returnedVariables1.get(0), returnedVariables2.get(0)) &&
					variableBelongingToParameterizedDifferences == null &&
					!variableIsPassedAsCommonParameter(returnedVariables1.get(0), returnedVariables2.get(0))) {
				ITypeBinding returnedTypeBinding = extractTypeBinding(returnedVariables1.get(0));
				Expression initializer = generateDefaultValue(sourceRewriter, ast, returnedTypeBinding);
				VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
				sourceRewriter.set(fragment, VariableDeclarationFragment.NAME_PROPERTY, returnedVariables1.get(0).getName(), null);
				sourceRewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, initializer, null);
				VariableDeclarationStatement declarationStatement = ast.newVariableDeclarationStatement(fragment);
				Type returnedType = RefactoringUtility.generateTypeFromTypeBinding(returnedTypeBinding, ast, sourceRewriter);
				sourceRewriter.set(declarationStatement, VariableDeclarationStatement.TYPE_PROPERTY, returnedType, null);
				methodBodyRewrite.insertFirst(declarationStatement, null);
			}
		}
		else {
			if(returnTypeBinding != null && !root.containsMappedReturnStatementInDirectChildren() && !root.lastIfElseIfChainContainsReturnOrThrowStatements()) {
				//create a default return statement at the end of the method
				ReturnStatement returnStatement = ast.newReturnStatement();
				Expression expression = generateDefaultValue(sourceRewriter, ast, returnTypeBinding);
				sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, expression, null);
				methodBodyRewrite.insertLast(returnStatement, null);
			}
		}
		
		//add parameters for the differences between the clones
		int existingArgValue = findExistingParametersWithArgName();
		int i = 0;
		if(existingArgValue > 0) {
			i = existingArgValue + 1;
		}
		for(ASTNodeDifference difference : parameterizedDifferenceMap.values()) {
			AbstractExpression expression1 = difference.getExpression1();
			AbstractExpression expression2 = difference.getExpression2();
			boolean isReturnedVariable = isReturnedVariable(difference);
			ITypeBinding typeBinding1 = expression1 != null ? ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression1.getExpression()).resolveTypeBinding()
					: ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression2.getExpression()).resolveTypeBinding();
			ITypeBinding typeBinding2 = expression2 != null ? ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression2.getExpression()).resolveTypeBinding()
					: ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression1.getExpression()).resolveTypeBinding();
			if(!isReturnedVariable ||
					(returnedVariables1.size() == 1 && returnedVariables2.size() == 1 && variableBelongsToParameterizedDifferences(returnedVariables1.get(0), returnedVariables2.get(0)) != null)) {
				ITypeBinding typeBinding = null;
				if(difference.containsDifferenceType(DifferenceType.SUBCLASS_TYPE_MISMATCH) || difference.containsDifferenceType(DifferenceType.METHOD_INVOCATION_NAME_MISMATCH) ||
						difference.containsDifferenceType(DifferenceType.ARGUMENT_NUMBER_MISMATCH) || differenceContainsSubDifferenceWithSubclassTypeMismatch(difference)) {
					typeBinding = PreconditionExaminer.determineType(typeBinding1, typeBinding2);
				}
				else {
					if(expression1 != null && !typeBinding1.getQualifiedName().equals("null")) {
						if(typeBinding1.getErasure().getQualifiedName().equals("java.lang.Class") && typeBinding2.getErasure().getQualifiedName().equals("java.lang.Class") &&
								(!typeBinding1.isEqualTo(typeBinding2) || !typeBinding1.getQualifiedName().equals(typeBinding2.getQualifiedName())) ) {
							typeBinding = typeBinding1.getErasure();
						}
						else {
							typeBinding = typeBinding1;
						}
					}
					else {
						typeBinding = typeBinding2;
					}
				}
				Type type = null;
				if(typeBinding.isPrimitive() && (typeBinding1.getQualifiedName().equals("null") || typeBinding2.getQualifiedName().equals("null"))) {
					type = RefactoringUtility.generateWrapperTypeForPrimitiveTypeBinding(typeBinding, ast);
				}
				else {
					type = RefactoringUtility.generateTypeFromTypeBinding(typeBinding, ast, sourceRewriter);
				}
				Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
				typeBindings.add(typeBinding);
				RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
				SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
				sourceRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, ast.newSimpleName("arg" + i), null);
				i++;
				sourceRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, type, null);
				parameterRewrite.insertLast(parameter, null);
			}
		}
		//add parameters for the fields that should be parameterized instead of being pulled up
		int j=0;
		List<VariableDeclaration> fieldDeclarationsToBeParameterizedG2 = new ArrayList<VariableDeclaration>(fieldDeclarationsToBeParameterized.get(1));
		for(VariableDeclaration variableDeclaration1 : fieldDeclarationsToBeParameterized.get(0)) {
			if(accessedLocalFieldsG1.contains(variableDeclaration1)) {
				VariableDeclaration variableDeclaration2 = fieldDeclarationsToBeParameterizedG2.get(j);
				ITypeBinding typeBinding1 = variableDeclaration1.resolveBinding().getType();
				ITypeBinding typeBinding2 = variableDeclaration2.resolveBinding().getType();
				ITypeBinding typeBinding = PreconditionExaminer.determineType(typeBinding1, typeBinding2);
				SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
				Type type = RefactoringUtility.generateTypeFromTypeBinding(typeBinding, ast, sourceRewriter);
				sourceRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, type, null);
				String identifier = (variableDeclaration1.resolveBinding().getModifiers() & Modifier.STATIC) != 0 ? variableDeclaration1.getName().getIdentifier() :
						createNameForParameterizedFieldAccess(variableDeclaration1.getName().getIdentifier());
				sourceRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, ast.newSimpleName(identifier), null);
				parameterRewrite.insertLast(parameter, null);
			}
			j++;
		}
		sourceRewriter.set(newMethodDeclaration, MethodDeclaration.BODY_PROPERTY, newMethodBody, null);
		bodyDeclarationsRewrite.insertLast(newMethodDeclaration, null);
		cloneInfo.requiredImportTypeBindings = requiredImportTypeBindings;
		cloneInfo.methodBodyRewrite = methodBodyRewrite;
		cloneInfo.parameterRewrite = parameterRewrite;
	}

	private boolean containsSuperMethodCall(TypeDeclaration typeDeclaration, IMethodBinding methodBinding) {
		SuperMethodInvocationVisitor visitor = new SuperMethodInvocationVisitor();
		typeDeclaration.accept(visitor);
		List<SuperMethodInvocation> superMethodInvocations = visitor.getSuperMethodInvocations();
		for(SuperMethodInvocation superMethodInvocation : superMethodInvocations) {
			if(methodBinding.overrides(superMethodInvocation.resolveMethodBinding())) {
				return true;
			}
		}
		return false;
	}

	private boolean commonSuperTypeDeclaresOrInheritsMethodWithIdenticalSignature(IMethodBinding methodBinding, ITypeBinding typeBinding) {
		if(typeBinding != null && !typeBinding.isInterface()) {
			for(IMethodBinding superMethodBinding : typeBinding.getDeclaredMethods()) {
				if(MethodCallAnalyzer.equalSignature(superMethodBinding, methodBinding)) {
					return true;
				}
			}
			return commonSuperTypeDeclaresOrInheritsMethodWithIdenticalSignature(methodBinding, typeBinding.getSuperclass());
		}
		return false;
	}

	private void addTypeBinding(ITypeBinding typeBinding, Set<ITypeBinding> thrownExceptionTypeBindings) {
		boolean found = false;
		for(ITypeBinding thrownExceptionTypeBinding : thrownExceptionTypeBindings) {
			if(typeBinding.isEqualTo(thrownExceptionTypeBinding)) {
				found = true;
				break;
			}
		}
		if(!found) {
			thrownExceptionTypeBindings.add(typeBinding);
		}
	}

	private Set<Expression> extractMethodInvocations(PDGNode pdgNode) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		Set<Expression> allMethodInvocations = new LinkedHashSet<Expression>();
		AbstractStatement abstractStatement = pdgNode.getStatement();
		if(abstractStatement instanceof StatementObject) {
			StatementObject statement = (StatementObject)abstractStatement;
			allMethodInvocations.addAll(expressionExtractor.getMethodInvocations(statement.getStatement()));
		}
		else if(abstractStatement instanceof CompositeStatementObject) {
			CompositeStatementObject composite = (CompositeStatementObject)abstractStatement;
			for(AbstractExpression expression : composite.getExpressions()) {
				allMethodInvocations.addAll(expressionExtractor.getMethodInvocations(expression.getExpression()));
			}
			if(composite instanceof TryStatementObject) {
				TryStatementObject tryStatement = (TryStatementObject)composite;
				List<CatchClauseObject> catchClauses = tryStatement.getCatchClauses();
				for(CatchClauseObject catchClause : catchClauses) {
					allMethodInvocations.addAll(expressionExtractor.getMethodInvocations(catchClause.getBody().getStatement()));
				}
				if(tryStatement.getFinallyClause() != null) {
					allMethodInvocations.addAll(expressionExtractor.getMethodInvocations(tryStatement.getFinallyClause().getStatement()));
				}
			}
		}
		return allMethodInvocations;
	}

	private void createDifferencesForStaticMethodCalls(IMethodBinding binding1, IMethodBinding binding2) {
		for(PDGNodeMapping nodeMapping : mapper.getMaximumStateWithMinimumDifferences().getNodeMappings()) {
			PDGNode nodeG1 = nodeMapping.getNodeG1();
			PDGNode nodeG2 = nodeMapping.getNodeG2();
			Set<Expression> allMethodInvocations1 = extractMethodInvocations(nodeG1);
			Set<Expression> allMethodInvocations2 = extractMethodInvocations(nodeG2);
			for(Expression expr1 : allMethodInvocations1) {
				if(expr1 instanceof MethodInvocation) {
					MethodInvocation methodInvocation1 = (MethodInvocation)expr1;
					IMethodBinding methodBinding1 = methodInvocation1.resolveMethodBinding();
					if(methodBinding1.isEqualTo(binding1) && (methodBinding1.getModifiers() & Modifier.STATIC) != 0 && methodBinding1.getDeclaringClass().isEqualTo(sourceTypeDeclarations.get(0).resolveBinding())) {
						for(Expression expr2 : allMethodInvocations2) {
							if(expr2 instanceof MethodInvocation) {
								MethodInvocation methodInvocation2 = (MethodInvocation)expr2;
								IMethodBinding methodBinding2 = methodInvocation2.resolveMethodBinding();
								if(methodBinding2.isEqualTo(binding2) && (methodBinding2.getModifiers() & Modifier.STATIC) != 0 && methodBinding2.getDeclaringClass().isEqualTo(sourceTypeDeclarations.get(1).resolveBinding())) {
									if(MethodCallAnalyzer.equalSignatureIgnoringSubclassTypeDifferences(methodBinding1, methodBinding2)) {
										ASTInformationGenerator.setCurrentITypeRoot(sourceCompilationUnits.get(0).getTypeRoot());
										AbstractExpression exp1 = new AbstractExpression(methodInvocation1);
										ASTInformationGenerator.setCurrentITypeRoot(sourceCompilationUnits.get(1).getTypeRoot());
										AbstractExpression exp2 = new AbstractExpression(methodInvocation2);
										if(methodBinding1.getReturnType().getQualifiedName().equals("void") || methodBinding2.getReturnType().getQualifiedName().equals("void")) {
											if(methodBinding1.getReturnType().getQualifiedName().equals("void")) {
												PreconditionViolation violation = new ExpressionPreconditionViolation(exp1, PreconditionViolationType.EXPRESSION_DIFFERENCE_IS_VOID_METHOD_CALL);
												RefactoringStatusContext context = JavaStatusContext.create(sourceCompilationUnits.get(0).getTypeRoot(), methodInvocation1);
												status.merge(RefactoringStatus.createErrorStatus(violation.getViolation(), context));
											}
											if(methodBinding2.getReturnType().getQualifiedName().equals("void")) {
												PreconditionViolation violation = new ExpressionPreconditionViolation(exp2, PreconditionViolationType.EXPRESSION_DIFFERENCE_IS_VOID_METHOD_CALL);
												RefactoringStatusContext context = JavaStatusContext.create(sourceCompilationUnits.get(1).getTypeRoot(), methodInvocation2);
												status.merge(RefactoringStatus.createErrorStatus(violation.getViolation(), context));
											}
										}
										else {
											ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
											Difference diff = new Difference(methodInvocation1.getName().getIdentifier(),methodInvocation2.getName().getIdentifier(),DifferenceType.METHOD_INVOCATION_NAME_MISMATCH);
											astNodeDifference.addDifference(diff);
											nodeMapping.getNodeDifferences().add(astNodeDifference);
										}
										break;
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean checkIfThisReferenceIsPassedAsArgumentToMethodInvocation(ITypeBinding argumentTypeBinding) {
		boolean thisReferenceIsPassedAsArgumentToMethodInvocation1 = false;
		boolean thisReferenceIsPassedAsArgumentToMethodInvocation2 = false;
		for(PDGNodeMapping nodeMapping : mapper.getMaximumStateWithMinimumDifferences().getNodeMappings()) {
			PDGNode nodeG1 = nodeMapping.getNodeG1();
			PDGNode nodeG2 = nodeMapping.getNodeG2();
			Set<Expression> allMethodInvocations1 = extractMethodInvocations(nodeG1);
			Set<Expression> allMethodInvocations2 = extractMethodInvocations(nodeG2);
			for(Expression expr1 : allMethodInvocations1) {
				if(expr1 instanceof MethodInvocation) {
					MethodInvocation methodInvocation1 = (MethodInvocation)expr1;
					IMethodBinding methodBinding1 = methodInvocation1.resolveMethodBinding();
					List<Expression> arguments = methodInvocation1.arguments();
					int position = 0;
					for(Expression argument : arguments) {
						if(argument instanceof ThisExpression) {
							ITypeBinding[] parameterTypeBindings = methodBinding1.getParameterTypes();
							ITypeBinding parameterTypeBinding = parameterTypeBindings[position];
							if(parameterTypeBinding.isEqualTo(argumentTypeBinding) && parameterTypeBinding.getQualifiedName().equals(argumentTypeBinding.getQualifiedName())) {
								thisReferenceIsPassedAsArgumentToMethodInvocation1 = true;
								break;
							}
						}
						position++;
					}
					if(thisReferenceIsPassedAsArgumentToMethodInvocation1)
						break;
				}
			}
			for(Expression expr2 : allMethodInvocations2) {
				if(expr2 instanceof MethodInvocation) {
					MethodInvocation methodInvocation2 = (MethodInvocation)expr2;
					IMethodBinding methodBinding2 = methodInvocation2.resolveMethodBinding();
					List<Expression> arguments = methodInvocation2.arguments();
					int position = 0;
					for(Expression argument : arguments) {
						if(argument instanceof ThisExpression) {
							ITypeBinding[] parameterTypeBindings = methodBinding2.getParameterTypes();
							ITypeBinding parameterTypeBinding = parameterTypeBindings[position];
							if(parameterTypeBinding.isEqualTo(argumentTypeBinding) && parameterTypeBinding.getQualifiedName().equals(argumentTypeBinding.getQualifiedName())) {
								thisReferenceIsPassedAsArgumentToMethodInvocation2 = true;
								break;
							}
						}
						position++;
					}
					if(thisReferenceIsPassedAsArgumentToMethodInvocation2)
						break;
				}
			}
			if(thisReferenceIsPassedAsArgumentToMethodInvocation1 && thisReferenceIsPassedAsArgumentToMethodInvocation2)
				break;
		}
		return thisReferenceIsPassedAsArgumentToMethodInvocation1 && thisReferenceIsPassedAsArgumentToMethodInvocation2;
	}

	private boolean noneOfTheConstructorsContainsSuperConstructorCall(TypeDeclaration typeDeclaration) {
		int constructorCounter = 0;
		int constructorWithoutSuperCallCounter = 0;
		for(MethodDeclaration methodDeclaration : typeDeclaration.getMethods()) {
			if(methodDeclaration.isConstructor()) {
				SuperConstructorInvocation superConstructorInvocation = firstStatementIsSuperConstructorInvocation(methodDeclaration);
				if(superConstructorInvocation == null) {
					constructorWithoutSuperCallCounter++;
				}
				constructorCounter++;
			}
		}
		return constructorCounter > 0 && constructorCounter == constructorWithoutSuperCallCounter;
	}

	private boolean containsConstructorCallingSuperConstructorWithoutArguments(TypeDeclaration typeDeclaration) {
		for(MethodDeclaration methodDeclaration : typeDeclaration.getMethods()) {
			if(methodDeclaration.isConstructor()) {
				SuperConstructorInvocation superConstructorInvocation = firstStatementIsSuperConstructorInvocation(methodDeclaration);
				if(superConstructorInvocation != null && superConstructorInvocation.arguments().size() == 0) {
					return true;
				}
			}
		}
		return false;
	}

	private MethodDeclaration createDefaultConstructor(AST intermediateAST, ASTRewrite intermediateRewriter, SimpleName intermediateName) {
		MethodDeclaration constructor = intermediateAST.newMethodDeclaration();
		intermediateRewriter.set(constructor, MethodDeclaration.NAME_PROPERTY, intermediateName, null);
		intermediateRewriter.set(constructor, MethodDeclaration.CONSTRUCTOR_PROPERTY, true, null);
		ListRewrite constructorModifierRewriter = intermediateRewriter.getListRewrite(constructor, MethodDeclaration.MODIFIERS2_PROPERTY);
		constructorModifierRewriter.insertLast(intermediateAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
		Block constructorBody = intermediateAST.newBlock();
		ListRewrite constructorBodyRewriter = intermediateRewriter.getListRewrite(constructorBody, Block.STATEMENTS_PROPERTY);
		constructorBodyRewriter.insertLast(intermediateAST.newSuperConstructorInvocation(), null);
		intermediateRewriter.set(constructor, MethodDeclaration.BODY_PROPERTY, constructorBody, null);
		return constructor;
	}

	private MethodDeclaration copyConstructor(MethodDeclaration constructorDeclaration, AST intermediateAST, ASTRewrite intermediateRewriter, SimpleName intermediateName,
			Set<ITypeBinding> requiredImportTypeBindings) {
		List<SingleVariableDeclaration> parameters = constructorDeclaration.parameters();
		SuperConstructorInvocation superConstructorInvocation = firstStatementIsSuperConstructorInvocation(constructorDeclaration);
		MethodDeclaration constructor = intermediateAST.newMethodDeclaration();
		intermediateRewriter.set(constructor, MethodDeclaration.NAME_PROPERTY, intermediateName, null);
		intermediateRewriter.set(constructor, MethodDeclaration.CONSTRUCTOR_PROPERTY, true, null);
		ListRewrite constructorModifierRewriter = intermediateRewriter.getListRewrite(constructor, MethodDeclaration.MODIFIERS2_PROPERTY);
		List<IExtendedModifier> modifiers = constructorDeclaration.modifiers();
		for(IExtendedModifier modifier : modifiers) {
			if(modifier instanceof Modifier) {
				constructorModifierRewriter.insertLast((Modifier)modifier, null);
			}
		}
		ListRewrite parameterRewriter = intermediateRewriter.getListRewrite(constructor, MethodDeclaration.PARAMETERS_PROPERTY);
		List<Expression> superConstructorArguments = superConstructorInvocation.arguments();
		Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
		for(Expression argument : superConstructorArguments) {
			if(argument instanceof SimpleName) {
				SimpleName simpleName = (SimpleName)argument;	
				for(SingleVariableDeclaration parameter : parameters) {
					if(parameter.resolveBinding().isEqualTo(simpleName.resolveBinding())) {
						typeBindings.add(parameter.getType().resolveBinding());
						parameterRewriter.insertLast(parameter, null);
						break;
					}
				}
			}
		}
		RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
		Block constructorBody = intermediateAST.newBlock();
		if(superConstructorInvocation != null) {
			ListRewrite constructorBodyRewriter = intermediateRewriter.getListRewrite(constructorBody, Block.STATEMENTS_PROPERTY);
			constructorBodyRewriter.insertLast(superConstructorInvocation, null);
		}
		intermediateRewriter.set(constructor, MethodDeclaration.BODY_PROPERTY, constructorBody, null);
		return constructor;
	}

	private void updateAccessModifier(BodyDeclaration bodyDeclaration, Modifier.ModifierKeyword modifierKeyword) {
		ChildListPropertyDescriptor modifiersChildListPropertyDescriptor = null;
		if(bodyDeclaration instanceof MethodDeclaration) {
			modifiersChildListPropertyDescriptor = MethodDeclaration.MODIFIERS2_PROPERTY;
		}
		else if(bodyDeclaration instanceof FieldDeclaration) {
			modifiersChildListPropertyDescriptor = FieldDeclaration.MODIFIERS2_PROPERTY;
		}
		AST ast = bodyDeclaration.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		CompilationUnit compilationUnit = RefactoringUtility.findCompilationUnit(bodyDeclaration);
		ListRewrite modifiersRewrite = rewriter.getListRewrite(bodyDeclaration, modifiersChildListPropertyDescriptor);
		List<IExtendedModifier> originalModifiers = bodyDeclaration.modifiers();
		boolean accessModifierFound = false;
		for(IExtendedModifier extendedModifier : originalModifiers) {
			if(extendedModifier.isModifier()) {
				Modifier modifier = (Modifier)extendedModifier;
				if(modifier.isProtected() || modifier.isPrivate() || modifier.isPublic()) {
					accessModifierFound = true;
					if(modifier.getKeyword().toFlagValue() != modifierKeyword.toFlagValue()) {
						modifiersRewrite.replace(modifier, ast.newModifier(modifierKeyword), null);
					}
				}
			}
		}
		if(!accessModifierFound) {
			modifiersRewrite.insertFirst(ast.newModifier(modifierKeyword), null);
		}
		try {
			TextEdit sourceEdit = rewriter.rewriteAST();
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)compilationUnit.getJavaElement();
			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
			if(change == null) {
				MultiTextEdit sourceMultiTextEdit = new MultiTextEdit();
				change = new CompilationUnitChange("", sourceICompilationUnit);
				change.setEdit(sourceMultiTextEdit);
				compilationUnitChanges.put(sourceICompilationUnit, change);
			}
			change.getEdit().addChild(sourceEdit);
			change.addTextEditGroup(new TextEditGroup("Update access modifier", new TextEdit[] {sourceEdit}));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private SuperConstructorInvocation firstStatementIsSuperConstructorInvocation(MethodDeclaration methodDeclaration) {
		Block body = methodDeclaration.getBody();
		if(methodDeclaration.isConstructor() && body != null) {
			List<Statement> statements = body.statements();
			if(statements.size() > 0) {
				Statement firstStatement = statements.get(0);
				if(firstStatement instanceof SuperConstructorInvocation) {
					return (SuperConstructorInvocation)firstStatement;
				}
			}
		}
		return null;
	}
	
	private boolean matchingArgumentTypes(List<Expression> arguments1, List<Expression> arguments2) {
		if(arguments1.size() != arguments2.size()) {
			return false;
		}
		else {
			for(int i=0; i<arguments1.size(); i++) {
				Expression argument1 = arguments1.get(i);
				Expression argument2 = arguments2.get(i);
				if(!argument1.resolveTypeBinding().isEqualTo(argument2.resolveTypeBinding())) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean matchingParameterTypesWithArgumentTypes(List<SingleVariableDeclaration> parameters1, List<Expression> arguments2) {
		if(parameters1.size() != arguments2.size()) {
			return false;
		}
		else {
			for(int i=0; i<parameters1.size(); i++) {
				SingleVariableDeclaration parameter1 = parameters1.get(i);
				Expression argument2 = arguments2.get(i);
				if(!parameter1.getType().resolveBinding().isEqualTo(argument2.resolveTypeBinding())) {
					return false;
				}
			}
		}
		return true;
	}
	
	private boolean type2Clones(MethodDeclaration methodDeclaration1, MethodDeclaration methodDeclaration2) {
		if(methodDeclaration1.getBody() != null && methodDeclaration2.getBody() != null) {
			StatementCollector collector1 = new StatementCollector();
			methodDeclaration1.getBody().accept(collector1);
			List<ASTNode> statements1 = collector1.getStatementList();
			StatementCollector collector2 = new StatementCollector();
			methodDeclaration2.getBody().accept(collector2);
			List<ASTNode> statements2 = collector2.getStatementList();
			ITypeRoot typeRoot1 = RefactoringUtility.findCompilationUnit(methodDeclaration1).getTypeRoot();
			ITypeRoot typeRoot2 = RefactoringUtility.findCompilationUnit(methodDeclaration2).getTypeRoot();
			if(statements1.size() != statements2.size()) {
				return false;
			}
			else {
				for(int i=0; i<statements1.size(); i++) {
					ASTNode node1 = statements1.get(i);
					ASTNode node2 = statements2.get(i);
					boolean match = compareStatements(typeRoot1, typeRoot2, node1, node2);
					if(!match)
						return false;
				}
				return true;
			}
		}
		else if(methodDeclaration1.getBody() != null && methodDeclaration2.getBody() == null) {
			return false;
		}
		else if(methodDeclaration1.getBody() == null && methodDeclaration2.getBody() != null) {
			return false;
		}
		else {
			//both methods are abstract
			return true;
		}
	}

	private boolean compareStatements(ITypeRoot typeRoot1, ITypeRoot typeRoot2, ASTNode node1, ASTNode node2) {
		boolean exactClones = false;
		ASTNodeMatcher astMatcher = new ASTNodeMatcher(typeRoot1, typeRoot2);
		boolean match = node1.subtreeMatch(astMatcher, node2);
		if(match) {
			List<ASTNodeDifference> differences = astMatcher.getDifferences();
			boolean onlyLocalVariableNameMismatches = true; 
			for(ASTNodeDifference difference : differences) {
				if(!difference.containsOnlyDifferenceType(DifferenceType.VARIABLE_NAME_MISMATCH)) {
					onlyLocalVariableNameMismatches = false;
					break;
				}
				else {
					Expression expr1 = difference.getExpression1().getExpression();
					Expression expr2 = difference.getExpression2().getExpression();
					if(isField(expr1) || isField(expr2)) {
						onlyLocalVariableNameMismatches = false;
						break;
					}
				}
			}
			exactClones = onlyLocalVariableNameMismatches;
		}
		return exactClones;
	}
	
	private boolean isField(Expression expr) {
		boolean expressionIsField = false;
		if(expr instanceof SimpleName) {
			SimpleName simpleName = (SimpleName)expr;
			if(simpleName.resolveBinding().getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)simpleName.resolveBinding();
				expressionIsField = variableBinding.isField();
			}
		}
		else if(expr instanceof FieldAccess) {
			FieldAccess fieldAccess = (FieldAccess)expr;
			SimpleName simpleName = fieldAccess.getName();
			if(simpleName.resolveBinding().getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)simpleName.resolveBinding();
				expressionIsField = variableBinding.isField();
			}
		}
		return expressionIsField;
	}

	private Set<ITypeBinding> extractTypeBindings(AbstractStatement abstractStatement) {
		Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
		if(abstractStatement instanceof StatementObject) {
			StatementObject statement = (StatementObject)abstractStatement;
			TypeVisitor typeVisitor = new TypeVisitor();
			statement.getStatement().accept(typeVisitor);
			typeBindings.addAll(typeVisitor.getTypeBindings());
		}
		else if(abstractStatement instanceof CompositeStatementObject) {
			CompositeStatementObject composite = (CompositeStatementObject)abstractStatement;
			for(AbstractExpression expression : composite.getExpressions()) {
				TypeVisitor typeVisitor = new TypeVisitor();
				expression.getExpression().accept(typeVisitor);
				typeBindings.addAll(typeVisitor.getTypeBindings());
			}
			if(composite instanceof TryStatementObject) {
				TryStatementObject tryStatement = (TryStatementObject)composite;
				List<CatchClauseObject> catchClauses = tryStatement.getCatchClauses();
				for(CatchClauseObject catchClause : catchClauses) {
					TypeVisitor typeVisitor = new TypeVisitor();
					for(AbstractExpression expression : catchClause.getExpressions()) {
						Expression expr = expression.getExpression();
						if(expr instanceof SimpleName)
							expr.getParent().accept(typeVisitor);
						else
							expr.accept(typeVisitor);
					}
					catchClause.getBody().getStatement().accept(typeVisitor);
					typeBindings.addAll(typeVisitor.getTypeBindings());
				}
				if(tryStatement.getFinallyClause() != null) {
					TypeVisitor typeVisitor = new TypeVisitor();
					tryStatement.getFinallyClause().getStatement().accept(typeVisitor);
					typeBindings.addAll(typeVisitor.getTypeBindings());
				}
			}
		}
		return typeBindings;
	}

	private boolean superclassDirectlyInheritedFromRefactoredSubclasses(ITypeBinding commonSuperTypeOfSourceTypeDeclarations,
			ITypeBinding typeBinding1, ITypeBinding typeBinding2) {
		return typeBinding1.getSuperclass().isEqualTo(commonSuperTypeOfSourceTypeDeclarations) &&
				typeBinding2.getSuperclass().isEqualTo(commonSuperTypeOfSourceTypeDeclarations);
	}

	private Set<VariableDeclaration> getFieldsAccessedInMethod(Set<VariableDeclaration> indirectlyAccessedLocalFields,
			MethodDeclaration methodDeclaration) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> simpleNames = expressionExtractor.getVariableInstructions(methodDeclaration.getBody());
		Set<VariableDeclaration> fieldsAccessedInMethod = new LinkedHashSet<VariableDeclaration>();
		for(Expression expression : simpleNames) {
			SimpleName simpleName = (SimpleName)expression;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null && binding.getKind() == IBinding.VARIABLE) {
				for(VariableDeclaration variableDeclaration : indirectlyAccessedLocalFields) {
					if(variableDeclaration.resolveBinding().isEqualTo(binding)) {
						fieldsAccessedInMethod.add(variableDeclaration);
					}
				}
			}
		}
		return fieldsAccessedInMethod;
	}

	private Set<VariableDeclaration> additionalFieldUsedAsInitializers(Set<VariableDeclaration> allFields, TypeDeclaration typeDeclaration) {
		Set<VariableDeclaration> additionalFieldUsedAsInitializers = new LinkedHashSet<VariableDeclaration>();
		for(VariableDeclaration field : allFields) {
			Expression initializer = field.getInitializer();
			if(initializer != null && initializer instanceof SimpleName) {
				SimpleName simpleName = (SimpleName)initializer;
				IBinding binding = simpleName.resolveBinding();
				if(binding.getKind() == IBinding.VARIABLE) {
					IVariableBinding variableBinding = (IVariableBinding)binding;
					if(variableBinding.isField()) {
						VariableDeclaration initializerField = RefactoringUtility.findFieldDeclaration(new PlainVariable(variableBinding), typeDeclaration);
						if(initializerField != null) {
							additionalFieldUsedAsInitializers.add(initializerField);
						}
					}
				}
			}
		}
		return additionalFieldUsedAsInitializers;
	}

	private void pullUpLocallyAccessedFields(Set<VariableDeclaration> accessedLocalFieldsG1, Set<VariableDeclaration> accessedLocalFieldsG2,
			Set<VariableDeclaration> modifiedLocalFieldsG1, Set<VariableDeclaration> modifiedLocalFieldsG2,
			ListRewrite bodyDeclarationsRewrite, Set<ITypeBinding> requiredImportTypeBindings) {
		Set<VariableDeclaration> allFieldsG1 = new LinkedHashSet<VariableDeclaration>(accessedLocalFieldsG1);
		Set<VariableDeclaration> allFieldsG2 = new LinkedHashSet<VariableDeclaration>(accessedLocalFieldsG2);
		allFieldsG1.addAll(modifiedLocalFieldsG1);
		allFieldsG2.addAll(modifiedLocalFieldsG2);
		allFieldsG1.addAll(additionalFieldUsedAsInitializers(allFieldsG1, sourceTypeDeclarations.get(0)));
		allFieldsG2.addAll(additionalFieldUsedAsInitializers(allFieldsG2, sourceTypeDeclarations.get(1)));
		ASTRewrite sourceRewriter = cloneInfo.sourceRewriter;
		AST ast = cloneInfo.ast;
		TypeDeclaration sourceTypeDeclaration = cloneInfo.sourceTypeDeclaration;
		for(VariableDeclaration localFieldG1 : allFieldsG1) {
			FieldDeclaration originalFieldDeclarationG1 = (FieldDeclaration)localFieldG1.getParent();
			for(VariableDeclaration localFieldG2 : allFieldsG2) {
				FieldDeclaration originalFieldDeclarationG2 = (FieldDeclaration)localFieldG2.getParent();
				if(localFieldG1.getName().getIdentifier().equals(localFieldG2.getName().getIdentifier()) &&
						/*localFieldG1.getRoot().equals(sourceCompilationUnits.get(0)) && localFieldG2.getRoot().equals(sourceCompilationUnits.get(1)) &&*/
						!fieldDeclarationsToBePulledUp.get(0).contains(localFieldG1) && !fieldDeclarationsToBePulledUp.get(1).contains(localFieldG2)) {
					//ITypeBinding commonSuperType = commonSuperType(originalFieldDeclarationG1.getType().resolveBinding(), originalFieldDeclarationG2.getType().resolveBinding());
					if(originalFieldDeclarationG1.getType().resolveBinding().isEqualTo(originalFieldDeclarationG2.getType().resolveBinding())) {
						/*String innerTypeName = null;
						if(!originalFieldDeclarationG1.getType().resolveBinding().isEqualTo(originalFieldDeclarationG2.getType().resolveBinding())) {
							//check if the types of the fields are inner types
							TypeDeclaration innerType1 = null;
							TypeDeclaration innerType2 = null;
							for(TypeDeclaration innerType : sourceTypeDeclarations.get(0).getTypes()) {
								if(innerType.resolveBinding().isEqualTo(originalFieldDeclarationG1.getType().resolveBinding())) {
									innerType1 = innerType;
									break;
								}
							}
							for(TypeDeclaration innerType : sourceTypeDeclarations.get(1).getTypes()) {
								if(innerType.resolveBinding().isEqualTo(originalFieldDeclarationG2.getType().resolveBinding())) {
									innerType2 = innerType;
									break;
								}
							}
							if(innerType1 != null && innerType2 != null) {
								MethodDeclaration[] methodDeclarations1 = innerType1.getMethods();
								MethodDeclaration[] methodDeclarations2 = innerType2.getMethods();
								List<MethodDeclaration> methods1 = new ArrayList<MethodDeclaration>();
								for(MethodDeclaration methodDeclaration1 : methodDeclarations1) {
									if(!methodDeclaration1.isConstructor()) {
										methods1.add(methodDeclaration1);
									}
								}
								List<MethodDeclaration> methods2 = new ArrayList<MethodDeclaration>();
								for(MethodDeclaration methodDeclaration2 : methodDeclarations2) {
									if(!methodDeclaration2.isConstructor()) {
										methods2.add(methodDeclaration2);
									}
								}
								int numberOfMethods1 = methods1.size();
								int numberOfMethods2 = methods2.size();
								int equalSignatureCount = 0;
								for(MethodDeclaration method1 : methods1) {
									for(MethodDeclaration method2 : methods2) {
										if(MethodCallAnalyzer.equalSignature(method1.resolveBinding(), method2.resolveBinding())) {
											equalSignatureCount++;
											break;
										}
									}
								}
								if(numberOfMethods1 == equalSignatureCount && numberOfMethods2 == equalSignatureCount) {
									TypeDeclaration innerTypeDeclaration = ast.newTypeDeclaration();
									innerTypeName = "Intermediate" + innerType1.getName().getIdentifier();
									sourceRewriter.set(innerTypeDeclaration, TypeDeclaration.NAME_PROPERTY, ast.newSimpleName(innerTypeName), null);
									ListRewrite innerTypeModifiersRewrite = sourceRewriter.getListRewrite(innerTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
									innerTypeModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
									innerTypeModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
									sourceRewriter.set(innerTypeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, innerType1.getSuperclassType(), null);
									Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
									typeBindings.add(innerType1.getSuperclassType().resolveBinding());
									RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
									
									ListRewrite innerTypeBodyRewrite = sourceRewriter.getListRewrite(innerTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
									for(MethodDeclaration method1 : methods1) {
										MethodDeclaration innerTypeMethodDeclaration = ast.newMethodDeclaration();
										sourceRewriter.set(innerTypeMethodDeclaration, MethodDeclaration.NAME_PROPERTY, method1.getName(), null);
										sourceRewriter.set(innerTypeMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, method1.getReturnType2(), null);
										
										List<SingleVariableDeclaration> parameters = method1.parameters();
										ListRewrite parametersRewrite = sourceRewriter.getListRewrite(innerTypeMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
										for(SingleVariableDeclaration parameter : parameters) {
											parametersRewrite.insertLast(parameter, null);
										}
										List<IExtendedModifier> modifiers = method1.modifiers();
										ListRewrite modifiersRewrite = sourceRewriter.getListRewrite(innerTypeMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
										for(IExtendedModifier modifier : modifiers) {
											if(modifier instanceof Modifier)
												modifiersRewrite.insertLast((Modifier)modifier, null);
										}
										modifiersRewrite.insertLast(ast.newModifier(ModifierKeyword.ABSTRACT_KEYWORD), null);
										innerTypeBodyRewrite.insertLast(innerTypeMethodDeclaration, null);
									}
									
									ListRewrite bodyRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
									bodyRewrite.insertLast(innerTypeDeclaration, null);
									
									//change the superclass of the original inner types to the new inner type
									String qualifiedInnerType = intermediateClassName != null ?
											intermediateClassName + "." + innerTypeName :
											commonSuperTypeOfSourceTypeDeclarations.getName() + "." + innerTypeName;
									modifySuperclassType(sourceCompilationUnits.get(0), innerType1, qualifiedInnerType);
									modifySuperclassType(sourceCompilationUnits.get(1), innerType2, qualifiedInnerType);
								}
							}
						}*/
						boolean avoidPullUpDueToSerialization1 = RefactoringUtility.isSerializedField(sourceTypeDeclarations.get(0), localFieldG1);
						boolean avoidPullUpDueToSerialization2 = RefactoringUtility.isSerializedField(sourceTypeDeclarations.get(1), localFieldG2);
						if(!avoidPullUpDueToSerialization1 && !avoidPullUpDueToSerialization2 && sameInitializers(localFieldG1, localFieldG2)) {
							//check if the common superclass is one of the source classes
							if(!sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding()) && !bothFieldsDeclaredInCommonSuperclass(localFieldG1, localFieldG2)) {
								fieldDeclarationsToBePulledUp.get(0).add(localFieldG1);
							}
							if(!sourceTypeDeclarations.get(1).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding()) && !bothFieldsDeclaredInCommonSuperclass(localFieldG1, localFieldG2)) {
								fieldDeclarationsToBePulledUp.get(1).add(localFieldG2);
							}
							if(!sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding()) &&
									!sourceTypeDeclarations.get(1).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
								Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
								typeBindings.add(localFieldG1.resolveBinding().getType());
								VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
								sourceRewriter.set(fragment, VariableDeclarationFragment.NAME_PROPERTY, ast.newSimpleName(localFieldG1.getName().getIdentifier()), null);
								if(localFieldG1.getInitializer() != null && localFieldG2.getInitializer() != null) {
									Expression initializer1 = localFieldG1.getInitializer();
									Expression initializer2 = localFieldG2.getInitializer();
									if(initializer1.subtreeMatch(new ASTMatcher(), initializer2)) {
										sourceRewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, ASTNode.copySubtree(ast, initializer1), null);
										TypeVisitor typeVisitor = new TypeVisitor();
										initializer1.accept(typeVisitor);
										typeBindings.addAll(typeVisitor.getTypeBindings());
									}
								}
								RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
								FieldDeclaration newFieldDeclaration = ast.newFieldDeclaration(fragment);
								sourceRewriter.set(newFieldDeclaration, FieldDeclaration.TYPE_PROPERTY, originalFieldDeclarationG1.getType(), null);
								/*if(originalFieldDeclarationG1.getType().resolveBinding().isEqualTo(originalFieldDeclarationG2.getType().resolveBinding())) {
									sourceRewriter.set(newFieldDeclaration, FieldDeclaration.TYPE_PROPERTY, originalFieldDeclarationG1.getType(), null);
								}
								else if(innerTypeName != null) {
									Name typeName = ast.newName(innerTypeName);
									sourceRewriter.set(newFieldDeclaration, FieldDeclaration.TYPE_PROPERTY, ast.newSimpleType(typeName), null);
								}
								else {
									Name typeName = ast.newName(commonSuperType.getQualifiedName());
									sourceRewriter.set(newFieldDeclaration, FieldDeclaration.TYPE_PROPERTY, ast.newSimpleType(typeName), null);
								}*/
								if(originalFieldDeclarationG1.getJavadoc() != null) {
									sourceRewriter.set(newFieldDeclaration, FieldDeclaration.JAVADOC_PROPERTY, originalFieldDeclarationG1.getJavadoc(), null);
								}
								ListRewrite newFieldDeclarationModifiersRewrite = sourceRewriter.getListRewrite(newFieldDeclaration, FieldDeclaration.MODIFIERS2_PROPERTY);
								newFieldDeclarationModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD), null);
								List<IExtendedModifier> originalModifiers = originalFieldDeclarationG1.modifiers();
								for(IExtendedModifier extendedModifier : originalModifiers) {
									if(extendedModifier.isModifier()) {
										Modifier modifier = (Modifier)extendedModifier;
										if(modifier.isFinal()) {
											newFieldDeclarationModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
										}
										else if(modifier.isStatic()) {
											newFieldDeclarationModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD), null);
										}
										else if(modifier.isTransient()) {
											newFieldDeclarationModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.TRANSIENT_KEYWORD), null);
										}
										else if(modifier.isVolatile()) {
											newFieldDeclarationModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.VOLATILE_KEYWORD), null);
										}
									}
								}
								bodyDeclarationsRewrite.insertLast(newFieldDeclaration, null);
							}
							else if(sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding()) && !bothFieldsDeclaredInCommonSuperclass(localFieldG1, localFieldG2)) {
								if((originalFieldDeclarationG1.getModifiers() & Modifier.PROTECTED) == 0 && (originalFieldDeclarationG1.getModifiers() & Modifier.PUBLIC) == 0) {
									updateAccessModifier(originalFieldDeclarationG1, Modifier.ModifierKeyword.PROTECTED_KEYWORD);
								}
							}
							else if(sourceTypeDeclarations.get(1).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding()) && !bothFieldsDeclaredInCommonSuperclass(localFieldG1, localFieldG2)) {
								if((originalFieldDeclarationG2.getModifiers() & Modifier.PROTECTED) == 0 && (originalFieldDeclarationG2.getModifiers() & Modifier.PUBLIC) == 0) {
									updateAccessModifier(originalFieldDeclarationG2, Modifier.ModifierKeyword.PROTECTED_KEYWORD);
								}
							}
						}
						else {
							//if the fields are both modified and used, then they should be replaced with getter calls, instead of being parameterized
							if(accessedLocalFieldsG1.contains(localFieldG1) && accessedLocalFieldsG2.contains(localFieldG2) && modifiedLocalFieldsG1.contains(localFieldG1) && modifiedLocalFieldsG2.contains(localFieldG2)) {
								if(!sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
									accessedFieldDeclarationsToBeReplacedWithGetter.get(0).add(localFieldG1);
								}
								if(!sourceTypeDeclarations.get(1).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
									accessedFieldDeclarationsToBeReplacedWithGetter.get(1).add(localFieldG2);
								}
								if(!sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding()) &&
										!sourceTypeDeclarations.get(1).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
									//create abstract getter methods in the common superclass for the accessed fields that need to be serialized
									Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
									typeBindings.add(localFieldG1.resolveBinding().getType());
									RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
									MethodDeclaration getterMethodDeclaration = ast.newMethodDeclaration();
									
									sourceRewriter.set(getterMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, originalFieldDeclarationG1.getType(), null);
									String originalFieldName = localFieldG1.getName().getIdentifier();
									String accessedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
									String getterMethodName = GETTER_PREFIX + accessedFieldName;
									getterMethodName = appendAccessorMethodSuffix(getterMethodName);
									SimpleName getterMethodSimpleName = ast.newSimpleName(getterMethodName);
									sourceRewriter.set(getterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, getterMethodSimpleName, null);
									
									ListRewrite getterMethodDeclarationModifiersRewrite = sourceRewriter.getListRewrite(getterMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
									getterMethodDeclarationModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
									if(cloneInfo.superclassNotDirectlyInheritedFromRefactoredSubclasses) {
										Block methodBody = ast.newBlock();
										ReturnStatement returnStatement = ast.newReturnStatement();
										sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, generateDefaultValue(sourceRewriter, ast, originalFieldDeclarationG1.getType().resolveBinding()), null);
										ListRewrite methodBodyStatementRewrite = sourceRewriter.getListRewrite(methodBody, Block.STATEMENTS_PROPERTY);
										methodBodyStatementRewrite.insertLast(returnStatement, null);
										sourceRewriter.set(getterMethodDeclaration, MethodDeclaration.BODY_PROPERTY, methodBody, null);
									}
									else {
										getterMethodDeclarationModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
									}
									
									bodyDeclarationsRewrite.insertLast(getterMethodDeclaration, null);
								}
							}
							else {
								if(!bothFieldsDeclaredInCommonSuperclass(localFieldG1, localFieldG2) && !modifiedLocalFieldsG1.contains(localFieldG1) && !modifiedLocalFieldsG2.contains(localFieldG2)) {
									fieldDeclarationsToBeParameterized.get(0).add(localFieldG1);
									fieldDeclarationsToBeParameterized.get(1).add(localFieldG2);
									Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
									typeBindings.add(localFieldG1.resolveBinding().getType());
									RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
								}
							}
						}
						if((avoidPullUpDueToSerialization1 || avoidPullUpDueToSerialization2 || !sameInitializers(localFieldG1, localFieldG2)) && modifiedLocalFieldsG1.contains(localFieldG1) && modifiedLocalFieldsG2.contains(localFieldG2)) {
							//check if the common superclass is one of the source classes
							if(!sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
								assignedFieldDeclarationsToBeReplacedWithSetter.get(0).add(localFieldG1);
							}
							if(!sourceTypeDeclarations.get(1).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
								assignedFieldDeclarationsToBeReplacedWithSetter.get(1).add(localFieldG2);
							}
							if(!sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding()) &&
									!sourceTypeDeclarations.get(1).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
								//create abstract setter methods in the common superclass for the modified fields that need to be serialized
								Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
								typeBindings.add(localFieldG1.resolveBinding().getType());
								RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
								MethodDeclaration setterMethodDeclaration = ast.newMethodDeclaration();
								
								PrimitiveType returnType = ast.newPrimitiveType(PrimitiveType.VOID);
								sourceRewriter.set(setterMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnType, null);
								String originalFieldName = localFieldG1.getName().getIdentifier();
								String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
								String setterMethodName = SETTER_PREFIX + modifiedFieldName;
								setterMethodName = appendAccessorMethodSuffix(setterMethodName);
								SimpleName setterMethodSimpleName = ast.newSimpleName(setterMethodName);
								sourceRewriter.set(setterMethodDeclaration, MethodDeclaration.NAME_PROPERTY, setterMethodSimpleName, null);
								
								ListRewrite setterMethodDeclarationModifiersRewrite = sourceRewriter.getListRewrite(setterMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
								setterMethodDeclarationModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
								if(cloneInfo.superclassNotDirectlyInheritedFromRefactoredSubclasses) {
									Block methodBody = ast.newBlock();
									sourceRewriter.set(setterMethodDeclaration, MethodDeclaration.BODY_PROPERTY, methodBody, null);
								}
								else {
									setterMethodDeclarationModifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
								}
								ListRewrite setterMethodDeclarationParametersRewrite = sourceRewriter.getListRewrite(setterMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
								SingleVariableDeclaration setterMethodParameter = ast.newSingleVariableDeclaration();
								sourceRewriter.set(setterMethodParameter, SingleVariableDeclaration.TYPE_PROPERTY, originalFieldDeclarationG1.getType(), null);
								sourceRewriter.set(setterMethodParameter, SingleVariableDeclaration.NAME_PROPERTY, ast.newSimpleName(localFieldG1.getName().getIdentifier()), null);
								setterMethodDeclarationParametersRewrite.insertLast(setterMethodParameter, null);
								
								bodyDeclarationsRewrite.insertLast(setterMethodDeclaration, null);
							}
						}
						break;
					}
					else {
						ITypeBinding commonSuperType = ASTNodeMatcher.commonSuperType(originalFieldDeclarationG1.getType().resolveBinding(), originalFieldDeclarationG2.getType().resolveBinding());
						if(ASTNodeMatcher.validCommonSuperType(commonSuperType)) {
							fieldDeclarationsToBeParameterized.get(0).add(localFieldG1);
							fieldDeclarationsToBeParameterized.get(1).add(localFieldG2);
							Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
							typeBindings.add(commonSuperType);
							RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
						}
					}
				}
			}
		}
	}

	private boolean bothFieldsDeclaredInCommonSuperclass(VariableDeclaration localFieldG1, VariableDeclaration localFieldG2) {
		if(sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(cloneInfo.sourceTypeDeclaration.resolveBinding())) {
			return localFieldG1.getRoot().equals(sourceCompilationUnits.get(0)) && localFieldG2.getRoot().equals(sourceCompilationUnits.get(0));
		}
		if(sourceTypeDeclarations.get(1).resolveBinding().isEqualTo(cloneInfo.sourceTypeDeclaration.resolveBinding())) {
			return localFieldG1.getRoot().equals(sourceCompilationUnits.get(1)) && localFieldG2.getRoot().equals(sourceCompilationUnits.get(1));
		}
		return false;
	}

	private boolean sameInitializers(VariableDeclaration localFieldG1, VariableDeclaration localFieldG2) {
		return (localFieldG1.getInitializer() == null && localFieldG2.getInitializer() == null) || 
				(localFieldG1.getInitializer() != null && localFieldG2.getInitializer() != null &&
				localFieldG1.getInitializer().subtreeMatch(new ASTMatcher(), localFieldG2.getInitializer()));
	}
	
	private boolean avoidPullUpMethodDueToSerialization(TypeDeclaration typeDeclaration, Set<VariableDeclaration> fieldsAccessedInMethod, Set<VariableDeclaration> fieldModifiedInMethod) {
		Set<VariableDeclaration> allFields = new LinkedHashSet<VariableDeclaration>(fieldsAccessedInMethod);
		allFields.addAll(fieldModifiedInMethod);
		for(VariableDeclaration localField : allFields) {
			if(RefactoringUtility.isSerializedField(typeDeclaration, localField)) {
				return true;
			}
		}
		return false;
	}
	/*private boolean variableIsUsedByExtractedStatement(CloneStructureNode node, PlainVariable variable) {
		PDGNodeMapping nodeMapping = (PDGNodeMapping) node.getMapping();
		List<ASTNodeDifference> differences = nodeMapping.getNodeDifferences();
		PDGNode node1 = nodeMapping.getNodeG1();
		if(!nodesToBePreservedInTheOriginalMethod.get(0).contains(node1)) {
			if(node1.usesLocalVariable(variable)) {
				boolean variableFoundInDifferences = false;
				for(ASTNodeDifference difference : differences) {
					BindingSignaturePair pair = difference.getBindingSignaturePair();
					BindingSignature signature = pair.getSignature1();
					if(signature.containsBinding(variable.getVariableBindingKey())) {
						variableFoundInDifferences = true;
					}
				}
				if(!variableFoundInDifferences) {
					return true;
				}
			}
		}
		for(CloneStructureNode child : node.getChildren()) {
			if(child.getMapping() instanceof PDGNodeMapping) {
				boolean variableIsUsedByChild = variableIsUsedByExtractedStatement(child, variable);
				if(variableIsUsedByChild) {
					return true;
				}
			}
		}
		return false;
	}

	private Map<VariableBindingKeyPair, ArrayList<VariableDeclaration>> getDeclaredLocalVariables(
			LinkedHashMap<VariableBindingKeyPair, ArrayList<PlainVariable>> declaredLocalVariables) {
		Map<VariableBindingKeyPair, ArrayList<VariableDeclaration>> declaredVariables = new LinkedHashMap<VariableBindingKeyPair, ArrayList<VariableDeclaration>>();
		Set<VariableDeclaration> variableDeclarationsAndAccessedFieldsInMethod1 = mapper.getPDG1().getVariableDeclarationsAndAccessedFieldsInMethod();
		Set<VariableDeclaration> variableDeclarationsAndAccessedFieldsInMethod2 = mapper.getPDG2().getVariableDeclarationsAndAccessedFieldsInMethod();
		for(VariableBindingKeyPair key : declaredLocalVariables.keySet()) {
			ArrayList<PlainVariable> value = declaredLocalVariables.get(key);
			PlainVariable variableDeclaration1 = value.get(0);
			PlainVariable variableDeclaration2 = value.get(1);
			ArrayList<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
			for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFieldsInMethod1) {
				if(variableDeclaration.resolveBinding().getKey().equals(variableDeclaration1.getVariableBindingKey())) {
					variableDeclarations.add(variableDeclaration);
					break;
				}
			}
			for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFieldsInMethod2) {
				if(variableDeclaration.resolveBinding().getKey().equals(variableDeclaration2.getVariableBindingKey())) {
					variableDeclarations.add(variableDeclaration);
					break;
				}
			}
			declaredVariables.put(key, variableDeclarations);
		}
		return declaredVariables;
	}*/

	private void finalizeCloneExtraction() {
		/*TreeSet<PDGNode> nodesToBePreservedInTheOriginalMethod1 = nodesToBePreservedInTheOriginalMethod.get(0);
		ArrayList<PDGNode> nodesToBePreservedInTheOriginalMethod2 = new ArrayList<PDGNode>(nodesToBePreservedInTheOriginalMethod.get(1));
		LinkedHashMap<VariableBindingKeyPair, ArrayList<PlainVariable>> declaredLocalVariables =
				new LinkedHashMap<VariableBindingKeyPair, ArrayList<PlainVariable>>();
		int counter = 0;
		for(PDGNode nodeG1 : nodesToBePreservedInTheOriginalMethod1) {
			PDGNode nodeG2 = nodesToBePreservedInTheOriginalMethod2.get(counter);
			List<AbstractVariable> nonAnonymousDeclaredVariablesG1 = new ArrayList<AbstractVariable>();
			Iterator<AbstractVariable> declaredVariableIteratorG1 = nodeG1.getDeclaredVariableIterator();
			while(declaredVariableIteratorG1.hasNext()) {
				AbstractVariable declaredVariableG1 = declaredVariableIteratorG1.next();
				String key1 = declaredVariableG1.getVariableBindingKey();
				String declaringType1 = key1.substring(0, key1.indexOf(";"));
				if(!declaringType1.contains("$")) {
					nonAnonymousDeclaredVariablesG1.add(declaredVariableG1);
				}
			}
			List<AbstractVariable> nonAnonymousDeclaredVariablesG2 = new ArrayList<AbstractVariable>();
			Iterator<AbstractVariable> declaredVariableIteratorG2 = nodeG2.getDeclaredVariableIterator();
			while(declaredVariableIteratorG2.hasNext()) {
				AbstractVariable declaredVariableG2 = declaredVariableIteratorG2.next();
				String key2 = declaredVariableG2.getVariableBindingKey();
				String declaringType2 = key2.substring(0, key2.indexOf(";"));
				if(!declaringType2.contains("$")) {
					nonAnonymousDeclaredVariablesG2.add(declaredVariableG2);
				}
			}
			int min = Math.min(nonAnonymousDeclaredVariablesG1.size(), nonAnonymousDeclaredVariablesG2.size());
			for(int i=0; i<min; i++) {
				AbstractVariable declaredVariableG1 = nonAnonymousDeclaredVariablesG1.get(i);
				AbstractVariable declaredVariableG2 = nonAnonymousDeclaredVariablesG2.get(i);
				if(declaredVariableG1 instanceof PlainVariable && declaredVariableG2 instanceof PlainVariable) {
					ArrayList<PlainVariable> declaredVariables = new ArrayList<PlainVariable>();
					declaredVariables.add((PlainVariable)declaredVariableG1);
					declaredVariables.add((PlainVariable)declaredVariableG2);
					VariableBindingKeyPair keyPair = new VariableBindingKeyPair(declaredVariableG1.getVariableBindingKey(),
							declaredVariableG2.getVariableBindingKey());
					declaredLocalVariables.put(keyPair, declaredVariables);
				}
			}
			counter++;
			Statement statement = nodeG1.getASTStatement();
			cloneInfo.methodBodyRewrite.remove(statement, null);
		}
		Set<VariableBindingKeyPair> variableBindingKeyPairsToBePassedAsParameters = new LinkedHashSet<VariableBindingKeyPair>();
		for(VariableBindingKeyPair key : declaredLocalVariables.keySet()) {
			ArrayList<PlainVariable> variables = declaredLocalVariables.get(key);
			PlainVariable plainVariable = variables.get(0);
			CloneStructureNode root = mapper.getCloneStructureRoot();
			for(CloneStructureNode child : root.getChildren()) {
				if(child.getMapping() instanceof PDGNodeMapping) {
					boolean variableIsUsedByChild = variableIsUsedByExtractedStatement(child, plainVariable);
					if(variableIsUsedByChild) {
						variableBindingKeyPairsToBePassedAsParameters.add(key);
					}
				}
			}
		}
		Map<VariableBindingKeyPair, ArrayList<VariableDeclaration>> localVariables = getDeclaredLocalVariables(declaredLocalVariables);
		for(VariableBindingKeyPair key : variableBindingKeyPairsToBePassedAsParameters) {
			ArrayList<VariableDeclaration> variables = localVariables.get(key);
			VariableDeclaration variableDeclaration1 = variables.get(0);
			VariableDeclaration variableDeclaration2 = variables.get(1);
			SimpleName variableName = variableDeclaration1.getName();
			ITypeBinding typeBinding = variableName.resolveTypeBinding();
			Type variableType = generateTypeFromTypeBinding(typeBinding, cloneInfo.ast, cloneInfo.sourceRewriter);
			SingleVariableDeclaration parameter = cloneInfo.ast.newSingleVariableDeclaration();
			cloneInfo.sourceRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, variableName, null);
			cloneInfo.sourceRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, variableType, null);
			cloneInfo.parameterRewrite.insertLast(parameter, null);
			//modify the arguments of the method calls to the extracted method
			cloneInfo.argumentRewriteList.get(0).insertLast(variableDeclaration1.getName(), null);
			cloneInfo.argumentRewriteList.get(1).insertLast(variableDeclaration2.getName(), null);
		}*/
		if(sourceMethodDeclarations.get(0).equals(sourceMethodDeclarations.get(1))) {
			try {
				List<TextEdit> textEdits = new ArrayList<TextEdit>();
				for(ASTRewrite methodBodyRewrite : cloneInfo.originalMethodBodyRewriteList) {
					TextEdit sourceEdit = methodBodyRewrite.rewriteAST();
					if(textEdits.size() > 0) {
						TextEdit previousTextEdit = textEdits.get(textEdits.size()-1);
						for(TextEdit previousChild : previousTextEdit.getChildren()) {
							for(TextEdit currentChild : sourceEdit.getChildren()) {
								if(previousChild.covers(currentChild) && previousChild.getOffset() + previousChild.getLength() != currentChild.getOffset()) {
									sourceEdit.removeChild(currentChild);
								}
							}
						}
					}
					textEdits.add(sourceEdit);
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnits.get(0).getJavaElement();
					CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
					change.getEdit().addChild(sourceEdit);
					change.addTextEditGroup(new TextEditGroup("Modify source method", new TextEdit[] {sourceEdit}));
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		else {
			finalizeOriginalMethod(sourceCompilationUnits.get(0), cloneInfo.originalMethodBodyRewriteList.get(0));
			finalizeOriginalMethod(sourceCompilationUnits.get(1), cloneInfo.originalMethodBodyRewriteList.get(1));
		}
		if(sourceMethodDeclarations.get(0).equals(sourceMethodDeclarations.get(1))) {
			AST ast = sourceMethodDeclarations.get(0).getAST();
			Set<VariableDeclaration> declaredVariablesInRemainingNodesDefinedByMappedNodesG1 = mapper.getDeclaredVariablesInRemainingNodesDefinedByMappedNodesG1();
			Set<VariableDeclaration> declaredVariablesInRemainingNodesDefinedByMappedNodesG2 = mapper.getDeclaredVariablesInRemainingNodesDefinedByMappedNodesG2();
			for(VariableBindingKeyPair parameterName : originalPassedParameters.keySet()) {
				List<VariableDeclaration> variableDeclarations = originalPassedParameters.get(parameterName);
				VariableDeclaration variableDeclaration1 = variableDeclarations.get(0);
				//create initializer for passed parameter
				if(variableDeclaration1.getInitializer() == null && !variableDeclaration1.resolveBinding().isParameter() && !variableDeclaration1.resolveBinding().isField() &&
						declaredVariablesInRemainingNodesDefinedByMappedNodesG1.contains(variableDeclaration1) &&
						variableDeclaration1 instanceof VariableDeclarationFragment) {
					ASTRewrite methodBodyRewriter = ASTRewrite.create(ast);
					Expression initializer = generateDefaultValue(methodBodyRewriter, ast, variableDeclaration1.resolveBinding().getType());
					methodBodyRewriter.set((VariableDeclarationFragment)variableDeclaration1, VariableDeclarationFragment.INITIALIZER_PROPERTY, initializer, null);
					try {
						TextEdit sourceEdit = methodBodyRewriter.rewriteAST();
						ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnits.get(0).getJavaElement();
						CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
						change.getEdit().addChild(sourceEdit);
						change.addTextEditGroup(new TextEditGroup("Initialize variable passed as parameter to the extracted method", new TextEdit[] {sourceEdit}));
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
				VariableDeclaration variableDeclaration2 = variableDeclarations.get(1);
				if(!variableDeclaration2.resolveBinding().isEqualTo(variableDeclaration1.resolveBinding())) {
					if(variableDeclaration2.getInitializer() == null && !variableDeclaration2.resolveBinding().isParameter() && !variableDeclaration2.resolveBinding().isField() &&
							declaredVariablesInRemainingNodesDefinedByMappedNodesG2.contains(variableDeclaration2) &&
							variableDeclaration2 instanceof VariableDeclarationFragment) {
						ASTRewrite methodBodyRewriter = ASTRewrite.create(ast);
						Expression initializer = generateDefaultValue(methodBodyRewriter, ast, variableDeclaration2.resolveBinding().getType());
						methodBodyRewriter.set((VariableDeclarationFragment)variableDeclaration2, VariableDeclarationFragment.INITIALIZER_PROPERTY, initializer, null);
						try {
							TextEdit sourceEdit = methodBodyRewriter.rewriteAST();
							ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnits.get(0).getJavaElement();
							CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
							change.getEdit().addChild(sourceEdit);
							change.addTextEditGroup(new TextEditGroup("Initialize variable passed as parameter to the extracted method", new TextEdit[] {sourceEdit}));
						} catch (JavaModelException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		try {
			CompilationUnitChange change = compilationUnitChanges.get(cloneInfo.sourceICompilationUnit);
			if(change != null) {
				ImportRewrite importRewrite = null;
				if(cloneInfo.importRewrite != null) {
					importRewrite = cloneInfo.importRewrite;
				}
				else {
					importRewrite = ImportRewrite.create(cloneInfo.sourceCompilationUnit, true);
				}
				for(ITypeBinding typeBinding : cloneInfo.requiredImportTypeBindings) {
					if(!typeBinding.isNested())
						importRewrite.addImport(typeBinding);
				}
				
				TextEdit importEdit = importRewrite.rewriteImports(null);
				if(importRewrite.getCreatedImports().length > 0) {
					change.getEdit().addChild(importEdit);
					change.addTextEditGroup(new TextEditGroup("Add required import declarations", new TextEdit[] {importEdit}));
				}
				
				TextEdit sourceEdit = cloneInfo.sourceRewriter.rewriteAST();
				change.getEdit().addChild(sourceEdit);
				change.addTextEditGroup(new TextEditGroup("Create method for the extracted duplicated code", new TextEdit[] {sourceEdit}));
			}
			if(cloneInfo.document != null) {
				for(ITypeBinding typeBinding : cloneInfo.requiredImportTypeBindings) {
					addImportDeclaration(typeBinding, cloneInfo.sourceCompilationUnit, cloneInfo.sourceRewriter);
				}
				TextEdit intermediateEdit = cloneInfo.sourceRewriter.rewriteAST(cloneInfo.document, null);
				intermediateEdit.apply(cloneInfo.document);
				CreateCompilationUnitChange createCompilationUnitChange =
						new CreateCompilationUnitChange(cloneInfo.sourceICompilationUnit, cloneInfo.document.get(), cloneInfo.file.getCharset());
				createCompilationUnitChanges.put(cloneInfo.sourceICompilationUnit, createCompilationUnitChange);
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private boolean differenceContainsSubDifferenceWithSubclassTypeMismatch(ASTNodeDifference difference) {
		if(difference.getExpression1() == null || difference.getExpression2() == null) {
			return false;
		}
		Expression expression1 = difference.getExpression1().getExpression();
		Expression expression2 = difference.getExpression2().getExpression();
		ITypeBinding typeBinding1 = expression1.resolveTypeBinding();
		ITypeBinding typeBinding2 = expression2.resolveTypeBinding();
		List<ASTNodeDifference> allDifferences = mapper.getNodeDifferences();
		for(ASTNodeDifference diff : allDifferences) {
			if(difference.isParentNodeDifferenceOf(diff)) {
				if(diff.containsDifferenceType(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
					Expression expr1 = diff.getExpression1().getExpression();
					Expression expr2 = diff.getExpression2().getExpression();
					if(expr1.resolveTypeBinding().isEqualTo(typeBinding1) &&
							expr2.resolveTypeBinding().isEqualTo(typeBinding2))
						return true;
				}
			}
		}
		return false;
	}

	private boolean parameterIsUsedByNodesWithoutDifferences(VariableDeclaration variableDeclaration1, VariableDeclaration variableDeclaration2) {
		PlainVariable variable1 = new PlainVariable(variableDeclaration1);
		PlainVariable variable2 = new PlainVariable(variableDeclaration2);
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		for(PDGNodeMapping pdgNodeMapping : sortedNodeMappings) {
			PDGNode node1 = pdgNodeMapping.getNodeG1();
			PDGNode node2 = pdgNodeMapping.getNodeG2();
			if((node1.usesLocalVariable(variable1) || node1.definesLocalVariable(variable1)) &&
					(node2.usesLocalVariable(variable2) || node2.definesLocalVariable(variable2))) {
				List<ASTNodeDifference> differences = pdgNodeMapping.getNonOverlappingNodeDifferences();
				if(differences.isEmpty())
					return true;
				int occurrencesInDifferences1 = 0;
				int occurrencesInDifferences2 = 0;
				boolean isRenamedVariable = false;
				for(ASTNodeDifference difference : differences) {
					BindingSignaturePair signaturePair = difference.getBindingSignaturePair();
					if(!difference.containsOnlyDifferenceType(DifferenceType.VARIABLE_TYPE_MISMATCH) && !difference.containsOnlyDifferenceType(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
						int occurrences1 = signaturePair.getSignature1().getOccurrences(variableDeclaration1.resolveBinding().getKey());
						int occurrences2 = signaturePair.getSignature2().getOccurrences(variableDeclaration2.resolveBinding().getKey());
						if(occurrences1 > 0 && occurrences2 > 0 && mapper.getRenamedVariables().contains(signaturePair)) {
							isRenamedVariable = true;
						}
						occurrencesInDifferences1 += occurrences1;
						occurrencesInDifferences2 += occurrences2;
					}
				}
				List<Expression> simpleNames1 = expressionExtractor.getVariableInstructions(node1.getASTStatement());
				List<Expression> simpleNames2 = expressionExtractor.getVariableInstructions(node2.getASTStatement());
				int occurrencesInStatement1 = 0;
				int occurrencesInStatement2 = 0;
				for(Expression expression : simpleNames1) {
					SimpleName simpleName = (SimpleName)expression;
					IBinding binding = simpleName.resolveBinding();
					if(binding != null && binding.isEqualTo(variableDeclaration1.resolveBinding())) {
						occurrencesInStatement1++;
					}
				}
				for(Expression expression : simpleNames2) {
					SimpleName simpleName = (SimpleName)expression;
					IBinding binding = simpleName.resolveBinding();
					if(binding != null && binding.isEqualTo(variableDeclaration2.resolveBinding())) {
						occurrencesInStatement2++;
					}
				}
				if(isRenamedVariable) {
					if(occurrencesInStatement1 >= occurrencesInDifferences1 || occurrencesInStatement2 >= occurrencesInDifferences2)
						return true;
				}
				else {
					if(occurrencesInStatement1 > occurrencesInDifferences1 || occurrencesInStatement2 > occurrencesInDifferences2)
						return true;
				}
			}
		}
		return false;
	}

	private Statement processCloneStructureNode(CloneStructureNode node, AST ast, ASTRewrite sourceRewriter) {
		Statement newStatement = null;
		if(processableMappedNode(node)) {
			PDGNodeMapping nodeMapping = (PDGNodeMapping) node.getMapping();
			PDGNode nodeG1 = nodeMapping.getNodeG1();
			Statement oldStatement = nodeG1.getASTStatement();
			if(oldStatement instanceof IfStatement) {
				IfStatement oldIfStatement = (IfStatement)oldStatement;
				IfStatement newIfStatement = ast.newIfStatement();
				Expression newIfExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, oldIfStatement.getExpression(), nodeMapping);
				sourceRewriter.set(newIfStatement, IfStatement.EXPRESSION_PROPERTY, newIfExpression, null);
				List<CloneStructureNode> trueControlDependentChildren = new ArrayList<CloneStructureNode>();
				List<CloneStructureNode> falseControlDependentChildren = new ArrayList<CloneStructureNode>();
				boolean symmetricalIfElse = nodeMapping.isSymmetricalIfElse();
				for(CloneStructureNode child : node.getChildren()) {
					if(processableNode(child)) {
						if(child.getMapping() instanceof PDGNodeMapping) {
							PDGNodeMapping childMapping = (PDGNodeMapping) child.getMapping();
							PDGNodeMapping symmetrical = childMapping.getSymmetricalIfNodePair();
							if(symmetrical != null) {
								if(symmetrical.equals(nodeMapping)) {
									falseControlDependentChildren.add(child);
								}
								else {
									processIfStatementChild(child, trueControlDependentChildren, falseControlDependentChildren, true);
								}
							}
							else {
								processIfStatementChild(child, trueControlDependentChildren, falseControlDependentChildren, symmetricalIfElse);
							}
						}
						else if(child.getMapping() instanceof PDGNodeGap) {
							//special handling for an if/else matched with a ternary operator
							PDGNode childNodeG1 = child.getMapping().getNodeG1();
							if(childNodeG1 != null) {
								PDGControlDependence controlDependence1 = childNodeG1.getIncomingControlDependence();
								if(controlDependence1 != null) {
									if(controlDependence1.isTrueControlDependence()) {
										trueControlDependentChildren.add(child);
									}
									else if(controlDependence1.isFalseControlDependence()) {
										falseControlDependentChildren.add(child);
									}
								}
								else {
									if(isNestedUnderElse(childNodeG1)) {
										falseControlDependentChildren.add(child);
									}
									else if(!isNestedUnderElse(childNodeG1)) {
										trueControlDependentChildren.add(child);
									}
								}
							}
						}
					}
					else if(child.getMapping() instanceof PDGElseMapping) {
						for(CloneStructureNode child2 : child.getChildren()) {
							if(processableNode(child2)) {
								if(child2.getMapping() instanceof PDGNodeMapping) {
									PDGNodeMapping childMapping = (PDGNodeMapping) child2.getMapping();
									PDGNodeMapping symmetrical = childMapping.getSymmetricalIfNodePair();
									if(symmetrical != null) {
										if(symmetrical.equals(nodeMapping)) {
											falseControlDependentChildren.add(child2);
										}
										else {
											processIfStatementChild(child2, trueControlDependentChildren, falseControlDependentChildren, true);
										}
									}
									else {
										processIfStatementChild(child2, trueControlDependentChildren, falseControlDependentChildren, symmetricalIfElse);
									}
								}
								else if(child2.getMapping() instanceof PDGNodeGap) {
									PDGNodeGap childMapping = (PDGNodeGap) child2.getMapping();
									PDGNode childNode = childMapping.getNodeG1();
									if(childNode != null) {
										PDGControlDependence controlDependence = childNode.getIncomingControlDependence();
										if(controlDependence != null) {
											if(controlDependence.isTrueControlDependence()) {
												trueControlDependentChildren.add(child2);
											}
											else if(controlDependence.isFalseControlDependence()) {
												falseControlDependentChildren.add(child2);
											}
										}
										else {
											if(isNestedUnderElse(childNode)) {
												falseControlDependentChildren.add(child2);
											}
											else if(!isNestedUnderElse(childNode)) {
												trueControlDependentChildren.add(child2);
											}
										}
									}
								}
							}
						}
					}
					else if(child.getMapping() instanceof PDGElseGap) {
						//special handling for an if/else matched with a ternary operator
						for(CloneStructureNode child2 : child.getChildren()) {
							if(processableNode(child2)) {
								if(child2.getMapping() instanceof PDGNodeGap) {
									PDGNodeGap childMapping = (PDGNodeGap) child2.getMapping();
									PDGNode childNode = childMapping.getNodeG1();
									PDGControlDependence controlDependence = childNode.getIncomingControlDependence();
									if(controlDependence != null) {
										if(controlDependence.isTrueControlDependence()) {
											trueControlDependentChildren.add(child2);
										}
										else if(controlDependence.isFalseControlDependence()) {
											falseControlDependentChildren.add(child2);
										}
									}
									else {
										if(isNestedUnderElse(childNode)) {
											falseControlDependentChildren.add(child2);
										}
										else if(!isNestedUnderElse(childNode)) {
											trueControlDependentChildren.add(child2);
										}
									}
								}
							}
						}
					}
				}
				if(oldIfStatement.getThenStatement() instanceof Block || trueControlDependentChildren.size() > 1) {
					Block thenBlock = ast.newBlock();
					ListRewrite thenBodyRewrite = sourceRewriter.getListRewrite(thenBlock, Block.STATEMENTS_PROPERTY);
					for(CloneStructureNode child : trueControlDependentChildren) {
						thenBodyRewrite.insertLast(processCloneStructureNode(child, ast, sourceRewriter), null);
					}
					sourceRewriter.set(newIfStatement, IfStatement.THEN_STATEMENT_PROPERTY, thenBlock, null);
				}
				else if(trueControlDependentChildren.size() == 1) {
					CloneStructureNode child = trueControlDependentChildren.get(0);
					sourceRewriter.set(newIfStatement, IfStatement.THEN_STATEMENT_PROPERTY, processCloneStructureNode(child, ast, sourceRewriter), null);
				}
				if(oldIfStatement.getElseStatement() instanceof Block || falseControlDependentChildren.size() > 1) {
					Block elseBlock = ast.newBlock();
					ListRewrite elseBodyRewrite = sourceRewriter.getListRewrite(elseBlock, Block.STATEMENTS_PROPERTY);
					for(CloneStructureNode child : falseControlDependentChildren) {
						elseBodyRewrite.insertLast(processCloneStructureNode(child, ast, sourceRewriter), null);
					}
					sourceRewriter.set(newIfStatement, IfStatement.ELSE_STATEMENT_PROPERTY, elseBlock, null);
				}
				else if(falseControlDependentChildren.size() == 1) {
					CloneStructureNode child = falseControlDependentChildren.get(0);
					sourceRewriter.set(newIfStatement, IfStatement.ELSE_STATEMENT_PROPERTY, processCloneStructureNode(child, ast, sourceRewriter), null);
				}
				newStatement = newIfStatement;
			}
			else if(oldStatement instanceof SynchronizedStatement) {
				SynchronizedStatement oldSynchronizedStatement = (SynchronizedStatement)oldStatement;
				SynchronizedStatement newSynchronizedStatement = ast.newSynchronizedStatement();
				Expression newExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, oldSynchronizedStatement.getExpression(), nodeMapping);
				sourceRewriter.set(newSynchronizedStatement, SynchronizedStatement.EXPRESSION_PROPERTY, newExpression, null);
				Block newBlock = ast.newBlock();
				ListRewrite blockRewrite = sourceRewriter.getListRewrite(newBlock, Block.STATEMENTS_PROPERTY);
				for(CloneStructureNode child : node.getChildren()) {
					if(processableNode(child)) {
						blockRewrite.insertLast(processCloneStructureNode(child, ast, sourceRewriter), null);
					}
				}
				sourceRewriter.set(newSynchronizedStatement, SynchronizedStatement.BODY_PROPERTY, newBlock, null);
				newStatement = newSynchronizedStatement;
			}
			else if(oldStatement instanceof TryStatement) {
				TryStatement oldTryStatement = (TryStatement)oldStatement;
				TryStatement newTryStatement = ast.newTryStatement();
				ListRewrite resourceRewrite = sourceRewriter.getListRewrite(newTryStatement, TryStatement.RESOURCES_PROPERTY);
				List<VariableDeclarationExpression> resources = oldTryStatement.resources();
				for(VariableDeclarationExpression expression : resources) {
					Expression newResourceExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, expression, nodeMapping);
					resourceRewrite.insertLast(newResourceExpression, null);
				}
				Block newBlock = ast.newBlock();
				ListRewrite blockRewrite = sourceRewriter.getListRewrite(newBlock, Block.STATEMENTS_PROPERTY);
				for(CloneStructureNode child : node.getChildren()) {
					if(processableNode(child)) {
						blockRewrite.insertLast(processCloneStructureNode(child, ast, sourceRewriter), null);
					}
				}
				sourceRewriter.set(newTryStatement, TryStatement.BODY_PROPERTY, newBlock, null);
				ListRewrite catchClauseRewrite = sourceRewriter.getListRewrite(newTryStatement, TryStatement.CATCH_CLAUSES_PROPERTY);
				List<CatchClause> catchClauses = oldTryStatement.catchClauses();
				for(CatchClause catchClause : catchClauses) {
					CatchClause newCatchClause = ast.newCatchClause();
					SingleVariableDeclaration newSingleVariableDeclaration = (SingleVariableDeclaration)processASTNodeWithDifferences(ast, sourceRewriter, catchClause.getException(), nodeMapping);
					sourceRewriter.set(newCatchClause, CatchClause.EXCEPTION_PROPERTY, newSingleVariableDeclaration, null);
					Block newCatchBody = ast.newBlock();
					ListRewrite newCatchBodyRewrite = sourceRewriter.getListRewrite(newCatchBody, Block.STATEMENTS_PROPERTY);
					List<Statement> oldCatchStatements = catchClause.getBody().statements();
					for(Statement oldCatchStatement : oldCatchStatements) {
						Statement newStatement2 = (Statement)processASTNodeWithDifferences(ast, sourceRewriter, oldCatchStatement, nodeMapping);
						newCatchBodyRewrite.insertLast(newStatement2, null);
					}
					sourceRewriter.set(newCatchClause, CatchClause.BODY_PROPERTY, newCatchBody, null);
					catchClauseRewrite.insertLast(newCatchClause, null);
				}
				if(oldTryStatement.getFinally() != null) {
					Block newFinallyBody = ast.newBlock();
					ListRewrite newFinallyBodyRewrite = sourceRewriter.getListRewrite(newFinallyBody, Block.STATEMENTS_PROPERTY);
					List<Statement> oldFinallyStatements = oldTryStatement.getFinally().statements();
					for(Statement oldFinallyStatement : oldFinallyStatements) {
						Statement newStatement2 = (Statement)processASTNodeWithDifferences(ast, sourceRewriter, oldFinallyStatement, nodeMapping);
						newFinallyBodyRewrite.insertLast(newStatement2, null);
					}
					sourceRewriter.set(newTryStatement, TryStatement.FINALLY_PROPERTY, newFinallyBody, null);
				}
				newStatement = newTryStatement;
			}
			else if(oldStatement instanceof SwitchStatement) {
				SwitchStatement oldSwitchStatement = (SwitchStatement)oldStatement;
				SwitchStatement newSwitchStatement = ast.newSwitchStatement();
				Expression newSwitchExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, oldSwitchStatement.getExpression(), nodeMapping);
				sourceRewriter.set(newSwitchStatement, SwitchStatement.EXPRESSION_PROPERTY, newSwitchExpression, null);
				ListRewrite switchStatementsRewrite = sourceRewriter.getListRewrite(newSwitchStatement, SwitchStatement.STATEMENTS_PROPERTY);
				for(CloneStructureNode child : node.getChildren()) {
					if(processableNode(child)) {
						switchStatementsRewrite.insertLast(processCloneStructureNode(child, ast, sourceRewriter), null);
					}
				}
				newStatement = newSwitchStatement;
			}
			else if(oldStatement instanceof WhileStatement) {
				WhileStatement oldWhileStatement = (WhileStatement)oldStatement;
				WhileStatement newWhileStatement = ast.newWhileStatement();
				Expression newWhileExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, oldWhileStatement.getExpression(), nodeMapping);
				sourceRewriter.set(newWhileStatement, WhileStatement.EXPRESSION_PROPERTY, newWhileExpression, null);
				Block loopBlock = ast.newBlock();
				ListRewrite loopBlockRewrite = sourceRewriter.getListRewrite(loopBlock, Block.STATEMENTS_PROPERTY);
				for(CloneStructureNode child : node.getChildren()) {
					if(processableNode(child)) {
						loopBlockRewrite.insertLast(processCloneStructureNode(child, ast, sourceRewriter), null);
					}
				}
				sourceRewriter.set(newWhileStatement, WhileStatement.BODY_PROPERTY, loopBlock, null);
				newStatement = newWhileStatement;
			}
			else if(oldStatement instanceof ForStatement) {
				ForStatement oldForStatement = (ForStatement)oldStatement;
				ForStatement newForStatement = ast.newForStatement();
				Expression newForExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, oldForStatement.getExpression(), nodeMapping);
				sourceRewriter.set(newForStatement, ForStatement.EXPRESSION_PROPERTY, newForExpression, null);
				ListRewrite initializerRewrite = sourceRewriter.getListRewrite(newForStatement, ForStatement.INITIALIZERS_PROPERTY);
				List<Expression> initializers = oldForStatement.initializers();
				for(Expression expression : initializers) {
					Expression newInitializerExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, expression, nodeMapping);
					if(nodeMapping.isAdvancedMatch()) {
						List<AbstractMethodFragment> additionallyMatchedFragments1 = nodeMapping.getAdditionallyMatchedFragments1();
						List<AbstractMethodFragment> additionallyMatchedFragments2 = nodeMapping.getAdditionallyMatchedFragments2();
						if(additionallyMatchedFragments1.size() > additionallyMatchedFragments2.size() && newInitializerExpression instanceof Assignment) {
							Assignment oldAssignment = (Assignment)expression;
							Assignment newAssignment = (Assignment)newInitializerExpression;
							if(oldAssignment.getLeftHandSide() instanceof SimpleName) {
								SimpleName oldLeftHandSide = (SimpleName)oldAssignment.getLeftHandSide();
								for(AbstractMethodFragment fragment : additionallyMatchedFragments1) {
									if(fragment instanceof StatementObject) {
										StatementObject abstractStatement = (StatementObject)fragment;
										Statement astStatement = abstractStatement.getStatement();
										if(astStatement instanceof VariableDeclarationStatement) {
											VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)astStatement;
											List<VariableDeclarationFragment> variableDeclarationFragments = variableDeclarationStatement.fragments();
											boolean sameVariableFound = false;
											for(VariableDeclarationFragment declarationFragment : variableDeclarationFragments) {
												if(declarationFragment.getName().resolveBinding().isEqualTo(oldLeftHandSide.resolveBinding())) {
													sameVariableFound = true;
													break;
												}
											}
											if(sameVariableFound) {
												Type variableType = variableDeclarationStatement.getType();
												VariableDeclarationFragment newFragment = ast.newVariableDeclarationFragment();
												sourceRewriter.set(newFragment, VariableDeclarationFragment.NAME_PROPERTY, newAssignment.getLeftHandSide(), null);
												sourceRewriter.set(newFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, newAssignment.getRightHandSide(), null);
												VariableDeclarationExpression newVariableDeclarationExpression = ast.newVariableDeclarationExpression(newFragment);
												sourceRewriter.set(newVariableDeclarationExpression, VariableDeclarationExpression.TYPE_PROPERTY, variableType, null);
												newInitializerExpression = newVariableDeclarationExpression;
												break;
											}
										}
									}
								}
							}
						}
					}
					initializerRewrite.insertLast(newInitializerExpression, null);
				}
				ListRewrite updaterRewrite = sourceRewriter.getListRewrite(newForStatement, ForStatement.UPDATERS_PROPERTY);
				List<Expression> updaters = oldForStatement.updaters();
				for(Expression expression : updaters) {
					Expression newUpdaterExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, expression, nodeMapping);
					updaterRewrite.insertLast(newUpdaterExpression, null);
				}
				Block loopBlock = ast.newBlock();
				ListRewrite loopBlockRewrite = sourceRewriter.getListRewrite(loopBlock, Block.STATEMENTS_PROPERTY);
				for(CloneStructureNode child : node.getChildren()) {
					if(processableNode(child)) {
						loopBlockRewrite.insertLast(processCloneStructureNode(child, ast, sourceRewriter), null);
					}
				}
				sourceRewriter.set(newForStatement, ForStatement.BODY_PROPERTY, loopBlock, null);
				newStatement = newForStatement;
			}
			else if(oldStatement instanceof EnhancedForStatement) {
				EnhancedForStatement oldEnhancedForStatement = (EnhancedForStatement)oldStatement;
				EnhancedForStatement newEnhancedForStatement = ast.newEnhancedForStatement();
				sourceRewriter.set(newEnhancedForStatement, EnhancedForStatement.PARAMETER_PROPERTY, oldEnhancedForStatement.getParameter(), null);
				Expression newEnhancedForExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, oldEnhancedForStatement.getExpression(), nodeMapping);
				sourceRewriter.set(newEnhancedForStatement, EnhancedForStatement.EXPRESSION_PROPERTY, newEnhancedForExpression, null);
				Block loopBlock = ast.newBlock();
				ListRewrite loopBlockRewrite = sourceRewriter.getListRewrite(loopBlock, Block.STATEMENTS_PROPERTY);
				for(CloneStructureNode child : node.getChildren()) {
					if(processableNode(child)) {
						loopBlockRewrite.insertLast(processCloneStructureNode(child, ast, sourceRewriter), null);
					}
				}
				sourceRewriter.set(newEnhancedForStatement, EnhancedForStatement.BODY_PROPERTY, loopBlock, null);
				newStatement = newEnhancedForStatement;
			}
			else if(oldStatement instanceof DoStatement) {
				DoStatement oldDoStatement = (DoStatement)oldStatement;
				DoStatement newDoStatement = ast.newDoStatement();
				Expression newDoExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, oldDoStatement.getExpression(), nodeMapping);
				sourceRewriter.set(newDoStatement, DoStatement.EXPRESSION_PROPERTY, newDoExpression, null);
				Block loopBlock = ast.newBlock();
				ListRewrite loopBlockRewrite = sourceRewriter.getListRewrite(loopBlock, Block.STATEMENTS_PROPERTY);
				for(CloneStructureNode child : node.getChildren()) {
					if(processableNode(child)) {
						loopBlockRewrite.insertLast(processCloneStructureNode(child, ast, sourceRewriter), null);
					}
				}
				sourceRewriter.set(newDoStatement, DoStatement.BODY_PROPERTY, loopBlock, null);
				newStatement = newDoStatement;
			}
			else {
				newStatement = (Statement)processASTNodeWithDifferences(ast, sourceRewriter, oldStatement, nodeMapping);
			}
			LabeledStatement labeled1 = belongsToLabeledStatement(nodeG1);
			LabeledStatement labeled2 = belongsToLabeledStatement(nodeMapping.getNodeG2());
			if(labeled1 != null && labeled2 != null) {
				labeledStatementsToBeRemoved.get(0).add(labeled1);
				labeledStatementsToBeRemoved.get(1).add(labeled2);
				LabeledStatement newLabeledStatement = ast.newLabeledStatement();
				sourceRewriter.set(newLabeledStatement, LabeledStatement.LABEL_PROPERTY, labeled1.getLabel(), null);
				sourceRewriter.set(newLabeledStatement, LabeledStatement.BODY_PROPERTY, newStatement, null);
				newStatement = newLabeledStatement;
			}
		}
		else if(processableGapNode(node)) {
			PDGNodeGap nodeGap = (PDGNodeGap) node.getMapping();
			PDGNode nodeG1 = nodeGap.getNodeG1();
			Statement oldStatement = nodeG1.getASTStatement();
			newStatement = (Statement)processASTNodeWithDifferences(ast, sourceRewriter, oldStatement, nodeGap);
		}
		return newStatement;
	}

	private void processIfStatementChild(CloneStructureNode child,
			List<CloneStructureNode> trueControlDependentChildren,
			List<CloneStructureNode> falseControlDependentChildren, boolean symmetricalIfElse) {
		PDGNode childNodeG1 = child.getMapping().getNodeG1();
		PDGNode childNodeG2 = child.getMapping().getNodeG2();
		PDGControlDependence controlDependence1 = childNodeG1.getIncomingControlDependence();
		PDGControlDependence controlDependence2 = childNodeG2.getIncomingControlDependence();
		if(controlDependence1 != null && controlDependence2 != null) {
			if((controlDependence1.isTrueControlDependence() && controlDependence2.isTrueControlDependence()) ||
					(controlDependence1.isTrueControlDependence() && symmetricalIfElse)) {
				trueControlDependentChildren.add(child);
			}
			else if((controlDependence1.isFalseControlDependence() && controlDependence2.isFalseControlDependence()) ||
					(controlDependence1.isFalseControlDependence() && symmetricalIfElse)) {
				falseControlDependentChildren.add(child);
			}
		}
		else {
			if((isNestedUnderElse(childNodeG1) && isNestedUnderElse(childNodeG2)) ||
					(isNestedUnderElse(childNodeG1) && symmetricalIfElse)) {
				falseControlDependentChildren.add(child);
			}
			else if((!isNestedUnderElse(childNodeG1) && !isNestedUnderElse(childNodeG2)) ||
					(!isNestedUnderElse(childNodeG1) && symmetricalIfElse)) {
				trueControlDependentChildren.add(child);
			}
		}
	}

	private boolean processableNode(CloneStructureNode child) {
		return processableMappedNode(child)  || processableGapNode(child);
	}

	private boolean processableMappedNode(CloneStructureNode child) {
		return child.getMapping() instanceof PDGNodeMapping;
	}

	private boolean processableGapNode(CloneStructureNode child) {
		return child.getMapping() instanceof PDGNodeGap && child.getMapping().isAdvancedMatch() && child.getMapping().getNodeG1() != null;
	}

	private Statement processCloneStructureGapNode(CloneStructureNode node, AST ast, ASTRewrite sourceRewriter, int index) {
		PDGNodeGap nodeMapping = (PDGNodeGap) node.getMapping();
		PDGNode pdgNode = null;
		if(index == 0)
			pdgNode = nodeMapping.getNodeG1();
		else
			pdgNode = nodeMapping.getNodeG2();
		Statement oldStatement = pdgNode.getASTStatement();
		Statement newStatement = null;
		if(oldStatement instanceof IfStatement) {
			IfStatement oldIfStatement = (IfStatement)oldStatement;
			IfStatement newIfStatement = ast.newIfStatement();
			Expression newIfExpression = oldIfStatement.getExpression();
			sourceRewriter.set(newIfStatement, IfStatement.EXPRESSION_PROPERTY, newIfExpression, null);
			List<CloneStructureNode> trueControlDependentChildren = new ArrayList<CloneStructureNode>();
			List<CloneStructureNode> falseControlDependentChildren = new ArrayList<CloneStructureNode>();
			for(CloneStructureNode child : node.getChildren()) {
				if(child.getMapping() instanceof PDGNodeGap) {
					PDGNodeGap childMapping = (PDGNodeGap) child.getMapping();
					PDGNode childNode = null;
					if(index == 0)
						childNode = childMapping.getNodeG1();
					else
						childNode = childMapping.getNodeG2();
					PDGControlDependence controlDependence = childNode.getIncomingControlDependence();
					if(controlDependence != null) {
						if(controlDependence.isTrueControlDependence()) {
							trueControlDependentChildren.add(child);
						}
						else if(controlDependence.isFalseControlDependence()) {
							falseControlDependentChildren.add(child);
						}
					}
					else {
						if(isNestedUnderElse(childNode)) {
							falseControlDependentChildren.add(child);
						}
						else if(!isNestedUnderElse(childNode)) {
							trueControlDependentChildren.add(child);
						}
					}
				}
				else if(child.getMapping() instanceof PDGElseGap) {
					for(CloneStructureNode child2 : child.getChildren()) {
						if(child2.getMapping() instanceof PDGNodeGap) {
							PDGNodeGap childMapping = (PDGNodeGap) child2.getMapping();
							PDGNode childNode = null;
							if(index == 0)
								childNode = childMapping.getNodeG1();
							else
								childNode = childMapping.getNodeG2();
							PDGControlDependence controlDependence = childNode.getIncomingControlDependence();
							if(controlDependence != null) {
								if(controlDependence.isTrueControlDependence()) {
									trueControlDependentChildren.add(child2);
								}
								else if(controlDependence.isFalseControlDependence()) {
									falseControlDependentChildren.add(child2);
								}
							}
							else {
								if(isNestedUnderElse(childNode)) {
									falseControlDependentChildren.add(child2);
								}
								else if(!isNestedUnderElse(childNode)) {
									trueControlDependentChildren.add(child2);
								}
							}
						}
					}
				}
			}
			if(oldIfStatement.getThenStatement() instanceof Block || trueControlDependentChildren.size() > 1) {
				Block thenBlock = ast.newBlock();
				ListRewrite thenBodyRewrite = sourceRewriter.getListRewrite(thenBlock, Block.STATEMENTS_PROPERTY);
				for(CloneStructureNode child : trueControlDependentChildren) {
					thenBodyRewrite.insertLast(processCloneStructureGapNode(child, ast, sourceRewriter, index), null);
				}
				sourceRewriter.set(newIfStatement, IfStatement.THEN_STATEMENT_PROPERTY, thenBlock, null);
			}
			else if(trueControlDependentChildren.size() == 1) {
				CloneStructureNode child = trueControlDependentChildren.get(0);
				sourceRewriter.set(newIfStatement, IfStatement.THEN_STATEMENT_PROPERTY, processCloneStructureGapNode(child, ast, sourceRewriter, index), null);
			}
			if(oldIfStatement.getElseStatement() instanceof Block || falseControlDependentChildren.size() > 1) {
				Block elseBlock = ast.newBlock();
				ListRewrite elseBodyRewrite = sourceRewriter.getListRewrite(elseBlock, Block.STATEMENTS_PROPERTY);
				for(CloneStructureNode child : falseControlDependentChildren) {
					elseBodyRewrite.insertLast(processCloneStructureGapNode(child, ast, sourceRewriter, index), null);
				}
				sourceRewriter.set(newIfStatement, IfStatement.ELSE_STATEMENT_PROPERTY, elseBlock, null);
			}
			else if(falseControlDependentChildren.size() == 1) {
				CloneStructureNode child = falseControlDependentChildren.get(0);
				sourceRewriter.set(newIfStatement, IfStatement.ELSE_STATEMENT_PROPERTY, processCloneStructureGapNode(child, ast, sourceRewriter, index), null);
			}
			newStatement = newIfStatement;
		}
		else if(oldStatement instanceof SynchronizedStatement) {
			SynchronizedStatement oldSynchronizedStatement = (SynchronizedStatement)oldStatement;
			SynchronizedStatement newSynchronizedStatement = ast.newSynchronizedStatement();
			Expression newExpression = oldSynchronizedStatement.getExpression();
			sourceRewriter.set(newSynchronizedStatement, SynchronizedStatement.EXPRESSION_PROPERTY, newExpression, null);
			Block newBlock = ast.newBlock();
			ListRewrite blockRewrite = sourceRewriter.getListRewrite(newBlock, Block.STATEMENTS_PROPERTY);
			for(CloneStructureNode child : node.getChildren()) {
				if(child.getMapping() instanceof PDGNodeGap) {
					blockRewrite.insertLast(processCloneStructureGapNode(child, ast, sourceRewriter, index), null);
				}
			}
			sourceRewriter.set(newSynchronizedStatement, SynchronizedStatement.BODY_PROPERTY, newBlock, null);
			newStatement = newSynchronizedStatement;
		}
		else if(oldStatement instanceof TryStatement) {
			TryStatement oldTryStatement = (TryStatement)oldStatement;
			TryStatement newTryStatement = ast.newTryStatement();
			ListRewrite resourceRewrite = sourceRewriter.getListRewrite(newTryStatement, TryStatement.RESOURCES_PROPERTY);
			List<VariableDeclarationExpression> resources = oldTryStatement.resources();
			for(VariableDeclarationExpression expression : resources) {
				Expression newResourceExpression = expression;
				resourceRewrite.insertLast(newResourceExpression, null);
			}
			Block newBlock = ast.newBlock();
			ListRewrite blockRewrite = sourceRewriter.getListRewrite(newBlock, Block.STATEMENTS_PROPERTY);
			for(CloneStructureNode child : node.getChildren()) {
				if(child.getMapping() instanceof PDGNodeGap) {
					blockRewrite.insertLast(processCloneStructureGapNode(child, ast, sourceRewriter, index), null);
				}
			}
			sourceRewriter.set(newTryStatement, TryStatement.BODY_PROPERTY, newBlock, null);
			ListRewrite catchClauseRewrite = sourceRewriter.getListRewrite(newTryStatement, TryStatement.CATCH_CLAUSES_PROPERTY);
			List<CatchClause> catchClauses = oldTryStatement.catchClauses();
			for(CatchClause catchClause : catchClauses) {
				CatchClause newCatchClause = ast.newCatchClause();
				sourceRewriter.set(newCatchClause, CatchClause.EXCEPTION_PROPERTY, catchClause.getException(), null);
				Block newCatchBody = ast.newBlock();
				ListRewrite newCatchBodyRewrite = sourceRewriter.getListRewrite(newCatchBody, Block.STATEMENTS_PROPERTY);
				List<Statement> oldCatchStatements = catchClause.getBody().statements();
				for(Statement oldCatchStatement : oldCatchStatements) {
					Statement newStatement2 = oldCatchStatement;
					newCatchBodyRewrite.insertLast(newStatement2, null);
				}
				sourceRewriter.set(newCatchClause, CatchClause.BODY_PROPERTY, newCatchBody, null);
				catchClauseRewrite.insertLast(newCatchClause, null);
			}
			if(oldTryStatement.getFinally() != null) {
				Block newFinallyBody = ast.newBlock();
				ListRewrite newFinallyBodyRewrite = sourceRewriter.getListRewrite(newFinallyBody, Block.STATEMENTS_PROPERTY);
				List<Statement> oldFinallyStatements = oldTryStatement.getFinally().statements();
				for(Statement oldFinallyStatement : oldFinallyStatements) {
					Statement newStatement2 = oldFinallyStatement;
					newFinallyBodyRewrite.insertLast(newStatement2, null);
				}
				sourceRewriter.set(newTryStatement, TryStatement.FINALLY_PROPERTY, newFinallyBody, null);
			}
			newStatement = newTryStatement;
		}
		else if(oldStatement instanceof SwitchStatement) {
			SwitchStatement oldSwitchStatement = (SwitchStatement)oldStatement;
			SwitchStatement newSwitchStatement = ast.newSwitchStatement();
			Expression newSwitchExpression = oldSwitchStatement.getExpression();
			sourceRewriter.set(newSwitchStatement, SwitchStatement.EXPRESSION_PROPERTY, newSwitchExpression, null);
			ListRewrite switchStatementsRewrite = sourceRewriter.getListRewrite(newSwitchStatement, SwitchStatement.STATEMENTS_PROPERTY);
			for(CloneStructureNode child : node.getChildren()) {
				if(child.getMapping() instanceof PDGNodeGap) {
					switchStatementsRewrite.insertLast(processCloneStructureGapNode(child, ast, sourceRewriter, index), null);
				}
			}
			newStatement = newSwitchStatement;
		}
		else if(oldStatement instanceof WhileStatement) {
			WhileStatement oldWhileStatement = (WhileStatement)oldStatement;
			WhileStatement newWhileStatement = ast.newWhileStatement();
			Expression newWhileExpression = oldWhileStatement.getExpression();
			sourceRewriter.set(newWhileStatement, WhileStatement.EXPRESSION_PROPERTY, newWhileExpression, null);
			Block loopBlock = ast.newBlock();
			ListRewrite loopBlockRewrite = sourceRewriter.getListRewrite(loopBlock, Block.STATEMENTS_PROPERTY);
			for(CloneStructureNode child : node.getChildren()) {
				if(child.getMapping() instanceof PDGNodeGap) {
					loopBlockRewrite.insertLast(processCloneStructureGapNode(child, ast, sourceRewriter, index), null);
				}
			}
			sourceRewriter.set(newWhileStatement, WhileStatement.BODY_PROPERTY, loopBlock, null);
			newStatement = newWhileStatement;
		}
		else if(oldStatement instanceof ForStatement) {
			ForStatement oldForStatement = (ForStatement)oldStatement;
			ForStatement newForStatement = ast.newForStatement();
			Expression newForExpression = oldForStatement.getExpression();
			sourceRewriter.set(newForStatement, ForStatement.EXPRESSION_PROPERTY, newForExpression, null);
			ListRewrite initializerRewrite = sourceRewriter.getListRewrite(newForStatement, ForStatement.INITIALIZERS_PROPERTY);
			List<Expression> initializers = oldForStatement.initializers();
			for(Expression expression : initializers) {
				Expression newInitializerExpression = expression;
				initializerRewrite.insertLast(newInitializerExpression, null);
			}
			ListRewrite updaterRewrite = sourceRewriter.getListRewrite(newForStatement, ForStatement.UPDATERS_PROPERTY);
			List<Expression> updaters = oldForStatement.updaters();
			for(Expression expression : updaters) {
				Expression newUpdaterExpression = expression;
				updaterRewrite.insertLast(newUpdaterExpression, null);
			}
			Block loopBlock = ast.newBlock();
			ListRewrite loopBlockRewrite = sourceRewriter.getListRewrite(loopBlock, Block.STATEMENTS_PROPERTY);
			for(CloneStructureNode child : node.getChildren()) {
				if(child.getMapping() instanceof PDGNodeGap) {
					loopBlockRewrite.insertLast(processCloneStructureGapNode(child, ast, sourceRewriter, index), null);
				}
			}
			sourceRewriter.set(newForStatement, ForStatement.BODY_PROPERTY, loopBlock, null);
			newStatement = newForStatement;
		}
		else if(oldStatement instanceof EnhancedForStatement) {
			EnhancedForStatement oldEnhancedForStatement = (EnhancedForStatement)oldStatement;
			EnhancedForStatement newEnhancedForStatement = ast.newEnhancedForStatement();
			sourceRewriter.set(newEnhancedForStatement, EnhancedForStatement.PARAMETER_PROPERTY, oldEnhancedForStatement.getParameter(), null);
			Expression newEnhancedForExpression = oldEnhancedForStatement.getExpression();
			sourceRewriter.set(newEnhancedForStatement, EnhancedForStatement.EXPRESSION_PROPERTY, newEnhancedForExpression, null);
			Block loopBlock = ast.newBlock();
			ListRewrite loopBlockRewrite = sourceRewriter.getListRewrite(loopBlock, Block.STATEMENTS_PROPERTY);
			for(CloneStructureNode child : node.getChildren()) {
				if(child.getMapping() instanceof PDGNodeGap) {
					loopBlockRewrite.insertLast(processCloneStructureGapNode(child, ast, sourceRewriter, index), null);
				}
			}
			sourceRewriter.set(newEnhancedForStatement, EnhancedForStatement.BODY_PROPERTY, loopBlock, null);
			newStatement = newEnhancedForStatement;
		}
		else if(oldStatement instanceof DoStatement) {
			DoStatement oldDoStatement = (DoStatement)oldStatement;
			DoStatement newDoStatement = ast.newDoStatement();
			Expression newDoExpression = oldDoStatement.getExpression();
			sourceRewriter.set(newDoStatement, DoStatement.EXPRESSION_PROPERTY, newDoExpression, null);
			Block loopBlock = ast.newBlock();
			ListRewrite loopBlockRewrite = sourceRewriter.getListRewrite(loopBlock, Block.STATEMENTS_PROPERTY);
			for(CloneStructureNode child : node.getChildren()) {
				if(child.getMapping() instanceof PDGNodeGap) {
					loopBlockRewrite.insertLast(processCloneStructureGapNode(child, ast, sourceRewriter, index), null);
				}
			}
			sourceRewriter.set(newDoStatement, DoStatement.BODY_PROPERTY, loopBlock, null);
			newStatement = newDoStatement;
		}
		else {
			newStatement = oldStatement;
		}
		LabeledStatement labeled = belongsToLabeledStatement(pdgNode);
		if(labeled != null) {
			LabeledStatement newLabeledStatement = ast.newLabeledStatement();
			sourceRewriter.set(newLabeledStatement, LabeledStatement.LABEL_PROPERTY, labeled.getLabel(), null);
			sourceRewriter.set(newLabeledStatement, LabeledStatement.BODY_PROPERTY, newStatement, null);
			newStatement = newLabeledStatement;
		}
		return newStatement;
	}

	private boolean isNestedUnderElse(PDGNode pdgNode) {
		Statement statement = pdgNode.getASTStatement();
		if(statement.getParent() instanceof Block) {
			Block block = (Block)statement.getParent();
			if(block.getParent() instanceof IfStatement) {
				IfStatement ifStatement = (IfStatement)block.getParent();
				if(ifStatement.getElseStatement() != null && ifStatement.getElseStatement().equals(block))
					return true;
			}
		}
		else if(statement.getParent() instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement)statement.getParent();
			if(ifStatement.getElseStatement() != null && ifStatement.getElseStatement().equals(statement))
				return true;
		}
		return false;
	}

	private LabeledStatement belongsToLabeledStatement(PDGNode pdgNode) {
		Statement statement = pdgNode.getASTStatement();
		if(statement.getParent() instanceof LabeledStatement) {
			return (LabeledStatement) statement.getParent();
		}
		return null;
	}

	private ASTNode processASTNodeWithDifferences(AST ast, ASTRewrite sourceRewriter, ASTNode oldASTNode, NodeMapping nodeMapping) {
		List<ASTNodeDifference> differences = nodeMapping.getNonOverlappingNodeDifferences();
		if(differences.isEmpty()) {
			return preprocessASTNode(oldASTNode, ast, sourceRewriter);
		}
		else {
			Set<VariableBindingKeyPair> parameterBindingKeys = originalPassedParameters.keySet();
			Set<VariableBindingKeyPair> commonPassedParameterBindingKeys = mapper.getCommonPassedParameters().keySet();
			Set<VariableBindingKeyPair> declaredLocalVariableBindingKeysWithinAnonymousClass = mapper.getDeclaredLocalVariablesInMappedNodesWithinAnonymousClass();
			Set<VariableBindingKeyPair> declaredLocalVariableBindingKeys = mapper.getDeclaredLocalVariablesInMappedNodes().keySet();
			Set<String> declaredLocalVariableBindingKeysInAdditionallyMatchedNodes1 = mapper.getDeclaredLocalVariableBindingKeysInAdditionallyMatchedNodesG1();
			Set<String> declaredLocalVariableBindingKeysInAdditionallyMatchedNodes2 = mapper.getDeclaredLocalVariableBindingKeysInAdditionallyMatchedNodesG2();
			ASTNode astNode = preprocessASTNode(oldASTNode, ast, sourceRewriter);
			ASTNode newASTNode = null;
			if(astNode.equals(oldASTNode)) {
				newASTNode = ASTNode.copySubtree(ast, oldASTNode);
			}
			else {
				newASTNode = astNode;
			}
			for(ASTNodeDifference difference : differences) {
				if(!nodeMapping.isDifferenceInConditionalExpressionOfAdvancedLoopMatch(difference)) {
					Expression oldExpression = difference.getExpression1().getExpression();
					oldExpression = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(oldExpression);
					Expression oldExpression2 = difference.getExpression2().getExpression();
					oldExpression2 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(oldExpression2);
					boolean isCommonParameter = false;
					if(oldExpression instanceof SimpleName && oldExpression2 instanceof SimpleName) {
						SimpleName oldSimpleName = (SimpleName)oldExpression;
						SimpleName oldSimpleName2 = (SimpleName)oldExpression2;
						IBinding binding = oldSimpleName.resolveBinding();
						IBinding binding2 = oldSimpleName2.resolveBinding();
						VariableBindingKeyPair keyPair = new VariableBindingKeyPair(binding.getKey(), binding2.getKey());
						if(parameterBindingKeys.contains(keyPair) || commonPassedParameterBindingKeys.contains(keyPair) ||
								declaredLocalVariableBindingKeys.contains(keyPair) || declaredLocalVariableBindingKeysWithinAnonymousClass.contains(keyPair) ||
								declaredLocalVariableBindingKeysInAdditionallyMatchedNodes1.contains(binding.getKey()) ||
								declaredLocalVariableBindingKeysInAdditionallyMatchedNodes2.contains(binding2.getKey()))
							isCommonParameter = true;
					}
					else if(oldExpression instanceof QualifiedName && oldExpression2 instanceof QualifiedName) {
						QualifiedName oldQualifiedName = (QualifiedName)oldExpression;
						QualifiedName oldQualifiedName2 = (QualifiedName)oldExpression2;
						SimpleName oldSimpleName = oldQualifiedName.getName();
						SimpleName oldSimpleName2 = oldQualifiedName2.getName();
						IBinding binding = oldSimpleName.resolveBinding();
						IBinding binding2 = oldSimpleName2.resolveBinding();
						VariableBindingKeyPair keyPair = new VariableBindingKeyPair(binding.getKey(), binding2.getKey());
						if(parameterBindingKeys.contains(keyPair) || commonPassedParameterBindingKeys.contains(keyPair) ||
								declaredLocalVariableBindingKeys.contains(keyPair) || declaredLocalVariableBindingKeysWithinAnonymousClass.contains(keyPair) ||
								declaredLocalVariableBindingKeysInAdditionallyMatchedNodes1.contains(binding.getKey()) ||
								declaredLocalVariableBindingKeysInAdditionallyMatchedNodes2.contains(binding2.getKey()))
							isCommonParameter = true;
						if(oldQualifiedName.getQualifier().equals(difference.getExpression1().getExpression()) &&
								oldQualifiedName2.getQualifier().equals(difference.getExpression2().getExpression())) {
							if(oldQualifiedName.getQualifier() instanceof SimpleName && oldQualifiedName2.getQualifier() instanceof SimpleName) {
							oldSimpleName = (SimpleName)oldQualifiedName.getQualifier();
							oldSimpleName2 = (SimpleName)oldQualifiedName2.getQualifier();
							binding = oldSimpleName.resolveBinding();
							binding2 = oldSimpleName2.resolveBinding();
							keyPair = new VariableBindingKeyPair(binding.getKey(), binding2.getKey());
							if(parameterBindingKeys.contains(keyPair) || commonPassedParameterBindingKeys.contains(keyPair) ||
									declaredLocalVariableBindingKeys.contains(keyPair) || declaredLocalVariableBindingKeysWithinAnonymousClass.contains(keyPair) ||
									declaredLocalVariableBindingKeysInAdditionallyMatchedNodes1.contains(binding.getKey()) ||
									declaredLocalVariableBindingKeysInAdditionallyMatchedNodes2.contains(binding2.getKey()))
								isCommonParameter = true;
							}
						}
					}
					if(!isCommonParameter) {
						if(difference instanceof FieldAccessReplacedWithGetterInvocationDifference) {
							FieldAccessReplacedWithGetterInvocationDifference nodeDifference =
									(FieldAccessReplacedWithGetterInvocationDifference)difference;
							boolean fieldIsParameterized = false;
							for(VariableDeclaration field : fieldDeclarationsToBeParameterized.get(0)) {
								Expression expr = nodeDifference.getExpression1().getExpression();
								if(expr instanceof SimpleName) {
									SimpleName simpleName = (SimpleName)expr;
									if(simpleName.resolveBinding().isEqualTo(field.resolveBinding())) {
										fieldIsParameterized = true;
									}
								}
								else if(expr instanceof FieldAccess) {
									FieldAccess fieldAccess = (FieldAccess)expr;
									if(fieldAccess.getName().resolveBinding().isEqualTo(field.resolveBinding())) {
										fieldIsParameterized = true;
									}
								}
							}
							if(!fieldIsParameterized) {
								MethodInvocation newGetterMethodInvocation = generateGetterMethodInvocation(ast, sourceRewriter, nodeDifference);
								if(oldASTNode.equals(oldExpression)) {
									return newGetterMethodInvocation;
								}
								else {
									replaceExpression(sourceRewriter, oldASTNode, newASTNode, oldExpression, newGetterMethodInvocation);
								}
							}
						}
						else if(difference instanceof FieldAssignmentReplacedWithSetterInvocationDifference) {
							FieldAssignmentReplacedWithSetterInvocationDifference nodeDifference =
									(FieldAssignmentReplacedWithSetterInvocationDifference)difference;
							MethodInvocation newSetterMethodInvocation = generateSetterMethodInvocation(ast, sourceRewriter, nodeDifference);
							if(oldASTNode.equals(oldExpression)) {
								return newSetterMethodInvocation;
							}
							else {
								replaceExpression(sourceRewriter, oldASTNode, newASTNode, oldExpression, newSetterMethodInvocation);
							}
						}
						else if(oldExpression.getParent() instanceof Type) {
							Type oldType = (Type)oldExpression.getParent();
							if(difference.containsDifferenceType(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
								ITypeBinding typeBinding1 = difference.getExpression1().getExpression().resolveTypeBinding();
								ITypeBinding typeBinding2 = difference.getExpression2().getExpression().resolveTypeBinding();
								ITypeBinding commonSuperTypeBinding = ASTNodeMatcher.commonSuperType(typeBinding1, typeBinding2);
								if(commonSuperTypeBinding != null) {
									Type arg = RefactoringUtility.generateTypeFromTypeBinding(commonSuperTypeBinding, ast, sourceRewriter);
									TypeVisitor oldTypeVisitor = new TypeVisitor();
									oldASTNode.accept(oldTypeVisitor);
									List<Type> oldTypes = oldTypeVisitor.getTypes();
									TypeVisitor newTypeVisitor = new TypeVisitor();
									newASTNode.accept(newTypeVisitor);
									List<Type> newTypes = newTypeVisitor.getTypes();
									int j = 0;
									for(Type type : oldTypes) {
										Type newType = newTypes.get(j);
										if(type.equals(oldType)) {
											sourceRewriter.replace(newType, arg, null);
											break;
										}
										j++;
									}
								}
							}
						}
						else {
							Set<VariableDeclaration> fields1 = fieldDeclarationsToBePulledUp.get(0);
							BindingSignature bindingSignature1 = difference.getBindingSignaturePair().getSignature1();
							boolean expression1IsFieldToBePulledUp = false;
							for(VariableDeclaration field : fields1) {
								if(bindingSignature1.containsOnlyBinding(field.resolveBinding().getKey())) {
									expression1IsFieldToBePulledUp = true;
									break;
								}
							}
							Set<VariableDeclaration> fields2 = fieldDeclarationsToBePulledUp.get(1);
							BindingSignature bindingSignature2 = difference.getBindingSignaturePair().getSignature2();
							boolean expression2IsFieldToBePulledUp = false;
							for(VariableDeclaration field : fields2) {
								if(bindingSignature2.containsOnlyBinding(field.resolveBinding().getKey())) {
									expression2IsFieldToBePulledUp = true;
									break;
								}
							}
							boolean expressionIsFieldToBePulledUp = expression1IsFieldToBePulledUp && expression2IsFieldToBePulledUp;
							if(!expressionIsFieldToBePulledUp) {
								Expression argument = createArgument(ast, difference);
								if(oldASTNode.equals(oldExpression)) {
									return argument;
								}
								else {
									replaceExpression(sourceRewriter, oldASTNode, newASTNode, oldExpression, argument);
								}
							}
						}
					}
				}
			}
			return newASTNode;
		}
	}

	private ASTNode preprocessASTNode(ASTNode oldASTNode, AST ast, ASTRewrite sourceRewriter) {
		boolean replacement = false;
		ASTNode newASTNode = ASTNode.copySubtree(ast, oldASTNode);
		if(!assignedFieldDeclarationsToBeReplacedWithSetter.get(0).isEmpty()) {
			if(oldASTNode instanceof Assignment) {
				ASTNode node = createReplacementForFieldAssignment(sourceRewriter, ast, (Assignment)oldASTNode);
				if(node != null) {
					newASTNode = node;
					replacement = true;
				}
			}
			else {
				replacement = replacement || replaceFieldAssignmentsWithSetterMethodInvocations(sourceRewriter, ast, oldASTNode, newASTNode);
			}
		}
		if(!accessedFieldDeclarationsToBeReplacedWithGetter.get(0).isEmpty()) {
			if(oldASTNode instanceof FieldAccess || oldASTNode instanceof SimpleName) {
				ASTNode node = createReplacementForFieldAccessWithGetterInvocation(sourceRewriter, ast, (Expression)oldASTNode);
				if(node != null) {
					newASTNode = node;
					replacement = true;
				}
			}
			else {
				replacement = replacement || replaceFieldAccessesWithGetterMethodInvocations(sourceRewriter, ast, oldASTNode, newASTNode);
			}
		}
		if(!fieldDeclarationsToBeParameterized.get(0).isEmpty()) {
			if(oldASTNode instanceof FieldAccess || oldASTNode instanceof SimpleName) {
				ASTNode node = createReplacementForFieldAccessOfParameterizedFields(sourceRewriter, ast, (Expression)oldASTNode);
				if(node != null) {
					newASTNode = node;
					replacement = true;
				}
			}
			else {
				replacement = replacement || replaceFieldAccessesOfParameterizedFields(sourceRewriter, ast, oldASTNode, newASTNode);
			}
		}
		if(oldASTNode instanceof SuperMethodInvocation) {
			ASTNode node = createReplacementForSuperMethodCall(sourceRewriter, ast, (SuperMethodInvocation)oldASTNode);
			if(node != null) {
				newASTNode = node;
				replacement = true;
			}
		}
		else {
			replacement = replacement || replaceSuperMethodCallsWithRegularMethodCalls(sourceRewriter, ast, oldASTNode, newASTNode);
		}
		if(replacement)
			return newASTNode;
		else
			return oldASTNode;
	}

	private boolean replaceFieldAccessesOfParameterizedFields(ASTRewrite sourceRewriter, AST ast, ASTNode oldASTNode, ASTNode newASTNode) {
		boolean replacement = false;
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> oldFieldAccesses = new ArrayList<Expression>();
		List<Expression> newFieldAccesses = new ArrayList<Expression>();
		if(oldASTNode instanceof Expression) {
			oldFieldAccesses.addAll(expressionExtractor.getFieldAccesses((Expression)oldASTNode));
			newFieldAccesses.addAll(expressionExtractor.getFieldAccesses((Expression)newASTNode));
		}
		else if(oldASTNode instanceof Statement) {
			oldFieldAccesses.addAll(expressionExtractor.getFieldAccesses((Statement)oldASTNode));
			newFieldAccesses.addAll(expressionExtractor.getFieldAccesses((Statement)newASTNode));
		}
		int j = 0;
		for(Expression oldExpression : oldFieldAccesses) {
			FieldAccess oldFieldAccess = (FieldAccess)oldExpression;
			FieldAccess newFieldAccess = (FieldAccess)newFieldAccesses.get(j);
			for(VariableDeclaration variableDeclaration : fieldDeclarationsToBeParameterized.get(0)) {
				if(oldFieldAccess.getName().resolveBinding().isEqualTo(variableDeclaration.resolveBinding())) {
					String identifier = (variableDeclaration.resolveBinding().getModifiers() & Modifier.STATIC) != 0 ? variableDeclaration.getName().getIdentifier() :
						createNameForParameterizedFieldAccess(variableDeclaration.getName().getIdentifier());
					sourceRewriter.replace(newFieldAccess, ast.newSimpleName(identifier), null);
					replacement = true;
					break;
				}
			}
			j++;
		}
		oldFieldAccesses = new ArrayList<Expression>();
		newFieldAccesses = new ArrayList<Expression>();
		List<Expression> oldVariableInstructions = new ArrayList<Expression>();
		List<Expression> newVariableInstructions = new ArrayList<Expression>();
		if(oldASTNode instanceof Expression) {
			oldVariableInstructions.addAll(expressionExtractor.getVariableInstructions((Expression)oldASTNode));
			newVariableInstructions.addAll(expressionExtractor.getVariableInstructions((Expression)newASTNode));
		}
		else if(oldASTNode instanceof Statement) {
			oldVariableInstructions.addAll(expressionExtractor.getVariableInstructions((Statement)oldASTNode));
			newVariableInstructions.addAll(expressionExtractor.getVariableInstructions((Statement)newASTNode));
		}
		int k = 0;
		for(Expression e : oldVariableInstructions) {
			SimpleName simpleName = (SimpleName)e;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null && binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				if(variableBinding.isField() && !(simpleName.getParent() instanceof FieldAccess)) {
					oldFieldAccesses.add(simpleName);
					newFieldAccesses.add(newVariableInstructions.get(k));
				}
			}
			k++;
		}
		j = 0;
		for(Expression oldExpression : oldFieldAccesses) {
			SimpleName oldFieldAccess = (SimpleName)oldExpression;
			SimpleName newFieldAccess = (SimpleName)newFieldAccesses.get(j);
			for(VariableDeclaration variableDeclaration : fieldDeclarationsToBeParameterized.get(0)) {
				if(oldFieldAccess.resolveBinding().isEqualTo(variableDeclaration.resolveBinding())) {
					String identifier = (variableDeclaration.resolveBinding().getModifiers() & Modifier.STATIC) != 0 ? variableDeclaration.getName().getIdentifier() :
						createNameForParameterizedFieldAccess(variableDeclaration.getName().getIdentifier());
					sourceRewriter.replace(newFieldAccess, ast.newSimpleName(identifier), null);
					replacement = true;
					break;
				}
			}
			j++;
		}
		return replacement;
	}

	private boolean replaceFieldAccessesWithGetterMethodInvocations(ASTRewrite sourceRewriter, AST ast, ASTNode oldASTNode, ASTNode newASTNode) {
		boolean replacement = false;
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> oldFieldAccesses = new ArrayList<Expression>();
		List<Expression> newFieldAccesses = new ArrayList<Expression>();
		if(oldASTNode instanceof Expression) {
			oldFieldAccesses.addAll(expressionExtractor.getFieldAccesses((Expression)oldASTNode));
			newFieldAccesses.addAll(expressionExtractor.getFieldAccesses((Expression)newASTNode));
		}
		else if(oldASTNode instanceof Statement) {
			oldFieldAccesses.addAll(expressionExtractor.getFieldAccesses((Statement)oldASTNode));
			newFieldAccesses.addAll(expressionExtractor.getFieldAccesses((Statement)newASTNode));
		}
		int j = 0;
		for(Expression oldExpression : oldFieldAccesses) {
			FieldAccess oldFieldAccess = (FieldAccess)oldExpression;
			FieldAccess newFieldAccess = (FieldAccess)newFieldAccesses.get(j);
			for(VariableDeclaration variableDeclaration : accessedFieldDeclarationsToBeReplacedWithGetter.get(0)) {
				if(oldFieldAccess.getName().resolveBinding().isEqualTo(variableDeclaration.resolveBinding())) {
					MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
					String originalFieldName = oldFieldAccess.getName().getIdentifier();
					String accessedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
					String getterMethodName = GETTER_PREFIX + accessedFieldName;
					getterMethodName = appendAccessorMethodSuffix(getterMethodName);
					SimpleName getterMethodSimpleName = ast.newSimpleName(getterMethodName);
					sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, getterMethodSimpleName, null);
					sourceRewriter.replace(newFieldAccess, getterMethodInvocation, null);
					replacement = true;
					break;
				}
			}
			j++;
		}
		oldFieldAccesses = new ArrayList<Expression>();
		newFieldAccesses = new ArrayList<Expression>();
		List<Expression> oldVariableInstructions = new ArrayList<Expression>();
		List<Expression> newVariableInstructions = new ArrayList<Expression>();
		if(oldASTNode instanceof Expression) {
			oldVariableInstructions.addAll(expressionExtractor.getVariableInstructions((Expression)oldASTNode));
			newVariableInstructions.addAll(expressionExtractor.getVariableInstructions((Expression)newASTNode));
		}
		else if(oldASTNode instanceof Statement) {
			oldVariableInstructions.addAll(expressionExtractor.getVariableInstructions((Statement)oldASTNode));
			newVariableInstructions.addAll(expressionExtractor.getVariableInstructions((Statement)newASTNode));
		}
		int k = 0;
		for(Expression e : oldVariableInstructions) {
			SimpleName simpleName = (SimpleName)e;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null && binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				if(variableBinding.isField() && !(simpleName.getParent() instanceof FieldAccess)) {
					oldFieldAccesses.add(simpleName);
					newFieldAccesses.add(newVariableInstructions.get(k));
				}
			}
			k++;
		}
		j = 0;
		for(Expression oldExpression : oldFieldAccesses) {
			SimpleName oldFieldAccess = (SimpleName)oldExpression;
			SimpleName newFieldAccess = (SimpleName)newFieldAccesses.get(j);
			for(VariableDeclaration variableDeclaration : accessedFieldDeclarationsToBeReplacedWithGetter.get(0)) {
				if(oldFieldAccess.resolveBinding().isEqualTo(variableDeclaration.resolveBinding())) {
					MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
					String originalFieldName = oldFieldAccess.getIdentifier();
					String accessedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
					String getterMethodName = GETTER_PREFIX + accessedFieldName;
					getterMethodName = appendAccessorMethodSuffix(getterMethodName);
					SimpleName getterMethodSimpleName = ast.newSimpleName(getterMethodName);
					sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, getterMethodSimpleName, null);
					sourceRewriter.replace(newFieldAccess, getterMethodInvocation, null);
					replacement = true;
					break;
				}
			}
			j++;
		}
		return replacement;
	}

	private boolean replaceFieldAssignmentsWithSetterMethodInvocations(ASTRewrite sourceRewriter, AST ast, ASTNode oldASTNode, ASTNode newASTNode) {
		boolean replacement = false;
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> oldAssignments = new ArrayList<Expression>();
		List<Expression> newAssignments = new ArrayList<Expression>();
		if(oldASTNode instanceof Expression) {
			oldAssignments.addAll(expressionExtractor.getAssignments((Expression)oldASTNode));
			newAssignments.addAll(expressionExtractor.getAssignments((Expression)newASTNode));
		}
		else if(oldASTNode instanceof Statement) {
			oldAssignments.addAll(expressionExtractor.getAssignments((Statement)oldASTNode));
			newAssignments.addAll(expressionExtractor.getAssignments((Statement)newASTNode));
		}
		int j = 0;
		for(Expression oldExpression : oldAssignments) {
			Assignment oldAssignment = (Assignment)oldExpression;
			Assignment newAssignment = (Assignment)newAssignments.get(j);
			Expression oldLeftHandSide = oldAssignment.getLeftHandSide();
			Expression newLeftHandSide = newAssignment.getLeftHandSide();
			SimpleName oldFieldName = null;
			SimpleName newFieldName = null;
			if(oldLeftHandSide instanceof SimpleName) {
				oldFieldName = (SimpleName)oldLeftHandSide;
				newFieldName = (SimpleName)newLeftHandSide;
			}
			else if(oldLeftHandSide instanceof FieldAccess) {
				oldFieldName = ((FieldAccess)oldLeftHandSide).getName();
				newFieldName = ((FieldAccess)newLeftHandSide).getName();
			}
			for(VariableDeclaration variableDeclaration : assignedFieldDeclarationsToBeReplacedWithSetter.get(0)) {
				if(oldFieldName != null && oldFieldName.resolveBinding().isEqualTo(variableDeclaration.resolveBinding())) {
					MethodInvocation setterMethodInvocation = ast.newMethodInvocation();
					String originalFieldName = newFieldName.getIdentifier();
					String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
					String setterMethodName = SETTER_PREFIX + modifiedFieldName;
					setterMethodName = appendAccessorMethodSuffix(setterMethodName);
					SimpleName setterMethodSimpleName = ast.newSimpleName(setterMethodName);
					sourceRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, setterMethodSimpleName, null);
					ListRewrite setterMethodInvocationArgumentsRewrite = sourceRewriter.getListRewrite(setterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
					setterMethodInvocationArgumentsRewrite.insertLast(newAssignment.getRightHandSide(), null);
					sourceRewriter.replace(newAssignment, setterMethodInvocation, null);
					replacement = true;
					break;
				}
			}
			j++;
		}
		return replacement;
	}

	private boolean replaceSuperMethodCallsWithRegularMethodCalls(ASTRewrite sourceRewriter, AST ast, ASTNode oldASTNode, ASTNode newASTNode) {
		boolean replacement = false;
		ITypeBinding typeBinding1 = sourceTypeDeclarations.get(0).resolveBinding();
		ITypeBinding typeBinding2 = sourceTypeDeclarations.get(1).resolveBinding();
		ITypeBinding commonSuperTypeOfSourceTypeDeclarations = ASTNodeMatcher.commonSuperType(typeBinding1, typeBinding2);
		if(commonSuperTypeOfSourceTypeDeclarations != null && cloneInfo.intermediateClassName == null && !cloneInfo.extractUtilityClass) {
			ExpressionExtractor expressionExtractor = new ExpressionExtractor();
			List<Expression> oldSuperMethodInvocations = new ArrayList<Expression>();
			List<Expression> newSuperMethodInvocations = new ArrayList<Expression>();
			if(oldASTNode instanceof Expression) {
				oldSuperMethodInvocations.addAll(expressionExtractor.getSuperMethodInvocations((Expression)oldASTNode));
				newSuperMethodInvocations.addAll(expressionExtractor.getSuperMethodInvocations((Expression)newASTNode));
			}
			else if(oldASTNode instanceof Statement) {
				oldSuperMethodInvocations.addAll(expressionExtractor.getSuperMethodInvocations((Statement)oldASTNode));
				newSuperMethodInvocations.addAll(expressionExtractor.getSuperMethodInvocations((Statement)newASTNode));
			}
			int j = 0;
			for(Expression oldExpression : oldSuperMethodInvocations) {
				SuperMethodInvocation oldSuperMethodInvocation = (SuperMethodInvocation)oldExpression;
				SuperMethodInvocation newSuperMethodInvocation = (SuperMethodInvocation)newSuperMethodInvocations.get(j);
				if(oldSuperMethodInvocation.resolveMethodBinding().getDeclaringClass().isEqualTo(commonSuperTypeOfSourceTypeDeclarations)) {
					MethodInvocation newMethodInvocation = ast.newMethodInvocation();
					sourceRewriter.set(newMethodInvocation, MethodInvocation.NAME_PROPERTY, oldSuperMethodInvocation.getName(), null);
					ListRewrite argumentRewrite = sourceRewriter.getListRewrite(newMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
					List<Expression> oldArguments = oldSuperMethodInvocation.arguments();
					for(Expression oldArgument : oldArguments) {
						argumentRewrite.insertLast(oldArgument, null);
					}
					sourceRewriter.replace(newSuperMethodInvocation, newMethodInvocation, null);
					replacement = true;
					break;
				}
				j++;
			}
		}
		return replacement;
	}

	private ASTNode createReplacementForFieldAccessOfParameterizedFields(ASTRewrite sourceRewriter, AST ast, Expression oldASTNode) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> oldFieldAccesses = new ArrayList<Expression>();
		oldFieldAccesses.addAll(expressionExtractor.getFieldAccesses(oldASTNode));
		for(Expression oldExpression : oldFieldAccesses) {
			FieldAccess oldFieldAccess = (FieldAccess)oldExpression;
			for(VariableDeclaration variableDeclaration : fieldDeclarationsToBeParameterized.get(0)) {
				if(oldFieldAccess.getName().resolveBinding().isEqualTo(variableDeclaration.resolveBinding())) {
					String identifier = (variableDeclaration.resolveBinding().getModifiers() & Modifier.STATIC) != 0 ? variableDeclaration.getName().getIdentifier() :
						createNameForParameterizedFieldAccess(variableDeclaration.getName().getIdentifier());
					return ast.newSimpleName(identifier);
				}
			}
		}
		oldFieldAccesses = new ArrayList<Expression>();
		List<Expression> oldVariableInstructions = expressionExtractor.getVariableInstructions(oldASTNode);
		for(Expression oldExpression : oldVariableInstructions) {
			SimpleName simpleName = (SimpleName)oldExpression;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null && binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				if(variableBinding.isField() && !(simpleName.getParent() instanceof FieldAccess)) {
					oldFieldAccesses.add(simpleName);
				}
			}
		}
		for(Expression oldExpression : oldFieldAccesses) {
			SimpleName oldFieldAccess = (SimpleName)oldExpression;
			for(VariableDeclaration variableDeclaration : fieldDeclarationsToBeParameterized.get(0)) {
				if(oldFieldAccess.resolveBinding().isEqualTo(variableDeclaration.resolveBinding())) {
					String identifier = (variableDeclaration.resolveBinding().getModifiers() & Modifier.STATIC) != 0 ? variableDeclaration.getName().getIdentifier() :
						createNameForParameterizedFieldAccess(variableDeclaration.getName().getIdentifier());
					return ast.newSimpleName(identifier);
				}
			}
		}
		return null;
	}

	private ASTNode createReplacementForFieldAccessWithGetterInvocation(ASTRewrite sourceRewriter, AST ast, Expression oldASTNode) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> oldFieldAccesses = new ArrayList<Expression>();
		oldFieldAccesses.addAll(expressionExtractor.getFieldAccesses(oldASTNode));
		for(Expression oldExpression : oldFieldAccesses) {
			FieldAccess oldFieldAccess = (FieldAccess)oldExpression;
			for(VariableDeclaration variableDeclaration : accessedFieldDeclarationsToBeReplacedWithGetter.get(0)) {
				if(oldFieldAccess.getName().resolveBinding().isEqualTo(variableDeclaration.resolveBinding())) {
					MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
					String originalFieldName = oldFieldAccess.getName().getIdentifier();
					String accessedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
					String getterMethodName = GETTER_PREFIX + accessedFieldName;
					getterMethodName = appendAccessorMethodSuffix(getterMethodName);
					SimpleName getterMethodSimpleName = ast.newSimpleName(getterMethodName);
					sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, getterMethodSimpleName, null);
					return getterMethodInvocation;
				}
			}
		}
		oldFieldAccesses = new ArrayList<Expression>();
		List<Expression> oldVariableInstructions = expressionExtractor.getVariableInstructions(oldASTNode);
		for(Expression oldExpression : oldVariableInstructions) {
			SimpleName simpleName = (SimpleName)oldExpression;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null && binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				if(variableBinding.isField() && !(simpleName.getParent() instanceof FieldAccess)) {
					oldFieldAccesses.add(simpleName);
				}
			}
		}
		for(Expression oldExpression : oldFieldAccesses) {
			SimpleName oldFieldAccess = (SimpleName)oldExpression;
			for(VariableDeclaration variableDeclaration : accessedFieldDeclarationsToBeReplacedWithGetter.get(0)) {
				if(oldFieldAccess.resolveBinding().isEqualTo(variableDeclaration.resolveBinding())) {
					MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
					String originalFieldName = oldFieldAccess.getIdentifier();
					String accessedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
					String getterMethodName = GETTER_PREFIX + accessedFieldName;
					getterMethodName = appendAccessorMethodSuffix(getterMethodName);
					SimpleName getterMethodSimpleName = ast.newSimpleName(getterMethodName);
					sourceRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, getterMethodSimpleName, null);
					return getterMethodInvocation;
				}
			}
		}
		return null;
	}

	private String createNameForParameterizedFieldAccess(String fieldName) {
		return "this" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1, fieldName.length());
	}

	private ASTNode createReplacementForFieldAssignment(ASTRewrite sourceRewriter, AST ast, Assignment oldASTNode) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> oldAssignments = new ArrayList<Expression>();
		oldAssignments.addAll(expressionExtractor.getAssignments(oldASTNode));
		for(Expression oldExpression : oldAssignments) {
			Assignment oldAssignment = (Assignment)oldExpression;
			Expression oldLeftHandSide = oldAssignment.getLeftHandSide();
			SimpleName fieldName = null;
			if(oldLeftHandSide instanceof SimpleName) {
				fieldName = (SimpleName)oldLeftHandSide;
			}
			else if(oldLeftHandSide instanceof FieldAccess) {
				fieldName = ((FieldAccess)oldLeftHandSide).getName();
			}
			for(VariableDeclaration variableDeclaration : assignedFieldDeclarationsToBeReplacedWithSetter.get(0)) {
				if(fieldName != null && fieldName.resolveBinding().isEqualTo(variableDeclaration.resolveBinding())) {
					MethodInvocation setterMethodInvocation = ast.newMethodInvocation();
					String originalFieldName = fieldName.getIdentifier();
					String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
					String setterMethodName = SETTER_PREFIX + modifiedFieldName;
					setterMethodName = appendAccessorMethodSuffix(setterMethodName);
					SimpleName setterMethodSimpleName = ast.newSimpleName(setterMethodName);
					sourceRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, setterMethodSimpleName, null);
					ListRewrite setterMethodInvocationArgumentsRewrite = sourceRewriter.getListRewrite(setterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
					setterMethodInvocationArgumentsRewrite.insertLast(oldAssignment.getRightHandSide(), null);
					return setterMethodInvocation;
				}
			}
		}
		return null;
	}
	
	private ASTNode createReplacementForSuperMethodCall(ASTRewrite sourceRewriter, AST ast, SuperMethodInvocation oldASTNode) {
		ITypeBinding typeBinding1 = sourceTypeDeclarations.get(0).resolveBinding();
		ITypeBinding typeBinding2 = sourceTypeDeclarations.get(1).resolveBinding();
		ITypeBinding commonSuperTypeOfSourceTypeDeclarations = ASTNodeMatcher.commonSuperType(typeBinding1, typeBinding2);
		if(commonSuperTypeOfSourceTypeDeclarations != null && cloneInfo.intermediateClassName == null && !cloneInfo.extractUtilityClass) {
			ExpressionExtractor expressionExtractor = new ExpressionExtractor();
			List<Expression> oldSuperMethodInvocations = new ArrayList<Expression>();
			oldSuperMethodInvocations.addAll(expressionExtractor.getSuperMethodInvocations(oldASTNode));
			for(Expression oldExpression : oldSuperMethodInvocations) {
				SuperMethodInvocation oldSuperMethodInvocation = (SuperMethodInvocation)oldExpression;
				if(oldSuperMethodInvocation.resolveMethodBinding().getDeclaringClass().isEqualTo(commonSuperTypeOfSourceTypeDeclarations)) {
					MethodInvocation newMethodInvocation = ast.newMethodInvocation();
					sourceRewriter.set(newMethodInvocation, MethodInvocation.NAME_PROPERTY, oldSuperMethodInvocation.getName(), null);
					ListRewrite argumentRewrite = sourceRewriter.getListRewrite(newMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
					List<Expression> oldArguments = oldSuperMethodInvocation.arguments();
					for(Expression oldArgument : oldArguments) {
						argumentRewrite.insertLast(oldArgument, null);
					}
					return newMethodInvocation;
				}
			}
		}
		return null;
	}

	private Expression createArgument(AST ast, ASTNodeDifference argumentDifference) {
		Expression argument;
		int existingArgValue = findExistingParametersWithArgName();
		int i = 0;
		if(existingArgValue > 0) {
			i = existingArgValue + 1;
		}
		if(parameterizedDifferenceMap.containsKey(argumentDifference.getBindingSignaturePair())) {
			List<BindingSignaturePair> list = new ArrayList<BindingSignaturePair>(parameterizedDifferenceMap.keySet());
			int index = list.indexOf(argumentDifference.getBindingSignaturePair());
			argument = ast.newSimpleName("arg" + (i + index));
		}
		else {
			argument = ast.newSimpleName("arg" + (i + parameterizedDifferenceMap.size()));
			parameterizedDifferenceMap.put(argumentDifference.getBindingSignaturePair(), argumentDifference);
		}
		return argument;
	}

	private MethodInvocation generateSetterMethodInvocation(AST ast, ASTRewrite sourceRewriter,
			FieldAssignmentReplacedWithSetterInvocationDifference nodeDifference) {
		MethodInvocation newSetterMethodInvocation = ast.newMethodInvocation();
		sourceRewriter.set(newSetterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(nodeDifference.getSetterMethodName()), null);
		AbstractExpression invoker1 = nodeDifference.getInvoker1();
		AbstractExpression invoker2 = nodeDifference.getInvoker2();
		Expression invoker = null;
		if(invoker1 != null && invoker2 == null) {
			Expression expression1 = invoker1.getExpression();
			if(expression1 instanceof ThisExpression) {
				// do nothing
			}
			else {
				ASTNodeDifference invokerDifference = new ASTNodeDifference(invoker1, invoker2);
				invoker = createArgument(ast, invokerDifference);
			}
		}
		else if(invoker1 == null && invoker2 != null) {
			Expression expression2 = invoker2.getExpression();
			if(expression2 instanceof ThisExpression) {
				// do nothing
			}
			else {
				ASTNodeDifference invokerDifference = new ASTNodeDifference(invoker1, invoker2);
				invoker = createArgument(ast, invokerDifference);
			}
		}
		else if(!nodeDifference.getInvokerDifferences().isEmpty()) {
			List<ASTNodeDifference> invokerDifferences = nodeDifference.getInvokerDifferences();
			invoker = processNestedDifferences(ast, sourceRewriter, invoker1.getExpression(), invoker2.getExpression(), invokerDifferences);
		}
		else {
			// the invokers are the same
			if(invoker1 != null && invoker2 != null) {
				invoker = invoker1.getExpression();
			}
		}
		if(invoker != null) {
			sourceRewriter.set(newSetterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, invoker, null);
		}
		Expression argument1 = nodeDifference.getArgument1().getExpression();
		Expression argument2 = nodeDifference.getArgument2().getExpression();
		Expression argument = null;
		if(!nodeDifference.getArgumentDifferences().isEmpty()) {
			List<ASTNodeDifference> argumentDifferences = nodeDifference.getArgumentDifferences();
			argument = processNestedDifferences(ast, sourceRewriter, argument1, argument2, argumentDifferences);
		}
		else {
			// the arguments are the same
			argument = argument1;
		}
		ListRewrite argumentRewriter = sourceRewriter.getListRewrite(newSetterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
		argumentRewriter.insertLast(argument, null);
		return newSetterMethodInvocation;
	}

	private Expression processNestedDifferences(AST ast, ASTRewrite sourceRewriter, Expression entireExpression1, Expression entireExpression2,
			List<ASTNodeDifference> nestedDifferences) {
		Expression argument = null;
		boolean differenceCoversTheEntireExpression = false;
		for(ASTNodeDifference argumentDifference : nestedDifferences) {
			Expression expression1 = argumentDifference.getExpression1().getExpression();
			Expression expression2 = argumentDifference.getExpression2().getExpression();
			if(expression1.equals(entireExpression1) && expression2.equals(entireExpression2)) {
				differenceCoversTheEntireExpression = true;
				break;
			}
		}
		if(!differenceCoversTheEntireExpression) {
			Expression newArgument = (Expression)ASTNode.copySubtree(ast, entireExpression1);
			for(ASTNodeDifference argumentDifference : nestedDifferences) {
				Expression oldExpression = argumentDifference.getExpression1().getExpression();
				Expression replacement;
				if(argumentDifference instanceof FieldAccessReplacedWithGetterInvocationDifference) {
					replacement = generateGetterMethodInvocation(ast, sourceRewriter, (FieldAccessReplacedWithGetterInvocationDifference) argumentDifference);
				}
				else {
					replacement = createArgument(ast, argumentDifference);
				}
				replaceExpression(sourceRewriter, entireExpression1, newArgument, oldExpression, replacement);
			}
			argument = newArgument;
		}
		else {
			for(ASTNodeDifference argumentDifference : nestedDifferences) {
				Expression expression1 = argumentDifference.getExpression1().getExpression();
				Expression expression2 = argumentDifference.getExpression2().getExpression();
				if(expression1.equals(entireExpression1) && expression2.equals(entireExpression2)) {
					argument = createArgument(ast, argumentDifference);
					break;
				}
			}
		}
		return argument;
	}

	private MethodInvocation generateGetterMethodInvocation(AST ast, ASTRewrite sourceRewriter,
			FieldAccessReplacedWithGetterInvocationDifference nodeDifference) {
		MethodInvocation newGetterMethodInvocation = ast.newMethodInvocation();
		sourceRewriter.set(newGetterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(nodeDifference.getGetterMethodName()), null);
		AbstractExpression invoker1 = nodeDifference.getInvoker1();
		AbstractExpression invoker2 = nodeDifference.getInvoker2();
		Expression invoker = null;
		if(invoker1 != null && invoker2 == null) {
			Expression expression1 = invoker1.getExpression();
			if(expression1 instanceof ThisExpression) {
				// do nothing
			}
			else {
				ASTNodeDifference invokerDifference = new ASTNodeDifference(invoker1, invoker2);
				invoker = createArgument(ast, invokerDifference);
			}
		}
		else if(invoker1 == null && invoker2 != null) {
			Expression expression2 = invoker2.getExpression();
			if(expression2 instanceof ThisExpression) {
				// do nothing
			}
			else {
				ASTNodeDifference invokerDifference = new ASTNodeDifference(invoker1, invoker2);
				invoker = createArgument(ast, invokerDifference);
			}
		}
		else if(!nodeDifference.getInvokerDifferences().isEmpty()) {
			List<ASTNodeDifference> invokerDifferences = nodeDifference.getInvokerDifferences();
			invoker = processNestedDifferences(ast, sourceRewriter, invoker1.getExpression(), invoker2.getExpression(), invokerDifferences);
		}
		else {
			// the invokers are the same
			if(invoker1 != null && invoker2 != null) {
				invoker = invoker1.getExpression();
			}
		}
		if(invoker != null) {
			sourceRewriter.set(newGetterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, invoker, null);
		}
		return newGetterMethodInvocation;
	}

	private void replaceExpression(ASTRewrite sourceRewriter, ASTNode oldASTNode, ASTNode newASTNode,
			Expression oldExpression, Expression replacement) {
		int j = 0;
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> oldExpressions = expressionExtractor.getAllExpressions(oldASTNode);
		List<Expression> newExpressions = expressionExtractor.getAllExpressions(newASTNode);
		for(Expression expression : oldExpressions) {
			Expression newExpression = newExpressions.get(j);
			if(expression.equals(oldExpression)) {
				sourceRewriter.replace(newExpression, replacement, null);
				break;
			}
			if(oldExpression instanceof QualifiedName) {
				QualifiedName oldQualifiedName = (QualifiedName)oldExpression;
				if(expression.equals(oldQualifiedName.getName())) {
					sourceRewriter.replace(newExpression.getParent(), replacement, null);
					break;
				}
			}
			j++;
		}
	}

	private void addConstructorDeclaration(MethodDeclaration methodDeclaration, TypeDeclaration typeDeclaration, CompilationUnit compilationUnit, Set<ITypeBinding> requiredImportTypeBindings) {
		//check if there is already a constructor declared with the same signature as the super constructor call
		SuperConstructorInvocation superConstructorInvocation = firstStatementIsSuperConstructorInvocation(methodDeclaration);
		boolean constructorFound = false;
		for(MethodDeclaration method : typeDeclaration.getMethods()) {
			if(method.isConstructor()) {
				List<SingleVariableDeclaration> parameters = method.parameters();
				List<Expression> arguments = superConstructorInvocation.arguments();
				if(matchingParameterTypesWithArgumentTypes(parameters, arguments)) {
					constructorFound = true;
					break;
				}
			}
		}
		if(!constructorFound) {
			AST ast = typeDeclaration.getAST();
			ASTRewrite rewriter = ASTRewrite.create(ast);
			MethodDeclaration constructor = copyConstructor(methodDeclaration, ast, rewriter, typeDeclaration.getName(), requiredImportTypeBindings);
			ListRewrite bodyRewrite = rewriter.getListRewrite(typeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			bodyRewrite.insertFirst(constructor, null);
			try {
				TextEdit sourceEdit = rewriter.rewriteAST();
				ICompilationUnit sourceICompilationUnit = (ICompilationUnit)compilationUnit.getJavaElement();
				CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
				if(change == null) {
					MultiTextEdit sourceMultiTextEdit = new MultiTextEdit();
					change = new CompilationUnitChange("", sourceICompilationUnit);
					change.setEdit(sourceMultiTextEdit);
					compilationUnitChanges.put(sourceICompilationUnit, change);
				}
				change.getEdit().addChild(sourceEdit);
				String message = "Create constructor in subclass";
				change.addTextEditGroup(new TextEditGroup(message, new TextEdit[] {sourceEdit}));
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
	}

	private void modifySourceClass(CompilationUnit compilationUnit, TypeDeclaration typeDeclaration,
			Set<VariableDeclaration> fieldDeclarationsToBePulledUp, Set<MethodDeclaration> methodDeclarationsToBePulledUp,
			Set<MethodDeclaration> constructorsToBeCopied, Set<VariableDeclaration> accessedFieldDeclarations, Set<VariableDeclaration> assignedFieldDeclarations) {
		if(cloneInfo.intermediateClassName != null && !cloneInfo.extractUtilityClass) {
			modifySuperclassType(compilationUnit, typeDeclaration, cloneInfo.intermediateClassName);
		}
		for(MethodDeclaration constructor : constructorsToBeCopied) {
			addConstructorDeclaration(constructor, typeDeclaration, compilationUnit, cloneInfo.requiredImportTypeBindings);
		}
		removeMethodDeclarations(methodDeclarationsToBePulledUp);
		removeFieldDeclarations(fieldDeclarationsToBePulledUp);
		for(VariableDeclaration variableDeclaration : accessedFieldDeclarations) {
			if(variableDeclaration.getRoot().equals(compilationUnit)) {
				createGetterMethodDeclaration(variableDeclaration, RefactoringUtility.findTypeDeclaration(variableDeclaration), RefactoringUtility.findCompilationUnit(variableDeclaration));
			}
		}
		for(VariableDeclaration variableDeclaration : assignedFieldDeclarations) {
			if(variableDeclaration.getRoot().equals(compilationUnit)) {
				createSetterMethodDeclaration(variableDeclaration, RefactoringUtility.findTypeDeclaration(variableDeclaration), RefactoringUtility.findCompilationUnit(variableDeclaration));
			}
		}
	}

	private void modifySourceCompilationUnitImportDeclarations(CompilationUnit compilationUnit, boolean reuseImportRewrite) {
		try {
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)compilationUnit.getJavaElement();
			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
			ImportRewrite importRewrite = ImportRewrite.create(compilationUnit, true);
			if(cloneInfo.intermediateClassPackageBinding != null) {
				if(compilationUnit.getPackage() != null && !compilationUnit.getPackage().resolveBinding().isEqualTo(cloneInfo.intermediateClassPackageBinding)) {
					importRewrite.addImport(cloneInfo.intermediateClassPackageBinding.getName() +
							"." + cloneInfo.intermediateClassName);
				}
				else if(compilationUnit.getPackage() == null && cloneInfo.intermediateClassPackageBinding != null) {
					importRewrite.addImport(cloneInfo.intermediateClassPackageBinding.getName() +
							"." + cloneInfo.intermediateClassName);
				}
			}
			for(ITypeBinding typeBinding : cloneInfo.requiredImportTypeBindings) {
				if(!typeBinding.isNested())
					importRewrite.addImport(typeBinding);
			}
			
			if(reuseImportRewrite) {
				cloneInfo.importRewrite = importRewrite;
			}
			else {
				TextEdit importEdit = importRewrite.rewriteImports(null);
				if(importRewrite.getCreatedImports().length > 0) {
					change.getEdit().addChild(importEdit);
					change.addTextEditGroup(new TextEditGroup("Add required import declarations", new TextEdit[] {importEdit}));
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private boolean typeContainsPureSetterMethodForVariable(TypeDeclaration typeDeclaration, VariableDeclaration variableDeclaration) {
		for(MethodDeclaration methodDeclaration : typeDeclaration.getMethods()) {
			SimpleName simpleName = MethodDeclarationUtility.isSetter(methodDeclaration);
			if(simpleName != null && simpleName.resolveBinding().isEqualTo(variableDeclaration.resolveBinding())) {
				String setterMethodName = variableDeclaration.getName().getIdentifier();
				setterMethodName = SETTER_PREFIX + setterMethodName.substring(0,1).toUpperCase() + setterMethodName.substring(1,setterMethodName.length());
				if(methodDeclaration.getName().getIdentifier().equals(setterMethodName)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean typeContainsPureGetterMethodForVariable(TypeDeclaration typeDeclaration, VariableDeclaration variableDeclaration) {
		for(MethodDeclaration methodDeclaration : typeDeclaration.getMethods()) {
			SimpleName simpleName = MethodDeclarationUtility.isGetter(methodDeclaration);
			if(simpleName != null && simpleName.resolveBinding().isEqualTo(variableDeclaration.resolveBinding())) {
				String getterMethodName = variableDeclaration.getName().getIdentifier();
				getterMethodName = GETTER_PREFIX + getterMethodName.substring(0,1).toUpperCase() + getterMethodName.substring(1,getterMethodName.length());
				if(methodDeclaration.getName().getIdentifier().equals(getterMethodName)) {
					return true;
				}
			}
		}
		return false;
	}

	private void createSetterMethodDeclaration(VariableDeclaration variableDeclaration, TypeDeclaration typeDeclaration, CompilationUnit compilationUnit) {
		String setterMethodName = variableDeclaration.getName().getIdentifier();
		setterMethodName = SETTER_PREFIX + setterMethodName.substring(0,1).toUpperCase() + setterMethodName.substring(1,setterMethodName.length());
		if(typeContainsPureSetterMethodForVariable(typeDeclaration, variableDeclaration) && !mapper.getMethodName1().equals(setterMethodName) && !mapper.getMethodName2().equals(setterMethodName)) {
			return;
		}
		FieldDeclaration[] fieldDeclarations = typeDeclaration.getFields();
		for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
			List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				if(variableDeclaration.resolveBinding().isEqualTo(fragment.resolveBinding())) {
					ASTRewrite sourceRewriter = ASTRewrite.create(typeDeclaration.getAST());
					AST contextAST = typeDeclaration.getAST();
					MethodDeclaration newMethodDeclaration = contextAST.newMethodDeclaration();
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, contextAST.newPrimitiveType(PrimitiveType.VOID), null);
					ListRewrite methodDeclarationModifiersRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
					methodDeclarationModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
					setterMethodName = appendAccessorMethodSuffix(setterMethodName);
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, contextAST.newSimpleName(setterMethodName), null);
					ListRewrite methodDeclarationParametersRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
					SingleVariableDeclaration parameter = contextAST.newSingleVariableDeclaration();
					sourceRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, fieldDeclaration.getType(), null);
					sourceRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, fragment.getName(), null);
					methodDeclarationParametersRewrite.insertLast(parameter, null);
					Block methodDeclarationBody = contextAST.newBlock();
					ListRewrite methodDeclarationBodyStatementsRewrite = sourceRewriter.getListRewrite(methodDeclarationBody, Block.STATEMENTS_PROPERTY);
					Assignment assignment = contextAST.newAssignment();
					sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, fragment.getName(), null);
					sourceRewriter.set(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.ASSIGN, null);
					FieldAccess fieldAccess = contextAST.newFieldAccess();
					sourceRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, contextAST.newThisExpression(), null);
					sourceRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, fragment.getName(), null);
					sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, fieldAccess, null);
					ExpressionStatement expressionStatement = contextAST.newExpressionStatement(assignment);
					methodDeclarationBodyStatementsRewrite.insertLast(expressionStatement, null);
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.BODY_PROPERTY, methodDeclarationBody, null);
					ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(typeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
					contextBodyRewrite.insertLast(newMethodDeclaration, null);
					try {
						TextEdit sourceEdit = sourceRewriter.rewriteAST();
						ICompilationUnit sourceICompilationUnit = (ICompilationUnit)compilationUnit.getJavaElement();
						CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
						change.getEdit().addChild(sourceEdit);
						change.addTextEditGroup(new TextEditGroup("Create setter method for field " + variableDeclaration.resolveBinding().getName(), new TextEdit[] {sourceEdit}));
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void createGetterMethodDeclaration(VariableDeclaration variableDeclaration, TypeDeclaration typeDeclaration, CompilationUnit compilationUnit) {
		String getterMethodName = variableDeclaration.getName().getIdentifier();
		getterMethodName = GETTER_PREFIX + getterMethodName.substring(0,1).toUpperCase() + getterMethodName.substring(1,getterMethodName.length());
		if(typeContainsPureGetterMethodForVariable(typeDeclaration, variableDeclaration) && !mapper.getMethodName1().equals(getterMethodName) && !mapper.getMethodName2().equals(getterMethodName)) {
			return;
		}
		FieldDeclaration[] fieldDeclarations = typeDeclaration.getFields();
		for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
			List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				if(variableDeclaration.resolveBinding().isEqualTo(fragment.resolveBinding())) {
					ASTRewrite sourceRewriter = ASTRewrite.create(typeDeclaration.getAST());
					AST contextAST = typeDeclaration.getAST();
					MethodDeclaration newMethodDeclaration = contextAST.newMethodDeclaration();
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, fieldDeclaration.getType(), null);
					ListRewrite methodDeclarationModifiersRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
					methodDeclarationModifiersRewrite.insertLast(contextAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
					getterMethodName = appendAccessorMethodSuffix(getterMethodName);
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, contextAST.newSimpleName(getterMethodName), null);
					Block methodDeclarationBody = contextAST.newBlock();
					ListRewrite methodDeclarationBodyStatementsRewrite = sourceRewriter.getListRewrite(methodDeclarationBody, Block.STATEMENTS_PROPERTY);
					FieldAccess fieldAccess = contextAST.newFieldAccess();
					sourceRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, contextAST.newThisExpression(), null);
					sourceRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, fragment.getName(), null);
					ReturnStatement returnStatement = contextAST.newReturnStatement();
					sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, fieldAccess, null);
					methodDeclarationBodyStatementsRewrite.insertLast(returnStatement, null);
					sourceRewriter.set(newMethodDeclaration, MethodDeclaration.BODY_PROPERTY, methodDeclarationBody, null);
					ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(typeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
					contextBodyRewrite.insertLast(newMethodDeclaration, null);
					try {
						TextEdit sourceEdit = sourceRewriter.rewriteAST();
						ICompilationUnit sourceICompilationUnit = (ICompilationUnit)compilationUnit.getJavaElement();
						CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
						change.getEdit().addChild(sourceEdit);
						change.addTextEditGroup(new TextEditGroup("Create getter method for field " + variableDeclaration.resolveBinding().getName(), new TextEdit[] {sourceEdit}));
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private MethodDeclaration typeContainsMethodWithName(TypeDeclaration typeDeclaration, String methodName) {
		for(MethodDeclaration methodDeclaration : typeDeclaration.getMethods()) {
			if(methodDeclaration.getName().getIdentifier().equals(methodName)) {
				return methodDeclaration;
			}
		}
		return null;
	}

	private MethodDeclaration typeContainsMethodWithSignature(TypeDeclaration typeDeclaration, MethodDeclaration methodDeclaration) {
		for(MethodDeclaration methodDeclaration2 : typeDeclaration.getMethods()) {
			if(MethodCallAnalyzer.equalSignature(methodDeclaration2.resolveBinding(), methodDeclaration.resolveBinding()) ||
					MethodCallAnalyzer.equalSignatureIgnoringSubclassTypeDifferences(methodDeclaration2.resolveBinding(), methodDeclaration.resolveBinding())) {
				return methodDeclaration2;
			}
		}
		return null;
	}

	private String appendAccessorMethodSuffix(String accessorMethodName) {
		boolean sourceTypeContainsMethodWithAccessorNameThatIsNotPureAccessor = false;
		for(TypeDeclaration typeDeclaration : sourceTypeDeclarations) {
			MethodDeclaration accessor = typeContainsMethodWithName(typeDeclaration, accessorMethodName);
			if(accessor != null) {
				if(accessorMethodName.startsWith(GETTER_PREFIX)) {
					SimpleName simpleName = MethodDeclarationUtility.isGetter(accessor);
					if(simpleName == null) {
						sourceTypeContainsMethodWithAccessorNameThatIsNotPureAccessor = true;
						break;
					}
				}
				else if(accessorMethodName.startsWith(SETTER_PREFIX)) {
					SimpleName simpleName = MethodDeclarationUtility.isSetter(accessor);
					if(simpleName == null) {
						sourceTypeContainsMethodWithAccessorNameThatIsNotPureAccessor = true;
						break;
					}
				}
			}
		}
		if(mapper.getMethodName1().equals(accessorMethodName) || mapper.getMethodName2().equals(accessorMethodName) || sourceTypeContainsMethodWithAccessorNameThatIsNotPureAccessor) {
			accessorMethodName += ACCESSOR_SUFFIX;
		}
		return accessorMethodName;
	}

	private void removeMethodDeclarations(Set<MethodDeclaration> methodDeclarationsToBePulledUp) {
		TreeSet<MethodDeclaration> orderedMethods = new TreeSet<MethodDeclaration>(new EarliestStartPositionComparator());
		orderedMethods.addAll(methodDeclarationsToBePulledUp);
		List<TextEdit> textEdits = new ArrayList<TextEdit>();
		for(MethodDeclaration methodDeclaration : orderedMethods) {
			TypeDeclaration typeDeclaration = RefactoringUtility.findTypeDeclaration(methodDeclaration);
			CompilationUnit compilationUnit = RefactoringUtility.findCompilationUnit(methodDeclaration);
			if(methodDeclaration.getRoot().equals(compilationUnit)) {
				AST ast = typeDeclaration.getAST();
				ASTRewrite rewriter = ASTRewrite.create(ast);
				ListRewrite bodyRewrite = rewriter.getListRewrite(typeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
				bodyRewrite.remove(methodDeclaration, null);
				try {
					TextEdit sourceEdit = rewriter.rewriteAST();
					if(textEdits.size() > 0) {
						TextEdit previousTextEdit = textEdits.get(textEdits.size()-1);
						for(TextEdit previousChild : previousTextEdit.getChildren()) {
							for(TextEdit currentChild : sourceEdit.getChildren()) {
								if(currentChild.getOffset() == previousChild.getOffset() && currentChild.getLength() == previousChild.getLength()) {
									sourceEdit.removeChild(currentChild);
									break;
								}
							}
						}
					}
					textEdits.add(sourceEdit);
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit)compilationUnit.getJavaElement();
					CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
					if(change == null) {
						MultiTextEdit sourceMultiTextEdit = new MultiTextEdit();
						change = new CompilationUnitChange("", sourceICompilationUnit);
						change.setEdit(sourceMultiTextEdit);
						compilationUnitChanges.put(sourceICompilationUnit, change);
					}
					change.getEdit().addChild(sourceEdit);
					String message = cloneInfo.extractUtilityClass ? "Move method to utility class" : "Pull up method to superclass";
					change.addTextEditGroup(new TextEditGroup(message, new TextEdit[] {sourceEdit}));
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void removeFieldDeclarations(Set<VariableDeclaration> variableDeclarations) {
		TreeSet<VariableDeclaration> orderedFields = new TreeSet<VariableDeclaration>(new EarliestStartPositionComparator());
		orderedFields.addAll(variableDeclarations);
		List<TextEdit> textEdits = new ArrayList<TextEdit>();
		for(VariableDeclaration variableDeclaration : orderedFields) {
			TypeDeclaration typeDeclaration = RefactoringUtility.findTypeDeclaration(variableDeclaration);
			CompilationUnit compilationUnit = RefactoringUtility.findCompilationUnit(variableDeclaration);
			if(variableDeclaration.getRoot().equals(compilationUnit)) {
				boolean found = false;
				AST ast = typeDeclaration.getAST();
				ASTRewrite rewriter = ASTRewrite.create(ast);
				ListRewrite bodyRewrite = rewriter.getListRewrite(typeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
				FieldDeclaration[] fieldDeclarations = typeDeclaration.getFields();
				for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
					List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
					ListRewrite fragmentsRewrite = rewriter.getListRewrite(fieldDeclaration, FieldDeclaration.FRAGMENTS_PROPERTY);
					for(VariableDeclarationFragment fragment : fragments) {
						if(fragment.resolveBinding().isEqualTo(variableDeclaration.resolveBinding())) {
							found = true;
							if(fragments.size() > 1) {
								fragmentsRewrite.remove(fragment, null);
							}
							else {
								bodyRewrite.remove(fieldDeclaration, null);
							}
							break;
						}
					}
					if(found)
						break;
				}
				try {
					TextEdit sourceEdit = rewriter.rewriteAST();
					if(textEdits.size() > 0) {
						TextEdit previousTextEdit = textEdits.get(textEdits.size()-1);
						for(TextEdit previousChild : previousTextEdit.getChildren()) {
							for(TextEdit currentChild : sourceEdit.getChildren()) {
								if(currentChild.getOffset() == previousChild.getOffset() && currentChild.getLength() == previousChild.getLength()) {
									sourceEdit.removeChild(currentChild);
									break;
								}
							}
						}
					}
					textEdits.add(sourceEdit);
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit)compilationUnit.getJavaElement();
					CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
					if(change == null) {
						MultiTextEdit sourceMultiTextEdit = new MultiTextEdit();
						change = new CompilationUnitChange("", sourceICompilationUnit);
						change.setEdit(sourceMultiTextEdit);
						compilationUnitChanges.put(sourceICompilationUnit, change);
					}
					change.getEdit().addChild(sourceEdit);
					String message = cloneInfo.extractUtilityClass ? "Move field to utility class" : "Pull up field to superclass";
					change.addTextEditGroup(new TextEditGroup(message, new TextEdit[] {sourceEdit}));
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void modifySuperclassType(CompilationUnit compilationUnit, TypeDeclaration typeDeclaration, String superclassTypeName) {
		AST ast = typeDeclaration.getAST();
		ASTRewrite superClassTypeRewriter = ASTRewrite.create(ast);
		if(superclassTypeName.contains(".")) {
			String qualifier = superclassTypeName.substring(0, superclassTypeName.lastIndexOf("."));
			String innerType = superclassTypeName.substring(superclassTypeName.lastIndexOf(".") + 1, superclassTypeName.length());
			QualifiedType newQualifiedType = ast.newQualifiedType(ast.newSimpleType(ast.newName(qualifier)), ast.newSimpleName(innerType));
			if(cloneInfo.intermediateClassTypeParameters != null && cloneInfo.intermediateClassTypeParameters.length > 0) {
				ParameterizedType parameterizedType = ast.newParameterizedType(newQualifiedType);
				ListRewrite typeArgumentsRewrite = superClassTypeRewriter.getListRewrite(parameterizedType, ParameterizedType.TYPE_ARGUMENTS_PROPERTY);
				for(ITypeBinding typeArgument : cloneInfo.intermediateClassTypeParameters) {
					typeArgumentsRewrite.insertLast(RefactoringUtility.generateTypeFromTypeBinding(typeArgument, ast, superClassTypeRewriter), null);
				}
				superClassTypeRewriter.set(typeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, parameterizedType, null);
			}
			else {
				superClassTypeRewriter.set(typeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, newQualifiedType, null);
			}
		}
		else {
			SimpleType newSimpleType = ast.newSimpleType(ast.newSimpleName(superclassTypeName));
			if(cloneInfo.intermediateClassTypeParameters != null && cloneInfo.intermediateClassTypeParameters.length > 0) {
				ParameterizedType parameterizedType = ast.newParameterizedType(newSimpleType);
				ListRewrite typeArgumentsRewrite = superClassTypeRewriter.getListRewrite(parameterizedType, ParameterizedType.TYPE_ARGUMENTS_PROPERTY);
				for(ITypeBinding typeArgument : cloneInfo.intermediateClassTypeParameters) {
					typeArgumentsRewrite.insertLast(RefactoringUtility.generateTypeFromTypeBinding(typeArgument, ast, superClassTypeRewriter), null);
				}
				superClassTypeRewriter.set(typeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, parameterizedType, null);
			}
			else {
				superClassTypeRewriter.set(typeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, newSimpleType, null);
			}
		}
		try {
			TextEdit sourceEdit = superClassTypeRewriter.rewriteAST();
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)compilationUnit.getJavaElement();
			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
			change.getEdit().addChild(sourceEdit);
			change.addTextEditGroup(new TextEditGroup("Modify superclass type", new TextEdit[] {sourceEdit}));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void modifySourceMethod(CompilationUnit compilationUnit, MethodDeclaration methodDeclaration, TreeSet<PDGNode> removableNodes,
			TreeSet<PDGNode> remainingNodesMovableBefore, TreeSet<PDGNode> remainingNodesMovableAfter, List<VariableDeclaration> returnedVariables,
			Set<VariableDeclaration> fieldsToBeParameterized, int index) {
		AST ast = methodDeclaration.getAST();
		ASTRewrite methodBodyRewriter = ASTRewrite.create(ast);
		MethodInvocation methodInvocation = ast.newMethodInvocation();
		methodBodyRewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(extractedMethodName), null);
		if(cloneInfo.extractUtilityClass) {
			methodBodyRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(cloneInfo.intermediateClassName), null);
		}
		ListRewrite argumentsRewrite = methodBodyRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
		for(VariableBindingKeyPair parameterName : originalPassedParameters.keySet()) {
			List<VariableDeclaration> variableDeclarations = originalPassedParameters.get(parameterName);
			VariableDeclaration variableDeclaration = variableDeclarations.get(index);
			argumentsRewrite.insertLast(variableDeclaration.getName(), null);
			//create initializer for passed parameter
			Set<VariableDeclaration> declaredVariablesInRemainingNodesDefinedByMappedNodes = index == 0 ? mapper.getDeclaredVariablesInRemainingNodesDefinedByMappedNodesG1() :
				mapper.getDeclaredVariablesInRemainingNodesDefinedByMappedNodesG2();
			if(variableDeclaration.getInitializer() == null && !variableDeclaration.resolveBinding().isParameter() && !variableDeclaration.resolveBinding().isField() &&
					declaredVariablesInRemainingNodesDefinedByMappedNodes.contains(variableDeclaration) &&
					variableDeclaration instanceof VariableDeclarationFragment) {
				if(!sourceMethodDeclarations.get(0).equals(sourceMethodDeclarations.get(1))) {
					Expression initializer = generateDefaultValue(methodBodyRewriter, ast, variableDeclaration.resolveBinding().getType());
					methodBodyRewriter.set((VariableDeclarationFragment)variableDeclaration, VariableDeclarationFragment.INITIALIZER_PROPERTY, initializer, null);
				}
			}
		}
		for(ASTNodeDifference difference : parameterizedDifferenceMap.values()) {
			List<Expression> expressions = new ArrayList<Expression>();
			if(difference.getExpression1() != null) {
				Expression expression1 = difference.getExpression1().getExpression();
				expression1 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression1);
				expressions.add(expression1);
			}
			else {
				expressions.add(null);
			}
			if(difference.getExpression2() != null) {
				Expression expression2 = difference.getExpression2().getExpression();
				expression2 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression2);
				expressions.add(expression2);
			}
			else {
				expressions.add(null);
			}
			Expression expression = expressions.get(index);
			boolean isReturnedVariable = isReturnedVariable(difference);
			List<VariableDeclaration> returnedVariables1 = this.returnedVariables.get(0);
			List<VariableDeclaration> returnedVariables2 = this.returnedVariables.get(1);
			if(!isReturnedVariable ||
					(returnedVariables1.size() == 1 && returnedVariables2.size() == 1 && variableBelongsToParameterizedDifferences(returnedVariables1.get(0), returnedVariables2.get(0)) != null)) {
				if(expression != null) {
					if(difference.containsDifferenceType(DifferenceType.IF_ELSE_SYMMETRICAL_MATCH) && index == 1) {
						ParenthesizedExpression parenthesizedExpression = ast.newParenthesizedExpression();
						methodBodyRewriter.set(parenthesizedExpression, ParenthesizedExpression.EXPRESSION_PROPERTY, expression, null);
						PrefixExpression prefixExpression = ast.newPrefixExpression();
						methodBodyRewriter.set(prefixExpression, PrefixExpression.OPERAND_PROPERTY, parenthesizedExpression, null);
						methodBodyRewriter.set(prefixExpression, PrefixExpression.OPERATOR_PROPERTY, PrefixExpression.Operator.NOT, null);
						argumentsRewrite.insertLast(prefixExpression, null);
					}
					else {
						argumentsRewrite.insertLast(expression, null);
					}
				}
				else {
					argumentsRewrite.insertLast(ast.newThisExpression(), null);
				}
			}
		}
		Set<VariableDeclaration> accessedLocalFields = null;
		ITypeBinding sourceClassTypeBinding = null;
		if(index == 0) {
			accessedLocalFields = getLocallyAccessedFields(mapper.getDirectlyAccessedLocalFieldsG1(), sourceTypeDeclarations.get(0));
			sourceClassTypeBinding = sourceTypeDeclarations.get(0).resolveBinding();
		}
		else {
			accessedLocalFields = getLocallyAccessedFields(mapper.getDirectlyAccessedLocalFieldsG2(), sourceTypeDeclarations.get(1));
			sourceClassTypeBinding = sourceTypeDeclarations.get(1).resolveBinding();
		}
		for(VariableDeclaration variableDeclaration : fieldsToBeParameterized) {
			if(accessedLocalFields.contains(variableDeclaration)) {
				//check if the field is private and declared in another class
				if((variableDeclaration.resolveBinding().getModifiers() & Modifier.PRIVATE) != 0 && !sourceClassTypeBinding.isEqualTo(variableDeclaration.resolveBinding().getDeclaringClass())) {
					TypeDeclaration sourceTypeDeclaration = index == 0 ? sourceTypeDeclarations.get(0) : sourceTypeDeclarations.get(1);
					MethodDeclaration getterDeclaration = RefactoringUtility.findGetterDeclarationForField(variableDeclaration, sourceTypeDeclaration);
					if(getterDeclaration != null) {
						MethodInvocation getterInvocation = ast.newMethodInvocation();
						methodBodyRewriter.set(getterInvocation, MethodInvocation.NAME_PROPERTY, getterDeclaration.getName(), null);
						argumentsRewrite.insertLast(getterInvocation, null);
					}
				}
				else if((variableDeclaration.resolveBinding().getModifiers() & Modifier.STATIC) != 0) {
					argumentsRewrite.insertLast(variableDeclaration.getName(), null);
				}
				else {
					FieldAccess fieldAccess = ast.newFieldAccess();
					methodBodyRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY, ast.newThisExpression(), null);
					methodBodyRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, variableDeclaration.getName(), null);
					argumentsRewrite.insertLast(fieldAccess, null);
				}
			}
		}
		cloneInfo.argumentRewriteList.add(index, argumentsRewrite);
		/*TreeSet<PDGNode> nodesToBeRemoved = new TreeSet<PDGNode>();
		for(PDGNode pdgNode : removableNodes) {
			boolean declaresParameterizedVariable = false;
			Iterator<AbstractVariable> declaredVariableIterator = pdgNode.getDeclaredVariableIterator();
			while(declaredVariableIterator.hasNext()) {
				AbstractVariable variable = declaredVariableIterator.next();
				if(variable instanceof PlainVariable) {
					PlainVariable plainVariable = (PlainVariable)variable;
					for(BindingSignaturePair bindingSignaturePair : parameterizedDifferenceMap.keySet()) {
						BindingSignature bindingSignature = null;
						if(index == 0) {
							bindingSignature = bindingSignaturePair.getSignature1();
						}
						else if(index == 1) {
							bindingSignature = bindingSignaturePair.getSignature2();
						}
						if(bindingSignature != null && bindingSignature.containsOnlyBinding(plainVariable.getVariableBindingKey())) {
							declaresParameterizedVariable = true;
							break;
						}
					}
				}
			}
			if(!declaresParameterizedVariable) {
				nodesToBeRemoved.add(pdgNode);
			}
			else {
				nodesToBePreservedInTheOriginalMethod.get(index).add(pdgNode);
			}
		}*/
		//place the code in the parent block of the first removable node
		Statement firstStatement = /*nodesToBeRemoved*/removableNodes.first().getASTStatement();
		if(firstStatement.getParent() instanceof LabeledStatement) {
			firstStatement = (LabeledStatement)firstStatement.getParent();
		}
		ListRewrite blockRewrite = null;
		if(firstStatement.getParent() instanceof Block) {
			Block parentBlock = (Block)firstStatement.getParent();
			blockRewrite = methodBodyRewriter.getListRewrite(parentBlock, Block.STATEMENTS_PROPERTY);
		}
		else if(firstStatement.getParent() instanceof SwitchStatement) {
			SwitchStatement parentSwitch = (SwitchStatement)firstStatement.getParent();
			blockRewrite = methodBodyRewriter.getListRewrite(parentSwitch, SwitchStatement.STATEMENTS_PROPERTY);
		}
		else if(firstStatement.getParent() instanceof IfStatement) {
			//first statement is directly nested under an else or an if clause
			IfStatement ifStatement = (IfStatement)firstStatement.getParent();
			if(ifStatement.getThenStatement().equals(firstStatement)) {
				Block thenBlock = ast.newBlock();
				methodBodyRewriter.replace(firstStatement, thenBlock, null);
				blockRewrite = methodBodyRewriter.getListRewrite(thenBlock, Block.STATEMENTS_PROPERTY);
			}
			else if(ifStatement.getElseStatement().equals(firstStatement)) {
				Block elseBlock = ast.newBlock();
				methodBodyRewriter.replace(firstStatement, elseBlock, null);
				blockRewrite = methodBodyRewriter.getListRewrite(elseBlock, Block.STATEMENTS_PROPERTY);
			}
		}
		CloneStructureNode root = mapper.getCloneStructureRoot();
		List<CloneStructureNode> processedCloneStructureGapNodes = new ArrayList<CloneStructureNode>();
		Set<PDGNode> remainingMovableNodes = new TreeSet<PDGNode>();
		remainingMovableNodes.addAll(remainingNodesMovableBefore);
		remainingMovableNodes.addAll(remainingNodesMovableAfter);
		List<Statement> statementsToBeMovedBefore = new ArrayList<Statement>();
		List<Statement> statementsToBeMovedAfter = new ArrayList<Statement>();
		for(PDGNode remainingNode : remainingMovableNodes) {
			CloneStructureNode remainingCloneStructureNode = null;
			if(index == 0)
				remainingCloneStructureNode = root.findNodeG1(remainingNode);
			else
				remainingCloneStructureNode = root.findNodeG2(remainingNode);
			if(!processedCloneStructureGapNodes.contains(remainingCloneStructureNode.getParent())) {
				Statement statement = processCloneStructureGapNode(remainingCloneStructureNode, ast, methodBodyRewriter, index);
				if(remainingNodesMovableBefore.contains(remainingNode) && remainingNode.getId() > removableNodes.first().getId()) {
					statementsToBeMovedBefore.add(statement);
					methodBodyRewriter.remove(remainingNode.getASTStatement(), null);
				}
				else if(remainingNodesMovableAfter.contains(remainingNode)) {
					statementsToBeMovedAfter.add(statement);
					methodBodyRewriter.remove(remainingNode.getASTStatement(), null);
				}
			}
			processedCloneStructureGapNodes.add(remainingCloneStructureNode);
			for(CloneStructureNode child : remainingCloneStructureNode.getChildren()) {
				if(child.getMapping() instanceof PDGElseGap)
					processedCloneStructureGapNodes.add(child);
			}
		}
		Statement extractedMethodInvocationStatement = null;
		if(returnedVariables.size() == 1) {
			Statement methodInvocationStatement = null;
			VariableDeclaration variableDeclaration = returnedVariables.get(0);
			if(variableDeclaration.resolveBinding().isParameter() || variableDeclaration.resolveBinding().isField()
					|| statementsToBeMovedBefore.contains(variableDeclaration.getParent()) ||
					(variableIsPassedAsCommonParameter(variableDeclaration) && !variableIsDeclaredInMappedNodes(variableDeclaration, removableNodes))) {
				//create an assignment statement
				ITypeBinding variableTypeBinding = extractTypeBinding(variableDeclaration);
				Assignment assignment = ast.newAssignment();
				methodBodyRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, variableDeclaration.getName(), null);
				ITypeBinding returnTypeBinding = mapper.getReturnTypeBinding();
				if(returnTypeBinding != null && !returnTypeBinding.isEqualTo(variableTypeBinding)) {
					CastExpression castExpression = ast.newCastExpression();
					methodBodyRewriter.set(castExpression, CastExpression.EXPRESSION_PROPERTY, methodInvocation, null);
					Type variableType = RefactoringUtility.generateTypeFromTypeBinding(variableTypeBinding, ast, methodBodyRewriter);
					methodBodyRewriter.set(castExpression, CastExpression.TYPE_PROPERTY, variableType, null);
					methodBodyRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, castExpression, null);
				}
				else {
					methodBodyRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, methodInvocation, null);
				}
				ExpressionStatement expressionStatement = ast.newExpressionStatement(assignment);
				methodInvocationStatement = expressionStatement;
				if(statementsToBeMovedBefore.contains(variableDeclaration.getParent()) && variableDeclaration.getInitializer() == null) {
					methodBodyRewriter.set(variableDeclaration, VariableDeclarationFragment.INITIALIZER_PROPERTY,
							generateDefaultValue(methodBodyRewriter, ast, variableTypeBinding), null);
				}
			}
			else {
				//create a variable declaration statement
				ITypeBinding variableTypeBinding = extractTypeBinding(variableDeclaration);
				boolean makeQualifiedType = RefactoringUtility.hasQualifiedType(variableDeclaration);
				Type variableType = makeQualifiedType ? RefactoringUtility.generateQualifiedTypeFromTypeBinding(variableTypeBinding, ast, methodBodyRewriter) :
					RefactoringUtility.generateTypeFromTypeBinding(variableTypeBinding, ast, methodBodyRewriter);
				VariableDeclarationFragment newFragment = ast.newVariableDeclarationFragment();
				methodBodyRewriter.set(newFragment, VariableDeclarationFragment.NAME_PROPERTY, variableDeclaration.getName(), null);
				ITypeBinding returnTypeBinding = mapper.getReturnTypeBinding();
				if(returnTypeBinding != null && !returnTypeBinding.isEqualTo(variableTypeBinding)) {
					CastExpression castExpression = ast.newCastExpression();
					methodBodyRewriter.set(castExpression, CastExpression.EXPRESSION_PROPERTY, methodInvocation, null);
					methodBodyRewriter.set(castExpression, CastExpression.TYPE_PROPERTY, variableType, null);
					methodBodyRewriter.set(newFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, castExpression, null);
				}
				else {
					methodBodyRewriter.set(newFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, methodInvocation, null);
				}
				VariableDeclarationStatement newVariableDeclarationStatement = ast.newVariableDeclarationStatement(newFragment);
				methodBodyRewriter.set(newVariableDeclarationStatement, VariableDeclarationStatement.TYPE_PROPERTY, variableType, null);
				methodInvocationStatement = newVariableDeclarationStatement;
			}
			if(firstStatement.getParent() instanceof IfStatement) {
				blockRewrite.insertFirst(methodInvocationStatement, null);
			}
			else {
				blockRewrite.insertBefore(methodInvocationStatement, firstStatement, null);
			}
			/*if(nodesToBePreservedInTheOriginalMethod.get(index).isEmpty()) {
				blockRewrite.insertBefore(methodInvocationStatement, firstStatement, null);
			}
			else {
				Statement lastPreservedStatement = nodesToBePreservedInTheOriginalMethod.get(index).last().getASTStatement();
				blockRewrite.insertAfter(methodInvocationStatement, lastPreservedStatement, null);
			}*/
			extractedMethodInvocationStatement = methodInvocationStatement;
		}
		else {
			ITypeBinding returnTypeBinding = mapper.getReturnTypeBinding();
			Statement methodInvocationStatement = null;
			if(returnTypeBinding != null && !returnTypeBinding.getQualifiedName().equals("void")) {
				ReturnStatement returnStatement = ast.newReturnStatement();
				if(returnTypeBinding.isEqualTo(methodDeclaration.getReturnType2().resolveBinding())) {
					methodBodyRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, methodInvocation, null);
				}
				else {
					CastExpression castExpression = ast.newCastExpression();
					methodBodyRewriter.set(castExpression, CastExpression.EXPRESSION_PROPERTY, methodInvocation, null);
					methodBodyRewriter.set(castExpression, CastExpression.TYPE_PROPERTY, methodDeclaration.getReturnType2(), null);
					methodBodyRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, castExpression, null);
				}
				methodInvocationStatement = returnStatement;
			}
			else {
				methodInvocationStatement = ast.newExpressionStatement(methodInvocation);
			}
			if(firstStatement.getParent() instanceof IfStatement) {
				blockRewrite.insertFirst(methodInvocationStatement, null);
			}
			else {
				blockRewrite.insertBefore(methodInvocationStatement, firstStatement, null);
			}
			/*if(nodesToBePreservedInTheOriginalMethod.get(index).isEmpty()) {
				blockRewrite.insertBefore(methodInvocationStatement, firstStatement, null);
			}
			else {
				Statement lastPreservedStatement = nodesToBePreservedInTheOriginalMethod.get(index).last().getASTStatement();
				blockRewrite.insertAfter(methodInvocationStatement, lastPreservedStatement, null);
			}*/
			extractedMethodInvocationStatement = methodInvocationStatement;
		}
		for(Statement movedBefore : statementsToBeMovedBefore) {
			blockRewrite.insertBefore(movedBefore, extractedMethodInvocationStatement, null);
		}
		for(int i=statementsToBeMovedAfter.size()-1; i>=0; i--) {
			Statement movedAfter = statementsToBeMovedAfter.get(i);
			blockRewrite.insertAfter(movedAfter, extractedMethodInvocationStatement, null);
		}
		for(PDGNode pdgNode : /*nodesToBeRemoved*/removableNodes) {
			Statement statement = pdgNode.getASTStatement();
			if(statement.equals(firstStatement) && statement.getParent() instanceof IfStatement) {
				continue;
			}
			methodBodyRewriter.remove(statement, null);
		}
		Set<LabeledStatement> labeledStatements = labeledStatementsToBeRemoved.get(index);
		for(LabeledStatement labeled : labeledStatements) {
			methodBodyRewriter.remove(labeled, null);
		}
		cloneInfo.originalMethodBodyRewriteList.add(index, methodBodyRewriter);
	}

	private void finalizeOriginalMethod(CompilationUnit compilationUnit, ASTRewrite methodBodyRewriter) {
		try {
			TextEdit sourceEdit = methodBodyRewriter.rewriteAST();
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)compilationUnit.getJavaElement();
			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
			change.getEdit().addChild(sourceEdit);
			change.addTextEditGroup(new TextEditGroup("Modify source method", new TextEdit[] {sourceEdit}));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private boolean isReturnedVariable(ASTNodeDifference difference) {
		AbstractExpression expression1 = difference.getExpression1();
		AbstractExpression expression2 = difference.getExpression2();
		boolean isReturnedVariable = false;
		if(expression1 != null) {
			isReturnedVariable = isReturnedVariable(expression1.getExpression(), this.returnedVariables.get(0));
		}
		else if(expression2 != null) {
			isReturnedVariable = isReturnedVariable(expression2.getExpression(), this.returnedVariables.get(1));
		}
		return isReturnedVariable;
	}

	private boolean isReturnedVariable(Expression expression, List<VariableDeclaration> returnedVariables) {
		IBinding binding = null;
		if(expression instanceof SimpleName) {
			SimpleName simpleName = (SimpleName)expression;
			binding = simpleName.resolveBinding();
		}
		for(VariableDeclaration returnedVariable : returnedVariables) {
			if(returnedVariable.resolveBinding().isEqualTo(binding)) {
				return true;
			}
		}
		return false;
	}

	private void addImportDeclaration(ITypeBinding typeBinding, CompilationUnit targetCompilationUnit, ASTRewrite targetRewriter) {
		String qualifiedName = typeBinding.getQualifiedName();
		String qualifiedPackageName = "";
		if(qualifiedName.contains("."))
			qualifiedPackageName = qualifiedName.substring(0,qualifiedName.lastIndexOf("."));
		String sourcePackageDeclarationName = "";
		if(cloneInfo.intermediateClassPackageBinding != null) {
			sourcePackageDeclarationName = cloneInfo.intermediateClassPackageBinding.getName();
		}
		else {
			PackageDeclaration sourcePackageDeclaration = sourceCompilationUnits.get(0).getPackage();
			if(sourcePackageDeclaration != null)
				sourcePackageDeclarationName = sourcePackageDeclaration.getName().getFullyQualifiedName();
		}
		if(!qualifiedPackageName.equals("") && !qualifiedPackageName.equals("java.lang") &&
				!qualifiedPackageName.equals(sourcePackageDeclarationName) && !typeBinding.isNested()) {
			List<ImportDeclaration> importDeclarationList = targetCompilationUnit.imports();
			boolean found = false;
			for(ImportDeclaration importDeclaration : importDeclarationList) {
				if(!importDeclaration.isOnDemand()) {
					if(qualifiedName.equals(importDeclaration.getName().getFullyQualifiedName())) {
						found = true;
						break;
					}
				}
				else {
					if(qualifiedPackageName.equals(importDeclaration.getName().getFullyQualifiedName())) {
						found = true;
						break;
					}
				}
			}
			if(!found) {
				AST ast = targetCompilationUnit.getAST();
				ImportDeclaration importDeclaration = ast.newImportDeclaration();
				targetRewriter.set(importDeclaration, ImportDeclaration.NAME_PROPERTY, ast.newName(qualifiedName), null);
				ListRewrite importRewrite = targetRewriter.getListRewrite(targetCompilationUnit, CompilationUnit.IMPORTS_PROPERTY);
				importRewrite.insertLast(importDeclaration, null);
			}
		}
	}

	private int findExistingParametersWithArgName() {
		int arg = 0;
		for(MethodDeclaration sourceMethodDeclaration : this.sourceMethodDeclarations) {
			List<SingleVariableDeclaration> parameters = sourceMethodDeclaration.parameters();
			for(SingleVariableDeclaration parameter : parameters) {
				String parameterName = parameter.getName().getIdentifier();
				if(parameterName.startsWith("arg")) {
					try {
						int value = Integer.parseInt(parameterName.substring(3));
						if(value > arg) {
							arg = value;
						}
					}
					catch(NumberFormatException e) {
					}
				}
			}
		}
		return arg;
	}

	/*private void processTryStatement(TryStatement tryStatement) {
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
	}*/

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		try {
			pm.beginTask("Checking preconditions...", 2);
			AbstractMethodDeclaration methodObject1 = mapper.getPDG1().getMethod();
			AbstractMethodDeclaration methodObject2 = mapper.getPDG2().getMethod();
			MethodDeclaration methodDeclaration1 = methodObject1.getMethodDeclaration();
			MethodDeclaration methodDeclaration2 = methodObject2.getMethodDeclaration();
			if(!(methodDeclaration1.getParent() instanceof TypeDeclaration) || !(methodDeclaration2.getParent() instanceof TypeDeclaration)) { 
				status.merge(RefactoringStatus.createErrorStatus("At least one of the clone fragments is inside an enum or annotation type declaration"));
			}
			for(PreconditionViolation violation : mapper.getPreconditionViolations()) {
				if(violation instanceof StatementPreconditionViolation) {
					StatementPreconditionViolation statementViolation = (StatementPreconditionViolation)violation;
					Statement statement = statementViolation.getStatement().getStatement();
					CompilationUnit cu = (CompilationUnit)statement.getRoot();
					RefactoringStatusContext context = JavaStatusContext.create(cu.getTypeRoot(), statement);
					status.merge(RefactoringStatus.createErrorStatus(violation.getViolation(), context));
				}
				else if(violation instanceof ExpressionPreconditionViolation) {
					ExpressionPreconditionViolation expressionViolation = (ExpressionPreconditionViolation)violation;
					Expression expression = expressionViolation.getExpression().getExpression();
					expression = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression);
					CompilationUnit cu = (CompilationUnit)expression.getRoot();
					RefactoringStatusContext context = JavaStatusContext.create(cu.getTypeRoot(), expression);
					status.merge(RefactoringStatus.createErrorStatus(violation.getViolation(), context));
				}
				else if(violation instanceof DualExpressionPreconditionViolation) {
					DualExpressionPreconditionViolation dualExpressionViolation = (DualExpressionPreconditionViolation)violation;
					Expression expression1 = dualExpressionViolation.getExpression1().getExpression();
					expression1 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression1);
					CompilationUnit cu1 = (CompilationUnit)expression1.getRoot();
					RefactoringStatusContext context1 = JavaStatusContext.create(cu1.getTypeRoot(), expression1);
					status.merge(RefactoringStatus.createErrorStatus(violation.getViolation(), context1));
					
					Expression expression2 = dualExpressionViolation.getExpression2().getExpression();
					expression2 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression2);
					CompilationUnit cu2 = (CompilationUnit)expression2.getRoot();
					RefactoringStatusContext context2 = JavaStatusContext.create(cu2.getTypeRoot(), expression2);
					status.merge(RefactoringStatus.createErrorStatus(violation.getViolation(), context2));
				}
				else if(violation instanceof ReturnedVariablePreconditionViolation) {
					status.merge(RefactoringStatus.createErrorStatus(violation.getViolation()));
				}
				else if(violation instanceof UncommonSuperclassPreconditionViolation) {
					status.merge(RefactoringStatus.createErrorStatus(violation.getViolation()));
				}
				else if(violation instanceof ZeroMatchedStatementsPreconditionViolation) {
					status.merge(RefactoringStatus.createErrorStatus(violation.getViolation()));
				}
				else if(violation instanceof NotAllPossibleExecutionFlowsEndInReturnPreconditionViolation) {
					status.merge(RefactoringStatus.createErrorStatus(violation.getViolation()));
				}
			}
			if(status.getEntries().length == 0) {
				apply();
			}
			else {
				initialize();
			}
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
			final Collection<Change> changes = new ArrayList<Change>();
			changes.addAll(compilationUnitChanges.values());
			changes.addAll(createCompilationUnitChanges.values());
			CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {
				@Override
				public ChangeDescriptor getDescriptor() {
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnits.get(0).getJavaElement();
					String project = sourceICompilationUnit.getJavaProject().getElementName();
					String description = MessageFormat.format("Extract Clone in class ''{0}''", new Object[] { sourceICompilationUnit.getElementName() });
					String comment = null;
					return new RefactoringChangeDescriptor(new ExtractCloneRefactoringDescriptor(project, description, comment, mappers));
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
