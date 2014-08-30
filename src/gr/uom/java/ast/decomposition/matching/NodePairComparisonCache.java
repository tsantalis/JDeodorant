package gr.uom.java.ast.decomposition.matching;

import gr.uom.java.ast.decomposition.AbstractMethodFragment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NodePairComparisonCache {
	private static NodePairComparisonCache instance;
	private Map<NodePair, List<ASTNodeDifference>> differenceMap;
	private Map<NodePair, Boolean> matchMap;
	private Map<NodePair, List<AbstractMethodFragment>> additionallyMatchedFragmentMap1;
	private Map<NodePair, List<AbstractMethodFragment>> additionallyMatchedFragmentMap2;
	
	private NodePairComparisonCache() {
		differenceMap = new LinkedHashMap<NodePair, List<ASTNodeDifference>>();
		matchMap = new LinkedHashMap<NodePair, Boolean>();
		additionallyMatchedFragmentMap1 = new LinkedHashMap<NodePair, List<AbstractMethodFragment>>();
		additionallyMatchedFragmentMap2 = new LinkedHashMap<NodePair, List<AbstractMethodFragment>>();
	}
	
	public static NodePairComparisonCache getInstance() {
		if(instance == null) {
			instance = new NodePairComparisonCache();
		}
		return instance;
	}
	
	public void clearCache() {
		differenceMap.clear();
		matchMap.clear();
		additionallyMatchedFragmentMap1.clear();
		additionallyMatchedFragmentMap2.clear();
	}

	public boolean containsNodePair(NodePair pair) {
		return differenceMap.containsKey(pair) && matchMap.containsKey(pair);
	}

	public List<ASTNodeDifference> getDifferencesForNodePair(NodePair pair) {
		return differenceMap.get(pair);
	}
	
	public boolean getMatchForNodePair(NodePair pair) {
		return matchMap.get(pair);
	}

	public List<AbstractMethodFragment> getAdditionallyMatchedFragments1(NodePair pair) {
		return additionallyMatchedFragmentMap1.get(pair);
	}

	public List<AbstractMethodFragment> getAdditionallyMatchedFragments2(NodePair pair) {
		return additionallyMatchedFragmentMap2.get(pair);
	}

	public void addDifferencesForNodePair(NodePair pair, List<ASTNodeDifference> differences) {
		differenceMap.put(pair, differences);
	}
	
	public void addMatchForNodePair(NodePair pair, boolean match) {
		matchMap.put(pair, match);
	}
	
	public void setAdditionallyMatchedFragments1(NodePair pair, List<AbstractMethodFragment> fragments) {
		additionallyMatchedFragmentMap1.put(pair, fragments);
	}
	
	public void setAdditionallyMatchedFragments2(NodePair pair, List<AbstractMethodFragment> fragments) {
		additionallyMatchedFragmentMap2.put(pair, fragments);
	}

	public int getMapSize() {
		return matchMap.size();
	}
}
