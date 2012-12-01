package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.cfg.PDGNode;

public class PDGNodeMapping {
	private PDGNode nodeG1;
	private PDGNode nodeG2;
	private List<Replacement> replacements;
	private boolean validMatch;
	
	public PDGNodeMapping(PDGNode nodeG1, PDGNode nodeG2) {
		this.nodeG1 = nodeG1;
		this.nodeG2 = nodeG2;
		this.replacements = new ArrayList<Replacement>();
		
		AbstractStatement s1 = nodeG1.getStatement();
		AbstractStatement s2 = nodeG2.getStatement();
		if(!s1.toString().equals(s2.toString())) {
			List<Replacement> replacements = s1.findReplacements(s2);
			String tempS1 = s1.toString();
			for(Replacement replacement : replacements) {
				//we need to make a safer replacement strategy
				//Scenario: literal '0' is part of a variable name, we don't want literal '0' to be replaced in the variable name, but only in the literal's position
				tempS1 = tempS1.replaceFirst(Pattern.quote(replacement.getValue1()), Matcher.quoteReplacement(replacement.getValue2()));
			}
			if(tempS1.equals(s2.toString())) {
				this.replacements.addAll(replacements);
				validMatch = true;
			}
		}
		else {
			validMatch = true;
		}
	}
	
	public boolean isValidMatch() {
		return validMatch;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(nodeG1);
		sb.append(nodeG2);
		sb.append(replacements);
		return sb.toString();
	}
}
