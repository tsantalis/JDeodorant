package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.MethodObject;
import gr.uom.java.jdeodorant.preferences.PreferenceConstants;
import gr.uom.java.jdeodorant.refactoring.Activator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jface.preference.IPreferenceStore;

public class PDGSliceUnion {
	private PDG pdg;
	private List<PDGSlice> slices;
	private MethodObject method;
	private BasicBlock boundaryBlock;
	private Set<PDGNode> nodeCriteria;
	private AbstractVariable localVariableCriterion;
	private IFile iFile;
	private int methodSize;
	private PDGSlice subgraph;
	private Set<PDGNode> sliceNodes;
	private Set<AbstractVariable> passedParameters;
	private Set<PDGNode> indispensableNodes;
	private Set<PDGNode> removableNodes;
	
	public PDGSliceUnion(PDG pdg, BasicBlock boundaryBlock, Set<PDGNode> nodeCriteria,
			AbstractVariable localVariableCriterion) {
		this.pdg = pdg;
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
		this.subgraph = slices.get(0);
		this.sliceNodes = getSliceNodes();
		Set<PDGNode> remainingNodes = new TreeSet<PDGNode>();
		for(GraphNode node : pdg.nodes) {
			PDGNode pdgNode = (PDGNode)node;
			if(!sliceNodes.contains(pdgNode))
				remainingNodes.add(pdgNode);
		}
		this.passedParameters = new LinkedHashSet<AbstractVariable>();
		Set<PDGNode> nCD = new LinkedHashSet<PDGNode>();
		Set<PDGNode> nDD = new LinkedHashSet<PDGNode>();
		for(GraphEdge edge : pdg.edges) {
			PDGDependence dependence = (PDGDependence)edge;
			PDGNode srcPDGNode = (PDGNode)dependence.src;
			PDGNode dstPDGNode = (PDGNode)dependence.dst;
			if(dependence instanceof PDGDataDependence) {
				PDGDataDependence dataDependence = (PDGDataDependence)dependence;
				if(remainingNodes.contains(srcPDGNode) && sliceNodes.contains(dstPDGNode))
					passedParameters.add(dataDependence.getData());
				if(sliceNodes.contains(srcPDGNode) && remainingNodes.contains(dstPDGNode) &&
						!dataDependence.getData().equals(localVariableCriterion))
					nDD.add(srcPDGNode);
			}
			else if(dependence instanceof PDGControlDependence) {
				if(sliceNodes.contains(srcPDGNode) && remainingNodes.contains(dstPDGNode))
					nCD.add(srcPDGNode);
			}
		}
		Set<PDGNode> controlIndispensableNodes = new LinkedHashSet<PDGNode>();
		for(PDGNode p : nCD) {
			for(AbstractVariable usedVariable : p.usedVariables) {
				Set<PDGNode> pSliceNodes = subgraph.computeSlice(p, usedVariable);
				for(GraphNode node : pdg.nodes) {
					PDGNode q = (PDGNode)node;
					if(pSliceNodes.contains(q) || q.equals(p))
						controlIndispensableNodes.add(q);
				}
			}
		}
		Set<PDGNode> dataIndispensableNodes = new LinkedHashSet<PDGNode>();
		for(PDGNode p : nDD) {
			for(AbstractVariable definedVariable : p.definedVariables) {
				Set<PDGNode> pSliceNodes = subgraph.computeSlice(p, definedVariable);
				for(GraphNode node : pdg.nodes) {
					PDGNode q = (PDGNode)node;
					if(pSliceNodes.contains(q))
						dataIndispensableNodes.add(q);
				}
			}
		}
		this.indispensableNodes = new TreeSet<PDGNode>();
		indispensableNodes.addAll(controlIndispensableNodes);
		indispensableNodes.addAll(dataIndispensableNodes);
		this.removableNodes = new LinkedHashSet<PDGNode>();
		for(GraphNode node : pdg.nodes) {
			PDGNode pdgNode = (PDGNode)node;
			if(!remainingNodes.contains(pdgNode) && !indispensableNodes.contains(pdgNode))
				removableNodes.add(pdgNode);
		}
	}

	public List<PDGSlice> getSlices() {
		return slices;
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
		return ((TreeSet<PDGNode>)sliceNodes).first();
	}

	public AbstractVariable getLocalVariableCriterion() {
		return localVariableCriterion;
	}

	public Set<PDGNode> getSliceNodes() {
		if(this.sliceNodes != null)
			return this.sliceNodes;
		else {
			Set<PDGNode> sliceNodes = new TreeSet<PDGNode>();
			for(PDGSlice slice : slices) {
				sliceNodes.addAll(slice.getSliceNodes());
			}
			return sliceNodes;
		}
	}

	public Set<AbstractVariable> getPassedParameters() {
		return passedParameters;
	}

	public Set<PDGNode> getRemovableNodes() {
		return removableNodes;
	}

