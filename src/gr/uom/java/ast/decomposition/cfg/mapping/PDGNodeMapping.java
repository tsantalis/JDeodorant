package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;

import gr.uom.java.ast.decomposition.AbstractMethodFragment;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CompositeVariable;
import gr.uom.java.ast.decomposition.cfg.PDGBlockNode;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.ExpressionPreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.PreconditionViolation;
import gr.uom.java.ast.decomposition.cfg.mapping.precondition.PreconditionViolationType;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.matching.Difference;
import gr.uom.java.ast.decomposition.matching.DifferenceType;

public class PDGNodeMapping extends IdBasedMapping {
	private PDGNode nodeG1;
	private PDGNode nodeG2;
	private ASTNodeMatcher matcher;
	private List<ASTNodeDifference> nodeDifferences;
	private List<AbstractMethodFragment> additionallyMatchedFragments1;
	private List<AbstractMethodFragment> additionallyMatchedFragments2;
	private PDGNodeMapping symmetricalIfNodePair;
	private boolean symmetricalIfElse;
	private volatile int hashCode = 0;
	
	public PDGNodeMapping(PDGNode nodeG1, PDGNode nodeG2, ASTNodeMatcher matcher) {
		super(nodeG1.getId(), nodeG2.getId());
		this.nodeG1 = nodeG1;
		this.nodeG2 = nodeG2;
		this.matcher = matcher;
		this.nodeDifferences = matcher.getDifferences();
		this.additionallyMatchedFragments1 = matcher.getAdditionallyMatchedFragments1();
		this.additionallyMatchedFragments2 = matcher.getAdditionallyMatchedFragments2();
	}
	
	public PDGNode getNodeG1() {
		return nodeG1;
	}

	public PDGNode getNodeG2() {
		return nodeG2;
	}

	public List<ASTNodeDifference> getNodeDifferences() {
		return nodeDifferences;
	}

	public boolean isAdvancedMatch() {
		return additionallyMatchedFragments1.size() > 0 || additionallyMatchedFragments2.size() > 0 || symmetricalIfElse;
	}

	public List<AbstractMethodFragment> getAdditionallyMatchedFragments1() {
		return additionallyMatchedFragments1;
	}

	public List<AbstractMethodFragment> getAdditionallyMatchedFragments2() {
		return additionallyMatchedFragments2;
	}

