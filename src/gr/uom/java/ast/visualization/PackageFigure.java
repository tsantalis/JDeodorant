package gr.uom.java.ast.visualization;

import java.util.TreeSet;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.CompoundBorder;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MarginBorder;


public class PackageFigure extends PackageMapFigure{

	private TreeSet<PackageMapFigure> children = new TreeSet<PackageMapFigure>();
	private String name;
	private int depth;


	public PackageFigure(String name){
		this.name= name;

		setLayoutManager(new ProportionalFlowLayout(1.5,5, 10));
		if(name==null)
			name = "default";
		setToolTip(new Label(name, DecorationConstants.PACKAGE));
		
	}

	public PackageFigure(String name, double scale){
		this.name= name;

		setLayoutManager(new ProportionalFlowLayout(scale,5, 10));
		setToolTip(new Label(name));
		LineBorder border = new LineBorder();
		border.setColor(ColorConstants.white);
		
		setBorder(new CompoundBorder(border, new MarginBorder(10, 5, 10, 5)));

	}

	public void addToSet(PackageMapFigure figure){
		children.add(figure);
	}

	public void draw(){
		for(PackageMapFigure figure: this.children){

			figure.draw();
			this.add(figure);

		}
	}
	public int compareTo(PackageMapFigure o) {
		if(o instanceof PMClassFigure){
			return -1;
		}else{
			PackageFigure packageFigure = (PackageFigure) o;
			int thisNum = this.numberOfClasses();
			int otherNum = packageFigure.numberOfClasses();
			if(thisNum == otherNum){
				return this.name.compareTo(packageFigure.name);
			} else
				return -(thisNum - otherNum);

		}

	}

	@Override
	public int numberOfClasses() {

		int numberOfClasses = 0 ;

		for(PackageMapFigure figure: children){
			numberOfClasses += figure.numberOfClasses();
		}
		
		return numberOfClasses;
	}

	public TreeSet<PackageMapFigure> getChildrenSet() {
		return children;
	}

	
	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}




}
