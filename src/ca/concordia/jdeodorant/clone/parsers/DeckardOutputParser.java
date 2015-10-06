package ca.concordia.jdeodorant.clone.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;

public class DeckardOutputParser extends CloneDetectorOutputParser {

	public DeckardOutputParser(IJavaProject javaProject, String deckardOutputFilePath) {
		super(javaProject, deckardOutputFilePath);
	}

	@Override
	public CloneGroupList readInputFile() {

		CloneGroupList cloneGroups = new CloneGroupList();

		String resultsFile = getResultsFileContents();
		
		int groupID = 0;
		int cloneCount = 0;
		
		Pattern pattern = Pattern.compile("(?m)^\\s*$");
		Matcher matcher = pattern.matcher(resultsFile);
		while (matcher.find())
			cloneCount++;
		
		boolean inGroup = false;
		int cloneInstancNumber = 0;
		CloneGroup cloneGroup = null;
		
		pattern = Pattern.compile("(.*)\n");
		matcher = pattern.matcher(resultsFile);

		while (matcher.find()) {

			String strLine = matcher.group(1);

			String lookingFor = "[0-9]+\\sdist:\\d+\\.\\d+\\sFILE\\s([[\\w\\s\\.-]+/]+[\\w\\s\\.-]+)\\sLINE:([0-9]+):([0-9]+)\\s.*";

			Pattern linePattern = Pattern.compile(lookingFor);
			Matcher lineMatcher = linePattern.matcher(strLine);

			if (lineMatcher.find()) {

				if (!inGroup) {
					inGroup = true;
					groupID++;
					cloneInstancNumber = 1;
					cloneGroup = new CloneGroup(groupID);
					cloneGroups.add(cloneGroup);
				}

				String filePath = lineMatcher.group(1);
				try {
					ResourceInfo resourceInfo = ResourceInfo.getResourceInfo(this.getIJavaProject(), filePath);
					int startLine = Integer.parseInt(lineMatcher.group(2));
					int length = Integer.parseInt(lineMatcher.group(3));
					int endLine = startLine + length - 1;

					CloneInstanceLocationInfo locationInfo = new CloneInstanceLocationInfo(resourceInfo.getFullPath(), startLine, 0, endLine, 0);
					CloneInstance cloneInstance = new CloneInstance(locationInfo, cloneGroup, cloneInstancNumber);
					cloneInstance.setSourceFolder(resourceInfo.getSourceFolder());
					cloneInstance.setPackageName(resourceInfo.getPackageName());
					cloneInstance.setClassName(resourceInfo.getClassName());
					IMethod iMethod = getIMethod(resourceInfo.getICompilationUnit(), resourceInfo.getCompilationUnit(), 
							locationInfo.getStartOffset(), locationInfo.getLength());
					cloneInstance.setIMethod(iMethod);
					cloneInstancNumber++;

					cloneGroup.addClone(cloneInstance);
				} catch(Exception ex) {
					ex.printStackTrace();
				}
			} else {
				inGroup = false;
				progress(groupID / (double)cloneCount);
			}

		}
		
		return cloneGroups;
	}
}
