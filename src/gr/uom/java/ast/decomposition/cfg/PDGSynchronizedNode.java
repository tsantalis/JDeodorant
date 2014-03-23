package gr.uom.java.ast.decomposition.cfg;

import java.util.Set;

import org.eclipse.jdt.core.dom.VariableDeclaration;

public class PDGSynchronizedNode extends PDGBlockNode {
	public PDGSynchronizedNode(CFGSynchronizedNode cfgSynchronizedNode, Set<VariableDeclaration> variableDeclarationsInMethod,
			Set<VariableDeclaration> fieldsAccessedInMethod) {
		super(cfgSynchronizedNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
		this.controlParent = cfgSynchronizedNode.getControlParent();
		determineDefinedAndUsedVariables();
	}
}
