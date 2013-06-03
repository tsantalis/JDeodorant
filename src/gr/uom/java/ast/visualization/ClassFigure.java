package gr.uom.java.ast.visualization;


import org.eclipse.draw2d.CompoundBorder;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;


public class ClassFigure extends Figure {
	public static Color classColor = new Color(null,255,255,206);
	public static final Image CLASS = JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CLASS);
	
	private CompartmentFigure fieldFigure = new CompartmentFigure();
	private CompartmentFigure methodFigure = new CompartmentFigure();
	private CompartmentFigure extractMethodFigure = new CompartmentFigure();
	private SectionCompartment methodSectionCompartment ;
	private SectionCompartment fieldSectionCompartment = new SectionCompartment(3) ;
	


	public ClassFigure(String name, Color color) {
		ToolbarLayout layout = new ToolbarLayout();
		layout.setSpacing(5);
		setLayoutManager(layout);	
		setBorder(new CompoundBorder( new LineBorder(1), new MarginBorder(0, 0, 0, 0)));
		setBackgroundColor(color);
		setOpaque(true);
		

		Font classFont = new Font(null, "Arial", 12, SWT.BOLD);
		Label className = new Label(name, CLASS);
		className.setToolTip(new Label(name));
		className.setFont(classFont);
		
		add(className);
		
		new ClassFigureMover(this);
	}


		

	public void addThreeCompartments(){
		addFieldCompartment();
		extractMethodFigure.setBorder(new LineBorder());
		extractMethodFigure.setBackgroundColor(EntityFigure.entityColor);
		add(extractMethodFigure);
		add(methodFigure);
	}

	public void addTwoCompartments(){
		this.addFieldCompartment();
		methodFigure.setBorder(new CompartmentFigureBorder());
		add(methodFigure);
	}
	
	public void addFieldCompartment(){
		fieldFigure.setBorder(new CompartmentFigureBorder());
		add(fieldFigure);
	}
	
	public void addMethodSectionCompartment(int columns){
		methodSectionCompartment = new SectionCompartment(columns);
		add(methodSectionCompartment);
	}
	
	public void addFieldSectionCompartment(){
		add(fieldSectionCompartment);
	}
	
	public SectionCompartment getFieldSectionCompartment() {
		return fieldSectionCompartment;
	}

	
	public SectionCompartment getMethodSectionCompartment() {
		return methodSectionCompartment;
	}



	public CompartmentFigure getFieldsCompartment() {
		return fieldFigure;
	}
	public CompartmentFigure getMethodsCompartment() {
		return methodFigure;
	}

	public CompartmentFigure getExtractMethodCompartment() {
		return extractMethodFigure;
	}

}
