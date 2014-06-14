package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.AbstractMethodFragment;
import gr.uom.java.ast.decomposition.StatementObject;
import gr.uom.java.ast.decomposition.cfg.CFGBranchIfNode;
import gr.uom.java.ast.decomposition.cfg.GraphEdge;
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

	public List<ASTNodeDifference> getNonOverlappingNodeDifferences() {
		List<ASTNodeDifference> nodeDifferences = new ArrayList<ASTNodeDifference>();
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			nodeDifferences.addAll(nodeMapping.getNonOverlappingNodeDifferences());
		}
		return nodeDifferences;
	}

	public int getDistinctDifferenceCount() {
		Set<Difference> differences = new LinkedHashSet<Difference>();
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			for(ASTNodeDifference difference : nodeMapping.getNodeDifferences()) {
				differences.addAll(difference.getDifferences());
			}
		}
		return differences.size();
	}

	public int getNonDistinctDifferenceCount() {
		List<Difference> differences = new ArrayList<Difference>();
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			for(ASTNodeDifference difference : nodeMapping.getNodeDifferences()) {
				differences.addAll(difference.getDifferences());
			}
		}
		return differences.size();
	}

	//returns the sum of the differences in the node Ids of the mapped nodes
	public int getNodeMappingIdDiff() {
		int sum = 0;
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			sum += Math.abs(nodeMapping.getNodeG1().getId() - nodeMapping.getNodeG2().getId());
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
				maximumStates.clear();
				maximumStates.add(state);
			}
			else if(state.getSize() == max) {
				if(!containsSameState(maximumStates, state))
					maximumStates.add(state);
			}
		}
		return maximumStates;
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
							if(match && astNodeMatcher.isParameterizable() && (mappedControlParents(dstNodeG1, dstNodeG2) || symmetricalIfNodes)) {
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

	private PDGNodeMapping findMappingWithBothNodes(PDGNode nodeG1, PDGNode nodeG2) {
		for(PDGNodeMapping nodeMapping : getNodeMappings()) {
			if(nodeMapping.getNodeG1().equals(nodeG1) &&
					nodeMapping.getNodeG2().equals(nodeG2)) {
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

	public Set<PDGNodeMapping> getNodeMappings() {
		return this.nodeMappings;
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
