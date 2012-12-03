package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CFGBranchNode;
import gr.uom.java.ast.decomposition.cfg.PDGAntiDependence;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGDataDependence;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGOutputDependence;

public class PDGEdgeMapping {
	private PDGDependence edgeG1;
	private PDGDependence edgeG2;
	private volatile int hashCode = 0;
	
	public PDGEdgeMapping(PDGDependence edgeG1, PDGDependence edgeG2) {
		this.edgeG1 = edgeG1;
		this.edgeG2 = edgeG2;
	}
	
	public boolean isCompatible(PDGNodeMapping nodeMapping) {
		if(edgeG1.getClass() == edgeG2.getClass()) {
			if(edgeG1 instanceof PDGControlDependence) {
				return true;
			}
			AbstractVariable edgeG1Variable = null;
			AbstractVariable edgeG2Variable = null;
			CFGBranchNode edgeG1LoopNode = null;
			CFGBranchNode edgeG2LoopNode = null;
			if(edgeG1 instanceof PDGDataDependence) {
				edgeG1Variable = ((PDGDataDependence)edgeG1).getData();
				edgeG2Variable = ((PDGDataDependence)edgeG2).getData();
				edgeG1LoopNode = ((PDGDataDependence)edgeG1).getLoop();
				edgeG2LoopNode = ((PDGDataDependence)edgeG2).getLoop();
			}
			else if(edgeG1 instanceof PDGAntiDependence) {
				edgeG1Variable = ((PDGAntiDependence)edgeG1).getData();
				edgeG2Variable = ((PDGAntiDependence)edgeG2).getData();
				edgeG1LoopNode = ((PDGAntiDependence)edgeG1).getLoop();
				edgeG2LoopNode = ((PDGAntiDependence)edgeG2).getLoop();
			}
			else if(edgeG1 instanceof PDGOutputDependence) {
				edgeG1Variable = ((PDGOutputDependence)edgeG1).getData();
				edgeG2Variable = ((PDGOutputDependence)edgeG2).getData();
				edgeG1LoopNode = ((PDGOutputDependence)edgeG1).getLoop();
				edgeG2LoopNode = ((PDGOutputDependence)edgeG2).getLoop();
			}
			if(equalData(edgeG1Variable, edgeG2Variable) || nodeMapping.matchingVariableReplacement(edgeG1Variable, edgeG2Variable)) {
				if(edgeG1LoopNode == null && edgeG2LoopNode == null) {
					return true;
				}
				else if(edgeG1LoopNode != null && edgeG2LoopNode != null) {
					return edgeG1LoopNode.isEquivalent(edgeG2LoopNode);
				}
			}
		}
		return false;
	}
	
	private boolean equalData(AbstractVariable edgeG1Variable, AbstractVariable edgeG2Variable) {
		return edgeG1Variable.getVariableName().equals(edgeG2Variable.getVariableName()) &&
				edgeG1Variable.getVariableType().equals(edgeG2Variable.getVariableType()) &&
				edgeG1Variable.isField() == edgeG2Variable.isField() &&
				edgeG1Variable.isParameter() == edgeG2Variable.isParameter();
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
