package gr.uom.java.jdeodorant.refactoring.views;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.decomposition.cfg.mapping.CloneStructureNode;
import gr.uom.java.ast.decomposition.cfg.mapping.DivideAndConquerMatcher;
import gr.uom.java.ast.decomposition.cfg.mapping.VariableBindingPair;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;
import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractCloneRefactoring;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Text;

public class CloneDiffWizardPage extends UserInputWizardPage {

	private static final Font CONSOLAS_NORMAL_FONT = new Font(null, new FontData("consolas", 9, SWT.NORMAL));
	private static final Font CONSOLAS_BOLD_FONT = new Font(null, new FontData("consolas", 9, SWT.BOLD));
	private ExtractCloneRefactoring refactoring;
	private List<? extends DivideAndConquerMatcher> mappers;
	private DivideAndConquerMatcher mapper;
	private CloneStructureNode cloneStructureRoot;
	private TreeViewer treeViewerLeft;
	private TreeViewer treeViewerRight;
	private Text extractedMethodNameField;
	private Group renamedVariables;
	//Special Boolean for selection synchronization listeners
	private boolean correspondingTreeAlreadyChanged = false;
	
	public CloneDiffWizardPage(ExtractCloneRefactoring refactoring) {
		super("Diff Clones");
		this.refactoring = refactoring;
		this.mappers = refactoring.getMappers();
		this.mapper = mappers.get(0);
		this.cloneStructureRoot = mapper.getCloneStructureRoot();
	}
	
