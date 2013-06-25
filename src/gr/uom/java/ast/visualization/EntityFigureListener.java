package gr.uom.java.ast.visualization;

import java.util.List;


import org.eclipse.draw2d.ConnectionEndpointLocator;
import org.eclipse.draw2d.ConnectionLocator;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseMotionListener;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PositionConstants;


public class EntityFigureListener implements MouseMotionListener{

	private final EntityFigure figure;

	public EntityFigureListener(EntityFigure figure) {
		this.figure = figure;
		figure.addMouseMotionListener(this);

	}

	public void mouseDragged(MouseEvent me) {
		// TODO Auto-generated method stub

	}

	public void mouseEntered(MouseEvent me) {

		List<JConnection> connections = figure.getOutgoingConnections();
		for(JConnection connection: connections){

			connection.setLineWidth(3);
			Label l = connection.getLabel();

			if(l != null){

				//String fontStyle = "Arial";
				ConnectionEndpointLocator locator = new ConnectionEndpointLocator(connection, true);

				if(connection.isWrite()){
					locator.setUDistance(95);
					locator.setVDistance(0);
				} else{
					locator.setUDistance(42);
					locator.setVDistance(0);
				}


				//l.setFont(new Font(null, fontStyle, 14 , SWT.BOLD));
				l.setFont(DecorationConstants.highlightFont);

				connection.add(l, locator);
			}

			PolygonDecoration decoration = new PolygonDecoration();
			decoration.setTemplate(PolygonDecoration.TRIANGLE_TIP);
			decoration.setSize(20, 20);
			decoration.setBackgroundColor(connection.getForegroundColor());
			connection.setTargetDecoration(decoration);
		}
	}

	public void mouseExited(MouseEvent me) {

		List<JConnection> connections = figure.getOutgoingConnections();
		for(JConnection connection: connections){
			connection.setAlpha(null);
			connection.setLineWidth(1);
			Label l = connection.getLabel();


			if(l != null){
				//l.setFont(new Font(null, "Arial", 10, SWT.BOLD));
				l.setFont(DecorationConstants.normalFont);
				ConnectionLocator locator = connection.getLocator();
				locator.setRelativePosition(PositionConstants.CENTER);
				connection.add(l, locator);
			}

		}
	}

	public void mouseHover(MouseEvent me) {
		// TODO Auto-generated method stub



	}

	public void mouseMoved(MouseEvent me) {
		// TODO Auto-generated method stub

	}

}
