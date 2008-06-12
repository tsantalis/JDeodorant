package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.util.StatementExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class TypeCheckEliminationResults {
	private Map<TypeCheckElimination, Integer> systemOccurrencesMap;
	private Map<TypeCheckElimination, Integer> classOccurrencesMap;
	private Map<TypeCheckElimination, Double> averageNumberOfStatementsMap;
	private List<TypeCheckElimination> typeCheckEliminations;
	private Set<TypeCheckEliminationGroup> typeCheckEliminationGroups;
	
	public TypeCheckEliminationResults() {
		this.systemOccurrencesMap = new HashMap<TypeCheckElimination, Integer>();
		this.classOccurrencesMap = new HashMap<TypeCheckElimination, Integer>();
		this.averageNumberOfStatementsMap = new HashMap<TypeCheckElimination, Double>();
		this.typeCheckEliminations = new ArrayList<TypeCheckElimination>();
		this.typeCheckEliminationGroups = new TreeSet<TypeCheckEliminationGroup>();
	}
	
	public void addGroup(List<TypeCheckElimination> typeCheckEliminations) {
		this.typeCheckEliminations.addAll(typeCheckEliminations);
		Map<TypeDeclaration, ArrayList<TypeCheckElimination>> typeDeclarationMap = new HashMap<TypeDeclaration, ArrayList<TypeCheckElimination>>();
		double totalNumberOfAverageStatements = 0;
		for(TypeCheckElimination elimination : typeCheckEliminations) {
			TypeDeclaration typeCheckClass = elimination.getTypeCheckClass();
			if(typeDeclarationMap.containsKey(typeCheckClass)) {
				ArrayList<TypeCheckElimination> tempTypeCheckEliminations = typeDeclarationMap.get(typeCheckClass);
				tempTypeCheckEliminations.add(elimination);
			}
			else {
				ArrayList<TypeCheckElimination> tempTypeCheckEliminations = new ArrayList<TypeCheckElimination>();
				tempTypeCheckEliminations.add(elimination);
				typeDeclarationMap.put(typeCheckClass, tempTypeCheckEliminations);
			}
			systemOccurrencesMap.put(elimination, typeCheckEliminations.size());
			double avgNumberOfStatements = getAvgNumberOfStatements(elimination);
			averageNumberOfStatementsMap.put(elimination, avgNumberOfStatements);
			totalNumberOfAverageStatements += avgNumberOfStatements;
		}
		TypeCheckEliminationGroup group = new TypeCheckEliminationGroup(typeCheckEliminations, totalNumberOfAverageStatements/typeCheckEliminations.size());
		typeCheckEliminationGroups.add(group);
		for(TypeDeclaration typeCheckClass : typeDeclarationMap.keySet()) {
			ArrayList<TypeCheckElimination> tempTypeCheckEliminations = typeDeclarationMap.get(typeCheckClass);
			for(TypeCheckElimination elimination : tempTypeCheckEliminations) {
				classOccurrencesMap.put(elimination, tempTypeCheckEliminations.size());
			}
		}
	}
	
	private double getAvgNumberOfStatements(TypeCheckElimination elimination) {
		List<ArrayList<Statement>> typeCheckStatements = elimination.getTypeCheckStatements();
		ArrayList<Statement> defaultCaseStatements = elimination.getDefaultCaseStatements();
		if(!defaultCaseStatements.isEmpty())
			typeCheckStatements.add(defaultCaseStatements);
		StatementExtractor statementExtractor = new StatementExtractor();
		int numberOfCases = typeCheckStatements.size();
		int totalNumberOfStatements = 0;
		for(ArrayList<Statement> statements : typeCheckStatements) {
			for(Statement statement : statements) {
				totalNumberOfStatements += statementExtractor.getTotalNumberOfStatements(statement);
			}
		}
		return (double)totalNumberOfStatements/(double)numberOfCases;
	}
	
	public int getSystemOccurrences(TypeCheckElimination elimination) {
		return systemOccurrencesMap.get(elimination);
	}

	public int getClassOccurrences(TypeCheckElimination elimination) {
		return classOccurrencesMap.get(elimination);
	}

	public double getAverageNumberOfStatements(TypeCheckElimination elimination) {
		return averageNumberOfStatementsMap.get(elimination);
	}
	
	public List<TypeCheckElimination> getTypeCheckEliminations() {
		return typeCheckEliminations;
	}
	
	public TypeCheckEliminationGroup getTypeCheckEliminationGroup(TypeCheckElimination elimination) {
		for(TypeCheckEliminationGroup group : typeCheckEliminationGroups) {
			if(group.containsTypeCheckElimination(elimination))
				return group;
		}
		return null;
	}
	
	public int getPositionOfTypeCheckEliminationGroup(TypeCheckElimination elimination) {
		int counter = 0;
		for(TypeCheckEliminationGroup group : typeCheckEliminationGroups) {
			if(group.containsTypeCheckElimination(elimination))
				return counter;
			counter++;
		}
		return -1;
	}
}
