package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.decomposition.ASTNodeDifference;
import gr.uom.java.ast.decomposition.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.BindingSignaturePair;
import gr.uom.java.ast.decomposition.CompositeStatementObject;
import gr.uom.java.ast.decomposition.DifferenceType;
import gr.uom.java.ast.decomposition.DualExpressionPreconditionViolation;
import gr.uom.java.ast.decomposition.ExpressionPreconditionViolation;
import gr.uom.java.ast.decomposition.PreconditionViolation;
import gr.uom.java.ast.decomposition.PreconditionViolationType;
import gr.uom.java.ast.decomposition.ReturnedVariablePreconditionViolation;
import gr.uom.java.ast.decomposition.StatementObject;
import gr.uom.java.ast.decomposition.StatementPreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CFGBranchIfNode;
import gr.uom.java.ast.decomposition.cfg.CFGBreakNode;
import gr.uom.java.ast.decomposition.cfg.CFGContinueNode;
import gr.uom.java.ast.decomposition.cfg.CFGExitNode;
import gr.uom.java.ast.decomposition.cfg.CFGNode;
import gr.uom.java.ast.decomposition.cfg.GraphEdge;
import gr.uom.java.ast.decomposition.cfg.GraphNode;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.PDGAbstractDataDependence;
import gr.uom.java.ast.decomposition.cfg.PDGAntiDependence;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGControlPredicateNode;
import gr.uom.java.ast.decomposition.cfg.PDGDataDependence;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGExpression;
import gr.uom.java.ast.decomposition.cfg.PDGMethodEntryNode;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PDGOutputDependence;
import gr.uom.java.ast.decomposition.cfg.PDGTryNode;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class PDGSubTreeMapper {
	private PDG pdg1;
	private PDG pdg2;
	private ICompilationUnit iCompilationUnit1;
	private ICompilationUnit iCompilationUnit2;
	private MappingState maximumStateWithMinimumDifferences;
	private CloneStructureNode cloneStructureRoot;
	private TreeSet<PDGNode> mappedNodesG1;
	private TreeSet<PDGNode> mappedNodesG2;
	private TreeSet<PDGNode> nonMappedNodesG1;
	private TreeSet<PDGNode> nonMappedNodesG2;
	private Map<String, ArrayList<AbstractVariable>> commonPassedParameters;
	private Map<String, ArrayList<AbstractVariable>> declaredLocalVariablesInMappedNodes;
	private Set<AbstractVariable> passedParametersG1;
	private Set<AbstractVariable> passedParametersG2;
	private Set<AbstractVariable> accessedLocalFieldsG1;
	private Set<AbstractVariable> accessedLocalFieldsG2;
	private Set<MethodInvocationObject> accessedLocalMethodsG1;
	private Set<MethodInvocationObject> accessedLocalMethodsG2;
	private Set<AbstractVariable> declaredVariablesInMappedNodesUsedByNonMappedNodesG1;
	private Set<AbstractVariable> declaredVariablesInMappedNodesUsedByNonMappedNodesG2;
	private TreeSet<PDGNode> allNodesInSubTreePDG1;
	private TreeSet<PDGNode> allNodesInSubTreePDG2;
	//if true full tree match is performed, otherwise subtree match is performed
	private boolean fullTreeMatch;
	private IProgressMonitor monitor;
	private List<PreconditionViolation> preconditionViolations;
	
	public PDGSubTreeMapper(PDG pdg1, PDG pdg2,
			ICompilationUnit iCompilationUnit1, ICompilationUnit iCompilationUnit2,
			ControlDependenceTreeNode controlDependenceSubTreePDG1,
			ControlDependenceTreeNode controlDependenceSubTreePDG2,
			boolean fullTreeMatch, IProgressMonitor monitor) {
		this.pdg1 = pdg1;
		this.pdg2 = pdg2;
		this.iCompilationUnit1 = iCompilationUnit1;
		this.iCompilationUnit2 = iCompilationUnit2;
		this.fullTreeMatch = fullTreeMatch;
		this.nonMappedNodesG1 = new TreeSet<PDGNode>();
		this.nonMappedNodesG2 = new TreeSet<PDGNode>();
		this.commonPassedParameters = new LinkedHashMap<String, ArrayList<AbstractVariable>>();
		this.declaredLocalVariablesInMappedNodes = new LinkedHashMap<String, ArrayList<AbstractVariable>>();
		this.passedParametersG1 = new LinkedHashSet<AbstractVariable>();
		this.passedParametersG2 = new LinkedHashSet<AbstractVariable>();
		this.accessedLocalFieldsG1 = new LinkedHashSet<AbstractVariable>();
		this.accessedLocalFieldsG2 = new LinkedHashSet<AbstractVariable>();
		this.accessedLocalMethodsG1 = new LinkedHashSet<MethodInvocationObject>();
		this.accessedLocalMethodsG2 = new LinkedHashSet<MethodInvocationObject>();
		this.declaredVariablesInMappedNodesUsedByNonMappedNodesG1 = new LinkedHashSet<AbstractVariable>();
		this.declaredVariablesInMappedNodesUsedByNonMappedNodesG2 = new LinkedHashSet<AbstractVariable>();
		this.monitor = monitor;
		this.allNodesInSubTreePDG1 = new TreeSet<PDGNode>();
		this.allNodesInSubTreePDG2 = new TreeSet<PDGNode>();
		//creates CloneStructureRoot
		matchBasedOnControlDependenceTreeStructure(controlDependenceSubTreePDG1, controlDependenceSubTreePDG2);
		this.mappedNodesG1 = maximumStateWithMinimumDifferences.getMappedNodesG1();
		this.mappedNodesG2 = maximumStateWithMinimumDifferences.getMappedNodesG2();
		findNonMappedNodes(pdg1, allNodesInSubTreePDG1, mappedNodesG1, nonMappedNodesG1);
		findNonMappedNodes(pdg2, allNodesInSubTreePDG2, mappedNodesG2, nonMappedNodesG2);
		for(PDGNode nodeG1 : nonMappedNodesG1) {
			PDGNodeGap nodeGap = new PDGNodeGap(nodeG1, null);
			CloneStructureNode node = new CloneStructureNode(nodeGap);
			PDGTryNode tryNode = pdg1.isDirectlyNestedWithinTryNode(nodeG1);
			if(tryNode != null) {
				CloneStructureNode cloneStructureTry = cloneStructureRoot.findNodeG1(tryNode);
				if(cloneStructureTry != null) {
					node.setParent(cloneStructureTry);
				}
			}
			else {
				cloneStructureRoot.addGapChild(node);
			}
		}
		for(PDGNode nodeG2 : nonMappedNodesG2) {
			PDGNodeGap nodeGap = new PDGNodeGap(null, nodeG2);
			CloneStructureNode node = new CloneStructureNode(nodeGap);
			PDGTryNode tryNode = pdg2.isDirectlyNestedWithinTryNode(nodeG2);
			if(tryNode != null) {
				CloneStructureNode cloneStructureTry = cloneStructureRoot.findNodeG2(tryNode);
				if(cloneStructureTry != null) {
					node.setParent(cloneStructureTry);
				}
			}
			else {
				cloneStructureRoot.addGapChild(node);
			}
		}
		findDeclaredVariablesInMappedNodesUsedByNonMappedNodes(mappedNodesG1, nonMappedNodesG1, declaredVariablesInMappedNodesUsedByNonMappedNodesG1);
		findDeclaredVariablesInMappedNodesUsedByNonMappedNodes(mappedNodesG2, nonMappedNodesG2, declaredVariablesInMappedNodesUsedByNonMappedNodesG2);
		findPassedParameters();
		findLocallyAccessedFields(pdg1, mappedNodesG1, accessedLocalFieldsG1, accessedLocalMethodsG1);
		findLocallyAccessedFields(pdg2, mappedNodesG2, accessedLocalFieldsG2, accessedLocalMethodsG2);
		this.preconditionViolations = new ArrayList<PreconditionViolation>();
		checkCloneStructureNodeForPreconditions(cloneStructureRoot);
		determineVariablesToBeReturned();
	}

	private void findNonMappedNodes(PDG pdg, TreeSet<PDGNode> allNodes, Set<PDGNode> mappedNodes, Set<PDGNode> nonMappedNodes) {
		PDGNode first = allNodes.first();
		PDGNode last = allNodes.last();
		Iterator<GraphNode> iterator = pdg.getNodeIterator();
		while(iterator.hasNext()) {
			PDGNode pdgNode = (PDGNode)iterator.next();
			if(pdgNode.getId() >= first.getId() && pdgNode.getId() <= last.getId()) {
				if(!mappedNodes.contains(pdgNode)) {
					nonMappedNodes.add(pdgNode);
				}
			}
		}
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

	private void findPassedParameters() {
		Set<AbstractVariable> passedParametersG1 = extractPassedParameters(pdg1, mappedNodesG1);
		Set<AbstractVariable> passedParametersG2 = extractPassedParameters(pdg2, mappedNodesG2);
		for(PDGNodeMapping nodeMapping : maximumStateWithMinimumDifferences.getNodeMappings()) {
			PDGNode nodeG1 = nodeMapping.getNodeG1();
			PDGNode nodeG2 = nodeMapping.getNodeG2();
			Iterator<AbstractVariable> declaredVariableIteratorG1 = nodeG1.getDeclaredVariableIterator();
			Iterator<AbstractVariable> declaredVariableIteratorG2 = nodeG2.getDeclaredVariableIterator();
			while(declaredVariableIteratorG1.hasNext()) {
				AbstractVariable declaredVariableG1 = declaredVariableIteratorG1.next();
				AbstractVariable declaredVariableG2 = declaredVariableIteratorG2.next();
				ArrayList<AbstractVariable> declaredVariables = new ArrayList<AbstractVariable>();
				declaredVariables.add(declaredVariableG1);
				declaredVariables.add(declaredVariableG2);
				declaredLocalVariablesInMappedNodes.put(declaredVariableG1.getVariableBindingKey(), declaredVariables);
			}
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
			if(dataDependences1.size() == dataDependences2.size()) {
				List<AbstractVariable> variables1 = new ArrayList<AbstractVariable>(dataDependences1);
				List<AbstractVariable> variables2 = new ArrayList<AbstractVariable>(dataDependences2);
				for(int i=0; i<variables1.size(); i++) {
					AbstractVariable variable1 = variables1.get(i);
					AbstractVariable variable2 = variables2.get(i);
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
		}
		this.passedParametersG1.addAll(passedParametersG1);
		this.passedParametersG2.addAll(passedParametersG2);
	}

	private Set<AbstractVariable> extractPassedParameters(PDG pdg, Set<PDGNode> mappedNodes) {
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

	private void findLocallyAccessedFields(PDG pdg, Set<PDGNode> mappedNodes, Set<AbstractVariable> accessedFields,
			Set<MethodInvocationObject> accessedMethods) {
		Set<PlainVariable> usedLocalFields = new LinkedHashSet<PlainVariable>();
		Set<MethodInvocationObject> accessedLocalMethods = new LinkedHashSet<MethodInvocationObject>();
		for(PDGNode pdgNode : mappedNodes) {
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
			int minimum = maximumStates.get(0).getDistinctDifferenceCount();
			maximumStatesWithMinimumDifferences.add(maximumStates.get(0));
			for(int i=1; i<maximumStates.size(); i++) {
				MappingState currentState = maximumStates.get(i);
				if(currentState.getDistinctDifferenceCount() < minimum) {
					minimum = currentState.getDistinctDifferenceCount();
					maximumStatesWithMinimumDifferences.clear();
					maximumStatesWithMinimumDifferences.add(currentState);
				}
				else if(currentState.getDistinctDifferenceCount() == minimum) {
					maximumStatesWithMinimumDifferences.add(currentState);
				}
			}
		}
		//TODO: Introduce comparison of difference "weights" in the case of multiple maximum states with minimum differences
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

	private Set<PDGNode> getNodesInRegion(PDG pdg, PDGNode controlPredicate, Set<PDGNode> controlPredicateNodesInCurrentLevel,
			Set<PDGNode> controlPredicateNodesInNextLevel, ControlDependenceTreeNode controlDependenceTreeRoot) {
		Set<PDGNode> nodesInRegion = new TreeSet<PDGNode>();
		if(!(controlPredicate instanceof PDGMethodEntryNode) &&
				!controlPredicate.equals(controlDependenceTreeRoot.getNode()))
			nodesInRegion.add(controlPredicate);
		if(controlPredicate instanceof PDGTryNode) {
			Set<PDGNode> nestedNodesWithinTryNode = pdg.getNestedNodesWithinTryNode((PDGTryNode)controlPredicate);
			for(PDGNode nestedNode : nestedNodesWithinTryNode) {
				if(!controlPredicateNodesInNextLevel.contains(nestedNode) && !controlPredicateNodesInCurrentLevel.contains(nestedNode)) {
					if(!(nestedNode instanceof PDGControlPredicateNode))
						nodesInRegion.add(nestedNode);
				}
			}
		}
		else {
			Iterator<GraphEdge> edgeIterator = controlPredicate.getOutgoingDependenceIterator();
			while(edgeIterator.hasNext()) {
				PDGDependence dependence = (PDGDependence)edgeIterator.next();
				if(dependence instanceof PDGControlDependence) {
					PDGNode pdgNode = (PDGNode)dependence.getDst();
					PDGTryNode tryNode = pdg.isDirectlyNestedWithinTryNode(pdgNode);
					if(!controlPredicateNodesInNextLevel.contains(pdgNode) && !controlPredicateNodesInCurrentLevel.contains(pdgNode) && tryNode == null) {
						if(!(pdgNode instanceof PDGControlPredicateNode))
							nodesInRegion.add(pdgNode);
					}
				}
			}
		}
		return nodesInRegion;
	}

	private void matchBasedOnControlDependenceTreeStructure(ControlDependenceTreeNode controlDependenceTreePDG1, ControlDependenceTreeNode controlDependenceTreePDG2) {
		int maxLevel1 = controlDependenceTreePDG1.getMaxLevel();
		int level1 = maxLevel1;
		int maxLevel2 = controlDependenceTreePDG2.getMaxLevel();
		int level2 = maxLevel2;
		if(monitor != null)
			monitor.beginTask("Mapping Program Dependence Graphs", Math.min(maxLevel1, maxLevel2));
		CloneStructureNode root = null;
		MappingState finalState = null;
		List<CloneStructureNode> parents = new ArrayList<CloneStructureNode>();
		while(level1 >= 0 && level2 >= 0) {
			Set<PDGNode> controlPredicateNodesG1 = controlDependenceTreePDG1.getControlPredicateNodesInLevel(level1);
			Set<PDGNode> controlPredicateNodesG2 = controlDependenceTreePDG2.getControlPredicateNodesInLevel(level2);
			Set<PDGNode> controlPredicateNodesInNextLevelG1 = new LinkedHashSet<PDGNode>();
			Set<PDGNode> controlPredicateNodesInNextLevelG2 = new LinkedHashSet<PDGNode>();
			if(level1 < maxLevel1) {
				Set<PDGNode> nodesInNextLevel = controlDependenceTreePDG1.getControlPredicateNodesInLevel(level1+1);
				controlPredicateNodesInNextLevelG1.addAll(nodesInNextLevel);
				for(PDGNode node : nodesInNextLevel) {
					if(node instanceof PDGTryNode) {
						controlPredicateNodesInNextLevelG1.addAll(pdg1.getNestedNodesWithinTryNode((PDGTryNode)node));
					}
				}
			}
			if(level2 < maxLevel2) {
				Set<PDGNode> nodesInNextLevel = controlDependenceTreePDG2.getControlPredicateNodesInLevel(level2+1);
				controlPredicateNodesInNextLevelG2.addAll(nodesInNextLevel);
				for(PDGNode node : nodesInNextLevel) {
					if(node instanceof PDGTryNode) {
						controlPredicateNodesInNextLevelG2.addAll(pdg2.getNestedNodesWithinTryNode((PDGTryNode)node));
					}
				}
			}
			for(PDGNode predicate1 : controlPredicateNodesG1) {
				Set<PDGNode> nodesG1 = getNodesInRegion(pdg1, predicate1, controlPredicateNodesG1, controlPredicateNodesInNextLevelG1, controlDependenceTreePDG1);
				//special handling in level 0 for sub tree match
				if(level1 == 0 && !fullTreeMatch) {
					int maxId = allNodesInSubTreePDG1.last().getId();
					Set<PDGNode> nodesG1ToBeRemoved = new LinkedHashSet<PDGNode>();
					for(PDGNode nodeG1 : nodesG1) {
						if(nodeG1.getId() > maxId) {
							nodesG1ToBeRemoved.add(nodeG1);
						}
						if(controlDependenceTreePDG1.isElseNode()) {
							double elseNodeId = controlDependenceTreePDG1.getId();
							if(nodeG1.getId() < elseNodeId) {
								nodesG1ToBeRemoved.add(nodeG1);
							}
						}
					}
					nodesG1.removeAll(nodesG1ToBeRemoved);
				}
				this.allNodesInSubTreePDG1.addAll(nodesG1);
				List<MappingState> currentStates = new ArrayList<MappingState>();
				for(PDGNode predicate2 : controlPredicateNodesG2) {
					Set<PDGNode> nodesG2 = getNodesInRegion(pdg2, predicate2, controlPredicateNodesG2, controlPredicateNodesInNextLevelG2, controlDependenceTreePDG2);
					if(level2 == 0 && !fullTreeMatch) {
						int maxId = allNodesInSubTreePDG2.last().getId();
						Set<PDGNode> nodesG2ToBeRemoved = new LinkedHashSet<PDGNode>();
						for(PDGNode nodeG2 : nodesG2) {
							if(nodeG2.getId() > maxId) {
								nodesG2ToBeRemoved.add(nodeG2);
							}
							if(controlDependenceTreePDG2.isElseNode()) {
								double elseNodeId = controlDependenceTreePDG2.getId();
								if(nodeG2.getId() < elseNodeId) {
									nodesG2ToBeRemoved.add(nodeG2);
								}
							}
						}
						nodesG2.removeAll(nodesG2ToBeRemoved);
					}
					this.allNodesInSubTreePDG2.addAll(nodesG2);
					List<MappingState> maxStates = null;
					if(level1 == 0 || level2 == 0) {
						maxStates = matchBasedOnCodeFragments(finalState, nodesG1, nodesG2);
					}
					else {
						maxStates = processPDGNodes(finalState, nodesG1, nodesG2);
					}
					for(MappingState temp : maxStates) {
						if(!currentStates.contains(temp)) {
							currentStates.add(temp);
						}
					}
				}
				if(!currentStates.isEmpty()) {
					MappingState best = findMaximumStateWithMinimumDifferences(currentStates);
					List<PDGNodeMapping> nodeMappings = new ArrayList<PDGNodeMapping>(best.getNodeMappings());
					int index = 0;
					for(PDGNodeMapping mapping : nodeMappings) {
						if(mapping.getNodeG1().equals(predicate1)) {
							controlPredicateNodesG2.remove(mapping.getNodeG2());
							break;
						}
						index++;
					}
					finalState = best;
					CloneStructureNode parent = null;
					CloneStructureNode newElseParent = null;
					for(int i=index; i<nodeMappings.size(); i++) {
						PDGNodeMapping mapping = nodeMappings.get(i);
						if(parent == null) {
							parent = new CloneStructureNode(mapping);
						}
						else {
							PDGTryNode nestedUnderTry1 = pdg1.isDirectlyNestedWithinTryNode(mapping.getNodeG1());
							PDGTryNode nestedUnderTry2 = pdg2.isDirectlyNestedWithinTryNode(mapping.getNodeG2());
							boolean nestedUnderTry = nestedUnderTry1 != null && nestedUnderTry2 != null;
							if(mapping.isFalseControlDependent() && !nestedUnderTry) {
								if(newElseParent == null) {
									ControlDependenceTreeNode elseNodeG1 = controlDependenceTreePDG1.getElseNode(parent.getMapping().getNodeG1());
									ControlDependenceTreeNode elseNodeG2 = controlDependenceTreePDG2.getElseNode(parent.getMapping().getNodeG2());
									if(elseNodeG1 != null && elseNodeG2 != null) {
										PDGElseMapping elseMapping = new PDGElseMapping(elseNodeG1.getId(), elseNodeG2.getId());
										newElseParent = new CloneStructureNode(elseMapping);
										parent.addChild(newElseParent);
										CloneStructureNode child = new CloneStructureNode(mapping);
										newElseParent.addChild(child);
									}
								}
								else {
									CloneStructureNode child = new CloneStructureNode(mapping);
									newElseParent.addChild(child);
								}
							}
							else {
								CloneStructureNode child = new CloneStructureNode(mapping);
								parent.addChild(child);
							}
						}
					}
					if(parent != null) {
						//add previous parents under the new parent
						PDGNodeMapping parentNodeMapping = (PDGNodeMapping) parent.getMapping();
						double parentId1 = parentNodeMapping.getId1();
						double parentId2 = parentNodeMapping.getId2();
						for(ListIterator<CloneStructureNode> parentIterator = parents.listIterator(); parentIterator.hasNext();) {
							CloneStructureNode previousParent = parentIterator.next();
							PDGNodeMapping previousParentNodeMapping = (PDGNodeMapping) previousParent.getMapping();
							double previousParentId1 = previousParentNodeMapping.getId1();
							double previousParentId2 = previousParentNodeMapping.getId2();
							if(controlDependenceTreePDG1.parentChildRelationship(parentId1, previousParentId1) &&
									controlDependenceTreePDG2.parentChildRelationship(parentId2, previousParentId2)) {
								parent.addChild(previousParent);
								parentIterator.remove();
							}
							else if(previousParentNodeMapping.isFalseControlDependent()) {
								if(newElseParent == null) {
									ControlDependenceTreeNode elseNodeG1 = controlDependenceTreePDG1.getElseNode(parentNodeMapping.getNodeG1());
									ControlDependenceTreeNode elseNodeG2 = controlDependenceTreePDG2.getElseNode(parentNodeMapping.getNodeG2());
									if(elseNodeG1 != null && elseNodeG2 != null) {
										if(controlDependenceTreePDG1.parentChildRelationship(elseNodeG1.getId(), previousParentId1) &&
												controlDependenceTreePDG2.parentChildRelationship(elseNodeG2.getId(), previousParentId2)) {
											PDGElseMapping elseMapping = new PDGElseMapping(elseNodeG1.getId(), elseNodeG2.getId());
											newElseParent = new CloneStructureNode(elseMapping);
											parent.addChild(newElseParent);
											newElseParent.addChild(previousParent);
											parentIterator.remove();
										}
									}
								}
								else {
									PDGElseMapping elseMapping = (PDGElseMapping) newElseParent.getMapping();
									if(controlDependenceTreePDG1.parentChildRelationship(elseMapping.getId1(), previousParentId1) &&
											controlDependenceTreePDG2.parentChildRelationship(elseMapping.getId2(), previousParentId2)) {
										newElseParent.addChild(previousParent);
										parentIterator.remove();
									}
								}
							}
						}
						//parents.add(parent);
						boolean isTryBlock = (parentNodeMapping.getNodeG1() instanceof PDGTryNode) && (parentNodeMapping.getNodeG2() instanceof PDGTryNode);
						if(!parent.getChildren().isEmpty() || isTryBlock) {
							parents.add(parent);
						}
						else {
							//remove parent mapping from the current state, if no children have been mapped
							finalState.getNodeMappings().remove(parent.getMapping());
						}
					}
					else {
						//create the root node of the clone structure
						root = new CloneStructureNode(null);
						for(ListIterator<CloneStructureNode> parentIterator = parents.listIterator(); parentIterator.hasNext();) {
							CloneStructureNode previousParent = parentIterator.next();
							root.addChild(previousParent);
							parentIterator.remove();
						}
						for(PDGNodeMapping nodeMapping : best.getNodeMappings()) {
							if(nodesG1.contains(nodeMapping.getNodeG1())) {
								CloneStructureNode childNode = new CloneStructureNode(nodeMapping);
								root.addChild(childNode);
							}
						}
					}
				}
			}
			level1--;
			level2--;
			if(monitor != null)
				monitor.worked(1);
		}
		if(monitor != null)
			monitor.done();
		if(root == null) {
			//create the root node of the clone structure
			root = new CloneStructureNode(null);
			for(ListIterator<CloneStructureNode> parentIterator = parents.listIterator(); parentIterator.hasNext();) {
				CloneStructureNode previousParent = parentIterator.next();
				root.addChild(previousParent);
				parentIterator.remove();
			}
		}
		this.maximumStateWithMinimumDifferences = finalState;
		this.cloneStructureRoot = root;
	}

	private List<MappingState> matchBasedOnCodeFragments(MappingState parent, Set<PDGNode> nodesG1, Set<PDGNode> nodesG2) {
		CodeFragmentDecomposer cfd1 = new CodeFragmentDecomposer(nodesG1);
		CodeFragmentDecomposer cfd2 = new CodeFragmentDecomposer(nodesG2);
		Map<PlainVariable, Set<PDGNode>> map1 = cfd1.getObjectNodeMap();
		Map<PlainVariable, Set<PDGNode>> map2 = cfd2.getObjectNodeMap();
		if(map1.isEmpty() || map2.isEmpty()) {
			return processPDGNodes(parent, nodesG1, nodesG2);
		}
		else {
			MappingState finalState = parent;
			for(PlainVariable variable1 : map1.keySet()) {
				Set<PDGNode> variableNodesG1 = map1.get(variable1);
				Set<PDGNode> tempNodesG1 = new LinkedHashSet<PDGNode>(variableNodesG1);
				Map<PlainVariable, List<MappingState>> currentStateMap = new LinkedHashMap<PlainVariable, List<MappingState>>();
				for(PlainVariable variable2 : map2.keySet()) {
					Set<PDGNode> variableNodesG2 = map2.get(variable2);
					Set<PDGNode> tempNodesG2 = new LinkedHashSet<PDGNode>(variableNodesG2);
					if(finalState != null) {
						for(PDGNodeMapping mapping : finalState.getNodeMappings()) {
							if(tempNodesG1.contains(mapping.getNodeG1()))
								tempNodesG1.remove(mapping.getNodeG1());
							if(tempNodesG2.contains(mapping.getNodeG2()))
								tempNodesG2.remove(mapping.getNodeG2());
						}
					}
					List<MappingState> maxStates = processPDGNodes(finalState, tempNodesG1, tempNodesG2);
					currentStateMap.put(variable2, maxStates);
				}
				List<MappingState> currentStates = new ArrayList<MappingState>();
				for(PlainVariable variable2 : currentStateMap.keySet()) {
					currentStates.addAll(currentStateMap.get(variable2));
				}
				if(!currentStates.isEmpty()) {
					MappingState best = findMaximumStateWithMinimumDifferences(currentStates);
					PlainVariable variableToRemove = null;
					for(PlainVariable variable2 : currentStateMap.keySet()) {
						if(currentStateMap.get(variable2).contains(best)) {
							variableToRemove = variable2;
							break;
						}
					}
					map2.remove(variableToRemove);
					finalState = best;
				}
			}
			List<MappingState> currentStates = new ArrayList<MappingState>();
			Set<PDGNode> tempNodesG1 = new LinkedHashSet<PDGNode>(nodesG1);
			Set<PDGNode> tempNodesG2 = new LinkedHashSet<PDGNode>(nodesG2);
			for(PDGNodeMapping mapping : finalState.getNodeMappings()) {
				if(tempNodesG1.contains(mapping.getNodeG1()))
					tempNodesG1.remove(mapping.getNodeG1());
				if(tempNodesG2.contains(mapping.getNodeG2()))
					tempNodesG2.remove(mapping.getNodeG2());
			}
			if(tempNodesG1.isEmpty() || tempNodesG2.isEmpty()) {
				currentStates.add(finalState);
			}
			else {
				List<MappingState> maxStates = processPDGNodes(finalState, tempNodesG1, tempNodesG2);
				for(MappingState temp : maxStates) {
					if(!currentStates.contains(temp)) {
						currentStates.add(temp);
					}
				}
			}
			return currentStates;
		}
	}

	private List<MappingState> processPDGNodes(MappingState parent, Set<PDGNode> nodesG1, Set<PDGNode> nodesG2) {
		MappingState.setRestrictedNodesG1(nodesG1);
		MappingState.setRestrictedNodesG2(nodesG2);
		List<MappingState> finalStates = new ArrayList<MappingState>();
		for(PDGNode node1 : nodesG1) {
			List<MappingState> currentStates = new ArrayList<MappingState>();
			for(PDGNode node2 : nodesG2) {
				ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
				boolean match = astNodeMatcher.match(node1, node2);
				if(match && astNodeMatcher.isParameterizable()) {
					PDGNodeMapping mapping = new PDGNodeMapping(node1, node2, astNodeMatcher);
					PDGNodeMapping symmetricalIfNodes = symmetricalIfNodes(node1, node2);
					if(symmetricalIfNodes != null)
						mapping.setSymmetricalIfNodePair(symmetricalIfNodes);
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

	private PDGNodeMapping symmetricalIfNodes(PDGNode nodeG1, PDGNode nodeG2) {
		PDGNode dstNodeG1 = falseControlDependentNode(nodeG1);
		PDGNode dstNodeG2 = falseControlDependentNode(nodeG2);
		if(dstNodeG1 != null && dstNodeG2 != null) {
			PDGNode nodeG1ControlParent = nodeG1.getControlDependenceParent();
			PDGNode nodeG2ControlParent = nodeG2.getControlDependenceParent();
			PDGNode dstNodeG1ControlParent = dstNodeG1.getControlDependenceParent();
			PDGNode dstNodeG2ControlParent = dstNodeG2.getControlDependenceParent();
			if((dstNodeG1ControlParent != null && dstNodeG1ControlParent.equals(nodeG1) && nodeG2ControlParent != null && nodeG2ControlParent.equals(dstNodeG2)) ||
					(dstNodeG2ControlParent != null && dstNodeG2ControlParent.equals(nodeG2) && nodeG1ControlParent != null && nodeG1ControlParent.equals(dstNodeG1))) {
				ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
				astNodeMatcher.match(dstNodeG1, dstNodeG2);
				return new PDGNodeMapping(dstNodeG1, dstNodeG2, astNodeMatcher);
			}
		}
		return null;
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
			int minimum = maximumStates.get(0).getDistinctDifferenceCount();
			maximumStatesWithMinimumDifferences.add(maximumStates.get(0));
			for(int i=1; i<maximumStates.size(); i++) {
				MappingState currentState = maximumStates.get(i);
				if(currentState.getDistinctDifferenceCount() < minimum) {
					minimum = currentState.getDistinctDifferenceCount();
					maximumStatesWithMinimumDifferences.clear();
					maximumStatesWithMinimumDifferences.add(currentState);
				}
				else if(currentState.getDistinctDifferenceCount() == minimum) {
					maximumStatesWithMinimumDifferences.add(currentState);
				}
			}
		}
		return maximumStatesWithMinimumDifferences;
	}

	public PDG getPDG1() {
		return pdg1;
	}

	public PDG getPDG2() {
		return pdg2;
	}

	public MappingState getMaximumStateWithMinimumDifferences() {
		return maximumStateWithMinimumDifferences;
	}

	public CloneStructureNode getCloneStructureRoot() {
		return cloneStructureRoot;
	}

	public TreeSet<PDGNode> getRemovableNodesG1() {
		return mappedNodesG1;
	}

	public TreeSet<PDGNode> getRemovableNodesG2() {
		return mappedNodesG2;
	}

	public TreeSet<PDGNode> getRemainingNodesG1() {
		return nonMappedNodesG1;
	}

	public TreeSet<PDGNode> getRemainingNodesG2() {
		return nonMappedNodesG2;
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

	public Map<String, ArrayList<VariableDeclaration>> getDeclaredLocalVariablesInMappedNodes() {
		Map<String, ArrayList<VariableDeclaration>> declaredVariables = new LinkedHashMap<String, ArrayList<VariableDeclaration>>();
		Set<VariableDeclaration> variableDeclarationsAndAccessedFieldsInMethod1 = pdg1.getVariableDeclarationsAndAccessedFieldsInMethod();
		Set<VariableDeclaration> variableDeclarationsAndAccessedFieldsInMethod2 = pdg2.getVariableDeclarationsAndAccessedFieldsInMethod();
		for(String key : this.declaredLocalVariablesInMappedNodes.keySet()) {
			ArrayList<AbstractVariable> value = this.declaredLocalVariablesInMappedNodes.get(key);
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
			declaredVariables.put(key, variableDeclarations);
		}
		return declaredVariables;
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

	public List<ASTNodeDifference> getNodeDifferences() {
		return maximumStateWithMinimumDifferences.getNodeDifferences();
	}

	public List<ASTNodeDifference> getNonOverlappingNodeDifferences() {
		return maximumStateWithMinimumDifferences.getNonOverlappingNodeDifferences();
	}

	public Set<BindingSignaturePair> getRenamedVariables() {
		return maximumStateWithMinimumDifferences.getRenamedVariables();
	}

	public List<PreconditionViolation> getPreconditionViolations() {
		return preconditionViolations;
	}

	private Set<PlainVariable> variablesToBeReturned(PDG pdg, Set<PDGNode> mappedNodes) {
		Set<PDGNode> remainingNodes = new TreeSet<PDGNode>();
		Iterator<GraphNode> iterator = pdg.getNodeIterator();
		while(iterator.hasNext()) {
			PDGNode pdgNode = (PDGNode)iterator.next();
			if(!mappedNodes.contains(pdgNode)) {
				remainingNodes.add(pdgNode);
			}
		}
		Set<PlainVariable> variablesToBeReturned = new LinkedHashSet<PlainVariable>();
		for(PDGNode remainingNode : remainingNodes) {
			Iterator<GraphEdge> incomingDependenceIt = remainingNode.getIncomingDependenceIterator();
			while(incomingDependenceIt.hasNext()) {
				PDGDependence dependence = (PDGDependence)incomingDependenceIt.next();
				if(dependence instanceof PDGDataDependence) {
					PDGDataDependence dataDependence = (PDGDataDependence)dependence;
					PDGNode srcNode = (PDGNode)dataDependence.getSrc();
					if(mappedNodes.contains(srcNode) && dataDependence.getData() instanceof PlainVariable) {
						PlainVariable variable = (PlainVariable)dataDependence.getData();
						if(!variable.isField())
							variablesToBeReturned.add(variable);
					}
				}
			}
		}
		return variablesToBeReturned;
	}

	private void determineVariablesToBeReturned() {
		TreeSet<PDGNode> removableNodesG1 = getRemovableNodesG1();
		TreeSet<PDGNode> removableNodesG2 = getRemovableNodesG2();
		Set<PlainVariable> variablesToBeReturnedG1 = variablesToBeReturned(pdg1, removableNodesG1);
		Set<PlainVariable> variablesToBeReturnedG2 = variablesToBeReturned(pdg2, removableNodesG2);
		//if the returned variables are more than one, the precondition is violated
		if(variablesToBeReturnedG1.size() > 1 || variablesToBeReturnedG2.size() > 1) {
			PreconditionViolation violation = new ReturnedVariablePreconditionViolation(variablesToBeReturnedG1, variablesToBeReturnedG2,
					PreconditionViolationType.MULTIPLE_RETURNED_VARIABLES);
			preconditionViolations.add(violation);
		}
		else if(variablesToBeReturnedG1.size() == 1 && variablesToBeReturnedG2.size() == 1) {
			PlainVariable returnedVariable1 = variablesToBeReturnedG1.iterator().next();
			PlainVariable returnedVariable2 = variablesToBeReturnedG2.iterator().next();
			if(!returnedVariable1.getVariableName().equals(returnedVariable2.getVariableName())) {
				Set<BindingSignaturePair> renamedVariables = getRenamedVariables();
				boolean isRenamed = false;
				for(BindingSignaturePair signaturePair : renamedVariables) {
					if(signaturePair.getSignature1().containsBinding(returnedVariable1.getVariableBindingKey()) &&
							signaturePair.getSignature2().containsBinding(returnedVariable2.getVariableBindingKey())) {
						isRenamed = true;
						break;
					}
				}
				if(!isRenamed) {
					PreconditionViolation violation = new ReturnedVariablePreconditionViolation(variablesToBeReturnedG1, variablesToBeReturnedG2,
							PreconditionViolationType.DIFFERENT_RETURNED_VARIABLE);
					preconditionViolations.add(violation);
				}
			}
		}
	}

	private void branchStatementWithInnermostLoop(NodeMapping nodeMapping, PDGNode node, Set<PDGNode> mappedNodes) {
		CFGNode cfgNode = node.getCFGNode();
		if(cfgNode instanceof CFGBreakNode) {
			CFGBreakNode breakNode = (CFGBreakNode)cfgNode;
			CFGNode innerMostLoopNode = breakNode.getInnerMostLoopNode();
			if(innerMostLoopNode != null && !mappedNodes.contains(innerMostLoopNode.getPDGNode())) {
				PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
						PreconditionViolationType.BREAK_STATEMENT_WITHOUT_LOOP);
				nodeMapping.addPreconditionViolation(violation);
				preconditionViolations.add(violation);
			}
		}
		else if(cfgNode instanceof CFGContinueNode) {
			CFGContinueNode continueNode = (CFGContinueNode)cfgNode;
			CFGNode innerMostLoopNode = continueNode.getInnerMostLoopNode();
			if(innerMostLoopNode != null && !mappedNodes.contains(innerMostLoopNode.getPDGNode())) {
				PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
						PreconditionViolationType.CONTINUE_STATEMENT_WITHOUT_LOOP);
				nodeMapping.addPreconditionViolation(violation);
				preconditionViolations.add(violation);
			}
		}
	}

	private void checkCloneStructureNodeForPreconditions(CloneStructureNode node) {
		if(node.getMapping() != null)
			checkPreconditions(node);
		for(CloneStructureNode child : node.getChildren()) {
			checkCloneStructureNodeForPreconditions(child);
		}
	}

	private void checkPreconditions(CloneStructureNode node) {
		Set<BindingSignaturePair> renamedVariables = getRenamedVariables();
		TreeSet<PDGNode> removableNodesG1 = getRemovableNodesG1();
		TreeSet<PDGNode> removableNodesG2 = getRemovableNodesG2();
		NodeMapping nodeMapping = node.getMapping();
		for(ASTNodeDifference difference : nodeMapping.getNodeDifferences()) {
			AbstractExpression abstractExpression1 = difference.getExpression1();
			Expression expression1 = abstractExpression1.getExpression();
			AbstractExpression abstractExpression2 = difference.getExpression2();
			Expression expression2 = abstractExpression2.getExpression();
			if(!renamedVariables.contains(difference.getBindingSignaturePair())) {
				if(!isParameterizableExpression(removableNodesG1, abstractExpression1, pdg1.getVariableDeclarationsInMethod(), iCompilationUnit1)) {
					PreconditionViolation violation = new ExpressionPreconditionViolation(difference.getExpression1(),
							PreconditionViolationType.EXPRESSION_DIFFERENCE_CANNOT_BE_PARAMETERIZED);
					nodeMapping.addPreconditionViolation(violation);
					preconditionViolations.add(violation);
					IMethodBinding methodBinding = getMethodBinding(expression1);
					if(methodBinding != null) {
						int methodModifiers = methodBinding.getModifiers();
						if((methodModifiers & Modifier.PRIVATE) != 0) {
							String message = "Inline private method " + methodBinding.getName();
							violation.addSuggestion(message);
						}
					}
				}
				if(!isParameterizableExpression(removableNodesG2, abstractExpression2, pdg2.getVariableDeclarationsInMethod(), iCompilationUnit2)) {
					PreconditionViolation violation = new ExpressionPreconditionViolation(difference.getExpression2(),
							PreconditionViolationType.EXPRESSION_DIFFERENCE_CANNOT_BE_PARAMETERIZED);
					nodeMapping.addPreconditionViolation(violation);
					preconditionViolations.add(violation);
					IMethodBinding methodBinding = getMethodBinding(expression2);
					if(methodBinding != null) {
						int methodModifiers = methodBinding.getModifiers();
						if((methodModifiers & Modifier.PRIVATE) != 0) {
							String message = "Inline private method " + methodBinding.getName();
							violation.addSuggestion(message);
						}
					}
				}
			}
			if(difference.containsDifferenceType(DifferenceType.VARIABLE_TYPE_MISMATCH)) {
				PreconditionViolation violation = new DualExpressionPreconditionViolation(difference.getExpression1(), difference.getExpression2(),
						PreconditionViolationType.INFEASIBLE_UNIFICATION_DUE_TO_VARIABLE_TYPE_MISMATCH);
				nodeMapping.addPreconditionViolation(violation);
				preconditionViolations.add(violation);
				ITypeBinding typeBinding1 = expression1.resolveTypeBinding();
				ITypeBinding typeBinding2 = expression2.resolveTypeBinding();
				if(!typeBinding1.isPrimitive() && !typeBinding2.isPrimitive()) {
					String message = "Make classes " + typeBinding1.getQualifiedName() + " and " + typeBinding2.getQualifiedName() + " extend a common superclass";
					violation.addSuggestion(message);
				}
			}
		}
		if(nodeMapping instanceof PDGNodeGap) {
			if(nodeMapping.getNodeG1() != null) {
				processNonMappedNode(nodeMapping, nodeMapping.getNodeG1(), removableNodesG1);
			}
			if(nodeMapping.getNodeG2() != null) {
				processNonMappedNode(nodeMapping, nodeMapping.getNodeG2(), removableNodesG2);
			}
		}
		if(nodeMapping instanceof PDGNodeMapping) {
			branchStatementWithInnermostLoop(nodeMapping, nodeMapping.getNodeG1(), removableNodesG1);
			branchStatementWithInnermostLoop(nodeMapping, nodeMapping.getNodeG2(), removableNodesG2);
		}
	}

	private void processNonMappedNode(NodeMapping nodeMapping, PDGNode node, TreeSet<PDGNode> removableNodes) {
		if(!removableNodes.isEmpty() && !movableNonMappedNodeBeforeFirstMappedNode(removableNodes, node)) {
			PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
					PreconditionViolationType.UNMATCHED_STATEMENT_CANNOT_BE_MOVED_BEFORE_THE_EXTRACTED_CODE);
			nodeMapping.addPreconditionViolation(violation);
			preconditionViolations.add(violation);
		}
		CFGNode cfgNode = node.getCFGNode();
		if(cfgNode instanceof CFGBreakNode) {
			PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
					PreconditionViolationType.UNMATCHED_BREAK_STATEMENT);
			nodeMapping.addPreconditionViolation(violation);
			preconditionViolations.add(violation);
		}
		else if(cfgNode instanceof CFGContinueNode) {
			PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
					PreconditionViolationType.UNMATCHED_CONTINUE_STATEMENT);
			nodeMapping.addPreconditionViolation(violation);
			preconditionViolations.add(violation);
		}
		else if(cfgNode instanceof CFGExitNode) {
			PreconditionViolation violation = new StatementPreconditionViolation(node.getStatement(),
					PreconditionViolationType.UNMATCHED_RETURN_STATEMENT);
			nodeMapping.addPreconditionViolation(violation);
			preconditionViolations.add(violation);
		}
	}
	//precondition: non-mapped statement can be moved before the first mapped statement
	private boolean movableNonMappedNodeBeforeFirstMappedNode(TreeSet<PDGNode> mappedNodes, PDGNode nonMappedNode) {
		int nodeId = nonMappedNode.getId();
		if(nodeId >= mappedNodes.first().getId() && nodeId <= mappedNodes.last().getId()) {
			Iterator<GraphEdge> incomingDependenceIterator = nonMappedNode.getIncomingDependenceIterator();
			while(incomingDependenceIterator.hasNext()) {
				PDGDependence dependence = (PDGDependence)incomingDependenceIterator.next();
				if(dependence instanceof PDGAbstractDataDependence) {
					PDGAbstractDataDependence dataDependence = (PDGAbstractDataDependence)dependence;
					PDGNode srcPDGNode = (PDGNode)dataDependence.getSrc();
					if(mappedNodes.contains(srcPDGNode))
						return false;
				}
			}
		}
		return true;
	}

	//precondition: differences in expressions should be parameterizable
	private boolean isParameterizableExpression(TreeSet<PDGNode> mappedNodes, AbstractExpression initialAbstractExpression,
			Set<VariableDeclaration> variableDeclarationsInMethod, ICompilationUnit iCompilationUnit) {
		Expression initialExpression = initialAbstractExpression.getExpression();
		Expression expr;
		PDGExpression pdgExpression;
		if(isMethodName(initialExpression)) {
			expr = (Expression)initialExpression.getParent();
			ASTInformationGenerator.setCurrentITypeRoot(iCompilationUnit);
			AbstractExpression tempExpression = new AbstractExpression(expr);
			pdgExpression = new PDGExpression(tempExpression, variableDeclarationsInMethod);
		}
		else {
			expr = initialExpression;
			pdgExpression = new PDGExpression(initialAbstractExpression, variableDeclarationsInMethod);
		}
		//find mapped node containing the expression
		PDGNode nodeContainingExpression = null;
		for(PDGNode node : mappedNodes) {
			if(isExpressionUnderStatement(expr, node.getASTStatement())) {
				nodeContainingExpression = node;
				break;
			}
		}
		if(nodeContainingExpression != null) {
			TreeSet<PDGNode> nodes = new TreeSet<PDGNode>(mappedNodes);
			nodes.remove(nodeContainingExpression);
			Iterator<GraphEdge> incomingDependenceIterator = nodeContainingExpression.getIncomingDependenceIterator();
			while(incomingDependenceIterator.hasNext()) {
				PDGDependence dependence = (PDGDependence)incomingDependenceIterator.next();
				if(dependence instanceof PDGAbstractDataDependence && nodes.contains(dependence.getSrc())) {
					if(dependence instanceof PDGDataDependence) {
						PDGDataDependence dataDependence = (PDGDataDependence)dependence;
						//check if pdgExpression is using dataDependence.data
						if(pdgExpression.usesLocalVariable(dataDependence.getData()))
							return false;
					}
					else if(dependence instanceof PDGAntiDependence) {
						PDGAntiDependence antiDependence = (PDGAntiDependence)dependence;
						//check if pdgExpression is defining dataDependence.data
						if(pdgExpression.definesLocalVariable(antiDependence.getData()))
							return false;
					}
					else if(dependence instanceof PDGOutputDependence) {
						PDGOutputDependence outputDependence = (PDGOutputDependence)dependence;
						//check if pdgExpression is defining dataDependence.data
						if(pdgExpression.definesLocalVariable(outputDependence.getData()))
							return false;
					}
				}
			}
		}
		else {
			//the expression is within the catch/finally blocks of a try statement
			for(PDGNode mappedNode : mappedNodes) {
				Iterator<AbstractVariable> definedVariableIterator = mappedNode.getDefinedVariableIterator();
				while(definedVariableIterator.hasNext()) {
					AbstractVariable definedVariable = definedVariableIterator.next();
					if(pdgExpression.usesLocalVariable(definedVariable) || pdgExpression.definesLocalVariable(definedVariable))
						return false;
				}
				Iterator<AbstractVariable> usedVariableIterator = mappedNode.getUsedVariableIterator();
				while(usedVariableIterator.hasNext()) {
					AbstractVariable usedVariable = usedVariableIterator.next();
					if(pdgExpression.definesLocalVariable(usedVariable))
						return false;
				}
			}
		}
		return true;
	}

	private boolean isExpressionUnderStatement(ASTNode expression, Statement statement) {
		ASTNode parent = expression.getParent();
		if(parent.equals(statement))
			return true;
		if(!(parent instanceof Statement))
			return isExpressionUnderStatement(parent, statement);
		else
			return false;
	}

	private boolean isMethodName(Expression expression) {
		if(expression instanceof SimpleName) {
			SimpleName simpleName = (SimpleName)expression;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null && binding.getKind() == IBinding.METHOD) {
				if(expression.getParent() instanceof Expression) {
					return true;
				}
			}
		}
		return false;
	}
	
	private IMethodBinding getMethodBinding(Expression expression) {
		if(expression instanceof SimpleName) {
			SimpleName simpleName = (SimpleName)expression;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null && binding.getKind() == IBinding.METHOD) {
				return (IMethodBinding)binding;
			}
		}
		else if(expression instanceof MethodInvocation) {
			MethodInvocation methodInvocation = (MethodInvocation)expression;
			return methodInvocation.resolveMethodBinding();
		}
		else if(expression instanceof SuperMethodInvocation) {
			SuperMethodInvocation methodInvocation = (SuperMethodInvocation)expression;
			return methodInvocation.resolveMethodBinding();
		}
		return null;
	}
}
