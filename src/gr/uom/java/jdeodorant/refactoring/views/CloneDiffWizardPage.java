package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.decomposition.cfg.mapping.CloneStructureNode;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGSubTreeMapper;
import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractCloneRefactoring;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;

public class CloneDiffWizardPage extends UserInputWizardPage {

	private PDGSubTreeMapper mapper;
	private CloneStructureNode cloneStructureRoot;
	//Special Boolean for selection synchronization listeners
	private boolean correspondingTreeAlreadyChanged = false;
	
	public CloneDiffWizardPage(ExtractCloneRefactoring refactoring) {
		super("Diff Clones");
		this.mapper = refactoring.getMapper();
		this.cloneStructureRoot = mapper.getCloneStructureRoot();
	}
	
	public void createControl(Composite parent){
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
		methodLeftName.setFont(new Font(parent.getDisplay(), new FontData("consolas", 9, SWT.NORMAL)));
		GridData methodLeftNameGridData = new GridData(SWT.LEFT, SWT.FILL, true, false);
		methodLeftNameGridData.horizontalSpan = 3;
		methodLeftName.setLayoutData(methodLeftNameGridData);
		
		Label methodRightName = new Label(result, SWT.WRAP);
		methodRightName.setText(mapper.getPDG2().getMethod().toString());
		methodRightName.setFont(new Font(parent.getDisplay(), new FontData("consolas", 9, SWT.NORMAL)));
		GridData methodRightNameGridData = new GridData(SWT.LEFT, SWT.FILL, true, false);
		methodRightNameGridData.horizontalSpan = 3;
		methodRightName.setLayoutData(methodRightNameGridData);
		
		
		final TreeViewer treeViewerLeft = new TreeViewer(result, SWT.PUSH | SWT.V_SCROLL | SWT.H_SCROLL);
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
		
		final TreeViewer treeViewerRight = new TreeViewer(result, SWT.PUSH | SWT.V_SCROLL | SWT.H_SCROLL);
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
	
		//Information Footer
		//Legend
		final Group legend = new Group(result, SWT.SHADOW_NONE);
		legend.setText("Legend");
		GridData legendGridData = new GridData(SWT.LEFT, SWT.FILL, true, false);
		legendGridData.horizontalSpan = 2;
		legendGridData.verticalSpan = 5;
		legend.setLayoutData(legendGridData);
		GridLayout legendLayout = new GridLayout();
		legendLayout.numColumns = 2;
		legend.setLayout(legendLayout);
		//Unmapped Statements Label
		CLabel unmappedStatementsLegendColor = new CLabel(legend, SWT.NONE);
		unmappedStatementsLegendColor.setText("           ");
		unmappedStatementsLegendColor.setBackground(new Color(null, 255, 156, 156));
		CLabel unmappedStatementsLegendLabel = new CLabel(legend, SWT.NONE);
		unmappedStatementsLegendLabel.setText("Unmapped Statement");
		
		CLabel differencesLegendColor = new CLabel(legend, SWT.NONE);
		differencesLegendColor.setText("           ");
		differencesLegendColor.setBackground(new Color(null, 255, 255, 200));
		CLabel differencesLegendLabel = new CLabel(legend, SWT.NONE);
		differencesLegendLabel.setText("Difference");
		
		
		/*//LegendItem
		StyledText unmappedLabel = new StyledText(legend, SWT.SINGLE | SWT.READ_ONLY);
		unmappedLabel.setText("                        Unmapped Statements  ");
		StyleRange unmappedStyleRange = new StyleRange();
		unmappedStyleRange.start = 0;
		unmappedStyleRange.length = 20;
		Color unmappedColor = new Color(null, 255, 156, 156);
		unmappedStyleRange.background = unmappedColor;
		unmappedLabel.setStyleRange(unmappedStyleRange);
		
		StyledText differencesLabel = new StyledText(legend, SWT.MULTI | SWT.READ_ONLY);
		differencesLabel.setText("                        Differences within Mapped Statements  ");
		StyleRange differenceStyleRange = new StyleRange();
		differenceStyleRange.start = 0;
		differenceStyleRange.length = 20;
		Color differencesColor = new Color(null, 255, 255, 200);
		differenceStyleRange.background= differencesColor;
		differencesLabel.setStyleRange(differenceStyleRange);
		
		GridData differencesLabelGridData = new GridData(SWT.LEFT, SWT.FILL, true, false);
		differencesLabelGridData.horizontalSpan = 2;
		differencesLabelGridData.verticalSpan = 5;
		differencesLabel.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, true, false));*/
	
		
		/*Label differencesLabel = new Label(legend, SWT.DEFAULT);
		differencesLabel.setText("Hello");
		GridData differencesLabelGridData = new GridData(SWT.LEFT, SWT.FILL, true, false);
		differencesLabelGridData.horizontalSpan = 2;
		differencesLabelGridData.verticalSpan = 5;
		differencesLabel.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, true, false));*/
	
	
		//Tooltips
		//ColumnViewerToolTipSupport.enableFor(treeViewerLeft);
		//ColumnViewerToolTipSupport.enableFor(treeViewerRight);
		
		
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
}
