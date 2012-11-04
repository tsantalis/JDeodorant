package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.ParameterObject;
import gr.uom.java.jdeodorant.preferences.PreferenceConstants;
import gr.uom.java.jdeodorant.refactoring.Activator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jface.preference.IPreferenceStore;

public class PDG extends Graph {
	private CFG cfg;
	private PDGMethodEntryNode entryNode;
	private Map<CFGBranchNode, Set<CFGNode>> nestingMap;
	private Set<VariableDeclaration> variableDeclarationsInMethod;
	private Set<VariableDeclaration> fieldsAccessedInMethod;
	private Map<PDGNode, Set<BasicBlock>> dominatedBlockMap;
	private IFile iFile;
	private IProgressMonitor monitor;
	
	public PDG(CFG cfg, IFile iFile, Set<FieldObject> accessedFields, IProgressMonitor monitor) {
		this.cfg = cfg;
		this.iFile = iFile;
		this.monitor = monitor;
		if(monitor != null)
			monitor.beginTask("Constructing program dependence graph", cfg.nodes.size());
		this.entryNode = new PDGMethodEntryNode(cfg.getMethod());
		this.nestingMap = new LinkedHashMap<CFGBranchNode, Set<CFGNode>>();
		for(GraphNode node : cfg.nodes) {
			CFGNode cfgNode = (CFGNode)node;
			if(cfgNode instanceof CFGBranchNode) {
				CFGBranchNode branchNode = (CFGBranchNode)cfgNode;
				nestingMap.put(branchNode, branchNode.getImmediatelyNestedNodesFromAST());
			}
		}
		this.variableDeclarationsInMethod = new LinkedHashSet<VariableDeclaration>();
		this.fieldsAccessedInMethod = new LinkedHashSet<VariableDeclaration>();
		for(FieldObject field : accessedFields) {
			this.fieldsAccessedInMethod.add(field.getVariableDeclaration());
		}
		ListIterator<ParameterObject> parameterIterator = cfg.getMethod().getParameterListIterator();
		while(parameterIterator.hasNext()) {
			ParameterObject parameter = parameterIterator.next();
			variableDeclarationsInMethod.add(parameter.getSingleVariableDeclaration());
		}
		for(LocalVariableDeclarationObject localVariableDeclaration : cfg.getMethod().getLocalVariableDeclarations()) {
			variableDeclarationsInMethod.add(localVariableDeclaration.getVariableDeclaration());
		}
		createControlDependenciesFromEntryNode();
		if(!nodes.isEmpty()) {
			IPreferenceStore store = Activator.getDefault().getPreferenceStore();
			boolean enabledAliasAnalysis = store.getBoolean(PreferenceConstants.P_ENABLE_ALIAS_ANALYSIS);
			if(enabledAliasAnalysis)
				performAliasAnalysis();
			createDataDependencies();
		}
		this.dominatedBlockMap = new LinkedHashMap<PDGNode, Set<BasicBlock>>();
		GraphNode.resetNodeNum();
		handleSwitchCaseNodes();
		handleJumpNodes();
		handleThrowExceptionNodes();
		if(monitor != null)
			monitor.done();
	}

	public PDGMethodEntryNode getEntryNode() {
		return entryNode;
	}

	public MethodObject getMethod() {
		return cfg.getMethod();
	}

	public IFile getIFile() {
		return iFile;
	}

	public Set<VariableDeclaration> getVariableDeclarationsInMethod() {
		return variableDeclarationsInMethod;
	}

	public Set<VariableDeclaration> getFieldsAccessedInMethod() {
		return fieldsAccessedInMethod;
	}

	public List<PDGNode> getNestedNodesWithinTryNode(PDGTryNode tryNode) {
		Map<CFGTryNode, List<CFGNode>> directlyNestedNodesInTryBlocks = cfg.getDirectlyNestedNodesInTryBlocks();
		List<CFGNode> directlyNestedCFGNodes = directlyNestedNodesInTryBlocks.get((CFGTryNode)tryNode.getCFGNode());
		List<PDGNode> directlyNestedPDGNodes = new ArrayList<PDGNode>();
		for(CFGNode cfgNode : directlyNestedCFGNodes) {
			directlyNestedPDGNodes.add(cfgNode.getPDGNode());
		}
		return directlyNestedPDGNodes;
	}

