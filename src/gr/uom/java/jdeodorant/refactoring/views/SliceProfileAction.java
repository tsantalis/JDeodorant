package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.decomposition.cfg.CFG;
import gr.uom.java.ast.decomposition.cfg.PDG;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

public class SliceProfileAction implements IObjectActionDelegate {

	private IWorkbenchPart part;
	private ISelection selection;
	private PDG pdg;
	private boolean selectedMethodHasNoBody;

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.part = targetPart;
		JavaCore.addElementChangedListener(new ElementChangedListener());
	}

	public void run(IAction action) {
		try {
			CompilationUnitCache.getInstance().clearCache();
			if(selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection)selection;
				Object element = structuredSelection.getFirstElement();
				if(element instanceof IMethod) {
					this.pdg = null;
					this.selectedMethodHasNoBody = false;
					final IMethod method = (IMethod)element;
					final IJavaProject selectedProject = method.getJavaProject();
					IWorkbench wb = PlatformUI.getWorkbench();
					IProgressService ps = wb.getProgressService();
					if(ASTReader.getSystemObject() != null && selectedProject.equals(ASTReader.getExaminedProject())) {
						ps.busyCursorWhile(new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								new ASTReader(selectedProject, ASTReader.getSystemObject(), monitor);
							}
						});
					}
					else {
						ps.busyCursorWhile(new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								new ASTReader(selectedProject, monitor);
							}
						});
					}
					ps.busyCursorWhile(new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							SystemObject systemObject = ASTReader.getSystemObject();
							MethodObject methodObject = systemObject.getMethodObject(method);
							if(methodObject != null) {
								if(methodObject.getMethodBody() != null) {
									ClassObject classObject = systemObject.getClassObject(methodObject.getClassName());
									ITypeRoot typeRoot = classObject.getITypeRoot();
									CompilationUnitCache.getInstance().lock(typeRoot);
									CFG cfg = new CFG(methodObject);
									pdg = new PDG(cfg, classObject.getIFile(), classObject.getFieldsAccessedInsideMethod(methodObject), monitor);
									CompilationUnitCache.getInstance().releaseLock();
								}
								else {
									selectedMethodHasNoBody = true;
								}
							}
						}
					});
					if(method.isConstructor())
						MessageDialog.openInformation(part.getSite().getShell(), "Slice-based Cohesion Metrics", "The selected method corresponds to a constructor.");
					if(selectedMethodHasNoBody)
						MessageDialog.openInformation(part.getSite().getShell(), "Slice-based Cohesion Metrics", "The selected method corresponds to an abstract method.");
					if(pdg != null) {
						if(pdg.getVariableDeclarationsInMethod().size() == 0)
							MessageDialog.openInformation(part.getSite().getShell(), "Slice-based Cohesion Metrics", "The selected method does not declare any local variables.");
						else {
							SliceProfileDialog dialog = new SliceProfileDialog(part.getSite().getWorkbenchWindow(), pdg);
							dialog.open();
						}
					}
				}
			}
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}
}
