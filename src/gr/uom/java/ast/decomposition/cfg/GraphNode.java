package gr.uom.java.ast.decomposition.cfg;

import java.util.LinkedHashSet;
import java.util.Set;

public class GraphNode {
	private static int nodeNum = 0;
	protected int id;
	protected Set<GraphEdge> incomingEdges;
	protected Set<GraphEdge> outgoingEdges;
	
	public GraphNode() {
		nodeNum++;
		this.id = nodeNum;
		this.incomingEdges = new LinkedHashSet<GraphEdge>();
		this.outgoingEdges = new LinkedHashSet<GraphEdge>();
	}
	
	public int getId() {
		return id;
	}

	public void addIncomingEdge(GraphEdge edge) {
		incomingEdges.add(edge);
	}
	
	public void addOutgoingEdge(GraphEdge edge) {
		outgoingEdges.add(edge);
	}
	
	public static void resetNodeNum() {
		nodeNum = 0;
	}
}
