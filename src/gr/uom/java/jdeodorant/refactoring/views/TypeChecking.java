package gr.uom.java.jdeodorant.refactoring.views;


import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.jdeodorant.refactoring.manipulators.ReplaceConditionalWithPolymorphism;
import gr.uom.java.jdeodorant.refactoring.manipulators.ReplaceTypeCodeWithStateStrategy;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckElimination;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.part.*;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
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
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
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

public class TypeChecking extends ViewPart {
	private TableViewer tableViewer;
	private Action identifyBadSmellsAction;
	private Action applyRefactoringAction;
	private Action doubleClickAction;
	private Action renameMethodAction;
	private Action saveResultsAction;
	private IJavaProject selectedProject;
	private IPackageFragmentRoot selectedPackageFragmentRoot;
	private IPackageFragment selectedPackageFragment;
	private ICompilationUnit selectedCompilationUnit;
	private IType selectedType;
	private TypeCheckElimination[] typeCheckEliminationTable;

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
					return Integer.toString(typeCheckElimination.getGroupSizeAtSystemLevel());
				case 4:
					return Integer.toString(typeCheckElimination.getGroupSizeAtClassLevel());
				case 5:
					return Double.toString(typeCheckElimination.getAverageNumberOfStatements());
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
			
			int groupSizeAtSystemLevel1 = typeCheckElimination1.getGroupSizeAtSystemLevel();
			int groupSizeAtSystemLevel2 = typeCheckElimination2.getGroupSizeAtSystemLevel();
			double averageNumberOfStatementsInGroup1 = typeCheckElimination1.getAverageNumberOfStatementsInGroup();
			double averageNumberOfStatementsInGroup2 = typeCheckElimination2.getAverageNumberOfStatementsInGroup();
			
			if(groupSizeAtSystemLevel1 > groupSizeAtSystemLevel2)
				return -1;
			if(groupSizeAtSystemLevel1 < groupSizeAtSystemLevel2)
				return 1;
			
			if(averageNumberOfStatementsInGroup1 > averageNumberOfStatementsInGroup2)
				return -1;
			if(averageNumberOfStatementsInGroup1 < averageNumberOfStatementsInGroup2)
				return 1;
			
			int groupSizeAtClassLevel1 = typeCheckElimination1.getGroupSizeAtClassLevel();
			int groupSizeAtClassLevel2 = typeCheckElimination2.getGroupSizeAtClassLevel();
			double averageNumberOfStatements1 = typeCheckElimination1.getAverageNumberOfStatements();
			double averageNumberOfStatements2 = typeCheckElimination2.getAverageNumberOfStatements();
			String refactoringName1 = typeCheckElimination1.toString();
			String refactoringName2 = typeCheckElimination2.toString();
			
