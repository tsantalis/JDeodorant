package gr.uom.java.jdeodorant.refactoring.manipulators;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;

public class RefactoringUtility {

	public static Type generateQualifiedTypeFromTypeBinding(ITypeBinding typeBinding, AST ast, ASTRewrite rewriter) {
		Type type = null;
		if(typeBinding.isParameterizedType()) {
			type = createQualifiedParameterizedType(ast, typeBinding, rewriter);
		}
		else if(typeBinding.isClass() || typeBinding.isInterface()) {
			if(typeBinding.isMember()) {
				ITypeBinding declaringClassTypeBinding = typeBinding.getDeclaringClass();
				Type declaringClassType = generateQualifiedTypeFromTypeBinding(declaringClassTypeBinding, ast, rewriter);
				type = ast.newQualifiedType(declaringClassType, ast.newSimpleName(typeBinding.getName()));
			}
			else {
				type = ast.newSimpleType(ast.newQualifiedName(ast.newName(typeBinding.getPackage().getName()), ast.newSimpleName(typeBinding.getName())));
			}
		}
		else if(typeBinding.isArray()) {
			ITypeBinding elementTypeBinding = typeBinding.getElementType();
			Type elementType = generateQualifiedTypeFromTypeBinding(elementTypeBinding, ast, rewriter);
			type = ast.newArrayType(elementType, typeBinding.getDimensions());
		}
		return type;
	}

	private static ParameterizedType createQualifiedParameterizedType(AST ast, ITypeBinding typeBinding, ASTRewrite rewriter) {
		ITypeBinding erasure = typeBinding.getErasure();
		ITypeBinding[] typeArguments = typeBinding.getTypeArguments();
		ParameterizedType parameterizedType = ast.newParameterizedType(generateQualifiedTypeFromTypeBinding(erasure, ast, rewriter));
		ListRewrite typeArgumentsRewrite = rewriter.getListRewrite(parameterizedType, ParameterizedType.TYPE_ARGUMENTS_PROPERTY);
		for(ITypeBinding typeArgument : typeArguments) {
			typeArgumentsRewrite.insertLast(generateQualifiedTypeFromTypeBinding(typeArgument, ast, rewriter), null);
		}
		return parameterizedType;
	}

	public static Type generateTypeFromTypeBinding(ITypeBinding typeBinding, AST ast, ASTRewrite rewriter) {
		Type type = null;
		if(typeBinding.isParameterizedType()) {
			type = createParameterizedType(ast, typeBinding, rewriter);
		}
		else if(typeBinding.isClass() || typeBinding.isInterface()) {
			if(typeBinding.isMember()) {
				ITypeBinding declaringClassTypeBinding = typeBinding.getDeclaringClass();
				Type declaringClassType = generateTypeFromTypeBinding(declaringClassTypeBinding, ast, rewriter);
				type = ast.newQualifiedType(declaringClassType, ast.newSimpleName(typeBinding.getName()));
			}
			else {
				type = ast.newSimpleType(ast.newSimpleName(typeBinding.getName()));
			}
		}
		else if(typeBinding.isPrimitive()) {
			String primitiveType = typeBinding.getName();
			if(primitiveType.equals("int"))
				type = ast.newPrimitiveType(PrimitiveType.INT);
			else if(primitiveType.equals("double"))
				type = ast.newPrimitiveType(PrimitiveType.DOUBLE);
			else if(primitiveType.equals("byte"))
				type = ast.newPrimitiveType(PrimitiveType.BYTE);
			else if(primitiveType.equals("short"))
				type = ast.newPrimitiveType(PrimitiveType.SHORT);
			else if(primitiveType.equals("char"))
				type = ast.newPrimitiveType(PrimitiveType.CHAR);
			else if(primitiveType.equals("long"))
				type = ast.newPrimitiveType(PrimitiveType.LONG);
			else if(primitiveType.equals("float"))
				type = ast.newPrimitiveType(PrimitiveType.FLOAT);
			else if(primitiveType.equals("boolean"))
				type = ast.newPrimitiveType(PrimitiveType.BOOLEAN);
		}
		else if(typeBinding.isArray()) {
			ITypeBinding elementTypeBinding = typeBinding.getElementType();
			Type elementType = generateTypeFromTypeBinding(elementTypeBinding, ast, rewriter);
			type = ast.newArrayType(elementType, typeBinding.getDimensions());
		}
		return type;
	}

