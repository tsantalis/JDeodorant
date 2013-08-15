package gr.uom.java.ast.visualization;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class SearchDialog extends Dialog {

	private Text inputText;
	private String className;




	public SearchDialog(Shell parentShell) {
		super(parentShell);

	}

	@Override
	public void create() {
		super.create();
		this.getShell().setText("Search");


	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout(1, false);
		container.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		container.setLayout(layout);
		Label enterClassNameLabel = new Label(container, SWT.NONE);
		enterClassNameLabel.setText("Enter Class Name: ");

		GridData dataClassName = new GridData();
		dataClassName.grabExcessHorizontalSpace = true;
		dataClassName.horizontalAlignment = GridData.FILL;

		inputText = new Text(container, SWT.BORDER);
		inputText.setLayoutData(dataClassName);

		ArrayList<PMClassFigure> classes = (ArrayList<PMClassFigure>) PackageMapDiagram.allClassFigures;
		String[] classNames= new String[classes.size()];
		for(int i=0; i< classes.size(); i++){
			PMClassFigure figure = classes.get(i);
			classNames[i] = figure.getName(); 

		}


		//new AutoCompleteField(inputText, new TextContentAdapter(),classNames);
		MyContentProposalProvider provider = new  MyContentProposalProvider(classNames);
		ContentProposalAdapter adapter = new ContentProposalAdapter(inputText, new TextContentAdapter(), provider, null, null);
		adapter.setPropagateKeys(true);
		adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);


		return area;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	// We need to save the values of the Text fields into Strings because the UI
	// gets disposed
	// and the Text fields are not accessible any more.
	private void saveInput() {
		className = inputText.getText();


	}

	@Override
	protected Point getInitialSize() {
		return new Point(400, 150);
	}



	@Override
	protected void okPressed() {
		saveInput();
		super.okPressed();
	}

	public String getClassName() {
		return className;
	}


} 