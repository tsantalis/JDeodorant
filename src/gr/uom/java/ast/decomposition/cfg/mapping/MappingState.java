package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.AbstractMethodFragment;
import gr.uom.java.ast.decomposition.StatementObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CFGBranchIfNode;
import gr.uom.java.ast.decomposition.cfg.GraphEdge;
import gr.uom.java.ast.decomposition.cfg.PDGAbstractDataDependence;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGMethodEntryNode;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.matching.Difference;
import gr.uom.java.ast.decomposition.matching.DifferenceType;
import gr.uom.java.ast.util.math.LevenshteinDistance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class MappingState {
	private List<MappingState> children;
	private Set<PDGNodeMapping> nodeMappings;
	private Set<PDGDependence> visitedEdgesG1;
	private static Set<PDGNode> restrictedNodesG1;
	private static Set<PDGNode> restrictedNodesG2;
	
	public MappingState(MappingState parent, PDGNodeMapping nodeMapping) {
		this.children = new ArrayList<MappingState>();
		this.nodeMappings = new LinkedHashSet<PDGNodeMapping>();
		this.visitedEdgesG1 = new LinkedHashSet<PDGDependence>();
		if(parent != null) {
			this.nodeMappings.addAll(parent.nodeMappings);
			this.visitedEdgesG1.addAll(parent.visitedEdgesG1);
		}
		this.nodeMappings.add(nodeMapping);
	}

	public static void setRestrictedNodesG1(Set<PDGNode> restrictedNodesG1) {
		MappingState.restrictedNodesG1 = restrictedNodesG1;
	}

	public static void setRestrictedNodesG2(Set<PDGNode> restrictedNodesG2) {
		MappingState.restrictedNodesG2 = restrictedNodesG2;
	}

	public void addChild(MappingState state) {
		children.add(state);
	}

	public TreeSet<PDGNode> getMappedNodesG1() {
		TreeSet<PDGNode> nodesG1 = new TreeSet<PDGNode>();
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			nodesG1.add(nodeMapping.getNodeG1());
		}
		return nodesG1;
	}

	public TreeSet<PDGNode> getMappedNodesG2() {
		TreeSet<PDGNode> nodesG2 = new TreeSet<PDGNode>();
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			nodesG2.add(nodeMapping.getNodeG2());
		}
		return nodesG2;
	}

	public List<ASTNodeDifference> getNodeDifferences() {
		List<ASTNodeDifference> nodeDifferences = new ArrayList<ASTNodeDifference>();
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			nodeDifferences.addAll(nodeMapping.getNodeDifferences());
		}
		return nodeDifferences;
	}

	public List<ASTNodeDifference> getSortedNodeDifferences() {
		List<ASTNodeDifference> nodeDifferences = new ArrayList<ASTNodeDifference>();
		for(PDGNodeMapping nodeMapping : getSortedNodeMappings()) {
			nodeDifferences.addAll(nodeMapping.getNodeDifferences());
		}
		return nodeDifferences;
	}

	public List<ASTNodeDifference> getNonOverlappingNodeDifferences() {
		List<ASTNodeDifference> nodeDifferences = new ArrayList<ASTNodeDifference>();
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			nodeDifferences.addAll(nodeMapping.getNonOverlappingNodeDifferences());
		}
		return nodeDifferences;
	}

	public int getDistinctDifferenceCount() {
		Set<Difference> differences = new LinkedHashSet<Difference>();
		int count = 0;
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			for(ASTNodeDifference difference : nodeMapping.getNodeDifferences()) {
				for(Difference diff : difference.getDifferences()) {
					if(!diff.getType().equals(DifferenceType.VARIABLE_TYPE_MISMATCH) && !diff.getType().equals(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
						if(!differences.contains(diff)) {
							differences.add(diff);
							count += diff.getWeight();
						}
					}
				}
			}
		}
		return count;
	}

	public int getDistinctDifferenceCountIncludingTypeMismatches() {
		Set<Difference> differences = new LinkedHashSet<Difference>();
		int count = 0;
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			for(ASTNodeDifference difference : nodeMapping.getNodeDifferences()) {
				for(Difference diff : difference.getDifferences()) {
					if(!differences.contains(diff)) {
						differences.add(diff);
						count += diff.getWeight();
					}
				}
			}
		}
		return count;
	}

	public int getNonDistinctDifferenceCount() {
		int count = 0;
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			for(ASTNodeDifference difference : nodeMapping.getNodeDifferences()) {
				for(Difference diff : difference.getDifferences()) {
					if(!diff.getType().equals(DifferenceType.VARIABLE_TYPE_MISMATCH) && !diff.getType().equals(DifferenceType.SUBCLASS_TYPE_MISMATCH))
						count += diff.getWeight();
				}
			}
		}
		return count;
	}

	public int getNonDistinctDifferenceCountIncludingTypeMismatches() {
		int count = 0;
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			for(ASTNodeDifference difference : nodeMapping.getNodeDifferences()) {
				for(Difference diff : difference.getDifferences()) {
					count += diff.getWeight();
				}
			}
		}
		return count;
	}

	//returns the sum of the differences in the node Ids of the mapped nodes
	public int getNodeMappingRelativeIdDiff(int minId1, int minId2) {
		int sum = 0;
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			sum += Math.abs((nodeMapping.getNodeG1().getId()-minId1) - (nodeMapping.getNodeG2().getId()-minId2));
		}
		return sum;
	}

	public int getEditDistanceOfDifferences() {
		int sum = 0;
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			for(ASTNodeDifference difference : nodeMapping.getNodeDifferences()) {
				for(Difference diff : difference.getDifferences()) {
					if(diff.getType().equals(DifferenceType.VARIABLE_NAME_MISMATCH) ||
							diff.getType().equals(DifferenceType.METHOD_INVOCATION_NAME_MISMATCH) ||
							diff.getType().equals(DifferenceType.LITERAL_VALUE_MISMATCH)) {
						sum += LevenshteinDistance.computeLevenshteinDistance(diff.getFirstValue(), diff.getSecondValue());
					}
				}
			}
		}
		return sum;
	}

	public List<MappingState> getMaximumCommonSubGraphs() {
		List<MappingState> leaves = this.getLeaves();
		int max = 0;
		List<MappingState> maximumStates = new ArrayList<MappingState>();
		for(MappingState state : leaves) {
			if(state.getSize() > max) {
				max = state.getSize();
				clear(maximumStates, max);
				maximumStates.add(state);
			}
			else if(state.getSize() == max) {
				if(!containsSameState(maximumStates, state))
					maximumStates.add(state);
			}
		}
		return maximumStates;
	}

	private void clear(List<MappingState> maximumStates, int max) {
		List<MappingState> keepStates = new ArrayList<MappingState>();
		for(MappingState state : maximumStates) {
			if(state.getSize() == max-1) {
				keepStates.add(state);
			}
		}
		maximumStates.clear();
		maximumStates.addAll(keepStates);
	}

	private boolean containsSameState(List<MappingState> states, MappingState state) {
		for(MappingState oldState : states) {
			if(oldState.sameNodeMappings(state))
				return true;
		}
		return false;
	}

	private List<MappingState> getLeaves() {
		List<MappingState> list = new ArrayList<MappingState>();
		if(this.children.isEmpty()) {
			list.add(this);
		}
		else {
			for(MappingState childState : this.children) {
				list.addAll(childState.getLeaves());
			}
		}
		return list;
	}

	public int getNodeMappingSize() {
		int size = 0;
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			size++;
			//TODO fix the case where the a statement is matched in both clones, count only once
			for(AbstractMethodFragment fragment : nodeMapping.getAdditionallyMatchedFragments1()) {
				if(fragment instanceof StatementObject) {
					size++;
				}
			}
			for(AbstractMethodFragment fragment : nodeMapping.getAdditionallyMatchedFragments2()) {
				if(fragment instanceof StatementObject) {
					size++;
				}
			}
		}
		return size;
	}

	public int getSize() {
		return getNodeMappingSize();
	}

	public void traverse(PDGNodeMapping initialNodeMapping) {
		PDGNode nodeG1 = initialNodeMapping.getNodeG1();
		PDGNode nodeG2 = initialNodeMapping.getNodeG2();
		Iterator<GraphEdge> nodeG1EdgeIterator = nodeG1.getDependenceIterator();
		while(nodeG1EdgeIterator.hasNext()) {
			PDGDependence edgeG1 = (PDGDependence)nodeG1EdgeIterator.next();
			if(!visitedEdgesG1.contains(edgeG1)) {
				visitedEdgesG1.add(edgeG1);
				Set<PDGEdgeMapping> edgeMappings = new HashSet<PDGEdgeMapping>();
				Iterator<GraphEdge> nodeG2EdgeIterator = nodeG2.getDependenceIterator();
				while(nodeG2EdgeIterator.hasNext()) {
					PDGDependence edgeG2 = (PDGDependence)nodeG2EdgeIterator.next();
					PDGEdgeMapping edgeMapping = new PDGEdgeMapping(edgeG1, edgeG2);
					if(!edgeMappingWithAlreadyVisitedNodes(edgeMappings, edgeMapping) && edgeMapping.isCompatible(initialNodeMapping)) {
						edgeMappings.add(edgeMapping);
						PDGNode dstNodeG1 = null;
						PDGNode dstNodeG2 = null;
						boolean symmetricalIfNodes = false;
						if(edgeG1.getSrc().equals(nodeG1) && edgeG2.getSrc().equals(nodeG2)) {
							//get destination nodes if the edge is outgoing
							dstNodeG1 = (PDGNode)edgeG1.getDst();
							dstNodeG2 = (PDGNode)edgeG2.getDst();
						}
						else if(edgeG1.getDst().equals(nodeG1) && edgeG2.getDst().equals(nodeG2)) {
							//get source nodes if the edge is incoming
							dstNodeG1 = (PDGNode)edgeG1.getSrc();
							dstNodeG2 = (PDGNode)edgeG2.getSrc();
						}
						else {
							if(edgeG1.getSrc().equals(nodeG1)) {
								dstNodeG1 = (PDGNode)edgeG1.getDst();
							}
							else {
								dstNodeG1 = (PDGNode)edgeG1.getSrc();
							}
							if(edgeG2.getSrc().equals(nodeG2)) {
								dstNodeG2 = (PDGNode)edgeG2.getDst();
							}
							else {
								dstNodeG2 = (PDGNode)edgeG2.getSrc();
							}
							if(!symmetricalIfNodes(nodeG1, nodeG2, dstNodeG1, dstNodeG2)) {
								dstNodeG1 = null;
								dstNodeG2 = null;
							}
							else {
								symmetricalIfNodes = true;
							}
						}
						if(dstNodeG1 != null && dstNodeG2 != null && restrictedNodesG1.contains(dstNodeG1) && restrictedNodesG2.contains(dstNodeG2)) {
							ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(initialNodeMapping.getTypeRoot1(), initialNodeMapping.getTypeRoot2());
							boolean match;
							if(dstNodeG1 instanceof PDGMethodEntryNode || dstNodeG2 instanceof PDGMethodEntryNode)
								match = false;
							else 
								match = astNodeMatcher.match(dstNodeG1, dstNodeG2);
							if(match && astNodeMatcher.isParameterizable() && (mappedControlParents(dstNodeG1, dstNodeG2) || symmetricalIfNodes) && nodesDeclareVariableUsedInMappedNodes(dstNodeG1, dstNodeG2)) {
								PDGNodeMapping dstNodeMapping = new PDGNodeMapping(dstNodeG1, dstNodeG2, astNodeMatcher);
								if(symmetricalIfNodes) {
									dstNodeMapping.setSymmetricalIfNodePair(initialNodeMapping);
								}
								if(!this.containsAtLeastOneNodeInMappings(dstNodeMapping) && this.getChildStateWithNodeMapping(dstNodeMapping) == null) {
									MappingState newMappingState = new MappingState(this, dstNodeMapping);
									boolean pruneBranch = pruneBranch(newMappingState);
									if(!pruneBranch) {
										this.children.add(newMappingState);
										newMappingState.traverse(dstNodeMapping);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean nodesDeclareVariableUsedInMappedNodes(PDGNode dstNodeG1, PDGNode dstNodeG2) {
		boolean nodeG1DeclaresVariableUsedInMappedNodes = false;
		Iterator<AbstractVariable> declaredVariableIteratorG1 = dstNodeG1.getDeclaredVariableIterator();
		while(declaredVariableIteratorG1.hasNext()) {
			AbstractVariable variable = declaredVariableIteratorG1.next();
			for(PDGNodeMapping mapping : nodeMappings) {
				if(mapping.getNodeG1().definesLocalVariable(variable) || mapping.getNodeG1().usesLocalVariable(variable)) {
					nodeG1DeclaresVariableUsedInMappedNodes = true;
					break;
				}
			}
		}
		boolean nodeG2DeclaresVariableUsedInMappedNodes = false;
		Iterator<AbstractVariable> declaredVariableIteratorG2 = dstNodeG2.getDeclaredVariableIterator();
		while(declaredVariableIteratorG2.hasNext()) {
			AbstractVariable variable = declaredVariableIteratorG2.next();
			for(PDGNodeMapping mapping : nodeMappings) {
				if(mapping.getNodeG2().definesLocalVariable(variable) || mapping.getNodeG2().usesLocalVariable(variable)) {
					nodeG2DeclaresVariableUsedInMappedNodes = true;
					break;
				}
			}
		}
		return nodeG1DeclaresVariableUsedInMappedNodes == nodeG2DeclaresVariableUsedInMappedNodes;
	}

	private boolean edgeMappingWithAlreadyVisitedNodes(Set<PDGEdgeMapping> edgeMappings, PDGEdgeMapping edgeMapping) {
		PDGNode srcEdgeG1 = (PDGNode)edgeMapping.getEdgeG1().getSrc();
		PDGNode srcEdgeG2 = (PDGNode)edgeMapping.getEdgeG2().getSrc();
		PDGNode dstEdgeG1 = (PDGNode)edgeMapping.getEdgeG1().getDst();
		PDGNode dstEdgeG2 = (PDGNode)edgeMapping.getEdgeG2().getDst();
		for(PDGEdgeMapping mapping : edgeMappings) {
			PDGNode srcG1 = (PDGNode)mapping.getEdgeG1().getSrc();
			PDGNode srcG2 = (PDGNode)mapping.getEdgeG2().getSrc();
			PDGNode dstG1 = (PDGNode)mapping.getEdgeG1().getDst();
			PDGNode dstG2 = (PDGNode)mapping.getEdgeG2().getDst();
			if(srcG1.equals(srcEdgeG1) && srcG2.equals(srcEdgeG2) &&
					dstG1.equals(dstEdgeG1) && dstG2.equals(dstEdgeG2))
				return true;
		}
		return false;
	}
	
	private boolean pruneBranch(MappingState state) {
		List<MappingState> leaves = this.getLeaves();
		Set<PDGNodeMapping> stateNodeMappings = state.getNodeMappings();
		for(MappingState leaf : leaves) {
			Set<PDGNodeMapping> leafNodeMappings = leaf.getNodeMappings();
			if(leafNodeMappings.containsAll(stateNodeMappings))
				return true;
		}
		return false;
	}

	private boolean symmetricalIfNodes(PDGNode nodeG1, PDGNode nodeG2, PDGNode dstNodeG1, PDGNode dstNodeG2) {
		PDGNode nodeG1ControlParent = nodeG1.getControlDependenceParent();
		PDGNode nodeG2ControlParent = nodeG2.getControlDependenceParent();
		PDGNode dstNodeG1ControlParent = dstNodeG1.getControlDependenceParent();
		PDGNode dstNodeG2ControlParent = dstNodeG2.getControlDependenceParent();
		if(((dstNodeG1ControlParent != null && dstNodeG1ControlParent.equals(nodeG1) && nodeG2ControlParent != null && nodeG2ControlParent.equals(dstNodeG2)) ||
				(dstNodeG2ControlParent != null && dstNodeG2ControlParent.equals(nodeG2) && nodeG1ControlParent != null && nodeG1ControlParent.equals(dstNodeG1))) &&
				dstNodeG1.getCFGNode() instanceof CFGBranchIfNode && dstNodeG2.getCFGNode() instanceof CFGBranchIfNode) {
			return true;
		}
		return false;
	}

	public boolean mappedControlParents(PDGNode dstNodeG1, PDGNode dstNodeG2) {
		PDGNode nodeG1ControlParent = dstNodeG1.getControlDependenceParent();
		PDGNode nodeG2ControlParent = dstNodeG2.getControlDependenceParent();
		PDGControlDependence nodeG1IncomingControlDependence = dstNodeG1.getIncomingControlDependence();
		PDGControlDependence nodeG2IncomingControlDependence = dstNodeG2.getIncomingControlDependence();
		PDGNodeMapping parentNodeMapping = findMappingWithBothNodes(nodeG1ControlParent, nodeG2ControlParent);
		if(this.containsBothNodesInMappings(nodeG1ControlParent, nodeG2ControlParent)
				&& (nodeG1IncomingControlDependence.sameLabel(nodeG2IncomingControlDependence) || parentNodeMapping.isSymmetricalIfElse()))
			return true;
		if(!this.containsNodeG1InMappings(nodeG1ControlParent) && !this.containsNodeG2InMappings(nodeG2ControlParent))
			return true;
		//special case
		//if(dstNodeG1.getCFGNode() instanceof CFGBranchIfNode && dstNodeG2.getCFGNode() instanceof CFGBranchIfNode) {
			if(nodeG1ControlParent.getCFGNode() instanceof CFGBranchIfNode && nodeG1IncomingControlDependence.isFalseControlDependence()) {
				PDGNode nodeG1ControlGrandParent = nodeG1ControlParent.getControlDependenceParent();
				PDGControlDependence nodeG1ParentIncomingControlDependence = nodeG1ControlParent.getIncomingControlDependence();
				if(nodeG1ControlGrandParent != null && this.containsBothNodesInMappings(nodeG1ControlGrandParent, nodeG2ControlParent)
						&& nodeG1ParentIncomingControlDependence.sameLabel(nodeG2IncomingControlDependence))
					return true;
			}
			if(nodeG2ControlParent.getCFGNode() instanceof CFGBranchIfNode && nodeG2IncomingControlDependence.isFalseControlDependence()) {
				PDGNode nodeG2ControlGrandParent = nodeG2ControlParent.getControlDependenceParent();
				PDGControlDependence nodeG2ParentIncomingControlDependence = nodeG2ControlParent.getIncomingControlDependence();
				if(nodeG2ControlGrandParent != null && this.containsBothNodesInMappings(nodeG1ControlParent, nodeG2ControlGrandParent)
						&& nodeG1IncomingControlDependence.sameLabel(nodeG2ParentIncomingControlDependence))
					return true;
			}
		//}
		return false;
	}

	public boolean incomingDataDependenciesFromUnvisitedNodes(PDGNodeMapping mapping) {
		PDGNode nodeG1 = mapping.getNodeG1();
		PDGNode nodeG2 = mapping.getNodeG2();
		Iterator<GraphEdge> incomingEdgeIterator1 = nodeG1.getIncomingDependenceIterator();
		boolean incomingDataDependenceFromUnvisitedNodeG1 = false;
		while(incomingEdgeIterator1.hasNext()) {
			PDGDependence dependence = (PDGDependence)incomingEdgeIterator1.next();
			PDGNode srcPDGNode = (PDGNode)dependence.getSrc();
			if(dependence instanceof PDGAbstractDataDependence && restrictedNodesG1.contains(srcPDGNode)) {
				PDGAbstractDataDependence dataDependence = (PDGAbstractDataDependence)dependence;
				PDGNode nodeDeclaringVariable = findNodeDeclaringVariable(dataDependence.getData(), srcPDGNode, getMappedNodesG1());
				boolean containsDeclaringNodeG1InMappings = nodeDeclaringVariable != null ? containsNodeG1InMappings(nodeDeclaringVariable) : false;
				if(!containsNodeG1InMappings(srcPDGNode) && !containsDeclaringNodeG1InMappings &&
						!nodeIsUnmappedTemporaryVariableDeclaration(srcPDGNode, dataDependence.getData(), mapping, getMappedNodesG1())) {
					incomingDataDependenceFromUnvisitedNodeG1 = true;
					break;
				}
			}
		}
		
		Iterator<GraphEdge> incomingEdgeIterator2 = nodeG2.getIncomingDependenceIterator();
		boolean incomingDataDependenceFromUnvisitedNodeG2 = false;
		while(incomingEdgeIterator2.hasNext()) {
			PDGDependence dependence = (PDGDependence)incomingEdgeIterator2.next();
			PDGNode srcPDGNode = (PDGNode)dependence.getSrc();
			if(dependence instanceof PDGAbstractDataDependence && restrictedNodesG2.contains(srcPDGNode)) {
				PDGAbstractDataDependence dataDependence = (PDGAbstractDataDependence)dependence;
				PDGNode nodeDeclaringVariable = findNodeDeclaringVariable(dataDependence.getData(), srcPDGNode, getMappedNodesG2());
				boolean containsDeclaringNodeG2InMappings = nodeDeclaringVariable != null ? containsNodeG2InMappings(nodeDeclaringVariable) : false;
				if(!containsNodeG2InMappings(srcPDGNode) && !containsDeclaringNodeG2InMappings &&
						!nodeIsUnmappedTemporaryVariableDeclaration(srcPDGNode, dataDependence.getData(), mapping, getMappedNodesG2())) {
					incomingDataDependenceFromUnvisitedNodeG2 = true;
					break;
				}
			}
		}
		return incomingDataDependenceFromUnvisitedNodeG1 != incomingDataDependenceFromUnvisitedNodeG2 &&
				restrictedNodesG1.size() == restrictedNodesG2.size();
	}

	private boolean nodeIsUnmappedTemporaryVariableDeclaration(PDGNode srcPDGNode, AbstractVariable variable, PDGNodeMapping mapping, Set<PDGNode> mappedNodes) {
		if(srcPDGNode.declaresLocalVariable(variable.getInitialVariable()) && !mappedNodes.contains(srcPDGNode)) {
			if(srcPDGNode.getASTStatement() instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)srcPDGNode.getASTStatement();
				List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
				for(VariableDeclarationFragment fragment : fragments) {
					if(fragment.resolveBinding().getKey().equals(variable.getInitialVariable().getVariableBindingKey())) {
						String initializer = fragment.getInitializer().toString();
						for(ASTNodeDifference difference : mapping.getNodeDifferences()) {
							String expr1 = difference.getExpression1().toString();
							String expr2 = difference.getExpression2().toString();
							if(expr1.contains(initializer) || expr2.contains(initializer)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	public boolean incomingDataDependenciesFromNonMatchingNodes(PDGNode nodeG1, PDGNode nodeG2) {
		TreeSet<PDGNode> incomingDataDependenciesFromMappedNodesG1 = new TreeSet<PDGNode>();
		Iterator<GraphEdge> incomingEdgeIterator1 = nodeG1.getIncomingDependenceIterator();
		while(incomingEdgeIterator1.hasNext()) {
			PDGDependence dependence = (PDGDependence)incomingEdgeIterator1.next();
			PDGNode srcPDGNode = (PDGNode)dependence.getSrc();
			if(dependence instanceof PDGAbstractDataDependence && restrictedNodesG1.contains(srcPDGNode)) {
				if(containsNodeG1InMappings(srcPDGNode)) {
					incomingDataDependenciesFromMappedNodesG1.add(srcPDGNode);
				}
			}
		}
		
		TreeSet<PDGNode> incomingDataDependenciesFromMappedNodesG2 = new TreeSet<PDGNode>();
		Iterator<GraphEdge> incomingEdgeIterator2 = nodeG2.getIncomingDependenceIterator();
		while(incomingEdgeIterator2.hasNext()) {
			PDGDependence dependence = (PDGDependence)incomingEdgeIterator2.next();
			PDGNode srcPDGNode = (PDGNode)dependence.getSrc();
			if(dependence instanceof PDGAbstractDataDependence && restrictedNodesG2.contains(srcPDGNode)) {
				if(containsNodeG2InMappings(srcPDGNode)) {
					incomingDataDependenciesFromMappedNodesG2.add(srcPDGNode);
				}
			}
		}
		
		if(incomingDataDependenciesFromMappedNodesG1.size() == 1 && incomingDataDependenciesFromMappedNodesG2.size() == 1) {
			if(!containsBothNodesInMappings(incomingDataDependenciesFromMappedNodesG1.first(), incomingDataDependenciesFromMappedNodesG2.first())) {
				return true;
			}
		}
		else if(incomingDataDependenciesFromMappedNodesG1.size() == 1 && incomingDataDependenciesFromMappedNodesG2.size() == 0) {
			PDGNodeMapping nodeMapping = findMappingWithNodeG1(incomingDataDependenciesFromMappedNodesG1.first());
			PDGNode otherNode = nodeMapping.getNodeG2();
			Iterator<GraphEdge> outgoingEdgeIterator = otherNode.getOutgoingDependenceIterator();
			while(outgoingEdgeIterator.hasNext()) {
				PDGDependence dependence = (PDGDependence)outgoingEdgeIterator.next();
				PDGNode dstPDGNode = (PDGNode)dependence.getDst();
				if(dependence instanceof PDGAbstractDataDependence && restrictedNodesG2.contains(dstPDGNode)) {
					if(!containsNodeG2InMappings(dstPDGNode)) {
						return true;
					}
				}
			}
		}
		else if(incomingDataDependenciesFromMappedNodesG1.size() == 0 && incomingDataDependenciesFromMappedNodesG2.size() == 1) {
			PDGNodeMapping nodeMapping = findMappingWithNodeG2(incomingDataDependenciesFromMappedNodesG2.first());
			PDGNode otherNode = nodeMapping.getNodeG1();
			Iterator<GraphEdge> outgoingEdgeIterator = otherNode.getOutgoingDependenceIterator();
			while(outgoingEdgeIterator.hasNext()) {
				PDGDependence dependence = (PDGDependence)outgoingEdgeIterator.next();
				PDGNode dstPDGNode = (PDGNode)dependence.getDst();
				if(dependence instanceof PDGAbstractDataDependence && restrictedNodesG1.contains(dstPDGNode)) {
					if(!containsNodeG1InMappings(dstPDGNode)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private PDGNode findNodeDeclaringVariable(AbstractVariable variable, PDGNode srcPDGNode, Set<PDGNode> nodes) {
		for(PDGNode node : nodes) {
			if(node.declaresLocalVariable(variable.getInitialVariable()) && node.getId() != srcPDGNode.getId()) {
				return node;
			}
		}
		return null;
	}

	private PDGNodeMapping findMappingWithBothNodes(PDGNode nodeG1, PDGNode nodeG2) {
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			if(nodeMapping.getNodeG1().equals(nodeG1) &&
					nodeMapping.getNodeG2().equals(nodeG2)) {
				return nodeMapping;
			}
		}
		return null;
	}

	private PDGNodeMapping findMappingWithNodeG1(PDGNode nodeG1) {
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			if(nodeMapping.getNodeG1().equals(nodeG1)) {
				return nodeMapping;
			}
		}
		return null;
	}

	private PDGNodeMapping findMappingWithNodeG2(PDGNode nodeG2) {
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			if(nodeMapping.getNodeG2().equals(nodeG2)) {
				return nodeMapping;
			}
		}
		return null;
	}

	private boolean containsBothNodesInMappings(PDGNode nodeG1, PDGNode nodeG2) {
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			if(nodeMapping.getNodeG1().equals(nodeG1) &&
					nodeMapping.getNodeG2().equals(nodeG2)) {
				return true;
			}
		}
		return false;
	}

	private boolean containsNodeG1InMappings(PDGNode nodeG1) {
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			if(nodeMapping.getNodeG1().equals(nodeG1))
				return true;
		}
		return false;
	}

	private boolean containsNodeG2InMappings(PDGNode nodeG2) {
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			if(nodeMapping.getNodeG2().equals(nodeG2))
				return true;
		}
		return false;
	}

	private MappingState getChildStateWithNodeMapping(PDGNodeMapping nodeMapping) {
		for(MappingState state : children) {
			if(state.nodeMappings.contains(nodeMapping))
				return state;
		}
		return null;
	}
	
	public boolean containsAtLeastOneNodeInMappings(PDGNodeMapping dstNodeMapping) {
		Set<PDGNodeMapping> nodeMappings = getNodeMappings();
		for(PDGNodeMapping nodeMapping : nodeMappings) {
			if(nodeMapping.getNodeG1().equals(dstNodeMapping.getNodeG1()) ||
					nodeMapping.getNodeG2().equals(dstNodeMapping.getNodeG2()))
				return true;
		}
		return false;
	}
	
	public boolean containsNodeInMappings(PDGNode node) {
		Set<PDGNodeMapping> nodeMappings = getNodeMappings();
		for(PDGNodeMapping nodeMapping : nodeMappings) {
			if(nodeMapping.getNodeG1().equals(node) || nodeMapping.getNodeG2().equals(node))
				return true;
		}
		return false;
	}

	public Set<PDGNodeMapping> getNodeMappings() {
		return this.nodeMappings;
	}

	public TreeSet<PDGNodeMapping> getSortedNodeMappings() {
		return new TreeSet<PDGNodeMapping>(this.nodeMappings);
	}

	private boolean sameNodeMappings(MappingState other) {
		Set<PDGNodeMapping> thisNodeMappings = this.getNodeMappings();
		Set<PDGNodeMapping> otherNodeMappings = other.getNodeMappings();
		return thisNodeMappings.size() == otherNodeMappings.size() && thisNodeMappings.containsAll(otherNodeMappings);
	}
	
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof MappingState) {
			MappingState state = (MappingState)o;
			return this.getNodeMappings().equals(state.getNodeMappings());
		}
		return false;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			sb.append(nodeMapping).append("\n");
		}
		return sb.toString();
	}
}
