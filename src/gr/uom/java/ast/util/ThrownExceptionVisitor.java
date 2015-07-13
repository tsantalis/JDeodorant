package gr.uom.java.ast.util;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;

public class ThrownExceptionVisitor extends ASTVisitor {
	private Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();

	public boolean visit(MethodInvocation node) {
		IMethodBinding methodBinding = node.resolveMethodBinding();
		for(ITypeBinding exceptionType : methodBinding.getExceptionTypes()) {
			typeBindings.add(exceptionType);
		}
		return super.visit(node);
	}

	public boolean visit(SuperMethodInvocation node) {
		IMethodBinding methodBinding = node.resolveMethodBinding();
		for(ITypeBinding exceptionType : methodBinding.getExceptionTypes()) {
			typeBindings.add(exceptionType);
		}
		return super.visit(node);
	}

	public boolean visit(ClassInstanceCreation node) {
		IMethodBinding methodBinding = node.resolveConstructorBinding();
		for(ITypeBinding exceptionType : methodBinding.getExceptionTypes()) {
			typeBindings.add(exceptionType);
		}
		return super.visit(node);
	}

	public boolean visit(ThrowStatement node) {
		Expression expression = node.getExpression();
		typeBindings.add(expression.resolveTypeBinding());
		return super.visit(node);
	}

	public Set<ITypeBinding> getTypeBindings() {
		return typeBindings;
	}
}
