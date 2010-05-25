package gr.uom.java.jdeodorant.refactoring.views;

import org.eclipse.jface.text.source.Annotation;

public class SliceAnnotation extends Annotation {
	public static final String EXTRACTION = "gr.uom.java.jdeodorant.extractionAnnotation";
	public static final String DUPLICATION = "gr.uom.java.jdeodorant.duplicationAnnotation";

	public SliceAnnotation(String type, String text) {
		super(type, false, text);
	}
}
