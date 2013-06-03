package gr.uom.java.ast.visualization;

import java.util.List;


import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseMotionListener;
import org.eclipse.draw2d.PolygonDecoration;


public class EntityFigureListener implements MouseMotionListener{
	
	private final EntityFigure figure;
	//private Point location;
	
	public EntityFigureListener(EntityFigure figure) {
		this.figure = figure;
		figure.addMouseMotionListener(this);
		
	}

	public void mouseDragged(MouseEvent me) {
		// TODO Auto-generated method stub
		
	}

	public void mouseEntered(MouseEvent me) {
		// TODO Auto-generated method stub
		List<JConnection> connections = figure.getOutgoingConnections();
		for(JConnection connection: connections){
			connection.setLineWidth(3);
			PolygonDecoration decoration = new PolygonDecoration();
			decoration.setTemplate(PolygonDecoration.TRIANGLE_TIP);
			decoration.setSize(20, 20);
			decoration.setBackgroundColor(connection.getForegroundColor());
			connection.setTargetDecoration(decoration);
		}
	}

	public void mouseExited(MouseEvent me) {
		// TODO Auto-generated method stub
		List<JConnection> connections = figure.getOutgoingConnections();
		for(JConnection connection: connections){
			connection.setLineWidth(1);
			
		}
	}

	public void mouseHover(MouseEvent me) {
		// TODO Auto-generated method stub
		
		
		
		//me.consume();
		
	}

	public void mouseMoved(MouseEvent me) {
		// TODO Auto-generated method stub
		
	}

}
