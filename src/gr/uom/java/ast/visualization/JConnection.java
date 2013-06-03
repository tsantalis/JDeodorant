package gr.uom.java.ast.visualization;

import java.util.Arrays;

import org.eclipse.draw2d.Bendpoint;
import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.ConnectionLocator;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.PolylineDecoration;
import org.eclipse.draw2d.RelativeBendpoint;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

public class JConnection extends PolylineConnection {
	private ConnectionType type;
	//public static Color methodToMethodColor = ColorConstants.cyan;
	//public static Color methodToMethodColor = new Color(null, 49,79,79);
	public static Color methodToMethodColor = new Color(null,60,179,113);
	
	
	
	public JConnection(ConnectionType type){
		this.type = type;
	}
	
	public ConnectionType getType() {
		return type;
	}

	public PolylineConnection setRightLeftAnchors(Label source, Label target){
		RightAnchor sourceAnchor = new RightAnchor(source);
		LeftAnchor targetAnchor = new LeftAnchor(target);
		this.setSourceAnchor(sourceAnchor);
		this.setTargetAnchor(targetAnchor);
		
		return this;
	}
	
	
	public PolylineConnection setLeftLeftAnchors(Label source, Label target){
		LeftAnchor sourceAnchor = new LeftAnchor(source);
		LeftAnchor targetAnchor = new LeftAnchor(target);
		this.setSourceAnchor(sourceAnchor);
		this.setTargetAnchor(targetAnchor);
		
		return this;
	}
	
	public PolylineConnection setLeftRightAnchors(Label source, Label target){
		LeftAnchor sourceAnchor = new LeftAnchor(source);
		RightAnchor targetAnchor = new RightAnchor(target);
		this.setSourceAnchor(sourceAnchor);
		this.setTargetAnchor(targetAnchor);
		
		return this;
	}
	
	public PolylineConnection setRightRightAnchors(Label source, Label target){
		RightAnchor sourceAnchor = new RightAnchor(source);
		RightAnchor targetAnchor = new RightAnchor(target);
		this.setSourceAnchor(sourceAnchor);
		this.setTargetAnchor(targetAnchor);
		
		
		return this;
	}
	
	public PolylineConnection setMethodToMethodStyle(){
		PolygonDecoration decoration = new PolygonDecoration();
		decoration.setTemplate(PolygonDecoration.TRIANGLE_TIP);
		decoration.setBackgroundColor(ColorConstants.black);
		this.setTargetDecoration(decoration);

		this.setForegroundColor(JConnection.methodToMethodColor);

		return this;
	}
	
	public PolylineConnection setWriteStyle(){
		PolylineDecoration decoration = new PolylineDecoration();
		decoration.setOpaque(true);
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
		Label l = new Label(Integer.toString(occurences));
		ConnectionLocator locator = new ConnectionLocator(this, ConnectionLocator.MIDDLE);
		locator.setGap(5);
		l.setFont(new Font(null, "Arial", 10, SWT.BOLD));
		l.setForegroundColor(ColorConstants.black);
		this.add(l,locator);
		
		return this;
	}
	
	public PolylineConnection setTargetBendRouter(){
		BendpointConnectionRouter router = new BendpointConnectionRouter();
		RelativeBendpoint bp1 = new RelativeBendpoint(this);
		bp1.setRelativeDimensions(new Dimension(8,8), new Dimension(8, 8));
		bp1.setWeight(0.5f);
		
		router.setConstraint(this, Arrays.asList(new Bendpoint[] {bp1}));

		this.setConnectionRouter(router);
		
		return this;
	}
	
	public PolylineConnection setSourceBendRouter(int bendHeightX){
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