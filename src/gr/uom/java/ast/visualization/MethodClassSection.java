package gr.uom.java.ast.visualization;

import org.eclipse.draw2d.CompoundBorder;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.ToolbarLayout;

public class MethodClassSection extends Figure {

	private int numOfMethods=0;

	public MethodClassSection(){
		//setFont( Display.getCurrent().getSystemFont() ); 
		ToolbarLayout layout = new ToolbarLayout();
		layout.setSpacing(5);
		layout.setMinorAlignment(ToolbarLayout.ALIGN_TOPLEFT);
		layout.setStretchMinorAxis(true);
		setLayoutManager(layout);

	}

	public int getNumOfMethods() {
		return numOfMethods;
	}

	public void addFigure(Figure figure){
		this.add(figure);
		figure.setBackgroundColor(DecorationConstants.entityColor);
		figure.setBorder(new CompoundBorder( new LineBorder(1), new MarginBorder(3,3,3,3)));
		numOfMethods++;
	}

}
