package gr.uom.java.ast.decomposition.matching;

import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.MethodDeclarationUtility;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.WhileStatement;

public class ASTNodeMatcher extends ASTMatcher{

	private List<ASTNodeDifference> differences = new ArrayList<ASTNodeDifference>();
	private ITypeRoot typeRoot1;
	private ITypeRoot typeRoot2;
	
	public ASTNodeMatcher(ITypeRoot root1, ITypeRoot root2) {
		this.typeRoot1 = root1;
		this.typeRoot2 = root2;
	}

	public boolean match(PDGNode nodeG1, PDGNode nodeG2) {
		NodePair pair = new NodePair(nodeG1.getId(), nodeG2.getId());
		NodePairComparisonCache cache = NodePairComparisonCache.getInstance();
		if(cache.containsNodePair(pair)) {
			this.differences.addAll(cache.getDifferencesForNodePair(pair));
			return cache.getMatchForNodePair(pair);
		}
		else {
			boolean match = nodeG1.getASTStatement().subtreeMatch(this, nodeG2.getASTStatement());
			cache.addDifferencesForNodePair(pair, this.differences);
			cache.addMatchForNodePair(pair, match);
			return match;
		}
	}

	public List<ASTNodeDifference> getDifferences() {
		return differences;
	}

	public ITypeRoot getTypeRoot1() {
		return typeRoot1;
	}

	public ITypeRoot getTypeRoot2() {
		return typeRoot2;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(ASTNodeDifference diff : differences) {
			sb.append(diff.toString());
		}
		return sb.toString();
	}

	public boolean isParameterizable() {
		if(onlyVariableTypeMismatchDifferences())
			return true;
		else {
			for(ASTNodeDifference diff : differences) {
				if(!diff.isParameterizable())
					return false;
			}
			return true;
		}
	}
	
	private boolean onlyVariableTypeMismatchDifferences() {
		int diffCount = 0;
		int variableTypeMismatchCount = 0;
		for(ASTNodeDifference difference : differences) {
			for(Difference diff : difference.getDifferences()) {
				diffCount++;
				if(diff.getType().equals(DifferenceType.VARIABLE_TYPE_MISMATCH)) {
					variableTypeMismatchCount++;
				}
			}
		}
		if(diffCount > 0 && diffCount == variableTypeMismatchCount)
			return true;
		return false;
	}

	private boolean isTypeHolder(Object o) {
		if(o.getClass().equals(MethodInvocation.class) || o.getClass().equals(SuperMethodInvocation.class)			
				|| o.getClass().equals(NumberLiteral.class) || o.getClass().equals(StringLiteral.class)
				|| o.getClass().equals(CharacterLiteral.class) || o.getClass().equals(BooleanLiteral.class)
				|| o.getClass().equals(TypeLiteral.class) || o.getClass().equals(NullLiteral.class)
				|| o.getClass().equals(ArrayCreation.class)
				|| o.getClass().equals(ClassInstanceCreation.class)
				|| o.getClass().equals(ArrayAccess.class) || o.getClass().equals(FieldAccess.class)
				|| o.getClass().equals(SuperFieldAccess.class) || o.getClass().equals(ParenthesizedExpression.class)
				|| o.getClass().equals(SimpleName.class) || o.getClass().equals(QualifiedName.class)
				|| o.getClass().equals(CastExpression.class) || o.getClass().equals(InfixExpression.class)
				|| o.getClass().equals(PrefixExpression.class)
				|| o.getClass().equals(ThisExpression.class) || o.getClass().equals(ConditionalExpression.class))
			return true;
		return false;
	}

	private ITypeBinding getTypeBinding(Object o) {
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
		else if(o.getClass().equals(CastExpression.class)) {
			CastExpression castExpression = (CastExpression) o;
			return castExpression.resolveTypeBinding();
		}
		else if(o.getClass().equals(InfixExpression.class)) {
			InfixExpression infixExpression = (InfixExpression) o;
			return infixExpression.resolveTypeBinding();
		}
		else if(o.getClass().equals(NullLiteral.class)) {
			NullLiteral nullLiteral = (NullLiteral) o;
			return nullLiteral.resolveTypeBinding();
		}
		else if(o.getClass().equals(ParenthesizedExpression.class)) {
			ParenthesizedExpression expression = (ParenthesizedExpression) o;
			return expression.resolveTypeBinding();
		}
		else if(o.getClass().equals(PrefixExpression.class)) {
			PrefixExpression expression = (PrefixExpression) o;
			return expression.resolveTypeBinding();
		}
		else if(o.getClass().equals(ThisExpression.class)) {
			ThisExpression expression = (ThisExpression) o;
			return expression.resolveTypeBinding();
		}
		else if(o.getClass().equals(ConditionalExpression.class)) {
			ConditionalExpression expression = (ConditionalExpression) o;
			return expression.resolveTypeBinding();
		}
		return null;
	}

