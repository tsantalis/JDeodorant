package gr.uom.java.ast.util;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;

public class TypeVisitor extends ASTVisitor {
	private Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
	
	public boolean visit(SimpleType node) {
		ITypeBinding typeBinding = node.resolveBinding();
		if(typeBinding != null)
			typeBindings.add(typeBinding);
		return super.visit(node);
	}

	public boolean visit(SimpleName node) {
		IBinding binding = node.resolveBinding();
		if(binding != null && binding.getKind() == IBinding.TYPE) {
			ITypeBinding typeBinding = (ITypeBinding)binding;
			typeBindings.add(typeBinding);
		}
		return super.visit(node);
	}

	public Set<ITypeBinding> getTypeBindings() {
		return typeBindings;
	}
}
