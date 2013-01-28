package gr.uom.java.ast.decomposition;

public class Difference {
	private String firstValue;
	private String secondValue;
	private DifferenceType type; 
	public Difference(String firstValue, String secondValue, DifferenceType type) {
		this.firstValue = firstValue;
		this.secondValue = secondValue;
		this.type = type;
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
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(firstValue).append("\t").append(secondValue).append("\t").append(type);
		return sb.toString();
		
	}
}
