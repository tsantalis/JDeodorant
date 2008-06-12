package gr.uom.java.jdeodorant.refactoring.views;


import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.jdeodorant.refactoring.manipulators.Refactoring;
import gr.uom.java.jdeodorant.refactoring.manipulators.ReplaceConditionalWithPolymorphism;
import gr.uom.java.jdeodorant.refactoring.manipulators.ReplaceTypeCodeWithStateStrategy;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckElimination;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckEliminationResults;
import gr.uom.java.jdeodorant.refactoring.manipulators.UndoRefactoring;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.part.*;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
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

public class TypeChecking extends ViewPart {
	private TableViewer tableViewer;
	private Action identifyBadSmellsAction;
	private Action applyRefactoringAction;
	private Action undoRefactoringAction;
	private Action doubleClickAction;
	private Action renameMethodAction;
	private IProject selectedProject;
	private ASTReader astReader;
	private TypeCheckElimination[] typeCheckEliminationTable;
	private TypeCheckEliminationResults typeCheckEliminationResults;
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
			if(typeCheckEliminationTable != null) {
				return typeCheckEliminationTable;
			}
			else {
				return new TypeCheckElimination[] {};
			}
		}
	}
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			TypeCheckElimination typeCheckElimination = (TypeCheckElimination)obj;
			switch(index) {
				case 0:
					if(typeCheckElimination.getExistingInheritanceTree() == null)
						return "Replace Type Code with State/Strategy";
					else
						return "Replace Conditional with Polymorphism";
				case 1:
					return typeCheckElimination.toString();
				case 2:
					return typeCheckElimination.getAbstractMethodName();
				case 3:
					return Integer.toString(typeCheckEliminationResults.getSystemOccurrences(typeCheckElimination));
				case 4:
					return Integer.toString(typeCheckEliminationResults.getClassOccurrences(typeCheckElimination));
				case 5:
					return Double.toString(typeCheckEliminationResults.getAverageNumberOfStatements(typeCheckElimination));
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
			TypeCheckElimination typeCheckElimination1 = (TypeCheckElimination)obj1;
			TypeCheckElimination typeCheckElimination2 = (TypeCheckElimination)obj2;
			int positionOfGroup1 = typeCheckEliminationResults.getPositionOfTypeCheckEliminationGroup(typeCheckElimination1);
			int positionOfGroup2 = typeCheckEliminationResults.getPositionOfTypeCheckEliminationGroup(typeCheckElimination2);
			int classOccurrences1 = typeCheckEliminationResults.getClassOccurrences(typeCheckElimination1);
			int classOccurrences2 = typeCheckEliminationResults.getClassOccurrences(typeCheckElimination2);
			double averageNumberOfStatements1 = typeCheckEliminationResults.getAverageNumberOfStatements(typeCheckElimination1);
			double averageNumberOfStatements2 = typeCheckEliminationResults.getAverageNumberOfStatements(typeCheckElimination2);
			String refactoringName1 = typeCheckElimination1.toString();
			String refactoringName2 = typeCheckElimination2.toString();
			
			if(positionOfGroup1 > positionOfGroup2)
				return 1;
			if(positionOfGroup1 < positionOfGroup2)
				return -1;
			
			if(classOccurrences1 > classOccurrences2)
				return -1;
			if(classOccurrences1 < classOccurrences2)
				return 1;
			
			if(averageNumberOfStatements1 > averageNumberOfStatements2)
				return -1;
			else if(averageNumberOfStatements1 < averageNumberOfStatements2)
				return 1;
			
			return refactoringName1.compareTo(refactoringName2);	
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
						identifyBadSmellsAction.setEnabled(true);
						applyRefactoringAction.setEnabled(false);
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
		layout.addColumnData(new ColumnWeightData(50, true));
		layout.addColumnData(new ColumnWeightData(100, true));
		layout.addColumnData(new ColumnWeightData(30, true));
		layout.addColumnData(new ColumnWeightData(20, true));
		layout.addColumnData(new ColumnWeightData(20, true));
		layout.addColumnData(new ColumnWeightData(20, true));
		tableViewer.getTable().setLayout(layout);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		TableColumn column0 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column0.setText("Refactoring Type");
		column0.setResizable(true);
		column0.pack();
		TableColumn column1 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column1.setText("Type Checking Method");
		column1.setResizable(true);
		column1.pack();
		TableColumn column2 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column2.setText("Abstract Method Name");
		column2.setResizable(true);
		column2.pack();
		TableColumn column3 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column3.setText("System-Level Occurrences");
		column3.setResizable(true);
		column3.pack();
		TableColumn column4 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column4.setText("Class-Level Occurrences");
		column4.setResizable(true);
		column4.pack();
		TableColumn column5 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column5.setText("Average #statements per case");
		column5.setResizable(true);
		column5.pack();
		makeActions();
		hookDoubleClickAction();
		contributeToActionBars();
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(selectionListener);
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
				typeCheckEliminationTable = getTable(selectedProject);
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
				TypeCheckElimination typeCheckElimination = (TypeCheckElimination)selection.getFirstElement();
				Refactoring refactoring = null;
				IEditorPart sourceEditor = null;
				TypeDeclaration sourceTypeDeclaration = typeCheckElimination.getTypeCheckClass();
				CompilationUnit sourceCompilationUnit = astReader.getCompilationUnit(sourceTypeDeclaration);
				IFile sourceFile = astReader.getFile(sourceTypeDeclaration);
				if(typeCheckElimination.getExistingInheritanceTree() == null) {
					refactoring = new ReplaceTypeCodeWithStateStrategy(sourceFile, sourceCompilationUnit, sourceTypeDeclaration, typeCheckElimination);
				}
				else {
					refactoring = new ReplaceConditionalWithPolymorphism(sourceFile, sourceCompilationUnit, sourceTypeDeclaration, typeCheckElimination);
				}
				try {
					IJavaElement sourceJavaElement = JavaCore.create(sourceFile);
					sourceEditor = JavaUI.openInEditor(sourceJavaElement);
				} catch (PartInitException e) {
					e.printStackTrace();
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
				refactoring.apply();
				sourceEditor.doSave(null);
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
						for(IFile file : undoRefactoring.getNewlyCreatedFiles()) {
							try {
								file.delete(true, null);
							} catch (CoreException e) {
								e.printStackTrace();
							}
						}
						if(undoStack.empty())
							undoRefactoringAction.setEnabled(false);
					}
				}
				applyRefactoringAction.setEnabled(false);
			}
		};
		undoRefactoringAction.setToolTipText("Undo Refactoring");
		undoRefactoringAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_TOOL_UNDO));
		undoRefactoringAction.setEnabled(false);
		
		renameMethodAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)tableViewer.getSelection();
				TypeCheckElimination entry = (TypeCheckElimination)selection.getFirstElement();
				String methodName = entry.getAbstractMethodName();
				IInputValidator methodNameValidator = new MethodNameValidator();
				InputDialog dialog = new InputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Rename Method", "Please enter a new name", methodName, methodNameValidator);
				dialog.open();
				if(dialog.getValue() != null) {
					entry.setAbstractMethodName(dialog.getValue());
					tableViewer.refresh();
				}
			}
		};
		renameMethodAction.setToolTipText("Rename Abstract Method");
		renameMethodAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJ_FILE));
		renameMethodAction.setEnabled(false);
		
		doubleClickAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)tableViewer.getSelection();
				TypeCheckElimination typeCheckElimination = (TypeCheckElimination)selection.getFirstElement();
				IFile sourceFile = astReader.getFile(typeCheckElimination.getTypeCheckClass());
				String typeCheckMethodName = typeCheckElimination.toString();
				Statement typeCheckCodeFragment = typeCheckElimination.getTypeCheckCodeFragment();
				try {
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
					Annotation annotation = new Annotation("org.eclipse.jdt.ui.occurrences", false, typeCheckMethodName);
					Position position = new Position(typeCheckCodeFragment.getStartPosition(), typeCheckCodeFragment.getLength());
					annotationModel.addAnnotation(annotation, position);
					sourceEditor.setHighlightRange(typeCheckCodeFragment.getStartPosition(), typeCheckCodeFragment.getLength(), true);
				} catch (PartInitException e) {
					e.printStackTrace();
				} catch (JavaModelException e) {
					e.printStackTrace();
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

	private TypeCheckElimination[] getTable(IProject iProject) {
		astReader = new ASTReader(iProject);
		SystemObject systemObject = astReader.getSystemObject();
		typeCheckEliminationResults = systemObject.generateTypeCheckEliminations();
		List<TypeCheckElimination> typeCheckEliminations = typeCheckEliminationResults.getTypeCheckEliminations();
		TypeCheckElimination[] table = new TypeCheckElimination[typeCheckEliminations.size()];
		int i = 0;
		for(TypeCheckElimination typeCheckElimination : typeCheckEliminations) {
			table[i] = typeCheckElimination;
			i++;
		}
		return table;
	}

	private void saveResults() {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("C:\\results.txt"));
			Table table = tableViewer.getTable();
			for(int i=0; i<table.getItemCount(); i++) {
				TableItem tableItem = table.getItem(i);
				for(int j=0; j<table.getColumnCount(); j++) {
					out.write(tableItem.getText(j) + "\t");
				}
				out.newLine();
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}