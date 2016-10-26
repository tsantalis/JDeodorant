package gr.uom.java.jdeodorant.refactoring.views;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
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
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
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
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
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
import ca.concordia.jdeodorant.clone.parsers.CloneInstanceStatus;
import ca.concordia.jdeodorant.clone.parsers.JavaModelUtility;
import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.CompilationErrorDetectedException;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.decomposition.cfg.mapping.CloneInstanceMapper;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGRegionSubTreeMapper;
import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractCloneRefactoring;

public class DuplicatedCode extends ViewPart {
	private static final String MESSAGE_DIALOG_TITLE = "Duplicated Code Refactoring";
	private TreeViewer treeViewer;
	private Action importClonesAction;
	private Action doubleClickAction;
	private IJavaProject selectedProject;
	private CloneGroupList cloneGroupList;
	private CloneInstanceMapper mapper;
	private boolean filterBasedOnOpenedDocuments;
	
	private static final Color LINK_COLOR = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
	private static final Color TEXT_COLOR = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
	private static final Color MODIFIED_BG_COLOR = new Color(Display.getCurrent(), 218, 255, 215);
	
	class ViewContentProvider implements ITreeContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {

			CloneGroup[] cloneGroupTable = null;

			if (cloneGroupList != null) {
				if (filterBasedOnOpenedDocuments) {

					Set<String> locationOfOpenedFiles = new HashSet<String>();

					IEditorReference[] editorReferences = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
					for (IEditorReference editorReference : editorReferences) {
						try {
							IEditorInput editorInput = editorReference.getEditorInput();
							if (editorInput instanceof IFileEditorInput) {
								IFileEditorInput iFileEditorInput = (IFileEditorInput) editorInput;
								locationOfOpenedFiles.add(iFileEditorInput.getFile().getLocation().toPortableString());
							}
						} catch (PartInitException e) {
							e.printStackTrace();
						}
					}

					CloneGroupList filteredCloneGroupList = new CloneGroupList(selectedProject); 						
					for (CloneGroup cloneGroup : cloneGroupList.getCloneGroups()) {
						CloneGroup filteredCloneGroup = new CloneGroup(cloneGroup.getCloneGroupID());
						for (CloneInstance cloneInstance : cloneGroup.getCloneInstances()) {
							String cloneInstanceFilePath = cloneInstance.getLocationInfo().getContainingFilePath();
							if (locationOfOpenedFiles.contains(new Path(cloneInstanceFilePath).toPortableString())) {
								filteredCloneGroup.addClone(cloneInstance);
							}
						}
						if (filteredCloneGroup.getCloneGroupSize() > 0) {
							filteredCloneGroupList.add(cloneGroup);
						}
					}
					cloneGroupTable = filteredCloneGroupList.getCloneGroups();
				} else {
					cloneGroupTable = cloneGroupList.getCloneGroups();
				}
			}
			
			if(cloneGroupTable != null) {
				return cloneGroupTable;
			} else {
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
			CloneGroup[] cloneGroupTable = cloneGroupList.getCloneGroups();
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
				switch (cloneInstance.getStatus()) {
				case OFFSETS_SHIFTED:
					StyleRange myStyledRange = new StyleRange(0, text.length(), TEXT_COLOR, MODIFIED_BG_COLOR);
					styleRanges.add(myStyledRange);
					break;
				case TAMPERED:
					myStyledRange = new StyleRange(0, text.length(), TEXT_COLOR, null);
					myStyledRange.strikeout = true;
					styleRanges.add(myStyledRange);
					break;
				default:
					break;
				} 
			}
			else if(obj instanceof CloneGroup) {
				CloneGroup group = (CloneGroup)obj;
				if (group.isUpdated()) {
					cell.setBackground(MODIFIED_BG_COLOR);
				} else {
					cell.setBackground(null);
				}
				switch(index){
				case 0:
					text = "Clone group " + group.getCloneGroupID() + " (" + group.getCloneGroupSize() + " clone instances)";
					break;
				case 2:
					if (group.isSubClone()) {
						text = "Subclone of clone group " + String.valueOf(group.getSubcloneOf().getCloneGroupID());
						if (!group.getSubcloneOf().containsClassLevelClone()) {
							StyleRange myStyledRange = new StyleRange(0, text.length(), LINK_COLOR, null);
							myStyledRange.underline = true;
							styleRanges.add(myStyledRange);
						}
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
			} else {
				cell.setStyleRanges(null);
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
				}
			}
		}
	};

