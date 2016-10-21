package gr.uom.java.jdeodorant.refactoring.manipulators;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class ASTSliceGroup implements Comparable<ASTSliceGroup> {
	private Set<ASTSlice> candidates;
	private double averageNumberOfExtractedStatementsInGroup = 0.0;
	private double averageNumberOfDuplicatedStatementsInGroup = 0.0;
	private double averageDuplicationRatioInGroup = 0.0;
	private int maximumNumberOfExtractedStatementsInGroup = 0;

	public ASTSliceGroup() {
		this.candidates = new LinkedHashSet<ASTSlice>();
	}

	public void addCandidate(ASTSlice slice) {
		Set<ASTSlice> slicesToBeRemoved = new LinkedHashSet<ASTSlice>();
		for(ASTSlice previousSlice : candidates) {
			if(previousSlice.getNumberOfSliceStatements() == slice.getNumberOfSliceStatements() &&
					previousSlice.getNumberOfDuplicatedStatements() == slice.getNumberOfDuplicatedStatements()) {
				slicesToBeRemoved.add(previousSlice);
			}
		}
		this.candidates.removeAll(slicesToBeRemoved);
		this.candidates.add(slice);
	}

	public Set<ASTSlice> getCandidates() {
		return candidates;
	}

	public TypeDeclaration getSourceTypeDeclaration() {
		return ((ASTSlice)candidates.toArray()[0]).getSourceTypeDeclaration();
	}

	public MethodDeclaration getSourceMethodDeclaration() {
		return ((ASTSlice)candidates.toArray()[0]).getSourceMethodDeclaration();
	}

	public VariableDeclaration getLocalVariableCriterion() {
		return ((ASTSlice)candidates.toArray()[0]).getLocalVariableCriterion();
	}

	public void setAverageNumberOfExtractedStatementsInGroup(double averageNumberOfExtractedStatementsInGroup) {
		this.averageNumberOfExtractedStatementsInGroup = averageNumberOfExtractedStatementsInGroup;
	}

	public void setAverageNumberOfDuplicatedStatementsInGroup(double averageNumberOfDuplicatedStatementsInGroup) {
		this.averageNumberOfDuplicatedStatementsInGroup = averageNumberOfDuplicatedStatementsInGroup;
	}

	public void setAverageDuplicationRatioInGroup(double averageDuplicationRatioInGroup) {
		this.averageDuplicationRatioInGroup = averageDuplicationRatioInGroup;
	}

	public void setMaximumNumberOfExtractedStatementsInGroup(int maximumNumberOfExtractedStatementsInGroup) {
		this.maximumNumberOfExtractedStatementsInGroup = maximumNumberOfExtractedStatementsInGroup;
	}

	public int compareTo(ASTSliceGroup other) {
		double duplicationRatio1 = this.averageDuplicationRatioInGroup;
		double duplicationRatio2 = other.averageDuplicationRatioInGroup;
		
		if(duplicationRatio1 < duplicationRatio2)
			return -1;
		else if(duplicationRatio1 > duplicationRatio2)
			return 1;
		//same duplication ratio
		double averageNumberOfDuplicatedStatements1 = this.averageNumberOfDuplicatedStatementsInGroup;
		double averageNumberOfDuplicatedStatements2 = other.averageNumberOfDuplicatedStatementsInGroup;
		
		if(averageNumberOfDuplicatedStatements1 != 0 && averageNumberOfDuplicatedStatements2 != 0) {
			if(averageNumberOfDuplicatedStatements1 < averageNumberOfDuplicatedStatements2)
				return -1;
			else if(averageNumberOfDuplicatedStatements1 > averageNumberOfDuplicatedStatements2)
				return 1;
		}
		
		int maximumNumberOfExtractedStatements1 = this.maximumNumberOfExtractedStatementsInGroup;
		int maximumNumberOfExtractedStatements2 = other.maximumNumberOfExtractedStatementsInGroup;
		
		if(averageNumberOfDuplicatedStatements1 == 0 && averageNumberOfDuplicatedStatements2 == 0) {
			if(maximumNumberOfExtractedStatements1 < maximumNumberOfExtractedStatements2)
				return 1;
			else if(maximumNumberOfExtractedStatements1 > maximumNumberOfExtractedStatements2)
				return -1;
		}
		
		double averageNumberOfExtractedStatements1 = this.averageNumberOfExtractedStatementsInGroup;
		double averageNumberOfExtractedStatements2 = other.averageNumberOfExtractedStatementsInGroup;
		
		if(averageNumberOfExtractedStatements1 < averageNumberOfExtractedStatements2)
			return 1;
		else if(averageNumberOfExtractedStatements1 > averageNumberOfExtractedStatements2)
			return -1;
		
		String group1 = this.getSourceTypeDeclaration().resolveBinding().getQualifiedName() + "::" +
						this.getSourceMethodDeclaration().resolveBinding().toString() + "::" +
						this.getLocalVariableCriterion().getName().getIdentifier();
		
		String group2 = other.getSourceTypeDeclaration().resolveBinding().getQualifiedName() + "::" +
						other.getSourceMethodDeclaration().resolveBinding().toString() + "::" +
						other.getLocalVariableCriterion().getName().getIdentifier();
		return group1.compareTo(group2);
	}
}
