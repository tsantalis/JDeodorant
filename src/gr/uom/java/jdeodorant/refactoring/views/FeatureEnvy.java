package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.LCOM;
import gr.uom.java.ast.MMImportCoupling;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.distance.CandidateRefactoring;
import gr.uom.java.distance.ExtractAndMoveMethodCandidateRefactoring;
import gr.uom.java.distance.CurrentSystem;
import gr.uom.java.distance.MoveMethodCandidateRefactoring;
import gr.uom.java.distance.DistanceMatrix;
import gr.uom.java.distance.MySystem;
import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractAndMoveMethodRefactoring;
import gr.uom.java.jdeodorant.refactoring.manipulators.MoveMethodRefactoring;
import gr.uom.java.jdeodorant.refactoring.manipulators.Refactoring;
import gr.uom.java.jdeodorant.refactoring.manipulators.UndoRefactoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.part.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.ui.JavaUI;

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
	private Action undoRefactoringAction;
	private Action doubleClickAction;
	private Action renameMethodAction;
	private IProject selectedProject;
	private CandidateRefactoring[] candidateRefactoringTable;
	private ASTReader astReader;
	private Map<IProject, Stack<UndoRefactoring>> undoStackMap = new HashMap<IProject, Stack<UndoRefactoring>>();

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
				else if(entry instanceof ExtractAndMoveMethodCandidateRefactoring)
					return "Extract and Move Method";
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
	class NameSorter extends ViewerSorter{
		public int compare(Viewer viewer, Object obj1, Object obj2) {
			double value1 = ((CandidateRefactoring)obj1).getEntityPlacement();
			double value2 = ((CandidateRefactoring)obj2).getEntityPlacement();
			if(value1<value2) {
				return -1;
			}
			else if(value1>value2) {
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
				if(element instanceof IJavaProject) {
					IJavaProject javaProject = (IJavaProject)element;
					if(!javaProject.getProject().equals(selectedProject)) {
						selectedProject = javaProject.getProject();
						if(candidateRefactoringTable != null)
							tableViewer.remove(candidateRefactoringTable);
						identifyBadSmellsAction.setEnabled(true);
						applyRefactoringAction.setEnabled(false);
						renameMethodAction.setEnabled(false);
						if(undoStackMap.containsKey(selectedProject)) {
							Stack<UndoRefactoring> undoStack = undoStackMap.get(selectedProject);
							if(undoStack.empty())
								undoRefactoringAction.setEnabled(false);
							else
								undoRefactoringAction.setEnabled(true);
						}
						else
							undoRefactoringAction.setEnabled(false);
					}
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
		final TooltipLabelListener tooltipLabelListener = new TooltipLabelListener();
		Listener tableListener = new Listener() {
			Shell tooltip = null;
			Label label = null;
			public void handleEvent(Event event) {
				switch(event.type) {
					case SWT.KeyDown:
					case SWT.Dispose:
					case SWT.MouseMove: {
						if(tooltip == null)
							break;
						tooltip.dispose();
						tooltip = null;
						label = null;
						break;
					}
					case SWT.MouseHover: {
						Table table = tableViewer.getTable();
						Point coords = new Point(event.x, event.y);
						TableItem item = table.getItem(coords);
						if(item != null) {
							List<CandidateRefactoring> prerequisiteRefactorings = getPrerequisiteRefactorings((CandidateRefactoring)item.getData());
							if(!prerequisiteRefactorings.isEmpty()) {
								int columnCount = table.getColumnCount();
								for(int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
									if(item.getBounds(columnIndex).contains(coords)) {
										if(tooltip != null && !tooltip.isDisposed())
											tooltip.dispose();
										tooltip = new Shell(table.getShell(), SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
										tooltip.setBackground(table.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
										FillLayout layout = new FillLayout();
										layout.marginWidth = 2;
										tooltip.setLayout(layout);
										label = new Label(tooltip, SWT.NONE);
										label.setForeground(table.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
										label.setBackground(table.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
										label.setData("_TableItem_", item);
										label.setText("APPLY FIRST " + prerequisiteRefactorings.get(0));
										label.addListener(SWT.MouseExit, tooltipLabelListener);
										label.addListener(SWT.MouseDown, tooltipLabelListener);
										Point size = tooltip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
										Rectangle rect = item.getBounds(columnIndex);
										Point pt = table.toDisplay(rect.x, rect.y);
										tooltip.setBounds(pt.x, pt.y, size.x, size.y);
										tooltip.setVisible(true);
										break;
									}
								}
							}
						}
					}
				}
			}
		};
		tableViewer.getTable().addListener(SWT.Dispose, tableListener);
		tableViewer.getTable().addListener(SWT.KeyDown, tableListener);
		tableViewer.getTable().addListener(SWT.MouseMove, tableListener);
		tableViewer.getTable().addListener(SWT.MouseHover, tableListener);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(identifyBadSmellsAction);
		manager.add(applyRefactoringAction);
		manager.add(undoRefactoringAction);
		manager.add(renameMethodAction);
	}

	private void makeActions() {
		identifyBadSmellsAction = new Action() {
			public void run() {
				candidateRefactoringTable = getTable(selectedProject);
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
				IFile sourceFile = null;
				IFile targetFile = null;
				Refactoring refactoring = null;
				if(entry instanceof MoveMethodCandidateRefactoring) {
					MoveMethodCandidateRefactoring candidate = (MoveMethodCandidateRefactoring)entry;
					sourceFile = astReader.getFile(candidate.getSourceClassTypeDeclaration());
					targetFile = astReader.getFile(candidate.getTargetClassTypeDeclaration());
					CompilationUnit sourceCompilationUnit = astReader.getCompilationUnit(candidate.getSourceClassTypeDeclaration());
					CompilationUnit targetCompilationUnit = astReader.getCompilationUnit(candidate.getTargetClassTypeDeclaration());
					
					List<ClassObject> excludedClasses = new ArrayList<ClassObject>();
					excludedClasses.add(candidate.getSourceClass().getClassObject());
					excludedClasses.add(candidate.getTargetClass().getClassObject());
					boolean leaveDelegate = astReader.getSystemObject().containsMethodInvocation(candidate.getSourceMethod().getMethodObject().generateMethodInvocation(), excludedClasses);
					
					refactoring = new MoveMethodRefactoring(sourceFile, targetFile, sourceCompilationUnit, targetCompilationUnit,
						candidate.getSourceClassTypeDeclaration(), candidate.getTargetClassTypeDeclaration(), candidate.getSourceMethodDeclaration(), candidate.getAdditionalMethodsToBeMoved(), leaveDelegate);
				}
				else if(entry instanceof ExtractAndMoveMethodCandidateRefactoring) {
					ExtractAndMoveMethodCandidateRefactoring candidate = (ExtractAndMoveMethodCandidateRefactoring)entry;
					sourceFile = astReader.getFile(candidate.getSourceClassTypeDeclaration());
					targetFile = astReader.getFile(candidate.getTargetClassTypeDeclaration());
					CompilationUnit sourceCompilationUnit = astReader.getCompilationUnit(candidate.getSourceClassTypeDeclaration());
					CompilationUnit targetCompilationUnit = astReader.getCompilationUnit(candidate.getTargetClassTypeDeclaration());
					
					refactoring = new ExtractAndMoveMethodRefactoring(sourceFile, targetFile, sourceCompilationUnit, targetCompilationUnit,
						candidate.getSourceClassTypeDeclaration(), candidate.getTargetClassTypeDeclaration(), candidate.getSourceMethodDeclaration(), candidate.getASTExtractionBlock());
				}
				IEditorPart targetEditor = null;
				IEditorPart sourceEditor = null;
				try {
					IJavaElement targetJavaElement = JavaCore.create(targetFile);
					targetEditor = JavaUI.openInEditor(targetJavaElement);
					IJavaElement sourceJavaElement = JavaCore.create(sourceFile);
					sourceEditor = JavaUI.openInEditor(sourceJavaElement);
				} catch (PartInitException e) {
					e.printStackTrace();
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
				refactoring.apply();
				sourceEditor.doSave(null);
				targetEditor.doSave(null);
				UndoRefactoring undoRefactoring = refactoring.getUndoRefactoring();
				if(undoStackMap.containsKey(selectedProject)) {
					Stack<UndoRefactoring> undoStack = undoStackMap.get(selectedProject);
					undoStack.push(undoRefactoring);
				}
				else {
					Stack<UndoRefactoring> undoStack = new Stack<UndoRefactoring>();
					undoStack.push(undoRefactoring);
					undoStackMap.put(selectedProject, undoStack);
				}
				applyRefactoringAction.setEnabled(false);
				undoRefactoringAction.setEnabled(true);
			}
		};
		applyRefactoringAction.setToolTipText("Apply Refactoring");
		applyRefactoringAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_DEF_VIEW));
		applyRefactoringAction.setEnabled(false);
		
		doubleClickAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)tableViewer.getSelection();
				CandidateRefactoring entry = (CandidateRefactoring)selection.getFirstElement();
				if(entry instanceof MoveMethodCandidateRefactoring) {
					MoveMethodCandidateRefactoring candidate = (MoveMethodCandidateRefactoring)entry;
					IFile sourceFile = astReader.getFile(candidate.getSourceClassTypeDeclaration());
					IFile targetFile = astReader.getFile(candidate.getTargetClassTypeDeclaration());
					try {
						IJavaElement targetJavaElement = JavaCore.create(targetFile);
						JavaUI.openInEditor(targetJavaElement);
						IJavaElement sourceJavaElement = JavaCore.create(sourceFile);
						ITextEditor sourceEditor = (ITextEditor)JavaUI.openInEditor(sourceJavaElement);
						AnnotationModel annotationModel = (AnnotationModel)sourceEditor.getDocumentProvider().getAnnotationModel(sourceEditor.getEditorInput());
						Iterator<Annotation> annotationIterator = annotationModel.getAnnotationIterator();
						while(annotationIterator.hasNext()) {
							Annotation currentAnnotation = annotationIterator.next();
							if(currentAnnotation.getType().equals("org.eclipse.jdt.ui.occurrences")) {
								annotationModel.removeAnnotation(currentAnnotation);
							}
						}
						Annotation annotation = new Annotation("org.eclipse.jdt.ui.occurrences", false, candidate.getSourceEntity());
						Position position = new Position(candidate.getSourceMethodDeclaration().getStartPosition(), candidate.getSourceMethodDeclaration().getLength());
						annotationModel.addAnnotation(annotation, position);
						sourceEditor.setHighlightRange(candidate.getSourceMethodDeclaration().getStartPosition(), candidate.getSourceMethodDeclaration().getLength(), true);
					} catch (PartInitException e) {
						e.printStackTrace();
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
				else if(entry instanceof ExtractAndMoveMethodCandidateRefactoring) {
					ExtractAndMoveMethodCandidateRefactoring candidate = (ExtractAndMoveMethodCandidateRefactoring)entry;
					IFile sourceFile = astReader.getFile(candidate.getSourceClassTypeDeclaration());
					IFile targetFile = astReader.getFile(candidate.getTargetClassTypeDeclaration());
					try {
						IJavaElement targetJavaElement = JavaCore.create(targetFile);
						JavaUI.openInEditor(targetJavaElement);
						IJavaElement sourceJavaElement = JavaCore.create(sourceFile);
						ITextEditor sourceEditor = (ITextEditor)JavaUI.openInEditor(sourceJavaElement);
						List<Statement> statementList = candidate.getStatementsForExtraction();
						AnnotationModel annotationModel = (AnnotationModel)sourceEditor.getDocumentProvider().getAnnotationModel(sourceEditor.getEditorInput());
						Iterator<Annotation> annotationIterator = annotationModel.getAnnotationIterator();
						while(annotationIterator.hasNext()) {
							Annotation currentAnnotation = annotationIterator.next();
							if(currentAnnotation.getType().equals("org.eclipse.jdt.ui.occurrences")) {
								annotationModel.removeAnnotation(currentAnnotation);
							}
						}
						for(Statement statement : statementList) {
							Annotation annotation = new Annotation("org.eclipse.jdt.ui.occurrences", false, candidate.getSourceEntity());
							Position position = new Position(statement.getStartPosition(), statement.getLength());
							annotationModel.addAnnotation(annotation, position);
						}
						Statement firstStatement = statementList.get(0);
						Statement lastStatement = statementList.get(statementList.size()-1);
						int offset = firstStatement.getStartPosition();
						int length = lastStatement.getStartPosition() + lastStatement.getLength() - firstStatement.getStartPosition();
						sourceEditor.setHighlightRange(offset, length, true);
					} catch (PartInitException e) {
						e.printStackTrace();
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
			}
		};
		
		undoRefactoringAction = new Action() {
			public void run() {
				if(undoStackMap.containsKey(selectedProject)) {
					Stack<UndoRefactoring> undoStack = undoStackMap.get(selectedProject);
					if(!undoStack.empty()) {
						UndoRefactoring undoRefactoring = undoStack.pop();
						undoRefactoring.apply();
						Set<IFile> fileKeySet = undoRefactoring.getFileKeySet();
						for(IFile key : fileKeySet) {
							try {
								IJavaElement iJavaElement = JavaCore.create(key);
								IEditorPart editor = JavaUI.openInEditor(iJavaElement);
								editor.doSave(null);
							} catch (PartInitException e) {
								e.printStackTrace();
							} catch (JavaModelException e) {
								e.printStackTrace();
							}
						}
						if(undoStack.empty())
							undoRefactoringAction.setEnabled(false);
					}
				}
			}
		};
		undoRefactoringAction.setToolTipText("Undo Refactoring");
		undoRefactoringAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_TOOL_UNDO));
		undoRefactoringAction.setEnabled(false);
		
		renameMethodAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)tableViewer.getSelection();
				CandidateRefactoring entry = (CandidateRefactoring)selection.getFirstElement();
				if(entry instanceof ExtractAndMoveMethodCandidateRefactoring) {
					ExtractAndMoveMethodCandidateRefactoring candidate = (ExtractAndMoveMethodCandidateRefactoring)entry;
					String methodName = candidate.getExtractionBlock().getExtractedMethodName();
					IInputValidator methodNameValidator = new IInputValidator() {
						public String isValid(String text) {
							String pattern = "[a-zA-Z\\$_][a-zA-Z0-9\\$_]*";
							if(Pattern.matches(pattern, text)) {
								return null;
							}
							else {
								return "Invalid method name";
							}
						}
					};
					InputDialog dialog = new InputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Rename Method", "Please enter a new name", methodName, methodNameValidator);
					dialog.open();
					if(dialog.getValue() != null) {
						candidate.getExtractionBlock().setExtractedMethodName(dialog.getValue());
						tableViewer.refresh();
					}
				}
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
				else if(candidate instanceof ExtractAndMoveMethodCandidateRefactoring) {
					ExtractAndMoveMethodCandidateRefactoring extractCandidate = (ExtractAndMoveMethodCandidateRefactoring)candidate;
					if(extractCandidate.isSubRefactoringOf(candidateRefactoring))
						extractMethodPrerequisiteRefactorings.add(candidate);
				}
			}
		}
		if(!moveMethodPrerequisiteRefactorings.isEmpty())
			return moveMethodPrerequisiteRefactorings;
		else
			return extractMethodPrerequisiteRefactorings;
	}
	
	private CandidateRefactoring[] getTable(IProject iProject) {
		astReader = new ASTReader(iProject);
		SystemObject systemObject = astReader.getSystemObject();
		MMImportCoupling mmic = new MMImportCoupling(systemObject);
		System.out.println("System Average MMIC: " + mmic.getSystemAverageCoupling());
		LCOM lcom = new LCOM(systemObject);
		System.out.println("System Average LCOM2: " + lcom.getSystemAverageLCOM2());
		System.out.println("System Average LCOM3: " + lcom.getSystemAverageLCOM3());
		MySystem system = new MySystem(systemObject);
		DistanceMatrix distanceMatrix = new DistanceMatrix(system);

		List<MoveMethodCandidateRefactoring> moveMethodCandidateList = distanceMatrix.getMoveMethodCandidateRefactorings();
		List<ExtractAndMoveMethodCandidateRefactoring> extractMethodCandidateList = distanceMatrix.getExtractAndMoveMethodCandidateRefactorings();
		
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
		}
		
		CandidateRefactoring[] table = new CandidateRefactoring[moveMethodCandidateList.size() + finalExtractMethodCandidateList.size() + 1];
		table[0] = new CurrentSystem(distanceMatrix);
		int counter = 1;
		for(ExtractAndMoveMethodCandidateRefactoring candidate : finalExtractMethodCandidateList) {
			table[counter] = candidate;
			counter++;
		}
		for(MoveMethodCandidateRefactoring candidate : moveMethodCandidateList) {
			table[counter] = candidate;
			counter++;
		}
		return table;		
	}
	
	final class TooltipLabelListener implements Listener {
		private boolean isCTRLDown(Event e) {
			return (e.stateMask & SWT.CTRL) != 0;
		}
		public void handleEvent(Event event) {
			Label label = (Label)event.widget;
			Shell shell = label.getShell();
			switch(event.type) {
				case SWT.MouseDown:
					Event e = new Event();
					e.item = (TableItem)label.getData("_TableItem_");
					Table table = ((TableItem)e.item).getParent();
					TableItem[] newSelection = null;
					if (isCTRLDown(event)) {
						TableItem[] sel = table.getSelection();
						for(int i = 0; i < sel.length; ++i) {
							if(e.item.equals(sel[i])) {
								newSelection = new TableItem[sel.length - 1];
								System.arraycopy(sel, 0, newSelection, 0, i);
								System.arraycopy(sel, i+1, newSelection, i, sel.length - i - 1);
								break;
							}
						}
						if(newSelection == null) {
							newSelection = new TableItem[sel.length + 1];
							System.arraycopy(sel, 0, newSelection, 0, sel.length);
							newSelection[sel.length] = (TableItem)e.item;
						}
					}
					else {
						newSelection = new TableItem[] { (TableItem) e.item };
					}
					table.setSelection(newSelection);
					table.notifyListeners(SWT.Selection, e);
					shell.dispose();
					table.setFocus();
					break;
				case SWT.MouseExit:
					shell.dispose();
					break;
			}
		}
	}
}