	private static ParameterizedType createParameterizedType(AST ast, ITypeBinding typeBinding, ASTRewrite rewriter) {
		ITypeBinding erasure = typeBinding.getErasure();
		ITypeBinding[] typeArguments = typeBinding.getTypeArguments();
		ParameterizedType parameterizedType = ast.newParameterizedType(generateTypeFromTypeBinding(erasure, ast, rewriter));
		ListRewrite typeArgumentsRewrite = rewriter.getListRewrite(parameterizedType, ParameterizedType.TYPE_ARGUMENTS_PROPERTY);
		for(ITypeBinding typeArgument : typeArguments) {
			typeArgumentsRewrite.insertLast(generateTypeFromTypeBinding(typeArgument, ast, rewriter), null);
		}
		return parameterizedType;
	}

	public static Type generateWrapperTypeForPrimitiveTypeBinding(ITypeBinding typeBinding, AST ast) {
		Type type = null;
		if(typeBinding.isPrimitive()) {
			String primitiveType = typeBinding.getName();
			if(primitiveType.equals("int"))
				type = ast.newSimpleType(ast.newSimpleName("Integer"));
			else if(primitiveType.equals("double"))
				type = ast.newSimpleType(ast.newSimpleName("Double"));
			else if(primitiveType.equals("byte"))
				type = ast.newSimpleType(ast.newSimpleName("Byte"));
			else if(primitiveType.equals("short"))
				type = ast.newSimpleType(ast.newSimpleName("Short"));
			else if(primitiveType.equals("char"))
				type = ast.newSimpleType(ast.newSimpleName("Character"));
			else if(primitiveType.equals("long"))
				type = ast.newSimpleType(ast.newSimpleName("Long"));
			else if(primitiveType.equals("float"))
				type = ast.newSimpleType(ast.newSimpleName("Float"));
			else if(primitiveType.equals("boolean"))
				type = ast.newSimpleType(ast.newSimpleName("Boolean"));
		}
		return type;
	}

	public static void getSimpleTypeBindings(Set<ITypeBinding> typeBindings, Set<ITypeBinding> finalTypeBindings) {
		for(ITypeBinding typeBinding : typeBindings) {
			if(typeBinding.isPrimitive()) {

			}
			else if(typeBinding.isArray()) {
				ITypeBinding elementTypeBinding = typeBinding.getElementType();
				Set<ITypeBinding> typeBindingList = new LinkedHashSet<ITypeBinding>();
				typeBindingList.add(elementTypeBinding);
				getSimpleTypeBindings(typeBindingList, finalTypeBindings);
			}
			else if(typeBinding.isParameterizedType()) {
				Set<ITypeBinding> typeBindingList = new LinkedHashSet<ITypeBinding>();
				typeBindingList.add(typeBinding.getTypeDeclaration());
				ITypeBinding[] typeArgumentBindings = typeBinding.getTypeArguments();
				for(ITypeBinding typeArgumentBinding : typeArgumentBindings)
					typeBindingList.add(typeArgumentBinding);
				getSimpleTypeBindings(typeBindingList, finalTypeBindings);
			}
			else if(typeBinding.isWildcardType() && typeBinding.getBound() != null) {
				Set<ITypeBinding> typeBindingList = new LinkedHashSet<ITypeBinding>();
				typeBindingList.add(typeBinding.getBound());
				getSimpleTypeBindings(typeBindingList, finalTypeBindings);
			}
			else {
				if(typeBinding.isNested()) {
					if(!containsTypeBinding(typeBinding.getDeclaringClass(), finalTypeBindings))
						finalTypeBindings.add(typeBinding.getDeclaringClass());
				}
				if(!containsTypeBinding(typeBinding, finalTypeBindings))
					finalTypeBindings.add(typeBinding);
			}
		}
	}

