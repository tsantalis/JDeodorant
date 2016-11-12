package gr.uom.java.ast.visualization;

import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.ScalableFreeformLayeredPane;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.widgets.Display;

public class GodClassDiagram2 {
	private ScalableFreeformLayeredPane root;
	private FreeformLayer primary;
	private ConnectionLayer connections;
	private List<JConnection> connectionList= new ArrayList<JConnection>();

	public GodClassDiagram2(GodClassVisualizationData data) {

		// Create a root figure and simple layout to contain all other figures
		root = new ScalableFreeformLayeredPane();
		primary = new FreeformLayer();
		primary.setLayoutManager(new FreeformLayout());
		root.setFont(Display.getDefault().getSystemFont());
		root.add(primary,"Primary");

		connections = new ConnectionLayer();
		
		int sourceClassWidth = 200;
		int targetClassWidth = 450;
		
		
		
		int totalExtractedMethods = data.getExtractedMethods().size();
		Set<MethodObject> union= new HashSet<MethodObject>();
		union.addAll(data.getExternalFieldReadMap().keySet());
		union.addAll(data.getExternalFieldWriteMap().keySet());
		union.addAll(data.getExternalMethodInvocationMap().keySet());

		boolean oneSection= false;
		//if(totalExtractedMethods == union.size()|| union.size()== 0)
		if(totalExtractedMethods == union.size()|| totalExtractedMethods == 1){
			oneSection = true;
			targetClassWidth = 200;
		}
		
		
		int targetSectionWidth = targetClassWidth/3;
		int bendGap;
		
		// Creates Source Class
		final ClassFigure source = new ClassFigure(data.getSourceClass().getName(), DecorationConstants.classColor);
		source.setToolTip(new Label("Source Class"));
		source.addTwoCompartments();

		//Creates Extracted Class
		final ClassFigure extractedClass = new ClassFigure("Extracted Class",  DecorationConstants.classColor);

		if(oneSection){
			extractedClass.addFieldCompartment();
			for(FieldObject field : data.getExtractedFields()){
				EntityFigure fieldFigure = new EntityFigure(field.getName(), DecorationConstants.FIELD, true);
				extractedClass.getFieldsCompartment().addFigure(fieldFigure);
			}
		} else
		{
			extractedClass.addFieldSectionCompartment();

			for(FieldObject field : data.getExtractedFields()){
				EntityFigure fieldFigure = new EntityFigure(field.getName(), DecorationConstants.FIELD, true);
				extractedClass.getFieldSectionCompartment().getSectionTwo().addFigure(fieldFigure);
			}
		}


		if(oneSection)
			extractedClass.addMethodSectionCompartment(1);
		else
			extractedClass.addMethodSectionCompartment(3);
		//extractedClass.getMethodSectionCompartment().addAllSections();
		extractedClass.setToolTip(new Label("Extracted Class"));



		MethodClassSection sectionOne = extractedClass.getMethodSectionCompartment().getSectionOne();
		MethodClassSection sectionThree = extractedClass.getMethodSectionCompartment().getSectionThree();


		//Adds Read Connections from Methods to Fields in Source Class
		for(Entry<MethodObject, Map<FieldInstructionObject, Integer>> entry : data.getExternalFieldReadMap().entrySet()){

			MethodObject extractedMethod = entry.getKey();
			Map<FieldInstructionObject, Integer> connectionMap = entry.getValue();

			EntityFigure extractedMethodFigure = new EntityFigure(extractedMethod.getSignature(), DecorationConstants.METHOD, true);
			sectionOne.addFigure(extractedMethodFigure);

			for(Entry<FieldInstructionObject, Integer> map  : connectionMap.entrySet()){
				FieldInstructionObject field = map.getKey();
				Integer occurences = map.getValue();

				EntityFigure fieldFigure = new EntityFigure(field.getName(), DecorationConstants.FIELD, true);


				boolean contains= false;
				for(Object child : source.getFieldsCompartment().getChildren()){
					EntityFigure entity = (EntityFigure) child;
					if (entity.getName().equals(fieldFigure.getName())){
						contains = true;
						JConnection connection = extractedMethodFigure.addToSourceWeakReadConnection(ConnectionType.READ_FIELD_SOURCE, entity, occurences);
						connectionList.add(connection);
						connections.add(connection);
					}


				}

				if(!contains)	{

					//source.getFieldsCompartment().add(fieldFigure);
					source.getFieldsCompartment().addFigure(fieldFigure);
					JConnection connection = extractedMethodFigure.addToSourceWeakReadConnection(ConnectionType.READ_FIELD_SOURCE, fieldFigure, occurences);
					connectionList.add(connection);
					connections.add(connection);
				}


			}
		}


		//Adds Write Connections from Methods to Fields in Source Class
		for(Entry<MethodObject, Map<FieldInstructionObject, Integer>> entry : data.getExternalFieldWriteMap().entrySet()){

			MethodObject extractedMethod = entry.getKey();
			Map<FieldInstructionObject, Integer> connectionMap = entry.getValue();
			EntityFigure connectionSource= null;
			boolean contains= false;

			EntityFigure extractedMethodFigure = new EntityFigure(extractedMethod.getSignature(), DecorationConstants.METHOD, true);

			for(Object child : sectionOne.getChildren()){
				EntityFigure entity = (EntityFigure) child;
				if (entity.getName().equals(extractedMethodFigure.getName())){
					connectionSource = entity;
					contains = true;
				}

			}
			if(!contains){
				sectionOne.addFigure(extractedMethodFigure);
				connectionSource = extractedMethodFigure;
			}


			for(Entry<FieldInstructionObject, Integer> map  : connectionMap.entrySet()){
				FieldInstructionObject field = map.getKey();
				Integer occurences = map.getValue();

				EntityFigure fieldFigure = new EntityFigure(field.getName(), DecorationConstants.FIELD, true);

				contains= false;
				for(Object child : source.getFieldsCompartment().getChildren()){
					EntityFigure entity = (EntityFigure) child;
					if (entity.getName().equals(fieldFigure.getName())){
						contains = true;
						//JConnection connection = connectionSource.addToSourceWeakWriteConnection(ConnectionType.WRITE_FIELD_SOURCE, entity, occurences);
						JConnection connection = connectionSource.addToSourceBendConnection(ConnectionType.WRITE_FIELD_SOURCE, entity, occurences);
						connectionList.add(connection);
						connections.add(connection);
					}


				}

				if(!contains)	{

					source.getFieldsCompartment().addFigure(fieldFigure);
					//source.getFieldsCompartment().add(fieldFigure);
					JConnection connection = connectionSource.addToSourceWeakWriteConnection(ConnectionType.WRITE_FIELD_SOURCE, fieldFigure, occurences);
					connectionList.add(connection);
					connections.add(connection);
				}
			}
		}

		//Adds Connections from Methods to other Methods in Source Class

		for(Entry<MethodObject, Map<MethodInvocationObject, Integer>> entry : data.getExternalMethodInvocationMap().entrySet()){

			MethodObject extractedMethod = entry.getKey();
			Map<MethodInvocationObject, Integer> connectionMap = entry.getValue();
			EntityFigure connectionSource= null;
			boolean contains= false;

			EntityFigure extractedMethodFigure = new EntityFigure(extractedMethod.getSignature(), DecorationConstants.METHOD, true);

			for(Object child : sectionOne.getChildren()){
				EntityFigure entity = (EntityFigure) child;
				if (entity.getName().equals(extractedMethodFigure.getName())){
					connectionSource = entity;
					contains = true;
				}

			}
			if(!contains){
				sectionOne.addFigure(extractedMethodFigure);
				connectionSource = extractedMethodFigure;
			}

			for(Entry<MethodInvocationObject, Integer> map  : connectionMap.entrySet()){
				MethodInvocationObject method = map.getKey();
				Integer occurences = map.getValue();

				EntityFigure methodFigure = new EntityFigure(method.getSignature(), DecorationConstants.METHOD, true);
				contains= false;
				for(Object child : source.getMethodsCompartment().getChildren()){
					EntityFigure entity = (EntityFigure) child;
					if (entity.getName().equals(methodFigure.getName())){
						contains = true;
						JConnection connection =connectionSource.addToSourceMethodConnection(ConnectionType.METHOD_CALL_SOURCE,entity, occurences);
						connectionList.add(connection);
						connections.add(connection);
					}

				}

				if(!contains)	{

					source.getMethodsCompartment().addFigure(methodFigure);
					//source.getFieldsCompartment().add(methodFigure);
					JConnection connection =connectionSource.addToSourceMethodConnection(ConnectionType.METHOD_CALL_SOURCE,methodFigure, occurences);
					connectionList.add(connection);
					connections.add(connection);
				}

			}
		}



		//Adds Write Connections from Methods to Fields in Extracted Class
		int bendHeight;
		for(Entry<MethodObject, Map<FieldInstructionObject, Integer>> entry : data.getInternalFieldWriteMap().entrySet()){
			
			bendGap = 40;

			MethodObject extractedMethod = entry.getKey();
			Map<FieldInstructionObject, Integer> connectionMap = entry.getValue();
			EntityFigure connectionSource = null;

			boolean contains= false;

			EntityFigure extractedMethodFigure = new EntityFigure(extractedMethod.getSignature(), DecorationConstants.METHOD, true);

			for(Object child : sectionOne.getChildren()){
				EntityFigure entity = (EntityFigure) child;
				if (entity.getName().equals(extractedMethodFigure.getName())){
					connectionSource = entity;
					contains = true;
				}

			}
			if(!contains){
				if(oneSection)
					sectionOne.addFigure(extractedMethodFigure);
				else
					sectionThree.addFigure(extractedMethodFigure);

				connectionSource = extractedMethodFigure;
			}

			for(Entry<FieldInstructionObject, Integer> map  : connectionMap.entrySet()){
				FieldInstructionObject field = map.getKey();
				Integer occurences = map.getValue();

				EntityFigure fieldFigure = new EntityFigure(field.getName(), DecorationConstants.FIELD, true);
				List fields;
				if (oneSection){
					fields = extractedClass.getFieldsCompartment().getChildren();
					bendHeight = targetClassWidth +bendGap ;
				} else
				{
					fields = extractedClass.getFieldSectionCompartment().getSectionTwo().getChildren();
					bendHeight = targetSectionWidth + bendGap ;
				}
				for(Object child : fields){
					EntityFigure entity = (EntityFigure) child;
					if (entity.getName().equals(fieldFigure.getName())){
						JConnection connection;
						if(!contains|| oneSection){
							connection = connectionSource.addToSameClassWriteConnectionRR(ConnectionType.WRITE_FIELD_TARGET,entity, occurences, bendHeight);
							connectionList.add(connection);
							connections.add(connection);
						}

						else{
							connection = connectionSource.addToSameClassWriteConnectionLL(ConnectionType.WRITE_FIELD_TARGET,entity, occurences, bendHeight);
							connectionList.add(connection);
							connections.add(connection);
						}

					}
				}
			}
		}


		//Adds Read Connections from Methods to Fields in Extracted Class
		for(Entry<MethodObject, Map<FieldInstructionObject, Integer>> entry : data.getInternalFieldReadMap().entrySet()){
			bendGap = 10;

			MethodObject extractedMethod = entry.getKey();
			Map<FieldInstructionObject, Integer> connectionMap = entry.getValue();
			EntityFigure connectionSource = null;
			boolean contains= false;
			boolean inRightSection = false;

			EntityFigure extractedMethodFigure = new EntityFigure(extractedMethod.getSignature(), DecorationConstants.METHOD, true);

			for(Object child : sectionOne.getChildren()){
				EntityFigure entity = (EntityFigure) child;
				if (entity.getName().equals(extractedMethodFigure.getName())){
					connectionSource = entity;
					contains = true;
				}

			}
			if(!contains){
				for(Object child : sectionThree.getChildren()){
					EntityFigure entity = (EntityFigure) child;
					if (entity.getName().equals(extractedMethodFigure.getName())){
						connectionSource = entity;
						inRightSection = true;
						contains = true;
					}

				}
			}

			if(!contains){

				if(sectionOne.getNumOfMethods()<= sectionThree.getNumOfMethods())
					sectionOne.addFigure(extractedMethodFigure);
				else{
					sectionThree.addFigure(extractedMethodFigure);
					inRightSection = true;
				}

				connectionSource = extractedMethodFigure;
			}



			for(Entry<FieldInstructionObject, Integer> map  : connectionMap.entrySet()){
				
				FieldInstructionObject field = map.getKey();
				Integer occurences = map.getValue();

				EntityFigure fieldFigure = new EntityFigure(field.getName(), DecorationConstants.FIELD, true);

				List fields;
				if (oneSection){
					fields = extractedClass.getFieldsCompartment().getChildren();
					bendHeight = targetClassWidth + bendGap;
				} else
				{
					fields = extractedClass.getFieldSectionCompartment().getSectionTwo().getChildren();
					bendHeight = targetSectionWidth + bendGap;
				}

				for(Object child : fields){
					EntityFigure entity = (EntityFigure) child;
					if (entity.getName().equals(fieldFigure.getName())){
						JConnection connection;
						if(inRightSection || oneSection){
							connection = connectionSource.addToSameClassReadConnectionRR(ConnectionType.READ_FIELD_TARGET,entity, occurences,  bendHeight);
							connectionList.add(connection);
							connections.add(connection);
						}
						else{
							connection = connectionSource.addToSameClassReadConnectionLL(ConnectionType.READ_FIELD_TARGET,entity, occurences,bendHeight);
							connectionList.add(connection);
							connections.add(connection);

						}


					}
				}
			}
		}

		//Adds Connections from Methods to other Methods in Extracted Class
		Set<Entry<MethodObject, Map<MethodInvocationObject, Integer>>> internalMethodInvocation = data.getInternalMethodInvocationMap().entrySet();
		for(Entry<MethodObject, Map<MethodInvocationObject, Integer>> entry : internalMethodInvocation){
			bendGap = -20;

			MethodObject extractedMethod = entry.getKey();
			Map<MethodInvocationObject, Integer> connectionMap = entry.getValue();
			EntityFigure connectionSource = null;
			
			boolean contains= false;
			boolean sourceinRightSection = false;
			

			EntityFigure extractedMethodFigure = new EntityFigure(extractedMethod.getSignature(), DecorationConstants.METHOD, true);

			//check if method is in Left Section already
			for(Object child : sectionOne.getChildren()){
				EntityFigure entity = (EntityFigure) child;
				if (entity.getName().equals(extractedMethodFigure.getName())){
					connectionSource = entity;
					contains = true;
				}

			}
			//Check if method is in Right Section already
			if(!contains){
				for(Object child : sectionThree.getChildren()){
					EntityFigure entity = (EntityFigure) child;
					if (entity.getName().equals(extractedMethodFigure.getName())){
						connectionSource = entity;
						sourceinRightSection = true;
						contains = true;
					}

				}
			}

			//If its not already there, add it so that the two sides are even
			if(!contains){

				if(sectionOne.getNumOfMethods()<= sectionThree.getNumOfMethods())
					sectionOne.addFigure(extractedMethodFigure);
				else{
					sectionThree.addFigure(extractedMethodFigure);
					sourceinRightSection = true;
				}

				connectionSource = extractedMethodFigure;
			}


			for(Entry<MethodInvocationObject, Integer> map  : connectionMap.entrySet()){
				contains = false;
				MethodInvocationObject target = map.getKey();
				Integer occurences = map.getValue();

				EntityFigure targetFigure = new EntityFigure(target.getSignature(), DecorationConstants.METHOD, true);


				//checks if Target Connection Method is in Left Section
				for(Object child : sectionOne.getChildren()){
					EntityFigure entity = (EntityFigure) child;
					if (entity.getName().equals(targetFigure.getName())){
						//connectionTarget = entity;
						contains = true;
						JConnection connection;
						if(sourceinRightSection){
							connection = connectionSource.addLeftRightMethodConnection(ConnectionType.METHOD_CALL_TARGET,entity, occurences);
							connectionList.add(connection);
							connections.add(connection);
						}

						else{
							if(oneSection)
								bendHeight = targetClassWidth + bendGap;
							else
								bendHeight = targetSectionWidth + bendGap;

							connection = connectionSource.addToSameClassMethodConnectionRR(ConnectionType.METHOD_CALL_TARGET, entity, occurences, bendHeight);
							connectionList.add(connection);
							connections.add(connection);
						}

					}

				}

				//checks if Target Connection Method is in Right Section
				if(!contains){
					for(Object child : sectionThree.getChildren()){
						EntityFigure entity = (EntityFigure) child;
						if (entity.getName().equals(targetFigure.getName())){
							//connectionTarget = entity;
							contains = true;
							//targetinRightSection = true;
							JConnection connection;
							if(sourceinRightSection){
								bendHeight = targetSectionWidth + bendGap;
								connection = connectionSource.addToSameClassMethodConnectionLL(ConnectionType.METHOD_CALL_TARGET,entity, occurences, bendHeight);
								connectionList.add(connection);
								connections.add(connection);
							}

							else{
								connection = connectionSource.addRightLeftMethodConnection(ConnectionType.METHOD_CALL_TARGET,entity, occurences);
								connectionList.add(connection);
								connections.add(connection);
							}
						}

					}
				}

				//If its not already there, add it to Right Section
				if(!contains){

					sectionThree.addFigure(targetFigure);
					JConnection connection ;
					if(sourceinRightSection){
						if(oneSection)
							bendHeight = targetClassWidth + bendGap;
						else
							bendHeight = targetSectionWidth + bendGap;
						connection = connectionSource.addToSameClassMethodConnectionRR(ConnectionType.METHOD_CALL_TARGET,targetFigure, occurences, bendHeight);
						connectionList.add(connection);
						connections.add(connection);
					}
					else{
						connection = connectionSource.addRightLeftMethodConnection(ConnectionType.METHOD_CALL_TARGET, targetFigure, occurences);
						connectionList.add(connection);
						connections.add(connection);
					}


				}


			}
		}

		boolean contains;
		//Adds Methods that were not already added
		for(MethodObject method : data.getExtractedMethods()){
			contains = false;
			EntityFigure methodFigure = new EntityFigure(method.getSignature(), DecorationConstants.METHOD, true);
			//checks if Method is in Left Section
			for(Object child : sectionOne.getChildren()){
				EntityFigure entity = (EntityFigure) child;
				if (entity.getName().equals(methodFigure.getName())){
					//connectionTarget = entity;
					contains = true;
				}

			}

			//checks if Method is in Right Section
			if(!contains){
				for(Object child : sectionThree.getChildren()){
					EntityFigure entity = (EntityFigure) child;
					if (entity.getName().equals(methodFigure.getName())){
						contains= true;
					}

				}
			}

			//If its not already there, add it so it evens out the sections
			if(!contains){

				if(sectionOne.getNumOfMethods()<= sectionThree.getNumOfMethods())
					sectionOne.addFigure(methodFigure);
				else{
					sectionThree.addFigure(methodFigure);
				}

			}
		}



		int startPointX = 100;
		int startPointY = 50;
		int gap = 300;

		
		final Legend legend = new Legend(connectionList, true);
		int legendWidth = 325;
		int legendHeight = 150;
		int legendGap = 200;

		primary.add(source, new Rectangle(startPointX,startPointY,sourceClassWidth,-1));
		primary.add(extractedClass, new Rectangle(startPointX + sourceClassWidth + gap, startPointY, targetClassWidth,-1));
		int sourceH = source.getPreferredSize().height;
		primary.add(legend, new Rectangle(startPointX,sourceH+ legendGap, legendWidth, legendHeight));

		root.add(connections, "Connections");
		
		//this.canvas= new FigureCanvas(parent, SWT.DOUBLE_BUFFERED);
	}

	public ScalableFreeformLayeredPane getRoot() {
		return root;
	}
}
