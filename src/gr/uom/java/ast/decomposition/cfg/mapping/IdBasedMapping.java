package gr.uom.java.ast.decomposition.cfg.mapping;

public abstract class IdBasedMapping extends NodeMapping {
	protected double id1;
	protected double id2;
	
	public IdBasedMapping(double id1, double id2) {
		this.id1 = id1;
		this.id2 = id2;
	}

	public double getId1() {
		return id1;
	}

	public double getId2() {
		return id2;
	}

	public int compareTo(IdBasedMapping other) {
		return Double.compare(this.id1, other.id1);
	}
}
