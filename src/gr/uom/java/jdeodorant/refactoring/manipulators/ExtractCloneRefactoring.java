package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.AbstractMethodDeclaration;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.CatchClauseObject;
import gr.uom.java.ast.decomposition.CompositeStatementObject;
import gr.uom.java.ast.decomposition.StatementObject;
import gr.uom.java.ast.decomposition.TryStatementObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CFGBranchDoLoopNode;
import gr.uom.java.ast.decomposition.cfg.CFGNode;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGExitNode;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.decomposition.cfg.mapping.CloneStructureNode;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGElseGap;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGElseMapping;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGNodeGap;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGNodeMapping;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGSubTreeMapper;
import gr.uom.java.ast.decomposition.cfg.mapping.StatementCollector;
import gr.uom.java.ast.decomposition.cfg.mapping.VariableBindingKeyPair;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.DualExpressionPreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.ExpressionPreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.PreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.ReturnedVariablePreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.StatementPreconditionViolation;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.matching.BindingSignature;
import gr.uom.java.ast.decomposition.matching.BindingSignaturePair;
import gr.uom.java.ast.decomposition.matching.DifferenceType;
import gr.uom.java.ast.decomposition.matching.FieldAccessReplacedWithGetterInvocationDifference;
import gr.uom.java.ast.decomposition.matching.FieldAssignmentReplacedWithSetterInvocationDifference;
import gr.uom.java.ast.util.ExpressionExtractor;
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
import org.eclipse.jdt.core.IType;
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
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
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
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
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
	private List<PDGSubTreeMapper> mappers;
	private PDGSubTreeMapper mapper;
	private List<CompilationUnit> sourceCompilationUnits;
	private List<TypeDeclaration> sourceTypeDeclarations;
	private List<MethodDeclaration> sourceMethodDeclarations;
	private List<Set<VariableDeclaration>> fieldDeclarationsToBePulledUp;
	private List<Set<VariableDeclaration>> fieldDeclarationsToBeParameterized;
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
	//private List<TreeSet<PDGNode>> nodesToBePreservedInTheOriginalMethod;
	private CloneInformation cloneInfo;
	
	private class CloneInformation {
		private ICompilationUnit sourceICompilationUnit;
		private CompilationUnit sourceCompilationUnit;
		private TypeDeclaration sourceTypeDeclaration;
		private AST ast;
		private IFile file;
		private ASTRewrite sourceRewriter;
		private Document document;
		private Set<ITypeBinding> requiredImportTypeBindings;
		private ListRewrite methodBodyRewrite;
		private ListRewrite parameterRewrite;
		private List<ListRewrite> argumentRewriteList = new ArrayList<ListRewrite>();
		private List<ASTRewrite> originalMethodBodyRewriteList = new ArrayList<ASTRewrite>();
		private boolean superclassNotDirectlyInheritedFromRefactoredSubclasses;
		private boolean extractUtilityClass;
		private String intermediateClassName;
		private IPackageBinding intermediateClassPackageBinding;
	}
	
	public ExtractCloneRefactoring(List<PDGSubTreeMapper> mappers) {
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

	public List<PDGSubTreeMapper> getMappers() {
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
		returnedVariables.add(new ArrayList<VariableDeclaration>(this.mapper.getDeclaredVariablesInMappedNodesUsedByNonMappedNodesG1()));
		returnedVariables.add(new ArrayList<VariableDeclaration>(this.mapper.getDeclaredVariablesInMappedNodesUsedByNonMappedNodesG2()));
		this.fieldDeclarationsToBePulledUp = new ArrayList<Set<VariableDeclaration>>();
		this.fieldDeclarationsToBeParameterized = new ArrayList<Set<VariableDeclaration>>();
		this.methodDeclarationsToBePulledUp = new ArrayList<Set<MethodDeclaration>>();
		this.labeledStatementsToBeRemoved = new ArrayList<Set<LabeledStatement>>();
		//this.nodesToBePreservedInTheOriginalMethod = new ArrayList<TreeSet<PDGNode>>();
		for(int i=0; i<2; i++) {
			fieldDeclarationsToBePulledUp.add(new LinkedHashSet<VariableDeclaration>());
			fieldDeclarationsToBeParameterized.add(new LinkedHashSet<VariableDeclaration>());
			methodDeclarationsToBePulledUp.add(new LinkedHashSet<MethodDeclaration>());
			labeledStatementsToBeRemoved.add(new LinkedHashSet<LabeledStatement>());
			//nodesToBePreservedInTheOriginalMethod.add(new TreeSet<PDGNode>());
		}
		this.compilationUnitChanges = new LinkedHashMap<ICompilationUnit, CompilationUnitChange>();
		this.createCompilationUnitChanges = new LinkedHashMap<ICompilationUnit, CreateCompilationUnitChange>();
		this.javaElementsToOpenInEditor = new LinkedHashSet<IJavaElement>();
		
		this.sourceMethodDeclarations.add(methodDeclaration1);
		this.sourceMethodDeclarations.add(methodDeclaration2);
		this.sourceTypeDeclarations.add((TypeDeclaration)methodDeclaration1.getParent());
		this.sourceTypeDeclarations.add((TypeDeclaration)methodDeclaration2.getParent());
		this.sourceCompilationUnits.add((CompilationUnit)methodDeclaration1.getRoot());
		this.sourceCompilationUnits.add((CompilationUnit)methodDeclaration2.getRoot());
		this.originalPassedParameters = new LinkedHashMap<VariableBindingKeyPair, ArrayList<VariableDeclaration>>();
		this.parameterizedDifferenceMap = new LinkedHashMap<BindingSignaturePair, ASTNodeDifference>();
		this.sortedNodeMappings = new TreeSet<PDGNodeMapping>(this.mapper.getMaximumStateWithMinimumDifferences().getNodeMappings());
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
	}

	public Set<IJavaElement> getJavaElementsToOpenInEditor() {
		return javaElementsToOpenInEditor;
	}

	public PDGSubTreeMapper getMapper() {
		return mapper;
	}

	public void setMapper(PDGSubTreeMapper mapper) {
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
		for(int i=0; i<sourceCompilationUnits.size(); i++) {
			modifySourceClass(sourceCompilationUnits.get(i), sourceTypeDeclarations.get(i), fieldDeclarationsToBePulledUp.get(i), methodDeclarationsToBePulledUp.get(i));
			modifySourceMethod(sourceCompilationUnits.get(i), sourceMethodDeclarations.get(i), removableStatements.get(i),
					remainingStatementsMovableBefore.get(i), remainingStatementsMovableAfter.get(i), returnedVariables.get(i), fieldDeclarationsToBeParameterized.get(i), i);
		}
		finalizeCloneExtraction();
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
	
	private boolean variableIsPassedAsCommonParameter(VariableDeclaration variableDeclaration1, VariableDeclaration variableDeclaration2) {
		for(VariableBindingKeyPair pair : mapper.getCommonPassedParameters().keySet()) {
			if(pair.getKey1().equals(variableDeclaration1.resolveBinding().getKey()) &&
					pair.getKey2().equals(variableDeclaration2.resolveBinding().getKey())) {
				return true;
			}
		}
		return false;
	}

	private ITypeBinding findReturnTypeBinding() {
		List<ITypeBinding> returnedTypeBindings1 = new ArrayList<ITypeBinding>();
		List<ITypeBinding> returnedTypeBindings2 = new ArrayList<ITypeBinding>();
		for(PDGNodeMapping pdgNodeMapping : sortedNodeMappings) {
			PDGNode pdgNode1 = pdgNodeMapping.getNodeG1();
			extractReturnTypeBinding(pdgNode1, returnedTypeBindings1);
			PDGNode pdgNode2 = pdgNodeMapping.getNodeG2();
			extractReturnTypeBinding(pdgNode2, returnedTypeBindings2);
		}
		if(returnedTypeBindings1.size() == 1 && returnedTypeBindings2.size() == 1) {
			ITypeBinding typeBinding1 = returnedTypeBindings1.get(0);
			ITypeBinding typeBinding2 = returnedTypeBindings2.get(0);
			if(typeBinding1.isEqualTo(typeBinding2))
				return typeBinding1;
			else
				return ASTNodeMatcher.commonSuperType(typeBinding1, typeBinding2);
		}
		return null;
	}

	private void extractReturnTypeBinding(PDGNode pdgNode, List<ITypeBinding> returnedTypeBindings) {
		if(pdgNode instanceof PDGExitNode) {
			PDGExitNode exitNode = (PDGExitNode)pdgNode;
			ReturnStatement returnStatement = (ReturnStatement)exitNode.getASTStatement();
			Expression returnedExpression = returnStatement.getExpression();
			if(returnedExpression != null && !(returnedExpression instanceof NullLiteral)) {
				ITypeBinding typeBinding = returnedExpression.resolveTypeBinding();
				if(typeBinding != null) {
					boolean alreadyContained = false;
					for(ITypeBinding binding : returnedTypeBindings) {
						if(binding.isEqualTo(typeBinding)) {
							alreadyContained = true;
							break;
						}
					}
					if(!alreadyContained)
						returnedTypeBindings.add(typeBinding);
				}
			}
		}
	}

	private Set<VariableDeclaration> getLocallyAccessedFields(Set<AbstractVariable> accessedFields, TypeDeclaration typeDeclaration) {
		Set<VariableDeclaration> accessedLocalFields = new LinkedHashSet<VariableDeclaration>();
		for(AbstractVariable variable : accessedFields) {
			VariableDeclaration fieldDeclaration = findFieldDeclaration(variable, typeDeclaration);
			if(fieldDeclaration != null) {
				accessedLocalFields.add(fieldDeclaration);
			}
		}
		return accessedLocalFields;
	}

	private VariableDeclaration findFieldDeclaration(AbstractVariable variable, TypeDeclaration typeDeclaration) {
		for(FieldDeclaration fieldDeclaration : typeDeclaration.getFields()) {
			List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				if(variable.getVariableBindingKey().equals(fragment.resolveBinding().getKey())) {
					return fragment;
				}
			}
		}
		//fragment was not found in typeDeclaration
		Type superclassType = typeDeclaration.getSuperclassType();
		if(superclassType != null) {
			String superclassQualifiedName = superclassType.resolveBinding().getQualifiedName();
			SystemObject system = ASTReader.getSystemObject();
			ClassObject superclassObject = system.getClassObject(superclassQualifiedName);
			if(superclassObject != null) {
				AbstractTypeDeclaration superclassTypeDeclaration = superclassObject.getAbstractTypeDeclaration();
				if(superclassTypeDeclaration instanceof TypeDeclaration) {
					return findFieldDeclaration(variable, (TypeDeclaration)superclassTypeDeclaration);
				}
			}
		}
		return null;
	}

	private void extractClone() {
		this.cloneInfo = new CloneInformation();
		ITypeBinding commonSuperTypeOfSourceTypeDeclarations = null;
		if(sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclarations.get(1).resolveBinding())) {
			cloneInfo.sourceCompilationUnit = sourceCompilationUnits.get(0);
			cloneInfo.sourceICompilationUnit = (ICompilationUnit)cloneInfo.sourceCompilationUnit.getJavaElement();
			cloneInfo.sourceTypeDeclaration = sourceTypeDeclarations.get(0);
			cloneInfo.sourceRewriter = ASTRewrite.create(cloneInfo.sourceTypeDeclaration.getAST());
			cloneInfo.ast = cloneInfo.sourceTypeDeclaration.getAST();
		}
		else {
			//check if they have a common superclass
			ITypeBinding typeBinding1 = sourceTypeDeclarations.get(0).resolveBinding();
			ITypeBinding typeBinding2 = sourceTypeDeclarations.get(1).resolveBinding();
			commonSuperTypeOfSourceTypeDeclarations = ASTNodeMatcher.commonSuperType(typeBinding1, typeBinding2);
			if(commonSuperTypeOfSourceTypeDeclarations != null) {
				if(pullUpToCommonSuperclass(commonSuperTypeOfSourceTypeDeclarations, typeBinding1, typeBinding2)) {
					IJavaElement javaElement = commonSuperTypeOfSourceTypeDeclarations.getJavaElement();
					javaElementsToOpenInEditor.add(javaElement);
					ICompilationUnit iCompilationUnit = (ICompilationUnit)javaElement.getParent();
					ASTParser parser = ASTParser.newParser(AST.JLS4);
					parser.setKind(ASTParser.K_COMPILATION_UNIT);
					parser.setSource(iCompilationUnit);
					parser.setResolveBindings(true); // we need bindings later on
					CompilationUnit compilationUnit = (CompilationUnit)parser.createAST(null);
					List<AbstractTypeDeclaration> typeDeclarations = compilationUnit.types();
					for(AbstractTypeDeclaration abstractTypeDeclaration : typeDeclarations) {
						if(abstractTypeDeclaration instanceof TypeDeclaration) {
							TypeDeclaration typeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
							if(typeDeclaration.resolveBinding().isEqualTo(commonSuperTypeOfSourceTypeDeclarations)) {
								cloneInfo.sourceCompilationUnit = compilationUnit;
								cloneInfo.sourceICompilationUnit = iCompilationUnit;
								cloneInfo.sourceTypeDeclaration = typeDeclaration;
								cloneInfo.sourceRewriter = ASTRewrite.create(cloneInfo.sourceTypeDeclaration.getAST());
								cloneInfo.ast = cloneInfo.sourceTypeDeclaration.getAST();
								cloneInfo.superclassNotDirectlyInheritedFromRefactoredSubclasses =
										!superclassDirectlyInheritedFromRefactoredSubclasses(commonSuperTypeOfSourceTypeDeclarations,
										typeBinding1, typeBinding2);
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
					if(extractToUtilityClass(commonSuperTypeOfSourceTypeDeclarations)) {
						cloneInfo.extractUtilityClass = true;
					}
					if(cloneInfo.extractUtilityClass) {
						cloneInfo.intermediateClassName = "Utility";
					}
					else {
						cloneInfo.intermediateClassName = "Intermediate" + commonSuperTypeOfSourceTypeDeclarations.getName();
					}
					ClassObject commonSuperType = ASTReader.getSystemObject().getClassObject(commonSuperTypeOfSourceTypeDeclarations.getQualifiedName());
					CompilationUnit compilationUnit = null;
					if(commonSuperType != null) {
						compilationUnit = findCompilationUnit(commonSuperType.getAbstractTypeDeclaration());
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
					ASTParser intermediateParser = ASTParser.newParser(AST.JLS4);
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
						ListRewrite intermediateModifiersRewrite = intermediateRewriter.getListRewrite(intermediateTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
						intermediateModifiersRewrite.insertLast(intermediateAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
						if(!cloneInfo.extractUtilityClass) {
							intermediateModifiersRewrite.insertLast(intermediateAST.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
							intermediateRewriter.set(intermediateTypeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY,
									intermediateAST.newSimpleType(intermediateAST.newSimpleName(commonSuperTypeOfSourceTypeDeclarations.getName())), null);
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
		Set<ITypeBinding> requiredImportTypeBindings = new LinkedHashSet<ITypeBinding>();
		ListRewrite bodyDeclarationsRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		if(commonSuperTypeOfSourceTypeDeclarations != null) {
			Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
			typeBindings.add(commonSuperTypeOfSourceTypeDeclarations);
			getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
		}
		Set<VariableDeclaration> accessedLocalFieldsG1 = getLocallyAccessedFields(mapper.getDirectlyAccessedLocalFieldsG1(), sourceTypeDeclarations.get(0));
		Set<VariableDeclaration> accessedLocalFieldsG2 = getLocallyAccessedFields(mapper.getDirectlyAccessedLocalFieldsG2(), sourceTypeDeclarations.get(1));
		if(!sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclarations.get(1).resolveBinding())) {
			pullUpLocallyAccessedFields(accessedLocalFieldsG1, accessedLocalFieldsG2, bodyDeclarationsRewrite, requiredImportTypeBindings);

			Set<VariableDeclaration> indirectlyAccessedLocalFieldsG1 = getLocallyAccessedFields(mapper.getIndirectlyAccessedLocalFieldsG1(), sourceTypeDeclarations.get(0));
			Set<VariableDeclaration> indirectlyAccessedLocalFieldsG2 = getLocallyAccessedFields(mapper.getIndirectlyAccessedLocalFieldsG2(), sourceTypeDeclarations.get(1));
			Set<MethodObject> accessedLocalMethodsG1 = mapper.getAccessedLocalMethodsG1();
			Set<MethodObject> accessedLocalMethodsG2 = mapper.getAccessedLocalMethodsG2();
			for(MethodObject localMethodG1 : accessedLocalMethodsG1) {
				for(MethodObject localMethodG2 : accessedLocalMethodsG2) {
					if(localMethodG1.getName().equals(localMethodG2.getName()) &&
							localMethodG1.getReturnType().equals(localMethodG2.getReturnType()) &&
							localMethodG1.getParameterTypeList().equals(localMethodG2.getParameterTypeList())) {
						MethodDeclaration methodDeclaration1 = localMethodG1.getMethodDeclaration();
						MethodDeclaration methodDeclaration2 = localMethodG2.getMethodDeclaration();
						Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
						boolean clones = type2Clones(methodDeclaration1, methodDeclaration2);
						Type returnType = methodDeclaration1.getReturnType2();
						TypeDeclaration typeDeclaration1 = findTypeDeclaration(methodDeclaration1);
						TypeDeclaration typeDeclaration2 = findTypeDeclaration(methodDeclaration2);
						Set<VariableDeclaration> fieldsAccessedInMethod1 = getFieldsAccessedInMethod(indirectlyAccessedLocalFieldsG1, methodDeclaration1);
						Set<VariableDeclaration> fieldsAccessedInMethod2 = getFieldsAccessedInMethod(indirectlyAccessedLocalFieldsG2, methodDeclaration2);
						boolean avoidPullUpDueToSerialization1 = avoidPullUpMethodDueToSerialization(sourceTypeDeclarations.get(0), fieldsAccessedInMethod1);
						boolean avoidPullUpDueToSerialization2 = avoidPullUpMethodDueToSerialization(sourceTypeDeclarations.get(1), fieldsAccessedInMethod2);
						if(clones && !avoidPullUpDueToSerialization1 && !avoidPullUpDueToSerialization2) {
							//check if the common superclass is one of the source classes
							if(!typeDeclaration1.resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding()) &&
									!typeDeclaration2.resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
								bodyDeclarationsRewrite.insertLast(methodDeclaration1, null);
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
								pullUpLocallyAccessedFields(fieldsAccessedInMethod1, fieldsAccessedInMethod2, bodyDeclarationsRewrite, requiredImportTypeBindings);
							}
							if(!typeDeclaration1.resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
								methodDeclarationsToBePulledUp.get(0).add(methodDeclaration1);
							}
							if(!typeDeclaration2.resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
								methodDeclarationsToBePulledUp.get(1).add(methodDeclaration2);
							}
						}
						else {
							if(!typeDeclaration1.resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding()) &&
									!typeDeclaration2.resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
								MethodDeclaration newMethodDeclaration = ast.newMethodDeclaration();
								sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, ast.newSimpleName(methodDeclaration1.getName().getIdentifier()), null);
								sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnType, null);
								typeBindings.add(returnType.resolveBinding());
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
											modifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
											if((methodDeclaration2.getModifiers() & Modifier.PUBLIC) == 0) {
												updateAccessModifier(methodDeclaration2, Modifier.ModifierKeyword.PUBLIC_KEYWORD);
											}
										}
									}
								}
								if(cloneInfo.superclassNotDirectlyInheritedFromRefactoredSubclasses) {
									Block methodBody = ast.newBlock();
									sourceRewriter.set(newMethodDeclaration, MethodDeclaration.BODY_PROPERTY, methodBody, null);
									//create a default return statement
									Expression returnedExpression = generateDefaultValue(sourceRewriter, ast, returnType);
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
								List<SingleVariableDeclaration> parameters = methodDeclaration1.parameters();
								for(SingleVariableDeclaration parameter : parameters) {
									parametersRewrite.insertLast(parameter, null);
									typeBindings.add(parameter.getType().resolveBinding());
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
						getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
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
		ITypeBinding returnTypeBinding = null;
		if(returnedVariables1.size() == 1 && returnedVariables2.size() == 1) {
			Type returnType1 = extractType(returnedVariables1.get(0));
			Type returnType2 = extractType(returnedVariables2.get(0));
			if(returnType1.resolveBinding().isEqualTo(returnType2.resolveBinding()))
				returnTypeBinding = returnType1.resolveBinding();
			else
				returnTypeBinding = ASTNodeMatcher.commonSuperType(returnType1.resolveBinding(), returnType2.resolveBinding());
		}
		else {
			returnTypeBinding = findReturnTypeBinding();
		}
		if(returnTypeBinding != null) {
			Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
			typeBindings.add(returnTypeBinding);
			getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
			Type returnType = generateTypeFromTypeBinding(returnTypeBinding, ast, sourceRewriter);
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
		
		if(((sourceMethodDeclarations.get(0).getModifiers() & Modifier.STATIC) != 0 &&
				(sourceMethodDeclarations.get(1).getModifiers() & Modifier.STATIC) != 0) || cloneInfo.extractUtilityClass) {
			Modifier staticModifier = newMethodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD);
			modifierRewrite.insertLast(staticModifier, null);
		}
		
		ListRewrite thrownExceptionRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
		List<Name> thrownExceptions1 = sourceMethodDeclarations.get(0).thrownExceptions();
		List<Name> thrownExceptions2 = sourceMethodDeclarations.get(1).thrownExceptions();
		for(Name thrownException1 : thrownExceptions1) {
			for(Name thrownException2 : thrownExceptions2) {
				if(thrownException1.resolveTypeBinding().isEqualTo(thrownException2.resolveTypeBinding())) {
					thrownExceptionRewrite.insertLast(thrownException1, null);
					break;
				}
			}
		}
		
		ListRewrite parameterRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		Map<VariableBindingKeyPair, ArrayList<VariableDeclaration>> commonPassedParameters = mapper.getCommonPassedParameters();
		for(VariableBindingKeyPair parameterName : commonPassedParameters.keySet()) {
			ArrayList<VariableDeclaration> variableDeclarations = commonPassedParameters.get(parameterName);
			VariableDeclaration variableDeclaration1 = variableDeclarations.get(0);
			VariableDeclaration variableDeclaration2 = variableDeclarations.get(1);
			if(parameterIsUsedByNodesWithoutDifferences(variableDeclaration1, variableDeclaration2)) {
				if(!variableDeclaration1.resolveBinding().isField() && !variableDeclaration2.resolveBinding().isField()) {
					ITypeBinding typeBinding1 = extractType(variableDeclaration1).resolveBinding();
					ITypeBinding typeBinding2 = extractType(variableDeclaration2).resolveBinding();
					ITypeBinding typeBinding = null;
					if(!typeBinding1.isEqualTo(typeBinding2)) {
						ITypeBinding commonSuperTypeBinding = ASTNodeMatcher.commonSuperType(typeBinding1, typeBinding2);
						if(commonSuperTypeBinding != null) {
							typeBinding = commonSuperTypeBinding;
						}
					}
					else {
						typeBinding = typeBinding1;
					}
					Type variableType = generateTypeFromTypeBinding(typeBinding, ast, sourceRewriter);
					Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
					typeBindings.add(typeBinding);
					getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
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
		for(PDGNodeMapping pdgNodeMapping : sortedNodeMappings) {
			PDGNode pdgNode1 = pdgNodeMapping.getNodeG1();
			Statement statement1 = pdgNode1.getASTStatement();
			TypeVisitor typeVisitor1 = new TypeVisitor();
			statement1.accept(typeVisitor1);
			getSimpleTypeBindings(typeVisitor1.getTypeBindings(), requiredImportTypeBindings);
			
			PDGNode pdgNode2 = pdgNodeMapping.getNodeG2();
			Statement statement2 = pdgNode2.getASTStatement();
			TypeVisitor typeVisitor2 = new TypeVisitor();
			statement2.accept(typeVisitor2);
			getSimpleTypeBindings(typeVisitor2.getTypeBindings(), requiredImportTypeBindings);
		}
		
		CloneStructureNode root = mapper.getCloneStructureRoot();
		for(CloneStructureNode child : root.getChildren()) {
			if(processableNode(child)) {
				Statement statement = processCloneStructureNode(child, ast, sourceRewriter);
				methodBodyRewrite.insertLast(statement, null);
			}
		}
		if(returnedVariables1.size() == 1 && returnedVariables2.size() == 1 &&
				!mappedNodesContainStatementReturningVariable(returnedVariables1.get(0), returnedVariables2.get(0))) {
			ReturnStatement returnStatement = ast.newReturnStatement();
			sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, returnedVariables1.get(0).getName(), null);
			methodBodyRewrite.insertLast(returnStatement, null);
			if(!mappedNodesContainStatementDeclaringVariable(returnedVariables1.get(0), returnedVariables2.get(0)) &&
					!variableIsPassedAsCommonParameter(returnedVariables1.get(0), returnedVariables2.get(0))) {
				Type returnedType = extractType(returnedVariables1.get(0));
				Expression initializer = generateDefaultValue(sourceRewriter, ast, returnedType);
				VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
				sourceRewriter.set(fragment, VariableDeclarationFragment.NAME_PROPERTY, returnedVariables1.get(0).getName(), null);
				sourceRewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, initializer, null);
				VariableDeclarationStatement declarationStatement = ast.newVariableDeclarationStatement(fragment);
				sourceRewriter.set(declarationStatement, VariableDeclarationStatement.TYPE_PROPERTY, returnedType, null);
				methodBodyRewrite.insertFirst(declarationStatement, null);
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
			boolean isReturnedVariable = false;
			if(expression1 != null) {
				isReturnedVariable = isReturnedVariable(expression1.getExpression(), this.returnedVariables.get(0));
			}
			else if(expression2 != null) {
				isReturnedVariable = isReturnedVariable(expression2.getExpression(), this.returnedVariables.get(1));
			}
			ITypeBinding typeBinding1 = expression1 != null ? expression1.getExpression().resolveTypeBinding()
					: expression2.getExpression().resolveTypeBinding();
			ITypeBinding typeBinding2 = expression2 != null ? expression2.getExpression().resolveTypeBinding()
					: expression1.getExpression().resolveTypeBinding();
			if(!isReturnedVariable) {
				ITypeBinding typeBinding = null;
				if(difference.containsDifferenceType(DifferenceType.SUBCLASS_TYPE_MISMATCH) ||
						differenceContainsSubDifferenceWithSubclassTypeMismatch(difference)) {
					if(!typeBinding1.isEqualTo(typeBinding2)) {
						ITypeBinding commonSuperTypeBinding = ASTNodeMatcher.commonSuperType(typeBinding1, typeBinding2);
						if(commonSuperTypeBinding != null) {
							typeBinding = commonSuperTypeBinding;
						}
					}
					else {
						typeBinding = typeBinding1;
					}
				}
				else {
					if(expression1 != null && !typeBinding1.getQualifiedName().equals("null")) {
						typeBinding = typeBinding1;
					}
					else {
						typeBinding = typeBinding2;
					}
				}
				Type type = generateTypeFromTypeBinding(typeBinding, ast, sourceRewriter);
				Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
				typeBindings.add(typeBinding);
				getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
				SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
				sourceRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, ast.newSimpleName("arg" + i), null);
				i++;
				sourceRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, type, null);
				parameterRewrite.insertLast(parameter, null);
			}
		}
		//add parameters for the fields that should be parameterized instead of being pulled up
		for(VariableDeclaration variableDeclaration : fieldDeclarationsToBeParameterized.get(0)) {
			if(accessedLocalFieldsG1.contains(variableDeclaration)) {
				SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
				Type type = generateTypeFromTypeBinding(variableDeclaration.resolveBinding().getType(), ast, sourceRewriter);
				sourceRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, type, null);
				sourceRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, variableDeclaration.getName(), null);
				parameterRewrite.insertLast(parameter, null);
			}
		}
		sourceRewriter.set(newMethodDeclaration, MethodDeclaration.BODY_PROPERTY, newMethodBody, null);
		bodyDeclarationsRewrite.insertLast(newMethodDeclaration, null);
		cloneInfo.requiredImportTypeBindings = requiredImportTypeBindings;
		cloneInfo.methodBodyRewrite = methodBodyRewrite;
		cloneInfo.parameterRewrite = parameterRewrite;
	}

	private void updateAccessModifier(MethodDeclaration methodDeclaration, Modifier.ModifierKeyword modifierKeyword) {
		AST ast = methodDeclaration.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		CompilationUnit compilationUnit = findCompilationUnit(methodDeclaration);
		ListRewrite modifiersRewrite = rewriter.getListRewrite(methodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		List<IExtendedModifier> originalModifiers = methodDeclaration.modifiers();
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

	private boolean type2Clones(MethodDeclaration methodDeclaration1, MethodDeclaration methodDeclaration2) {
		if(methodDeclaration1.getBody() != null && methodDeclaration2.getBody() != null) {
			StatementCollector collector1 = new StatementCollector();
			methodDeclaration1.getBody().accept(collector1);
			List<ASTNode> statements1 = collector1.getStatementList();
			StatementCollector collector2 = new StatementCollector();
			methodDeclaration2.getBody().accept(collector2);
			List<ASTNode> statements2 = collector2.getStatementList();
			ITypeRoot typeRoot1 = findCompilationUnit(methodDeclaration1).getTypeRoot();
			ITypeRoot typeRoot2 = findCompilationUnit(methodDeclaration2).getTypeRoot();
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

	private boolean extractToUtilityClass(ITypeBinding commonSuperTypeOfSourceTypeDeclarations) {
		return cloneFragmentsDoNotAccessFieldsOrMethods() && commonSuperTypeOfSourceTypeDeclarations.getQualifiedName().equals("java.lang.Object");
	}

	private boolean cloneFragmentsDoNotAccessFieldsOrMethods() {
		Set<AbstractVariable> accessedLocalFields1 = new LinkedHashSet<AbstractVariable>();
		accessedLocalFields1.addAll(mapper.getDirectlyAccessedLocalFieldsG1());
		//accessedLocalFields1.addAll(mapper.getIndirectlyAccessedLocalFieldsG1());
		Set<AbstractVariable> accessedLocalFields2 = new LinkedHashSet<AbstractVariable>();
		accessedLocalFields2.addAll(mapper.getDirectlyAccessedLocalFieldsG2());
		//accessedLocalFields2.addAll(mapper.getIndirectlyAccessedLocalFieldsG2());
		Set<Expression> allSimpleNames1 = extractSimpleNames(mapper.getRemovableNodesG1());
		Set<Expression> allSimpleNames2 = extractSimpleNames(mapper.getRemovableNodesG2());
		int counter = 0;
		for(AbstractVariable variable1 : accessedLocalFields1) {
			if(variable1 instanceof PlainVariable) {
				for(Expression expression : allSimpleNames1) {
					SimpleName simpleName = (SimpleName)expression;
					if(simpleName.resolveBinding().getKey().equals(variable1.getVariableBindingKey())) {
						IVariableBinding variableBinding = (IVariableBinding)simpleName.resolveBinding();
						boolean isStaticField = false;
						if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) != 0) {
							isStaticField = true;
						}
						boolean foundInDifferences = false;
						for(ASTNodeDifference difference : mapper.getNodeDifferences()) {
							Expression expr1 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(difference.getExpression1().getExpression());
							if(isExpressionWithinExpression(simpleName, expr1)) {
								foundInDifferences = true;
								break;
							}
						}
						if(!foundInDifferences && !isStaticField) {
							counter++;
						}
					}
				}
			}
		}
		for(AbstractVariable variable2 : accessedLocalFields2) {
			if(variable2 instanceof PlainVariable) {
				for(Expression expression : allSimpleNames2) {
					SimpleName simpleName = (SimpleName)expression;
					if(simpleName.resolveBinding().getKey().equals(variable2.getVariableBindingKey())) {
						IVariableBinding variableBinding = (IVariableBinding)simpleName.resolveBinding();
						boolean isStaticField = false;
						if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) != 0) {
							isStaticField = true;
						}
						boolean foundInDifferences = false;
						for(ASTNodeDifference difference : mapper.getNodeDifferences()) {
							Expression expr2 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(difference.getExpression2().getExpression());
							if(isExpressionWithinExpression(simpleName, expr2)) {
								foundInDifferences = true;
								break;
							}
						}
						if(!foundInDifferences && !isStaticField) {
							counter++;
						}
					}
				}
			}
		}
		Set<Expression> allMethodInvocations1 = extractMethodInvocations(mapper.getRemovableNodesG1());
		Set<Expression> allMethodInvocations2 = extractMethodInvocations(mapper.getRemovableNodesG2());
		for(MethodObject m : mapper.getAccessedLocalMethodsG1()) {
			for(Expression expression : allMethodInvocations1) {
				if(expression instanceof MethodInvocation) {
					MethodInvocation methodInvocation = (MethodInvocation)expression;
					if(methodInvocation.resolveMethodBinding().isEqualTo(m.getMethodDeclaration().resolveBinding())) {
						boolean foundInDifferences = false;
						for(ASTNodeDifference difference : mapper.getNodeDifferences()) {
							Expression expr1 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(difference.getExpression1().getExpression());
							if(isExpressionWithinExpression(methodInvocation, expr1)) {
								foundInDifferences = true;
								break;
							}
						}
						if(!foundInDifferences && !m.isStatic()) {
							counter++;
						}
					}
				}
			}
		}
		for(MethodObject m : mapper.getAccessedLocalMethodsG2()) {
			for(Expression expression : allMethodInvocations2) {
				if(expression instanceof MethodInvocation) {
					MethodInvocation methodInvocation = (MethodInvocation)expression;
					if(methodInvocation.resolveMethodBinding().isEqualTo(m.getMethodDeclaration().resolveBinding())) {
						boolean foundInDifferences = false;
						for(ASTNodeDifference difference : mapper.getNodeDifferences()) {
							Expression expr2 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(difference.getExpression2().getExpression());
							if(isExpressionWithinExpression(methodInvocation, expr2)) {
								foundInDifferences = true;
								break;
							}
						}
						if(!foundInDifferences && !m.isStatic()) {
							counter++;
						}
					}
				}
			}
		}
		return counter == 0;
	}

	private boolean isExpressionWithinExpression(ASTNode expression, Expression parentExpression) {
		if(expression.equals(parentExpression))
			return true;
		ASTNode parent = expression.getParent();
		if(!(parent instanceof Statement))
			return isExpressionWithinExpression(parent, parentExpression);
		else
			return false;
	}
	
	private Set<Expression> extractSimpleNames(Set<PDGNode> mappedNodes) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		Set<Expression> allSimpleNames = new LinkedHashSet<Expression>();
		for(PDGNode pdgNode : mappedNodes) {
			AbstractStatement abstractStatement = pdgNode.getStatement();
			if(abstractStatement instanceof StatementObject) {
				StatementObject statement = (StatementObject)abstractStatement;
				allSimpleNames.addAll(expressionExtractor.getVariableInstructions(statement.getStatement()));
			}
			else if(abstractStatement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)abstractStatement;
				for(AbstractExpression expression : composite.getExpressions()) {
					allSimpleNames.addAll(expressionExtractor.getVariableInstructions(expression.getExpression()));
				}
				if(composite instanceof TryStatementObject) {
					TryStatementObject tryStatement = (TryStatementObject)composite;
					List<CatchClauseObject> catchClauses = tryStatement.getCatchClauses();
					for(CatchClauseObject catchClause : catchClauses) {
						allSimpleNames.addAll(expressionExtractor.getVariableInstructions(catchClause.getBody().getStatement()));
					}
					if(tryStatement.getFinallyClause() != null) {
						allSimpleNames.addAll(expressionExtractor.getVariableInstructions(tryStatement.getFinallyClause().getStatement()));
					}
				}
			}
		}
		return allSimpleNames;
	}

	private Set<Expression> extractMethodInvocations(Set<PDGNode> mappedNodes) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		Set<Expression> allMethodInvocations = new LinkedHashSet<Expression>();
		for(PDGNode pdgNode : mappedNodes) {
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
		}
		return allMethodInvocations;
	}

	private boolean pullUpToCommonSuperclass(ITypeBinding commonSuperTypeOfSourceTypeDeclarations,
			ITypeBinding typeBinding1, ITypeBinding typeBinding2) {
		return ASTReader.getSystemObject().getClassObject(commonSuperTypeOfSourceTypeDeclarations.getQualifiedName()) != null &&
				(cloneFragmentsDoNotAccessFieldsOrMethods() ||
				superclassInheritedOnlyByRefactoredSubclasses(commonSuperTypeOfSourceTypeDeclarations, typeBinding1, typeBinding2) ||
				superclassIsOneOfRefactoredSubclasses(commonSuperTypeOfSourceTypeDeclarations, typeBinding1, typeBinding2) ||
				!superclassDirectlyInheritedFromRefactoredSubclasses(commonSuperTypeOfSourceTypeDeclarations, typeBinding1, typeBinding2));
	}

	private boolean superclassDirectlyInheritedFromRefactoredSubclasses(ITypeBinding commonSuperTypeOfSourceTypeDeclarations,
			ITypeBinding typeBinding1, ITypeBinding typeBinding2) {
		return typeBinding1.getSuperclass().isEqualTo(commonSuperTypeOfSourceTypeDeclarations) &&
				typeBinding2.getSuperclass().isEqualTo(commonSuperTypeOfSourceTypeDeclarations);
	}

	private boolean superclassIsOneOfRefactoredSubclasses(ITypeBinding commonSuperTypeOfSourceTypeDeclarations,
			ITypeBinding typeBinding1, ITypeBinding typeBinding2) {
		if(typeBinding1.isEqualTo(commonSuperTypeOfSourceTypeDeclarations) ||
				typeBinding2.isEqualTo(commonSuperTypeOfSourceTypeDeclarations)) {
			return true;
		}
		return false;
	}

	private boolean superclassInheritedOnlyByRefactoredSubclasses(ITypeBinding commonSuperTypeOfSourceTypeDeclarations,
			ITypeBinding typeBinding1, ITypeBinding typeBinding2) {
		if(!commonSuperTypeOfSourceTypeDeclarations.getQualifiedName().equals("java.lang.Object")) {
			CompilationUnitCache cache = CompilationUnitCache.getInstance();
			Set<IType> subTypes = cache.getSubTypes((IType)commonSuperTypeOfSourceTypeDeclarations.getJavaElement());
			if(subTypes.size() == 2 && subTypes.contains((IType)typeBinding1.getJavaElement()) &&
					subTypes.contains((IType)typeBinding2.getJavaElement())) {
				return true;
			}
		}
		return false;
	}

	private Set<VariableDeclaration> getFieldsAccessedInMethod(Set<VariableDeclaration> indirectlyAccessedLocalFields,
			MethodDeclaration methodDeclaration) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> simpleNames = expressionExtractor.getVariableInstructions(methodDeclaration.getBody());
		Set<VariableDeclaration> fieldsAccessedInMethod = new LinkedHashSet<VariableDeclaration>();
		for(Expression expression : simpleNames) {
			SimpleName simpleName = (SimpleName)expression;
			IBinding binding = simpleName.resolveBinding();
			if(binding.getKind() == IBinding.VARIABLE) {
				for(VariableDeclaration variableDeclaration : indirectlyAccessedLocalFields) {
					if(variableDeclaration.resolveBinding().isEqualTo(binding)) {
						fieldsAccessedInMethod.add(variableDeclaration);
					}
				}
			}
		}
		return fieldsAccessedInMethod;
	}

	private void pullUpLocallyAccessedFields(Set<VariableDeclaration> accessedLocalFieldsG1, Set<VariableDeclaration> accessedLocalFieldsG2,
			ListRewrite bodyDeclarationsRewrite, Set<ITypeBinding> requiredImportTypeBindings) {
		ASTRewrite sourceRewriter = cloneInfo.sourceRewriter;
		AST ast = cloneInfo.ast;
		TypeDeclaration sourceTypeDeclaration = cloneInfo.sourceTypeDeclaration;
		for(VariableDeclaration localFieldG1 : accessedLocalFieldsG1) {
			FieldDeclaration originalFieldDeclarationG1 = (FieldDeclaration)localFieldG1.getParent();
			for(VariableDeclaration localFieldG2 : accessedLocalFieldsG2) {
				FieldDeclaration originalFieldDeclarationG2 = (FieldDeclaration)localFieldG2.getParent();
				if(localFieldG1.getName().getIdentifier().equals(localFieldG2.getName().getIdentifier())) {
					//ITypeBinding commonSuperType = commonSuperType(originalFieldDeclarationG1.getType().resolveBinding(), originalFieldDeclarationG2.getType().resolveBinding());
					if(originalFieldDeclarationG1.getType().resolveBinding().isEqualTo(originalFieldDeclarationG2.getType().resolveBinding()) /*||
							(commonSuperType != null && !commonSuperType.getQualifiedName().equals("java.lang.Object"))*/) {
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
									getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
									
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
						boolean avoidPullUpDueToSerialization1 = avoidPullUpFieldDueToSerialization(sourceTypeDeclarations.get(0), localFieldG1);
						boolean avoidPullUpDueToSerialization2 = avoidPullUpFieldDueToSerialization(sourceTypeDeclarations.get(1), localFieldG2);
						if(!avoidPullUpDueToSerialization1 && !avoidPullUpDueToSerialization2) {
							//check if the common superclass is one of the source classes
							if(!sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
								fieldDeclarationsToBePulledUp.get(0).add(localFieldG1);
							}
							if(!sourceTypeDeclarations.get(1).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
								fieldDeclarationsToBePulledUp.get(1).add(localFieldG2);
							}
							if(!sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding()) &&
									!sourceTypeDeclarations.get(1).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
								Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
								typeBindings.add(localFieldG1.resolveBinding().getType());
								getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
								VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
								sourceRewriter.set(fragment, VariableDeclarationFragment.NAME_PROPERTY, ast.newSimpleName(localFieldG1.getName().getIdentifier()), null);
								if(localFieldG1.getInitializer() != null && localFieldG2.getInitializer() != null) {
									Expression initializer1 = localFieldG1.getInitializer();
									Expression initializer2 = localFieldG2.getInitializer();
									if(initializer1.subtreeMatch(new ASTMatcher(), initializer2)) {
										sourceRewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, ASTNode.copySubtree(ast, initializer1), null);
									}
								}
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
						}
						else {
							fieldDeclarationsToBeParameterized.get(0).add(localFieldG1);
							fieldDeclarationsToBeParameterized.get(1).add(localFieldG2);
							Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
							typeBindings.add(localFieldG1.resolveBinding().getType());
							getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
						}
						break;
					}
				}
			}
		}
	}

	private boolean implementsSerializableInterface(ITypeBinding typeBinding) {
		ITypeBinding[] implementedInterfaces = typeBinding.getInterfaces();
		for(ITypeBinding implementedInterface : implementedInterfaces) {
			if(implementedInterface.getQualifiedName().equals("java.io.Serializable")) {
				return true;
			}
		}
		ITypeBinding superclassTypeBinding = typeBinding.getSuperclass();
		if(superclassTypeBinding != null) {
			return implementsSerializableInterface(superclassTypeBinding);
		}
		return false;
	}
	
	private boolean avoidPullUpFieldDueToSerialization(TypeDeclaration typeDeclaration, VariableDeclaration localField) {
		return implementsSerializableInterface(typeDeclaration.resolveBinding()) &&
				(localField.resolveBinding().getModifiers() & Modifier.TRANSIENT) == 0;
	}
	
	private boolean avoidPullUpMethodDueToSerialization(TypeDeclaration typeDeclaration, Set<VariableDeclaration> fieldsAccessedInMethod) {
		for(VariableDeclaration localField : fieldsAccessedInMethod) {
			if(avoidPullUpFieldDueToSerialization(typeDeclaration, localField)) {
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
		finalizeOriginalMethod(sourceCompilationUnits.get(0), cloneInfo.originalMethodBodyRewriteList.get(0));
		finalizeOriginalMethod(sourceCompilationUnits.get(1), cloneInfo.originalMethodBodyRewriteList.get(1));
		try {
			CompilationUnitChange change = compilationUnitChanges.get(cloneInfo.sourceICompilationUnit);
			if(change != null) {
				ImportRewrite importRewrite = ImportRewrite.create(cloneInfo.sourceCompilationUnit, true);
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
				for(ASTNodeDifference difference : differences) {
					BindingSignaturePair signaturePair = difference.getBindingSignaturePair();
					occurrencesInDifferences1 += signaturePair.getSignature1().getOccurrences(variableDeclaration1.resolveBinding().getKey());
					occurrencesInDifferences2 += signaturePair.getSignature2().getOccurrences(variableDeclaration2.resolveBinding().getKey());
				}
				List<Expression> simpleNames1 = expressionExtractor.getVariableInstructions(node1.getASTStatement());
				List<Expression> simpleNames2 = expressionExtractor.getVariableInstructions(node2.getASTStatement());
				int occurrencesInStatement1 = 0;
				int occurrencesInStatement2 = 0;
				for(Expression expression : simpleNames1) {
					SimpleName simpleName = (SimpleName)expression;
					if(simpleName.resolveBinding().isEqualTo(variableDeclaration1.resolveBinding())) {
						occurrencesInStatement1++;
					}
				}
				for(Expression expression : simpleNames2) {
					SimpleName simpleName = (SimpleName)expression;
					if(simpleName.resolveBinding().isEqualTo(variableDeclaration2.resolveBinding())) {
						occurrencesInStatement2++;
					}
				}
				if(occurrencesInStatement1 > occurrencesInDifferences1 || occurrencesInStatement2 > occurrencesInDifferences2) {
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
				Expression newIfExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, oldIfStatement.getExpression(), nodeMapping.getNonOverlappingNodeDifferences());
				sourceRewriter.set(newIfStatement, IfStatement.EXPRESSION_PROPERTY, newIfExpression, null);
				List<CloneStructureNode> trueControlDependentChildren = new ArrayList<CloneStructureNode>();
				List<CloneStructureNode> falseControlDependentChildren = new ArrayList<CloneStructureNode>();
				boolean symmetricalIfElse = nodeMapping.isSymmetricalIfElse();
				for(CloneStructureNode child : node.getChildren()) {
					if(processableNode(child)) {
						PDGNodeMapping childMapping = (PDGNodeMapping) child.getMapping();
						PDGNodeMapping symmetrical = childMapping.getSymmetricalIfNodePair();
						if(symmetrical != null) {
							if(symmetrical.equals(nodeMapping)) {
								falseControlDependentChildren.add(child);
							}
							else {
								trueControlDependentChildren.add(child);
							}
						}
						else {
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
										isNestedUnderElse(childNodeG1) && symmetricalIfElse) {
									falseControlDependentChildren.add(child);
								}
								else if((!isNestedUnderElse(childNodeG1) && !isNestedUnderElse(childNodeG2)) ||
										!isNestedUnderElse(childNodeG1) && symmetricalIfElse) {
									trueControlDependentChildren.add(child);
								}
							}
						}
					}
					else if(child.getMapping() instanceof PDGElseMapping) {
						for(CloneStructureNode child2 : child.getChildren()) {
							if(processableNode(child2)) {
								PDGNodeMapping childMapping = (PDGNodeMapping) child2.getMapping();
								PDGNodeMapping symmetrical = childMapping.getSymmetricalIfNodePair();
								if(symmetrical != null) {
									if(symmetrical.equals(nodeMapping)) {
										falseControlDependentChildren.add(child2);
									}
									else {
										trueControlDependentChildren.add(child2);
									}
								}
								else {
									PDGNode childNodeG1 = child2.getMapping().getNodeG1();
									PDGNode childNodeG2 = child2.getMapping().getNodeG2();
									PDGControlDependence controlDependence1 = childNodeG1.getIncomingControlDependence();
									PDGControlDependence controlDependence2 = childNodeG2.getIncomingControlDependence();
									if(controlDependence1 != null && controlDependence2 != null) {
										if((controlDependence1.isTrueControlDependence() && controlDependence2.isTrueControlDependence()) ||
												(controlDependence1.isTrueControlDependence() && symmetricalIfElse)) {
											trueControlDependentChildren.add(child2);
										}
										else if((controlDependence1.isFalseControlDependence() && controlDependence2.isFalseControlDependence()) ||
												(controlDependence1.isFalseControlDependence() && symmetricalIfElse)) {
											falseControlDependentChildren.add(child2);
										}
									}
									else {
										if((isNestedUnderElse(childNodeG1) && isNestedUnderElse(childNodeG2)) ||
												(isNestedUnderElse(childNodeG1) && symmetricalIfElse)) {
											falseControlDependentChildren.add(child2);
										}
										else if((!isNestedUnderElse(childNodeG1) && !isNestedUnderElse(childNodeG2)) ||
												(!isNestedUnderElse(childNodeG1) && symmetricalIfElse)) {
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
				Expression newExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, oldSynchronizedStatement.getExpression(), nodeMapping.getNonOverlappingNodeDifferences());
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
					Expression newResourceExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, expression, nodeMapping.getNonOverlappingNodeDifferences());
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
					sourceRewriter.set(newCatchClause, CatchClause.EXCEPTION_PROPERTY, catchClause.getException(), null);
					Block newCatchBody = ast.newBlock();
					ListRewrite newCatchBodyRewrite = sourceRewriter.getListRewrite(newCatchBody, Block.STATEMENTS_PROPERTY);
					List<Statement> oldCatchStatements = catchClause.getBody().statements();
					for(Statement oldCatchStatement : oldCatchStatements) {
						Statement newStatement2 = (Statement)processASTNodeWithDifferences(ast, sourceRewriter, oldCatchStatement, nodeMapping.getNonOverlappingNodeDifferences());
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
						Statement newStatement2 = (Statement)processASTNodeWithDifferences(ast, sourceRewriter, oldFinallyStatement, nodeMapping.getNonOverlappingNodeDifferences());
						newFinallyBodyRewrite.insertLast(newStatement2, null);
					}
					sourceRewriter.set(newTryStatement, TryStatement.FINALLY_PROPERTY, newFinallyBody, null);
				}
				newStatement = newTryStatement;
			}
			else if(oldStatement instanceof SwitchStatement) {
				SwitchStatement oldSwitchStatement = (SwitchStatement)oldStatement;
				SwitchStatement newSwitchStatement = ast.newSwitchStatement();
				Expression newSwitchExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, oldSwitchStatement.getExpression(), nodeMapping.getNonOverlappingNodeDifferences());
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
				Expression newWhileExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, oldWhileStatement.getExpression(), nodeMapping.getNonOverlappingNodeDifferences());
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
				Expression newForExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, oldForStatement.getExpression(), nodeMapping.getNonOverlappingNodeDifferences());
				sourceRewriter.set(newForStatement, ForStatement.EXPRESSION_PROPERTY, newForExpression, null);
				ListRewrite initializerRewrite = sourceRewriter.getListRewrite(newForStatement, ForStatement.INITIALIZERS_PROPERTY);
				List<Expression> initializers = oldForStatement.initializers();
				for(Expression expression : initializers) {
					Expression newInitializerExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, expression, nodeMapping.getNonOverlappingNodeDifferences());
					initializerRewrite.insertLast(newInitializerExpression, null);
				}
				ListRewrite updaterRewrite = sourceRewriter.getListRewrite(newForStatement, ForStatement.UPDATERS_PROPERTY);
				List<Expression> updaters = oldForStatement.updaters();
				for(Expression expression : updaters) {
					Expression newUpdaterExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, expression, nodeMapping.getNonOverlappingNodeDifferences());
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
				Expression newEnhancedForExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, oldEnhancedForStatement.getExpression(), nodeMapping.getNonOverlappingNodeDifferences());
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
				Expression newDoExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, oldDoStatement.getExpression(), nodeMapping.getNonOverlappingNodeDifferences());
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
				newStatement = (Statement)processASTNodeWithDifferences(ast, sourceRewriter, oldStatement, nodeMapping.getNonOverlappingNodeDifferences());
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
			newStatement = oldStatement;
		}
		return newStatement;
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

	private Type generateTypeFromTypeBinding(ITypeBinding typeBinding, AST ast, ASTRewrite rewriter) {
		Type type = null;
		if(typeBinding.isParameterizedType()) {
			type = createParameterizedType(ast, typeBinding, rewriter);
		}
		else if(typeBinding.isClass() || typeBinding.isInterface()) {
			type = ast.newSimpleType(ast.newSimpleName(typeBinding.getName()));
		}
		else if(typeBinding.isPrimitive()) {
			String primitiveType = typeBinding.getName();
			if(primitiveType.equals("int"))
				type = ast.newPrimitiveType(PrimitiveType.INT);
			else if(primitiveType.equals("double"))
				type = ast.newPrimitiveType(PrimitiveType.DOUBLE);
			else if(primitiveType.equals("byte"))
				type = ast.newPrimitiveType(PrimitiveType.BYTE);
			else if(primitiveType.equals("short"))
				type = ast.newPrimitiveType(PrimitiveType.SHORT);
			else if(primitiveType.equals("char"))
				type = ast.newPrimitiveType(PrimitiveType.CHAR);
			else if(primitiveType.equals("long"))
				type = ast.newPrimitiveType(PrimitiveType.LONG);
			else if(primitiveType.equals("float"))
				type = ast.newPrimitiveType(PrimitiveType.FLOAT);
			else if(primitiveType.equals("boolean"))
				type = ast.newPrimitiveType(PrimitiveType.BOOLEAN);
		}
		else if(typeBinding.isArray()) {
			ITypeBinding elementTypeBinding = typeBinding.getElementType();
			Type elementType = generateTypeFromTypeBinding(elementTypeBinding, ast, rewriter);
			type = ast.newArrayType(elementType, typeBinding.getDimensions());
		}
		return type;
	}

	private ParameterizedType createParameterizedType(AST ast, ITypeBinding typeBinding, ASTRewrite rewriter) {
		ITypeBinding erasure = typeBinding.getErasure();
		ITypeBinding[] typeArguments = typeBinding.getTypeArguments();
		ParameterizedType parameterizedType = ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName(erasure.getName())));
		ListRewrite typeArgumentsRewrite = rewriter.getListRewrite(parameterizedType, ParameterizedType.TYPE_ARGUMENTS_PROPERTY);
		for(ITypeBinding typeArgument : typeArguments) {
			if(typeArgument.isParameterizedType()) {
				typeArgumentsRewrite.insertLast(createParameterizedType(ast, typeArgument, rewriter), null);
			}
			else if(typeArgument.isClass() || typeArgument.isInterface()) {
				typeArgumentsRewrite.insertLast(ast.newSimpleType(ast.newSimpleName(typeArgument.getName())), null);
			}
		}
		return parameterizedType;
	}

	private ASTNode processASTNodeWithDifferences(AST ast, ASTRewrite sourceRewriter, ASTNode oldASTNode, List<ASTNodeDifference> differences) {
		if(differences.isEmpty()) {
			if(!fieldDeclarationsToBeParameterized.get(0).isEmpty()) {
				ASTNode newASTNode = ASTNode.copySubtree(ast, oldASTNode);
				replaceFieldAccessesOfParameterizedFields(sourceRewriter, ast, oldASTNode, newASTNode);
				return newASTNode;
			}
			else {
				return oldASTNode;
			}
		}
		else {
			Set<VariableBindingKeyPair> parameterBindingKeys = originalPassedParameters.keySet();
			Set<VariableBindingKeyPair> declaredLocalVariableBindingKeys = mapper.getDeclaredLocalVariablesInMappedNodes().keySet();
			Set<String> declaredLocalVariableBindingKeysInAdditionallyMatchedNodes1 = mapper.getDeclaredLocalVariableBindingKeysInAdditionallyMatchedNodesG1();
			Set<String> declaredLocalVariableBindingKeysInAdditionallyMatchedNodes2 = mapper.getDeclaredLocalVariableBindingKeysInAdditionallyMatchedNodesG2();
			ASTNode newASTNode = ASTNode.copySubtree(ast, oldASTNode);
			if(!fieldDeclarationsToBeParameterized.get(0).isEmpty()) {
				replaceFieldAccessesOfParameterizedFields(sourceRewriter, ast, oldASTNode, newASTNode);
			}
			for(ASTNodeDifference difference : differences) {
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
					if(parameterBindingKeys.contains(keyPair) || declaredLocalVariableBindingKeys.contains(keyPair) ||
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
					if(parameterBindingKeys.contains(keyPair) || declaredLocalVariableBindingKeys.contains(keyPair) ||
							declaredLocalVariableBindingKeysInAdditionallyMatchedNodes1.contains(binding.getKey()) ||
							declaredLocalVariableBindingKeysInAdditionallyMatchedNodes2.contains(binding2.getKey()))
						isCommonParameter = true;
				}
				if(!isCommonParameter) {
					if(difference instanceof FieldAccessReplacedWithGetterInvocationDifference) {
						FieldAccessReplacedWithGetterInvocationDifference nodeDifference =
								(FieldAccessReplacedWithGetterInvocationDifference)difference;
						MethodInvocation newGetterMethodInvocation = generateGetterMethodInvocation(ast, sourceRewriter, nodeDifference);
						if(oldASTNode.equals(oldExpression)) {
							return newGetterMethodInvocation;
						}
						else {
							replaceExpression(sourceRewriter, oldASTNode, newASTNode, oldExpression, newGetterMethodInvocation);
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
								Type arg = generateTypeFromTypeBinding(commonSuperTypeBinding, ast, sourceRewriter);
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
			return newASTNode;
		}
	}

	private void replaceFieldAccessesOfParameterizedFields(ASTRewrite sourceRewriter, AST ast, ASTNode oldASTNode, ASTNode newASTNode) {
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
					sourceRewriter.replace(newFieldAccess, ast.newSimpleName(variableDeclaration.getName().getIdentifier()), null);
					break;
				}
			}
			j++;
		}
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

	private void modifySourceClass(CompilationUnit compilationUnit, TypeDeclaration typeDeclaration,
			Set<VariableDeclaration> fieldDeclarationsToBePulledUp, Set<MethodDeclaration> methodDeclarationsToBePulledUp) {
		if(cloneInfo.intermediateClassName != null && !cloneInfo.extractUtilityClass) {
			modifySuperclassType(compilationUnit, typeDeclaration, cloneInfo.intermediateClassName);
		}
		for(MethodDeclaration methodDeclaration : methodDeclarationsToBePulledUp) {
			removeMethodDeclaration(methodDeclaration, findTypeDeclaration(methodDeclaration), findCompilationUnit(methodDeclaration));
		}
		for(VariableDeclaration variableDeclaration : fieldDeclarationsToBePulledUp) {
			removeFieldDeclaration(variableDeclaration, findTypeDeclaration(variableDeclaration), findCompilationUnit(variableDeclaration));
		}
		try {
			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)compilationUnit.getJavaElement();
			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
			ImportRewrite importRewrite = ImportRewrite.create(compilationUnit, true);
			if(cloneInfo.intermediateClassPackageBinding != null) {
				if(!compilationUnit.getPackage().resolveBinding().isEqualTo(cloneInfo.intermediateClassPackageBinding)) {
					importRewrite.addImport(cloneInfo.intermediateClassPackageBinding.getName() +
							"." + cloneInfo.intermediateClassName);
				}
			}		
			TextEdit importEdit = importRewrite.rewriteImports(null);
			if(importRewrite.getCreatedImports().length > 0) {
				change.getEdit().addChild(importEdit);
				change.addTextEditGroup(new TextEditGroup("Add required import declarations", new TextEdit[] {importEdit}));
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private void removeFieldDeclaration(VariableDeclaration variableDeclaration, TypeDeclaration typeDeclaration, CompilationUnit compilationUnit) {
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
	
	private TypeDeclaration findTypeDeclaration(ASTNode node) {
		ASTNode parent = node.getParent();
		while(parent != null) {
			if(parent instanceof TypeDeclaration) {
				return (TypeDeclaration)parent;
			}
			parent = parent.getParent();
		}
		return null;
	}
	
	private CompilationUnit findCompilationUnit(ASTNode node) {
		ASTNode parent = node.getParent();
		while(parent != null) {
			if(parent instanceof CompilationUnit) {
				return (CompilationUnit)parent;
			}
			parent = parent.getParent();
		}
		return null;
	}

	private void removeMethodDeclaration(MethodDeclaration methodDeclaration, TypeDeclaration typeDeclaration, CompilationUnit compilationUnit) {
		AST ast = typeDeclaration.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		ListRewrite bodyRewrite = rewriter.getListRewrite(typeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		bodyRewrite.remove(methodDeclaration, null);
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
			String message = cloneInfo.extractUtilityClass ? "Move method to utility class" : "Pull up method to superclass";
			change.addTextEditGroup(new TextEditGroup(message, new TextEdit[] {sourceEdit}));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void modifySuperclassType(CompilationUnit compilationUnit, TypeDeclaration typeDeclaration, String superclassTypeName) {
		AST ast = typeDeclaration.getAST();
		ASTRewrite superClassTypeRewriter = ASTRewrite.create(ast);
		if(superclassTypeName.contains(".")) {
			String qualifier = superclassTypeName.substring(0, superclassTypeName.lastIndexOf("."));
			String innerType = superclassTypeName.substring(superclassTypeName.lastIndexOf(".") + 1, superclassTypeName.length());
			superClassTypeRewriter.set(typeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY,
					ast.newQualifiedType(ast.newSimpleType(ast.newName(qualifier)), ast.newSimpleName(innerType)), null);
		}
		else {
			superClassTypeRewriter.set(typeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, ast.newSimpleType(ast.newSimpleName(superclassTypeName)), null);
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
			argumentsRewrite.insertLast(variableDeclarations.get(index).getName(), null);
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
			boolean isReturnedVariable = false;
			if(expression != null)
				isReturnedVariable = isReturnedVariable(expression, returnedVariables);
			if(!isReturnedVariable) {
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
		if(index == 0)
			accessedLocalFields = getLocallyAccessedFields(mapper.getDirectlyAccessedLocalFieldsG1(), sourceTypeDeclarations.get(0));
		else
			accessedLocalFields = getLocallyAccessedFields(mapper.getDirectlyAccessedLocalFieldsG2(), sourceTypeDeclarations.get(1));
		for(VariableDeclaration variableDeclaration : fieldsToBeParameterized) {
			if(accessedLocalFields.contains(variableDeclaration)) {
				argumentsRewrite.insertLast(variableDeclaration.getName(), null);
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
		Block parentBlock = (Block)firstStatement.getParent();
		ListRewrite blockRewrite = methodBodyRewriter.getListRewrite(parentBlock, Block.STATEMENTS_PROPERTY);
		CloneStructureNode root = mapper.getCloneStructureRoot();
		List<CloneStructureNode> processedCloneStructureGapNodes = new ArrayList<CloneStructureNode>();
		Set<PDGNode> remainingNodes = new TreeSet<PDGNode>();
		remainingNodes.addAll(remainingNodesMovableBefore);
		remainingNodes.addAll(remainingNodesMovableAfter);
		List<Statement> statementsToBeMovedBefore = new ArrayList<Statement>();
		List<Statement> statementsToBeMovedAfter = new ArrayList<Statement>();
		for(PDGNode remainingNode : remainingNodes) {
			CloneStructureNode remainingCloneStructureNode = null;
			if(index == 0)
				remainingCloneStructureNode = root.findNodeG1(remainingNode);
			else
				remainingCloneStructureNode = root.findNodeG2(remainingNode);
			if(!processedCloneStructureGapNodes.contains(remainingCloneStructureNode.getParent())) {
				Statement statement = processCloneStructureGapNode(remainingCloneStructureNode, ast, methodBodyRewriter, index);
				if(remainingNodesMovableBefore.contains(remainingNode)) {
					statementsToBeMovedBefore.add(statement);
				}
				else if(remainingNodesMovableAfter.contains(remainingNode)) {
					statementsToBeMovedAfter.add(statement);
				}
				methodBodyRewriter.remove(remainingNode.getASTStatement(), null);
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
					|| statementsToBeMovedBefore.contains(variableDeclaration.getParent())) {
				//create an assignment statement
				Type variableType = extractType(variableDeclaration);
				Assignment assignment = ast.newAssignment();
				methodBodyRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY, variableDeclaration.getName(), null);
				ITypeBinding returnTypeBinding = findReturnTypeBinding();
				if(returnTypeBinding != null && !returnTypeBinding.isEqualTo(variableType.resolveBinding())) {
					CastExpression castExpression = ast.newCastExpression();
					methodBodyRewriter.set(castExpression, CastExpression.EXPRESSION_PROPERTY, methodInvocation, null);
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
							generateDefaultValue(methodBodyRewriter, ast, variableType), null);
				}
			}
			else {
				//create a variable declaration statement
				Type variableType = extractType(variableDeclaration);
				VariableDeclarationFragment newFragment = ast.newVariableDeclarationFragment();
				methodBodyRewriter.set(newFragment, VariableDeclarationFragment.NAME_PROPERTY, variableDeclaration.getName(), null);
				ITypeBinding returnTypeBinding = findReturnTypeBinding();
				if(returnTypeBinding != null && !returnTypeBinding.isEqualTo(variableType.resolveBinding())) {
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
			blockRewrite.insertBefore(methodInvocationStatement, firstStatement, null);
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
			ITypeBinding returnTypeBinding = findReturnTypeBinding();
			Statement methodInvocationStatement = null;
			if(returnTypeBinding != null) {
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
			blockRewrite.insertBefore(methodInvocationStatement, firstStatement, null);
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

	private void getSimpleTypeBindings(Set<ITypeBinding> typeBindings, Set<ITypeBinding> finalTypeBindings) {
		for(ITypeBinding typeBinding : typeBindings) {
			if(typeBinding.isPrimitive()) {

			}
			else if(typeBinding.isArray()) {
				ITypeBinding elementTypeBinding = typeBinding.getElementType();
				Set<ITypeBinding> typeBindingList = new LinkedHashSet<ITypeBinding>();
				typeBindingList.add(elementTypeBinding);
				getSimpleTypeBindings(typeBindingList, finalTypeBindings);
			}
			else if(typeBinding.isParameterizedType()) {
				Set<ITypeBinding> typeBindingList = new LinkedHashSet<ITypeBinding>();
				typeBindingList.add(typeBinding.getTypeDeclaration());
				ITypeBinding[] typeArgumentBindings = typeBinding.getTypeArguments();
				for(ITypeBinding typeArgumentBinding : typeArgumentBindings)
					typeBindingList.add(typeArgumentBinding);
				getSimpleTypeBindings(typeBindingList, finalTypeBindings);
			}
			else if(typeBinding.isWildcardType()) {
				Set<ITypeBinding> typeBindingList = new LinkedHashSet<ITypeBinding>();
				typeBindingList.add(typeBinding.getBound());
				getSimpleTypeBindings(typeBindingList, finalTypeBindings);
			}
			else {
				if(typeBinding.isNested()) {
					if(!containsTypeBinding(typeBinding.getDeclaringClass(), finalTypeBindings))
						finalTypeBindings.add(typeBinding.getDeclaringClass());
				}
				if(!containsTypeBinding(typeBinding, finalTypeBindings))
					finalTypeBindings.add(typeBinding);
			}
		}
	}

	private boolean containsTypeBinding(ITypeBinding typeBinding, Set<ITypeBinding> typeBindings) {
		for(ITypeBinding typeBinding2 : typeBindings) {
			if(typeBinding2.getKey().equals(typeBinding.getKey()))
				return true;
		}
		return false;
	}

	private void addImportDeclaration(ITypeBinding typeBinding, CompilationUnit targetCompilationUnit, ASTRewrite targetRewriter) {
		String qualifiedName = typeBinding.getQualifiedName();
		String qualifiedPackageName = "";
		if(qualifiedName.contains("."))
			qualifiedPackageName = qualifiedName.substring(0,qualifiedName.lastIndexOf("."));
		PackageDeclaration sourcePackageDeclaration = sourceCompilationUnits.get(0).getPackage();
		String sourcePackageDeclarationName = "";
		if(sourcePackageDeclaration != null)
			sourcePackageDeclarationName = sourcePackageDeclaration.getName().getFullyQualifiedName();     
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
						e.printStackTrace();
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
		final RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 2);
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
			}
			if(mapper.getPreconditionViolations().isEmpty()) {
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
