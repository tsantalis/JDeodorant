package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.decomposition.matching.ASTNodeMatcher;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.ThrownExceptionVisitor;

public class PDGExpressionGap extends Gap {
	private ASTNodeDifference difference;
	
	public PDGExpressionGap(ASTNodeDifference difference) {
		this.difference = difference;
	}

	public ASTNodeDifference getASTNodeDifference() {
		return difference;
	}

	public Set<IVariableBinding> getUsedVariableBindingsG1() {
		return getUsedVariableBindings(difference.getExpression1());
	}

	public Set<IVariableBinding> getUsedVariableBindingsG2() {
		return getUsedVariableBindings(difference.getExpression2());
	}

	private Set<IVariableBinding> getUsedVariableBindings(AbstractExpression expression) {
		Expression expr = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(expression.getExpression());
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> localVariableInstructions = expressionExtractor.getVariableInstructions(expr);
		Set<IVariableBinding> usedVariableBindings = new LinkedHashSet<IVariableBinding>();
		for(Expression variableInstruction : localVariableInstructions) {
			SimpleName simpleName = (SimpleName)variableInstruction;
			IBinding binding = simpleName.resolveBinding();
			if(binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding) binding;
				if(!variableBinding.isField() && !simpleName.isDeclaration())
					usedVariableBindings.add(variableBinding);
			}
		}
		return usedVariableBindings;
	}

	public ITypeBinding getReturnType() {
		ITypeBinding typeBinding1 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(difference.getExpression1().getExpression()).resolveTypeBinding();
		ITypeBinding typeBinding2 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(difference.getExpression2().getExpression()).resolveTypeBinding();
		if(typeBinding1 != null && typeBinding2 != null) {
			if(typeBinding1.getQualifiedName().equals("null") && !typeBinding2.getQualifiedName().equals("null")) {
				return typeBinding2;
			}
			else if(typeBinding2.getQualifiedName().equals("null") && !typeBinding1.getQualifiedName().equals("null")) {
				return typeBinding1;
			}
			else if(typeBinding1.isEqualTo(typeBinding2)) {
				return typeBinding1;
			}
			else {
				ITypeBinding typeBinding = ASTNodeMatcher.commonSuperType(typeBinding1, typeBinding2);
				return typeBinding;
			}
		}
		else if(typeBinding1 == null && typeBinding2 != null) {
			return typeBinding2;
		}
		else if(typeBinding2 == null && typeBinding1 != null) {
			return typeBinding1;
		}
		return null;
	}

	public Set<ITypeBinding> getThrownExceptions() {
		Set<ITypeBinding> thrownExceptionTypeBindings = new LinkedHashSet<ITypeBinding>();
		Expression expr1 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(difference.getExpression1().getExpression());
		Expression expr2 = ASTNodeDifference.getParentExpressionOfMethodNameOrTypeName(difference.getExpression2().getExpression());
		ThrownExceptionVisitor thrownExceptionVisitor = new ThrownExceptionVisitor();
		expr1.accept(thrownExceptionVisitor);
		for(ITypeBinding typeBinding : thrownExceptionVisitor.getTypeBindings()) {
			addTypeBinding(typeBinding, thrownExceptionTypeBindings);
		}
		thrownExceptionVisitor = new ThrownExceptionVisitor();
		expr2.accept(thrownExceptionVisitor);
		for(ITypeBinding typeBinding : thrownExceptionVisitor.getTypeBindings()) {
			addTypeBinding(typeBinding, thrownExceptionTypeBindings);
		}
		return thrownExceptionTypeBindings;
	}
}
