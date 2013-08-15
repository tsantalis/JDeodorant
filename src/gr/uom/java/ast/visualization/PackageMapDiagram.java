package gr.uom.java.ast.visualization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.CompoundBorder;
import org.eclipse.draw2d.FlowLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Layer;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.MouseMotionListener;
import org.eclipse.draw2d.ScalableLayeredPane;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.ASTReader;
import gr.uom.java.distance.CandidateRefactoring;



public class PackageMapDiagram {


	private ScalableLayeredPane root;
	private Layer primary;
	public static String projectName;
	private HashMap<String, List<CandidateRefactoring>> keyMap = new HashMap<String, List<CandidateRefactoring>>();
	public static List<PMClassFigure> allClassFigures= new ArrayList<PMClassFigure>();
	private List<PackageFigure> hasSubPackages = new ArrayList<PackageFigure>();
	private int minDepth= Integer.MAX_VALUE;
	private int max = 0;
	private int min = Integer.MAX_VALUE;
	private PMClassFigure selectedClass;


	public PackageMapDiagram(CandidateRefactoring[] candidateRefactoring, IProgressMonitor monitor){
		allClassFigures = new ArrayList<PMClassFigure>();
		root = new ScalableLayeredPane();
		root.setLayoutManager(new ToolbarLayout());

		primary = new Layer();
		primary.setLayoutManager(new FlowLayout());
		primary.addMouseListener(new MouseListener(){

			public void mousePressed(org.eclipse.draw2d.MouseEvent me) {
				// TODO Auto-generated method stub

			}

			public void mouseReleased(org.eclipse.draw2d.MouseEvent me) {
				// TODO Auto-generated method stub

			}

			public void mouseDoubleClicked(org.eclipse.draw2d.MouseEvent me) {
				for(PMClassFigure figure: allClassFigures){
					if(figure.isSelected())
						figure.setSelected(false);
					figure.setToOriginalState();
				}

			}

		});
		root.setFont(Display.getDefault().getSystemFont());


		//get all Candidates for Refactoring
		for(CandidateRefactoring candidate : candidateRefactoring){
			TypeDeclaration typeDeclaration = candidate.getSourceClassTypeDeclaration();
			ITypeBinding binding= null;
			if(typeDeclaration != null){
				binding = typeDeclaration.resolveBinding();
			}
			if(binding!= null){
				String key = binding.getKey();
				List<CandidateRefactoring> candidateList;

				if(!keyMap.isEmpty()){
					if(keyMap.containsKey(key)){
						candidateList = keyMap.get(key);
						candidateList.add(candidate);

					}else{
						candidateList = new ArrayList<CandidateRefactoring>();
						candidateList.add(candidate);
						keyMap.put(key, candidateList);
					}
				}else{
					candidateList = new ArrayList<CandidateRefactoring>();
					candidateList.add(candidate);
					keyMap.put(key, candidateList);
				}
			}

		}


		IJavaProject javaProject = ASTReader.getExaminedProject();
		projectName = javaProject.getElementName();

		Label label = new Label(projectName);
		label.setLabelAlignment(Label.LEFT);
		root.add(label);
		root.add(primary,"Primary");

		if(monitor != null)
			monitor.beginTask("Parsing selected Java Project", ASTReader.getNumberOfCompilationUnits(javaProject));

		//parses JavaProject
		try {
			IPackageFragment[] allPackageFragments = javaProject.getPackageFragments();

			List<IPackageFragment> packageFragments = new ArrayList<IPackageFragment>();

			for(IPackageFragment packageFragment : allPackageFragments){
				if(packageFragment.getKind() == IPackageFragmentRoot.K_SOURCE){
					packageFragments.add(packageFragment);
				}
			}

			PackageFigure systemFigure = new PackageFigure(javaProject.getElementName(), 1.7);
			ArrayList<PackageFigure> packageFigures = new ArrayList<PackageFigure>(); 

			for(IPackageFragment packageFragment : packageFragments){

				if(packageFragment.hasChildren()){

					PackageFigure packageFigure=null;
					boolean alreadyExists=false;

					//if package name already exists then merge packages
					if(!packageFigures.isEmpty()){	
						for(PackageFigure figure: packageFigures){
							if(figure.getName().equals(packageFragment.getElementName())){
								packageFigure = figure;
								alreadyExists = true;
								break;
							}
						}

					}
					//creates a new package figure
					if(!alreadyExists){
						String packageName;
						if(packageFragment.getElementName().equals(""))
							packageName = "default";
						else
							packageName = packageFragment.getElementName();
						packageFigure = new PackageFigure(packageName);
						int depth = calculateDepth(packageName);
						packageFigure.setDepth(depth);
						if(depth<minDepth)
							minDepth = depth;
					}


					if(packageFragment.hasSubpackages())
						hasSubPackages.add(packageFigure);

					IJavaElement[] children = packageFragment.getChildren();
					for(IJavaElement child: children){
						ICompilationUnit iCompilationUnit = (ICompilationUnit) child;

						ASTInformationGenerator.setCurrentITypeRoot(iCompilationUnit);
						ASTParser parser = ASTParser.newParser(AST.JLS4);
						parser.setKind(ASTParser.K_COMPILATION_UNIT);
						parser.setSource(iCompilationUnit);
						parser.setResolveBindings(true); // we need bindings later on
						CompilationUnit compilationUnit = (CompilationUnit)parser.createAST(null);


						List<AbstractTypeDeclaration> topLevelTypeDeclarations = compilationUnit.types();

						//searches through all the classes of the packages
						for(AbstractTypeDeclaration abstractTypeDeclaration: topLevelTypeDeclarations){
							if(abstractTypeDeclaration instanceof TypeDeclaration){
								TypeDeclaration topLevelTypeDeclaration = (TypeDeclaration) abstractTypeDeclaration;

								//creates new class figure
								PMClassFigure classFigure = createClassFigure(topLevelTypeDeclaration);

								String key = topLevelTypeDeclaration.resolveBinding().getKey();

								//For classes that contain Feature Envy methods
								if(keyMap.containsKey(key)){
									createFeatureEnvyClass(classFigure, key);
								}

								allClassFigures.add(classFigure);
								packageFigure.addToSet(classFigure);

								TypeDeclaration[] types = topLevelTypeDeclaration.getTypes();

								//For inner classes
								for(TypeDeclaration type:types){
									PMClassFigure innerClassFigure = createClassFigure(type);

									innerClassFigure.setOriginalBackgroundColor(new Color(null,233,233,233));
									innerClassFigure.getOriginalBorder().setWidth(4);
									innerClassFigure.setInnerClass(true);
									String innerClassKey = type.resolveBinding().getKey();

									if(keyMap.containsKey(innerClassKey))
										createFeatureEnvyClass(innerClassFigure,innerClassKey);

									allClassFigures.add(innerClassFigure);
									packageFigure.addToSet(innerClassFigure);
								}
							}
						}
						if(monitor != null)
							monitor.worked(1);
					}

					packageFigures.add(packageFigure);


				}
			}

			//Places the subPackages in the appropriate packages
			for(int i = packageFigures.size()-1; i >= 0; i--) {
				PackageFigure currentPackage = packageFigures.get(i);
				Color color = calculateDepthColor(currentPackage.getDepth());
				currentPackage.setBackgroundColor(color);
				LineBorder border = new LineBorder();
				border.setColor(color);

				currentPackage.setBorder(new CompoundBorder(border, new MarginBorder(10, 5, 10, 5)));

				String name;
				PackageFigure parentPackage = null;
				//	boolean isSubPackage=false;

				name = currentPackage.getName();

				if(name.equals("org.apache.tools.ant"))
					System.out.println("");

				for(PackageFigure figure: hasSubPackages){
					PackageFigure p = (PackageFigure) figure;
					if(p!=null && name.contains(p.getName()) && !p.getName().equals(name)){
						parentPackage = p;

					}

				}
				if(parentPackage !=null){
					parentPackage.addToSet(currentPackage);
					packageFigures.set(i, null);
				}

			}

			//adds the remaining packages to the system
			for(PackageFigure p: packageFigures){
				if(p != null)
					systemFigure.addToSet(p);
			}


			PMClassFigure.setMAX_NUM(max);
			PMClassFigure.setMIN_NUM(min);

			systemFigure.draw();
			primary.add(systemFigure);
			if(monitor != null)
				monitor.done();


		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}




	}