	private IElementChangedListener elementChangedListener = new IElementChangedListener() {
		public void elementChanged(ElementChangedEvent event) {
			final IJavaElementDelta delta = event.getDelta();
			Display.getDefault().syncExec(new Runnable() {
			    public void run() {
			    	processDelta(delta);
			    }
			});
		}
		private void processDelta(IJavaElementDelta delta) {
			boolean shouldUpdate = false;
			IJavaElement javaElement = delta.getElement();
			switch(javaElement.getElementType()) {
			case IJavaElement.JAVA_MODEL:
			case IJavaElement.JAVA_PROJECT:
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
			case IJavaElement.PACKAGE_FRAGMENT:
				IJavaElementDelta[] affectedChildren = delta.getAffectedChildren();
				for(IJavaElementDelta affectedChild : affectedChildren) {
					processDelta(affectedChild);
				}
				break;
			case IJavaElement.COMPILATION_UNIT:
				ICompilationUnit compilationUnit = (ICompilationUnit)javaElement;
				String pathOfJavaElement = compilationUnit.getResource().getLocation().toOSString();
				if(delta.getKind() == IJavaElementDelta.REMOVED) {
					if (cloneGroupList != null) {
						shouldUpdate = cloneGroupList.removeClonesExistingInFile(pathOfJavaElement);
					}
				}
				else if (delta.getKind() == IJavaElementDelta.ADDED) {
					String newSourceCode = JavaModelUtility.getIDocument(javaElement).get();
					if (cloneGroupList != null ) {
						shouldUpdate = cloneGroupList.updateClonesExistingInFile(pathOfJavaElement, newSourceCode);
					}
				}
				else if(delta.getKind() == IJavaElementDelta.CHANGED) {
					if((delta.getFlags() & IJavaElementDelta.F_FINE_GRAINED) != 0) {
						String newSourceCode = JavaModelUtility.getIDocument(javaElement).get();
						if (cloneGroupList != null ) {
							shouldUpdate = cloneGroupList.updateClonesExistingInFile(pathOfJavaElement, newSourceCode);
						}
					}
				}
			}
			if (shouldUpdate) {
				treeViewer.refresh();
			}
		}
	};

