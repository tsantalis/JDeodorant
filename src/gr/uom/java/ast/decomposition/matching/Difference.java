package gr.uom.java.ast.decomposition.matching;

public class Difference {
	private String firstValue;
	private String secondValue;
	private DifferenceType type;
	private int weight = 1;
	private volatile int hashCode = 0;
	
	public Difference(String firstValue, String secondValue, DifferenceType type) {
		this.firstValue = firstValue;
		this.secondValue = secondValue;
		this.type = type;
	}

	public Difference(String firstValue, String secondValue, DifferenceType type, int weight) {
		this(firstValue, secondValue, type);
		this.weight = weight;
	}

	public DifferenceType getType() {
		return type;
	}
	
	public String getFirstValue() {
		return firstValue;
	}
	
	public String getSecondValue() {
		return secondValue;
	}
	
	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof Difference) {
			Difference other = (Difference)o;
			return this.type.equals(other.type) &&
					this.firstValue.equals(other.firstValue) &&
					this.secondValue.equals(other.secondValue);
		}
		return false;
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 37*result + type.hashCode();
			result = 37*result + firstValue.hashCode();
			result = 37*result + secondValue.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(firstValue).append("\t").append(secondValue).append("\t").append(type);
		return sb.toString();
	}
}
