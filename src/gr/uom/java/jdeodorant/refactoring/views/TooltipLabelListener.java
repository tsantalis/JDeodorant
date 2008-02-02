package gr.uom.java.jdeodorant.refactoring.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;


final class TooltipLabelListener implements Listener {
	private boolean isCTRLDown(Event e) {
		return (e.stateMask & SWT.CTRL) != 0;
	}
	public void handleEvent(Event event) {
		Label label = (Label)event.widget;
		Shell shell = label.getShell();
		switch(event.type) {
			case SWT.MouseDown:
				Event e = new Event();
				e.item = (TableItem)label.getData("_TableItem_");
				Table table = ((TableItem)e.item).getParent();
				TableItem[] newSelection = null;
				if (isCTRLDown(event)) {
					TableItem[] sel = table.getSelection();
					for(int i = 0; i < sel.length; ++i) {
						if(e.item.equals(sel[i])) {
							newSelection = new TableItem[sel.length - 1];
							System.arraycopy(sel, 0, newSelection, 0, i);
							System.arraycopy(sel, i+1, newSelection, i, sel.length - i - 1);
							break;
						}
					}
					if(newSelection == null) {
						newSelection = new TableItem[sel.length + 1];
						System.arraycopy(sel, 0, newSelection, 0, sel.length);
						newSelection[sel.length] = (TableItem)e.item;
					}
				}
				else {
					newSelection = new TableItem[] { (TableItem) e.item };
				}
				table.setSelection(newSelection);
				table.notifyListeners(SWT.Selection, e);
				shell.dispose();
				table.setFocus();
				break;
			case SWT.MouseExit:
				shell.dispose();
				break;
		}
	}
}
