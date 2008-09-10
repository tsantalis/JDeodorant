package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.MethodObject;

public class PDGMethodEntryNode extends PDGNode {
	private MethodObject method;
	
	public PDGMethodEntryNode(MethodObject method) {
		this.method = method;
		this.id = 0;
	}
}
