package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.decomposition.AbstractStatement;

public class CFGBranchSwitchNode extends CFGBranchConditionalNode {

	public CFGBranchSwitchNode(AbstractStatement statement) {
		super(statement);
	}
}
