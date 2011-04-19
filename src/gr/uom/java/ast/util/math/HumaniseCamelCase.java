package gr.uom.java.ast.util.math;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class HumaniseCamelCase {
    //----------------------------------------------------------------------- Static Properties and Constants

    private static final String CAMEL_CASE_PATTERN = "([A-Z]|[a-z])[a-z]*";

    //----------------------------------------------------------------------- Instance Properties

    private String humanisedString;
    private String acronym;

    //----------------------------------------------------------------------- Instance Methods

    /**
     * Converts a camelCase to a more human form, with spaces. E.g. 'Camel case'
     *
     * @param camelCaseString
     * @return a humanised version of a camelCaseString if it is indeed camel-case. Returns the
     * original string if it is'nt camel-case
     */
    public String humanise(String camelCaseString) {
        reset();
        Matcher wordMatcher = camelCaseWordMatcher(camelCaseString);
        while(wordMatcher.find()) {
            String word = wordMatcher.group();
            boolean wordIsSingleCapitalLetter = word.matches("^[A-Z]$");
            if(wordIsSingleCapitalLetter) {
                addToAcronym(word);
            } else {
                appendAcronymIfThereIsOne();
                appendWord(word);
            }
        }
        appendAcronymIfThereIsOne();
        return humanisedString.length() > 0 ? humanisedString : camelCaseString;
    }

    private Matcher camelCaseWordMatcher(String camelCaseString) {
        return Pattern.compile(CAMEL_CASE_PATTERN).matcher(camelCaseString);
    }

    private void reset() {
        humanisedString = "";
        acronym = "";
    }

    private void addToAcronym(String word) {
        acronym += word;
    }

    private void appendWord(String word) {
        boolean firstWord = humanisedString.length() == 0;
        humanisedString += firstWord ? capitaliseFirstLetter(word) : " " + word.toLowerCase();
    }

    private void appendAcronymIfThereIsOne() {
        if(acronym.length() > 0) {
            boolean firstWord = humanisedString.length() == 0;
            humanisedString += firstWord ? acronym : " " + acronym;
            acronym = "";
        }
    }

    private String capitaliseFirstLetter(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

}
