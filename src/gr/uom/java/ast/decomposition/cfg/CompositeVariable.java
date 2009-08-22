package gr.uom.java.ast.decomposition.cfg;

import org.eclipse.jdt.core.dom.VariableDeclaration;

public class CompositeVariable extends AbstractVariable {
	private AbstractVariable rightPart;
	private volatile int hashCode = 0;
	
	public CompositeVariable(VariableDeclaration referenceName, AbstractVariable rightPart) {
		super(referenceName);
		this.rightPart = rightPart;
	}

	//if composite variable is "one.two.three" then right part is "two.three"
	public AbstractVariable getRightPart() {
		return rightPart;
	}

	//if composite variable is "one.two.three" then left part is "one.two"
	public AbstractVariable getLeftPart() {
		if(rightPart instanceof PlainVariable) {
			return new PlainVariable(name);
		}
		else {
			CompositeVariable compositeVariable = (CompositeVariable)rightPart;
			return new CompositeVariable(name, compositeVariable.getLeftPart());
		}
	}

	//if composite variable is "one.two.three" then final variable is "three"
	public PlainVariable getFinalVariable() {
		if(rightPart instanceof PlainVariable) {
			return (PlainVariable)rightPart;
		}
		else {
			return ((CompositeVariable)rightPart).getFinalVariable();
		}
	}

	public boolean isLocalVariable() {
		return false;
	}

	public boolean containsPlainVariable(PlainVariable variable) {
		if(this.name.equals(variable.name))
			return true;
		return rightPart.containsPlainVariable(variable);
	}

	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(o instanceof CompositeVariable) {
			CompositeVariable composite = (CompositeVariable)o;
			return this.name.equals(composite.name) &&
			this.rightPart.equals(composite.rightPart);
		}
		return false;
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 31*result + name.hashCode();
			result = 31*result + rightPart.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name.getName().getIdentifier());
		sb.append(".");
		sb.append(rightPart.toString());
		return sb.toString();
	}
}
