package gr.uom.java.ast.visualization;

import java.util.Arrays;

import org.eclipse.draw2d.Bendpoint;
import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.ConnectionLocator;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.RelativeBendpoint;
import org.eclipse.draw2d.geometry.Dimension;


public class JConnection extends PolylineConnection {
	private ConnectionType type;
	private Label label;
	private ConnectionLocator locator;
	private boolean isWrite = false;
	
	public JConnection(ConnectionType type){
		this.type = type;
	}

	public ConnectionType getType() {
		return type;
	}

	public Label getLabel() {
		return label;
	}
	
	public boolean isWrite(){
		return isWrite;
	}

	public ConnectionLocator getLocator() {
		return locator;
	}

	//changed Label to EntityFigure
	public PolylineConnection setRightLeftAnchors(EntityFigure source, EntityFigure target){
		RightAnchor sourceAnchor;
		LeftAnchor targetAnchor;
		
		if (source.getRightAnchor() != null)
			sourceAnchor = source.getRightAnchor();
		else{
			sourceAnchor = new RightAnchor(source);
			source.setRightAnchor(sourceAnchor);
		}
		
		if(target.getLeftAnchor() != null){
			targetAnchor  = target.getLeftAnchor();
		}
		else {
			targetAnchor  = new LeftAnchor(target);
			target.setLeftAnchor(targetAnchor);
		}
		
		this.setSourceAnchor(sourceAnchor);
		this.setTargetAnchor(targetAnchor);

		return this;
	}


	public PolylineConnection setLeftLeftAnchors(EntityFigure source, EntityFigure target){
		LeftAnchor sourceAnchor;
		LeftAnchor targetAnchor;
		
		if (source.getLeftAnchor() != null)
			sourceAnchor = source.getLeftAnchor();
		else{
			sourceAnchor = new LeftAnchor(source);
			source.setLeftAnchor(sourceAnchor);
		}
		
		if(target.getLeftAnchor() != null){
			targetAnchor  = target.getLeftAnchor();
		}
		else {
			targetAnchor  = new LeftAnchor(target);
			target.setLeftAnchor(targetAnchor);
		}
		//LeftAnchor sourceAnchor = new LeftAnchor(source);
		//LeftAnchor targetAnchor = new LeftAnchor(target);
		this.setSourceAnchor(sourceAnchor);
		this.setTargetAnchor(targetAnchor);

		return this;
	}

	public PolylineConnection setLeftRightAnchors(EntityFigure source, EntityFigure target){
		LeftAnchor sourceAnchor;
		RightAnchor targetAnchor;
		
		if (source.getLeftAnchor() != null)
			sourceAnchor = source.getLeftAnchor();
		else{
			sourceAnchor = new LeftAnchor(source);
			source.setLeftAnchor(sourceAnchor);
		}
		
		if(target.getRightAnchor() != null){
			targetAnchor  = target.getRightAnchor();
		}
		else {
			targetAnchor  = new RightAnchor(target);
			target.setRightAnchor(targetAnchor);
		}
		//LeftAnchor sourceAnchor = new LeftAnchor(source);
		//RightAnchor targetAnchor = new RightAnchor(target);
		this.setSourceAnchor(sourceAnchor);
		this.setTargetAnchor(targetAnchor);

		return this;
	}

	public PolylineConnection setRightRightAnchors(EntityFigure source, EntityFigure target){
		RightAnchor sourceAnchor;
		RightAnchor targetAnchor;
		
		if (source.getRightAnchor() != null)
			sourceAnchor = source.getRightAnchor();
		else{
			sourceAnchor = new RightAnchor(source);
			source.setRightAnchor(sourceAnchor);
		}
		
		if(target.getRightAnchor() != null){
			targetAnchor  = target.getRightAnchor();
		}
		else {
			targetAnchor  = new RightAnchor(target);
			target.setRightAnchor(targetAnchor);
		}

		//RightAnchor sourceAnchor = new RightAnchor(source);
		//RightAnchor targetAnchor = new RightAnchor(target);
		this.setSourceAnchor(sourceAnchor);
		this.setTargetAnchor(targetAnchor);

		return this;
	}

