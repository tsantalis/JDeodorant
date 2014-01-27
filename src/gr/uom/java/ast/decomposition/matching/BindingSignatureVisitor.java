package gr.uom.java.ast.decomposition.matching;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WildcardType;

public class BindingSignatureVisitor extends ASTVisitor {
	private List<String> bindingKeys = new ArrayList<String>();
	
	public List<String> getBindingKeys() {
		return bindingKeys;
	}

	public boolean visit(SimpleName expr) {
		IBinding binding = expr.resolveBinding();
		if(binding != null) {
			bindingKeys.add(binding.getKey());
		}
		else {
			bindingKeys.add(expr.toString());
		}
		return false;
	}
	
	public boolean visit(BooleanLiteral expr) {
		bindingKeys.add(String.valueOf(expr.booleanValue()));
		return false;
	}
	
	public boolean visit(CharacterLiteral expr) {
		bindingKeys.add(expr.getEscapedValue());
		return false;
	}
	
	public boolean visit(NullLiteral expr) {
		bindingKeys.add(expr.toString());
		return false;
	}

	public boolean visit(NumberLiteral expr) {
		bindingKeys.add(expr.getToken());
		return false;
	}

	public boolean visit(StringLiteral expr) {
		bindingKeys.add(expr.getEscapedValue());
		return false;
	}

	public boolean visit(TypeLiteral expr) {
		String key;
		if(expr.getType().resolveBinding() != null)
			key = expr.getType().resolveBinding().getKey() + ".class";
		else
			key = expr.toString();
		bindingKeys.add(key);
		return false;
	}

	public boolean visit(InfixExpression expr) {
		handleExpression(expr.getLeftOperand());
		bindingKeys.add(expr.getOperator().toString());
		handleExpression(expr.getRightOperand());
		List extendedOperands = expr.extendedOperands();
		for (int i = 0; i < extendedOperands.size(); i++){
			bindingKeys.add(expr.getOperator().toString());
			handleExpression((Expression) extendedOperands.get(i));
		}
		return false;
	}

	public boolean visit(Assignment expr) {
		handleExpression(expr.getLeftHandSide());
		bindingKeys.add(expr.getOperator().toString());
		handleExpression(expr.getRightHandSide());
		return false;
	}

	public boolean visit(PostfixExpression expr) {
		handleExpression(expr.getOperand());
		bindingKeys.add(expr.getOperator().toString());
		return false;
	}

	public boolean visit(PrefixExpression expr) {
		bindingKeys.add(expr.getOperator().toString());
		handleExpression(expr.getOperand());
		return false;
	}

	public boolean visit(ArrayAccess expr) {
		handleExpression(expr.getArray());
		handleExpression(expr.getIndex());
		return false;
	}

	public boolean visit(ArrayCreation expr) {
		handleType(expr.getType());
		List dimensions = expr.dimensions();
		for (int i = 0; i < dimensions.size(); i++) {
			handleExpression((Expression) dimensions.get(i));
		}
		if(expr.getInitializer() != null) {
			visit(expr.getInitializer());
		}
		return false;
	}

	public boolean visit(ArrayInitializer expr) {
		List expressions = expr.expressions();
		for (int i = 0; i < expressions.size(); i++) {
			handleExpression((Expression) expressions.get(i));
		}
		return false;
	}

	public boolean visit(ArrayType type) {
		handleType(type.getElementType());
		return false;
	}

	public boolean visit(CastExpression expr) {
		handleType(expr.getType());
		handleExpression(expr.getExpression());
		return false;
	}

	public boolean visit(ClassInstanceCreation expr) {
		handleType(expr.getType());
		handleParameters(expr.arguments());
		return false;
	}

	public boolean visit(ConditionalExpression expr) {
		handleExpression(expr.getExpression());
		handleExpression(expr.getThenExpression());
		handleExpression(expr.getElseExpression());
		return false;
	}

	public boolean visit(FieldAccess expr) {
		handleExpression(expr.getExpression());
		handleExpression(expr.getName());
		return false;
	}
	
	public boolean visit(InstanceofExpression expr) {
		handleExpression(expr.getLeftOperand());
		handleType(expr.getRightOperand());
		return false;
	}

	public boolean visit(MethodInvocation expr) {
		if (expr.getExpression() != null) {
			handleExpression(expr.getExpression());
		}
		handleExpression(expr.getName());
		handleParameters(expr.arguments());
		return false;
	}

	public boolean visit(Modifier modifier) {
		bindingKeys.add(modifier.getKeyword().toString());
		return false;
	}

	public boolean visit(ParameterizedType type) {
		handleType(type.getType());
		List typeArguments = type.typeArguments();
		for (int i = 0; i < typeArguments.size(); i++) {
			handleType((Type) typeArguments.get(i));
		}
		return false;
	}

	public boolean visit(ParenthesizedExpression expr) {
		handleExpression(expr.getExpression());
		return false;
	}

	public boolean visit(PrimitiveType type) {
		bindingKeys.add(type.getPrimitiveTypeCode().toString());
		return false;
	}

	public boolean visit(QualifiedName name) {
		if (name.getQualifier() != null) {
			handleExpression(name.getQualifier());
		}
		handleExpression(name.getName());
		return false;
	}

