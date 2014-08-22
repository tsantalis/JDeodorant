package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.cfg.PDGNode;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.SwitchCase;

public class SwitchBodyDecomposer {
	private Map<PDGNode, Set<PDGNode>> switchCaseNodeMap;
	
	public SwitchBodyDecomposer(Set<PDGNode> nodes) {
		this.switchCaseNodeMap = new LinkedHashMap<PDGNode, Set<PDGNode>>();
		PDGNode currentSwitchCase = null;
		for(PDGNode node : nodes) {
			if(node.getASTStatement() instanceof SwitchCase) {
				currentSwitchCase = node;
				switchCaseNodeMap.put(currentSwitchCase, new LinkedHashSet<PDGNode>());
			}
			else {
				if(currentSwitchCase != null) {
					if(switchCaseNodeMap.containsKey(currentSwitchCase)) {
						Set<PDGNode> switchCaseNodes = switchCaseNodeMap.get(currentSwitchCase);
						switchCaseNodes.add(node);
					}
					else {
						Set<PDGNode> switchCaseNodes = new LinkedHashSet<PDGNode>();
						switchCaseNodes.add(node);
						switchCaseNodeMap.put(currentSwitchCase, switchCaseNodes);
					}
				}
			}
		}
	}

	public Map<PDGNode, Set<PDGNode>> getSwitchCaseNodeMap() {
		return switchCaseNodeMap;
	}
}
