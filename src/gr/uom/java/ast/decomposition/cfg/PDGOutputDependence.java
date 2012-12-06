package gr.uom.java.ast.decomposition.cfg;

public class PDGOutputDependence extends PDGAbstractDataDependence {

	public PDGOutputDependence(PDGNode src, PDGNode dst,
			AbstractVariable data, CFGBranchNode loop) {
		super(src, dst, PDGDependenceType.OUTPUT, data, loop);
	}
	
}