	public ScalableLayeredPane getRoot() {
		return root;
	}
	public Color calculateSeverityColor(int numOfMethods){
		Color color= null;
		switch (numOfMethods)
		{ 	case 0:
		case 1: 
			color = new Color(null,255,153,153);
			break;
		case 2:
			color = new Color(null, 255, 102, 102);
			break;
		case 3: 
		case 4:
			color = new Color(null, 255, 0, 0);
			break;
		case 5: 

		case 6:
			color = new Color(null, 204, 0, 0);
			break;

		default:
			color = new Color(null, 185, 0, 0);
			break;

		}
		return color;
	}


	public Color calculateDepthColor(int depth){
		Color color = null;
		int degree=220;

		if (depth>minDepth){
			for(int i = minDepth+1; i<=depth; i++){
				degree -= 25;

			}
		}


		color = new Color(null, degree,degree,degree);

		return color;
	}

	public int calculateDepth(String name){
		int depth=0;
		for(int i= 0; i< name.length(); i ++){
			if (name.charAt(i)== '.')
				depth++;
		}
		return depth;

	}
	public List<PMClassFigure> getAllClassFigures() {
		return allClassFigures;
	}



	public PMClassFigure getSelectedClass() {
		return selectedClass;
	}



	public void setSelectedClass(PMClassFigure selectedClass) {
		this.selectedClass = selectedClass;
	}


