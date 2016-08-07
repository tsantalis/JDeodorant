package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.CompositeStatementObject;
import gr.uom.java.ast.decomposition.StatementObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CFGBreakNode;
import gr.uom.java.ast.decomposition.cfg.CFGContinueNode;
import gr.uom.java.ast.decomposition.cfg.CFGNode;
import gr.uom.java.ast.decomposition.cfg.GraphEdge;
import gr.uom.java.ast.decomposition.cfg.PDGBlockNode;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGControlPredicateNode;
import gr.uom.java.ast.decomposition.cfg.PDGDataDependence;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PDGStatementNode;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.ThrownExceptionVisitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;

public class PDGNodeBlockGap extends Gap {
	private CloneStructureNode parent;
	private TreeSet<PDGNode> nodesG1;
	private TreeSet<PDGNode> nodesG2;
	private List<ASTNodeDifference> nodeDifferences;
	private VariableBindingPair returnedVariableBinding;
	
	public PDGNodeBlockGap(CloneStructureNode parent) {
		this.parent = parent;
		this.nodesG1 = new TreeSet<PDGNode>();
		this.nodesG2 = new TreeSet<PDGNode>();
		this.nodeDifferences = new ArrayList<ASTNodeDifference>();
	}

	public CloneStructureNode getParent() {
		return parent;
	}

	public TreeSet<PDGNode> getNodesG1() {
		return nodesG1;
	}

	public TreeSet<PDGNode> getNodesG2() {
		return nodesG2;
	}

	public VariableBindingPair getReturnedVariableBinding() {
		return returnedVariableBinding;
	}

	public void setReturnedVariableBinding(VariableBindingPair returnedVariableBinding) {
		this.returnedVariableBinding = returnedVariableBinding;
	}

	public List<ASTNodeDifference> getNodeDifferences() {
		return nodeDifferences;
	}

	public PDGNode getLastNodeG1() {
		PDGNode last = nodesG1.last();
		return findLastNodeG1(last);
	}

	private PDGNode findLastNodeG1(PDGNode last) {
		PDGNode lastControlParent = last.getControlDependenceParent();
		if(nodesG1.contains(lastControlParent)) {
			return findLastNodeG1(lastControlParent);
		}
		CloneStructureNode lastNode = parent.findNodeG1(last);
		NodeMapping lastCloneStructureParentMapping = lastNode.getParent().getMapping();
		if(lastCloneStructureParentMapping != null) {
			PDGNode lastCloneStructureParent = lastCloneStructureParentMapping.getNodeG1();
			if(lastCloneStructureParent != null && nodesG1.contains(lastCloneStructureParent)) {
				return findLastNodeG1(lastCloneStructureParent);
			}
		}
		return last;
	}

	public PDGNode getLastNodeG2() {
		PDGNode last = nodesG2.last();
		return findLastNodeG2(last);
	}

	private PDGNode findLastNodeG2(PDGNode last) {
		PDGNode lastControlParent = last.getControlDependenceParent();
		if(nodesG2.contains(lastControlParent)) {
			return findLastNodeG2(lastControlParent);
		}
		CloneStructureNode lastNode = parent.findNodeG2(last);
		NodeMapping lastCloneStructureParentMapping = lastNode.getParent().getMapping();
		if(lastCloneStructureParentMapping != null) {
			PDGNode lastCloneStructureParent = lastCloneStructureParentMapping.getNodeG2();
			if(lastCloneStructureParent != null && nodesG2.contains(lastCloneStructureParent)) {
				return findLastNodeG2(lastCloneStructureParent);
			}
		}
		return last;
	}

	public void add(PDGNodeGap nodeGap) {
		if(!nodeGap.isAdvancedMatch()) {
			if(nodeGap.getNodeG1() != null && nodeGap.getNodeG2() == null) {
				nodesG1.add(nodeGap.getNodeG1());
			}
			else if(nodeGap.getNodeG1() == null && nodeGap.getNodeG2() != null) {
				nodesG2.add(nodeGap.getNodeG2());
			}
		}
	}

