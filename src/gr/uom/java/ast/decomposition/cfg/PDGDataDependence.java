package gr.uom.java.ast.decomposition.cfg;

public class PDGDataDependence extends PDGDependence {
	private AbstractVariable data;
	private CFGBranchNode loop;
	private volatile int hashCode = 0;
	
	public PDGDataDependence(PDGNode src, PDGNode dst, AbstractVariable data, CFGBranchNode loop) {
		super(src, dst);
		this.data = data;
		this.loop = loop;
		src.addOutgoingEdge(this);
		dst.addIncomingEdge(this);
	}

	public AbstractVariable getData() {
		return data;
	}

	public CFGBranchNode getLoop() {
		return loop;
	}

	public boolean isLoopCarried() {
		if(loop != null)
			return true;
		else
			return false;
	}

	public boolean equals(Object o) {
		if(this == o)
    		return true;
		
		if(o instanceof PDGDataDependence) {
			PDGDataDependence dataDependence = (PDGDataDependence)o;
			boolean equalLoop = false;
			if(this.loop != null && dataDependence.loop != null)
				equalLoop = this.loop.equals(dataDependence.loop);
			if(this.loop == null && dataDependence.loop == null)
				equalLoop = true;
			return this.src.equals(dataDependence.src) && this.dst.equals(dataDependence.dst) &&
				this.data.equals(dataDependence.data) && equalLoop;
		}
		return false;
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 37*result + src.hashCode();
			result = 37*result + dst.hashCode();
			result = 37*result + data.hashCode();
			if(loop != null)
				result = 37*result + loop.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	public String toString() {
		return src.toString() + "-->" + data.toString() + "\n" + dst.toString();
	}
}
