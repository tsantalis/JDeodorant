package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.decomposition.ASTNodeDifference;
import gr.uom.java.ast.decomposition.DifferenceType;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CFGBranchDoLoopNode;
import gr.uom.java.ast.decomposition.cfg.CFGNode;
import gr.uom.java.ast.decomposition.cfg.PDGControlPredicateNode;
import gr.uom.java.ast.decomposition.cfg.PDGExitNode;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PDGTryNode;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGMapper;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGNodeMapping;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.StatementExtractor;
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
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
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
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreateCompilationUnitChange;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ExtractCloneRefactoring extends ExtractMethodFragmentRefactoring {
	private PDGMapper mapper;
	private List<CompilationUnit> sourceCompilationUnits;
	private List<TypeDeclaration> sourceTypeDeclarations;
	private List<MethodDeclaration> sourceMethodDeclarations;
	private List<Set<VariableDeclaration>> fieldDeclarationsToBePulledUp;
	private Map<ICompilationUnit, CompilationUnitChange> compilationUnitChanges;
	private Map<ICompilationUnit, CreateCompilationUnitChange> createCompilationUnitChanges;
	private Set<PDGNodeMapping> sortedNodeMappings;
	private List<Set<PDGNode>> removableStatements;
	private List<TreeSet<PDGNode>> remainingStatements;
	private List<ASTNodeDifference> parameterizedDifferences;
	private String intermediateClassName;
	
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
		this.removableStatements = new ArrayList<Set<PDGNode>>();
		removableStatements.add(mapper.getRemovableNodesG1());
		removableStatements.add(mapper.getRemovableNodesG2());
		this.remainingStatements = new ArrayList<TreeSet<PDGNode>>();
		remainingStatements.add(mapper.getRemainingNodesG1());
		remainingStatements.add(mapper.getRemainingNodesG2());
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
		this.parameterizedDifferences = new ArrayList<ASTNodeDifference>();
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
			modifySourceMethod(sourceCompilationUnits.get(i), sourceMethodDeclarations.get(i), removableStatements.get(i), remainingStatements.get(i), i);
		}
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
				if(mapper.getAccessedLocalFieldsG1().isEmpty() && mapper.getAccessedLocalFieldsG2().isEmpty() &&
						mapper.getAccessedLocalMethodsG1().isEmpty() && mapper.getAccessedLocalMethodsG2().isEmpty()) {
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
							modifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
							modifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD), null);
							ListRewrite parametersRewrite = sourceRewriter.getListRewrite(newMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
							List<SingleVariableDeclaration> parameters = methodDeclarationG1.parameters();
							for(SingleVariableDeclaration parameter : parameters) {
								parametersRewrite.insertLast(parameter, null);
							}
							bodyDeclarationsRewrite.insertLast(newMethodDeclaration, null);
							break;
						}
					}
					break;
				}
			}
		}
		
		MethodDeclaration newMethodDeclaration = ast.newMethodDeclaration();
		sourceRewriter.set(newMethodDeclaration, MethodDeclaration.NAME_PROPERTY, ast.newSimpleName(sourceMethodDeclaration.getName().getIdentifier()), null);
		Type returnType = findReturnType();
		if(returnType != null) {
			Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
			typeBindings.add(returnType.resolveBinding());
			getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
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
			List<VariableDeclaration> variableDeclarations = commonPassedParameters.get(parameterName);
			VariableDeclaration variableDeclaration = variableDeclarations.get(0);
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
				Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
				typeBindings.add(variableType.resolveBinding());
				getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
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
			Statement statement = pdgNode.getASTStatement();
			TypeVisitor typeVisitor = new TypeVisitor();
			statement.accept(typeVisitor);
			requiredImportTypeBindings.addAll(typeVisitor.getTypeBindings());
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
					processStatementNode(bodyRewrite, node, ast, sourceRewriter);
					sliceNodes.remove(node);
				}
			}
		}
		
		//add parameters for the differences between the clones
		int i = 0;
		for(ASTNodeDifference difference : parameterizedDifferences) {
			ITypeBinding typeBinding = null;
			if(difference.containsDifferenceType(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
				ITypeBinding typeBinding1 = difference.getExpression1().getExpression().resolveTypeBinding();
				ITypeBinding typeBinding2 = difference.getExpression2().getExpression().resolveTypeBinding();
				ITypeBinding commonSuperTypeBinding = commonSuperType(typeBinding1, typeBinding2);
				if(commonSuperTypeBinding != null) {
					typeBinding = commonSuperTypeBinding;
				}
			}
			else {
				typeBinding = difference.getExpression1().getExpression().resolveTypeBinding();
			}
			Type type = generateTypeFromTypeBinding(typeBinding, ast, sourceRewriter);
			Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
			typeBindings.add(typeBinding);
			getSimpleTypeBindings(typeBindings, requiredImportTypeBindings);
			SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
			sourceRewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, ast.newSimpleName("arg" + i), null);
			sourceRewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, type, null);
			parameterRewrite.insertLast(parameter, null);
			i++;
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
			Type elementType = ast.newSimpleType(ast.newSimpleName(elementTypeBinding.getName()));
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

	protected TryStatement copyTryStatement(ASTRewrite sourceRewriter, AST ast, TryStatement tryStatementParent) {
		PDGTryNode pdgTryNode = mapper.getPDG1().getPDGTryNode(tryStatementParent);
		PDGNodeMapping tryNodeMapping = getNodeMappingForPDGNode(pdgTryNode);
		TryStatement newTryStatement = ast.newTryStatement();
		ListRewrite resourceRewrite = sourceRewriter.getListRewrite(newTryStatement, TryStatement.RESOURCES_PROPERTY);
		List<VariableDeclarationExpression> resources = tryStatementParent.resources();
		for(VariableDeclarationExpression expression : resources) {
			resourceRewrite.insertLast(expression, null);
		}
		ListRewrite catchClauseRewrite = sourceRewriter.getListRewrite(newTryStatement, TryStatement.CATCH_CLAUSES_PROPERTY);
		List<CatchClause> catchClauses = tryStatementParent.catchClauses();
		for(CatchClause catchClause : catchClauses) {
			CatchClause newCatchClause = ast.newCatchClause();
			sourceRewriter.set(newCatchClause, CatchClause.EXCEPTION_PROPERTY, catchClause.getException(), null);
			Block newCatchBody = ast.newBlock();
			ListRewrite newCatchBodyRewrite = sourceRewriter.getListRewrite(newCatchBody, Block.STATEMENTS_PROPERTY);
			List<Statement> oldStatements = catchClause.getBody().statements();
			for(Statement oldStatement : oldStatements) {
				processStatementWithDifferences(newCatchBodyRewrite, ast, sourceRewriter, oldStatement, tryNodeMapping.getNodeDifferences());
			}
			sourceRewriter.set(newCatchClause, CatchClause.BODY_PROPERTY, newCatchBody, null);
			catchClauseRewrite.insertLast(newCatchClause, null);
		}
		if(tryStatementParent.getFinally() != null) {
			Block newFinallyBody = ast.newBlock();
			ListRewrite newFinallyBodyRewrite = sourceRewriter.getListRewrite(newFinallyBody, Block.STATEMENTS_PROPERTY);
			List<Statement> oldStatements = tryStatementParent.getFinally().statements();
			for(Statement oldStatement : oldStatements) {
				processStatementWithDifferences(newFinallyBodyRewrite, ast, sourceRewriter, oldStatement, tryNodeMapping.getNodeDifferences());
			}
			sourceRewriter.set(newTryStatement, TryStatement.FINALLY_PROPERTY, newFinallyBody, null);
		}
		return newTryStatement;
	}

	protected void processStatementNode(ListRewrite bodyRewrite, PDGNode dstPDGNode, AST ast, ASTRewrite sourceRewriter) {
		PDGNodeMapping nodeMapping = getNodeMappingForPDGNode(dstPDGNode);
		Statement oldStatement = dstPDGNode.getASTStatement();
		List<ASTNodeDifference> differences = nodeMapping.getNodeDifferences();
		processStatementWithDifferences(bodyRewrite, ast, sourceRewriter, oldStatement, differences);
	}

	private void processStatementWithDifferences(ListRewrite bodyRewrite, AST ast, ASTRewrite sourceRewriter, Statement oldStatement, List<ASTNodeDifference> differences) {
		if(differences.isEmpty()) {
			bodyRewrite.insertLast(oldStatement, null);
		}
		else {
			Set<String> parameterBindingKeys = mapper.getCommonPassedParameters().keySet();
			ExpressionExtractor expressionExtractor = new ExpressionExtractor();
			Statement newStatement = (Statement)ASTNode.copySubtree(ast, oldStatement);
			for(ASTNodeDifference difference : differences) {
				Expression oldExpression = difference.getExpression1().getExpression();
				boolean isCommonParameter = false;
				if(oldExpression instanceof SimpleName) {
					SimpleName oldSimpleName = (SimpleName)oldExpression;
					IBinding binding = oldSimpleName.resolveBinding();
					if(parameterBindingKeys.contains(binding.getKey()))
						isCommonParameter = true;
				}
				if(!isCommonParameter) {
					if(!(oldExpression.getParent() instanceof Type)) {
						SimpleName argument = ast.newSimpleName("arg" + parameterizedDifferences.size());
						parameterizedDifferences.add(difference);
						int j = 0;
						List<Expression> oldExpressions = expressionExtractor.getAllExpressions(oldStatement);
						List<Expression> newExpressions = expressionExtractor.getAllExpressions(newStatement);
						for(Expression expression : oldExpressions) {
							Expression newExpression = newExpressions.get(j);
							if(expression.equals(oldExpression)) {
								sourceRewriter.replace(newExpression, argument, null);
								break;
							}
							j++;
						}
					}
					else {
						Type oldType = (Type)oldExpression.getParent();
						if(difference.containsDifferenceType(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
							ITypeBinding typeBinding1 = difference.getExpression1().getExpression().resolveTypeBinding();
							ITypeBinding typeBinding2 = difference.getExpression2().getExpression().resolveTypeBinding();
							ITypeBinding commonSuperTypeBinding = commonSuperType(typeBinding1, typeBinding2);
							if(commonSuperTypeBinding != null) {
								Type arg = generateTypeFromTypeBinding(commonSuperTypeBinding, ast, sourceRewriter);
								TypeVisitor oldTypeVisitor = new TypeVisitor();
								oldStatement.accept(oldTypeVisitor);
								List<Type> oldTypes = oldTypeVisitor.getTypes();
								TypeVisitor newTypeVisitor = new TypeVisitor();
								newStatement.accept(newTypeVisitor);
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
				}
			}
			bodyRewrite.insertLast(newStatement, null);
		}
	}

	private PDGNodeMapping getNodeMappingForPDGNode(PDGNode nodeG1) {
		for(PDGNodeMapping pdgNodeMapping : sortedNodeMappings) {
			PDGNode pdgNode = pdgNodeMapping.getNodeG1();
			if(pdgNode.equals(nodeG1))
				return pdgNodeMapping;
		}
		return null;
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

	private void modifySourceMethod(CompilationUnit compilationUnit, MethodDeclaration methodDeclaration, Set<PDGNode> removableNodes,
			TreeSet<PDGNode> remainingNodes, int index) {
		AST ast = methodDeclaration.getAST();
		ASTRewrite methodBodyRewriter = ASTRewrite.create(ast);
		MethodInvocation methodInvocation = ast.newMethodInvocation();
		methodBodyRewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(methodDeclaration.getName().getIdentifier()), null);
		Map<String, ArrayList<VariableDeclaration>> commonPassedParameters = mapper.getCommonPassedParameters();
		ListRewrite argumentsRewrite = methodBodyRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
		for(String parameterName : commonPassedParameters.keySet()) {
			List<VariableDeclaration> variableDeclarations = commonPassedParameters.get(parameterName);
			argumentsRewrite.insertLast(variableDeclarations.get(index).getName(), null);
		}
		for(ASTNodeDifference difference : parameterizedDifferences) {
			List<Expression> expressions = new ArrayList<Expression>();
			expressions.add(difference.getExpression1().getExpression());
			expressions.add(difference.getExpression2().getExpression());
			argumentsRewrite.insertLast(expressions.get(index), null);
		}
		Statement methodInvocationStatement = null;
		if(methodDeclaration.getReturnType2() != null) {
			ReturnStatement returnStatement = ast.newReturnStatement();
			methodBodyRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, methodInvocation, null);
			methodInvocationStatement = returnStatement;
		}
		else {
			methodInvocationStatement = ast.newExpressionStatement(methodInvocation);
		}
		if(!remainingNodes.isEmpty()) {
			Statement lastStatement = remainingNodes.last().getASTStatement();
			Block parentStatement = (Block)lastStatement.getParent();
			ListRewrite blockRewrite = methodBodyRewriter.getListRewrite(parentStatement, Block.STATEMENTS_PROPERTY);
			blockRewrite.insertAfter(methodInvocationStatement, lastStatement, null);
		}
		else {
			ListRewrite blockRewrite = methodBodyRewriter.getListRewrite(methodDeclaration.getBody(), Block.STATEMENTS_PROPERTY);
			blockRewrite.insertLast(methodInvocationStatement, null);
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
					finalTypeBindings.add(typeBinding.getDeclaringClass());
				}
				finalTypeBindings.add(typeBinding);
			}
		}
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