	public void add(PDGNodeMapping nodeMapping) {
		nodesG1.add(nodeMapping.getNodeG1());
		nodesG2.add(nodeMapping.getNodeG2());
	}

	public boolean isBackwardsExpandable() {
		//find the previous nodeMapping with precondition violations and add it
		PDGNode firstNodeG1 = nodesG1.isEmpty() ? null : nodesG1.first();
		PDGNode firstNodeG2 = nodesG2.isEmpty() ? null : nodesG2.first();
		List<PDGNodeMapping> mappingsBeforeFirst = new ArrayList<PDGNodeMapping>();
		for(CloneStructureNode child : parent.getChildren()) {
			if(child.getMapping() instanceof PDGNodeGap) {
				PDGNodeGap nodeGap = (PDGNodeGap)child.getMapping();
				if(!nodeGap.isAdvancedMatch()) {
					PDGNode nodeG1 = nodeGap.getNodeG1();
					PDGNode nodeG2 = nodeGap.getNodeG2();
					if(nodeG1 != null && nodeG2 == null) {
						if(nodeG1.equals(firstNodeG1))
							break;
					}
					else if(nodeG1 == null && nodeG2 != null) {
						if(nodeG2.equals(firstNodeG2))
							break;
					}
				}
			}
			else if(child.getMapping() instanceof PDGNodeMapping) {
				PDGNodeMapping nodeMapping = (PDGNodeMapping)child.getMapping();
				PDGNode nodeG1 = nodeMapping.getNodeG1();
				PDGNode nodeG2 = nodeMapping.getNodeG2();
				if(nodeG1.equals(firstNodeG1))
					break;
				if(nodeG2.equals(firstNodeG2))
					break;
				mappingsBeforeFirst.add(nodeMapping);
			}
		}
		if(!mappingsBeforeFirst.isEmpty()) {
			PDGNodeMapping last = mappingsBeforeFirst.get(mappingsBeforeFirst.size()-1);
			nodeDifferences.addAll(last.getNodeDifferences());
			nodesG1.add(last.getNodeG1());
			nodesG2.add(last.getNodeG2());
			return true;
		}
		return false;
	}

	public boolean isForwardsExpandable() {
		//find the next nodeMapping with precondition violations and add it
		boolean lastNodeG1Found = false;
		boolean lastNodeG2Found = false;
		PDGNode lastNodeG1 = nodesG1.isEmpty() ? null : nodesG1.last();
		PDGNode lastNodeG2 = nodesG2.isEmpty() ? null : nodesG2.last();
		for(CloneStructureNode child : parent.getChildren()) {
			if(child.getMapping() instanceof PDGNodeGap) {
				PDGNodeGap nodeGap = (PDGNodeGap)child.getMapping();
				if(!nodeGap.isAdvancedMatch()) {
					PDGNode nodeG1 = nodeGap.getNodeG1();
					PDGNode nodeG2 = nodeGap.getNodeG2();
					if(nodeG1 != null && nodeG2 == null) {
						if(nodeG1.equals(lastNodeG1))
							lastNodeG1Found = true;
					}
					else if(nodeG1 == null && nodeG2 != null) {
						if(nodeG2.equals(lastNodeG2))
							lastNodeG2Found = true;
					}
				}
			}
			else if(child.getMapping() instanceof PDGNodeMapping) {
				PDGNodeMapping nodeMapping = (PDGNodeMapping)child.getMapping();
				PDGNode nodeG1 = nodeMapping.getNodeG1();
				PDGNode nodeG2 = nodeMapping.getNodeG2();
				if(lastNodeG1Found && lastNodeG2Found && !nodeMapping.getPreconditionViolations().isEmpty() && !nodesG1.contains(nodeG1) && !nodesG2.contains(nodeG2)) {
					nodeDifferences.addAll(nodeMapping.getNodeDifferences());
					nodesG1.add(nodeG1);
					nodesG2.add(nodeG2);
					return true;
				}
				if(nodeG1.equals(lastNodeG1))
					lastNodeG1Found = true;
				if(nodeG2.equals(lastNodeG2))
					lastNodeG2Found = true;
			}
		}
		return false;
	}

