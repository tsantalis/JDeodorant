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
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;

import ca.concordia.jdeodorant.clone.parsers.ResourceInfo.ICompilationUnitNotFoundException;

public abstract class CloneDetectorOutputParser {
	
	private final String toolOutputFilePath;
	private final IJavaProject iJavaProject;
	private List<CloneDetectorOutputParserProgressObserver> cloneDetectorOutputParserProgressObservers = 
			new ArrayList<CloneDetectorOutputParserProgressObserver>();
	private boolean operationCanceled;
	private List<Throwable> exceptions = new ArrayList<Throwable>();
	private int cloneGroupCount;
	
	public CloneDetectorOutputParser(IJavaProject iJavaProject, String cloneOutputFilePath) throws InvalidInputFileException {
		this.toolOutputFilePath = cloneOutputFilePath;
		this.iJavaProject = iJavaProject; 
	}

	public String getToolOutputFilePath() {
		return toolOutputFilePath;
	}
	
	public IJavaProject getIJavaProject() {
		return this.iJavaProject;
	}
	
	public int getCloneGroupCount() {
		return this.cloneGroupCount;
	}
	
	protected void setCloneGroupCount(int cloneGroupCount) {
		this.cloneGroupCount = cloneGroupCount;
	}
	
	public abstract CloneGroupList readInputFile() throws InvalidInputFileException;
	
	public String readFileContents(String filePath) {
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
		return readFileContents(getToolOutputFilePath());
	}
	
	protected void progress(int cloneGroupIndex) {
		for (CloneDetectorOutputParserProgressObserver observer : cloneDetectorOutputParserProgressObservers)
			observer.notify(cloneGroupIndex);
	}
	
	public void addParserProgressObserver(CloneDetectorOutputParserProgressObserver observer) {
		cloneDetectorOutputParserProgressObservers.add(observer);
	}

	public static IMethod getIMethod(ICompilationUnit iCompilationUnit, CompilationUnit cunit, int begin, int length) {

		IMethod iMethod = null;

		try {
			ASTNode node = NodeFinder.perform(cunit.getRoot(), begin, length, iCompilationUnit);
			
			if (node == null)
				return null;
			
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
	
	public static String getMethodJavaSignature(IMethod iMethod) {

		StringBuilder toReturn = new StringBuilder();

		try {
			toReturn.append(Signature.toString(iMethod.getReturnType()));
		} catch (IllegalArgumentException e) {

		} catch (JavaModelException e) {
			
		}
		toReturn.append(" ");
		toReturn.append(iMethod.getElementName());
		toReturn.append("(");

		String comma = "";
		for (String type : iMethod .getParameterTypes()) {
			toReturn.append(comma);
			comma = ", ";
			toReturn.append(Signature.toString(type));
		}
		toReturn.append(")");

		return toReturn.toString();
	}


	public void cancelOperation() {
		this.operationCanceled = true;
	}

	public boolean isOperationCanceled() {
		return operationCanceled;
	}

	protected void addExceptionHappenedDuringParsing(Throwable ex) {
		exceptions.add(new CloneDetectorOutputParseException(ex));
	}

	public List<Throwable> getWarningExceptions() {
		return new ArrayList<Throwable>(this.exceptions);
	}
	
	protected CloneInstance getCloneInstance(String filePath, int cloneInstanceID, boolean isAbsoluteFilePath, 
			int startLine, int startColumn, int endLine, int endColumn) 
			throws JavaModelException, ICompilationUnitNotFoundException {
		ResourceInfo resourceInfo = ResourceInfo.getResourceInfo(this.getIJavaProject(), filePath, isAbsoluteFilePath);
		CloneInstanceLocationInfo locationInfo = new CloneInstanceLocationInfo(resourceInfo.getFullPath(), startLine, startColumn, endLine, endColumn);
		CloneInstance cloneInstance = getCloneInstance(cloneInstanceID, resourceInfo, locationInfo);
		return cloneInstance;
	}
	
	protected CloneInstance getCloneInstance(String filePath, int cloneInstanceIndex, boolean isAbsoluteFilePath, 
			int startOffset, int endOffset) 
			throws JavaModelException, ICompilationUnitNotFoundException {
		ResourceInfo resourceInfo = ResourceInfo.getResourceInfo(this.getIJavaProject(), filePath, isAbsoluteFilePath);
		CloneInstanceLocationInfo locationInfo = new CloneInstanceLocationInfo(resourceInfo.getFullPath(), startOffset, endOffset);
		CloneInstance cloneInstance = getCloneInstance(cloneInstanceIndex, resourceInfo, locationInfo);
		return cloneInstance;
	}

	private CloneInstance getCloneInstance(int cloneInstanceIndex, ResourceInfo resourceInfo, CloneInstanceLocationInfo locationInfo) {
		CloneInstance cloneInstance = new CloneInstance(locationInfo, cloneInstanceIndex);
		cloneInstance.setSourceFolder(resourceInfo.getSourceFolder());
		cloneInstance.setPackageName(resourceInfo.getPackageName());
		cloneInstance.setClassName(resourceInfo.getClassName());
		IMethod iMethod = getIMethod(resourceInfo.getICompilationUnit(), resourceInfo.getCompilationUnit(), 
				locationInfo.getStartOffset(), locationInfo.getLength());
		if (iMethod != null) {
			cloneInstance.setMethodName(iMethod.getElementName());
			try {
				cloneInstance.setIMethodSignature(iMethod.getSignature());
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
			cloneInstance.setMethodSignature(getMethodJavaSignature(iMethod));
			IJavaElement parent = iMethod.getParent();
			if (parent instanceof IType) {
				cloneInstance.setContainingClassFullyQualifiedName(((IType)parent).getFullyQualifiedName());
			}
		}
		return cloneInstance;
	}
	
	protected static String formatPath(String cloneDROutputFilePath) {
		cloneDROutputFilePath = cloneDROutputFilePath.replace("\\", "/");
		if (!cloneDROutputFilePath.endsWith("/"))
			cloneDROutputFilePath += "/";
		return cloneDROutputFilePath;
	}
	
}
