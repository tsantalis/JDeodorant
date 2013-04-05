package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.decomposition.ASTNodeDifference;
import gr.uom.java.ast.decomposition.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.CompositeStatementObject;
import gr.uom.java.ast.decomposition.StatementObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.BasicBlock;
import gr.uom.java.ast.decomposition.cfg.CFGBranchIfNode;
import gr.uom.java.ast.decomposition.cfg.GraphEdge;
import gr.uom.java.ast.decomposition.cfg.GraphNode;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGDataDependence;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGMethodEntryNode;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PDGSlice;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;

public class PDGMapper {
	private PDG pdg1;
	private PDG pdg2;
	private ControlDependenceTreeNode controlDependenceTreePDG1;
	private ControlDependenceTreeNode controlDependenceTreePDG2;
	private ICompilationUnit iCompilationUnit1;
	private ICompilationUnit iCompilationUnit2;
	private MappingState maximumStateWithMinimumDifferences;
	private TreeSet<PDGNode> mappedNodesG1;
	private TreeSet<PDGNode> mappedNodesG2;
	private Set<PDGNode> nonMappedNodesG1;
	private Set<PDGNode> nonMappedNodesG2;
	private TreeSet<PDGNode> nonMappedNodesSliceUnionG1;
	private TreeSet<PDGNode> nonMappedNodesSliceUnionG2;
	private Map<String, ArrayList<AbstractVariable>> commonPassedParameters;
	private Set<AbstractVariable> passedParametersG1;
	private Set<AbstractVariable> passedParametersG2;
	private Set<AbstractVariable> accessedLocalFieldsG1;
	private Set<AbstractVariable> accessedLocalFieldsG2;
	private Set<MethodInvocationObject> accessedLocalMethodsG1;
	private Set<MethodInvocationObject> accessedLocalMethodsG2;
	private Set<AbstractVariable> declaredVariablesInMappedNodesUsedByNonMappedNodesG1;
	private Set<AbstractVariable> declaredVariablesInMappedNodesUsedByNonMappedNodesG2;
	private IProgressMonitor monitor;
	
	public PDGMapper(PDG pdg1, PDG pdg2, IProgressMonitor monitor) {
		this.pdg1 = pdg1;
		this.pdg2 = pdg2;
		this.controlDependenceTreePDG1 = new ControlDependenceTreeNode(null, pdg1.getEntryNode());
		this.controlDependenceTreePDG2 = new ControlDependenceTreeNode(null, pdg2.getEntryNode());
		CompilationUnit cu1 = (CompilationUnit)pdg1.getMethod().getMethodDeclaration().getRoot();
		this.iCompilationUnit1 = (ICompilationUnit)cu1.getJavaElement();
		CompilationUnit cu2 = (CompilationUnit)pdg2.getMethod().getMethodDeclaration().getRoot();
		this.iCompilationUnit2 = (ICompilationUnit)cu2.getJavaElement();
		this.nonMappedNodesG1 = new LinkedHashSet<PDGNode>();
		this.nonMappedNodesG2 = new LinkedHashSet<PDGNode>();
		this.nonMappedNodesSliceUnionG1 = new TreeSet<PDGNode>();
		this.nonMappedNodesSliceUnionG2 = new TreeSet<PDGNode>();
		this.commonPassedParameters = new LinkedHashMap<String, ArrayList<AbstractVariable>>();
		this.passedParametersG1 = new LinkedHashSet<AbstractVariable>();
		this.passedParametersG2 = new LinkedHashSet<AbstractVariable>();
		this.accessedLocalFieldsG1 = new LinkedHashSet<AbstractVariable>();
		this.accessedLocalFieldsG2 = new LinkedHashSet<AbstractVariable>();
		this.accessedLocalMethodsG1 = new LinkedHashSet<MethodInvocationObject>();
		this.accessedLocalMethodsG2 = new LinkedHashSet<MethodInvocationObject>();
		this.declaredVariablesInMappedNodesUsedByNonMappedNodesG1 = new LinkedHashSet<AbstractVariable>();
		this.declaredVariablesInMappedNodesUsedByNonMappedNodesG2 = new LinkedHashSet<AbstractVariable>();
		this.monitor = monitor;
		processPDGNodes();
		this.mappedNodesG1 = maximumStateWithMinimumDifferences.getMappedNodesG1();
		this.mappedNodesG2 = maximumStateWithMinimumDifferences.getMappedNodesG2();
		findNonMappedNodes(pdg1, mappedNodesG1, nonMappedNodesG1);
		findNonMappedNodes(pdg2, mappedNodesG2, nonMappedNodesG2);
		findDeclaredVariablesInMappedNodesUsedByNonMappedNodes(mappedNodesG1, nonMappedNodesG1, declaredVariablesInMappedNodesUsedByNonMappedNodesG1);
		findDeclaredVariablesInMappedNodesUsedByNonMappedNodes(mappedNodesG2, nonMappedNodesG2, declaredVariablesInMappedNodesUsedByNonMappedNodesG2);
		computeSliceForNonMappedNodes(pdg1, mappedNodesG1, nonMappedNodesG1, nonMappedNodesSliceUnionG1);
		computeSliceForNonMappedNodes(pdg2, mappedNodesG2, nonMappedNodesG2, nonMappedNodesSliceUnionG2);
		findPassedParameters();
		findLocallyAccessedFields(pdg1, mappedNodesG1, accessedLocalFieldsG1, accessedLocalMethodsG1);
		findLocallyAccessedFields(pdg2, mappedNodesG2, accessedLocalFieldsG2, accessedLocalMethodsG2);
	}