	public boolean declarationOfVariableCriterionBelongsToSliceNodes() {
		for(PDGNode node : sliceNodes) {
			if(node.declaresLocalVariable(localVariableCriterion))
				return true;
		}
		return false;
	}

	public boolean declarationOfVariableCriterionBelongsToRemovableNodes() {
		for(PDGNode node : removableNodes) {
			if(node.declaresLocalVariable(localVariableCriterion))
				return true;
		}
		return false;
	}

	private boolean sliceContainsDeclaration(AbstractVariable variableDeclaration) {
		for(PDGNode node : sliceNodes) {
			if(node.declaresLocalVariable(variableDeclaration))
				return true;
		}
		return false;
	}

	public boolean allNodeCriteriaAreDuplicated() {
		Set<PDGNode> duplicatedNodes = new LinkedHashSet<PDGNode>();
		duplicatedNodes.addAll(sliceNodes);
		duplicatedNodes.retainAll(indispensableNodes);
		for(PDGNode nodeCriterion : nodeCriteria) {
			if(!duplicatedNodes.contains(nodeCriterion))
				return false;
		}
		return true;
	}

	private boolean nonDuplicatedSliceNodeAntiDependsOnNonRemovableNode() {
		Set<PDGNode> duplicatedNodes = new LinkedHashSet<PDGNode>();
		duplicatedNodes.addAll(sliceNodes);
		duplicatedNodes.retainAll(indispensableNodes);
		for(PDGNode sliceNode : sliceNodes) {
			if(!duplicatedNodes.contains(sliceNode)) {
				for(GraphEdge edge : sliceNode.incomingEdges) {
					PDGDependence dependence = (PDGDependence)edge;
					if(subgraph.edgeBelongsToBlockBasedRegion(dependence) && dependence instanceof PDGAntiDependence) {
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

	private boolean duplicatedSliceNodeWithClassInstantiationHasDependenceOnRemovableNode() {
		Set<PDGNode> duplicatedNodes = new LinkedHashSet<PDGNode>();
		duplicatedNodes.addAll(sliceNodes);
		duplicatedNodes.retainAll(indispensableNodes);
		for(PDGNode duplicatedNode : duplicatedNodes) {
			if(duplicatedNode.containsClassInstanceCreation()) {
				Map<VariableDeclaration, ClassInstanceCreation> classInstantiations = duplicatedNode.getClassInstantiations();
				for(VariableDeclaration variableDeclaration : classInstantiations.keySet()) {
					for(GraphEdge edge : duplicatedNode.outgoingEdges) {
						PDGDependence dependence = (PDGDependence)edge;
						if(subgraph.edgeBelongsToBlockBasedRegion(dependence) && dependence instanceof PDGDependence) {
							PDGDependence dataDependence = (PDGDependence)dependence;
							PDGNode dstPDGNode = (PDGNode)dataDependence.dst;
							if(removableNodes.contains(dstPDGNode)) {
								if(dstPDGNode.changesStateOfReference(variableDeclaration) ||
										dstPDGNode.assignsReference(variableDeclaration) || dstPDGNode.accessesReference(variableDeclaration))
									return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private boolean containsDuplicateNodeWithStateChangingMethodInvocation() {
		Set<PDGNode> duplicatedNodes = new LinkedHashSet<PDGNode>();
		duplicatedNodes.addAll(sliceNodes);
		duplicatedNodes.retainAll(indispensableNodes);
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

	private boolean containsBreakContinueReturnSliceNode() {
		for(PDGNode node : sliceNodes) {
			Statement statement = node.getASTStatement();
			if(statement instanceof BreakStatement || statement instanceof ContinueStatement ||
					statement instanceof ReturnStatement)
				return true;
		}
		return false;
	}

	private boolean variableCriterionIsReturnedVariableInOriginalMethod() {
		if(pdg.getReturnedVariables().contains(localVariableCriterion))
			return true;
		return false;
	}

	private boolean sliceContainsOnlyOneNodeCriterionAndDeclarationOfVariableCriterion() {
		if(slices.size() == 1 && sliceNodes.size() == 2 &&
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
		
		int sliceSize = sliceNodes.size();
		int duplicatedSize = sliceSize - removableNodes.size();
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
		if(sliceContainsOnlyOneNodeCriterionAndDeclarationOfVariableCriterion() ||
				variableCriterionIsReturnedVariableInOriginalMethod() || (sliceNodes.size() <= nodeCriteria.size()) ||
				allNodeCriteriaAreDuplicated() || containsBreakContinueReturnSliceNode() ||
				containsDuplicateNodeWithStateChangingMethodInvocation() ||
				nonDuplicatedSliceNodeAntiDependsOnNonRemovableNode() ||
				duplicatedSliceNodeWithClassInstantiationHasDependenceOnRemovableNode() ||
				!complyWithUserThresholds())
			return false;
		return true;
	}
}
