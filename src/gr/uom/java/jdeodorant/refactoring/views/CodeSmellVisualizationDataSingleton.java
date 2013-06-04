package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.visualization.VisualizationData;

public class CodeSmellVisualizationDataSingleton {
	private static VisualizationData data;

	public static VisualizationData getData() {
		return data;
	}

	public static void setData(VisualizationData data) {
		CodeSmellVisualizationDataSingleton.data = data;
	}
}
