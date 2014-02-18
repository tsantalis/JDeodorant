package gr.uom.java.ast.decomposition.matching;

import jscl.math.Generic;
import jscl.text.ParseException;
import org.eclipse.jdt.core.dom.ASTNode;

public class SimplificationMatcher {

	private String simplifiedExpression;
	
	public SimplificationMatcher(ASTNode node1,ASTNode node2){
		this.simplifiedExpression = expressionSimplification(node1,node2);
	}

	public String getSimplifiedExpression() {
		return simplifiedExpression;
	}

	public boolean isSimplifiedToTheSameExpression() {
		return simplifiedExpression != null;
	}

	private String expressionSimplification(ASTNode exp1, ASTNode exp2) {
		String simplifiedExpression = null;
		String stringAfterConveration1 = null;
		String stringAfterConveration2 = null;
		String expString1 = exp1.toString();
		String expString2 = exp2.toString();
		if (!exp1.toString().equals(exp2.toString())) {
			stringAfterConveration1 = convertString(expString1);
			stringAfterConveration2 = convertString(expString2);
			try {
				jscl.math.Expression e1 = jscl.math.Expression
						.valueOf(stringAfterConveration1);
				jscl.math.Expression e2 = jscl.math.Expression
						.valueOf(stringAfterConveration2);
				Generic g1 = e1.simplify();
				Generic g2 = e2.simplify();
				if (g1.equals(g2)) {
					simplifiedExpression = g1.toString();
					System.out.println(simplifiedExpression);
				}
			} catch (ParseException e) {
				
			}
		}
		return simplifiedExpression;
	}
	
	private String convertString(String expString){
		expString=replaceOtherMathMethodCalls(replacePow(replaceSIGNUM(replaceCBRT(replacePI(expString)))));
		return expString;
	}
	
	private String replaceOtherMathMethodCalls(String expString) {
		while (expString.contains("Math.")) {
			expString = expString.replace("Math.", "");
		}
		try {
			jscl.math.Expression e = jscl.math.Expression.valueOf(expString);
			Generic g = e.elementary();
			expString = g.toString();
		} catch (ParseException e) {

		}
		return expString;
	}
	
	private String replacePow(String expString) {
		while (expString.contains("pow")) {
			int beginPosition = expString.indexOf("pow(");
			int startPosition = expString.indexOf("(", beginPosition);
			int midPosition = expString.indexOf(",", beginPosition);
			int endPosition = expString.indexOf(")", midPosition);
			String base = expString.substring(startPosition + 1, midPosition);
			String power = expString.substring(midPosition + 1, endPosition);
			String replaceString = base + "^" + power;
			expString = expString.replace("pow(" + base + "," + power + ")",replaceString);
		}
		return expString;
	}
	
	private String replacePI(String expString){
		while(expString.contains("PI") || expString.contains("3.141592653589793")){
			expString = expString.replace("PI", "pi");
			expString = expString.replace("3.141592653589793", "pi");
		}
		return expString;
	}
	
	private String replaceCBRT(String expString) {
		while(expString.contains("cbrt")){
			expString = expString.replace("cbrt", "cubic");
		}
		return expString;
	}
	
	private String replaceSIGNUM(String expString){
		
		while(expString.contains("signum")){
			expString = expString.replace("signum", "sgn");
		}
		return expString;
	}
}
