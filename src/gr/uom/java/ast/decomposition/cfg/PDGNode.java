package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassInstanceCreationObject;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.ConstructorObject;
import gr.uom.java.ast.CreationObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.VariableDeclarationObject;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.util.ExpressionExtractor;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class PDGNode extends GraphNode implements Comparable<PDGNode> {
	private CFGNode cfgNode;
	protected Set<AbstractVariable> declaredVariables;
	protected Set<AbstractVariable> definedVariables;
	protected Set<AbstractVariable> usedVariables;
	protected Set<CreationObject> createdTypes;
	protected Set<String> thrownExceptionTypes;
	protected Set<VariableDeclarationObject> variableDeclarationsInMethod;
	protected Set<FieldObject> fieldsAccessedInMethod;
	private Set<AbstractVariable> originalDefinedVariables;
	private Set<AbstractVariable> originalUsedVariables;
	private MethodCallAnalyzer methodCallAnalyzer;
	
	public PDGNode() {
		super();
		this.declaredVariables = new LinkedHashSet<AbstractVariable>();
		this.definedVariables = new LinkedHashSet<AbstractVariable>();
		this.usedVariables = new LinkedHashSet<AbstractVariable>();
		this.createdTypes = new LinkedHashSet<CreationObject>();
		this.thrownExceptionTypes = new LinkedHashSet<String>();
	}
	
	public PDGNode(CFGNode cfgNode, Set<VariableDeclarationObject> variableDeclarationsInMethod,
			Set<FieldObject> fieldsAccessedInMethod) {
		super();
		this.cfgNode = cfgNode;
		this.variableDeclarationsInMethod = variableDeclarationsInMethod;
		this.fieldsAccessedInMethod = fieldsAccessedInMethod;
		this.id = cfgNode.id;
		cfgNode.setPDGNode(this);
		this.declaredVariables = new LinkedHashSet<AbstractVariable>();
		this.definedVariables = new LinkedHashSet<AbstractVariable>();
		this.usedVariables = new LinkedHashSet<AbstractVariable>();
		this.createdTypes = new LinkedHashSet<CreationObject>();
		this.thrownExceptionTypes = new LinkedHashSet<String>();
		this.methodCallAnalyzer = new MethodCallAnalyzer(definedVariables, usedVariables, thrownExceptionTypes, this.variableDeclarationsInMethod);
	}

	public Iterator<AbstractVariable> getDeclaredVariableIterator() {
		return declaredVariables.iterator();
	}

	public Iterator<AbstractVariable> getDefinedVariableIterator() {
		return definedVariables.iterator();
	}

	public Iterator<AbstractVariable> getUsedVariableIterator() {
		return usedVariables.iterator();
	}

	public CFGNode getCFGNode() {
		return cfgNode;
	}

	public Set<String> getThrownExceptionTypes() {
		return thrownExceptionTypes;
	}

	public Iterator<GraphEdge> getDependenceIterator() {
		Set<GraphEdge> allEdges = new LinkedHashSet<GraphEdge>();
		allEdges.addAll(incomingEdges);
		allEdges.addAll(outgoingEdges);
		return allEdges.iterator();
	}

	public Iterator<GraphEdge> getOutgoingDependenceIterator() {
		return outgoingEdges.iterator();
	}

	public Iterator<GraphEdge> getIncomingDependenceIterator() {
		return incomingEdges.iterator();
	}

	public Set<PDGNode> getControlDependentNodes() {
		Set<PDGNode> nodes = new LinkedHashSet<PDGNode>();
		for(GraphEdge edge : outgoingEdges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(dependence instanceof PDGControlDependence) {
				PDGControlDependence controlDependence = (PDGControlDependence)dependence;
				PDGNode dstNode = (PDGNode)controlDependence.getDst();
				nodes.add(dstNode);
			}
		}
		return nodes;
	}

	public Set<PDGNode> getTrueControlDependentNodes() {
		Set<PDGNode> nodes = new LinkedHashSet<PDGNode>();
		for(GraphEdge edge : outgoingEdges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(dependence instanceof PDGControlDependence) {
				PDGControlDependence controlDependence = (PDGControlDependence)dependence;
				if(controlDependence.isTrueControlDependence()) {
					PDGNode dstNode = (PDGNode)controlDependence.getDst();
					nodes.add(dstNode);
				}
			}
		}
		return nodes;
	}

	public PDGNode getControlDependenceParent() {
		for(GraphEdge edge : incomingEdges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(dependence instanceof PDGControlDependence) {
				PDGNode srcNode = (PDGNode)dependence.src;
				return srcNode;
			}
		}
		return null;
	}

	public boolean isControlDependentOnNode(PDGNode node) {
		PDGNode parent = this.getControlDependenceParent();
		while(parent != null) {
			if(parent.equals(node)) {
				return true;
			}
			parent = parent.getControlDependenceParent();
		}
		return false;
	}

	public boolean isControlDependentOnOneOfTheNodes(Set<PDGNode> nodes) {
		for(PDGNode node : nodes) {
			if(this.isControlDependentOnNode(node)) {
				return true;
			}
		}
		return false;
	}

	public PDGControlDependence getIncomingControlDependence() {
		for(GraphEdge edge : incomingEdges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(dependence instanceof PDGControlDependence) {
				return (PDGControlDependence)dependence;
			}
		}
		return null;
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

	public Set<AbstractVariable> incomingDataDependencesFromNodesDeclaringOrDefiningVariables() {
		Set<AbstractVariable> dataDependences = new LinkedHashSet<AbstractVariable>();
		for(GraphEdge edge : incomingEdges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(dependence instanceof PDGDataDependence) {
				PDGDataDependence dataDependence = (PDGDataDependence)dependence;
				PDGNode srcNode = (PDGNode)dependence.src;
				if(srcNode.declaresLocalVariable(dataDependence.getData()) || srcNode.definesLocalVariable(dataDependence.getData())) {
					dataDependences.add(dataDependence.getData());
				}
			}
			else if(dependence instanceof PDGOutputDependence) {
				PDGOutputDependence outputDependence = (PDGOutputDependence)dependence;
				PDGNode srcNode = (PDGNode)dependence.src;
				if(srcNode.declaresLocalVariable(outputDependence.getData()) || srcNode.definesLocalVariable(outputDependence.getData())) {
					dataDependences.add(outputDependence.getData());
				}
			}
		}
		return dataDependences;
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
		if(variable instanceof PlainVariable && this.definesLocalVariable(variable)) {
			PlainVariable plainVariable = (PlainVariable)variable;
			String variableType = plainVariable.getVariableType();
			for(CreationObject creation : createdTypes) {
				if(creation instanceof ClassInstanceCreationObject) {
					ITypeBinding createdTypeBinding = ((ClassInstanceCreationObject)creation).getClassInstanceCreation().resolveTypeBinding();
					String superclassName = createdTypeBinding.getSuperclass() != null ? createdTypeBinding.getSuperclass().getQualifiedName() : null;
					Set<String> implementedInterfaces = new LinkedHashSet<String>();
					for(ITypeBinding implementedInterface : createdTypeBinding.getInterfaces()) {
						implementedInterfaces.add(implementedInterface.getQualifiedName());
					}
					if(variableType.equals(createdTypeBinding.getQualifiedName()) || variableType.equals(superclassName) || implementedInterfaces.contains(variableType))
						return true;
				}
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

	protected void processArgumentsOfInternalMethodInvocation(MethodInvocationObject methodInvocationObject, AbstractVariable variable) {
		SystemObject systemObject = ASTReader.getSystemObject();
		MethodInvocation methodInvocation = methodInvocationObject.getMethodInvocation();
		IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
		ClassObject classObject = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
		MethodObject methodObject = null;
		if(classObject != null) {
			methodObject = classObject.getMethod(methodInvocationObject);
		}
		if(classObject == null || methodObject != null) {
			//classObject == null => external method call
			//methodObject != null => the internal method might not exist, in the case of built-in enumeration methods, such as values() and valueOf()
			methodCallAnalyzer.processArgumentsOfInternalMethodInvocation(classObject, methodObject, methodInvocation.arguments(), methodBinding, variable);
		}
	}

	protected void processArgumentsOfInternalClassInstanceCreation(ClassInstanceCreationObject classInstanceCreationObject, AbstractVariable variable) {
		SystemObject systemObject = ASTReader.getSystemObject();
		ClassInstanceCreation classInstanceCreation = classInstanceCreationObject.getClassInstanceCreation();
		IMethodBinding methodBinding = classInstanceCreation.resolveConstructorBinding();
		ClassObject classObject = systemObject.getClassObject(classInstanceCreationObject.getType().getClassType());
		ConstructorObject constructorObject = null;
		if(classObject != null) {
			constructorObject = classObject.getConstructor(classInstanceCreationObject);
		}
		if((classObject == null && !methodBinding.getDeclaringClass().isAnonymous() && !methodBinding.getDeclaringClass().isLocal()) || constructorObject != null) {
			//classObject == null && !methodBinding.getDeclaringClass().isAnonymous() => external constructor call that is not an anonymous class declaration
			//constructorObject != null => the internal constructor might not exist, in the case the default constructor is called
			methodCallAnalyzer.processArgumentsOfInternalMethodInvocation(classObject, constructorObject, classInstanceCreation.arguments(), methodBinding, variable);
		}
	}

	public void updateReachingAliasSet(ReachingAliasSet reachingAliasSet) {
		Set<VariableDeclarationObject> variableDeclarations = new LinkedHashSet<VariableDeclarationObject>();
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
						for(VariableDeclarationObject declarationObject : variableDeclarations) {
							VariableDeclaration declaration = declarationObject.getVariableDeclaration();
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
			Set<VariableDeclarationObject> variableDeclarations, Assignment assignment) {
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
			for(VariableDeclarationObject declarationObject : variableDeclarations) {
				VariableDeclaration declaration = declarationObject.getVariableDeclaration();
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
				for(VariableDeclarationObject declarationObject : variableDeclarations) {
					VariableDeclaration declaration = declarationObject.getVariableDeclaration();
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
		Set<VariableDeclarationObject> variableDeclarations = new LinkedHashSet<VariableDeclarationObject>();
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
						for(VariableDeclarationObject declarationObject : variableDeclarations) {
							VariableDeclaration declaration = declarationObject.getVariableDeclaration();
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

	public boolean changesStateOfVariable(PlainVariable plainVariable) {
		for(AbstractVariable abstractVariable : definedVariables) {
			if(abstractVariable instanceof CompositeVariable) {
				CompositeVariable compositeVariable = (CompositeVariable)abstractVariable;
				if(compositeVariable.getInitialVariable().equals(plainVariable)) {
					return true;
				}
			}
		}
		return false;
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
}
