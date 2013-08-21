package gr.uom.java.ast.visualization;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.swt.widgets.Shell;

public class GodClassInformationControlCreator implements
		ICustomInformationControlCreator {

	public IInformationControl createInformationControl(Shell parent) {
		ToolBarManager manager = new ToolBarManager();
		return new GodClassInformationControl(parent, manager);
	}

	public boolean isSupported(Object info) {
		return PMClassFigure.class.isInstance(info);
	}

}
