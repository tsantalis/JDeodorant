package gr.uom.java.ast.decomposition.cfg;

import java.util.Set;

import org.eclipse.jdt.core.dom.VariableDeclaration;

public class PDGTryNode extends PDGBlockNode {
	public PDGTryNode(CFGTryNode cfgTryNode, Set<VariableDeclaration> variableDeclarationsInMethod,
			Set<VariableDeclaration> fieldsAccessedInMethod) {
		super(cfgTryNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
		this.controlParent = cfgTryNode.getControlParent();
		determineDefinedAndUsedVariables();
	}
}
