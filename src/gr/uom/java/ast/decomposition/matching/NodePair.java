package gr.uom.java.ast.decomposition.matching;

public class NodePair {
	private int id1;
	private int id2;
	
	public NodePair(int id1, int id2) {
		this.id1 = id1;
		this.id2 = id2;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 17;
		result = prime * result + id1;
		result = prime * result + id2;
		return result;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if(o instanceof NodePair) {
			NodePair other = (NodePair)o;
			return this.id1 == other.id1 && this.id2 == other.id2;
		}
		return false;
	}
}
