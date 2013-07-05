package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.decomposition.cfg.mapping.CloneStructureNode;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGMapper;
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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;

public class CloneDiffWizardPage extends UserInputWizardPage {

	PDGMapper mapper;
	CloneStructureNode cloneStructureRoot;
	//Special Boolean for selection synchronization listeners
	public boolean correspondingTreeAlreadyChanged = false;
	
	public CloneDiffWizardPage(ExtractCloneRefactoring refactoring) {
		super("Diff Clones");
		this.mapper = refactoring.getMapper();
		this.cloneStructureRoot = mapper.getCloneStructureRoot();
	}
	
	public void createControl(Composite parent){
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout gridLayout = new GridLayout(2, true);
		//gridLayout.numColumns = 6;
		//gridLayout.horizontalSpacing = 15;
		//gridLayout.marginLeft = 15;
		//gridLayout.marginRight = 15;
		result.setLayout(gridLayout);
	
		Label methodLeftName = new Label(result, SWT.WRAP);
		methodLeftName.setText(mapper.getPDG1().getMethod().toString());
		methodLeftName.setFont(new Font(parent.getDisplay(), new FontData("consolas", 9, SWT.NORMAL)));
		GridData methodLeftNameGridData = new GridData(SWT.LEFT, SWT.FILL, true, false);
		//methodLeftNameGridData.horizontalSpan = 3;
		methodLeftName.setLayoutData(methodLeftNameGridData);
		
		Label methodRightName = new Label(result, SWT.WRAP);
		methodRightName.setText(mapper.getPDG2().getMethod().toString());
		methodRightName.setFont(new Font(parent.getDisplay(), new FontData("consolas", 9, SWT.NORMAL)));
		GridData methodRightNameGridData = new GridData(SWT.LEFT, SWT.FILL, true, false);
		//methodRightNameGridData.horizontalSpan = 3;
		methodRightName.setLayoutData(methodRightNameGridData);
		
		
		final TreeViewer treeViewerLeft = new TreeViewer(result, SWT.PUSH | SWT.V_SCROLL);
		treeViewerLeft.setLabelProvider(new CloneDiffStyledLabelProvider(CloneDiffSide.LEFT));
		treeViewerLeft.setContentProvider(new CloneDiffContentProvider());
		treeViewerLeft.setInput(new CloneStructureNode[]{cloneStructureRoot});
		treeViewerLeft.getTree().setLinesVisible(true);
		treeViewerLeft.expandAll();
		//GridData
		GridData treeLeftGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		treeLeftGridData.horizontalAlignment = SWT.FILL;
		treeLeftGridData.verticalAlignment = SWT.FILL;
		//treeLeftGridData.horizontalSpan = 3;
		//treeLeftGridData.verticalSpan = 2;
		treeViewerLeft.getTree().setLayoutData(treeLeftGridData);
		
		final TreeViewer treeViewerRight = new TreeViewer(result, SWT.PUSH);
		treeViewerRight.setLabelProvider(new CloneDiffStyledLabelProvider(CloneDiffSide.RIGHT));
		treeViewerRight.setContentProvider(new CloneDiffContentProvider());
		treeViewerRight.setInput(new CloneStructureNode[]{cloneStructureRoot});
		treeViewerRight.getTree().setLinesVisible(true);
		treeViewerRight.expandAll();
		//GridData
		GridData treeRightGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		treeRightGridData.horizontalAlignment = SWT.FILL;
		treeRightGridData.verticalAlignment = SWT.FILL;
		//treeRightGridData.horizontalSpan = 3;
		//treeRightGridData.verticalSpan = 2;
		treeViewerRight.getTree().setLayoutData(treeRightGridData);
	
		/*
		//Information Footer
		//List of Differences discovered on the line
		final List listOfDifferences = new List (shell, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		listOfDifferences.setItems (new String [] {""});
		GridData listOfDifferencesGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		listOfDifferencesGridData.horizontalSpan = 1;
		listOfDifferencesGridData.verticalSpan = 5;
		listOfDifferences.setLayoutData(listOfDifferencesGridData);
		//Re-populate List when new line is selected:
		treeViewerLeft.addPostSelectionChangedListener(new ISelectionChangedListener() { 
			public void selectionChanged(SelectionChangedEvent event) {
				ArrayList<String> items = new ArrayList<String>(10); 
				TreeSelection treeSelection = (TreeSelection) event.getSelection();
				CloneStructureNode node = (CloneStructureNode) treeSelection.getFirstElement();
				if (node.getMapping() != null){
					for (int i = 0; i < node.getMapping().getNodeDifferences().size(); i++){
						ASTNodeDifference differences = node.getMapping().getNodeDifferences().get(i);
						for (int j = 0; j < differences.getDifferences().size(); j++){
							items.add(differences.getDifferences().get(j).getType().toString());
						}
					}
					if (items.size() != 0)
						listOfDifferences.setItems(Arrays.copyOf(items.toArray(), items.toArray().length, String[].class));
					else
						listOfDifferences.setItems (new String [] {"(No differences in these statements)"});
				}
			}
		});
		//Box with more detailed information
		final Text informationBox = new Text(shell, SWT.BORDER);
		informationBox.setText("The two variable names used are from the same class, but have different names.");
		//Make the box non-editable
		informationBox.addListener(SWT.Verify, new Listener()
		{
			public void handleEvent(Event e){
				e.doit = false;
			}
		});
		//Update Information Box when a List item is selected
		listOfDifferences.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				System.out.println("List Listener");
				informationBox.setText("Something has been clicked");
			}
		});
	
		
		GridData informationBoxGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		informationBoxGridData.horizontalSpan = 5;
		informationBoxGridData.verticalSpan = 5;
		informationBox.setLayoutData(informationBoxGridData);
		*/
		
		//Tooltips
		//ColumnViewerToolTipSupport.enableFor(treeViewerLeft);
		//ColumnViewerToolTipSupport.enableFor(treeViewerRight);
		
		CloneDiffTooltip.enableFor(treeViewerLeft, ToolTip.NO_RECREATE);
		CloneDiffTooltip.enableFor(treeViewerRight, ToolTip.NO_RECREATE);
		
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
		
		//Synchronize Scroll Bars
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
	}
}
