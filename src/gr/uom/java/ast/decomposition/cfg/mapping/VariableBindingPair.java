package gr.uom.java.ast.decomposition.cfg.mapping;

import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;

public class VariableBindingPair {
	private IVariableBinding binding1;
	private IVariableBinding binding2;
	private boolean hasQualifiedType = false;
	
	public VariableBindingPair(IVariableBinding binding1, IVariableBinding binding2) {
		this.binding1 = binding1;
		this.binding2 = binding2;
	}
	
	public VariableBindingPair(IVariableBinding binding1, IVariableBinding binding2, Type type) {
		this(binding1, binding2);
		this.hasQualifiedType = isQualifiedType(type);
	}

	private boolean isQualifiedType(Type type) {
		if(type instanceof SimpleType) {
			SimpleType simpleType = (SimpleType)type;
			Name name = simpleType.getName();
			if(name instanceof QualifiedName) {
				return true;
			}
		}
		else if(type instanceof QualifiedType) {
			QualifiedType qualifiedType = (QualifiedType)type;
			Type qualifier = qualifiedType.getQualifier();
			return isQualifiedType(qualifier);
		}
		else if(type instanceof ArrayType) {
			ArrayType arrayType = (ArrayType)type;
			Type elementType = arrayType.getElementType();
			return isQualifiedType(elementType);
		}
		else if(type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType)type;
			Type erasureType = parameterizedType.getType();
			return isQualifiedType(erasureType);
		}
		return false;
	}

	public IVariableBinding getBinding1() {
		return binding1;
	}

	public IVariableBinding getBinding2() {
		return binding2;
	}

	public boolean hasQualifiedType() {
		return hasQualifiedType;
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof VariableBindingPair) {
			VariableBindingPair keyPair = (VariableBindingPair)o;
			return this.binding1.equals(keyPair.binding1) &&
					this.binding2.equals(keyPair.binding2);
		}
		return false;
	}

	public int hashCode() {
		int result = 17;
		result = 37*result + binding1.hashCode();
		result = 37*result + binding2.hashCode();
		return result;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(binding1);
		sb.append("\n");
		sb.append(binding2);
		return sb.toString();
	}
}
