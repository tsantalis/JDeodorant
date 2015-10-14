package gr.uom.java.history;

import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.decomposition.MethodBodyObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class MethodEvolution implements Evolution {
	private Map<ProjectVersionPair, Double> methodSimilarityMap;
	private Map<ProjectVersionPair, Double> methodChangeMap;
	private Map<ProjectVersion, String> methodCodeMap;
	
	public MethodEvolution(ProjectEvolution projectEvolution, IMethod selectedMethod, IProgressMonitor monitor) {
		this.methodSimilarityMap = new LinkedHashMap<ProjectVersionPair, Double>();
		this.methodChangeMap = new LinkedHashMap<ProjectVersionPair, Double>();
		this.methodCodeMap = new LinkedHashMap<ProjectVersion, String>();
		List<Entry<ProjectVersion, IJavaProject>> projectEntries = projectEvolution.getProjectEntries();
		String declaringTypeName = selectedMethod.getDeclaringType().getFullyQualifiedName('.');
		if(monitor != null)
			monitor.beginTask("Comparing method " + selectedMethod.getElementName(), projectEntries.size()-1);
		
		try {
			Entry<ProjectVersion, IJavaProject> currentEntry = projectEntries.get(0);
			ProjectVersion currentProjectVersion = currentEntry.getKey();
			IJavaProject currentProject = currentEntry.getValue();
			IType currentType = currentProject.findType(declaringTypeName);
			IMethod currentMethod = null;
			if(currentType != null) {
				for(IMethod method : currentType.getMethods()) {
					if(method.isSimilar(selectedMethod)) {
						currentMethod = method;
						break;
					}
				}
			}
			List<String> currentStringRepresentation = getStringRepresentation(currentMethod, currentProjectVersion);
			
			for(int i=1; i<projectEntries.size(); i++) {
				if(monitor != null && monitor.isCanceled())
	    			throw new OperationCanceledException();
				Entry<ProjectVersion, IJavaProject> nextEntry = projectEntries.get(i);
				ProjectVersion nextProjectVersion = nextEntry.getKey();
				IJavaProject nextProject = nextEntry.getValue();
				if(monitor != null)
					monitor.subTask("Comparing method " + selectedMethod.getElementName() + " between versions " + currentProjectVersion + " and " + nextProjectVersion);
				IType nextType = nextProject.findType(declaringTypeName);
				IMethod nextMethod = null;
				if(nextType != null) {
					for(IMethod method : nextType.getMethods()) {
						if(method.isSimilar(selectedMethod)) {
							nextMethod = method;
							break;
						}
					}
				}
				List<String> nextStringRepresentation = getStringRepresentation(nextMethod, nextProjectVersion);
				ProjectVersionPair pair = new ProjectVersionPair(currentProjectVersion, nextProjectVersion);
				if(currentStringRepresentation != null && nextStringRepresentation != null) {
					int editDistance = editDistance(currentStringRepresentation, nextStringRepresentation);
					int maxSize = Math.max(currentStringRepresentation.size(), nextStringRepresentation.size());
					double undirectedSimilarity = (double)(maxSize - editDistance)/(double)maxSize;
					methodSimilarityMap.put(pair, undirectedSimilarity);
					double change = (double)editDistance/(double)maxSize;
					methodChangeMap.put(pair, change);
				}
				else {
					methodSimilarityMap.put(pair, null);
					methodChangeMap.put(pair, null);
				}
				currentProjectVersion = nextProjectVersion;
				currentStringRepresentation = nextStringRepresentation;
				if(monitor != null)
					monitor.worked(1);
			}
			if(monitor != null)
				monitor.done();
		}
		catch(JavaModelException e) {
			e.printStackTrace();
		}
	}

	private List<String> getStringRepresentation(IMethod method, ProjectVersion version) {
		List<String> stringRepresentation = null;
		if(method != null) {
			ICompilationUnit iCompilationUnit = method.getCompilationUnit();
			ASTParser parser = ASTParser.newParser(ASTReader.JLS);
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
				methodCodeMap.put(version, matchingMethodDeclaration.toString());
				ASTInformationGenerator.setCurrentITypeRoot(iCompilationUnit);
				MethodBodyObject methodBody = new MethodBodyObject(matchingMethodDeclaration.getBody());
				stringRepresentation = methodBody.stringRepresentation();
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

	public Set<Entry<ProjectVersionPair, Double>> getSimilarityEntries() {
		return methodSimilarityMap.entrySet();
	}

	public Set<Entry<ProjectVersionPair, Double>> getChangeEntries() {
		return methodChangeMap.entrySet();
	}

	public String getCode(ProjectVersion projectVersion) {
		return methodCodeMap.get(projectVersion);
	}
}
