package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.LocalVariableInstructionObject;

import java.util.LinkedHashSet;
import java.util.Set;

public class PDGSlice extends Graph {
	private PDGNode sliceNode;
	private LocalVariableInstructionObject sliceLocalVariable;
	
	public PDGSlice(PDG pdg, BasicBlock boundaryBlock, PDGNode sliceNode,
			LocalVariableInstructionObject sliceLocalVariable) {
		super();
		Set<PDGNode> regionNodes = pdg.blockBasedRegion(boundaryBlock);
		for(PDGNode node : regionNodes) {
			nodes.add(node);
		}
		for(GraphEdge edge : pdg.edges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(nodes.contains(dependence.src) && nodes.contains(dependence.dst))
				edges.add(dependence);
		}
		this.sliceNode = sliceNode;
		this.sliceLocalVariable = sliceLocalVariable;
	}

	public Set<PDGNode> getSliceNodes() {
		Set<PDGNode> sliceNodes = new LinkedHashSet<PDGNode>();
		if(sliceNode.definesLocalVariable(sliceLocalVariable)) {
			sliceNodes.addAll(traverseBackward(sliceNode));
		}
		else if(sliceNode.usesLocalVariable(sliceLocalVariable)) {
			Set<PDGNode> defNodes = getDefNodes(sliceNode, sliceLocalVariable);
			for(PDGNode defNode : defNodes) {
				sliceNodes.addAll(traverseBackward(defNode));
			}
		}
		return sliceNodes;
	}

	private Set<PDGNode> getDefNodes(PDGNode node, LocalVariableInstructionObject localVariable) {
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

	private Set<PDGNode> traverseBackward(PDGNode node) {
		Set<PDGNode> sliceNodes = new LinkedHashSet<PDGNode>();
		sliceNodes.add(node);
		for(GraphEdge edge : node.incomingEdges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(edges.contains(dependence)) {
				PDGNode srcPDGNode = (PDGNode)dependence.src;
				sliceNodes.addAll(traverseBackward(srcPDGNode));
			}
		}
		return sliceNodes;
	}
}
