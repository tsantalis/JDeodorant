package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import gr.uom.java.ast.decomposition.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.cfg.GraphNode;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.PDGNode;

public class PDGMapper {
	private PDG pdg1;
	private PDG pdg2;
	private List<MappingState> maximumStates;
	
	public PDGMapper(PDG pdg1, PDG pdg2) {
		this.pdg1 = pdg1;
		this.pdg2 = pdg2;
		this.maximumStates = new ArrayList<MappingState>();
		processPDGNodes();
	}
	
	public MappingState getMaximumState() {
		MappingState maximumState = null;
		if(maximumStates.size() == 1) {
			maximumState = maximumStates.get(0);
		}
		else {
			int minimum = maximumStates.get(0).getDifferenceCount();
			maximumState = maximumStates.get(0);
			for(int i=1; i<maximumStates.size(); i++) {
				MappingState currentState = maximumStates.get(i);
				if(currentState.getDifferenceCount() < minimum) {
					minimum = currentState.getDifferenceCount();
					maximumState = currentState;
				}
			}
		}
		return maximumState;
	}

	private void processPDGNodes() {
		List<MappingState> finalStates = new ArrayList<MappingState>();
		Iterator<GraphNode> nodeIterator1 = pdg1.getNodeIterator();
		while(nodeIterator1.hasNext()) {
			PDGNode node1 = (PDGNode)nodeIterator1.next();
			Iterator<GraphNode> nodeIterator2 = pdg2.getNodeIterator();
			List<MappingState> currentStates = new ArrayList<MappingState>();
			while(nodeIterator2.hasNext()) {
				PDGNode node2 = (PDGNode)nodeIterator2.next();
				ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher();
				boolean match = node1.getASTStatement().subtreeMatch(astNodeMatcher, node2.getASTStatement());
				if(match) {
					PDGNodeMapping mapping = new PDGNodeMapping(node1, node2, astNodeMatcher.getDifferences());
					if(finalStates.isEmpty()) {
						MappingState state = new MappingState(null, mapping);
						List<MappingState> maxStates = state.getMaximumCommonSubGraphs();
						for(MappingState temp : maxStates) {
							if(!currentStates.contains(temp)) {
								currentStates.add(temp);
							}
						}
					}
					else {
						for(MappingState previousState : finalStates) {
							MappingState state = new MappingState(previousState, mapping);
							List<MappingState> maxStates = state.getMaximumCommonSubGraphs();
							for(MappingState temp : maxStates) {
								if(!currentStates.contains(temp)) {
									currentStates.add(temp);
								}
							}
						}
					}
				}
			}
			if(!currentStates.isEmpty())
				finalStates = getMaximumStates(currentStates);
		}
		maximumStates = finalStates;
	}
	
	private List<MappingState> getMaximumStates(List<MappingState> currentStates) {
		int max = 0;
		List<MappingState> maximumStates = new ArrayList<MappingState>();
		for(MappingState state : currentStates) {
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
}
