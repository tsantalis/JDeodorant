package gr.uom.java.ast.util.math;

public class Edge implements Comparable<Edge> {

	protected final Node from, to;
	protected final int weight;
	private volatile int hashCode = 0;

	public Edge(final Node argFrom, final Node argTo, final int argWeight){
		from = argFrom;
		to = argTo;
		weight = argWeight;
	}

	public Node getSource() {
		return from;
	}

	public Node getTarget() {
		return to;
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		
		if(o instanceof Edge) {
			Edge edge = (Edge)o;
			return this.from.equals(edge.from) && this.to.equals(edge.to);
		}
		return false;
	}

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + from.hashCode();
    		result = 37*result + to.hashCode();
    		hashCode = result;
    	}
    	return hashCode;
    }

    public String toString() {
    	return from + " -> " + to;
    }

	public int compareTo(final Edge argEdge){
		return weight - argEdge.weight;
	}
}
