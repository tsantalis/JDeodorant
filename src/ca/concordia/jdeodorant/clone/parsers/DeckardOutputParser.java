package ca.concordia.jdeodorant.clone.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import ca.concordia.jdeodorant.clone.parsers.ResourceInfo.ICompilationUnitNotFoundException;

public class DeckardOutputParser extends CloneDetectorOutputParser {
	private String resultsFile;
	
	public DeckardOutputParser(IJavaProject javaProject, String deckardOutputFilePath) throws InvalidInputFileException {
		super(javaProject, deckardOutputFilePath);
		resultsFile = getResultsFileContents();
		Pattern pattern = Pattern.compile("(?m)^\\s*$");
		Matcher matcher = pattern.matcher(resultsFile);
		int cloneGroupCount = 0;
		while (matcher.find())
			cloneGroupCount++;
		if (cloneGroupCount == 0) {
			resultsFile = null;
			throw new InvalidInputFileException();
		}
		this.setCloneGroupCount(cloneGroupCount);
	}

	@Override
	public CloneGroupList readInputFile() throws InvalidInputFileException {
		
		if (resultsFile == null)
			throw new InvalidInputFileException();
		
		CloneGroupList cloneGroups = new CloneGroupList(getIJavaProject());
		
		int groupID = 0;
		
		boolean inGroup = false;
		int cloneInstanceNumber = 0;
		CloneGroup cloneGroup = null;
		
		Pattern pattern = Pattern.compile("(.*)\r?\n");
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
				}

				String filePath = lineMatcher.group(1);
				try {
					int startLine = Integer.parseInt(lineMatcher.group(2));
					int length = Integer.parseInt(lineMatcher.group(3));
					int endLine = startLine + length - 1;
					CloneInstance cloneInstance = getCloneInstance(filePath, cloneInstanceNumber, false, startLine, 0, endLine, 0);
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
				if (cloneGroup != null && cloneGroup.getCloneGroupSize() > 1)
					cloneGroups.add(cloneGroup);
				inGroup = false;
				progress(groupID);
			}

		}
		
		if (cloneGroups.getCloneGroupsCount() == 0)
			throw new InvalidInputFileException();
		
		return cloneGroups;
	}
}