	public boolean isEmpty() {
		return nodesG1.isEmpty() && nodesG2.isEmpty();
	}

	public Set<IVariableBinding> getUsedVariableBindingsG1() {
		return getUsedVariableBindings(nodesG1);
	}

	public Set<IVariableBinding> getUsedVariableBindingsG2() {
		return getUsedVariableBindings(nodesG2);
	}

	public boolean variableIsDefinedButNotUsedInBlockGap(VariableBindingPair pair) {
		boolean variable1IsDefinedButNotUsed = variableDefinedInNodes(nodesG1, pair.getBinding1()) && !variableUsedInNodes(nodesG1, pair.getBinding1());
		boolean variable2IsDefinedButNotUsed = variableDefinedInNodes(nodesG2, pair.getBinding2()) && !variableUsedInNodes(nodesG2, pair.getBinding2());
		return variable1IsDefinedButNotUsed && variable2IsDefinedButNotUsed;
	}

	public boolean variableIsDefinedAndUsedInBlockGap(VariableBindingPair pair) {
		return variableDefinedInNodes(nodesG1, pair.getBinding1()) && variableDefinedInNodes(nodesG2, pair.getBinding2()) &&
				variableUsedInNodes(nodesG1, pair.getBinding1()) && variableUsedInNodes(nodesG2, pair.getBinding2());
	}

	public boolean variableIsDeclaredInBlockGap(VariableBindingPair pair) {
		return variableDeclaredInNodes(nodesG1, pair.getBinding1()) && variableDeclaredInNodes(nodesG2, pair.getBinding2());
	}

	public boolean variableIsUsedInBlockGap(VariableBindingPair pair) {
		return variableUsedInNodes(nodesG1, pair.getBinding1()) && variableUsedInNodes(nodesG2, pair.getBinding2());
	}

	public Set<IVariableBinding> getVariablesToBeReturnedG1() {
		return variablesToBeReturned(nodesG1);
	}

	public Set<IVariableBinding> getVariablesToBeReturnedG2() {
		return variablesToBeReturned(nodesG2);
	}

	public ITypeBinding getReturnTypeBindingFromReturnStatementG1() {
		TreeSet<PDGNode> nodesInBlock1 = new TreeSet<PDGNode>(nodesG1);
		for(PDGNode nodeG1 : nodesG1) {
			Iterator<GraphEdge> iterator = nodeG1.getOutgoingDependenceIterator();
			while(iterator.hasNext()) {
				PDGDependence dependence = (PDGDependence)iterator.next();
				if(dependence instanceof PDGControlDependence) {
					PDGNode dstNode = (PDGNode)dependence.getDst();
					nodesInBlock1.add(dstNode);
				}
			}
		}
		return getReturnTypeBindingFromReturnStatement(nodesInBlock1);
	}

	public ITypeBinding getReturnTypeBindingFromReturnStatementG2() {
		TreeSet<PDGNode> nodesInBlock2 = new TreeSet<PDGNode>(nodesG2);
		for(PDGNode nodeG2 : nodesG2) {
			Iterator<GraphEdge> iterator = nodeG2.getOutgoingDependenceIterator();
			while(iterator.hasNext()) {
				PDGDependence dependence = (PDGDependence)iterator.next();
				if(dependence instanceof PDGControlDependence) {
					PDGNode dstNode = (PDGNode)dependence.getDst();
					nodesInBlock2.add(dstNode);
				}
			}
		}
		return getReturnTypeBindingFromReturnStatement(nodesInBlock2);
	}

