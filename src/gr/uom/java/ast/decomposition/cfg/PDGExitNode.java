package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.VariableDeclarationObject;

import java.util.Set;

import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class PDGExitNode extends PDGStatementNode {
	private AbstractVariable returnedVariable;
	
	public PDGExitNode(CFGNode cfgNode, Set<VariableDeclarationObject> variableDeclarationsInMethod) {
		super(cfgNode, variableDeclarationsInMethod);
		if(cfgNode instanceof CFGExitNode) {
			CFGExitNode exitNode = (CFGExitNode)cfgNode;
			SimpleName returnedVariableSimpleName = null;
			if(exitNode.getReturnedVariable() != null)
				returnedVariableSimpleName = exitNode.getReturnedVariable().getSimpleName();
			if(returnedVariableSimpleName != null) {
				for(VariableDeclarationObject declarationObject : variableDeclarationsInMethod) {
					VariableDeclaration declaration = declarationObject.getVariableDeclaration();
					if(declaration.resolveBinding().isEqualTo(returnedVariableSimpleName.resolveBinding())) {
						returnedVariable = new PlainVariable(declarationObject);
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
