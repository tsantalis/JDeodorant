package gr.uom.java.ast.util.math;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.Collection;

public class AdjacencyList {

	private Map<Node, LinkedHashSet<Edge>> adjacencies = new HashMap<Node, LinkedHashSet<Edge>>();

	public void addEdge(Node source, Node target, int weight){
		LinkedHashSet<Edge> list;
		if(!adjacencies.containsKey(source)){
			list = new LinkedHashSet<Edge>();
			adjacencies.put(source, list);
		}else{
			list = adjacencies.get(source);
		}
		list.add(new Edge(source, target, weight));
	}

	public LinkedHashSet<Edge> getAdjacent(Node source){
		if(adjacencies.containsKey(source))
			return adjacencies.get(source);
		else
			return new LinkedHashSet<Edge>();
	}

	public void reverseEdge(Edge e){
		adjacencies.get(e.from).remove(e);
		addEdge(e.to, e.from, e.weight);
	}

	public void reverseGraph(){
		adjacencies = getReversedList().adjacencies;
	}

	public AdjacencyList getReversedList(){
		AdjacencyList newlist = new AdjacencyList();
		for(LinkedHashSet<Edge> edges : adjacencies.values()){
			for(Edge e : edges){
				newlist.addEdge(e.to, e.from, e.weight);
			}
		}
		return newlist;
	}

	public Set<Node> getSourceNodeSet(){
		return adjacencies.keySet();
	}

	public Collection<Edge> getAllEdges(){
		ArrayList<Edge> edges = new ArrayList<Edge>();
		for(LinkedHashSet<Edge> e : adjacencies.values()){
			edges.addAll(e);
		}
		return edges;
	}
}
