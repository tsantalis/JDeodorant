package gr.uom.java.history;

import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.distance.MoveMethodCandidateRefactoring;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class FeatureEnvyEvolution implements Evolution {
	private Map<ProjectVersionPair, Double> featureEnvySimilarityMap;
	private Map<ProjectVersionPair, Double> featureEnvyChangeMap;
	private Map<ProjectVersion, String> methodCodeMap;

	public FeatureEnvyEvolution(ProjectEvolution projectEvolution, MoveMethodCandidateRefactoring moveMethodRefactoring, IProgressMonitor monitor) {
		this.featureEnvySimilarityMap = new LinkedHashMap<ProjectVersionPair, Double>();
		this.featureEnvyChangeMap = new LinkedHashMap<ProjectVersionPair, Double>();
		this.methodCodeMap = new LinkedHashMap<ProjectVersion, String>();
		IMethod sourceMethod = (IMethod)moveMethodRefactoring.getSourceMethodDeclaration().resolveBinding().getJavaElement();
		String targetClassName = moveMethodRefactoring.getTarget();
		List<Entry<ProjectVersion, IJavaProject>> projectEntries = projectEvolution.getProjectEntries();
		String sourceClassName = sourceMethod.getDeclaringType().getFullyQualifiedName('.');
		if(monitor != null)
			monitor.beginTask("Comparing method " + sourceMethod.getElementName(), projectEntries.size()-1);
		
		try {
			Entry<ProjectVersion, IJavaProject> currentEntry = projectEntries.get(0);
			ProjectVersion currentProjectVersion = currentEntry.getKey();
			IJavaProject currentProject = currentEntry.getValue();
			IType currentSourceType = currentProject.findType(sourceClassName);
			IMethod currentMethod = null;
			if(currentSourceType != null) {
				for(IMethod method : currentSourceType.getMethods()) {
					if(method.isSimilar(sourceMethod)) {
						currentMethod = method;
						break;
					}
				}
			}
			int currentNumberOfEnviedElements = getNumberOfEnviedElements(currentMethod, targetClassName, currentProjectVersion);
			IType currentTargetType = currentProject.findType(targetClassName);
			int currentNumberOfAccessibleElements = 0;
			if(currentTargetType != null) {
				currentNumberOfAccessibleElements = getNumberOfAccessibleMembers(currentTargetType);
			}
			
			for(int i=1; i<projectEntries.size(); i++) {
				if(monitor != null && monitor.isCanceled())
	    			throw new OperationCanceledException();
				Entry<ProjectVersion, IJavaProject> nextEntry = projectEntries.get(i);
				ProjectVersion nextProjectVersion = nextEntry.getKey();
				IJavaProject nextProject = nextEntry.getValue();
				if(monitor != null)
					monitor.subTask("Comparing method " + sourceMethod.getElementName() + " between versions " + currentProjectVersion + " and " + nextProjectVersion);
				IType nextSourceType = nextProject.findType(sourceClassName);
				IMethod nextMethod = null;
				if(nextSourceType != null) {
					for(IMethod method : nextSourceType.getMethods()) {
						if(method.isSimilar(sourceMethod)) {
							nextMethod = method;
							break;
						}
					}
				}
				int nextNumberOfEnviedElements = getNumberOfEnviedElements(nextMethod, targetClassName, nextProjectVersion);
				IType nextTargetType = nextProject.findType(targetClassName);
				int nextNumberOfAccessibleElements = 0;
				if(nextTargetType != null) {
					nextNumberOfAccessibleElements = getNumberOfAccessibleMembers(nextTargetType);
				}
				
				ProjectVersionPair pair = new ProjectVersionPair(currentProjectVersion, nextProjectVersion);
				if(currentMethod != null && nextMethod != null && currentTargetType != null && nextTargetType != null) {
					int maxNumberOfAccessibleEntities = Math.max(currentNumberOfAccessibleElements, nextNumberOfAccessibleElements);
					double similarity = (double)(maxNumberOfAccessibleEntities - Math.abs(nextNumberOfEnviedElements-currentNumberOfEnviedElements))/(double)maxNumberOfAccessibleEntities;
					featureEnvySimilarityMap.put(pair, similarity);
					double change = (double)Math.abs(nextNumberOfEnviedElements-currentNumberOfEnviedElements)/(double)maxNumberOfAccessibleEntities;
					featureEnvyChangeMap.put(pair, change);
				}
				else {
					featureEnvySimilarityMap.put(pair, null);
					featureEnvyChangeMap.put(pair, null);
				}
				currentProjectVersion = nextProjectVersion;
				currentMethod = nextMethod;
				currentNumberOfEnviedElements = nextNumberOfEnviedElements;
				currentTargetType = nextTargetType;
				currentNumberOfAccessibleElements = nextNumberOfAccessibleElements;
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

	private int getNumberOfEnviedElements(IMethod method, String targetClassName, ProjectVersion version) {
		int numberOfEnviedElements = 0;
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
				Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughFields = methodBody.getInvokedMethodsThroughFields();
				for(AbstractVariable variable : invokedMethodsThroughFields.keySet()) {
					if(variable.getVariableType().equals(targetClassName) && variable instanceof PlainVariable) {
						LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughFields.get(variable);
						numberOfEnviedElements += methodInvocations.size();
					}
				}
				Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughParameters = methodBody.getInvokedMethodsThroughParameters();
				for(AbstractVariable variable : invokedMethodsThroughParameters.keySet()) {
					if(variable.getVariableType().equals(targetClassName) && variable instanceof PlainVariable) {
						LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughParameters.get(variable);
						numberOfEnviedElements += methodInvocations.size();
					}
				}
				Set<AbstractVariable> definedFieldsThroughFields = methodBody.getDefinedFieldsThroughFields();
				for(AbstractVariable variable : definedFieldsThroughFields) {
					if(variable.getVariableType().equals(targetClassName))
						numberOfEnviedElements++;
				}
				Set<AbstractVariable> usedFieldsThroughFields = methodBody.getUsedFieldsThroughFields();
				for(AbstractVariable variable : usedFieldsThroughFields) {
					if(variable.getVariableType().equals(targetClassName))
						numberOfEnviedElements++;
				}
				Set<AbstractVariable> definedFieldsThroughParameters = methodBody.getDefinedFieldsThroughParameters();
				for(AbstractVariable variable : definedFieldsThroughParameters) {
					if(variable.getVariableType().equals(targetClassName))
						numberOfEnviedElements++;
				}
				Set<AbstractVariable> usedFieldsThroughParameters = methodBody.getUsedFieldsThroughParameters();
				for(AbstractVariable variable : usedFieldsThroughParameters) {
					if(variable.getVariableType().equals(targetClassName))
						numberOfEnviedElements++;
				}
			}
		}
		return numberOfEnviedElements;
	}

	private int getNumberOfAccessibleMembers(IType type) throws JavaModelException {
		int accessibleMembers = 0;
		for(IField field : type.getFields()) {
			int flags = field.getFlags();
			if(!Flags.isPrivate(flags) && !Flags.isStatic(flags))
				accessibleMembers++;
		}
		for(IMethod method : type.getMethods()) {
			int flags = method.getFlags();
			if(!Flags.isPrivate(flags) && !Flags.isStatic(flags) && !method.isConstructor() && !method.isMainMethod())
				accessibleMembers++;
		}
		return accessibleMembers;
	}

	public Set<Entry<ProjectVersionPair, Double>> getSimilarityEntries() {
		return featureEnvySimilarityMap.entrySet();
	}

	public Set<Entry<ProjectVersionPair, Double>> getChangeEntries() {
		return featureEnvyChangeMap.entrySet();
	}

	public String getCode(ProjectVersion projectVersion) {
		return methodCodeMap.get(projectVersion);
	}
}