	private void computeSliceForNonMappedNodes(PDG pdg, TreeSet<PDGNode> mappedNodes, Set<PDGNode> nonMappedNodes, Set<PDGNode> nonMappedNodesSliceUnion) {
		List<BasicBlock> basicBlocks = pdg.getBasicBlocks();
		//we need a strategy to select the appropriate basic block according to the region of the duplicated code
		BasicBlock block = basicBlocks.get(0);
		if(!nonMappedNodes.isEmpty()) {
			PDGSlice subgraph = new PDGSlice(pdg, block);
			for(PDGNode nodeCriterion : nonMappedNodes) {
				int nodeId = nodeCriterion.getId();
				if(nodeId >= mappedNodes.first().getId() && nodeId <= mappedNodes.last().getId())
					nonMappedNodesSliceUnion.addAll(subgraph.computeSlice(nodeCriterion));
				else
					nonMappedNodesSliceUnion.add(nodeCriterion);
			}
		}
	}

	private void findLocallyAccessedFields(PDG pdg, Set<PDGNode> mappedNodes, Set<AbstractVariable> accessedFields,
			Set<MethodInvocationObject> accessedMethods) {
		Set<PlainVariable> usedLocalFields = new LinkedHashSet<PlainVariable>();
		Set<MethodInvocationObject> accessedLocalMethods = new LinkedHashSet<MethodInvocationObject>();
		Iterator<GraphNode> iterator = pdg.getNodeIterator();
		while(iterator.hasNext()) {
			PDGNode pdgNode = (PDGNode)iterator.next();
			if(mappedNodes.contains(pdgNode)) {
				AbstractStatement abstractStatement = pdgNode.getStatement();
				if(abstractStatement instanceof StatementObject) {
					StatementObject statement = (StatementObject)abstractStatement;
					usedLocalFields.addAll(statement.getUsedFieldsThroughThisReference());
					accessedLocalMethods.addAll(statement.getInvokedMethodsThroughThisReference());
				}
				else if(abstractStatement instanceof CompositeStatementObject) {
					CompositeStatementObject composite = (CompositeStatementObject)abstractStatement;
					usedLocalFields.addAll(composite.getUsedFieldsThroughThisReferenceInExpressions());
					accessedLocalMethods.addAll(composite.getInvokedMethodsThroughThisReferenceInExpressions());
				}
			}
		}
		ITypeBinding declaringClassTypeBinding = pdg.getMethod().getMethodDeclaration().resolveBinding().getDeclaringClass();
		Set<VariableDeclaration> fieldsAccessedInMethod = pdg.getFieldsAccessedInMethod();
		for(PlainVariable variable : usedLocalFields) {
			for(VariableDeclaration fieldDeclaration : fieldsAccessedInMethod) {
				if(variable.getVariableBindingKey().equals(fieldDeclaration.resolveBinding().getKey()) &&
						fieldDeclaration.resolveBinding().getDeclaringClass().isEqualTo(declaringClassTypeBinding)) {
					accessedFields.add(variable);
					break;
				}
			}
		}
		for(MethodInvocationObject invocation : accessedLocalMethods) {
			if(invocation.getMethodInvocation().resolveMethodBinding().getDeclaringClass().isEqualTo(declaringClassTypeBinding)) {
				accessedMethods.add(invocation);
			}
		}
	}

