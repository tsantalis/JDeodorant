package gr.uom.java.ast.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;

public class TypeVisitor extends ASTVisitor {
	private Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
	private List<Type> types = new ArrayList<Type>();
	
	public boolean visit(SimpleType node) {
		ITypeBinding typeBinding = node.resolveBinding();
		if(typeBinding != null)
			typeBindings.add(typeBinding);
		types.add(node);
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

	public List<Type> getTypes() {
		return types;
	}
}