	private static boolean containsTypeBinding(ITypeBinding typeBinding, Set<ITypeBinding> typeBindings) {
		for(ITypeBinding typeBinding2 : typeBindings) {
			if(typeBinding2.getKey().equals(typeBinding.getKey()))
				return true;
		}
		return false;
	}

	public static boolean hasQualifiedType(VariableDeclaration variableDeclaration) {
		return isQualifiedType(extractType(variableDeclaration));
	}

	private static Type extractType(VariableDeclaration variableDeclaration) {
		Type returnedVariableType = null;
		if(variableDeclaration instanceof SingleVariableDeclaration) {
			SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)variableDeclaration;
			returnedVariableType = singleVariableDeclaration.getType();
		}
		else if(variableDeclaration instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment)variableDeclaration;
			if(fragment.getParent() instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)fragment.getParent();
				returnedVariableType = variableDeclarationStatement.getType();
			}
			else if(fragment.getParent() instanceof VariableDeclarationExpression) {
				VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)fragment.getParent();
				returnedVariableType = variableDeclarationExpression.getType();
			}
			else if(fragment.getParent() instanceof FieldDeclaration) {
				FieldDeclaration fieldDeclaration = (FieldDeclaration)fragment.getParent();
				returnedVariableType = fieldDeclaration.getType();
			}
		}
		return returnedVariableType;
	}

	private static boolean isQualifiedType(Type type) {
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

	public static VariableDeclaration findFieldDeclaration(AbstractVariable variable, TypeDeclaration typeDeclaration) {
		for(FieldDeclaration fieldDeclaration : typeDeclaration.getFields()) {
			List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				if(variable.getVariableBindingKey().equals(fragment.resolveBinding().getKey())) {
					return fragment;
				}
			}
		}
		//fragment was not found in typeDeclaration
		Type superclassType = typeDeclaration.getSuperclassType();
		if(superclassType != null) {
			String superclassQualifiedName = superclassType.resolveBinding().getQualifiedName();
			SystemObject system = ASTReader.getSystemObject();
			ClassObject superclassObject = system.getClassObject(superclassQualifiedName);
			if(superclassObject != null) {
				AbstractTypeDeclaration superclassTypeDeclaration = superclassObject.getAbstractTypeDeclaration();
				if(superclassTypeDeclaration instanceof TypeDeclaration) {
					return findFieldDeclaration(variable, (TypeDeclaration)superclassTypeDeclaration);
				}
			}
		}
		return null;
	}

	public static TypeDeclaration findDeclaringTypeDeclaration(IVariableBinding variableBinding, TypeDeclaration typeDeclaration) {
		if(typeDeclaration.resolveBinding().isEqualTo(variableBinding.getDeclaringClass())) {
			return typeDeclaration;
		}
		//fragment was not found in typeDeclaration
		Type superclassType = typeDeclaration.getSuperclassType();
		if(superclassType != null) {
			String superclassQualifiedName = superclassType.resolveBinding().getQualifiedName();
			SystemObject system = ASTReader.getSystemObject();
			ClassObject superclassObject = system.getClassObject(superclassQualifiedName);
			if(superclassObject != null) {
				AbstractTypeDeclaration superclassTypeDeclaration = superclassObject.getAbstractTypeDeclaration();
				if(superclassTypeDeclaration instanceof TypeDeclaration) {
					return findDeclaringTypeDeclaration(variableBinding, (TypeDeclaration)superclassTypeDeclaration);
				}
			}
		}
		return null;
	}
}
