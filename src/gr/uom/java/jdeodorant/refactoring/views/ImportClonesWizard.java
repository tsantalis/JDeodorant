package gr.uom.java.jdeodorant.refactoring.views;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;

import ca.concordia.jdeodorant.clone.parsers.CloneDetectorOutputParseException;
import ca.concordia.jdeodorant.clone.parsers.CloneDetectorOutputParser;
import ca.concordia.jdeodorant.clone.parsers.CloneDetectorOutputParserFactory;
import ca.concordia.jdeodorant.clone.parsers.CloneDetectorOutputParserProgressObserver;
import ca.concordia.jdeodorant.clone.parsers.CloneDetectorType;
import ca.concordia.jdeodorant.clone.parsers.CloneGroupList;

public class ImportClonesWizard extends Wizard {
	private IJavaProject javaProject;
	private ImportClonesWizardPage importClonesWizardPage;
	private CloneGroupList cloneGroupList;
	
	public ImportClonesWizard(IJavaProject javaProject) {
		this.javaProject = javaProject;
		this.setNeedsProgressMonitor(true);
		this.setHelpAvailable(false);
		this.importClonesWizardPage = new ImportClonesWizardPage(javaProject);
	}
	
	public CloneGroupList getCloneGroupList() {
		return cloneGroupList;
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
		final CloneDetectorOutputParser parser = CloneDetectorOutputParserFactory.getCloneToolParser(selectedCloneDetectorType, javaProject, mainInputFile, secondaryInputFile);
		final int cloneGroupCount = parser.getCloneGroupCount();
		boolean errorHappened = false;
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				
				public void run(final IProgressMonitor monitor)
						throws InvocationTargetException,
						InterruptedException {

					monitor.beginTask("Parsing ", cloneGroupCount);
					parser.addParserProgressObserver(new CloneDetectorOutputParserProgressObserver() {
						public void notify(int cloneGroupIndex) {
							if (monitor.isCanceled()) {
								parser.cancelOperation();
							}
							//monitor.set
							int percentage = (int)(cloneGroupIndex / (double)cloneGroupCount * 100);
							monitor.subTask(percentage + "%");
							monitor.worked(1);
						}
					});
					
					try {
						cloneGroupList = parser.readInputFile();
					} catch (CloneDetectorOutputParseException e) {
						int style = SWT.ICON_ERROR | SWT.OK ;
					    MessageBox messageBox = new MessageBox(getShell(), style);
					    messageBox.setMessage("An error has occured during parsing. The input file(s) may be corrupt.");
					    messageBox.open();
						throw new RuntimeException(e);
					}
					
					monitor.done();

				}
			});
		} catch (RuntimeException e) {
			e.printStackTrace();
			errorHappened = true;
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (parser.getWarningExceptions().size() > 0) {
			int style = SWT.ICON_ERROR | SWT.OK ;
		    MessageBox messageBox = new MessageBox(getShell(), style);
		    messageBox.setMessage(String.format("%s warning%s happened during parsing.", 
		    		parser.getWarningExceptions().size(), 
		    		parser.getWarningExceptions().size() > 1 ? "s" : ""));
		    messageBox.open();
		    System.out.println(parser.getWarningExceptions());
		    if (cloneGroupList.getCloneGroupsCount() > 0)
		    	return true;
		    else
		    	return false;
		} else if (errorHappened || parser.isOperationCanceled()) {
			return false;
		}
		
		return true;
	}

}
