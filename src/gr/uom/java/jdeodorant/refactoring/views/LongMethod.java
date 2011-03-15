package gr.uom.java.jdeodorant.refactoring.views;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.decomposition.cfg.CFG;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.PDGObjectSliceUnion;
import gr.uom.java.ast.decomposition.cfg.PDGObjectSliceUnionCollection;
import gr.uom.java.ast.decomposition.cfg.PDGSlice;
import gr.uom.java.ast.decomposition.cfg.PDGSliceUnion;
import gr.uom.java.ast.decomposition.cfg.PDGSliceUnionCollection;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.util.StatementExtractor;
import gr.uom.java.history.MethodEvolution;
import gr.uom.java.history.ProjectEvolution;
import gr.uom.java.jdeodorant.preferences.PreferenceConstants;
import gr.uom.java.jdeodorant.refactoring.Activator;
import gr.uom.java.jdeodorant.refactoring.manipulators.ASTSlice;
import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractMethodRefactoring;

import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
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

public class LongMethod extends ViewPart {
	private TableViewer tableViewer;
	private Action identifyBadSmellsAction;
	private Action applyRefactoringAction;
	private Action doubleClickAction;
	private Action renameMethodAction;
	private Action saveResultsAction;
	private Action evolutionAnalysisAction;
	private IJavaProject selectedProject;
	private IPackageFragmentRoot selectedPackageFragmentRoot;
	private IPackageFragment selectedPackageFragment;
	private ICompilationUnit selectedCompilationUnit;
	private IType selectedType;
	private IMethod selectedMethod;
	private ASTSlice[] sliceTable;
	private MethodEvolution methodEvolution;
	
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
				String declaringClass = entry.getSourceTypeDeclaration().resolveBinding().getQualifiedName();
				String methodName = entry.getSourceMethodDeclaration().resolveBinding().toString();
				return declaringClass + "::" + methodName;
			case 2:
				return entry.getLocalVariableCriterion().getName().getIdentifier();
			case 3:
				return "B" + entry.getBoundaryBlock().getId();
			case 4:
				int numberOfSliceStatements = entry.getSliceStatements().size();
				int numberOfRemovableStatements = entry.getRemovableStatements().size();
				int numberOfDuplicatedStatements = numberOfSliceStatements - numberOfRemovableStatements;
				return numberOfDuplicatedStatements + "/" + numberOfSliceStatements;
			case 5:
				Integer userRate = entry.getUserRate();
				return (userRate == null) ? "" : userRate.toString();
			default:
				return "";
			}
			
		}
		public Image getColumnImage(Object obj, int index) {
			ASTSlice entry = (ASTSlice)obj;
			int rate = -1;
			Integer userRate = entry.getUserRate();
			if(userRate != null)
				rate = userRate;
			Image image = null;
			switch(index) {
			case 5:
				if(rate != -1) {
					image = Activator.getImageDescriptor("/icons/" + String.valueOf(rate) + ".jpg").createImage();
				}
			default:
	            break;
			}
			return image;
		}
		public Image getImage(Object obj) {
			return null;
		}
	}

	class NameSorter extends ViewerSorter {
		public int compare(Viewer viewer, Object obj1, Object obj2) {
			ASTSlice slice1 = (ASTSlice)obj1;
			ASTSlice slice2 = (ASTSlice)obj2;
			
			double duplicationRatio1 = slice1.getAverageDuplicationRatioInGroup();
			double duplicationRatio2 = slice2.getAverageDuplicationRatioInGroup();
			
			if(duplicationRatio1 < duplicationRatio2)
				return -1;
			else if(duplicationRatio1 > duplicationRatio2)
				return 1;
			//same duplication ratio
			double averageNumberOfDuplicatedStatements1 = slice1.getAverageNumberOfDuplicatedStatementsInGroup();
			double averageNumberOfDuplicatedStatements2 = slice2.getAverageNumberOfDuplicatedStatementsInGroup();
			
			if(averageNumberOfDuplicatedStatements1 != 0 && averageNumberOfDuplicatedStatements2 != 0) {
				if(averageNumberOfDuplicatedStatements1 < averageNumberOfDuplicatedStatements2)
					return -1;
				else if(averageNumberOfDuplicatedStatements1 > averageNumberOfDuplicatedStatements2)
					return 1;
			}
			
			int maximumNumberOfExtractedStatements1 = slice1.getMaximumNumberOfExtractedStatementsInGroup();
			int maximumNumberOfExtractedStatements2 = slice2.getMaximumNumberOfExtractedStatementsInGroup();
			
			if(averageNumberOfDuplicatedStatements1 == 0 && averageNumberOfDuplicatedStatements2 == 0) {
				if(maximumNumberOfExtractedStatements1 < maximumNumberOfExtractedStatements2)
					return 1;
				else if(maximumNumberOfExtractedStatements1 > maximumNumberOfExtractedStatements2)
					return -1;
			}
			
			double averageNumberOfExtractedStatements1 = slice1.getAverageNumberOfExtractedStatementsInGroup();
			double averageNumberOfExtractedStatements2 = slice2.getAverageNumberOfExtractedStatementsInGroup();
			
			if(averageNumberOfExtractedStatements1 < averageNumberOfExtractedStatements2)
				return 1;
			else if(averageNumberOfExtractedStatements1 > averageNumberOfExtractedStatements2)
				return -1;
			
			//slices belong to the same group
			return Integer.valueOf(slice1.getBoundaryBlock().getId()).compareTo(Integer.valueOf(slice2.getBoundaryBlock().getId()));
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
					selectedMethod = null;
				}
				else if(element instanceof IPackageFragmentRoot) {
					IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot)element;
					javaProject = packageFragmentRoot.getJavaProject();
					selectedPackageFragmentRoot = packageFragmentRoot;
					selectedPackageFragment = null;
					selectedCompilationUnit = null;
					selectedType = null;
					selectedMethod = null;
				}
				else if(element instanceof IPackageFragment) {
					IPackageFragment packageFragment = (IPackageFragment)element;
					javaProject = packageFragment.getJavaProject();
					selectedPackageFragment = packageFragment;
					selectedPackageFragmentRoot = null;
					selectedCompilationUnit = null;
					selectedType = null;
					selectedMethod = null;
				}
				else if(element instanceof ICompilationUnit) {
					ICompilationUnit compilationUnit = (ICompilationUnit)element;
					javaProject = compilationUnit.getJavaProject();
					selectedCompilationUnit = compilationUnit;
					selectedPackageFragmentRoot = null;
					selectedPackageFragment = null;
					selectedType = null;
					selectedMethod = null;
				}
				else if(element instanceof IType) {
					IType type = (IType)element;
					javaProject = type.getJavaProject();
					selectedType = type;
					selectedPackageFragmentRoot = null;
					selectedPackageFragment = null;
					selectedCompilationUnit = null;
					selectedMethod = null;
				}
				else if(element instanceof IMethod) {
					IMethod method = (IMethod)element;
					javaProject = method.getJavaProject();
					selectedMethod = method;
					selectedPackageFragmentRoot = null;
					selectedPackageFragment = null;
					selectedCompilationUnit = null;
					selectedType = null;
				}
				if(javaProject != null && !javaProject.equals(selectedProject)) {
					selectedProject = javaProject;
					/*if(sliceTable != null)
						tableViewer.remove(sliceTable);*/
					identifyBadSmellsAction.setEnabled(true);
					applyRefactoringAction.setEnabled(false);
					renameMethodAction.setEnabled(false);
					saveResultsAction.setEnabled(false);
					evolutionAnalysisAction.setEnabled(false);
				}
			}
		}
	};

	@Override
	public void createPartControl(Composite parent) {
		tableViewer = new TableViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		tableViewer.setContentProvider(new ViewContentProvider());
		tableViewer.setLabelProvider(new ViewLabelProvider());
		tableViewer.setSorter(new NameSorter());
		tableViewer.setInput(getViewSite());
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(20, true));
		layout.addColumnData(new ColumnWeightData(60, true));
		layout.addColumnData(new ColumnWeightData(40, true));
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
		column1.setText("Source Method");
		column1.setResizable(true);
		column1.pack();
		TableColumn column2 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column2.setText("Variable Criterion");
		column2.setResizable(true);
		column2.pack();
		TableColumn column3 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column3.setText("Block-Based Region");
		column3.setResizable(true);
		column3.pack();
		TableColumn column4 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column4.setText("Duplicated/Extracted");
		column4.setResizable(true);
		column4.pack();
		
		TableColumn column5 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column5.setText("Rate it!");
		column5.setResizable(true);
		column5.pack();
		
		tableViewer.setColumnProperties(new String[] {"type", "source", "variable", "block", "duplicationRatio", "rate"});
		tableViewer.setCellEditors(new CellEditor[] {
				new TextCellEditor(), new TextCellEditor(), new TextCellEditor(), new TextCellEditor(), new TextCellEditor(),
				new MyComboBoxCellEditor(tableViewer.getTable(), new String[] {"0", "1", "2", "3", "4", "5"}, SWT.READ_ONLY)
		});
		
		tableViewer.setCellModifier(new ICellModifier() {
			public boolean canModify(Object element, String property) {
				return property.equals("rate");
			}

			public Object getValue(Object element, String property) {
				if(element instanceof ASTSlice) {
					ASTSlice slice = (ASTSlice)element;
					if(slice.getUserRate() != null)
						return slice.getUserRate();
					else
						return 0;
				}
				return 0;
			}

			public void modify(Object element, String property, Object value) {
				TableItem item = (TableItem)element;
				Object data = item.getData();
				if(data instanceof ASTSlice) {
					ASTSlice slice = (ASTSlice)data;
					slice.setUserRate((Integer)value);
					IPreferenceStore store = Activator.getDefault().getPreferenceStore();
					boolean allowUsageReporting = store.getBoolean(PreferenceConstants.P_ENABLE_USAGE_REPORTING);
					if(allowUsageReporting) {
						Table table = tableViewer.getTable();
						int rankingPosition = -1;
						for(int i=0; i<table.getItemCount(); i++) {
							TableItem tableItem = table.getItem(i);
							if(tableItem.equals(item)) {
								rankingPosition = i;
								break;
							}
						}
						try {
							boolean allowSourceCodeReporting = store.getBoolean(PreferenceConstants.P_ENABLE_SOURCE_CODE_REPORTING);
							String declaringClass = slice.getSourceTypeDeclaration().resolveBinding().getQualifiedName();
							String methodName = slice.getSourceMethodDeclaration().resolveBinding().toString();
							String sourceMethodName = declaringClass + "::" + methodName;
							String content = URLEncoder.encode("project_name", "UTF-8") + "=" + URLEncoder.encode(selectedProject.getElementName(), "UTF-8");
							content += "&" + URLEncoder.encode("source_method_name", "UTF-8") + "=" + URLEncoder.encode(sourceMethodName, "UTF-8");
							content += "&" + URLEncoder.encode("variable_name", "UTF-8") + "=" + URLEncoder.encode(slice.getLocalVariableCriterion().resolveBinding().toString(), "UTF-8");
							content += "&" + URLEncoder.encode("block", "UTF-8") + "=" + URLEncoder.encode("B" + slice.getBoundaryBlock().getId(), "UTF-8");
							content += "&" + URLEncoder.encode("object_slice", "UTF-8") + "=" + URLEncoder.encode(slice.isObjectSlice() ? "1" : "0", "UTF-8");
							int numberOfSliceStatements = slice.getSliceStatements().size();
							int numberOfRemovableStatements = slice.getRemovableStatements().size();
							int numberOfDuplicatedStatements = numberOfSliceStatements - numberOfRemovableStatements;
							content += "&" + URLEncoder.encode("duplicated_statements", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(numberOfDuplicatedStatements), "UTF-8");
							content += "&" + URLEncoder.encode("extracted_statements", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(numberOfSliceStatements), "UTF-8");
							content += "&" + URLEncoder.encode("ranking_position", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(rankingPosition), "UTF-8");
							content += "&" + URLEncoder.encode("total_opportunities", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(table.getItemCount()), "UTF-8");
							if(allowSourceCodeReporting) {
								content += "&" + URLEncoder.encode("source_method_code", "UTF-8") + "=" + URLEncoder.encode(slice.getSourceMethodDeclaration().toString(), "UTF-8");
								content += "&" + URLEncoder.encode("slice_statements", "UTF-8") + "=" + URLEncoder.encode(slice.sliceToString(), "UTF-8");
							}
							content += "&" + URLEncoder.encode("rating", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(slice.getUserRate()), "UTF-8");
							content += "&" + URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(System.getProperty("user.name"), "UTF-8");
							content += "&" + URLEncoder.encode("tb", "UTF-8") + "=" + URLEncoder.encode("2", "UTF-8");
							URL url = new URL(Activator.RANK_URL);
							URLConnection urlConn = url.openConnection();
							urlConn.setDoInput(true);
							urlConn.setDoOutput(true);
							urlConn.setUseCaches(false);
							urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
							DataOutputStream printout = new DataOutputStream(urlConn.getOutputStream());
							printout.writeBytes(content);
							printout.flush();
							printout.close();
							DataInputStream input = new DataInputStream(urlConn.getInputStream());
							input.close();
						} catch (IOException ioe) {
							ioe.printStackTrace();
						}
					}
					tableViewer.update(data, null);
				}
			}
		});
		
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
					evolutionAnalysisAction.setEnabled(false);
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
		manager.add(evolutionAnalysisAction);
	}

	private void makeActions() {
		identifyBadSmellsAction = new Action() {
			public void run() {
				CompilationUnitCache.getInstance().clearCache();
				sliceTable = getTable();
				tableViewer.setContentProvider(new ViewContentProvider());
				applyRefactoringAction.setEnabled(true);
				renameMethodAction.setEnabled(true);
				saveResultsAction.setEnabled(true);
				evolutionAnalysisAction.setEnabled(true);
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
		
		evolutionAnalysisAction = new Action() {
			public void run() {
				methodEvolution = null;
				IStructuredSelection selection = (IStructuredSelection)tableViewer.getSelection();
				final ASTSlice slice = (ASTSlice)selection.getFirstElement();
				try {
					IWorkbench wb = PlatformUI.getWorkbench();
					IProgressService ps = wb.getProgressService();
					ps.busyCursorWhile(new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							ProjectEvolution projectEvolution = new ProjectEvolution(selectedProject);
							if(projectEvolution.getProjectEntries().size() > 1) {
								methodEvolution = new MethodEvolution(projectEvolution, (IMethod)slice.getSourceMethodDeclaration().resolveBinding().getJavaElement(), monitor);
							}
						}
					});
					if(methodEvolution != null) {
						EvolutionDialog dialog = new EvolutionDialog(getSite().getWorkbenchWindow(), methodEvolution, "Method Evolution", false);
						dialog.open();
					}
					else
						MessageDialog.openInformation(getSite().getShell(), "Method Evolution",
						"Method evolution analysis cannot be performed, since only a single version of the examined project is loaded in the workspace.");
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		evolutionAnalysisAction.setToolTipText("Evolution Analysis");
		evolutionAnalysisAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJ_ELEMENT));
		evolutionAnalysisAction.setEnabled(false);
		
		applyRefactoringAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)tableViewer.getSelection();
				ASTSlice slice = (ASTSlice)selection.getFirstElement();
				TypeDeclaration sourceTypeDeclaration = slice.getSourceTypeDeclaration();
				CompilationUnit sourceCompilationUnit = (CompilationUnit)sourceTypeDeclaration.getRoot();
				IFile sourceFile = slice.getIFile();
				IPreferenceStore store = Activator.getDefault().getPreferenceStore();
				boolean allowUsageReporting = store.getBoolean(PreferenceConstants.P_ENABLE_USAGE_REPORTING);
				if(allowUsageReporting) {
					Table table = tableViewer.getTable();
					int rankingPosition = -1;
					for(int i=0; i<table.getItemCount(); i++) {
						TableItem tableItem = table.getItem(i);
						if(tableItem.getData().equals(slice)) {
							rankingPosition = i;
							break;
						}
					}
					try {
						boolean allowSourceCodeReporting = store.getBoolean(PreferenceConstants.P_ENABLE_SOURCE_CODE_REPORTING);
						String declaringClass = slice.getSourceTypeDeclaration().resolveBinding().getQualifiedName();
						String methodName = slice.getSourceMethodDeclaration().resolveBinding().toString();
						String sourceMethodName = declaringClass + "::" + methodName;
						String content = URLEncoder.encode("project_name", "UTF-8") + "=" + URLEncoder.encode(selectedProject.getElementName(), "UTF-8");
						content += "&" + URLEncoder.encode("source_method_name", "UTF-8") + "=" + URLEncoder.encode(sourceMethodName, "UTF-8");
						content += "&" + URLEncoder.encode("variable_name", "UTF-8") + "=" + URLEncoder.encode(slice.getLocalVariableCriterion().resolveBinding().toString(), "UTF-8");
						content += "&" + URLEncoder.encode("block", "UTF-8") + "=" + URLEncoder.encode("B" + slice.getBoundaryBlock().getId(), "UTF-8");
						content += "&" + URLEncoder.encode("object_slice", "UTF-8") + "=" + URLEncoder.encode(slice.isObjectSlice() ? "1" : "0", "UTF-8");
						int numberOfSliceStatements = slice.getSliceStatements().size();
						int numberOfRemovableStatements = slice.getRemovableStatements().size();
						int numberOfDuplicatedStatements = numberOfSliceStatements - numberOfRemovableStatements;
						content += "&" + URLEncoder.encode("duplicated_statements", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(numberOfDuplicatedStatements), "UTF-8");
						content += "&" + URLEncoder.encode("extracted_statements", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(numberOfSliceStatements), "UTF-8");
						content += "&" + URLEncoder.encode("ranking_position", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(rankingPosition), "UTF-8");
						content += "&" + URLEncoder.encode("total_opportunities", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(table.getItemCount()), "UTF-8");
						if(allowSourceCodeReporting) {
							content += "&" + URLEncoder.encode("source_method_code", "UTF-8") + "=" + URLEncoder.encode(slice.getSourceMethodDeclaration().toString(), "UTF-8");
							content += "&" + URLEncoder.encode("slice_statements", "UTF-8") + "=" + URLEncoder.encode(slice.sliceToString(), "UTF-8");
						}
						content += "&" + URLEncoder.encode("application", "UTF-8") + "=" + URLEncoder.encode(String.valueOf("1"), "UTF-8");
						content += "&" + URLEncoder.encode("application_selected_name", "UTF-8") + "=" + URLEncoder.encode(slice.getExtractedMethodName(), "UTF-8");
						content += "&" + URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(System.getProperty("user.name"), "UTF-8");
						content += "&" + URLEncoder.encode("tb", "UTF-8") + "=" + URLEncoder.encode("2", "UTF-8");
						URL url = new URL(Activator.RANK_URL);
						URLConnection urlConn = url.openConnection();
						urlConn.setDoInput(true);
						urlConn.setDoOutput(true);
						urlConn.setUseCaches(false);
						urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
						DataOutputStream printout = new DataOutputStream(urlConn.getOutputStream());
						printout.writeBytes(content);
						printout.flush();
						printout.close();
						DataInputStream input = new DataInputStream(urlConn.getInputStream());
						input.close();
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
				}
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
		renameMethodAction.setToolTipText("Rename Extracted Method");
		renameMethodAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJ_FILE));
		renameMethodAction.setEnabled(false);
		
		doubleClickAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)tableViewer.getSelection();
				ASTSlice slice = (ASTSlice)selection.getFirstElement();
				IFile sourceFile = slice.getIFile();
				try {
					IJavaElement sourceJavaElement = JavaCore.create(sourceFile);
					ITextEditor sourceEditor = (ITextEditor)JavaUI.openInEditor(sourceJavaElement);
					Object[] highlightPositionMaps = slice.getHighlightPositions();
					Map<Position, String> annotationMap = (Map<Position, String>)highlightPositionMaps[0];
					Map<Position, Boolean> duplicationMap = (Map<Position, Boolean>)highlightPositionMaps[1];
					AnnotationModel annotationModel = (AnnotationModel)sourceEditor.getDocumentProvider().getAnnotationModel(sourceEditor.getEditorInput());
					Iterator<Annotation> annotationIterator = annotationModel.getAnnotationIterator();
					while(annotationIterator.hasNext()) {
						Annotation currentAnnotation = annotationIterator.next();
						if(currentAnnotation.getType().equals(SliceAnnotation.EXTRACTION) || currentAnnotation.getType().equals(SliceAnnotation.DUPLICATION)) {
							annotationModel.removeAnnotation(currentAnnotation);
						}
					}
					for(Position position : annotationMap.keySet()) {
						SliceAnnotation annotation = null;
						String annotationText = annotationMap.get(position);
						boolean duplicated = duplicationMap.get(position);
						if(duplicated)
							annotation = new SliceAnnotation(SliceAnnotation.DUPLICATION, annotationText);
						else
							annotation = new SliceAnnotation(SliceAnnotation.EXTRACTION, annotationText);
						annotationModel.addAnnotation(annotation, position);
					}
					List<Position> positions = new ArrayList<Position>(annotationMap.keySet());
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

	public void dispose() {
		super.dispose();
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(selectionListener);
	}

	private ASTSlice[] getTable() {
		ASTSlice[] table = null;
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
			final Set<MethodObject> methodObjectsToBeExamined = new LinkedHashSet<MethodObject>();
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
			else if(selectedMethod != null) {
				MethodObject methodObject = systemObject.getMethodObject(selectedMethod);
				if(methodObject != null)
					methodObjectsToBeExamined.add(methodObject);
			}
			else {
				classObjectsToBeExamined.addAll(systemObject.getClassObjects());
			}
			final List<ASTSlice> extractedSlices = new ArrayList<ASTSlice>();

			ps.busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					if(!classObjectsToBeExamined.isEmpty()) {
						int workSize = 0;
						for(ClassObject classObject : classObjectsToBeExamined) {
							workSize += classObject.getNumberOfMethods();
						}
						monitor.beginTask("Identification of Extract Method refactoring opportunities", workSize);
						for(ClassObject classObject : classObjectsToBeExamined) {
							ListIterator<MethodObject> methodIterator = classObject.getMethodIterator();
							while(methodIterator.hasNext()) {
								if(monitor.isCanceled())
									throw new OperationCanceledException();
								MethodObject methodObject = methodIterator.next();
								processMethod(extractedSlices,classObject, methodObject);
								monitor.worked(1);
							}
						}
					}
					else if(!methodObjectsToBeExamined.isEmpty()) {
						int workSize = methodObjectsToBeExamined.size();
						monitor.beginTask("Identification of Extract Method refactoring opportunities", workSize);
						for(MethodObject methodObject : methodObjectsToBeExamined) {
							if(monitor.isCanceled())
								throw new OperationCanceledException();
							ClassObject classObject = systemObject.getClassObject(methodObject.getClassName());
							processMethod(extractedSlices, classObject, methodObject);
							monitor.worked(1);
						}
					}
					monitor.done();
				}
			});

			table = new ASTSlice[extractedSlices.size()];
			for(int i=0; i<extractedSlices.size(); i++) {
				table[i] = extractedSlices.get(i);
			}
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return table;
	}

	private void processMethod(final List<ASTSlice> extractedSlices, ClassObject classObject, MethodObject methodObject) {
		if(methodObject.getMethodBody() != null) {
			IPreferenceStore store = Activator.getDefault().getPreferenceStore();
			int minimumMethodSize = store.getInt(PreferenceConstants.P_MINIMUM_METHOD_SIZE);
			StatementExtractor statementExtractor = new StatementExtractor();
			int numberOfStatements = statementExtractor.getTotalNumberOfStatements(methodObject.getMethodBody().getCompositeStatement().getStatement());
			if(numberOfStatements >= minimumMethodSize) {
				ITypeRoot typeRoot = classObject.getITypeRoot();
				CompilationUnitCache.getInstance().lock(typeRoot);
				CFG cfg = new CFG(methodObject);
				PDG pdg = new PDG(cfg, classObject.getIFile(), classObject.getFieldsAccessedInsideMethod(methodObject), null);
				for(VariableDeclaration declaration : pdg.getVariableDeclarationsInMethod()) {
					PlainVariable variable = new PlainVariable(declaration);
					PDGSliceUnionCollection sliceUnionCollection = new PDGSliceUnionCollection(pdg, variable);
					List<ASTSlice> variableSlices = new ArrayList<ASTSlice>();
					double sumOfExtractedStatementsInGroup = 0.0;
					double sumOfDuplicatedStatementsInGroup = 0.0;
					double sumOfDuplicationRatioInGroup = 0.0;
					int maximumNumberOfExtractedStatementsInGroup = 0;
					for(PDGSliceUnion sliceUnion : sliceUnionCollection.getSliceUnions()) {
						ASTSlice slice = new ASTSlice(sliceUnion);
						int numberOfExtractedStatements = slice.getSliceStatements().size();
						int numberOfRemovableStatements = slice.getRemovableStatements().size();
						int numberOfDuplicatedStatements = numberOfExtractedStatements - numberOfRemovableStatements;
						double duplicationRatio = (double)numberOfDuplicatedStatements/(double)numberOfExtractedStatements;
						sumOfExtractedStatementsInGroup += numberOfExtractedStatements;
						sumOfDuplicatedStatementsInGroup += numberOfDuplicatedStatements;
						sumOfDuplicationRatioInGroup += duplicationRatio;
						if(numberOfExtractedStatements > maximumNumberOfExtractedStatementsInGroup)
							maximumNumberOfExtractedStatementsInGroup = numberOfExtractedStatements;
						variableSlices.add(slice);
						extractedSlices.add(slice);
					}
					for(ASTSlice slice : variableSlices) {
						slice.setAverageNumberOfExtractedStatementsInGroup(sumOfExtractedStatementsInGroup/(double)variableSlices.size());
						slice.setAverageNumberOfDuplicatedStatementsInGroup(sumOfDuplicatedStatementsInGroup/(double)variableSlices.size());
						slice.setAverageDuplicationRatioInGroup(sumOfDuplicationRatioInGroup/(double)variableSlices.size());
						slice.setMaximumNumberOfExtractedStatementsInGroup(maximumNumberOfExtractedStatementsInGroup);
					}
				}
				for(VariableDeclaration declaration : pdg.getVariableDeclarationsAndAccessedFieldsInMethod()) {
					PlainVariable variable = new PlainVariable(declaration);
					PDGObjectSliceUnionCollection objectSliceUnionCollection = new PDGObjectSliceUnionCollection(pdg, variable);
					List<ASTSlice> variableSlices = new ArrayList<ASTSlice>();
					double sumOfExtractedStatementsInGroup = 0.0;
					double sumOfDuplicatedStatementsInGroup = 0.0;
					double sumOfDuplicationRatioInGroup = 0.0;
					int maximumNumberOfExtractedStatementsInGroup = 0;
					for(PDGObjectSliceUnion objectSliceUnion : objectSliceUnionCollection.getSliceUnions()) {
						ASTSlice slice = new ASTSlice(objectSliceUnion);
						int numberOfExtractedStatements = slice.getSliceStatements().size();
						int numberOfRemovableStatements = slice.getRemovableStatements().size();
						int numberOfDuplicatedStatements = numberOfExtractedStatements - numberOfRemovableStatements;
						double duplicationRatio = (double)numberOfDuplicatedStatements/(double)numberOfExtractedStatements;
						sumOfExtractedStatementsInGroup += numberOfExtractedStatements;
						sumOfDuplicatedStatementsInGroup += numberOfDuplicatedStatements;
						sumOfDuplicationRatioInGroup += duplicationRatio;
						if(numberOfExtractedStatements > maximumNumberOfExtractedStatementsInGroup)
							maximumNumberOfExtractedStatementsInGroup = numberOfExtractedStatements;
						variableSlices.add(slice);
						extractedSlices.add(slice);
					}
					for(ASTSlice slice : variableSlices) {
						slice.setAverageNumberOfExtractedStatementsInGroup(sumOfExtractedStatementsInGroup/(double)variableSlices.size());
						slice.setAverageNumberOfDuplicatedStatementsInGroup(sumOfDuplicatedStatementsInGroup/(double)variableSlices.size());
						slice.setAverageDuplicationRatioInGroup(sumOfDuplicationRatioInGroup/(double)variableSlices.size());
						slice.setMaximumNumberOfExtractedStatementsInGroup(maximumNumberOfExtractedStatementsInGroup);
					}
				}
				CompilationUnitCache.getInstance().releaseLock();
			}
		}
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
