package gr.uom.java.ast.visualization;

import org.eclipse.draw2d.RectangleFigure;

public abstract class PackageMapFigure extends RectangleFigure implements Comparable<PackageMapFigure>  {
	
	
	public abstract void draw();
	
	public abstract int numberOfClasses();

}
