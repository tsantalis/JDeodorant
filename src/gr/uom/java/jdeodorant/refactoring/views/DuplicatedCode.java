package gr.uom.java.jdeodorant.refactoring.views;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.decomposition.cfg.mapping.CloneInstanceMapper;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGRegionSubTreeMapper;
import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractCloneRefactoring;

import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.texteditor.ITextEditor;

import ca.concordia.jdeodorant.clone.parsers.CloneGroup;
import ca.concordia.jdeodorant.clone.parsers.CloneGroupList;
import ca.concordia.jdeodorant.clone.parsers.CloneInstance;

public class DuplicatedCode extends ViewPart {
	private TreeViewer treeViewer;
	private Action importClonesAction;
	private Action applyRefactoringAction;
	private Action doubleClickAction;
	private IJavaProject selectedProject;
	private CloneGroup[] cloneGroupTable;
	private CloneInstanceMapper mapper;
	
	class ViewContentProvider implements ITreeContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if(cloneGroupTable!=null) {
				return cloneGroupTable;
			}
			else {
				return new CloneGroup[] {};
			}
		}
		public Object[] getChildren(Object arg) {
			if (arg instanceof CloneGroup) {
				return ((CloneGroup)arg).getCloneInstances().toArray();
			}
			else {
				return new CloneInstance[] {};
			}
		}
		public Object getParent(Object arg0) {
			if(arg0 instanceof CloneInstance) {
				CloneInstance cloneInstance = (CloneInstance)arg0;
				for(int i=0; i<cloneGroupTable.length; i++) {
					if(cloneGroupTable[i].getCloneInstances().contains(cloneInstance))
						return cloneGroupTable[i];
				}
			}
			return null;
		}
		public boolean hasChildren(Object arg0) {
			return getChildren(arg0).length > 0;
		}
	}

	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if(obj instanceof CloneInstance) {
				CloneInstance cloneInstance = (CloneInstance)obj;
				switch(index){
				case 0:
					return cloneInstance.getPackageName() + "." + cloneInstance.getClassName();
				case 1:
					return cloneInstance.getMethodSignature();
				default:
					return "";
				}
			}
			else if(obj instanceof CloneGroup) {
				CloneGroup group = (CloneGroup)obj;
				switch(index){
				case 0:
					return "Clone group " + group.getCloneGroupID();
				default:
					return "";
				}
			}
			return "";
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}
	
	private ISelectionListener selectionListener = new ISelectionListener() {
		public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection)selection;
				Object element = structuredSelection.getFirstElement();
				IJavaProject javaProject = null;
				if(element instanceof IJavaProject) {
					javaProject = (IJavaProject)element;
				}
				else if(element instanceof IPackageFragmentRoot) {
					IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot)element;
					javaProject = packageFragmentRoot.getJavaProject();
				}
				else if(element instanceof IPackageFragment) {
					IPackageFragment packageFragment = (IPackageFragment)element;
					javaProject = packageFragment.getJavaProject();
				}
				else if(element instanceof ICompilationUnit) {
					ICompilationUnit compilationUnit = (ICompilationUnit)element;
					javaProject = compilationUnit.getJavaProject();
				}
				else if(element instanceof IType) {
					IType type = (IType)element;
					javaProject = type.getJavaProject();
				}
				else if(element instanceof IMethod) {
					IMethod method = (IMethod)element;
					javaProject = method.getJavaProject();
				}
				else if(element instanceof IField) {
					IField field = (IField)element;
					javaProject = field.getJavaProject();
				}
				if(javaProject != null && !javaProject.equals(selectedProject)) {
					selectedProject = javaProject;
					importClonesAction.setEnabled(true);
					applyRefactoringAction.setEnabled(false);
					//saveResultsAction.setEnabled(false);
				}
			}
		}
	};

	@Override
	public void createPartControl(Composite parent) {
		treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		treeViewer.setContentProvider(new ViewContentProvider());
		treeViewer.setLabelProvider(new ViewLabelProvider());
		treeViewer.setInput(getViewSite());
		
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(60, true));
		layout.addColumnData(new ColumnWeightData(40, true));
		
		treeViewer.getTree().setLayout(layout);
		treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		treeViewer.getTree().setLinesVisible(true);
		treeViewer.getTree().setHeaderVisible(true);
		TreeColumn column0 = new TreeColumn(treeViewer.getTree(),SWT.LEFT);
		column0.setText("Clone Java file");
		column0.setResizable(true);
		column0.pack();
		
		TreeColumn column1 = new TreeColumn(treeViewer.getTree(),SWT.LEFT);
		column1.setText("Clone method");
		column1.setResizable(true);
		column1.pack();
		
		treeViewer.expandAll();
		
		makeActions();
		hookDoubleClickAction();
		contributeToActionBars();
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(selectionListener);
		JavaCore.addElementChangedListener(new ElementChangedListener());
		getSite().getWorkbenchWindow().getWorkbench().getOperationSupport().getOperationHistory().addOperationHistoryListener(new IOperationHistoryListener() {
			public void historyNotification(OperationHistoryEvent event) {
				int eventType = event.getEventType();
				if(eventType == OperationHistoryEvent.UNDONE  || eventType == OperationHistoryEvent.REDONE ||
						eventType == OperationHistoryEvent.OPERATION_ADDED || eventType == OperationHistoryEvent.OPERATION_REMOVED) {
					applyRefactoringAction.setEnabled(false);
					//saveResultsAction.setEnabled(false);
				}
			}
		});
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(importClonesAction);
		manager.add(applyRefactoringAction);
		//manager.add(saveResultsAction);
	}
	
	private void makeActions() {
		importClonesAction = new Action() {
			public void run() {
				CompilationUnitCache.getInstance().clearCache();
				ImportClonesWizard wizard = new ImportClonesWizard(selectedProject);
				WizardDialog dialog = new WizardDialog(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
						wizard);
				dialog.open();
				CloneGroupList cloneGroupList = wizard.getCloneGroupList();
				if(cloneGroupList != null) {
					cloneGroupTable = cloneGroupList.getCloneGroups();
					treeViewer.setContentProvider(new ViewContentProvider());
					applyRefactoringAction.setEnabled(true);
					//saveResultsAction.setEnabled(true);
				}
			}
		};
		importClonesAction.setToolTipText("Import Clones");
		importClonesAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		importClonesAction.setEnabled(false);
		
		applyRefactoringAction = new Action() {
			public void run() {
				try {
					CompilationUnitCache.getInstance().clearCache();
					IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
					if(selection instanceof IStructuredSelection) {
						IStructuredSelection structuredSelection = (IStructuredSelection)selection;
						final List list = structuredSelection.toList();
						if(list.size() == 2) {
							if(list.get(0) instanceof CloneInstance && list.get(1) instanceof CloneInstance) {
								final CloneInstance instance1 = (CloneInstance) list.get(0);
								final CloneInstance instance2 = (CloneInstance) list.get(1);
								if(instance1.getBelongingCloneGroup().equals(instance2.getBelongingCloneGroup())) {
									IWorkbench wb = PlatformUI.getWorkbench();
									IProgressService ps = wb.getProgressService();
									if(ASTReader.getSystemObject() != null && selectedProject.equals(ASTReader.getExaminedProject())) {
										new ASTReader(selectedProject, ASTReader.getSystemObject(), null);
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
											mapper = new CloneInstanceMapper(instance1, instance2, selectedProject, monitor);
										}
									});
									if(mapper != null && !mapper.getSubTreeMappers().isEmpty()) {
										try {
											for(PDGRegionSubTreeMapper subTreeMapper : mapper.getSubTreeMappers()) {
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
										MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Duplicated Code Refactoring",
												"Unfortunatley, no refactoring opportunities were found.");
									}
									CompilationUnitCache.getInstance().releaseLock();
								}
								else {
									wrongSelectionMessage();
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
				}
			}
		};
		applyRefactoringAction.setToolTipText("Apply Refactoring");
		applyRefactoringAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_DEF_VIEW));
		applyRefactoringAction.setEnabled(false);
		
		doubleClickAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
				if(selection.getFirstElement() instanceof CloneInstance) {
					CloneInstance cloneInstance = (CloneInstance)selection.getFirstElement();
					String fullName = cloneInstance.getPackageName().replace(".", "/") + "/" +
							cloneInstance.getClassName() + ".java";
					try {
						ICompilationUnit sourceJavaElement = getICompilationUnit(selectedProject, fullName);
						ITextEditor sourceEditor = (ITextEditor)JavaUI.openInEditor(sourceJavaElement);
						AnnotationModel annotationModel = (AnnotationModel)sourceEditor.getDocumentProvider().getAnnotationModel(sourceEditor.getEditorInput());
						Iterator<Annotation> annotationIterator = annotationModel.getAnnotationIterator();
						while(annotationIterator.hasNext()) {
							Annotation currentAnnotation = annotationIterator.next();
							if(currentAnnotation.getType().equals(SliceAnnotation.EXTRACTION) || currentAnnotation.getType().equals(SliceAnnotation.DUPLICATION)) {
								annotationModel.removeAnnotation(currentAnnotation);
							}
						}
						int offset = cloneInstance.getLocationInfo().getStartOffset();
						int length = cloneInstance.getLocationInfo().getLength();
						Position position = new Position(offset, length);
						SliceAnnotation annotation = new SliceAnnotation(SliceAnnotation.EXTRACTION, null);
						annotationModel.addAnnotation(annotation, position);
						sourceEditor.setHighlightRange(offset, length, true);
					} catch (PartInitException e) {
						e.printStackTrace();
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
			}
		};
	}

	private void hookDoubleClickAction() {
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}
	
	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}

	public void dispose() {
		super.dispose();
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(selectionListener);
	}

	private ICompilationUnit getICompilationUnit(IJavaProject iJavaProject, String fullName) {
		try {
			IClasspathEntry[] classpathEntries = iJavaProject.getResolvedClasspath(true);
			for(int i = 0; i < classpathEntries.length; i++){
				IClasspathEntry entry = classpathEntries[i];

				if(entry.getContentKind() == IPackageFragmentRoot.K_SOURCE){
					IPath path = entry.getPath();  
					if (path.toString().length() > iJavaProject.getProject().getName().length() + 2) {
						String fullPath = path.toString().substring(iJavaProject.getProject().getName().length() + 2) + "/" + fullName;

						ICompilationUnit iCompilationUnit = (ICompilationUnit)JavaCore.create(iJavaProject.getProject().getFile(fullPath));
						if (iCompilationUnit != null && iCompilationUnit.exists())
							return iCompilationUnit;
					}
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void wrongSelectionMessage() {
		MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Duplicated Code Refactoring",
				"You must select two (2) clone instances from the same clone group.");
	}
}
