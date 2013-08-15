package gr.uom.java.ast.visualization;

import org.eclipse.jface.text.IInformationControlCreator;

/**
 * {@link IInformationControlCreator} for custom information inputs.
 */
public interface ICustomInformationControlCreator extends IInformationControlCreator {

	/**
	 * Returns whether the given information input is supported by this control creator or not.
	 * @param info the information input
	 * @return <code>true</code> if the given information input is supported, <code>false</code> otherwise
	 */
	boolean isSupported(Object info);
}
