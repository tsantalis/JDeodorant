package gr.uom.java.ast.decomposition.cfg.mapping.precondition;

import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.TextStyle;

public class NormalStyler extends Styler {

	private static final Font TAHOMA_NORMAL_FONT = new Font(null, new FontData("Tahoma", 8, SWT.NORMAL));

	@Override
	public void applyStyles(TextStyle textStyle) {
		textStyle.font = TAHOMA_NORMAL_FONT;
	}

}
