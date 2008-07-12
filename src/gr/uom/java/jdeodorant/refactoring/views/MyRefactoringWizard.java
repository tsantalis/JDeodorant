package gr.uom.java.jdeodorant.refactoring.views;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class MyRefactoringWizard extends RefactoringWizard {

	public MyRefactoringWizard(Refactoring refactoring) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle(refactoring.getName());
	}

	@Override
	protected void addUserInputPages() {
	}

}
