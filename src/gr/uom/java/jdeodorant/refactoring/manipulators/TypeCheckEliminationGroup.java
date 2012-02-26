package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.inheritance.InheritanceTree;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.SimpleName;

public class TypeCheckEliminationGroup implements Comparable<TypeCheckEliminationGroup> {
	private Set<TypeCheckElimination> candidates;
	private int groupSizeAtSystemLevel;
	private double averageGroupSizeAtClassLevel;
	private double averageNumberOfStatementsInGroup;
	
	public TypeCheckEliminationGroup() {
		this.candidates = new LinkedHashSet<TypeCheckElimination>();
	}

	public void addCandidate(TypeCheckElimination elimination) {
		this.candidates.add(elimination);
	}

	public Set<TypeCheckElimination> getCandidates() {
		return candidates;
	}

	public int getGroupSizeAtSystemLevel() {
		return groupSizeAtSystemLevel;
	}

	public void setGroupSizeAtSystemLevel(int groupSizeAtSystemLevel) {
		this.groupSizeAtSystemLevel = groupSizeAtSystemLevel;
	}

	public double getAverageGroupSizeAtClassLevel() {
		return averageGroupSizeAtClassLevel;
	}

	public void setAverageGroupSizeAtClassLevel(double averageGroupSizeAtClassLevel) {
		this.averageGroupSizeAtClassLevel = averageGroupSizeAtClassLevel;
	}

	public double getAverageNumberOfStatementsInGroup() {
		return averageNumberOfStatementsInGroup;
	}

	public void setAverageNumberOfStatementsInGroup(double averageNumberOfStatementsInGroup) {
		this.averageNumberOfStatementsInGroup = averageNumberOfStatementsInGroup;
	}

	public int compareTo(TypeCheckEliminationGroup other) {
		int groupSizeAtSystemLevel1 = this.getGroupSizeAtSystemLevel();
		int groupSizeAtSystemLevel2 = other.getGroupSizeAtSystemLevel();
		double averageNumberOfStatementsInGroup1 = this.getAverageNumberOfStatementsInGroup();
		double averageNumberOfStatementsInGroup2 = other.getAverageNumberOfStatementsInGroup();
		
		if(groupSizeAtSystemLevel1 > groupSizeAtSystemLevel2)
			return -1;
		if(groupSizeAtSystemLevel1 < groupSizeAtSystemLevel2)
			return 1;
		
		if(averageNumberOfStatementsInGroup1 > averageNumberOfStatementsInGroup2)
			return -1;
		if(averageNumberOfStatementsInGroup1 < averageNumberOfStatementsInGroup2)
			return 1;
		
		return 0;
	}

	public String toString() {
		TypeCheckElimination elimination = new ArrayList<TypeCheckElimination>(candidates).get(0);
		if(elimination.getExistingInheritanceTree() == null && elimination.getInheritanceTreeMatchingWithStaticTypes() == null) {
			Set<String> states = new TreeSet<String>();
			for(SimpleName simpleName : elimination.getStaticFields())
				states.add(simpleName.getIdentifier());
			for(SimpleName simpleName : elimination.getAdditionalStaticFields())
				states.add(simpleName.getIdentifier());
			return "states: " + states.toString();
		}
		else {
			InheritanceTree tree = null;
			if(elimination.getExistingInheritanceTree() != null)
				tree = elimination.getExistingInheritanceTree();
			else if(elimination.getInheritanceTreeMatchingWithStaticTypes() != null) {
				tree = elimination.getInheritanceTreeMatchingWithStaticTypes();
			}
			if(tree != null)
				return "inheritance hierarchy: " + "[" + tree.getRootNode().getUserObject().toString() + "]";
			
		}
		return "";
	}
}
