package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PDGSliceUnionCollection {
	private Map<BasicBlock, PDGSliceUnion> sliceUnionMap;
	
	public PDGSliceUnionCollection(PDG pdg, PlainVariable localVariableCriterion) {
		this.sliceUnionMap = new LinkedHashMap<BasicBlock, PDGSliceUnion>();
		Set<PDGNode> nodeCriteria = pdg.getAssignmentNodesOfVariableCriterion(localVariableCriterion);
		Map<PDGNode, Set<BasicBlock>> boundaryBlockMap = new LinkedHashMap<PDGNode, Set<BasicBlock>>();
		for(PDGNode nodeCriterion : nodeCriteria) {
			Set<BasicBlock> boundaryBlocks = pdg.boundaryBlocks(nodeCriterion);
			boundaryBlockMap.put(nodeCriterion, boundaryBlocks);
		}
		List<Set<BasicBlock>> list = new ArrayList<Set<BasicBlock>>(boundaryBlockMap.values());
		if(!list.isEmpty()) {
			Set<BasicBlock> basicBlockIntersection = new LinkedHashSet<BasicBlock>(list.get(0));
			for(int i=1; i<list.size(); i++) {
				basicBlockIntersection.retainAll(list.get(i));
			}
			for(BasicBlock basicBlock : basicBlockIntersection) {
				PDGSliceUnion sliceUnion = new PDGSliceUnion(pdg, basicBlock, nodeCriteria, localVariableCriterion);
				if(sliceUnion.satisfiesRules())
					sliceUnionMap.put(basicBlock, sliceUnion);
			}
		}
	}

	public Collection<PDGSliceUnion> getSliceUnions() {
		return sliceUnionMap.values();
	}
}
