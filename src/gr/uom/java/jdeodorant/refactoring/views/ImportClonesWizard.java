package gr.uom.java.jdeodorant.refactoring.views;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.wizard.Wizard;

public class ImportClonesWizard extends Wizard {
	private IJavaProject javaProject;
	
	public ImportClonesWizard(IJavaProject javaProject) {
		this.javaProject = javaProject;
		this.setNeedsProgressMonitor(true);
		this.setHelpAvailable(false);
	}

	@Override
	public void addPages() {
		addPage(new ImportClonesWizardPage(javaProject));
	}

	@Override
	public boolean performFinish() {
		return false;
	}

}
