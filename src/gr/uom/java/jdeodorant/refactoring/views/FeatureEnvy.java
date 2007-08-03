package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.distance.CandidateRefactoring;
import gr.uom.java.distance.ExtractAndMoveMethodCandidateRefactoring;
import gr.uom.java.distance.InitialSystem;
import gr.uom.java.distance.MoveMethodCandidateRefactoring;
import gr.uom.java.distance.DistanceMatrix;
import gr.uom.java.distance.MySystem;
import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractMethodRefactoring;
import gr.uom.java.jdeodorant.refactoring.manipulators.MoveMethodRefactoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.action.*;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.UndoEdit;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

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
	private IProject selectedProject;
	private CandidateRefactoring[] candidateRefactoringTable;
	private ASTReader astReader;
	private Map<IDocument, IFile> fileMap = new LinkedHashMap<IDocument, IFile>();
	private Map<IProject, Stack<Map<IDocument, UndoEdit>>> undoStackMap = new HashMap<IProject, Stack<Map<IDocument, UndoEdit>>>();

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
			//return PlatformUI.getWorkbench().
			//		getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
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
					selectedProject = javaProject.getProject();
					tableViewer.remove(candidateRefactoringTable);
					applyRefactoringAction.setEnabled(false);
					if(undoStackMap.containsKey(selectedProject)) {
						Stack<Map<IDocument, UndoEdit>> undoStack = undoStackMap.get(selectedProject);
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
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(selectionListener);
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				FeatureEnvy.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(tableViewer.getControl());
		tableViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, tableViewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(identifyBadSmellsAction);
		manager.add(new Separator());
		manager.add(applyRefactoringAction);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(identifyBadSmellsAction);
		manager.add(applyRefactoringAction);
		manager.add(undoRefactoringAction);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(identifyBadSmellsAction);
		manager.add(applyRefactoringAction);
		manager.add(undoRefactoringAction);
	}

	private void makeActions() {
		identifyBadSmellsAction = new Action() {
			public void run() {
				System.out.println("Run Detection executed");
				candidateRefactoringTable=getTable(selectedProject);
				tableViewer.setContentProvider(new ViewContentProvider());
				applyRefactoringAction.setEnabled(true);
			}
		};
		identifyBadSmellsAction.setToolTipText("Identify Bad Smells");
		identifyBadSmellsAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		
		applyRefactoringAction = new Action() {
			public void run() {
				System.out.println("Do Refactor executed");
				IStructuredSelection selection = (IStructuredSelection)tableViewer.getSelection();
				CandidateRefactoring entry = (CandidateRefactoring)selection.getFirstElement();
				if(entry instanceof MoveMethodCandidateRefactoring) {
					MoveMethodCandidateRefactoring candidate = (MoveMethodCandidateRefactoring)entry;
					IFile sourceFile = astReader.getFile(candidate.getSourceClassTypeDeclaration());
					IFile targetFile = astReader.getFile(candidate.getTargetClassTypeDeclaration());
					CompilationUnit sourceCompilationUnit = astReader.getCompilationUnit(candidate.getSourceClassTypeDeclaration());
					CompilationUnit targetCompilationUnit = astReader.getCompilationUnit(candidate.getTargetClassTypeDeclaration());
					
					List<String> excludedClasses = new ArrayList<String>();
					excludedClasses.add(candidate.getSourceClass().getClassObject().getName());
					excludedClasses.add(candidate.getTargetClass().getClassObject().getName());
					boolean leaveDelegate = astReader.getSystemObject().containsMethodInvocation(candidate.getSourceMethod().getMethodObject().generateMethodInvocation(), excludedClasses);
					
					MoveMethodRefactoring refactoring = new MoveMethodRefactoring(sourceFile, targetFile, sourceCompilationUnit, targetCompilationUnit,
						candidate.getSourceClassTypeDeclaration(), candidate.getTargetClassTypeDeclaration(), candidate.getSourceMethodDeclaration(), leaveDelegate);
					ITextEditor targetEditor = null;
					ITextEditor sourceEditor = null;
					try {
						targetEditor = (ITextEditor)EditorUtility.openInEditor(targetFile);
						sourceEditor = (ITextEditor)EditorUtility.openInEditor(sourceFile);
					} catch (PartInitException e) {
						e.printStackTrace();
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
					refactoring.apply();
					sourceEditor.doSave(null);
					targetEditor.doSave(null);
					fileMap.put(refactoring.getDocument(sourceFile), sourceFile);
					fileMap.put(refactoring.getDocument(targetFile), targetFile);
					if(undoStackMap.containsKey(selectedProject)) {
						Stack<Map<IDocument, UndoEdit>> undoStack = undoStackMap.get(selectedProject);
						undoStack.push(refactoring.getUndoEditMap());
					}
					else {
						Stack<Map<IDocument, UndoEdit>> undoStack = new Stack<Map<IDocument, UndoEdit>>();
						undoStack.push(refactoring.getUndoEditMap());
						undoStackMap.put(selectedProject, undoStack);
					}
					undoRefactoringAction.setEnabled(true);
				}
				else if(entry instanceof ExtractAndMoveMethodCandidateRefactoring) {
					ExtractAndMoveMethodCandidateRefactoring candidate = (ExtractAndMoveMethodCandidateRefactoring)entry;
					IFile sourceFile = astReader.getFile(candidate.getSourceClassTypeDeclaration());
				    ExtractMethodRefactoring refactoring = new ExtractMethodRefactoring(sourceFile, candidate.getSourceClassTypeDeclaration(), candidate.getSourceMethodDeclaration(), 
				    	candidate.getVariableDeclarationStatement(), candidate.getVariableDeclarationFragment(), candidate.getStatementList(), candidate.getVariableDeclarationStatements());
				    refactoring.extractMethod();
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
				CandidateRefactoring entry = (CandidateRefactoring)selection.getFirstElement();
				System.out.println("Double-click detected on "+entry.toString());
				if(entry instanceof MoveMethodCandidateRefactoring) {
					MoveMethodCandidateRefactoring candidate = (MoveMethodCandidateRefactoring)entry;
					IFile sourceFile = astReader.getFile(candidate.getSourceClassTypeDeclaration());
					IFile targetFile = astReader.getFile(candidate.getTargetClassTypeDeclaration());
					
					try {
						ITextEditor targetEditor = (ITextEditor)EditorUtility.openInEditor(targetFile);
						ITextEditor sourceEditor = (ITextEditor)EditorUtility.openInEditor(sourceFile);
						sourceEditor.selectAndReveal(candidate.getSourceMethodDeclaration().getStartPosition(), candidate.getSourceMethodDeclaration().getLength());
					} catch (PartInitException e) {
						e.printStackTrace();
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
				else if(entry instanceof ExtractAndMoveMethodCandidateRefactoring) {
					ExtractAndMoveMethodCandidateRefactoring candidate = (ExtractAndMoveMethodCandidateRefactoring)entry;
					IFile sourceFile = astReader.getFile(candidate.getSourceClassTypeDeclaration());
					
					try {
						ITextEditor sourceEditor = (ITextEditor)EditorUtility.openInEditor(sourceFile);
						List<Statement> statementList = candidate.getStatementList();
						int startPosition = statementList.get(0).getStartPosition();
						Statement lastStatement = statementList.get(statementList.size()-1);
						int endPosition = lastStatement.getStartPosition() + lastStatement.getLength();
						int length = endPosition - startPosition;
						sourceEditor.selectAndReveal(startPosition, length);
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
				System.out.println("Undo executed");
				if(undoStackMap.containsKey(selectedProject)) {
					Stack<Map<IDocument, UndoEdit>> undoStack = undoStackMap.get(selectedProject);
					if(!undoStack.empty()) {
						Map<IDocument, UndoEdit> undoEditMap = undoStack.pop();
						Set<IDocument> keySet = undoEditMap.keySet();
						for(IDocument key : keySet){
							UndoEdit undoEdit = undoEditMap.get(key);
							try {
								undoEdit.apply(key);
								ITextEditor editor = (ITextEditor)EditorUtility.openInEditor(fileMap.get(key));
								editor.doSave(null);
							} catch (MalformedTreeException e) {
								e.printStackTrace();
							} catch (BadLocationException e) {
								e.printStackTrace();
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
	
	public CandidateRefactoring[] getTable(IProject iProject){
		astReader = new ASTReader(iProject);
		SystemObject systemObject = astReader.getSystemObject();
		MySystem system = new MySystem(systemObject);
		DistanceMatrix distanceMatrix = new DistanceMatrix(system);

		List<MoveMethodCandidateRefactoring> moveMethodCandidateList = distanceMatrix.getMoveMethodCandidateRefactorings();
		List<ExtractAndMoveMethodCandidateRefactoring> extractMethodCandidateList = distanceMatrix.getExtractAndMoveMethodCandidateRefactorings();
		
		CandidateRefactoring[] table = new CandidateRefactoring[moveMethodCandidateList.size() + extractMethodCandidateList.size() + 1];
		
		table[0] = new InitialSystem(distanceMatrix);
		int counter = 1;
		for(ExtractAndMoveMethodCandidateRefactoring candidate : extractMethodCandidateList) {
			table[counter] = candidate;
			counter++;
		}
		
		for(MoveMethodCandidateRefactoring candidate : moveMethodCandidateList) {
			table[counter] = candidate;
			counter++;
		}
		return table;
		
	}
	/*
	private IResource extractSelection(ISelection sel) {
	      if (!(sel instanceof IStructuredSelection))
	         return null;
	      IStructuredSelection ss = (IStructuredSelection) sel;
	      Object element =ss.getFirstElement();
	      return (IResource) element;
	   }
	
	private IProject getSelectedProject(){
		IWorkbenchPage page=this.getViewSite().getPage();
		ISelection selection=page.getSelection(IPageLayout.ID_RES_NAV);
		IResource res=extractSelection(selection);
		IProject iproject=res.getProject();
		//IPath ilocation=iproject.getLocation();
		//String location =ilocation.toOSString();
		//return new File(location);
		return iproject;
	}
	*/
}