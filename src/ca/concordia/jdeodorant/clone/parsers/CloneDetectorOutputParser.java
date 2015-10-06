package ca.concordia.jdeodorant.clone.parsers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;

public abstract class CloneDetectorOutputParser {
	
	private final String toolOutputFilePath;
	private final IJavaProject iJavaProject;
	private List<CloneDetectorOutputParserProgressObserver> cloneDetectorOutputParserProgressObservers = 
			new ArrayList<CloneDetectorOutputParserProgressObserver>();
	
	public CloneDetectorOutputParser(IJavaProject iJavaProject, String cloneOutputFilePath) {
		this.toolOutputFilePath = cloneOutputFilePath;
		this.iJavaProject = iJavaProject; 
	}

//	private String formatPath(String path) {
//		path = path.replace("\\", "/");
//		if (!path.endsWith("/"))
//			path += "/";
//		return path;
//	}

	public String getToolOutputFilePath() {
		return toolOutputFilePath;
	}
	
	public IJavaProject getIJavaProject() {
		return this.iJavaProject;
	}
	
	public abstract CloneGroupList readInputFile();
	
	private String readResultsFile(String filePath) {
		try {
			StringBuffer fileData;
			char[] buffer;
			int numRead = 0;
			String readData;

			BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));

			fileData = new StringBuffer(1000);
			buffer = new char[1024];

			while ((numRead = bufferedReader.read(buffer)) != -1) {
				readData = String.valueOf(buffer, 0, numRead);
				fileData.append(readData);
				buffer = new char[1024];
			}

			bufferedReader.close();

			return fileData.toString();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected String getResultsFileContents() {
		return readResultsFile(getToolOutputFilePath());
	}
	
	protected void progress(double percentage) {
		for (CloneDetectorOutputParserProgressObserver observer : cloneDetectorOutputParserProgressObservers)
			observer.notify(percentage);
	}
	
	public void addParserProgressObserver(CloneDetectorOutputParserProgressObserver observer) {
		cloneDetectorOutputParserProgressObservers.add(observer);
	}

	public static IMethod getIMethod(ICompilationUnit iCompilationUnit, CompilationUnit cunit, int begin, int length) {

		IMethod iMethod = null;

		try {
			ASTNode node = NodeFinder.perform(cunit.getRoot(), begin, length, iCompilationUnit);

			if (!(node instanceof MethodDeclaration)) {
				ASTNode parent = node.getParent();
				while (parent != null) {
					if (parent instanceof MethodDeclaration) {
						node = parent;
						break;
					}
					parent = parent.getParent();
				}
			}

			if (node instanceof MethodDeclaration) {
				MethodDeclaration method = (MethodDeclaration) node;
				IJavaElement element;

				try {
					element = iCompilationUnit.getElementAt(method
							.getStartPosition());
					iMethod = (IMethod) element;

				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		} catch (JavaModelException e1) {
			e1.printStackTrace();
		}
		return iMethod;

	}
	
}
