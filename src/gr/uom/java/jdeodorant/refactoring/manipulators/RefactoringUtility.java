package gr.uom.java.jdeodorant.refactoring.manipulators;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

public class RefactoringUtility {

	public static Type generateTypeFromTypeBinding(ITypeBinding typeBinding, AST ast, ASTRewrite rewriter) {
		Type type = null;
		if(typeBinding.isParameterizedType()) {
			type = createParameterizedType(ast, typeBinding, rewriter);
		}
		else if(typeBinding.isClass() || typeBinding.isInterface()) {
			type = ast.newSimpleType(ast.newSimpleName(typeBinding.getName()));
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
		ParameterizedType parameterizedType = ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName(erasure.getName())));
		ListRewrite typeArgumentsRewrite = rewriter.getListRewrite(parameterizedType, ParameterizedType.TYPE_ARGUMENTS_PROPERTY);
		for(ITypeBinding typeArgument : typeArguments) {
			if(typeArgument.isParameterizedType()) {
				typeArgumentsRewrite.insertLast(createParameterizedType(ast, typeArgument, rewriter), null);
			}
			else if(typeArgument.isClass() || typeArgument.isInterface()) {
				typeArgumentsRewrite.insertLast(ast.newSimpleType(ast.newSimpleName(typeArgument.getName())), null);
			}
		}
		return parameterizedType;
	}
}
