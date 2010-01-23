package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.distance.CandidateRefactoring;
import gr.uom.java.distance.CurrentSystem;
import gr.uom.java.distance.MoveMethodCandidateRefactoring;
import gr.uom.java.distance.DistanceMatrix;
import gr.uom.java.distance.MySystem;
import gr.uom.java.jdeodorant.refactoring.manipulators.MoveMethodRefactoring;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.part.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;

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

public class FeatureEnvy extends ViewPart {
	private TableViewer tableViewer;
	private Action identifyBadSmellsAction;
	private Action applyRefactoringAction;
	private Action doubleClickAction;
	private Action renameMethodAction;
	private IJavaProject selectedProject;
	private IPackageFragment selectedPackage;
	private CandidateRefactoring[] candidateRefactoringTable;

	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */
	 
	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if(candidateRefactoringTable!=null) {
				return candidateRefactoringTable;
			}
			else {
				return new CandidateRefactoring[] {};
			}
		}
	}
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			CandidateRefactoring entry = (CandidateRefactoring)obj;
			switch(index){
			case 0:
				if(entry instanceof MoveMethodCandidateRefactoring)
					return "Move Method";
				/*else if(entry instanceof ExtractAndMoveMethodCandidateRefactoring)
					return "Extract and Move Method";*/
				else
					return "";
			case 1:
				return entry.getSourceEntity();
			case 2:
				return entry.getTarget();
			case 3:
				return Double.toString(entry.getEntityPlacement());
			default:
				return "";
			}
			
		}
		public Image getColumnImage(Object obj, int index) {
			return null;
		}
		public Image getImage(Object obj) {
			return null;
		}
	}
	class NameSorter extends ViewerSorter {
		public int compare(Viewer viewer, Object obj1, Object obj2) {
			double value1 = ((CandidateRefactoring)obj1).getEntityPlacement();
			double value2 = ((CandidateRefactoring)obj2).getEntityPlacement();
			if(value1 < value2) {
				return -1;
			}
			else if(value1 > value2) {
				return 1;
			}
			else {
				return 0;
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
					selectedPackage = null;
				}
				else if(element instanceof IPackageFragment) {
					IPackageFragment packageFragment = (IPackageFragment)element;
					javaProject = packageFragment.getJavaProject();
					selectedPackage = packageFragment;
				}
				if(javaProject != null && !javaProject.equals(selectedProject)) {
					selectedProject = javaProject;
					if(candidateRefactoringTable != null)
						tableViewer.remove(candidateRefactoringTable);
					identifyBadSmellsAction.setEnabled(true);
					applyRefactoringAction.setEnabled(false);
					renameMethodAction.setEnabled(false);
				}
			}
		}
	};

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		tableViewer = new TableViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		tableViewer.setContentProvider(new ViewContentProvider());
		tableViewer.setLabelProvider(new ViewLabelProvider());
		tableViewer.setSorter(new NameSorter());
		tableViewer.setInput(getViewSite());
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(40, true));
		layout.addColumnData(new ColumnWeightData(60, true));
		layout.addColumnData(new ColumnWeightData(40, true));
		layout.addColumnData(new ColumnWeightData(40, true));
		tableViewer.getTable().setLayout(layout);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		TableColumn column0 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column0.setText("Refactoring Type");
		column0.setResizable(true);
		column0.pack();
		TableColumn column1 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column1.setText("Source Entity");
		column1.setResizable(true);
		column1.pack();
		TableColumn column2 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column2.setText("Target Class");
		column2.setResizable(true);
		column2.pack();
		TableColumn column3 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column3.setText("Entity Placement");
		column3.setResizable(true);
		column3.pack();
		makeActions();
		hookDoubleClickAction();
		contributeToActionBars();
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(selectionListener);
		getSite().getWorkbenchWindow().getWorkbench().getOperationSupport().getOperationHistory().addOperationHistoryListener(new OperationHistoryListener());
		
		JFaceResources.getFontRegistry().put(MyToolTip.HEADER_FONT, JFaceResources.getFontRegistry().getBold(JFaceResources.getDefaultFont().getFontData()[0].getName()).getFontData());
		MyToolTip toolTip = new MyToolTip(tableViewer.getControl());
		toolTip.setShift(new Point(-5, -5));
		toolTip.setHideOnMouseDown(false);
		toolTip.activate();
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(identifyBadSmellsAction);
		manager.add(applyRefactoringAction);
		manager.add(renameMethodAction);
	}

	private void makeActions() {
		identifyBadSmellsAction = new Action() {
			public void run() {
				candidateRefactoringTable = getTable();
				tableViewer.setContentProvider(new ViewContentProvider());
				applyRefactoringAction.setEnabled(true);
				renameMethodAction.setEnabled(true);
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
				if(entry.getSourceClassTypeDeclaration() != null && entry.getTargetClassTypeDeclaration() != null) {
					IFile sourceFile = entry.getSourceIFile();
					IFile targetFile = entry.getTargetIFile();
					CompilationUnit sourceCompilationUnit = (CompilationUnit)entry.getSourceClassTypeDeclaration().getRoot();
					CompilationUnit targetCompilationUnit = (CompilationUnit)entry.getTargetClassTypeDeclaration().getRoot();
					Refactoring refactoring = null;
					if(entry instanceof MoveMethodCandidateRefactoring) {
						MoveMethodCandidateRefactoring candidate = (MoveMethodCandidateRefactoring)entry;
						refactoring = new MoveMethodRefactoring(sourceCompilationUnit, targetCompilationUnit,
								candidate.getSourceClassTypeDeclaration(), candidate.getTargetClassTypeDeclaration(), candidate.getSourceMethodDeclaration(),
								candidate.getAdditionalMethodsToBeMoved(), candidate.leaveDelegate(), candidate.getMovedMethodName());
					}
					/*else if(entry instanceof ExtractAndMoveMethodCandidateRefactoring) {
						ExtractAndMoveMethodCandidateRefactoring candidate = (ExtractAndMoveMethodCandidateRefactoring)entry;
						refactoring = new ExtractAndMoveMethodRefactoring(sourceFile, sourceCompilationUnit, targetCompilationUnit,
								candidate.getSourceClassTypeDeclaration(), candidate.getTargetClassTypeDeclaration(), candidate.getSourceMethodDeclaration(),
								candidate.getASTExtractionBlock());
					}*/
					MyRefactoringWizard wizard = new MyRefactoringWizard(refactoring, applyRefactoringAction);
					RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard); 
					try { 
						String titleForFailedChecks = ""; //$NON-NLS-1$ 
						op.run(getSite().getShell(), titleForFailedChecks); 
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
					try {
						IJavaElement targetJavaElement = JavaCore.create(targetFile);
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
				if(candidate.getSourceClassTypeDeclaration() != null && candidate.getTargetClassTypeDeclaration() != null) {
					IFile sourceFile = candidate.getSourceIFile();
					IFile targetFile = candidate.getTargetIFile();
					try {
						IJavaElement targetJavaElement = JavaCore.create(targetFile);
						JavaUI.openInEditor(targetJavaElement);
						IJavaElement sourceJavaElement = JavaCore.create(sourceFile);
						ITextEditor sourceEditor = (ITextEditor)JavaUI.openInEditor(sourceJavaElement);
						List<Position> positions = candidate.getPositions();
						AnnotationModel annotationModel = (AnnotationModel)sourceEditor.getDocumentProvider().getAnnotationModel(sourceEditor.getEditorInput());
						Iterator<Annotation> annotationIterator = annotationModel.getAnnotationIterator();
						while(annotationIterator.hasNext()) {
							Annotation currentAnnotation = annotationIterator.next();
							if(currentAnnotation.getType().equals("org.eclipse.jdt.ui.occurrences")) {
								annotationModel.removeAnnotation(currentAnnotation);
							}
						}
						for(Position position : positions) {
							Annotation annotation = new Annotation("org.eclipse.jdt.ui.occurrences", false, candidate.getAnnotationText());
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
		
		renameMethodAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)tableViewer.getSelection();
				CandidateRefactoring entry = (CandidateRefactoring)selection.getFirstElement();
				if(entry instanceof MoveMethodCandidateRefactoring) {
					MoveMethodCandidateRefactoring candidate = (MoveMethodCandidateRefactoring)entry;
					String methodName = candidate.getMovedMethodName();
					IInputValidator methodNameValidator = new MethodNameValidator();
					InputDialog dialog = new InputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Rename Method", "Please enter a new name", methodName, methodNameValidator);
					dialog.open();
					if(dialog.getValue() != null) {
						candidate.setMovedMethodName(dialog.getValue());
						tableViewer.refresh();
					}
				}
				/*else if(entry instanceof ExtractAndMoveMethodCandidateRefactoring) {
					ExtractAndMoveMethodCandidateRefactoring candidate = (ExtractAndMoveMethodCandidateRefactoring)entry;
					String methodName = candidate.getExtractionBlock().getExtractedMethodName();
					IInputValidator methodNameValidator = new MethodNameValidator();
					InputDialog dialog = new InputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Rename Method", "Please enter a new name", methodName, methodNameValidator);
					dialog.open();
					if(dialog.getValue() != null) {
						candidate.getExtractionBlock().setExtractedMethodName(dialog.getValue());
						tableViewer.refresh();
					}
				}*/
			}
		};
		renameMethodAction.setToolTipText("Rename Method");
		renameMethodAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJ_FILE));
		renameMethodAction.setEnabled(false);
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
	
	private List<CandidateRefactoring> getPrerequisiteRefactorings(CandidateRefactoring candidateRefactoring) {
		List<CandidateRefactoring> moveMethodPrerequisiteRefactorings = new ArrayList<CandidateRefactoring>();
		List<CandidateRefactoring> extractMethodPrerequisiteRefactorings = new ArrayList<CandidateRefactoring>();
		if(candidateRefactoringTable != null) {
			Set<String> entitySet = candidateRefactoring.getEntitySet();
			for(CandidateRefactoring candidate : candidateRefactoringTable) {
				if(candidate instanceof MoveMethodCandidateRefactoring) {
					if(entitySet.contains(candidate.getSourceEntity())/* && candidateRefactoring.getTarget().equals(candidate.getTarget())*/)
						moveMethodPrerequisiteRefactorings.add(candidate);
				}
				/*else if(candidate instanceof ExtractAndMoveMethodCandidateRefactoring) {
					ExtractAndMoveMethodCandidateRefactoring extractCandidate = (ExtractAndMoveMethodCandidateRefactoring)candidate;
					if(extractCandidate.isSubRefactoringOf(candidateRefactoring))
						extractMethodPrerequisiteRefactorings.add(candidate);
				}*/
			}
		}
		if(!moveMethodPrerequisiteRefactorings.isEmpty())
			return moveMethodPrerequisiteRefactorings;
		else
			return extractMethodPrerequisiteRefactorings;
	}
	
	private CandidateRefactoring[] getTable() {
		if(selectedPackage != null)
			new ASTReader(selectedPackage);
		else
			new ASTReader(selectedProject);
		SystemObject systemObject = ASTReader.getSystemObject();
		/*MMImportCoupling mmic = new MMImportCoupling(systemObject);
		System.out.println("System Average MMIC: " + mmic.getSystemAverageCoupling());
		ConnectivityMetric co = new ConnectivityMetric(systemObject);
		System.out.println("System Average Connectivity: " + co.getSystemAverageConnectivity());*/
		MySystem system = new MySystem(systemObject);
		final DistanceMatrix distanceMatrix = new DistanceMatrix(system);

		final List<MoveMethodCandidateRefactoring> moveMethodCandidateList = new ArrayList<MoveMethodCandidateRefactoring>();
		IWorkbenchWindow window = getSite().getWorkbenchWindow();
		try {
			window.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
			     public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			    	 moveMethodCandidateList.addAll(distanceMatrix.getMoveMethodCandidateRefactoringsByAccess(monitor));
			     }
			});
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		/*List<ExtractAndMoveMethodCandidateRefactoring> extractMethodCandidateList = distanceMatrix.getExtractAndMoveMethodCandidateRefactoringsByAccess();
		
		List<ExtractAndMoveMethodCandidateRefactoring> finalExtractMethodCandidateList = new ArrayList<ExtractAndMoveMethodCandidateRefactoring>();
		for(ExtractAndMoveMethodCandidateRefactoring candidate : extractMethodCandidateList) {
			boolean subRefactoring = false;
			for(ExtractAndMoveMethodCandidateRefactoring extractMethodCandidate : extractMethodCandidateList) {
				if(candidate.isSubRefactoringOf(extractMethodCandidate) && candidate.getTargetClass().equals(extractMethodCandidate.getTargetClass())) {
					subRefactoring = true;
					System.out.println(candidate.toString() + "\tis sub-refactoring of\t" + extractMethodCandidate.toString());
					break;
				}
			}
			if(!subRefactoring) {
				finalExtractMethodCandidateList.add(candidate);
			}
		}*/
		
		CandidateRefactoring[] table = new CandidateRefactoring[moveMethodCandidateList.size() /*+ finalExtractMethodCandidateList.size()*/ + 1];
		table[0] = new CurrentSystem(distanceMatrix);
		int counter = 1;
		/*for(ExtractAndMoveMethodCandidateRefactoring candidate : finalExtractMethodCandidateList) {
			table[counter] = candidate;
			counter++;
		}*/
		for(MoveMethodCandidateRefactoring candidate : moveMethodCandidateList) {
			table[counter] = candidate;
			counter++;
		}
		return table;		
	}
	
	protected class MyToolTip extends ToolTip {
		//public static final String HEADER_BG_COLOR = Policy.JFACE + ".TOOLTIP_HEAD_BG_COLOR";
		//public static final String HEADER_FG_COLOR = Policy.JFACE + ".TOOLTIP_HEAD_FG_COLOR";
		public static final String HEADER_FONT = Policy.JFACE + ".TOOLTIP_HEAD_FONT";
		
		public MyToolTip(Control control) {
			super(control);
		}
		
		protected Composite createToolTipContentArea(Event event, Composite parent) {
			Composite comp = new Composite(parent,SWT.NONE);
			GridLayout gl = new GridLayout(1,false);
			gl.marginBottom=0;
			gl.marginTop=0;
			gl.marginHeight=0;
			gl.marginWidth=0;
			gl.marginLeft=0;
			gl.marginRight=0;
			gl.verticalSpacing=1;
			comp.setLayout(gl);
			
			Composite topArea = new Composite(comp,SWT.NONE);
			GridData data = new GridData(SWT.FILL,SWT.FILL,true,false);
			data.widthHint=200;
			topArea.setLayoutData(data);
			topArea.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			
			gl = new GridLayout(1,false);
			gl.marginBottom=2;
			gl.marginTop=2;
			gl.marginHeight=0;
			gl.marginWidth=0;
			gl.marginLeft=5;
			gl.marginRight=2;
			
			topArea.setLayout(gl);
			
			Label label = new Label(topArea,SWT.NONE);
			label.setText("APPLY FIRST");
			label.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			label.setFont(JFaceResources.getFontRegistry().get(HEADER_FONT));
			//label.setForeground(JFaceResources.getColorRegistry().get(HEADER_FG_COLOR));
			label.setLayoutData(new GridData(GridData.FILL_BOTH));
			
			Table table = tableViewer.getTable();
			Point coords = new Point(event.x, event.y);
			TableItem item = table.getItem(coords);
			if(item != null) {
				List<CandidateRefactoring> prerequisiteRefactorings = getPrerequisiteRefactorings((CandidateRefactoring)item.getData());
				if(!prerequisiteRefactorings.isEmpty()) {
					final CandidateRefactoring firstPrerequisite = prerequisiteRefactorings.get(0);
					Composite comp2 = new Composite(comp,SWT.NONE);
					comp2.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
					FillLayout layout = new FillLayout();
					layout.marginWidth=5;
					comp2.setLayout(layout);
					Link link = new Link(comp2,SWT.NONE);
					link.setText("<a>" + firstPrerequisite.getSourceEntity() + "\n->" + firstPrerequisite.getTarget() + "</a>");
					link.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
					link.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							Table table = tableViewer.getTable();
							for(int i=0; i<table.getItemCount(); i++) {
								Object tableElement = tableViewer.getElementAt(i);
								CandidateRefactoring candidate = (CandidateRefactoring)tableElement;
								if(candidate.equals(firstPrerequisite)) {
									table.setSelection(i);
									break;
								}
							}
						}
					});
					comp2.setLayoutData(new GridData(GridData.FILL_BOTH));
				}
			}
			return comp;
		}
		
		protected boolean shouldCreateToolTip(Event event) {
			Table table = tableViewer.getTable();
			Point coords = new Point(event.x, event.y);
			TableItem item = table.getItem(coords);
			if(item != null) {
				List<CandidateRefactoring> prerequisiteRefactorings = getPrerequisiteRefactorings((CandidateRefactoring)item.getData());
				if(!prerequisiteRefactorings.isEmpty())
					return true;
			}
			return false;
		}
	}
}