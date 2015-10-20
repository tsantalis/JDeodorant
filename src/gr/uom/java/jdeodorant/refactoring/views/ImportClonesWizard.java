package gr.uom.java.jdeodorant.refactoring.views;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

import ca.concordia.jdeodorant.clone.parsers.CloneDetectorOutputParser;
import ca.concordia.jdeodorant.clone.parsers.CloneDetectorOutputParserFactory;
import ca.concordia.jdeodorant.clone.parsers.CloneDetectorOutputParserProgressObserver;
import ca.concordia.jdeodorant.clone.parsers.CloneDetectorType;
import ca.concordia.jdeodorant.clone.parsers.CloneGroupList;
import ca.concordia.jdeodorant.clone.parsers.InvalidInputFileException;
import ca.concordia.jdeodorant.clone.parsers.ResourceInfo.ICompilationUnitNotFoundException;

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
		importClonesWizardPage.setErrorMessage(null);
		CloneDetectorType selectedCloneDetectorType = importClonesWizardPage.getSelectedCloneDetectorType();
		String mainInputFile = importClonesWizardPage.getBasicInputFile();
		String secondaryInputFile = importClonesWizardPage.getSecondaryInputFile();
		try {
			final CloneDetectorOutputParser parser = CloneDetectorOutputParserFactory.getCloneToolParser(selectedCloneDetectorType, javaProject, mainInputFile, secondaryInputFile);
			final int cloneGroupCount = parser.getCloneGroupCount();
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
								int percentage = (int)(cloneGroupIndex / (double)cloneGroupCount * 100);
								monitor.subTask(percentage + "%");
								monitor.worked(1);
							}
						});

						try {
							cloneGroupList = parser.readInputFile();
						} catch (InvalidInputFileException e) {
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									importClonesWizardPage.setErrorMessage("An error has occured during parsing. The input file(s) may be corrupt.");
								}
							});
						}

						monitor.done();

					}
				});
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			List<Throwable> warningExceptions = parser.getWarningExceptions();
			if (warningExceptions.size() > 0) {
				int iCompilationUnitNotFoundExceptions = 0;
				for (Throwable throwable : warningExceptions) {
					if (throwable.getCause() instanceof ICompilationUnitNotFoundException) {
						iCompilationUnitNotFoundExceptions++;
					}
				}
				int style = SWT.ICON_ERROR | SWT.OK ;
			    MessageBox messageBox = new MessageBox(getShell(), style);
			    String message = String.format("%s warning%s happened during parsing.", 
			    		warningExceptions.size(), 
			    		warningExceptions.size() > 1 ? "s" : "");
			    if (iCompilationUnitNotFoundExceptions > 0) {
			    	message += "\n";
			    	message += String.format("The source code of %s clone instance%s was not found.", 
			    			iCompilationUnitNotFoundExceptions,
			    			iCompilationUnitNotFoundExceptions > 1 ? "s" : "");
			    }
			    messageBox.setMessage(message);
			    messageBox.open();
				
				if (cloneGroupList != null && cloneGroupList.getCloneGroupsCount() > 0)
					return true;
				else
					return false;
			} else if (parser.isOperationCanceled() || cloneGroupList == null || cloneGroupList.getCloneGroupsCount() == 0) {
				return false;
			}

			return true;

		} catch(InvalidInputFileException invaludInputFileException) {
			importClonesWizardPage.setErrorMessage("Invalid input file");
			return false;
		}
	}

}