	@Override
	public void createPartControl(Composite parent) {
		GridLayout gridLayout = new GridLayout();
	    gridLayout.numColumns = 1;		
	    parent.setLayout(gridLayout);
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
						if (cloneGroup.isSubClone() && !cloneGroup.getSubcloneOf().containsClassLevelClone()) {
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

		ColumnViewerToolTipSupport.enableFor(treeViewer, ToolTip.NO_RECREATE);
		
		treeViewer.expandAll();
		
		makeActions();
		hookDoubleClickAction();
		contributeToActionBars();
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(selectionListener);
		JavaCore.addElementChangedListener(ElementChangedListener.getInstance());
		JavaCore.addElementChangedListener(elementChangedListener);
		getSite().getWorkbenchWindow().getWorkbench().getOperationSupport().getOperationHistory().addOperationHistoryListener(new IOperationHistoryListener() {
			public void historyNotification(OperationHistoryEvent event) {
				int eventType = event.getEventType();
				if(eventType == OperationHistoryEvent.UNDONE  || eventType == OperationHistoryEvent.REDONE ||
						eventType == OperationHistoryEvent.OPERATION_ADDED || eventType == OperationHistoryEvent.OPERATION_REMOVED) {
					//
				}
			}
		});
		
		Composite bottomBar = new Composite(parent, SWT.NONE);
		bottomBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		bottomBar.setLayout(new GridLayout(3, true));
		
		createDetectionSettingsPanel(bottomBar);
		createLegend(bottomBar);
	}

	private void createDetectionSettingsPanel(Composite bottomBar) {
		final Group detectionSettingsPanel = new Group(bottomBar, SWT.SHADOW_NONE);
		detectionSettingsPanel.setText("Filtering settings");
		detectionSettingsPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridLayout detectionSettingsPanelLayout = new GridLayout();
		detectionSettingsPanelLayout.numColumns = 2;
		detectionSettingsPanelLayout.horizontalSpacing = 20;
		detectionSettingsPanel.setLayout(detectionSettingsPanelLayout);
		
		final Button filterBasedOnOpenedDocumentsButton;
		if(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
			filterBasedOnOpenedDocumentsButton = new MultilineButton(detectionSettingsPanel, SWT.WRAP | SWT.CHECK);
		}
		else {
			filterBasedOnOpenedDocumentsButton = new Button(detectionSettingsPanel, SWT.WRAP | SWT.CHECK);
		}
		filterBasedOnOpenedDocumentsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				filterBasedOnOpenedDocuments = filterBasedOnOpenedDocumentsButton.getSelection();
				treeViewer.refresh();
			}
		});
		filterBasedOnOpenedDocumentsButton.setText("Show only clone groups for the files opened in the editor");
		filterBasedOnOpenedDocumentsButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 1, 1));
	}

	private void createLegend(Composite parent) {
		final Group legendGroup = new Group(parent, SWT.SHADOW_NONE);
		legendGroup.setText("Legend");
		GridData legendGridData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		legendGroup.setLayoutData(legendGridData);
		GridLayout legendLayout = new GridLayout();
		legendLayout.numColumns = 6;
		legendLayout.horizontalSpacing = 20;
		legendGroup.setLayout(legendLayout);
		
		Composite modifiedLegendColorComposite = new Composite(legendGroup, SWT.NONE);
		modifiedLegendColorComposite.setLayout(new GridLayout(2, false));
		CLabel modifiedLegendColor = new CLabel(modifiedLegendColorComposite, SWT.BORDER);
		modifiedLegendColor.setText("      ");
		modifiedLegendColor.setMargins(0, 0, 0, 0);
		modifiedLegendColor.setBackground(MODIFIED_BG_COLOR);
	    GridData layoutData = new GridData();
	    layoutData.heightHint = 18;
	    modifiedLegendColor.setLayoutData(layoutData);
		Label modifiedLegendLabel = new Label(modifiedLegendColorComposite, SWT.NONE);
		modifiedLegendLabel.setText("Updated clone group / shifted clone instance");
		
		Composite tamperedLegendComposite = new Composite(legendGroup, SWT.NONE);
		tamperedLegendComposite.setLayout(new GridLayout(2, false));
		StyledText tamperedLegend = new StyledText(tamperedLegendComposite, SWT.BORDER);
	    tamperedLegend.setEditable(false);
	    tamperedLegend.setEnabled(false);
	    layoutData = new GridData();
	    //layoutData.heightHint = 18;
	    tamperedLegend.setLayoutData(layoutData);
	    String tamperedText = "Clone instance";
		tamperedLegend.setText(tamperedText);
		StyleRange myStyledRange = new StyleRange(0, tamperedText.length(), TEXT_COLOR, null);
		myStyledRange.strikeout = true;
	    tamperedLegend.setStyleRange(myStyledRange);
	    Label tamperedLegendLabel = new Label(tamperedLegendComposite, SWT.NONE);
	    tamperedLegendLabel.setText("Eliminated / modified clone instance");
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
				CloneGroupList importedCloneGroupList = wizard.getCloneGroupList();
				if(importedCloneGroupList != null) {
					cloneGroupList = importedCloneGroupList;
					treeViewer.setContentProvider(new ViewContentProvider());
				}
			}
		};
		importClonesAction.setToolTipText("Import Clones");
		importClonesAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		importClonesAction.setEnabled(false);

		doubleClickAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
				if(selection.getFirstElement() instanceof CloneInstance) {
					CloneInstance cloneInstance = (CloneInstance)selection.getFirstElement();
					if (!cloneInstance.getStatus().equals(CloneInstanceStatus.TAMPERED)) {
						String fullName = cloneInstance.getPackageName().replace(".", "/") + "/" +
								cloneInstance.getClassName() + ".java";
						try {
							final IJavaProject importedProject = cloneGroupList.getJavaProject();
							ICompilationUnit sourceJavaElement = getICompilationUnit(importedProject, fullName);
							ITextEditor sourceEditor = (ITextEditor)JavaUI.openInEditor(sourceJavaElement);
							AnnotationModel annotationModel = (AnnotationModel)sourceEditor.getDocumentProvider().getAnnotationModel(sourceEditor.getEditorInput());
							Iterator<Annotation> annotationIterator = annotationModel.getAnnotationIterator();
							while(annotationIterator.hasNext()) {
								Annotation currentAnnotation = annotationIterator.next();
								if(currentAnnotation.getType().equals(SliceAnnotation.EXTRACTION) || currentAnnotation.getType().equals(SliceAnnotation.DUPLICATION)) {
									annotationModel.removeAnnotation(currentAnnotation);
								}
							}
							int offset = cloneInstance.getLocationInfo().getUpdatedStartOffset();
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
		JavaCore.removeElementChangedListener(elementChangedListener);
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
		MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), MESSAGE_DIALOG_TITLE,
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
				final IJavaProject importedProject = cloneGroupList.getJavaProject();
				if(ASTReader.getSystemObject() != null && importedProject.equals(ASTReader.getExaminedProject())) {
					new ASTReader(importedProject, ASTReader.getSystemObject(), null);
				}
				else {
					ps.busyCursorWhile(new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							try {
								new ASTReader(importedProject, monitor);
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
				if(ASTReader.getSystemObject() != null) {
					ps.busyCursorWhile(new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							mapper = new CloneInstanceMapper(instance1, instance2, importedProject, monitor);
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
						MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), MESSAGE_DIALOG_TITLE,
								"Unfortunatley, no refactoring opportunities were found.");
					}
					CompilationUnitCache.getInstance().releaseLock();
				}
			}
			else {
				wrongSelectionMessage();
			}
					
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (CompilationErrorDetectedException e) {
			MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), MESSAGE_DIALOG_TITLE,
					"Compilation errors were detected in the project. Fix the errors before using JDeodorant.");
		}
	}

	private CloneInstance[] getSelectedCloneInstances() {
		CloneInstance[] toReturn = new CloneInstance[] {};
		IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
		if(selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection)selection;
			Object[] selectedItems = structuredSelection.toArray();
			if (selectedItems.length == 2) {
				if(selectedItems[0] instanceof CloneInstance && selectedItems[1] instanceof CloneInstance) {
					final CloneInstance instance1 = (CloneInstance) selectedItems[0];
					final CloneInstance instance2 = (CloneInstance) selectedItems[1];
					if (instance1.getBelongingCloneGroup().equals(instance2.getBelongingCloneGroup()) &&
							!instance1.getStatus().equals(CloneInstanceStatus.TAMPERED) && !instance2.getStatus().equals(CloneInstanceStatus.TAMPERED)) {
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
			
			CompareEditorInput input = new CompareInput(instance1.getOriginalCodeFragment(), instance2.getOriginalCodeFragment());
			CompareUI.openCompareDialog(input);
			
		} else {
			wrongSelectionMessage();
		}

	}
}
