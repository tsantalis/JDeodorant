package gr.uom.java.ast.decomposition.cfg.mapping;

public abstract class Replacement implements Comparable<Replacement> {
	private int startPosition1;
	private int startPosition2;
	
	public Replacement(int startPosition1, int startPosition2) {
		this.startPosition1 = startPosition1;
		this.startPosition2 = startPosition2;
	}

	public int getStartPosition1() {
		return startPosition1;
	}

	public int getStartPosition2() {
		return startPosition2;
	}

	public abstract String getValue1();
	public abstract String getValue2();
	
	public abstract int getLength1();
	public abstract int getLength2();

	public int compareTo(Replacement r) {
		return Integer.compare(this.startPosition1, r.startPosition1);
	}
}
