package gr.uom.java.ast.decomposition.matching;

import gr.uom.java.ast.decomposition.AbstractExpression;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;

public class ASTNodeDifference {
	private AbstractExpression expression1;
	private AbstractExpression expression2;
	private List<Difference> differences;
	private BindingSignaturePair bindingSignaturePair;
	
	public ASTNodeDifference(AbstractExpression e1, AbstractExpression e2) {
		this.expression1=e1;
		this.expression2=e2;
		this.bindingSignaturePair = new BindingSignaturePair(e1, e2);
		this.differences = new ArrayList<Difference>();
	}

	public List<Difference> getDifferences() {
		return differences;
	}

	public AbstractExpression getExpression1() {
		return expression1;
	}

	public AbstractExpression getExpression2() {
		return expression2;
	}

	public BindingSignaturePair getBindingSignaturePair() {
		return bindingSignaturePair;
	}

	public void addDifference(Difference diff)
	{
		differences.add(diff);
	}

	public boolean containsDifferenceType(DifferenceType type) {
		for(Difference difference : differences)
		{
			if(difference.getType().equals(type))
				return true;
		}
		return false;
	}

	public boolean containsOnlyDifferenceType(DifferenceType type) {
		for(Difference difference : differences)
		{
			if(!difference.getType().equals(type))
				return false;
		}
		if(!differences.isEmpty())
			return true;
		return false;
	}

	public boolean isParameterizable()
	{
		for(Difference difference : differences)
		{
			if(typeMismatch(difference))
				return false;
		}
		return true;
	}
	
	private boolean typeMismatch(Difference diff)
	{
		if(diff.getType().equals(DifferenceType.AST_TYPE_MISMATCH)
			|| (diff.getType().equals(DifferenceType.VARIABLE_TYPE_MISMATCH) && isVariableTypeMismatch())
			|| (diff.getType().equals(DifferenceType.OPERATOR_MISMATCH) && isExpressionOfIfStatementNestedAtLevelZero())
			|| diff.getType().equals(DifferenceType.ANONYMOUS_CLASS_DECLARATION_MISMATCH))
					return true;
		return false;
	}

	private boolean isVariableTypeMismatch() {
		if(isQualifierOfQualifiedName() || isExpressionOfFieldAccess())
			return false;
		return true;
	}

	private boolean isExpressionOfIfStatementNestedAtLevelZero() {
		if(expression1.getExpression().getParent() instanceof IfStatement &&
				expression2.getExpression().getParent() instanceof IfStatement) {
			IfStatement if1 = (IfStatement)expression1.getExpression().getParent();
			IfStatement if2 = (IfStatement)expression2.getExpression().getParent();
			boolean noElsePart = if1.getElseStatement() == null && if2.getElseStatement() == null;
			ASTNode parent1 = if1.getParent();
			while(parent1 instanceof Block) {
				parent1 = parent1.getParent();
			}
			ASTNode parent2 = if2.getParent();
			while(parent2 instanceof Block) {
				parent2 = parent2.getParent();
			}
			if(parent1 instanceof MethodDeclaration && parent2 instanceof MethodDeclaration) {
				return noElsePart;
			}
			if(parent1 instanceof TryStatement && parent2 instanceof TryStatement) {
				TryStatement try1 = (TryStatement)parent1;
				TryStatement try2 = (TryStatement)parent2;
				parent1 = try1.getParent();
				while(parent1 instanceof Block) {
					parent1 = parent1.getParent();
				}
				parent2 = try2.getParent();
				while(parent2 instanceof Block) {
					parent2 = parent2.getParent();
				}
				if(parent1 instanceof MethodDeclaration && parent2 instanceof MethodDeclaration) {
					return noElsePart;
				}
			}
		}
		return false;
	}

	private boolean isQualifierOfQualifiedName() {
		Expression exp1 = expression1.getExpression();
		Expression exp2 = expression2.getExpression();
		ASTNode node1 = exp1.getParent();
		ASTNode node2 = exp2.getParent();
		if(node1 instanceof QualifiedName && node2 instanceof QualifiedName) {
			QualifiedName qual1 = (QualifiedName)node1;
			QualifiedName qual2 = (QualifiedName)node2;
			if(qual1.getQualifier().equals(exp1) && qual2.getQualifier().equals(exp2))
				return true;
		}
		return false;
	}

	private boolean isExpressionOfFieldAccess() {
		Expression exp1 = expression1.getExpression();
		Expression exp2 = expression2.getExpression();
		ASTNode node1 = exp1.getParent();
		ASTNode node2 = exp2.getParent();
		if(node1 instanceof FieldAccess && node2 instanceof FieldAccess) {
			FieldAccess fieldAccess1 = (FieldAccess)node1;
			FieldAccess fieldAccess2 = (FieldAccess)node2;
			if(fieldAccess1.getExpression().equals(exp1) && fieldAccess2.getExpression().equals(exp2))
				return true;
		}
		return false;
	}