	private boolean typeBindingMatch(ITypeBinding binding1, ITypeBinding binding2) {
		//if bindings are both null then they were recovered from SimpleName expressions representing labels
		if(binding1 == null && binding2 == null)
			return true;
		if(binding1.isEqualTo(binding2))
			return true;
		if(binding1.getName().equals("null") && !binding2.isPrimitive()) {
			return true;
		}
		if(binding2.getName().equals("null") && !binding1.isPrimitive()) {
			return true;
		}
		ITypeBinding commonSuperType = commonSuperType(binding1, binding2);
		if(commonSuperType != null && !commonSuperType.getQualifiedName().equals("java.lang.Object"))
			return true;
		return false;
	}

	private ITypeBinding commonSuperType(ITypeBinding typeBinding1, ITypeBinding typeBinding2) {
		Set<ITypeBinding> superTypes1 = getAllSuperTypes(typeBinding1);
		Set<ITypeBinding> superTypes2 = getAllSuperTypes(typeBinding2);
		boolean found = false;
		ITypeBinding commonSuperType = null;
		for(ITypeBinding superType1 : superTypes1) {
			for(ITypeBinding superType2 : superTypes2) {
				if(superType1.isEqualTo(superType2)) {
					commonSuperType = superType1;
					found = true;
					break;
				}
			}
			if(found)
				break;
		}
		return commonSuperType;
	}

	private Set<ITypeBinding> getAllSuperTypes(ITypeBinding typeBinding) {
		Set<ITypeBinding> superTypes = new LinkedHashSet<ITypeBinding>();
		ITypeBinding superTypeBinding = typeBinding.getSuperclass();
		if(superTypeBinding != null) {
			superTypes.add(superTypeBinding);
			superTypes.addAll(getAllSuperTypes(superTypeBinding));
		}
		return superTypes;
	}

	private boolean isInfixExpressionWithCompositeParent(ASTNode node) {
		if(node instanceof InfixExpression &&
				(node.getParent() instanceof IfStatement || node.getParent() instanceof InfixExpression ||
				node.getParent() instanceof WhileStatement || node.getParent() instanceof DoStatement ||
				node.getParent() instanceof ForStatement)) {
			return true;
		}
		return false;
	}

