package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.MethodObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.VariableDeclaration;

public class PDGSliceUnion {
	private List<PDGSlice> slices;
	private MethodObject method;
	private BasicBlock boundaryBlock;
	private Set<PDGNode> nodeCriterions;
	private VariableDeclaration localVariableCriterion;
	
	public PDGSliceUnion(PDG pdg, BasicBlock boundaryBlock, Set<PDGNode> nodeCriterions,
			VariableDeclaration localVariableCriterion) {
		this.slices = new ArrayList<PDGSlice>();
		for(PDGNode nodeCriterion : nodeCriterions) {
			PDGSlice slice = new PDGSlice(pdg, boundaryBlock, nodeCriterion, localVariableCriterion);
			slices.add(slice);
		}
		this.method = pdg.getMethod();
		this.boundaryBlock = boundaryBlock;
		this.nodeCriterions = nodeCriterions;
		this.localVariableCriterion = localVariableCriterion;
	}

	public MethodObject getMethod() {
		return method;
	}

	public PDGNode getExtractedMethodInvocationInsertionNode() {
		return boundaryBlock.getLeader().getPDGNode();
	}

	public VariableDeclaration getLocalVariableCriterion() {
		return localVariableCriterion;
	}

	public Set<PDGNode> getSliceNodes() {
		Set<PDGNode> sliceNodes = new TreeSet<PDGNode>();
		for(PDGSlice slice : slices) {
			sliceNodes.addAll(slice.getSliceNodes());
		}
		return sliceNodes;
	}

	public Set<VariableDeclaration> getPassedParameters() {
		Set<VariableDeclaration> passedParameters = new LinkedHashSet<VariableDeclaration>();
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
		return removableNodes;
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

	private boolean allNodeCriterionsAreDuplicated() {
		int counter = 0;
		for(PDGSlice slice : slices) {
			if(slice.nodeCriterionIsDuplicated())
				counter++;
		}
		if(nodeCriterions.size() == counter)
			return true;
		return false;
	}

	private boolean sliceContainsOnlyOneNodeCriterionAndDeclarationOfVariableCriterion() {
		if(slices.size() == 1 && getSliceNodes().size() == 2 &&
				declarationOfVariableCriterionBelongsToSliceNodes())
			return true;
		return false;
	}

	public boolean satisfiesRules() {
		for(PDGSlice slice : slices) {
			if(!slice.satisfiesRules())
				return false;
		}
		if(allNodeCriterionsAreDuplicated() ||
				sliceContainsOnlyOneNodeCriterionAndDeclarationOfVariableCriterion())
			return false;
		if(getSliceNodes().size() <= nodeCriterions.size())
			return false;
		return true;
	}
}
