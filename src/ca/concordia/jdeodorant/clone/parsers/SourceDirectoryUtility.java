package ca.concordia.jdeodorant.clone.parsers;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

public class SourceDirectoryUtility {
	public static Set<String> getAllSourceDirectories(IJavaProject jProject)
			throws JavaModelException {
		/*
		 * We sort the src paths by their character lengths in a non-increasing
		 * order, because we are going to see whether a Java file's path starts
		 * with a specific source path For example, if the Java file's path is
		 * "src/main/org/blah/blah", the "src/main" is considered the source
		 * path not "src/"
		 */
		Set<String> allSrcDirectories = new TreeSet<String>(new Comparator<String>() {
			public int compare(String o1, String o2) {
				if (o1.equals(o2))
					return 0;

				if (o1.length() == o2.length())
					return 1;

				return -Integer.compare(o1.length(), o2.length());
			}
		});

		IClasspathEntry[] classpathEntries = jProject
				.getResolvedClasspath(true);

		for (int i = 0; i < classpathEntries.length; i++) {
			IClasspathEntry entry = classpathEntries[i];
			if (entry.getContentKind() == IPackageFragmentRoot.K_SOURCE) {
				IPath path = entry.getPath();
				if (path.toString().equals(
						"/" + jProject.getProject().getName()))
					allSrcDirectories.add(path.toString());
				else if (path.toString().length() > jProject.getProject()
						.getName().length() + 2) {
					String srcDirectory = path.toString().substring(
							jProject.getProject().getName().length() + 2);
					allSrcDirectories.add(srcDirectory);
				}
			}
		}
		return allSrcDirectories;
	}
}
