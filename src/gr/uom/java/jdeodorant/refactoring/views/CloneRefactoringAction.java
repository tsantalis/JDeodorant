package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.AbstractMethodDeclaration;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.CompilationErrorDetectedException;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.decomposition.cfg.CFG;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGMapper;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGSubTreeMapper;
import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractCloneRefactoring;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

public class CloneRefactoringAction implements IObjectActionDelegate {
	private static final String MESSAGE_DIALOG_TITLE = "Duplicated Code Refactoring";
	private IWorkbenchPart part;
	private ISelection selection;
	private PDGMapper mapper;
	
	public void run(IAction action) {
		try {
			CompilationUnitCache.getInstance().clearCache();
			if(selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection)selection;
				final List list = structuredSelection.toList();
				if(list.size() == 2) {
					final IMethod method1 = (IMethod)list.get(0);
					final IMethod method2 = (IMethod)list.get(1);
					final IJavaProject project1 = method1.getJavaProject();
					final IJavaProject project2 = method2.getJavaProject();
					if(project1.equals(project2)) {
						final IJavaProject selectedProject = method1.getJavaProject();
						IWorkbench wb = PlatformUI.getWorkbench();
						IProgressService ps = wb.getProgressService();
						if(ASTReader.getSystemObject() != null && selectedProject.equals(ASTReader.getExaminedProject())) {
							new ASTReader(selectedProject, ASTReader.getSystemObject(), null);
						}
						else {
							ps.busyCursorWhile(new IRunnableWithProgress() {
								public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
									try {
										new ASTReader(selectedProject, monitor);
									} catch (CompilationErrorDetectedException e) {
										Display.getDefault().asyncExec(new Runnable() {
											public void run() {
												MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), MESSAGE_DIALOG_TITLE,
														"Compilation errors were detected in the project. Fix the errors before using JDeodorant.");
											}
										});
									}
								}
							});
						}
						SystemObject systemObject = ASTReader.getSystemObject();
						if(systemObject != null) {
							final AbstractMethodDeclaration methodObject1 = systemObject.getMethodObject(method1);
							final AbstractMethodDeclaration methodObject2 = systemObject.getMethodObject(method2);
							if(methodObject1 != null && methodObject2 != null && methodObject1.getMethodBody() != null && methodObject2.getMethodBody() != null) {
								final ClassObject classObject1 = systemObject.getClassObject(methodObject1.getClassName());
								final ClassObject classObject2 = systemObject.getClassObject(methodObject2.getClassName());
								if(classObject1 != null && !classObject1.isEnum() && !classObject1.isInterface() &&
										classObject2 != null && !classObject2.isEnum() && !classObject2.isInterface()) {
									ps.busyCursorWhile(new IRunnableWithProgress() {
										public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
											ITypeRoot typeRoot1 = classObject1.getITypeRoot();
											ITypeRoot typeRoot2 = classObject2.getITypeRoot();
											CompilationUnitCache.getInstance().lock(typeRoot1);
											CompilationUnitCache.getInstance().lock(typeRoot2);
											CFG cfg1 = new CFG(methodObject1);
											final PDG pdg1 = new PDG(cfg1, classObject1.getIFile(), classObject1.getFieldsAccessedInsideMethod(methodObject1), monitor);
											CFG cfg2 = new CFG(methodObject2);
											final PDG pdg2 = new PDG(cfg2, classObject2.getIFile(), classObject2.getFieldsAccessedInsideMethod(methodObject2), monitor);
											mapper = new PDGMapper(pdg1, pdg2, monitor);
											//CompilationUnitCache.getInstance().releaseLock();
										}
									});
								}
								else {
									MessageDialog.openInformation(part.getSite().getShell(), MESSAGE_DIALOG_TITLE,
											"At least one of the selected methods belongs to an interface, enum, or anonymous class.");
								}
							}
							else {
								MessageDialog.openInformation(part.getSite().getShell(), MESSAGE_DIALOG_TITLE,
										"At least one of the selected methods is abstract.");
							}
							if(mapper != null && !mapper.getSubTreeMappers().isEmpty()) {
								try {
									for(PDGSubTreeMapper subTreeMapper : mapper.getSubTreeMappers()) {
										JavaUI.openInEditor(((CompilationUnit)subTreeMapper.getPDG1().getMethod().getMethodDeclaration().getRoot()).getJavaElement());
										JavaUI.openInEditor(((CompilationUnit)subTreeMapper.getPDG2().getMethod().getMethodDeclaration().getRoot()).getJavaElement());
									}
								} catch (PartInitException e) {
									e.printStackTrace();
								} catch (JavaModelException e) {
									e.printStackTrace();
								}
								Refactoring refactoring = new ExtractCloneRefactoring(mapper.getSubTreeMappers());
								MyRefactoringWizard wizard = new MyRefactoringWizard(refactoring, null);
								RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
								try { 
									String titleForFailedChecks = ""; //$NON-NLS-1$ 
									op.run(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), titleForFailedChecks); 
								} catch(InterruptedException e) {
									e.printStackTrace();
								}
							}
							else {
								MessageDialog.openInformation(part.getSite().getShell(), MESSAGE_DIALOG_TITLE,
										"Unfortunatley, no refactoring opportunities were found.");
							}
							CompilationUnitCache.getInstance().releaseLock();
						}
					}
					else {
						wrongSelectionMessage();
					}
				}
				else {
					wrongSelectionMessage();
				}
			}
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (CompilationErrorDetectedException e) {
			MessageDialog.openInformation(part.getSite().getShell(), MESSAGE_DIALOG_TITLE,
					"Compilation errors were detected in the project. Fix the errors before using JDeodorant.");
		}
	}

	private void wrongSelectionMessage() {
		MessageDialog.openInformation(part.getSite().getShell(), MESSAGE_DIALOG_TITLE,
				"You must select two (2) methods from the same project.");
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.part = targetPart;
		JavaCore.addElementChangedListener(new ElementChangedListener());
	}
}
