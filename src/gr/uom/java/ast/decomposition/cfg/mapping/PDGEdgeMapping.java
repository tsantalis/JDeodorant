package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CFGBranchNode;
import gr.uom.java.ast.decomposition.cfg.PDGAbstractDataDependence;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;

public class PDGEdgeMapping {
	private PDGDependence edgeG1;
	private PDGDependence edgeG2;
	private volatile int hashCode = 0;
	
	public PDGEdgeMapping(PDGDependence edgeG1, PDGDependence edgeG2) {
		this.edgeG1 = edgeG1;
		this.edgeG2 = edgeG2;
	}

	public PDGDependence getEdgeG1() {
		return edgeG1;
	}

	public PDGDependence getEdgeG2() {
		return edgeG2;
	}

	public boolean isCompatible(PDGNodeMapping nodeMapping) {
		if(edgeG1.getClass() == edgeG2.getClass()) {
			if(edgeG1 instanceof PDGControlDependence) {
				PDGControlDependence controlG1 = (PDGControlDependence)edgeG1;
				PDGControlDependence controlG2 = (PDGControlDependence)edgeG2;
				return controlG1.sameLabel(controlG2);
			}
			if(edgeG1 instanceof PDGAbstractDataDependence) {
				AbstractVariable edgeG1Variable = ((PDGAbstractDataDependence)edgeG1).getData();
				AbstractVariable edgeG2Variable = ((PDGAbstractDataDependence)edgeG2).getData();
				CFGBranchNode edgeG1LoopNode = ((PDGAbstractDataDependence)edgeG1).getLoop();
				CFGBranchNode edgeG2LoopNode = ((PDGAbstractDataDependence)edgeG2).getLoop();
				if(equalData(edgeG1Variable, edgeG2Variable) || nodeMapping.matchingVariableDifference(edgeG1Variable, edgeG2Variable)) {
					if(edgeG1LoopNode == null && edgeG2LoopNode == null) {
						return true;
					}
					else if(edgeG1LoopNode != null && edgeG2LoopNode != null) {
						ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(nodeMapping.getTypeRoot1(), nodeMapping.getTypeRoot2());
						boolean match = astNodeMatcher.match(edgeG1LoopNode.getPDGNode(), edgeG2LoopNode.getPDGNode());
						return match && astNodeMatcher.isParameterizable();
					}
				}
			}
		}
		return false;
	}
	
	private boolean equalData(AbstractVariable edgeG1Variable, AbstractVariable edgeG2Variable) {
		if(edgeG1Variable.getClass() == edgeG2Variable.getClass()) {
			return edgeG1Variable.toString().equals(edgeG2Variable.toString()) &&
					edgeG1Variable.getVariableType().equals(edgeG2Variable.getVariableType()) &&
					edgeG1Variable.isField() == edgeG2Variable.isField() &&
					edgeG1Variable.isParameter() == edgeG2Variable.isParameter();
		}
		return false;
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof PDGEdgeMapping) {
			PDGEdgeMapping mapping = (PDGEdgeMapping)o;
			return this.edgeG1.equals(mapping.edgeG1) &&
					this.edgeG2.equals(mapping.edgeG2);
		}
		return false;
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 37*result + edgeG1.hashCode();
			result = 37*result + edgeG2.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(edgeG1);
		sb.append(edgeG2);
		return sb.toString();
	}
}
