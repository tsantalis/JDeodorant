package gr.uom.java.jdeodorant.refactoring.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;


public class MultilineButton extends Button {

    public MultilineButton(Composite parent, int style) {
        super(parent, style);
    }

    @Override
    protected void checkSubclass() {
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        final Point size = super.computeSize(wHint, hHint, changed);
        final GC gc = new GC(this);

        final String multiLineText = this.getText();
        final Point multiLineTextSize = gc.textExtent(multiLineText, SWT.DRAW_DELIMITER);

        final String flatText = multiLineText.replace('\n', ' ');
        final Point flatTextSize = gc.textExtent(flatText);

        gc.dispose();

        size.x -= flatTextSize.x - multiLineTextSize.x;
        size.y += multiLineTextSize.y - flatTextSize.y;

        return size;
    }
}
