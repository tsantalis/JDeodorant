package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.decomposition.cfg.mapping.CloneStructureNode;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGElseGap;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGElseMapping;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGNodeGap;
import gr.uom.java.ast.decomposition.cfg.mapping.PDGNodeMapping;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;

public class CloneDiffStyledLabelProvider extends StyledCellLabelProvider {
	
	private CloneDiffSide position;
	
	//Constructor specifies which mapping position this provider will be working with and generated Regex based on the keywords
	public CloneDiffStyledLabelProvider(CloneDiffSide position){
		this.position = position;
	}
	
	//Primary Method for all Styling
	public void update(ViewerCell cell) { 
		StyledString styledString = null;
		//Get Node
		Object element = cell.getElement();
		CloneStructureNode theNode = (CloneStructureNode) element;
		
		if(theNode.getMapping() instanceof PDGNodeMapping) {
			styledString = generateStyledString(theNode, position);
			if(theNode.getMapping().isAdvancedMatch()) {
				cell.setBackground(StyledStringVisitor.ADVANCED_MATCH_COLOR);
			}
		}
		else if(theNode.getMapping() instanceof PDGElseMapping) {
			styledString = new StyledString();
			styledString.append("else", new StyledStringStyler(StyledStringVisitor.initializeKeywordStyle()));
		}
		else if(theNode.getMapping() instanceof PDGNodeGap) {
			styledString = generateStyledStringForGap(theNode, position);
			if ((position == CloneDiffSide.LEFT && theNode.getMapping().getNodeG1() != null) ||
					(position == CloneDiffSide.RIGHT && theNode.getMapping().getNodeG2() != null)) {
				setCellBackgroundWithCode(cell, theNode);
			}
			else {
				setCellBackgroundWithoutCode(cell, theNode);
			}
		}
		else if(theNode.getMapping() instanceof PDGElseGap) {
			styledString = generateStyledStringForElseGap((PDGElseGap)theNode.getMapping(), position);
			if ((position == CloneDiffSide.LEFT && ((PDGElseGap)theNode.getMapping()).getId1() != 0) ||
					(position == CloneDiffSide.RIGHT && ((PDGElseGap)theNode.getMapping()).getId2() != 0)) {
				setCellBackgroundWithCode(cell, theNode);
			}
			else {
				setCellBackgroundWithoutCode(cell, theNode);
			}
		}
		else {
			styledString = new StyledString();
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

	private void setCellBackgroundWithoutCode(ViewerCell cell, CloneStructureNode theNode) {
		if(theNode.getMapping().isAdvancedMatch()) {
			cell.setBackground(StyledStringVisitor.ADVANCED_MATCH_LIGHT_COLOR);
		}
		else {
			cell.setBackground(StyledStringVisitor.UNMAPPED_LIGHT_COLOR);
		}
	}

	private void setCellBackgroundWithCode(ViewerCell cell, CloneStructureNode theNode) {
		if(theNode.getMapping().isAdvancedMatch()) {
			cell.setBackground(StyledStringVisitor.ADVANCED_MATCH_COLOR);
		}
		else {
			cell.setBackground(StyledStringVisitor.UNMAPPED_COLOR);
		}
	}
	
	private StyledString generateStyledString(CloneStructureNode theNode, CloneDiffSide diffSide) {
		ASTNode astStatement = null;
		StyledString styledString;
		StyledStringVisitor leafVisitor = new StyledStringVisitor(theNode, diffSide);
		if (diffSide == CloneDiffSide.LEFT){
			astStatement = theNode.getMapping().getNodeG1().getASTStatement();
		}
		else if (diffSide == CloneDiffSide.RIGHT){
			astStatement = theNode.getMapping().getNodeG2().getASTStatement();
		}
		astStatement.accept(leafVisitor);
		styledString = leafVisitor.getStyledString();
		return styledString;
	}
	
	private StyledString generateStyledStringForGap(CloneStructureNode theNode, CloneDiffSide diffSide) {
		StyledString styledString = null;
		if (diffSide == CloneDiffSide.LEFT) {
			if(theNode.getMapping().getNodeG1() != null) {
				styledString = generateStyledString(theNode, position);
			}
			//This creates a blank Label containing only spaces, to match the length of the corresponding Gap statement. 
			else{
				String correspondingStatement = theNode.getMapping().getNodeG2().toString();
				StringBuilder str = new StringBuilder();
				for (int i = 0; i < correspondingStatement.length(); i++){
					str.append("  ");
				}
				styledString = new StyledString(str.toString());
			}
		}
		else if (diffSide == CloneDiffSide.RIGHT) {
			if(theNode.getMapping().getNodeG2() != null) {
				styledString = generateStyledString(theNode, position);
			}
			//This creates a blank Label containing only spaces, to match the length of the corresponding Gap statement. 
			else{
				String correspondingStatement = theNode.getMapping().getNodeG1().toString();
				StringBuilder str = new StringBuilder();
				for (int i = 0; i < correspondingStatement.length(); i++){
					str.append("  ");
				}
				styledString = new StyledString(str.toString());
			}
		}
		return styledString;
	}
	
	private StyledString generateStyledStringForElseGap(PDGElseGap elseGap, CloneDiffSide diffSide) {
		StyledString styledString = null;
		if (diffSide == CloneDiffSide.LEFT) {
			if(elseGap.getId1() != 0) {
				styledString = new StyledString();
				styledString.append("else", new StyledStringStyler(StyledStringVisitor.initializeKeywordStyle()));
			}
			//This creates a blank Label containing only spaces, to match the length of the corresponding Gap statement. 
			else{
				String correspondingStatement = "else";
				StringBuilder str = new StringBuilder();
				for (int i = 0; i < correspondingStatement.length(); i++){
					str.append("  ");
				}
				styledString = new StyledString(str.toString());
			}
		}
		else if (diffSide == CloneDiffSide.RIGHT) {
			if(elseGap.getId2() != 0) {
				styledString = new StyledString();
				styledString.append("else", new StyledStringStyler(StyledStringVisitor.initializeKeywordStyle()));
			}
			//This creates a blank Label containing only spaces, to match the length of the corresponding Gap statement. 
			else{
				String correspondingStatement = "else";
				StringBuilder str = new StringBuilder();
				for (int i = 0; i < correspondingStatement.length(); i++){
					str.append("  ");
				}
				styledString = new StyledString(str.toString());
			}
		}
		return styledString;
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
