package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.decomposition.cfg.CFG;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGMapper;
import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractCloneRefactoring;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

public class CloneRefactoringAction implements IObjectActionDelegate {

	private ISelection selection;
	private PDGMapper mapper;
	
	public void run(IAction action) {
		try {
			CompilationUnitCache.getInstance().clearCache();
			if(selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection)selection;
				Object element = structuredSelection.getFirstElement();
				final List list = structuredSelection.toList();
				if(element instanceof IMethod) {
					final IMethod method1 = (IMethod)element;
					final IJavaProject selectedProject = method1.getJavaProject();
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
							if(list.size() == 2) {
								IMethod method2 = (IMethod)list.get(1);
								MethodObject methodObject1 = systemObject.getMethodObject(method1);
								MethodObject methodObject2 = systemObject.getMethodObject(method2);
								if(methodObject1 != null && methodObject2 != null && methodObject1.getMethodBody() != null && methodObject2.getMethodBody() != null) {
									ClassObject classObject1 = systemObject.getClassObject(methodObject1.getClassName());
									ClassObject classObject2 = systemObject.getClassObject(methodObject2.getClassName());
									ITypeRoot typeRoot1 = classObject1.getITypeRoot();
									ITypeRoot typeRoot2 = classObject1.getITypeRoot();
									CompilationUnitCache.getInstance().lock(typeRoot1);
									CompilationUnitCache.getInstance().lock(typeRoot2);
									CFG cfg1 = new CFG(methodObject1);
									final PDG pdg1 = new PDG(cfg1, classObject1.getIFile(), classObject1.getFieldsAccessedInsideMethod(methodObject1), monitor);
									CFG cfg2 = new CFG(methodObject2);
									final PDG pdg2 = new PDG(cfg2, classObject2.getIFile(), classObject2.getFieldsAccessedInsideMethod(methodObject2), monitor);
									mapper = new PDGMapper(pdg1, pdg2, monitor);
									CompilationUnitCache.getInstance().releaseLock();
								}
							}
						}
					});
					Refactoring refactoring = new ExtractCloneRefactoring(mapper);
					MyRefactoringWizard wizard = new MyRefactoringWizard(refactoring, null);
					RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
					try { 
						String titleForFailedChecks = ""; //$NON-NLS-1$ 
						op.run(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), titleForFailedChecks); 
					} catch(InterruptedException e) {
						e.printStackTrace();
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
		JavaCore.addElementChangedListener(new ElementChangedListener());
	}

}
