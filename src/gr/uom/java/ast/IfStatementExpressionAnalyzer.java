package gr.uom.java.ast;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;

public class IfStatementExpressionAnalyzer {
	//parent nodes are CONDITIONAL_AND (&&), CONDITIONAL_OR (||) infix operators, while leaf nodes are expressions
	private DefaultMutableTreeNode root;
	private Expression completeExpression;
	//contains the expressions corresponding to each candidate type variable
	private Map<SimpleName, Expression> typeVariableExpressionMap;
	//contains the static fields corresponding to each candidate type variable
	private Map<SimpleName, SimpleName> typeVariableStaticFieldMap;
	//contains the subclass types corresponding to each candidate type variable
	private Map<SimpleName, Type> typeVariableSubclassMap;
	
	//contains the expressions corresponding to each candidate type method invocation
	private Map<MethodInvocation, Expression> typeMethodInvocationExpressionMap;
	//contains the static fields corresponding to each candidate type method invocation
	private Map<MethodInvocation, SimpleName> typeMethodInvocationStaticFieldMap;
	//contains the subclass types corresponding to each candidate type method invocation
	private Map<MethodInvocation, Type> typeMethodInvocationSubclassMap;
	
	public IfStatementExpressionAnalyzer(Expression completeExpression) {
		this.root = new DefaultMutableTreeNode();
		this.completeExpression = completeExpression;
		this.typeVariableExpressionMap = new LinkedHashMap<SimpleName, Expression>();
		this.typeVariableStaticFieldMap = new LinkedHashMap<SimpleName, SimpleName>();
		this.typeVariableSubclassMap = new LinkedHashMap<SimpleName, Type>();
		this.typeMethodInvocationExpressionMap = new LinkedHashMap<MethodInvocation, Expression>();
		this.typeMethodInvocationStaticFieldMap = new LinkedHashMap<MethodInvocation, SimpleName>();
		this.typeMethodInvocationSubclassMap = new LinkedHashMap<MethodInvocation, Type>();
		processExpression(root, completeExpression);
	}
	
	public void putTypeVariableExpression(SimpleName typeVariable, Expression expression) {
		typeVariableExpressionMap.put(typeVariable, expression);
	}
	
	public Set<SimpleName> getTargetVariables() {
		return typeVariableExpressionMap.keySet();
	}
	
	public Expression getTypeVariableExpression(SimpleName typeVariable) {
		return typeVariableExpressionMap.get(typeVariable);
	}
	
	public void putTypeVariableStaticField(SimpleName typeVariable, SimpleName staticField) {
		typeVariableStaticFieldMap.put(typeVariable, staticField);
	}
	
	public SimpleName getTypeVariableStaticField(SimpleName typeVariable) {
		return typeVariableStaticFieldMap.get(typeVariable);
	}
	
	public void putTypeVariableSubclass(SimpleName typeVariable, Type subclass) {
		typeVariableSubclassMap.put(typeVariable, subclass);
	}
	
	public Type getTypeVariableSubclass(SimpleName typeVariable) {
		return typeVariableSubclassMap.get(typeVariable);
	}
	
	public void putTypeMethodInvocationExpression(MethodInvocation typeMethodInvocation, Expression expression) {
		typeMethodInvocationExpressionMap.put(typeMethodInvocation, expression);
	}
	
	public Set<MethodInvocation> getTargetMethodInvocations() {
		return typeMethodInvocationExpressionMap.keySet();
	}
	
	public Expression getTypeMethodInvocationExpression(MethodInvocation typeMethodInvocation) {
		return typeMethodInvocationExpressionMap.get(typeMethodInvocation);
	}
	
	public void putTypeMethodInvocationStaticField(MethodInvocation typeMethodInvocation, SimpleName staticField) {
		typeMethodInvocationStaticFieldMap.put(typeMethodInvocation, staticField);
	}
	
	public SimpleName getTypeMethodInvocationStaticField(MethodInvocation typeMethodInvocation) {
		return typeMethodInvocationStaticFieldMap.get(typeMethodInvocation);
	}
	
	public void putTypeMethodInvocationSubclass(MethodInvocation typeMethodInvocation, Type subclass) {
		typeMethodInvocationSubclassMap.put(typeMethodInvocation, subclass);
	}
	
	public Type getTypeMethodInvocationSubclass(MethodInvocation typeMethodInvocation) {
		return typeMethodInvocationSubclassMap.get(typeMethodInvocation);
	}
	
