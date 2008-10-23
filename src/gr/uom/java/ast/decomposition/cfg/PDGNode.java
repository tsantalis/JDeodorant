package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.ast.decomposition.AbstractStatement;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class PDGNode extends GraphNode implements Comparable<PDGNode> {
	private CFGNode cfgNode;
	protected Set<VariableDeclaration> declaredVariables;
	protected Set<VariableDeclaration> definedVariables;
	protected Set<VariableDeclaration> usedVariables;
	private Map<VariableDeclaration, MethodInvocation> stateChangingMethodInvocationMap;
	
	public PDGNode() {
		super();
		this.declaredVariables = new LinkedHashSet<VariableDeclaration>();
		this.definedVariables = new LinkedHashSet<VariableDeclaration>();
		this.usedVariables = new LinkedHashSet<VariableDeclaration>();
		this.stateChangingMethodInvocationMap = new LinkedHashMap<VariableDeclaration, MethodInvocation>();
	}
	
	public PDGNode(CFGNode cfgNode) {
		super();
		this.cfgNode = cfgNode;
		this.id = cfgNode.id;
		cfgNode.setPDGNode(this);
		this.declaredVariables = new LinkedHashSet<VariableDeclaration>();
		this.definedVariables = new LinkedHashSet<VariableDeclaration>();
		this.usedVariables = new LinkedHashSet<VariableDeclaration>();
		this.stateChangingMethodInvocationMap = new LinkedHashMap<VariableDeclaration, MethodInvocation>();
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

	public boolean declaresLocalVariable(VariableDeclaration variable) {
		return declaredVariables.contains(variable);
	}

	public boolean definesLocalVariable(VariableDeclaration variable) {
		return definedVariables.contains(variable);
	}

	public boolean usesLocalVariable(VariableDeclaration variable) {
		return usedVariables.contains(variable);
	}

	public Set<VariableDeclaration> getStateChangingVariables() {
		return stateChangingMethodInvocationMap.keySet();
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

	protected void processMethodInvocation(LocalVariableInstructionObject variableInstruction, VariableDeclaration variableDeclaration) {
		SimpleName variableInstructionName = variableInstruction.getSimpleName();
		if(variableInstructionName.getParent() instanceof MethodInvocation) {
			MethodInvocation methodInvocation = (MethodInvocation)variableInstructionName.getParent();
			if(methodInvocation.getExpression() != null && methodInvocation.getExpression().equals(variableInstructionName)) {
				TypeObject type = variableInstruction.getType();
				String classType = type.getClassType();
				String methodInvocationName = methodInvocation.getName().getIdentifier();
				if(classType.equals("java.util.Iterator") && methodInvocationName.equals("next")) {
					stateChangingMethodInvocationMap.put(variableDeclaration, methodInvocation);
					definedVariables.add(variableDeclaration);
				}
				else if(classType.equals("java.util.Enumeration") && methodInvocationName.equals("nextElement")) {
					stateChangingMethodInvocationMap.put(variableDeclaration, methodInvocation);
					definedVariables.add(variableDeclaration);
				}
				else if(classType.equals("java.util.ListIterator") &&
						(methodInvocationName.equals("next") || methodInvocationName.equals("previous"))) {
					stateChangingMethodInvocationMap.put(variableDeclaration, methodInvocation);
					definedVariables.add(variableDeclaration);
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
					stateChangingMethodInvocationMap.put(variableDeclaration, methodInvocation);
					definedVariables.add(variableDeclaration);
				}
				else if((classType.equals("java.util.Map") || classType.equals("java.util.AbstractMap") ||
						classType.equals("java.util.HashMap") || classType.equals("java.util.Hashtable") ||
						classType.equals("java.util.IdentityHashMap") || classType.equals("java.util.WeakHashMap") ||
						classType.equals("java.util.SortedMap") || classType.equals("java.util.TreeMap")) &&
						(methodInvocationName.equals("put") || methodInvocationName.equals("remove") ||
								methodInvocationName.equals("putAll") || methodInvocationName.equals("clear"))) {
					stateChangingMethodInvocationMap.put(variableDeclaration, methodInvocation);
					definedVariables.add(variableDeclaration);
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
					stateChangingMethodInvocationMap.put(variableDeclaration, methodInvocation);
					definedVariables.add(variableDeclaration);
				}
			}
		}
		usedVariables.add(variableDeclaration);
	}
}
