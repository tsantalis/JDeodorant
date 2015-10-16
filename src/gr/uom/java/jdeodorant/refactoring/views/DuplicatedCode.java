package gr.uom.java.jdeodorant.refactoring.views;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.compare.BufferedContent;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
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
import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.decomposition.cfg.mapping.CloneInstanceMapper;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGRegionSubTreeMapper;
import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractCloneRefactoring;

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

	class ViewLabelProvider extends StyledCellLabelProvider {
		
		@Override
		public void update(ViewerCell cell) {
			
			List<StyleRange> styleRanges = new ArrayList<StyleRange>();
			
			Object obj = cell.getElement();
			int index = cell.getColumnIndex();
			String text = "";

			if(obj instanceof CloneInstance) {
				CloneInstance cloneInstance = (CloneInstance)obj;
				switch(index){
				case 0:
					text = cloneInstance.getPackageName() + "." + cloneInstance.getClassName();
					break;
				case 1:
					text = cloneInstance.getMethodSignature();
					break;
				default:
					text = "";
				}
			}
			else if(obj instanceof CloneGroup) {
				CloneGroup group = (CloneGroup)obj;
				switch(index){
				case 0:
					text = "Clone group " + group.getCloneGroupID() + " (" + group.getCloneGroupSize() + " clone instances)";
					break;
				case 2:
					if (group.isSubClone()) {
						text = "Subclone of clone group " + String.valueOf(group.getSubcloneOf().getCloneGroupID());
						StyleRange myStyledRange = 
							new StyleRange(0, text.length(), Display.getCurrent().getSystemColor(SWT.COLOR_BLUE), null);
						myStyledRange.underline = true;
						styleRanges.add(myStyledRange);
					}
					else { 
						text = "";
					}
					break;
				default:
					text = "";
				}
			}
			
			if (styleRanges.size() > 0) {
				StyleRange[] range = new StyleRange[] {};
				range = styleRanges.toArray(range);
				cell.setStyleRanges(range);
			}
			
			cell.setText(text);
			super.update(cell);

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
		layout.addColumnData(new ColumnWeightData(40, true));
		layout.addColumnData(new ColumnWeightData(30, true));
		layout.addColumnData(new ColumnWeightData(15, true));
		
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
		
		TreeColumn column2 = new TreeColumn(treeViewer.getTree(),SWT.LEFT);
		column2.setText("Subclone Information");
		column2.setResizable(true);
		column2.pack();

		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				treeViewer.getTree().setMenu(null);
				CloneInstance[] selectedCloneInstances = getSelectedCloneInstances();
				if (selectedCloneInstances.length == 2) {
					treeViewer.getTree().setMenu(getRightClickMenu(treeViewer));
				}
			}
		});

		treeViewer.getTree().addListener(SWT.MouseDown, new Listener() {

			public void handleEvent(Event event) {
				Point pt = new Point(event.x, event.y);
				TreeItem item = treeViewer.getTree().getItem(pt);
				
				if (item == null)
					return;
				
				Object selectedItemData = item.getData();
				
				if (selectedItemData instanceof CloneGroup) {
				
					int selectedColumn = -1;
					for (int i = 0; i < treeViewer.getTree().getColumnCount(); i++) {
						Rectangle rect = item.getBounds(i);
						if (rect.contains(pt)) {
							selectedColumn = i;
							break;
						}
					}

					if (selectedColumn == 2) {
						CloneGroup selectedCloneGroup = (CloneGroup)selectedItemData;
						if (selectedCloneGroup.isSubClone()) {
							CloneGroup subCloneOf = selectedCloneGroup.getSubcloneOf();
							treeViewer.setSelection(new TreeSelection(new TreePath(new Object[] {subCloneOf})));
						}
					}

				}
			}

		});

		treeViewer.getTree().addListener(SWT.MouseMove, new Listener() {
			public void handleEvent(Event event) {
				if (treeViewer.getTree().getCursor() != null)
					treeViewer.getTree().setCursor(null);
				Point pt = new Point(event.x, event.y);
				TreeItem item = treeViewer.getTree().getItem(pt);
				if (item != null) {
					Object selectedItemData = item.getData();
					if (selectedItemData instanceof CloneGroup) {
						CloneGroup cloneGroup = (CloneGroup) selectedItemData;
						if (cloneGroup.isSubClone()) {
							Rectangle rect = item.getBounds(2);
							if (rect.contains(pt)) {
								Cursor cursor = new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
								treeViewer.getTree().setCursor(cursor);
							}	
						}
					}
				}
			}
		});

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
	
	private Menu getRightClickMenu(TreeViewer treeViewer) {
		Menu popupMenu = new Menu(treeViewer.getControl());
	    
		MenuItem textualDiffMenuItem = new MenuItem(popupMenu, SWT.NONE);
	    textualDiffMenuItem.setText("Show textual diff");
	    textualDiffMenuItem.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent arg0) {
				showCompareDialog();
			}
			
			public void widgetDefaultSelected(SelectionEvent arg0) {}
			
		});
	    
	    MenuItem refactorMenuItem = new MenuItem(popupMenu, SWT.NONE);
	    refactorMenuItem.setText("Refactor");
	    refactorMenuItem.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent arg0) {
				applyRefactoring();
			}
			
			public void widgetDefaultSelected(SelectionEvent arg0) {}
			
		});
	    popupMenu.setVisible(false);
	    
	    return popupMenu;
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
				applyRefactoring();
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
						@SuppressWarnings("unchecked")
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

	private void applyRefactoring() {
		try {
			CloneInstance[] selectedCloneInstances = getSelectedCloneInstances();
			
			if (selectedCloneInstances.length == 2) {
				
				final CloneInstance instance1 = selectedCloneInstances[0];
				final CloneInstance instance2 = selectedCloneInstances[1];
				
				CompilationUnitCache.getInstance().clearCache();
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
					
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private CloneInstance[] getSelectedCloneInstances() {
		CloneInstance[] toReturn = new CloneInstance[] {};
		IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
		if(selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection)selection;
			Object[] selectedItems = structuredSelection.toArray();
			if(selectedItems.length == 2) {
				if(selectedItems[0] instanceof CloneInstance && selectedItems[1] instanceof CloneInstance) {
					final CloneInstance instance1 = (CloneInstance) selectedItems[0];
					final CloneInstance instance2 = (CloneInstance) selectedItems[1];
					if(instance1.getBelongingCloneGroup().equals(instance2.getBelongingCloneGroup())) {
						return new CloneInstance[] {instance1, instance2 };
					}
				}
			}
		}
		return toReturn;
	}
	
	static class CompareInput extends CompareEditorInput {

		private final String left;
		private final String right;

		public CompareInput(String left, String right) {
			super(new CompareConfiguration());
			this.left = left;
			this.right = right;
		}

		@Override
		protected Object prepareInput(IProgressMonitor pm) {
			CompareItem firstText = new CompareItem("First", this.left);
			CompareItem secondText = new CompareItem("Second", this.right);
			
			return new DiffNode(firstText, secondText);
		}
		
	}

	static class CompareItem extends BufferedContent implements ITypedElement {
		 
		private final String name;
		private final String content;
		
		public CompareItem(String name, String content) {
			this.name = name;
			this.content = content;
		}
				 
		public Image getImage() {return null;}
		public String getName() {return name;}
		public String getType() {return ITypedElement.TEXT_TYPE;}
		
		@Override
		protected InputStream createStream() throws CoreException {
			return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
		}
	 
	}
	
	private void showCompareDialog() {
		
		CloneInstance[] selectedCloneInstances = getSelectedCloneInstances();
		
		if (selectedCloneInstances.length == 2) {
			
			final CloneInstance instance1 = selectedCloneInstances[0];
			final CloneInstance instance2 = selectedCloneInstances[1];
			
			CompareEditorInput input = new CompareInput(instance1.getActualCodeFragment(), instance2.getActualCodeFragment());
			CompareUI.openCompareDialog(input);
			
		} else {
			wrongSelectionMessage();
		}

	}
}
