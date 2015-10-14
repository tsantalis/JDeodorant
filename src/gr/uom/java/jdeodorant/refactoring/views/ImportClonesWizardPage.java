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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
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
	}

	public void createControl(Composite parent) {
		Composite resultComposite = new Composite(parent, SWT.NONE);
		setControl(resultComposite);
		GridLayout layout = new GridLayout(2, false);
		resultComposite.setLayout(layout);

		Group cloneInputGroup = new Group(resultComposite, SWT.SHADOW_ETCHED_IN);
		cloneInputGroup.setText("Select clone detection tool");
		GridLayoutFactory.fillDefaults().applyTo(cloneInputGroup);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(cloneInputGroup);
		GridLayout grid = new GridLayout();
		grid.marginLeft = 2;
		grid.marginTop = 2;
		cloneInputGroup.setLayout(grid);
		
		Group fileInputGroup = new Group(resultComposite, SWT.SHADOW_ETCHED_IN);
		fileInputGroup.setText("Select input files");
		
		final Composite composite = new Composite(fileInputGroup, SWT.NONE);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		composite.setLayoutData(gridData);
		composite.setLayout(new GridLayout(2, false));
		
		for (final CloneDetectorType type : CloneDetectorType.values()) {
			final Button button = new Button(cloneInputGroup, SWT.RADIO);
			button.setText(type.toString());
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					if (((Button)event.getSource()).getSelection()) {
						if (selectedCloneDetectorType == null || selectedCloneDetectorType != type) {
							selectedCloneDetectorType = type;
							initializeBasicFileInput(composite);
							//secondary directory input - only for CCFinder
							initializeSecondaryFileInput(composite);
						}
					} else {
						for (Control control : composite.getChildren()) {
							control.dispose();
						}
					}
				}
			});
		}
		
		GridLayoutFactory.fillDefaults().applyTo(fileInputGroup);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fileInputGroup);
	}

	private void initializeBasicFileInput(Composite composite) {
		
		Label inputFileLabel = new Label(composite, SWT.NONE);
		String groupTitle = "Path to " + selectedCloneDetectorType.toString() + " results ";
		final Dialog dialog;;
		if (this.selectedCloneDetectorType == CloneDetectorType.CLONEDR) {
			dialog = new DirectoryDialog(ImportClonesWizardPage.this.getShell(), SWT.OPEN);
			groupTitle += "folder";
		} else {
			dialog = new FileDialog(ImportClonesWizardPage.this.getShell(), SWT.OPEN);
			FileDialog fileDialog = (FileDialog)dialog;
			String[] extensions = new String[]{};
			switch (selectedCloneDetectorType) {
			case CCFINDER:
				extensions = new String[]{"*.ccfxd"};
				break;
			case NICAD:
			case CONQAT:
				extensions = new String[]{"*.xml"};
				break;
			default:
				break;
			}
			fileDialog.setFilterExtensions(extensions);
			groupTitle += "file";
		}
		groupTitle += ":";
		
		inputFileLabel.setText(groupTitle);
		inputFileLabel.setFont(MyRefactoringWizard.INPUT_PAGE_FONT);
		GridData grForLabel = new GridData();
		grForLabel.horizontalSpan = 2;
		grForLabel.horizontalAlignment = GridData.FILL;
		inputFileLabel.setLayoutData(grForLabel);
		
		final Text inputFileText = new Text(composite, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = GridData.FILL;
		inputFileText.setLayoutData(gd);
		inputFileText.setEditable(false);
		
		Button inputFileButton = new Button(composite, SWT.PUSH);
		inputFileButton.setText("Browse");
		
		inputFileButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				dialog.setText("Browse to clone detection results file");
				String inputFile = null;
				if (dialog instanceof FileDialog)
					inputFile = ((FileDialog)dialog).open();
				else if (dialog instanceof DirectoryDialog)
					inputFile = ((DirectoryDialog)dialog).open();
				if(inputFile != null) {
					basicInputFile = inputFile;
					inputFileText.setText(basicInputFile);
				}
			}
		});
		composite.getParent().layout(true, true);
	}

	private void initializeSecondaryFileInput(final Composite composite) {

		if (this.selectedCloneDetectorType == CloneDetectorType.CCFINDER) {

			Label inputFileLabel = new Label(composite, SWT.NONE);
			inputFileLabel.setText("Path to .ccfxprepdir directory:");
			inputFileLabel.setFont(MyRefactoringWizard.INPUT_PAGE_FONT);
			GridData gridData = new GridData();
			gridData.horizontalSpan = 2;
			gridData.horizontalAlignment = GridData.FILL;
			inputFileLabel.setLayoutData(gridData);

			final Text inputFileText = new Text(composite, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.grabExcessHorizontalSpace = true;
			gd.horizontalAlignment = GridData.FILL;
			inputFileText.setLayoutData(gd);
			inputFileText.setEditable(false);

			Button inputFileButton = new Button(composite, SWT.PUSH);
			inputFileButton.setText("Browse");

			inputFileButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					DirectoryDialog directoryDialog = new DirectoryDialog(ImportClonesWizardPage.this.getShell(), SWT.OPEN);
					directoryDialog.setText("Browse to .ccfxprepdir directory");
					if (inputFileText.getText() != null)
						directoryDialog.setFilterPath(inputFileText.getText());
					String inputFile = directoryDialog.open();
					if(inputFile != null) {
						secondaryInputFile = inputFile;
						inputFileText.setText(secondaryInputFile);
					}
				}
			});
			
			composite.getParent().layout(true, true);

		}
		
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
