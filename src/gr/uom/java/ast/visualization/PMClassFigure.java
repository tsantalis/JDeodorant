package gr.uom.java.ast.visualization;

import java.util.ArrayList;
import java.util.List;
import gr.uom.java.distance.CandidateRefactoring;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.graphics.Color;

public class PMClassFigure extends PackageMapFigure{
	
	private int numOfAttributes;
	private int numOfMethods;
	private String name;
	private Figure originalTooltip= new Figure();
	private List<CandidateRefactoring> candidates = new ArrayList<CandidateRefactoring>();
	private boolean isSelected= false;
	private Color originalColor = ColorConstants.white;
	private LineBorder originalBorder = new LineBorder();
	private boolean isInnerClass= false;

	public static int MAX_NUM;
	public static int MIN_NUM;
	
	
	public PMClassFigure(String name, int numOfAttributes, int numOfMethods){
		this.numOfAttributes = numOfAttributes;
		this.numOfMethods= numOfMethods;
		this.name = name;
		Label classLabel = new Label(name , DecorationConstants.CLASS);
		Label infoLabel = new Label(" Number Of Methods: "+ numOfMethods + "\n Number Of Attributes: "+ numOfAttributes);
		originalTooltip.setLayoutManager(new ToolbarLayout());
		originalTooltip.add(classLabel);
		originalTooltip.add(infoLabel);
		setToolTip(originalTooltip);
		
		originalBorder.setColor(originalColor);
		setBorder(originalBorder);
		
		setBackgroundColor(originalColor);
	}

	public Dimension calculateSize(){
		Dimension size = new Dimension();
		int minSize = 20;
		int maxSize = 150 ;
		double width, height;
		double range =(double) (MAX_NUM - MIN_NUM);
		width =  (((numOfAttributes - MIN_NUM)/range) * maxSize) + minSize;
		height =  (((numOfMethods - MIN_NUM)/range) * maxSize) + minSize;
		
		size.width = (int) width;
		size.height = (int) height;
		return size;
	}

	public static void setMAX_NUM(int mAX_NUM) {
		MAX_NUM = mAX_NUM;
	}

	public static void setMIN_NUM(int mIN_NUM) {
		MIN_NUM = mIN_NUM;
	}

	public int compareTo(PackageMapFigure o) {
		if(o instanceof PackageFigure)
			return 1;
		else{
			PMClassFigure classFigure = (PMClassFigure) o;
			
			int thisSize = this.numOfAttributes + this.numOfMethods;
			int oSize = classFigure.numOfAttributes + classFigure.numOfMethods;
			
			if(thisSize == oSize){
				if(this.numOfMethods == classFigure.numOfMethods)
					return this.name.compareTo(classFigure.name);
				else
					return -(this.numOfMethods-classFigure.numOfMethods);
			} else
				return -(thisSize - oSize);
		}
		
	}

	@Override
	public void draw() {
		Dimension size = calculateSize();
		this.setPreferredSize(size);
		
	}

	@Override
	public int numberOfClasses() {
		// TODO Auto-generated method stub
		return 1;
	}
	
	public List<CandidateRefactoring> getCandidates() {
		return candidates;
	}
	
	

	public void setCandidates(List<CandidateRefactoring> candidates) {
		this.candidates = candidates;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	public Color getOriginalColor() {
		return originalColor;
	}
	
	

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public void setOriginalBackgroundColor(Color originalColor) {
		this.originalColor = originalColor;
		this.setBackgroundColor(originalColor);
	}

	public Figure getOriginalTooltip() {
		return originalTooltip;
	}

	public void setOriginalTooltip(Figure originalTooltip) {
		this.originalTooltip = originalTooltip;
	}

	
	public void setToOriginalState(){
		this.setBackgroundColor(this.getOriginalColor());
		this.setBorder(getOriginalBorder());
		this.setToolTip(originalTooltip);
	}

	public LineBorder getOriginalBorder() {
		return originalBorder;
	}

	public void setOriginalBorder(LineBorder originalBorder) {
		this.originalBorder = originalBorder;
		this.setBorder(originalBorder);
	}

	public boolean isInnerClass() {
		return isInnerClass;
	}

	public void setInnerClass(boolean isInnerClass) {
		this.isInnerClass = isInnerClass;
	}
	
	
	
	
	
	
}
