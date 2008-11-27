package gr.uom.java.ast.decomposition.cfg;

import java.util.ListIterator;

import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.ParameterObject;

public class PDGMethodEntryNode extends PDGNode {
	private MethodObject method;
	
	public PDGMethodEntryNode(MethodObject method) {
		super();
		this.method = method;
		this.id = 0;
		ListIterator<ParameterObject> parameterIterator = method.getParameterListIterator();
		while(parameterIterator.hasNext()) {
			ParameterObject parameter = parameterIterator.next();
			PlainVariable parameterVariable = new PlainVariable(parameter.getSingleVariableDeclaration());
			declaredVariables.add(parameterVariable);
			definedVariables.add(parameterVariable);
		}
	}

	public BasicBlock getBasicBlock() {
		return null;
	}

	public boolean equals(Object o) {
		if(this == o)
    		return true;
    	
    	if(o instanceof PDGMethodEntryNode) {
    		PDGMethodEntryNode pdgNode = (PDGMethodEntryNode)o;
    		return this.method.equals(pdgNode.method);
    	}
    	return false;
	}

	public int hashCode() {
		return method.hashCode();
	}

	public String toString() {
		return id + "\t" + method.getName();
	}
}
