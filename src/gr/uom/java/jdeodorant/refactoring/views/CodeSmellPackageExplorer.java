package gr.uom.java.jdeodorant.refactoring.views;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import gr.uom.java.ast.visualization.DecorationConstants;
import gr.uom.java.ast.visualization.GodClassInformationControlCreator;
import gr.uom.java.ast.visualization.ICustomInformationControlCreator;
import gr.uom.java.ast.visualization.IInformationProvider;
import gr.uom.java.ast.visualization.FeatureEnviedMethodInformationControlCreator;
import gr.uom.java.ast.visualization.InformationControlManager;
import gr.uom.java.ast.visualization.SearchInputAction;
import gr.uom.java.ast.visualization.ZoomInputAction;
import gr.uom.java.ast.visualization.PackageMapDiagramInformationProvider;
import gr.uom.java.ast.visualization.PackageMapDiagram;
import gr.uom.java.ast.visualization.ZoomAction;
import gr.uom.java.distance.CandidateRefactoring;
import gr.uom.java.jdeodorant.refactoring.Activator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.ScalableLayeredPane;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IProgressService;

public class CodeSmellPackageExplorer extends ViewPart {
	public static final String ID = "gr.uom.java.jdeodorant.views.CodeSmellPackageExplorer";
	private FigureCanvas figureCanvas; 
	private ScalableLayeredPane root = null;
	private boolean ctrlPressed= false;
	public static double SCALE_FACTOR=1;
	private PackageMapDiagram diagram;
	protected static CodeSmellType CODE_SMELL_TYPE;

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		figureCanvas = new FigureCanvas(parent, SWT.DOUBLE_BUFFERED);
		figureCanvas.setBackground(ColorConstants.white);
		//preinitializing DecorationConstants
		Image im = DecorationConstants.PACKAGE;
		try {
			IWorkbench wb = PlatformUI.getWorkbench();
			IProgressService ps = wb.getProgressService();
			ps.busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					CandidateRefactoring[] candidates = CodeSmellVisualizationDataSingleton.getCandidates();
					if(candidates != null){
						diagram = new PackageMapDiagram(candidates, monitor);
						root = diagram.getRoot();
					}
				}
			});
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//figureCanvas.setViewport(new FreeformViewport());
		figureCanvas.addKeyListener( new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == SWT.CTRL){
					ctrlPressed = true;
				}
			}
			public void keyReleased(KeyEvent e) {
				if(e.keyCode== SWT.CTRL)
					ctrlPressed = false;
			}
		});
		MouseWheelListener listener = new MouseWheelListener() {
			private double scale;
			private static final double ZOOM_INCRENENT = 0.1;
			private static final double ZOOM_DECREMENT = 0.1;

			private void zoom(int count, Point point) {
				if (count > 0) {
					scale += ZOOM_INCRENENT;

				} else {
					scale -= ZOOM_DECREMENT;
				}

				if (scale <= 0) {
					scale = 0;
				}
				Viewport viewport = (Viewport) root.getParent();

				if(scale>1){
					viewport.setHorizontalLocation((int) (point.x*(scale -1)+ scale*viewport.getLocation().x));
					viewport.setVerticalLocation((int) (point.y*(scale-1)+scale*viewport.getLocation().y));
				}

				SCALE_FACTOR=scale;
				root.setScale(scale);
			}

			public void mouseScrolled(MouseEvent e) {
				if(ctrlPressed == true){
					scale = root.getScale();
					Point point = new Point(e.x,e.y);
					int count = e.count;
					zoom(count, point);

				}
			}
		};

		figureCanvas.addMouseWheelListener(listener);
		figureCanvas.setContents(root);
		if(diagram != null)
			hookTooltips(diagram);

		ImageDescriptor imageDescriptor = Activator.getImageDescriptor("/icons/" + "magnifier.png");
		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager manager = bars.getToolBarManager();

		if(diagram != null && diagram.getProjectName() != null){
			LabelControlContribution infoLabel= null;
			if(CODE_SMELL_TYPE != null) {
				if(CODE_SMELL_TYPE.equals(CodeSmellType.FEATURE_ENVY))
					infoLabel = new LabelControlContribution("Label", "Feature Envy Analysis of system: ", null);
				else if (CODE_SMELL_TYPE.equals(CodeSmellType.GOD_CLASS))
					infoLabel = new LabelControlContribution("Label", "God Class Analysis of system: ", null);
			}

			LabelControlContribution projectNameLabel = new LabelControlContribution("Label", diagram.getProjectName() +"  ");

			if(infoLabel != null)
				manager.add(infoLabel);
			manager.add(projectNameLabel);
			manager.add(new Separator());
		}

		Action act=new Action("Zoom",SWT.DROP_DOWN){};
		act.setImageDescriptor(imageDescriptor);
		act.setMenuCreator(new MyMenuCreator());
		manager.add(act);

		SearchInputAction searchAction = new SearchInputAction();
		searchAction.setText("Search");
		manager.add(searchAction);

	}

	class MyMenuCreator implements IMenuCreator{
		private IAction action;
		private Menu menu;

		public void selectionChanged(IAction action, ISelection selection)
		{
			if (action != this.action)
			{
				action.setMenuCreator(this);
				this.action = action;
			}
		} 

		public Menu getMenu(Control ctrl){
			Menu menu = new Menu(ctrl);
			addActionToMenu(menu, newZoomAction(0.5));
			addActionToMenu(menu, newZoomAction(1));
			addActionToMenu(menu, newZoomAction(2));
			//	addActionToMenu(menu, newZoomAction(0));

			ZoomInputAction inputZoomAction = new ZoomInputAction(root);
			inputZoomAction.setText("Other...");

			addActionToMenu(menu, inputZoomAction);
			return menu;
		}

		public void dispose() {
			if (menu != null)
			{
				menu.dispose();
			}
		}

		public Menu getMenu(Menu parent) {
			return null;
		}

		private void addActionToMenu(Menu menu, IAction action)
		{
			ActionContributionItem item= new ActionContributionItem(action);
			item.fill(menu, -1);
		}
	}

	public ZoomAction newZoomAction(double scale){
		ZoomAction zoomAction = new ZoomAction(root, scale);
		if(scale != 0){
			double percent = scale*100;
			zoomAction.setText((int) percent +"%");
			zoomAction.setImageDescriptor(Activator.getImageDescriptor("/icons/" + "magnifier.png"));
		}else
			zoomAction.setText("Scale to Fit");
		return zoomAction;
	}
	@Override
	public void setFocus() {
		
	}

	private void hookTooltips(PackageMapDiagram diagram) {
		// Create an information provider for our table viewer
		IInformationProvider informationProvider = new PackageMapDiagramInformationProvider(diagram);
		List<ICustomInformationControlCreator> informationControlCreators = new ArrayList<ICustomInformationControlCreator>();
		if(CODE_SMELL_TYPE != null) {
			if(CODE_SMELL_TYPE.equals(CodeSmellType.FEATURE_ENVY))
				informationControlCreators.add(new FeatureEnviedMethodInformationControlCreator());
			else if(CODE_SMELL_TYPE.equals(CodeSmellType.GOD_CLASS))
				informationControlCreators.add(new GodClassInformationControlCreator());
		}
		Control control =figureCanvas;
		final InformationControlManager informationControlManager = new InformationControlManager(informationProvider, informationControlCreators, false);
		informationControlManager.install(control);

		// MouseListener to show the information when the user hovers a table item
		control.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseHover(MouseEvent event) {
				informationControlManager.showInformation();
			}
		});

		// DisposeListener to uninstall the information control manager

		DisposeListener listener = new DisposeListener(){
			public void widgetDisposed(DisposeEvent e) {
				informationControlManager.dispose();
			}
		};
		control.addDisposeListener(listener);
		// Install tooltips
		//Tooltips.install(diagram.getControl(), informationProvider, informationControlCreators, false);
	}

	protected enum CodeSmellType{
		FEATURE_ENVY, GOD_CLASS;
	}
}
