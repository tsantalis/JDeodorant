package gr.uom.java.jdeodorant.refactoring.views;

import java.lang.reflect.Field;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.TextStyle;

/*
 * This class provides the ability to append Stylers on top of one another
 * and also is used as the Styler class for StyledStringVisitor as it appends
 * text to its StyledString object.
 * 
 */
public class StyledStringStyler extends Styler {

	private TextStyle textStyleAttributeStyle = new TextStyle();
	
	public StyledStringStyler() { 
	}
	public StyledStringStyler(TextStyle textStyle){
		textStyleAttributeStyle.font = textStyle.font;
		textStyleAttributeStyle.background = textStyle.background;
		textStyleAttributeStyle.borderColor = textStyle.borderColor;
		textStyleAttributeStyle.borderStyle = textStyle.borderStyle;
		textStyleAttributeStyle.foreground = textStyle.foreground;
		textStyleAttributeStyle.metrics = textStyle.metrics;
		textStyleAttributeStyle.rise = textStyle.rise;
		textStyleAttributeStyle.strikeout = textStyle.strikeout;
		textStyleAttributeStyle.strikeoutColor = textStyle.strikeoutColor;
		textStyleAttributeStyle.underline = textStyle.underline;
		textStyleAttributeStyle.underlineColor = textStyle.underlineColor;
		textStyleAttributeStyle.underlineStyle = textStyle.underlineStyle;
	}
	/*
	 * When appending TextStyles, the TextStyle which changes the font should always be appended last. This is because the new font always 
	 * overrides the old font - unless the new font is null. However, for other attributes, the new attribute only overrides the old attribute
	 * if the old attribute is set to "null" or its default setting ("0" or "false").
	 */
	public void appendTextStyle(TextStyle textStyle){
		//If new font is not null, always replace the old one with it
		if (textStyle.font != null){
			textStyleAttributeStyle.font = textStyle.font;
		}
		if (textStyleAttributeStyle.background == null){
			textStyleAttributeStyle.background = textStyle.background;
		}
		if (textStyleAttributeStyle.borderColor == null){
			textStyleAttributeStyle.borderColor = textStyle.borderColor;
		}
		if (textStyleAttributeStyle.borderStyle == 0){
			textStyleAttributeStyle.borderStyle = textStyle.borderStyle;
		}
		if (textStyleAttributeStyle.data == null){
			textStyleAttributeStyle.data = textStyle.data;
		}
		if (textStyleAttributeStyle.foreground == null){
			textStyleAttributeStyle.foreground = textStyle.foreground;
		}
		if (textStyleAttributeStyle.metrics == null){
			textStyleAttributeStyle.metrics = textStyle.metrics;
		}
		if (textStyleAttributeStyle.rise == 0){
			textStyleAttributeStyle.rise = textStyle.rise;
		}
		if (textStyleAttributeStyle.strikeout == false){
			textStyleAttributeStyle.strikeout = textStyle.strikeout;
		}
		if (textStyleAttributeStyle.strikeoutColor == null){
			textStyleAttributeStyle.strikeoutColor = textStyle.strikeoutColor;
		}
		if (textStyleAttributeStyle.underline == false){
			textStyleAttributeStyle.underline = textStyle.underline;
		}
		if (textStyleAttributeStyle.underlineColor == null){
			textStyleAttributeStyle.underlineColor = textStyle.underlineColor;
		}
		if (textStyleAttributeStyle.underlineStyle == 0){
			textStyleAttributeStyle.underlineStyle = textStyle.underlineStyle;
		}

		/*
		Field[] textStyleFields = TextStyle.class.getFields();
		for (Field field : textStyleFields){
			Object currentField;
			try {
				//CurrentField is the value of the attribute textStyleAttributeStyle for the given field. If it is null, we will allow it to be overridden by the value
				//of the corresponding field of the parameter. 
				currentField = field.get(textStyleAttributeStyle);
				//Check either if the field is null, a boolean that is false, or a primitive that is zero - all of which would mean we override.
				if ((currentField == null || (field.getType() == boolean.class && ((Boolean.FALSE.equals(field.get(currentField))))) || (field.getType().isPrimitive() && ((Number) currentField).doubleValue() == 0))){
					try {
						field.set(textStyleAttributeStyle, field.get(textStyle));
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			} catch (IllegalArgumentException e1) {
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			}
		}
		*/
	}
	
	public void applyStyles(TextStyle textStyle) {
		Field[] textStyleFields = TextStyle.class.getFields();
		for (Field field : textStyleFields){
			try {
				field.set(textStyle, field.get(textStyleAttributeStyle));
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}


}