	public PolylineConnection setMethodToMethodStyle(){
		PolygonDecoration decoration = new PolygonDecoration();
		decoration.setTemplate(PolygonDecoration.TRIANGLE_TIP);
		decoration.setBackgroundColor(DecorationConstants.methodToMethodColor);
		this.setTargetDecoration(decoration);

		this.setForegroundColor(DecorationConstants.methodToMethodColor);

		return this;
	}

	public PolylineConnection setWriteStyle(){
		isWrite = true;
		PolygonDecoration decoration = new PolygonDecoration();
		decoration.setTemplate(PolygonDecoration.TRIANGLE_TIP);
		decoration.setBackgroundColor(ColorConstants.red);
		this.setTargetDecoration(decoration);
		this.setForegroundColor(ColorConstants.red);

		return this;
	}

	public PolylineConnection setReadStyle(){
		PolygonDecoration decoration = new PolygonDecoration();
		decoration.setTemplate(PolygonDecoration.TRIANGLE_TIP);
		decoration.setBackgroundColor(ColorConstants.black);
		this.setTargetDecoration(decoration);

		this.setForegroundColor(ColorConstants.darkBlue);

		return this;
	}

	public PolylineConnection setDottedLine(){
		this.setLineDash(new float[] {2f});

		return this;
	}


	public PolylineConnection setLabel(Integer occurences){

		Label l;
		if(occurences != DecorationConstants.NO_OCCURENCES){
			l = new Label(Integer.toString(occurences));
			ConnectionLocator locator = new ConnectionLocator(this, ConnectionLocator.MIDDLE);
			//locator.setGap(5);
			l.setFont(DecorationConstants.normalFont);
			l.setForegroundColor(ColorConstants.black);
			this.add(l,locator);
			this.label=l;
			this.locator = locator;
		}

		return this;
	}

	public PolylineConnection setSlightBendRouter(){
		BendpointConnectionRouter router = new BendpointConnectionRouter();
		RelativeBendpoint bp1 = new RelativeBendpoint(this);
		bp1.setRelativeDimensions(new Dimension(20,20), new Dimension(20, 20));
		bp1.setWeight(0.5f);

		router.setConstraint(this, Arrays.asList(new Bendpoint[] {bp1}));

		this.setConnectionRouter(router);

		return this;
	}
	
	
	public PolylineConnection setFullBendRouter(int bendHeightX){
		float weight = 0.3f;
		int bendHeightY = 50;

		int secondBendHeight = -(bendHeightX+(bendHeightX/3));

		/*int gap =10;
		if(classWidth<0)
			gap = -gap-25;
		 */

		BendpointConnectionRouter router = new BendpointConnectionRouter();


		RelativeBendpoint bp2 = new RelativeBendpoint(this);
		bp2.setRelativeDimensions(new Dimension(secondBendHeight,0), new Dimension(0,0));
		bp2.setWeight(weight);

		RelativeBendpoint bp3 = new RelativeBendpoint(this);
		bp3.setRelativeDimensions(new Dimension(-bendHeightX,bendHeightY), new Dimension(-bendHeightX,-bendHeightY));
		//bp1.setWeight(weight);

		RelativeBendpoint bp4 = new RelativeBendpoint(this);
		bp4.setRelativeDimensions(new Dimension(0,0), new Dimension(secondBendHeight,0));
		bp4.setWeight(1 - weight);

		/*RelativeBendpoint bp5 = new RelativeBendpoint(this);
		bp5.setRelativeDimensions(new Dimension(0,0), new Dimension(-((classWidth/2)+gap),0));
		bp5.setWeight(1);*/

		router.setConstraint(this, Arrays.asList(new Bendpoint[] {bp2, bp3, bp4}));

		this.setConnectionRouter(router);

		return this;
	}
}