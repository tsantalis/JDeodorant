package gr.uom.java.history;

import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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

public class FeatureEnvyEvolution {
	private Map<ProjectVersionPair, String> featureEnvyChangeMap;
	private final DecimalFormat decimalFormat = new DecimalFormat("0.000");
	
	public FeatureEnvyEvolution(ProjectEvolution projectEvolution, String methodBindingKey, String targetClassName, IProgressMonitor monitor) {
		this.featureEnvyChangeMap = new LinkedHashMap<ProjectVersionPair, String>();
		List<Entry<ProjectVersion, IJavaProject>> projectEntries = projectEvolution.getProjectEntries();
		if(monitor != null)
			monitor.beginTask("Comparing the selected method", projectEntries.size()-1);
		
		try {
			Entry<ProjectVersion, IJavaProject> currentEntry = projectEntries.get(0);
			ProjectVersion currentProjectVersion = currentEntry.getKey();
			IJavaProject currentProject = currentEntry.getValue();
			IMethod currentMethod = (IMethod)currentProject.findElement(methodBindingKey, null);
			int currentNumberOfEnviedElements = getNumberOfEnviedElements(currentMethod, targetClassName);
			
			for(int i=1; i<projectEntries.size(); i++) {
				Entry<ProjectVersion, IJavaProject> nextEntry = projectEntries.get(i);
				ProjectVersion nextProjectVersion = nextEntry.getKey();
				IJavaProject nextProject = nextEntry.getValue();
				if(monitor != null)
					monitor.subTask("Comparing the selected method between versions " + currentProjectVersion + " and " + nextProjectVersion);
				IMethod nextMethod = (IMethod)nextProject.findElement(methodBindingKey, null);
				int nextNumberOfEnviedElements = getNumberOfEnviedElements(nextMethod, targetClassName);
				ProjectVersionPair pair = new ProjectVersionPair(currentProjectVersion, nextProjectVersion);
				if(currentMethod != null && nextMethod != null) {
					int maxNumberOfEnviedEntities = Math.max(currentNumberOfEnviedElements, nextNumberOfEnviedElements);
					//double similarity = (double)(maxNumberOfEnviedEntities - Math.abs(nextNumberOfEnviedElements-currentNumberOfEnviedElements))/(double)maxNumberOfEnviedEntities;
					double change = (double)Math.abs(nextNumberOfEnviedElements-currentNumberOfEnviedElements)/(double)maxNumberOfEnviedEntities;
					featureEnvyChangeMap.put(pair, decimalFormat.format(change));
				}
				else {
					featureEnvyChangeMap.put(pair, "N/A");
				}
				currentProjectVersion = nextProjectVersion;
				currentNumberOfEnviedElements = nextNumberOfEnviedElements;
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
	
	private int getNumberOfEnviedElements(IMethod method, String targetClassName) {
		int numberOfEnviedElements = 0;
		if(method != null) {
			ICompilationUnit iCompilationUnit = method.getCompilationUnit();
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
				Map<AbstractVariable, ArrayList<MethodInvocationObject>> nonDistinctInvokedMethodsThroughFields = methodBody.getNonDistinctInvokedMethodsThroughFields();
				for(AbstractVariable variable : nonDistinctInvokedMethodsThroughFields.keySet()) {
					if(variable.getVariableType().equals(targetClassName) && variable instanceof PlainVariable) {
						ArrayList<MethodInvocationObject> methodInvocations = nonDistinctInvokedMethodsThroughFields.get(variable);
						numberOfEnviedElements += methodInvocations.size();
					}
				}
				Map<AbstractVariable, ArrayList<MethodInvocationObject>> nonDistinctInvokedMethodsThroughParameters = methodBody.getNonDistinctInvokedMethodsThroughParameters();
				for(AbstractVariable variable : nonDistinctInvokedMethodsThroughParameters.keySet()) {
					if(variable.getVariableType().equals(targetClassName) && variable instanceof PlainVariable) {
						ArrayList<MethodInvocationObject> methodInvocations = nonDistinctInvokedMethodsThroughParameters.get(variable);
						numberOfEnviedElements += methodInvocations.size();
					}
				}
				List<AbstractVariable> nonDistinctDefinedFieldsThroughFields = methodBody.getNonDistinctDefinedFieldsThroughFields();
				for(AbstractVariable variable : nonDistinctDefinedFieldsThroughFields) {
					if(variable.getVariableType().equals(targetClassName))
						numberOfEnviedElements++;
				}
				List<AbstractVariable> nonDistinctUsedFieldsThroughFields = methodBody.getNonDistinctUsedFieldsThroughFields();
				for(AbstractVariable variable : nonDistinctUsedFieldsThroughFields) {
					if(variable.getVariableType().equals(targetClassName))
						numberOfEnviedElements++;
				}
				List<AbstractVariable> nonDistinctDefinedFieldsThroughParameters = methodBody.getNonDistinctDefinedFieldsThroughParameters();
				for(AbstractVariable variable : nonDistinctDefinedFieldsThroughParameters) {
					if(variable.getVariableType().equals(targetClassName))
						numberOfEnviedElements++;
				}
				List<AbstractVariable> nonDistinctUsedFieldsThroughParameters = methodBody.getNonDistinctUsedFieldsThroughParameters();
				for(AbstractVariable variable : nonDistinctUsedFieldsThroughParameters) {
					if(variable.getVariableType().equals(targetClassName))
						numberOfEnviedElements++;
				}
			}
		}
		return numberOfEnviedElements;
	}

	public Set<Entry<ProjectVersionPair, String>> getFeatureEnvyChangeEntries() {
		return featureEnvyChangeMap.entrySet();
	}
}
