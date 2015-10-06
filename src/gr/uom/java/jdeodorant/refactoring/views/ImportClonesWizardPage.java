package gr.uom.java.jdeodorant.refactoring.views;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ca.concordia.jdeodorant.clone.parsers.CloneDetectorType;

public class ImportClonesWizardPage extends WizardPage {

	private CloneDetectorType selectedCloneDetectorType;
	private String basicInputFile;
	private String secondaryInputFile;
	
	public ImportClonesWizardPage(IJavaProject javaProject) {
		super("Import clones for project " + javaProject.getElementName(),
				"Import clones for project " + javaProject.getElementName(), null);
		this.selectedCloneDetectorType = CloneDetectorType.values()[0];
	}

	public void createControl(Composite parent) {
		Composite resultComposite = new Composite(parent, SWT.NONE);
		setControl(resultComposite);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		resultComposite.setLayout(layout);

		Group cloneInputGroup = new Group(resultComposite, SWT.SHADOW_ETCHED_IN);
		cloneInputGroup.setText("Select clone detection tool");
		GridLayoutFactory.fillDefaults().applyTo(cloneInputGroup);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(cloneInputGroup);
		
		for (final CloneDetectorType type : CloneDetectorType.values()) {
			final Button button = new Button(cloneInputGroup, SWT.RADIO);
			button.setText(type.toString());
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					selectedCloneDetectorType = type;
				}
			});
		}
		
		Group fileInputGroup = new Group(resultComposite, SWT.SHADOW_ETCHED_IN);
		fileInputGroup.setLayout(new GridLayout(1, false));
		fileInputGroup.setText("Select input files");
		
		initializeBasicFileInput(fileInputGroup);
		
		//secondary directory input - only for CCFinder
		initializeSecondaryFileInput(fileInputGroup);
		
		GridLayoutFactory.fillDefaults().applyTo(fileInputGroup);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fileInputGroup);
	}

	private void initializeBasicFileInput(final Group fileInputGroup) {
		Label inputFileLabel = new Label(fileInputGroup, SWT.NONE);
		inputFileLabel.setText("Path to " + selectedCloneDetectorType.toString() + " results file:");
		inputFileLabel.setFont(MyRefactoringWizard.INPUT_PAGE_FONT);
		final Text inputFileText = new Text(fileInputGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = GridData.FILL;
		inputFileText.setLayoutData(gd);
		inputFileText.setEditable(false);
		Button inputFileButton = new Button(fileInputGroup, SWT.PUSH);
		inputFileButton.setText("Browse");
		
		inputFileButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				FileDialog fileDialog = new FileDialog(ImportClonesWizardPage.this.getShell(), SWT.OPEN);
				fileDialog.setText("Browse to clone detection results file");
				if(selectedCloneDetectorType.equals(CloneDetectorType.CCFINDER)) {
					fileDialog.setFilterExtensions(new String[]{"*.ccfxd"});
				}
				else if(selectedCloneDetectorType.equals(CloneDetectorType.NICAD)) {
					fileDialog.setFilterExtensions(new String[]{"*.xml"});
				}
				String inputFile = fileDialog.open();
				if(inputFile != null) {
					basicInputFile = inputFile;
					inputFileText.setText(basicInputFile);
				}
			}
		});
	}

	private void initializeSecondaryFileInput(final Group fileInputGroup) {
		Label inputFileLabel = new Label(fileInputGroup, SWT.NONE);
		inputFileLabel.setText("Path to .ccfxprepdir directory:");
		inputFileLabel.setFont(MyRefactoringWizard.INPUT_PAGE_FONT);
		final Text inputFileText = new Text(fileInputGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = GridData.FILL;
		inputFileText.setLayoutData(gd);
		inputFileText.setEditable(false);
		Button inputFileButton = new Button(fileInputGroup, SWT.PUSH);
		inputFileButton.setText("Browse");
		
		inputFileButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				DirectoryDialog directoryDialog = new DirectoryDialog(ImportClonesWizardPage.this.getShell(), SWT.OPEN);
				directoryDialog.setText("Browse to .ccfxprepdir directory");
				String inputFile = directoryDialog.open();
				if(inputFile != null) {
					secondaryInputFile = inputFile;
					inputFileText.setText(secondaryInputFile);
				}
			}
		});
	}

	public CloneDetectorType getSelectedCloneDetectorType() {
		return selectedCloneDetectorType;
	}

	public String getBasicInputFile() {
		return basicInputFile;
	}

	public String getSecondaryInputFile() {
		return secondaryInputFile;
	}

}
