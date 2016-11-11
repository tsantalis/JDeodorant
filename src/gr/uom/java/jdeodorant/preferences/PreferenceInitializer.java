package gr.uom.java.jdeodorant.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import gr.uom.java.jdeodorant.refactoring.Activator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(PreferenceConstants.P_MINIMUM_SLICE_SIZE, 0);
		store.setDefault(PreferenceConstants.P_MAXIMUM_SLICE_SIZE, 0);
		store.setDefault(PreferenceConstants.P_MAXIMUM_DUPLICATION, 100);
		store.setDefault(PreferenceConstants.P_MAXIMUM_RATIO_OF_DUPLICATED_TO_EXTRACTED, 1.0);
		store.setDefault(PreferenceConstants.P_MINIMUM_METHOD_SIZE, 0);
		store.setDefault(PreferenceConstants.P_MAXIMUM_CALL_GRAPH_ANALYSIS_DEPTH, 3);
		store.setDefault(PreferenceConstants.P_ENABLE_ALIAS_ANALYSIS, true);
		store.setDefault(PreferenceConstants.P_PROJECT_COMPILATION_UNIT_CACHE_SIZE, 20);
		store.setDefault(PreferenceConstants.P_LIBRARY_COMPILATION_UNIT_CACHE_SIZE, 50);
		store.setDefault(PreferenceConstants.P_ENABLE_USAGE_REPORTING, true);
		store.setDefault(PreferenceConstants.P_ENABLE_SOURCE_CODE_REPORTING, false);
		store.setDefault(PreferenceConstants.P_MAXIMUM_NUMBER_OF_SOURCE_CLASS_MEMBERS_ACCESSED_BY_MOVE_METHOD_CANDIDATE, 2);
		store.setDefault(PreferenceConstants.P_MAXIMUM_NUMBER_OF_SOURCE_CLASS_MEMBERS_ACCESSED_BY_EXTRACT_CLASS_CANDIDATE, 2);
	}

}
