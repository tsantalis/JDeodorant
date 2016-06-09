package gr.uom.java.jdeodorant.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
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
	private IntegerFieldEditor minimumMethodSizeFieldEditor;
	private IntegerFieldEditor maximumCallGraphAnalysisDepthFieldEditor;
	private BooleanFieldEditor enableCallGraphAnalysisFieldEditor;
	private BooleanFieldEditor enableAliasAnalysisFieldEditor;
	private IntegerFieldEditor projectCompilationUnitCacheSizeFieldEditor;
	private IntegerFieldEditor libraryCompilationUnitCacheSizeFieldEditor;
	private BooleanFieldEditor enableUsageReportingFieldEditor;
	private BooleanFieldEditor enableSourceCodeReportingFieldEditor;
	
	public PropertyManagerPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		Composite composite = new Composite(getFieldEditorParent(), SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		
		Group sliceExtractionPreferenceGroup = new Group(composite, SWT.SHADOW_ETCHED_IN);
		sliceExtractionPreferenceGroup.setLayout(new GridLayout(1, false));
		sliceExtractionPreferenceGroup.setText("Slice Extraction Preferences");
		
		minimumSliceSizeFieldEditor = new IntegerFieldEditor(
				PreferenceConstants.P_MINIMUM_SLICE_SIZE,
				"&Minimum number of slice statements:", sliceExtractionPreferenceGroup);
		minimumSliceSizeFieldEditor.setEmptyStringAllowed(false);
		addField(minimumSliceSizeFieldEditor);
		
		maximumSliceSizeFieldEditor = new IntegerFieldEditor(
				PreferenceConstants.P_MAXIMUM_SLICE_SIZE,
				"&Maximum number of slice statements (method size - n):", sliceExtractionPreferenceGroup);
		maximumSliceSizeFieldEditor.setEmptyStringAllowed(false);
		addField(maximumSliceSizeFieldEditor);
		
		maximumDuplicationFieldEditor = new IntegerFieldEditor(
				PreferenceConstants.P_MAXIMUM_DUPLICATION,
				"&Maximum number of duplicated statements:", sliceExtractionPreferenceGroup);
		maximumDuplicationFieldEditor.setEmptyStringAllowed(false);
		addField(maximumDuplicationFieldEditor);
		
		maximumRatioOfDuplicatedToExtractedFieldEditor = new StringFieldEditor(
				PreferenceConstants.P_MAXIMUM_RATIO_OF_DUPLICATED_TO_EXTRACTED,
				"&Maximum ratio of duplicated to extracted statements:", sliceExtractionPreferenceGroup);
		maximumRatioOfDuplicatedToExtractedFieldEditor.setEmptyStringAllowed(false);
		addField(maximumRatioOfDuplicatedToExtractedFieldEditor);
		
		minimumMethodSizeFieldEditor = new IntegerFieldEditor(
				PreferenceConstants.P_MINIMUM_METHOD_SIZE,
				"&Minimum number of statements in method:", sliceExtractionPreferenceGroup);
		minimumMethodSizeFieldEditor.setEmptyStringAllowed(false);
		addField(minimumMethodSizeFieldEditor);
		
		Group callGraphAnalysisPreferenceGroup = new Group(composite, SWT.SHADOW_ETCHED_IN);
		callGraphAnalysisPreferenceGroup.setLayout(new GridLayout(1, false));
		callGraphAnalysisPreferenceGroup.setText("Call Graph Analysis Preferences");
		
		enableCallGraphAnalysisFieldEditor = new BooleanFieldEditor(
				PreferenceConstants.P_ENABLE_CALL_GRAPH_ANALYSIS,
				"&Enable Call Graph Analysis", callGraphAnalysisPreferenceGroup);
		addField(enableCallGraphAnalysisFieldEditor);
		
		enableAliasAnalysisFieldEditor = new BooleanFieldEditor(
				PreferenceConstants.P_ENABLE_ALIAS_ANALYSIS,
				"&Enable Alias Analysis", callGraphAnalysisPreferenceGroup);
		addField(enableAliasAnalysisFieldEditor);
		
		maximumCallGraphAnalysisDepthFieldEditor = new IntegerFieldEditor(
				PreferenceConstants.P_MAXIMUM_CALL_GRAPH_ANALYSIS_DEPTH,
				"&Maximum depth of method call graph analysis:", callGraphAnalysisPreferenceGroup);
		maximumCallGraphAnalysisDepthFieldEditor.setEmptyStringAllowed(false);
		addField(maximumCallGraphAnalysisDepthFieldEditor);
		
		Group compilationUnitCachePreferenceGroup = new Group(composite, SWT.SHADOW_ETCHED_IN);
		compilationUnitCachePreferenceGroup.setLayout(new GridLayout(1, false));
		compilationUnitCachePreferenceGroup.setText("CompilationUnit Cache Preferences");
		
		projectCompilationUnitCacheSizeFieldEditor = new IntegerFieldEditor(
				PreferenceConstants.P_PROJECT_COMPILATION_UNIT_CACHE_SIZE,
				"&Project CompilationUnit cache size:", compilationUnitCachePreferenceGroup);
		projectCompilationUnitCacheSizeFieldEditor.setEmptyStringAllowed(false);
		addField(projectCompilationUnitCacheSizeFieldEditor);

		libraryCompilationUnitCacheSizeFieldEditor = new IntegerFieldEditor(
				PreferenceConstants.P_LIBRARY_COMPILATION_UNIT_CACHE_SIZE,
				"&Library CompilationUnit cache size:", compilationUnitCachePreferenceGroup);
		libraryCompilationUnitCacheSizeFieldEditor.setEmptyStringAllowed(false);
		addField(libraryCompilationUnitCacheSizeFieldEditor);
		
		Group usageReportingGroup = new Group(composite, SWT.SHADOW_ETCHED_IN);
		usageReportingGroup.setLayout(new GridLayout(1, false));
		usageReportingGroup.setText("Usage Reporting");
		
		enableUsageReportingFieldEditor = new BooleanFieldEditor(
				PreferenceConstants.P_ENABLE_USAGE_REPORTING,
				"&Allow the JDeodorant team to receive your ratings and refactoring applications", usageReportingGroup);
		addField(enableUsageReportingFieldEditor);
		
		enableSourceCodeReportingFieldEditor = new BooleanFieldEditor(
				PreferenceConstants.P_ENABLE_SOURCE_CODE_REPORTING,
				"&Allow the JDeodorant team to receive source code related information", usageReportingGroup);
		addField(enableSourceCodeReportingFieldEditor);
	}

	protected void checkState() {
		super.checkState();
		try {
			int minimumSliceSize = minimumSliceSizeFieldEditor.getIntValue();
			if(minimumSliceSize >= 0) {
				setErrorMessage(null);
				setValid(true);
			}
			else {
				setErrorMessage("Minimum number of slice statements must be >= 0");
				setValid(false);
				return;
			}
		}
		catch(NumberFormatException e) {
			setErrorMessage("Minimum number of slice statements must be an Integer");
			setValid(false);
			return;
		}
		try {
			int maximumSliceSize = maximumSliceSizeFieldEditor.getIntValue();
			if(maximumSliceSize >= 0) {
				setErrorMessage(null);
				setValid(true);
			}
			else {
				setErrorMessage("Maximum number of slice statements must be >= 0");
				setValid(false);
				return;
			}
		}
		catch(NumberFormatException e) {
			setErrorMessage("Maximum number of slice statements must be an Integer");
			setValid(false);
			return;
		}
		try {
			int maximumDuplication = maximumDuplicationFieldEditor.getIntValue();
			if(maximumDuplication >= 0) {
				setErrorMessage(null);
				setValid(true);
			}
			else {
				setErrorMessage("Maximum number of duplicated statements must be >= 0");
				setValid(false);
				return;
			}
		}
		catch(NumberFormatException e) {
			setErrorMessage("Maximum number of duplicated statements must be an Integer");
			setValid(false);
			return;
		}
		try {
			String ratio = maximumRatioOfDuplicatedToExtractedFieldEditor.getStringValue();
			double r = Double.parseDouble(ratio);
			if(r >= 0.0 && r <= 1.0) {
				setErrorMessage(null);
				setValid(true);
			}
			else {
				setErrorMessage("Duplication ratio must be a Double within range [0, 1]");
				setValid(false);
				return;
			}
		}
		catch(NumberFormatException e) {
			setErrorMessage("Duplication ratio must be a Double");
			setValid(false);
			return;
		}
		try {
			int minimumMethodSize = minimumMethodSizeFieldEditor.getIntValue();
			if(minimumMethodSize >= 0) {
				setErrorMessage(null);
				setValid(true);
			}
			else {
				setErrorMessage("Minimum number of statements in method must be >= 0");
				setValid(false);
				return;
			}
		}
		catch(NumberFormatException e) {
			setErrorMessage("Minimum number of statements in method must be an Integer");
			setValid(false);
			return;
		}
		try {
			int maximumCallGraphAnalysisDepth = maximumCallGraphAnalysisDepthFieldEditor.getIntValue();
			if(maximumCallGraphAnalysisDepth >= 0) {
				setErrorMessage(null);
				setValid(true);
			}
			else {
				setErrorMessage("Maximum depth of call graph analysis must be >= 0");
				setValid(false);
				return;
			}
		}
		catch(NumberFormatException e) {
			setErrorMessage("Maximum depth of call graph analysis must be an Integer");
			setValid(false);
			return;
		}
		try {
			int projectCompilationUnitCacheSize = projectCompilationUnitCacheSizeFieldEditor.getIntValue();
			if(projectCompilationUnitCacheSize >= 10) {
				setErrorMessage(null);
				setValid(true);
			}
			else {
				setErrorMessage("Cache size is recommended to be >= 10");
				setValid(false);
				return;
			}
		}
		catch(NumberFormatException e) {
			setErrorMessage("Cache size must be an Integer");
			setValid(false);
			return;
		}
		try {
			int libraryCompilationUnitCacheSize = libraryCompilationUnitCacheSizeFieldEditor.getIntValue();
			if(libraryCompilationUnitCacheSize >= 20) {
				setErrorMessage(null);
				setValid(true);
			}
			else {
				setErrorMessage("Cache size is recommended to be >= 20");
				setValid(false);
				return;
			}
		}
		catch(NumberFormatException e) {
			setErrorMessage("Cache size must be an Integer");
			setValid(false);
			return;
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		if(event.getProperty().equals(FieldEditor.VALUE)) {
			checkState();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
}