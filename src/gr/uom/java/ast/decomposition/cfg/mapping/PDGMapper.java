package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.Iterator;
import java.util.List;

import gr.uom.java.ast.decomposition.cfg.GraphNode;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.PDGNode;

public class PDGMapper {
	private PDG pdg1;
	private PDG pdg2;
	private MappingState maximumState;
	
	public PDGMapper(PDG pdg1, PDG pdg2) {
		this.pdg1 = pdg1;
		this.pdg2 = pdg2;
		processPDGNodes();
	}
	
	public MappingState getMaximumState() {
		return maximumState;
	}

	private void processPDGNodes() {
		Iterator<GraphNode> nodeIterator1 = pdg1.getNodeIterator();
		while(nodeIterator1.hasNext()) {
			PDGNode node1 = (PDGNode)nodeIterator1.next();
			Iterator<GraphNode> nodeIterator2 = pdg2.getNodeIterator();
			while(nodeIterator2.hasNext()) {
				PDGNode node2 = (PDGNode)nodeIterator2.next();
				if(node1.isEquivalent(node2)) {
					PDGNodeMapping mapping = new PDGNodeMapping(node1, node2);
					if(mapping.isValidMatch()) {
						MappingState state = new MappingState(mapping);
						List<MappingState> maxStates = state.getMaximumCommonSubgraph();
						if(maximumState == null) {
							maximumState = maxStates.get(0);
						}
						else if(maxStates.get(0).getSize() > maximumState.getSize()) {
							maximumState = maxStates.get(0);
						}
					}
				}
			}
		}
	}
}
