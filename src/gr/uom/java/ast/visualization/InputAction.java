package gr.uom.java.ast.visualization;
import gr.uom.java.jdeodorant.refactoring.views.ZoomValueValidator;

import org.eclipse.draw2d.ScalableFreeformLayeredPane;
import org.eclipse.jface.action.Action;  
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;  
import org.eclipse.ui.PlatformUI;  
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;  

public class InputAction extends Action implements IWorkbenchAction{  

	private static final String ID = "gr.uom.java.ast.visualiztion.InputAction";  
	private ScalableFreeformLayeredPane root;

	public InputAction(ScalableFreeformLayeredPane root){  
		setId(ID); 
		this.root = root;
	}  

	public void run() {  

		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();  
		String dialogBoxTitle = "Custom Zoom";  
		String message = "Enter Zoom Value (in percent): ";  
		String initialValue = "";
		
		
		InputDialog dialog = new InputDialog(shell, dialogBoxTitle, message, initialValue, new ZoomValueValidator() );
		if(dialog.open() == Window.OK){
			String value = dialog.getValue();
			if(value != null && root != null){
				this.root.setScale(Double.parseDouble(value)/100);
			}
		}
	}  

	public void dispose() {}  

}  
