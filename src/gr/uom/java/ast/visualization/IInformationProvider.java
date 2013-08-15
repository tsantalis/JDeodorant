package gr.uom.java.ast.visualization;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Information provider interface to get information about elements under the mouse cursor.
 * This interface is analog to {@link org.eclipse.jface.text.information.IInformationProvider} in
 * {@link org.eclipse.jface.text.information.InformationPresenter}.
 */
public interface IInformationProvider {

	/**
	 * Returns information about the element at the specified location.
	 * The information returned is used to display an appropriate tooltip.
	 * @param location the location of the element (the coordinate is in the receiver's coordinate system)
	 * @return information about the element, or <code>null</code> if none is available
	 */
	PMClassFigure getInformation(Point location);

	/**
	 * Returns the area of the element at the specified location.
	 * The area returned is used to place an appropriate tooltip.
	 * @param location the location of the element (the coordinate is in the receiver's coordinate system)
	 * @return the area of the element, or <code>null</code> if none is available
	 */
	Rectangle getArea(Point location);
}