package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.decomposition.ASTNodeDifference;
import gr.uom.java.ast.decomposition.Difference;
import gr.uom.java.ast.decomposition.cfg.mapping.CloneStructureNode;

import java.util.List;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;


public class CloneDiffTooltip extends ColumnViewerToolTipSupport {

	private Table table;
	
	public CloneDiffTooltip(ColumnViewer viewer, int style,
			boolean manualActivation) {
		super(viewer, style, manualActivation);
	}
	
	@Override
	protected Composite createViewerToolTipContentArea(Event event, ViewerCell cell, Composite parent) {
		CloneStructureNode nodeHoveredOver = (CloneStructureNode) cell.getElement();
		//Table table = new Table(comp, SWT.DEFAULT);

		//Prepare Formatted Header Text
		ASTNode astStatement;
		List<ASTNodeDifference> differences = nodeHoveredOver.getMapping().getNodeDifferences();
		if (differences.size() == 0)
			return null;
		Composite comp = new Composite(parent,SWT.NONE);
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
		GridLayout gridLayout = new GridLayout(2, false);
		comp.setLayout(gridLayout);
		StyledText label1 = new StyledText(comp, SWT.BORDER); label1.setText(" " + nodeHoveredOver.getMapping().getNodeG1().getCFGNode().getId() + " ");
		StyledText styledText1 = new StyledText(comp, SWT.BORDER);
		StyledText label2 = new StyledText(comp, SWT.BORDER); label2.setText(" " + nodeHoveredOver.getMapping().getNodeG2().getCFGNode().getId() + " ");
		StyledText styledText2 = new StyledText(comp, SWT.BORDER);
		styledText1.setText(styledString1.toString());
		styledText1.setStyleRanges(styledString1.getStyleRanges());
		styledText2.setText(styledString2.toString());
		styledText2.setStyleRanges(styledString2.getStyleRanges());
		GridData gridData = new GridData(SWT.LEFT, SWT.FILL, true, false);
		gridData.horizontalAlignment = SWT.FILL;
		styledText1.setLayoutData(gridData);
		styledText2.setLayoutData(gridData);
		
		table = new Table (comp, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
		table.setLinesVisible (true);
		table.setHeaderVisible (true);
		GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
		//tableData.heightHint = 50;
		tableData.horizontalSpan = 2;
		table.setLayoutData(tableData);
		String[] titles = {"Expression 1", "Expression 2", "Difference Type", "Description"};
		for (int i=0; i<titles.length; i++) {
			TableColumn column = new TableColumn (table, SWT.NONE);
			column.setText (titles [i]);
			column.pack();
		}	
		//Eliminates extra column at the end
		table.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e){
				packAndFillLastColumn();
			}
		});
		
		for (ASTNodeDifference nodeDifference : differences) {
			for (Difference diff : nodeDifference.getDifferences()) {
				TableItem item = new TableItem (table, SWT.NONE);
				item.setText (0, nodeDifference.getExpression1().toString());
				item.setText (1, nodeDifference.getExpression2().toString());
				item.setText (2, diff.getType().name());
				item.setText (3, diff.getType().toString());
			}
		}
		for (int i=0; i<titles.length; i++) {
			table.getColumn(i).pack();
		}	
		
		return comp;
	}
	
	// Resize last column in Table viewer so that it fills the client area completely if extra space.
	protected void packAndFillLastColumn() {
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
}
