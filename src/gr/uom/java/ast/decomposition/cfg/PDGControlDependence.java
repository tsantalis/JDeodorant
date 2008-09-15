package gr.uom.java.ast.decomposition.cfg;

public class PDGControlDependence extends PDGDependence {
	private boolean trueControlDependence = false;
	private boolean falseControlDependence = false;
	
	public PDGControlDependence(PDGNode src, PDGNode dst) {
		super(src, dst);
	}

	public boolean isTrueControlDependence() {
		return trueControlDependence;
	}

	public void setTrueControlDependence(boolean trueControlDependence) {
		this.trueControlDependence = trueControlDependence;
	}

	public boolean isFalseControlDependence() {
		return falseControlDependence;
	}

	public void setFalseControlDependence(boolean falseControlDependence) {
		this.falseControlDependence = falseControlDependence;
	}

	public String toString() {
		StringBuilder type = new StringBuilder();
		if(trueControlDependence)
			type.append("T");
		else if(falseControlDependence)
			type.append("F");
		return src.toString() + "-->" + type.toString() + "\n" + dst.toString();
	}
}
