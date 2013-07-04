package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.decomposition.cfg.mapping.CloneStructureNode;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGMapper;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;

public class NodeVisualCompare {

	PDGMapper mapper;
	CloneStructureNode cloneStructureRoot;
	//Special Boolean for selection synchronization listeners
	public boolean correspondingTreeAlreadyChanged = false;
	
	public NodeVisualCompare(PDGMapper mapper){
		this.mapper = mapper;
		this.cloneStructureRoot = mapper.getCloneStructureRoot();
	}

	
	public void createTree(Shell shell){
	
		GridLayout gridLayout = new GridLayout(2, true);
		gridLayout.numColumns = 6;
		gridLayout.horizontalSpacing = 15;
		gridLayout.marginLeft = 15;
		gridLayout.marginRight = 15;
		shell.setLayout(gridLayout);
	
		Label methodLeftName = new Label(shell, SWT.WRAP);
		methodLeftName.setText(mapper.getPDG1().getMethod().toString());
		methodLeftName.setFont(new Font(null, new FontData("consolas", 9, SWT.NORMAL)));
		GridData methodLeftNameGridData = new GridData(SWT.LEFT, SWT.FILL, true, false);
		methodLeftNameGridData.horizontalSpan = 3;
		methodLeftName.setLayoutData(methodLeftNameGridData);
		
		Label methodRightName = new Label(shell, SWT.NONE);
		methodRightName.setText(mapper.getPDG2().getMethod().toString());
		methodRightName.setFont(new Font(null, new FontData("consolas", 9, SWT.NORMAL)));
		GridData methodRightNameGridData = new GridData(SWT.LEFT, SWT.FILL, true, false);
		methodRightNameGridData.horizontalSpan = 3;
		methodRightName.setLayoutData(methodRightNameGridData);
		
		
		final TreeViewer treeViewerLeft = new TreeViewer(shell, SWT.PUSH | SWT.V_SCROLL);
		treeViewerLeft.setLabelProvider(new NodeVisualCompareStyledLabelProvider(NodeVisualComparePosition.LEFT)); //new NodeVisualCompareLabelProvider(NodeVisualComparePosition.LEFT));
		treeViewerLeft.setContentProvider(new NodeVisualCompareContentProvider());
		treeViewerLeft.setInput(new CloneStructureNode[]{cloneStructureRoot});
		treeViewerLeft.getTree().setLinesVisible(true);
		treeViewerLeft.expandAll();
		//GridData
		GridData treeLeftGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		treeLeftGridData.horizontalAlignment = SWT.FILL;
		treeLeftGridData.verticalAlignment = SWT.FILL;
		treeLeftGridData.horizontalSpan = 3;
		treeLeftGridData.verticalSpan = 2;
		treeViewerLeft.getTree().setLayoutData(treeLeftGridData);
		
		final TreeViewer treeViewerRight = new TreeViewer(shell, SWT.PUSH);
		treeViewerRight.setLabelProvider(new NodeVisualCompareStyledLabelProvider(NodeVisualComparePosition.RIGHT));
		treeViewerRight.setContentProvider(new NodeVisualCompareContentProvider());
		treeViewerRight.setInput(new CloneStructureNode[]{cloneStructureRoot});
		treeViewerRight.getTree().setLinesVisible(true);
		treeViewerRight.expandAll();
		//GridData
		GridData treeRightGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		treeRightGridData.horizontalAlignment = SWT.FILL;
		treeRightGridData.verticalAlignment = SWT.FILL;
		treeRightGridData.horizontalSpan = 3;
		//Set the Width of the tree, according to the longest statement. Only necessary for one tree since column widths are set to "Equal".
		TreeItem[] items = treeViewerRight.getTree().getItems();
		treeRightGridData.widthHint = getMaxLengthOfTreeItems(items[0]) * 10;
		
		treeRightGridData.verticalSpan = 2;
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
		
		NodeVisualCompareTooltips.enableFor(treeViewerLeft, ToolTip.NO_RECREATE);
		NodeVisualCompareTooltips.enableFor(treeViewerRight, ToolTip.NO_RECREATE);
		
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
	
		
		
		//TODO Prevents the item in the Tree not currently in focus from graying out. 

		shell.setMaximized(true);
		shell.pack();
		
		
	}
	
	private int getMaxLengthOfTreeItems(TreeItem treeItem){
		int currentLongest = treeItem.getText().length();
		TreeItem[] children = treeItem.getItems();
		for (int i = 0; i < children.length; i++){
			int maxOfChild = getMaxLengthOfTreeItems(children[i]);
			if (maxOfChild > currentLongest)
				currentLongest = maxOfChild;
		}
		return currentLongest;
	}
}
