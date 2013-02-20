package gr.uom.java.ast.decomposition.cfg;

import java.util.LinkedHashSet;
import java.util.Set;

public class Graph {
	protected Set<GraphNode> nodes;
	protected Set<GraphEdge> edges;
	
	public Graph() {
		this.nodes = new LinkedHashSet<GraphNode>();
		this.edges = new LinkedHashSet<GraphEdge>();
	}
	
	public Set<GraphNode> getNodes() {
		return nodes;
	}

	public Set<GraphEdge> getEdges() {
		return edges;
	}

	public void addNode(GraphNode node) {
		nodes.add(node);
	}
	
	public void addEdge(GraphEdge edge) {
		edges.add(edge);
	}
}
