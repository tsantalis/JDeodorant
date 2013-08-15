package gr.uom.java.ast.visualization;

import org.eclipse.draw2d.FreeformViewport;
import org.eclipse.draw2d.ScalableFreeformLayeredPane;
import org.eclipse.draw2d.ScalableLayeredPane;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

public class ZoomAction extends Action implements IWorkbenchAction {

	private static final String ID = "gr.uom.java.ast.visualization.ZoomAction";  
	private double scale;
	private ScalableFreeformLayeredPane freeformRoot= null;
	private ScalableLayeredPane root= null;
	private boolean isFreeform= true;


	public ZoomAction(ScalableLayeredPane root, double scale){  
		setId(ID);  

		this.scale = scale;
		this.root = root;
		isFreeform = false;
	}  
	public ZoomAction(ScalableFreeformLayeredPane root, double scale){  
		setId(ID);  

		this.scale = scale;
		this.freeformRoot = root;
	}  

	public void run() {  

		if(freeformRoot != null || root!=null){
			if(scale!= 0){
				if(isFreeform)
					this.freeformRoot.setScale(this.scale);
				else
					this.root.setScale(this.scale);
			}
			else
				scaleToFit();
		}

	}

	public void dispose() {

	}

	private void scaleToFit(){
		if(isFreeform){
			FreeformViewport viewport = (FreeformViewport) freeformRoot.getParent();
			Rectangle viewArea = viewport.getClientArea();

			this.freeformRoot.setScale(1);
			Rectangle rootArea = freeformRoot.getFreeformExtent().union(0,0);

			double wScale = ((double) viewArea.width)/rootArea.width;
			double hScale = ((double) viewArea.height)/rootArea.height;
			double newScale = Math.min(wScale, hScale);

			this.freeformRoot.setScale(newScale);
		}
	}

}
