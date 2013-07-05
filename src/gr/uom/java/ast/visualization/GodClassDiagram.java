package gr.uom.java.ast.visualization;

import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Layer;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class GodClassDiagram {
	private LayeredPane root;
	Layer primary;
	ConnectionLayer connections;
	private FigureCanvas canvas;

	public GodClassDiagram(GodClassVisualizationData data, Composite parent) {

		// Create a root figure and simple layout to contain all other figures
		root = new LayeredPane();
		primary = new Layer();
		primary.setLayoutManager(new XYLayout());
		root.setFont(parent.getFont());
		root.add(primary,"Primary");

		connections = new ConnectionLayer();
		//connections.setConnectionRouter( new ShortestPathConnectionRouter(primary));

		int classWidth = 200;

		// Creates Source Class
		final ClassFigure source = new ClassFigure(data.getSourceClass().getName(), DecorationConstants.classColor);
		source.setToolTip(new Label("Source Class"));
		source.addTwoCompartments();

		//Creates Extracted Class
		final ClassFigure extractedClass = new ClassFigure("Extracted Class", DecorationConstants.classColor);
		extractedClass.addTwoCompartments();
		extractedClass.setToolTip(new Label("Extracted Class"));

		for(FieldObject field : data.getExtractedFields()){
			EntityFigure fieldFigure = new EntityFigure(field.getName(), DecorationConstants.FIELD, true);
			extractedClass.getFieldsCompartment().add(fieldFigure);
		}

		for(MethodObject method : data.getExtractedMethods()){
			EntityFigure methodFigure = new EntityFigure(method.getName(), DecorationConstants.METHOD, true);
			extractedClass.getMethodsCompartment().add(methodFigure);
		}



		//Adds Connections from Methods to other Methods in Extracted Class
		Set<Entry<MethodObject, Map<MethodInvocationObject, Integer>>> internalMethodInvocation = data.getInternalMethodInvocationMap().entrySet();
		for(Entry<MethodObject, Map<MethodInvocationObject, Integer>> entry : internalMethodInvocation){

			MethodObject extractedMethod = entry.getKey();
			Map<MethodInvocationObject, Integer> connectionMap = entry.getValue();
			EntityFigure connectionSource = null;

			for(Object child : extractedClass.getMethodsCompartment().getChildren()){
				EntityFigure entity = (EntityFigure) child;
				if (entity.getName().equals(extractedMethod.getName()))
					connectionSource = entity;
			}

			for(Entry<MethodInvocationObject, Integer> map  : connectionMap.entrySet()){
				MethodInvocationObject method = map.getKey();
				Integer occurences = map.getValue();

				EntityFigure methodFigure = new EntityFigure(method.getMethodName(), DecorationConstants.METHOD, true);

				for(Object child : extractedClass.getMethodsCompartment().getChildren()){
					EntityFigure entity = (EntityFigure) child;
					if (entity.getName().equals(methodFigure.getName())){
						connections.add(connectionSource.addToSameClassMethodConnectionRR(ConnectionType.METHOD_CALL_TARGET, entity, occurences, classWidth));
					}
				}
			}
		}


		//Adds Connections from Methods to other Methods in Source Class

		for(Entry<MethodObject, Map<MethodInvocationObject, Integer>> entry : data.getExternalMethodInvocationMap().entrySet()){

			MethodObject extractedMethod = entry.getKey();
			Map<MethodInvocationObject, Integer> connectionMap = entry.getValue();
			EntityFigure connectionSource = null;

			for(Object child : extractedClass.getMethodsCompartment().getChildren()){
				EntityFigure entity = (EntityFigure) child;
				if (entity.getName().equals(extractedMethod.getName()))
					connectionSource = entity;
			}

			for(Entry<MethodInvocationObject, Integer> map  : connectionMap.entrySet()){
				MethodInvocationObject method = map.getKey();
				Integer occurences = map.getValue();

				EntityFigure methodFigure = new EntityFigure(method.getMethodName(), DecorationConstants.METHOD, true);
				boolean contains= false;
				for(Object child : source.getMethodsCompartment().getChildren()){
					EntityFigure entity = (EntityFigure) child;
					if (entity.getName().equals(methodFigure.getName())){
						contains = true;
						connections.add(connectionSource.addToSourceMethodConnection(ConnectionType.METHOD_CALL_SOURCE,entity, occurences));
					}
						

				}

				if(!contains)	{

					source.getMethodsCompartment().add(methodFigure);
					connections.add(connectionSource.addToSourceMethodConnection(ConnectionType.METHOD_CALL_SOURCE, methodFigure, occurences));
				}

			}
		}


		//Adds Read Connections from Methods to Fields in Extracted Class
		for(Entry<MethodObject, Map<FieldInstructionObject, Integer>> entry : data.getInternalFieldReadMap().entrySet()){

			MethodObject extractedMethod = entry.getKey();
			Map<FieldInstructionObject, Integer> connectionMap = entry.getValue();
			EntityFigure connectionSource = null;



			for(Object child : extractedClass.getMethodsCompartment().getChildren()){
				EntityFigure entity = (EntityFigure) child;
				if (entity.getName().equals(extractedMethod.getName()))
					connectionSource = entity;
			}

			for(Entry<FieldInstructionObject, Integer> map  : connectionMap.entrySet()){
				FieldInstructionObject field = map.getKey();
				Integer occurences = map.getValue();

				EntityFigure fieldFigure = new EntityFigure(field.getName(), DecorationConstants.FIELD, true);

				for(Object child : extractedClass.getFieldsCompartment().getChildren()){
					EntityFigure entity = (EntityFigure) child;
					if (entity.getName().equals(fieldFigure.getName())){
						connections.add(connectionSource.addToSameClassReadConnectionRR(ConnectionType.READ_FIELD_TARGET,entity, occurences, classWidth));
					}
				}
			}
		}


		//Adds Write Connections from Methods to Fields in Extracted Class
		for(Entry<MethodObject, Map<FieldInstructionObject, Integer>> entry : data.getInternalFieldWriteMap().entrySet()){

			MethodObject extractedMethod = entry.getKey();
			Map<FieldInstructionObject, Integer> connectionMap = entry.getValue();
			EntityFigure connectionSource = null;



			for(Object child : extractedClass.getMethodsCompartment().getChildren()){
				EntityFigure entity = (EntityFigure) child;
				if (entity.getName().equals(extractedMethod.getName()))
					connectionSource = entity;
			}

			for(Entry<FieldInstructionObject, Integer> map  : connectionMap.entrySet()){
				FieldInstructionObject field = map.getKey();
				Integer occurences = map.getValue();

				EntityFigure fieldFigure = new EntityFigure(field.getName(), DecorationConstants.FIELD, true);

				for(Object child : extractedClass.getFieldsCompartment().getChildren()){
					EntityFigure entity = (EntityFigure) child;
					if (entity.getName().equals(fieldFigure.getName())){
						connections.add(connectionSource.addToSameClassWriteConnectionRR(ConnectionType.WRITE_FIELD_TARGET, entity, occurences, classWidth));
					}
				}
			}
		}


		//Adds Read Connections from Methods to Fields in Source Class
		for(Entry<MethodObject, Map<FieldInstructionObject, Integer>> entry : data.getExternalFieldReadMap().entrySet()){

			MethodObject extractedMethod = entry.getKey();
			Map<FieldInstructionObject, Integer> connectionMap = entry.getValue();
			EntityFigure connectionSource = null;



			for(Object child : extractedClass.getMethodsCompartment().getChildren()){
				EntityFigure entity = (EntityFigure) child;
				if (entity.getName().equals(extractedMethod.getName()))
					connectionSource = entity;
			}

			for(Entry<FieldInstructionObject, Integer> map  : connectionMap.entrySet()){
				FieldInstructionObject field = map.getKey();
				Integer occurences = map.getValue();

				EntityFigure fieldFigure = new EntityFigure(field.getName(), DecorationConstants.FIELD, true);
				
				
				boolean contains= false;
				for(Object child : source.getFieldsCompartment().getChildren()){
					EntityFigure entity = (EntityFigure) child;
					if (entity.getName().equals(fieldFigure.getName())){
						contains = true;
						connections.add(connectionSource.addToSourceWeakReadConnection(ConnectionType.READ_FIELD_SOURCE,entity, occurences));
					}
						

				}

				if(!contains)	{

					source.getFieldsCompartment().add(fieldFigure);
					connections.add(connectionSource.addToSourceWeakReadConnection(ConnectionType.READ_FIELD_SOURCE, fieldFigure, occurences));
				}

				
			}
		}


		//Adds Write Connections from Methods to Fields in Source Class
		for(Entry<MethodObject, Map<FieldInstructionObject, Integer>> entry : data.getExternalFieldWriteMap().entrySet()){

			MethodObject extractedMethod = entry.getKey();
			Map<FieldInstructionObject, Integer> connectionMap = entry.getValue();
			EntityFigure connectionSource = null;



			for(Object child : extractedClass.getMethodsCompartment().getChildren()){
				EntityFigure entity = (EntityFigure) child;
				if (entity.getName().equals(extractedMethod.getName()))
					connectionSource = entity;
			}

			for(Entry<FieldInstructionObject, Integer> map  : connectionMap.entrySet()){
				FieldInstructionObject field = map.getKey();
				Integer occurences = map.getValue();

				EntityFigure fieldFigure = new EntityFigure(field.getName(), DecorationConstants.FIELD, true);

				boolean contains= false;
				for(Object child : source.getFieldsCompartment().getChildren()){
					EntityFigure entity = (EntityFigure) child;
					if (entity.getName().equals(fieldFigure.getName())){
						contains = true;
						connections.add(connectionSource.addToSourceWeakWriteConnection(ConnectionType.WRITE_FIELD_SOURCE, entity, occurences));
					}
						

				}

				if(!contains)	{

					source.getFieldsCompartment().add(fieldFigure);
					connections.add(connectionSource.addToSourceWeakWriteConnection(ConnectionType.WRITE_FIELD_SOURCE, fieldFigure, occurences));
				}
			}
		}

		int startPointX = 100;
		int startPointY = 50;
		int gap = 300;


		primary.add(source, new Rectangle(startPointX,startPointY,classWidth,-1));
		primary.add(extractedClass, new Rectangle(startPointX + classWidth + gap, startPointY, classWidth,-1));


		root.add(connections, "Connections");

		this.canvas= new FigureCanvas(parent, SWT.DOUBLE_BUFFERED);


	}

	public FigureCanvas createDiagram(){

		canvas.setBackground(ColorConstants.white);
		LightweightSystem lws = new LightweightSystem(canvas);
		lws.setContents(this.root);
		return canvas;
	}

}
