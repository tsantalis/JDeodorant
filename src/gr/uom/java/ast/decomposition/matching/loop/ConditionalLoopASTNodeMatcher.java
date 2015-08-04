package gr.uom.java.ast.decomposition.matching.loop;

import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;
import gr.uom.java.ast.decomposition.matching.Difference;
import gr.uom.java.ast.decomposition.matching.DifferenceType;

import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.SimpleName;

public class ConditionalLoopASTNodeMatcher extends ASTNodeMatcher {

	public ConditionalLoopASTNodeMatcher(ITypeRoot root1, ITypeRoot root2) {
		super(root1, root2);
	}

	public boolean isInfixExpressionWithCompositeParent(ASTNode node) {
		return false;
	}

	public boolean match(InfixExpression node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(getTypeRoot1());
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(getTypeRoot2());
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (!(other instanceof InfixExpression)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			else 
			{
				InfixExpression o = (InfixExpression) other;
				// be careful not to trigger lazy creation of extended operand lists
				if (node.hasExtendedOperands() && o.hasExtendedOperands()) {
					if(node.extendedOperands().size() != o.extendedOperands().size()) {
						Difference diff = new Difference(node.toString(),o.toString(),DifferenceType.INFIX_EXTENDED_OPERAND_NUMBER_MISMATCH);
						astNodeDifference.addDifference(diff);
					}
					else {
						safeSubtreeListMatch(node.extendedOperands(), o.extendedOperands());
					}
				}
				if (node.hasExtendedOperands() != o.hasExtendedOperands()) {
					Difference diff = new Difference(node.toString(),o.toString(),DifferenceType.INFIX_EXTENDED_OPERAND_NUMBER_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
				if(!node.getOperator().equals(o.getOperator())) {
					Difference diff = new Difference(node.getOperator().toString(),o.getOperator().toString(),DifferenceType.OPERATOR_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
				boolean leftOperandMatch = safeSubtreeMatch(node.getLeftOperand(), o.getLeftOperand());
				boolean rightOperandMatch = safeSubtreeMatch(node.getRightOperand(), o.getRightOperand());
				if(!leftOperandMatch && !rightOperandMatch) {
					//if both left and right operands do not match, then the entire infix expression should be parameterized
					Difference leftDiff = new Difference(node.getLeftOperand().toString(),o.getLeftOperand().toString(),DifferenceType.INFIX_LEFT_OPERAND_MISMATCH);
					astNodeDifference.addDifference(leftDiff);
					Difference rightDiff = new Difference(node.getRightOperand().toString(),o.getRightOperand().toString(),DifferenceType.INFIX_RIGHT_OPERAND_MISMATCH);
					astNodeDifference.addDifference(rightDiff);
				}
				else if(!leftOperandMatch && rightOperandMatch) {
					//if only the left operand does not match, then the left operand should be parameterized
					if(node.getLeftOperand() instanceof InfixExpression || o.getLeftOperand() instanceof InfixExpression) {
						Difference leftOperandDiff = new Difference(node.getLeftOperand().toString(),o.getLeftOperand().toString(),DifferenceType.INFIX_LEFT_OPERAND_MISMATCH);
						ASTInformationGenerator.setCurrentITypeRoot(getTypeRoot1());
						AbstractExpression leftOp1 = new AbstractExpression(node.getLeftOperand());
						ASTInformationGenerator.setCurrentITypeRoot(getTypeRoot2());
						AbstractExpression leftOp2 = new AbstractExpression(o.getLeftOperand());
						ASTNodeDifference astLeftOperandDifference = new ASTNodeDifference(leftOp1, leftOp2);
						astLeftOperandDifference.addDifference(leftOperandDiff);
						addDifference(astLeftOperandDifference);
					}
				}
				else if(leftOperandMatch && !rightOperandMatch) {
					//if only the right operand does not match, then the right operand should be parameterized
					if(node.getRightOperand() instanceof InfixExpression || o.getRightOperand() instanceof InfixExpression) {
						Difference rightOperandDiff = new Difference(node.getRightOperand().toString(),o.getRightOperand().toString(),DifferenceType.INFIX_RIGHT_OPERAND_MISMATCH);
						ASTInformationGenerator.setCurrentITypeRoot(getTypeRoot1());
						AbstractExpression rightOp1 = new AbstractExpression(node.getRightOperand());
						ASTInformationGenerator.setCurrentITypeRoot(getTypeRoot2());
						AbstractExpression rightOp2 = new AbstractExpression(o.getRightOperand());
						ASTNodeDifference astRightOperandDifference = new ASTNodeDifference(rightOp1, rightOp2);
						astRightOperandDifference.addDifference(rightOperandDiff);
						addDifference(astRightOperandDifference);
					}
				}
			}
			if(!astNodeDifference.isEmpty())
				addDifference(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
		return false;
	}
	
	public void compareTypes(Expression node, Expression other) {
		if(node != null && other != null) {
			ASTInformationGenerator.setCurrentITypeRoot(getTypeRoot1());
			AbstractExpression exp1 = new AbstractExpression(node);
			ASTInformationGenerator.setCurrentITypeRoot(getTypeRoot2());
			AbstractExpression exp2 = new AbstractExpression((Expression)other);
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
			if(!typeMatch) {
				Difference diff = new Difference(node.resolveTypeBinding().getQualifiedName(),other.resolveTypeBinding().getQualifiedName(),DifferenceType.VARIABLE_TYPE_MISMATCH);
				astNodeDifference.addDifference(diff);
			}
			else if(node instanceof SimpleName && other instanceof SimpleName) {
				SimpleName nodeSimpleName = (SimpleName)node;
				SimpleName otherSimpleName = (SimpleName)other;
				IBinding nodeBinding = nodeSimpleName.resolveBinding();
				IBinding otherBinding = otherSimpleName.resolveBinding();
				if(nodeBinding != null && otherBinding != null && nodeBinding.getKind() == IBinding.VARIABLE && otherBinding.getKind() == IBinding.VARIABLE) {
					IVariableBinding nodeVariableBinding = (IVariableBinding)nodeBinding;
					IVariableBinding otherVariableBinding = (IVariableBinding)otherBinding;
					if(nodeVariableBinding.isField() || otherVariableBinding.isField()) {
						if(!nodeSimpleName.getIdentifier().equals(otherSimpleName.getIdentifier())) {
							Difference diff = new Difference(nodeSimpleName.getIdentifier(),otherSimpleName.getIdentifier(),DifferenceType.VARIABLE_NAME_MISMATCH);
							astNodeDifference.addDifference(diff);
						}
					}
					ITypeBinding nodeTypeBinding = nodeVariableBinding.getType();
					ITypeBinding otherTypeBinding = otherVariableBinding.getType();
					if(nodeTypeBinding != null && otherTypeBinding != null && (!nodeTypeBinding.isEqualTo(otherTypeBinding) || !nodeTypeBinding.getQualifiedName().equals(otherTypeBinding.getQualifiedName()))) {
						Difference diff = new Difference(nodeTypeBinding.getQualifiedName(),otherTypeBinding.getQualifiedName(),DifferenceType.SUBCLASS_TYPE_MISMATCH);
						astNodeDifference.addDifference(diff);
					}
				}
			}
			else {
				safeSubtreeMatch(node, other);
			}
			if(!astNodeDifference.isEmpty())
				addDifference(astNodeDifference);
		}
	}
}