	public Set<VariableDeclaration> getVariableDeclarationsAndAccessedFieldsInMethod() {
		Set<VariableDeclaration> variableDeclarations = new LinkedHashSet<VariableDeclaration>();
		variableDeclarations.addAll(variableDeclarationsInMethod);
		variableDeclarations.addAll(fieldsAccessedInMethod);
		return variableDeclarations;
	}

	public Set<PlainVariable> getVariablesWithMethodBodyScope() {
		Set<PlainVariable> variables = new LinkedHashSet<PlainVariable>();
		for(AbstractVariable variable : entryNode.declaredVariables)
			variables.add((PlainVariable)variable);
		for(GraphNode node : nodes) {
			PDGNode pdgNode = (PDGNode)node;
			if(pdgNode.hasIncomingControlDependenceFromMethodEntryNode() && !(pdgNode instanceof PDGControlPredicateNode)) {
				for(AbstractVariable variable : pdgNode.declaredVariables)
					variables.add((PlainVariable)variable);
			}
		}
		return variables;
	}

	public int getTotalNumberOfStatements() {
		return nodes.size();
	}

	public Iterator<GraphNode> getNodeIterator() {
		return nodes.iterator();
	}

	public Map<CompositeVariable, LinkedHashSet<PDGNode>> getDefinedAttributesOfReference(PlainVariable reference) {
		Map<CompositeVariable, LinkedHashSet<PDGNode>> definedPropertiesMap = new LinkedHashMap<CompositeVariable, LinkedHashSet<PDGNode>>();
		for(GraphNode node : nodes) {
			PDGNode pdgNode = (PDGNode)node;
			for(AbstractVariable definedVariable : pdgNode.definedVariables) {
				if(definedVariable instanceof CompositeVariable) {
					CompositeVariable compositeVariable = (CompositeVariable)definedVariable;
					if(compositeVariable.getVariableBindingKey().equals(reference.getVariableBindingKey())) {
						if(definedPropertiesMap.containsKey(compositeVariable)) {
							LinkedHashSet<PDGNode> nodeCriteria = definedPropertiesMap.get(compositeVariable);
							nodeCriteria.add(pdgNode);
						}
						else {
							LinkedHashSet<PDGNode> nodeCriteria = new LinkedHashSet<PDGNode>();
							nodeCriteria.add(pdgNode);
							definedPropertiesMap.put(compositeVariable, nodeCriteria);
						}
					}
				}
			}
		}
		return definedPropertiesMap;
	}

	public Set<PDGNode> getAssignmentNodesOfVariableCriterion(AbstractVariable localVariableCriterion) {
		Set<PDGNode> nodeCriteria = new LinkedHashSet<PDGNode>();
		for(GraphNode node : nodes) {
			PDGNode pdgNode = (PDGNode)node;
			if(pdgNode.definesLocalVariable(localVariableCriterion) &&
					!pdgNode.declaresLocalVariable(localVariableCriterion))
				nodeCriteria.add(pdgNode);
		}
		return nodeCriteria;
	}

	public Set<PDGNode> getAssignmentNodesOfVariableCriterionIncludingDeclaration(AbstractVariable localVariableCriterion) {
		Set<PDGNode> nodeCriteria = new LinkedHashSet<PDGNode>();
		for(GraphNode node : nodes) {
			PDGNode pdgNode = (PDGNode)node;
			if(pdgNode.definesLocalVariable(localVariableCriterion))
				nodeCriteria.add(pdgNode);
		}
		return nodeCriteria;
	}