	private void processExpression(DefaultMutableTreeNode parent, Expression expression) {
		if(expression instanceof InfixExpression) {
			InfixExpression infixExpression = (InfixExpression)expression;
			InfixExpression.Operator operator = infixExpression.getOperator();
			if(operator.equals(InfixExpression.Operator.CONDITIONAL_AND) || operator.equals(InfixExpression.Operator.CONDITIONAL_OR)) {
				parent.setUserObject(operator);
				DefaultMutableTreeNode leftOperandNode = new DefaultMutableTreeNode();
				DefaultMutableTreeNode rightOperandNode = new DefaultMutableTreeNode();
				parent.add(leftOperandNode);
				parent.add(rightOperandNode);
				processExpression(leftOperandNode, infixExpression.getLeftOperand());
				processExpression(rightOperandNode, infixExpression.getRightOperand());
				if(infixExpression.hasExtendedOperands()) {
					DefaultMutableTreeNode grandParent = (DefaultMutableTreeNode)parent.getParent();
					int parentIndex = -1;
					if(grandParent != null)
						parentIndex = grandParent.getIndex(parent);
					DefaultMutableTreeNode newParent = processExtendedOperands(parent, infixExpression.extendedOperands());
					if(grandParent != null)
						grandParent.insert(newParent, parentIndex);
					else
						root = newParent;
				}
			}
			else {
				parent.setUserObject(infixExpression);
			}
		}
		else {
			parent.setUserObject(expression);
		}
	}
	
	private DefaultMutableTreeNode processExtendedOperands(DefaultMutableTreeNode parent, List<Expression> extendedOperands) {
		InfixExpression.Operator operator = (InfixExpression.Operator)parent.getUserObject();
		DefaultMutableTreeNode newParent = null;
		DefaultMutableTreeNode oldParent = parent;
		for(Expression extendedOperand : extendedOperands) {
			newParent = new DefaultMutableTreeNode();
			newParent.setUserObject(operator);
			newParent.insert(oldParent, 0);
			DefaultMutableTreeNode rightOperandNode = new DefaultMutableTreeNode();
			rightOperandNode.setUserObject(extendedOperand);
			newParent.insert(rightOperandNode, 1);
			oldParent = newParent;
		}
		return newParent;
	}
	
	public List<InstanceofExpression> getInstanceofExpressions() {
		List<InstanceofExpression> expressionList = new ArrayList<InstanceofExpression>();
		DefaultMutableTreeNode leaf = root.getFirstLeaf();
		while(leaf != null) {
			Expression expression = (Expression)leaf.getUserObject();
			if(expression instanceof InstanceofExpression) {
				InstanceofExpression instanceofExpression = (InstanceofExpression)expression;
				expressionList.add(instanceofExpression);
			}
			leaf = leaf.getNextLeaf();
		}
		return expressionList;
	}
	
	public List<InfixExpression> getInfixExpressionsWithEqualsOperator() {
		List<InfixExpression> expressionList = new ArrayList<InfixExpression>();
		DefaultMutableTreeNode leaf = root.getFirstLeaf();
		while(leaf != null) {
			Expression expression = (Expression)leaf.getUserObject();
			if(expression instanceof InfixExpression) {
				InfixExpression infixExpression = (InfixExpression)expression;
				InfixExpression.Operator operator = infixExpression.getOperator();
				if(operator.equals(InfixExpression.Operator.EQUALS))
					expressionList.add(infixExpression);
			}
			leaf = leaf.getNextLeaf();
		}
		return expressionList;
	}
	
	public DefaultMutableTreeNode getRemainingExpression(Expression expressionToBeRemoved) {
		DefaultMutableTreeNode newRoot = new DefaultMutableTreeNode();
		processExpression(newRoot, completeExpression);
		DefaultMutableTreeNode leaf = newRoot.getFirstLeaf();
		while(leaf != null) {
			Expression expression = (Expression)leaf.getUserObject();
			if(expression.equals(expressionToBeRemoved)) {
				DefaultMutableTreeNode parent = (DefaultMutableTreeNode)leaf.getParent();
				if(parent != null) {
					DefaultMutableTreeNode grandParent = (DefaultMutableTreeNode)parent.getParent();
					DefaultMutableTreeNode sibling = null;
					if(leaf.getNextSibling() != null) {
						sibling = leaf.getNextSibling();
					}
					else if(leaf.getPreviousSibling() != null) {
						sibling = leaf.getPreviousSibling();
					}
					if(grandParent != null) {
						int parentIndex = grandParent.getIndex(parent);
						grandParent.remove(parent);
						grandParent.insert(sibling, parentIndex);
					}
					else {
						newRoot = sibling;
					}
					break;
				}
				else {
					newRoot = null;
					break;
				}
			}
			leaf = leaf.getNextLeaf();
		}
		return newRoot;
	}
	
	public boolean allParentNodesAreConditionalAndOperators() {
		Enumeration<DefaultMutableTreeNode> enumeration = root.breadthFirstEnumeration();
		while(enumeration.hasMoreElements()) {
			DefaultMutableTreeNode node = enumeration.nextElement();
			if(!node.isLeaf()) {
				InfixExpression.Operator operator = (InfixExpression.Operator)node.getUserObject();
				if(!operator.equals(InfixExpression.Operator.CONDITIONAL_AND))
					return false;
			}
		}
		return true;
	}
	
	public Expression getCompleteExpression() {
		return completeExpression;
	}

	public String toString() {
		return completeExpression.toString();
	}
}