	public void createControl(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout gridLayout = new GridLayout(6, true);
		//gridLayout.numColumns = 6;
		//gridLayout.horizontalSpacing = 15;
		//gridLayout.marginLeft = 15;
		//gridLayout.marginRight = 15;
		result.setLayout(gridLayout);
	
		Label methodLeftName = new Label(result, SWT.WRAP);
		methodLeftName.setText(mapper.getPDG1().getMethod().toString());
		methodLeftName.setFont(CONSOLAS_NORMAL_FONT);
		GridData methodLeftNameGridData = new GridData(SWT.LEFT, SWT.FILL, true, false);
		methodLeftNameGridData.horizontalSpan = 3;
		methodLeftName.setLayoutData(methodLeftNameGridData);
		
		Label methodRightName = new Label(result, SWT.WRAP);
		methodRightName.setText(mapper.getPDG2().getMethod().toString());
		methodRightName.setFont(CONSOLAS_NORMAL_FONT);
		GridData methodRightNameGridData = new GridData(SWT.LEFT, SWT.FILL, true, false);
		methodRightNameGridData.horizontalSpan = 3;
		methodRightName.setLayoutData(methodRightNameGridData);
		
		Label selectRefactoringOpportunityLabel = new Label(result, SWT.WRAP);
		selectRefactoringOpportunityLabel.setText("Select Refactoring Opportunity:");
		selectRefactoringOpportunityLabel.setFont(CONSOLAS_BOLD_FONT);
		GridData selectRefactoringOpportunityLabelData = new GridData();
		selectRefactoringOpportunityLabelData.horizontalSpan = 1;
		selectRefactoringOpportunityLabel.setLayoutData(selectRefactoringOpportunityLabelData);
		
		Combo combo = new Combo(result, SWT.READ_ONLY);
		GridData comboData = new GridData();
		comboData.horizontalSpan = 2;
		combo.setLayoutData(comboData);
		final ComboViewer comboViewer = new ComboViewer(combo);
		comboViewer.setContentProvider(ArrayContentProvider.getInstance());
		comboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if(element instanceof DivideAndConquerMatcher) {
					DivideAndConquerMatcher mapper = (DivideAndConquerMatcher)element;
					int index = mappers.indexOf(mapper);
					return "Subtree " + (index+1);
				}
				return super.getText(element);
			}
		});
		comboViewer.setInput(mappers.toArray());
		comboViewer.setSelection(new StructuredSelection(mappers.get(0)));
		
		Label selectMethodNameLabel = new Label(result, SWT.WRAP);
		selectMethodNameLabel.setText("Specify Extracted Method Name:");
		selectMethodNameLabel.setFont(CONSOLAS_BOLD_FONT);
		GridData selectMethodNameLabelData = new GridData();
		selectMethodNameLabelData.horizontalSpan = 1;
		selectMethodNameLabel.setLayoutData(selectMethodNameLabelData);
		
		extractedMethodNameField = new Text(result, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		GridData nameFieldGridData = new GridData(GridData.FILL_HORIZONTAL);
		nameFieldGridData.horizontalSpan = 2;
		extractedMethodNameField.setLayoutData(nameFieldGridData);
		extractedMethodNameField.setText(refactoring.getExtractedMethodName());
		extractedMethodNameField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				handleInputChanged();
			}
		});
		
		treeViewerLeft = new TreeViewer(result, SWT.PUSH | SWT.V_SCROLL | SWT.H_SCROLL);
		treeViewerLeft.setLabelProvider(new CloneDiffStyledLabelProvider(CloneDiffSide.LEFT));
		treeViewerLeft.setContentProvider(new CloneDiffContentProvider());
		treeViewerLeft.setInput(new CloneStructureNode[]{cloneStructureRoot});
		treeViewerLeft.getTree().setLinesVisible(true);
		treeViewerLeft.expandAll();
		//GridData
		GridData treeLeftGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		treeLeftGridData.horizontalAlignment = SWT.FILL;
		treeLeftGridData.verticalAlignment = SWT.FILL;
		treeLeftGridData.horizontalSpan = 3;
		//treeLeftGridData.verticalSpan = 2;
		treeViewerLeft.getTree().setLayoutData(treeLeftGridData);
		
		treeViewerRight = new TreeViewer(result, SWT.PUSH | SWT.V_SCROLL | SWT.H_SCROLL);
		treeViewerRight.setLabelProvider(new CloneDiffStyledLabelProvider(CloneDiffSide.RIGHT));
		treeViewerRight.setContentProvider(new CloneDiffContentProvider());
		treeViewerRight.setInput(new CloneStructureNode[]{cloneStructureRoot});
		treeViewerRight.getTree().setLinesVisible(true);
		treeViewerRight.expandAll();
		//GridData
		GridData treeRightGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		treeRightGridData.horizontalAlignment = SWT.FILL;
		treeRightGridData.verticalAlignment = SWT.FILL;
		treeRightGridData.horizontalSpan = 3;
		//treeRightGridData.verticalSpan = 2;
		treeViewerRight.getTree().setLayoutData(treeRightGridData);
	
		comboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				DivideAndConquerMatcher selectedMapper = (DivideAndConquerMatcher)selection.getFirstElement();
				CloneStructureNode selectedCloneStructureRoot = selectedMapper.getCloneStructureRoot();
				if(cloneStructureRoot != selectedCloneStructureRoot) {
					mapper = selectedMapper;
					cloneStructureRoot = selectedCloneStructureRoot;
					refactoring.setMapper(mapper);
					treeViewerLeft.setInput(new CloneStructureNode[]{cloneStructureRoot});
					treeViewerLeft.refresh();
					treeViewerRight.setInput(new CloneStructureNode[]{cloneStructureRoot});
					treeViewerRight.refresh();
					treeViewerLeft.expandAll();
					treeViewerRight.expandAll();
					updateRenamedVariables();
				}
			}
		});
		//Information Footer
		//Legend
		final Group legend = new Group(result, SWT.SHADOW_NONE);
		legend.setText("Legend");
		GridData legendGridData = new GridData(SWT.LEFT, SWT.FILL, true, false);
		legendGridData.horizontalSpan = 3;
		legendGridData.verticalSpan = 5;
		legend.setLayoutData(legendGridData);
		GridLayout legendLayout = new GridLayout();
		legendLayout.numColumns = 6;
		legend.setLayout(legendLayout);
		
		CLabel differencesLegendColor = new CLabel(legend, SWT.BORDER);
		differencesLegendColor.setText("           ");
		differencesLegendColor.setBackground(StyledStringVisitor.DIFFERENCE_COLOR);
		CLabel differencesLegendLabel = new CLabel(legend, SWT.NONE);
		differencesLegendLabel.setText("Difference");
		
		//Unmapped Statements Label
		CLabel unmappedStatementsLegendColor = new CLabel(legend, SWT.BORDER);
		unmappedStatementsLegendColor.setText("           ");
		unmappedStatementsLegendColor.setBackground(StyledStringVisitor.UNMAPPED_COLOR);
		CLabel unmappedStatementsLegendLabel = new CLabel(legend, SWT.NONE);
		unmappedStatementsLegendLabel.setText("Unmapped Statement");
		
		//Unmapped Statements Label
		CLabel advancedMatchLegendColor = new CLabel(legend, SWT.BORDER);
		advancedMatchLegendColor.setText("           ");
		advancedMatchLegendColor.setBackground(StyledStringVisitor.ADVANCED_MATCH_COLOR);
		CLabel advancedMatchLegendLabel = new CLabel(legend, SWT.NONE);
		advancedMatchLegendLabel.setText("Advanced Match");
		
		renamedVariables = new Group(result, SWT.SHADOW_NONE);
		renamedVariables.setText("Renamed Variables");
		GridData renamedVariablesGridData = new GridData(SWT.LEFT, SWT.FILL, true, false);
		renamedVariablesGridData.horizontalSpan = 3;
		renamedVariablesGridData.verticalSpan = 5;
		renamedVariables.setLayoutData(renamedVariablesGridData);
		GridLayout renamedVariablesLayout = new GridLayout();
		renamedVariablesLayout.numColumns = 3;
		renamedVariables.setLayout(renamedVariablesLayout);
		
		updateRenamedVariables();
		
		handleInputChanged();
		@SuppressWarnings("unused")
		CloneDiffTooltip tooltipLeft = new CloneDiffTooltip(treeViewerLeft, ToolTip.NO_RECREATE, false);
		@SuppressWarnings("unused")
		CloneDiffTooltip tooltipRight = new CloneDiffTooltip(treeViewerRight, ToolTip.NO_RECREATE, false);
		
		//Selection Listeners - Synchronize Selections
		treeViewerLeft.addPostSelectionChangedListener(new ISelectionChangedListener() { 
			public void selectionChanged(SelectionChangedEvent event) {
				if (!correspondingTreeAlreadyChanged){
					TreeSelection treeSelection = (TreeSelection) event.getSelection();
					correspondingTreeAlreadyChanged = true;
					treeViewerRight.setSelection(treeSelection);
					correspondingTreeAlreadyChanged = false;
				}
				return;
			}
		});
		treeViewerRight.addPostSelectionChangedListener(new ISelectionChangedListener() {
			
			public void selectionChanged(SelectionChangedEvent event) {
				if (!correspondingTreeAlreadyChanged){
					TreeSelection treeSelection = (TreeSelection) event.getSelection();
					correspondingTreeAlreadyChanged = true;
					treeViewerLeft.setSelection(treeSelection);
					correspondingTreeAlreadyChanged = false;
				}
				return;
				
			}
		});
		
		//Synchronize Expands and Collapses
		treeViewerLeft.addTreeListener(new ITreeViewerListener() {
			
			public void treeExpanded(TreeExpansionEvent event) {
				CloneStructureNode nodeExpanded = (CloneStructureNode) event.getElement();
				treeViewerRight.expandToLevel(nodeExpanded, 1);
			}
			
			public void treeCollapsed(TreeExpansionEvent event) {
				CloneStructureNode nodeCollapsed = (CloneStructureNode) event.getElement();
				treeViewerRight.collapseToLevel(nodeCollapsed, 1);
				
			}
		});
		treeViewerRight.addTreeListener(new ITreeViewerListener() {
			
			public void treeExpanded(TreeExpansionEvent event) {
				CloneStructureNode nodeExpanded = (CloneStructureNode) event.getElement();
				treeViewerLeft.expandToLevel(nodeExpanded, 1);
			}
			
			public void treeCollapsed(TreeExpansionEvent event) {
				CloneStructureNode nodeCollapsed = (CloneStructureNode) event.getElement();
				treeViewerLeft.collapseToLevel(nodeCollapsed, 1);
				
			}
		});
		
		//Synchronize Vertical ScrollBars
		ScrollBar leftVertical = treeViewerLeft.getTree().getVerticalBar();
		leftVertical.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				treeViewerRight.getTree().setTopItem(treeViewerLeft.getTree().getTopItem());   //setTopItem(treeViewerLeft.getTree().getTopItem());
			}
		});
		ScrollBar rightVertical = treeViewerRight.getTree().getVerticalBar();
		rightVertical.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				treeViewerLeft.getTree().setTopItem(treeViewerRight.getTree().getTopItem());
			}
		});
		
		//Synchronize Horizontal ScrollBars
		final ScrollBar leftHorizontal = treeViewerLeft.getTree().getHorizontalBar();
		final ScrollBar rightHorizontal = treeViewerRight.getTree().getHorizontalBar();
		leftHorizontal.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				rightHorizontal.setValues(
						leftHorizontal.getSelection(),
						leftHorizontal.getMinimum(),
						leftHorizontal.getMaximum(),
						leftHorizontal.getThumb(),
						leftHorizontal.getIncrement(),
						leftHorizontal.getPageIncrement());
			}
		});
		
		rightHorizontal.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				leftHorizontal.setValues(
						rightHorizontal.getSelection(),
						rightHorizontal.getMinimum(),
						rightHorizontal.getMaximum(),
						rightHorizontal.getThumb(),
						rightHorizontal.getIncrement(),
						rightHorizontal.getPageIncrement());
			}
		});
	}

	private void updateRenamedVariables() {
		Control[] children = renamedVariables.getChildren();
		for(Control child : children) {
			child.dispose();
		}
		Set<VariableBindingPair> renamedVariableBindingPairs = mapper.getRenamedVariableBindings();
		if(renamedVariableBindingPairs.size() > 0) {
			for(VariableBindingPair bindingPair : renamedVariableBindingPairs) {
				CLabel renamedVariableLabel = new CLabel(renamedVariables, SWT.NONE);
				String variable1 = bindingPair.getBinding1().getName();
				String variable2 = bindingPair.getBinding2().getName();
				renamedVariableLabel.setText(variable1 + " -> " + variable2);
				renamedVariableLabel.setFont(CONSOLAS_BOLD_FONT);
			}
		}
		renamedVariables.layout();
		renamedVariables.pack();
	}

	private void handleInputChanged() {
		String methodNamePattern = "[a-zA-Z\\$_][a-zA-Z0-9\\$_]*";
		ITypeBinding typeBinding1 = mapper.getPDG1().getMethod().getMethodDeclaration().resolveBinding().getDeclaringClass();
		ITypeBinding typeBinding2 = mapper.getPDG2().getMethod().getMethodDeclaration().resolveBinding().getDeclaringClass();
		ITypeBinding commonSuperclass = ASTNodeMatcher.commonSuperType(typeBinding1, typeBinding2);
		if(!Pattern.matches(methodNamePattern, extractedMethodNameField.getText())) {
			setPageComplete(false);
			String message = "Method name \"" + extractedMethodNameField.getText() + "\" is not valid";
			setMessage(message, ERROR);
			return;
		}
		else {
			if(typeBinding1 != null && methodDeclaredInTypeBinding(typeBinding1, extractedMethodNameField.getText())) {
				setPageComplete(false);
				String message = "A method with name \"" + extractedMethodNameField.getText() + "\" is declared in class " +
						typeBinding1.getQualifiedName();
				setMessage(message, ERROR);
				return;
			}
			if(typeBinding2 != null && methodDeclaredInTypeBinding(typeBinding2, extractedMethodNameField.getText())) {
				setPageComplete(false);
				String message = "A method with name \"" + extractedMethodNameField.getText() + "\" is declared in class " +
						typeBinding2.getQualifiedName();
				setMessage(message, ERROR);
				return;
			}
			if(commonSuperclass != null && methodDeclaredInTypeBinding(commonSuperclass, extractedMethodNameField.getText())) {
				setPageComplete(false);
				String message = "A method with name \"" + extractedMethodNameField.getText() + "\" is declared in class " +
						commonSuperclass.getQualifiedName();
				setMessage(message, ERROR);
				return;
			}
		}
		refactoring.setExtractedMethodName(extractedMethodNameField.getText());
		setPageComplete(true);
		setMessage("", NONE);
	}
	
	private boolean methodDeclaredInTypeBinding(ITypeBinding typeBinding, String methodName) {
		if(!typeBinding.getQualifiedName().equals("java.lang.Object") &&
				ASTReader.getSystemObject().getClassObject(typeBinding.getQualifiedName()) != null) {
			IMethodBinding[] declaredMethods = typeBinding.getDeclaredMethods();
			for(IMethodBinding declaredMethod : declaredMethods) {
				if(declaredMethod.getName().equals(methodName)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public void dispose() {
		super.dispose();
		treeViewerLeft.getTree().dispose();
		treeViewerRight.getTree().dispose();
	}
}
