package gr.uom.java.jdeodorant.refactoring.views;


import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.CompilationErrorDetectedException;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.jdeodorant.preferences.PreferenceConstants;
import gr.uom.java.jdeodorant.refactoring.Activator;
import gr.uom.java.jdeodorant.refactoring.manipulators.ReplaceConditionalWithPolymorphism;
import gr.uom.java.jdeodorant.refactoring.manipulators.ReplaceTypeCodeWithStateStrategy;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckElimination;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckEliminationGroup;

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
import java.util.Set;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
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
	private static final String MESSAGE_DIALOG_TITLE = "Type Checking";
	private TreeViewer treeViewer;
	private Action identifyBadSmellsAction;
	private Action applyRefactoringAction;
	private Action doubleClickAction;
	private Action renameMethodAction;
	private Action saveResultsAction;
	//private Action evolutionAnalysisAction;
	private IJavaProject selectedProject;
	private IJavaProject activeProject;
	private IPackageFragmentRoot selectedPackageFragmentRoot;
	private IPackageFragment selectedPackageFragment;
	private ICompilationUnit selectedCompilationUnit;
	private IType selectedType;
	private TypeCheckEliminationGroup[] typeCheckEliminationGroupTable;
	//private TypeCheckingEvolution typeCheckingEvolution;

	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */
	 
	class ViewContentProvider implements ITreeContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if(typeCheckEliminationGroupTable != null) {
				return typeCheckEliminationGroupTable;
			}
			else {
				return new TypeCheckEliminationGroup[] {};
			}
		}
		public Object[] getChildren(Object arg) {
			if (arg instanceof TypeCheckEliminationGroup) {
				return ((TypeCheckEliminationGroup)arg).getCandidates().toArray();
			}
			else {
				return new TypeCheckElimination[] {};
			}
		}
		public Object getParent(Object arg0) {
			if(arg0 instanceof TypeCheckElimination) {
				TypeCheckElimination elimination = (TypeCheckElimination)arg0;
				for(int i=0; i<typeCheckEliminationGroupTable.length; i++) {
					if(typeCheckEliminationGroupTable[i].getCandidates().contains(elimination))
						return typeCheckEliminationGroupTable[i];
				}
			}
			return null;
		}
		public boolean hasChildren(Object arg0) {
			return getChildren(arg0).length > 0;
		}
	}
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if(obj instanceof TypeCheckElimination) {
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
				/*case 3:
					return Integer.toString(typeCheckElimination.getGroupSizeAtSystemLevel());*/
				case 4:
					return Integer.toString(typeCheckElimination.getGroupSizeAtClassLevel());
				case 5:
					return Double.toString(typeCheckElimination.getAverageNumberOfStatements());
				case 6:
					Integer userRate = typeCheckElimination.getUserRate();
					return (userRate == null) ? "" : userRate.toString();
				default:
					return "";
				}
			}
			else if(obj instanceof TypeCheckEliminationGroup) {
				TypeCheckEliminationGroup group = (TypeCheckEliminationGroup)obj;
				switch(index){
				case 1:
					return group.toString();
				case 3:
					return Integer.toString(group.getGroupSizeAtSystemLevel());
				case 4:
					return Double.toString(group.getAverageGroupSizeAtClassLevel());
				case 5:
					return Double.toString(group.getAverageNumberOfStatementsInGroup());
				default:
					return "";
				}
			}
			return "";
		}
		public Image getColumnImage(Object obj, int index) {
			Image image = null;
			if(obj instanceof TypeCheckElimination) {
				TypeCheckElimination entry = (TypeCheckElimination)obj;
				int rate = -1;
				Integer userRate = entry.getUserRate();
				if(userRate != null)
					rate = userRate;
				switch(index) {
				case 6:
					if(rate != -1) {
						image = Activator.getImageDescriptor("/icons/" + String.valueOf(rate) + ".jpg").createImage();
					}
				default:
					break;
				}
			}
			return image;
		}
		public Image getImage(Object obj) {
			return null;
		}
	}
	class NameSorter extends ViewerSorter {
		public int compare(Viewer viewer, Object obj1, Object obj2) {
			if(obj1 instanceof TypeCheckEliminationGroup && obj2 instanceof TypeCheckEliminationGroup) {
				TypeCheckEliminationGroup typeCheckEliminationGroup1 = (TypeCheckEliminationGroup)obj1;
				TypeCheckEliminationGroup typeCheckEliminationGroup2 = (TypeCheckEliminationGroup)obj2;
				return typeCheckEliminationGroup1.compareTo(typeCheckEliminationGroup2);
			}
			else {
				TypeCheckElimination typeCheckElimination1 = (TypeCheckElimination)obj1;
				TypeCheckElimination typeCheckElimination2 = (TypeCheckElimination)obj2;
				return typeCheckElimination1.compareTo(typeCheckElimination2);
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
				}
			}
		}
	};

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		treeViewer = new TreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		treeViewer.setContentProvider(new ViewContentProvider());
		treeViewer.setLabelProvider(new ViewLabelProvider());
		treeViewer.setSorter(new NameSorter());
		treeViewer.setInput(getViewSite());
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(50, true));
		layout.addColumnData(new ColumnWeightData(100, true));
		layout.addColumnData(new ColumnWeightData(30, true));
		layout.addColumnData(new ColumnWeightData(20, true));
		layout.addColumnData(new ColumnWeightData(20, true));
		layout.addColumnData(new ColumnWeightData(20, true));
		layout.addColumnData(new ColumnWeightData(20, true));
		treeViewer.getTree().setLayout(layout);
		treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		treeViewer.getTree().setLinesVisible(true);
		treeViewer.getTree().setHeaderVisible(true);
		TreeColumn column0 = new TreeColumn(treeViewer.getTree(),SWT.LEFT);
		column0.setText("Refactoring Type");
		column0.setResizable(true);
		column0.pack();
		TreeColumn column1 = new TreeColumn(treeViewer.getTree(),SWT.LEFT);
		column1.setText("Type Checking Method");
		column1.setResizable(true);
		column1.pack();
		TreeColumn column2 = new TreeColumn(treeViewer.getTree(),SWT.LEFT);
		column2.setText("Abstract Method Name");
		column2.setResizable(true);
		column2.pack();
		TreeColumn column3 = new TreeColumn(treeViewer.getTree(),SWT.LEFT);
		column3.setText("System-Level Occurrences");
		column3.setResizable(true);
		column3.pack();
		TreeColumn column4 = new TreeColumn(treeViewer.getTree(),SWT.LEFT);
		column4.setText("Class-Level Occurrences");
		column4.setResizable(true);
		column4.pack();
		TreeColumn column5 = new TreeColumn(treeViewer.getTree(),SWT.LEFT);
		column5.setText("Average #statements per case");
		column5.setResizable(true);
		column5.pack();
		
		TreeColumn column6 = new TreeColumn(treeViewer.getTree(),SWT.LEFT);
		column6.setText("Rate it!");
		column6.setResizable(true);
		column6.pack();
		treeViewer.expandAll();
		
		treeViewer.setColumnProperties(new String[] {"type", "source", "methodName", "systemOccurrences", "classOccurrences", "averageStatements", "rate"});
		treeViewer.setCellEditors(new CellEditor[] {
				new TextCellEditor(), new TextCellEditor(), new TextCellEditor(), new TextCellEditor(), new TextCellEditor(), new TextCellEditor(),
				new MyComboBoxCellEditor(treeViewer.getTree(), new String[] {"0", "1", "2", "3", "4", "5"}, SWT.READ_ONLY)
		});
		
		treeViewer.setCellModifier(new ICellModifier() {
			public boolean canModify(Object element, String property) {
				return property.equals("rate");
			}

			public Object getValue(Object element, String property) {
				if(element instanceof TypeCheckElimination) {
					TypeCheckElimination elimination = (TypeCheckElimination)element;
					if(elimination.getUserRate() != null)
						return elimination.getUserRate();
					else
						return 0;
				}
				return 0;
			}

			public void modify(Object element, String property, Object value) {
				TreeItem item = (TreeItem)element;
				Object data = item.getData();
				if(data instanceof TypeCheckElimination) {
					TypeCheckElimination elimination = (TypeCheckElimination)data;
					elimination.setUserRate((Integer)value);
					IPreferenceStore store = Activator.getDefault().getPreferenceStore();
					boolean allowUsageReporting = store.getBoolean(PreferenceConstants.P_ENABLE_USAGE_REPORTING);
					if(allowUsageReporting) {
						Tree tree = treeViewer.getTree();
						int groupPosition = -1;
						int totalGroups = tree.getItemCount();
						int groupSizeAtSystemLevel = 0;
						for(int i=0; i<tree.getItemCount(); i++) {
							TreeItem treeItem = tree.getItem(i);
							TypeCheckEliminationGroup group = (TypeCheckEliminationGroup)treeItem.getData();
							if(group.getCandidates().contains(elimination)) {
								groupPosition = i;
								groupSizeAtSystemLevel = group.getGroupSizeAtSystemLevel();
								break;
							}
						}
						try {
							boolean allowSourceCodeReporting = store.getBoolean(PreferenceConstants.P_ENABLE_SOURCE_CODE_REPORTING);
							String declaringClass = elimination.getTypeCheckClass().resolveBinding().getQualifiedName();
							String methodName = elimination.getTypeCheckMethod().resolveBinding().toString();
							String sourceMethodName = declaringClass + "::" + methodName;
							String content = URLEncoder.encode("project_name", "UTF-8") + "=" + URLEncoder.encode(activeProject.getElementName(), "UTF-8");
							content += "&" + URLEncoder.encode("source_method_name", "UTF-8") + "=" + URLEncoder.encode(sourceMethodName, "UTF-8");
							content += "&" + URLEncoder.encode("system_level_occurrences", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(groupSizeAtSystemLevel), "UTF-8");
							content += "&" + URLEncoder.encode("class_level_occurrences", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(elimination.getGroupSizeAtClassLevel()), "UTF-8");
							content += "&" + URLEncoder.encode("average_statements_per_branch", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(elimination.getAverageNumberOfStatements()), "UTF-8");
							content += "&" + URLEncoder.encode("branches", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(elimination.getTypeCheckExpressions().size()), "UTF-8");
							int totalNumberOfStates = -1;
							if(elimination.getExistingInheritanceTree() == null && elimination.getInheritanceTreeMatchingWithStaticTypes() == null)
								totalNumberOfStates = elimination.getStaticFields().size() + elimination.getAdditionalStaticFields().size();
							content += "&" + URLEncoder.encode("total_states", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(totalNumberOfStates), "UTF-8");
							content += "&" + URLEncoder.encode("ranking_position", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(groupPosition), "UTF-8");
							content += "&" + URLEncoder.encode("total_opportunities", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(totalGroups), "UTF-8");
							if(allowSourceCodeReporting)
								content += "&" + URLEncoder.encode("conditional_code_fragment", "UTF-8") + "=" + URLEncoder.encode(elimination.getTypeCheckCodeFragment().toString(), "UTF-8");
							content += "&" + URLEncoder.encode("rating", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(elimination.getUserRate()), "UTF-8");
							content += "&" + URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(System.getProperty("user.name"), "UTF-8");
							content += "&" + URLEncoder.encode("tb", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8");
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
					treeViewer.update(data, null);
				}
			}
		});
		
		makeActions();
		hookDoubleClickAction();
		contributeToActionBars();
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(selectionListener);
		JavaCore.addElementChangedListener(ElementChangedListener.getInstance());
		getSite().getWorkbenchWindow().getWorkbench().getOperationSupport().getOperationHistory().addOperationHistoryListener(new IOperationHistoryListener() {
			public void historyNotification(OperationHistoryEvent event) {
				int eventType = event.getEventType();
				if(eventType == OperationHistoryEvent.UNDONE  || eventType == OperationHistoryEvent.REDONE ||
						eventType == OperationHistoryEvent.OPERATION_ADDED || eventType == OperationHistoryEvent.OPERATION_REMOVED) {
					if(activeProject != null && CompilationUnitCache.getInstance().getAffectedProjects().contains(activeProject)) {
						applyRefactoringAction.setEnabled(false);
						renameMethodAction.setEnabled(false);
						saveResultsAction.setEnabled(false);
						//evolutionAnalysisAction.setEnabled(false);
					}
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
		//manager.add(evolutionAnalysisAction);
	}

	private void makeActions() {
		identifyBadSmellsAction = new Action() {
			public void run() {
				activeProject = selectedProject;
				CompilationUnitCache.getInstance().clearCache();
				typeCheckEliminationGroupTable = getTable();
				treeViewer.setContentProvider(new ViewContentProvider());
				applyRefactoringAction.setEnabled(true);
				renameMethodAction.setEnabled(true);
				saveResultsAction.setEnabled(true);
				//evolutionAnalysisAction.setEnabled(true);
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
		
		/*evolutionAnalysisAction = new Action() {
			public void run() {
				typeCheckingEvolution = null;
				IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
				if(selection.getFirstElement() instanceof TypeCheckElimination) {
					final TypeCheckElimination typeCheckElimination = (TypeCheckElimination)selection.getFirstElement();
					try {
						IWorkbench wb = PlatformUI.getWorkbench();
						IProgressService ps = wb.getProgressService();
						ps.busyCursorWhile(new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								ProjectEvolution projectEvolution = new ProjectEvolution(selectedProject);
								if(projectEvolution.getProjectEntries().size() > 1) {
									typeCheckingEvolution = new TypeCheckingEvolution(projectEvolution, typeCheckElimination, monitor);
								}
							}
						});
						if(typeCheckingEvolution != null) {
							EvolutionDialog dialog = new EvolutionDialog(getSite().getWorkbenchWindow(), typeCheckingEvolution, "Type Checking Evolution", false);
							dialog.open();
						}
						else
							MessageDialog.openInformation(getSite().getShell(), "Type Checking Evolution",
									"Type Checking evolution analysis cannot be performed, since only a single version of the examined project is loaded in the workspace.");
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		evolutionAnalysisAction.setToolTipText("Evolution Analysis");
		evolutionAnalysisAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJ_ELEMENT));
		evolutionAnalysisAction.setEnabled(false);*/
		
		applyRefactoringAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
				if(selection != null && selection.getFirstElement() instanceof TypeCheckElimination) {
					TypeCheckElimination typeCheckElimination = (TypeCheckElimination)selection.getFirstElement();
					TypeDeclaration sourceTypeDeclaration = typeCheckElimination.getTypeCheckClass();
					CompilationUnit sourceCompilationUnit = (CompilationUnit)sourceTypeDeclaration.getRoot();
					IFile sourceFile = typeCheckElimination.getTypeCheckIFile();
					IPreferenceStore store = Activator.getDefault().getPreferenceStore();
					boolean allowUsageReporting = store.getBoolean(PreferenceConstants.P_ENABLE_USAGE_REPORTING);
					if(allowUsageReporting) {
						Tree tree = treeViewer.getTree();
						int groupPosition = -1;
						int totalGroups = tree.getItemCount();
						int groupSizeAtSystemLevel = 0;
						for(int i=0; i<tree.getItemCount(); i++) {
							TreeItem treeItem = tree.getItem(i);
							TypeCheckEliminationGroup group = (TypeCheckEliminationGroup)treeItem.getData();
							if(group.getCandidates().contains(typeCheckElimination)) {
								groupPosition = i;
								groupSizeAtSystemLevel = group.getGroupSizeAtSystemLevel();
								break;
							}
						}
						try {
							boolean allowSourceCodeReporting = store.getBoolean(PreferenceConstants.P_ENABLE_SOURCE_CODE_REPORTING);
							String declaringClass = typeCheckElimination.getTypeCheckClass().resolveBinding().getQualifiedName();
							String methodName = typeCheckElimination.getTypeCheckMethod().resolveBinding().toString();
							String sourceMethodName = declaringClass + "::" + methodName;
							String content = URLEncoder.encode("project_name", "UTF-8") + "=" + URLEncoder.encode(activeProject.getElementName(), "UTF-8");
							content += "&" + URLEncoder.encode("source_method_name", "UTF-8") + "=" + URLEncoder.encode(sourceMethodName, "UTF-8");
							content += "&" + URLEncoder.encode("system_level_occurrences", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(groupSizeAtSystemLevel), "UTF-8");
							content += "&" + URLEncoder.encode("class_level_occurrences", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(typeCheckElimination.getGroupSizeAtClassLevel()), "UTF-8");
							content += "&" + URLEncoder.encode("average_statements_per_branch", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(typeCheckElimination.getAverageNumberOfStatements()), "UTF-8");
							content += "&" + URLEncoder.encode("branches", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(typeCheckElimination.getTypeCheckExpressions().size()), "UTF-8");
							int totalNumberOfStates = -1;
							if(typeCheckElimination.getExistingInheritanceTree() == null && typeCheckElimination.getInheritanceTreeMatchingWithStaticTypes() == null)
								totalNumberOfStates = typeCheckElimination.getStaticFields().size() + typeCheckElimination.getAdditionalStaticFields().size();
							content += "&" + URLEncoder.encode("total_states", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(totalNumberOfStates), "UTF-8");
							content += "&" + URLEncoder.encode("ranking_position", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(groupPosition), "UTF-8");
							content += "&" + URLEncoder.encode("total_opportunities", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(totalGroups), "UTF-8");
							if(allowSourceCodeReporting)
								content += "&" + URLEncoder.encode("conditional_code_fragment", "UTF-8") + "=" + URLEncoder.encode(typeCheckElimination.getTypeCheckCodeFragment().toString(), "UTF-8");
							content += "&" + URLEncoder.encode("application", "UTF-8") + "=" + URLEncoder.encode(String.valueOf("1"), "UTF-8");
							content += "&" + URLEncoder.encode("application_selected_name", "UTF-8") + "=" + URLEncoder.encode(typeCheckElimination.getAbstractMethodName(), "UTF-8");
							content += "&" + URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(System.getProperty("user.name"), "UTF-8");
							content += "&" + URLEncoder.encode("tb", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8");
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
					Refactoring refactoring = null;
					if(typeCheckElimination.getExistingInheritanceTree() == null) {
						refactoring = new ReplaceTypeCodeWithStateStrategy(sourceFile, sourceCompilationUnit, sourceTypeDeclaration, typeCheckElimination);
					}
					else {
						refactoring = new ReplaceConditionalWithPolymorphism(sourceFile, sourceCompilationUnit, sourceTypeDeclaration, typeCheckElimination);
					}
					try {
						IJavaElement sourceJavaElement = JavaCore.create(sourceFile);
						JavaUI.openInEditor(sourceJavaElement);
					} catch (PartInitException e) {
						e.printStackTrace();
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
					MyRefactoringWizard wizard = new MyRefactoringWizard(refactoring, applyRefactoringAction);
					RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard); 
					try { 
						String titleForFailedChecks = ""; //$NON-NLS-1$ 
						op.run(getSite().getShell(), titleForFailedChecks); 
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		applyRefactoringAction.setToolTipText("Apply Refactoring");
		applyRefactoringAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_DEF_VIEW));
		applyRefactoringAction.setEnabled(false);
		
		renameMethodAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
				if(selection != null && selection.getFirstElement() instanceof TypeCheckElimination) {
					TypeCheckElimination entry = (TypeCheckElimination)selection.getFirstElement();
					String methodName = entry.getAbstractMethodName();
					IInputValidator methodNameValidator = new MethodNameValidator();
					InputDialog dialog = new InputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Rename Method", "Please enter a new name", methodName, methodNameValidator);
					dialog.open();
					if(dialog.getValue() != null) {
						entry.setAbstractMethodName(dialog.getValue());
						treeViewer.refresh();
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
				IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
				if(selection.getFirstElement() instanceof TypeCheckElimination) {
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

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}

	public void dispose() {
		super.dispose();
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(selectionListener);
	}

	private TypeCheckEliminationGroup[] getTable() {
		TypeCheckEliminationGroup[] table = null;
		try {
			IWorkbench wb = PlatformUI.getWorkbench();
			IProgressService ps = wb.getProgressService();
			if(ASTReader.getSystemObject() != null && activeProject.equals(ASTReader.getExaminedProject())) {
				new ASTReader(activeProject, ASTReader.getSystemObject(), null);
			}
			else {
				ps.busyCursorWhile(new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							new ASTReader(activeProject, monitor);
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
			final SystemObject systemObject = ASTReader.getSystemObject();
			if(systemObject != null) {
				Set<ClassObject> classObjectsToBeExamined = new LinkedHashSet<ClassObject>();
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
				final Set<ClassObject> filteredClassObjectsToBeExamined = new LinkedHashSet<ClassObject>();
				for(ClassObject classObject : classObjectsToBeExamined) {
					if(!classObject.isEnum() && !classObject.isInterface() && !classObject.isGeneratedByParserGenenator()) {
						filteredClassObjectsToBeExamined.add(classObject);
					}
				}
				final List<TypeCheckEliminationGroup> typeCheckEliminationGroups = new ArrayList<TypeCheckEliminationGroup>();
				ps.busyCursorWhile(new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						typeCheckEliminationGroups.addAll(systemObject.generateTypeCheckEliminations(filteredClassObjectsToBeExamined, monitor));
					}
				});

				table = new TypeCheckEliminationGroup[typeCheckEliminationGroups.size()];
				int i = 0;
				for(TypeCheckEliminationGroup typeCheckEliminationGroup : typeCheckEliminationGroups) {
					table[i] = typeCheckEliminationGroup;
					i++;
				}
			}
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (CompilationErrorDetectedException e) {
			MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), MESSAGE_DIALOG_TITLE,
					"Compilation errors were detected in the project. Fix the errors before using JDeodorant.");
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
        		Tree tree = treeViewer.getTree();
        		/*TableColumn[] columns = table.getColumns();
        		for(int i=0; i<columns.length; i++) {
        			if(i == columns.length-1)
        				out.write(columns[i].getText());
        			else
        				out.write(columns[i].getText() + "\t");
        		}
        		out.newLine();*/
        		for(int i=0; i<tree.getItemCount(); i++) {
        			TreeItem treeItem = tree.getItem(i);
        			TypeCheckEliminationGroup group = (TypeCheckEliminationGroup)treeItem.getData();
        			for(TypeCheckElimination candidate : group.getCandidates()) {
        				out.write(candidate.toString());
        				out.newLine();
        			}
        		}
        		out.close();
        	} catch (IOException e) {
        		e.printStackTrace();
        	}
        }
	}
}