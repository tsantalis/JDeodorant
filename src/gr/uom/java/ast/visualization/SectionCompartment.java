package gr.uom.java.ast.visualization;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;

public class SectionCompartment extends Figure {
	private MethodClassSection sectionOne = new MethodClassSection();
	private MethodClassSection sectionTwo= new MethodClassSection();
	private MethodClassSection sectionThree= new MethodClassSection();

	public SectionCompartment(int columns) {

		//setFont( Display.getCurrent().getSystemFont() ); 
		GridLayout layout = new GridLayout(columns,true);

		setLayoutManager(layout);
		setBorder(new CompartmentFigureBorder());

		this.add(sectionOne, new GridData(GridData.FILL_HORIZONTAL) );

		if( columns == 2 )
			this.add(sectionTwo, new GridData(GridData.FILL_HORIZONTAL));
		if(columns == 3){
			this.add(sectionTwo, new GridData(GridData.FILL_HORIZONTAL));
			this.add(sectionThree, new GridData(GridData.FILL_HORIZONTAL));	
		}
	}

	public MethodClassSection getSectionOne() {
		return sectionOne;
	}

	public MethodClassSection getSectionTwo() {
		return sectionTwo;
	}

	public MethodClassSection getSectionThree() {
		return sectionThree;
	}

}
