package gr.uom.java.ast.decomposition.cfg;

public class PDGAntiDependence extends PDGDependence {
	private Variable data;
	private boolean loopCarried;
	private volatile int hashCode = 0;
	
	public PDGAntiDependence(PDGNode src, PDGNode dst, Variable data, boolean loopCarried) {
		super(src, dst);
		this.data = data;
		this.loopCarried = loopCarried;
		src.addOutgoingEdge(this);
		dst.addIncomingEdge(this);
	}

	public Variable getData() {
		return data;
	}

	public boolean isLoopCarried() {
		return loopCarried;
	}

	public boolean equals(Object o) {
		if(this == o)
    		return true;
		
		if(o instanceof PDGAntiDependence) {
			PDGAntiDependence antiDependence = (PDGAntiDependence)o;
			return this.src.equals(antiDependence.src) && this.dst.equals(antiDependence.dst) &&
				this.data.equals(antiDependence.data);
		}
		return false;
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 37*result + src.hashCode();
			result = 37*result + dst.hashCode();
			result = 37*result + data.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	public String toString() {
		return src.toString() + "-->" + data.toString() + "\n" + dst.toString();
	}
}
