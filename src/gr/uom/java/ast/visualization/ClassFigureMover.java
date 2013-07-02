package gr.uom.java.ast.visualization;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.*;

/**
 * A Draw2D mouse listener for dragging figures around the diagram. Listeners such as this
 * are useful for manipulating Draw2D diagrams, but are superseded by higher level GEF
 * functionality if the GEF framework is used.
 */
public class ClassFigureMover
	implements MouseListener, MouseMotionListener
{
	private final IFigure figure;
	private Point location;

	/**
	 * Construct a new instance for dragging the specified figure around the diagram.
	 * 
	 * @param figure the figure to be dragged.
	 */
	public ClassFigureMover(IFigure figure) {
		this.figure = figure;
		figure.addMouseListener(this);
		figure.addMouseMotionListener(this);
	}

	/**
	 * Cache the mouse down location and mark the event as consumed so that the Draw2D
	 * event dispatcher will send all mouse events to the figure associated with this
	 * listener until the mouse button is released.
	 */
	public void mousePressed(MouseEvent event) {
		location = event.getLocation();
		event.consume();
	}

	/**
	 * Process mouse drag events by moving the associated figure and updating the
	 * appropriate figure in the diagram.
	 */
	public void mouseDragged(MouseEvent event) {
		if (location == null)
			return;
		Point newLocation = event.getLocation();
		if (newLocation == null)
			return;
		Dimension offset = newLocation.getDifference(location);
		if (offset.width == 0 && offset.height == 0)
			return;
		location = newLocation;

		UpdateManager updateMgr = figure.getUpdateManager();
		LayoutManager layoutMgr = figure.getParent().getLayoutManager();

		Rectangle bounds = figure.getBounds();
		updateMgr.addDirtyRegion(figure.getParent(), bounds);
		// Copy the rectangle using getCopy() to prevent undesired side-effects
		bounds = bounds.getCopy().translate(offset.width, offset.height);
		layoutMgr.setConstraint(figure, bounds);
		figure.translate(offset.width, offset.height);
		updateMgr.addDirtyRegion(figure.getParent(), bounds);
		
		event.consume();
	}

	/**
	 * Clear the last cached mouse location signaling the end of the drag figure
	 * operation.
	 */
	public void mouseReleased(MouseEvent event) {
		if (location == null)
			return;
		location = null;
		event.consume();
	}

	public void mouseMoved(MouseEvent event) {
	}

	public void mouseDoubleClicked(MouseEvent event) {
	}

	public void mouseEntered(MouseEvent event) {
	}

	public void mouseExited(MouseEvent event) {
	}

	public void mouseHover(MouseEvent event) {
	}
}