package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.decomposition.cfg.mapping.CloneStructureNode;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;

public class CloneDiffStyledLabelProvider extends StyledCellLabelProvider {
	
	public CloneDiffSide position;
	
	//Constructor specifies which mapping position this provider will be working with and generated Regex based on the keywords
	public CloneDiffStyledLabelProvider(CloneDiffSide position){
		this.position = position;
	}
	
	//Primary Method for all Styling
	public void update(ViewerCell cell) { 
		//Local Variables
		ASTNode astStatement;
		StyledString styledString = new StyledString();
		//Get Node
		Object element = cell.getElement();
		CloneStructureNode theNode = (CloneStructureNode) element;
		
		if(theNode.getMapping() != null) {
			//Separate LEFT and RIGHT trees...
			if (position == CloneDiffSide.LEFT) {
				if(theNode.getMapping().getNodeG1() != null) {
					StyledStringVisitor leafVisitor = new StyledStringVisitor(theNode, CloneDiffSide.LEFT);
					astStatement = theNode.getMapping().getNodeG1().getASTStatement();
					astStatement.accept(leafVisitor);
					styledString = leafVisitor.getStyledString();
				}
			}
			else if (position == CloneDiffSide.RIGHT) {
				if(theNode.getMapping().getNodeG2() != null) {
					StyledStringVisitor leafVisitor = new StyledStringVisitor(theNode, CloneDiffSide.RIGHT);
					astStatement = theNode.getMapping().getNodeG2().getASTStatement();
					astStatement.accept(leafVisitor);
					styledString = leafVisitor.getStyledString();
				}
			}
		}
		else {
			styledString.append("Root", null);
		}
		
		/*
		Image image = new Image(null, "C:\\Users\\ra_stein\\Pictures\\xicon3.png");
		cell.setImage(image);
		*/
		cell.setText(styledString.toString());
		cell.setStyleRanges(styledString.getStyleRanges()); 
		super.update(cell);
	}
	
	//Tooltip
	public String getToolTipText(Object element) {
		return "yellow"; //element.toString();
	}
	public Point getToolTipShift(Object object) {
		return new Point(20,0);
	}
	public Point getToolTipLocation(MouseEvent event){
		return new Point(500, 500);
	}
	public int getToolTipDisplayDelayTime(Object object) {
		return 300;
	}
	public int getToolTipTimeDisplayed(Object object) {
		return 50000;
	}
}
