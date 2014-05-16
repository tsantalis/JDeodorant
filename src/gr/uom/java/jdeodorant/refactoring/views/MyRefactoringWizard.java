package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractClassRefactoring;
import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractCloneRefactoring;
import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractMethodRefactoring;
import gr.uom.java.jdeodorant.refactoring.manipulators.MoveMethodRefactoring;
import gr.uom.java.jdeodorant.refactoring.manipulators.ReplaceConditionalWithPolymorphism;
import gr.uom.java.jdeodorant.refactoring.manipulators.ReplaceTypeCodeWithStateStrategy;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.ui.PartInitException;

public class MyRefactoringWizard extends RefactoringWizard {
	
	public static final Font INPUT_PAGE_FONT = new Font(null, new FontData("Segoe UI", 9, SWT.NORMAL));
	private Refactoring refactoring;
	private Action action;
	
	public MyRefactoringWizard(Refactoring refactoring, Action action) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE | NO_BACK_BUTTON_ON_STATUS_DIALOG);
		setDefaultPageTitle(refactoring.getName());
		this.refactoring = refactoring;
		this.action = action;
	}

	@Override
	protected void addUserInputPages() {
		if(refactoring instanceof ReplaceTypeCodeWithStateStrategy) {
			addPage(new ReplaceTypeCodeWithStateStrategyInputPage((ReplaceTypeCodeWithStateStrategy)refactoring));
		}
		else if(refactoring instanceof ExtractClassRefactoring) {
			addPage(new ExtractClassInputPage((ExtractClassRefactoring)refactoring));
		}
		else if(refactoring instanceof MoveMethodRefactoring) {
			addPage(new MoveMethodInputPage((MoveMethodRefactoring)refactoring));
		}
		else if(refactoring instanceof ExtractMethodRefactoring) {
			addPage(new ExtractMethodInputPage((ExtractMethodRefactoring)refactoring));
		}
		else if(refactoring instanceof ExtractCloneRefactoring) {
			addPage(new CloneDiffWizardPage((ExtractCloneRefactoring)refactoring));
		}
	}
	
	@Override
	public boolean performFinish() {
		boolean finish = super.performFinish();
		if(action != null)
			action.setEnabled(false);
		Set<IJavaElement> javaElementsToOpenInEditor = new LinkedHashSet<IJavaElement>();
		if(refactoring instanceof ReplaceTypeCodeWithStateStrategy) {
			javaElementsToOpenInEditor.addAll(((ReplaceTypeCodeWithStateStrategy)refactoring).getJavaElementsToOpenInEditor());
		}
		else if(refactoring instanceof ReplaceConditionalWithPolymorphism) {
			javaElementsToOpenInEditor.addAll(((ReplaceConditionalWithPolymorphism)refactoring).getJavaElementsToOpenInEditor());
		}
		else if(refactoring instanceof ExtractClassRefactoring) {
			javaElementsToOpenInEditor.addAll(((ExtractClassRefactoring)refactoring).getJavaElementsToOpenInEditor());
		}
		else if(refactoring instanceof ExtractCloneRefactoring) {
			javaElementsToOpenInEditor.addAll(((ExtractCloneRefactoring)refactoring).getJavaElementsToOpenInEditor());
		}
		for(IJavaElement javaElement : javaElementsToOpenInEditor) {
			try {
				JavaUI.openInEditor(javaElement);
			} catch (PartInitException e) {
				e.printStackTrace();
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return finish;
	}
}
