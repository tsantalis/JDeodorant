package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.decomposition.cfg.PlainVariable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SliceProfileRow {

	private int statementID;
	private Map<PlainVariable, Boolean> sliceCoverageMap;
	
	public SliceProfileRow(int statementID) {
		this.statementID = statementID;
		this.sliceCoverageMap = new LinkedHashMap<PlainVariable, Boolean>();
	}

	public int getStatementID() {
		return statementID;
	}

	public void put(PlainVariable variable, boolean value) {
		sliceCoverageMap.put(variable, value);
	}

	public boolean getValue(PlainVariable variable) {
		return sliceCoverageMap.get(variable);
	}

	public boolean statementBelongsToAllSlices(Set<PlainVariable> variables) {
		if(variables.isEmpty())
			return false;
		for(PlainVariable variable : variables) {
			if(sliceCoverageMap.get(variable) == false)
				return false;
		}
		return true;
	}
}