	private void handleThrowExceptionNodes() {
		for(GraphNode node : this.nodes) {
			PDGNode pdgNode = (PDGNode)node;
			CFGNode cfgNode = pdgNode.getCFGNode();
			if(cfgNode instanceof CFGThrowNode || pdgNode.throwsException()) {
				boolean matchingTryNode = false;
				Map<CFGTryNode, List<CFGNode>> directlyNestedNodesInTryBlocks = cfg.getDirectlyNestedNodesInTryBlocks();
				for(CFGTryNode tryNode : directlyNestedNodesInTryBlocks.keySet()) {
					List<CFGNode> directlyNestedNodes = directlyNestedNodesInTryBlocks.get(tryNode);
					for(CFGNode directlyNestedNode : directlyNestedNodes) {
						if(pdgNode.equals(directlyNestedNode.getPDGNode()) || isControlDependent(pdgNode, directlyNestedNode.getPDGNode())) {
							matchingTryNode = true;
							PDGControlDependence cd = new PDGControlDependence(tryNode.getPDGNode(), directlyNestedNode.getPDGNode(), true);
							edges.add(cd);
							break;
						}
					}
					if(matchingTryNode && cfgNode instanceof CFGThrowNode) {
						for(CFGNode directlyNestedNode : directlyNestedNodes) {
							if(directlyNestedNode.getPDGNode().getId() > pdgNode.getId()) {
								PDGControlDependence cd = new PDGControlDependence(pdgNode, directlyNestedNode.getPDGNode(), false);
								edges.add(cd);
							}
						}
						break;
					}
				}
			}
		}
	}

	private boolean isControlDependent(PDGNode node, PDGNode targetNode) {
		for(GraphEdge edge : node.incomingEdges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(dependence instanceof PDGControlDependence) {
				PDGControlDependence controlDependence = (PDGControlDependence)dependence;
				PDGNode srcPDGNode = (PDGNode)controlDependence.src;
				if(srcPDGNode.equals(targetNode))
					return true;
				return isControlDependent(srcPDGNode, targetNode);
			}
		}
		return false;
	}

	private void handleSwitchCaseNodes() {
		Map<PDGNode, Set<PDGNode>> switchCaseMap = new LinkedHashMap<PDGNode, Set<PDGNode>>();
		Stack<PDGNode> switchNodeStack = new Stack<PDGNode>();
		for(GraphNode node : this.nodes) {
			PDGNode pdgNode = (PDGNode)node;
			CFGNode cfgNode = pdgNode.getCFGNode();
			if(!switchNodeStack.isEmpty()) {
				PDGNode pdgSwitchNode = switchNodeStack.peek();
				CFGBranchSwitchNode cfgSwitchNode = (CFGBranchSwitchNode)pdgSwitchNode.getCFGNode();
				if(cfgSwitchNode.getJoinNode() != null && cfgSwitchNode.getJoinNode().getId() == cfgNode.getId())
					switchNodeStack.pop();
			}
			if(!switchNodeStack.isEmpty()) {
				PDGNode currentSwitchNode = switchNodeStack.peek();
				if(currentSwitchNode != null && isDirectlyDependentOnSwitchNode(pdgNode, currentSwitchNode)) {
					if(cfgNode instanceof CFGSwitchCaseNode) {
						if(switchCaseMap.containsKey(currentSwitchNode)) {
							switchCaseMap.get(currentSwitchNode).add(pdgNode);
						}
						else {
							Set<PDGNode> switchCaseSet = new LinkedHashSet<PDGNode>();
							switchCaseSet.add(pdgNode);
							switchCaseMap.put(currentSwitchNode, switchCaseSet);
						}
					}
					else if(cfgNode instanceof CFGBreakNode) {
						if(switchCaseMap.containsKey(currentSwitchNode)) {
							Set<PDGNode> switchCaseSet = switchCaseMap.get(currentSwitchNode);
							for(PDGNode switchCase : switchCaseSet) {
								PDGControlDependence cd = new PDGControlDependence(pdgNode, switchCase, false);
								edges.add(cd);
							}
							switchCaseMap.get(currentSwitchNode).clear();
						}
					}
					else {
						if(switchCaseMap.containsKey(currentSwitchNode)) {
							Set<PDGNode> switchCaseSet = switchCaseMap.get(currentSwitchNode);
							for(PDGNode switchCase : switchCaseSet) {
								PDGControlDependence cd = new PDGControlDependence(switchCase, pdgNode, true);
								edges.add(cd);
							}
						}
					}
				}
			}
			if(cfgNode instanceof CFGBranchSwitchNode) {
				switchNodeStack.push(pdgNode);
			}
		}
	}

