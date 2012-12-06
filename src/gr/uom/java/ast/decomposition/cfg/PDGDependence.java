package gr.uom.java.ast.decomposition.cfg;

public abstract class PDGDependence extends GraphEdge {
	private PDGDependenceType type;
	
	public PDGDependence(PDGNode src, PDGNode dst, PDGDependenceType type) {
		super(src, dst);
		this.type = type;
	}

	public GraphNode getSrc() {
		return src;
	}

	public GraphNode getDst() {
		return dst;
	}

	public PDGDependenceType getType() {
		return type;
	}
}
