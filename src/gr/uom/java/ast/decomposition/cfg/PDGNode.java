package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.LibraryClassStorage;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.ParameterObject;
import gr.uom.java.ast.SuperMethodInvocationObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.jdeodorant.preferences.PreferenceConstants;
import gr.uom.java.jdeodorant.refactoring.Activator;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jface.preference.IPreferenceStore;

public class PDGNode extends GraphNode implements Comparable<PDGNode> {
	private CFGNode cfgNode;
	protected Set<AbstractVariable> declaredVariables;
	protected Set<AbstractVariable> definedVariables;
	protected Set<AbstractVariable> usedVariables;
	protected Set<TypeObject> createdTypes;
	protected Set<String> thrownExceptionTypes;
	protected Set<VariableDeclaration> variableDeclarationsInMethod;
	protected Set<VariableDeclaration> fieldsAccessedInMethod;
	private Set<AbstractVariable> originalDefinedVariables;
	private Set<AbstractVariable> originalUsedVariables;
	private int maximumCallGraphAnalysisDepth;
	
	public PDGNode() {
		super();
		this.declaredVariables = new LinkedHashSet<AbstractVariable>();
		this.definedVariables = new LinkedHashSet<AbstractVariable>();
		this.usedVariables = new LinkedHashSet<AbstractVariable>();
		this.createdTypes = new LinkedHashSet<TypeObject>();
		this.thrownExceptionTypes = new LinkedHashSet<String>();
	}
	
	public PDGNode(CFGNode cfgNode, Set<VariableDeclaration> variableDeclarationsInMethod,
			Set<VariableDeclaration> fieldsAccessedInMethod) {
		super();
		this.cfgNode = cfgNode;
		this.variableDeclarationsInMethod = variableDeclarationsInMethod;
		this.fieldsAccessedInMethod = fieldsAccessedInMethod;
		this.id = cfgNode.id;
		cfgNode.setPDGNode(this);
		this.declaredVariables = new LinkedHashSet<AbstractVariable>();
		this.definedVariables = new LinkedHashSet<AbstractVariable>();
		this.usedVariables = new LinkedHashSet<AbstractVariable>();
		this.createdTypes = new LinkedHashSet<TypeObject>();
		this.thrownExceptionTypes = new LinkedHashSet<String>();
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		this.maximumCallGraphAnalysisDepth = store.getInt(PreferenceConstants.P_MAXIMUM_CALL_GRAPH_ANALYSIS_DEPTH);
	}

	public CFGNode getCFGNode() {
		return cfgNode;
	}

	public Iterator<GraphEdge> getOutgoingDependenceIterator() {
		return outgoingEdges.iterator();
	}

	public Iterator<GraphEdge> getIncomingDependenceIterator() {
		return incomingEdges.iterator();
	}

	public boolean hasIncomingControlDependenceFromMethodEntryNode() {
		for(GraphEdge edge : incomingEdges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(dependence instanceof PDGControlDependence) {
				PDGNode srcNode = (PDGNode)dependence.src;
				if(srcNode instanceof PDGMethodEntryNode)
					return true;
			}
		}
		return false;
	}

	public boolean declaresLocalVariable(AbstractVariable variable) {
		return declaredVariables.contains(variable);
	}

	public boolean definesLocalVariable(AbstractVariable variable) {
		return definedVariables.contains(variable);
	}

	public boolean usesLocalVariable(AbstractVariable variable) {
		return usedVariables.contains(variable);
	}

	public boolean instantiatesLocalVariable(AbstractVariable variable) {
		if(variable instanceof PlainVariable) {
			PlainVariable plainVariable = (PlainVariable)variable;
			String variableType = plainVariable.getVariableType();
			for(TypeObject type : createdTypes) {
				if(variableType.equals(type.getClassType()))
					return true;
			}
		}
		return false;
	}

	public boolean containsClassInstanceCreation() {
		if(!createdTypes.isEmpty())
			return true;
		return false;
	}

	public boolean throwsException() {
		if(!thrownExceptionTypes.isEmpty())
			return true;
		return false;
	}

	public BasicBlock getBasicBlock() {
		return cfgNode.getBasicBlock();
	}

	public AbstractStatement getStatement() {
		return cfgNode.getStatement();
	}

	public Statement getASTStatement() {
		return cfgNode.getASTStatement();
	}

	public boolean equals(Object o) {
		if(this == o)
    		return true;
    	
    	if(o instanceof PDGNode) {
    		PDGNode pdgNode = (PDGNode)o;
    		return this.cfgNode.equals(pdgNode.cfgNode);
    	}
    	return false;
	}

	public int hashCode() {
		return cfgNode.hashCode();
	}

	public String toString() {
		return cfgNode.toString();
	}

	public int compareTo(PDGNode node) {
		if(this.getId() > node.getId())
			return 1;
		else if(this.getId() < node.getId())
			return -1;
		else
			return 0;
	}

	public String getAnnotation() {
		return "Def = " + definedVariables + " , Use = " + usedVariables;
	}

	private Set<TypeDeclaration> extractTypeDeclarations(CompilationUnit compilationUnit) {
		Set<TypeDeclaration> typeDeclarations = new LinkedHashSet<TypeDeclaration>();
		if(compilationUnit != null) {
			List<AbstractTypeDeclaration> topLevelTypeDeclarations = compilationUnit.types();
	        for(AbstractTypeDeclaration abstractTypeDeclaration : topLevelTypeDeclarations) {
	        	if(abstractTypeDeclaration instanceof TypeDeclaration) {
	        		TypeDeclaration topLevelTypeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
	        		typeDeclarations.add(topLevelTypeDeclaration);
	        		TypeDeclaration[] types = topLevelTypeDeclaration.getTypes();
	        		for(TypeDeclaration type : types) {
	        			typeDeclarations.add(type);
	        		}
	        	}
	        }
		}
		return typeDeclarations;
	}