	private boolean isDirectlyDependentOnSwitchNode(PDGNode node, PDGNode switchNode) {
		for(GraphEdge edge : node.incomingEdges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(dependence instanceof PDGControlDependence) {
				PDGControlDependence controlDependence = (PDGControlDependence)dependence;
				PDGNode srcPDGNode = (PDGNode)controlDependence.src;
				CFGNode srcCFGNode = srcPDGNode.getCFGNode();
				if(srcCFGNode instanceof CFGBranchSwitchNode && srcPDGNode.equals(switchNode))
					return true;
			}
		}
		return false;
	}

	private void handleJumpNodes() {
		//key is the jump node and value is the innermost loop node
		Map<PDGNode, PDGNode> jumpNodeMap = getInnerMostLoopNodesForJumpNodes();
		for(PDGNode jumpNode : jumpNodeMap.keySet()) {
			PDGNode innerMostLoopNode = jumpNodeMap.get(jumpNode);
			CFGNode innerMostLoopCFGNode = innerMostLoopNode.getCFGNode();
			if(innerMostLoopCFGNode instanceof CFGBranchLoopNode || innerMostLoopCFGNode instanceof CFGBranchDoLoopNode) {
				for(GraphEdge edge : innerMostLoopNode.outgoingEdges) {
					PDGDependence dependence = (PDGDependence)edge;
					if(dependence instanceof PDGControlDependence) {
						PDGControlDependence controlDependence = (PDGControlDependence)dependence;
						PDGNode dstPDGNode = (PDGNode)controlDependence.dst;
						if(dstPDGNode.getId() > jumpNode.getId()) {
							PDGControlDependence cd = new PDGControlDependence(jumpNode, dstPDGNode, false);
							edges.add(cd);
						}
					}
				}
				PDGControlDependence cd = new PDGControlDependence(jumpNode, innerMostLoopNode, false);
				edges.add(cd);
				CFGNode jumpCFGNode = jumpNode.getCFGNode();
				if(jumpCFGNode instanceof CFGBreakNode) {
					CFGBreakNode breakNode = (CFGBreakNode)jumpCFGNode;
					breakNode.setInnerMostLoopNode(innerMostLoopCFGNode);
				}
				else if(jumpCFGNode instanceof CFGContinueNode) {
					CFGContinueNode continueNode = (CFGContinueNode)jumpCFGNode;
					continueNode.setInnerMostLoopNode(innerMostLoopCFGNode);
				}
			}
		}
	}

	private Map<PDGNode, PDGNode> getInnerMostLoopNodesForJumpNodes() {
		Map<PDGNode, PDGNode> map = new LinkedHashMap<PDGNode, PDGNode>();
		for(GraphNode node : this.nodes) {
			PDGNode pdgNode = (PDGNode)node;
			CFGNode cfgNode = pdgNode.getCFGNode();
			boolean unlabeledJump = false;
			boolean isBreak = false;
			if(cfgNode instanceof CFGBreakNode) {
				CFGBreakNode breakNode = (CFGBreakNode)cfgNode;
				isBreak = true;
				if(!breakNode.isLabeled())
					unlabeledJump = true;
			}
			else if(cfgNode instanceof CFGContinueNode) {
				CFGContinueNode continueNode = (CFGContinueNode)cfgNode;
				isBreak = false;
				if(!continueNode.isLabeled())
					unlabeledJump = true;
			}
			if(unlabeledJump) {
				map.put(pdgNode, getInnerMostLoopNode(pdgNode, isBreak));
			}
		}
		return map;
	}

