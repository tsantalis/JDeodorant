package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.CompositeStatementObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CFGBranchDoLoopNode;
import gr.uom.java.ast.decomposition.cfg.CFGBranchIfNode;
import gr.uom.java.ast.decomposition.cfg.CFGBranchSwitchNode;
import gr.uom.java.ast.decomposition.cfg.GraphEdge;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.PDGBlockNode;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGControlPredicateNode;
import gr.uom.java.ast.decomposition.cfg.PDGDataDependence;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGMethodEntryNode;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PDGStatementNode;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.PreconditionViolation;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.matching.BindingSignaturePair;
import gr.uom.java.ast.decomposition.matching.Difference;
import gr.uom.java.ast.decomposition.matching.DifferenceType;

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
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public abstract class DivideAndConquerMatcher {

	//if true full tree match is performed, otherwise subtree match is performed
	private boolean fullTreeMatch;
	private IProgressMonitor monitor;
	private PDG pdg1;
	private PDG pdg2;
	private ICompilationUnit iCompilationUnit1;
	private ICompilationUnit iCompilationUnit2;
	private ControlDependenceTreeNode controlDependenceTreePDG1;
	private ControlDependenceTreeNode controlDependenceTreePDG2;
	private TreeSet<PDGNode> allNodesInSubTreePDG1;
	private TreeSet<PDGNode> allNodesInSubTreePDG2;
	private CloneStructureNode root;
	private MappingState finalState;
	protected PreconditionExaminer preconditionExaminer;
	
	public DivideAndConquerMatcher(PDG pdg1, PDG pdg2,
			ICompilationUnit iCompilationUnit1, ICompilationUnit iCompilationUnit2,
			ControlDependenceTreeNode controlDependenceTreePDG1, ControlDependenceTreeNode controlDependenceTreePDG2,
			boolean fullTreeMatch, IProgressMonitor monitor) {
		this.pdg1 = pdg1;
		this.pdg2 = pdg2;
		this.iCompilationUnit1 = iCompilationUnit1;
		this.iCompilationUnit2 = iCompilationUnit2;
		this.controlDependenceTreePDG1 = controlDependenceTreePDG1;
		this.controlDependenceTreePDG2 = controlDependenceTreePDG2;
		this.fullTreeMatch = fullTreeMatch;
		this.monitor = monitor;
		this.allNodesInSubTreePDG1 = new TreeSet<PDGNode>();
		this.allNodesInSubTreePDG2 = new TreeSet<PDGNode>();
	}

	public boolean hasMappedNodes() {
		return root != null && !root.getChildren().isEmpty() && !getRemovableNodesG1().isEmpty() && !getRemovableNodesG2().isEmpty();
	}

	public PDG getPDG1() {
		return pdg1;
	}

	public PDG getPDG2() {
		return pdg2;
	}

	public String getMethodName1() {
		return pdg1.getMethod().getName();
	}

	public String getMethodName2() {
		return pdg2.getMethod().getName();
	}

	public TreeSet<PDGNode> getRemovableNodesG1() {
		return preconditionExaminer.getRemovableNodesG1();
	}

	public TreeSet<PDGNode> getRemovableNodesG2() {
		return preconditionExaminer.getRemovableNodesG2();
	}

	public TreeSet<PDGNode> getRemainingNodesG1() {
		return preconditionExaminer.getRemainingNodesG1();
	}

	public TreeSet<PDGNode> getRemainingNodesG2() {
		return preconditionExaminer.getRemainingNodesG2();
	}

	public TreeSet<PDGNode> getNonMappedPDGNodesG1MovableBefore() {
		return preconditionExaminer.getNonMappedPDGNodesG1MovableBefore();
	}

	public TreeSet<PDGNode> getNonMappedPDGNodesG1MovableAfter() {
		return preconditionExaminer.getNonMappedPDGNodesG1MovableAfter();
	}

	public TreeSet<PDGNode> getNonMappedPDGNodesG2MovableBefore() {
		return preconditionExaminer.getNonMappedPDGNodesG2MovableBefore();
	}

	public TreeSet<PDGNode> getNonMappedPDGNodesG2MovableAfter() {
		return preconditionExaminer.getNonMappedPDGNodesG2MovableAfter();
	}

	public TreeSet<PDGNode> getAdditionallyMatchedNodesG1() {
		return preconditionExaminer.getAdditionallyMatchedNodesG1();
	}

	public TreeSet<PDGNode> getAdditionallyMatchedNodesG2() {
		return preconditionExaminer.getAdditionallyMatchedNodesG2();
	}

	public Set<AbstractVariable> getDirectlyAccessedLocalFieldsG1() {
		return preconditionExaminer.getDirectlyAccessedLocalFieldsG1();
	}

	public Set<AbstractVariable> getDirectlyAccessedLocalFieldsG2() {
		return preconditionExaminer.getDirectlyAccessedLocalFieldsG2();
	}

	public Set<AbstractVariable> getIndirectlyAccessedLocalFieldsG1() {
		return preconditionExaminer.getIndirectlyAccessedLocalFieldsG1();
	}

	public Set<AbstractVariable> getIndirectlyAccessedLocalFieldsG2() {
		return preconditionExaminer.getIndirectlyAccessedLocalFieldsG2();
	}

	public Set<AbstractVariable> getDirectlyModifiedLocalFieldsG1() {
		return preconditionExaminer.getDirectlyModifiedLocalFieldsG1();
	}

	public Set<AbstractVariable> getDirectlyModifiedLocalFieldsG2() {
		return preconditionExaminer.getDirectlyModifiedLocalFieldsG2();
	}

	public Set<AbstractVariable> getIndirectlyModifiedLocalFieldsG1() {
		return preconditionExaminer.getIndirectlyModifiedLocalFieldsG1();
	}

	public Set<AbstractVariable> getIndirectlyModifiedLocalFieldsG2() {
		return preconditionExaminer.getIndirectlyModifiedLocalFieldsG2();
	}

	public Set<MethodObject> getAccessedLocalMethodsG1() {
		return preconditionExaminer.getAccessedLocalMethodsG1();
	}

	public Set<MethodObject> getAccessedLocalMethodsG2() {
		return preconditionExaminer.getAccessedLocalMethodsG2();
	}

	public Set<MethodObject> getDirectlyAccessedLocalMethodsG1() {
		return preconditionExaminer.getDirectlyAccessedLocalMethodsG1();
	}

	public Set<MethodObject> getDirectlyAccessedLocalMethodsG2() {
		return preconditionExaminer.getDirectlyAccessedLocalMethodsG2();
	}

	public Set<MethodObject> getIndirectlyAccessedLocalMethodsG1() {
		return preconditionExaminer.getIndirectlyAccessedLocalMethodsG1();
	}

	public Set<MethodObject> getIndirectlyAccessedLocalMethodsG2() {
		return preconditionExaminer.getIndirectlyAccessedLocalMethodsG2();
	}

	public Map<VariableBindingKeyPair, ArrayList<VariableDeclaration>> getDeclaredLocalVariablesInMappedNodes() {
		return preconditionExaminer.getDeclaredLocalVariablesInMappedNodes();
	}

	public Set<VariableBindingKeyPair> getDeclaredLocalVariablesInMappedNodesWithinAnonymousClass() {
		return preconditionExaminer.getDeclaredLocalVariablesInMappedNodesWithinAnonymousClass();
	}

	public Set<VariableDeclaration> getDeclaredLocalVariablesInAdditionallyMatchedNodesG1() {
		return preconditionExaminer.getDeclaredLocalVariablesInAdditionallyMatchedNodesG1();
	}

	public Set<String> getDeclaredLocalVariableBindingKeysInAdditionallyMatchedNodesG1() {
		return preconditionExaminer.getDeclaredLocalVariableBindingKeysInAdditionallyMatchedNodesG1();
	}

	public Set<VariableDeclaration> getDeclaredLocalVariablesInAdditionallyMatchedNodesG2() {
		return preconditionExaminer.getDeclaredLocalVariablesInAdditionallyMatchedNodesG2();
	}

	public Set<String> getDeclaredLocalVariableBindingKeysInAdditionallyMatchedNodesG2() {
		return preconditionExaminer.getDeclaredLocalVariableBindingKeysInAdditionallyMatchedNodesG2();
	}

	public Map<VariableBindingKeyPair, ArrayList<VariableDeclaration>> getCommonPassedParameters() {
		return preconditionExaminer.getCommonPassedParameters();
	}

	public List<ASTNodeDifference> getNodeDifferences() {
		return preconditionExaminer.getNodeDifferences();
	}

	public List<ASTNodeDifference> getNonOverlappingNodeDifferences() {
		return preconditionExaminer.getNonOverlappingNodeDifferences();
	}

	public List<PreconditionViolation> getPreconditionViolations() {
		return preconditionExaminer.getPreconditionViolations();
	}

	public Set<BindingSignaturePair> getRenamedVariables() {
		return preconditionExaminer.getRenamedVariables();
	}

	public Set<VariableBindingPair> getRenamedVariableBindings() {
		return preconditionExaminer.getRenamedVariableBindings();
	}

	public Set<VariableDeclaration> getVariablesToBeReturnedG1() {
		return preconditionExaminer.getVariablesToBeReturnedG1();
	}

	public Set<VariableDeclaration> getVariablesToBeReturnedG2() {
		return preconditionExaminer.getVariablesToBeReturnedG2();
	}

	public Set<VariableDeclaration> getDeclaredVariablesInRemainingNodesDefinedByMappedNodesG1() {
		return preconditionExaminer.getDeclaredVariablesInRemainingNodesDefinedByMappedNodesG1();
	}

	public Set<VariableDeclaration> getDeclaredVariablesInRemainingNodesDefinedByMappedNodesG2() {
		return preconditionExaminer.getDeclaredVariablesInRemainingNodesDefinedByMappedNodesG2();
	}

	public boolean movableBeforeFirstMappedNode(PDGNodeMapping nodeMapping) {
		return preconditionExaminer.movableBeforeFirstMappedNode(nodeMapping);
	}

	public ITypeBinding getReturnTypeBinding() {
		return preconditionExaminer.getReturnTypeBinding();
	}

	public CloneType getCloneType() {
		return preconditionExaminer.getCloneType();
	}

	public CloneRefactoringType getCloneRefactoringType() {
		return preconditionExaminer.getCloneRefactoringType();
	}

	public boolean isTemplateMethodApplicable() {
		return preconditionExaminer.isTemplateMethodApplicable();
	}

	protected abstract Set<PDGNode> getNodesInRegion1(PDG pdg, PDGNode controlPredicate, Set<PDGNode> controlPredicateNodesInCurrentLevel,
			Set<PDGNode> controlPredicateNodesInNextLevel, ControlDependenceTreeNode controlDependenceTreeRoot);

	protected abstract Set<PDGNode> getNodesInRegion2(PDG pdg, PDGNode controlPredicate, Set<PDGNode> controlPredicateNodesInCurrentLevel,
			Set<PDGNode> controlPredicateNodesInNextLevel, ControlDependenceTreeNode controlDependenceTreeRoot);

	protected abstract Set<PDGNode> getElseNodesOfSymmetricalIfStatement1(PDG pdg, PDGNode controlPredicate, Set<PDGNode> controlPredicateNodesInCurrentLevel,
			Set<PDGNode> controlPredicateNodesInNextLevel);

	protected abstract Set<PDGNode> getElseNodesOfSymmetricalIfStatement2(PDG pdg, PDGNode controlPredicate, Set<PDGNode> controlPredicateNodesInCurrentLevel,
			Set<PDGNode> controlPredicateNodesInNextLevel);

	protected abstract List<ControlDependenceTreeNode> getIfParentChildren1(ControlDependenceTreeNode cdtNode);

	protected abstract List<ControlDependenceTreeNode> getIfParentChildren2(ControlDependenceTreeNode cdtNode);

	public CloneStructureNode getCloneStructureRoot() {
		return root;
	}
	
	public MappingState getMaximumStateWithMinimumDifferences() {
		return finalState;
	}

	public TreeSet<PDGNode> getAllNodesInSubTreePDG1() {
		return allNodesInSubTreePDG1;
	}

	public TreeSet<PDGNode> getAllNodesInSubTreePDG2() {
		return allNodesInSubTreePDG2;
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
		
		List<MappingState> maximumStatesWithMinimumNonDistinctDifferences = new ArrayList<MappingState>();
		if(maximumStatesWithMinimumDifferences.size() == 1) {
			maximumStatesWithMinimumNonDistinctDifferences.add(maximumStatesWithMinimumDifferences.get(0));
		}
		else {
			int minimum = maximumStatesWithMinimumDifferences.get(0).getNonDistinctDifferenceCount();
			maximumStatesWithMinimumNonDistinctDifferences.add(maximumStatesWithMinimumDifferences.get(0));
			for(int i=1; i<maximumStatesWithMinimumDifferences.size(); i++) {
				MappingState currentState = maximumStatesWithMinimumDifferences.get(i);
				if(currentState.getNonDistinctDifferenceCount() < minimum) {
					minimum = currentState.getNonDistinctDifferenceCount();
					maximumStatesWithMinimumNonDistinctDifferences.clear();
					maximumStatesWithMinimumNonDistinctDifferences.add(currentState);
				}
				else if(currentState.getNonDistinctDifferenceCount() == minimum) {
					maximumStatesWithMinimumNonDistinctDifferences.add(currentState);
				}
			}
		}
		
		List<MappingState> maximumStatesWithMinimumNonDistinctDifferencesIncludingTypeMismatches = new ArrayList<MappingState>();
		if(maximumStatesWithMinimumNonDistinctDifferences.size() == 1) {
			maximumStatesWithMinimumNonDistinctDifferencesIncludingTypeMismatches.add(maximumStatesWithMinimumNonDistinctDifferences.get(0));
		}
		else {
			int minimum = maximumStatesWithMinimumNonDistinctDifferences.get(0).getNonDistinctDifferenceCountIncludingTypeMismatches();
			maximumStatesWithMinimumNonDistinctDifferencesIncludingTypeMismatches.add(maximumStatesWithMinimumNonDistinctDifferences.get(0));
			for(int i=1; i<maximumStatesWithMinimumNonDistinctDifferences.size(); i++) {
				MappingState currentState = maximumStatesWithMinimumNonDistinctDifferences.get(i);
				if(currentState.getNonDistinctDifferenceCountIncludingTypeMismatches() < minimum) {
					minimum = currentState.getNonDistinctDifferenceCountIncludingTypeMismatches();
					maximumStatesWithMinimumNonDistinctDifferencesIncludingTypeMismatches.clear();
					maximumStatesWithMinimumNonDistinctDifferencesIncludingTypeMismatches.add(currentState);
				}
				else if(currentState.getNonDistinctDifferenceCountIncludingTypeMismatches() == minimum) {
					maximumStatesWithMinimumNonDistinctDifferencesIncludingTypeMismatches.add(currentState);
				}
			}
		}
		//TODO: Introduce comparison of difference "weights" in the case of multiple maximum states with minimum differences
		List<MappingState> maximumStatesWithMinimumDifferencesAndMinimumIdDiff = new ArrayList<MappingState>();
		if(maximumStatesWithMinimumNonDistinctDifferencesIncludingTypeMismatches.size() == 1) {
			maximumStatesWithMinimumDifferencesAndMinimumIdDiff.add(maximumStatesWithMinimumNonDistinctDifferencesIncludingTypeMismatches.get(0));
		}
		else {
			int minId1 = allNodesInSubTreePDG1.isEmpty() ? 0 : allNodesInSubTreePDG1.first().getId();
			int minId2 = allNodesInSubTreePDG2.isEmpty() ? 0 : allNodesInSubTreePDG2.first().getId();
			int minimum = maximumStatesWithMinimumNonDistinctDifferencesIncludingTypeMismatches.get(0).getNodeMappingRelativeIdDiff(minId1, minId2);
			maximumStatesWithMinimumDifferencesAndMinimumIdDiff.add(maximumStatesWithMinimumNonDistinctDifferencesIncludingTypeMismatches.get(0));
			for(int i=1; i<maximumStatesWithMinimumNonDistinctDifferencesIncludingTypeMismatches.size(); i++) {
				MappingState currentState = maximumStatesWithMinimumNonDistinctDifferencesIncludingTypeMismatches.get(i);
				if(currentState.getNodeMappingRelativeIdDiff(minId1, minId2) < minimum) {
					minimum = currentState.getNodeMappingRelativeIdDiff(minId1, minId2);
					maximumStatesWithMinimumDifferencesAndMinimumIdDiff.clear();
					maximumStatesWithMinimumDifferencesAndMinimumIdDiff.add(currentState);
				}
				else if(currentState.getNodeMappingRelativeIdDiff(minId1, minId2) == minimum) {
					maximumStatesWithMinimumDifferencesAndMinimumIdDiff.add(currentState);
				}
			}
		}
		
		if(maximumStatesWithMinimumDifferencesAndMinimumIdDiff.size() == 1) {
			return maximumStatesWithMinimumDifferencesAndMinimumIdDiff.get(0);
		}
		else {
			int minimum = maximumStatesWithMinimumDifferencesAndMinimumIdDiff.get(0).getEditDistanceOfDifferences();
			MappingState maximumStateWithMinimumDifferences = maximumStatesWithMinimumDifferencesAndMinimumIdDiff.get(0);
			for(int i=1; i<maximumStatesWithMinimumDifferencesAndMinimumIdDiff.size(); i++) {
				MappingState currentState = maximumStatesWithMinimumDifferencesAndMinimumIdDiff.get(i);
				if(currentState.getEditDistanceOfDifferences() < minimum) {
					minimum = currentState.getEditDistanceOfDifferences();
					maximumStateWithMinimumDifferences = currentState;
				}
			}
			return maximumStateWithMinimumDifferences;
		}
	}

	protected void matchBasedOnControlDependenceTreeStructure() {
		int maxLevel1 = controlDependenceTreePDG1.getMaxLevel();
		int level1 = maxLevel1;
		int maxLevel2 = controlDependenceTreePDG2.getMaxLevel();
		int level2 = maxLevel2;
		if(monitor != null)
			monitor.beginTask("Mapping Program Dependence Graphs", Math.min(maxLevel1, maxLevel2));
		
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
					if(node instanceof PDGBlockNode) {
						controlPredicateNodesInNextLevelG1.addAll(pdg1.getNestedNodesWithinBlockNode((PDGBlockNode)node));
					}
				}
			}
			if(level2 < maxLevel2) {
				Set<PDGNode> nodesInNextLevel = controlDependenceTreePDG2.getControlPredicateNodesInLevel(level2+1);
				controlPredicateNodesInNextLevelG2.addAll(nodesInNextLevel);
				for(PDGNode node : nodesInNextLevel) {
					if(node instanceof PDGBlockNode) {
						controlPredicateNodesInNextLevelG2.addAll(pdg2.getNestedNodesWithinBlockNode((PDGBlockNode)node));
					}
				}
			}
			for(PDGNode predicate1 : controlPredicateNodesG1) {
				Set<PDGNode> nodesG1 = getNodesInRegion1(pdg1, predicate1, controlPredicateNodesG1, controlPredicateNodesInNextLevelG1, controlDependenceTreePDG1);
				//special handling in level 0 for sub tree match
				if(level1 == 0 && !fullTreeMatch) {
					//int minId = allNodesInSubTreePDG1.first().getId();
					int maxId = allNodesInSubTreePDG1.last().getId();
					Set<PDGNode> nodesG1ToBeRemoved = new LinkedHashSet<PDGNode>();
					for(PDGNode nodeG1 : nodesG1) {
						if(nodeG1.getId() > maxId/* || nodeG1.getId() < minId*/) {
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
					Set<PDGNode> nodesG2 = getNodesInRegion2(pdg2, predicate2, controlPredicateNodesG2, controlPredicateNodesInNextLevelG2, controlDependenceTreePDG2);
					//special handling to add the nodes inside the final else of a symmetrical if/else if
					for(CloneStructureNode parentCloneStructure : parents) {
						if(parentCloneStructure.getMapping() instanceof PDGNodeMapping) {
							PDGNodeMapping pdgNodeMapping = (PDGNodeMapping)parentCloneStructure.getMapping();
							PDGNodeMapping symmetricalPDGNodeMapping = pdgNodeMapping.getSymmetricalIfNodePair();
							if(symmetricalPDGNodeMapping != null) {
								if(symmetricalPDGNodeMapping.getNodeG1().equals(predicate1) && symmetricalPDGNodeMapping.getNodeG2().equals(predicate2)) {
									Set<PDGNode> elseNodes = getElseNodesOfSymmetricalIfStatement2(pdg2, pdgNodeMapping.getNodeG2(), controlPredicateNodesG2, controlPredicateNodesInNextLevelG2);
									nodesG2.addAll(elseNodes);
								}
							}
						}
					}
					if(level2 == 0 && !fullTreeMatch) {
						//int minId = allNodesInSubTreePDG2.first().getId();
						int maxId = allNodesInSubTreePDG2.last().getId();
						Set<PDGNode> nodesG2ToBeRemoved = new LinkedHashSet<PDGNode>();
						for(PDGNode nodeG2 : nodesG2) {
							if(nodeG2.getId() > maxId/* || nodeG2.getId() < minId*/) {
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
						if(predicate1.getCFGNode() instanceof CFGBranchSwitchNode && predicate2.getCFGNode() instanceof CFGBranchSwitchNode) {
							maxStates = matchBasedOnSwitchCases(finalState, nodesG1, nodesG2);
						}
						else {
							maxStates = matchBasedOnCodeFragments(finalState, nodesG1, nodesG2);
						}
					}
					else {
						ControlDependenceTreeNode cdtNode1 = controlDependenceTreePDG1.getNode(predicate1);
						ControlDependenceTreeNode cdtNode2 = controlDependenceTreePDG2.getNode(predicate2);
						//check parent-child relationship preservation (parent in the current level, children in the previously examined level)
						if(level1 < maxLevel1 && level2 < maxLevel2 && cdtNode1 != null && cdtNode2 != null) {
							List<ControlDependenceTreeNode> children1 = cdtNode1.getChildren();
							List<ControlDependenceTreeNode> children2 = cdtNode2.getChildren();
							if(finalState != null && !children1.isEmpty() && !children2.isEmpty()) {
								Set<PDGNodeMapping> nodeMappings = finalState.getNodeMappings();
								boolean childPairFoundInFinalState = false;
								for(PDGNodeMapping nodeMapping : nodeMappings) {
									ControlDependenceTreeNode cdtChildNode1 = controlDependenceTreePDG1.getNode(nodeMapping.getNodeG1());
									ControlDependenceTreeNode cdtChildNode2 = controlDependenceTreePDG2.getNode(nodeMapping.getNodeG2());
									if(cdtChildNode1 != null && cdtChildNode2 != null) {
										if(children1.contains(cdtChildNode1) && children2.contains(cdtChildNode2)) {
											childPairFoundInFinalState = true;
											break;
										}
									}
								}
								if(!childPairFoundInFinalState) {
									Set<ControlDependenceTreeNode> elseNodes1 = controlDependenceTreePDG1.getElseNodesInLevel(level1);
									Set<ControlDependenceTreeNode> elseNodes2 = controlDependenceTreePDG2.getElseNodesInLevel(level2);
									ControlDependenceTreeNode elseNode1 = controlDependenceTreePDG1.getElseNode(predicate1);
									ControlDependenceTreeNode elseNode2 = controlDependenceTreePDG2.getElseNode(predicate2);
									if(elseNode1 != null && elseNode2 != null && elseNodes1.contains(elseNode1) && elseNodes2.contains(elseNode2)) {
										List<ControlDependenceTreeNode> elseChildren1 = elseNode1.getChildren();
										List<ControlDependenceTreeNode> elseChildren2 = elseNode2.getChildren();
										boolean ifElseChildPairFoundInFinalState = false;
										boolean elseIfChildPairFoundInFinalState = false;
										for(PDGNodeMapping nodeMapping : nodeMappings) {
											ControlDependenceTreeNode cdtChildNode1 = controlDependenceTreePDG1.getNode(nodeMapping.getNodeG1());
											ControlDependenceTreeNode cdtChildNode2 = controlDependenceTreePDG2.getNode(nodeMapping.getNodeG2());
											if(cdtChildNode1 != null && cdtChildNode2 != null) {
												if(children1.contains(cdtChildNode1) && elseChildren2.contains(cdtChildNode2)) {
													ifElseChildPairFoundInFinalState = true;
												}
												if(elseChildren1.contains(cdtChildNode1) && children2.contains(cdtChildNode2)) {
													elseIfChildPairFoundInFinalState = true;
												}
											}
										}
										if(ifElseChildPairFoundInFinalState && elseIfChildPairFoundInFinalState) {
											//symmetrical if-else match
											Set<PDGNode> nodesNestedUnderIf1 = new LinkedHashSet<PDGNode>();
											Set<PDGNode> nodesNestedUnderIf2 = new LinkedHashSet<PDGNode>();
											Set<PDGNode> nodesNestedUnderElse1 = new LinkedHashSet<PDGNode>();
											Set<PDGNode> nodesNestedUnderElse2 = new LinkedHashSet<PDGNode>();
											findNodesNestedUnderIfAndElse(predicate1, nodesG1, nodesNestedUnderIf1, nodesNestedUnderElse1);
											findNodesNestedUnderIfAndElse(predicate2, nodesG2, nodesNestedUnderIf2, nodesNestedUnderElse2);
											ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
											boolean match = astNodeMatcher.match(predicate1, predicate2);
											if(match && astNodeMatcher.isParameterizable()) {
												CompositeStatementObject composite1 = (CompositeStatementObject)predicate1.getStatement();
												List<AbstractExpression> expressions1 = composite1.getExpressions();
												CompositeStatementObject composite2 = (CompositeStatementObject)predicate2.getStatement();
												List<AbstractExpression> expressions2 = composite2.getExpressions();
												AbstractExpression conditional1 = expressions1.get(0);
												AbstractExpression conditional2 = expressions2.get(0);
												//check if there is already a difference covering the entire conditional expressions
												boolean found = false;
												for(ASTNodeDifference nodeDifference : astNodeMatcher.getDifferences()) {
													if(nodeDifference.getExpression1().toString().equals(conditional1.toString()) &&
															nodeDifference.getExpression2().toString().equals(conditional2.toString())) {
														found = true;
														Difference diff = new Difference(conditional1.toString(),conditional2.toString(),
																DifferenceType.IF_ELSE_SYMMETRICAL_MATCH);
														nodeDifference.addDifference(diff);
														break;
													}
												}
												if(!found) {
													ASTNodeDifference nodeDifference = new ASTNodeDifference(conditional1, conditional2);
													Difference diff = new Difference(conditional1.toString(),conditional2.toString(),
															DifferenceType.IF_ELSE_SYMMETRICAL_MATCH);
													nodeDifference.addDifference(diff);
													astNodeMatcher.getDifferences().add(nodeDifference);
												}
												PDGNodeMapping mapping = new PDGNodeMapping(predicate1, predicate2, astNodeMatcher);
												mapping.setSymmetricalIfElse(true);
												MappingState state = new MappingState(finalState, mapping);
												if(finalState != null)
													finalState.addChild(state);
												//match nested nodes symmetrically
												maxStates = matchBasedOnIfElseSymmetry(state, nodesNestedUnderIf1, nodesNestedUnderIf2,
														nodesNestedUnderElse1, nodesNestedUnderElse2);
												for(MappingState temp : maxStates) {
													if(!currentStates.contains(temp)) {
														currentStates.add(temp);
													}
												}
											}
										}
									}
								}
								if(!childPairFoundInFinalState) {
									continue;
								}
							}
							//handle the case where only one of the cdtNodes has mapped children
							if(finalState != null && !children1.isEmpty() && children2.isEmpty()) {
								//cdtNode2 has no children
								Set<PDGNodeMapping> nodeMappings = finalState.getNodeMappings();
								boolean cdtNode1MappedChild = false;
								for(PDGNodeMapping nodeMapping : nodeMappings) {
									ControlDependenceTreeNode cdtChildNode1 = controlDependenceTreePDG1.getNode(nodeMapping.getNodeG1());
									if(cdtChildNode1 != null && children1.contains(cdtChildNode1)) {
										cdtNode1MappedChild = true;
										break;
									}
								}
								if(cdtNode1MappedChild) {
									continue;
								}
							}
							if(finalState != null && children1.isEmpty() && !children2.isEmpty()) {
								//cdtNode1 has no children
								Set<PDGNodeMapping> nodeMappings = finalState.getNodeMappings();
								boolean cdtNode2MappedChild = false;
								for(PDGNodeMapping nodeMapping : nodeMappings) {
									ControlDependenceTreeNode cdtChildNode2 = controlDependenceTreePDG2.getNode(nodeMapping.getNodeG2());
									if(cdtChildNode2 != null && children2.contains(cdtChildNode2)) {
										cdtNode2MappedChild = true;
										break;
									}
								}
								if(cdtNode2MappedChild) {
									continue;
								}
							}
						}
						//check sibling relationship preservation (all siblings in the current level)
						ControlDependenceTreeNode cdtNode1Parent = null;
						ControlDependenceTreeNode cdtNode2Parent = null;
						if(cdtNode1 != null && cdtNode2 != null) {
							cdtNode1Parent = cdtNode1.getParent();
							cdtNode2Parent = cdtNode2.getParent();
							if(finalState != null) {
								Set<PDGNodeMapping> nodeMappings = finalState.getNodeMappings();
								List<ControlDependenceTreeNode> parentChildren1 = cdtNode1Parent.getChildren();
								//set true only if cdtNode is the first node in the list of children
								boolean siblingPairFoundInFinalState = !parentChildren1.isEmpty() && parentChildren1.get(0).equals(cdtNode1);
								boolean ifParentChildFoundInFinalState = false;
								List<ControlDependenceTreeNode> ifParentChildren1 = getIfParentChildren1(cdtNode1Parent);
								List<ControlDependenceTreeNode> ifParentChildren2 = getIfParentChildren2(cdtNode2Parent);
								List<ControlDependenceTreeNode> siblings1 = cdtNode1.getSiblings();
								List<ControlDependenceTreeNode> siblings2 = cdtNode2.getSiblings();
								for(PDGNodeMapping nodeMapping : nodeMappings) {
									ControlDependenceTreeNode cdtSiblingNode1 = controlDependenceTreePDG1.getNode(nodeMapping.getNodeG1());
									ControlDependenceTreeNode cdtSiblingNode2 = controlDependenceTreePDG2.getNode(nodeMapping.getNodeG2());
									if(cdtSiblingNode1 != null && cdtSiblingNode2 != null) {
										if(cdtNode1Parent.isElseNode() && cdtNode2Parent.isElseNode()) {
											if(ifParentChildren1.contains(cdtSiblingNode1) && ifParentChildren2.contains(cdtSiblingNode2)) {
												ifParentChildFoundInFinalState = true;
											}
										}
										if(siblings1.contains(cdtSiblingNode1) && siblings2.contains(cdtSiblingNode2)) {
											siblingPairFoundInFinalState = true;
											break;
										}
									}
								}
								if(cdtNode1Parent.isElseNode() && cdtNode2Parent.isElseNode() &&
										!ifParentChildren1.isEmpty() && !ifParentChildren2.isEmpty()) {
									if(!ifParentChildFoundInFinalState) {
										continue;
									}
								}
								else if(!siblingPairFoundInFinalState) {
									continue;
								}
							}
						}
						if(predicate1.getASTStatement() instanceof SwitchStatement && predicate2.getASTStatement() instanceof SwitchStatement) {
							ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
							boolean match = astNodeMatcher.match(predicate1, predicate2);
							if(match && astNodeMatcher.isParameterizable()) {
								PDGNodeMapping mapping = new PDGNodeMapping(predicate1, predicate2, astNodeMatcher);
								MappingState state = new MappingState(finalState, mapping);
								if(finalState != null)
									finalState.addChild(state);
								//remove switch nodes from the nodes to be processed
								Set<PDGNode> switchBodyNodes1 = new LinkedHashSet<PDGNode>(nodesG1);
								switchBodyNodes1.remove(predicate1);
								Set<PDGNode> switchBodyNodes2 = new LinkedHashSet<PDGNode>(nodesG2);
								switchBodyNodes2.remove(predicate2);
								maxStates = matchBasedOnSwitchCases(state, switchBodyNodes1, switchBodyNodes2);
							}
							else {
								maxStates = new ArrayList<MappingState>();
							}
						}
						else {
							ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
							if(cdtNode1 != null && cdtNode2 != null && (cdtNode1.ifStatementInsideElseIfChain() || cdtNode2.ifStatementInsideElseIfChain()) &&
									!cdtNode1.isTernary() && !cdtNode2.isTernary()) {
								if(cdtNode1.getLengthOfElseIfChain() == cdtNode2.getLengthOfElseIfChain()) {
									maxStates = processPDGNodes(finalState, nodesG1, nodesG2);
								}
								else {
									maxStates = new ArrayList<MappingState>();
								}
							}
							else if(cdtNode1 != null && cdtNode2 != null && astNodeMatcher.match(predicate1, predicate2) && astNodeMatcher.isParameterizable()) {
								maxStates = processPDGNodes(finalState, nodesG1, nodesG2);
							}
							else {
								maxStates = new ArrayList<MappingState>();
							}
						}
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
					//if predicate is a do-loop place it before the nodes nested inside it
					if(predicate1.getCFGNode() instanceof CFGBranchDoLoopNode) {
						Set<PDGNode> controlDependentNodes1 = new LinkedHashSet<PDGNode>();
						for(PDGNode pdgNode : predicate1.getControlDependentNodes()) {
							if(pdg1.isDirectlyNestedWithinBlockNode(pdgNode) == null) {
								controlDependentNodes1.add(pdgNode);
							}
						}
						PDGNodeMapping firstNonPredicateNestedInDoLoop = null;
						for(PDGNodeMapping mapping : nodeMappings) {
							if(mapping.getNodeG1() instanceof PDGStatementNode && controlDependentNodes1.contains(mapping.getNodeG1())) {
								firstNonPredicateNestedInDoLoop = mapping;
								break;
							}
						}
						PDGNodeMapping doLoop = null;
						for(PDGNodeMapping mapping : nodeMappings) {
							if(mapping.getNodeG1().equals(predicate1)) {
								doLoop = mapping;
								break;
							}
						}
						if(firstNonPredicateNestedInDoLoop != null && doLoop != null) {
							int firstNonPredicateNestedInDoLoopPosition = nodeMappings.indexOf(firstNonPredicateNestedInDoLoop);
							int doLoopPosition = nodeMappings.indexOf(doLoop);
							nodeMappings.remove(doLoopPosition);
							nodeMappings.add(firstNonPredicateNestedInDoLoopPosition, doLoop);
						}
					}
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
							//search for symmetrical if statements
							for(int j=0; j<index; j++) {
								PDGNodeMapping otherMapping = nodeMappings.get(j);
								if(symmetricalIfNodes(mapping.getNodeG1(), mapping.getNodeG2(), otherMapping.getNodeG1(), otherMapping.getNodeG2())) {
									mapping.setSymmetricalIfNodePair(otherMapping);
									otherMapping.setSymmetricalIfNodePair(mapping);
								}
							}
						}
						else {
							PDGBlockNode nestedUnderTry1 = pdg1.isDirectlyNestedWithinBlockNode(mapping.getNodeG1());
							PDGBlockNode nestedUnderTry2 = pdg2.isDirectlyNestedWithinBlockNode(mapping.getNodeG2());
							boolean nestedUnderTry = nestedUnderTry1 != null && nestedUnderTry2 != null;
							boolean isNode1FalseControlDependent = false;
							if(parent.getMapping() instanceof PDGNodeMapping) {
								PDGNodeMapping pdgNodeMapping = (PDGNodeMapping)parent.getMapping();
								if(pdgNodeMapping.isSymmetricalIfElse() && mapping.isNode1FalseControlDependent()) {
									isNode1FalseControlDependent = true;
								}
							}
							if((mapping.isFalseControlDependent() || isNode1FalseControlDependent) && !nestedUnderTry) {
								if(newElseParent == null) {
									ControlDependenceTreeNode elseNodeG1 = controlDependenceTreePDG1.getElseNode(parent.getMapping().getNodeG1());
									ControlDependenceTreeNode elseNodeG2 = controlDependenceTreePDG2.getElseNode(parent.getMapping().getNodeG2());
									if(parent.getMapping() instanceof PDGNodeMapping) {
										PDGNodeMapping pdgNodeMapping = (PDGNodeMapping)parent.getMapping();
										PDGNodeMapping symmetricalIfNodeMapping = pdgNodeMapping.getSymmetricalIfNodePair();
										if(symmetricalIfNodeMapping != null) {
											boolean symmetricalIfFoundInParents = false;
											for(CloneStructureNode parentCloneStructureNode : parents) {
												if(parentCloneStructureNode.getMapping().equals(symmetricalIfNodeMapping)) {
													symmetricalIfFoundInParents = true;
													break;
												}
											}
											if(symmetricalIfFoundInParents) {
												if(elseNodeG1 == null) {
													elseNodeG1 = controlDependenceTreePDG1.getElseNode(symmetricalIfNodeMapping.getNodeG1());
												}
												if(elseNodeG2 == null) {
													elseNodeG2 = controlDependenceTreePDG2.getElseNode(symmetricalIfNodeMapping.getNodeG2());
												}
											}
										}
									}
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
							boolean isNode1FalseControlDependent = parentNodeMapping.isSymmetricalIfElse() && previousParentNodeMapping.isNode1FalseControlDependent();
							boolean newParentChildRelationship1 = controlDependenceTreePDG1.parentChildRelationship(parentId1, previousParentId1);
							boolean newParentChildRelationship2 = controlDependenceTreePDG2.parentChildRelationship(parentId2, previousParentId2);
							if((newParentChildRelationship1 && newParentChildRelationship2) || (newParentChildRelationship1 && parentNodeMapping.isSymmetricalIfElse())) {
								parent.addChild(previousParent);
								parentIterator.remove();
							}
							else if(previousParentNodeMapping.isFalseControlDependent() || isNode1FalseControlDependent) {
								if(newElseParent == null) {
									ControlDependenceTreeNode elseNodeG1 = controlDependenceTreePDG1.getElseNode(parentNodeMapping.getNodeG1());
									ControlDependenceTreeNode elseNodeG2 = controlDependenceTreePDG2.getElseNode(parentNodeMapping.getNodeG2());
									PDGNodeMapping symmetricalIfNodeMapping = parentNodeMapping.getSymmetricalIfNodePair();
									if(symmetricalIfNodeMapping != null) {
										boolean symmetricalIfFoundInParents = false;
										for(CloneStructureNode parentCloneStructureNode : parents) {
											if(parentCloneStructureNode.getMapping().equals(symmetricalIfNodeMapping)) {
												symmetricalIfFoundInParents = true;
												break;
											}
										}
										if(symmetricalIfFoundInParents) {
											if(elseNodeG1 == null) {
												elseNodeG1 = controlDependenceTreePDG1.getElseNode(symmetricalIfNodeMapping.getNodeG1());
											}
											if(elseNodeG2 == null) {
												elseNodeG2 = controlDependenceTreePDG2.getElseNode(symmetricalIfNodeMapping.getNodeG2());
											}
										}
									}
									if(elseNodeG1 != null && elseNodeG2 != null) {
										boolean parentChildRelationship1 = controlDependenceTreePDG1.parentChildRelationship(elseNodeG1.getId(), previousParentId1);
										boolean parentChildRelationship2 = controlDependenceTreePDG2.parentChildRelationship(elseNodeG2.getId(), previousParentId2);
										if((parentChildRelationship1 && parentChildRelationship2) || (parentChildRelationship1 && isNode1FalseControlDependent)) {
											PDGElseMapping elseMapping = new PDGElseMapping(elseNodeG1.getId(), elseNodeG2.getId());
											if(parentNodeMapping.isSymmetricalIfElse()) {
												elseMapping.setSymmetricalIfNodePair(parentNodeMapping);
											}
											newElseParent = new CloneStructureNode(elseMapping);
											parent.addChild(newElseParent);
											newElseParent.addChild(previousParent);
											parentIterator.remove();
										}
									}
								}
								else {
									PDGElseMapping elseMapping = (PDGElseMapping) newElseParent.getMapping();
									boolean parentChildRelationship1 = controlDependenceTreePDG1.parentChildRelationship(elseMapping.getId1(), previousParentId1);
									boolean parentChildRelationship2 = controlDependenceTreePDG2.parentChildRelationship(elseMapping.getId2(), previousParentId2);
									if((parentChildRelationship1 && parentChildRelationship2) || (parentChildRelationship1 && isNode1FalseControlDependent)) {
										newElseParent.addChild(previousParent);
										parentIterator.remove();
									}
								}
							}
						}
						parents.add(parent);
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
	}

	private void findNodesNestedUnderIfAndElse(PDGNode predicate, Set<PDGNode> nodes, Set<PDGNode> nodesNestedUnderIf, Set<PDGNode> nodesNestedUnderElse) {
		for(PDGNode node : nodes) {
			if(node.getControlDependenceParent().equals(predicate)) {
				PDGControlDependence controlDependence = node.getIncomingControlDependence();
				if(controlDependence.isTrueControlDependence()) {
					nodesNestedUnderIf.add(node);
				}
				if(controlDependence.isFalseControlDependence()) {
					nodesNestedUnderElse.add(node);
				}
			}
		}
	}

	private boolean symmetricalIfNodes(PDGNode nodeG1, PDGNode nodeG2, PDGNode dstNodeG1, PDGNode dstNodeG2) {
		PDGNode nodeG1ControlParent = nodeG1.getControlDependenceParent();
		PDGNode nodeG2ControlParent = nodeG2.getControlDependenceParent();
		PDGNode dstNodeG1ControlParent = dstNodeG1.getControlDependenceParent();
		PDGNode dstNodeG2ControlParent = dstNodeG2.getControlDependenceParent();
		if(((dstNodeG1ControlParent != null && dstNodeG1ControlParent.equals(nodeG1) && nodeG2ControlParent != null && nodeG2ControlParent.equals(dstNodeG2)) ||
				(dstNodeG2ControlParent != null && dstNodeG2ControlParent.equals(nodeG2) && nodeG1ControlParent != null && nodeG1ControlParent.equals(dstNodeG1))) &&
				dstNodeG1.getCFGNode() instanceof CFGBranchIfNode && dstNodeG2.getCFGNode() instanceof CFGBranchIfNode) {
			return true;
		}
		return false;
	}

	private List<MappingState> matchBasedOnIfElseSymmetry(MappingState parent, Set<PDGNode> nodesNestedUnderIf1, Set<PDGNode> nodesNestedUnderIf2,
			Set<PDGNode> nodesNestedUnderElse1, Set<PDGNode> nodesNestedUnderElse2) {
		Map<Integer, Set<PDGNode>> map1 = new LinkedHashMap<Integer, Set<PDGNode>>();
		Map<Integer, Set<PDGNode>> map2 = new LinkedHashMap<Integer, Set<PDGNode>>();
		map1.put(1, nodesNestedUnderIf1);
		map1.put(2, nodesNestedUnderElse1);
		map2.put(1, nodesNestedUnderElse2);
		map2.put(2, nodesNestedUnderIf2);
		MappingState finalState = parent;
		for(Integer variable1 : map1.keySet()) {
			Set<PDGNode> variableNodesG1 = map1.get(variable1);
			Set<PDGNode> tempNodesG1 = new LinkedHashSet<PDGNode>(variableNodesG1);
			Map<Integer, List<MappingState>> currentStateMap = new LinkedHashMap<Integer, List<MappingState>>();
			for(Integer variable2 : map2.keySet()) {
				if(variable1.equals(variable2)) {
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
			}
			List<MappingState> currentStates = new ArrayList<MappingState>();
			for(Integer variable2 : currentStateMap.keySet()) {
				currentStates.addAll(currentStateMap.get(variable2));
			}
			if(!currentStates.isEmpty()) {
				MappingState best = findMaximumStateWithMinimumDifferences(currentStates);
				finalState = best;
			}
		}
		List<MappingState> currentStates = new ArrayList<MappingState>();
		if(finalState != null)
			currentStates.add(finalState);
		return currentStates;
	}

	private List<MappingState> matchBasedOnCodeFragments(MappingState parent, Set<PDGNode> nodesG1, Set<PDGNode> nodesG2) {
		CodeFragmentDecomposer cfd1 = new CodeFragmentDecomposer(nodesG1, pdg1.getFieldsAccessedInMethod());
		CodeFragmentDecomposer cfd2 = new CodeFragmentDecomposer(nodesG2, pdg2.getFieldsAccessedInMethod());
		Map<PlainVariable, Set<PDGNode>> map1 = new LinkedHashMap<PlainVariable, Set<PDGNode>>(cfd1.getObjectNodeMap());
		Map<PlainVariable, Set<PDGNode>> map2 = new LinkedHashMap<PlainVariable, Set<PDGNode>>(cfd2.getObjectNodeMap());
		Set<PlainVariable> variables1 = new LinkedHashSet<PlainVariable>(map1.keySet());
		Set<PlainVariable> variables2 = new LinkedHashSet<PlainVariable>(map2.keySet());
		MappingState finalState = parent;
		if(map1.isEmpty() || map2.isEmpty()) {
			List<MappingState> currentStates = new ArrayList<MappingState>();
			Set<PDGNode> tempNodesG1 = new LinkedHashSet<PDGNode>(nodesG1);
			Set<PDGNode> tempNodesG2 = new LinkedHashSet<PDGNode>(nodesG2);
			List<MappingState> tempStates = matchBasedOnIdenticalStatements(finalState, tempNodesG1, tempNodesG2, variables1, variables2);
			if(!tempStates.isEmpty()) {
				finalState = findMaximumStateWithMinimumDifferences(tempStates);
				for(PDGNodeMapping mapping : finalState.getNodeMappings()) {
					if(tempNodesG1.contains(mapping.getNodeG1()))
						tempNodesG1.remove(mapping.getNodeG1());
					if(tempNodesG2.contains(mapping.getNodeG2()))
						tempNodesG2.remove(mapping.getNodeG2());
				}
			}
			if(tempNodesG1.isEmpty() || tempNodesG2.isEmpty()) {
				if(finalState != null)
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
		else {
			if(map1.keySet().size() <= map2.keySet().size()) {
				for(PlainVariable variable1 : map1.keySet()) {
					Set<PDGNode> variableNodesG1 = map1.get(variable1);
					Set<PDGNode> tempNodesG1 = new LinkedHashSet<PDGNode>(variableNodesG1);
					Set<String> incomingVariables1 = getIncomingDependenciesFromVariables(variable1.getVariableName(), cfd1);
					incomingVariables1.addAll(getIncomingDependenciesFromVariables(variable1.getVariableName(), cfd2));
					Map<PlainVariable, List<MappingState>> currentStateMap = new LinkedHashMap<PlainVariable, List<MappingState>>();
					for(PlainVariable variable2 : map2.keySet()) {
						Set<PDGNode> variableNodesG2 = map2.get(variable2);
						Set<PDGNode> tempNodesG2 = new LinkedHashSet<PDGNode>(variableNodesG2);
						Set<String> incomingVariables2 = getIncomingDependenciesFromVariables(variable2.getVariableName(), cfd2);
						incomingVariables2.addAll(getIncomingDependenciesFromVariables(variable2.getVariableName(), cfd1));
						if(incomingVariables1.contains(variable2.getVariableName()) || incomingVariables2.contains(variable1.getVariableName())) {
							continue;
						}
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
			}
			else {
				for(PlainVariable variable2 : map2.keySet()) {
					Set<PDGNode> variableNodesG2 = map2.get(variable2);
					Set<PDGNode> tempNodesG2 = new LinkedHashSet<PDGNode>(variableNodesG2);
					Set<String> incomingVariables2 = getIncomingDependenciesFromVariables(variable2.getVariableName(), cfd2);
					incomingVariables2.addAll(getIncomingDependenciesFromVariables(variable2.getVariableName(), cfd1));
					Map<PlainVariable, List<MappingState>> currentStateMap = new LinkedHashMap<PlainVariable, List<MappingState>>();
					for(PlainVariable variable1 : map1.keySet()) {
						Set<PDGNode> variableNodesG1 = map1.get(variable1);
						Set<PDGNode> tempNodesG1 = new LinkedHashSet<PDGNode>(variableNodesG1);
						Set<String> incomingVariables1 = getIncomingDependenciesFromVariables(variable1.getVariableName(), cfd1);
						incomingVariables1.addAll(getIncomingDependenciesFromVariables(variable1.getVariableName(), cfd2));
						if(incomingVariables1.contains(variable2.getVariableName()) || incomingVariables2.contains(variable1.getVariableName())) {
							continue;
						}
						if(finalState != null) {
							for(PDGNodeMapping mapping : finalState.getNodeMappings()) {
								if(tempNodesG1.contains(mapping.getNodeG1()))
									tempNodesG1.remove(mapping.getNodeG1());
								if(tempNodesG2.contains(mapping.getNodeG2()))
									tempNodesG2.remove(mapping.getNodeG2());
							}
						}
						List<MappingState> maxStates = processPDGNodes(finalState, tempNodesG1, tempNodesG2);
						currentStateMap.put(variable1, maxStates);
					}
					List<MappingState> currentStates = new ArrayList<MappingState>();
					for(PlainVariable variable1 : currentStateMap.keySet()) {
						currentStates.addAll(currentStateMap.get(variable1));
					}
					if(!currentStates.isEmpty()) {
						MappingState best = findMaximumStateWithMinimumDifferences(currentStates);
						PlainVariable variableToRemove = null;
						for(PlainVariable variable1 : currentStateMap.keySet()) {
							if(currentStateMap.get(variable1).contains(best)) {
								variableToRemove = variable1;
								break;
							}
						}
						map1.remove(variableToRemove);
						finalState = best;
					}
				}
			}
			List<MappingState> currentStates = new ArrayList<MappingState>();
			Set<PDGNode> tempNodesG1 = new LinkedHashSet<PDGNode>(nodesG1);
			Set<PDGNode> tempNodesG2 = new LinkedHashSet<PDGNode>(nodesG2);
			if(finalState != null) {
				for(PDGNodeMapping mapping : finalState.getNodeMappings()) {
					if(tempNodesG1.contains(mapping.getNodeG1()))
						tempNodesG1.remove(mapping.getNodeG1());
					if(tempNodesG2.contains(mapping.getNodeG2()))
						tempNodesG2.remove(mapping.getNodeG2());
				}
			}
			if(tempNodesG1.isEmpty() || tempNodesG2.isEmpty()) {
				if(finalState != null)
					currentStates.add(finalState);
			}
			else {
				List<MappingState> tempStates = matchBasedOnIdenticalStatements(finalState, tempNodesG1, tempNodesG2, variables1, variables2);
				if(!tempStates.isEmpty()) {
					finalState = findMaximumStateWithMinimumDifferences(tempStates);
					for(PDGNodeMapping mapping : finalState.getNodeMappings()) {
						if(tempNodesG1.contains(mapping.getNodeG1()))
							tempNodesG1.remove(mapping.getNodeG1());
						if(tempNodesG2.contains(mapping.getNodeG2()))
							tempNodesG2.remove(mapping.getNodeG2());
					}
				}
				if(tempNodesG1.isEmpty() || tempNodesG2.isEmpty()) {
					if(finalState != null)
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
			}
			return currentStates;
		}
	}

	private Set<String> getIncomingDependenciesFromVariables(String variableName, CodeFragmentDecomposer cfd) {
		Set<PlainVariable> allVariables = cfd.getObjectNodeMap().keySet();
		Set<String> variableNames = new LinkedHashSet<String>();
		for(PlainVariable currentVariable : cfd.getObjectNodeMap().keySet()) {
			if(currentVariable.getVariableName().equals(variableName)) {
				Set<PDGNode> currentNodes = cfd.getObjectNodeMap().get(currentVariable);
				for(PDGNode node : currentNodes) {
					Iterator<GraphEdge> iterator = node.getIncomingDependenceIterator();
					while(iterator.hasNext()) {
						PDGDependence dependence = (PDGDependence)iterator.next();
						if(dependence instanceof PDGDataDependence) {
							PDGDataDependence dataDependence = (PDGDataDependence)dependence;
							if(dataDependence.getData() instanceof PlainVariable) {
								PlainVariable plainVariable = (PlainVariable)dataDependence.getData();
								if(!plainVariable.equals(currentVariable) && allVariables.contains(plainVariable))
									variableNames.add(plainVariable.getVariableName());
							}
						}
					}
				}
			}
		}
		return variableNames;
	}

	private List<MappingState> matchBasedOnIdenticalStatements(MappingState parent, Set<PDGNode> nodesG1, Set<PDGNode> nodesG2,
			Set<PlainVariable> variables1, Set<PlainVariable> variables2) {
		IdenticalStatementDecomposer isd1 = new IdenticalStatementDecomposer(nodesG1);
		IdenticalStatementDecomposer isd2 = new IdenticalStatementDecomposer(nodesG2);
		Map<String, Set<PDGNode>> map1 = isd1.getIdenticalNodeMap();
		Map<String, Set<PDGNode>> map2 = isd2.getIdenticalNodeMap();
		MappingState finalState = parent;
		for(String variable1 : map1.keySet()) {
			Set<PDGNode> variableNodesG1 = map1.get(variable1);
			Set<PDGNode> tempNodesG1 = new LinkedHashSet<PDGNode>(variableNodesG1);
			Map<String, List<MappingState>> currentStateMap = new LinkedHashMap<String, List<MappingState>>();
			for(String variable2 : map2.keySet()) {
				String renamedVariable2 = variable2;
				if(variables1.size() == variables2.size()) {
					Iterator<PlainVariable> iterator1 = variables1.iterator();
					Iterator<PlainVariable> iterator2 = variables2.iterator();
					while(iterator1.hasNext()) {
						PlainVariable var1 = iterator1.next();
						PlainVariable var2 = iterator2.next();
						renamedVariable2 = renamedVariable2.replaceAll(var2.getVariableName(), var1.getVariableName());
					}
				}
				if(variable1.equals(variable2) || variable1.equals(renamedVariable2)) {
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
					List<MappingState> maxStates = processIdenticalPDGNodes(finalState, tempNodesG1, tempNodesG2);
					currentStateMap.put(variable2, maxStates);
				}
			}
			List<MappingState> currentStates = new ArrayList<MappingState>();
			for(String variable2 : currentStateMap.keySet()) {
				currentStates.addAll(currentStateMap.get(variable2));
			}
			if(!currentStates.isEmpty()) {
				MappingState best = findMaximumStateWithMinimumDifferences(currentStates);
				finalState = best;
			}
		}
		List<MappingState> currentStates = new ArrayList<MappingState>();
		if(finalState != null)
			currentStates.add(finalState);
		return currentStates;
	}

	private List<MappingState> matchBasedOnSwitchCases(MappingState parent, Set<PDGNode> nodesG1, Set<PDGNode> nodesG2) {
		SwitchBodyDecomposer sbd1 = new SwitchBodyDecomposer(nodesG1);
		SwitchBodyDecomposer sbd2 = new SwitchBodyDecomposer(nodesG2);
		Map<PDGNode, Set<PDGNode>> map1 = sbd1.getSwitchCaseNodeMap();
		Map<PDGNode, Set<PDGNode>> map2 = sbd2.getSwitchCaseNodeMap();
		if(map1.isEmpty() || map2.isEmpty()) {
			return processPDGNodes(parent, nodesG1, nodesG2);
		}
		else {
			MappingState finalState = parent;
			for(PDGNode switchCase1 : map1.keySet()) {
				Set<PDGNode> switchCaseNodesG1 = map1.get(switchCase1);
				Set<PDGNode> tempNodesG1 = new LinkedHashSet<PDGNode>();
				tempNodesG1.add(switchCase1);
				tempNodesG1.addAll(switchCaseNodesG1);
				Map<PDGNode, List<MappingState>> currentStateMap = new LinkedHashMap<PDGNode, List<MappingState>>();
				for(PDGNode switchCase2 : map2.keySet()) {
					Set<PDGNode> switchCaseNodesG2 = map2.get(switchCase2);
					Set<PDGNode> tempNodesG2 = new LinkedHashSet<PDGNode>();
					tempNodesG2.add(switchCase2);
					tempNodesG2.addAll(switchCaseNodesG2);
					if(finalState != null) {
						for(PDGNodeMapping mapping : finalState.getNodeMappings()) {
							if(tempNodesG1.contains(mapping.getNodeG1()))
								tempNodesG1.remove(mapping.getNodeG1());
							if(tempNodesG2.contains(mapping.getNodeG2()))
								tempNodesG2.remove(mapping.getNodeG2());
						}
					}
					List<MappingState> maxStates = processPDGNodes(finalState, tempNodesG1, tempNodesG2);
					currentStateMap.put(switchCase2, maxStates);
				}
				List<MappingState> currentStates = new ArrayList<MappingState>();
				for(PDGNode switchCase2 : currentStateMap.keySet()) {
					currentStates.addAll(currentStateMap.get(switchCase2));
				}
				if(!currentStates.isEmpty()) {
					MappingState best = findMaximumStateWithMinimumDifferences(currentStates);
					PDGNode switchCaseToRemove = null;
					for(PDGNode switchCase2 : currentStateMap.keySet()) {
						if(currentStateMap.get(switchCase2).contains(best)) {
							switchCaseToRemove = switchCase2;
							break;
						}
					}
					map2.remove(switchCaseToRemove);
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
				if(finalState != null)
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

	private boolean sameBasicBlock(MappingState parent, PDGNode nodeG1, PDGNode nodeG2) {
		if((nodeG1 instanceof PDGControlPredicateNode && nodeG2 instanceof PDGControlPredicateNode) ||
				(nodeG1 instanceof PDGBlockNode && nodeG2 instanceof PDGBlockNode)) {
			return true;
		}
		else if(nodeG1.getASTStatement().toString().equals(nodeG2.getASTStatement().toString())) {
			return true;
		}
		PDGNodeMapping previousControlMappingG1 = getPreviousControlMappingG1(parent, nodeG1);
		PDGNodeMapping previousControlMappingG2 = getPreviousControlMappingG2(parent, nodeG2);
		PDGNodeMapping nextControlMappingG1 = getNextControlMappingG1(parent, nodeG1);
		PDGNodeMapping nextControlMappingG2 = getNextControlMappingG2(parent, nodeG2);
		if(previousControlMappingG1 != null && nodeG1.isControlDependentOnNode(previousControlMappingG1.getNodeG1()) &&
				previousControlMappingG2 != null && nodeG2.isControlDependentOnNode(previousControlMappingG2.getNodeG2())) {
			return true;
		}
		if(symmetricalIfNodes(previousControlMappingG1, nextControlMappingG2) || symmetricalIfNodes(previousControlMappingG2, nextControlMappingG1)) {
			return true;
		}
		boolean samePreviousControlMapping = false;
		if(previousControlMappingG1 != null && previousControlMappingG2 != null) {
			samePreviousControlMapping = previousControlMappingG1.equals(previousControlMappingG2)/* || symmetrical*/;
		}
		else if(previousControlMappingG1 == null && previousControlMappingG2 == null) {
			samePreviousControlMapping = true;
		}
		boolean sameNextControlMapping = false;
		if(nextControlMappingG1 != null && nextControlMappingG2 != null) {
			sameNextControlMapping = nextControlMappingG1.equals(nextControlMappingG2)/* || symmetrical*/;
		}
		else if(nextControlMappingG1 == null && nextControlMappingG2 == null) {
			sameNextControlMapping = true;
		}
		return samePreviousControlMapping || sameNextControlMapping;
	}

	private boolean symmetricalIfNodes(PDGNodeMapping mapping1, PDGNodeMapping mapping2) {
		if(mapping1 != null && mapping2 != null) {
			PDGNode mapping1NodeG1ControlDependenceParent = mapping1.getNodeG1().getControlDependenceParent();
			PDGNode mapping1NodeG2ControlDependenceParent = mapping1.getNodeG2().getControlDependenceParent();
			PDGNode mapping2NodeG1ControlDependenceParent = mapping2.getNodeG1().getControlDependenceParent();
			PDGNode mapping2NodeG2ControlDependenceParent = mapping2.getNodeG2().getControlDependenceParent();
			while(!(mapping1NodeG1ControlDependenceParent instanceof PDGMethodEntryNode) &&
					!(mapping1NodeG2ControlDependenceParent instanceof PDGMethodEntryNode) &&
					!(mapping2NodeG1ControlDependenceParent instanceof PDGMethodEntryNode) &&
					!(mapping2NodeG2ControlDependenceParent instanceof PDGMethodEntryNode)) {
				if(mapping1NodeG1ControlDependenceParent.equals(mapping2NodeG1ControlDependenceParent) &&
						mapping1NodeG2ControlDependenceParent.equals(mapping2NodeG2ControlDependenceParent) &&
						mapping1NodeG1ControlDependenceParent.getCFGNode() instanceof CFGBranchIfNode &&
						mapping1NodeG2ControlDependenceParent.getCFGNode() instanceof CFGBranchIfNode) {
					return true;
				}
				mapping1NodeG1ControlDependenceParent = mapping1NodeG1ControlDependenceParent.getControlDependenceParent();
				mapping1NodeG2ControlDependenceParent = mapping1NodeG2ControlDependenceParent.getControlDependenceParent();
				mapping2NodeG1ControlDependenceParent = mapping2NodeG1ControlDependenceParent.getControlDependenceParent();
				mapping2NodeG2ControlDependenceParent = mapping2NodeG2ControlDependenceParent.getControlDependenceParent();
			}
		}
		return false;
	}

	private PDGNodeMapping getPreviousControlMappingG1(MappingState parent, PDGNode nodeG1) {
		PDGNodeMapping previousControlMapping = null;
		if(parent != null) {
			for(PDGNodeMapping mapping : parent.getSortedNodeMappings()) {
				if((mapping.getNodeG1() instanceof PDGControlPredicateNode || mapping.getNodeG2() instanceof PDGControlPredicateNode) && mapping.getNodeG1().getId() < nodeG1.getId()) {
					previousControlMapping = mapping;
				}
			}
		}
		return previousControlMapping;
	}

	private PDGNodeMapping getPreviousControlMappingG2(MappingState parent, PDGNode nodeG2) {
		PDGNodeMapping previousControlMapping = null;
		if(parent != null) {
			for(PDGNodeMapping mapping : parent.getSortedNodeMappings()) {
				if((mapping.getNodeG1() instanceof PDGControlPredicateNode || mapping.getNodeG2() instanceof PDGControlPredicateNode) && mapping.getNodeG2().getId() < nodeG2.getId()) {
					previousControlMapping = mapping;
				}
			}
		}
		return previousControlMapping;
	}

	private PDGNodeMapping getNextControlMappingG1(MappingState parent, PDGNode nodeG1) {
		if(parent != null) {
			for(PDGNodeMapping mapping : parent.getSortedNodeMappings()) {
				if((mapping.getNodeG1() instanceof PDGControlPredicateNode || mapping.getNodeG2() instanceof PDGControlPredicateNode) && mapping.getNodeG1().getId() > nodeG1.getId()) {
					return mapping;
				}
			}
		}
		return null;
	}

	private PDGNodeMapping getNextControlMappingG2(MappingState parent, PDGNode nodeG2) {
		if(parent != null) {
			for(PDGNodeMapping mapping : parent.getSortedNodeMappings()) {
				if((mapping.getNodeG1() instanceof PDGControlPredicateNode || mapping.getNodeG2() instanceof PDGControlPredicateNode) && mapping.getNodeG2().getId() > nodeG2.getId()) {
					return mapping;
				}
			}
		}
		return null;
	}

	private List<MappingState> processPDGNodes(MappingState parent, Set<PDGNode> nodesG1, Set<PDGNode> nodesG2) {
		MappingState.setRestrictedNodesG1(nodesG1);
		MappingState.setRestrictedNodesG2(nodesG2);
		List<MappingState> finalStates = null;
		if(nodesG1.size() < nodesG2.size()) {
			finalStates = processPDGNodesWithFirstCloneAsReference(parent, nodesG1, nodesG2);
		}
		else if(nodesG1.size() > nodesG2.size()) {
			finalStates = processPDGNodesWithSecondCloneAsReference(parent, nodesG1, nodesG2);
		}
		else {
			List<MappingState> finalStates1 = processPDGNodesWithFirstCloneAsReference(parent, nodesG1, nodesG2);
			List<MappingState> finalStates2 = processPDGNodesWithSecondCloneAsReference(parent, nodesG1, nodesG2);
			finalStates = new ArrayList<MappingState>(finalStates1);
			for(MappingState state : finalStates2) {
				if(!finalStates.contains(state)) {
					finalStates.add(state);
				}
			}
		}
		if(finalStates.isEmpty() && parent != null)
			finalStates.add(parent);
		return finalStates;
	}

	private List<MappingState> processPDGNodesWithFirstCloneAsReference(MappingState parent, Set<PDGNode> nodesG1, Set<PDGNode> nodesG2) {
		List<MappingState> finalStates = new ArrayList<MappingState>();
		for(PDGNode node1 : nodesG1) {
			List<MappingState> currentStates = new ArrayList<MappingState>();
			for(PDGNode node2 : nodesG2) {
				ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
				boolean match = astNodeMatcher.match(node1, node2);
				if(match && astNodeMatcher.isParameterizable() && sameBasicBlock(parent, node1, node2)) {
					PDGNodeMapping mapping = new PDGNodeMapping(node1, node2, astNodeMatcher);
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
							if(!previousState.containsAtLeastOneNodeInMappings(mapping) && previousState.mappedControlParents(node1, node2) &&
									!previousState.incomingDataDependenciesFromUnvisitedNodes(mapping) && !previousState.incomingDataDependenciesFromNonMatchingNodes(node1, node2)) {
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

	private List<MappingState> processPDGNodesWithSecondCloneAsReference(MappingState parent, Set<PDGNode> nodesG1, Set<PDGNode> nodesG2) {
		List<MappingState> finalStates = new ArrayList<MappingState>();
		for(PDGNode node2 : nodesG2) {
			List<MappingState> currentStates = new ArrayList<MappingState>();
			for(PDGNode node1 : nodesG1) {
				ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
				boolean match = astNodeMatcher.match(node1, node2);
				if(match && astNodeMatcher.isParameterizable() && sameBasicBlock(parent, node1, node2)) {
					PDGNodeMapping mapping = new PDGNodeMapping(node1, node2, astNodeMatcher);
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
							if(!previousState.containsAtLeastOneNodeInMappings(mapping) && previousState.mappedControlParents(node1, node2) &&
									!previousState.incomingDataDependenciesFromUnvisitedNodes(mapping) && !previousState.incomingDataDependenciesFromNonMatchingNodes(node1, node2)) {
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

	private List<MappingState> processIdenticalPDGNodes(MappingState parent, Set<PDGNode> nodesG1, Set<PDGNode> nodesG2) {
		MappingState finalState = parent;
		if(nodesG1.size() <= nodesG2.size()) {
			for(PDGNode node1 : nodesG1) {
				List<MappingState> currentStates = new ArrayList<MappingState>();
				for(PDGNode node2 : nodesG2) {
					ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
					boolean match = astNodeMatcher.match(node1, node2);
					if(match && astNodeMatcher.isParameterizable() && sameBasicBlock(parent, node1, node2)) {
						PDGNodeMapping mapping = new PDGNodeMapping(node1, node2, astNodeMatcher);
						if(finalState != null && finalState.containsAtLeastOneNodeInMappings(mapping)) {
							continue;
						}
						MappingState state = new MappingState(finalState, mapping);
						List<MappingState> maxStates = state.getMaximumCommonSubGraphs();
						for(MappingState temp : maxStates) {
							if(!currentStates.contains(temp)) {
								currentStates.add(temp);
							}
						}
					}
				}
				if(!currentStates.isEmpty())
					finalState = findMaximumStateWithMinimumDifferences(currentStates);
			}
		}
		else {
			for(PDGNode node2 : nodesG2) {
				List<MappingState> currentStates = new ArrayList<MappingState>();
				for(PDGNode node1 : nodesG1) {
					ASTNodeMatcher astNodeMatcher = new ASTNodeMatcher(iCompilationUnit1, iCompilationUnit2);
					boolean match = astNodeMatcher.match(node1, node2);
					if(match && astNodeMatcher.isParameterizable() && sameBasicBlock(parent, node1, node2)) {
						PDGNodeMapping mapping = new PDGNodeMapping(node1, node2, astNodeMatcher);
						if(finalState != null && finalState.containsAtLeastOneNodeInMappings(mapping)) {
							continue;
						}
						MappingState state = new MappingState(finalState, mapping);
						List<MappingState> maxStates = state.getMaximumCommonSubGraphs();
						for(MappingState temp : maxStates) {
							if(!currentStates.contains(temp)) {
								currentStates.add(temp);
							}
						}
					}
				}
				if(!currentStates.isEmpty())
					finalState = findMaximumStateWithMinimumDifferences(currentStates);
			}
		}
		List<MappingState> finalStates = new ArrayList<MappingState>();
		if(finalState != null)
			finalStates.add(finalState);
		return finalStates;
	}

	private List<MappingState> getMaximumStates(List<MappingState> currentStates) {
		int max = 0;
		List<MappingState> maximumStates = new ArrayList<MappingState>();
		for(MappingState currentState : currentStates) {
			if(currentState.getSize() > max) {
				max = currentState.getSize();
				clear(maximumStates, max);
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

	private void clear(List<MappingState> maximumStates, int max) {
		List<MappingState> keepStates = new ArrayList<MappingState>();
		for(MappingState state : maximumStates) {
			if(state.getSize() == max-1) {
				keepStates.add(state);
			}
		}
		maximumStates.clear();
		maximumStates.addAll(keepStates);
	}
}
