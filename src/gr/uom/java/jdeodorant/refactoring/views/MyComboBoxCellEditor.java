package gr.uom.java.jdeodorant.refactoring.views;

import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class MyComboBoxCellEditor extends ComboBoxCellEditor {

	public MyComboBoxCellEditor(Composite parent, String[] items, int style) {
		super(parent, items, style);
	}

	protected Control createControl(Composite parent) {
		CCombo control = (CCombo)super.createControl(parent);
		control.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				focusLost();
			}
		});
		return control;
	}
}
