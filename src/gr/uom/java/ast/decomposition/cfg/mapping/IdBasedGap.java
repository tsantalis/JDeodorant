package gr.uom.java.ast.decomposition.cfg.mapping;

public abstract class IdBasedGap extends NodeMapping {
	protected double id1;
	protected double id2;
	private boolean advancedMatch;
	//if an id is equal to zero it means that this id does not exist
	public IdBasedGap(double id1, double id2, boolean advancedMatch) {
		this.id1 = id1;
		this.id2 = id2;
		this.advancedMatch = advancedMatch;
	}

	public double getId1() {
		return id1;
	}

	public double getId2() {
		return id2;
	}

	public boolean isAdvancedMatch() {
		return advancedMatch;
	}

	public int compareTo(IdBasedGap other) {
		if(this.id1 != 0 && other.id1 != 0)
			return Double.compare(this.id1, other.id1);
		if(this.id2 != 0 && other.id2 != 0)
			return Double.compare(this.id2, other.id2);
		
		if(this.id1 != 0 && other.id2 != 0) {
			double id1 = this.id1;
			double id2 = other.id2;
			if(id1 == id2)
				return -1;
			else
				return Double.compare(id1, id2);
		}
		if(other.id1 != 0 && this.id2 != 0) {
			double id2 = other.id1;
			double id1 = this.id2;
			if(id1 == id2)
				return -1;
			else
				return Double.compare(id1, id2);
		}
		return 0;
	}
}
