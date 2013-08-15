package gr.uom.java.ast.visualization;

import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

public class DecorationConstants {

	public static final Font normalFont = new Font(null, "Arial", 10, SWT.BOLD);
	public static final Font highlightFont = new Font(null, "Arial", 14 , SWT.BOLD);
	public static final Color entityColor = new Color(null,255,255,240);
	public static final Image FIELD = JavaUI.getSharedImages().getImage(ISharedImages.IMG_FIELD_PRIVATE);
	public static final Image METHOD = JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PUBLIC);
	public static final Image PACKAGE = JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);
	public static final Color methodToMethodColor = new Color(null,60,179,113);
	public static final int NO_OCCURENCES = -1;
	public static final Color classColor = new Color(null,255,255,206);
	public static final Image CLASS = JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CLASS);
	public static final Font classFont = new Font(null, "Arial", 12, SWT.BOLD);

}
