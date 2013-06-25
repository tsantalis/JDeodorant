package gr.uom.java.ast.visualization;


import org.eclipse.draw2d.*;



public class CompartmentFigure extends Figure {
	public CompartmentFigure() {
		//setFont( Display.getCurrent().getSystemFont() ); 
	    
		ToolbarLayout layout = new ToolbarLayout();
	    layout.setMinorAlignment(ToolbarLayout.ALIGN_TOPLEFT);
	    layout.setStretchMinorAxis(true);
	    layout.setSpacing(5);
	    setLayoutManager(layout);
	    
	    setBorder(new CompoundBorder (new MarginBorder(3,3,3,3), new CompartmentFigureBorder()));
	   
	    setOpaque(true);
	  }
	
	//adds EntityFigure with different color and border
	public void addFigure(Figure figure){
		figure.setBackgroundColor(DecorationConstants.entityColor);
		figure.setBorder(new CompoundBorder( new LineBorder(1), new MarginBorder(3,3,3,3)));
		add(figure);
	}
	    
	  

}
