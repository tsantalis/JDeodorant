package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.LocalVariableInstructionObject;

import java.util.LinkedHashSet;
import java.util.Set;

public class PDGNode extends GraphNode {
	private CFGNode cfgNode;
	protected Set<LocalVariableInstructionObject> definedVariables;
	protected Set<LocalVariableInstructionObject> usedVariables;
	
	public PDGNode() {
		super();
		this.definedVariables = new LinkedHashSet<LocalVariableInstructionObject>();
		this.usedVariables = new LinkedHashSet<LocalVariableInstructionObject>();
	}
	
	public PDGNode(CFGNode cfgNode) {
		super();
		this.cfgNode = cfgNode;
		this.id = cfgNode.id;
		cfgNode.setPDGNode(this);
		this.definedVariables = new LinkedHashSet<LocalVariableInstructionObject>();
		this.usedVariables = new LinkedHashSet<LocalVariableInstructionObject>();
	}

	public CFGNode getCFGNode() {
		return cfgNode;
	}

	public BasicBlock getBasicBlock() {
		return cfgNode.getBasicBlock();
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
}
