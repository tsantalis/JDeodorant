package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.decomposition.cfg.mapping.CloneStructureNode;
import gr.uom.java.ast.decomposition.cfg.mapping.NodeMapping;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGNodeMapping;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.PreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.Suggestion;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.decomposition.matching.Difference;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;


@SuppressWarnings("restriction")
public class CloneDiffTooltip extends ColumnViewerToolTipSupport {

	private static final Image PRECONDITION_VIOLATION_IMAGE = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
	private static final Image SUGGESTION_IMAGE = JavaPluginImages.get(JavaPluginImages.IMG_OBJS_QUICK_ASSIST);
	private static final Image HEADER_IMAGE = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
	private static final Color HEADER_BACKGROUND_COLOR = new Color(null, 150, 150, 0);
	private Table differencesTable;
	private TreeViewer preconditionViolationTreeViewer;
	
	List<PreconditionViolation> preconditionViolations;
	
	public CloneDiffTooltip(ColumnViewer viewer, int style,
			boolean manualActivation) {
		super(viewer, style, manualActivation);
	}
	
	@Override
	protected Composite createViewerToolTipContentArea(Event event, ViewerCell cell, Composite parent) {
		//Prepare Formatted Header Text
		CloneStructureNode nodeHoveredOver = (CloneStructureNode) cell.getElement();
		ASTNode astStatement;
		NodeMapping nodeMapping = nodeHoveredOver.getMapping();
		if(nodeMapping == null)
			return null;
		if (nodeMapping.getNodeDifferences().size() == 0 && nodeMapping.getPreconditionViolations().size() == 0){
			return null;
		}
		//Construct list of Precondition Violation
		preconditionViolations = nodeMapping.getPreconditionViolations();
		
		Composite comp = new Composite(parent,SWT.NONE);
		GridLayout gridLayout = new GridLayout(1, false);
		comp.setLayout(gridLayout);
		
		if(nodeMapping instanceof PDGNodeMapping) {
		Composite headerComp = new Composite(comp, SWT.NONE);
		GridLayout headerCompGridLayout = new GridLayout(2, false);
		headerComp.setLayout(headerCompGridLayout);
		GridData headerCompGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		headerComp.setLayoutData(headerCompGridData);
		//First Statement
		StyledStringVisitor leafVisitor1 = new StyledStringVisitor(nodeHoveredOver, CloneDiffSide.LEFT);
		astStatement = nodeHoveredOver.getMapping().getNodeG1().getASTStatement();
		astStatement.accept(leafVisitor1);
		StyledString styledString1 = leafVisitor1.getStyledString();
		//Second Statement
		StyledStringVisitor leafVisitor2 = new StyledStringVisitor(nodeHoveredOver, CloneDiffSide.RIGHT);
		astStatement = nodeHoveredOver.getMapping().getNodeG2().getASTStatement();
		astStatement.accept(leafVisitor2);
		StyledString styledString2 = leafVisitor2.getStyledString();
		//Include Statement IDs and put StyledStrings into StyledText labels
		StyledText label1 = new StyledText(headerComp, SWT.BORDER); label1.setText(" " + nodeHoveredOver.getMapping().getNodeG1().getCFGNode().getId() + " ");
		StyledText styledText1 = new StyledText(headerComp, SWT.BORDER);
		StyledText label2 = new StyledText(headerComp, SWT.BORDER); label2.setText(" " + nodeHoveredOver.getMapping().getNodeG2().getCFGNode().getId() + " ");
		StyledText styledText2 = new StyledText(headerComp, SWT.BORDER);
		styledText1.setText(styledString1.toString());
		styledText1.setStyleRanges(styledString1.getStyleRanges());
		styledText2.setText(styledString2.toString());
		styledText2.setStyleRanges(styledString2.getStyleRanges());
		GridData gridData = new GridData(SWT.LEFT, SWT.FILL, true, false);
		gridData.horizontalAlignment = SWT.FILL;
		styledText2.setLayoutData(gridData);
		styledText1.setLayoutData(gridData);
		
		//Differences Label and Table
		if(nodeMapping.getNodeDifferences().size() > 0) {
			CLabel differencesLabel = new CLabel(comp, SWT.NONE);
			differencesLabel.setText("Differences");
			differencesLabel.setImage(HEADER_IMAGE);
			differencesLabel.setBackground(HEADER_BACKGROUND_COLOR);
			GridData differencesLabelGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
			differencesLabel.setLayoutData(differencesLabelGridData);

			differencesTable = new Table (comp, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
			differencesTable.setLinesVisible (true);
			differencesTable.setHeaderVisible (true);
			GridData differencesTableData = new GridData(SWT.FILL, SWT.FILL, true, true);
			//tableData.heightHint = 50;
			differencesTable.setLayoutData(differencesTableData);
			String[] titles = {"Expression 1", "Expression 2", "Difference Type", "Description"};
			for (int i=0; i<titles.length; i++) {
				TableColumn column = new TableColumn (differencesTable, SWT.NONE);
				column.setText (titles [i]);
				column.pack();
			}	
			for (ASTNodeDifference nodeDifference : nodeMapping.getNodeDifferences()) {
				for (Difference diff : nodeDifference.getDifferences()) {
					TableItem item = new TableItem (differencesTable, SWT.NONE);
					item.setText (0, nodeDifference.getExpression1().toString());
					item.setText (1, nodeDifference.getExpression2().toString());
					item.setText (2, diff.getType().name());
					item.setText (3, diff.getType().toString());
				}
			}
			for (int i=0; i<titles.length; i++) {
				differencesTable.getColumn(i).pack();
			}
			//Eliminates extra column at the end
			differencesTable.addControlListener(new ControlAdapter() {
				public void controlResized(ControlEvent e){
					packAndFillLastColumn(differencesTable);
				}
			});
		}
		}
		//Precondition Violations
		if (preconditionViolations.size() > 0){
			CLabel preconditionsLabel = new CLabel(comp, SWT.NONE);
			preconditionsLabel.setBackground(HEADER_BACKGROUND_COLOR);
			preconditionsLabel.setImage(HEADER_IMAGE);
			preconditionsLabel.setText("Precondition Violations");
			GridData preconditionsLabelGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
			preconditionsLabel.setLayoutData(preconditionsLabelGridData);

			preconditionViolationTreeViewer = new TreeViewer(comp, SWT.NONE);
			preconditionViolationTreeViewer.setContentProvider(new PreconditionsViolationsTreeContentProvider());
			preconditionViolationTreeViewer.setLabelProvider(new PreconditionViolationsTreeLabelProvider());
			preconditionViolationTreeViewer.setInput(nodeHoveredOver);
			preconditionViolationTreeViewer.expandAll();
			GridData preconditionViolationGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
			preconditionViolationTreeViewer.getTree().setLayoutData(preconditionViolationGridData);
		}
		return comp;
	}
	
	
	
	
	// Resize last column in Table viewer so that it fills the client area completely if extra space.
	protected void packAndFillLastColumn(Table table) {
	    int columnsWidth = 0;
	    for (int i = 0; i < table.getColumnCount() - 1; i++) {
	        columnsWidth += table.getColumn(i).getWidth();
	    }
	    TableColumn lastColumn = table.getColumn(table.getColumnCount() - 1);
	    lastColumn.pack();

	    Rectangle area = table.getClientArea();

	    Point preferredSize = table.computeSize(SWT.DEFAULT, SWT.DEFAULT);
	    int width = area.width - 2*table.getBorderWidth();

	    if (preferredSize.y > area.height + table.getHeaderHeight()) {
	        // Subtract the scrollbar width from the total column width
	        // if a vertical scrollbar will be required
	        Point vBarSize = table.getVerticalBar().getSize();
	        width -= vBarSize.x;
	    }

	    // last column is packed, so that is the minimum. If more space is available, add it.
	    if(lastColumn.getWidth() < width - columnsWidth) {
	        lastColumn.setWidth(width - columnsWidth + 4);
	    }
	}

	public boolean isHideOnMouseDown() {
		return true;
	}
	
	private class PreconditionsViolationsTreeContentProvider implements ITreeContentProvider {

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if(preconditionViolations != null) {
				return preconditionViolations.toArray();
			}
			else {
				return new PreconditionViolation[] {};
			}
		}
		public Object[] getChildren(Object arg) {
			if (arg instanceof PreconditionViolation) {
				return ((PreconditionViolation)arg).getSuggestions().toArray();
			}
			else {
				return new Suggestion[]{};
			}
		}
		public Object getParent(Object arg0) {
			if(arg0 instanceof Suggestion) {
				Suggestion suggestion = (Suggestion)arg0;
				return suggestion.getPreconditionViolation();
			}
			return null;
		}
		public boolean hasChildren(Object arg0) {
			return getChildren(arg0).length > 0;
		}
		
	}
	private class PreconditionViolationsTreeLabelProvider extends StyledCellLabelProvider{
		public void update(ViewerCell cell) { 
			Object element = cell.getElement();
			if (element instanceof CloneStructureNode){
				cell.setText("CloneStructureNode");
			}
			if (element instanceof PreconditionViolation){
				PreconditionViolation preconditionViolation = (PreconditionViolation) element;
				StyledString styledString = preconditionViolation.getStyledViolation();
				cell.setText(styledString.getString());
				cell.setStyleRanges(styledString.getStyleRanges());
				cell.setImage(PRECONDITION_VIOLATION_IMAGE);
			}
			if (element instanceof Suggestion){
				Suggestion suggestion = (Suggestion) element;
				cell.setText(suggestion.getSuggestion());
				cell.setImage(SUGGESTION_IMAGE);
			}
		}
	}
}
