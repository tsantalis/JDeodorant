package gr.uom.java.ast.decomposition.matching;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NodePairComparisonCache {
	private static NodePairComparisonCache instance;
	private Map<NodePair, List<ASTNodeDifference>> differenceMap;
	private Map<NodePair, Boolean> matchMap;
	
	private NodePairComparisonCache() {
		differenceMap = new LinkedHashMap<NodePair, List<ASTNodeDifference>>();
		matchMap = new LinkedHashMap<NodePair, Boolean>();
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

	public void addDifferencesForNodePair(NodePair pair, List<ASTNodeDifference> differences) {
		differenceMap.put(pair, differences);
	}
	
	public void addMatchForNodePair(NodePair pair, boolean match) {
		matchMap.put(pair, match);
	}
}
