package gr.uom.java.ast.visualization;

import java.util.List;
import org.eclipse.draw2d.FlowLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;

public class ProportionalFlowLayout extends FlowLayout {

	private double scale;

	public ProportionalFlowLayout(double scale, int widthSpacing, int heightSpacing){
		setStretchMinorAxis(false);
		setMinorSpacing(widthSpacing);
		setMajorSpacing(heightSpacing);
		setMinorAlignment(FlowLayout.ALIGN_TOPLEFT);
		setMajorAlignment(FlowLayout.ALIGN_TOPLEFT);
		this.scale= scale;
	}


	@Override
	protected Dimension calculatePreferredSize(IFigure container, int wHint,
			int hHint) {
		// The preferred dimension that is to be calculated and returned

		Dimension prefSize;

		List children = container.getChildren();
		int width = 0;
		int height = 0;
		int wInsets = container.getInsets().getWidth();
		int hInsets = container.getInsets().getHeight();
		boolean newRow = true;
		int prevRowHeight = height;
		int firstInRowIndex= 0;
		int currentRowWidth=0;
		IFigure child;

		Dimension childSize;

		
		for (int i = 0; i < children.size(); i++) {

			child = (IFigure) children.get(i);
			childSize = child.getPreferredSize();

			if (i == 0) {
				
				width = childSize.width ;
				height = childSize.height;

			} else {
				
				

				//width needs to be increased
				if((double) width/height < scale){
					if(!newRow){

						IFigure firstInRow = (IFigure) children.get(firstInRowIndex);
						width += firstInRow.getPreferredSize().width+getMinorSpacing();
						currentRowWidth = currentRowWidth - (firstInRow.getPreferredSize().width + getMinorSpacing()) ;
						firstInRowIndex++;
						height = prevRowHeight + childSize.height + getMajorSpacing();
						currentRowWidth += childSize.width + getMinorSpacing();
					}

					else{
						width = width + childSize.width + getMinorSpacing();
						if(childSize.height>height)
						 height = childSize.height + getMajorSpacing();
					}
						

					


				} else {
					
					if((currentRowWidth + childSize.width) >= width && !newRow){
						//in need of a new Row
						newRow = true;
					}
					
					//makes a new row, height is increased
					if(newRow){
						
						prevRowHeight = height ;
						firstInRowIndex = i;
						height += childSize.height + getMajorSpacing();
						currentRowWidth = 0; 
						newRow = false;
					}
					//increases height if new figure added in new row is taller then previous
					if((prevRowHeight+childSize.height)>height){
						height = prevRowHeight + childSize.height +getMajorSpacing();
					}

					//updates the current RowWidth
					currentRowWidth += childSize.width+ getMinorSpacing();
				}

			}

			
		}
		prefSize = new Dimension(width+wInsets+4, height+hInsets+4);	
		
		return prefSize;

	}
}
