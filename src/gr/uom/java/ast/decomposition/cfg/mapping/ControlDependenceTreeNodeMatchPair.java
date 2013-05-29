package gr.uom.java.ast.decomposition.cfg.mapping;

public class ControlDependenceTreeNodeMatchPair {
	private ControlDependenceTreeNode node1;
	private ControlDependenceTreeNode node2;
	private volatile int hashCode = 0;

	public ControlDependenceTreeNodeMatchPair(ControlDependenceTreeNode node1, ControlDependenceTreeNode node2) {
		this.node1 = node1;
		this.node2 = node2;
	}

	public ControlDependenceTreeNode getNode1() {
		return node1;
	}

	public ControlDependenceTreeNode getNode2() {
		return node2;
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 37*result + node1.hashCode();
			result = 37*result + node2.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof ControlDependenceTreeNodeMatchPair) {
			ControlDependenceTreeNodeMatchPair pair = (ControlDependenceTreeNodeMatchPair)o;
			return this.node1.equals(pair.node1) && this.node2.equals(pair.node2);
		}
		return false;
	}

	public String toString() {
		return node1.toString() + node2.toString();
	}
}
