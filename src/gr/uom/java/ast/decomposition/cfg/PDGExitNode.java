package gr.uom.java.ast.decomposition.cfg;

import java.util.Set;

import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class PDGExitNode extends PDGStatementNode {
	private AbstractVariable returnedVariable;
	
	public PDGExitNode(CFGNode cfgNode, Set<VariableDeclaration> variableDeclarationsInMethod,
			Set<VariableDeclaration> fieldsAccessedInMethod) {
		super(cfgNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
		if(cfgNode instanceof CFGExitNode) {
			CFGExitNode exitNode = (CFGExitNode)cfgNode;
			SimpleName returnedVariableSimpleName = exitNode.getReturnedVariable();
			if(returnedVariableSimpleName != null) {
				for(VariableDeclaration declaration : variableDeclarationsInMethod) {
					if(declaration.resolveBinding().isEqualTo(returnedVariableSimpleName.resolveBinding())) {
						returnedVariable = new PlainVariable(declaration);
						break;
					}
				}
			}
		}
	}

	public AbstractVariable getReturnedVariable() {
		return returnedVariable;
	}
}
