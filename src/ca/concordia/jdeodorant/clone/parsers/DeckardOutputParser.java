package ca.concordia.jdeodorant.clone.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import ca.concordia.jdeodorant.clone.parsers.ResourceInfo.ICompilationUnitNotFoundException;

public class DeckardOutputParser extends CloneDetectorOutputParser {
	private String resultsFile;
	private int cloneGroupCount;
	public DeckardOutputParser(IJavaProject javaProject, String deckardOutputFilePath) {
		super(javaProject, deckardOutputFilePath);
		resultsFile = getResultsFileContents();
		Pattern pattern = Pattern.compile("(?m)^\\s*$");
		Matcher matcher = pattern.matcher(resultsFile);
		while (matcher.find())
			cloneGroupCount++;
	}

	@Override
	public CloneGroupList readInputFile() throws CloneDetectorOutputParseException {
		CloneGroupList cloneGroups = new CloneGroupList();
		
		int groupID = 0;
		
		boolean inGroup = false;
		int cloneInstanceNumber = 0;
		CloneGroup cloneGroup = null;
		
		Pattern pattern = Pattern.compile("(.*)\n");
		Matcher matcher = pattern.matcher(resultsFile);

		while (matcher.find()) {
			
			if (isOperationCanceled())
				return cloneGroups;

			String strLine = matcher.group(1);

			String lookingFor = "[0-9]+\\sdist:\\d+\\.\\d+\\sFILE\\s([[\\w\\s\\.-]+/]+[\\w\\s\\.-]+)\\sLINE:([0-9]+):([0-9]+)\\s.*";

			Pattern linePattern = Pattern.compile(lookingFor);
			Matcher lineMatcher = linePattern.matcher(strLine);

			if (lineMatcher.find()) {

				if (!inGroup) {
					inGroup = true;
					groupID++;
					cloneInstanceNumber = 1;
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
					CloneInstance cloneInstance = new CloneInstance(locationInfo, cloneGroup, cloneInstanceNumber);
					cloneInstance.setSourceFolder(resourceInfo.getSourceFolder());
					cloneInstance.setPackageName(resourceInfo.getPackageName());
					cloneInstance.setClassName(resourceInfo.getClassName());
					IMethod iMethod = getIMethod(resourceInfo.getICompilationUnit(), resourceInfo.getCompilationUnit(), 
							locationInfo.getStartOffset(), locationInfo.getLength());
					cloneInstance.setIMethod(iMethod);
					cloneInstanceNumber++;

					cloneGroup.addClone(cloneInstance);
				} catch(NumberFormatException ex) {
					addExceptionHappenedDuringParsing(ex);
				} catch (JavaModelException ex) {
					addExceptionHappenedDuringParsing(ex);
				} catch (ICompilationUnitNotFoundException ex) {
					addExceptionHappenedDuringParsing(ex);
				}
			} else {
				inGroup = false;
				progress(groupID);
			}

		}
		
		return cloneGroups;
	}

	@Override
	public int getCloneGroupCount() {
		return cloneGroupCount;
	}
}
