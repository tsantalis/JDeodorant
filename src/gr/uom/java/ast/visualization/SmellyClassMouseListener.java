package gr.uom.java.ast.visualization;

import gr.uom.java.distance.CandidateRefactoring;
import gr.uom.java.distance.MoveMethodCandidateRefactoring;
import java.util.List;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;


public class SmellyClassMouseListener implements MouseListener {

	private PackageMapDiagram diagram;
	private PMClassFigure classFigure;


	public SmellyClassMouseListener(PackageMapDiagram diagram, PMClassFigure classFigure){
		this.diagram = diagram;
		this.classFigure=classFigure;
		classFigure.addMouseListener(this);
	}

	public void mousePressed(MouseEvent me) {
		if(classFigure.isSelected()){
			classFigure.setSelected(false);

			classFigure.setToOriginalState();
		}
		else{
			classFigure.setSelected(true);

			classFigure.setBackgroundColor(classFigure.getOriginalColor());
			classFigure.setBorder(new LineBorder(4));
			classFigure.setToolTip(null);
		}

		List<PMClassFigure> classFigures = diagram.getAllClassFigures();
		List<CandidateRefactoring> candidates = classFigure.getCandidates();


		for(PMClassFigure figure: classFigures){

			//clearing previous decoration
			if(!figure.equals(classFigure)){
				if(figure.isSelected())
					figure.setSelected(false);


				figure.setToOriginalState();
			}

			if(!candidates.isEmpty() && candidates.get(0) instanceof MoveMethodCandidateRefactoring){


				//finding and decorating target classes
				for(CandidateRefactoring candidate: candidates){
					MoveMethodCandidateRefactoring moveCandidate = (MoveMethodCandidateRefactoring) candidate;
					String name = moveCandidate.getTarget();

					if(figure.getName().equals(name)){
						if (classFigure.isSelected()){
							figure.setBackgroundColor(ColorConstants.black);

							if(!figure.isInnerClass()){
								LineBorder border = new LineBorder();
								border.setColor(ColorConstants.black);
								figure.setBorder(border);
							}

						}

					}

				} 
			}

		}
	}

	public void mouseReleased(MouseEvent me) {

	}

	public void mouseDoubleClicked(MouseEvent me) {

	}
}
