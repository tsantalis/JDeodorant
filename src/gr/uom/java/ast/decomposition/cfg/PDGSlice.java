package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.AbstractMethodDeclaration;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class PDGSlice extends Graph {
	private PDG pdg;
	private AbstractMethodDeclaration method;
	private BasicBlock boundaryBlock;
	private PDGNode nodeCriterion;
	private AbstractVariable localVariableCriterion;
	private Set<PDGNode> sliceNodes;
	private Set<PDGNode> remainingNodes;
	private Set<AbstractVariable> passedParameters;
	private Set<PDGNode> indispensableNodes;
	private Set<PDGNode> removableNodes;
	private Set<AbstractVariable> returnedVariablesInOriginalMethod;
	private IFile iFile;
	private int methodSize;
	
	public PDGSlice(PDG pdg, BasicBlock boundaryBlock) {
		super();
		this.pdg = pdg;
		this.method = pdg.getMethod();
		this.iFile = pdg.getIFile();
		this.methodSize = pdg.getTotalNumberOfStatements();
		this.returnedVariablesInOriginalMethod = pdg.getReturnedVariables();
		this.boundaryBlock = boundaryBlock;
		Set<PDGNode> regionNodes = pdg.blockBasedRegion(boundaryBlock);
		for(PDGNode node : regionNodes) {
			nodes.add(node);
		}
		for(GraphEdge edge : pdg.edges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(nodes.contains(dependence.src) && nodes.contains(dependence.dst)) {
				if(dependence instanceof PDGDataDependence) {
					PDGDataDependence dataDependence = (PDGDataDependence)dependence;
					if(dataDependence.isLoopCarried()) {
						PDGNode loopNode = dataDependence.getLoop().getPDGNode();
						if(nodes.contains(loopNode))
							edges.add(dataDependence);
					}
					else
						edges.add(dataDependence);
				}
				else if(dependence instanceof PDGAntiDependence) {
					PDGAntiDependence antiDependence = (PDGAntiDependence)dependence;
					if(antiDependence.isLoopCarried()) {
						PDGNode loopNode = antiDependence.getLoop().getPDGNode();
						if(nodes.contains(loopNode))
							edges.add(antiDependence);
					}
					else
						edges.add(antiDependence);
				}
				else if(dependence instanceof PDGOutputDependence) {
					PDGOutputDependence outputDependence = (PDGOutputDependence)dependence;
					if(outputDependence.isLoopCarried()) {
						PDGNode loopNode = outputDependence.getLoop().getPDGNode();
						if(nodes.contains(loopNode))
							edges.add(outputDependence);
					}
					else
						edges.add(outputDependence);
				}
				else
					edges.add(dependence);
			}
		}
	}

	public PDGSlice(PDG pdg, BasicBlock boundaryBlock, PDGNode nodeCriterion, AbstractVariable localVariableCriterion) {
		this(pdg, boundaryBlock);
		this.nodeCriterion = nodeCriterion;
		this.localVariableCriterion = localVariableCriterion;
		this.sliceNodes = new TreeSet<PDGNode>();
		sliceNodes.addAll(computeSlice(nodeCriterion, localVariableCriterion));
		this.remainingNodes = new TreeSet<PDGNode>();
		remainingNodes.add(pdg.getEntryNode());
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
						!dataDependence.getData().equals(localVariableCriterion) && !dataDependence.getData().isField())
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
				Set<PDGNode> pSliceNodes = computeSlice(p, usedVariable);
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
				Set<PDGNode> pSliceNodes = computeSlice(p, definedVariable);
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

	public Set<VariableDeclaration> getVariableDeclarationsAndAccessedFieldsInMethod() {
		return pdg.getVariableDeclarationsAndAccessedFieldsInMethod();
	}

	public AbstractMethodDeclaration getMethod() {
		return method;
	}

	public IFile getIFile() {
		return iFile;
	}

	public int getMethodSize() {
		return methodSize;
	}

	public BasicBlock getBoundaryBlock() {
		return boundaryBlock;
	}

	public PDGNode getExtractedMethodInvocationInsertionNode() {
		return ((TreeSet<PDGNode>)sliceNodes).first();
	}

	public PDGNode getNodeCriterion() {
		return nodeCriterion;
	}

	public AbstractVariable getLocalVariableCriterion() {
		return localVariableCriterion;
	}

	public Set<PDGNode> getSliceNodes() {
		return sliceNodes;
	}

	public Set<AbstractVariable> getPassedParameters() {
		return passedParameters;
	}

	public Set<PDGNode> getRemovableNodes() {
		return removableNodes;
	}

	public boolean nodeBelongsToBlockBasedRegion(GraphNode node) {
		return nodes.contains(node);
	}

	public boolean edgeBelongsToBlockBasedRegion(GraphEdge edge) {
		return edges.contains(edge);
	}

	public PDGNode getDeclarationOfVariableCriterion() {
		PlainVariable plainVariable = localVariableCriterion.getInitialVariable();
		for(PDGNode pdgNode : sliceNodes) {
			if(pdgNode.declaresLocalVariable(plainVariable))
				return pdgNode;
		}
		return null;
	}

	public boolean declarationOfVariableCriterionBelongsToSliceNodes() {
		PlainVariable plainVariable = localVariableCriterion.getInitialVariable();
		for(PDGNode pdgNode : sliceNodes) {
			if(pdgNode.declaresLocalVariable(plainVariable))
				return true;
		}
		return false;
	}

	public boolean declarationOfVariableCriterionBelongsToRemovableNodes() {
		PlainVariable plainVariable = localVariableCriterion.getInitialVariable();
		for(PDGNode pdgNode : removableNodes) {
			if(pdgNode.declaresLocalVariable(plainVariable))
				return true;
		}
		return false;
	}

	private boolean nodeCriterionIsDuplicated() {
		Set<PDGNode> duplicatedNodes = new LinkedHashSet<PDGNode>();
		duplicatedNodes.addAll(sliceNodes);
		duplicatedNodes.retainAll(indispensableNodes);
		if(duplicatedNodes.contains(nodeCriterion))
			return true;
		return false;
	}

	public boolean satisfiesRules() {
		if(!nodeCritetionIsDeclarationOfVariableCriterion() && !nodeCriterionIsDuplicated() &&
				!declarationOfVariableCriterionIsDuplicated() && !sliceContainsReturnStatement() &&
				!variableCriterionIsReturnedVariableInOriginalMethod() &&
				!returnStatementIsControlDependentOnSliceNode() &&
				!containsDuplicateNodeWithStateChangingMethodInvocation() &&
				!nonDuplicatedSliceNodeAntiDependsOnNonRemovableNode() &&
				!nonDuplicatedSliceNodeOutputDependsOnNonRemovableNode() &&
				!duplicatedSliceNodeWithClassInstantiationHasDependenceOnRemovableNode() &&
				!sliceContainsBranchStatementWithoutInnermostLoop())
			return true;
		return false;
	}

	private boolean sliceContainsBranchStatementWithoutInnermostLoop() {
		for(PDGNode node : sliceNodes) {
			CFGNode cfgNode = node.getCFGNode();
			if(cfgNode instanceof CFGBreakNode) {
				CFGBreakNode breakNode = (CFGBreakNode)cfgNode;
				CFGNode innerMostLoopNode = breakNode.getInnerMostLoopNode();
				if(innerMostLoopNode != null && !sliceNodes.contains(innerMostLoopNode.getPDGNode()))
					return true;
			}
			else if(cfgNode instanceof CFGContinueNode) {
				CFGContinueNode continueNode = (CFGContinueNode)cfgNode;
				CFGNode innerMostLoopNode = continueNode.getInnerMostLoopNode();
				if(innerMostLoopNode != null && !sliceNodes.contains(innerMostLoopNode.getPDGNode()))
					return true;
			}
		}
		return false;
	}

	private boolean sliceContainsReturnStatement() {
		for(PDGNode node : sliceNodes) {
			if(node.getCFGNode() instanceof CFGExitNode)
				return true;
		}
		return false;
	}

	private boolean declarationOfVariableCriterionIsDuplicated() {
		Set<PDGNode> duplicatedNodes = new LinkedHashSet<PDGNode>();
		duplicatedNodes.addAll(sliceNodes);
		duplicatedNodes.retainAll(indispensableNodes);
		for(PDGNode node : duplicatedNodes) {
			if(node.declaresLocalVariable(localVariableCriterion) && !(node instanceof PDGTryNode))
				return true;
		}
		return false;
	}

	private boolean returnStatementIsControlDependentOnSliceNode() {
		for(GraphNode node : pdg.nodes) {
			PDGNode pdgNode = (PDGNode)node;
			if(pdgNode.getCFGNode() instanceof CFGExitNode) {
				if(isControlDependentOnSliceNode(pdgNode))
					return true;
				if(sliceNodes.contains(pdgNode))
					return true;
			}
		}
		return false;
	}

	private boolean isControlDependentOnSliceNode(PDGNode node) {
		for(GraphEdge edge : node.incomingEdges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(dependence instanceof PDGControlDependence) {
				PDGControlDependence controlDependence = (PDGControlDependence)dependence;
				PDGNode srcPDGNode = (PDGNode)controlDependence.src;
				if(sliceNodes.contains(srcPDGNode))
					return true;
				else
					return isControlDependentOnSliceNode(srcPDGNode);
			}
		}
		return false;
	}

	private boolean nonDuplicatedSliceNodeAntiDependsOnNonRemovableNode() {
		Set<PDGNode> duplicatedNodes = new LinkedHashSet<PDGNode>();
		duplicatedNodes.addAll(sliceNodes);
		duplicatedNodes.retainAll(indispensableNodes);
		for(PDGNode sliceNode : sliceNodes) {
			if(!duplicatedNodes.contains(sliceNode)) {
				for(GraphEdge edge : sliceNode.incomingEdges) {
					PDGDependence dependence = (PDGDependence)edge;
					if(edges.contains(dependence) && dependence instanceof PDGAntiDependence) {
						PDGAntiDependence antiDependence = (PDGAntiDependence)dependence;
						PDGNode srcPDGNode = (PDGNode)antiDependence.src;
						if(!removableNodes.contains(srcPDGNode) && !nodeDependsOnNonRemovableNode(srcPDGNode, antiDependence.getData()))
							return true;
					}
				}
			}
		}
		return false;
	}

	private boolean nodeDependsOnNonRemovableNode(PDGNode node, AbstractVariable variable) {
		for(GraphEdge edge : node.incomingEdges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(edges.contains(dependence) && dependence instanceof PDGDataDependence) {
				PDGDataDependence dataDependence = (PDGDataDependence)dependence;
				if(dataDependence.getData().equals(variable)) {
					PDGNode srcPDGNode = (PDGNode)dataDependence.src;
					if(!removableNodes.contains(srcPDGNode))
						return true;
				}
			}
		}
		return false;
	}

	private boolean nonDuplicatedSliceNodeOutputDependsOnNonRemovableNode() {
		Set<PDGNode> duplicatedNodes = new LinkedHashSet<PDGNode>();
		duplicatedNodes.addAll(sliceNodes);
		duplicatedNodes.retainAll(indispensableNodes);
		for(PDGNode sliceNode : sliceNodes) {
			if(!duplicatedNodes.contains(sliceNode)) {
				for(GraphEdge edge : sliceNode.incomingEdges) {
					PDGDependence dependence = (PDGDependence)edge;
					if(edges.contains(dependence) && dependence instanceof PDGOutputDependence) {
						PDGOutputDependence outputDependence = (PDGOutputDependence)dependence;
						PDGNode srcPDGNode = (PDGNode)outputDependence.src;
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
						if(edges.contains(dependence) && dependence instanceof PDGDependence) {
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

	private boolean nodeCritetionIsDeclarationOfVariableCriterion() {
		if(nodeCriterion.declaresLocalVariable(localVariableCriterion))
			return true;
		return false;
	}

	private boolean variableCriterionIsReturnedVariableInOriginalMethod() {
		if(returnedVariablesInOriginalMethod.contains(localVariableCriterion))
			return true;
		return false;
	}

	private boolean containsDuplicateNodeWithStateChangingMethodInvocation() {
		Set<PDGNode> duplicatedNodes = new LinkedHashSet<PDGNode>();
		duplicatedNodes.addAll(sliceNodes);
		duplicatedNodes.retainAll(indispensableNodes);
		for(PDGNode node : duplicatedNodes) {
			for(AbstractVariable stateChangingVariable : node.definedVariables) {
				if(stateChangingVariable instanceof CompositeVariable) {
					PlainVariable plainVariable = stateChangingVariable.getInitialVariable();
					if(!sliceContainsDeclaration(plainVariable))
						return true;
				}
				else if(stateChangingVariable instanceof PlainVariable) {
					PlainVariable plainVariable = stateChangingVariable.getInitialVariable();
					if(plainVariable.isField())
						return true;
				}
			}
		}
		return false;
	}

	private boolean sliceContainsDeclaration(AbstractVariable variableDeclaration) {
		for(PDGNode pdgNode : sliceNodes) {
			if(pdgNode.declaresLocalVariable(variableDeclaration))
				return true;
		}
		return false;
	}

	public Set<PDGNode> computeSlice(PDGNode nodeCriterion, AbstractVariable localVariableCriterion) {
		Set<PDGNode> sliceNodes = new LinkedHashSet<PDGNode>();
		if(nodeCriterion.definesLocalVariable(localVariableCriterion)) {
			sliceNodes.addAll(traverseBackward(nodeCriterion, new LinkedHashSet<PDGNode>()));
		}
		else if(nodeCriterion.usesLocalVariable(localVariableCriterion)) {
			Set<PDGNode> defNodes = getDefNodes(nodeCriterion, localVariableCriterion);
			for(PDGNode defNode : defNodes) {
				sliceNodes.addAll(traverseBackward(defNode, new LinkedHashSet<PDGNode>()));
			}
			sliceNodes.addAll(traverseBackward(nodeCriterion, new LinkedHashSet<PDGNode>()));
		}
		return sliceNodes;
	}

	public Set<PDGNode> computeSlice(PDGNode nodeCriterion) {
		Set<PDGNode> sliceNodes = new LinkedHashSet<PDGNode>();
		sliceNodes.addAll(traverseBackward(nodeCriterion, new LinkedHashSet<PDGNode>()));
		return sliceNodes;
	}

	private Set<PDGNode> getDefNodes(PDGNode node, AbstractVariable localVariable) {
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
			if(edges.contains(dependence) && !(dependence instanceof PDGAntiDependence) && !(dependence instanceof PDGOutputDependence)) {
				PDGNode srcPDGNode = (PDGNode)dependence.src;
				if(!visitedNodes.contains(srcPDGNode))
					sliceNodes.addAll(traverseBackward(srcPDGNode, visitedNodes));
			}
		}
		return sliceNodes;
	}

	public String toString() {
		return "<" + localVariableCriterion + ", " + nodeCriterion.getId() + "> [B" + boundaryBlock.getId() + "]\n" +
		sliceNodes + "\npassed parameters: " + passedParameters + "\nindispensable nodes: " + indispensableNodes;
	}
}
