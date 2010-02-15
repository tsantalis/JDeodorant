package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.MethodObject;
import gr.uom.java.jdeodorant.preferences.PreferenceConstants;
import gr.uom.java.jdeodorant.refactoring.Activator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.preference.IPreferenceStore;

public class PDGSliceUnion {
	private List<PDGSlice> slices;
	private MethodObject method;
	private BasicBlock boundaryBlock;
	private Set<PDGNode> nodeCriteria;
	private AbstractVariable localVariableCriterion;
	private IFile iFile;
	private int methodSize;
	
	public PDGSliceUnion(PDG pdg, BasicBlock boundaryBlock, Set<PDGNode> nodeCriteria,
			AbstractVariable localVariableCriterion) {
		this.slices = new ArrayList<PDGSlice>();
		for(PDGNode nodeCriterion : nodeCriteria) {
			PDGSlice slice = new PDGSlice(pdg, boundaryBlock, nodeCriterion, localVariableCriterion);
			slices.add(slice);
		}
		this.method = pdg.getMethod();
		this.iFile = pdg.getIFile();
		this.methodSize = pdg.getTotalNumberOfStatements();
		this.boundaryBlock = boundaryBlock;
		this.nodeCriteria = nodeCriteria;
		this.localVariableCriterion = localVariableCriterion;
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
		for(PDGSlice slice : slices) {
			firstNodesOfSlices.add(slice.getExtractedMethodInvocationInsertionNode());
		}
		return firstNodesOfSlices.first();
	}

	public AbstractVariable getLocalVariableCriterion() {
		return localVariableCriterion;
	}

	public Set<PDGNode> getSliceNodes() {
		Set<PDGNode> sliceNodes = new TreeSet<PDGNode>();
		for(PDGSlice slice : slices) {
			sliceNodes.addAll(slice.getSliceNodes());
		}
		return sliceNodes;
	}

	public Set<AbstractVariable> getPassedParameters() {
		Set<AbstractVariable> passedParameters = new LinkedHashSet<AbstractVariable>();
		for(PDGSlice slice : slices) {
			passedParameters.addAll(slice.getPassedParameters());
		}
		return passedParameters;
	}

	public Set<PDGNode> getRemovableNodes() {
		Set<PDGNode> removableNodes = new LinkedHashSet<PDGNode>();
		for(PDGSlice slice : slices) {
			removableNodes.addAll(slice.getRemovableNodes());
		}
		Set<PDGNode> sliceNodes = getSliceNodes();
		List<PDGNode> sliceNodesInReverseOrder = new ArrayList<PDGNode>(sliceNodes);
		Collections.reverse(sliceNodesInReverseOrder);
		for(PDGNode sliceNode : sliceNodesInReverseOrder) {
			if(sliceNode.getCFGNode() instanceof CFGBranchIfNode) {
				int numberOfControlDependentNodes = 0;
				int numberOfRemovableControlDependentNodes = 0;
				for(GraphEdge edge : sliceNode.outgoingEdges) {
					PDGDependence dependence = (PDGDependence)edge;
					if(dependence instanceof PDGControlDependence) {
						PDGNode dstPDGNode = (PDGNode)dependence.dst;
						numberOfControlDependentNodes++;
						if(removableNodes.contains(dstPDGNode))
							numberOfRemovableControlDependentNodes++;
					}
				}
				if(numberOfControlDependentNodes == numberOfRemovableControlDependentNodes)
					removableNodes.add(sliceNode);
			}
		}
		return removableNodes;
	}

	public boolean edgeBelongsToBlockBasedRegion(GraphEdge edge) {
		int counter = 0;
		for(PDGSlice slice : slices) {
			if(slice.edgeBelongsToBlockBasedRegion(edge))
				counter++;
		}
		if(slices.size() == counter)
			return true;
		return false;
	}

	public boolean declarationOfVariableCriterionBelongsToSliceNodes() {
		for(PDGSlice slice : slices) {
			if(slice.declarationOfVariableCriterionBelongsToSliceNodes())
				return true;
		}
		return false;
	}

	public boolean declarationOfVariableCriterionBelongsToRemovableNodes() {
		for(PDGSlice slice : slices) {
			if(slice.declarationOfVariableCriterionBelongsToRemovableNodes())
				return true;
		}
		return false;
	}

	public boolean allNodeCriteriaAreDuplicated() {
		int counter = 0;
		for(PDGSlice slice : slices) {
			if(slice.nodeCriterionIsDuplicated())
				counter++;
		}
		if(nodeCriteria.size() == counter)
			return true;
		return false;
	}

	private boolean sliceContainsOnlyOneNodeCriterionAndDeclarationOfVariableCriterion() {
		if(slices.size() == 1 && getSliceNodes().size() == 2 &&
				declarationOfVariableCriterionBelongsToSliceNodes())
			return true;
		return false;
	}

	private boolean complyWithUserThresholds() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		int minimumSliceSize = store.getInt(PreferenceConstants.P_MINIMUM_SLICE_SIZE);
		int maximumSliceSize = store.getInt(PreferenceConstants.P_MAXIMUM_SLICE_SIZE);
		int maximumDuplication = store.getInt(PreferenceConstants.P_MAXIMUM_DUPLICATION);
		double maximumRatioOfDuplicatedToExtracted = store.getDouble(
				PreferenceConstants.P_MAXIMUM_RATIO_OF_DUPLICATED_TO_EXTRACTED);
		
		int sliceSize = getSliceNodes().size();
		int duplicatedSize = sliceSize - getRemovableNodes().size();
		double ratioOfDuplicatedToExtracted = (double)duplicatedSize/(double)sliceSize;
		
		if(sliceSize < minimumSliceSize)
			return false;
		if(sliceSize > (methodSize - maximumSliceSize))
			return false;
		if(duplicatedSize > maximumDuplication)
			return false;
		if(ratioOfDuplicatedToExtracted > maximumRatioOfDuplicatedToExtracted)
			return false;
		return true;
	}

	public boolean satisfiesRules() {
		for(PDGSlice slice : slices) {
			if(!slice.satisfiesRules())
				return false;
		}
		if(allNodeCriteriaAreDuplicated() ||
				sliceContainsOnlyOneNodeCriterionAndDeclarationOfVariableCriterion())
			return false;
		if(getSliceNodes().size() <= nodeCriteria.size())
			return false;
		if(!complyWithUserThresholds())
			return false;
		return true;
	}
}
