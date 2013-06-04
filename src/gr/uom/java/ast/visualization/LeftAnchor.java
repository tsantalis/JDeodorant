package gr.uom.java.ast.visualization;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.geometry.Point;

public class LeftAnchor extends AbstractConnectionAnchor {

	public LeftAnchor(Figure owner){
		super(owner);
	}

	
	public Point getLocation(Point reference) {
		Point point = getOwner().getBounds().getLeft();
		getOwner().translateToAbsolute(point);
		return point;
	}
}
