package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.AbstractMethodDeclaration;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.decomposition.AbstractExpression;
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
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
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
	private List<Set<MethodDeclaration>> methodDeclarationsToBePulledUp;
	private List<Set<LabeledStatement>> labeledStatementsToBeRemoved;
	private Map<ICompilationUnit, CompilationUnitChange> compilationUnitChanges;
	private Map<ICompilationUnit, CreateCompilationUnitChange> createCompilationUnitChanges;
	private Set<IJavaElement> javaElementsToOpenInEditor;
	private Set<PDGNodeMapping> sortedNodeMappings;
	private List<TreeSet<PDGNode>> removableStatements;
	private List<TreeSet<PDGNode>> remainingStatementsMovableBefore;
	private List<TreeSet<PDGNode>> remainingStatementsMovableAfter;
	private Map<String, ArrayList<VariableDeclaration>> originalPassedParameters;
	private Map<BindingSignaturePair, ASTNodeDifference> parameterizedDifferenceMap;
	private List<ArrayList<VariableDeclaration>> returnedVariables;
	private String intermediateClassName;
	private String extractedMethodName;
	
	public ExtractCloneRefactoring(List<PDGSubTreeMapper> mappers) {
		super();
		this.mappers = mappers;
		this.mapper = mappers.get(0);
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
		this.methodDeclarationsToBePulledUp = new ArrayList<Set<MethodDeclaration>>();
		this.labeledStatementsToBeRemoved = new ArrayList<Set<LabeledStatement>>();
		for(int i=0; i<2; i++) {
			fieldDeclarationsToBePulledUp.add(new LinkedHashSet<VariableDeclaration>());
			methodDeclarationsToBePulledUp.add(new LinkedHashSet<MethodDeclaration>());
			labeledStatementsToBeRemoved.add(new LinkedHashSet<LabeledStatement>());
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
		this.originalPassedParameters = new LinkedHashMap<String, ArrayList<VariableDeclaration>>();
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
		this.intermediateClassName = null;
		//this.extractedMethodName = null;
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

	public void setExtractedMethodName(String extractedMethodName) {
		this.extractedMethodName = extractedMethodName;
	}

	public void apply() {
		initialize();
		extractClone();
		for(int i=0; i<sourceCompilationUnits.size(); i++) {
			modifySourceClass(sourceCompilationUnits.get(i), sourceTypeDeclarations.get(i), fieldDeclarationsToBePulledUp.get(i), methodDeclarationsToBePulledUp.get(i));
			modifySourceMethod(sourceCompilationUnits.get(i), sourceMethodDeclarations.get(i), removableStatements.get(i),
					remainingStatementsMovableBefore.get(i), remainingStatementsMovableAfter.get(i), returnedVariables.get(i), i);
		}
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

	private void extractClone() {
		CompilationUnit sourceCompilationUnit = null;
		ICompilationUnit sourceICompilationUnit = null;
		TypeDeclaration sourceTypeDeclaration = null;
		ASTRewrite sourceRewriter = null;
		AST ast = null;
		Document document = null;
		IFile file = null;
		ITypeBinding commonSuperTypeOfSourceTypeDeclarations = null;
		if(sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclarations.get(1).resolveBinding())) {
			sourceCompilationUnit = sourceCompilationUnits.get(0);
			sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
			sourceTypeDeclaration = sourceTypeDeclarations.get(0);
			sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
			ast = sourceTypeDeclaration.getAST();
		}
		else {
			//check is they have a common superclass
			ITypeBinding typeBinding1 = sourceTypeDeclarations.get(0).resolveBinding();
			ITypeBinding typeBinding2 = sourceTypeDeclarations.get(1).resolveBinding();
			commonSuperTypeOfSourceTypeDeclarations = ASTNodeMatcher.commonSuperType(typeBinding1, typeBinding2);
			if(commonSuperTypeOfSourceTypeDeclarations != null) {
				boolean superclassInheritedOnlyByRefactoringSubclasses = false;
				if(!commonSuperTypeOfSourceTypeDeclarations.getQualifiedName().equals("java.lang.Object")) {
					CompilationUnitCache cache = CompilationUnitCache.getInstance();
					Set<IType> subTypes = cache.getSubTypes((IType)commonSuperTypeOfSourceTypeDeclarations.getJavaElement());
					if(subTypes.size() == 2 && subTypes.contains((IType)typeBinding1.getJavaElement()) &&
							subTypes.contains((IType)typeBinding2.getJavaElement()))
						superclassInheritedOnlyByRefactoringSubclasses = true;
				}
				boolean superclassIsOneOfTheSourceTypeDeclarations = false;
				if(typeBinding1.isEqualTo(commonSuperTypeOfSourceTypeDeclarations) || typeBinding2.isEqualTo(commonSuperTypeOfSourceTypeDeclarations))
					superclassIsOneOfTheSourceTypeDeclarations = true;
				if(((mapper.getAccessedLocalFieldsG1().isEmpty() && mapper.getAccessedLocalFieldsG2().isEmpty() &&
						mapper.getAccessedLocalMethodsG1().isEmpty() && mapper.getAccessedLocalMethodsG2().isEmpty())
						|| superclassInheritedOnlyByRefactoringSubclasses || superclassIsOneOfTheSourceTypeDeclarations) &&
						ASTReader.getSystemObject().getClassObject(commonSuperTypeOfSourceTypeDeclarations.getQualifiedName()) != null) {
					//OR if the superclass in inherited ONLY by the subclasses participating in the refactoring
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
								sourceCompilationUnit = compilationUnit;
								sourceICompilationUnit = iCompilationUnit;
								sourceTypeDeclaration = typeDeclaration;
								sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
								ast = sourceTypeDeclaration.getAST();
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
					//create an intermediate superclass
					this.intermediateClassName = "Intermediate" + commonSuperTypeOfSourceTypeDeclarations.getName();
					CompilationUnit compilationUnit = sourceCompilationUnits.get(0);
					ICompilationUnit iCompilationUnit = (ICompilationUnit)compilationUnit.getJavaElement();
					IContainer container = (IContainer)iCompilationUnit.getResource().getParent();
					if(container instanceof IProject) {
						IProject contextProject = (IProject)container;
						file = contextProject.getFile(intermediateClassName + ".java");
					}
					else if(container instanceof IFolder) {
						IFolder contextFolder = (IFolder)container;
						file = contextFolder.getFile(intermediateClassName + ".java");
					}
					boolean intermediateAlreadyExists = false;
					ICompilationUnit intermediateICompilationUnit = JavaCore.createCompilationUnitFrom(file);
					javaElementsToOpenInEditor.add(intermediateICompilationUnit);
					ASTParser intermediateParser = ASTParser.newParser(AST.JLS4);
					intermediateParser.setKind(ASTParser.K_COMPILATION_UNIT);
					if(file.exists()) {
						intermediateAlreadyExists = true;
				        intermediateParser.setSource(intermediateICompilationUnit);
				        intermediateParser.setResolveBindings(true); // we need bindings later on
					}
					else {
						document = new Document();
						intermediateParser.setSource(document.get().toCharArray());
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
								if(typeDeclaration.getName().getIdentifier().equals(intermediateClassName)) {
									intermediateTypeDeclaration = typeDeclaration;
									int intermediateModifiers = intermediateTypeDeclaration.getModifiers();
									if((intermediateModifiers & Modifier.ABSTRACT) == 0) {
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
						SimpleName intermediateName = intermediateAST.newSimpleName(intermediateClassName);
						intermediateRewriter.set(intermediateTypeDeclaration, TypeDeclaration.NAME_PROPERTY, intermediateName, null);
						ListRewrite intermediateModifiersRewrite = intermediateRewriter.getListRewrite(intermediateTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
						intermediateModifiersRewrite.insertLast(intermediateAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
						intermediateModifiersRewrite.insertLast(intermediateAST.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
						intermediateRewriter.set(intermediateTypeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY,
								intermediateAST.newSimpleType(intermediateAST.newSimpleName(commonSuperTypeOfSourceTypeDeclarations.getName())), null);
						intermediateTypesRewrite.insertLast(intermediateTypeDeclaration, null);
					}
					sourceCompilationUnit = intermediateCompilationUnit;
					sourceICompilationUnit = intermediateICompilationUnit;
					sourceTypeDeclaration = intermediateTypeDeclaration;
					sourceRewriter = intermediateRewriter;
					ast = intermediateAST;
				}
			}
		}
		
		MethodDeclaration sourceMethodDeclaration = sourceMethodDeclarations.get(0);
		Set<ITypeBinding> requiredImportTypeBindings = new LinkedHashSet<ITypeBinding>();
		ListRewrite bodyDeclarationsRewrite = sourceRewriter.getListRewrite(sourceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		if(commonSuperTypeOfSourceTypeDeclarations != null) {
			Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
			typeBindings.add(commonSuperTypeOfSourceTypeDeclarations);
			getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
		}
		if(!sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclarations.get(1).resolveBinding())) {
			Set<VariableDeclaration> accessedLocalFieldsG1 = mapper.getAccessedLocalFieldsG1();
			Set<VariableDeclaration> accessedLocalFieldsG2 = mapper.getAccessedLocalFieldsG2();
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
							Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
							typeBindings.add(localFieldG1.resolveBinding().getType());
							getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
							fieldDeclarationsToBePulledUp.get(0).add(localFieldG1);
							fieldDeclarationsToBePulledUp.get(1).add(localFieldG2);
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
							break;
						}
					}
				}
			}

			Set<MethodInvocationObject> accessedLocalMethodsG1 = mapper.getAccessedLocalMethodsG1();
			Set<MethodInvocationObject> accessedLocalMethodsG2 = mapper.getAccessedLocalMethodsG2();
			for(MethodInvocationObject localMethodG1 : accessedLocalMethodsG1) {
				for(MethodInvocationObject localMethodG2 : accessedLocalMethodsG2) {
					if(localMethodG1.getMethodName().equals(localMethodG2.getMethodName()) &&
							localMethodG1.getReturnType().equals(localMethodG2.getReturnType()) &&
							localMethodG1.getParameterTypeList().equals(localMethodG2.getParameterTypeList())) {
						MethodDeclaration[] methodDeclarationsG1 = sourceTypeDeclarations.get(0).getMethods();
						IMethodBinding localMethodBindingG1 = localMethodG1.getMethodInvocation().resolveMethodBinding();
						MethodDeclaration methodDeclaration1 = null;
						for(MethodDeclaration methodDeclarationG1 : methodDeclarationsG1) {
							if(methodDeclarationG1.resolveBinding().isEqualTo(localMethodBindingG1)) {
								methodDeclaration1 = methodDeclarationG1;
								break;
							}
						}
						MethodDeclaration[] methodDeclarationsG2 = sourceTypeDeclarations.get(1).getMethods();
						IMethodBinding localMethodBindingG2 = localMethodG2.getMethodInvocation().resolveMethodBinding();
						MethodDeclaration methodDeclaration2 = null;
						for(MethodDeclaration methodDeclarationG2 : methodDeclarationsG2) {
							if(methodDeclarationG2.resolveBinding().isEqualTo(localMethodBindingG2)) {
								methodDeclaration2 = methodDeclarationG2;
								break;
							}
						}
						boolean exactClones = methodDeclaration1.subtreeMatch(new ASTMatcher(), methodDeclaration2);
						if(exactClones) {
							bodyDeclarationsRewrite.insertLast(methodDeclaration1, null);
							methodDeclarationsToBePulledUp.get(0).add(methodDeclaration1);
							methodDeclarationsToBePulledUp.get(1).add(methodDeclaration2);
						}
						else {
							MethodDeclaration newMethodDeclaration = ast.newMethodDeclaration();
							sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, ast.newSimpleName(methodDeclaration1.getName().getIdentifier()), null);
							sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, methodDeclaration1.getReturnType2(), null);	
							ListRewrite modifiersRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
							List<IExtendedModifier> originalModifiers = methodDeclaration1.modifiers();
							for(IExtendedModifier extendedModifier : originalModifiers) {
								if(extendedModifier.isModifier()) {
									Modifier modifier = (Modifier)extendedModifier;
									if(modifier.isProtected()) {
										modifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD), null);
									}
									else if(modifier.isPublic()) {
										modifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
									}
								}
							}
							modifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
							ListRewrite parametersRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
							List<SingleVariableDeclaration> parameters = methodDeclaration1.parameters();
							for(SingleVariableDeclaration parameter : parameters) {
								parametersRewrite.insertLast(parameter, null);
							}
							ListRewrite thrownExceptionsRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
							List<Name> thrownExceptions = methodDeclaration1.thrownExceptions();
							for(Name thrownException : thrownExceptions) {
								thrownExceptionsRewrite.insertLast(thrownException, null);
							}
							bodyDeclarationsRewrite.insertLast(newMethodDeclaration, null);
						}
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
		else {
			Modifier accessModifier = newMethodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD);
			modifierRewrite.insertLast(accessModifier, null);
		}
		
		if((sourceMethodDeclarations.get(0).getModifiers() & Modifier.STATIC) != 0 &&
				(sourceMethodDeclarations.get(1).getModifiers() & Modifier.STATIC) != 0) {
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
		Map<String, ArrayList<VariableDeclaration>> commonPassedParameters = mapper.getCommonPassedParameters();
		for(String parameterName : commonPassedParameters.keySet()) {
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
			if(child.getMapping() instanceof PDGNodeMapping) {
				Statement statement = processCloneStructureNode(child, ast, sourceRewriter);
				methodBodyRewrite.insertLast(statement, null);
			}
		}
		if(returnedVariables1.size() == 1 && returnedVariables2.size() == 1 && findReturnTypeBinding() == null) {
			ReturnStatement returnStatement = ast.newReturnStatement();
			sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, returnedVariables1.get(0).getName(), null);
			methodBodyRewrite.insertLast(returnStatement, null);
		}
		
		//add parameters for the differences between the clones
		int i = 0;
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
			if(!isReturnedVariable) {
				ITypeBinding typeBinding = null;
				if(difference.containsDifferenceType(DifferenceType.SUBCLASS_TYPE_MISMATCH) ||
						differenceContainsSubDifferenceWithSubclassTypeMismatch(difference)) {
					ITypeBinding typeBinding1 = expression1 != null ? expression1.getExpression().resolveTypeBinding()
																	: expression2.getExpression().resolveTypeBinding();
					ITypeBinding typeBinding2 = expression2 != null ? expression2.getExpression().resolveTypeBinding()
																	: expression1.getExpression().resolveTypeBinding();
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
					typeBinding = expression1 != null ? expression1.getExpression().resolveTypeBinding()
													: expression2.getExpression().resolveTypeBinding();
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
		sourceRewriter.set(newMethodDeclaration, MethodDeclaration.BODY_PROPERTY, newMethodBody, null);
		bodyDeclarationsRewrite.insertLast(newMethodDeclaration, null);
		
		try {
			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
			if(change != null) {
				ImportRewrite importRewrite = ImportRewrite.create(sourceCompilationUnit, true);
				for(ITypeBinding typeBinding : requiredImportTypeBindings) {
					if(!typeBinding.isNested())
						importRewrite.addImport(typeBinding);
				}
				
				TextEdit importEdit = importRewrite.rewriteImports(null);
				if(importRewrite.getCreatedImports().length > 0) {
					change.getEdit().addChild(importEdit);
					change.addTextEditGroup(new TextEditGroup("Add required import declarations", new TextEdit[] {importEdit}));
				}
				
				TextEdit sourceEdit = sourceRewriter.rewriteAST();
				change.getEdit().addChild(sourceEdit);
				change.addTextEditGroup(new TextEditGroup("Create method for the extracted duplicated code", new TextEdit[] {sourceEdit}));
			}
			if(document != null) {
				for(ITypeBinding typeBinding : requiredImportTypeBindings) {
					addImportDeclaration(typeBinding, sourceCompilationUnit, sourceRewriter);
				}
				TextEdit intermediateEdit = sourceRewriter.rewriteAST(document, null);
				intermediateEdit.apply(document);
				CreateCompilationUnitChange createCompilationUnitChange =
						new CreateCompilationUnitChange(sourceICompilationUnit, document.get(), file.getCharset());
				createCompilationUnitChanges.put(sourceICompilationUnit, createCompilationUnitChange);
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
		for(PDGNodeMapping pdgNodeMapping : sortedNodeMappings) {
			PDGNode node1 = pdgNodeMapping.getNodeG1();
			PDGNode node2 = pdgNodeMapping.getNodeG2();
			if(node1.usesLocalVariable(variable1) && node2.usesLocalVariable(variable2)) {
				List<ASTNodeDifference> differences = pdgNodeMapping.getNonOverlappingNodeDifferences();
				if(differences.isEmpty())
					return true;
				for(ASTNodeDifference difference : differences) {
					BindingSignaturePair signaturePair = difference.getBindingSignaturePair();
					if(!signaturePair.getSignature1().containsBinding(variableDeclaration1.resolveBinding().getKey()) &&
							!signaturePair.getSignature2().containsBinding(variableDeclaration2.resolveBinding().getKey())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private Statement processCloneStructureNode(CloneStructureNode node, AST ast, ASTRewrite sourceRewriter) {
		PDGNodeMapping nodeMapping = (PDGNodeMapping) node.getMapping();
		PDGNode nodeG1 = nodeMapping.getNodeG1();
		Statement oldStatement = nodeG1.getASTStatement();
		Statement newStatement = null;
		if(oldStatement instanceof IfStatement) {
			IfStatement oldIfStatement = (IfStatement)oldStatement;
			IfStatement newIfStatement = ast.newIfStatement();
			Expression newIfExpression = (Expression)processASTNodeWithDifferences(ast, sourceRewriter, oldIfStatement.getExpression(), nodeMapping.getNonOverlappingNodeDifferences());
			sourceRewriter.set(newIfStatement, IfStatement.EXPRESSION_PROPERTY, newIfExpression, null);
			List<CloneStructureNode> trueControlDependentChildren = new ArrayList<CloneStructureNode>();
			List<CloneStructureNode> falseControlDependentChildren = new ArrayList<CloneStructureNode>();
			for(CloneStructureNode child : node.getChildren()) {
				if(child.getMapping() instanceof PDGNodeMapping) {
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
							if(controlDependence1.isTrueControlDependence() && controlDependence2.isTrueControlDependence()) {
								trueControlDependentChildren.add(child);
							}
							else if(controlDependence1.isFalseControlDependence() && controlDependence2.isFalseControlDependence()) {
								falseControlDependentChildren.add(child);
							}
						}
						else {
							if(isNestedUnderElse(childNodeG1) && isNestedUnderElse(childNodeG2)) {
								falseControlDependentChildren.add(child);
							}
							else if(!isNestedUnderElse(childNodeG1) && !isNestedUnderElse(childNodeG2)) {
								trueControlDependentChildren.add(child);
							}
						}
					}
				}
				else if(child.getMapping() instanceof PDGElseMapping) {
					for(CloneStructureNode child2 : child.getChildren()) {
						if(child2.getMapping() instanceof PDGNodeMapping) {
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
									if(controlDependence1.isTrueControlDependence() && controlDependence2.isTrueControlDependence()) {
										trueControlDependentChildren.add(child2);
									}
									else if(controlDependence1.isFalseControlDependence() && controlDependence2.isFalseControlDependence()) {
										falseControlDependentChildren.add(child2);
									}
								}
								else {
									if(isNestedUnderElse(childNodeG1) && isNestedUnderElse(childNodeG2)) {
										falseControlDependentChildren.add(child2);
									}
									else if(!isNestedUnderElse(childNodeG1) && !isNestedUnderElse(childNodeG2)) {
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
				if(child.getMapping() instanceof PDGNodeMapping) {
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
				if(child.getMapping() instanceof PDGNodeMapping) {
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
				if(child.getMapping() instanceof PDGNodeMapping) {
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
				if(child.getMapping() instanceof PDGNodeMapping) {
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
				if(child.getMapping() instanceof PDGNodeMapping) {
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
				if(child.getMapping() instanceof PDGNodeMapping) {
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
				if(child.getMapping() instanceof PDGNodeMapping) {
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
		return newStatement;
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
			return oldASTNode;
		}
		else {
			Set<String> parameterBindingKeys = originalPassedParameters.keySet();
			Set<String> declaredLocalVariableBindingKeys = mapper.getDeclaredLocalVariablesInMappedNodes().keySet();
			ASTNode newASTNode = ASTNode.copySubtree(ast, oldASTNode);
			for(ASTNodeDifference difference : differences) {
				Expression oldExpression = difference.getExpression1().getExpression();
				oldExpression = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(oldExpression);
				boolean isCommonParameter = false;
				if(oldExpression instanceof SimpleName) {
					SimpleName oldSimpleName = (SimpleName)oldExpression;
					IBinding binding = oldSimpleName.resolveBinding();
					if(parameterBindingKeys.contains(binding.getKey()) || declaredLocalVariableBindingKeys.contains(binding.getKey()))
						isCommonParameter = true;
				}
				else if(oldExpression instanceof QualifiedName) {
					QualifiedName oldQualifiedName = (QualifiedName)oldExpression;
					SimpleName oldSimpleName = oldQualifiedName.getName();
					IBinding binding = oldSimpleName.resolveBinding();
					if(parameterBindingKeys.contains(binding.getKey()) || declaredLocalVariableBindingKeys.contains(binding.getKey()))
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

	private Expression createArgument(AST ast, ASTNodeDifference argumentDifference) {
		Expression argument;
		if(parameterizedDifferenceMap.containsKey(argumentDifference.getBindingSignaturePair())) {
			List<BindingSignaturePair> list = new ArrayList<BindingSignaturePair>(parameterizedDifferenceMap.keySet());
			int index = list.indexOf(argumentDifference.getBindingSignaturePair());
			argument = ast.newSimpleName("arg" + index);
		}
		else {
			argument = ast.newSimpleName("arg" + parameterizedDifferenceMap.size());
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
		AST ast = typeDeclaration.getAST();
		if(intermediateClassName != null) {
			modifySuperclassType(compilationUnit, typeDeclaration, intermediateClassName);
		}
		for(MethodDeclaration methodDeclaration : methodDeclarationsToBePulledUp) {
			ASTRewrite rewriter = ASTRewrite.create(ast);
			ListRewrite bodyRewrite = rewriter.getListRewrite(typeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			bodyRewrite.remove(methodDeclaration, null);
			try {
				TextEdit sourceEdit = rewriter.rewriteAST();
				ICompilationUnit sourceICompilationUnit = (ICompilationUnit)compilationUnit.getJavaElement();
				CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
				change.getEdit().addChild(sourceEdit);
				change.addTextEditGroup(new TextEditGroup("Pull up method to superclass", new TextEdit[] {sourceEdit}));
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		FieldDeclaration[] fieldDeclarations = typeDeclaration.getFields();
		for(VariableDeclaration variableDeclaration : fieldDeclarationsToBePulledUp) {
			boolean found = false;
			ASTRewrite rewriter = ASTRewrite.create(ast);
			ListRewrite bodyRewrite = rewriter.getListRewrite(typeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
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
				change.getEdit().addChild(sourceEdit);
				change.addTextEditGroup(new TextEditGroup("Pull up field to superclass", new TextEdit[] {sourceEdit}));
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
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
			TreeSet<PDGNode> remainingNodesMovableBefore, TreeSet<PDGNode> remainingNodesMovableAfter, List<VariableDeclaration> returnedVariables, int index) {
		AST ast = methodDeclaration.getAST();
		ASTRewrite methodBodyRewriter = ASTRewrite.create(ast);
		MethodInvocation methodInvocation = ast.newMethodInvocation();
		methodBodyRewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(extractedMethodName), null);
		ListRewrite argumentsRewrite = methodBodyRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
		for(String parameterName : originalPassedParameters.keySet()) {
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
				if(expression != null)
					argumentsRewrite.insertLast(expression, null);
				else
					argumentsRewrite.insertLast(ast.newThisExpression(), null);
			}
		}
		//place the code in the parent block of the first removable node
		Statement firstStatement = removableNodes.first().getASTStatement();
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
			//create a variable declaration statement
			VariableDeclaration variableDeclaration = returnedVariables.get(0);
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
			blockRewrite.insertBefore(newVariableDeclarationStatement, firstStatement, null);
			extractedMethodInvocationStatement = newVariableDeclarationStatement;
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
			extractedMethodInvocationStatement = methodInvocationStatement;
		}
		for(Statement movedBefore : statementsToBeMovedBefore) {
			blockRewrite.insertBefore(movedBefore, extractedMethodInvocationStatement, null);
		}
		for(int i=statementsToBeMovedAfter.size()-1; i>=0; i--) {
			Statement movedAfter = statementsToBeMovedAfter.get(i);
			blockRewrite.insertAfter(movedAfter, extractedMethodInvocationStatement, null);
		}
		for(PDGNode pdgNode : removableNodes) {
			Statement statement = pdgNode.getASTStatement();
			methodBodyRewriter.remove(statement, null);
		}
		Set<LabeledStatement> labeledStatements = labeledStatementsToBeRemoved.get(index);
		for(LabeledStatement labeled : labeledStatements) {
			methodBodyRewriter.remove(labeled, null);
		}
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
