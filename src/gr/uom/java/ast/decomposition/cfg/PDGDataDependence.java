package gr.uom.java.ast.decomposition.cfg;

public class PDGDataDependence extends PDGAbstractDataDependence {

	public PDGDataDependence(PDGNode src, PDGNode dst,
			AbstractVariable data, CFGBranchNode loop) {
		super(src, dst, PDGDependenceType.DATA, data, loop);
	}
	
}