			if(groupSizeAtClassLevel1 > groupSizeAtClassLevel2)
				return -1;
			if(groupSizeAtClassLevel1 < groupSizeAtClassLevel2)
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
					identifyBadSmellsAction.setEnabled(true);
					applyRefactoringAction.setEnabled(false);
					renameMethodAction.setEnabled(false);
					saveResultsAction.setEnabled(false);
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
		JavaCore.addElementChangedListener(new ElementChangedListener());
		getSite().getWorkbenchWindow().getWorkbench().getOperationSupport().getOperationHistory().addOperationHistoryListener(new IOperationHistoryListener() {
			public void historyNotification(OperationHistoryEvent event) {
				int eventType = event.getEventType();
				if(eventType == OperationHistoryEvent.UNDONE  || eventType == OperationHistoryEvent.REDONE ||
						eventType == OperationHistoryEvent.OPERATION_ADDED || eventType == OperationHistoryEvent.OPERATION_REMOVED) {
					applyRefactoringAction.setEnabled(false);
					renameMethodAction.setEnabled(false);
					saveResultsAction.setEnabled(false);
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
		manager.add(renameMethodAction);
		manager.add(saveResultsAction);
	}

	private void makeActions() {
		identifyBadSmellsAction = new Action() {
			public void run() {
				CompilationUnitCache.getInstance().clearCache();
				typeCheckEliminationTable = getTable();
				tableViewer.setContentProvider(new ViewContentProvider());
				applyRefactoringAction.setEnabled(true);
				renameMethodAction.setEnabled(true);
				saveResultsAction.setEnabled(true);
			}
		};
		identifyBadSmellsAction.setToolTipText("Identify Bad Smells");
		identifyBadSmellsAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		identifyBadSmellsAction.setEnabled(false);

		saveResultsAction = new Action() {
			public void run() {
				saveResults();
			}
		};
		saveResultsAction.setToolTipText("Save Results");
		saveResultsAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_ETOOL_SAVE_EDIT));
		saveResultsAction.setEnabled(false);
		
		applyRefactoringAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)tableViewer.getSelection();
				TypeCheckElimination typeCheckElimination = (TypeCheckElimination)selection.getFirstElement();
				TypeDeclaration sourceTypeDeclaration = typeCheckElimination.getTypeCheckClass();
				CompilationUnit sourceCompilationUnit = (CompilationUnit)sourceTypeDeclaration.getRoot();
				IFile sourceFile = typeCheckElimination.getTypeCheckIFile();
				Refactoring refactoring = null;
				if(typeCheckElimination.getExistingInheritanceTree() == null) {
					refactoring = new ReplaceTypeCodeWithStateStrategy(sourceFile, sourceCompilationUnit, sourceTypeDeclaration, typeCheckElimination);
				}
				else {
					refactoring = new ReplaceConditionalWithPolymorphism(sourceFile, sourceCompilationUnit, sourceTypeDeclaration, typeCheckElimination);
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
					IJavaElement sourceJavaElement = JavaCore.create(sourceFile);
					JavaUI.openInEditor(sourceJavaElement);
				} catch (PartInitException e) {
					e.printStackTrace();
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		};
		applyRefactoringAction.setToolTipText("Apply Refactoring");
		applyRefactoringAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_DEF_VIEW));
		applyRefactoringAction.setEnabled(false);
		
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
				IFile sourceFile = typeCheckElimination.getTypeCheckIFile();
				String typeCheckMethodName = typeCheckElimination.toString();
				Statement typeCheckCodeFragment = typeCheckElimination.getTypeCheckCodeFragment();
				try {
					IJavaElement sourceJavaElement = JavaCore.create(sourceFile);
					ITextEditor sourceEditor = (ITextEditor)JavaUI.openInEditor(sourceJavaElement);
					AnnotationModel annotationModel = (AnnotationModel)sourceEditor.getDocumentProvider().getAnnotationModel(sourceEditor.getEditorInput());
					Iterator<Annotation> annotationIterator = annotationModel.getAnnotationIterator();
					while(annotationIterator.hasNext()) {
						Annotation currentAnnotation = annotationIterator.next();
						if(currentAnnotation.getType().equals(SliceAnnotation.EXTRACTION)) {
							annotationModel.removeAnnotation(currentAnnotation);
						}
					}
					SliceAnnotation annotation = new SliceAnnotation(SliceAnnotation.EXTRACTION, typeCheckMethodName);
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

	public void dispose() {
		super.dispose();
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(selectionListener);
	}

	private TypeCheckElimination[] getTable() {
		TypeCheckElimination[] table = null;
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
			final SystemObject systemObject = ASTReader.getSystemObject();
			final Set<ClassObject> classObjectsToBeExamined = new LinkedHashSet<ClassObject>();
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
			final List<TypeCheckElimination> typeCheckEliminations = new ArrayList<TypeCheckElimination>();
			ps.busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					typeCheckEliminations.addAll(systemObject.generateTypeCheckEliminations(classObjectsToBeExamined, monitor));
				}
			});
			
			table = new TypeCheckElimination[typeCheckEliminations.size()];
			int i = 0;
			for(TypeCheckElimination typeCheckElimination : typeCheckEliminations) {
				table[i] = typeCheckElimination;
				i++;
			}
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return table;
	}

	private void saveResults() {
		FileDialog fd = new FileDialog(getSite().getWorkbenchWindow().getShell(), SWT.SAVE);
		fd.setText("Save Results");
        String[] filterExt = { "*.txt" };
        fd.setFilterExtensions(filterExt);
        String selected = fd.open();
        if(selected != null) {
        	try {
        		BufferedWriter out = new BufferedWriter(new FileWriter(selected));
        		Table table = tableViewer.getTable();
        		TableColumn[] columns = table.getColumns();
        		for(int i=0; i<columns.length; i++) {
        			if(i == columns.length-1)
        				out.write(columns[i].getText());
        			else
        				out.write(columns[i].getText() + "\t");
        		}
        		out.newLine();
        		for(int i=0; i<table.getItemCount(); i++) {
        			TableItem tableItem = table.getItem(i);
        			for(int j=0; j<table.getColumnCount(); j++) {
        				if(j == table.getColumnCount()-1)
        					out.write(tableItem.getText(j));
        				else
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
}