package gr.uom.java.jdeodorant.refactoring.views;

import java.lang.reflect.InvocationTargetException;

import gr.uom.java.history.MethodSimilarityEvolution;
import gr.uom.java.history.ProjectEvolution;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

public class MethodEvolutionAction implements IObjectActionDelegate {
	private IWorkbenchPart part;
	private ISelection selection;
	private MethodSimilarityEvolution methodSimilarityEvolution;
	
	public void run(IAction action) {
		try {
			if(selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection)selection;
				Object element = structuredSelection.getFirstElement();
				if(element instanceof IMethod) {
					final IMethod method = (IMethod)element;
					final IJavaProject selectedProject = method.getJavaProject();
					IWorkbench wb = PlatformUI.getWorkbench();
					IProgressService ps = wb.getProgressService();
					ps.busyCursorWhile(new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							ProjectEvolution projectEvolution = new ProjectEvolution(selectedProject);
							methodSimilarityEvolution = new MethodSimilarityEvolution(projectEvolution, method.getKey(), monitor);
						}
					});
					if(methodSimilarityEvolution != null) {
						MethodSimilarityEvolutionDialog dialog = new MethodSimilarityEvolutionDialog(part.getSite().getWorkbenchWindow(), methodSimilarityEvolution);
						dialog.open();
					}
				}
			}
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.part = targetPart;
	}

}