	public boolean match(ArrayAccess node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (!(other instanceof ArrayAccess)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
			}
			else {
				ArrayAccess o = (ArrayAccess) other;
				safeSubtreeMatch(node.getArray(), o.getArray());
				safeSubtreeMatch(node.getIndex(), o.getIndex());
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(ArrayCreation node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (!(other instanceof ArrayCreation)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			else {
				ArrayCreation o = (ArrayCreation) other;
				if(node.dimensions().size() != o.dimensions().size())
				{
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.ARRAY_DIMENSION_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
				ArrayInitializer initializer1 = node.getInitializer();
				ArrayInitializer initializer2 = o.getInitializer();
				if(initializer1 != null && initializer2 != null) {
					List<Expression> expressions1 = initializer1.expressions();
					List<Expression> expressions2 = initializer2.expressions();
					if(expressions1.size() != expressions2.size()) {
						Difference diff = new Difference(initializer1.toString(),initializer2.toString(),
								DifferenceType.ARRAY_INITIALIZER_EXPRESSION_NUMBER_MISMATCH);
						astNodeDifference.addDifference(diff);
					}
				}
				if((initializer1 == null && initializer2 != null) || (initializer1 != null && initializer2 == null)) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.MISSING_ARRAY_INITIALIZER);
					astNodeDifference.addDifference(diff);
				}
				safeSubtreeMatch(node.getType(), o.getType());
				safeSubtreeListMatch(node.dimensions(), o.dimensions());
				safeSubtreeMatch(node.getInitializer(), o.getInitializer());
			}
			if(!astNodeDifference.isEmpty())
				differences.add(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(Assignment node, Object other) {
		if(other instanceof MethodInvocation) {
			if(fieldAssignmentReplacedWithSetter(node, (MethodInvocation)other))
				return true;
		}
		return super.match(node, other);
	}

	public boolean match(Block node, Object other) {
		if (!(other instanceof Block)) {
			return false;
		}
		Block o = (Block)other;
		if(isNestedUnderAnonymousClassDeclaration(node) && isNestedUnderAnonymousClassDeclaration(o)) {
			return super.match(node, o);
		}
		return true;
	}

	public boolean match(BooleanLiteral node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (!(other instanceof BooleanLiteral)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
			}
			else 
			{
				BooleanLiteral o = (BooleanLiteral) other;
				if(node.booleanValue() != o.booleanValue())
				{
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.LITERAL_VALUE_MISMATCH);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(CastExpression node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (!(other instanceof CastExpression)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
			}
			else 
			{
				CastExpression o = (CastExpression) other;
				safeSubtreeMatch(node.getType(), o.getType());
				safeSubtreeMatch(node.getExpression(), o.getExpression());
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(CatchClause node, Object other) {
		if (!(other instanceof CatchClause)) {
			return false;
		}
		CatchClause o = (CatchClause) other;
		return (
			safeSubtreeMatch(node.getException(), o.getException())
				&& safeSubtreeListMatch(node.getBody().statements(), o.getBody().statements()));
	}

	public boolean match(CharacterLiteral node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (!(other instanceof CharacterLiteral)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
			}
			else 
			{
				CharacterLiteral o = (CharacterLiteral) other;
				if(!node.getEscapedValue().equals(o.getEscapedValue()))
				{
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.LITERAL_VALUE_MISMATCH);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(ClassInstanceCreation node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (!(other instanceof ClassInstanceCreation)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			else {
				ClassInstanceCreation o = (ClassInstanceCreation) other;
				if(node.arguments().size() != o.arguments().size()) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.ARGUMENT_NUMBER_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
				boolean anonymousClassDeclarationMatch = safeSubtreeMatch(node.getAnonymousClassDeclaration(),o.getAnonymousClassDeclaration());
				safeSubtreeMatch(node.getType(), o.getType());
				safeSubtreeListMatch(node.arguments(), o.arguments());
				safeSubtreeMatch(node.getExpression(), o.getExpression());
				if(node.getExpression()==null && o.getExpression()!=null) {
					Difference diff = new Difference("",o.getExpression().toString(),DifferenceType.MISSING_METHOD_INVOCATION_EXPRESSION);
					astNodeDifference.addDifference(diff);
				}
				else if(node.getExpression()!=null && o.getExpression()==null) {
					Difference diff = new Difference(node.getExpression().toString(),"",DifferenceType.MISSING_METHOD_INVOCATION_EXPRESSION);
					astNodeDifference.addDifference(diff);
				}
				if(!anonymousClassDeclarationMatch) {
					AnonymousClassDeclaration anounymousClassDeclaration1 = node.getAnonymousClassDeclaration();
					AnonymousClassDeclaration anounymousClassDeclaration2 = o.getAnonymousClassDeclaration();
					if(anounymousClassDeclaration1 == null && anounymousClassDeclaration2 != null) {
						Difference diff = new Difference("",anounymousClassDeclaration2.toString(),
								DifferenceType.ANONYMOUS_CLASS_DECLARATION_MISMATCH);
						astNodeDifference.addDifference(diff);
					}
					else if(anounymousClassDeclaration1 != null && anounymousClassDeclaration2 == null) {
						Difference diff = new Difference(anounymousClassDeclaration1.toString(),"",
								DifferenceType.ANONYMOUS_CLASS_DECLARATION_MISMATCH);
						astNodeDifference.addDifference(diff);
					}
					else if(anounymousClassDeclaration1 != null && anounymousClassDeclaration2 != null) {
						Difference diff = new Difference(anounymousClassDeclaration1.toString(),anounymousClassDeclaration2.toString(),
								DifferenceType.ANONYMOUS_CLASS_DECLARATION_MISMATCH);
						astNodeDifference.addDifference(diff);
					}
				}
			}
			if(!astNodeDifference.isEmpty())
				differences.add(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(ConditionalExpression node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (!(other instanceof ConditionalExpression)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
			}
			else {
				ConditionalExpression o = (ConditionalExpression)other;
				/*if(!node.resolveTypeBinding().isEqualTo(o.resolveTypeBinding()) && typeMatch) {
					Difference diff = new Difference(node.resolveTypeBinding().getName(),o.resolveTypeBinding().getName(),DifferenceType.SUBCLASS_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}*/
				safeSubtreeMatch(node.getExpression(),o.getExpression());
				safeSubtreeMatch(node.getThenExpression(), o.getThenExpression());
				safeSubtreeMatch(node.getElseExpression(), o.getElseExpression());
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(DoStatement node, Object other) {
		if (!(other instanceof DoStatement)) {
			return false;
		}
		DoStatement o = (DoStatement) other;
		if(isNestedUnderAnonymousClassDeclaration(node) && isNestedUnderAnonymousClassDeclaration(o)) {
			return super.match(node, o);
		}
		return (
				safeSubtreeMatch(node.getExpression(), o.getExpression()));
	}

	public boolean match(EnhancedForStatement node, Object other) {
		if (!(other instanceof EnhancedForStatement)) {
			return false;
		}
		EnhancedForStatement o = (EnhancedForStatement) other;
		if(isNestedUnderAnonymousClassDeclaration(node) && isNestedUnderAnonymousClassDeclaration(o)) {
			return super.match(node, o);
		}
		boolean paramMatch = safeSubtreeMatch(node.getParameter(), o.getParameter());
		boolean expMatch = safeSubtreeMatch(node.getExpression(), o.getExpression());
		return paramMatch && expMatch;
	}

	public boolean match(ExpressionStatement node, Object other) {
		if(other instanceof IfStatement) {
			if(ternaryOperatorReplacedWithIfStatement(node, (IfStatement)other))
				return true;
		}
		return super.match(node, other);
	}

	public boolean match(FieldAccess node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (other instanceof FieldAccess) {
				FieldAccess o = (FieldAccess) other;
				if(!node.getName().toString().equals(o.getName().toString())) {
					Difference diff = new Difference(node.getName().toString(),o.getName().toString(),DifferenceType.VARIABLE_NAME_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
				if(!typeMatch) {
					Difference diff = new Difference(node.resolveTypeBinding().getQualifiedName(),o.resolveTypeBinding().getQualifiedName(),DifferenceType.VARIABLE_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
				safeSubtreeMatch(node.getExpression(), o.getExpression());
			}
			else {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			if(!astNodeDifference.isEmpty())
				differences.add(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(ForStatement node, Object other) {
		if (!(other instanceof ForStatement)) {
			return false;
		}
		ForStatement o = (ForStatement) other;
		if(isNestedUnderAnonymousClassDeclaration(node) && isNestedUnderAnonymousClassDeclaration(o)) {
			return super.match(node, o);
		}
		boolean initializerMatch = safeSubtreeListMatch(node.initializers(), o.initializers());
		boolean expMatch = safeSubtreeMatch(node.getExpression(), o.getExpression());
		boolean updaterMatch = safeSubtreeListMatch(node.updaters(), o.updaters());
		return initializerMatch && expMatch && updaterMatch;
	}
	
	public boolean match(IfStatement node, Object other) {
		if (!(other instanceof IfStatement)) {
			if(other instanceof ExpressionStatement) {
				if(ifStatementReplacedWithTernaryOperator(node, (ExpressionStatement)other))
					return true;
			}
			return false;
		}
		IfStatement o = (IfStatement) other;
		if(isNestedUnderAnonymousClassDeclaration(node) && isNestedUnderAnonymousClassDeclaration(o)) {
			return super.match(node, o);
		}
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression()));
	}

	public boolean match(InfixExpression node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent(node) || isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (!(other instanceof InfixExpression)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			else 
			{
				InfixExpression o = (InfixExpression) other;
				// be careful not to trigger lazy creation of extended operand lists
				if (node.hasExtendedOperands() && o.hasExtendedOperands()) {
					if(node.extendedOperands().size() != o.extendedOperands().size()) {
						Difference diff = new Difference(node.toString(),o.toString(),DifferenceType.INFIX_EXTENDED_OPERAND_NUMBER_MISMATCH);
						astNodeDifference.addDifference(diff);
					}
					else {
						safeSubtreeListMatch(node.extendedOperands(), o.extendedOperands());
					}
				}
				if (node.hasExtendedOperands() != o.hasExtendedOperands()) {
					Difference diff = new Difference(node.toString(),o.toString(),DifferenceType.INFIX_EXTENDED_OPERAND_NUMBER_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
				if(!node.getOperator().equals(o.getOperator())) {
					Difference diff = new Difference(node.getOperator().toString(),o.getOperator().toString(),DifferenceType.OPERATOR_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
				boolean leftOperandMatch = safeSubtreeMatch(node.getLeftOperand(), o.getLeftOperand());
				boolean rightOperandMatch = safeSubtreeMatch(node.getRightOperand(), o.getRightOperand());
				if(!leftOperandMatch && !rightOperandMatch) {
					//if both left and right operands do not match, then the entire infix expression should be parameterized
					Difference leftDiff = new Difference(node.getLeftOperand().toString(),o.getLeftOperand().toString(),DifferenceType.INFIX_LEFT_OPERAND_MISMATCH);
					astNodeDifference.addDifference(leftDiff);
					Difference rightDiff = new Difference(node.getRightOperand().toString(),o.getRightOperand().toString(),DifferenceType.INFIX_RIGHT_OPERAND_MISMATCH);
					astNodeDifference.addDifference(rightDiff);
				}
				else if(!leftOperandMatch && rightOperandMatch) {
					//if only the left operand does not match, then the left operand should be parameterized
					if(node.getLeftOperand() instanceof InfixExpression || o.getLeftOperand() instanceof InfixExpression) {
						Difference leftOperandDiff = new Difference(node.getLeftOperand().toString(),o.getLeftOperand().toString(),DifferenceType.INFIX_LEFT_OPERAND_MISMATCH);
						ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
						AbstractExpression leftOp1 = new AbstractExpression(node.getLeftOperand());
						ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
						AbstractExpression leftOp2 = new AbstractExpression(o.getLeftOperand());
						ASTNodeDifference astLeftOperandDifference = new ASTNodeDifference(leftOp1, leftOp2);
						astLeftOperandDifference.addDifference(leftOperandDiff);
						differences.add(astLeftOperandDifference);
					}
				}
				else if(leftOperandMatch && !rightOperandMatch) {
					//if only the right operand does not match, then the right operand should be parameterized
					if(node.getRightOperand() instanceof InfixExpression || o.getRightOperand() instanceof InfixExpression) {
						Difference rightOperandDiff = new Difference(node.getRightOperand().toString(),o.getRightOperand().toString(),DifferenceType.INFIX_RIGHT_OPERAND_MISMATCH);
						ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
						AbstractExpression rightOp1 = new AbstractExpression(node.getRightOperand());
						ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
						AbstractExpression rightOp2 = new AbstractExpression(o.getRightOperand());
						ASTNodeDifference astRightOperandDifference = new ASTNodeDifference(rightOp1, rightOp2);
						astRightOperandDifference.addDifference(rightOperandDiff);
						differences.add(astRightOperandDifference);
					}
				}
			}
			if(!astNodeDifference.isEmpty())
				differences.add(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(LabeledStatement node, Object other) {
		if (!(other instanceof LabeledStatement)) {
			return false;
		}
		LabeledStatement o = (LabeledStatement) other;
		if(isNestedUnderAnonymousClassDeclaration(node) && isNestedUnderAnonymousClassDeclaration(o)) {
			return super.match(node, o);
		}
		return (
				safeSubtreeMatch(node.getLabel(), o.getLabel()));
	}

	public boolean match(MethodInvocation node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		if(other instanceof Assignment) {
			if(setterReplacedWithFieldAssignment(node, (Assignment)other)) {
				return true;
			}
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveMethodBinding().getReturnType(), getTypeBinding(other));
			if (!(other instanceof MethodInvocation)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			else {
				MethodInvocation o = (MethodInvocation) other;
				if(node.arguments().size() != o.arguments().size()) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.ARGUMENT_NUMBER_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
				safeSubtreeMatch(node.getName(), o.getName());
				safeSubtreeListMatch(node.arguments(), o.arguments());
				safeSubtreeMatch(node.getExpression(), o.getExpression());
				if(node.getExpression()==null && o.getExpression()!=null) {
					Difference diff = new Difference("",o.getExpression().toString(),DifferenceType.MISSING_METHOD_INVOCATION_EXPRESSION);
					astNodeDifference.addDifference(diff);
				}
				else if(node.getExpression()!=null && o.getExpression()==null) {
					Difference diff = new Difference(node.getExpression().toString(),"",DifferenceType.MISSING_METHOD_INVOCATION_EXPRESSION);
					astNodeDifference.addDifference(diff);
				}
			}
			if(!astNodeDifference.isEmpty()) 
				differences.add(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(NullLiteral node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (!(other instanceof NullLiteral)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
			}
			else {
				return true;
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(NumberLiteral node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (!(other instanceof NumberLiteral)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
			}
			else 
			{
				NumberLiteral o = (NumberLiteral) other;
				if(!node.getToken().equals(o.getToken()))
				{
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.LITERAL_VALUE_MISMATCH);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(ParenthesizedExpression node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (!(other instanceof ParenthesizedExpression)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
			}
			else {
				ParenthesizedExpression o = (ParenthesizedExpression) other;
				safeSubtreeMatch(node.getExpression(), o.getExpression());
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(PrefixExpression node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (!(other instanceof PrefixExpression)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
			}
			else {
				PrefixExpression o = (PrefixExpression) other;
				if(!node.getOperator().equals(o.getOperator())) {
					Difference diff = new Difference(node.getOperator().toString(),o.getOperator().toString(),DifferenceType.OPERATOR_MISMATCH);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
				safeSubtreeMatch(node.getOperand(), o.getOperand());
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(QualifiedName node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch;
			if(node.resolveTypeBinding() == null || getTypeBinding(other) == null)
			{
				if (other instanceof QualifiedName) {
					QualifiedName o = (QualifiedName) other;
					if(!node.getName().toString().equals(o.getName().toString())) {
						Difference diff = new Difference(node.getName().toString(),o.getName().toString(),DifferenceType.VARIABLE_NAME_MISMATCH);
						astNodeDifference.addDifference(diff);
					}
				}
				typeMatch = true;
			}
			else {
				typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
				if (other instanceof QualifiedName) {
					QualifiedName o = (QualifiedName) other;
					if(!node.getName().toString().equals(o.getName().toString())) {
						Difference diff = new Difference(node.getName().toString(),o.getName().toString(),DifferenceType.VARIABLE_NAME_MISMATCH);
						astNodeDifference.addDifference(diff);
					}
					if(!typeMatch) {
						Difference diff = new Difference(node.resolveTypeBinding().getQualifiedName(),o.resolveTypeBinding().getQualifiedName(),DifferenceType.VARIABLE_TYPE_MISMATCH);
						astNodeDifference.addDifference(diff);
					}
					safeSubtreeMatch(node.getQualifier(), o.getQualifier());
				}
				else {
					if(typeMatch) {
						Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
						astNodeDifference.addDifference(diff);
					}
					else {
						Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
						astNodeDifference.addDifference(diff);
					}
				}
			}
			if(!astNodeDifference.isEmpty()) 
				differences.add(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(SimpleName node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (other instanceof SimpleName) {
				SimpleName o = (SimpleName) other;
				IBinding nodeBinding = node.resolveBinding();
				IBinding otherBinding = o.resolveBinding();
				if(nodeBinding != null && otherBinding != null) {
					if(nodeBinding.getKind() == IBinding.METHOD && otherBinding.getKind() == IBinding.METHOD) {
						if(!node.getIdentifier().equals(o.getIdentifier())) {
							Difference diff = new Difference(node.getIdentifier(),o.getIdentifier(),DifferenceType.METHOD_INVOCATION_NAME_MISMATCH);
							astNodeDifference.addDifference(diff);
						}
					}
					else if(nodeBinding.getKind() == IBinding.TYPE && otherBinding.getKind() == IBinding.TYPE) {
						ITypeBinding nodeTypeBinding = (ITypeBinding)nodeBinding;
						ITypeBinding otherTypeBinding = (ITypeBinding)otherBinding;
						if(!nodeTypeBinding.isEqualTo(otherTypeBinding) && typeMatch) {
							Difference diff = new Difference(nodeTypeBinding.getQualifiedName(),
									otherTypeBinding.getQualifiedName(),DifferenceType.SUBCLASS_TYPE_MISMATCH);
							astNodeDifference.addDifference(diff);
						}
					}
					else if(nodeBinding.getKind() == IBinding.VARIABLE && otherBinding.getKind() == IBinding.VARIABLE) {
						if(!node.getIdentifier().equals(o.getIdentifier())) {
							Difference diff = new Difference(node.getIdentifier(),o.getIdentifier(),DifferenceType.VARIABLE_NAME_MISMATCH);
							astNodeDifference.addDifference(diff);
						}
					}
				}
				if(!typeMatch) {
					Difference diff = new Difference(node.resolveTypeBinding().getQualifiedName(),o.resolveTypeBinding().getQualifiedName(),DifferenceType.VARIABLE_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
				else {
					if(nodeBinding != null && otherBinding != null && nodeBinding.getKind() == IBinding.VARIABLE && otherBinding.getKind() == IBinding.VARIABLE) {
						IVariableBinding nodeVariableBinding = (IVariableBinding)nodeBinding;
						IVariableBinding otherVariableBinding = (IVariableBinding)otherBinding;
						ITypeBinding nodeTypeBinding = nodeVariableBinding.getType();
						ITypeBinding otherTypeBinding = otherVariableBinding.getType();
						if(nodeTypeBinding != null && otherTypeBinding != null && !nodeTypeBinding.isEqualTo(otherTypeBinding)) {
							Difference diff = new Difference(nodeTypeBinding.getQualifiedName(),otherTypeBinding.getQualifiedName(),DifferenceType.SUBCLASS_TYPE_MISMATCH);
							astNodeDifference.addDifference(diff);
						}
					}
				}
			}
			else {
				if(typeMatch) {
					Difference diff = new Difference(node.getIdentifier(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
				}
				else {
					Difference diff = new Difference(node.getIdentifier(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			if(!astNodeDifference.isEmpty()) 
				differences.add(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(node.getIdentifier(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(StringLiteral node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (!(other instanceof StringLiteral)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
			}
			else 
			{
				StringLiteral o = (StringLiteral) other;
				if(!node.getLiteralValue().equals(o.getLiteralValue()))
				{
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.LITERAL_VALUE_MISMATCH);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(SuperFieldAccess node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (other instanceof SuperFieldAccess) {
				SuperFieldAccess o = (SuperFieldAccess) other;
				if(!node.getName().toString().equals(o.getName().toString())) {
					Difference diff = new Difference(node.getName().toString(),o.getName().toString(),DifferenceType.VARIABLE_NAME_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
				if(!typeMatch) {
					Difference diff = new Difference(node.resolveTypeBinding().getQualifiedName(),o.resolveTypeBinding().getQualifiedName(),DifferenceType.VARIABLE_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			else {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			if(!astNodeDifference.isEmpty()) 
				differences.add(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(SuperMethodInvocation node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveMethodBinding().getReturnType(), getTypeBinding(other));
			if (!(other instanceof SuperMethodInvocation)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
			}
			else {
				SuperMethodInvocation o = (SuperMethodInvocation) other;
				if(node.arguments().size() != o.arguments().size()) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.ARGUMENT_NUMBER_MISMATCH);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
				safeSubtreeMatch(node.getName(), o.getName());
				safeSubtreeListMatch(node.arguments(), o.arguments());
				safeSubtreeMatch(node.getQualifier(), o.getQualifier());
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(SwitchStatement node, Object other) {
		if (!(other instanceof SwitchStatement)) {
			return false;
		}
		SwitchStatement o = (SwitchStatement) other;
		if(isNestedUnderAnonymousClassDeclaration(node) && isNestedUnderAnonymousClassDeclaration(o)) {
			return super.match(node, o);
		}
		return (
				safeSubtreeMatch(node.getExpression(), o.getExpression()));
	}

	public boolean match(SynchronizedStatement node, Object other) {
		if (!(other instanceof SynchronizedStatement)) {
			return false;
		}
		SynchronizedStatement o = (SynchronizedStatement) other;
		if(isNestedUnderAnonymousClassDeclaration(node) && isNestedUnderAnonymousClassDeclaration(o)) {
			return super.match(node, o);
		}
		return (
				safeSubtreeMatch(node.getExpression(), o.getExpression()));
	}

	public boolean match(ThisExpression node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (!(other instanceof ThisExpression)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
				}
			}
			else {
				ThisExpression o = (ThisExpression) other;
				safeSubtreeMatch(node.getQualifier(), o.getQualifier());
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		differences.add(astNodeDifference);
		return false;
	}

	public boolean match(TryStatement node, Object other) {
		if (!(other instanceof TryStatement)) {
			return false;
		}
		TryStatement o = (TryStatement) other;
		if(isNestedUnderAnonymousClassDeclaration(node) && isNestedUnderAnonymousClassDeclaration(o)) {
			return super.match(node, o);
		}
		boolean resourceMatch = safeSubtreeListMatch(node.resources(), o.resources());
		boolean catchClauseMatch = safeSubtreeListMatch(node.catchClauses(), o.catchClauses());
		boolean finallyMatch;
		if(node.getFinally() == null && o.getFinally() == null)
			finallyMatch = true;
		else if(node.getFinally() != null && o.getFinally() != null)
			finallyMatch = safeSubtreeListMatch(node.getFinally().statements(), o.getFinally().statements());
		else
			finallyMatch = false;
		return resourceMatch && catchClauseMatch && finallyMatch;
	}

	public boolean match(TypeLiteral node, Object other) {
		if(isTypeHolder(other)) {
			return typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
		}
		return false;
	}

	public boolean match(WhileStatement node, Object other) {
		if (!(other instanceof WhileStatement)) {
			return false;
		}
		WhileStatement o = (WhileStatement) other;
		if(isNestedUnderAnonymousClassDeclaration(node) && isNestedUnderAnonymousClassDeclaration(o)) {
			return super.match(node, o);
		}
		return (
				safeSubtreeMatch(node.getExpression(), o.getExpression()));
	}
	
	private boolean setterReplacedWithFieldAssignment(MethodInvocation setter, Assignment assignment) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(setter);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression(assignment);
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		
		Expression leftHandSide = assignment.getLeftHandSide();
		Expression rightHandSide = assignment.getRightHandSide();
		List arguments = setter.arguments();
		if(arguments.size() == 1) {
			if(leftHandSide instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)leftHandSide;
				boolean argumentRightHandSideMatch = safeSubtreeMatch(arguments.get(0), rightHandSide);
				if(setterMethodForField(setter, fieldAccess.getName()) && argumentRightHandSideMatch) {
					Difference diff = new Difference(setter.toString(),assignment.toString(),DifferenceType.FIELD_ASSIGNMENT_REPLACED_WITH_SETTER);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
					return true;
				}
			}
			else if(leftHandSide instanceof SimpleName) {
				SimpleName simpleName = (SimpleName)leftHandSide;
				boolean argumentRightHandSideMatch = safeSubtreeMatch(arguments.get(0), rightHandSide);
				if(setterMethodForField(setter, simpleName) && argumentRightHandSideMatch) {
					Difference diff = new Difference(setter.toString(),assignment.toString(),DifferenceType.FIELD_ASSIGNMENT_REPLACED_WITH_SETTER);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean fieldAssignmentReplacedWithSetter(Assignment assignment, MethodInvocation setter) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(assignment);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression(setter);
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		
		Expression leftHandSide = assignment.getLeftHandSide();
		Expression rightHandSide = assignment.getRightHandSide();
		List arguments = setter.arguments();
		if(arguments.size() == 1) {
			if(leftHandSide instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)leftHandSide;
				boolean argumentRightHandSideMatch = safeSubtreeMatch(rightHandSide, arguments.get(0));
				if(setterMethodForField(setter, fieldAccess.getName()) && argumentRightHandSideMatch) {
					Difference diff = new Difference(assignment.toString(),setter.toString(),DifferenceType.FIELD_ASSIGNMENT_REPLACED_WITH_SETTER);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
					return true;
				}
			}
			else if(leftHandSide instanceof SimpleName) {
				SimpleName simpleName = (SimpleName)leftHandSide;
				boolean argumentRightHandSideMatch = safeSubtreeMatch(rightHandSide, arguments.get(0));
				if(setterMethodForField(setter, simpleName) && argumentRightHandSideMatch) {
					Difference diff = new Difference(assignment.toString(),setter.toString(),DifferenceType.FIELD_ASSIGNMENT_REPLACED_WITH_SETTER);
					astNodeDifference.addDifference(diff);
					differences.add(astNodeDifference);
					return true;
				}
			}
		}
		return false;
	}

	private boolean setterMethodForField(MethodInvocation methodInvocation, SimpleName fieldName) {
		IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
		ITypeBinding declaringClassTypeBinding = methodBinding.getDeclaringClass();
		ClassObject declaringClass = ASTReader.getSystemObject().getClassObject(declaringClassTypeBinding.getQualifiedName());
		if(declaringClass != null) {
			ListIterator<MethodObject> methodIterator = declaringClass.getMethodIterator();
			while(methodIterator.hasNext()) {
				MethodObject method = methodIterator.next();
				MethodDeclaration methodDeclaration = method.getMethodDeclaration();
				if(methodDeclaration.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
					SimpleName setField = MethodDeclarationUtility.isSetter(methodDeclaration);
					if(setField != null) {
						if(setField.resolveBinding().getKind() == IBinding.VARIABLE &&
								fieldName.resolveBinding().getKind() == IBinding.VARIABLE) {
							IVariableBinding setFieldBinding = (IVariableBinding)setField.resolveBinding();
							IVariableBinding fieldNameBinding = (IVariableBinding)fieldName.resolveBinding();
							if(setFieldBinding.isEqualTo(fieldNameBinding) ||
									(setField.getIdentifier().equals(fieldName.getIdentifier()) &&
									setFieldBinding.getType().isEqualTo(fieldNameBinding.getType()))) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private boolean ifStatementReplacedWithTernaryOperator(IfStatement ifStatement, ExpressionStatement expressionStatement) {
		Expression ifExpression = ifStatement.getExpression();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> conditionalExpressions = expressionExtractor.getConditionalExpressions(expressionStatement);
		if(conditionalExpressions.size() == 1) {
			ConditionalExpression conditionalExpression = (ConditionalExpression)conditionalExpressions.get(0);
			boolean match = safeSubtreeMatch(ifExpression, conditionalExpression.getExpression());
			if(match) {
				//the ifStatement should have an else part, which is not an 'else if' statement
				if(ifStatement.getElseStatement() != null && !(ifStatement.getElseStatement() instanceof IfStatement)) {
					Statement thenStatement = ifStatement.getThenStatement();
					Statement elseStatement = ifStatement.getElseStatement();
					ExpressionStatement thenExpressionStatement = isExpressionStatement(thenStatement);
					ExpressionStatement elseExpressionStatement = isExpressionStatement(elseStatement);
					if(thenExpressionStatement != null && elseExpressionStatement != null) {
						//check whether thenExpression, elseExpression, and conditionalExpression have the same ASTNode type
						int thenExpressionType = thenExpressionStatement.getExpression().getNodeType();
						int elseExpressionType = elseExpressionStatement.getExpression().getNodeType();
						int conditionalExpressionType = expressionStatement.getExpression().getNodeType();
						if(thenExpressionType == elseExpressionType && thenExpressionType == conditionalExpressionType) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private ExpressionStatement isExpressionStatement(Statement statement) {
		ExpressionStatement expressionStatement = null;
		if(statement instanceof ExpressionStatement) {
			expressionStatement = (ExpressionStatement)statement;
		}
		else if(statement instanceof Block) {
			Block block = (Block)statement;
			List<Statement> blockStatements = block.statements();
			if(blockStatements.size() == 1 && blockStatements.get(0) instanceof ExpressionStatement) {
				expressionStatement = (ExpressionStatement)blockStatements.get(0);
			}
		}
		return expressionStatement;
	}

	private boolean ternaryOperatorReplacedWithIfStatement(ExpressionStatement expressionStatement, IfStatement ifStatement) {
		Expression ifExpression = ifStatement.getExpression();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> conditionalExpressions = expressionExtractor.getConditionalExpressions(expressionStatement);
		if(conditionalExpressions.size() == 1) {
			ConditionalExpression conditionalExpression = (ConditionalExpression)conditionalExpressions.get(0);
			boolean match = safeSubtreeMatch(conditionalExpression.getExpression(), ifExpression);
			if(match) {
				//the ifStatement should have an else part, which is not an 'else if' statement
				if(ifStatement.getElseStatement() != null && !(ifStatement.getElseStatement() instanceof IfStatement)) {
					Statement thenStatement = ifStatement.getThenStatement();
					Statement elseStatement = ifStatement.getElseStatement();
					ExpressionStatement thenExpressionStatement = isExpressionStatement(thenStatement);
					ExpressionStatement elseExpressionStatement = isExpressionStatement(elseStatement);
					if(thenExpressionStatement != null && elseExpressionStatement != null) {
						//check whether thenExpression, elseExpression, and conditionalExpression have the same ASTNode type
						int thenExpressionType = thenExpressionStatement.getExpression().getNodeType();
						int elseExpressionType = elseExpressionStatement.getExpression().getNodeType();
						int conditionalExpressionType = expressionStatement.getExpression().getNodeType();
						if(thenExpressionType == elseExpressionType && thenExpressionType == conditionalExpressionType) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	private boolean isNestedUnderAnonymousClassDeclaration(ASTNode node) {
		ASTNode parent = node.getParent();
		while(parent != null) {
			if(parent instanceof AnonymousClassDeclaration || parent instanceof CatchClause ||
					isFinallyBlockOfTryStatement(parent)) {
				return true;
			}
			parent = parent.getParent();
		}
		return false;
	}
	
	private boolean isFinallyBlockOfTryStatement(ASTNode node) {
		ASTNode parent = node.getParent();
		if(parent != null && parent instanceof TryStatement) {
			TryStatement tryStatement = (TryStatement)parent;
			Block finallyBlock = tryStatement.getFinally();
			if(node instanceof Block && finallyBlock != null) {
				return finallyBlock.equals((Block)node);
			}
		}
		return false;
	}
}
