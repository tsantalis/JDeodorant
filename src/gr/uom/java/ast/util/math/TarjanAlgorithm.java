package gr.uom.java.ast.util.math;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public class TarjanAlgorithm {
	private int index = 0;
	private ArrayList<Node> stack = new ArrayList<Node>();
	private AdjacencyList list;
	private LinkedHashSet<LinkedHashSet<Node>> SCC = new LinkedHashSet<LinkedHashSet<Node>>();
	
	public TarjanAlgorithm(AdjacencyList list) {
		this.list = list;
		for(Node v : list.getSourceNodeSet())
			if(v.index == -1)
				tarjan(v);
	}

	private void tarjan(Node v){
		v.index = index;
		v.lowlink = index;
		index++;
		stack.add(0, v);
		for(Edge e : list.getAdjacent(v)){
			Node n = e.to;
			if(n.index == -1){
				tarjan(n);
				v.lowlink = Math.min(v.lowlink, n.lowlink);
			}else if(stack.contains(n)){
				v.lowlink = Math.min(v.lowlink, n.index);
			}
		}
		if(v.lowlink == v.index){
			Node n;
			LinkedHashSet<Node> component = new LinkedHashSet<Node>();
			do{
				n = stack.remove(0);
				component.add(n);
			}while(n != v);
			SCC.add(component);
		}
	}

	public boolean belongToTheSameStronglyConnectedComponent(String s1, String s2) {
		Node n1 = new Node(s1);
		Node n2 = new Node(s2);
		for(LinkedHashSet<Node> component : SCC) {
			if(component.contains(n1) && component.contains(n2))
				return true;
		}
		return false;
	}
}
