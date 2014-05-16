package gr.uom.java.jdeodorant.refactoring.views;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import gr.uom.java.jdeodorant.refactoring.manipulators.MoveMethodRefactoring;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class MoveMethodInputPage extends UserInputWizardPage {

	private MoveMethodRefactoring refactoring;
	private Map<Text, String> textMap;
	private Map<Text, String> defaultNamingMap;
	
	public MoveMethodInputPage(MoveMethodRefactoring refactoring) {
		super("Moved Method Name");
		this.refactoring = refactoring;
		this.textMap = new LinkedHashMap<Text, String>();
		this.defaultNamingMap = new LinkedHashMap<Text, String>();
	}

	public void createControl(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);
		
		Label movedMethodNameLabel = new Label(result, SWT.NONE);
		movedMethodNameLabel.setText("Moved Method Name");
		movedMethodNameLabel.setFont(MyRefactoringWizard.INPUT_PAGE_FONT);
		
		Text movedMethodNameField = new Text(result, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		movedMethodNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		movedMethodNameField.setText(refactoring.getMovedMethodName());
		
		textMap.put(movedMethodNameField, refactoring.getMovedMethodName());
		defaultNamingMap.put(movedMethodNameField, refactoring.getMovedMethodName());
		
		final Button restoreButton = new Button(result, SWT.PUSH);
		restoreButton.setText("Restore Defaults");
		
		final Button delegateButton = new Button(result, SWT.CHECK);
		delegateButton.setText("Keep original method as delegate to the moved method");
		
		for(Text field : textMap.keySet()) {
			field.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					handleInputChanged();
				}
			});
		}
		
		restoreButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				for(Text field : defaultNamingMap.keySet()) {
					field.setText(defaultNamingMap.get(field));
				}
				delegateButton.setSelection(false);
				refactoring.setLeaveDelegate(false);
			}
		});
		
		delegateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				Button source = (Button)event.getSource();
				if(source.getSelection())
					refactoring.setLeaveDelegate(true);
				else
					refactoring.setLeaveDelegate(false);
			}
		});
		
		handleInputChanged();
	}

	private void handleInputChanged() {
		String methodNamePattern = "[a-zA-Z\\$_][a-zA-Z0-9\\$_]*";
		for(Text text : textMap.keySet()) {
			if(!Pattern.matches(methodNamePattern, text.getText())) {
				setPageComplete(false);
				String message = "Method name \"" + text.getText() + "\" is not valid";
				setMessage(message, ERROR);
				return;
			}
			else {
				refactoring.setMovedMethodName(text.getText());
			}
		}
		setPageComplete(true);
		setMessage("", NONE);
	}
}
