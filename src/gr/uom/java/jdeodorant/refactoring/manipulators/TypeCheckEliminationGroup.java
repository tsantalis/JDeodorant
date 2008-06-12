package gr.uom.java.jdeodorant.refactoring.manipulators;

import java.util.List;

public class TypeCheckEliminationGroup implements Comparable<Object> {
	private List<TypeCheckElimination> typeCheckEliminations;
	private double averageNumberOfStatementsInGroup;
	private int size;

	public TypeCheckEliminationGroup(List<TypeCheckElimination> typeCheckEliminations, double averageNumberOfStatementsInGroup) {
		this.typeCheckEliminations = typeCheckEliminations;
		this.averageNumberOfStatementsInGroup = averageNumberOfStatementsInGroup;
		this.size = typeCheckEliminations.size();
	}

	public boolean containsTypeCheckElimination(TypeCheckElimination elimination) {
		return typeCheckEliminations.contains(elimination);
	}

	public boolean equals(Object o) {
		if(this == o) {
            return true;
        }
		
		if(o instanceof TypeCheckEliminationGroup) {
			TypeCheckEliminationGroup group = (TypeCheckEliminationGroup)o;
			return this.typeCheckEliminations.equals(group.typeCheckEliminations);
		}
		
		return false;
	}

	public int compareTo(Object o) {
		TypeCheckEliminationGroup group = (TypeCheckEliminationGroup)o;
		
		if(this.size > group.size)
			return -1;
		if(this.size < group.size)
			return 1;
		
		if(this.averageNumberOfStatementsInGroup > group.averageNumberOfStatementsInGroup)
			return -1;
		if(this.averageNumberOfStatementsInGroup < group.averageNumberOfStatementsInGroup)
			return 1;
		
		return this.typeCheckEliminations.toString().compareTo(group.typeCheckEliminations.toString());
	}
}
