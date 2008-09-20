package gr.uom.java.ast.decomposition.cfg;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.VariableDeclaration;

public class PDGSlice extends Graph {
	private BasicBlock boundaryBlock;
	private PDGNode nodeCriterion;
	private VariableDeclaration localVariableCriterion;
	private Set<PDGNode> sliceNodes;
	private Set<PDGNode> remainingNodes;
	private Set<VariableDeclaration> passedParameters;
	
	public PDGSlice(PDG pdg, BasicBlock boundaryBlock, PDGNode nodeCriterion,
			VariableDeclaration localVariableCriterion) {
		super();
		this.boundaryBlock = boundaryBlock;
		this.nodeCriterion = nodeCriterion;
		this.localVariableCriterion = localVariableCriterion;
		Set<PDGNode> regionNodes = pdg.blockBasedRegion(boundaryBlock);
		for(PDGNode node : regionNodes) {
			nodes.add(node);
		}
		for(GraphEdge edge : pdg.edges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(nodes.contains(dependence.src) && nodes.contains(dependence.dst))
				edges.add(dependence);
		}
		this.sliceNodes = new LinkedHashSet<PDGNode>();
		if(nodeCriterion.definesLocalVariable(localVariableCriterion)) {
			sliceNodes.addAll(traverseBackward(nodeCriterion, new LinkedHashSet<PDGNode>()));
		}
		else if(nodeCriterion.usesLocalVariable(localVariableCriterion)) {
			Set<PDGNode> defNodes = getDefNodes(nodeCriterion, localVariableCriterion);
			for(PDGNode defNode : defNodes) {
				sliceNodes.addAll(traverseBackward(defNode, new LinkedHashSet<PDGNode>()));
			}
		}
		this.remainingNodes = new LinkedHashSet<PDGNode>();
		remainingNodes.add(pdg.getEntryNode());
		for(GraphNode node : pdg.nodes) {
			PDGNode pdgNode = (PDGNode)node;
			if(!sliceNodes.contains(pdgNode))
				remainingNodes.add(pdgNode);
		}
		this.passedParameters = new LinkedHashSet<VariableDeclaration>();
		for(GraphEdge edge : pdg.edges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(dependence instanceof PDGDataDependence) {
				PDGDataDependence dataDependence = (PDGDataDependence)dependence;
				PDGNode srcPDGNode = (PDGNode)dataDependence.src;
				PDGNode dstPDGNode = (PDGNode)dataDependence.dst;
				if(remainingNodes.contains(srcPDGNode) && sliceNodes.contains(dstPDGNode))
					passedParameters.add(dataDependence.getData());
			}
		}
	}

	public Set<PDGNode> getSliceNodes() {
		return sliceNodes;
	}

	public Set<VariableDeclaration> getPassedParameters() {
		return passedParameters;
	}

	private Set<PDGNode> getDefNodes(PDGNode node, VariableDeclaration localVariable) {
		Set<PDGNode> defNodes = new LinkedHashSet<PDGNode>();
		for(GraphEdge edge : node.incomingEdges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(edges.contains(dependence) && dependence instanceof PDGDataDependence) {
				PDGDataDependence dataDependence = (PDGDataDependence)dependence;
				if(dataDependence.getData().equals(localVariable)) {
					PDGNode srcPDGNode = (PDGNode)dependence.src;
					defNodes.add(srcPDGNode);
				}
			}
		}
		return defNodes;
	}

	private Set<PDGNode> traverseBackward(PDGNode node, Set<PDGNode> visitedNodes) {
		Set<PDGNode> sliceNodes = new LinkedHashSet<PDGNode>();
		sliceNodes.add(node);
		visitedNodes.add(node);
		for(GraphEdge edge : node.incomingEdges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(edges.contains(dependence)) {
				PDGNode srcPDGNode = (PDGNode)dependence.src;
				if(!visitedNodes.contains(srcPDGNode))
					sliceNodes.addAll(traverseBackward(srcPDGNode, visitedNodes));
			}
		}
		return sliceNodes;
	}

	public String toString() {
		return "<" + localVariableCriterion + ", " + nodeCriterion.getId() + "> [B" + boundaryBlock.getId() + "]\n" +
		sliceNodes + "\npassed parameters: " + passedParameters;
	}
}
