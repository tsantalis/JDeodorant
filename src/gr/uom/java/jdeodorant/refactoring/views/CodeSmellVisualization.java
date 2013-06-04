package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.visualization.VisualizationData;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class CodeSmellVisualization extends ViewPart {
	public static final String ID = "gr.uom.java.jdeodorant.views.CodeSmellVisualization";
	private ScrollingGraphicalViewer graphicalViewer; 
	private FigureCanvas figureCanvas; 

	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		graphicalViewer = new ScrollingGraphicalViewer();
		ScalableRootEditPart rootEditPart = new ScalableRootEditPart();
		graphicalViewer.setRootEditPart(rootEditPart);
		graphicalViewer.setEditDomain(new EditDomain());
		
		figureCanvas = (FigureCanvas) graphicalViewer.createControl(parent);
		VisualizationData data = CodeSmellVisualizationDataSingleton.getData();
		if(data != null) {
			CodeSmellVisualizationEditPart editPart = new CodeSmellVisualizationEditPart(data);
			graphicalViewer.setContents(editPart);
		}
	}

	public void setFocus() {
		
	}

}
