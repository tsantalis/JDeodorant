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
}
