package ca.concordia.jdeodorant.clone.parsers;

import org.eclipse.jdt.core.IJavaProject;

public class CloneDetectorOutputParserFactory {
	
	public static CloneDetectorOutputParser getCloneToolParser(CloneDetectorType tool, IJavaProject jProject, String mainFile, String... otherArgs) {
		switch (tool) {
		case CCFINDER:
//			return new CCFinderOutputParser(jProject, mainFile, otherArgs[0], otherArgs.length == 2 ? otherArgs[1] : "", outputExcelFile, launchApplication, binFolder);
		case CONQAT:
//			return new ConQatOutputParser(jProject, mainFile, outputExcelFile, launchApplication, binFolder);
		case DECKARD:
			return new DeckardOutputParser(jProject, mainFile);
		case NICAD:
//			return new NicadOutputParser(jProject, mainFile, outputExcelFile, otherArgs.length == 1 ?  otherArgs[0] : "", launchApplication, binFolder);
		case CLONEDR:
//			return new CloneDROutputParser(jProject, mainFile, outputExcelFile, otherArgs.length == 1 ?  otherArgs[0] : "", launchApplication, binFolder);
		default:
			throw new IllegalArgumentException("Not yet implemented.");
		}
	}
}
