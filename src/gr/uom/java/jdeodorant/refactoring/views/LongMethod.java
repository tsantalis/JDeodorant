package gr.uom.java.jdeodorant.refactoring.views;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.decomposition.cfg.CFG;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.PDGSlice;
import gr.uom.java.jdeodorant.refactoring.manipulators.ASTSlice;
import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractMethodRefactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

public class LongMethod extends ViewPart {
	private TableViewer tableViewer;
	private Action identifyBadSmellsAction;
	private Action applyRefactoringAction;
	private Action doubleClickAction;
	private Action renameMethodAction;
	private IProject selectedProject;
	private IPackageFragment selectedPackage;
	private ASTSlice[] sliceTable;
	private ASTReader astReader;
	
	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if(sliceTable!=null) {
				return sliceTable;
			}
			else {
				return new PDGSlice[] {};
			}
		}
	}

	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			ASTSlice entry = (ASTSlice)obj;
			switch(index){
			case 0:
				return "Extract Method";
			case 1:
				return entry.getSourceMethodDeclaration().getName().getIdentifier();
			case 2:
				return entry.getStatementCriterion().toString();
			case 3:
				return entry.getLocalVariableCriterion().getName().getIdentifier();
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
				if(javaProject != null && !javaProject.getProject().equals(selectedProject)) {
					selectedProject = javaProject.getProject();
					if(sliceTable != null)
						tableViewer.remove(sliceTable);
					identifyBadSmellsAction.setEnabled(true);
					applyRefactoringAction.setEnabled(false);
					renameMethodAction.setEnabled(false);
				}
			}
		}
	};

	@Override
	public void createPartControl(Composite parent) {
		tableViewer = new TableViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		tableViewer.setContentProvider(new ViewContentProvider());
		tableViewer.setLabelProvider(new ViewLabelProvider());
		//tableViewer.setSorter(new NameSorter());
		tableViewer.setInput(getViewSite());
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(20, true));
		layout.addColumnData(new ColumnWeightData(20, true));
		layout.addColumnData(new ColumnWeightData(60, true));
		layout.addColumnData(new ColumnWeightData(40, true));
		tableViewer.getTable().setLayout(layout);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		TableColumn column0 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column0.setText("Refactoring Type");
		column0.setResizable(true);
		column0.pack();
		TableColumn column1 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column1.setText("Source Method");
		column1.setResizable(true);
		column1.pack();
		TableColumn column2 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column2.setText("Statement Criterion");
		column2.setResizable(true);
		column2.pack();
		TableColumn column3 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column3.setText("Variable Criterion");
		column3.setResizable(true);
		column3.pack();
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
		manager.add(renameMethodAction);
	}

	private void makeActions() {
		identifyBadSmellsAction = new Action() {
			public void run() {
				sliceTable = getTable();
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
				ASTSlice slice = (ASTSlice)selection.getFirstElement();
				TypeDeclaration sourceTypeDeclaration = slice.getSourceTypeDeclaration();
				CompilationUnit sourceCompilationUnit = astReader.getCompilationUnit(sourceTypeDeclaration);
				IFile sourceFile = astReader.getFile(sourceTypeDeclaration);
				Refactoring refactoring = new ExtractMethodRefactoring(sourceCompilationUnit, slice);
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
				ASTSlice slice = (ASTSlice)selection.getFirstElement();
				String methodName = slice.getExtractedMethodName();
				IInputValidator methodNameValidator = new MethodNameValidator();
				InputDialog dialog = new InputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Rename Extracted Method", "Please enter a new name", methodName, methodNameValidator);
				dialog.open();
				if(dialog.getValue() != null) {
					slice.setExtractedMethodName(dialog.getValue());
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
				ASTSlice slice = (ASTSlice)selection.getFirstElement();
				IFile sourceFile = astReader.getFile(slice.getSourceTypeDeclaration());
				try {
					IJavaElement sourceJavaElement = JavaCore.create(sourceFile);
					ITextEditor sourceEditor = (ITextEditor)JavaUI.openInEditor(sourceJavaElement);
					List<Position> positions = slice.getHighlightPositions();
					AnnotationModel annotationModel = (AnnotationModel)sourceEditor.getDocumentProvider().getAnnotationModel(sourceEditor.getEditorInput());
					Iterator<Annotation> annotationIterator = annotationModel.getAnnotationIterator();
					while(annotationIterator.hasNext()) {
						Annotation currentAnnotation = annotationIterator.next();
						if(currentAnnotation.getType().equals("org.eclipse.jdt.ui.occurrences")) {
							annotationModel.removeAnnotation(currentAnnotation);
						}
					}
					for(Position position : positions) {
						Annotation annotation = new Annotation("org.eclipse.jdt.ui.occurrences", false, "");
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
		};
	}

	private void hookDoubleClickAction() {
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	@Override
	public void setFocus() {
		tableViewer.getControl().setFocus();
	}

	private ASTSlice[] getTable() {
		if(selectedPackage != null)
			astReader = new ASTReader(selectedPackage);
		else
			astReader = new ASTReader(selectedProject);
		SystemObject systemObject = astReader.getSystemObject();
		List<PDGSlice> extractedSlices = new ArrayList<PDGSlice>();
		ListIterator<ClassObject> classIterator = systemObject.getClassListIterator();
		while(classIterator.hasNext()) {
			ClassObject classObject = classIterator.next();
			ListIterator<MethodObject> methodIterator = classObject.getMethodIterator();
			while(methodIterator.hasNext()) {
				MethodObject methodObject = methodIterator.next();
				if(methodObject.getMethodBody() != null) {
					CFG cfg = new CFG(methodObject);
					PDG pdg = new PDG(cfg);
					Set<PDGSlice> pdgSlices = pdg.getAllProgramDependenceSlices();
					for(PDGSlice pdgSlice : pdgSlices) {
						if(!pdgSlice.getSliceNodes().isEmpty())
							extractedSlices.add(pdgSlice);
					}
				}
			}
		}
		ASTSlice[] table = new ASTSlice[extractedSlices.size()];
		for(int i=0; i<extractedSlices.size(); i++) {
			ASTSlice astSlice = new ASTSlice(extractedSlices.get(i));
			table[i] = astSlice;
		}
		return table;
	}
}
