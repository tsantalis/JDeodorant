package gr.uom.java.jdeodorant.refactoring.views;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class ImportClonesWizardDialog extends WizardDialog {
	private ProgressMonitorPart part;

	public ImportClonesWizardDialog(Shell parentShell, IWizard wizard) {
		super(parentShell, wizard);
	}

	@Override
	protected ProgressMonitorPart createProgressMonitorPart(Composite composite, GridLayout pmlayout) {
		part = super.createProgressMonitorPart(composite, pmlayout);
		return part;
	}

	private void updateWizardProgress() {
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				getProgressMonitor().beginTask("Completion: ", getWizard().getPageCount());
				//getProgressMonitor().worked(pageCount);
			}
		});
	}

	@Override
	protected Control createContents(Composite parent) {
		Control result = super.createContents(parent);
		part.setVisible(true);
		updateWizardProgress();
		return result;
	}
}
