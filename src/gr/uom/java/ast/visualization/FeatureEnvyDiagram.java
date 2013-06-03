package gr.uom.java.ast.visualization;


import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;



public class FeatureEnvyDiagram {

	private LayeredPane root;
	private Layer primary;
	private ConnectionLayer connections;
	private FigureCanvas canvas;
	private List<JConnection> connectionList= new ArrayList<JConnection>();

	public FeatureEnvyDiagram(FeatureEnvyVisualizationData data, Composite parent) {

		// Create a root figure and simple layout to contain all other figures
		root = new LayeredPane();

		primary = new Layer();
		primary.setLayoutManager(new XYLayout());

		root.setFont(parent.getFont());
		root.add(primary,"Primary");

		connections = new ConnectionLayer();


		// Create source class
		final ClassFigure source = new ClassFigure(data.getSourceClass().getName(), ClassFigure.classColor);
		source.setToolTip(new Label("Source Class"));
		source.addThreeCompartments();

		// Create target class
		final ClassFigure target = new ClassFigure(data.getTargetClass().getName(), ClassFigure.classColor);
		target.addTwoCompartments();
		target.setToolTip(new Label("Target Class"));

		// Create Extract Method figure that goes in the middle
		EntityFigure extractMethod = new EntityFigure(data.getMethodToBeMoved().getName(), EntityFigure.METHOD);
		extractMethod.setLabelAlignment(2);
		extractMethod.setToolTip(new Label("Method to be Extracted"));
		Font font = new Font(null, "Arial", 10, SWT.BOLD);
		extractMethod.setFont(font);

		// Create Extract Method figure that goes in Source Class
		EntityFigure extractMethod1 = new EntityFigure(data.getMethodToBeMoved().getName(), EntityFigure.METHOD);
		extractMethod1.setFont(font);
		extractMethod1.setLabelAlignment(2);
		extractMethod1.setBorder(new SimpleRaisedBorder(3));
		source.getExtractMethodCompartment().add(extractMethod1);


		// Determines interval of weight, ONLY needed when extract method is in Source class
		int size = data.getSourceMethodInvocationMap().size();
		float interval1 = 0.1f/size;

		float weight = 0.3f;

		// Creates Connections for SOURCE methods
		for(Entry<MethodInvocationObject, Integer> entry : data.getSourceMethodInvocationMap().entrySet()){

			MethodInvocationObject method = entry.getKey();
			Integer occurences = entry.getValue();

			weight=weight - interval1;

			//EntityFigure methodFigure = new EntityFigure(method.toString(),EntityFigure.METHOD);
			EntityFigure methodFigure = new EntityFigure(method.getSignature(),EntityFigure.METHOD);
			source.getMethodsCompartment().add(methodFigure);
			JConnection connection=extractMethod.addToSourceMethodConnection(ConnectionType.METHOD_CALL_SOURCE,methodFigure, occurences);
			connectionList.add(connection);
			connections.add(connection);

		}

		// Creates Connections for TARGET methods
		for(Entry<MethodInvocationObject, Integer> entry : data.getTargetMethodInvocationMap().entrySet()){
			MethodInvocationObject method = entry.getKey();
			Integer occurences = entry.getValue();

			//EntityFigure methodFigure = new EntityFigure(method.toString(), EntityFigure.METHOD);
			EntityFigure methodFigure = new EntityFigure(method.getSignature(), EntityFigure.METHOD);
			target.getMethodsCompartment().add(methodFigure);
			JConnection connection=extractMethod.addToTargetMethodConnection(ConnectionType.METHOD_CALL_TARGET,methodFigure, occurences);
			connectionList.add(connection);
			connections.add(connection);
		}

		// Creates Connections for SOURCE Read fields
		weight = 0.2f;
		float interval2 = 0.1f/data.getSourceFieldReadMap().size();
		for( Entry<FieldInstructionObject, Integer> entry : data.getSourceFieldReadMap().entrySet()){
			FieldInstructionObject field = entry.getKey();
			Integer occurences = entry.getValue();
			weight=weight + interval2;


			//EntityFigure fieldFigure = new EntityFigure(field.toString(),EntityFigure.FIELD);
			EntityFigure fieldFigure = new EntityFigure(field.getName(),EntityFigure.FIELD);
			source.getFieldsCompartment().add(fieldFigure);
			JConnection connection =extractMethod.addToSourceWeakReadConnection(ConnectionType.READ_FIELD_SOURCE,fieldFigure, occurences);
			connectionList.add(connection);
			connections.add(connection);

		}

		// Creates Connections for SOURCE Write methods
		float interval3 = 0.4f/data.getSourceFieldWriteMap().size();
		weight = 0.2f;
		for( Entry<FieldInstructionObject, Integer> entry : data.getSourceFieldWriteMap().entrySet()){
			FieldInstructionObject field = entry.getKey();
			Integer occurences = entry.getValue();
			weight=weight + interval3;

			//EntityFigure fieldFigure = new EntityFigure(field.toString(), EntityFigure.FIELD);
			EntityFigure fieldFigure = new EntityFigure(field.getName(), EntityFigure.FIELD);
			boolean contains = false;

			for(Object child : source.getFieldsCompartment().getChildren()){
				EntityFigure entity = (EntityFigure) child;
				if (entity.getName().equals(fieldFigure.getName()))
					contains = true;
				JConnection connection = extractMethod.addToSourceWeakWriteConnection(ConnectionType.WRITE_FIELD_SOURCE,entity, occurences);
				connectionList.add(connection);
				connections.add(connection);

			}

			if(!contains)	{
				source.getFieldsCompartment().add(fieldFigure);
				JConnection connection = extractMethod.addToSourceWeakWriteConnection(ConnectionType.WRITE_FIELD_SOURCE,fieldFigure, occurences);
				connectionList.add(connection);
				connections.add(connection);
			}

		}

		for( Entry<FieldInstructionObject, Integer> entry : data.getTargetFieldReadMap().entrySet()){
			FieldInstructionObject field = entry.getKey();
			Integer occurences = entry.getValue();

			//EntityFigure fieldFigure = new EntityFigure(field.toString(), EntityFigure.FIELD);
			EntityFigure fieldFigure = new EntityFigure(field.getName(), EntityFigure.FIELD);
			target.getFieldsCompartment().add(fieldFigure);
			JConnection connection =extractMethod.addToTargetReadConnection(ConnectionType.READ_FIELD_TARGET,fieldFigure, occurences);
			connectionList.add(connection);
			connections.add(connection);

		}

		for( Entry<FieldInstructionObject, Integer> entry : data.getTargetFieldWriteMap().entrySet()){
			FieldInstructionObject field = entry.getKey();
			Integer occurences = entry.getValue();

			//EntityFigure fieldFigure = new EntityFigure(field.toString(), EntityFigure.FIELD);
			EntityFigure fieldFigure = new EntityFigure(field.getName(), EntityFigure.FIELD);

			boolean contains = false;
			for(Object child : target.getFieldsCompartment().getChildren()){
				EntityFigure entity = (EntityFigure) child;
				if (entity.getName().equals(fieldFigure.getName())){
					contains = true;
					JConnection connection = extractMethod.addToTargetBendConnection(ConnectionType.WRITE_FIELD_TARGET,entity, occurences);
					connectionList.add(connection);
					connections.add(connection);
				}


			}

			if(!contains)	{
				target.getFieldsCompartment().add(fieldFigure);
				JConnection connection = extractMethod.addToTargetWriteConnection(ConnectionType.WRITE_FIELD_TARGET,fieldFigure, occurences);
				connectionList.add(connection);
				connections.add(connection);
			}

		}

		int classWidth = 300;
		int startPointX = 100;
		int startPointY = 50;
		int methodWidth = 150;
		int gap = 100;
		extractMethod.setMaximumSize(new Dimension(methodWidth,-1));

		final Legend legend = new Legend(connectionList, false);
		int legendHeight = 150;
		int legendWidth = 275;
		int legendGap = 200;

		primary.add(source, new Rectangle(startPointX,startPointY,classWidth,-1));
		primary.add(target, new Rectangle(startPointX+classWidth + methodWidth + 2*gap,startPointY,classWidth,-1));
		int sourceH = source.getPreferredSize().height;
		int targetH = target.getPreferredSize().height;
		primary.add(extractMethod, new Rectangle(startPointX+classWidth + gap, startPointY + Math.min(targetH, sourceH)/2 , -1,-1));
		;
		primary.add(legend, new Rectangle(startPointX,sourceH + legendGap, legendWidth, legendHeight));


		root.add(connections, "Connections");

		this.canvas = new FigureCanvas(parent, SWT.DOUBLE_BUFFERED);



	}

	public FigureCanvas createDiagram(){

		canvas.setBackground(ColorConstants.white);
		LightweightSystem lws = new LightweightSystem(canvas);
		lws.setContents(this.root);		
		return canvas;
	}



}

