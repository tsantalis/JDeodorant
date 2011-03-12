package gr.uom.java.ast.util.math;

public class Node implements Comparable<Node> {

	protected final String name;
	protected boolean visited = false;   // used for Kosaraju's algorithm and Edmonds's algorithm
	protected int lowlink = -1;          // used for Tarjan's algorithm
	protected int index = -1;            // used for Tarjan's algorithm
	private volatile int hashCode = 0;

	public Node(final String argName) {
		name = argName;
	}

	public String getName() {
		return name;
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		
		if(o instanceof Node) {
			Node node = (Node)o;
			return this.name.equals(node.name);
		}
		return false;
	}

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + name.hashCode();
    		hashCode = result;
    	}
    	return hashCode;
    }

    public String toString() {
    	return name;
    }

	public int compareTo(final Node argNode) {
		return argNode.equals(this) ? 0 : -1;
	}
}
