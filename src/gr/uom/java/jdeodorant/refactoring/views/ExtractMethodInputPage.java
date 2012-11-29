package gr.uom.java.jdeodorant.refactoring.views;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractMethodRefactoring;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ExtractMethodInputPage extends UserInputWizardPage {
	
	private ExtractMethodRefactoring refactoring;
	private Map<Text, String> textMap;
	private Map<Text, String> defaultNamingMap;

	public ExtractMethodInputPage(ExtractMethodRefactoring refactoring) {
		super("Extracted Method Name");
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

		FontData labelFontData = new FontData("Segoe UI", 9, SWT.NORMAL);
		Font font = new Font(parent.getDisplay(), labelFontData);

		Label extractedClassNameLabel = new Label(result, SWT.NONE);
		extractedClassNameLabel.setText("Extracted Method Name");
		extractedClassNameLabel.setFont(font);

		Text extractedClassNameField = new Text(result, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		extractedClassNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		extractedClassNameField.setText(refactoring.getExtractedMethodName());

		textMap.put(extractedClassNameField, refactoring.getExtractedMethodName());
		defaultNamingMap.put(extractedClassNameField, refactoring.getExtractedMethodName());

		final Button restoreButton = new Button(result, SWT.PUSH);
		restoreButton.setText("Restore Defaults");

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
				refactoring.setExtractedMethodName(text.getText());
			}
		}
		setPageComplete(true);
		setMessage("", NONE);
	}
}
