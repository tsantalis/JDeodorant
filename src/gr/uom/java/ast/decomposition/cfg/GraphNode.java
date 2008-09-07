package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.List;

public class GraphNode {
	private static int nodeNum = 0;
	protected int id;
	protected List<GraphEdge> incomingEdges;
	protected List<GraphEdge> outgoingEdges;
	
	public GraphNode() {
		nodeNum++;
		this.id = nodeNum;
		this.incomingEdges = new ArrayList<GraphEdge>();
		this.outgoingEdges = new ArrayList<GraphEdge>();
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