	public boolean containsAdditionallyMatchedFragment1(PDGNode node) {
		for(AbstractMethodFragment fragment : additionallyMatchedFragments1) {
			if(fragment instanceof AbstractStatement) {
				AbstractStatement statement = (AbstractStatement)fragment;
				if(statement.getStatement().equals(node.getASTStatement())) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean containsAdditionallyMatchedFragment2(PDGNode node) {
		for(AbstractMethodFragment fragment : additionallyMatchedFragments2) {
			if(fragment instanceof AbstractStatement) {
				AbstractStatement statement = (AbstractStatement)fragment;
				if(statement.getStatement().equals(node.getASTStatement())) {
					return true;
				}
			}
		}
		return false;
	}

	public ITypeRoot getTypeRoot1() {
		return matcher.getTypeRoot1();
	}

	public ITypeRoot getTypeRoot2() {
		return matcher.getTypeRoot2();
	}

	public PDGNodeMapping getSymmetricalIfNodePair() {
		return symmetricalIfNodePair;
	}

	public void setSymmetricalIfNodePair(PDGNodeMapping symmetricalIfNodePair) {
		this.symmetricalIfNodePair = symmetricalIfNodePair;
	}

	public boolean isSymmetricalIfElse() {
		return symmetricalIfElse;
	}

	public void setSymmetricalIfElse(boolean symmetricalIfElse) {
		this.symmetricalIfElse = symmetricalIfElse;
	}

	public boolean isFalseControlDependent() {
		PDGControlDependence controlDependence1 = nodeG1.getIncomingControlDependence();
		PDGControlDependence controlDependence2 = nodeG2.getIncomingControlDependence();
		if(controlDependence1 != null && controlDependence2 != null)
			return controlDependence1.isFalseControlDependence() && controlDependence2.isFalseControlDependence();
		if(nodeG1 instanceof PDGBlockNode && nodeG2 instanceof PDGBlockNode)
			return isNestedUnderElse((PDGBlockNode)nodeG1) && isNestedUnderElse((PDGBlockNode)nodeG2);
		return false;
	}

	public boolean isNode1FalseControlDependent() {
		PDGControlDependence controlDependence1 = nodeG1.getIncomingControlDependence();
		if(controlDependence1 != null)
			return controlDependence1.isFalseControlDependence();
		if(nodeG1 instanceof PDGBlockNode)
			return isNestedUnderElse((PDGBlockNode)nodeG1);
		return false;
	}

	public boolean isNode2FalseControlDependent() {
		PDGControlDependence controlDependence2 = nodeG2.getIncomingControlDependence();
		if(controlDependence2 != null)
			return controlDependence2.isFalseControlDependence();
		if(nodeG2 instanceof PDGBlockNode)
			return isNestedUnderElse((PDGBlockNode)nodeG2);
		return false;
	}

	private boolean isNestedUnderElse(PDGBlockNode blockNode) {
		Statement statement = blockNode.getASTStatement();
		if(statement.getParent() instanceof Block) {
			Block block = (Block)statement.getParent();
			if(block.getParent() instanceof IfStatement) {
				IfStatement ifStatement = (IfStatement)block.getParent();
				if(ifStatement.getElseStatement() != null && ifStatement.getElseStatement().equals(block))
					return true;
			}
		}
		else if(statement.getParent() instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement)statement.getParent();
			if(ifStatement.getElseStatement() != null && ifStatement.getElseStatement().equals(statement))
				return true;
		}
		return false;
	}

	public boolean matchingVariableDifference(AbstractVariable variable1, AbstractVariable variable2) {
		if(variable1.getClass() == variable2.getClass()) {
			String rightPartVariable1 = null;
			String rightPartVariable2 = null;
			if(variable1 instanceof CompositeVariable) {
				CompositeVariable comp1 = (CompositeVariable)variable1;
				CompositeVariable comp2 = (CompositeVariable)variable2;
				rightPartVariable1 = comp1.getRightPart().toString();
				rightPartVariable2 = comp2.getRightPart().toString();
			}
			boolean equalRightPart = false;
			if(rightPartVariable1 != null && rightPartVariable2 != null) {
				equalRightPart = rightPartVariable1.equals(rightPartVariable2);
			}
			else {
				equalRightPart = true;
			}
			for(ASTNodeDifference nodeDifference : nodeDifferences)
			{
				List<Difference> differences = nodeDifference.getDifferences();
				for(Difference difference : differences) {
					if(equalRightPart && difference.getFirstValue().equals(variable1.getVariableName()) &&
							difference.getSecondValue().equals(variable2.getVariableName())) {
						return true;
					}
					else if(difference.getType().equals(DifferenceType.SUBCLASS_TYPE_MISMATCH) &&
							difference.getFirstValue().equals(variable1.getVariableType()) &&
							difference.getSecondValue().equals(variable2.getVariableType())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean isVoidMethodCallDifferenceCoveringEntireStatement() {
		boolean expression1IsVoidMethodCallDifference = false;
		boolean expression2IsVoidMethodCallDifference = false;
		for(ASTNodeDifference difference : nodeDifferences) {
			Expression expr1 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(difference.getExpression1().getExpression());
			Expression expr2 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(difference.getExpression2().getExpression());
			for(PreconditionViolation violation : getPreconditionViolations()) {
				if(violation instanceof ExpressionPreconditionViolation && violation.getType().equals(PreconditionViolationType.EXPRESSION_DIFFERENCE_IS_VOID_METHOD_CALL)) {
					ExpressionPreconditionViolation expressionViolation = (ExpressionPreconditionViolation)violation;
					Expression expression = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expressionViolation.getExpression().getExpression());
					if(expression.equals(expr1)) {
						if(expr1.getParent() instanceof ExpressionStatement) {
							expression1IsVoidMethodCallDifference = true;
						}
					}
					if(expression.equals(expr2)) {
						if(expr2.getParent() instanceof ExpressionStatement) {
							expression2IsVoidMethodCallDifference = true;
						}
					}
				}
			}
		}
		return expression1IsVoidMethodCallDifference && expression2IsVoidMethodCallDifference;
	}

	public boolean declaresInconsistentlyRenamedVariable(Set<VariableBindingPair> renamedVariables) {
		VariableBindingPair variableBindingPair = declaresVariableWithVariableNameMismatch();
		if(variableBindingPair != null) {
			if(renamedVariables.contains(variableBindingPair))
				return false;
			else
				return true;
		}
		return false;
	}

	private VariableBindingPair declaresVariableWithVariableNameMismatch() {
		for(ASTNodeDifference difference : nodeDifferences) {
			if(difference.containsDifferenceType(DifferenceType.VARIABLE_NAME_MISMATCH)) {
				Expression expr1 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(difference.getExpression1().getExpression());
				Expression expr2 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(difference.getExpression2().getExpression());
				if(expr1 instanceof SimpleName && expr2 instanceof SimpleName) {
					SimpleName simpleName1 = (SimpleName)expr1;
					SimpleName simpleName2 = (SimpleName)expr2;
					IBinding binding1 = simpleName1.resolveBinding();
					IBinding binding2 = simpleName2.resolveBinding();
					if(binding1 != null && binding1.getKind() == IBinding.VARIABLE && binding2 != null && binding2.getKind() == IBinding.VARIABLE) {
						IVariableBinding variableBinding1 = (IVariableBinding)binding1;
						IVariableBinding variableBinding2 = (IVariableBinding)binding2;
						PlainVariable variable1 = new PlainVariable(variableBinding1);
						PlainVariable variable2 = new PlainVariable(variableBinding2);
						if(nodeG1.declaresLocalVariable(variable1) && nodeG2.declaresLocalVariable(variable2)) {
							return new VariableBindingPair(variableBinding1, variableBinding2);
						}
					}
				}
			}
		}
		return null;
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof PDGNodeMapping) {
			PDGNodeMapping mapping = (PDGNodeMapping)o;
			return this.nodeG1.equals(mapping.nodeG1) &&
					this.nodeG2.equals(mapping.nodeG2);
		}
		return false;
	}

	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 37*result + nodeG1.hashCode();
			result = 37*result + nodeG2.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(nodeG1);
		sb.append(nodeG2);
		for(ASTNodeDifference nodeDifference : nodeDifferences)
		{
			sb.append(nodeDifference.toString());
		}
		return sb.toString();
	}
}
