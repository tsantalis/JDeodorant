package gr.uom.java.jdeodorant.refactoring.views;

import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.IInputValidator;

public class MethodNameValidator implements IInputValidator {

	public String isValid(String newText) {
		String pattern = "[a-zA-Z\\$_][a-zA-Z0-9\\$_]*";
		if(Pattern.matches(pattern, newText)) {
			return null;
		}
		else {
			return "Invalid method name";
		}
	}

}