	private void findNonMappedNodes(PDG pdg, Set<PDGNode> mappedNodes, Set<PDGNode> nonMappedNodes) {
		Iterator<GraphNode> iterator = pdg.getNodeIterator();
		while(iterator.hasNext()) {
			PDGNode pdgNode = (PDGNode)iterator.next();
			if(!mappedNodes.contains(pdgNode)) {
				nonMappedNodes.add(pdgNode);
			}
		}
	}

	private Set<AbstractVariable> extractPassedParameters(PDG pdg,  Set<PDGNode> mappedNodes) {
		Set<AbstractVariable> passedParameters = new LinkedHashSet<AbstractVariable>();
		for(GraphEdge edge : pdg.getEdges()) {
			PDGDependence dependence = (PDGDependence)edge;
			PDGNode srcPDGNode = (PDGNode)dependence.getSrc();
			PDGNode dstPDGNode = (PDGNode)dependence.getDst();
			if(dependence instanceof PDGDataDependence) {
				PDGDataDependence dataDependence = (PDGDataDependence)dependence;
				if(!mappedNodes.contains(srcPDGNode) && mappedNodes.contains(dstPDGNode)) {
					passedParameters.add(dataDependence.getData());
				}
			}
		}
		return passedParameters;
	}

	private void findPassedParameters() {
		Set<AbstractVariable> passedParametersG1 = extractPassedParameters(pdg1, mappedNodesG1);
		Set<AbstractVariable> passedParametersG2 = extractPassedParameters(pdg2, mappedNodesG2);
		for(PDGNodeMapping nodeMapping : maximumStateWithMinimumDifferences.getNodeMappings()) {
			PDGNode nodeG1 = nodeMapping.getNodeG1();
			PDGNode nodeG2 = nodeMapping.getNodeG2();
			Set<AbstractVariable> dataDependences1 = nodeG1.incomingDataDependencesFromNodesDeclaringVariables();
			Set<AbstractVariable> dataDependences2 = nodeG2.incomingDataDependencesFromNodesDeclaringVariables();
			dataDependences1.retainAll(passedParametersG1);
			dataDependences2.retainAll(passedParametersG2);
			//remove already mapped parameters
			for(ArrayList<AbstractVariable> variableDeclarations : commonPassedParameters.values()) {
				AbstractVariable variableDeclaration1 = variableDeclarations.get(0);
				AbstractVariable variableDeclaration2 = variableDeclarations.get(1);
				dataDependences1.remove(variableDeclaration1);
				dataDependences2.remove(variableDeclaration2);
			}
			if(dataDependences1.size() == 1 && dataDependences2.size() == 1) {
				List<AbstractVariable> variables1 = new ArrayList<AbstractVariable>(dataDependences1);
				List<AbstractVariable> variables2 = new ArrayList<AbstractVariable>(dataDependences2);
				AbstractVariable variable1 = variables1.get(0);
				AbstractVariable variable2 = variables2.get(0);
				if(passedParametersG1.contains(variable1) && passedParametersG2.contains(variable2)) {
					ArrayList<AbstractVariable> variableDeclarations = new ArrayList<AbstractVariable>();
					variableDeclarations.add(variable1);
					variableDeclarations.add(variable2);
					commonPassedParameters.put(variable1.getVariableBindingKey(), variableDeclarations);
					passedParametersG1.remove(variable1);
					passedParametersG2.remove(variable2);
				}
			}
		}
		this.passedParametersG1.addAll(passedParametersG1);
		this.passedParametersG2.addAll(passedParametersG2);
	}

