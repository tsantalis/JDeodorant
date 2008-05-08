package gr.uom.java.jdeodorant.refactoring.views;


import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.jdeodorant.refactoring.manipulators.Refactoring;
import gr.uom.java.jdeodorant.refactoring.manipulators.ReplaceConditionalWithPolymorphism;
import gr.uom.java.jdeodorant.refactoring.manipulators.ReplaceTypeCodeWithStateStrategy;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckElimination;
import gr.uom.java.jdeodorant.refactoring.manipulators.UndoRefactoring;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
	private Refactoring[] refactoringTable;
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
			if(refactoringTable != null) {
				return refactoringTable;
			}
			else {
				return new ReplaceTypeCodeWithStateStrategy[] {};
			}
		}
	}
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			Refactoring refactoring = (Refactoring)obj;
			switch(index) {
				case 0:
					if(refactoring instanceof ReplaceTypeCodeWithStateStrategy)
						return "Replace Type Code with State/Strategy";
					else if(refactoring instanceof ReplaceConditionalWithPolymorphism)
						return "Replace Conditional with Polymorphism";
				case 1:
					if(refactoring instanceof ReplaceTypeCodeWithStateStrategy) {
						ReplaceTypeCodeWithStateStrategy replaceTypeCodeWithStateStrategyRefactoring =
							(ReplaceTypeCodeWithStateStrategy)refactoring;
						return replaceTypeCodeWithStateStrategyRefactoring.getTypeCheckMethodName();
					}
					else if(refactoring instanceof ReplaceConditionalWithPolymorphism) {
						ReplaceConditionalWithPolymorphism replaceConditionalWithPolymorphismRefactoring =
							(ReplaceConditionalWithPolymorphism)refactoring;
						return replaceConditionalWithPolymorphismRefactoring.getTypeCheckMethodName();
					}
				case 2:
					if(refactoring instanceof ReplaceTypeCodeWithStateStrategy) {
						ReplaceTypeCodeWithStateStrategy replaceTypeCodeWithStateStrategyRefactoring =
							(ReplaceTypeCodeWithStateStrategy)refactoring;
						return replaceTypeCodeWithStateStrategyRefactoring.getAbstractMethodName();
					}
					else if(refactoring instanceof ReplaceConditionalWithPolymorphism) {
						ReplaceConditionalWithPolymorphism replaceConditionalWithPolymorphismRefactoring =
							(ReplaceConditionalWithPolymorphism)refactoring;
						return replaceConditionalWithPolymorphismRefactoring.getAbstractMethodName();
					}
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
			Refactoring refactoring1 = (Refactoring)obj1;
			Refactoring refactoring2 = (Refactoring)obj2;
			String refactoringName1 = null;
			String refactoringName2 = null;
			if(refactoring1 instanceof ReplaceTypeCodeWithStateStrategy) {
				ReplaceTypeCodeWithStateStrategy replaceTypeCodeWithStateStrategyRefactoring =
					(ReplaceTypeCodeWithStateStrategy)refactoring1;
				refactoringName1 = replaceTypeCodeWithStateStrategyRefactoring.getTypeCheckMethodName();
			}
			else if(refactoring1 instanceof ReplaceConditionalWithPolymorphism) {
				ReplaceConditionalWithPolymorphism replaceConditionalWithPolymorphismRefactoring =
					(ReplaceConditionalWithPolymorphism)refactoring1;
				refactoringName1 = replaceConditionalWithPolymorphismRefactoring.getTypeCheckMethodName();
			}
			if(refactoring2 instanceof ReplaceTypeCodeWithStateStrategy) {
				ReplaceTypeCodeWithStateStrategy replaceTypeCodeWithStateStrategyRefactoring =
					(ReplaceTypeCodeWithStateStrategy)refactoring2;
				refactoringName2 = replaceTypeCodeWithStateStrategyRefactoring.getTypeCheckMethodName();
			}
			else if(refactoring2 instanceof ReplaceConditionalWithPolymorphism) {
				ReplaceConditionalWithPolymorphism replaceConditionalWithPolymorphismRefactoring =
					(ReplaceConditionalWithPolymorphism)refactoring2;
				refactoringName2 = replaceConditionalWithPolymorphismRefactoring.getTypeCheckMethodName();
			}
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
		layout.addColumnData(new ColumnWeightData(50, true));
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
				refactoringTable = getTable(selectedProject);
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
				Refactoring refactoring = (Refactoring)selection.getFirstElement();
				IEditorPart sourceEditor = null;
				IFile sourceFile = null;
				if(refactoring instanceof ReplaceTypeCodeWithStateStrategy) {
					ReplaceTypeCodeWithStateStrategy replaceTypeCodeWithStateStrategyRefactoring =
						(ReplaceTypeCodeWithStateStrategy)refactoring;
					sourceFile = replaceTypeCodeWithStateStrategyRefactoring.getSourceFile();
				}
				else if(refactoring instanceof ReplaceConditionalWithPolymorphism) {
					ReplaceConditionalWithPolymorphism replaceConditionalWithPolymorphismRefactoring =
						(ReplaceConditionalWithPolymorphism)refactoring;
					sourceFile = replaceConditionalWithPolymorphismRefactoring.getSourceFile();
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
				Refactoring entry = (Refactoring)selection.getFirstElement();
				if(entry instanceof ReplaceTypeCodeWithStateStrategy) {
					ReplaceTypeCodeWithStateStrategy refactoring = (ReplaceTypeCodeWithStateStrategy)entry;
					String methodName = refactoring.getAbstractMethodName();
					IInputValidator methodNameValidator = new MethodNameValidator();
					InputDialog dialog = new InputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Rename Method", "Please enter a new name", methodName, methodNameValidator);
					dialog.open();
					if(dialog.getValue() != null) {
						refactoring.setAbstractMethodName(dialog.getValue());
						tableViewer.refresh();
					}
				}
				else if(entry instanceof ReplaceConditionalWithPolymorphism) {
					ReplaceConditionalWithPolymorphism refactoring = (ReplaceConditionalWithPolymorphism)entry;
					String methodName = refactoring.getAbstractMethodName();
					IInputValidator methodNameValidator = new MethodNameValidator();
					InputDialog dialog = new InputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Rename Method", "Please enter a new name", methodName, methodNameValidator);
					dialog.open();
					if(dialog.getValue() != null) {
						refactoring.setAbstractMethodName(dialog.getValue());
						tableViewer.refresh();
					}
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
				Refactoring refactoring = (Refactoring)selection.getFirstElement();
				IFile sourceFile = null;
				String typeCheckMethodName = null;
				Statement typeCheckCodeFragment = null;
				if(refactoring instanceof ReplaceTypeCodeWithStateStrategy) {
					ReplaceTypeCodeWithStateStrategy replaceTypeCodeWithStateStrategyRefactoring =
						(ReplaceTypeCodeWithStateStrategy)refactoring;
					sourceFile = replaceTypeCodeWithStateStrategyRefactoring.getSourceFile();
					typeCheckMethodName = replaceTypeCodeWithStateStrategyRefactoring.getTypeCheckMethodName();
					typeCheckCodeFragment = replaceTypeCodeWithStateStrategyRefactoring.getTypeCheckCodeFragment();
				}
				else if(refactoring instanceof ReplaceConditionalWithPolymorphism) {
					ReplaceConditionalWithPolymorphism replaceConditionalWithPolymorphismRefactoring =
						(ReplaceConditionalWithPolymorphism)refactoring;
					sourceFile = replaceConditionalWithPolymorphismRefactoring.getSourceFile();
					typeCheckMethodName = replaceConditionalWithPolymorphismRefactoring.getTypeCheckMethodName();
					typeCheckCodeFragment = replaceConditionalWithPolymorphismRefactoring.getTypeCheckCodeFragment();
				}
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

	private Refactoring[] getTable(IProject iProject) {
		astReader = new ASTReader(iProject);
		SystemObject systemObject = astReader.getSystemObject();
		List<TypeCheckElimination> typeCheckEliminations = systemObject.generateTypeCheckEliminations();
		List<Refactoring> refactorings = new ArrayList<Refactoring>();
		for(TypeCheckElimination typeCheckElimination : typeCheckEliminations) {
			TypeDeclaration sourceTypeDeclaration = typeCheckElimination.getTypeCheckClass();
			CompilationUnit sourceCompilationUnit = astReader.getCompilationUnit(sourceTypeDeclaration);
			IFile sourceFile = astReader.getFile(sourceTypeDeclaration);
			if(typeCheckElimination.getExistingInheritanceTree() == null) {
				ReplaceTypeCodeWithStateStrategy replaceTypeCodeWithStateStrategyRefactoring =
					new ReplaceTypeCodeWithStateStrategy(sourceFile, sourceCompilationUnit, sourceTypeDeclaration, typeCheckElimination);
				refactorings.add(replaceTypeCodeWithStateStrategyRefactoring);
			}
			else {
				ReplaceConditionalWithPolymorphism replaceConditionalWithPolymorphismRefactoring =
					new ReplaceConditionalWithPolymorphism(sourceFile, sourceCompilationUnit, sourceTypeDeclaration, typeCheckElimination);
				refactorings.add(replaceConditionalWithPolymorphismRefactoring);
			}
		}
		Refactoring[] table = new Refactoring[refactorings.size()];
		int i = 0;
		for(Refactoring refactoring : refactorings) {
			table[i] = refactoring;
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