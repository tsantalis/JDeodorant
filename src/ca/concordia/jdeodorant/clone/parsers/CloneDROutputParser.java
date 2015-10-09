package ca.concordia.jdeodorant.clone.parsers;

import java.io.File;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import ca.concordia.jdeodorant.clone.parsers.ResourceInfo.ICompilationUnitNotFoundException;


public class CloneDROutputParser extends CloneDetectorOutputParser {

	private final Set<Integer> allCloneGroupIDs;

	public CloneDROutputParser(IJavaProject iJavaProject, String cloneDROutputFilePath) {
		super(iJavaProject, formatPath(cloneDROutputFilePath));
		this.allCloneGroupIDs = getAllCloneGroups(this.getToolOutputFilePath());
		this.setCloneGroupCount(this.allCloneGroupIDs.size());
	}

	private static String formatPath(String cloneDROutputFilePath) {
		cloneDROutputFilePath = Path.forPosix(cloneDROutputFilePath).toOSString();
		if (!cloneDROutputFilePath.endsWith(File.separator))
			cloneDROutputFilePath += File.separator;
		return cloneDROutputFilePath;
	}

	@Override
	public CloneGroupList readInputFile() throws CloneDetectorOutputParseException {

		CloneGroupList cloneGroups = new CloneGroupList();
		
		for (Integer cloneGroupID : this.allCloneGroupIDs) {
			
			if (isOperationCanceled())
				break;

			String filePath = this.getToolOutputFilePath() + "xCloneSet" + cloneGroupID + ".html";

			// There will be one clone group for each file
			CloneGroup cloneGroup = new CloneGroup(cloneGroupID);

			String fileContents = readFileContents(filePath);
			Pattern pattern = Pattern.compile("<a id=\\\"CloneInstance\\d+\\\">.*<br/>(\\d+)</a><td>Line Count<br/>(\\d+)</td><td>Source Line<br/>(\\d+).*Source File</div><pre>(.*)</pre>");
			Matcher cloneMatcher = pattern.matcher(fileContents);
			int cloneCount = 0;
			while (cloneMatcher.find()) {
				try {
					int cloneLineCount = Integer.parseInt(cloneMatcher.group(2));
					int startLine = Integer.parseInt(cloneMatcher.group(3));
					int endLine = startLine + cloneLineCount - 1;
					String cloneFilePath = cloneMatcher.group(4);
					cloneCount++;
					CloneInstance cloneInstance = getCloneInstance(cloneFilePath, cloneCount, true, startLine, 0, endLine, 0);
					cloneGroup.addClone(cloneInstance);
				} catch (NullPointerException npex) {
					addExceptionHappenedDuringParsing(npex);
				} catch (NumberFormatException nfex) {
					addExceptionHappenedDuringParsing(nfex);
				} catch (JavaModelException jme) {
					addExceptionHappenedDuringParsing(jme);
				} catch (ICompilationUnitNotFoundException infe) {
					addExceptionHappenedDuringParsing(infe);
				}
			}
			if (cloneGroup.getCloneGroupSize() > 0)
				cloneGroups.add(cloneGroup);
			progress(cloneGroupID);
		}
		
		return cloneGroups;
	}

	private Set<Integer> getAllCloneGroups(String pathToFiles) {

		File f = new File(pathToFiles);
		File[] files = f.listFiles();

		Set<Integer> toReturn = new TreeSet<Integer>(new Comparator<Integer>() {
			public int compare(Integer o1, Integer o2) {
				return Integer.compare(o1, o2);
			}
		});
		for (File file : files) {
			Pattern pattern = Pattern.compile("xCloneSet(\\d+).html");
			Matcher matcher = pattern.matcher(file.getName());
			if (matcher.find()) {
				toReturn.add(Integer.parseInt(matcher.group(1)));
			}
		}

		return toReturn;
	}

}
