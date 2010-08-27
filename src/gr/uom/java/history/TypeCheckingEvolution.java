package gr.uom.java.history;

import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.TypeCheckCodeFragmentAnalyzer;
import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckElimination;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class TypeCheckingEvolution implements Evolution {
	private Map<ProjectVersionPair, Double> typeCheckSimilarityMap;
	private Map<ProjectVersionPair, Double> typeCheckChangeMap;
	private Map<ProjectVersionPair, Double> weightedMovingAverageMap;
	private Map<ProjectVersion, String> typeCheckCodeMap;
	
	public TypeCheckingEvolution(ProjectEvolution projectEvolution, TypeCheckElimination selectedTypeCheckElimination, IProgressMonitor monitor) {
		this.typeCheckSimilarityMap = new LinkedHashMap<ProjectVersionPair, Double>();
		this.typeCheckChangeMap = new LinkedHashMap<ProjectVersionPair, Double>();
		this.weightedMovingAverageMap = new LinkedHashMap<ProjectVersionPair, Double>();
		this.typeCheckCodeMap = new LinkedHashMap<ProjectVersion, String>();
		List<Entry<ProjectVersion, IJavaProject>> projectEntries = projectEvolution.getProjectEntries();
		IMethod typeCheckMethod = (IMethod)selectedTypeCheckElimination.getTypeCheckMethod().resolveBinding().getJavaElement();
		String typeCheckClassName = typeCheckMethod.getDeclaringType().getFullyQualifiedName('.');
		if(monitor != null)
			monitor.beginTask("Comparing method " + typeCheckMethod.getElementName(), projectEntries.size()-1);
		
		try {
			Entry<ProjectVersion, IJavaProject> currentEntry = projectEntries.get(0);
			ProjectVersion currentProjectVersion = currentEntry.getKey();
			IJavaProject currentProject = currentEntry.getValue();
			IType currentType = currentProject.findType(typeCheckClassName);
			IMethod currentMethod = null;
			if(currentType != null) {
				for(IMethod method : currentType.getMethods()) {
					if(method.isSimilar(typeCheckMethod)) {
						currentMethod = method;
						break;
					}
				}
			}
			List<String> currentStringRepresentation = getTypeCheckingCodeFragment(currentMethod, selectedTypeCheckElimination, currentProjectVersion);
			
			for(int i=1; i<projectEntries.size(); i++) {
				Entry<ProjectVersion, IJavaProject> nextEntry = projectEntries.get(i);
				ProjectVersion nextProjectVersion = nextEntry.getKey();
				IJavaProject nextProject = nextEntry.getValue();
				if(monitor != null)
					monitor.subTask("Comparing method " + typeCheckMethod.getElementName() + " between versions " + currentProjectVersion + " and " + nextProjectVersion);
				IType nextType = nextProject.findType(typeCheckClassName);
				IMethod nextMethod = null;
				if(nextType != null) {
					for(IMethod method : nextType.getMethods()) {
						if(method.isSimilar(typeCheckMethod)) {
							nextMethod = method;
							break;
						}
					}
				}
				List<String> nextStringRepresentation = getTypeCheckingCodeFragment(nextMethod, selectedTypeCheckElimination, nextProjectVersion);
				ProjectVersionPair pair = new ProjectVersionPair(currentProjectVersion, nextProjectVersion);
				if(currentStringRepresentation != null && nextStringRepresentation != null) {
					int editDistance = editDistance(currentStringRepresentation, nextStringRepresentation);
					int maxSize = Math.max(currentStringRepresentation.size(), nextStringRepresentation.size());
					double undirectedSimilarity = (double)(maxSize - editDistance)/(double)maxSize;
					typeCheckSimilarityMap.put(pair, undirectedSimilarity);
					double change = (double)editDistance/(double)maxSize;
					typeCheckChangeMap.put(pair, change);
				}
				else {
					typeCheckSimilarityMap.put(pair, null);
					typeCheckChangeMap.put(pair, null);
				}
				currentProjectVersion = nextProjectVersion;
				currentStringRepresentation = nextStringRepresentation;
				if(monitor != null)
					monitor.worked(1);
			}
			computeWeightedMovingAverage();
			if(monitor != null)
				monitor.done();
		}
		catch(JavaModelException e) {
			e.printStackTrace();
		}
	}

	private List<String> getTypeCheckingCodeFragment(IMethod method, TypeCheckElimination selectedTypeCheckElimination, ProjectVersion version) {
		List<String> stringRepresentation = null;
		if(method != null) {
			ICompilationUnit iCompilationUnit = method.getCompilationUnit();
			IFile iFile = (IFile)iCompilationUnit.getResource();
			ASTParser parser = ASTParser.newParser(AST.JLS3);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setSource(iCompilationUnit);
			parser.setResolveBindings(true);
			CompilationUnit compilationUnit = (CompilationUnit)parser.createAST(null);
			IType declaringType = method.getDeclaringType();
			TypeDeclaration typeDeclaration = (TypeDeclaration)compilationUnit.findDeclaringNode(declaringType.getKey());
			MethodDeclaration matchingMethodDeclaration = null;
			for(MethodDeclaration methodDeclaration : typeDeclaration.getMethods()) {
				IMethod resolvedMethod = (IMethod)methodDeclaration.resolveBinding().getJavaElement();
				if(resolvedMethod.isSimilar(method)) {
					matchingMethodDeclaration = methodDeclaration;
					break;
				}
			}
			if(matchingMethodDeclaration != null && matchingMethodDeclaration.getBody() != null) {
				ASTInformationGenerator.setCurrentITypeRoot(iCompilationUnit);
				MethodBodyObject methodBody = new MethodBodyObject(matchingMethodDeclaration.getBody());
				List<TypeCheckElimination> finalTypeCheckEliminations = new ArrayList<TypeCheckElimination>();
				List<TypeCheckElimination> initialTypeCheckEliminations = methodBody.generateTypeCheckEliminations();
				for(TypeCheckElimination typeCheckElimination : initialTypeCheckEliminations) {
    				if(!typeCheckElimination.allTypeCheckBranchesAreEmpty()) {
    					new TypeCheckCodeFragmentAnalyzer(typeCheckElimination, typeDeclaration, matchingMethodDeclaration, iFile);
    					if((typeCheckElimination.getTypeField() != null || typeCheckElimination.getTypeLocalVariable() != null || typeCheckElimination.getTypeMethodInvocation() != null) &&
    							typeCheckElimination.allTypeCheckingsContainStaticFieldOrSubclassType() && typeCheckElimination.isApplicable()) {
    						finalTypeCheckEliminations.add(typeCheckElimination);
    					}
    				}
				}
				TypeCheckElimination matchingTypeCheckElimination = null;
				for(TypeCheckElimination typeCheckElimination : finalTypeCheckEliminations) {
					if(typeCheckElimination.matches(selectedTypeCheckElimination)) {
						matchingTypeCheckElimination = typeCheckElimination;
						break;
					}
				}
				if(matchingTypeCheckElimination != null) {
					stringRepresentation = matchingTypeCheckElimination.getTypeCheckCompositeStatement().stringRepresentation();
					typeCheckCodeMap.put(version, matchingTypeCheckElimination.getTypeCheckCodeFragment().toString());
				}
			}
		}
		return stringRepresentation;
	}

	private int editDistance(List<String> a, List<String> b) {
		int[][] d = new int[a.size()+1][b.size()+1];
		
		for(int i=0; i<=a.size(); i++)
			d[i][0] = i;
		for(int j=0; j<=b.size(); j++)
			d[0][j] = j;
		
		int j=1;
		for(String s1 : b) {
			int i = 1;
			for(String s2 : a) {
				if(s1.equals(s2))
					d[i][j] = d[i-1][j-1];
				else {
					int deletion = d[i-1][j] + 1;
					int insertion = d[i][j-1] + 1;
					int substitution = d[i-1][j-1] + 1;
					int min = Math.min(deletion, insertion);
					min = Math.min(min, substitution);
					d[i][j] = min;
				}
				i++;
			}
			j++;
		}
		return d[a.size()][b.size()];
	}

	private void computeWeightedMovingAverage() {
		Set<ProjectVersionPair> validPairs = new LinkedHashSet<ProjectVersionPair>();
		for(ProjectVersionPair pair : typeCheckChangeMap.keySet()) {
			if(typeCheckChangeMap.get(pair) != null)
				validPairs.add(pair);
		}
		int numberOfValidPairs = validPairs.size();
		double denominator = (double)(numberOfValidPairs * (numberOfValidPairs + 1)) / 2.0;
		int counter = 1;
		for(ProjectVersionPair pair : validPairs) {
			double weightedMovingAverage = (double)counter/denominator;
			weightedMovingAverageMap.put(pair, weightedMovingAverage);
			counter++;
		}
	}

	public Set<Entry<ProjectVersionPair, Double>> getSimilarityEntries() {
		return typeCheckSimilarityMap.entrySet();
	}

	public Set<Entry<ProjectVersionPair, Double>> getChangeEntries() {
		return typeCheckChangeMap.entrySet();
	}

	public String getCode(ProjectVersion projectVersion) {
		return typeCheckCodeMap.get(projectVersion);
	}
}
