package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.List;

public class Graph {
	protected List<GraphNode> nodes;
	protected List<GraphEdge> edges;
	
	public Graph() {
		this.nodes = new ArrayList<GraphNode>();
		this.edges = new ArrayList<GraphEdge>();
	}
	
	public void addNode(GraphNode node) {
		nodes.add(node);
	}
	
	public void addEdge(GraphEdge edge) {
		edges.add(edge);
	}
}
