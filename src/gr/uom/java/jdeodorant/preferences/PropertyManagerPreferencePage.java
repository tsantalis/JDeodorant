package gr.uom.java.jdeodorant.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import gr.uom.java.jdeodorant.refactoring.Activator;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class PropertyManagerPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	private IntegerFieldEditor minimumSliceSizeFieldEditor;
	private IntegerFieldEditor maximumSliceSizeFieldEditor;
	private IntegerFieldEditor maximumDuplicationFieldEditor;
	private StringFieldEditor maximumRatioOfDuplicatedToExtractedFieldEditor;
	private BooleanFieldEditor enableAliasAnalysisFieldEditor;
	
	public PropertyManagerPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Slice Extraction Preferences");
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		minimumSliceSizeFieldEditor = new IntegerFieldEditor(
				PreferenceConstants.P_MINIMUM_SLICE_SIZE,
				"&Minimum number of slice statements:", getFieldEditorParent());
		minimumSliceSizeFieldEditor.setEmptyStringAllowed(false);
		addField(minimumSliceSizeFieldEditor);
		
		maximumSliceSizeFieldEditor = new IntegerFieldEditor(
				PreferenceConstants.P_MAXIMUM_SLICE_SIZE,
				"&Maximum number of slice statements (method size - n):", getFieldEditorParent());
		maximumSliceSizeFieldEditor.setEmptyStringAllowed(false);
		addField(maximumSliceSizeFieldEditor);
		
		maximumDuplicationFieldEditor = new IntegerFieldEditor(
				PreferenceConstants.P_MAXIMUM_DUPLICATION,
				"&Maximum number of duplicated statements:", getFieldEditorParent());
		maximumDuplicationFieldEditor.setEmptyStringAllowed(false);
		addField(maximumDuplicationFieldEditor);
		
		maximumRatioOfDuplicatedToExtractedFieldEditor = new StringFieldEditor(
				PreferenceConstants.P_MAXIMUM_RATIO_OF_DUPLICATED_TO_EXTRACTED,
				"&Maximum ratio of duplicated to extracted statements:", getFieldEditorParent());
		maximumRatioOfDuplicatedToExtractedFieldEditor.setEmptyStringAllowed(false);
		addField(maximumRatioOfDuplicatedToExtractedFieldEditor);
		
		enableAliasAnalysisFieldEditor = new BooleanFieldEditor(
				PreferenceConstants.P_ENABLE_ALIAS_ANALYSIS,
				"&Enable Alias Analysis:", getFieldEditorParent());
		addField(enableAliasAnalysisFieldEditor);
	}

	protected void checkState() {
		super.checkState();
		String ratio = maximumRatioOfDuplicatedToExtractedFieldEditor.getStringValue();
		try {
			double r = Double.parseDouble(ratio);
			if(r >= 0.0 && r <= 1.0) {
				setErrorMessage(null);
				setValid(true);
			}
			else {
				setErrorMessage("Value must be a Double within range [0, 1]");
				setValid(false);
			}
		}
		catch(NumberFormatException e) {
			setErrorMessage("Value must be a Double");
			setValid(false);
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		if(event.getProperty().equals(FieldEditor.VALUE) &&
				event.getSource() == maximumRatioOfDuplicatedToExtractedFieldEditor) {
			checkState();
		}        
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
}