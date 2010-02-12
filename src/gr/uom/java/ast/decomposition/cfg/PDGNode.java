package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.LibraryClassStorage;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.ParameterObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.ast.TypeSearchRequestor;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.ast.util.ExpressionExtractor;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IField;
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
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

public class PDGNode extends GraphNode implements Comparable<PDGNode> {
	private CFGNode cfgNode;
	protected Set<AbstractVariable> declaredVariables;
	protected Set<AbstractVariable> definedVariables;
	protected Set<AbstractVariable> usedVariables;
	protected Set<TypeObject> createdTypes;
	protected Set<VariableDeclaration> variableDeclarationsInMethod;
	protected Set<VariableDeclaration> fieldsAccessedInMethod;
	private Map<AbstractVariable, LinkedHashSet<MethodInvocation>> stateChangingMethodInvocationMap;
	private Map<AbstractVariable, LinkedHashSet<AbstractVariable>> stateChangingFieldModificationMap;
	private Set<AbstractVariable> originalDefinedVariables;
	private Set<AbstractVariable> originalUsedVariables;
	
	public PDGNode() {
		super();
		this.declaredVariables = new LinkedHashSet<AbstractVariable>();
		this.definedVariables = new LinkedHashSet<AbstractVariable>();
		this.usedVariables = new LinkedHashSet<AbstractVariable>();
		this.createdTypes = new LinkedHashSet<TypeObject>();
		this.stateChangingMethodInvocationMap = new LinkedHashMap<AbstractVariable, LinkedHashSet<MethodInvocation>>();
		this.stateChangingFieldModificationMap = new LinkedHashMap<AbstractVariable, LinkedHashSet<AbstractVariable>>();
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
		this.stateChangingMethodInvocationMap = new LinkedHashMap<AbstractVariable, LinkedHashSet<MethodInvocation>>();
		this.stateChangingFieldModificationMap = new LinkedHashMap<AbstractVariable, LinkedHashSet<AbstractVariable>>();
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

	public boolean declaresLocalVariable(AbstractVariable variable) {
		return declaredVariables.contains(variable);
	}

	public boolean definesLocalVariable(AbstractVariable variable) {
		return definedVariables.contains(variable);
	}

	public boolean usesLocalVariable(AbstractVariable variable) {
		return usedVariables.contains(variable);
	}

	public boolean containsClassInstanceCreation() {
		if(!createdTypes.isEmpty())
			return true;
		return false;
	}

	public Set<AbstractVariable> getStateChangingVariables() {
		Set<AbstractVariable> stateChangingVariables = new LinkedHashSet<AbstractVariable>();
		stateChangingVariables.addAll(stateChangingMethodInvocationMap.keySet());
		stateChangingVariables.addAll(stateChangingFieldModificationMap.keySet());
		return stateChangingVariables;
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

	private void putInStateChangingMethodInvocationMap(AbstractVariable variableDeclaration, MethodInvocation methodInvocation) {
		if(stateChangingMethodInvocationMap.containsKey(variableDeclaration)) {
			LinkedHashSet<MethodInvocation> methodInvocations = stateChangingMethodInvocationMap.get(variableDeclaration);
			methodInvocations.add(methodInvocation);
		}
		else {
			LinkedHashSet<MethodInvocation> methodInvocations = new LinkedHashSet<MethodInvocation>();
			methodInvocations.add(methodInvocation);
			stateChangingMethodInvocationMap.put(variableDeclaration, methodInvocations);
		}
	}

	protected void putInStateChangingFieldModificationMap(AbstractVariable reference, AbstractVariable field) {
		if(stateChangingFieldModificationMap.containsKey(reference)) {
			LinkedHashSet<AbstractVariable> fields = stateChangingFieldModificationMap.get(reference);
			fields.add(field);
		}
		else {
			LinkedHashSet<AbstractVariable> fields = new LinkedHashSet<AbstractVariable>();
			fields.add(field);
			stateChangingFieldModificationMap.put(reference, fields);
		}
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

	private Map<MethodDeclaration, TypeDeclaration> getMatchingMethodDeclarations(IMethodBinding methodBinding, CompilationUnit compilationUnit) {
		Map<MethodDeclaration, TypeDeclaration> matchingMethodDeclarations = new LinkedHashMap<MethodDeclaration, TypeDeclaration>();
		Set<TypeDeclaration> typeDeclarations = extractTypeDeclarations(compilationUnit);
		for(TypeDeclaration typeDeclaration : typeDeclarations) {
			if(typeDeclaration.resolveBinding().isEqualTo(methodBinding.getDeclaringClass()) ||
					typeDeclaration.resolveBinding().getBinaryName().equals(methodBinding.getDeclaringClass().getBinaryName())) {
				MethodDeclaration[] methodDeclarations = typeDeclaration.getMethods();
				for(MethodDeclaration methodDeclaration : methodDeclarations) {
					if(methodDeclaration.resolveBinding().isEqualTo(methodBinding) || equalSignature(methodDeclaration.resolveBinding(), methodBinding)) {
						matchingMethodDeclarations.put(methodDeclaration, typeDeclaration);
					}
				}
			}
		}
		return matchingMethodDeclarations;
	}

	private Map <MethodDeclaration, TypeDeclaration> getMatchingMethodDeclarationsForSubType(IMethodBinding methodBinding,
			IType subclassType, CompilationUnit compilationUnit) {
		Map<MethodDeclaration, TypeDeclaration> matchingMethodDeclarations = new LinkedHashMap<MethodDeclaration, TypeDeclaration>();
		try {
			if(subclassType.isClass()/* || subclassType.isInterface()*/) {
				Set<TypeDeclaration> typeDeclarations = extractTypeDeclarations(compilationUnit);
				for(TypeDeclaration typeDeclaration : typeDeclarations) {
					if(typeDeclaration.resolveBinding().getQualifiedName().equals(subclassType.getFullyQualifiedName('.'))) {
						MethodDeclaration[] methodDeclarations = typeDeclaration.getMethods();
						for(MethodDeclaration methodDeclaration : methodDeclarations) {
							if(equalSignature(methodDeclaration.resolveBinding(), methodBinding)) {
								matchingMethodDeclarations.put(methodDeclaration, typeDeclaration);
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
								matchingMethodDeclarations.put(methodDeclaration, null);
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

	protected void processExternalMethodInvocation(MethodInvocation methodInvocation, IType subclassType, AbstractVariable variableDeclaration,
			Set<MethodInvocation> processedMethodInvocations) {
		if(variableDeclaration != null) {
			IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
			LibraryClassStorage instance = LibraryClassStorage.getInstance();
			Map<MethodDeclaration, TypeDeclaration> matchingMethodDeclarations = null;
			IClassFile iClassFile = null;
			if(subclassType == null) {
				IMethod iMethod = (IMethod)methodBinding.getJavaElement();
				iClassFile = iMethod.getClassFile();
				CompilationUnit compilationUnit = instance.getCompilationUnit(iClassFile);
				matchingMethodDeclarations = getMatchingMethodDeclarations(methodBinding, compilationUnit);
			}
			else {
				iClassFile = subclassType.getClassFile();
				CompilationUnit compilationUnit = instance.getCompilationUnit(iClassFile);
				matchingMethodDeclarations = getMatchingMethodDeclarationsForSubType(methodBinding, subclassType, compilationUnit);
			}
			for(MethodDeclaration methodDeclaration : matchingMethodDeclarations.keySet()) {
				TypeDeclaration typeDeclaration = matchingMethodDeclarations.get(methodDeclaration);
				Block methodBody = methodDeclaration.getBody();
				if(methodBody != null) {
					if(instance.isAnalyzed(methodDeclaration)) {
						LinkedHashSet<VariableDeclaration> recursivelyDefinedFields = 
							instance.getRecursivelyDefinedFields(methodDeclaration, new LinkedHashSet<MethodDeclaration>());
						for(VariableDeclaration definedField : recursivelyDefinedFields) {
							AbstractVariable originalField = new PlainVariable(definedField);
							AbstractVariable field = composeVariable(variableDeclaration, originalField);
							definedVariables.add(field);
						}
						if(recursivelyDefinedFields.size() > 0)
							putInStateChangingMethodInvocationMap(variableDeclaration, methodInvocation);
						LinkedHashSet<VariableDeclaration> recursivelyUsedFields = 
							instance.getRecursivelyUsedFields(methodDeclaration, new LinkedHashSet<MethodDeclaration>());
						for(VariableDeclaration usedField : recursivelyUsedFields) {
							AbstractVariable originalField = new PlainVariable(usedField);
							AbstractVariable field = composeVariable(variableDeclaration, originalField);
							usedVariables.add(field);
						}
					}
					else {
						ASTInformationGenerator.setCurrentITypeRoot(iClassFile);
						MethodBodyObject methodBodyObject = new MethodBodyObject(methodBody);
						LinkedHashSet<VariableDeclaration> definedFields = new LinkedHashSet<VariableDeclaration>();
						LinkedHashSet<VariableDeclaration> usedFields = new LinkedHashSet<VariableDeclaration>();
						List<FieldInstructionObject> fieldInstructions = methodBodyObject.getFieldInstructions();
						boolean stateChangingMethodInvocation = false;
						for(FieldInstructionObject fieldInstruction : fieldInstructions) {
							SimpleName fieldInstructionName = fieldInstruction.getSimpleName();
							AbstractVariable originalField = null;
							IBinding binding = fieldInstructionName.resolveBinding();
							if(binding.getKind() == IBinding.VARIABLE) {
								IVariableBinding variableBinding = (IVariableBinding)binding;
								if(variableBinding.isField()) {
									IField iField = (IField)variableBinding.getJavaElement();
									IClassFile fieldClassFile = iField.getClassFile();
									CompilationUnit fieldCompilationUnit = instance.getCompilationUnit(fieldClassFile);
									Set<TypeDeclaration> fieldTypeDeclarations = extractTypeDeclarations(fieldCompilationUnit);
									for(TypeDeclaration fieldTypeDeclaration : fieldTypeDeclarations) {
										if(fieldTypeDeclaration.resolveBinding().isEqualTo(variableBinding.getDeclaringClass()) ||
												fieldTypeDeclaration.resolveBinding().getBinaryName().equals(variableBinding.getDeclaringClass().getBinaryName())) {
											FieldDeclaration[] fieldDeclarations = fieldTypeDeclaration.getFields();
											for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
												List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
												for(VariableDeclarationFragment fragment : fragments) {
													if(fragment.resolveBinding().isEqualTo(variableBinding) || 
															fragment.resolveBinding().getName().equals(variableBinding.getName())) {
														originalField = new PlainVariable(fragment);
														break;
													}
												}
												if(originalField != null)
													break;
											}
											if(originalField != null)
												break;
										}
									}
								}
							}
							if(originalField != null) {
								AbstractVariable field = composeVariable(variableDeclaration, originalField);
								List<Assignment> fieldAssignments = methodBodyObject.getFieldAssignments(fieldInstruction);
								List<PostfixExpression> fieldPostfixAssignments = methodBodyObject.getFieldPostfixAssignments(fieldInstruction);
								List<PrefixExpression> fieldPrefixAssignments = methodBodyObject.getFieldPrefixAssignments(fieldInstruction);
								if(!fieldAssignments.isEmpty()) {
									definedVariables.add(field);
									definedFields.add(originalField.getName());
									for(Assignment assignment : fieldAssignments) {
										Assignment.Operator operator = assignment.getOperator();
										if(!operator.equals(Assignment.Operator.ASSIGN)) {
											usedVariables.add(field);
											usedFields.add(originalField.getName());
										}
									}
									stateChangingMethodInvocation = true;
								}
								else if(!fieldPostfixAssignments.isEmpty()) {
									definedVariables.add(field);
									definedFields.add(originalField.getName());
									usedVariables.add(field);
									usedFields.add(originalField.getName());
									stateChangingMethodInvocation = true;
								}
								else if(!fieldPrefixAssignments.isEmpty()) {
									definedVariables.add(field);
									definedFields.add(originalField.getName());
									usedVariables.add(field);
									usedFields.add(originalField.getName());
									stateChangingMethodInvocation = true;
								}
								else {
									usedVariables.add(field);
									usedFields.add(originalField.getName());
								}
							}
						}
						instance.setDefinedFields(methodDeclaration, definedFields);
						instance.setUsedFields(methodDeclaration, usedFields);
						if(stateChangingMethodInvocation) {
							putInStateChangingMethodInvocationMap(variableDeclaration, methodInvocation);
						}
						processedMethodInvocations.add(methodInvocation);
						List<MethodInvocationObject> methodInvocations = methodBodyObject.getMethodInvocations();
						for(MethodInvocationObject methodInvocationObject : methodInvocations) {
							MethodInvocation methodInvocation2 = methodInvocationObject.getMethodInvocation();
							if(methodInvocation2.getExpression() == null || methodInvocation2.getExpression() instanceof ThisExpression) {
								IMethodBinding methodBinding2 = methodInvocation2.resolveMethodBinding();
								MethodDeclaration invokedMethodDeclaration = null;
								IMethod iMethod2 = (IMethod)methodBinding2.getJavaElement();
								IClassFile methodClassFile = iMethod2.getClassFile();
								CompilationUnit methodCompilationUnit = instance.getCompilationUnit(methodClassFile);
								Set<TypeDeclaration> methodTypeDeclarations = extractTypeDeclarations(methodCompilationUnit);
								for(TypeDeclaration methodTypeDeclaration : methodTypeDeclarations) {
									if(methodTypeDeclaration.resolveBinding().isEqualTo(methodBinding2.getDeclaringClass()) ||
											methodTypeDeclaration.resolveBinding().getBinaryName().equals(methodBinding2.getDeclaringClass().getBinaryName())) {
										MethodDeclaration[] methodDeclarations2 = methodTypeDeclaration.getMethods();
										for(MethodDeclaration methodDeclaration2 : methodDeclarations2) {
											if(methodDeclaration2.resolveBinding().isEqualTo(methodBinding2) ||
													equalSignature(methodDeclaration2.resolveBinding(), methodBinding2)) {
												invokedMethodDeclaration = methodDeclaration2;
												break;
											}
										}
										if(invokedMethodDeclaration != null)
											break;
									}
								}
								if(invokedMethodDeclaration != null && !invokedMethodDeclaration.equals(methodDeclaration)) {
									if(!processedMethodInvocations.contains(methodInvocation2)) {
										if((invokedMethodDeclaration.getModifiers() & Modifier.NATIVE) != 0) {
											//method is native
										}
										else {
											instance.addInvokedMethod(methodDeclaration, invokedMethodDeclaration);
											processExternalMethodInvocation(methodInvocation2, null, variableDeclaration, processedMethodInvocations);
										}
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
						IType superType = (IType)typeDeclaration.resolveBinding().getJavaElement();
						Set<IType> subTypes = instance.getSubTypes(superType);
						for(IType subType : subTypes) {
							if(!subType.equals(superType)) {
								IClassFile classFile = subType.getClassFile();
								CompilationUnit compilationUnit = instance.getCompilationUnit(classFile);
								Map<MethodDeclaration, TypeDeclaration> matchingMethodDeclarationMap =
									getMatchingMethodDeclarationsForSubType(methodBinding, subType, compilationUnit);
								for(MethodDeclaration overridingMethod : matchingMethodDeclarationMap.keySet()) {
									instance.addOverridingMethod(methodDeclaration, overridingMethod);
								}
								processExternalMethodInvocation(methodInvocation, subType, variableDeclaration, processedMethodInvocations);
							}
						}
					}
				}
			}
		}
	}

	protected AbstractVariable processFieldInstruction(SimpleName fieldInstructionName,
			VariableDeclaration parameterDeclaration, AbstractVariable previousVariable) {
		VariableDeclaration variableDeclaration = null;
		IBinding binding = fieldInstructionName.resolveBinding();
		if(binding.getKind() == IBinding.VARIABLE) {
			IVariableBinding variableBinding = (IVariableBinding)binding;
			if(variableBinding.isField()) {
				ITypeBinding declaringClassBinding = variableBinding.getDeclaringClass();
				SystemObject systemObject = ASTReader.getSystemObject();
				ClassObject classObject = systemObject.getClassObject(declaringClassBinding.getQualifiedName());
				if(classObject != null) {
					ListIterator<FieldObject> fieldIterator = classObject.getFieldIterator();
					while(fieldIterator.hasNext()) {
						FieldObject fieldObject = fieldIterator.next();
						VariableDeclarationFragment fragment = fieldObject.getVariableDeclarationFragment();
						if(fragment.resolveBinding().isEqualTo(variableBinding)) {
							variableDeclaration = fragment;
							break;
						}
					}
				}
			}
			else if(variableBinding.isParameter() && parameterDeclaration != null) {
				if(parameterDeclaration.resolveBinding().isEqualTo(variableBinding))
					variableDeclaration = parameterDeclaration;
			}
			else {
				for(VariableDeclaration declaration : variableDeclarationsInMethod) {
					if(declaration.resolveBinding().isEqualTo(variableBinding)) {
						variableDeclaration = declaration;
						break;
					}
				}
			}
		}
		if(variableDeclaration != null) {
			AbstractVariable currentVariable = null;
			if(previousVariable == null)
				currentVariable = new PlainVariable(variableDeclaration);
			else
				currentVariable = new CompositeVariable(variableDeclaration, previousVariable);
			if(fieldInstructionName.getParent() instanceof QualifiedName) {
				QualifiedName qualifiedName = (QualifiedName)fieldInstructionName.getParent();
				Name qualifier = qualifiedName.getQualifier();
				if(qualifier instanceof SimpleName) {
					SimpleName qualifierSimpleName = (SimpleName)qualifier;
					if(!qualifierSimpleName.equals(fieldInstructionName))
						return processFieldInstruction(qualifierSimpleName, parameterDeclaration, currentVariable);
					else
						return currentVariable;
				}
				else if(qualifier instanceof QualifiedName) {
					QualifiedName qualifiedName2 = (QualifiedName)qualifier;
					return processFieldInstruction(qualifiedName2.getName(), parameterDeclaration, currentVariable);
				}
			}
			else if(fieldInstructionName.getParent() instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)fieldInstructionName.getParent();
				Expression fieldAccessExpression = fieldAccess.getExpression();
				if(fieldAccessExpression instanceof FieldAccess) {
					FieldAccess fieldAccess2 = (FieldAccess)fieldAccessExpression;
					return processFieldInstruction(fieldAccess2.getName(), parameterDeclaration, currentVariable);
				}
				else if(fieldAccessExpression instanceof ThisExpression) {
					return currentVariable;
				}
			}
			else {
				return currentVariable;
			}
		}
		return null;
	}

	protected AbstractVariable processMethodInvocationExpression(Expression expression, VariableDeclaration parameterDeclaration) {
		if(expression != null) {
			if(expression instanceof QualifiedName) {
				QualifiedName qualifiedName = (QualifiedName)expression;
				return processFieldInstruction(qualifiedName.getName(), parameterDeclaration, null);
			}
			else if(expression instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)expression;
				return processFieldInstruction(fieldAccess.getName(), parameterDeclaration, null);
			}
			else if(expression instanceof SimpleName) {
				SimpleName simpleName = (SimpleName)expression;
				return processFieldInstruction(simpleName, parameterDeclaration, null);
			}
		}
		return null;
	}

	private AbstractVariable composeVariable(AbstractVariable leftSide, AbstractVariable rightSide) {
		if(leftSide instanceof CompositeVariable) {
			CompositeVariable leftSideCompositeVariable = (CompositeVariable)leftSide;
			PlainVariable finalVariable = leftSideCompositeVariable.getFinalVariable();
			CompositeVariable newRightSide = new CompositeVariable(finalVariable.getName(), rightSide);
			AbstractVariable newLeftSide = leftSideCompositeVariable.getLeftPart();
			return composeVariable(newLeftSide, newRightSide);
		}
		else {
			return new CompositeVariable(leftSide.getName(), rightSide);
		}
	}

	protected void processInternalMethodInvocation(ClassObject classObject, MethodObject methodObject,
			MethodInvocation methodInvocation, AbstractVariable variableDeclaration, Set<MethodInvocation> processedMethodInvocations) {
		if(methodObject.isAbstract()) {
			TypeDeclaration typeDeclaration = classObject.getTypeDeclaration();
			IType superType = (IType)typeDeclaration.resolveBinding().getJavaElement();
			try {
				SearchPattern searchPattern = SearchPattern.createPattern(superType, IJavaSearchConstants.IMPLEMENTORS);
				SearchEngine searchEngine = new SearchEngine();
				IJavaSearchScope scope = SearchEngine.createHierarchyScope(superType);
				final Set<IType> subTypes = new LinkedHashSet<IType>();
				SearchRequestor requestor = new TypeSearchRequestor(subTypes);
				searchEngine.search(searchPattern, new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
						scope, requestor, null);
				SystemObject systemObject = ASTReader.getSystemObject();
				Set<IType> subTypesToBeAnalyzed = new LinkedHashSet<IType>();
				if(variableDeclaration != null) {
					VariableDeclaration initialReference = variableDeclaration.getName();
					ITypeBinding initialReferenceTypeBinding = initialReference.resolveBinding().getType();
					for(IType subType : subTypes) {
						if(subType.getFullyQualifiedName('.').equals(initialReferenceTypeBinding.getQualifiedName())) {
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
							if(equalSignature(subMethod.getMethodDeclaration().resolveBinding(), methodObject.getMethodDeclaration().resolveBinding())) {
								processInternalMethodInvocation(subClassObject, subMethod, methodInvocation, variableDeclaration, processedMethodInvocations);
								break;
							}
						}
					}
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		else {
			List<FieldInstructionObject> fieldInstructions = methodObject.getFieldInstructions();
			boolean stateChangingMethodInvocation = false;
			for(FieldInstructionObject fieldInstruction : fieldInstructions) {
				SimpleName fieldInstructionName = fieldInstruction.getSimpleName();
				AbstractVariable originalField = processFieldInstruction(fieldInstructionName, null, null);
				boolean alreadyContainsOriginalField = false;
				if(variableDeclaration != null && originalField instanceof PlainVariable) {
					if(variableDeclaration.containsPlainVariable((PlainVariable)originalField))
						alreadyContainsOriginalField = true;
				}
				if(originalField != null && !alreadyContainsOriginalField) {
					AbstractVariable field = null;
					if(variableDeclaration != null)
						field = composeVariable(variableDeclaration, originalField);
					else
						field = originalField;
					List<Assignment> fieldAssignments = methodObject.getFieldAssignments(fieldInstruction);
					List<PostfixExpression> fieldPostfixAssignments = methodObject.getFieldPostfixAssignments(fieldInstruction);
					List<PrefixExpression> fieldPrefixAssignments = methodObject.getFieldPrefixAssignments(fieldInstruction);
					if(!fieldAssignments.isEmpty()) {
						definedVariables.add(field);
						for(Assignment assignment : fieldAssignments) {
							Assignment.Operator operator = assignment.getOperator();
							if(!operator.equals(Assignment.Operator.ASSIGN))
								usedVariables.add(field);
						}
						stateChangingMethodInvocation = true;
					}
					else if(!fieldPostfixAssignments.isEmpty()) {
						definedVariables.add(field);
						usedVariables.add(field);
						stateChangingMethodInvocation = true;
					}
					else if(!fieldPrefixAssignments.isEmpty()) {
						definedVariables.add(field);
						usedVariables.add(field);
						stateChangingMethodInvocation = true;
					}
					else {
						List<MethodInvocationObject> methodInvocations = methodObject.getMethodInvocations();
						for(MethodInvocationObject methodInvocationObject : methodInvocations) {
							MethodInvocation methodInvocation2 = methodInvocationObject.getMethodInvocation();
							Expression methodInvocationExpression = methodInvocation2.getExpression();
							AbstractVariable variable = processMethodInvocationExpression(methodInvocationExpression, null);
							if(variable != null && variable.equals(originalField)) {
								processArgumentsOfInternalMethodInvocation(methodInvocationObject, methodInvocation2, field);
							}
						}
						usedVariables.add(field);
					}
				}
			}
			if(stateChangingMethodInvocation && variableDeclaration != null) {
				putInStateChangingMethodInvocationMap(variableDeclaration, methodInvocation);
			}
			processedMethodInvocations.add(methodInvocation);
			List<MethodInvocationObject> methodInvocations = methodObject.getMethodInvocations();
			for(MethodInvocationObject methodInvocationObject : methodInvocations) {
				MethodInvocation methodInvocation2 = methodInvocationObject.getMethodInvocation();
				if(methodInvocation2.getExpression() == null || methodInvocation2.getExpression() instanceof ThisExpression) {
					MethodObject methodObject2 = classObject.getMethod(methodInvocationObject);
					if(methodObject2 != null && !methodObject2.equals(methodObject)) {
						if(!processedMethodInvocations.contains(methodInvocation2))
							processInternalMethodInvocation(classObject, methodObject2, methodInvocation2, variableDeclaration, processedMethodInvocations);
					}
				}
			}
		}
	}

	private void processArgumentOfInternalMethodInvocation(MethodObject methodObject, MethodInvocation methodInvocation,
			AbstractVariable argumentDeclaration, VariableDeclaration parameterDeclaration, Set<MethodInvocation> processedMethodInvocations) {
		List<FieldInstructionObject> fieldInstructions = methodObject.getFieldInstructions();
		boolean stateChangingMethodInvocation = false;
		for(FieldInstructionObject fieldInstruction : fieldInstructions) {
			SimpleName fieldInstructionName = fieldInstruction.getSimpleName();
			AbstractVariable originalField = processFieldInstruction(fieldInstructionName, parameterDeclaration, null);
			if(originalField != null && parameterDeclaration.resolveBinding().isEqualTo(originalField.getName().resolveBinding())) {
				AbstractVariable field = new CompositeVariable(argumentDeclaration.getName(), ((CompositeVariable)originalField).getRightPart());
				List<Assignment> fieldAssignments = methodObject.getFieldAssignments(fieldInstruction);
				List<PostfixExpression> fieldPostfixAssignments = methodObject.getFieldPostfixAssignments(fieldInstruction);
				List<PrefixExpression> fieldPrefixAssignments = methodObject.getFieldPrefixAssignments(fieldInstruction);
				if(!fieldAssignments.isEmpty()) {
					definedVariables.add(field);
					for(Assignment assignment : fieldAssignments) {
						Assignment.Operator operator = assignment.getOperator();
						if(!operator.equals(Assignment.Operator.ASSIGN))
							usedVariables.add(field);
					}
					if(field instanceof CompositeVariable) {
						putInStateChangingFieldModificationMap(((CompositeVariable)field).getLeftPart(), field);
					}
					stateChangingMethodInvocation = true;
				}
				else if(!fieldPostfixAssignments.isEmpty()) {
					definedVariables.add(field);
					usedVariables.add(field);
					if(field instanceof CompositeVariable) {
						putInStateChangingFieldModificationMap(((CompositeVariable)field).getLeftPart(), field);
					}
					stateChangingMethodInvocation = true;
				}
				else if(!fieldPrefixAssignments.isEmpty()) {
					definedVariables.add(field);
					usedVariables.add(field);
					if(field instanceof CompositeVariable) {
						putInStateChangingFieldModificationMap(((CompositeVariable)field).getLeftPart(), field);
					}
					stateChangingMethodInvocation = true;
				}
				else {
					List<MethodInvocationObject> methodInvocations = methodObject.getMethodInvocations();
					for(MethodInvocationObject methodInvocationObject : methodInvocations) {
						MethodInvocation methodInvocation2 = methodInvocationObject.getMethodInvocation();
						Expression methodInvocationExpression = methodInvocation2.getExpression();
						AbstractVariable variable = processMethodInvocationExpression(methodInvocationExpression, parameterDeclaration);
						if(variable != null && variable.equals(originalField)) {
							processArgumentsOfInternalMethodInvocation(methodInvocationObject, methodInvocation2, field);
						}
					}
					usedVariables.add(field);
				}
			}
		}
		if(stateChangingMethodInvocation) {
			putInStateChangingMethodInvocationMap(argumentDeclaration, methodInvocation);
		}
		processedMethodInvocations.add(methodInvocation);
		List<MethodInvocationObject> methodInvocations = methodObject.getMethodInvocations();
		for(MethodInvocationObject methodInvocationObject : methodInvocations) {
			MethodInvocation methodInvocation2 = methodInvocationObject.getMethodInvocation();
			Expression invocationExpression = methodInvocation2.getExpression();
			if(invocationExpression != null && invocationExpression instanceof SimpleName) {
				SimpleName invocationExpressionName = (SimpleName)invocationExpression;
				if(parameterDeclaration.resolveBinding().isEqualTo(invocationExpressionName.resolveBinding())) {
					SystemObject systemObject = ASTReader.getSystemObject();
					ClassObject classObject2 = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
					if(classObject2 != null) {
						MethodObject methodObject2 = classObject2.getMethod(methodInvocationObject);
						if(methodObject2 != null) {
							if(!processedMethodInvocations.contains(methodInvocation2))
								processInternalMethodInvocation(classObject2, methodObject2, methodInvocation2,
										argumentDeclaration, new LinkedHashSet<MethodInvocation>());
						}
					}
				}
			}
			List<Expression> arguments = methodInvocation2.arguments();
			int argumentPosition = 0;
			for(Expression expression : arguments) {
				if(expression instanceof SimpleName) {
					SimpleName argumentName = (SimpleName)expression;
					if(parameterDeclaration.resolveBinding().isEqualTo(argumentName.resolveBinding())) {
						SystemObject systemObject = ASTReader.getSystemObject();
						ClassObject classObject2 = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
						if(classObject2 != null) {
							MethodObject methodObject2 = classObject2.getMethod(methodInvocationObject);
							if(methodObject2 != null && !methodObject2.equals(methodObject)) {
								ParameterObject parameter = methodObject2.getParameter(argumentPosition);
								VariableDeclaration parameterDeclaration2 = parameter.getSingleVariableDeclaration();
								if(!processedMethodInvocations.contains(methodInvocation2))
									processArgumentOfInternalMethodInvocation(methodObject2, methodInvocation2,
											argumentDeclaration, parameterDeclaration2, processedMethodInvocations);
							}
						}
					}
				}
				argumentPosition++;
			}
		}
	}

	protected void processArgumentsOfInternalMethodInvocation(MethodInvocationObject methodInvocationObject,
			MethodInvocation methodInvocation, AbstractVariable variable) {
		SystemObject systemObject = ASTReader.getSystemObject();
		ClassObject classObject = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
		if(classObject != null) {
			MethodObject methodObject = classObject.getMethod(methodInvocationObject);
			if(methodObject != null) {
				processInternalMethodInvocation(classObject, methodObject, methodInvocation, variable,
						new LinkedHashSet<MethodInvocation>());
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
							ClassObject classObject2 = systemObject.getClassObject(parameter.getType().getClassType());
							if(classObject2 != null) {
								PlainVariable argumentVariable = new PlainVariable(argumentDeclaration);
								processArgumentOfInternalMethodInvocation(methodObject, methodInvocation,
										argumentVariable, parameterDeclaration, new LinkedHashSet<MethodInvocation>());
							}
						}
					}
					argumentPosition++;
				}
			}
		}
		else {
			processExternalMethodInvocation(methodInvocation, null, variable, new LinkedHashSet<MethodInvocation>());
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
				VariableDeclaration reference = compositeVariable.getName();
				if(reachingAliasSet.containsAlias(reference)) {
					Set<VariableDeclaration> aliases = reachingAliasSet.getAliases(reference);
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
				VariableDeclaration reference = compositeVariable.getName();
				if(reachingAliasSet.containsAlias(reference)) {
					Set<VariableDeclaration> aliases = reachingAliasSet.getAliases(reference);
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
				if(compositeVariable.getName().equals(variableDeclaration))
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
}
