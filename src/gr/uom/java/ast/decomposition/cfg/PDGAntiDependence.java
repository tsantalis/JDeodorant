package gr.uom.java.ast.decomposition.cfg;

public class PDGAntiDependence extends PDGAbstractDataDependence {

	public PDGAntiDependence(PDGNode src, PDGNode dst,
			AbstractVariable data, CFGBranchNode loop) {
		super(src, dst, PDGDependenceType.ANTI, data, loop);
	}
	
}
