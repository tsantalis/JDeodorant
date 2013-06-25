package gr.uom.java.ast.visualization;

import java.util.List;

import org.eclipse.draw2d.ButtonModel;
import org.eclipse.draw2d.ChangeEvent;
import org.eclipse.draw2d.ChangeListener;
import org.eclipse.draw2d.CheckBox;
import org.eclipse.draw2d.Clickable;
import org.eclipse.draw2d.CompoundBorder;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.ToggleModel;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;

public class Legend extends Figure {

	private List<JConnection> connectionList;


	public Legend(List<JConnection> connectionList, boolean isGodClass) {

		this.connectionList= connectionList;
		ToolbarLayout layout = new ToolbarLayout();
		layout.setSpacing(5);
		setLayoutManager(layout);	
		setBorder(new CompoundBorder( new LineBorder(1), new MarginBorder(0, 0, 0, 0)));
		layout.setMinorAlignment(ToolbarLayout.ALIGN_TOPLEFT);
		layout.setStretchMinorAxis(true);

		setOpaque(true);

		Font classFont = DecorationConstants.classFont;
		Label className = new Label("Legend");
		className.setFont(classFont);

		new ClassFigureMover(this);


		Figure MethodSectionCompartment = new Figure();

		MethodSectionCompartment.setFont( Display.getCurrent().getSystemFont() ); 
		GridLayout gridLayout = new GridLayout(3,false);
		MethodSectionCompartment.setLayoutManager(gridLayout);
		//MethodSectionCompartment.setBorder(new CompartmentFigureBorder());
		//MethodSectionCompartment.setBorder(new LineBorder(2));
		if(isGodClass)
			MethodSectionCompartment.setPreferredSize(300,150);
		else
			MethodSectionCompartment.setPreferredSize(250,150);

		Figure connectionSection = new Figure();
		connectionSection.setFont( Display.getCurrent().getSystemFont() ); 
		ToolbarLayout layout2 = new ToolbarLayout();
		layout2.setSpacing(5);
		layout2.setMinorAlignment(ToolbarLayout.ALIGN_TOPLEFT);
		layout2.setStretchMinorAxis(true);
		connectionSection.setPreferredSize(50,150);
		connectionSection.setLayoutManager(layout2);

		Figure labelSection = new Figure();
		labelSection.setFont( Display.getCurrent().getSystemFont() );
		labelSection.setLayoutManager(layout2);
		labelSection.setPreferredSize(125,150);

		Figure checkBoxSection = new Figure();
		checkBoxSection.setFont( Display.getCurrent().getSystemFont() );
		ToolbarLayout tlayout = new ToolbarLayout();
		tlayout.setMinorAlignment(ToolbarLayout.ALIGN_TOPLEFT);
		tlayout.setStretchMinorAxis(true);
		tlayout.setSpacing(11);
		checkBoxSection.setLayoutManager(tlayout);
		checkBoxSection.setPreferredSize(25,150);
		//checkBoxSection.setBorder(new LineBorder(1));

		MethodSectionCompartment.add(checkBoxSection, new GridData());
		MethodSectionCompartment.add(connectionSection, new GridData() );
		MethodSectionCompartment.add(labelSection, new GridData(GridData.FILL_HORIZONTAL));

		String target;

		EntityFigure weakReadLabel =  new EntityFigure("Read Field From Source Class");
		weakReadLabel.setLabelAlignment(2);
		EntityFigure weakWriteLabel = new EntityFigure("Write Field From Source Class");
		weakWriteLabel.setLabelAlignment(2);
		EntityFigure sourceMethodCallLabel = new EntityFigure("Method Call From Source Class");
		sourceMethodCallLabel.setLabelAlignment(2);

		EntityFigure strongReadLabel;
		EntityFigure strongWriteLabel;
		EntityFigure targetMethodCallLabel;

		if(isGodClass){
			target = "Extracted Class";

		} else
			target = "Target Class";

		strongReadLabel = new EntityFigure("Read Field From "+ target);
		strongWriteLabel = new EntityFigure("Write Field From " + target);
		targetMethodCallLabel = new EntityFigure("Method Call From "+target);




		targetMethodCallLabel.setLabelAlignment(2);
		strongReadLabel.setLabelAlignment(2);
		strongWriteLabel.setLabelAlignment(2);


		//this.addMethodSectionCompartment(2);
		//this.getMethodSectionCompartment().getLeftSection().setBorder(new MarginBorder(5,5,5,5));
		labelSection.add(weakReadLabel);
		labelSection.add(weakWriteLabel);
		labelSection.add(strongReadLabel);
		labelSection.add(strongWriteLabel);
		labelSection.add(sourceMethodCallLabel);
		labelSection.add(targetMethodCallLabel);


		EntityFigure toSourceReadFigure = new EntityFigure(null);
		EntityFigure toSourceWriteFigure = new EntityFigure(null);
		EntityFigure toTargetReadFigure = new EntityFigure(null);
		EntityFigure toTargetWriteFigure = new EntityFigure(null);
		EntityFigure toSourceMethodFigure = new EntityFigure(null);
		EntityFigure toTargetMethodFigure = new EntityFigure(null); 

		connectionSection.add(toSourceReadFigure);
		connectionSection.add(toSourceWriteFigure);
		connectionSection.add(toTargetReadFigure);
		connectionSection.add(toTargetWriteFigure);
		connectionSection.add(toSourceMethodFigure);
		connectionSection.add(toTargetMethodFigure);


		CheckBox  toSourceReadBox = newCheckBox(ConnectionType.READ_FIELD_SOURCE);
		Clickable toSourceWriteBox = newCheckBox(ConnectionType.WRITE_FIELD_SOURCE);
		Clickable toTargetReadBox = newCheckBox(ConnectionType.READ_FIELD_TARGET);
		Clickable toTargetWriteBox = newCheckBox(ConnectionType.WRITE_FIELD_TARGET);
		Clickable methodToSourceBox = newCheckBox(ConnectionType.METHOD_CALL_SOURCE);
		Clickable methodToTargetBox = newCheckBox(ConnectionType.METHOD_CALL_TARGET);

		checkBoxSection.add(toSourceReadBox);
		checkBoxSection.add(toSourceWriteBox);
		checkBoxSection.add(toTargetReadBox);
		checkBoxSection.add(toTargetWriteBox);
		checkBoxSection.add(methodToSourceBox);
		checkBoxSection.add(methodToTargetBox);

		this.add(MethodSectionCompartment);

		this.add(toSourceReadFigure.addToSourceWeakReadConnection(ConnectionType.READ_FIELD_SOURCE,toSourceReadFigure,DecorationConstants.NO_OCCURENCES));
		this.add(toSourceWriteFigure.addToSourceWeakWriteConnection(ConnectionType.WRITE_FIELD_SOURCE,toSourceWriteFigure, DecorationConstants.NO_OCCURENCES));
		this.add(toTargetReadFigure.addToSourceReadConnection(ConnectionType.READ_FIELD_TARGET,toTargetReadFigure, DecorationConstants.NO_OCCURENCES));
		this.add(toTargetWriteFigure.addToSourceWriteConnection(ConnectionType.WRITE_FIELD_TARGET,toTargetWriteFigure, DecorationConstants.NO_OCCURENCES));
		this.add(toSourceMethodFigure.addToSourceMethodConnection(ConnectionType.METHOD_CALL_SOURCE, toSourceMethodFigure, DecorationConstants.NO_OCCURENCES));
		this.add(toTargetMethodFigure.addLeftRightMethodConnection(ConnectionType.METHOD_CALL_TARGET, toTargetMethodFigure, DecorationConstants.NO_OCCURENCES));




	}

	public CheckBox newCheckBox(ConnectionType type){
		CheckBox checkBox = new CheckBox();
		final ConnectionType connectionType = type;
		checkBox.setSelected(true);
		final ButtonModel model = new ToggleModel();
		model.addChangeListener(new ChangeListener() {
			public void handleStateChanged(ChangeEvent e) {
				boolean selected = model.isSelected();

				if(e.getPropertyName().equalsIgnoreCase("selected")){

					if(selected){
						for(JConnection connection : connectionList){
							if(connection.getType().equals(connectionType)){
								connection.setVisible(true);
							}
						}
					}

					else{
						for(JConnection connection : connectionList){
							if(connection.getType().equals(connectionType)){
								connection.setVisible(false);
							}
						}
					}

				}

			}
		});
		checkBox.setModel(model);

		return checkBox;
	}
}
