package gr.uom.java.ast.decomposition;

import jscl.math.Generic;
import jscl.text.ParseException;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;

public class SimplificationMatcher {

	private String simplifiedExpression;
	
	public SimplificationMatcher(InfixExpression expr1, InfixExpression expr2) {
		this.simplifiedExpression = expressionSimplification(expr1, expr2);
	}

	public String getSimplifiedExpression() {
		return simplifiedExpression;
	}

	public boolean isSimplifiedToTheSameExpression() {
		return simplifiedExpression != null;
	}

	private String expressionSimplification(Expression exp1, Expression exp2) {
		String simplifiedExpression = null;
		String expString1 = exp1.toString();
		String expString2 = exp2.toString();
		expString1 = replaceMathPowMethodCalls(expString1);
		expString2 = replaceMathPowMethodCalls(expString2);
		try {
			jscl.math.Expression e1 = jscl.math.Expression.valueOf(expString1);
			jscl.math.Expression e2 = jscl.math.Expression.valueOf(expString2);
			Generic g1 = e1.simplify();
			Generic g2 = e2.simplify();
			if(g1.equals(g2)) {
				simplifiedExpression = g1.toString();
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return simplifiedExpression;
	}
	
	private String replaceMathPowMethodCalls(String expString) {
		while(expString.contains("Math.pow")) {
			int beginPosition=expString.indexOf("Math.pow(");
			int startPosition=expString.indexOf("(", beginPosition);
			int midPosition=expString.indexOf(",",beginPosition);
			int endPosition=expString.indexOf(")", beginPosition);
			String base=expString.substring(startPosition+1, midPosition);
			String power=expString.substring(midPosition+1, endPosition);
			String replaceString=base+"^"+power;
			expString=expString.replace("Math.pow("+base+","+power+")", replaceString);
		}
		return expString;
	}
}
