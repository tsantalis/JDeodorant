package gr.uom.java.jdeodorant.refactoring.views;

import org.eclipse.ui.IViewPart;

import gr.uom.java.ast.visualization.VisualizationData;

public class CodeSmellVisualizationDataSingleton {
	private static IViewPart viewPart;
	private static VisualizationData data;

	public static VisualizationData getData() {
		return data;
	}

	public static void setData(VisualizationData data) {
		CodeSmellVisualizationDataSingleton.data = data;
	}

	public static IViewPart getViewPart() {
		return viewPart;
	}

	public static void setViewPart(IViewPart viewPart) {
		CodeSmellVisualizationDataSingleton.viewPart = viewPart;
	}
}
