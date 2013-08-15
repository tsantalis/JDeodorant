package gr.uom.java.ast.visualization;

import gr.uom.java.distance.CandidateRefactoring;
import gr.uom.java.distance.MoveMethodCandidateRefactoring;
import gr.uom.java.jdeodorant.refactoring.views.FeatureEnvy;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Layer;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.MouseMotionListener;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

public class FeatureEnviedMethodInformationControl extends AbstractInformationControl implements IInformationControlExtension2 {

	private FigureCanvas toolTipCanvas;
	private PMClassFigure classFigure;

	FeatureEnviedMethodInformationControl(Shell parent, ToolBarManager manager) {
		super(parent, manager);

		create();
	}
	public boolean hasContents() {

		//only put tooltip when the classFigure is selected and it contains methods affected by feature envy
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
			MoveMethodCandidateRefactoring moveCandidate = (MoveMethodCandidateRefactoring) candidate;
			String moveMethodName = moveCandidate.getSourceMethod().getMethodObject().getSignature();
			final String targetClassName = moveCandidate.getTarget();
			EntityFigure methodFigure =new EntityFigure(moveMethodName, DecorationConstants.METHOD, false);
			
			methodFigure.addMouseMotionListener(new MouseMotionListener(){

				Color currentColor;
				
				public void mouseDragged(MouseEvent me) {
					// TODO Auto-generated method stub

				}

				public void mouseEntered(MouseEvent me) {
					List<PMClassFigure> classFigures = PackageMapDiagram.allClassFigures;

					for(PMClassFigure figure: classFigures){

						if(figure.getName().equals(targetClassName)){
							currentColor = figure.getBackgroundColor();
							figure.setBackgroundColor(ColorConstants.green);
						}
					}

				}

				public void mouseExited(MouseEvent me) {
					List<PMClassFigure> classFigures = PackageMapDiagram.allClassFigures;

					for(PMClassFigure figure: classFigures){

						if(figure.getName().equals(targetClassName))
							figure.setBackgroundColor(currentColor);

					}

				}

				public void mouseHover(MouseEvent me) {
					// TODO Auto-generated method stub

				}

				public void mouseMoved(MouseEvent me) {
					// TODO Auto-generated method stub

				}

			});
			methodFigure.addMouseListener(new MouseListener(){

				public void mousePressed(MouseEvent me) {
					// TODO Auto-generated method stub
					
					
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					IViewPart viewPart = page.findView("gr.uom.java.jdeodorant.views.FeatureEnvy");
					if(viewPart != null){
						FeatureEnvy featureEnvy = (FeatureEnvy) viewPart;
						featureEnvy.setSelectedLine(candidateRefactoring);
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