	private PDGNode getInnerMostLoopNode(PDGNode node, boolean isBreak) {
		for(GraphEdge edge : node.incomingEdges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(dependence instanceof PDGControlDependence) {
				PDGControlDependence controlDependence = (PDGControlDependence)dependence;
				PDGNode srcPDGNode = (PDGNode)controlDependence.src;
				CFGNode srcCFGNode = srcPDGNode.getCFGNode();
				if(isBreak && (srcCFGNode instanceof CFGBranchLoopNode || srcCFGNode instanceof CFGBranchDoLoopNode || srcCFGNode instanceof CFGBranchSwitchNode))
					return srcPDGNode;
				if(!isBreak && (srcCFGNode instanceof CFGBranchLoopNode || srcCFGNode instanceof CFGBranchDoLoopNode))
					return srcPDGNode;
				return getInnerMostLoopNode(srcPDGNode, isBreak);
			}
		}
		return null;
	}

	private void createControlDependenciesFromEntryNode() {
		for(GraphNode node : cfg.nodes) {
			CFGNode cfgNode = (CFGNode)node;
			if(!isNested(cfgNode)) {
				processCFGNode(entryNode, cfgNode, true);
			}
		}
		Map<CFGTryNode, List<CFGNode>> directlyNestedNodesInTryBlocks = cfg.getDirectlyNestedNodesInTryBlocks();
		for(CFGTryNode tryNode : directlyNestedNodesInTryBlocks.keySet()) {
			if(!tryNode.hasResources())
				processCFGNode(entryNode, tryNode, true);
		}
	}

