package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PDGObjectSliceUnionCollection {
	private Map<BasicBlock, PDGObjectSliceUnion> objectSliceUnionMap;
	
	public PDGObjectSliceUnionCollection(PDG pdg, PlainVariable objectReference) {
		this.objectSliceUnionMap = new LinkedHashMap<BasicBlock, PDGObjectSliceUnion>();
		Map<CompositeVariable, LinkedHashSet<PDGNode>> definedAttributeNodeCriteriaMap = pdg.getDefinedAttributesOfReference(objectReference);
		Map<CompositeVariable, Set<BasicBlock>> definedAttributeBasicBlockMap = new LinkedHashMap<CompositeVariable, Set<BasicBlock>>();
		for(CompositeVariable compositeVariable : definedAttributeNodeCriteriaMap.keySet()) {
			Set<PDGNode> nodeCriteria = definedAttributeNodeCriteriaMap.get(compositeVariable);
			Map<PDGNode, Set<BasicBlock>> boundaryBlockMap = new LinkedHashMap<PDGNode, Set<BasicBlock>>();
			for(PDGNode nodeCriterion : nodeCriteria) {
				Set<BasicBlock> boundaryBlocks = pdg.boundaryBlocks(nodeCriterion);
				boundaryBlockMap.put(nodeCriterion, boundaryBlocks);
			}
			List<Set<BasicBlock>> basicBlockListPerNodeCriterion = new ArrayList<Set<BasicBlock>>(boundaryBlockMap.values());
			if(!basicBlockListPerNodeCriterion.isEmpty()) {
				Set<BasicBlock> basicBlockIntersection = new LinkedHashSet<BasicBlock>(basicBlockListPerNodeCriterion.get(0));
				for(int i=1; i<basicBlockListPerNodeCriterion.size(); i++) {
					basicBlockIntersection.retainAll(basicBlockListPerNodeCriterion.get(i));
				}
				definedAttributeBasicBlockMap.put(compositeVariable, basicBlockIntersection);
			}
			else {
				definedAttributeBasicBlockMap.put(compositeVariable, new LinkedHashSet<BasicBlock>());
			}
		}
		List<Set<BasicBlock>> basicBlockListPerCompositeVariable = new ArrayList<Set<BasicBlock>>(definedAttributeBasicBlockMap.values());
		if(!basicBlockListPerCompositeVariable.isEmpty()) {
			Set<BasicBlock> basicBlockIntersection = new LinkedHashSet<BasicBlock>(basicBlockListPerCompositeVariable.get(0));
			for(int i=1; i<basicBlockListPerCompositeVariable.size(); i++) {
				basicBlockIntersection.retainAll(basicBlockListPerCompositeVariable.get(i));
			}
			for(BasicBlock basicBlock : basicBlockIntersection) {
				Set<PDGNode> allNodeCriteria = new LinkedHashSet<PDGNode>();
				for(CompositeVariable compositeVariable : definedAttributeNodeCriteriaMap.keySet()) {
					Set<PDGNode> nodeCriteria = definedAttributeNodeCriteriaMap.get(compositeVariable);
					allNodeCriteria.addAll(nodeCriteria);
				}
				PDGObjectSliceUnion objectSliceUnion = new PDGObjectSliceUnion(pdg, basicBlock, allNodeCriteria, objectReference);
				if(objectSliceUnion.satisfiesRules())
					objectSliceUnionMap.put(basicBlock, objectSliceUnion);
			}
		}
	}

	public Collection<PDGObjectSliceUnion> getSliceUnions() {
		return objectSliceUnionMap.values();
	}
}