	private Set<AnonymousClassDeclaration> extractAnonymousClassDeclarations(CompilationUnit compilationUnit) {
		Set<AnonymousClassDeclaration> anonymousClassDeclarations = new LinkedHashSet<AnonymousClassDeclaration>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		if(compilationUnit != null) {
			List<AbstractTypeDeclaration> topLevelTypeDeclarations = compilationUnit.types();
	        for(AbstractTypeDeclaration abstractTypeDeclaration : topLevelTypeDeclarations) {
	        	if(abstractTypeDeclaration instanceof TypeDeclaration) {
	        		Set<TypeDeclaration> typeDeclarations = new LinkedHashSet<TypeDeclaration>();
	        		TypeDeclaration topLevelTypeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
	        		typeDeclarations.add(topLevelTypeDeclaration);
	        		TypeDeclaration[] types = topLevelTypeDeclaration.getTypes();
	        		for(TypeDeclaration type : types) {
	        			typeDeclarations.add(type);
	        		}
	        		for(TypeDeclaration typeDeclaration : typeDeclarations) {
	        			MethodDeclaration[] methodDeclarations = typeDeclaration.getMethods();
			        	for(MethodDeclaration methodDeclaration : methodDeclarations) {
			        		Block methodBody = methodDeclaration.getBody();
			        		if(methodBody != null) {
			        			List<Statement> statements = methodBody.statements();
			        			for(Statement statement : statements) {
			        				List<Expression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(statement);
			        				for(Expression expression : classInstanceCreations) {
			        					ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)expression;
			        					AnonymousClassDeclaration anonymousClassDeclaration = classInstanceCreation.getAnonymousClassDeclaration();
			        					if(anonymousClassDeclaration != null) {
			        						anonymousClassDeclarations.add(anonymousClassDeclaration);
			        					}
			        				}
			        			}
			        		}
			        	}
	        		}
	        	}
	        }
		}
		return anonymousClassDeclarations;
	}

	private boolean equalType(ITypeBinding typeBinding1, ITypeBinding typeBinding2) {
		if(typeBinding1.isPrimitive() && typeBinding2.isPrimitive())
			return typeBinding1.isEqualTo(typeBinding2);
		else if(typeBinding1.isArray() && typeBinding2.isArray()) {
			int dimensions1 = typeBinding1.getDimensions();
			int dimensions2 = typeBinding2.getDimensions();
			if(dimensions1 == dimensions2) {
				ITypeBinding elementType1 = typeBinding1.getElementType();
				ITypeBinding elementType2 = typeBinding2.getElementType();
				return equalType(elementType1, elementType2);
			}
			else return false;
		}
		else if(typeBinding1.isWildcardType() && typeBinding2.isWildcardType()) {
			ITypeBinding bound1 = typeBinding1.getBound();
			ITypeBinding bound2 = typeBinding2.getBound();
			if(bound1 != null && bound2 != null)
				return equalType(bound1, bound2);
			else return false;
		}
		else if(typeBinding1.isParameterizedType() && typeBinding2.isParameterizedType()) {
			ITypeBinding erasure1 = typeBinding1.getErasure();
			ITypeBinding erasure2 = typeBinding2.getErasure();
			if(erasure1.isEqualTo(erasure2)) {
				ITypeBinding[] typeArguments1 = typeBinding1.getTypeArguments();
				ITypeBinding[] typeArguments2 = typeBinding2.getTypeArguments();
				if(typeArguments1.length == typeArguments2.length) {
					for(int i=0; i<typeArguments1.length; i++) {
						if(!equalType(typeArguments1[i], typeArguments2[i]))
							return false;
					}
					return true;
				}
				else return false;
			}
			else return false;
		}
		else if(typeBinding1.isParameterizedType() && typeBinding2.isRawType()) {
			ITypeBinding erasure1 = typeBinding1.getErasure();
			ITypeBinding erasure2 = typeBinding2.getErasure();
			return erasure1.isEqualTo(erasure2);
		}
		else if(typeBinding1.isRawType() && typeBinding2.isParameterizedType()) {
			ITypeBinding erasure1 = typeBinding1.getErasure();
			ITypeBinding erasure2 = typeBinding2.getErasure();
			return erasure1.isEqualTo(erasure2);
		}
		else if(typeBinding1.isRawType() && typeBinding2.isRawType()) {
			ITypeBinding erasure1 = typeBinding1.getErasure();
			ITypeBinding erasure2 = typeBinding2.getErasure();
			return erasure1.isEqualTo(erasure2);
		}
		else if(typeBinding1.isClass() && typeBinding2.isClass())
			return typeBinding1.isEqualTo(typeBinding2);
		else if(typeBinding1.isInterface() && typeBinding2.isInterface())
			return typeBinding1.isEqualTo(typeBinding2);
		else if(typeBinding1.isTypeVariable() || typeBinding2.isTypeVariable()) {
			return true;
		}
		return false;
	}

	private boolean equalSignature(IMethodBinding methodBinding1, IMethodBinding methodBinding2) {
		if(!methodBinding1.getName().equals(methodBinding2.getName()))
			return false;
		ITypeBinding returnType1 = methodBinding1.getReturnType();
		ITypeBinding returnType2 = methodBinding2.getReturnType();
		if(!equalType(returnType1, returnType2))
			return false;
		ITypeBinding[] parameterTypes1 = methodBinding1.getParameterTypes();
		ITypeBinding[] parameterTypes2 = methodBinding2.getParameterTypes();
		if(parameterTypes1.length == parameterTypes2.length) {
			int i = 0;
			for(ITypeBinding typeBinding1 : parameterTypes1) {
				ITypeBinding typeBinding2 = parameterTypes2[i];
				if(!equalType(typeBinding1, typeBinding2))
					return false;
				i++;
			}
		}
		else return false;
		return true;
	}

	private Set<MethodDeclaration> getMatchingMethodDeclarationsForSubType(IMethodBinding methodBinding,
			IType subclassType, CompilationUnit compilationUnit) {
		Set<MethodDeclaration> matchingMethodDeclarations = new LinkedHashSet<MethodDeclaration>();
		try {
			if(subclassType.isClass()/* || subclassType.isInterface()*/) {
				Set<TypeDeclaration> typeDeclarations = extractTypeDeclarations(compilationUnit);
				for(TypeDeclaration typeDeclaration : typeDeclarations) {
					if(typeDeclaration.resolveBinding().getQualifiedName().equals(subclassType.getFullyQualifiedName('.'))) {
						MethodDeclaration[] methodDeclarations = typeDeclaration.getMethods();
						for(MethodDeclaration methodDeclaration : methodDeclarations) {
							if(equalSignature(methodDeclaration.resolveBinding(), methodBinding)) {
								matchingMethodDeclarations.add(methodDeclaration);
							}
						}
					}
				}
			}
			if(subclassType.isAnonymous()) {
				Set<AnonymousClassDeclaration> anonymousClassDeclarations = extractAnonymousClassDeclarations(compilationUnit);
				for(AnonymousClassDeclaration anonymousClassDeclaration : anonymousClassDeclarations) {
					List<BodyDeclaration> bodyDeclarations = anonymousClassDeclaration.bodyDeclarations();
					for(BodyDeclaration bodyDeclaration : bodyDeclarations) {
						if(bodyDeclaration instanceof MethodDeclaration) {
							MethodDeclaration methodDeclaration = (MethodDeclaration)bodyDeclaration;
							if(equalSignature(methodDeclaration.resolveBinding(), methodBinding)) {
								matchingMethodDeclarations.add(methodDeclaration);
							}
						}
					}
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return matchingMethodDeclarations;
	}

	private void processExternalMethodInvocation(MethodDeclaration methodDeclaration, AbstractVariable variableDeclaration,
			Set<String> processedMethods, int depth) {
		LibraryClassStorage instance = LibraryClassStorage.getInstance();
		IMethodBinding methodBinding = methodDeclaration.resolveBinding();
		if(instance.isAnalyzed(methodBinding.getKey())) {
			handleAlreadyAnalyzedMethod(methodBinding.getKey(), variableDeclaration, instance);
		}
		else {
			Block methodBody = methodDeclaration.getBody();
			if(methodBody != null) {
				IMethod iMethod = (IMethod)methodBinding.getJavaElement();
				IClassFile iClassFile = iMethod.getClassFile();
				ASTInformationGenerator.setCurrentITypeRoot(iClassFile);
				MethodBodyObject methodBodyObject = new MethodBodyObject(methodBody);
				LinkedHashSet<PlainVariable> definedFields = new LinkedHashSet<PlainVariable>();
				LinkedHashSet<PlainVariable> usedFields = new LinkedHashSet<PlainVariable>();
				for(PlainVariable originalField : methodBodyObject.getDefinedFieldsThroughThisReference()) {
					AbstractVariable field = composeVariable(variableDeclaration, originalField);
					definedVariables.add(field);
					definedFields.add(originalField);
				}
				for(PlainVariable originalField : methodBodyObject.getUsedFieldsThroughThisReference()) {
					AbstractVariable field = composeVariable(variableDeclaration, originalField);
					usedVariables.add(field);
					usedFields.add(originalField);
				}
				instance.setDefinedFields(methodDeclaration, definedFields);
				instance.setUsedFields(methodDeclaration, usedFields);
				processedMethods.add(methodDeclaration.resolveBinding().getKey());
				if(depth < maximumCallGraphAnalysisDepth) {
					for(MethodInvocationObject methodInvocationObject : methodBodyObject.getInvokedMethodsThroughThisReference()) {
						MethodInvocation methodInvocation = methodInvocationObject.getMethodInvocation();
						IMethodBinding methodBinding2 = methodInvocation.resolveMethodBinding();
						MethodDeclaration invokedMethodDeclaration = getInvokedMethodDeclaration(methodBinding2);
						if(invokedMethodDeclaration != null && !invokedMethodDeclaration.equals(methodDeclaration)) {
							if(!processedMethods.contains(methodBinding2.getKey())) {
								if((invokedMethodDeclaration.getModifiers() & Modifier.NATIVE) != 0) {
									//method is native
								}
								else {
									instance.addInvokedMethod(methodDeclaration, invokedMethodDeclaration);
									processExternalMethodInvocation(invokedMethodDeclaration, variableDeclaration, processedMethods, depth+1);
								}
							}
						}
					}
					Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughFields = methodBodyObject.getInvokedMethodsThroughFields();
					for(AbstractVariable originalField : invokedMethodsThroughFields.keySet()) {
						if(originalField instanceof PlainVariable) {
							LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughFields.get(originalField);
							AbstractVariable field = composeVariable(variableDeclaration, originalField);
							for(MethodInvocationObject methodInvocationObject : methodInvocations) {
								MethodInvocation methodInvocation = methodInvocationObject.getMethodInvocation();
								IMethodBinding methodBinding2 = methodInvocation.resolveMethodBinding();
								MethodDeclaration invokedMethodDeclaration = getInvokedMethodDeclaration(methodBinding2);
								if(invokedMethodDeclaration != null && !invokedMethodDeclaration.equals(methodDeclaration)) {
									if(!processedMethods.contains(methodBinding2.getKey())) {
										if((invokedMethodDeclaration.getModifiers() & Modifier.NATIVE) != 0) {
											//method is native
										}
										else {
											instance.addInvokedMethodThroughReference(methodDeclaration, invokedMethodDeclaration, (PlainVariable)originalField);
											processExternalMethodInvocation(invokedMethodDeclaration, field, processedMethods, depth+1);
										}
									}
								}
							}
						}
					}
					for(SuperMethodInvocationObject superMethodInvocationObject : methodBodyObject.getSuperMethodInvocations()) {
						SuperMethodInvocation superMethodInvocation = superMethodInvocationObject.getSuperMethodInvocation();
						IMethodBinding methodBinding2 = superMethodInvocation.resolveMethodBinding();
						MethodDeclaration invokedSuperMethodDeclaration = getInvokedMethodDeclaration(methodBinding2);
						if(invokedSuperMethodDeclaration != null) {
							if(!processedMethods.contains(methodBinding2.getKey())) {
								if((invokedSuperMethodDeclaration.getModifiers() & Modifier.NATIVE) != 0) {
									//method is native
								}
								else {
									instance.addInvokedMethod(methodDeclaration, invokedSuperMethodDeclaration);
									processExternalMethodInvocation(invokedSuperMethodDeclaration, variableDeclaration, processedMethods, depth+1);
								}
							}
						}
					}
				}
			}
			else {
				if((methodDeclaration.getModifiers() & Modifier.NATIVE) != 0) {
					//method is native
				}
				else {
					if(depth < maximumCallGraphAnalysisDepth) {
						IType superType = (IType)methodDeclaration.resolveBinding().getDeclaringClass().getJavaElement();
						Set<IType> subTypes = instance.getSubTypes(superType);
						for(IType subType : subTypes) {
							if(!subType.equals(superType)) {
								IClassFile classFile = subType.getClassFile();
								CompilationUnit compilationUnit = instance.getCompilationUnit(classFile);
								Set<MethodDeclaration> matchingSubTypeMethodDeclarations = getMatchingMethodDeclarationsForSubType(methodBinding, subType, compilationUnit);
								for(MethodDeclaration overridingMethod : matchingSubTypeMethodDeclarations) {
									instance.addOverridingMethod(methodDeclaration, overridingMethod);
									processExternalMethodInvocation(overridingMethod, variableDeclaration, processedMethods, depth);
								}
							}
						}
					}
				}
			}
		}
	}

	private MethodDeclaration getInvokedMethodDeclaration(IMethodBinding methodBinding) {
		MethodDeclaration invokedMethodDeclaration = null;
		IMethod iMethod2 = (IMethod)methodBinding.getJavaElement();
		IClassFile methodClassFile = iMethod2.getClassFile();
		LibraryClassStorage instance = LibraryClassStorage.getInstance();
		CompilationUnit methodCompilationUnit = instance.getCompilationUnit(methodClassFile);
		Set<TypeDeclaration> methodTypeDeclarations = extractTypeDeclarations(methodCompilationUnit);
		for(TypeDeclaration methodTypeDeclaration : methodTypeDeclarations) {
			if(methodTypeDeclaration.resolveBinding().isEqualTo(methodBinding.getDeclaringClass()) ||
					methodTypeDeclaration.resolveBinding().getBinaryName().equals(methodBinding.getDeclaringClass().getBinaryName())) {
				MethodDeclaration[] methodDeclarations2 = methodTypeDeclaration.getMethods();
				for(MethodDeclaration methodDeclaration2 : methodDeclarations2) {
					if(methodDeclaration2.resolveBinding().isEqualTo(methodBinding) ||
							equalSignature(methodDeclaration2.resolveBinding(), methodBinding)) {
						invokedMethodDeclaration = methodDeclaration2;
						break;
					}
				}
				if(invokedMethodDeclaration != null)
					break;
			}
		}
		return invokedMethodDeclaration;
	}

	private void handleAlreadyAnalyzedMethod(String methodBindingKey, AbstractVariable variableDeclaration, LibraryClassStorage indexer) {
		LinkedHashSet<PlainVariable> recursivelyDefinedFields = 
			indexer.getRecursivelyDefinedFields(methodBindingKey, new LinkedHashSet<String>());
		for(PlainVariable originalField : recursivelyDefinedFields) {
			AbstractVariable field = composeVariable(variableDeclaration, originalField);
			definedVariables.add(field);
		}
		LinkedHashSet<PlainVariable> recursivelyUsedFields = 
			indexer.getRecursivelyUsedFields(methodBindingKey, new LinkedHashSet<String>());
		for(PlainVariable originalField : recursivelyUsedFields) {
			AbstractVariable field = composeVariable(variableDeclaration, originalField);
			usedVariables.add(field);
		}
		Set<PlainVariable> invocationReferences = indexer.getInvocationReferences(methodBindingKey);
		if(invocationReferences != null) {
			for(PlainVariable invocationReference : invocationReferences) {
				LinkedHashSet<AbstractVariable> definedFieldsThroughReference = 
					indexer.getRecursivelyDefinedFieldsThroughReference(methodBindingKey, invocationReference, new LinkedHashSet<String>());
				for(AbstractVariable definedField : definedFieldsThroughReference) {
					AbstractVariable field = composeVariable(variableDeclaration, definedField);
					definedVariables.add(field);
				}
				LinkedHashSet<AbstractVariable> usedFieldsThroughReference = 
					indexer.getRecursivelyUsedFieldsThroughReference(methodBindingKey, invocationReference, new LinkedHashSet<String>());
				for(AbstractVariable usedField : usedFieldsThroughReference) {
					AbstractVariable field = composeVariable(variableDeclaration, usedField);
					usedVariables.add(field);
				}
			}
		}
	}

	private AbstractVariable composeVariable(AbstractVariable leftSide, AbstractVariable rightSide) {
		if(leftSide instanceof CompositeVariable) {
			CompositeVariable leftSideCompositeVariable = (CompositeVariable)leftSide;
			PlainVariable finalVariable = leftSideCompositeVariable.getFinalVariable();
			CompositeVariable newRightSide = new CompositeVariable(finalVariable.getVariableBindingKey(), finalVariable.getVariableName(),
					finalVariable.getVariableType(), finalVariable.isField(), finalVariable.isParameter(), rightSide);
			AbstractVariable newLeftSide = leftSideCompositeVariable.getLeftPart();
			return composeVariable(newLeftSide, newRightSide);
		}
		else {
			return new CompositeVariable(leftSide.getVariableBindingKey(), leftSide.getVariableName(),
					leftSide.getVariableType(), leftSide.isField(), leftSide.isParameter(), rightSide);
		}
	}

	private void processInternalMethodInvocation(ClassObject classObject, MethodObject methodObject, AbstractVariable variableDeclaration, Set<String> processedMethods) {
		SystemObject systemObject = ASTReader.getSystemObject();
		if(methodObject.isAbstract() || classObject.isInterface()) {
			TypeDeclaration typeDeclaration = classObject.getTypeDeclaration();
			IMethodBinding superMethodDeclarationBinding = methodObject.getMethodDeclaration().resolveBinding();
			IType superType = (IType)typeDeclaration.resolveBinding().getJavaElement();
			processedMethods.add(superMethodDeclarationBinding.getKey());
			Set<IType> subTypes = CompilationUnitCache.getInstance().getSubTypes(superType);
			Set<IType> subTypesToBeAnalyzed = new LinkedHashSet<IType>();
			if(variableDeclaration != null) {
				String initialReferenceType = variableDeclaration.getVariableType();
				for(IType subType : subTypes) {
					if(subType.getFullyQualifiedName('.').equals(initialReferenceType)) {
						subTypesToBeAnalyzed.add(subType);
						break;
					}
				}
				if(subTypesToBeAnalyzed.isEmpty())
					subTypesToBeAnalyzed.addAll(subTypes);
			}
			else
				subTypesToBeAnalyzed.addAll(subTypes);
			for(IType subType : subTypesToBeAnalyzed) {
				ClassObject subClassObject = systemObject.getClassObject(subType.getFullyQualifiedName('.'));
				if(subClassObject != null) {
					ListIterator<MethodObject> methodIterator = subClassObject.getMethodIterator();
					while(methodIterator.hasNext()) {
						MethodObject subMethod = methodIterator.next();
						if(equalSignature(subMethod.getMethodDeclaration().resolveBinding(), superMethodDeclarationBinding)) {
							processInternalMethodInvocation(subClassObject, subMethod, variableDeclaration, processedMethods);
							break;
						}
					}
				}
			}
		}
		else {
			for(PlainVariable originalField : methodObject.getDefinedFieldsThroughThisReference()) {
				boolean alreadyContainsOriginalField = false;
				if(variableDeclaration != null && originalField instanceof PlainVariable) {
					if(variableDeclaration.containsPlainVariable((PlainVariable)originalField))
						alreadyContainsOriginalField = true;
				}
				if(!alreadyContainsOriginalField) {
					AbstractVariable field = null;
					if(variableDeclaration != null)
						field = composeVariable(variableDeclaration, originalField);
					else
						field = originalField;
					definedVariables.add(field);
				}
			}
			for(PlainVariable originalField : methodObject.getUsedFieldsThroughThisReference()) {
				boolean alreadyContainsOriginalField = false;
				if(variableDeclaration != null && originalField instanceof PlainVariable) {
					if(variableDeclaration.containsPlainVariable((PlainVariable)originalField))
						alreadyContainsOriginalField = true;
				}
				if(!alreadyContainsOriginalField) {
					AbstractVariable field = null;
					if(variableDeclaration != null)
						field = composeVariable(variableDeclaration, originalField);
					else
						field = originalField;
					usedVariables.add(field);
				}
			}
			processedMethods.add(methodObject.getMethodDeclaration().resolveBinding().getKey());
			Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughFields = methodObject.getInvokedMethodsThroughFields();
			for(AbstractVariable originalField : invokedMethodsThroughFields.keySet()) {
				boolean alreadyContainsOriginalField = false;
				if(variableDeclaration != null && originalField instanceof PlainVariable) {
					if(variableDeclaration.containsPlainVariable((PlainVariable)originalField))
						alreadyContainsOriginalField = true;
				}
				if(!alreadyContainsOriginalField) {
					LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughFields.get(originalField);
					AbstractVariable field = null;
					if(variableDeclaration != null)
						field = composeVariable(variableDeclaration, originalField);
					else
						field = originalField;
					for(MethodInvocationObject methodInvocationObject : methodInvocations) {
						MethodInvocation methodInvocation2 = methodInvocationObject.getMethodInvocation();
						ClassObject classObject2 = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
						if(classObject2 != null) {
							MethodObject methodObject2 = classObject2.getMethod(methodInvocationObject);
							if(methodObject2 != null) {
								processInternalMethodInvocation(classObject2, methodObject2, field, new LinkedHashSet<String>());
							}
						}
						else {
							LibraryClassStorage instance = LibraryClassStorage.getInstance();
							IMethodBinding invokedMethodBinding = methodInvocation2.resolveMethodBinding();
							if(instance.isAnalyzed(invokedMethodBinding.getKey())) {
								handleAlreadyAnalyzedMethod(invokedMethodBinding.getKey(), field, instance);
							}
							else {
								MethodDeclaration invokedMethodDeclaration = getInvokedMethodDeclaration(invokedMethodBinding);
								if(invokedMethodDeclaration != null)
									processExternalMethodInvocation(invokedMethodDeclaration, field, new LinkedHashSet<String>(), 0);
							}
						}
					}
				}
			}
			for(MethodInvocationObject methodInvocationObject : methodObject.getInvokedMethodsThroughThisReference()) {
				MethodObject methodObject2 = classObject.getMethod(methodInvocationObject);
				if(methodObject2 != null && !methodObject2.equals(methodObject)) {
					MethodInvocation methodInvocation2 = methodInvocationObject.getMethodInvocation();
					if(!processedMethods.contains(methodInvocation2.resolveMethodBinding().getKey()))
						processInternalMethodInvocation(classObject, methodObject2, variableDeclaration, processedMethods);
				}
			}
			for(SuperMethodInvocationObject superMethodInvocationObject : methodObject.getSuperMethodInvocations()) {
				ClassObject classObject2 = systemObject.getClassObject(superMethodInvocationObject.getOriginClassName());
				if(classObject2 != null) {
					MethodObject methodObject2 = classObject2.getMethod(superMethodInvocationObject);
					if(methodObject2 != null) {
						SuperMethodInvocation superMethodInvocation = superMethodInvocationObject.getSuperMethodInvocation();
						if(!processedMethods.contains(superMethodInvocation.resolveMethodBinding().getKey()))
							processInternalMethodInvocation(classObject2, methodObject2, variableDeclaration, processedMethods);
					}
				}
			}
		}
	}

	private void processArgumentOfInternalMethodInvocation(MethodObject methodObject, AbstractVariable argumentDeclaration, int initialArgumentPosition,
			VariableDeclaration parameterDeclaration, Set<String> processedMethods) {
		SystemObject systemObject = ASTReader.getSystemObject();
		if(methodObject.getMethodBody() == null) {
			IMethodBinding superMethodDeclarationBinding = methodObject.getMethodDeclaration().resolveBinding();
			IType superType = (IType)superMethodDeclarationBinding.getDeclaringClass().getJavaElement();
			processedMethods.add(superMethodDeclarationBinding.getKey());
			Set<IType> subTypes = CompilationUnitCache.getInstance().getSubTypes(superType);
			for(IType subType : subTypes) {
				ClassObject subClassObject = systemObject.getClassObject(subType.getFullyQualifiedName('.'));
				if(subClassObject != null) {
					ListIterator<MethodObject> methodIterator = subClassObject.getMethodIterator();
					while(methodIterator.hasNext()) {
						MethodObject subMethod = methodIterator.next();
						if(equalSignature(subMethod.getMethodDeclaration().resolveBinding(), superMethodDeclarationBinding)) {
							ParameterObject parameterObject = subMethod.getParameter(initialArgumentPosition);
							VariableDeclaration parameterDeclaration2 = parameterObject.getSingleVariableDeclaration();
							processArgumentOfInternalMethodInvocation(subMethod, argumentDeclaration, initialArgumentPosition, parameterDeclaration2, processedMethods);
							break;
						}
					}
				}
			}
		}
		else {
			for(AbstractVariable originalField : methodObject.getDefinedFieldsThroughParameters()) {
				if(parameterDeclaration.resolveBinding().getKey().equals(originalField.getVariableBindingKey())) {
					AbstractVariable field = new CompositeVariable(argumentDeclaration.getVariableBindingKey(), argumentDeclaration.getVariableName(),
							argumentDeclaration.getVariableType(), argumentDeclaration.isField(), argumentDeclaration.isParameter(), ((CompositeVariable)originalField).getRightPart());
					definedVariables.add(field);
				}
			}
			for(AbstractVariable originalField : methodObject.getUsedFieldsThroughParameters()) {
				if(parameterDeclaration.resolveBinding().getKey().equals(originalField.getVariableBindingKey())) {
					AbstractVariable field = new CompositeVariable(argumentDeclaration.getVariableBindingKey(), argumentDeclaration.getVariableName(),
							argumentDeclaration.getVariableType(), argumentDeclaration.isField(), argumentDeclaration.isParameter(), ((CompositeVariable)originalField).getRightPart());
					usedVariables.add(field);
				}
			}
			processedMethods.add(methodObject.getMethodDeclaration().resolveBinding().getKey());
			Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughParameters = methodObject.getInvokedMethodsThroughParameters();
			for(AbstractVariable originalField : invokedMethodsThroughParameters.keySet()) {
				if(parameterDeclaration.resolveBinding().getKey().equals(originalField.getVariableBindingKey())) {
					AbstractVariable field = null;
					if(originalField instanceof PlainVariable)
						field = argumentDeclaration;
					else
						field = new CompositeVariable(argumentDeclaration.getVariableBindingKey(), argumentDeclaration.getVariableName(),
								argumentDeclaration.getVariableType(), argumentDeclaration.isField(), argumentDeclaration.isParameter(), ((CompositeVariable)originalField).getRightPart());
					LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughParameters.get(originalField);
					for(MethodInvocationObject methodInvocationObject : methodInvocations) {
						MethodInvocation methodInvocation2 = methodInvocationObject.getMethodInvocation();
						ClassObject classObject2 = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
						if(classObject2 != null) {
							MethodObject methodObject2 = classObject2.getMethod(methodInvocationObject);
							if(methodObject2 != null) {
								processInternalMethodInvocation(classObject2, methodObject2, field, new LinkedHashSet<String>());
							}
						}
						else {
							LibraryClassStorage instance = LibraryClassStorage.getInstance();
							IMethodBinding invokedMethodBinding = methodInvocation2.resolveMethodBinding();
							if(instance.isAnalyzed(invokedMethodBinding.getKey())) {
								handleAlreadyAnalyzedMethod(invokedMethodBinding.getKey(), field, instance);
							}
							else {
								MethodDeclaration invokedMethodDeclaration = getInvokedMethodDeclaration(invokedMethodBinding);
								if(invokedMethodDeclaration != null)
									processExternalMethodInvocation(invokedMethodDeclaration, field, new LinkedHashSet<String>(), 0);
							}
						}
					}
				}
			}
			Map<PlainVariable, LinkedHashSet<MethodInvocationObject>> parametersPassedAsArgumentsInMethodInvocations = methodObject.getParametersPassedAsArgumentsInMethodInvocations();
			for(PlainVariable parameter : parametersPassedAsArgumentsInMethodInvocations.keySet()) {
				if(parameterDeclaration.resolveBinding().getKey().equals(parameter.getVariableBindingKey())) {
					LinkedHashSet<MethodInvocationObject> methodInvocations = parametersPassedAsArgumentsInMethodInvocations.get(parameter);
					for(MethodInvocationObject methodInvocationObject : methodInvocations) {
						ClassObject classObject2 = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
						if(classObject2 != null) {
							MethodObject methodObject2 = classObject2.getMethod(methodInvocationObject);
							if(methodObject2 != null && !methodObject2.equals(methodObject)) {
								MethodInvocation methodInvocation2 = methodInvocationObject.getMethodInvocation();
								int argumentPosition = getArgumentPosition(methodInvocation2.arguments(), parameter);
								ParameterObject parameterObject = methodObject2.getParameter(argumentPosition);
								VariableDeclaration parameterDeclaration2 = parameterObject.getSingleVariableDeclaration();
								if(!processedMethods.contains(methodInvocation2.resolveMethodBinding().getKey()))
									processArgumentOfInternalMethodInvocation(methodObject2, argumentDeclaration, argumentPosition, parameterDeclaration2, processedMethods);
							}
						}
					}
				}
			}
			Map<PlainVariable, LinkedHashSet<SuperMethodInvocationObject>> parametersPassedAsArgumentsInSuperMethodInvocations = methodObject.getParametersPassedAsArgumentsInSuperMethodInvocations();
			for(PlainVariable parameter : parametersPassedAsArgumentsInSuperMethodInvocations.keySet()) {
				if(parameterDeclaration.resolveBinding().getKey().equals(parameter.getVariableBindingKey())) {
					LinkedHashSet<SuperMethodInvocationObject> superMethodInvocations = parametersPassedAsArgumentsInSuperMethodInvocations.get(parameter);
					for(SuperMethodInvocationObject superMethodInvocationObject : superMethodInvocations) {
						ClassObject classObject2 = systemObject.getClassObject(superMethodInvocationObject.getOriginClassName());
						if(classObject2 != null) {
							MethodObject methodObject2 = classObject2.getMethod(superMethodInvocationObject);
							if(methodObject2 != null) {
								SuperMethodInvocation superMethodInvocation = superMethodInvocationObject.getSuperMethodInvocation();
								int argumentPosition = getArgumentPosition(superMethodInvocation.arguments(), parameter);
								ParameterObject parameterObject = methodObject2.getParameter(argumentPosition);
								VariableDeclaration parameterDeclaration2 = parameterObject.getSingleVariableDeclaration();
								if(!processedMethods.contains(superMethodInvocation.resolveMethodBinding().getKey()))
									processArgumentOfInternalMethodInvocation(methodObject2, argumentDeclaration, argumentPosition, parameterDeclaration2, processedMethods);
							}
						}
					}
				}
			}
		}
	}

	private int getArgumentPosition(List<Expression> arguments, PlainVariable argument) {
		int argumentPostion = 0;
		for(Expression arg : arguments) {
			if(arg instanceof SimpleName) {
				SimpleName argName = (SimpleName)arg;
				if(argName.resolveBinding().getKey().equals(argument.getVariableBindingKey()))
					return argumentPostion;
			}
			argumentPostion++;
		}
		return -1;
	}

	protected void processArgumentsOfInternalMethodInvocation(MethodInvocationObject methodInvocationObject, AbstractVariable variable) {
		SystemObject systemObject = ASTReader.getSystemObject();
		MethodInvocation methodInvocation = methodInvocationObject.getMethodInvocation();
		ClassObject classObject = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
		if(classObject != null) {
			MethodObject methodObject = classObject.getMethod(methodInvocationObject);
			if(methodObject != null) {
				processInternalMethodInvocation(classObject, methodObject, variable, new LinkedHashSet<String>());
				List<Expression> arguments = methodInvocation.arguments();
				int argumentPosition = 0;
				for(Expression argument : arguments) {
					if(argument instanceof SimpleName) {
						SimpleName argumentName = (SimpleName)argument;
						VariableDeclaration argumentDeclaration = null;
						for(VariableDeclaration variableDeclaration : variableDeclarationsInMethod) {
							if(variableDeclaration.resolveBinding().isEqualTo(argumentName.resolveBinding())) {
								argumentDeclaration = variableDeclaration;
								break;
							}
						}
						if(argumentDeclaration != null) {
							ParameterObject parameter = methodObject.getParameter(argumentPosition);
							VariableDeclaration parameterDeclaration = parameter.getSingleVariableDeclaration();
							PlainVariable argumentVariable = new PlainVariable(argumentDeclaration);
							processArgumentOfInternalMethodInvocation(methodObject, argumentVariable, argumentPosition, parameterDeclaration, new LinkedHashSet<String>());
						}
					}
					argumentPosition++;
				}
			}
		}
		else {
			if(variable != null) {
				LibraryClassStorage instance = LibraryClassStorage.getInstance();
				IMethodBinding invokedMethodBinding = methodInvocation.resolveMethodBinding();
				if(instance.isAnalyzed(invokedMethodBinding.getKey())) {
					handleAlreadyAnalyzedMethod(invokedMethodBinding.getKey(), variable, instance);
				}
				else {
					MethodDeclaration invokedMethodDeclaration = getInvokedMethodDeclaration(invokedMethodBinding);
					if(invokedMethodDeclaration != null)
						processExternalMethodInvocation(invokedMethodDeclaration, variable, new LinkedHashSet<String>(), 0);
				}
			}
		}
	}

	public void updateReachingAliasSet(ReachingAliasSet reachingAliasSet) {
		Set<VariableDeclaration> variableDeclarations = new LinkedHashSet<VariableDeclaration>();
		variableDeclarations.addAll(variableDeclarationsInMethod);
		variableDeclarations.addAll(fieldsAccessedInMethod);
		Statement statement = getASTStatement();
		if(statement instanceof VariableDeclarationStatement) {
			VariableDeclarationStatement vDStatement = (VariableDeclarationStatement)statement;
			if(!vDStatement.getType().resolveBinding().isPrimitive()) {
				List<VariableDeclarationFragment> fragments = vDStatement.fragments();
				for(VariableDeclarationFragment fragment : fragments) {
					Expression initializer = fragment.getInitializer();
					SimpleName initializerSimpleName = null;
					if(initializer != null) {
						if(initializer instanceof SimpleName) {
							initializerSimpleName = (SimpleName)initializer;
						}
						else if(initializer instanceof FieldAccess) {
							FieldAccess fieldAccess = (FieldAccess)initializer;
							initializerSimpleName = fieldAccess.getName();
						}
					}
					if(initializerSimpleName != null) {
						VariableDeclaration initializerVariableDeclaration = null;
						for(VariableDeclaration declaration : variableDeclarations) {
							if(declaration.resolveBinding().isEqualTo(initializerSimpleName.resolveBinding())) {
								initializerVariableDeclaration = declaration;
								break;
							}
						}
						if(initializerVariableDeclaration != null) {
							reachingAliasSet.insertAlias(fragment, initializerVariableDeclaration);
						}
					}
				}
			}
		}
		else if(statement instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement = (ExpressionStatement)statement;
			Expression expression = expressionStatement.getExpression();
			if(expression instanceof Assignment) {
				Assignment assignment = (Assignment)expression;
				processAssignment(reachingAliasSet, variableDeclarations, assignment);
			}
		}
	}

	private void processAssignment(ReachingAliasSet reachingAliasSet,
			Set<VariableDeclaration> variableDeclarations, Assignment assignment) {
		Expression leftHandSideExpression = assignment.getLeftHandSide();
		Expression rightHandSideExpression = assignment.getRightHandSide();
		SimpleName leftHandSideSimpleName = null;
		if(leftHandSideExpression instanceof SimpleName) {
			leftHandSideSimpleName = (SimpleName)leftHandSideExpression;
		}
		else if(leftHandSideExpression instanceof FieldAccess) {
			FieldAccess fieldAccess = (FieldAccess)leftHandSideExpression;
			leftHandSideSimpleName = fieldAccess.getName();
		}
		if(leftHandSideSimpleName != null && !leftHandSideSimpleName.resolveTypeBinding().isPrimitive()) {
			VariableDeclaration leftHandSideVariableDeclaration = null;
			for(VariableDeclaration declaration : variableDeclarations) {
				if(declaration.resolveBinding().isEqualTo(leftHandSideSimpleName.resolveBinding())) {
					leftHandSideVariableDeclaration = declaration;
					break;
				}
			}
			SimpleName rightHandSideSimpleName = null;
			if(rightHandSideExpression instanceof SimpleName) {
				rightHandSideSimpleName = (SimpleName)rightHandSideExpression;
			}
			else if(rightHandSideExpression instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)rightHandSideExpression;
				rightHandSideSimpleName = fieldAccess.getName();
			}
			else if(rightHandSideExpression instanceof Assignment) {
				Assignment rightHandSideAssignment = (Assignment)rightHandSideExpression;
				processAssignment(reachingAliasSet, variableDeclarations, rightHandSideAssignment);
				Expression leftHandSideExpressionOfRightHandSideAssignment = rightHandSideAssignment.getLeftHandSide();
				SimpleName leftHandSideSimpleNameOfRightHandSideAssignment = null;
				if(leftHandSideExpressionOfRightHandSideAssignment instanceof SimpleName) {
					leftHandSideSimpleNameOfRightHandSideAssignment = (SimpleName)leftHandSideExpressionOfRightHandSideAssignment;
				}
				else if(leftHandSideExpressionOfRightHandSideAssignment instanceof FieldAccess) {
					FieldAccess fieldAccess = (FieldAccess)leftHandSideExpressionOfRightHandSideAssignment;
					leftHandSideSimpleNameOfRightHandSideAssignment = fieldAccess.getName();
				}
				if(leftHandSideSimpleNameOfRightHandSideAssignment != null) {
					rightHandSideSimpleName = leftHandSideSimpleNameOfRightHandSideAssignment;
				}
			}
			if(rightHandSideSimpleName != null) {
				VariableDeclaration rightHandSideVariableDeclaration = null;
				for(VariableDeclaration declaration : variableDeclarations) {
					if(declaration.resolveBinding().isEqualTo(rightHandSideSimpleName.resolveBinding())) {
						rightHandSideVariableDeclaration = declaration;
						break;
					}
				}
				if(leftHandSideVariableDeclaration != null && rightHandSideVariableDeclaration != null) {
					reachingAliasSet.insertAlias(leftHandSideVariableDeclaration, rightHandSideVariableDeclaration);
				}
			}
			else {
				if(leftHandSideVariableDeclaration != null) {
					reachingAliasSet.removeAlias(leftHandSideVariableDeclaration);
				}
			}
		}
	}

	public void applyReachingAliasSet(ReachingAliasSet reachingAliasSet) {
		if(originalDefinedVariables == null)
			originalDefinedVariables = new LinkedHashSet<AbstractVariable>(definedVariables);
		Set<AbstractVariable> defVariablesToBeAdded = new LinkedHashSet<AbstractVariable>();
		for(AbstractVariable abstractVariable : originalDefinedVariables) {
			if(abstractVariable instanceof CompositeVariable) {
				CompositeVariable compositeVariable = (CompositeVariable)abstractVariable;
				if(reachingAliasSet.containsAlias(compositeVariable)) {
					Set<VariableDeclaration> aliases = reachingAliasSet.getAliases(compositeVariable);
					for(VariableDeclaration alias : aliases) {
						CompositeVariable aliasCompositeVariable = new CompositeVariable(alias, compositeVariable.getRightPart());
						defVariablesToBeAdded.add(aliasCompositeVariable);
					}
				}
			}
		}
		definedVariables.addAll(defVariablesToBeAdded);
		if(originalUsedVariables == null)
			originalUsedVariables = new LinkedHashSet<AbstractVariable>(usedVariables);
		Set<AbstractVariable> useVariablesToBeAdded = new LinkedHashSet<AbstractVariable>();
		for(AbstractVariable abstractVariable : originalUsedVariables) {
			if(abstractVariable instanceof CompositeVariable) {
				CompositeVariable compositeVariable = (CompositeVariable)abstractVariable;
				if(reachingAliasSet.containsAlias(compositeVariable)) {
					Set<VariableDeclaration> aliases = reachingAliasSet.getAliases(compositeVariable);
					for(VariableDeclaration alias : aliases) {
						CompositeVariable aliasCompositeVariable = new CompositeVariable(alias, compositeVariable.getRightPart());
						useVariablesToBeAdded.add(aliasCompositeVariable);
					}
				}
			}
		}
		usedVariables.addAll(useVariablesToBeAdded);
	}

	public Map<VariableDeclaration, ClassInstanceCreation> getClassInstantiations() {
		Map<VariableDeclaration, ClassInstanceCreation> classInstantiationMap = new LinkedHashMap<VariableDeclaration, ClassInstanceCreation>();
		Set<VariableDeclaration> variableDeclarations = new LinkedHashSet<VariableDeclaration>();
		variableDeclarations.addAll(variableDeclarationsInMethod);
		variableDeclarations.addAll(fieldsAccessedInMethod);
		Statement statement = getASTStatement();
		if(statement instanceof VariableDeclarationStatement) {
			VariableDeclarationStatement vDStatement = (VariableDeclarationStatement)statement;
			List<VariableDeclarationFragment> fragments = vDStatement.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				Expression initializer = fragment.getInitializer();
				if(initializer instanceof ClassInstanceCreation) {
					ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)initializer;
					classInstantiationMap.put(fragment, classInstanceCreation);
				}
			}
		}
		else if(statement instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement = (ExpressionStatement)statement;
			Expression expression = expressionStatement.getExpression();
			ExpressionExtractor expressionExtractor = new ExpressionExtractor();
			List<Expression> assignments = expressionExtractor.getAssignments(expression);
			for(Expression assignmentExpression : assignments) {
				Assignment assignment = (Assignment)assignmentExpression;
				Expression leftHandSideExpression = assignment.getLeftHandSide();
				Expression rightHandSideExpression = assignment.getRightHandSide();
				if(rightHandSideExpression instanceof ClassInstanceCreation) {
					ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)rightHandSideExpression;
					SimpleName leftHandSideSimpleName = null;
					if(leftHandSideExpression instanceof SimpleName) {
						leftHandSideSimpleName = (SimpleName)leftHandSideExpression;
					}
					else if(leftHandSideExpression instanceof FieldAccess) {
						FieldAccess fieldAccess = (FieldAccess)leftHandSideExpression;
						leftHandSideSimpleName = fieldAccess.getName();
					}
					if(leftHandSideSimpleName != null) {
						VariableDeclaration leftHandSideVariableDeclaration = null;
						for(VariableDeclaration declaration : variableDeclarations) {
							if(declaration.resolveBinding().isEqualTo(leftHandSideSimpleName.resolveBinding())) {
								leftHandSideVariableDeclaration = declaration;
								break;
							}
						}
						if(leftHandSideVariableDeclaration != null) {
							classInstantiationMap.put(leftHandSideVariableDeclaration, classInstanceCreation);
						}
					}
				}
			}
		}
		return classInstantiationMap;
	}

	public boolean changesStateOfReference(VariableDeclaration variableDeclaration) {
		for(AbstractVariable abstractVariable : definedVariables) {
			if(abstractVariable instanceof CompositeVariable) {
				CompositeVariable compositeVariable = (CompositeVariable)abstractVariable;
				if(variableDeclaration.resolveBinding().getKey().equals(compositeVariable.getVariableBindingKey()))
					return true;
			}
		}
		return false;
	}

	public boolean accessesReference(VariableDeclaration variableDeclaration) {
		for(AbstractVariable abstractVariable : usedVariables) {
			if(abstractVariable instanceof PlainVariable) {
				PlainVariable plainVariable = (PlainVariable)abstractVariable;
				if(variableDeclaration.resolveBinding().getKey().equals(plainVariable.getVariableBindingKey()))
					return true;
			}
		}
		return false;
	}

	public boolean assignsReference(VariableDeclaration variableDeclaration) {
		Statement statement = getASTStatement();
		if(statement instanceof VariableDeclarationStatement) {
			VariableDeclarationStatement vDStatement = (VariableDeclarationStatement)statement;
			List<VariableDeclarationFragment> fragments = vDStatement.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				Expression initializer = fragment.getInitializer();
				SimpleName initializerSimpleName = null;
				if(initializer != null) {
					if(initializer instanceof SimpleName) {
						initializerSimpleName = (SimpleName)initializer;
					}
					else if(initializer instanceof FieldAccess) {
						FieldAccess fieldAccess = (FieldAccess)initializer;
						initializerSimpleName = fieldAccess.getName();
					}
				}
				if(initializerSimpleName != null) {
					if(variableDeclaration.resolveBinding().isEqualTo(initializerSimpleName.resolveBinding())) {
						return true;
					}
				}
			}
		}
		else if(statement instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement = (ExpressionStatement)statement;
			Expression expression = expressionStatement.getExpression();
			ExpressionExtractor expressionExtractor = new ExpressionExtractor();
			List<Expression> assignments = expressionExtractor.getAssignments(expression);
			for(Expression assignmentExpression : assignments) {
				Assignment assignment = (Assignment)assignmentExpression;
				Expression rightHandSideExpression = assignment.getRightHandSide();
				SimpleName rightHandSideSimpleName = null;
				if(rightHandSideExpression instanceof SimpleName) {
					rightHandSideSimpleName = (SimpleName)rightHandSideExpression;
				}
				else if(rightHandSideExpression instanceof FieldAccess) {
					FieldAccess fieldAccess = (FieldAccess)rightHandSideExpression;
					rightHandSideSimpleName = fieldAccess.getName();
				}
				if(rightHandSideSimpleName != null) {
					if(variableDeclaration.resolveBinding().isEqualTo(rightHandSideSimpleName.resolveBinding())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean isEquivalent(PDGNode node) {
		if(this instanceof PDGMethodEntryNode && node instanceof PDGMethodEntryNode)
			return true;
		else
			return this.getCFGNode().isEquivalent(node.getCFGNode());
	}
}