	public boolean visit(QualifiedType type) {
		handleType(type.getQualifier());
		handleExpression(type.getName());
		return false;
	}

	public boolean visit(SimpleType type) {
		ITypeBinding typeBinding = type.resolveBinding();
		if(typeBinding != null)
			bindingKeys.add(typeBinding.getKey());
		else
			bindingKeys.add(type.toString());
		return false;
	}

	public boolean visit(SuperFieldAccess expr) {
		if (expr.getQualifier() != null) {
			handleExpression(expr.getQualifier());
		}
		handleExpression(expr.getName());
		return false;
	}

	public boolean visit(SuperMethodInvocation expr) {
		if (expr.getQualifier() != null) {
			handleExpression(expr.getQualifier());
		}
		List typeArguments = expr.typeArguments();
		for (int i = 0; i < typeArguments.size(); i++) {
			handleType((Type) typeArguments.get(i));
		}
		handleExpression(expr.getName());
		handleParameters(expr.arguments());
		return false;
	}

	public boolean visit(UnionType type) {
		List types = type.types();
		for (int i = 0; i < types.size(); i++){
			handleType((Type) types.get(i));
		}
		return false;
	}

	public boolean visit(VariableDeclarationExpression expr) {
		List modifiers = expr.modifiers();
		for (int i = 0; i < modifiers.size(); i++) {
			visit((Modifier) modifiers.get(i));
		}
		// Append Type
		handleType(expr.getType());
		// Visit Fragments
		List fragments = expr.fragments();
		for (int i = 0; i < fragments.size(); i++) {
			visit((VariableDeclarationFragment) fragments.get(i));
		}
		return false;
	}

	public boolean visit(VariableDeclarationFragment expr) {
		handleExpression(expr.getName());
		if (expr.getInitializer() != null) {
			handleExpression(expr.getInitializer());
		}
		return false;
	}

	public boolean visit(WildcardType type) {
		if(type.getBound() != null) {
			if (type.isUpperBound()) {
				bindingKeys.add("extends");
			} else {
				bindingKeys.add("super");
			}
			handleType(type.getBound());
		}
		return false;
	}

	// Handle expressions and determine which "Visit" to visit
	private void handleExpression(Expression expression) {
		if (expression instanceof ArrayAccess) {
			visit((ArrayAccess) expression);
		} else if (expression instanceof ArrayCreation) {
			visit((ArrayCreation) expression);
		} else if (expression instanceof ArrayInitializer) {
			visit((ArrayInitializer) expression);
		} else if (expression instanceof Assignment) {
			visit((Assignment) expression);
		} else if (expression instanceof BooleanLiteral) {
			visit((BooleanLiteral) expression);
		} else if (expression instanceof CastExpression) {
			visit((CastExpression) expression);
		} else if (expression instanceof CharacterLiteral) {
			visit((CharacterLiteral) expression);
		} else if (expression instanceof ClassInstanceCreation) {
			visit((ClassInstanceCreation) expression);
		} else if (expression instanceof ConditionalExpression) {
			visit((ConditionalExpression) expression);
		} else if (expression instanceof FieldAccess) {
			visit((FieldAccess) expression);
		} else if (expression instanceof InfixExpression) {
			visit((InfixExpression) expression);
		} else if (expression instanceof InstanceofExpression) {
			visit((InstanceofExpression) expression);
		} else if (expression instanceof MethodInvocation) {
			visit((MethodInvocation) expression);
		} else if (expression instanceof NullLiteral) {
			visit((NullLiteral) expression);
		} else if (expression instanceof NumberLiteral) {
			visit((NumberLiteral) expression);
		} else if (expression instanceof ParenthesizedExpression) {
			visit((ParenthesizedExpression) expression);
		} else if (expression instanceof PostfixExpression) {
			visit((PostfixExpression) expression);
		} else if (expression instanceof PrefixExpression) {
			visit((PrefixExpression) expression);
		} else if ((expression instanceof QualifiedName)) {
			visit((QualifiedName) expression);
		} else if (expression instanceof SimpleName) {
			visit((SimpleName) expression);
		} else if (expression instanceof StringLiteral) {
			visit((StringLiteral) expression);
		} else if (expression instanceof SuperFieldAccess) {
			visit((SuperFieldAccess) expression);
		} else if (expression instanceof SuperMethodInvocation) {
			visit((SuperMethodInvocation) expression);
		} else if (expression instanceof ThisExpression) {
			visit((ThisExpression) expression);
		} else if (expression instanceof TypeLiteral) {
			visit((TypeLiteral) expression);
		} else if (expression instanceof VariableDeclarationExpression) {
			visit((VariableDeclarationExpression) expression);
		}
	}

	private void handleType(Type type) {
		if (type instanceof PrimitiveType) {
			visit((PrimitiveType) type);
		} else if (type instanceof ArrayType) {
			visit((ArrayType) type);
		} else if (type instanceof SimpleType) {
			visit((SimpleType) type);
		} else if (type instanceof QualifiedType) {
			visit((QualifiedType) type);
		} else if (type instanceof ParameterizedType) {
			visit((ParameterizedType) type);
		} else if (type instanceof WildcardType) {
			visit((WildcardType) type);
		}
	}

	private void handleParameters(List args) {
		for (int i = 0; i < args.size(); i++) {
			handleExpression((Expression) args.get(i));
		}
	}
}
