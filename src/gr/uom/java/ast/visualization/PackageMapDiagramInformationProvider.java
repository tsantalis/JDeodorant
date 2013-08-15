package gr.uom.java.ast.visualization;

import gr.uom.java.jdeodorant.refactoring.views.CodeSmellPackageExplorer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;


public class PackageMapDiagramInformationProvider implements IInformationProvider {

	private final PackageMapDiagram diagram;

	public PackageMapDiagramInformationProvider(PackageMapDiagram diagram){
		this.diagram = diagram;
	}

	public PMClassFigure getInformation(Point location) {
		PMClassFigure classFigure = diagram.getSelectedClass();
		if(classFigure != null){
			
			return classFigure;
		}
		return null;
	}

	public Rectangle getArea(Point location) {
		// TODO Auto-generated method stub
		PMClassFigure classFigure = diagram.getSelectedClass();

		if(classFigure != null){
			org.eclipse.draw2d.geometry.Rectangle bounds = classFigure.getBounds();
			double scaleFactor = CodeSmellPackageExplorer.SCALE_FACTOR;
			Rectangle rect;
			org.eclipse.draw2d.geometry.Point figureLocation = classFigure.getLocation();
			classFigure.translateToAbsolute(figureLocation);

			if(scaleFactor <1)
				rect = new Rectangle((int) (figureLocation.x*scaleFactor), (int) (figureLocation.y*scaleFactor), (int) (bounds.width*scaleFactor), (int) (bounds.height*scaleFactor));
			else
				rect = new Rectangle(figureLocation.x,figureLocation.y, bounds.width, bounds.height);
			return rect;
		}
		return null;
	}

}
