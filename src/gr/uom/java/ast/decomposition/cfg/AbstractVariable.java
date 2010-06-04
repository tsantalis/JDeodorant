package gr.uom.java.ast.decomposition.cfg;

import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public abstract class AbstractVariable {
	//protected VariableDeclaration name;
	protected String variableBindingKey;
	protected String variableName;
	protected String variableType;
	protected boolean isField;
	protected boolean isParameter;
	
	public AbstractVariable(VariableDeclaration name) {
		IVariableBinding variableBinding = name.resolveBinding();
		this.variableBindingKey = variableBinding.getKey();
		this.variableName = variableBinding.getName();
		this.variableType = variableBinding.getType().getQualifiedName();
		this.isField = variableBinding.isField();
		this.isParameter = variableBinding.isParameter();
	}

	public AbstractVariable(String variableBindingKey, String variableName, String variableType, boolean isField, boolean isParameter) {
		this.variableBindingKey = variableBindingKey;
		this.variableName = variableName;
		this.variableType = variableType;
		this.isField = isField;
		this.isParameter = isParameter;
	}

	public String getVariableBindingKey() {
		return variableBindingKey;
	}

	public String getVariableName() {
		return variableName;
	}

	public String getVariableType() {
		return variableType;
	}

	public boolean isField() {
		return isField;
	}

	public boolean isParameter() {
		return isParameter;
	}

	public abstract boolean containsPlainVariable(PlainVariable variable);
	
	public abstract PlainVariable getInitialVariable();
}
