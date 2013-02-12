package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.cfg.GraphEdge;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGMethodEntryNode;
import gr.uom.java.ast.decomposition.cfg.PDGNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
		traverse(this, initialNodeMapping);
	}

	public Set<PDGNodeMapping> getNodeMappings() {
		return nodeMappings;
	}

	public Set<PDGEdgeMapping> getEdgeMappings() {
		return edgeMappings;
	}

	public int getDifferenceCount() {
		int count = 0;
		for(PDGNodeMapping nodeMapping : nodeMappings) {
			count += nodeMapping.getDifferenceCount();
		}
		return count;
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

	private void traverse(MappingState state, PDGNodeMapping nodeMapping) {
		PDGNode nodeG1 = nodeMapping.getNodeG1();
		PDGNode nodeG2 = nodeMapping.getNodeG2();
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
					else {
						//get source nodes if the edge is incoming
						dstNodeG1 = (PDGNode)edgeG1.getSrc();
						dstNodeG2 = (PDGNode)edgeG2.getSrc();
					}
					
					ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher();
					boolean match;
			
					if(dstNodeG1 instanceof PDGMethodEntryNode || dstNodeG2 instanceof PDGMethodEntryNode)
						match = false;
					else 
						match = dstNodeG1.getASTStatement().subtreeMatch(astNodeMatcher, dstNodeG2.getASTStatement());
					if(match) {
						PDGNodeMapping dstNodeMapping = new PDGNodeMapping(dstNodeG1, dstNodeG2, astNodeMatcher.getDifferences());
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
							traverse(newMappingState, dstNodeMapping);
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
