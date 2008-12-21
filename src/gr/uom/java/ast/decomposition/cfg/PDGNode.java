package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.ParameterObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.ast.decomposition.AbstractStatement;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class PDGNode extends GraphNode implements Comparable<PDGNode> {
	private CFGNode cfgNode;
	protected Set<AbstractVariable> declaredVariables;
	protected Set<AbstractVariable> definedVariables;
	protected Set<AbstractVariable> usedVariables;
	protected Set<VariableDeclaration> variableDeclarationsInMethod;
	private Map<AbstractVariable, LinkedHashSet<MethodInvocation>> stateChangingMethodInvocationMap;
	private Map<AbstractVariable, LinkedHashSet<AbstractVariable>> stateChangingFieldModificationMap;
	
	public PDGNode() {
		super();
		this.declaredVariables = new LinkedHashSet<AbstractVariable>();
		this.definedVariables = new LinkedHashSet<AbstractVariable>();
		this.usedVariables = new LinkedHashSet<AbstractVariable>();
		this.stateChangingMethodInvocationMap = new LinkedHashMap<AbstractVariable, LinkedHashSet<MethodInvocation>>();
		this.stateChangingFieldModificationMap = new LinkedHashMap<AbstractVariable, LinkedHashSet<AbstractVariable>>();
	}
	
	public PDGNode(CFGNode cfgNode, Set<VariableDeclaration> variableDeclarationsInMethod) {
		super();
		this.cfgNode = cfgNode;
		this.variableDeclarationsInMethod = variableDeclarationsInMethod;
		this.id = cfgNode.id;
		cfgNode.setPDGNode(this);
		this.declaredVariables = new LinkedHashSet<AbstractVariable>();
		this.definedVariables = new LinkedHashSet<AbstractVariable>();
		this.usedVariables = new LinkedHashSet<AbstractVariable>();
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

	protected void processExternalMethodInvocation(MethodInvocation methodInvocation, AbstractVariable variableDeclaration) {
		if(variableDeclaration != null) {
			IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
			ITypeBinding declaringClassBinding = methodBinding.getDeclaringClass();
			TypeObject type = TypeObject.extractTypeObject(declaringClassBinding.getQualifiedName());
			String classType = type.getClassType();
			String methodInvocationName = methodInvocation.getName().getIdentifier();
			if(classType.equals("java.util.Iterator") && methodInvocationName.equals("next")) {
				putInStateChangingMethodInvocationMap(variableDeclaration, methodInvocation);
			}
			else if(classType.equals("java.util.Enumeration") && methodInvocationName.equals("nextElement")) {
				putInStateChangingMethodInvocationMap(variableDeclaration, methodInvocation);
			}
			else if(classType.equals("java.util.ListIterator") &&
					(methodInvocationName.equals("next") || methodInvocationName.equals("previous"))) {
				putInStateChangingMethodInvocationMap(variableDeclaration, methodInvocation);
			}
			else if((classType.equals("java.util.Collection") || classType.equals("java.util.AbstractCollection") ||
					classType.equals("java.util.List") || classType.equals("java.util.AbstractList") ||
					classType.equals("java.util.ArrayList") || classType.equals("java.util.LinkedList") ||
					classType.equals("java.util.Set") || classType.equals("java.util.AbstractSet") ||
					classType.equals("java.util.HashSet") || classType.equals("java.util.LinkedHashSet") ||
					classType.equals("java.util.SortedSet") || classType.equals("java.util.TreeSet") ||
					classType.equals("java.util.Vector") || classType.equals("java.util.Stack")) &&
					(methodInvocationName.equals("add") || methodInvocationName.equals("remove") ||
							methodInvocationName.equals("addAll") || methodInvocationName.equals("removeAll") ||
							methodInvocationName.equals("addFirst") || methodInvocationName.equals("removeFirst") ||
							methodInvocationName.equals("addLast") || methodInvocationName.equals("removeLast") ||
							methodInvocationName.equals("addElement") || methodInvocationName.equals("removeElement") ||
							methodInvocationName.equals("insertElementAt") || methodInvocationName.equals("removeElementAt") ||
							methodInvocationName.equals("retainAll") || methodInvocationName.equals("set") ||
							methodInvocationName.equals("setElementAt") || methodInvocationName.equals("clear") ||
							methodInvocationName.equals("removeAllElements") ||
							methodInvocationName.equals("push") || methodInvocationName.equals("pop"))) {
				putInStateChangingMethodInvocationMap(variableDeclaration, methodInvocation);
			}
			else if((classType.equals("java.util.Map") || classType.equals("java.util.AbstractMap") ||
					classType.equals("java.util.HashMap") || classType.equals("java.util.Hashtable") ||
					classType.equals("java.util.IdentityHashMap") || classType.equals("java.util.WeakHashMap") ||
					classType.equals("java.util.SortedMap") || classType.equals("java.util.TreeMap")) &&
					(methodInvocationName.equals("put") || methodInvocationName.equals("remove") ||
							methodInvocationName.equals("putAll") || methodInvocationName.equals("clear"))) {
				putInStateChangingMethodInvocationMap(variableDeclaration, methodInvocation);
			}
			else if((classType.equals("java.util.Queue") || classType.equals("java.util.AbstractQueue") || classType.equals("java.util.Deque") ||
					classType.equals("java.util.concurrent.BlockingQueue") || classType.equals("java.util.concurrent.BlockingDeque") ||
					classType.equals("java.util.concurrent.ArrayBlockingQueue") || classType.equals("java.util.ArrayDeque") ||
					classType.equals("java.util.concurrent.ConcurrentLinkedQueue") || classType.equals("java.util.concurrent.DelayQueue") ||
					classType.equals("java.util.concurrent.LinkedBlockingDeque") || classType.equals("java.util.concurrent.LinkedBlockingQueue") ||
					classType.equals("java.util.concurrent.PriorityBlockingQueue") || classType.equals("java.util.PriorityQueue") ||
					classType.equals("java.util.concurrent.SynchronousQueue")) &&
					(methodInvocationName.equals("add") || methodInvocationName.equals("offer") ||
							methodInvocationName.equals("remove") || methodInvocationName.equals("poll") ||
							methodInvocationName.equals("addAll") || methodInvocationName.equals("clear") ||
							methodInvocationName.equals("drainTo") ||
							methodInvocationName.equals("addFirst") || methodInvocationName.equals("addLast") ||
							methodInvocationName.equals("offerFirst") || methodInvocationName.equals("offerLast") ||
							methodInvocationName.equals("removeFirst") || methodInvocationName.equals("removeLast") ||
							methodInvocationName.equals("pollFirst") || methodInvocationName.equals("pollLast") ||
							methodInvocationName.equals("removeFirstOccurrence") || methodInvocationName.equals("removeLastOccurrence") ||
							methodInvocationName.equals("put") || methodInvocationName.equals("take") ||
							methodInvocationName.equals("putFirst") || methodInvocationName.equals("putLast") ||
							methodInvocationName.equals("takeFirst") || methodInvocationName.equals("takeLast"))) {
				putInStateChangingMethodInvocationMap(variableDeclaration, methodInvocation);
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
		List<FieldInstructionObject> fieldInstructions = methodObject.getFieldInstructions();
		boolean stateChangingMethodInvocation = false;
		for(FieldInstructionObject fieldInstruction : fieldInstructions) {
			SimpleName fieldInstructionName = fieldInstruction.getSimpleName();
			AbstractVariable originalField = processFieldInstruction(fieldInstructionName, null, null);
			if(originalField != null) {
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
			processExternalMethodInvocation(methodInvocation, variable);
		}
	}
}
