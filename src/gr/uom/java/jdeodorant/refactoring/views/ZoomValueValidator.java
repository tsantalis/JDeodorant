package gr.uom.java.jdeodorant.refactoring.views;

import org.eclipse.jface.dialogs.IInputValidator;

public class ZoomValueValidator implements IInputValidator {

	public String isValid(String newText) {
		try{
			int value = Integer.parseInt(newText);
			if (value>0)
				return null;
			else
				return "Zoom value should be a positive integer";
		} catch(NumberFormatException exception){
			return "Zoom value should be a positive integer";
		} 
		
	}

}
