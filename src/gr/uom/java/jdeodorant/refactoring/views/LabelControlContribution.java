package gr.uom.java.jdeodorant.refactoring.views;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class LabelControlContribution extends ControlContribution  
{  
	private String name;
	private Font font;
	private static final Font CONSOLAS_BOLD_FONT = new Font(null, "Consolas", 10, SWT.BOLD);
	private Label label;

	protected LabelControlContribution(String id, String name, Font font)  
	{ 
		super(id);  
		this.name= name;
		this.font= font;
	}  

	protected LabelControlContribution(String id, String name)  
	{ 
		super(id);  
		this.name= name;
		this.font= CONSOLAS_BOLD_FONT;
	}  

	@Override  
	protected Control createControl(Composite parent)  
	{  
		label= new Label(parent, SWT.CENTER);
		label.setText(name);
		if(font != null)
			label.setFont(font);
		return label;
	}
}