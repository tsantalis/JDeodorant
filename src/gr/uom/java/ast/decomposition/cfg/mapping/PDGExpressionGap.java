package gr.uom.java.ast.decomposition.cfg.mapping;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.util.ExpressionExtractor;

public class PDGExpressionGap {
	private ASTNodeDifference difference;
	private Set<VariableBindingPair> parameterBindings;
	
	public PDGExpressionGap(ASTNodeDifference difference) {
		this.difference = difference;
		this.parameterBindings = new LinkedHashSet<VariableBindingPair>();
	}

	public ASTNodeDifference getASTNodeDifference() {
		return difference;
	}

	public Set<VariableBindingPair> getParameterBindings() {
		return parameterBindings;
	}

	public void addParameterBinding(VariableBindingPair parameterBinding) {
		this.parameterBindings.add(parameterBinding);
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
}
