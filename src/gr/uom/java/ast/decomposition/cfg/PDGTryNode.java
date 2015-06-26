package gr.uom.java.ast.decomposition.cfg;

import java.util.Set;

import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.VariableDeclarationObject;

public class PDGTryNode extends PDGBlockNode {
	public PDGTryNode(CFGTryNode cfgTryNode, Set<VariableDeclarationObject> variableDeclarationsInMethod,
			Set<FieldObject> fieldsAccessedInMethod) {
		super(cfgTryNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
		this.controlParent = cfgTryNode.getControlParent();
		determineDefinedAndUsedVariables();
	}
}