	private void processCFGNode(PDGNode previousNode, CFGNode cfgNode, boolean controlType) {
		if(cfgNode instanceof CFGBranchNode) {
			PDGControlPredicateNode predicateNode = new PDGControlPredicateNode(cfgNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
			nodes.add(predicateNode);
			if(monitor != null)
				monitor.worked(1);
			PDGControlDependence controlDependence = new PDGControlDependence(previousNode, predicateNode, controlType);
			edges.add(controlDependence);
			processControlPredicate(predicateNode);
		}
		else {
			PDGNode pdgNode = null;
			if(cfgNode instanceof CFGExitNode)
				pdgNode = new PDGExitNode(cfgNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
			else if(cfgNode instanceof CFGTryNode)
				pdgNode = new PDGTryNode((CFGTryNode)cfgNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
			else
				pdgNode = new PDGStatementNode(cfgNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
			nodes.add(pdgNode);
			if(monitor != null)
				monitor.worked(1);
			PDGControlDependence controlDependence = new PDGControlDependence(previousNode, pdgNode, controlType);
			edges.add(controlDependence);
		}
	}

	private void processControlPredicate(PDGControlPredicateNode predicateNode) {
		CFGBranchNode branchNode = (CFGBranchNode)predicateNode.getCFGNode();
		if(branchNode instanceof CFGBranchIfNode) {
			CFGBranchIfNode conditionalNode = (CFGBranchIfNode)branchNode;
			Set<CFGNode> nestedNodesInTrueControlFlow = conditionalNode.getImmediatelyNestedNodesInTrueControlFlow();
			for(CFGNode nestedNode : nestedNodesInTrueControlFlow) {
				processCFGNode(predicateNode, nestedNode, true);
			}
			Set<CFGNode> nestedNodesInFalseControlFlow = conditionalNode.getImmediatelyNestedNodesInFalseControlFlow();
			for(CFGNode nestedNode : nestedNodesInFalseControlFlow) {
				processCFGNode(predicateNode, nestedNode, false);
			}
		}
		else {
			Set<CFGNode> nestedNodes = nestingMap.get(branchNode);
			for(CFGNode nestedNode : nestedNodes) {
				processCFGNode(predicateNode, nestedNode, true);
			}
		}
	}

	private boolean isNested(CFGNode node) {
		for(CFGBranchNode key : nestingMap.keySet()) {
			Set<CFGNode> nestedNodes = nestingMap.get(key);
			if(nestedNodes.contains(node))
				return true;
		}
		return false;
	}

	private void performAliasAnalysis() {
		PDGNode firstPDGNode = (PDGNode)nodes.toArray()[0];
		ReachingAliasSet reachingAliasSet = new ReachingAliasSet();
		firstPDGNode.updateReachingAliasSet(reachingAliasSet);
		aliasSearch(firstPDGNode, new LinkedHashSet<PDGNode>(), false, reachingAliasSet);
	}

	private void createDataDependencies() {
		PDGNode firstPDGNode = (PDGNode)nodes.toArray()[0];
		for(AbstractVariable variableInstruction : entryNode.definedVariables) {
			if(firstPDGNode.usesLocalVariable(variableInstruction)) {
				PDGDataDependence dataDependence = new PDGDataDependence(entryNode, firstPDGNode, variableInstruction, null);
				edges.add(dataDependence);
			}
			if(!firstPDGNode.definesLocalVariable(variableInstruction)) {
				dataDependenceSearch(entryNode, variableInstruction, firstPDGNode, new LinkedHashSet<PDGNode>(), null);
			}
			else if(entryNode.declaresLocalVariable(variableInstruction)) {
				//create def-order data dependence edge
				PDGDataDependence dataDependence = new PDGDataDependence(entryNode, firstPDGNode, variableInstruction, null);
				edges.add(dataDependence);
			}
		}
		for(GraphNode node : nodes) {
			PDGNode pdgNode = (PDGNode)node;
			for(AbstractVariable variableInstruction : pdgNode.definedVariables) {
				dataDependenceSearch(pdgNode, variableInstruction, pdgNode, new LinkedHashSet<PDGNode>(), null);
				outputDependenceSearch(pdgNode, variableInstruction, pdgNode, new LinkedHashSet<PDGNode>(), null);
			}
			for(AbstractVariable variableInstruction : pdgNode.usedVariables) {
				antiDependenceSearch(pdgNode, variableInstruction, pdgNode, new LinkedHashSet<PDGNode>(), null);
			}
		}
	}

	private void aliasSearch(PDGNode currentNode, Set<PDGNode> visitedNodes, boolean visitedFromLoopbackFlow, ReachingAliasSet reachingAliasSet) {
		if(visitedNodes.contains(currentNode))
			return;
		else
			visitedNodes.add(currentNode);
		CFGNode currentCFGNode = currentNode.getCFGNode();
		for(GraphEdge edge : currentCFGNode.outgoingEdges) {
			Flow flow = (Flow)edge;
			if(!visitedFromLoopbackFlow || (visitedFromLoopbackFlow && flow.isFalseControlFlow())) {
				CFGNode srcCFGNode = (CFGNode)flow.src;
				CFGNode dstCFGNode = (CFGNode)flow.dst;
				PDGNode dstPDGNode = dstCFGNode.getPDGNode();
				ReachingAliasSet reachingAliasSetCopy = reachingAliasSet.copy();
				dstPDGNode.applyReachingAliasSet(reachingAliasSetCopy);
				dstPDGNode.updateReachingAliasSet(reachingAliasSetCopy);
				if(!(srcCFGNode instanceof CFGBranchDoLoopNode && flow.isTrueControlFlow())) {
					if(flow.isLoopbackFlow())
						aliasSearch(dstPDGNode, visitedNodes, true, reachingAliasSetCopy);
					else
						aliasSearch(dstPDGNode, visitedNodes, false, reachingAliasSetCopy);
				}
			}
		}
	}

	private void dataDependenceSearch(PDGNode initialNode, AbstractVariable variableInstruction,
			PDGNode currentNode, Set<PDGNode> visitedNodes, CFGBranchNode loop) {
		if(visitedNodes.contains(currentNode))
			return;
		else
			visitedNodes.add(currentNode);
		CFGNode currentCFGNode = currentNode.getCFGNode();
		for(GraphEdge edge : currentCFGNode.outgoingEdges) {
			Flow flow = (Flow)edge;
			CFGNode srcCFGNode = (CFGNode)flow.src;
			CFGNode dstCFGNode = (CFGNode)flow.dst;
			if(flow.isLoopbackFlow()) {
				if(dstCFGNode instanceof CFGBranchLoopNode)
					loop = (CFGBranchLoopNode)dstCFGNode;
				if(srcCFGNode instanceof CFGBranchDoLoopNode)
					loop = (CFGBranchDoLoopNode)srcCFGNode;
			}
			PDGNode dstPDGNode = dstCFGNode.getPDGNode();
			if(dstPDGNode.usesLocalVariable(variableInstruction)) {
				PDGDataDependence dataDependence = new PDGDataDependence(initialNode, dstPDGNode, variableInstruction, loop);
				edges.add(dataDependence);
			}
			if(!dstPDGNode.definesLocalVariable(variableInstruction)) {
				dataDependenceSearch(initialNode, variableInstruction, dstPDGNode, visitedNodes, loop);
			}
			else if(initialNode.declaresLocalVariable(variableInstruction) && !initialNode.equals(dstPDGNode)) {
				//create def-order data dependence edge
				PDGDataDependence dataDependence = new PDGDataDependence(initialNode, dstPDGNode, variableInstruction, loop);
				edges.add(dataDependence);
			}
		}
	}

	private void antiDependenceSearch(PDGNode initialNode, AbstractVariable variableInstruction,
			PDGNode currentNode, Set<PDGNode> visitedNodes, CFGBranchNode loop) {
		if(visitedNodes.contains(currentNode))
			return;
		else
			visitedNodes.add(currentNode);
		CFGNode currentCFGNode = currentNode.getCFGNode();
		for(GraphEdge edge : currentCFGNode.outgoingEdges) {
			Flow flow = (Flow)edge;
			CFGNode srcCFGNode = (CFGNode)flow.src;
			CFGNode dstCFGNode = (CFGNode)flow.dst;
			if(flow.isLoopbackFlow()) {
				if(dstCFGNode instanceof CFGBranchLoopNode)
					loop = (CFGBranchLoopNode)dstCFGNode;
				if(srcCFGNode instanceof CFGBranchDoLoopNode)
					loop = (CFGBranchDoLoopNode)srcCFGNode;
			}
			PDGNode dstPDGNode = dstCFGNode.getPDGNode();
			if(dstPDGNode.definesLocalVariable(variableInstruction)) {
				PDGAntiDependence antiDependence = new PDGAntiDependence(initialNode, dstPDGNode, variableInstruction, loop);
				edges.add(antiDependence);
			}
			else
				antiDependenceSearch(initialNode, variableInstruction, dstPDGNode, visitedNodes, loop);
		}
	}

	private void outputDependenceSearch(PDGNode initialNode, AbstractVariable variableInstruction,
			PDGNode currentNode, Set<PDGNode> visitedNodes, CFGBranchNode loop) {
		if(visitedNodes.contains(currentNode))
			return;
		else
			visitedNodes.add(currentNode);
		CFGNode currentCFGNode = currentNode.getCFGNode();
		for(GraphEdge edge : currentCFGNode.outgoingEdges) {
			Flow flow = (Flow)edge;
			CFGNode srcCFGNode = (CFGNode)flow.src;
			CFGNode dstCFGNode = (CFGNode)flow.dst;
			if(flow.isLoopbackFlow()) {
				if(dstCFGNode instanceof CFGBranchLoopNode)
					loop = (CFGBranchLoopNode)dstCFGNode;
				if(srcCFGNode instanceof CFGBranchDoLoopNode)
					loop = (CFGBranchDoLoopNode)srcCFGNode;
			}
			PDGNode dstPDGNode = dstCFGNode.getPDGNode();
			if(dstPDGNode.definesLocalVariable(variableInstruction)) {
				PDGOutputDependence outputDependence = new PDGOutputDependence(initialNode, dstPDGNode, variableInstruction, loop);
				edges.add(outputDependence);
			}
			else
				outputDependenceSearch(initialNode, variableInstruction, dstPDGNode, visitedNodes, loop);
		}
	}

	public List<BasicBlock> getBasicBlocks() {
		return cfg.getBasicBlocks();
	}

	private Set<BasicBlock> forwardReachableBlocks(BasicBlock basicBlock) {
		return cfg.getBasicBlockCFG().forwardReachableBlocks(basicBlock);
	}

	//returns the node (branch or method entry) that directly dominates the leader of the block
	private PDGNode directlyDominates(BasicBlock block) {
		CFGNode leaderCFGNode = block.getLeader();
		PDGNode leaderPDGNode = leaderCFGNode.getPDGNode();
		for(GraphEdge edge : leaderPDGNode.incomingEdges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(dependence instanceof PDGControlDependence) {
				PDGNode srcNode = (PDGNode)dependence.src;
				return srcNode;
			}
		}
		return null;
	}

	private Set<BasicBlock> dominatedBlocks(BasicBlock block) {
		PDGNode pdgNode = directlyDominates(block);
		if(dominatedBlockMap.containsKey(pdgNode)) {
			return dominatedBlockMap.get(pdgNode);
		}
		else {
			Set<BasicBlock> dominatedBlocks = dominatedBlocks(pdgNode);
			dominatedBlockMap.put(pdgNode, dominatedBlocks);
			return dominatedBlocks;
		}
	}
	
	private Set<BasicBlock> dominatedBlocks(PDGNode branchNode) {
		Set<BasicBlock> dominatedBlocks = new LinkedHashSet<BasicBlock>();
		for(GraphEdge edge : branchNode.outgoingEdges) {
			PDGDependence dependence = (PDGDependence)edge;
			if(dependence instanceof PDGControlDependence) {
				PDGNode dstNode = (PDGNode)dependence.dst;
				BasicBlock dstBlock = dstNode.getBasicBlock();
				dominatedBlocks.add(dstBlock);
				PDGNode dstBlockLastNode = dstBlock.getLastNode().getPDGNode();
				if(dstBlockLastNode instanceof PDGControlPredicateNode && !dstBlockLastNode.equals(branchNode))
					dominatedBlocks.addAll(dominatedBlocks(dstBlockLastNode));
			}
		}
		return dominatedBlocks;
	}

	public Set<BasicBlock> boundaryBlocks(PDGNode node) {
		Set<BasicBlock> boundaryBlocks = new LinkedHashSet<BasicBlock>();
		BasicBlock srcBlock = node.getBasicBlock();
		for(BasicBlock block : getBasicBlocks()) {
			Set<BasicBlock> forwardReachableBlocks = forwardReachableBlocks(block);
			Set<BasicBlock> dominatedBlocks = dominatedBlocks(block);
			Set<BasicBlock> intersection = new LinkedHashSet<BasicBlock>();
			intersection.addAll(forwardReachableBlocks);
			intersection.retainAll(dominatedBlocks);
			if(intersection.contains(srcBlock))
				boundaryBlocks.add(block);
		}
		return boundaryBlocks;
	}

	public Set<PDGNode> blockBasedRegion(BasicBlock block) {
		Set<PDGNode> regionNodes = new LinkedHashSet<PDGNode>();
		Set<BasicBlock> reachableBlocks = forwardReachableBlocks(block);
		for(BasicBlock reachableBlock : reachableBlocks) {
			List<CFGNode> blockNodes = reachableBlock.getAllNodesIncludingTry();
			for(CFGNode cfgNode : blockNodes) {
				regionNodes.add(cfgNode.getPDGNode());
			}
		}
		return regionNodes;
	}

	public Set<AbstractVariable> getReturnedVariables() {
		Set<AbstractVariable> returnedVariables = new LinkedHashSet<AbstractVariable>();
		for(GraphNode node : nodes) {
			PDGNode pdgNode = (PDGNode)node;
			if(pdgNode instanceof PDGExitNode) {
				PDGExitNode exitNode = (PDGExitNode)pdgNode;
				AbstractVariable returnedVariable = exitNode.getReturnedVariable();
				if(returnedVariable != null)
					returnedVariables.add(returnedVariable);
			}
		}
		return returnedVariables;
	}

	public PDGNode getFirstDef(PlainVariable variable) {
		for(GraphNode node : nodes) {
			PDGNode pdgNode = (PDGNode)node;
			if(pdgNode.definesLocalVariable(variable))
				return pdgNode;
		}
		return null;
	}

	public PDGNode getLastUse(PlainVariable variable) {
		List<GraphNode> reversedNodeList = new ArrayList<GraphNode>(nodes);
		Collections.reverse(reversedNodeList);
		for(GraphNode node : reversedNodeList) {
			PDGNode pdgNode = (PDGNode)node;
			if(pdgNode.usesLocalVariable(variable))
				return pdgNode;
		}
		return null;
	}
}
