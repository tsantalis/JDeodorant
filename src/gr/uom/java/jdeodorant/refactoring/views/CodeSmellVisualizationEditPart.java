package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.visualization.FeatureEnvyDiagram;
import gr.uom.java.ast.visualization.FeatureEnvyVisualizationData;
import gr.uom.java.ast.visualization.GodClassDiagram2;
import gr.uom.java.ast.visualization.GodClassVisualizationData;
import gr.uom.java.ast.visualization.VisualizationData;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

public class CodeSmellVisualizationEditPart extends AbstractGraphicalEditPart {
	private VisualizationData visualizationData;
	
	public CodeSmellVisualizationEditPart(VisualizationData visualizationData) {
		this.visualizationData = visualizationData;
	}

	@Override
	protected IFigure createFigure() {
		if(visualizationData instanceof GodClassVisualizationData) {
			GodClassDiagram2 diagram = new GodClassDiagram2((GodClassVisualizationData)visualizationData);
			return diagram.getRoot();
		}
		if(visualizationData instanceof FeatureEnvyVisualizationData) {
			FeatureEnvyDiagram diagram = new FeatureEnvyDiagram((FeatureEnvyVisualizationData)visualizationData);
			return diagram.getRoot();
		}
		return null;
	}

	@Override
	protected void createEditPolicies() {
		
	}

}
