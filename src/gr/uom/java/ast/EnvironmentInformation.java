package gr.uom.java.ast;

public interface EnvironmentInformation {
	public String[] getClasspathEntries();
	public String[] getSourcepathEntries();
	public String[] getJavaFilePaths();
}
