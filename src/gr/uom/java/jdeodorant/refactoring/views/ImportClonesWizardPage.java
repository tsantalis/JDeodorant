package gr.uom.java.jdeodorant.refactoring.views;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

public class ImportClonesWizardPage extends WizardPage {

	protected ImportClonesWizardPage(IJavaProject javaProject) {
		super("Import clones for project " + javaProject.getElementName(),
				"Import clones for project " + javaProject.getElementName(), null);
	}

	public void createControl(Composite parent) {
		Composite result = new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);

		Group group = new Group(result, SWT.NONE);
		group.setText("Select clone detection tool");
		GridLayoutFactory.fillDefaults().applyTo(group);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(group);
		
		// Options for the radio buttons
		String[] names = new String[] {"CCFinder", "Deckard", "CloneDR", "NiCad"};
		final Label label = new Label(result, SWT.NONE);
		label.setText(names[0]);
		GridDataFactory.fillDefaults().applyTo(label);
		
		for (String name : names) {
			final Button button = new Button(group, SWT.RADIO);
			button.setText(name);
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					label.setText(button.getText());
				}
			});
		}
	}

}