	public PDG getPDG1() {
		return pdg1;
	}

	public PDG getPDG2() {
		return pdg2;
	}

	private void findDeclaredVariablesInMappedNodesUsedByNonMappedNodes(Set<PDGNode> mappedNodes, Set<PDGNode> nonMappedNodes, Set<AbstractVariable> variables) {
		for(PDGNode mappedNode : mappedNodes) {
			for(Iterator<AbstractVariable> declaredVariableIterator = mappedNode.getDeclaredVariableIterator(); declaredVariableIterator.hasNext();) {
				AbstractVariable declaredVariable = declaredVariableIterator.next();
				for(PDGNode node : nonMappedNodes) {
					if(node.usesLocalVariable(declaredVariable) || node.definesLocalVariable(declaredVariable)) {
						variables.add(declaredVariable);
						break;
					}
				}
			}
		}
	}

	public MappingState getMaximumStateWithMinimumDifferences() {
		return maximumStateWithMinimumDifferences;
	}

	public Set<PDGNode> getNonMappedNodesG1() {
		return nonMappedNodesG1;
	}

	public Set<PDGNode> getNonMappedNodesG2() {
		return nonMappedNodesG2;
	}

	public Map<String, ArrayList<VariableDeclaration>> getCommonPassedParameters() {
		Map<String, ArrayList<VariableDeclaration>> commonPassedParameters = new LinkedHashMap<String, ArrayList<VariableDeclaration>>();
		Set<VariableDeclaration> variableDeclarationsAndAccessedFieldsInMethod1 = pdg1.getVariableDeclarationsAndAccessedFieldsInMethod();
		Set<VariableDeclaration> variableDeclarationsAndAccessedFieldsInMethod2 = pdg2.getVariableDeclarationsAndAccessedFieldsInMethod();
		for(String key : this.commonPassedParameters.keySet()) {
			ArrayList<AbstractVariable> value = this.commonPassedParameters.get(key);
			AbstractVariable variableDeclaration1 = value.get(0);
			AbstractVariable variableDeclaration2 = value.get(1);
			ArrayList<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
			for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFieldsInMethod1) {
				if(variableDeclaration.resolveBinding().getKey().equals(variableDeclaration1.getVariableBindingKey())) {
					variableDeclarations.add(variableDeclaration);
					break;
				}
			}
			for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFieldsInMethod2) {
				if(variableDeclaration.resolveBinding().getKey().equals(variableDeclaration2.getVariableBindingKey())) {
					variableDeclarations.add(variableDeclaration);
					break;
				}
			}
			commonPassedParameters.put(key, variableDeclarations);
		}
		return commonPassedParameters;
	}

	public Set<VariableDeclaration> getPassedParametersG1() {
		Set<VariableDeclaration> passedParametersG1 = new LinkedHashSet<VariableDeclaration>();
		Set<VariableDeclaration> variableDeclarationsAndAccessedFieldsInMethod1 = pdg1.getVariableDeclarationsAndAccessedFieldsInMethod();
		for(AbstractVariable variable1 : this.passedParametersG1) {
			for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFieldsInMethod1) {
				if(variableDeclaration.resolveBinding().getKey().equals(variable1.getVariableBindingKey())) {
					passedParametersG1.add(variableDeclaration);
					break;
				}
			}
		}
		return passedParametersG1;
	}

	public Set<VariableDeclaration> getPassedParametersG2() {
		Set<VariableDeclaration> passedParametersG2 = new LinkedHashSet<VariableDeclaration>();
		Set<VariableDeclaration> variableDeclarationsAndAccessedFieldsInMethod2 = pdg2.getVariableDeclarationsAndAccessedFieldsInMethod();
		for(AbstractVariable variable2 : this.passedParametersG2) {
			for(VariableDeclaration variableDeclaration : variableDeclarationsAndAccessedFieldsInMethod2) {
				if(variableDeclaration.resolveBinding().getKey().equals(variable2.getVariableBindingKey())) {
					passedParametersG2.add(variableDeclaration);
					break;
				}
			}
		}
		return passedParametersG2;
	}

	public Set<VariableDeclaration> getAccessedLocalFieldsG1() {
		Set<VariableDeclaration> accessedLocalFieldsG1 = new LinkedHashSet<VariableDeclaration>();
		Set<VariableDeclaration> fieldsAccessedInMethod1 = pdg1.getFieldsAccessedInMethod();
		for(AbstractVariable variable : this.accessedLocalFieldsG1) {
			for(VariableDeclaration fieldDeclaration : fieldsAccessedInMethod1) {
				if(variable.getVariableBindingKey().equals(fieldDeclaration.resolveBinding().getKey())) {
					accessedLocalFieldsG1.add(fieldDeclaration);
					break;
				}
			}
		}
		return accessedLocalFieldsG1;
	}

	public Set<VariableDeclaration> getAccessedLocalFieldsG2() {
		Set<VariableDeclaration> accessedLocalFieldsG2 = new LinkedHashSet<VariableDeclaration>();
		Set<VariableDeclaration> fieldsAccessedInMethod2 = pdg2.getFieldsAccessedInMethod();
		for(AbstractVariable variable : this.accessedLocalFieldsG2) {
			for(VariableDeclaration fieldDeclaration : fieldsAccessedInMethod2) {
				if(variable.getVariableBindingKey().equals(fieldDeclaration.resolveBinding().getKey())) {
					accessedLocalFieldsG2.add(fieldDeclaration);
					break;
				}
			}
		}
		return accessedLocalFieldsG2;
	}

	public Set<MethodInvocationObject> getAccessedLocalMethodsG1() {
		return accessedLocalMethodsG1;
	}

	public Set<MethodInvocationObject> getAccessedLocalMethodsG2() {
		return accessedLocalMethodsG2;
	}

	public Set<PDGNode> getRemovableNodesG1() {
		Set<PDGNode> removableNodes = mappedNodesG1;
		removableNodes.removeAll(nonMappedNodesSliceUnionG1);
		return removableNodes;
	}

	public Set<PDGNode> getRemovableNodesG2() {
		Set<PDGNode> removableNodes = mappedNodesG2;
		removableNodes.removeAll(nonMappedNodesSliceUnionG2);
		return removableNodes;
	}

	public TreeSet<PDGNode> getRemainingNodesG1() {
		return nonMappedNodesSliceUnionG1;
	}

	public TreeSet<PDGNode> getRemainingNodesG2() {
		return nonMappedNodesSliceUnionG2;
	}

	public List<ASTNodeDifference> getNodeDifferences() {
		return maximumStateWithMinimumDifferences.getNodeDifferences();
	}

	public Set<VariableDeclaration> getDeclaredVariablesInMappedNodesUsedByNonMappedNodesG1() {
		Set<VariableDeclaration> declaredVariablesG1 = new LinkedHashSet<VariableDeclaration>();
		Set<VariableDeclaration> variableDeclarationsInMethod1 = pdg1.getVariableDeclarationsInMethod();
		for(AbstractVariable variable1 : this.declaredVariablesInMappedNodesUsedByNonMappedNodesG1) {
			for(VariableDeclaration variableDeclaration : variableDeclarationsInMethod1) {
				if(variableDeclaration.resolveBinding().getKey().equals(variable1.getVariableBindingKey())) {
					declaredVariablesG1.add(variableDeclaration);
					break;
				}
			}
		}
		return declaredVariablesG1;
	}

	public Set<VariableDeclaration> getDeclaredVariablesInMappedNodesUsedByNonMappedNodesG2() {
		Set<VariableDeclaration> declaredVariablesG2 = new LinkedHashSet<VariableDeclaration>();
		Set<VariableDeclaration> variableDeclarationsInMethod2 = pdg2.getVariableDeclarationsInMethod();
		for(AbstractVariable variable2 : this.declaredVariablesInMappedNodesUsedByNonMappedNodesG2) {
			for(VariableDeclaration variableDeclaration : variableDeclarationsInMethod2) {
				if(variableDeclaration.resolveBinding().getKey().equals(variable2.getVariableBindingKey())) {
					declaredVariablesG2.add(variableDeclaration);
					break;
				}
			}
		}
		return declaredVariablesG2;
	}

	private MappingState findMaximumStateWithMinimumDifferences(List<MappingState> states) {
		int max = 0;
		List<MappingState> maximumStates = new ArrayList<MappingState>();
		for(MappingState currentState : states) {
			if(currentState.getSize() > max) {
				max = currentState.getSize();
				maximumStates.clear();
				maximumStates.add(currentState);
			}
			else if(currentState.getSize() == max) {
				maximumStates.add(currentState);
			}
		}
		
		List<MappingState> maximumStatesWithMinimumDifferences = new ArrayList<MappingState>();
		if(maximumStates.size() == 1) {
			maximumStatesWithMinimumDifferences.add(maximumStates.get(0));
		}
		else {
			int minimum = maximumStates.get(0).getDifferenceCount();
			maximumStatesWithMinimumDifferences.add(maximumStates.get(0));
			for(int i=1; i<maximumStates.size(); i++) {
				MappingState currentState = maximumStates.get(i);
				if(currentState.getDifferenceCount() < minimum) {
					minimum = currentState.getDifferenceCount();
					maximumStatesWithMinimumDifferences.clear();
					maximumStatesWithMinimumDifferences.add(currentState);
				}
				else if(currentState.getDifferenceCount() == minimum) {
					maximumStatesWithMinimumDifferences.add(currentState);
				}
			}
		}
		//in the case where we have more than one maximum states with minimum differences, return the state where the node mappings have closer Ids
		if(maximumStatesWithMinimumDifferences.size() == 1) {
			return maximumStatesWithMinimumDifferences.get(0);
		}
		else {
			int minimum = maximumStatesWithMinimumDifferences.get(0).getNodeMappingIdDiff();
			MappingState maximumStateWithMinimumDifferences = maximumStatesWithMinimumDifferences.get(0);
			for(int i=1; i<maximumStatesWithMinimumDifferences.size(); i++) {
				MappingState currentState = maximumStatesWithMinimumDifferences.get(i);
				if(currentState.getNodeMappingIdDiff() < minimum) {
					minimum = currentState.getNodeMappingIdDiff();
					maximumStateWithMinimumDifferences = currentState;
				}
			}
			return maximumStateWithMinimumDifferences;
		}
	}

	private Set<PDGNode> getNodesInRegion(PDGNode controlPredicate, Set<PDGNode> controlPredicateNodesInNextLevel) {
		Set<PDGNode> nodesInRegion = new TreeSet<PDGNode>();
		if(!(controlPredicate instanceof PDGMethodEntryNode))
			nodesInRegion.add(controlPredicate);
		Iterator<GraphEdge> edgeIterator = controlPredicate.getOutgoingDependenceIterator();
		while(edgeIterator.hasNext()) {
			PDGDependence dependence = (PDGDependence)edgeIterator.next();
			if(dependence instanceof PDGControlDependence) {
				PDGNode pdgNode = (PDGNode)dependence.getDst();
				if(!controlPredicateNodesInNextLevel.contains(pdgNode))
					nodesInRegion.add(pdgNode);
			}
		}
		return nodesInRegion;
	}

	private void processPDGNodes() {
		int maxLevel1 = controlDependenceTreePDG1.getMaxLevel();
		int level1 = maxLevel1;
		int maxLevel2 = controlDependenceTreePDG2.getMaxLevel();
		int level2 = maxLevel2;
		if(monitor != null)
			monitor.beginTask("Mapping Program Dependence Graphs", Math.min(maxLevel1, maxLevel2));
		MappingState finalState = null;
		while(level1 >= 0 && level2 >= 0) {
			Set<PDGNode> controlPredicateNodesG1 = controlDependenceTreePDG1.getControlPredicateNodesInLevel(level1);
			Set<PDGNode> controlPredicateNodesG2 = controlDependenceTreePDG2.getControlPredicateNodesInLevel(level2);
			Set<PDGNode> controlPredicateNodesInNextLevelG1 = new LinkedHashSet<PDGNode>();
			Set<PDGNode> controlPredicateNodesInNextLevelG2 = new LinkedHashSet<PDGNode>();
			if(level1 < maxLevel1) {
				controlPredicateNodesInNextLevelG1.addAll(controlDependenceTreePDG1.getControlPredicateNodesInLevel(level1+1));
			}
			if(level2 < maxLevel2) {
				controlPredicateNodesInNextLevelG2.addAll(controlDependenceTreePDG2.getControlPredicateNodesInLevel(level2+1));
			}
			for(PDGNode predicate1 : controlPredicateNodesG1) {
				Set<PDGNode> nodesG1 = getNodesInRegion(predicate1, controlPredicateNodesInNextLevelG1);
				MappingState.setRestrictedNodesG1(nodesG1);
				List<MappingState> currentStates = new ArrayList<MappingState>();
				for(PDGNode predicate2 : controlPredicateNodesG2) {
					Set<PDGNode> nodesG2 = getNodesInRegion(predicate2, controlPredicateNodesInNextLevelG2);
					MappingState.setRestrictedNodesG2(nodesG2);
					List<MappingState> maxStates = processPDGNodes(finalState, nodesG1, nodesG2);
					for(MappingState temp : maxStates) {
						if(!currentStates.contains(temp)) {
							currentStates.add(temp);
						}
					}
				}
				if(!currentStates.isEmpty()) {
					MappingState best = findMaximumStateWithMinimumDifferences(currentStates);
					List<PDGNodeMapping> nodeMappings = new ArrayList<PDGNodeMapping>(best.getNodeMappings());
					for(PDGNodeMapping mapping : nodeMappings) {
						if(mapping.getNodeG1().equals(predicate1)) {
							controlPredicateNodesG2.remove(mapping.getNodeG2());
							break;
						}
					}
					finalState = best;
				}
			}
			level1--;
			level2--;
			if(monitor != null)
				monitor.worked(1);
		}
		if(monitor != null)
			monitor.done();
		maximumStateWithMinimumDifferences = finalState;
	}
	
	private List<MappingState> processPDGNodes(MappingState parent, Set<PDGNode> nodesG1, Set<PDGNode> nodesG2) {
		List<MappingState> finalStates = new ArrayList<MappingState>();
		for(PDGNode node1 : nodesG1) {
			List<MappingState> currentStates = new ArrayList<MappingState>();
			for(PDGNode node2 : nodesG2) {
				ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
				boolean match = node1.getASTStatement().subtreeMatch(astNodeMatcher, node2.getASTStatement());
				if(match && astNodeMatcher.isParameterizable()) {
					PDGNodeMapping mapping = new PDGNodeMapping(node1, node2, astNodeMatcher);
					boolean symmetricalIfNodes = symmetricalIfNodes(node1, node2);
					if(symmetricalIfNodes)
						mapping.setSymmetricalIfNodePair(true);
					if(finalStates.isEmpty()) {
						MappingState state = new MappingState(parent, mapping);
						state.traverse(mapping);
						List<MappingState> maxStates = state.getMaximumCommonSubGraphs();
						for(MappingState temp : maxStates) {
							if(!currentStates.contains(temp)) {
								currentStates.add(temp);
							}
						}
					}
					else {
						for(MappingState previousState : finalStates) {
							if(!previousState.containsAtLeastOneNodeInMappings(mapping) && previousState.mappedControlParents(node1, node2)) {
								MappingState state = new MappingState(previousState, mapping);
								previousState.addChild(state);
								state.traverse(mapping);
								List<MappingState> maxStates = state.getMaximumCommonSubGraphs();
								for(MappingState temp : maxStates) {
									if(!currentStates.contains(temp)) {
										currentStates.add(temp);
									}
								}
							}
						}
					}
				}
			}
			if(!currentStates.isEmpty())
				finalStates = getMaximumStates(currentStates);
		}
		return finalStates;
	}

	private boolean symmetricalIfNodes(PDGNode nodeG1, PDGNode nodeG2) {
		PDGNode dstNodeG1 = falseControlDependentNode(nodeG1);
		PDGNode dstNodeG2 = falseControlDependentNode(nodeG2);
		if(dstNodeG1 != null && dstNodeG2 != null) {
			PDGNode nodeG1ControlParent = nodeG1.getControlDependenceParent();
			PDGNode nodeG2ControlParent = nodeG2.getControlDependenceParent();
			PDGNode dstNodeG1ControlParent = dstNodeG1.getControlDependenceParent();
			PDGNode dstNodeG2ControlParent = dstNodeG2.getControlDependenceParent();
			if((dstNodeG1ControlParent != null && dstNodeG1ControlParent.equals(nodeG1) && nodeG2ControlParent != null && nodeG2ControlParent.equals(dstNodeG2)) ||
					(dstNodeG2ControlParent != null && dstNodeG2ControlParent.equals(nodeG2) && nodeG1ControlParent != null && nodeG1ControlParent.equals(dstNodeG1))) {
				return true;
			}
		}
		return false;
	}

	private PDGNode falseControlDependentNode(PDGNode node) {
		PDGNode dstNode = null;
		int count = 0;
		for(Iterator<GraphEdge> outgoingDependenceIterator = node.getOutgoingDependenceIterator(); outgoingDependenceIterator.hasNext();) {
			PDGDependence dependence = (PDGDependence)outgoingDependenceIterator.next();
			if(dependence instanceof PDGControlDependence) {
				PDGControlDependence controlDependence = (PDGControlDependence)dependence;
				if(controlDependence.isFalseControlDependence()) {
					dstNode = (PDGNode)controlDependence.getDst();
					count++;
				}
			}
		}
		if(count == 1 && dstNode.getCFGNode() instanceof CFGBranchIfNode)
			return dstNode;
		
		dstNode = null;
		count = 0;
		for(Iterator<GraphEdge> incomingDependenceIterator = node.getIncomingDependenceIterator(); incomingDependenceIterator.hasNext();) {
			PDGDependence dependence = (PDGDependence)incomingDependenceIterator.next();
			if(dependence instanceof PDGControlDependence) {
				PDGControlDependence controlDependence = (PDGControlDependence)dependence;
				if(controlDependence.isFalseControlDependence()) {
					dstNode = (PDGNode)controlDependence.getSrc();
					count++;
				}
			}
		}
		if(count == 1 && dstNode.getCFGNode() instanceof CFGBranchIfNode)
			return dstNode;
		return null;
	}

	private List<MappingState> getMaximumStates(List<MappingState> currentStates) {
		int max = 0;
		List<MappingState> maximumStates = new ArrayList<MappingState>();
		for(MappingState currentState : currentStates) {
			if(currentState.getSize() > max) {
				max = currentState.getSize();
				maximumStates.clear();
				maximumStates.add(currentState);
			}
			else if(currentState.getSize() == max) {
				maximumStates.add(currentState);
			}
		}
		List<MappingState> maximumStatesWithMinimumDifferences = new ArrayList<MappingState>();
		if(maximumStates.size() == 1) {
			maximumStatesWithMinimumDifferences.add(maximumStates.get(0));
		}
		else {
			int minimum = maximumStates.get(0).getDifferenceCount();
			maximumStatesWithMinimumDifferences.add(maximumStates.get(0));
			for(int i=1; i<maximumStates.size(); i++) {
				MappingState currentState = maximumStates.get(i);
				if(currentState.getDifferenceCount() < minimum) {
					minimum = currentState.getDifferenceCount();
					maximumStatesWithMinimumDifferences.clear();
					maximumStatesWithMinimumDifferences.add(currentState);
				}
				else if(currentState.getDifferenceCount() == minimum) {
					maximumStatesWithMinimumDifferences.add(currentState);
				}
			}
		}
		return maximumStatesWithMinimumDifferences;
	}
}
