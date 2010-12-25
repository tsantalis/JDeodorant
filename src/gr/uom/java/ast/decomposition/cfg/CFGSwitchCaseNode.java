package gr.uom.java.ast.decomposition.cfg;

import org.eclipse.jdt.core.dom.SwitchCase;

import gr.uom.java.ast.decomposition.AbstractStatement;

public class CFGSwitchCaseNode extends CFGNode {
	private boolean isDefault;
	
	public CFGSwitchCaseNode(AbstractStatement statement) {
		super(statement);
		SwitchCase switchCase = (SwitchCase)statement.getStatement();
		if(switchCase.isDefault())
			isDefault = true;
		else
			isDefault = false;
	}

	public boolean isDefault() {
		return isDefault;
	}
}
