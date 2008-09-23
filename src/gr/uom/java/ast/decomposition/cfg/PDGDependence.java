package gr.uom.java.ast.decomposition.cfg;

public abstract class PDGDependence extends GraphEdge {

	public PDGDependence(PDGNode src, PDGNode dst) {
		super(src, dst);
	}

	public GraphNode getSrc() {
		return src;
	}

	public GraphNode getDst() {
		return dst;
	}
}
