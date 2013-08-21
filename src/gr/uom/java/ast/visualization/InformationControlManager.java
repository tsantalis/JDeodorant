package gr.uom.java.ast.visualization;

import java.util.List;
import org.eclipse.jface.text.AbstractInformationControlManager;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * Information control manager.
 * Refactored from {@link org.eclipse.jface.text.information.InformationPresenter}
 * for general usage instead of {@link ITextViewer}.
 */
public class InformationControlManager extends AbstractInformationControlManager {

	/**
	 * Internal information control closer. Listens to several events issued by its subject control
	 * and closes the information control when necessary.
	 */
	class InformationControlCloser implements IInformationControlCloser, ControlListener, MouseListener, FocusListener, KeyListener {

		/** The subject control. */
		private Control subjectControl;
		/** The information control. */
		private IInformationControl informationControlToClose;
		/** Indicates whether this closer is active. */
		private boolean isActive = false;

		
		public void setSubjectControl(Control control) {
			subjectControl = control;
		}

		
		public void setInformationControl(IInformationControl control) {
			informationControlToClose = control;
		}

		
		public void start(Rectangle informationArea) {
			if (isActive) {
				return;
			}
			isActive = true;

			if (subjectControl != null && !subjectControl.isDisposed()) {
				subjectControl.addControlListener(this);
				subjectControl.addMouseListener(this);
				subjectControl.addFocusListener(this);
				subjectControl.addKeyListener(this);
			}

			if (informationControlToClose != null) {
				informationControlToClose.addFocusListener(this);
			}
		}

		
		public void stop() {
			if (!isActive) {
				return;
			}
			isActive = false;

			if (informationControlToClose != null) {
				informationControlToClose.removeFocusListener(this);
			}

			if (subjectControl != null && !subjectControl.isDisposed()) {
				subjectControl.removeControlListener(this);
				subjectControl.removeMouseListener(this);
				subjectControl.removeFocusListener(this);
				subjectControl.removeKeyListener(this);
			}
		}

	
		public void controlResized(ControlEvent e) {
			 hideInformationControl();
		}

		
		public void controlMoved(ControlEvent e) {
			 hideInformationControl();
		}

		
		public void mouseDown(MouseEvent e) {
			 hideInformationControl();
		}

		
		public void mouseUp(MouseEvent e) {
			// nothing to do
		}

		
		public void mouseDoubleClick(MouseEvent e) {
			hideInformationControl();
		}

		
		public void focusGained(FocusEvent e) {
			// nothing to do
		}

	
		public void focusLost(FocusEvent e) {
			Display d = subjectControl.getDisplay();
			d.asyncExec(new Runnable() {
				// Without the asyncExec, mouse clicks to the workbench window are swallowed.
				
				public void run() {
					if (informationControlToClose == null || !informationControlToClose.isFocusControl()) {
						hideInformationControl();
					}
				}
			});
		}

	
		public void keyPressed(KeyEvent e) {
			hideInformationControl();
		}

		
		public void keyReleased(KeyEvent e) {
			// nothing to do
		}


		public void mouseDragged(org.eclipse.draw2d.MouseEvent me) {
			// TODO Auto-generated method stub
			
		}


		
	}

	private static final IInformationControlCreator DEFAULT_INFORMATION_CONTROL_CREATOR = new FeatureEnviedMethodInformationControlCreator();

	private final IInformationProvider informationProvider;
	private final List<ICustomInformationControlCreator> customControlCreators;


	/**
	 * Creates a new information control manager that uses the given information provider and control creators.
	 * The manager is not installed on any control yet. By default, an information
	 * control closer is set that closes the information control in the event of key strokes,
	 * resizing, moves, focus changes, mouse clicks, and disposal - all of those applied to
	 * the information control's parent control. Optionally, the setup ensures that the information
	 * control when made visible will request the focus.
	 *
	 * @param informationProvider the information provider to be used
	 * @param customControlCreators the control creators to be used
	 * @param takeFocusWhenVisible set to <code>true</code> if the information control should take focus when made visible
	 */
	public InformationControlManager(IInformationProvider informationProvider, List<ICustomInformationControlCreator> customControlCreators, boolean takeFocusWhenVisible) {
		super(DEFAULT_INFORMATION_CONTROL_CREATOR);
		this.informationProvider = informationProvider;
		this.customControlCreators = customControlCreators;

		setCloser(new InformationControlCloser());
		takesFocusWhenVisible(takeFocusWhenVisible);
	}

	@Override
	protected void computeInformation() {
		Display display = getSubjectControl().getDisplay();
		Point mouseLocation = display.getCursorLocation();
		mouseLocation = getSubjectControl().toControl(mouseLocation);

		// Compute information input
		Object info = informationProvider.getInformation(mouseLocation);

		// Find an information control creator for the computed information input
		IInformationControlCreator customControlCreator = null;
		for (ICustomInformationControlCreator controlCreator : customControlCreators) {
			if (controlCreator.isSupported(info)) {
				customControlCreator = controlCreator;
				break;
			}
		}
		setCustomInformationControlCreator(customControlCreator);

		// Convert to String for default TextLabelInformationControl
		// (Fallback, if no custom control creator has been found)
		//if (info != null && customControlCreator == null) {
		//	info = info.toString();
		//}

		// Trigger the presentation of the computed information
		if(customControlCreator != null){
		Rectangle area = informationProvider.getArea(mouseLocation);
		setInformation(info, area);
		}
	}

	@Override
	protected Point computeLocation(Rectangle subjectArea, Point controlSize, Anchor anchor) {
		Point location = super.computeLocation(subjectArea, controlSize, anchor);
		location.x += 20;
		
		return location;
	}
}