	public boolean isEmpty() {
		return differences.isEmpty();
	}
	
	public boolean isParentNodeDifferenceOf(ASTNodeDifference nodeDifference) {
		Expression thisExpression1 = expression1.getExpression();
		Expression thisExpression2 = expression2.getExpression();
		
		thisExpression1 = getParentExpressionOfMethodNameOrTypeName(thisExpression1);
		thisExpression2 = getParentExpressionOfMethodNameOrTypeName(thisExpression2);
		
		Expression otherExpression1 = nodeDifference.expression1.getExpression();
		Expression otherExpression2 = nodeDifference.expression2.getExpression();
		
		if(isParent(thisExpression1, otherExpression1) && isParent(thisExpression2, otherExpression2))
			return true;
		return false;
	}
	
	private boolean isParent(Expression parent, ASTNode child) {
		if(child.getParent().equals(parent))
			return true;
		else if(child.getParent() instanceof Expression) {
			return isParent(parent, (Expression)child.getParent());
		}
		else if(child.getParent() instanceof Type) {
			return isParent(parent, (Type)child.getParent());
		}
		else {
			return false;
		}
	}

	public boolean isLeftHandSideOfAssignment() {
		Expression exp1 = expression1.getExpression();
		Expression exp2 = expression2.getExpression();
		ASTNode node1 = exp1.getParent();
		ASTNode node2 = exp2.getParent();
		if(node1 instanceof Assignment && node2 instanceof Assignment) {
			Assignment assignment1 = (Assignment)node1;
			Assignment assignment2 = (Assignment)node2;
			if(assignment1.getLeftHandSide().equals(exp1) && assignment2.getLeftHandSide().equals(exp2)) {
				return true;
			}
		}
		return false;
	}

	public static Expression getParentExpressionOfMethodNameOrTypeName(Expression expression) {
		if(expression instanceof SimpleName) {
			SimpleName simpleName = (SimpleName)expression;
			IBinding binding = simpleName.resolveBinding();
			if(binding != null) {
				if(binding.getKind() == IBinding.METHOD) {
					if(expression.getParent() instanceof Expression) {
						return (Expression)expression.getParent();
					}
				}
				if(binding.getKind() == IBinding.TYPE) {
					if(expression.getParent() instanceof Type) {
						Type type = (Type)expression.getParent();
						if(type.getParent() instanceof Expression) {
							return (Expression)type.getParent();
						}
					}
				}
				if(binding.getKind() == ITypeBinding.VARIABLE) {
					if(expression.getParent() instanceof QualifiedName) {
						QualifiedName fieldAccess = (QualifiedName)expression.getParent();
						SimpleName fieldName = fieldAccess.getName();
						IBinding fieldNameBinding = fieldName.resolveBinding();
						if(fieldNameBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding fieldNameVariableBinding = (IVariableBinding)fieldNameBinding;
							if(fieldAccess.getQualifier().equals(expression) && fieldNameVariableBinding.isField()) {
								return fieldAccess;
							}
						}
					}
				}
			}
		}
		else if(expression instanceof QualifiedName) {
			QualifiedName qualifiedName = (QualifiedName)expression;
			IBinding binding = qualifiedName.resolveBinding();
			if(binding != null) {
				if(binding.getKind() == IBinding.TYPE) {
					if(qualifiedName.getParent() instanceof Type) {
						Type type = (Type)qualifiedName.getParent();
						if(type.getParent() instanceof Expression) {
							return (Expression)type.getParent();
						}
					}
				}
			}
		}
		return expression;
	}

	public int getWeight() {
		return bindingSignaturePair.getWeight();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((differences == null) ? 0 : differences.hashCode());
		result = prime * result
				+ ((expression1 == null) ? 0 : expression1.hashCode());
		result = prime * result
				+ ((expression2 == null) ? 0 : expression2.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ASTNodeDifference other = (ASTNodeDifference) obj;
		if (differences == null) {
			if (other.differences != null)
				return false;
		} else if (!differences.equals(other.differences))
			return false;
		if (expression1 == null) {
			if (other.expression1 != null)
				return false;
		} else if (!expression1.equals(other.expression1))
			return false;
		if (expression2 == null) {
			if (other.expression2 != null)
				return false;
		} else if (!expression2.equals(other.expression2))
			return false;
		return true;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for(Difference difference : differences)
		{
			sb.append(difference.toString()).append("\n");
		}
		return sb.toString();
	}
}
