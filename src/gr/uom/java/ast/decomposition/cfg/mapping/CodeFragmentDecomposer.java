package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CompositeVariable;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class CodeFragmentDecomposer {

	private Map<PlainVariable, Set<PDGNode>> objectNodeMap;
	
	public CodeFragmentDecomposer(Set<PDGNode> nodes) {
		this.objectNodeMap = new LinkedHashMap<PlainVariable, Set<PDGNode>>();
		Set<PlainVariable> declaredVariables = getDeclaredVariables(nodes);
		for(PlainVariable objectReference : declaredVariables) {
			Set<PDGNode> nodesDefiningAttributes = getNodesDefiningAttributesOfReference(objectReference, nodes);
			if(!nodesDefiningAttributes.isEmpty()) {
				Set<PDGNode> objectNodes = new LinkedHashSet<PDGNode>();
				PDGNode nodeDeclaringReference = getNodeDeclaringReference(objectReference, nodes);
				if(nodeDeclaringReference != null)
					objectNodes.add(nodeDeclaringReference);
				objectNodes.addAll(nodesDefiningAttributes);
				objectNodeMap.put(objectReference, objectNodes);
			}
		}
	}

	public Map<PlainVariable, Set<PDGNode>> getObjectNodeMap() {
		return objectNodeMap;
	}

	private Set<PDGNode> getNodesDefiningAttributesOfReference(PlainVariable reference, Set<PDGNode> nodes) {
		Set<PDGNode> nodesDefiningAttributes = new LinkedHashSet<PDGNode>();
		for(PDGNode pdgNode : nodes) {
			Iterator<AbstractVariable> definedVariableIterator = pdgNode.getDefinedVariableIterator();
			while(definedVariableIterator.hasNext()) {
				AbstractVariable definedVariable = definedVariableIterator.next();
				if(definedVariable instanceof CompositeVariable) {
					CompositeVariable compositeVariable = (CompositeVariable)definedVariable;
					if(compositeVariable.getVariableBindingKey().equals(reference.getVariableBindingKey())) {
						nodesDefiningAttributes.add(pdgNode);
						break;
					}
				}
			}
		}
		return nodesDefiningAttributes;
	}

	private PDGNode getNodeDeclaringReference(PlainVariable reference, Set<PDGNode> nodes) {
		for(PDGNode pdgNode : nodes) {
			if(pdgNode.declaresLocalVariable(reference))
				return pdgNode;
		}
		return null;
	}

	private Set<PlainVariable> getDeclaredVariables(Set<PDGNode> nodes) {
		Set<PlainVariable> declaredVariables = new LinkedHashSet<PlainVariable>();
		for(PDGNode node : nodes) {
			Iterator<AbstractVariable> iterator = node.getDeclaredVariableIterator();
			if(iterator.hasNext()) {
				AbstractVariable declaredVariable = iterator.next();
				if(declaredVariable instanceof PlainVariable) {
					declaredVariables.add((PlainVariable)declaredVariable);
				}
			}
		}
		return declaredVariables;
	}
}
