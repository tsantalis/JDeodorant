package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.ParameterObject;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class PDG extends Graph {
	private CFG cfg;
	private PDGMethodEntryNode entryNode;
	private Map<CFGBranchNode, Set<CFGNode>> nestingMap;
	private Set<VariableDeclaration> variableDeclarationsInMethod;
	private Set<VariableDeclaration> fieldsAccessedInMethod;
	private Map<PDGNode, Set<BasicBlock>> dominatedBlockMap;
	private IFile iFile;
	
	public PDG(CFG cfg, IFile iFile, Set<FieldObject> accessedFields) {
		this.cfg = cfg;
		this.iFile = iFile;
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
			performAliasAnalysis();
			createDataDependencies();
		}
		this.dominatedBlockMap = new LinkedHashMap<PDGNode, Set<BasicBlock>>();
		GraphNode.resetNodeNum();
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

	public Set<VariableDeclaration> getVariableDeclarationsAndAccessedFieldsInMethod() {
		Set<VariableDeclaration> variableDeclarations = new LinkedHashSet<VariableDeclaration>();
		variableDeclarations.addAll(variableDeclarationsInMethod);
		variableDeclarations.addAll(fieldsAccessedInMethod);
		return variableDeclarations;
	}

	public int getTotalNumberOfStatements() {
		return nodes.size();
	}

	public Map<CompositeVariable, LinkedHashSet<PDGNode>> getDefinedAttributesOfReference(PlainVariable reference) {
		Map<CompositeVariable, LinkedHashSet<PDGNode>> definedPropertiesMap = new LinkedHashMap<CompositeVariable, LinkedHashSet<PDGNode>>();
		for(GraphNode node : nodes) {
			PDGNode pdgNode = (PDGNode)node;
			for(AbstractVariable definedVariable : pdgNode.definedVariables) {
				if(definedVariable instanceof CompositeVariable) {
					CompositeVariable compositeVariable = (CompositeVariable)definedVariable;
					if(compositeVariable.getName().equals(reference.getName())) {
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

	private void createControlDependenciesFromEntryNode() {
		for(GraphNode node : cfg.nodes) {
			CFGNode cfgNode = (CFGNode)node;
			if(!isNested(cfgNode)) {
				processCFGNode(entryNode, cfgNode, true);
			}
		}
	}

	private void processCFGNode(PDGNode previousNode, CFGNode cfgNode, boolean controlType) {
		if(cfgNode instanceof CFGBranchNode) {
			PDGControlPredicateNode predicateNode = new PDGControlPredicateNode(cfgNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
			nodes.add(predicateNode);
			PDGControlDependence controlDependence = new PDGControlDependence(previousNode, predicateNode, controlType);
			edges.add(controlDependence);
			processControlPredicate(predicateNode);
		}
		else {
			PDGNode pdgNode = null;
			if(cfgNode instanceof CFGExitNode)
				pdgNode = new PDGExitNode(cfgNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
			else
				pdgNode = new PDGStatementNode(cfgNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
			nodes.add(pdgNode);
			PDGControlDependence controlDependence = new PDGControlDependence(previousNode, pdgNode, controlType);
			edges.add(controlDependence);
		}
	}

	private void processControlPredicate(PDGControlPredicateNode predicateNode) {
		CFGBranchNode branchNode = (CFGBranchNode)predicateNode.getCFGNode();
		if(branchNode instanceof CFGBranchConditionalNode) {
			CFGBranchConditionalNode conditionalNode = (CFGBranchConditionalNode)branchNode;
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
		aliasSearch(firstPDGNode, false, reachingAliasSet);
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
			}
			for(AbstractVariable variableInstruction : pdgNode.usedVariables) {
				antiDependenceSearch(pdgNode, variableInstruction, pdgNode, new LinkedHashSet<PDGNode>(), null);
			}
		}
	}

	private void aliasSearch(PDGNode currentNode, boolean visitedFromLoopbackFlow, ReachingAliasSet reachingAliasSet) {
		CFGNode currentCFGNode = currentNode.getCFGNode();
		for(GraphEdge edge : currentCFGNode.outgoingEdges) {
			Flow flow = (Flow)edge;
			if(!visitedFromLoopbackFlow || (visitedFromLoopbackFlow && flow.isFalseControlFlow())) {
				CFGNode dstCFGNode = (CFGNode)flow.dst;
				PDGNode dstPDGNode = dstCFGNode.getPDGNode();
				ReachingAliasSet reachingAliasSetCopy = reachingAliasSet.copy();
				dstPDGNode.applyReachingAliasSet(reachingAliasSetCopy);
				dstPDGNode.updateReachingAliasSet(reachingAliasSetCopy);
				if(flow.isLoopbackFlow())
					aliasSearch(dstPDGNode, true, reachingAliasSetCopy);
				else
					aliasSearch(dstPDGNode, false, reachingAliasSetCopy);
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
			if(!dstPDGNode.usesLocalVariable(variableInstruction)) {
				antiDependenceSearch(initialNode, variableInstruction, dstPDGNode, visitedNodes, loop);
			}
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
			List<CFGNode> blockNodes = reachableBlock.getAllNodes();
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

	public Set<PDGSlice> getProgramDependenceSlices(PDGNode nodeCriterion, AbstractVariable variableCriterion) {
		Set<PDGSlice> slices = new LinkedHashSet<PDGSlice>();
		Set<BasicBlock> boundaryBlocks = boundaryBlocks(nodeCriterion);
		for(BasicBlock boundaryBlock : boundaryBlocks) {
			PDGSlice slice = new PDGSlice(this, boundaryBlock, nodeCriterion, variableCriterion);
			slices.add(slice);
		}
		return slices;
	}

	public Set<PDGSlice> getProgramDependenceSlices(PDGNode nodeCriterion) {
		Set<PDGSlice> slices = new LinkedHashSet<PDGSlice>();
		Set<AbstractVariable> examinedVariables = new LinkedHashSet<AbstractVariable>();
		for(AbstractVariable definedVariable : nodeCriterion.definedVariables) {
			if(!examinedVariables.contains(definedVariable) && definedVariable.isLocalVariable()) {
				slices.addAll(getProgramDependenceSlices(nodeCriterion, definedVariable));
				examinedVariables.add(definedVariable);
			}
		}
		/*for(AbstractVariable usedVariable : nodeCriterion.usedVariables) {
			if(!examinedVariables.contains(usedVariable) && usedVariable.isLocalVariable()) {
				slices.addAll(getProgramDependenceSlices(nodeCriterion, usedVariable));
				examinedVariables.add(usedVariable);
			}
		}*/
		return slices;
	}

	public Set<PDGSlice> getAllProgramDependenceSlices() {
		Set<PDGSlice> slices = new LinkedHashSet<PDGSlice>();
		for(GraphNode node : nodes) {
			PDGNode pdgNode = (PDGNode)node;
			slices.addAll(getProgramDependenceSlices(pdgNode));
		}
		return slices;
	}
}
