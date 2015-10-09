package ca.concordia.jdeodorant.clone.parsers;

import org.eclipse.jdt.core.IJavaProject;

public class CCFinderOutputParser extends CloneDetectorOutputParser {

	public CCFinderOutputParser(IJavaProject iJavaProject, String cloneOutputFilePath, String pathToCcfxprepdir) throws InvalidInputFileException {
		super(iJavaProject, cloneOutputFilePath);
		// TODO Auto-generated constructor stub
	}

	@Override
	public CloneGroupList readInputFile() throws InvalidInputFileException {
		// TODO Auto-generated method stub
		return null;
	}


}
