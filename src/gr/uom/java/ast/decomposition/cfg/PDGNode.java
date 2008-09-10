package gr.uom.java.ast.decomposition.cfg;

public class PDGNode extends GraphNode {
	private CFGNode cfgNode;
	
	public PDGNode() {
		super();
	}
	
	public PDGNode(CFGNode cfgNode) {
		this.cfgNode = cfgNode;
		this.id = cfgNode.id;
	}
}
