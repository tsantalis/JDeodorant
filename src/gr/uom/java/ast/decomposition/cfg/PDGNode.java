package gr.uom.java.ast.decomposition.cfg;

public class PDGNode extends GraphNode {
	private CFGNode cfgNode;
	
	public PDGNode() {
		super();
	}
	
	public PDGNode(CFGNode cfgNode) {
		super();
		this.cfgNode = cfgNode;
		this.id = cfgNode.id;
		cfgNode.setPDGNode(this);
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
