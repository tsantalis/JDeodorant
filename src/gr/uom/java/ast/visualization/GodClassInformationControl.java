package gr.uom.java.ast.visualization;

import gr.uom.java.distance.CandidateRefactoring;
import gr.uom.java.distance.ExtractClassCandidateRefactoring;
import gr.uom.java.jdeodorant.refactoring.views.GodClass;

import java.util.ArrayList;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Layer;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

public class GodClassInformationControl extends AbstractInformationControl
implements IInformationControlExtension2 {

	private FigureCanvas toolTipCanvas;
	private PMClassFigure classFigure;

	public GodClassInformationControl(Shell parentShell, ToolBarManager toolBarManager) {
		super(parentShell, toolBarManager);

		create();
	}

	public boolean hasContents() {
		// TODO Auto-generated method stub
		return (!classFigure.getCandidates().isEmpty() && classFigure.isSelected());
	}

	public void setInput(Object input) {
		setInput((PMClassFigure) input);

	}
	
	private void setInput(PMClassFigure classFigure){
		this.classFigure = classFigure;
		Layer root = new Layer();
		ToolbarLayout layout = new ToolbarLayout();
		layout.setSpacing(5);
		root.setLayoutManager(layout);

		Label className = new Label(classFigure.getName(), DecorationConstants.CLASS);
		className.setFont(DecorationConstants.normalFont);

		root.add(className);

		ArrayList<CandidateRefactoring> candidates = (ArrayList<CandidateRefactoring>) classFigure.getCandidates();

		for(CandidateRefactoring candidate: candidates){
			final CandidateRefactoring candidateRefactoring = candidate;
			ExtractClassCandidateRefactoring moveCandidate = (ExtractClassCandidateRefactoring) candidate;
			String extractClassName = ""+moveCandidate.getTopics();
			EntityFigure methodFigure =new EntityFigure(extractClassName, DecorationConstants.METHOD, false);
			
			
			methodFigure.addMouseListener(new MouseListener(){

				public void mousePressed(MouseEvent me) {
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					IViewPart viewPart = page.findView("gr.uom.java.jdeodorant.views.GodClass");
					if(viewPart != null){
						GodClass godClass = (GodClass) viewPart;
						godClass.setSelectedLine(candidateRefactoring);
					}
				}

				public void mouseReleased(MouseEvent me) {
					// TODO Auto-generated method stub
				}

				public void mouseDoubleClicked(MouseEvent me) {
					// TODO Auto-generated method stub
				}

			});
			root.add(methodFigure);
		}

		toolTipCanvas.setContents(root);
	}

	@Override
	protected void createContent(Composite parent) {
		toolTipCanvas = new FigureCanvas(parent,SWT.DOUBLE_BUFFERED);
		toolTipCanvas.setBackground(ColorConstants.white);

	}
	
	@Override
	public Point computeSizeHint() {
		Rectangle trim = super.computeTrim();
		Point size;
		Point actualSize = toolTipCanvas.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		if(actualSize.y>200)
			size = new Point (actualSize.x, 200);
		else
			size = actualSize;
		size.x += trim.width * 2;
		size.y += trim.height;
		return size;
	}

}
