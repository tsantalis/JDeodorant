package gr.uom.java.ast.decomposition;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class ASTNodeMatcher extends ASTMatcher{
	
	public boolean isTypeHolder(Object o) {
		if(o.getClass().equals(MethodInvocation.class) || o.getClass().equals(SuperMethodInvocation.class)			
				|| o.getClass().equals(NumberLiteral.class) || o.getClass().equals(StringLiteral.class)
				|| o.getClass().equals(CharacterLiteral.class) || o.getClass().equals(BooleanLiteral.class)
				|| o.getClass().equals(TypeLiteral.class) 
				|| o.getClass().equals(ArrayCreation.class)
				|| o.getClass().equals(ClassInstanceCreation.class)
				|| o.getClass().equals(ArrayAccess.class) || o.getClass().equals(FieldAccess.class) || o.getClass().equals(SuperFieldAccess.class)
				|| o.getClass().equals(SimpleName.class) || o.getClass().equals(QualifiedName.class))
				return true;
		return false;
	}
	
	public ITypeBinding getTypeBinding(Object o) {
		if(o.getClass().equals(MethodInvocation.class)) {
			MethodInvocation methodInvocation = (MethodInvocation) o;
			return methodInvocation.resolveMethodBinding().getReturnType();
		}
		else if(o.getClass().equals(SuperMethodInvocation.class)) {
			SuperMethodInvocation superMethodInvocation = (SuperMethodInvocation) o;
			return superMethodInvocation.resolveMethodBinding().getReturnType();
		}
		else if(o.getClass().equals(NumberLiteral.class)) {
			NumberLiteral numberLiteral = (NumberLiteral) o;
			return numberLiteral.resolveTypeBinding();
		}
		else if(o.getClass().equals(StringLiteral.class)) {
			StringLiteral stringLiteral = (StringLiteral) o;
			return stringLiteral.resolveTypeBinding();
		}
		else if(o.getClass().equals(CharacterLiteral.class)) {
			CharacterLiteral characterLiteral = (CharacterLiteral) o;
			return characterLiteral.resolveTypeBinding();
		}
		else if(o.getClass().equals(BooleanLiteral.class)) {
			BooleanLiteral booleanLiteral = (BooleanLiteral) o;
			return booleanLiteral.resolveTypeBinding();
		}
		else if(o.getClass().equals(TypeLiteral.class)) {
			TypeLiteral typeLiteral = (TypeLiteral) o;
			return typeLiteral.resolveTypeBinding();
		}
		else if(o.getClass().equals(ArrayCreation.class)) {
			ArrayCreation arrayCreation = (ArrayCreation) o;
			return arrayCreation.resolveTypeBinding();
		}
		else if(o.getClass().equals(ClassInstanceCreation.class)) {
			ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) o;
			return classInstanceCreation.resolveTypeBinding();
		}
		else if(o.getClass().equals(ArrayAccess.class)) {
			ArrayAccess arrayAccess = (ArrayAccess) o;
			return arrayAccess.resolveTypeBinding();
		}
		else if(o.getClass().equals(FieldAccess.class)) {
			FieldAccess fieldAccess = (FieldAccess) o;
			return fieldAccess.resolveTypeBinding();
		}
		else if(o.getClass().equals(SuperFieldAccess.class)) {
			SuperFieldAccess superFieldAccess = (SuperFieldAccess) o;
			return superFieldAccess.resolveTypeBinding();
		}
		else if(o.getClass().equals(SimpleName.class)) {
			SimpleName simpleName = (SimpleName) o;
			return simpleName.resolveTypeBinding();
		}
		else if(o.getClass().equals(QualifiedName.class)) {
			QualifiedName qualifiedName = (QualifiedName) o;
			return qualifiedName.resolveTypeBinding();
		}
		return null;
	}
	
	public boolean match(ArrayAccess node, Object other) {
		if(isTypeHolder(other)) {
			return (node.resolveTypeBinding().isEqualTo(getTypeBinding(other)));
		}
		return false;
	}

	public boolean match(ArrayCreation node, Object other) {
		if(isTypeHolder(other)) {
			return (node.resolveTypeBinding().isEqualTo(getTypeBinding(other)));
		}
		return false;
		/*ArrayCreation o = (ArrayCreation) other;
		return (
			safeSubtreeMatch(node.getType(), o.getType())
				&& safeSubtreeListMatch(node.dimensions(), o.dimensions())
				&& safeSubtreeMatch(node.getInitializer(), o.getInitializer())
				);*/
	}
	
	public boolean match(Block node, Object other) {
		if (!(other instanceof Block)) {
			return false;
		}
/*		Block o = (Block) other;
		return safeSubtreeListMatch(node.statements(), o.statements());*/
		return true;
	}

	public boolean match(BooleanLiteral node, Object other) {
		if(isTypeHolder(other)) {
			return (node.resolveTypeBinding().isEqualTo(getTypeBinding(other)));
		}
		return false;
/*		BooleanLiteral o = (BooleanLiteral) other;
		return node.booleanValue() == o.booleanValue();*/
	}

	public boolean match(CharacterLiteral node, Object other) {
		if(isTypeHolder(other)) {
			return (node.resolveTypeBinding().isEqualTo(getTypeBinding(other)));
		}
		return false;
	}

	public boolean match(ClassInstanceCreation node, Object other) {
		if(isTypeHolder(other)) {
			return (node.resolveTypeBinding().isEqualTo(getTypeBinding(other)));
		}
		return false;
		/*ClassInstanceCreation o = (ClassInstanceCreation) other;
		return
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeListMatch(node.arguments(), o.arguments())
				&& safeSubtreeMatch(
					node.getAnonymousClassDeclaration(),
					o.getAnonymousClassDeclaration());*/
	}

	public boolean match(DoStatement node, Object other) {
		if (!(other instanceof DoStatement)) {
			return false;
		}
		DoStatement o = (DoStatement) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				/*&& safeSubtreeMatch(node.getBody(), o.getBody())*/);
	}

	public boolean match(EnhancedForStatement node, Object other) {
		if (!(other instanceof EnhancedForStatement)) {
			return false;
		}
		EnhancedForStatement o = (EnhancedForStatement) other;
		return (
			safeSubtreeMatch(node.getParameter(), o.getParameter())
				&& safeSubtreeMatch(node.getExpression(), o.getExpression())
				/*&& safeSubtreeMatch(node.getBody(), o.getBody())*/);
	}


	public boolean match(FieldAccess node, Object other) {
		if(isTypeHolder(other)) {
			return (node.resolveTypeBinding().isEqualTo(getTypeBinding(other)));
		}
		return false;
	}

	public boolean match(ForStatement node, Object other) {
		if (!(other instanceof ForStatement)) {
			return false;
		}
		ForStatement o = (ForStatement) other;
		return (
			safeSubtreeListMatch(node.initializers(), o.initializers())
				&& safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeListMatch(node.updaters(), o.updaters())
				/*&& safeSubtreeMatch(node.getBody(), o.getBody())*/);
	}

	public boolean match(LabeledStatement node, Object other) {
		if (!(other instanceof LabeledStatement)) {
			return false;
		}
		LabeledStatement o = (LabeledStatement) other;
		return (
			safeSubtreeMatch(node.getLabel(), o.getLabel())
				/*&& safeSubtreeMatch(node.getBody(), o.getBody())*/);
	}

	public boolean match(MethodInvocation node, Object other) {
		if(isTypeHolder(other)) {
			return (node.resolveTypeBinding().isEqualTo(getTypeBinding(other)));
		}
		return false;
	}

	public boolean match(NumberLiteral node, Object other) {
		if(isTypeHolder(other)) {
			return (node.resolveTypeBinding().isEqualTo(getTypeBinding(other)));
		}
		return false;
	}

	public boolean match(QualifiedName node, Object other) {
		if(isTypeHolder(other)) {
			return (node.resolveTypeBinding().isEqualTo(getTypeBinding(other)));
		}
		return false;
	}

	public boolean match(SimpleName node, Object other) {
		if(isTypeHolder(other)) {
			return (node.resolveTypeBinding().isEqualTo(getTypeBinding(other)));
		}
		return false;
	}

	public boolean match(StringLiteral node, Object other) {
		if(isTypeHolder(other)) {
			return (node.resolveTypeBinding().isEqualTo(getTypeBinding(other)));
		}
		return false;
	}

	public boolean match(SuperFieldAccess node, Object other) {
		if(isTypeHolder(other)) {
			return (node.resolveTypeBinding().isEqualTo(getTypeBinding(other)));
		}
		return false;
	}

	public boolean match(SuperMethodInvocation node, Object other) {
		if(isTypeHolder(other)) {
			return (node.resolveTypeBinding().isEqualTo(getTypeBinding(other)));
		}
		return false;
	}

	public boolean match(SwitchStatement node, Object other) {
		if (!(other instanceof SwitchStatement)) {
			return false;
		}
		SwitchStatement o = (SwitchStatement) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				/*&& safeSubtreeListMatch(node.statements(), o.statements())*/);
	}

	public boolean match(SynchronizedStatement node, Object other) {
		if (!(other instanceof SynchronizedStatement)) {
			return false;
		}
		SynchronizedStatement o = (SynchronizedStatement) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				/*&& safeSubtreeMatch(node.getBody(), o.getBody())*/);
	}

	public boolean match(TryStatement node, Object other) {
		if (!(other instanceof TryStatement)) {
			return false;
		}
		TryStatement o = (TryStatement) other;
		return (
			safeSubtreeListMatch(node.resources(), o.resources())
			/*&& safeSubtreeMatch(node.getBody(), o.getBody())*/
			&& safeSubtreeListMatch(node.catchClauses(), o.catchClauses())
			&& safeSubtreeMatch(node.getFinally(), o.getFinally()));
	}

	public boolean match(TypeLiteral node, Object other) {
		if(isTypeHolder(other)) {
			return (node.resolveTypeBinding().isEqualTo(getTypeBinding(other)));
		}
		return false;
	}

	public boolean match(WhileStatement node, Object other) {
		if (!(other instanceof WhileStatement)) {
			return false;
		}
		WhileStatement o = (WhileStatement) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				/*&& safeSubtreeMatch(node.getBody(), o.getBody())*/);
	}
}
