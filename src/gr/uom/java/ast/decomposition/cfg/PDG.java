package gr.uom.java.ast.decomposition.cfg;

public class PDG extends Graph {
	private PDGMethodEntryNode entryNode;
	
	public PDG(CFG cfg) {
		this.entryNode = new PDGMethodEntryNode(cfg.getMethod());
	}
}
