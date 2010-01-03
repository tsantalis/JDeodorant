package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.MethodObject;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;

public class PDGObjectSliceUnion {
	private PDG pdg;
	private List<PDGSliceUnion> sliceUnions;
	private MethodObject method;
	private BasicBlock boundaryBlock;
	private AbstractVariable objectReference;
	private IFile iFile;
	private int methodSize;
	
	public PDGObjectSliceUnion(PDG pdg, BasicBlock boundaryBlock, List<PDGSliceUnion> sliceUnions,
			AbstractVariable objectReference) {
		this.pdg = pdg;
		this.sliceUnions = sliceUnions;
		this.method = pdg.getMethod();
		this.iFile = pdg.getIFile();
		this.methodSize = pdg.getTotalNumberOfStatements();
		this.boundaryBlock = boundaryBlock;
		this.objectReference = objectReference;
	}

	public MethodObject getMethod() {
		return method;
	}

	public IFile getIFile() {
		return iFile;
	}

	public BasicBlock getBoundaryBlock() {
		return boundaryBlock;
	}

	public PDGNode getExtractedMethodInvocationInsertionNode() {
		TreeSet<PDGNode> firstNodesOfSlices = new TreeSet<PDGNode>();
		for(PDGSliceUnion sliceUnion : sliceUnions) {
			firstNodesOfSlices.add(sliceUnion.getExtractedMethodInvocationInsertionNode());
		}
		return firstNodesOfSlices.first();
	}

	public AbstractVariable getObjectReference() {
		return objectReference;
	}

	public Set<PDGNode> getSliceNodes() {
		Set<PDGNode> sliceNodes = new TreeSet<PDGNode>();
		for(PDGSliceUnion sliceUnion : sliceUnions) {
			sliceNodes.addAll(sliceUnion.getSliceNodes());
		}
		return sliceNodes;
	}

	public Set<AbstractVariable> getPassedParameters() {
		Set<AbstractVariable> passedParameters = new LinkedHashSet<AbstractVariable>();
		for(PDGSliceUnion sliceUnion : sliceUnions) {
			passedParameters.addAll(sliceUnion.getPassedParameters());
		}
		return passedParameters;
	}

	public Set<PDGNode> getRemovableNodes() {
		Set<PDGNode> removableNodes = new LinkedHashSet<PDGNode>();
		for(PDGSliceUnion sliceUnion : sliceUnions) {
			removableNodes.addAll(sliceUnion.getRemovableNodes());
		}
		for(PDGNode node : getSliceNodes()) {
			if(node.declaresLocalVariable(objectReference) && !removableNodes.contains(node)) {
				removableNodes.add(node);
				break;
			}
		}
		return removableNodes;
	}

	public boolean edgeBelongsToBlockBasedRegion(GraphEdge edge) {
		int counter = 0;
		for(PDGSliceUnion sliceUnion : sliceUnions) {
			if(sliceUnion.edgeBelongsToBlockBasedRegion(edge))
				counter++;
		}
		if(sliceUnions.size() == counter)
			return true;
		return false;
	}

	public boolean declarationOfObjectReferenceBelongsToSliceNodes() {
		for(PDGNode node : getSliceNodes()) {
			if(node.declaresLocalVariable(objectReference))
				return true;
		}
		return false;
	}

	public boolean declarationOfObjectReferenceBelongsToRemovableNodes() {
		for(PDGNode node : getRemovableNodes()) {
			if(node.declaresLocalVariable(objectReference))
				return true;
		}
		return false;
	}

	private boolean sliceContainsDeclaration(AbstractVariable variableDeclaration) {
		for(PDGNode node : getSliceNodes()) {
			if(node.declaresLocalVariable(variableDeclaration))
				return true;
		}
		return false;
	}

	private boolean allNodeCriteriaAreDuplicated() {
		int counter = 0;
		for(PDGSliceUnion sliceUnion : sliceUnions) {
			if(sliceUnion.allNodeCriteriaAreDuplicated())
				counter++;
		}
		if(sliceUnions.size() == counter)
			return true;
		return false;
	}

