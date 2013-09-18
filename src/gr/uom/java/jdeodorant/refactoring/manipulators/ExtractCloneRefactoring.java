package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.AbstractMethodDeclaration;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.decomposition.ASTNodeDifference;
import gr.uom.java.ast.decomposition.BindingSignaturePair;
import gr.uom.java.ast.decomposition.DifferenceType;
import gr.uom.java.ast.decomposition.DualExpressionPreconditionViolation;
import gr.uom.java.ast.decomposition.ExpressionPreconditionViolation;
import gr.uom.java.ast.decomposition.PreconditionViolation;
import gr.uom.java.ast.decomposition.ReturnedVariablePreconditionViolation;
import gr.uom.java.ast.decomposition.StatementPreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.CFGBranchDoLoopNode;
import gr.uom.java.ast.decomposition.cfg.CFGNode;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGExitNode;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.decomposition.cfg.mapping.CloneStructureNode;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGElseMapping;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGNodeMapping;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGSubTreeMapper;
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
	private PDGSubTreeMapper mapper;
	private List<CompilationUnit> sourceCompilationUnits;
	private List<TypeDeclaration> sourceTypeDeclarations;
	private List<MethodDeclaration> sourceMethodDeclarations;
	private List<Set<VariableDeclaration>> fieldDeclarationsToBePulledUp;
	private Map<ICompilationUnit, CompilationUnitChange> compilationUnitChanges;
	private Map<ICompilationUnit, CreateCompilationUnitChange> createCompilationUnitChanges;
	private Set<PDGNodeMapping> sortedNodeMappings;
	private List<TreeSet<PDGNode>> removableStatements;
	private List<TreeSet<PDGNode>> remainingStatements;
	private Map<String, ArrayList<VariableDeclaration>> originalPassedParameters;
	private Map<BindingSignaturePair, ASTNodeDifference> parameterizedDifferenceMap;
	private List<ArrayList<VariableDeclaration>> returnedVariables;
	private String intermediateClassName;
	private String extractedMethodName;
	
	public ExtractCloneRefactoring(PDGSubTreeMapper mapper) {
		super();
		this.mapper = mapper;
		AbstractMethodDeclaration methodObject1 = mapper.getPDG1().getMethod();
		AbstractMethodDeclaration methodObject2 = mapper.getPDG2().getMethod();
		MethodDeclaration methodDeclaration1 = methodObject1.getMethodDeclaration();
		MethodDeclaration methodDeclaration2 = methodObject2.getMethodDeclaration();
		
		this.sourceCompilationUnits = new ArrayList<CompilationUnit>();
		this.sourceTypeDeclarations = new ArrayList<TypeDeclaration>();
		this.sourceMethodDeclarations = new ArrayList<MethodDeclaration>();
		this.removableStatements = new ArrayList<TreeSet<PDGNode>>();
		removableStatements.add(mapper.getRemovableNodesG1());
		removableStatements.add(mapper.getRemovableNodesG2());
		this.remainingStatements = new ArrayList<TreeSet<PDGNode>>();
		remainingStatements.add(mapper.getRemainingNodesG1());
		remainingStatements.add(mapper.getRemainingNodesG2());
		this.returnedVariables = new ArrayList<ArrayList<VariableDeclaration>>();
		returnedVariables.add(new ArrayList<VariableDeclaration>(mapper.getDeclaredVariablesInMappedNodesUsedByNonMappedNodesG1()));
		returnedVariables.add(new ArrayList<VariableDeclaration>(mapper.getDeclaredVariablesInMappedNodesUsedByNonMappedNodesG2()));
		this.fieldDeclarationsToBePulledUp = new ArrayList<Set<VariableDeclaration>>();
		for(int i=0; i<2; i++) {
			fieldDeclarationsToBePulledUp.add(new LinkedHashSet<VariableDeclaration>());
		}
		this.compilationUnitChanges = new LinkedHashMap<ICompilationUnit, CompilationUnitChange>();
		this.createCompilationUnitChanges = new LinkedHashMap<ICompilationUnit, CreateCompilationUnitChange>();
		
		this.sourceMethodDeclarations.add(methodDeclaration1);
		this.sourceMethodDeclarations.add(methodDeclaration2);
		this.sourceTypeDeclarations.add((TypeDeclaration)methodDeclaration1.getParent());
		this.sourceTypeDeclarations.add((TypeDeclaration)methodDeclaration2.getParent());
		this.sourceCompilationUnits.add((CompilationUnit)methodDeclaration1.getRoot());
		this.sourceCompilationUnits.add((CompilationUnit)methodDeclaration2.getRoot());
		this.originalPassedParameters = new LinkedHashMap<String, ArrayList<VariableDeclaration>>();
		this.parameterizedDifferenceMap = new LinkedHashMap<BindingSignaturePair, ASTNodeDifference>();
		this.sortedNodeMappings = new TreeSet<PDGNodeMapping>(mapper.getMaximumStateWithMinimumDifferences().getNodeMappings());
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
	}

	public PDGSubTreeMapper getMapper() {
		return mapper;
	}

	private ITypeBinding commonSuperType(ITypeBinding typeBinding1, ITypeBinding typeBinding2) {
		Set<ITypeBinding> superTypes1 = getAllSuperTypes(typeBinding1);
		Set<ITypeBinding> superTypes2 = getAllSuperTypes(typeBinding2);
		boolean found = false;
		ITypeBinding commonSuperType = null;
		for(ITypeBinding superType1 : superTypes1) {
			for(ITypeBinding superType2 : superTypes2) {
				if(superType1.isEqualTo(superType2)) {
					commonSuperType = superType1;
					found = true;
					break;
				}
			}
			if(found)
				break;
		}
		return commonSuperType;
	}

	private Set<ITypeBinding> getAllSuperTypes(ITypeBinding typeBinding) {
		Set<ITypeBinding> superTypes = new LinkedHashSet<ITypeBinding>();
		ITypeBinding superTypeBinding = typeBinding.getSuperclass();
		if(superTypeBinding != null) {
			superTypes.add(superTypeBinding);
			superTypes.addAll(getAllSuperTypes(superTypeBinding));
		}
		return superTypes;
	}

	public void apply() {
		extractClone();
		for(int i=0; i<sourceCompilationUnits.size(); i++) {
			modifySourceClass(sourceCompilationUnits.get(i), sourceTypeDeclarations.get(i), fieldDeclarationsToBePulledUp.get(i));
			modifySourceMethod(sourceCompilationUnits.get(i), sourceMethodDeclarations.get(i), removableStatements.get(i), remainingStatements.get(i), returnedVariables.get(i), i);
		}
	}

	private ITypeBinding findReturnTypeBinding() {
		List<ITypeBinding> returnedTypeBindings = new ArrayList<ITypeBinding>();
		for(PDGNodeMapping pdgNodeMapping : sortedNodeMappings) {
			PDGNode pdgNode = pdgNodeMapping.getNodeG1();
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
		if(returnedTypeBindings.size() == 1) {
			return returnedTypeBindings.get(0);
		}
		return null;
	}

	private void extractClone() {
		CompilationUnit sourceCompilationUnit = null;
		ICompilationUnit sourceICompilationUnit = null;
		TypeDeclaration sourceTypeDeclaration = null;
		ASTRewrite sourceRewriter = null;
		AST ast = null;
		Document document = null;
		IFile file = null;
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
			ITypeBinding commonSuperType = commonSuperType(typeBinding1, typeBinding2);
			if(commonSuperType != null) {
				CompilationUnitCache cache = CompilationUnitCache.getInstance();
				Set<IType> subTypes = cache.getSubTypes((IType)commonSuperType.getJavaElement());
				boolean superclassInheritedOnlyByRefactoringSubclasses = false;
				if(subTypes.size() == 2 && subTypes.contains((IType)typeBinding1.getJavaElement()) &&
						subTypes.contains((IType)typeBinding2.getJavaElement()))
					superclassInheritedOnlyByRefactoringSubclasses = true;
				if((mapper.getAccessedLocalFieldsG1().isEmpty() && mapper.getAccessedLocalFieldsG2().isEmpty() &&
						mapper.getAccessedLocalMethodsG1().isEmpty() && mapper.getAccessedLocalMethodsG2().isEmpty() &&
						 !commonSuperType.getQualifiedName().equals("java.lang.Object")) ||
						 superclassInheritedOnlyByRefactoringSubclasses) {
					//OR if the superclass in inherited ONLY by the subclasses participating in the refactoring
					IJavaElement javaElement = commonSuperType.getJavaElement();
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
							if(typeDeclaration.resolveBinding().isEqualTo(commonSuperType)) {
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
					this.intermediateClassName = "Intermediate" + commonSuperType.getName();
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
								intermediateAST.newSimpleType(intermediateAST.newSimpleName(commonSuperType.getName())), null);
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
		
		if(!sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclarations.get(1).resolveBinding())) {
			Set<VariableDeclaration> accessedLocalFieldsG1 = mapper.getAccessedLocalFieldsG1();
			Set<VariableDeclaration> accessedLocalFieldsG2 = mapper.getAccessedLocalFieldsG2();
			for(VariableDeclaration localFieldG1 : accessedLocalFieldsG1) {
				FieldDeclaration originalFieldDeclarationG1 = (FieldDeclaration)localFieldG1.getParent();
				for(VariableDeclaration localFieldG2 : accessedLocalFieldsG2) {
					FieldDeclaration originalFieldDeclarationG2 = (FieldDeclaration)localFieldG2.getParent();
					if(localFieldG1.getName().getIdentifier().equals(localFieldG2.getName().getIdentifier()) &&
							originalFieldDeclarationG1.getType().resolveBinding().isEqualTo(originalFieldDeclarationG2.getType().resolveBinding())) {
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

			Set<MethodInvocationObject> accessedLocalMethodsG1 = mapper.getAccessedLocalMethodsG1();
			Set<MethodInvocationObject> accessedLocalMethodsG2 = mapper.getAccessedLocalMethodsG2();
			for(MethodInvocationObject localMethodG1 : accessedLocalMethodsG1) {
				for(MethodInvocationObject localMethodG2 : accessedLocalMethodsG2) {
					if(localMethodG1.getMethodName().equals(localMethodG2.getMethodName()) &&
							localMethodG1.getReturnType().equals(localMethodG2.getReturnType()) &&
							localMethodG1.getParameterTypeList().equals(localMethodG2.getParameterTypeList())) {
						MethodDeclaration[] methodDeclarationsG1 = sourceTypeDeclarations.get(0).getMethods();
						IMethodBinding localMethodBindingG1 = localMethodG1.getMethodInvocation().resolveMethodBinding();
						for(MethodDeclaration methodDeclarationG1 : methodDeclarationsG1) {
							if(methodDeclarationG1.resolveBinding().isEqualTo(localMethodBindingG1)) {
								MethodDeclaration newMethodDeclaration = ast.newMethodDeclaration();
								sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, ast.newSimpleName(methodDeclarationG1.getName().getIdentifier()), null);
								sourceRewriter.set(newMethodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, methodDeclarationG1.getReturnType2(), null);	
								ListRewrite modifiersRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
								List<IExtendedModifier> originalModifiers = methodDeclarationG1.modifiers();
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
								List<SingleVariableDeclaration> parameters = methodDeclarationG1.parameters();
								for(SingleVariableDeclaration parameter : parameters) {
									parametersRewrite.insertLast(parameter, null);
								}
								ListRewrite thrownExceptionsRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
								List<Name> thrownExceptions = methodDeclarationG1.thrownExceptions();
								for(Name thrownException : thrownExceptions) {
									thrownExceptionsRewrite.insertLast(thrownException, null);
								}
								bodyDeclarationsRewrite.insertLast(newMethodDeclaration, null);
								break;
							}
						}
						break;
					}
				}
			}
		}
		
		MethodDeclaration newMethodDeclaration = ast.newMethodDeclaration();
		extractedMethodName = sourceMethodDeclaration.getName().getIdentifier();
		sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, ast.newSimpleName(extractedMethodName), null);
		List<VariableDeclaration> returnedVariables = this.returnedVariables.get(0);
		ITypeBinding returnTypeBinding = null;
		if(returnedVariables.size() == 1) {
			Type returnType = extractType(returnedVariables.get(0));
			returnTypeBinding = returnType.resolveBinding();
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
		if(sourceTypeDeclarations.get(0).resolveBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
			Modifier accessModifier = newMethodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD);
			modifierRewrite.insertLast(accessModifier, null);
		}
		else {
			Modifier accessModifier = newMethodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD);
			modifierRewrite.insertLast(accessModifier, null);
		}
		
		ListRewrite parameterRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		Map<String, ArrayList<VariableDeclaration>> commonPassedParameters = mapper.getCommonPassedParameters();
		for(String parameterName : commonPassedParameters.keySet()) {
			ArrayList<VariableDeclaration> variableDeclarations = commonPassedParameters.get(parameterName);
			VariableDeclaration variableDeclaration1 = variableDeclarations.get(0);
			VariableDeclaration variableDeclaration2 = variableDeclarations.get(1);
			if(parameterIsUsedByNodesWithoutDifferences(variableDeclaration1, variableDeclaration2)) {
				if(!variableDeclaration1.resolveBinding().isField()) {
					Type variableType = extractType(variableDeclaration1);
					Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
					typeBindings.add(variableType.resolveBinding());
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
		if(returnedVariables.size() == 1 && findReturnTypeBinding() == null) {
			ReturnStatement returnStatement = ast.newReturnStatement();
			sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, returnedVariables.get(0).getName(), null);
			methodBodyRewrite.insertLast(returnStatement, null);
		}
		
		//add parameters for the differences between the clones
		int i = 0;
		for(ASTNodeDifference difference : parameterizedDifferenceMap.values()) {
			Expression expression1 = difference.getExpression1().getExpression();
			Expression expression2 = difference.getExpression2().getExpression();
			boolean isReturnedVariable = isReturnedVariable(expression1, this.returnedVariables.get(0));
			if(!isReturnedVariable) {
				ITypeBinding typeBinding = null;
				if(difference.containsDifferenceType(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
					ITypeBinding typeBinding1 = expression1.resolveTypeBinding();
					ITypeBinding typeBinding2 = expression2.resolveTypeBinding();
					ITypeBinding commonSuperTypeBinding = commonSuperType(typeBinding1, typeBinding2);
					if(commonSuperTypeBinding != null) {
						typeBinding = commonSuperTypeBinding;
					}
				}
				else {
					typeBinding = expression1.resolveTypeBinding();
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

	private Type generateTypeFromTypeBinding(ITypeBinding typeBinding, AST ast, ASTRewrite rewriter) {
		Type type = null;
		if(typeBinding.isClass() || typeBinding.isInterface()) {
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
		else if(typeBinding.isParameterizedType()) {
			type = createParameterizedType(ast, typeBinding, rewriter);
		}
		return type;
	}

	private ParameterizedType createParameterizedType(AST ast, ITypeBinding typeBinding, ASTRewrite rewriter) {
		ITypeBinding erasure = typeBinding.getErasure();
		ITypeBinding[] typeArguments = typeBinding.getTypeArguments();
		ParameterizedType parameterizedType = ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName(erasure.getName())));
		ListRewrite typeArgumentsRewrite = rewriter.getListRewrite(parameterizedType, ParameterizedType.TYPE_ARGUMENTS_PROPERTY);
		for(ITypeBinding typeArgument : typeArguments) {
			if(typeArgument.isClass() || typeArgument.isInterface())
				typeArgumentsRewrite.insertLast(ast.newSimpleType(ast.newSimpleName(typeArgument.getName())), null);
			else if(typeArgument.isParameterizedType()) {
				typeArgumentsRewrite.insertLast(createParameterizedType(ast, typeArgument, rewriter), null);
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
			ExpressionExtractor expressionExtractor = new ExpressionExtractor();
			ASTNode newASTNode = ASTNode.copySubtree(ast, oldASTNode);
			for(ASTNodeDifference difference : differences) {
				Expression oldExpression = difference.getExpression1().getExpression();
				boolean isCommonParameter = false;
				if(isMethodName(oldExpression)) {
					SimpleName oldSimpleName = (SimpleName)oldExpression;
					IBinding binding = oldSimpleName.resolveBinding();
					//get method invocation from method name
					oldExpression = (Expression)oldExpression.getParent();
					if(parameterBindingKeys.contains(binding.getKey()) || declaredLocalVariableBindingKeys.contains(binding.getKey()))
						isCommonParameter = true;
				}
				else if(oldExpression instanceof SimpleName) {
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
					if(oldExpression.getParent() instanceof Type) {
						Type oldType = (Type)oldExpression.getParent();
						if(difference.containsDifferenceType(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
							ITypeBinding typeBinding1 = difference.getExpression1().getExpression().resolveTypeBinding();
							ITypeBinding typeBinding2 = difference.getExpression2().getExpression().resolveTypeBinding();
							ITypeBinding commonSuperTypeBinding = commonSuperType(typeBinding1, typeBinding2);
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
						SimpleName argument;
						if(parameterizedDifferenceMap.containsKey(difference.getBindingSignaturePair())) {
							List<BindingSignaturePair> list = new ArrayList<BindingSignaturePair>(parameterizedDifferenceMap.keySet());
							int index = list.indexOf(difference.getBindingSignaturePair());
							argument = ast.newSimpleName("arg" + index);
						}
						else {
							argument = ast.newSimpleName("arg" + parameterizedDifferenceMap.size());
							parameterizedDifferenceMap.put(difference.getBindingSignaturePair(), difference);
						}
						int j = 0;
						List<Expression> oldExpressions = expressionExtractor.getAllExpressions(oldASTNode);
						List<Expression> newExpressions = expressionExtractor.getAllExpressions(newASTNode);
						for(Expression expression : oldExpressions) {
							Expression newExpression = newExpressions.get(j);
							if(expression.equals(oldExpression)) {
								sourceRewriter.replace(newExpression, argument, null);
								break;
							}
							if(oldExpression instanceof QualifiedName) {
								QualifiedName oldQualifiedName = (QualifiedName)oldExpression;
								if(expression.equals(oldQualifiedName.getName())) {
									sourceRewriter.replace(newExpression.getParent(), argument, null);
									break;
								}
							}
							j++;
						}
					}
				}
			}
			return newASTNode;
		}
	}

	private void modifySourceClass(CompilationUnit compilationUnit, TypeDeclaration typeDeclaration, Set<VariableDeclaration> fieldDeclarationsToBePulledUp) {
		AST ast = typeDeclaration.getAST();
		if(intermediateClassName != null) {
			ASTRewrite superClassTypeRewriter = ASTRewrite.create(ast);
			superClassTypeRewriter.set(typeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, ast.newSimpleType(ast.newSimpleName(intermediateClassName)), null);
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

	private void modifySourceMethod(CompilationUnit compilationUnit, MethodDeclaration methodDeclaration, TreeSet<PDGNode> removableNodes,
			TreeSet<PDGNode> remainingNodes, List<VariableDeclaration> returnedVariables, int index) {
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
			Expression expression1 = difference.getExpression1().getExpression();
			expressions.add(isMethodName(expression1) ? (Expression)expression1.getParent() : expression1);
			Expression expression2 = difference.getExpression2().getExpression();
			expressions.add(isMethodName(expression2) ? (Expression)expression2.getParent() : expression2);
			Expression expression = expressions.get(index);
			boolean isReturnedVariable = isReturnedVariable(expression, returnedVariables);
			if(!isReturnedVariable)
				argumentsRewrite.insertLast(expression, null);
		}
		//place the code in the parent block of the first removable node
		Statement firstStatement = removableNodes.first().getASTStatement();
		Block parentBlock = (Block)firstStatement.getParent();
		ListRewrite blockRewrite = methodBodyRewriter.getListRewrite(parentBlock, Block.STATEMENTS_PROPERTY);
		for(PDGNode remainingNode : remainingNodes) {
			if(remainingNode.getId() >= removableNodes.first().getId() && remainingNode.getId() <= removableNodes.last().getId()) {
				blockRewrite.insertBefore(remainingNode.getASTStatement(), firstStatement, null);
			}
		}
		if(returnedVariables.size() == 1) {
			//create a variable declaration statement
			VariableDeclaration variableDeclaration = returnedVariables.get(0);
			VariableDeclarationFragment newFragment = ast.newVariableDeclarationFragment();
			methodBodyRewriter.set(newFragment, VariableDeclarationFragment.NAME_PROPERTY, variableDeclaration.getName(), null);
			methodBodyRewriter.set(newFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, methodInvocation, null);
			VariableDeclarationStatement newVariableDeclarationStatement = ast.newVariableDeclarationStatement(newFragment);
			Type variableType = extractType(variableDeclaration);
			methodBodyRewriter.set(newVariableDeclarationStatement, VariableDeclarationStatement.TYPE_PROPERTY, variableType, null);
			blockRewrite.insertBefore(newVariableDeclarationStatement, firstStatement, null);
		}
		else {
			ITypeBinding returnTypeBinding = findReturnTypeBinding();
			Statement methodInvocationStatement = null;
			if(returnTypeBinding != null) {
				ReturnStatement returnStatement = ast.newReturnStatement();
				methodBodyRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, methodInvocation, null);
				methodInvocationStatement = returnStatement;
			}
			else {
				methodInvocationStatement = ast.newExpressionStatement(methodInvocation);
			}
			blockRewrite.insertBefore(methodInvocationStatement, firstStatement, null);
		}
		for(PDGNode pdgNode : removableNodes) {
			Statement statement = pdgNode.getASTStatement();
			methodBodyRewriter.remove(statement, null);
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
					CompilationUnit cu = (CompilationUnit)expression.getRoot();
					RefactoringStatusContext context = JavaStatusContext.create(cu.getTypeRoot(), expression);
					status.merge(RefactoringStatus.createErrorStatus(violation.getViolation(), context));
				}
				else if(violation instanceof DualExpressionPreconditionViolation) {
					DualExpressionPreconditionViolation dualExpressionViolation = (DualExpressionPreconditionViolation)violation;
					Expression expression1 = dualExpressionViolation.getExpression1().getExpression();
					CompilationUnit cu1 = (CompilationUnit)expression1.getRoot();
					RefactoringStatusContext context1 = JavaStatusContext.create(cu1.getTypeRoot(), expression1);
					status.merge(RefactoringStatus.createErrorStatus(violation.getViolation(), context1));
					
					Expression expression2 = dualExpressionViolation.getExpression2().getExpression();
					CompilationUnit cu2 = (CompilationUnit)expression2.getRoot();
					RefactoringStatusContext context2 = JavaStatusContext.create(cu2.getTypeRoot(), expression2);
					status.merge(RefactoringStatus.createErrorStatus(violation.getViolation(), context2));
				}
				else if(violation instanceof ReturnedVariablePreconditionViolation) {
					status.merge(RefactoringStatus.createErrorStatus(violation.getViolation()));
				}
			}
		} finally {
			pm.done();
		}
		return status;
	}

	private boolean isMethodName(Expression expression) {
		if(expression instanceof SimpleName) {
			SimpleName simpleName = (SimpleName)expression;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null && binding.getKind() == IBinding.METHOD) {
				if(expression.getParent() instanceof Expression) {
					return true;
				}
			}
		}
		return false;
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
