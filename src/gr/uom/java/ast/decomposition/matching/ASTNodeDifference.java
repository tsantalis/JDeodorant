package gr.uom.java.ast.decomposition.matching;

import gr.uom.java.ast.decomposition.AbstractExpression;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
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
			|| diff.getType().equals(DifferenceType.OPERATOR_MISMATCH)
			|| diff.getType().equals(DifferenceType.ANONYMOUS_CLASS_DECLARATION_MISMATCH))
					return true;
		return false;
	}

	private boolean isVariableTypeMismatch() {
		if(isQualifierOfQualifiedName() || isExpressionOfFieldAccess())
			return false;
		return true;
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
			}
		}
		return expression;
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
