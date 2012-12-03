package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.cfg.GraphEdge;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MappingState {
	private List<PDGNodeMapping> nodeMappings;
	private List<PDGEdgeMapping> edgeMappings;
	private List<MappingState> children;

	private MappingState() {
		this.nodeMappings = new ArrayList<PDGNodeMapping>();
		this.edgeMappings = new ArrayList<PDGEdgeMapping>();
		this.children = new ArrayList<MappingState>();
	}

	public MappingState(PDGNodeMapping initialNodeMapping) {
		this.nodeMappings = new ArrayList<PDGNodeMapping>();
		this.edgeMappings = new ArrayList<PDGEdgeMapping>();
		this.children = new ArrayList<MappingState>();
		this.nodeMappings.add(initialNodeMapping);
		traverse(this, initialNodeMapping);
	}
	
	private void traverse(MappingState state, PDGNodeMapping nodeMapping) {
		PDGNode nodeG1 = nodeMapping.getNodeG1();
		PDGNode nodeG2 = nodeMapping.getNodeG2();
		Iterator<GraphEdge> nodeG1EdgeIterator = nodeG1.getOutgoingDependenceIterator();
		while(nodeG1EdgeIterator.hasNext()) {
			PDGDependence edgeG1 = (PDGDependence)nodeG1EdgeIterator.next();
			Iterator<GraphEdge> nodeG2EdgeIterator = nodeG2.getOutgoingDependenceIterator();
			while(nodeG2EdgeIterator.hasNext()) {
				PDGDependence edgeG2 = (PDGDependence)nodeG2EdgeIterator.next();
				PDGEdgeMapping edgeMapping = new PDGEdgeMapping(edgeG1, edgeG2);
				if(edgeMapping.isCompatible(nodeMapping)) {
					PDGNode dstNodeG1 = (PDGNode)edgeG1.getDst();
					PDGNode dstNodeG2 = (PDGNode)edgeG2.getDst();
					if(dstNodeG1.isEquivalent(dstNodeG2)) {
						PDGNodeMapping dstNodeMapping = new PDGNodeMapping(dstNodeG1, dstNodeG2);
						if(dstNodeMapping.isValidMatch()) {
							MappingState childState = state.getChildStateWithNodeMapping(dstNodeMapping);
							if(childState != null) {
								childState.edgeMappings.add(edgeMapping);
								childState.propagateEdgeMappingToChildren(edgeMapping);
							}
							else {
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
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(PDGNodeMapping nodeMapping : nodeMappings) {
			sb.append(nodeMapping).append("\n");
		}
		return sb.toString();
	}
}
