package gr.uom.java.jdeodorant.refactoring.views;


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.distance.CandidateRefactoring;
import gr.uom.java.distance.CurrentSystem;
import gr.uom.java.distance.DistanceMatrix;
import gr.uom.java.distance.ExtractClassCandidateRefactoring;
import gr.uom.java.distance.ExtractClassCandidatesGroup;
import gr.uom.java.distance.MySystem;
import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractClassRefactoring;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.*;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.jface.action.*;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.*;
import org.eclipse.swt.SWT;


/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class GodClass extends ViewPart {
	private TableTreeViewer tableViewer;
	private Action identifyBadSmellsAction;
	private Action applyRefactoringAction;
	private Action doubleClickAction;
	private ExtractClassCandidatesGroup[] candidateRefactoringTable;
	private IJavaProject selectedProject;
	private IPackageFragmentRoot selectedPackageFragmentRoot;
	private IPackageFragment selectedPackageFragment;
	private ICompilationUnit selectedCompilationUnit;
	private IType selectedType;

	class ViewContentProvider implements ITreeContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if(candidateRefactoringTable!=null) {
				return candidateRefactoringTable;
			}
			else {
				return new ExtractClassCandidatesGroup[] {};
			}
		}
		public Object[] getChildren(Object arg0) {
			if (arg0 instanceof ExtractClassCandidatesGroup) {
				return ((ExtractClassCandidatesGroup) arg0).getCandidates().toArray();
			}
			else {
				return new CandidateRefactoring[] {};
			}
		}
		public Object getParent(Object arg0) {
			return getParentCandidate(((CandidateRefactoring)arg0).getSourceEntity());
		}
		public boolean hasChildren(Object arg0) {
			return getChildren(arg0).length > 0;
		}
	}
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (obj instanceof ExtractClassCandidatesGroup) {
				ExtractClassCandidatesGroup entry = (ExtractClassCandidatesGroup) obj;
				switch (index) {
				case 0:
					return "Extract Class";
				case 1:
					return entry.getSource();
				case 2:
					return "";
				case 3:
					return ""+entry.getMinEP();
				default:
					return "";
				}
			}
			else if(obj instanceof CandidateRefactoring) {
				CandidateRefactoring entry = (CandidateRefactoring)obj;
				switch(index) {
				case 2:
					return entry.getSourceEntity();
				case 3:
					return ""+entry.getEntityPlacement();
				default:
					return "";
				}
			}
			else {
				return "";
			}
		}
		public Image getColumnImage(Object obj, int index) {
			return null;
		}
		public Image getImage(Object obj) {
			return null;
			//return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}

	class NameSorter extends ViewerSorter {
		public int compare(Viewer viewer, Object obj1, Object obj2) {
			if (obj1 instanceof CandidateRefactoring
					&& obj2 instanceof CandidateRefactoring) {
				double value1 = ((CandidateRefactoring) obj1)
				.getEntityPlacement();
				double value2 = ((CandidateRefactoring) obj2)
				.getEntityPlacement();
				if (value1 < value2) {
					return -1;
				} else if (value1 > value2) {
					return 1;
				} else {
					return 0;
				}
			} else {
				double value1 = ((ExtractClassCandidatesGroup) obj1)
				.getMinEP();
				double value2 = ((ExtractClassCandidatesGroup) obj2)
				.getMinEP();
				if (value1 < value2) {
					return -1;
				} else if (value1 > value2) {
					return 1;
				} else {
					return 0;
				}
			}
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
					selectedPackageFragmentRoot = null;
					selectedPackageFragment = null;
					selectedCompilationUnit = null;
					selectedType = null;
				}
				else if(element instanceof IPackageFragmentRoot) {
					IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot)element;
					javaProject = packageFragmentRoot.getJavaProject();
					selectedPackageFragmentRoot = packageFragmentRoot;
					selectedPackageFragment = null;
					selectedCompilationUnit = null;
					selectedType = null;
				}
				else if(element instanceof IPackageFragment) {
					IPackageFragment packageFragment = (IPackageFragment)element;
					javaProject = packageFragment.getJavaProject();
					selectedPackageFragment = packageFragment;
					selectedPackageFragmentRoot = null;
					selectedCompilationUnit = null;
					selectedType = null;
				}
				else if(element instanceof ICompilationUnit) {
					ICompilationUnit compilationUnit = (ICompilationUnit)element;
					javaProject = compilationUnit.getJavaProject();
					selectedCompilationUnit = compilationUnit;
					selectedPackageFragmentRoot = null;
					selectedPackageFragment = null;
					selectedType = null;
				}
				else if(element instanceof IType) {
					IType type = (IType)element;
					javaProject = type.getJavaProject();
					selectedType = type;
					selectedPackageFragmentRoot = null;
					selectedPackageFragment = null;
					selectedCompilationUnit = null;
				}
				if(javaProject != null && !javaProject.equals(selectedProject)) {
					selectedProject = javaProject;
					/*if(candidateRefactoringTable != null)
						tableViewer.remove(candidateRefactoringTable);*/
					identifyBadSmellsAction.setEnabled(true);
					applyRefactoringAction.setEnabled(false);
				}
			}
		}
	};


	/**
	 * The constructor.
	 */
	public GodClass() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		tableViewer = new TableTreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		tableViewer.setContentProvider(new ViewContentProvider());
		tableViewer.setLabelProvider(new ViewLabelProvider());
		tableViewer.setSorter(new NameSorter());
		tableViewer.setInput(getViewSite());
		tableViewer.getTableTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		Table table = tableViewer.getTableTree().getTable();
		new TableColumn(table, SWT.LEFT).setText("Refactoring Type");
		new TableColumn(table, SWT.LEFT).setText("Group Name");
		new TableColumn(table, SWT.LEFT).setText("Source Entity");
		new TableColumn(table, SWT.LEFT).setText("Entity Placement");
		tableViewer.expandAll();

		for (int i = 0, n = table.getColumnCount(); i < n; i++) {
			table.getColumn(i).pack();
		}

		table.setLinesVisible(true);
		table.setHeaderVisible(true);
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
				}
			}
		});
	}


	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}



	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(identifyBadSmellsAction);
		manager.add(applyRefactoringAction);
	}

	private void makeActions() {
		identifyBadSmellsAction = new Action() {
			public void run() {
				CompilationUnitCache.getInstance().clearCache();
				candidateRefactoringTable = getTable();
				tableViewer.setContentProvider(new ViewContentProvider());
				applyRefactoringAction.setEnabled(true);
				//saveResults();
			}
		};
		identifyBadSmellsAction.setToolTipText("Identify Bad Smells");
		identifyBadSmellsAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		identifyBadSmellsAction.setEnabled(false);

		applyRefactoringAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)tableViewer.getSelection();
				CandidateRefactoring entry = (CandidateRefactoring)selection.getFirstElement();
				if(entry.getSourceClassTypeDeclaration() != null) {
					IFile sourceFile = entry.getSourceIFile();
					CompilationUnit sourceCompilationUnit = (CompilationUnit)entry.getSourceClassTypeDeclaration().getRoot();
					Refactoring refactoring = null;
					if(entry instanceof ExtractClassCandidateRefactoring) {
						ExtractClassCandidateRefactoring candidate = (ExtractClassCandidateRefactoring)entry;
						String className = candidate.getTargetClassName().split("[.]")[candidate.getTargetClassName().split("[.]").length-1];
						candidate.setTargetClassName(className);
						refactoring = new ExtractClassRefactoring(sourceCompilationUnit, candidate.getSourceClassTypeDeclaration(), sourceFile, candidate.getExtractedEntities(), candidate.getLeaveDelegate(), candidate.getTargetClassName());
					}
					MyRefactoringWizard wizard = new MyRefactoringWizard(refactoring, applyRefactoringAction);
					RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard); 
					try { 
						String titleForFailedChecks = ""; //$NON-NLS-1$ 
						op.run(getSite().getShell(), titleForFailedChecks); 
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
					try {
						IJavaElement targetJavaElement = JavaCore.create(((ExtractClassRefactoring)refactoring).getTargetFile());
						JavaUI.openInEditor(targetJavaElement);
						IJavaElement sourceJavaElement = JavaCore.create(sourceFile);
						JavaUI.openInEditor(sourceJavaElement);
					} catch (PartInitException e) {
						e.printStackTrace();
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
			}
		};
		applyRefactoringAction.setToolTipText("Apply Refactoring");
		applyRefactoringAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_DEF_VIEW));
		applyRefactoringAction.setEnabled(false);

		doubleClickAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)tableViewer.getSelection();
				CandidateRefactoring candidate = (CandidateRefactoring)selection.getFirstElement();
				if(candidate.getSourceClassTypeDeclaration() != null) {
					IFile sourceFile = candidate.getSourceIFile();
					try {
						IJavaElement sourceJavaElement = JavaCore.create(sourceFile);
						ITextEditor sourceEditor = (ITextEditor)JavaUI.openInEditor(sourceJavaElement);
						List<Position> positions = candidate.getPositions();
						AnnotationModel annotationModel = (AnnotationModel)sourceEditor.getDocumentProvider().getAnnotationModel(sourceEditor.getEditorInput());
						Iterator<Annotation> annotationIterator = annotationModel.getAnnotationIterator();
						while(annotationIterator.hasNext()) {
							Annotation currentAnnotation = annotationIterator.next();
							if(currentAnnotation.getType().equals(SliceAnnotation.EXTRACTION)) {
								annotationModel.removeAnnotation(currentAnnotation);
							}
						}
						for(Position position : positions) {
							SliceAnnotation annotation = new SliceAnnotation(SliceAnnotation.EXTRACTION, candidate.getAnnotationText());
							annotationModel.addAnnotation(annotation, position);
						}
						Position firstPosition = positions.get(0);
						Position lastPosition = positions.get(positions.size()-1);
						int offset = firstPosition.getOffset();
						int length = lastPosition.getOffset() + lastPosition.getLength() - firstPosition.getOffset();
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
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		tableViewer.getControl().setFocus();
	}

	private ExtractClassCandidatesGroup[] getTable() {
		ExtractClassCandidatesGroup[] table = null;
		try {
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
			SystemObject systemObject = ASTReader.getSystemObject();
			Set<ClassObject> classObjectsToBeExamined = new LinkedHashSet<ClassObject>();
			if(selectedPackageFragmentRoot != null) {
				classObjectsToBeExamined.addAll(systemObject.getClassObjects(selectedPackageFragmentRoot));
			}
			else if(selectedPackageFragment != null) {
				classObjectsToBeExamined.addAll(systemObject.getClassObjects(selectedPackageFragment));
			}
			else if(selectedCompilationUnit != null) {
				classObjectsToBeExamined.addAll(systemObject.getClassObjects(selectedCompilationUnit));
			}
			else if(selectedType != null) {
				classObjectsToBeExamined.addAll(systemObject.getClassObjects(selectedType));
			}
			else {
				classObjectsToBeExamined.addAll(systemObject.getClassObjects());
			}
			final Set<String> classNamesToBeExamined = new LinkedHashSet<String>();
			for(ClassObject classObject : classObjectsToBeExamined) {
				classNamesToBeExamined.add(classObject.getName());
			}
			MySystem system = new MySystem(systemObject);
			final DistanceMatrix distanceMatrix = new DistanceMatrix(system);
			ps.busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					distanceMatrix.generateDistances(monitor);
				}
			});
			final List<ExtractClassCandidateRefactoring> extractClassCandidateList = new ArrayList<ExtractClassCandidateRefactoring>();

			ps.busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					extractClassCandidateList.addAll(distanceMatrix.getExtractClassCandidateRefactorings(classNamesToBeExamined, monitor));
				}
			});
			HashMap<String, ExtractClassCandidatesGroup> groupedBySourceClassList = new HashMap<String, ExtractClassCandidatesGroup>();
			for(ExtractClassCandidateRefactoring candidate : extractClassCandidateList) {
				if(groupedBySourceClassList.keySet().contains(candidate.getSourceEntity())) {
					groupedBySourceClassList.get(candidate.getSourceEntity()).addCandidate(candidate);
				}
				else {
					ExtractClassCandidatesGroup group = new ExtractClassCandidatesGroup(candidate.getSourceEntity());
					group.addCandidate(candidate);
					groupedBySourceClassList.put(candidate.getSourceEntity(), group);
				}
			}

			table = new ExtractClassCandidatesGroup[groupedBySourceClassList.values().size() + 1];
			ExtractClassCandidatesGroup currentSystem = new ExtractClassCandidatesGroup("current system");
			currentSystem.setMinEP(new CurrentSystem(distanceMatrix).getEntityPlacement());
			table[0] = currentSystem;
			int counter = 1;
			for(ExtractClassCandidatesGroup candidate : groupedBySourceClassList.values()) {
				table[counter] = candidate;
				counter++;
			}
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return table;		
	}

	private ExtractClassCandidatesGroup getParentCandidate(String sourceClass) {
		String[] classes = new String[candidateRefactoringTable.length];
		for(int i=0; i<candidateRefactoringTable.length; i++) {
			classes[i] = candidateRefactoringTable[i].getSource();
		}
		for(int i=0; i<classes.length; i++) {
			if(classes[i].equals(sourceClass)) {
				return candidateRefactoringTable[i];
			}
		}
		return null;
	}
}