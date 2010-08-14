package gr.uom.java.history;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class ProjectEvolution {
	private Map<ProjectVersion, IJavaProject> relevantProjectMap;

	public ProjectEvolution(IJavaProject selectedProject) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = root.getProjects();
		List<IJavaProject> javaProjects = new ArrayList<IJavaProject>();
		for(IProject project : projects) {
			try {
				if(project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
					IJavaProject javaProject = JavaCore.create(project);
					javaProjects.add(javaProject);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		String selectedProjectName = selectedProject.getElementName();
		String projectPrefix = null;
		for(int i=0; i<selectedProjectName.length(); i++) {
			if(Character.isDigit(selectedProjectName.charAt(i))) {
				projectPrefix = selectedProjectName.substring(0, i);
				break;
			}	
		}
		this.relevantProjectMap = new TreeMap<ProjectVersion, IJavaProject>();
		for(IJavaProject javaProject : javaProjects) {
			String javaProjectName = javaProject.getElementName();
			if(javaProjectName.startsWith(projectPrefix)) {
				String version = javaProjectName.substring(projectPrefix.length(), javaProjectName.length());
				ProjectVersion projectVersion = new ProjectVersion(version);
				relevantProjectMap.put(projectVersion, javaProject);
			}
		}
	}
	
	public List<Entry<ProjectVersion, IJavaProject>> getProjectEntries() {
		return new ArrayList<Entry<ProjectVersion, IJavaProject>>(relevantProjectMap.entrySet());
	}
}
