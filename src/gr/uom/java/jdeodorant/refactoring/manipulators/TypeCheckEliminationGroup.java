package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.inheritance.InheritanceTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.SimpleName;

public class TypeCheckEliminationGroup implements Comparable<TypeCheckEliminationGroup> {
	private List<TypeCheckElimination> candidates;
	private int groupSizeAtSystemLevel;
	private double averageGroupSizeAtClassLevel;
	private double averageNumberOfStatementsInGroup;
	
	public TypeCheckEliminationGroup() {
		this.candidates = new ArrayList<TypeCheckElimination>();
	}

	public void addCandidate(TypeCheckElimination elimination) {
		this.candidates.add(elimination);
	}

	public List<TypeCheckElimination> getCandidates() {
		Collections.sort(candidates);
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

	public Set<String> getConstantVariables() {
		TypeCheckElimination elimination = (TypeCheckElimination)candidates.toArray()[0];
		if(elimination.getExistingInheritanceTree() == null && elimination.getInheritanceTreeMatchingWithStaticTypes() == null) {
			Set<String> constantVariables = new LinkedHashSet<String>();
			for(SimpleName simpleName : elimination.getStaticFields())
				constantVariables.add(simpleName.getIdentifier());
			for(SimpleName simpleName : elimination.getAdditionalStaticFields())
				constantVariables.add(simpleName.getIdentifier());
			return constantVariables;
		}
		return null;
	}

	public InheritanceTree getInheritanceTree() {
		TypeCheckElimination elimination = (TypeCheckElimination)candidates.toArray()[0];
		InheritanceTree tree = null;
		if(elimination.getExistingInheritanceTree() != null)
			tree = elimination.getExistingInheritanceTree();
		else if(elimination.getInheritanceTreeMatchingWithStaticTypes() != null) {
			tree = elimination.getInheritanceTreeMatchingWithStaticTypes();
		}
		return tree;
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
		
		Set<String> constantVariables1 = this.getConstantVariables();
		Set<String> constantVariables2 = other.getConstantVariables();
		if(constantVariables1 != null && constantVariables2 != null) {
			if(!constantVariables1.equals(constantVariables2)) {
				StringBuilder sb1 = new StringBuilder();
				for(String s : constantVariables1)
					sb1.append(s);
				StringBuilder sb2 = new StringBuilder();
				for(String s : constantVariables2)
					sb2.append(s);
				return sb1.toString().compareTo(sb2.toString());
			}
		}
		InheritanceTree tree1 = this.getInheritanceTree();
		InheritanceTree tree2 = other.getInheritanceTree();
		if(tree1 != null && tree2 != null) {
			String root1 = (String)tree1.getRootNode().getUserObject();
			String root2 = (String)tree2.getRootNode().getUserObject();
			if(!root1.equals(root2)) {
				return root1.compareTo(root2);
			}
		}
		return 0;
	}

	public String toString() {
		Set<String> constantVariables = this.getConstantVariables();
		if(constantVariables != null)
			return "constant variables: " + constantVariables;
		InheritanceTree tree = this.getInheritanceTree();
		if(tree != null)
			return "inheritance hierarchy: " + "[" + tree.getRootNode().getUserObject().toString() + "]";
		return "";
	}
}
