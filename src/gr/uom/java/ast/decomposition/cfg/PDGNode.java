package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.decomposition.AbstractStatement;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class PDGNode extends GraphNode implements Comparable<PDGNode> {
	private CFGNode cfgNode;
	protected Set<VariableDeclaration> declaredVariables;
	protected Set<VariableDeclaration> definedVariables;
	protected Set<VariableDeclaration> usedVariables;
	protected Map<VariableDeclaration, MethodInvocation> stateChangingMethodInvocationMap;
	
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
}