	private Set<IVariableBinding> getUsedVariableBindings(Set<PDGNode> nodes) {
		Set<IVariableBinding> usedVariableBindings = new LinkedHashSet<IVariableBinding>();
		List<Expression> localVariableInstructions = getVariableInstructions(nodes);
		for(Expression variableInstruction : localVariableInstructions) {
			SimpleName simpleName = (SimpleName)variableInstruction;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null && binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding) binding;
				if(!variableBinding.isField() && !simpleName.isDeclaration() &&
						!variableDeclaredInNodes(nodes, variableBinding) && (variableUsedInNodes(nodes, variableBinding) || variableDefinedInNodes(nodes, variableBinding)))
					usedVariableBindings.add(variableBinding);
			}
		}
		return usedVariableBindings;
	}

	private List<Expression> getVariableInstructions(Set<PDGNode> nodes) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> localVariableInstructions = new ArrayList<Expression>();
		for(PDGNode node : nodes) {
			if(node instanceof PDGStatementNode) {
				PDGStatementNode statementNode = (PDGStatementNode)node;
				Statement statement = statementNode.getASTStatement();
				localVariableInstructions.addAll(expressionExtractor.getVariableInstructions(statement));
			}
			else if(node instanceof PDGControlPredicateNode || node instanceof PDGBlockNode) {
				if(node.getStatement() instanceof CompositeStatementObject) {
					CompositeStatementObject composite = (CompositeStatementObject)node.getStatement();
					List<AbstractExpression> expressions = composite.getExpressions();
					for(AbstractExpression expression : expressions) {
						Expression expr = expression.getExpression();
						localVariableInstructions.addAll(expressionExtractor.getVariableInstructions(expr));
					}
				}
			}
		}
		return localVariableInstructions;
	}

	private Set<IVariableBinding> getDeclaredVariableBindings(Set<PDGNode> nodes) {
		Set<IVariableBinding> variableBindings = new LinkedHashSet<IVariableBinding>();
		List<Expression> localVariableInstructions = getVariableInstructions(nodes);
		for(Expression variableInstruction : localVariableInstructions) {
			SimpleName simpleName = (SimpleName)variableInstruction;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null && binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding) binding;
				if(!variableBinding.isField() && (simpleName.isDeclaration() || variableDefinedInNodes(nodes, variableBinding)))
					variableBindings.add(variableBinding);
			}
		}
		return variableBindings;
	}

	private Set<IVariableBinding> variablesToBeReturned(Set<PDGNode> nodes) {
		Set<IVariableBinding> declaredVariableBindings = getDeclaredVariableBindings(nodes);
		Set<IVariableBinding> variablesToBeReturned = new LinkedHashSet<IVariableBinding>();
		for(PDGNode node : nodes) {
			Iterator<GraphEdge> outgoingDependenceIterator =  node.getOutgoingDependenceIterator();
			while(outgoingDependenceIterator.hasNext()) {
				PDGDependence dependence = (PDGDependence)outgoingDependenceIterator.next();
				if(dependence instanceof PDGDataDependence) {
					PDGDataDependence dataDependence = (PDGDataDependence)dependence;
					PDGNode dstPDGNode = (PDGNode)dataDependence.getDst();
					if(!nodes.contains(dstPDGNode)) {
						AbstractVariable data = dataDependence.getData();
						if(data instanceof PlainVariable) {
							PlainVariable plainVariable = (PlainVariable)data;
							for(IVariableBinding variableBinding : declaredVariableBindings) {
								if(variableBinding.getKey().equals(plainVariable.getVariableBindingKey())) {
									variablesToBeReturned.add(variableBinding);
									break;
								}
							}
						}
					}
				}
			}
		}
		return variablesToBeReturned;
	}

	private ITypeBinding getReturnTypeBindingFromReturnStatement(Set<PDGNode> nodes) {
		for(PDGNode node : nodes) {
			Statement statement = node.getASTStatement();
			if(statement instanceof ReturnStatement) {
				ReturnStatement returnStatement = (ReturnStatement)statement;
				if(returnStatement.getExpression() != null) {
					return returnStatement.getExpression().resolveTypeBinding();
				}
			}
		}
		return null;
	}

	public boolean sharesCommonStatements(PDGNodeBlockGap other) {
		Set<PDGNode> intersection1 = new TreeSet<PDGNode>(this.nodesG1);
		intersection1.retainAll(other.nodesG1);
		Set<PDGNode> intersection2 = new TreeSet<PDGNode>(this.nodesG2);
		intersection2.retainAll(other.nodesG2);
		return intersection1.size() > 0 && intersection2.size() > 0;
	}

	public boolean subsumes(PDGNodeBlockGap other) {
		return this.nodesG1.containsAll(other.nodesG1) && this.nodesG2.containsAll(other.nodesG2) &&
				this.nodesG1.size() >= other.nodesG1.size() && this.nodesG2.size() >= other.nodesG2.size();
	}

	public boolean isSubsumed(List<PDGNodeBlockGap> blockGaps) {
		for(PDGNodeBlockGap blockGap : blockGaps) {
			if(blockGap.subsumes(this)) {
				return true;
			}
		}
		return false;
	}

	public PDGNodeBlockGap merge(PDGNodeBlockGap other) {
		if(this.parent.equals(other.parent)) {
			PDGNodeBlockGap merged = new PDGNodeBlockGap(this.parent);
			merged.nodesG1.addAll(this.nodesG1);
			merged.nodesG1.addAll(other.nodesG1);
			merged.nodesG2.addAll(this.nodesG2);
			merged.nodesG2.addAll(other.nodesG2);
			merged.nodeDifferences.addAll(this.nodeDifferences);
			for(ASTNodeDifference difference : other.nodeDifferences) {
				if(!merged.nodeDifferences.contains(difference)) {
					merged.nodeDifferences.add(difference);
				}
			}
			return merged;
		}
		return null;
	}

	public ITypeBinding getReturnType() {
		if(returnedVariableBinding != null) {
			IVariableBinding returnedVariable1 = returnedVariableBinding.getBinding1();
			IVariableBinding returnedVariable2 = returnedVariableBinding.getBinding2();
			if(returnedVariable1.getType().isEqualTo(returnedVariable2.getType()) && returnedVariable1.getType().getQualifiedName().equals(returnedVariable2.getType().getQualifiedName())) {
				return returnedVariable1.getType();
			}
			else {
				ITypeBinding typeBinding = ASTNodeMatcher.commonSuperType(returnedVariable1.getType(), returnedVariable2.getType());
				return typeBinding;
			}
		}
		ITypeBinding returnTypeBinding1 = getReturnTypeBindingFromReturnStatementG1();
		ITypeBinding returnTypeBinding2 = getReturnTypeBindingFromReturnStatementG2();
		if(returnTypeBinding1 != null && returnTypeBinding2 != null) {
			if(returnTypeBinding1.isEqualTo(returnTypeBinding2) && returnTypeBinding1.getQualifiedName().equals(returnTypeBinding2.getQualifiedName())) {
				return returnTypeBinding1;
			}
			else {
				ITypeBinding typeBinding = ASTNodeMatcher.commonSuperType(returnTypeBinding1, returnTypeBinding2);
				return typeBinding;
			}
		}
		return null;
	}

	public Set<ITypeBinding> getThrownExceptions() {
		Set<ITypeBinding> thrownExceptionTypeBindings = new LinkedHashSet<ITypeBinding>();
		for(PDGNode nodeG1 : nodesG1) {
			Statement statement1 = nodeG1.getASTStatement();
			TryStatement tryStatement = isNestedUnderTryBlock(statement1);
			if(tryStatement != null && belongsToBlockGap(tryStatement)) {
				//do nothing
			}
			else {
				ThrownExceptionVisitor thrownExceptionVisitor = new ThrownExceptionVisitor();
				statement1.accept(thrownExceptionVisitor);
				for(ITypeBinding thrownException : thrownExceptionVisitor.getTypeBindings()) {
					if(nodeG1.getThrownExceptionTypes().contains(thrownException.getQualifiedName())) {
						addTypeBinding(thrownException, thrownExceptionTypeBindings);
					}
				}
			}
		}
		for(PDGNode nodeG2 : nodesG2) {
			Statement statement2 = nodeG2.getASTStatement();
			TryStatement tryStatement = isNestedUnderTryBlock(statement2);
			if(tryStatement != null && belongsToBlockGap(tryStatement)) {
				//do nothing
			}
			else {
				ThrownExceptionVisitor thrownExceptionVisitor = new ThrownExceptionVisitor();
				statement2.accept(thrownExceptionVisitor);
				for(ITypeBinding thrownException : thrownExceptionVisitor.getTypeBindings()) {
					if(nodeG2.getThrownExceptionTypes().contains(thrownException.getQualifiedName())) {
						addTypeBinding(thrownException, thrownExceptionTypeBindings);
					}
				}
			}
		}
		return thrownExceptionTypeBindings;
	}

	private TryStatement isNestedUnderTryBlock(ASTNode node) {
		ASTNode parent = node.getParent();
		while(parent != null) {
			if(parent instanceof TryStatement) {
				return (TryStatement)parent;
			}
			parent = parent.getParent();
		}
		return null;
	}

	private boolean belongsToBlockGap(Statement statement) {
		for(PDGNode node : getNodesG1()) {
			if(node.getASTStatement().equals(statement)) {
				return true;
			}
		}
		for(PDGNode node : getNodesG2()) {
			if(node.getASTStatement().equals(statement)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<IMethodBinding> getAllMethodsInvokedThroughVariable(VariableBindingPair variableBindingPair) {
		Set<IMethodBinding> methods = new LinkedHashSet<IMethodBinding>();
		for(PDGNode nodeG1 : nodesG1) {
			AbstractStatement abstractStatement = nodeG1.getStatement();
			if(abstractStatement instanceof StatementObject) {
				StatementObject statement = (StatementObject)abstractStatement;
				methods.addAll(getAllMethodsInvokedThroughVariable(statement, variableBindingPair.getBinding1()));
			}
			else if(abstractStatement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)abstractStatement;
				for(AbstractExpression expression : composite.getExpressions()) {
					methods.addAll(getAllMethodsInvokedThroughVariable(expression, variableBindingPair.getBinding1()));
				}
			}
		}
		for(PDGNode nodeG2 : nodesG2) {
			AbstractStatement abstractStatement = nodeG2.getStatement();
			if(abstractStatement instanceof StatementObject) {
				StatementObject statement = (StatementObject)abstractStatement;
				methods.addAll(getAllMethodsInvokedThroughVariable(statement, variableBindingPair.getBinding2()));
			}
			else if(abstractStatement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)abstractStatement;
				for(AbstractExpression expression : composite.getExpressions()) {
					methods.addAll(getAllMethodsInvokedThroughVariable(expression, variableBindingPair.getBinding2()));
				}
			}
		}
		return methods;
	}

	public boolean isRefactorable() {
		if(branchStatementWithoutInnermostLoop(nodesG1) || branchStatementWithoutInnermostLoop(nodesG2)) {
			return false;
		}
		if(notAllPossibleExecutionFlowsEndInReturn()) {
			return false;
		}
		return true;
	}

	private boolean notAllPossibleExecutionFlowsEndInReturn() {
		Set<PDGNode> allNodesUnderParentG1 = parent.getDescendantNodesG1();
		Set<PDGNode> allNodesUnderParentG2 = parent.getDescendantNodesG2();
		TreeSet<PDGNode> nodesInBlock1 = new TreeSet<PDGNode>(nodesG1);
		for(PDGNode nodeG1 : nodesG1) {
			Iterator<GraphEdge> iterator = nodeG1.getOutgoingDependenceIterator();
			while(iterator.hasNext()) {
				PDGDependence dependence = (PDGDependence)iterator.next();
				if(dependence instanceof PDGControlDependence) {
					PDGNode dstNode = (PDGNode)dependence.getDst();
					nodesInBlock1.add(dstNode);
				}
			}
		}
		TreeSet<PDGNode> nodesInBlock2 = new TreeSet<PDGNode>(nodesG2);
		for(PDGNode nodeG2 : nodesG2) {
			Iterator<GraphEdge> iterator = nodeG2.getOutgoingDependenceIterator();
			while(iterator.hasNext()) {
				PDGDependence dependence = (PDGDependence)iterator.next();
				if(dependence instanceof PDGControlDependence) {
					PDGNode dstNode = (PDGNode)dependence.getDst();
					nodesInBlock2.add(dstNode);
				}
			}
		}
		Set<PDGNode> allConditionalReturnStatements1 = PreconditionExaminer.extractConditionalReturnStatements(allNodesUnderParentG1);
		Set<PDGNode> allConditionalReturnStatements2 = PreconditionExaminer.extractConditionalReturnStatements(allNodesUnderParentG2);
		Set<PDGNode> conditionalReturnStatementsInsideBlock1 = PreconditionExaminer.extractConditionalReturnStatements(nodesInBlock1);
		Set<PDGNode> conditionalReturnStatementsInsideBlock2 = PreconditionExaminer.extractConditionalReturnStatements(nodesInBlock2);
		Set<PDGNode> returnStatementsAfterBlockNodes1 = nodesInBlock1.isEmpty() ? new TreeSet<PDGNode>() : PreconditionExaminer.extractReturnStatementsAfterId(allNodesUnderParentG1, nodesInBlock1.last().getId());
		Set<PDGNode> returnStatementsAfterBlockNodes2 = nodesInBlock2.isEmpty() ? new TreeSet<PDGNode>() : PreconditionExaminer.extractReturnStatementsAfterId(allNodesUnderParentG2, nodesInBlock2.last().getId());
		boolean notAllPossibleExecutionFlowsEndInReturn = false;
		ITypeBinding returnTypeBinding = getReturnType();
		if(returnTypeBinding != null && !parent.containsMappedReturnStatementInDirectChildren() && !parent.lastIfElseIfChainContainsReturnOrThrowStatements()) {
			notAllPossibleExecutionFlowsEndInReturn = true;
		}
		if((conditionalReturnStatementsInsideBlock1.size() > 0 && allConditionalReturnStatements1.size() > conditionalReturnStatementsInsideBlock1.size()) ||
				(conditionalReturnStatementsInsideBlock1.size() > 0 && returnStatementsAfterBlockNodes1.size() > 0) ||
				(conditionalReturnStatementsInsideBlock1.size() > 0 && notAllPossibleExecutionFlowsEndInReturn) ||
				(conditionalReturnStatementsInsideBlock2.size() > 0 && allConditionalReturnStatements2.size() > conditionalReturnStatementsInsideBlock2.size()) ||
				(conditionalReturnStatementsInsideBlock2.size() > 0 && returnStatementsAfterBlockNodes2.size() > 0) ||
				(conditionalReturnStatementsInsideBlock2.size() > 0 && notAllPossibleExecutionFlowsEndInReturn) ) {
			return true;
		}
		return false;
	}

	private boolean branchStatementWithoutInnermostLoop(Set<PDGNode> nodes) {
		for(PDGNode node : nodes) {
			CFGNode cfgNode = node.getCFGNode();
			if(cfgNode instanceof CFGBreakNode) {
				CFGBreakNode breakNode = (CFGBreakNode)cfgNode;
				CFGNode innerMostLoopNode = breakNode.getInnerMostLoopNode();
				if(innerMostLoopNode != null && !nodes.contains(innerMostLoopNode.getPDGNode())) {
					return true;
				}
			}
			else if(cfgNode instanceof CFGContinueNode) {
				CFGContinueNode continueNode = (CFGContinueNode)cfgNode;
				CFGNode innerMostLoopNode = continueNode.getInnerMostLoopNode();
				if(innerMostLoopNode != null && !nodes.contains(innerMostLoopNode.getPDGNode())) {
					return true;
				}
			}
		}
		return false;
	}
}
