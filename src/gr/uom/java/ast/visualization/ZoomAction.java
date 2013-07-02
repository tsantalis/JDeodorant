package gr.uom.java.ast.visualization;

import org.eclipse.draw2d.FreeformViewport;
import org.eclipse.draw2d.ScalableFreeformLayeredPane;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

public class ZoomAction extends Action implements IWorkbenchAction {

	private static final String ID = "gr.uom.java.ast.visualization.ZoomAction";  
	private double scale;
	private ScalableFreeformLayeredPane root;
	
	public ZoomAction(ScalableFreeformLayeredPane root, double scale){  
		setId(ID);  
		
		this.scale = scale;
		this.root = root;
	}  

	public void run() {  
		if(root != null){
			if(scale!= 0){
				this.root.setScale(this.scale);
			}
			else
				scaleToFit();
		}
	}

	public void dispose() {
		
	}

	private void scaleToFit(){
		FreeformViewport viewport = (FreeformViewport) root.getParent();
		Rectangle viewArea = viewport.getClientArea();

		this.root.setScale(1);
		Rectangle rootArea = root.getFreeformExtent().union(0,0);

		double wScale = ((double) viewArea.width)/rootArea.width;
		double hScale = ((double) viewArea.height)/rootArea.height;
		double newScale = Math.min(wScale, hScale);

		this.root.setScale(newScale);
	}

}