	private boolean nonDuplicatedSliceNodeAntiDependsOnNonRemovableNode() {
		Set<PDGNode> sliceNodes = getSliceNodes();
		Set<PDGNode> removableNodes = getRemovableNodes();
		Set<PDGNode> duplicatedNodes = new LinkedHashSet<PDGNode>();
		duplicatedNodes.addAll(sliceNodes);
		duplicatedNodes.removeAll(removableNodes);
		for(PDGNode sliceNode : sliceNodes) {
			if(!duplicatedNodes.contains(sliceNode)) {
				for(GraphEdge edge : sliceNode.incomingEdges) {
					PDGDependence dependence = (PDGDependence)edge;
					if(edgeBelongsToBlockBasedRegion(dependence) && dependence instanceof PDGAntiDependence) {
						PDGAntiDependence antiDependence = (PDGAntiDependence)dependence;
						PDGNode srcPDGNode = (PDGNode)antiDependence.src;
						if(!removableNodes.contains(srcPDGNode))
							return true;
					}
				}
			}
		}
		return false;
	}

	private boolean containsDuplicateNodeWithStateChangingMethodInvocation() {
		Set<PDGNode> duplicatedNodes = new LinkedHashSet<PDGNode>();
		duplicatedNodes.addAll(getSliceNodes());
		duplicatedNodes.removeAll(getRemovableNodes());
		for(PDGNode node : duplicatedNodes) {
			for(AbstractVariable stateChangingVariable : node.getStateChangingVariables()) {
				PlainVariable plainVariable = null;
				if(stateChangingVariable instanceof PlainVariable) {
					plainVariable = (PlainVariable)stateChangingVariable;
				}
				else if(stateChangingVariable instanceof CompositeVariable) {
					CompositeVariable compositeVariable = (CompositeVariable)stateChangingVariable;
					plainVariable = new PlainVariable(compositeVariable.getName());
				}
				if(!sliceContainsDeclaration(plainVariable))
					return true;
			}
		}
		return false;
	}

	private boolean containsDuplicateNodeWithClassInstanceCreation() {
		Set<PDGNode> duplicatedNodes = new LinkedHashSet<PDGNode>();
		duplicatedNodes.addAll(getSliceNodes());
		duplicatedNodes.removeAll(getRemovableNodes());
		for(PDGNode node : duplicatedNodes) {
			if(node.containsClassInstanceCreation() && !node.declaresLocalVariable(objectReference))
				return true;
		}
		return false;
	}

	private boolean containsBreakContinueReturnSliceNode() {
		for(PDGNode node : getSliceNodes()) {
			Statement statement = node.getASTStatement();
			if(statement instanceof BreakStatement || statement instanceof ContinueStatement ||
					statement instanceof ReturnStatement)
				return true;
		}
		return false;
	}

	private boolean objectSliceEqualsMethodBody() {
		int sliceSize = getSliceNodes().size();
		if(sliceSize == methodSize)
			return true;
		else if(sliceSize == methodSize - 1) {
			TreeSet<GraphNode> nonIncludedInSliceMethodNodes = new TreeSet<GraphNode>(pdg.nodes);
			nonIncludedInSliceMethodNodes.removeAll(getSliceNodes());
			PDGNode pdgNode = (PDGNode)nonIncludedInSliceMethodNodes.first();
			if(pdgNode instanceof PDGExitNode)
				return true;
		}
		return false;
	}

	private boolean objectSliceHasMinimumSize() {
		int sliceSize = getSliceNodes().size();
		if(sliceSize == 1)
			return true;
		else if(sliceSize == 2) {
			if(sliceContainsDeclaration(objectReference))
				return true;
		}
		return false;
	}

	private boolean objectReferenceIsReturnedVariableInOriginalMethod() {
		if(pdg.getReturnedVariables().contains(objectReference))
			return true;
		return false;
	}

	public boolean satisfiesRules() {
		if(objectSliceEqualsMethodBody() || objectSliceHasMinimumSize() ||
				objectReferenceIsReturnedVariableInOriginalMethod() ||
				allNodeCriteriaAreDuplicated() || containsBreakContinueReturnSliceNode() ||
				containsDuplicateNodeWithStateChangingMethodInvocation() ||
				nonDuplicatedSliceNodeAntiDependsOnNonRemovableNode() ||
				containsDuplicateNodeWithClassInstanceCreation())
			return false;
		return true;
	}
}