	public PMClassFigure createClassFigure(TypeDeclaration topLevelTypeDeclaration){
		int numOfAttributes = topLevelTypeDeclaration.getFields().length;
		int numOfMethods= topLevelTypeDeclaration.getMethods().length;



		if(Math.max(numOfAttributes, numOfMethods)>max){
			max = Math.max(numOfAttributes, numOfMethods);
		}
		if(Math.min(numOfAttributes, numOfMethods)<min){
			min = Math.min(numOfAttributes, numOfMethods);
		}
		String name = topLevelTypeDeclaration.resolveBinding().getQualifiedName();


		final PMClassFigure classFigure = new PMClassFigure(name, numOfAttributes, numOfMethods);
		classFigure.addMouseMotionListener(new MouseMotionListener(){

			public PMClassFigure figure = classFigure;

			public void mouseDragged(
					org.eclipse.draw2d.MouseEvent me) {
				// TODO Auto-generated method stub

			}

			public void mouseEntered(
					org.eclipse.draw2d.MouseEvent me) {
				setSelectedClass(figure);

			}

			public void mouseExited(
					org.eclipse.draw2d.MouseEvent me) {
				setSelectedClass(null);

			}

			public void mouseHover(
					org.eclipse.draw2d.MouseEvent me) {
				// TODO Auto-generated method stub

			}

			public void mouseMoved(
					org.eclipse.draw2d.MouseEvent me) {
				// TODO Auto-generated method stub

			}



		});

		return classFigure;
	}

	public PMClassFigure createFeatureEnvyClass(PMClassFigure classFigure, String key){
		List<CandidateRefactoring> candidates = keyMap.get(key);

		classFigure.setCandidates(candidates);


		int numOfAffectedMethods = candidates.size();

		Color color = calculateSeverityColor(numOfAffectedMethods);

		LineBorder border= (LineBorder)classFigure.getBorder();
		//classFigure.setToolTip(null);
		classFigure.setOriginalBackgroundColor(color);
		border.setColor(classFigure.getBackgroundColor());
		classFigure.setOriginalBorder(border);
		new FeatureEnvyClassMouseListener(this, classFigure);

		return classFigure;
	}








}
