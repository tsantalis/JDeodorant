package gr.uom.java.ast.visualization;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.geometry.Point;

public class RightAnchor extends AbstractConnectionAnchor {

	public RightAnchor(Figure owner){
		super(owner);
	}

	
	public Point getLocation(Point reference) {
		return getOwner().getBounds().getRight();
	}
}
