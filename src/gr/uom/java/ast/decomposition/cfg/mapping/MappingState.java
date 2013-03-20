package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.ASTNodeDifference;
import gr.uom.java.ast.decomposition.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.cfg.GraphEdge;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGMethodEntryNode;
import gr.uom.java.ast.decomposition.cfg.PDGNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class MappingState {
	private Set<PDGNodeMapping> nodeMappings;
	private Set<PDGEdgeMapping> edgeMappings;
	private List<MappingState> children;

	private MappingState() {
		this.nodeMappings = new LinkedHashSet<PDGNodeMapping>();
		this.edgeMappings = new LinkedHashSet<PDGEdgeMapping>();
		this.children = new ArrayList<MappingState>();
	}

	public MappingState(MappingState previous, PDGNodeMapping initialNodeMapping) {
		this.nodeMappings = new LinkedHashSet<PDGNodeMapping>();
		this.edgeMappings = new LinkedHashSet<PDGEdgeMapping>();
		this.children = new ArrayList<MappingState>();
		if(previous != null) {
			nodeMappings.addAll(previous.nodeMappings);
			edgeMappings.addAll(previous.edgeMappings);
		}
		if(!containsAtLeastOneNodeInMappings(initialNodeMapping))
			this.nodeMappings.add(initialNodeMapping);
		traverse(this, initialNodeMapping, new LinkedHashSet<PDGNode>(), new LinkedHashSet<PDGNode>());
	}

	public Set<PDGNode> getMappedNodesG1() {
		Set<PDGNode> nodesG1 = new TreeSet<PDGNode>();
		for(PDGNodeMapping nodeMapping : nodeMappings) {
			nodesG1.add(nodeMapping.getNodeG1());
		}
		return nodesG1;
	}

	public Set<PDGNode> getMappedNodesG2() {
		Set<PDGNode> nodesG2 = new TreeSet<PDGNode>();
		for(PDGNodeMapping nodeMapping : nodeMappings) {
			nodesG2.add(nodeMapping.getNodeG2());
		}
		return nodesG2;
	}

	public Set<PDGNodeMapping> getNodeMappings() {
		return nodeMappings;
	}

	public Set<PDGEdgeMapping> getEdgeMappings() {
		return edgeMappings;
	}

	public List<ASTNodeDifference> getNodeDifferences() {
		List<ASTNodeDifference> nodeDifferences = new ArrayList<ASTNodeDifference>();
		for(PDGNodeMapping nodeMapping : nodeMappings) {
			nodeDifferences.addAll(nodeMapping.getNodeDifferences());
		}
		return nodeDifferences;
	}

	public int getDifferenceCount() {
		int count = 0;
		for(PDGNodeMapping nodeMapping : nodeMappings) {
			count += nodeMapping.getDifferenceCount();
		}
		return count;
	}

	//returns the sum of the differences in the node Ids of the mapped nodes
	public int getNodeMappingIdDiff() {
		int sum = 0;
		for(PDGNodeMapping nodeMapping : nodeMappings) {
			sum += Math.abs(nodeMapping.getNodeG1().getId() - nodeMapping.getNodeG2().getId());
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
				maximumStates.add(state);
			}
		}
		return maximumStates;
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
		return nodeMappings.size();
	}

	public int getEdgeMappingSize() {
		return edgeMappings.size();
	}

	public int getSize() {
		return nodeMappings.size() + edgeMappings.size();
	}

	private void traverse(MappingState state, PDGNodeMapping nodeMapping, Set<PDGNode> visitedNodesG1, Set<PDGNode> visitedNodesG2) {
		PDGNode nodeG1 = nodeMapping.getNodeG1();
		PDGNode nodeG2 = nodeMapping.getNodeG2();
		if(visitedNodesG1.contains(nodeG1) && visitedNodesG2.contains(nodeG2))
			return;
		visitedNodesG1.add(nodeG1);
		visitedNodesG2.add(nodeG2);
		Iterator<GraphEdge> nodeG1EdgeIterator = nodeG1.getDependenceIterator();
		while(nodeG1EdgeIterator.hasNext()) {
			PDGDependence edgeG1 = (PDGDependence)nodeG1EdgeIterator.next();
			Iterator<GraphEdge> nodeG2EdgeIterator = nodeG2.getDependenceIterator();
			while(nodeG2EdgeIterator.hasNext()) {
				PDGDependence edgeG2 = (PDGDependence)nodeG2EdgeIterator.next();
				PDGEdgeMapping edgeMapping = new PDGEdgeMapping(edgeG1, edgeG2);
				if(edgeMapping.isCompatible(nodeMapping)) {
					PDGNode dstNodeG1 = null;
					PDGNode dstNodeG2 = null;
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
					
					if(dstNodeG1 != null && dstNodeG2 != null) {
						if(dstNodeG1 instanceof PDGMethodEntryNode && dstNodeG2 instanceof PDGMethodEntryNode) {
							if(!state.edgeMappings.contains(edgeMapping)) {
								state.edgeMappings.add(edgeMapping);
								state.propagateEdgeMappingToChildren(edgeMapping);
							}
						}
						ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(nodeMapping.getTypeRoot1(), nodeMapping.getTypeRoot2());
						boolean match;
						if(dstNodeG1 instanceof PDGMethodEntryNode || dstNodeG2 instanceof PDGMethodEntryNode)
							match = false;
						else 
							match = dstNodeG1.getASTStatement().subtreeMatch(astNodeMatcher, dstNodeG2.getASTStatement());
						if(match && astNodeMatcher.isParameterizable()) {
							PDGNode nodeG1ControlParent = dstNodeG1.getControlDependenceParent();
							PDGNode nodeG2ControlParent = dstNodeG2.getControlDependenceParent();
							PDGControlDependence nodeG1IncomingControlDependence = dstNodeG1.getIncomingControlDependence();
							PDGControlDependence nodeG2IncomingControlDependence = dstNodeG2.getIncomingControlDependence();
							boolean proceedToNodeMapping = false;
							if(state.containsBothNodesInMappings(nodeG1ControlParent, nodeG2ControlParent)
									&& nodeG1IncomingControlDependence.sameLabel(nodeG2IncomingControlDependence))
								proceedToNodeMapping = true;
							if(!state.containsNodeG1InMappings(nodeG1ControlParent) && !state.containsNodeG2InMappings(nodeG2ControlParent))
								proceedToNodeMapping = true;
							if(proceedToNodeMapping) {
							PDGNodeMapping dstNodeMapping = new PDGNodeMapping(dstNodeG1, dstNodeG2, astNodeMatcher);
							MappingState childState = state.getChildStateWithNodeMapping(dstNodeMapping);
							if(childState != null) {
								if(!childState.edgeMappings.contains(edgeMapping)) {
									childState.edgeMappings.add(edgeMapping);
									childState.propagateEdgeMappingToChildren(edgeMapping);
								}
							}
							else if(!state.containsAtLeastOneNodeInMappings(dstNodeMapping)) {
								MappingState newMappingState = state.copy();
								state.children.add(newMappingState);
								newMappingState.edgeMappings.add(edgeMapping);
								newMappingState.nodeMappings.add(dstNodeMapping);
								traverse(newMappingState, dstNodeMapping, visitedNodesG1, visitedNodesG2);
							}
							}
						}
					}
				}
			}
		}
	}

	private boolean containsAtLeastOneNodeInMappings(PDGNodeMapping dstNodeMapping) {
		for(PDGNodeMapping nodeMapping : nodeMappings) {
			if(nodeMapping.getNodeG1().equals(dstNodeMapping.getNodeG1()) ||
					nodeMapping.getNodeG2().equals(dstNodeMapping.getNodeG2())) {
				return true;
			}
		}
		return false;
	}

	public boolean containsBothNodesInMappings(PDGNode nodeG1, PDGNode nodeG2) {
		for(PDGNodeMapping nodeMapping : nodeMappings) {
			if(nodeMapping.getNodeG1().equals(nodeG1) &&
					nodeMapping.getNodeG2().equals(nodeG2)) {
				return true;
			}
		}
		return false;
	}

	public boolean containsNodeG1InMappings(PDGNode nodeG1) {
		for(PDGNodeMapping nodeMapping : nodeMappings) {
			if(nodeMapping.getNodeG1().equals(nodeG1))
				return true;
		}
		return false;
	}

	public boolean containsNodeG2InMappings(PDGNode nodeG2) {
		for(PDGNodeMapping nodeMapping : nodeMappings) {
			if(nodeMapping.getNodeG2().equals(nodeG2))
				return true;
		}
		return false;
	}

	private void propagateEdgeMappingToChildren(PDGEdgeMapping edgeMapping) {
		for(MappingState state : children) {
			state.edgeMappings.add(edgeMapping);
			state.propagateEdgeMappingToChildren(edgeMapping);
		}
	}

	private MappingState getChildStateWithNodeMapping(PDGNodeMapping nodeMapping) {
		for(MappingState state : children) {
			if(state.nodeMappings.contains(nodeMapping))
				return state;
		}
		return null;
	}

	private MappingState copy() {
		MappingState state = new MappingState();
		state.nodeMappings.addAll(this.nodeMappings);
		state.edgeMappings.addAll(this.edgeMappings);
		return state;
	}

	public boolean equalMappings(MappingState state) {
		return this.nodeMappings.containsAll(state.nodeMappings) && state.nodeMappings.containsAll(this.nodeMappings)/* &&
			this.edgeMappings.containsAll(state.edgeMappings) && state.edgeMappings.containsAll(this.edgeMappings)*/;
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof MappingState) {
			MappingState state = (MappingState)o;
			return this.nodeMappings.equals(state.nodeMappings) &&
					this.edgeMappings.equals(state.edgeMappings);
		}
		return false;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(PDGNodeMapping nodeMapping : nodeMappings) {
			sb.append(nodeMapping).append("\n");
		}
		return sb.toString();
	}
}
