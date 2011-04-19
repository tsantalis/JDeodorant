package gr.uom.java.ast.util;

import java.util.ArrayList;

import gr.uom.java.ast.util.math.HumaniseCamelCase;
import gr.uom.java.ast.util.math.Stemmer;

public interface TopicFinder {
	
	public void findTopic(Stemmer stemmer, HumaniseCamelCase humaniser, ArrayList<String> stopWords);

}
