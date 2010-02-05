package gr.uom.java.jdeodorant.refactoring.views;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import gr.uom.java.jdeodorant.refactoring.manipulators.ReplaceTypeCodeWithStateStrategy;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.SimpleName;
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

public class ReplaceTypeCodeWithStateStrategyInputPage extends UserInputWizardPage {

	private ReplaceTypeCodeWithStateStrategy refactoring;
	private IPackageFragment parentPackage;
	private List<String> parentPackageClassNames;
	private Map<Text, SimpleName> textMap;
	private Map<Text, String> defaultNamingMap;
	
	public ReplaceTypeCodeWithStateStrategyInputPage(ReplaceTypeCodeWithStateStrategy refactoring) {
		super("State/Strategy Type Names");
		this.refactoring = refactoring;
		ICompilationUnit sourceCompilationUnit = (ICompilationUnit)refactoring.getSourceCompilationUnit().getJavaElement();
		this.parentPackage = (IPackageFragment)sourceCompilationUnit.getParent();
		this.parentPackageClassNames = new ArrayList<String>();
		try {
			for(ICompilationUnit compilationUnit : parentPackage.getCompilationUnits()) {
				String className = compilationUnit.getElementName();
				parentPackageClassNames.add(className.substring(0, className.indexOf(".java")));
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		this.textMap = new LinkedHashMap<Text, SimpleName>();
		this.defaultNamingMap = new LinkedHashMap<Text, String>();
	}

	public void createControl(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);
		
		Label namedConstantTitle = new Label(result, SWT.NONE);
		namedConstantTitle.setText("Named Constants:");
		Label typeNameTitle = new Label(result, SWT.NONE);
		typeNameTitle.setText("State/Strategy Type Names:");
		
		for(Map.Entry<SimpleName, String> entry : refactoring.getStaticFieldMapEntrySet()) {
			Label label= new Label(result, SWT.NONE);
			label.setText(entry.getKey().getIdentifier());
			
			Text field= new Text(result, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
			field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			field.setText(entry.getValue());
			
			textMap.put(field, entry.getKey());
			defaultNamingMap.put(field, entry.getValue());
		}
		
		for(Map.Entry<SimpleName, String> entry : refactoring.getAdditionalStaticFieldMapEntrySet()) {
			Label label= new Label(result, SWT.NONE);
			label.setText(entry.getKey().getIdentifier());
			
			Text field= new Text(result, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
			field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			field.setText(entry.getValue());
			
			textMap.put(field, entry.getKey());
			defaultNamingMap.put(field, entry.getValue());
		}
		
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
		String classNamePattern = "[a-zA-Z_][a-zA-Z0-9_]*";
		for(Text text : textMap.keySet()) {
			if(!Pattern.matches(classNamePattern, text.getText())) {
				setPageComplete(false);
				String message = "Type name \"" + text.getText() + "\" is not valid";
				setMessage(message, ERROR);
				return;
			}
			else if(parentPackageClassNames.contains(text.getText())) {
				setPageComplete(false);
				String message = "A Type named \"" + text.getText() + "\" already exists in package " + parentPackage.getElementName();
				setMessage(message, ERROR);
				return;
			}
			else {
				SimpleName namedConstant = textMap.get(text);
				refactoring.setTypeNameForNamedConstant(namedConstant, text.getText());
			}
		}
		setPageComplete(true);
		setMessage("", NONE);
	}
}
