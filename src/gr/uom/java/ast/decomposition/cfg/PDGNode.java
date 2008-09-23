package gr.uom.java.ast.decomposition.cfg;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class PDGNode extends GraphNode implements Comparable<PDGNode> {
	private CFGNode cfgNode;
	protected Set<VariableDeclaration> definedVariables;
	protected Set<VariableDeclaration> usedVariables;
	
	public PDGNode() {
		super();
		this.definedVariables = new LinkedHashSet<VariableDeclaration>();
		this.usedVariables = new LinkedHashSet<VariableDeclaration>();
	}
	
	public PDGNode(CFGNode cfgNode) {
		super();
		this.cfgNode = cfgNode;
		this.id = cfgNode.id;
		cfgNode.setPDGNode(this);
		this.definedVariables = new LinkedHashSet<VariableDeclaration>();
		this.usedVariables = new LinkedHashSet<VariableDeclaration>();
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

	public boolean definesLocalVariable(VariableDeclaration variable) {
		return definedVariables.contains(variable);
	}

	public boolean usesLocalVariable(VariableDeclaration variable) {
		return usedVariables.contains(variable);
	}

	public BasicBlock getBasicBlock() {
		return cfgNode.getBasicBlock();
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
