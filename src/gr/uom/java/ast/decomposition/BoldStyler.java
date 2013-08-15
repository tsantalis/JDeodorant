package gr.uom.java.ast.decomposition;

import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.TextStyle;

public class BoldStyler extends Styler {

	public void applyStyles(TextStyle textStyle) {
		textStyle.font = new Font(null, new FontData("consolas", 10, SWT.BOLD));
	}
}
