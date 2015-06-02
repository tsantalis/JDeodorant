package gr.uom.java.ast.decomposition.cfg.mapping;

import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.CompositeStatementObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.GraphEdge;
import gr.uom.java.ast.decomposition.cfg.PDGBlockNode;
import gr.uom.java.ast.decomposition.cfg.PDGControlPredicateNode;
import gr.uom.java.ast.decomposition.cfg.PDGDataDependence;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PDGStatementNode;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.util.ExpressionExtractor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;

public class PDGNodeBlockGap {
	private CloneStructureNode parent;
	private TreeSet<PDGNode> nodesG1;
	private TreeSet<PDGNode> nodesG2;
	private List<ASTNodeDifference> nodeDifferences;
	private Set<VariableBindingPair> parameterBindings;
	private VariableBindingPair returnedVariableBinding;
	
	public PDGNodeBlockGap(CloneStructureNode parent) {
		this.parent = parent;
		this.nodesG1 = new TreeSet<PDGNode>();
		this.nodesG2 = new TreeSet<PDGNode>();
		this.nodeDifferences = new ArrayList<ASTNodeDifference>();
		this.parameterBindings = new LinkedHashSet<VariableBindingPair>();
	}

	public TreeSet<PDGNode> getNodesG1() {
		return nodesG1;
	}

	public TreeSet<PDGNode> getNodesG2() {
		return nodesG2;
	}

	public Set<VariableBindingPair> getParameterBindings() {
		return parameterBindings;
	}

	public void addParameterBinding(VariableBindingPair parameterBinding) {
		this.parameterBindings.add(parameterBinding);
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
				if(lastNodeG1Found && lastNodeG2Found && !nodeMapping.getPreconditionViolations().isEmpty()) {
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

	public Set<IVariableBinding> getVariablesToBeReturnedG1() {
		return variablesToBeReturned(nodesG1);
	}

	public Set<IVariableBinding> getVariablesToBeReturnedG2() {
		return variablesToBeReturned(nodesG2);
	}

	private Set<IVariableBinding> getUsedVariableBindings(Set<PDGNode> nodes) {
		Set<IVariableBinding> usedVariableBindings = new LinkedHashSet<IVariableBinding>();
		List<Expression> localVariableInstructions = getVariableInstructions(nodes);
		for(Expression variableInstruction : localVariableInstructions) {
			SimpleName simpleName = (SimpleName)variableInstruction;
			IBinding binding = simpleName.resolveBinding();
			if(binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding) binding;
				if(!variableBinding.isField() && !simpleName.isDeclaration() &&
						!variableDeclaredInNodes(nodes, variableBinding) && variableUsedInNodes(nodes, variableBinding))
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

	private boolean variableUsedInNodes(Set<PDGNode> nodes, IVariableBinding binding) {
		for(PDGNode node : nodes) {
			Iterator<AbstractVariable> declaredVariableIterator = node.getUsedVariableIterator();
			while(declaredVariableIterator.hasNext()) {
				AbstractVariable variable = declaredVariableIterator.next();
				if(variable instanceof PlainVariable) {
					PlainVariable plainVariable = (PlainVariable)variable;
					if(plainVariable.getVariableBindingKey().equals(binding.getKey())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean variableDeclaredInNodes(Set<PDGNode> nodes, IVariableBinding binding) {
		for(PDGNode node : nodes) {
			Iterator<AbstractVariable> declaredVariableIterator = node.getDeclaredVariableIterator();
			while(declaredVariableIterator.hasNext()) {
				AbstractVariable variable = declaredVariableIterator.next();
				if(variable instanceof PlainVariable) {
					PlainVariable plainVariable = (PlainVariable)variable;
					if(plainVariable.getVariableBindingKey().equals(binding.getKey())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private Set<IVariableBinding> getDeclaredVariableBindings(Set<PDGNode> nodes) {
		Set<IVariableBinding> variableBindings = new LinkedHashSet<IVariableBinding>();
		List<Expression> localVariableInstructions = getVariableInstructions(nodes);
		for(Expression variableInstruction : localVariableInstructions) {
			SimpleName simpleName = (SimpleName)variableInstruction;
			IBinding binding = simpleName.resolveBinding();
			if(binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding) binding;
				if(!variableBinding.isField() && (simpleName.isDeclaration() || variableDefinedInNodes(nodes, variableBinding)))
					variableBindings.add(variableBinding);
			}
		}
		return variableBindings;
	}

	private boolean variableDefinedInNodes(Set<PDGNode> nodes, IVariableBinding binding) {
		for(PDGNode node : nodes) {
			Iterator<AbstractVariable> declaredVariableIterator = node.getDefinedVariableIterator();
			while(declaredVariableIterator.hasNext()) {
				AbstractVariable variable = declaredVariableIterator.next();
				if(variable instanceof PlainVariable) {
					PlainVariable plainVariable = (PlainVariable)variable;
					if(plainVariable.getVariableBindingKey().equals(binding.getKey())) {
						return true;
					}
				}
			}
		}
		return false;
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
}
