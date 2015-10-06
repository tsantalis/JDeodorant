package gr.uom.java.jdeodorant.refactoring.views;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.MessageBox;

import ca.concordia.jdeodorant.clone.parsers.CloneDetectorOutputParser;
import ca.concordia.jdeodorant.clone.parsers.CloneDetectorOutputParserFactory;
import ca.concordia.jdeodorant.clone.parsers.CloneDetectorOutputParserProgressObserver;
import ca.concordia.jdeodorant.clone.parsers.CloneDetectorType;
import ca.concordia.jdeodorant.clone.parsers.CloneGroupList;

public class ImportClonesWizard extends Wizard {
	private IJavaProject javaProject;
	private ImportClonesWizardPage importClonesWizardPage;
	
	public ImportClonesWizard(IJavaProject javaProject) {
		this.javaProject = javaProject;
		this.setNeedsProgressMonitor(true);
		this.setHelpAvailable(false);
		this.importClonesWizardPage = new ImportClonesWizardPage(javaProject);
	}

	@Override
	public void addPages() {
		addPage(this.importClonesWizardPage);
	}

	@Override
	public boolean performFinish() {
		CloneDetectorType selectedCloneDetectorType = importClonesWizardPage.getSelectedCloneDetectorType();
		String mainInputFile = importClonesWizardPage.getBasicInputFile();
		String secondaryInputFile = importClonesWizardPage.getSecondaryInputFile();
		CloneDetectorOutputParser parser = CloneDetectorOutputParserFactory.getCloneToolParser(selectedCloneDetectorType, javaProject, mainInputFile, secondaryInputFile);
		parser.addParserProgressObserver(new CloneDetectorOutputParserProgressObserver() {
			
			public void notify(double percentage) {
				System.out.println(percentage);
			}
		});
		CloneGroupList cloneGroupList = parser.readInputFile();
		System.out.println();
		return false;
	}

}
