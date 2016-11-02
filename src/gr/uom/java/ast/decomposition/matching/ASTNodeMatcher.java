package gr.uom.java.ast.decomposition.matching;

import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.AbstractMethodFragment;
import gr.uom.java.ast.decomposition.StatementObject;
import gr.uom.java.ast.decomposition.StatementType;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.decomposition.matching.conditional.AbstractControlStructure;
import gr.uom.java.ast.decomposition.matching.conditional.AbstractControlStructureUtilities;
import gr.uom.java.ast.decomposition.matching.conditional.IfControlStructure;
import gr.uom.java.ast.decomposition.matching.conditional.SwitchControlStructure;
import gr.uom.java.ast.decomposition.matching.conditional.TernaryControlStructure;
import gr.uom.java.ast.decomposition.matching.loop.AbstractControlVariable;
import gr.uom.java.ast.decomposition.matching.loop.AbstractLoop;
import gr.uom.java.ast.decomposition.matching.loop.AbstractLoopUtilities;
import gr.uom.java.ast.decomposition.matching.loop.ConditionalLoop;
import gr.uom.java.ast.decomposition.matching.loop.ConditionalLoopASTNodeMatcher;
import gr.uom.java.ast.decomposition.matching.loop.ControlVariable;
import gr.uom.java.ast.decomposition.matching.loop.EnhancedForLoop;
import gr.uom.java.ast.inheritance.TypeBindingInheritanceDetection;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.MethodDeclarationUtility;
import gr.uom.java.ast.util.math.LevenshteinDistance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
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
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class ASTNodeMatcher extends ASTMatcher{

	private List<ASTNodeDifference> differences;
	private ITypeRoot typeRoot1;
	private ITypeRoot typeRoot2;
	private List<AbstractMethodFragment> additionallyMatchedFragments1;
	private List<AbstractMethodFragment> additionallyMatchedFragments2;
	
	public ASTNodeMatcher(ITypeRoot root1, ITypeRoot root2) {
		this.differences = new ArrayList<ASTNodeDifference>();
		this.typeRoot1 = root1;
		this.typeRoot2 = root2;
		this.additionallyMatchedFragments1 = new ArrayList<AbstractMethodFragment>();
		this.additionallyMatchedFragments2 = new ArrayList<AbstractMethodFragment>();
	}

	public boolean match(PDGNode nodeG1, PDGNode nodeG2) {
		NodePair pair = new NodePair(nodeG1.getId(), nodeG2.getId());
		NodePairComparisonCache cache = NodePairComparisonCache.getInstance();
		if(cache.containsNodePair(pair)) {
			this.differences.addAll(cache.getDifferencesForNodePair(pair));
			this.additionallyMatchedFragments1.addAll(cache.getAdditionallyMatchedFragments1(pair));
			this.additionallyMatchedFragments2.addAll(cache.getAdditionallyMatchedFragments2(pair));
			return cache.getMatchForNodePair(pair);
		}
		else {
			boolean match = nodeG1.getASTStatement().subtreeMatch(this, nodeG2.getASTStatement());
			cache.addDifferencesForNodePair(pair, this.differences);
			cache.addMatchForNodePair(pair, match);
			cache.setAdditionallyMatchedFragments1(pair, this.additionallyMatchedFragments1);
			cache.setAdditionallyMatchedFragments2(pair, this.additionallyMatchedFragments2);
			return match;
		}
	}

	protected void addDifference(ASTNodeDifference difference) {
		if(!differences.contains(difference))
			differences.add(difference);
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

	public List<AbstractMethodFragment> getAdditionallyMatchedFragments1() {
		return additionallyMatchedFragments1;
	}

	public List<AbstractMethodFragment> getAdditionallyMatchedFragments2() {
		return additionallyMatchedFragments2;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(ASTNodeDifference diff : differences) {
			sb.append(diff.toString());
		}
		return sb.toString();
	}

	public boolean isParameterizable() {
		if(onlyVariableTypeMismatchDifferences() || additionallyMatchedFragments1.size() > 0 || additionallyMatchedFragments2.size() > 0)
			return true;
		else if(methodInvocationMatchWithMissingExpressionAndDifferentNameAndDifferentArguments())
			return false;
		else {
			for(ASTNodeDifference diff : differences) {
				boolean expression1NestedUnderCatchClause = isNestedUnderAnonymousClassDeclaration(diff.getExpression1().getExpression());
				boolean expression2NestedUnderCatchClause = isNestedUnderAnonymousClassDeclaration(diff.getExpression2().getExpression());
				if(!diff.isParameterizable() && !expression1NestedUnderCatchClause && !expression2NestedUnderCatchClause)
					return false;
			}
			return true;
		}
	}

	private boolean methodInvocationMatchWithMissingExpressionAndDifferentNameAndDifferentArguments() {
		boolean missingExpression = false;
		boolean differentMethodName = false;
		boolean differentArguments = false;
		boolean expression1NestedUnderCatchClause = false;
		boolean expression2NestedUnderCatchClause = false;
		for(ASTNodeDifference difference : differences) {
			if(difference.containsDifferenceType(DifferenceType.MISSING_METHOD_INVOCATION_EXPRESSION))
				missingExpression = true;
			if(difference.containsDifferenceType(DifferenceType.METHOD_INVOCATION_NAME_MISMATCH))
				differentMethodName = true;
			if(difference.containsDifferenceType(DifferenceType.ARGUMENT_NUMBER_MISMATCH) || difference.containsDifferenceType(DifferenceType.TYPE_COMPATIBLE_REPLACEMENT))
				differentArguments = true;
			if(isNestedUnderAnonymousClassDeclaration(difference.getExpression1().getExpression()))
				expression1NestedUnderCatchClause = true;
			if(isNestedUnderAnonymousClassDeclaration(difference.getExpression2().getExpression()))
				expression2NestedUnderCatchClause = true;
		}
		return missingExpression && differentMethodName && differentArguments && !expression1NestedUnderCatchClause && !expression2NestedUnderCatchClause;
	}

	private boolean onlyVariableTypeMismatchDifferences() {
		int diffCount = 0;
		int variableTypeMismatchCount = 0;
		int subclassTypeMismatchCount = 0;
		int variableNameMismatchCount = 0;
		int methodInvocationNameMismatchCount = 0;
		int literalValueMismatchCount = 0;
		for(ASTNodeDifference difference : differences) {
			for(Difference diff : difference.getDifferences()) {
				diffCount++;
				if(diff.getType().equals(DifferenceType.VARIABLE_TYPE_MISMATCH)) {
					variableTypeMismatchCount++;
				}
				if(diff.getType().equals(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
					subclassTypeMismatchCount++;
				}
				if(diff.getType().equals(DifferenceType.VARIABLE_NAME_MISMATCH)) {
					variableNameMismatchCount++;
				}
				if(diff.getType().equals(DifferenceType.METHOD_INVOCATION_NAME_MISMATCH)) {
					methodInvocationNameMismatchCount++;
				}
				if(diff.getType().equals(DifferenceType.LITERAL_VALUE_MISMATCH)) {
					literalValueMismatchCount++;
				}
			}
		}
		if(diffCount > 0 && (diffCount == (variableTypeMismatchCount + subclassTypeMismatchCount + variableNameMismatchCount) ||
				diffCount == (variableTypeMismatchCount + subclassTypeMismatchCount + methodInvocationNameMismatchCount) ||
				diffCount == (variableTypeMismatchCount + subclassTypeMismatchCount + literalValueMismatchCount) ||
				diffCount == (variableTypeMismatchCount + subclassTypeMismatchCount + literalValueMismatchCount + methodInvocationNameMismatchCount) ||
				diffCount == (variableTypeMismatchCount + subclassTypeMismatchCount + literalValueMismatchCount + variableNameMismatchCount) ))
			return true;
		return false;
	}

	protected boolean isTypeHolder(Object o) {
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
				|| o.getClass().equals(PrefixExpression.class) || o.getClass().equals(InstanceofExpression.class)
				|| o.getClass().equals(ThisExpression.class) || o.getClass().equals(ConditionalExpression.class))
			return true;
		return false;
	}

	protected ITypeBinding getTypeBinding(Object o) {
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
			ASTNode parent = ((ASTNode)o).getParent();
			List<Expression> arguments = null;
			IMethodBinding methodBinding = null;
			if(parent instanceof MethodInvocation) {
				MethodInvocation parentMethodInvocation = (MethodInvocation)parent;
				arguments = parentMethodInvocation.arguments();
				methodBinding = parentMethodInvocation.resolveMethodBinding();
			}
			else if(parent instanceof SuperMethodInvocation) {
				SuperMethodInvocation parentMethodInvocation = (SuperMethodInvocation)parent;
				arguments = parentMethodInvocation.arguments();
				methodBinding = parentMethodInvocation.resolveMethodBinding();
			}
			else if(parent instanceof ClassInstanceCreation) {
				ClassInstanceCreation parentClassInstanceCreation = (ClassInstanceCreation)parent;
				arguments = parentClassInstanceCreation.arguments();
				methodBinding = parentClassInstanceCreation.resolveConstructorBinding();
			}
			if(arguments != null && methodBinding != null) {
				int argumentPosition = 0;
				for(Expression argument : arguments) {
					if(argument.equals(o)) {
						ITypeBinding[] parameterTypeBindings = methodBinding.getParameterTypes();
						//analyze only if the argument does not correspond to a varargs parameter
						if(argumentPosition < parameterTypeBindings.length) {
							return parameterTypeBindings[argumentPosition];
						}
					}
					argumentPosition++;
				}
			}
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
		else if(o.getClass().equals(InstanceofExpression.class)) {
			InstanceofExpression expression = (InstanceofExpression) o;
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

	protected boolean typeBindingMatch(ITypeBinding binding1, ITypeBinding binding2) {
		//if bindings are both null then they were recovered from SimpleName expressions representing labels
		if(binding1 == null && binding2 == null)
			return true;
		if(binding1 != null && binding1.isAnonymous() && binding2 != null && binding2.isAnonymous()) {
			ITypeBinding[] interfaces1 = binding1.getInterfaces();
			ITypeBinding[] interfaces2 = binding2.getInterfaces();
			if(interfaces1.length == interfaces2.length) {
				for(int i=0; i<interfaces1.length; i++) {
					ITypeBinding interface1 = interfaces1[i];
					ITypeBinding interface2 = interfaces2[i];
					if(subclassTypeMismatch(interface1, interface2)) {
						return false;
					}
				}
				//all interface bindings are equal
				return true;
			}
			else {
				//different number of implemented interfaces
				return false;
			}
		}
		if(binding1.isCapture() && binding2.isCapture()) {
			ITypeBinding wildcardTypeBinding1 = binding1.getWildcard();
			ITypeBinding wildcardTypeBinding2 = binding2.getWildcard();
			if(wildcardTypeBinding1.isEqualTo(wildcardTypeBinding2) && wildcardTypeBinding1.getQualifiedName().equals(wildcardTypeBinding2.getQualifiedName()))
				return true;
		}
		if(binding1.isParameterizedType() && binding2.isParameterizedType()) {
			ITypeBinding[] typeArguments1 = binding1.getTypeArguments();
			ITypeBinding[] typeArguments2 = binding2.getTypeArguments();
			boolean allTypeArgumentsMatch = true;
			if(typeArguments1.length == typeArguments2.length) {
				int i = 0;
				for(ITypeBinding typeArgument1 : typeArguments1) {
					ITypeBinding typeArgument2 = typeArguments2[i];
					if(!typeBindingMatch(typeArgument1, typeArgument2)) {
						allTypeArgumentsMatch = false;
						break;
					}
					i++;
				}
			}
			else {
				allTypeArgumentsMatch = false;
			}
			ITypeBinding declarationTypeBinding1 = binding1.getTypeDeclaration();
			ITypeBinding declarationTypeBinding2 = binding2.getTypeDeclaration();
			boolean declarationTypeMatch = typeBindingMatch(declarationTypeBinding1, declarationTypeBinding2);
			if(declarationTypeMatch && allTypeArgumentsMatch)
				return true;
		}
		if(binding1.isEqualTo(binding2) && binding1.getQualifiedName().equals(binding2.getQualifiedName()))
			return true;
		if(binding1.getQualifiedName().equals("java.lang.Number") && isNumberPrimitiveType(binding2))
			return true;
		if(isNumberPrimitiveType(binding1) && binding2.getQualifiedName().equals("java.lang.Number"))
			return true;
		if(binding1.getName().equals("float") && binding2.getName().equals("double")) {
			return true;
		}
		if(binding1.getName().equals("double") && binding2.getName().equals("float")) {
			return true;
		}
		if(binding1.getName().equals("int") && binding2.getName().equals("byte")) {
			return true;
		}
		if(binding1.getName().equals("byte") && binding2.getName().equals("int")) {
			return true;
		}
		if(binding1.getName().equals("null") && !binding2.isPrimitive()) {
			return true;
		}
		if(binding2.getName().equals("null") && !binding1.isPrimitive()) {
			return true;
		}
		ITypeBinding commonSuperType = commonSuperType(binding1, binding2);
		return validCommonSuperType(commonSuperType);
	}

	private boolean subclassTypeMismatch(ITypeBinding nodeTypeBinding, ITypeBinding otherTypeBinding) {
		if(nodeTypeBinding.isParameterizedType() && otherTypeBinding.isParameterizedType()) {
			ITypeBinding declarationTypeBinding1 = nodeTypeBinding.getTypeDeclaration();
			ITypeBinding declarationTypeBinding2 = otherTypeBinding.getTypeDeclaration();
			return !declarationTypeBinding1.isEqualTo(declarationTypeBinding2) || !typeBindingMatch(nodeTypeBinding, otherTypeBinding);
		}
		return !nodeTypeBinding.isEqualTo(otherTypeBinding) || !nodeTypeBinding.getQualifiedName().equals(otherTypeBinding.getQualifiedName());
	}

	private static boolean isNumberPrimitiveType(ITypeBinding typeBinding) {
		if(typeBinding.isPrimitive()) {
			String name = typeBinding.getQualifiedName();
			if(name.equals("byte") || name.equals("double") || name.equals("float") || name.equals("int") || name.equals("long") || name.equals("short"))
				return true;
		}
		return false;
	}

	public static boolean validCommonSuperType(ITypeBinding commonSuperType) {
		if(commonSuperType != null && !isTaggingInterface(commonSuperType))
			return true;
		return false;
	}

	public static boolean isTaggingInterface(ITypeBinding typeBinding) {
		return typeBinding.getQualifiedName().equals("java.lang.Object") ||
				typeBinding.getQualifiedName().equals("java.io.Serializable") ||
				typeBinding.getQualifiedName().equals("java.lang.Runnable") ||
				typeBinding.getQualifiedName().equals("java.lang.Comparable") ||
				typeBinding.getQualifiedName().equals("java.lang.Cloneable") ||
				typeBinding.getQualifiedName().equals("java.util.EventListener");
	}

	public static ITypeBinding commonSuperType(ITypeBinding typeBinding1, ITypeBinding typeBinding2) {
		if(typeBinding1.getQualifiedName().equals("java.lang.Number") && isNumberPrimitiveType(typeBinding2))
			return typeBinding1;
		if(isNumberPrimitiveType(typeBinding1) && typeBinding2.getQualifiedName().equals("java.lang.Number"))
			return typeBinding2;
		Set<ITypeBinding> superTypes1 = getAllSuperTypes(typeBinding1);
		Set<ITypeBinding> superTypes2 = getAllSuperTypes(typeBinding2);
		for(ITypeBinding superType2 : superTypes2) {
			if(superType2.getQualifiedName().equals(typeBinding1.getQualifiedName()) || superType2.getErasure().getQualifiedName().equals(typeBinding1.getQualifiedName()) ||
					implementsInterface(superType2, typeBinding1) || implementsInterface(superType2.getErasure(), typeBinding1))
				return typeBinding1;
		}
		for(ITypeBinding superType1 : superTypes1) {
			if(superType1.getQualifiedName().equals(typeBinding2.getQualifiedName()) || superType1.getErasure().getQualifiedName().equals(typeBinding2.getQualifiedName()) ||
					implementsInterface(superType1, typeBinding2) || implementsInterface(superType1.getErasure(), typeBinding2))
				return typeBinding2;
		}
		List<ITypeBinding> typeBindings = new ArrayList<ITypeBinding>();
		for(ITypeBinding superType1 : superTypes1) {
			for(ITypeBinding superType2 : superTypes2) {
				if(superType1.getQualifiedName().equals(superType2.getQualifiedName()) &&
						!superType1.getQualifiedName().equals("java.lang.Object")) {
					addTypeBinding(superType1, typeBindings);
				}
			}
		}
		if(typeBindings.size() > 1) {
			TypeBindingInheritanceDetection inheritanceDetection = new TypeBindingInheritanceDetection(typeBindings);
			Set<String> leaves = inheritanceDetection.getLeavesInDeepestLevels();
			if(leaves.isEmpty()) {
				return typeBindings.get(0);
			}
			else {
				List<ITypeBinding> leafTypeBindings = new ArrayList<ITypeBinding>();
				for(String leaf : leaves) {
					for(ITypeBinding typeBinding : typeBindings) {
						if(leaf.equals(typeBinding.getQualifiedName())) {
							leafTypeBindings.add(typeBinding);
							break;
						}
					}
				}
				//return the first leaf that is a system class, if no system class is found return the first leaf that is a system interface
				for(ITypeBinding leafTypeBinding : leafTypeBindings) {
					if(leafTypeBinding.isClass()) {
						return leafTypeBinding;
					}
				}
				for(ITypeBinding leafTypeBinding : leafTypeBindings) {
					if(leafTypeBinding.isInterface() && ASTReader.getSystemObject().getClassObject(leafTypeBinding.getQualifiedName()) != null) {
						return leafTypeBinding;
					}
				}
				return leafTypeBindings.get(0);
			}
		}
		else if(typeBindings.size() == 1) {
			return typeBindings.get(0);
		}
		else {
			if(superTypes1.size() == 1 && superTypes2.size() == 1) {
				for(ITypeBinding superType1 : superTypes1) {
					for(ITypeBinding superType2 : superTypes2) {
						if(superType1.getQualifiedName().equals("java.lang.Object") &&
								superType2.getQualifiedName().equals("java.lang.Object")) {
							return superType1;
						}
					}
				}
			}
			if(superTypes1.size() == 1) {
				for(ITypeBinding superType1 : superTypes1) {
					if(superType1.getQualifiedName().equals("java.lang.Object"))
						return superType1;
				}
			}
			if(superTypes2.size() == 1) {
				for(ITypeBinding superType2 : superTypes2) {
					if(superType2.getQualifiedName().equals("java.lang.Object"))
						return superType2;
				}
			}
			return null;
		}
	}

	public static boolean implementsInterface(ITypeBinding typeBinding, ITypeBinding interfaceType) {
		ITypeBinding[] implementedInterfaces = typeBinding.getInterfaces();
		for(ITypeBinding implementedInterface : implementedInterfaces) {
			if(implementedInterface.getQualifiedName().equals(interfaceType.getQualifiedName()))
				return true;
		}
		return false;
	}

	private static void addTypeBinding(ITypeBinding typeBinding, List<ITypeBinding> typeBindings) {
		boolean found = false;
		for(ITypeBinding typeBinding2 : typeBindings) {
			if(typeBinding.isEqualTo(typeBinding2) && typeBinding.getQualifiedName().equals(typeBinding2.getQualifiedName())) {
				found = true;
				break;
			}
		}
		if(!found) {
			typeBindings.add(typeBinding);
		}
	}

	private static Set<ITypeBinding> getAllSuperTypes(ITypeBinding typeBinding) {
		Set<ITypeBinding> superTypes = new LinkedHashSet<ITypeBinding>();
		ITypeBinding superTypeBinding = typeBinding.getSuperclass();
		if(superTypeBinding != null) {
			superTypes.add(superTypeBinding);
			superTypes.addAll(getAllSuperTypes(superTypeBinding));
		}
		ITypeBinding[] superInterfaces = typeBinding.getInterfaces();
		for(ITypeBinding superInterface : superInterfaces) {
			superTypes.add(superInterface);
			superTypes.addAll(getAllSuperTypes(superInterface));
		}
		return superTypes;
	}

	protected boolean isInfixExpressionWithCompositeParent(ASTNode node) {
		if(node instanceof InfixExpression &&
				(node.getParent() instanceof IfStatement || node.getParent() instanceof InfixExpression ||
				node.getParent() instanceof WhileStatement || node.getParent() instanceof DoStatement ||
				node.getParent() instanceof ForStatement)) {
			return true;
		}
		return false;
	}

	private void processClassInstanceCreationArguments(List<Expression> nodeArguments, List<Expression> otherArguments,
			ASTNodeDifference astNodeDifference, String nodeToString, String otherToString, boolean identicalConstructorErasureType) {
		if(identicalConstructorErasureType) {
			if(nodeArguments.size() != otherArguments.size()) {
				Difference diff = new Difference(nodeToString,otherToString,DifferenceType.ARGUMENT_NUMBER_MISMATCH);
				int size1 = nodeArguments.size() == 0 ? 1 : nodeArguments.size();
				int size2 = otherArguments.size() == 0 ? 1 : otherArguments.size();
				diff.setWeight(size1 * size2);
				astNodeDifference.addDifference(diff);
			}
			else {
				for(int i=0; i<nodeArguments.size(); i++) {
					int differenceCountBefore = differences.size();
					safeSubtreeMatch(nodeArguments.get(i), otherArguments.get(i));
					reduceWeightOfReversedArguments(differenceCountBefore);
				}
			}
		}
	}

	private void processMethodInvocationArguments(List<Expression> nodeArguments, List<Expression> otherArguments,
			ASTNodeDifference astNodeDifference, String nodeToString, String otherToString, boolean overloadedMethods) {
		if(nodeArguments.size() != otherArguments.size()) {
			Difference diff = new Difference(nodeToString,otherToString,DifferenceType.ARGUMENT_NUMBER_MISMATCH);
			if(!overloadedMethods) {
				int size1 = nodeArguments.size() == 0 ? 1 : nodeArguments.size();
				int size2 = otherArguments.size() == 0 ? 1 : otherArguments.size();
				diff.setWeight(size1 * size2);
			}
			astNodeDifference.addDifference(diff);
		}
		else {
			for(int i=0; i<nodeArguments.size(); i++) {
				int differenceCountBefore = differences.size();
				safeSubtreeMatch(nodeArguments.get(i), otherArguments.get(i));
				reduceWeightOfReversedArguments(differenceCountBefore);
			}
		}
	}

	private boolean overloadedMethods(IMethodBinding methodBinding1, IMethodBinding methodBinding2) {
		List<ITypeBinding> parameterTypes1 = new ArrayList<ITypeBinding>(Arrays.asList(methodBinding1.getParameterTypes()));
		List<ITypeBinding> parameterTypes2 = new ArrayList<ITypeBinding>(Arrays.asList(methodBinding2.getParameterTypes()));
		return methodBinding1.getDeclaringClass().isEqualTo(methodBinding2.getDeclaringClass()) && methodBinding1.getName().equals(methodBinding2.getName()) &&
				(parameterTypes1.containsAll(parameterTypes2) || parameterTypes2.containsAll(parameterTypes1));
	}

	private static boolean isExpressionWithinMethodInvocationArgument(ASTNode expression) {
		ASTNode parent = expression.getParent();
		while(parent != null) {
			if(parent instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)parent;
				List<Expression> arguments = methodInvocation.arguments();
				for(Expression argument : arguments) {
					if(argument.equals(expression)) {
						return true;
					}
				}
			}
			else if(parent instanceof SuperMethodInvocation) {
				SuperMethodInvocation methodInvocation = (SuperMethodInvocation)parent;
				List<Expression> arguments = methodInvocation.arguments();
				for(Expression argument : arguments) {
					if(argument.equals(expression)) {
						return true;
					}
				}
			}
			else if(parent instanceof ClassInstanceCreation) {
				ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)parent;
				List<Expression> arguments = classInstanceCreation.arguments();
				for(Expression argument : arguments) {
					if(argument.equals(expression)) {
						return true;
					}
				}
			}
			expression = parent;
			parent = parent.getParent();
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
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
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
		addDifference(astNodeDifference);
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
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
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
				boolean initializerMatch = safeSubtreeMatch(node.getInitializer(), o.getInitializer());
				if(!initializerMatch && initializer1 != null && initializer2 != null) {
					Difference diff = new Difference(initializer1.toString(),initializer2.toString(),
							DifferenceType.ARRAY_INITIALIZER_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			if(!astNodeDifference.isEmpty())
				addDifference(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
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
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
			}
			else 
			{
				BooleanLiteral o = (BooleanLiteral) other;
				if(node.booleanValue() != o.booleanValue())
				{
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.LITERAL_VALUE_MISMATCH);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
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
					if(other instanceof Expression && !(other instanceof NullLiteral)) {
						Expression o = (Expression)other;
						ITypeBinding nodeTypeBinding = node.resolveTypeBinding();
						ITypeBinding otherTypeBinding = o.resolveTypeBinding();
						if(subclassTypeMismatch(nodeTypeBinding, otherTypeBinding)) {
							Difference diff = new Difference(nodeTypeBinding.getQualifiedName(),otherTypeBinding.getQualifiedName(),DifferenceType.SUBCLASS_TYPE_MISMATCH);
							astNodeDifference.addDifference(diff);
						}
					}
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
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
		addDifference(astNodeDifference);
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
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
			}
			else 
			{
				CharacterLiteral o = (CharacterLiteral) other;
				if(!node.getEscapedValue().equals(o.getEscapedValue()))
				{
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.LITERAL_VALUE_MISMATCH);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
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
		String nodeToString = node.toString();
		String otherToString = other.toString();
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (!(other instanceof ClassInstanceCreation)) {
				if(typeMatch) {
					if(other instanceof Expression && !(other instanceof NullLiteral)) {
						Expression o = (Expression)other;
						ITypeBinding nodeTypeBinding = node.resolveTypeBinding();
						ITypeBinding otherTypeBinding = o.resolveTypeBinding();
						if(subclassTypeMismatch(nodeTypeBinding, otherTypeBinding)) {
							Difference diff = new Difference(nodeTypeBinding.getQualifiedName(),otherTypeBinding.getQualifiedName(),DifferenceType.SUBCLASS_TYPE_MISMATCH);
							astNodeDifference.addDifference(diff);
						}
					}
					Difference diff = new Difference(nodeToString,otherToString,DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
				}
				else {
					Difference diff = new Difference(nodeToString,otherToString,DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			else {
				ClassInstanceCreation o = (ClassInstanceCreation) other;
				int differenceCount = differences.size();
				boolean classTypeMatch = safeSubtreeMatch(node.getType(), o.getType());
				boolean identicalErasureType = node.getType().resolveBinding().getErasure().getQualifiedName().equals(o.getType().resolveBinding().getErasure().getQualifiedName());
				int differenceCountAfterTypeMatch = differences.size();
				List<Expression> nodeArguments = node.arguments();
				List<Expression> otherArguments = o.arguments();
				if(classTypeMatch && differenceCountAfterTypeMatch == differenceCount) {
					processClassInstanceCreationArguments(nodeArguments, otherArguments, astNodeDifference, nodeToString, otherToString, identicalErasureType);
					boolean anonymousClassDeclarationMatch = safeSubtreeMatch(node.getAnonymousClassDeclaration(),o.getAnonymousClassDeclaration());
					//safeSubtreeListMatch(node.arguments(), o.arguments());
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
				else {
					processClassInstanceCreationArguments(nodeArguments, otherArguments, astNodeDifference, nodeToString, otherToString, identicalErasureType);
				}
			}
			if(!astNodeDifference.isEmpty())
				addDifference(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(nodeToString,otherToString,DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
		return false;
	}

	private void reduceWeightOfReversedArguments(int differenceCountBefore) {
		//find if a new TYPE_COMPATIBLE_REPLACEMENT difference was added
		ASTNodeDifference typeCompatibleReplacementDifference = null;
		for(int j=differenceCountBefore; j<differences.size(); j++) {
			ASTNodeDifference difference = differences.get(j);
			if(difference.containsOnlyDifferenceType(DifferenceType.TYPE_COMPATIBLE_REPLACEMENT)) {
				typeCompatibleReplacementDifference = difference;
				break;
			}
		}
		if(typeCompatibleReplacementDifference != null) {
			//check if there is a reverse difference in the previously recorded differences
			for(int j=0; j<differenceCountBefore; j++) {
				ASTNodeDifference difference = differences.get(j);
				if(difference.containsOnlyDifferenceType(DifferenceType.TYPE_COMPATIBLE_REPLACEMENT) &&
						difference.getBindingSignaturePair().isReverse(typeCompatibleReplacementDifference.getBindingSignaturePair())) {
					Difference diff = typeCompatibleReplacementDifference.getDifferences().get(0);
					diff.setWeight(1);
					Difference reversedArgumentDifference = difference.getDifferences().get(0);
					reversedArgumentDifference.setWeight(1);
					break;
				}
			}
		}
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
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
			}
			else {
				ConditionalExpression o = (ConditionalExpression)other;
				/*if(!node.resolveTypeBinding().isEqualTo(o.resolveTypeBinding()) && typeMatch) {
					Difference diff = new Difference(node.resolveTypeBinding().getName(),o.resolveTypeBinding().getName(),DifferenceType.SUBCLASS_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}*/
				safeSubtreeMatch(node.getExpression(),o.getExpression());
				safeSubtreeMatch(node.getThenExpression(), o.getThenExpression());
				safeSubtreeMatch(node.getElseExpression(), o.getElseExpression());
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
		return false;
	}

	public boolean match(DoStatement node, Object other) {
		if (other instanceof DoStatement)
		{
			DoStatement o = (DoStatement) other;
			if(isNestedUnderAnonymousClassDeclaration(node) && isNestedUnderAnonymousClassDeclaration(o)) {
				return super.match(node, o);
			}
			if (safeSubtreeMatch(node.getExpression(), o.getExpression()))
			{
				return true;
			}
		}
		ConditionalLoop nodeConditionalLoop = new ConditionalLoop(node);
		return loopMatch(nodeConditionalLoop, other);
	}

	public boolean match(EnhancedForStatement node, Object other) {
		if (other instanceof EnhancedForStatement)
		{
			EnhancedForStatement o = (EnhancedForStatement) other;
			if(isNestedUnderAnonymousClassDeclaration(node) && isNestedUnderAnonymousClassDeclaration(o)) {
				return super.match(node, o);
			}
			boolean paramMatch = safeSubtreeMatch(node.getParameter(), o.getParameter());
			boolean expMatch = safeSubtreeMatch(node.getExpression(), o.getExpression());
			if (paramMatch && expMatch)
			{
				return true;
			}
		}
		EnhancedForLoop nodeEnhancedForLoop = new EnhancedForLoop(node);
		return loopMatch(nodeEnhancedForLoop, other);
	}

	public boolean match(ExpressionStatement node, Object other) {
		if (AbstractControlStructureUtilities.hasOneConditionalExpression(node) != null && other instanceof IfStatement)
		{
			TernaryControlStructure nodeTernaryControlStructure = new TernaryControlStructure(node);
			return ifMatch(nodeTernaryControlStructure, other);
		}
		else if(node.getExpression() instanceof Assignment && other instanceof VariableDeclarationStatement) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)other;
			List fragments = variableDeclarationStatement.fragments();
			if(fragments.size() == 1) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment)fragments.get(0);
				Assignment assignment = (Assignment)node.getExpression();
				Expression leftHandSide = assignment.getLeftHandSide();
				if(leftHandSide instanceof SimpleName) {
					SimpleName simpleName = (SimpleName)leftHandSide;
					boolean variableMatch = safeSubtreeMatch(simpleName, fragment.getName());
					boolean variableTypeMatch = false;
					IBinding simpleNameBinding = simpleName.resolveBinding();
					IBinding fragmentNameBinding = fragment.getName().resolveBinding();
					if(simpleNameBinding.getKind() == IBinding.VARIABLE && fragmentNameBinding.getKind() == IBinding.VARIABLE) {
						IVariableBinding simpleNameVariableBinding = (IVariableBinding)simpleNameBinding;
						IVariableBinding fragmentNameVariableBinding = (IVariableBinding)fragmentNameBinding;
						variableTypeMatch = simpleNameVariableBinding.getType().isEqualTo(fragmentNameVariableBinding.getType()) &&
								simpleNameVariableBinding.getType().getQualifiedName().equals(fragmentNameVariableBinding.getType().getQualifiedName());
					}
					boolean initializerMatch = false;
					boolean initializerTypeMatch = false;
					Expression initializer = fragment.getInitializer();
					Expression rightHandSide = assignment.getRightHandSide();
					if(initializer != null && initializer.getNodeType() == rightHandSide.getNodeType()) {
						initializerMatch = safeSubtreeMatch(rightHandSide, initializer);
						initializerTypeMatch = initializer.resolveTypeBinding().isEqualTo(rightHandSide.resolveTypeBinding()) &&
								initializer.resolveTypeBinding().getQualifiedName().equals(rightHandSide.resolveTypeBinding().getQualifiedName());
					}
					if(variableMatch && variableTypeMatch && initializerMatch && initializerTypeMatch) {
						VariableDeclaration variableDeclaration = AbstractLoopUtilities.getVariableDeclaration(simpleName);
						if(variableDeclaration != null && hasEmptyInitializer(variableDeclaration)) {
							safeSubtreeMatch(variableDeclaration.getName(), fragment.getName());
							List<ASTNode> astNodes = new ArrayList<ASTNode>();
							astNodes.add(variableDeclaration);
							ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
							reportAdditionalFragments(astNodes, this.additionallyMatchedFragments1);
							return true;
						}
					}
				}
			}
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
		if(other instanceof MethodInvocation) {
			MethodInvocation methodInvocation = (MethodInvocation)other;
			if(getterMethodForField(methodInvocation, node.getName())) {
				FieldAccessReplacedWithGetterInvocationDifference astNodeDifference = 
						new FieldAccessReplacedWithGetterInvocationDifference(exp1, exp2, methodInvocation.getName().getIdentifier());
				int size = differences.size();
				safeSubtreeMatch(node.getExpression(), methodInvocation.getExpression());
				for(int i=size; i<differences.size(); i++) {
					astNodeDifference.addInvokerDifference(differences.get(i));
				}
				if(node.getExpression()==null && methodInvocation.getExpression()!=null) {
					Difference diff = new Difference("",methodInvocation.getExpression().toString(),DifferenceType.MISSING_METHOD_INVOCATION_EXPRESSION);
					astNodeDifference.addDifference(diff);
				}
				else if(node.getExpression()!=null && methodInvocation.getExpression()==null) {
					Difference diff = new Difference(node.getExpression().toString(),"",DifferenceType.MISSING_METHOD_INVOCATION_EXPRESSION);
					astNodeDifference.addDifference(diff);
				}
				if(node.getExpression()!=null) {
					ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
					astNodeDifference.setInvoker1(new AbstractExpression(node.getExpression()));
				}
				if(methodInvocation.getExpression()!=null) {
					ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
					astNodeDifference.setInvoker2(new AbstractExpression(methodInvocation.getExpression()));
				}
				Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.FIELD_ACCESS_REPLACED_WITH_GETTER);
				astNodeDifference.addDifference(diff);
				addDifference(astNodeDifference);
				return true;
			}
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
				ITypeBinding nodeTypeBinding = node.resolveTypeBinding();
				ITypeBinding otherTypeBinding = o.resolveTypeBinding();
				if(subclassTypeMismatch(nodeTypeBinding, otherTypeBinding) && typeMatch) {
					Difference diff = new Difference(nodeTypeBinding.getQualifiedName(),otherTypeBinding.getQualifiedName(),DifferenceType.SUBCLASS_TYPE_MISMATCH);
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
					if(other instanceof Expression && !(other instanceof NullLiteral)) {
						Expression o = (Expression)other;
						ITypeBinding nodeTypeBinding = node.resolveTypeBinding();
						ITypeBinding otherTypeBinding = o.resolveTypeBinding();
						if(subclassTypeMismatch(nodeTypeBinding, otherTypeBinding)) {
							Difference diff = new Difference(nodeTypeBinding.getQualifiedName(),otherTypeBinding.getQualifiedName(),DifferenceType.SUBCLASS_TYPE_MISMATCH);
							astNodeDifference.addDifference(diff);
						}
					}
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			if(!astNodeDifference.isEmpty())
				addDifference(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
		return false;
	}

	public boolean match(ForStatement node, Object other) {
		if (other instanceof ForStatement)
		{
			ForStatement o = (ForStatement) other;
			if(isNestedUnderAnonymousClassDeclaration(node) && isNestedUnderAnonymousClassDeclaration(o)) {
				return super.match(node, o);
			}
			boolean initializerMatch = safeSubtreeListMatch(node.initializers(), o.initializers());
			boolean expMatch = safeSubtreeMatch(node.getExpression(), o.getExpression());
			boolean updaterMatch = safeSubtreeListMatch(node.updaters(), o.updaters());
			if (initializerMatch && expMatch && updaterMatch)
			{
				return true;
			}
		}
		ConditionalLoop nodeConditionalLoop = new ConditionalLoop(node);
		return loopMatch(nodeConditionalLoop, other);
	}
	
	public boolean match(IfStatement node, Object other) {
		if (!(other instanceof IfStatement)) {
			if(other instanceof ExpressionStatement)
			{
				ExpressionStatement otherExpressionStatement = (ExpressionStatement)other;
				if (AbstractControlStructureUtilities.hasOneConditionalExpression(otherExpressionStatement) != null)
				{
					IfControlStructure nodeIfControlStructure = new IfControlStructure(node);
					return ifMatch(nodeIfControlStructure, other);
				}
			}
			else if (other instanceof ReturnStatement)
			{
				ReturnStatement otherReturnStatement = (ReturnStatement)other;
				if (otherReturnStatement.getExpression() instanceof ConditionalExpression)
				{
					IfControlStructure nodeIfControlStructure = new IfControlStructure(node);
					return ifMatch(nodeIfControlStructure, other);
				}
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
		//if only one of them is infix expression with a composite parent, not both
		boolean infix1 = isInfixExpressionWithCompositeParent(node);
		boolean infix2 = isInfixExpressionWithCompositeParent((ASTNode)other);
		if((infix1 && !infix2) || (!infix1 && infix2)) {
			return super.match(node, other);
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveTypeBinding(), getTypeBinding(other));
			if (!(other instanceof InfixExpression)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
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
				int differenceCountBefore = differences.size();
				boolean leftOperandMatch = safeSubtreeMatch(node.getLeftOperand(), o.getLeftOperand());
				int differenceCountAfterLeftOperandMatch = differences.size();
				boolean rightOperandMatch = safeSubtreeMatch(node.getRightOperand(), o.getRightOperand());
				int differenceCountAfterRightOperandMatch = differences.size();
				if(!leftOperandMatch && !rightOperandMatch) {
					//if both left and right operands do not match, then the entire infix expression should be parameterized
					if(differenceCountAfterLeftOperandMatch == differenceCountBefore) {
						Difference leftDiff = new Difference(node.getLeftOperand().toString(),o.getLeftOperand().toString(),DifferenceType.INFIX_LEFT_OPERAND_MISMATCH);
						astNodeDifference.addDifference(leftDiff);
					}
					if(differenceCountAfterRightOperandMatch == differenceCountAfterLeftOperandMatch) {
						Difference rightDiff = new Difference(node.getRightOperand().toString(),o.getRightOperand().toString(),DifferenceType.INFIX_RIGHT_OPERAND_MISMATCH);
						astNodeDifference.addDifference(rightDiff);
					}
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
						addDifference(astLeftOperandDifference);
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
						addDifference(astRightOperandDifference);
					}
				}
			}
			if(!astNodeDifference.isEmpty())
				addDifference(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
		return false;
	}

	public boolean match(InstanceofExpression node, Object other) {
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
			if (!(other instanceof InstanceofExpression)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
			}
			else 
			{
				InstanceofExpression o = (InstanceofExpression) other;
				safeSubtreeMatch(node.getLeftOperand(), o.getLeftOperand());
				safeSubtreeMatch(node.getRightOperand(), o.getRightOperand());
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
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
		String nodeToString = node.toString();
		String otherToString = other.toString();
		if(other instanceof FieldAccess) {
			FieldAccess fieldAccess = (FieldAccess)other;
			if(getterMethodForField(node, fieldAccess.getName())) {
				FieldAccessReplacedWithGetterInvocationDifference astNodeDifference = 
						new FieldAccessReplacedWithGetterInvocationDifference(exp1, exp2, node.getName().getIdentifier());
				int size = differences.size();
				safeSubtreeMatch(node.getExpression(), fieldAccess.getExpression());
				for(int i=size; i<differences.size(); i++) {
					astNodeDifference.addInvokerDifference(differences.get(i));
				}
				if(node.getExpression()==null && fieldAccess.getExpression()!=null) {
					Difference diff = new Difference("",fieldAccess.getExpression().toString(),DifferenceType.MISSING_METHOD_INVOCATION_EXPRESSION);
					astNodeDifference.addDifference(diff);
				}
				else if(node.getExpression()!=null && fieldAccess.getExpression()==null) {
					Difference diff = new Difference(node.getExpression().toString(),"",DifferenceType.MISSING_METHOD_INVOCATION_EXPRESSION);
					astNodeDifference.addDifference(diff);
				}
				if(node.getExpression()!=null) {
					ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
					astNodeDifference.setInvoker1(new AbstractExpression(node.getExpression()));
				}
				if(fieldAccess.getExpression()!=null) {
					ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
					astNodeDifference.setInvoker2(new AbstractExpression(fieldAccess.getExpression()));
				}
				Difference diff = new Difference(nodeToString,otherToString,DifferenceType.FIELD_ACCESS_REPLACED_WITH_GETTER);
				astNodeDifference.addDifference(diff);
				addDifference(astNodeDifference);
				return true;
			}
		}
		if(other instanceof SimpleName) {
			SimpleName simpleName = (SimpleName)other;
			IBinding binding = simpleName.resolveBinding();
			if(binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				if(variableBinding.isField()) {
					if(getterMethodForField(node, simpleName)) {
						FieldAccessReplacedWithGetterInvocationDifference astNodeDifference = 
								new FieldAccessReplacedWithGetterInvocationDifference(exp1, exp2, node.getName().getIdentifier());
						if(node.getExpression() != null) {
							Difference diff = new Difference(node.getExpression().toString(),"",DifferenceType.MISSING_METHOD_INVOCATION_EXPRESSION);
							astNodeDifference.addDifference(diff);
							ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
							astNodeDifference.setInvoker1(new AbstractExpression(node.getExpression()));
						}
						Difference diff = new Difference(nodeToString,otherToString,DifferenceType.FIELD_ACCESS_REPLACED_WITH_GETTER);
						astNodeDifference.addDifference(diff);
						addDifference(astNodeDifference);
						return true;
					}
				}
			}
		}
		ASTNodeDifference astNodeDifference = new ASTNodeDifference(exp1, exp2);
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveMethodBinding().getReturnType(), getTypeBinding(other));
			if (!(other instanceof MethodInvocation)) {
				if(typeMatch) {
					if(other instanceof Expression && !(other instanceof NullLiteral)) {
						Expression o = (Expression)other;
						ITypeBinding nodeTypeBinding = node.resolveTypeBinding();
						ITypeBinding otherTypeBinding = o.resolveTypeBinding();
						if(subclassTypeMismatch(nodeTypeBinding, otherTypeBinding)) {
							Difference diff = new Difference(nodeTypeBinding.getQualifiedName(),otherTypeBinding.getQualifiedName(),DifferenceType.SUBCLASS_TYPE_MISMATCH);
							astNodeDifference.addDifference(diff);
						}
					}
					Difference diff = new Difference(nodeToString,otherToString,DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
				}
				else {
					Difference diff = new Difference(nodeToString,otherToString,DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			else {
				MethodInvocation o = (MethodInvocation) other;
				IMethodBinding nodeMethodBinding = node.resolveMethodBinding();
				IMethodBinding otherMethodBinding = o.resolveMethodBinding();
				boolean isNodeMethodBindingStatic = (nodeMethodBinding.getModifiers() & Modifier.STATIC) != 0;
				boolean isOtherMethodBindingStatic = (otherMethodBinding.getModifiers() & Modifier.STATIC) != 0;
				if(isNodeMethodBindingStatic != isOtherMethodBindingStatic) {
					if(typeMatch) {
						Difference diff = new Difference(nodeToString,otherToString,DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
						astNodeDifference.addDifference(diff);
						if(!(node.getParent() instanceof Statement) && !(o.getParent() instanceof Statement)) {
							addDifference(astNodeDifference);
							return typeMatch;
						}
					}
					else {
						Difference diff = new Difference(nodeToString,otherToString,DifferenceType.AST_TYPE_MISMATCH);
						astNodeDifference.addDifference(diff);
						if(!(node.getParent() instanceof Statement) && !(o.getParent() instanceof Statement)) {
							addDifference(astNodeDifference);
							return typeMatch;
						}
					}
				}
				if(isExpressionWithinMethodInvocationArgument(node) && isExpressionWithinMethodInvocationArgument(o) &&
						node.getExpression() != null && o.getExpression() != null &&
						node.getExpression().getNodeType() != o.getExpression().getNodeType() &&
						node.arguments().isEmpty() && o.arguments().isEmpty() &&
						typeBindingMatch(node.resolveMethodBinding().getReturnType(), o.resolveMethodBinding().getReturnType())) {
					Difference diff = new Difference(nodeToString,otherToString,DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
				}
				else {
					List<Expression> nodeArguments = node.arguments();
					List<Expression> otherArguments = o.arguments();
					processMethodInvocationArguments(nodeArguments, otherArguments, astNodeDifference, nodeToString, otherToString, overloadedMethods(nodeMethodBinding, otherMethodBinding));
					safeSubtreeMatch(node.getName(), o.getName());
					//safeSubtreeListMatch(node.arguments(), o.arguments());
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
			}
			if(!astNodeDifference.isEmpty())
				addDifference(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(nodeToString,otherToString,DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
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
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
			}
			else {
				return true;
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
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
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			else 
			{
				NumberLiteral o = (NumberLiteral) other;
				if(!node.getToken().equals(o.getToken()))
				{
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.LITERAL_VALUE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
				if(!typeMatch) {
					Difference diff = new Difference(node.resolveTypeBinding().getQualifiedName(),o.resolveTypeBinding().getQualifiedName(),DifferenceType.VARIABLE_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			if(!astNodeDifference.isEmpty())
				addDifference(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
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
					if(other instanceof Expression && !(other instanceof NullLiteral)) {
						Expression o = (Expression)other;
						ITypeBinding nodeTypeBinding = node.resolveTypeBinding();
						ITypeBinding otherTypeBinding = o.resolveTypeBinding();
						if(subclassTypeMismatch(nodeTypeBinding, otherTypeBinding)) {
							Difference diff = new Difference(nodeTypeBinding.getQualifiedName(),otherTypeBinding.getQualifiedName(),DifferenceType.SUBCLASS_TYPE_MISMATCH);
							astNodeDifference.addDifference(diff);
						}
					}
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
			}
			else {
				ParenthesizedExpression o = (ParenthesizedExpression) other;
				int differenceCountBefore = differences.size();
				boolean expressionMatch = safeSubtreeMatch(node.getExpression(), o.getExpression());
				int differenceCountAfter = differences.size();
				if(!expressionMatch && typeMatch && differenceCountAfter == differenceCountBefore) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
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
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
			}
			else {
				PrefixExpression o = (PrefixExpression) other;
				if(!node.getOperator().equals(o.getOperator())) {
					Difference diff = new Difference(node.getOperator().toString(),o.getOperator().toString(),DifferenceType.OPERATOR_MISMATCH);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
				safeSubtreeMatch(node.getOperand(), o.getOperand());
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
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
					ITypeBinding nodeTypeBinding = node.resolveTypeBinding();
					ITypeBinding otherTypeBinding = o.resolveTypeBinding();
					if(subclassTypeMismatch(nodeTypeBinding, otherTypeBinding) && typeMatch) {
						Difference diff = new Difference(nodeTypeBinding.getQualifiedName(),otherTypeBinding.getQualifiedName(),DifferenceType.SUBCLASS_TYPE_MISMATCH);
						astNodeDifference.addDifference(diff);
					}
					if(!typeMatch) {
						Difference diff = new Difference(nodeTypeBinding.getQualifiedName(),otherTypeBinding.getQualifiedName(),DifferenceType.VARIABLE_TYPE_MISMATCH);
						astNodeDifference.addDifference(diff);
					}
					safeSubtreeMatch(node.getQualifier(), o.getQualifier());
				}
				else {
					if(typeMatch) {
						if(other instanceof Expression && !(other instanceof NullLiteral)) {
							Expression o = (Expression)other;
							ITypeBinding nodeTypeBinding = node.resolveTypeBinding();
							ITypeBinding otherTypeBinding = o.resolveTypeBinding();
							if(subclassTypeMismatch(nodeTypeBinding, otherTypeBinding)) {
								Difference diff = new Difference(nodeTypeBinding.getQualifiedName(),otherTypeBinding.getQualifiedName(),DifferenceType.SUBCLASS_TYPE_MISMATCH);
								astNodeDifference.addDifference(diff);
							}
						}
						Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
						astNodeDifference.addDifference(diff);
					}
					else {
						Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
						astNodeDifference.addDifference(diff);
					}
				}
			}
			if(!astNodeDifference.isEmpty())
				addDifference(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
		return false;
	}

	public boolean match(ReturnStatement node, Object other) {
		if (node.getExpression() instanceof ConditionalExpression && other instanceof IfStatement)
		{
			TernaryControlStructure nodeTernaryControlStructure = new TernaryControlStructure(node);
			return ifMatch(nodeTernaryControlStructure, other);
		}
		return super.match(node, other);
	}

	public boolean match(SimpleName node, Object other) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(node);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression((Expression)other);
		if(isInfixExpressionWithCompositeParent((ASTNode)other)) {
			return super.match(node, other);
		}
		if(other instanceof MethodInvocation) {
			MethodInvocation methodInvocation = (MethodInvocation)other;
			IBinding binding = node.resolveBinding();
			if(binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				if(variableBinding.isField()) {
					if(getterMethodForField(methodInvocation, node)) {
						FieldAccessReplacedWithGetterInvocationDifference astNodeDifference = 
								new FieldAccessReplacedWithGetterInvocationDifference(exp1, exp2, methodInvocation.getName().getIdentifier());
						if(methodInvocation.getExpression() != null) {
							Difference diff = new Difference("",methodInvocation.getExpression().toString(),DifferenceType.MISSING_METHOD_INVOCATION_EXPRESSION);
							astNodeDifference.addDifference(diff);
							ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
							astNodeDifference.setInvoker2(new AbstractExpression(methodInvocation.getExpression()));
						}
						Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.FIELD_ACCESS_REPLACED_WITH_GETTER);
						astNodeDifference.addDifference(diff);
						addDifference(astNodeDifference);
						return true;
					}
				}
			}
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
							diff.setWeight(LevenshteinDistance.computeLevenshteinDistance(node.getIdentifier(),o.getIdentifier()));
							astNodeDifference.addDifference(diff);
						}
					}
					else if(nodeBinding.getKind() == IBinding.TYPE && otherBinding.getKind() == IBinding.TYPE) {
						ITypeBinding nodeTypeBinding = (ITypeBinding)nodeBinding;
						ITypeBinding otherTypeBinding = (ITypeBinding)otherBinding;
						if(subclassTypeMismatch(nodeTypeBinding, otherTypeBinding) && typeMatch) {
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
						if(nodeTypeBinding != null && otherTypeBinding != null && subclassTypeMismatch(nodeTypeBinding, otherTypeBinding)) {
							Difference diff = new Difference(nodeTypeBinding.getQualifiedName(),otherTypeBinding.getQualifiedName(),DifferenceType.SUBCLASS_TYPE_MISMATCH);
							astNodeDifference.addDifference(diff);
						}
					}
				}
			}
			else {
				if(typeMatch) {
					if(other instanceof Expression && !(other instanceof NullLiteral)) {
						Expression o = (Expression)other;
						ITypeBinding nodeTypeBinding = node.resolveTypeBinding();
						ITypeBinding otherTypeBinding = o.resolveTypeBinding();
						if(subclassTypeMismatch(nodeTypeBinding, otherTypeBinding)) {
							Difference diff = new Difference(nodeTypeBinding.getQualifiedName(),otherTypeBinding.getQualifiedName(),DifferenceType.SUBCLASS_TYPE_MISMATCH);
							astNodeDifference.addDifference(diff);
						}
					}
					Difference diff = new Difference(node.getIdentifier(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
				}
				else {
					Difference diff = new Difference(node.getIdentifier(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			if(!astNodeDifference.isEmpty())
				addDifference(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(node.getIdentifier(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
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
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
			}
			else 
			{
				StringLiteral o = (StringLiteral) other;
				if(!node.getLiteralValue().equals(o.getLiteralValue()))
				{
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.LITERAL_VALUE_MISMATCH);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
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
				ITypeBinding nodeTypeBinding = node.resolveTypeBinding();
				ITypeBinding otherTypeBinding = o.resolveTypeBinding();
				if(subclassTypeMismatch(nodeTypeBinding, otherTypeBinding) && typeMatch) {
					Difference diff = new Difference(nodeTypeBinding.getQualifiedName(),otherTypeBinding.getQualifiedName(),DifferenceType.SUBCLASS_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
				if(!typeMatch) {
					Difference diff = new Difference(node.resolveTypeBinding().getQualifiedName(),o.resolveTypeBinding().getQualifiedName(),DifferenceType.VARIABLE_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			else {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			if(!astNodeDifference.isEmpty())
				addDifference(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
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
		String nodeToString = node.toString();
		String otherToString = other.toString();
		if(isTypeHolder(other)) {
			boolean typeMatch = typeBindingMatch(node.resolveMethodBinding().getReturnType(), getTypeBinding(other));
			if (!(other instanceof SuperMethodInvocation)) {
				if(typeMatch) {
					Difference diff = new Difference(nodeToString,otherToString,DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
				}
				else {
					Difference diff = new Difference(nodeToString,otherToString,DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
				}
			}
			else {
				SuperMethodInvocation o = (SuperMethodInvocation) other;
				IMethodBinding nodeMethodBinding = node.resolveMethodBinding();
				IMethodBinding otherMethodBinding = o.resolveMethodBinding();
				List<Expression> nodeArguments = node.arguments();
				List<Expression> otherArguments = o.arguments();
				processMethodInvocationArguments(nodeArguments, otherArguments, astNodeDifference, nodeToString, otherToString, overloadedMethods(nodeMethodBinding, otherMethodBinding));
				safeSubtreeMatch(node.getName(), o.getName());
				//safeSubtreeListMatch(node.arguments(), o.arguments());
				safeSubtreeMatch(node.getQualifier(), o.getQualifier());
			}
			if(!astNodeDifference.isEmpty())
				addDifference(astNodeDifference);
			return typeMatch;
		}
		Difference diff = new Difference(nodeToString,otherToString,DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
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
		SwitchControlStructure nodeSwitchStructure = new SwitchControlStructure(node);
		SwitchControlStructure otherSwitchStructure = new SwitchControlStructure((SwitchStatement)other);
		return nodeSwitchStructure.match(otherSwitchStructure, this);
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
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
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
		addDifference(astNodeDifference);
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
			if (!(other instanceof TypeLiteral)) {
				if(typeMatch) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.TYPE_COMPATIBLE_REPLACEMENT,astNodeDifference.getWeight());
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
				else {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
			}
			else {
				TypeLiteral o = (TypeLiteral) other;
				if(!node.getType().resolveBinding().isEqualTo(o.getType().resolveBinding()) || !node.getType().resolveBinding().getQualifiedName().equals(o.getType().resolveBinding().getQualifiedName())) {
					Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.LITERAL_VALUE_MISMATCH);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
				}
			}
			return typeMatch;
		}
		Difference diff = new Difference(node.toString(),other.toString(),DifferenceType.AST_TYPE_MISMATCH);
		astNodeDifference.addDifference(diff);
		addDifference(astNodeDifference);
		return false;
	}

	public boolean match(VariableDeclarationStatement node, Object other) {
		List fragments = node.fragments();
		if(fragments.size() == 1 && other instanceof ExpressionStatement) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment)fragments.get(0);
			ExpressionStatement expressionStatement = (ExpressionStatement)other;
			Expression expression = expressionStatement.getExpression();
			if(expression instanceof Assignment) {
				Assignment assignment = (Assignment)expression;
				Expression leftHandSide = assignment.getLeftHandSide();
				if(leftHandSide instanceof SimpleName) {
					SimpleName simpleName = (SimpleName)leftHandSide;
					boolean variableMatch = safeSubtreeMatch(fragment.getName(), simpleName);
					boolean variableTypeMatch = false;
					IBinding simpleNameBinding = simpleName.resolveBinding();
					IBinding fragmentNameBinding = fragment.getName().resolveBinding();
					if(simpleNameBinding.getKind() == IBinding.VARIABLE && fragmentNameBinding.getKind() == IBinding.VARIABLE) {
						IVariableBinding simpleNameVariableBinding = (IVariableBinding)simpleNameBinding;
						IVariableBinding fragmentNameVariableBinding = (IVariableBinding)fragmentNameBinding;
						variableTypeMatch = simpleNameVariableBinding.getType().isEqualTo(fragmentNameVariableBinding.getType()) &&
								simpleNameVariableBinding.getType().getQualifiedName().equals(fragmentNameVariableBinding.getType().getQualifiedName());;
					}
					boolean initializerMatch = false;
					boolean initializerTypeMatch = false;
					Expression initializer = fragment.getInitializer();
					Expression rightHandSide = assignment.getRightHandSide();
					if(initializer != null && initializer.getNodeType() == rightHandSide.getNodeType()) {
						initializerMatch = safeSubtreeMatch(initializer, rightHandSide);
						initializerTypeMatch = initializer.resolveTypeBinding().isEqualTo(rightHandSide.resolveTypeBinding()) &&
								initializer.resolveTypeBinding().getQualifiedName().equals(rightHandSide.resolveTypeBinding().getQualifiedName());
					}
					if(variableMatch && variableTypeMatch && initializerMatch && initializerTypeMatch) {
						VariableDeclaration variableDeclaration = AbstractLoopUtilities.getVariableDeclaration(simpleName);
						if(variableDeclaration != null && hasEmptyInitializer(variableDeclaration)) {
							safeSubtreeMatch(fragment.getName(), variableDeclaration.getName());
							List<ASTNode> astNodes = new ArrayList<ASTNode>();
							astNodes.add(variableDeclaration);
							ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
							reportAdditionalFragments(astNodes, this.additionallyMatchedFragments2);
							return true;
						}
					}
				}
			}
		}
		return super.match(node, other);
	}

	public boolean match(WhileStatement node, Object other) {
		if (other instanceof WhileStatement)
		{
			WhileStatement o = (WhileStatement) other;
			if(isNestedUnderAnonymousClassDeclaration(node) && isNestedUnderAnonymousClassDeclaration(o)) {
				return super.match(node, o);
			}
			if ((safeSubtreeMatch(node.getExpression(), o.getExpression())))
			{
				return true;
			}
		}
		AbstractLoop nodeConditionalLoop = new ConditionalLoop(node);
		return loopMatch(nodeConditionalLoop, other);
	}

	private boolean setterReplacedWithFieldAssignment(MethodInvocation setter, Assignment assignment) {
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
		AbstractExpression exp1 = new AbstractExpression(setter);
		ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
		AbstractExpression exp2 = new AbstractExpression(assignment);
		FieldAssignmentReplacedWithSetterInvocationDifference astNodeDifference =
				new FieldAssignmentReplacedWithSetterInvocationDifference(exp1, exp2, setter.getName().getIdentifier());
		
		Expression leftHandSide = assignment.getLeftHandSide();
		Expression rightHandSide = assignment.getRightHandSide();
		List arguments = setter.arguments();
		if(arguments.size() == 1) {
			int size = differences.size();
			boolean argumentRightHandSideMatch = safeSubtreeMatch(arguments.get(0), rightHandSide);
			for(int i=size; i<differences.size(); i++) {
				astNodeDifference.addArgumentDifference(differences.get(i));
			}
			ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
			astNodeDifference.setArgument1(new AbstractExpression((Expression)arguments.get(0)));
			ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
			astNodeDifference.setArgument2(new AbstractExpression(rightHandSide));
			if(leftHandSide instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)leftHandSide;
				SimpleName fieldAccessName = fieldAccess.getName();
				SimpleName setField = setterMethodForField(setter, fieldAccessName);
				if(setField != null && argumentRightHandSideMatch) {
					size = differences.size();
					safeSubtreeMatch(setter.getExpression(), fieldAccess.getExpression());
					for(int i=size; i<differences.size(); i++) {
						astNodeDifference.addInvokerDifference(differences.get(i));
					}
					if(setter.getExpression()==null && fieldAccess.getExpression()!=null) {
						Difference diff = new Difference("",fieldAccess.getExpression().toString(),DifferenceType.MISSING_METHOD_INVOCATION_EXPRESSION);
						astNodeDifference.addDifference(diff);
					}
					else if(setter.getExpression()!=null && fieldAccess.getExpression()==null) {
						Difference diff = new Difference(setter.getExpression().toString(),"",DifferenceType.MISSING_METHOD_INVOCATION_EXPRESSION);
						astNodeDifference.addDifference(diff);
					}
					if(setter.getExpression()!=null) {
						ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
						astNodeDifference.setInvoker1(new AbstractExpression(setter.getExpression()));
					}
					if(fieldAccess.getExpression()!=null) {
						ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
						astNodeDifference.setInvoker2(new AbstractExpression(fieldAccess.getExpression()));
					}
					PlainVariable field2 = new PlainVariable((IVariableBinding)fieldAccessName.resolveBinding());
					PlainVariable field1 = new PlainVariable((IVariableBinding)setField.resolveBinding());
					astNodeDifference.setField1(field1);
					astNodeDifference.setField2(field2);
					Difference diff = new Difference(setter.toString(),assignment.toString(),DifferenceType.FIELD_ASSIGNMENT_REPLACED_WITH_SETTER);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
					return true;
				}
			}
			else if(leftHandSide instanceof SimpleName) {
				SimpleName simpleName = (SimpleName)leftHandSide;
				SimpleName setField = setterMethodForField(setter, simpleName);
				if(setField != null && argumentRightHandSideMatch) {
					if(setter.getExpression() != null) {
						Difference diff = new Difference(setter.getExpression().toString(),"",DifferenceType.MISSING_METHOD_INVOCATION_EXPRESSION);
						astNodeDifference.addDifference(diff);
						ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
						astNodeDifference.setInvoker1(new AbstractExpression(setter.getExpression()));
					}
					PlainVariable field2 = new PlainVariable((IVariableBinding)simpleName.resolveBinding());
					PlainVariable field1 = new PlainVariable((IVariableBinding)setField.resolveBinding());
					astNodeDifference.setField1(field1);
					astNodeDifference.setField2(field2);
					Difference diff = new Difference(setter.toString(),assignment.toString(),DifferenceType.FIELD_ASSIGNMENT_REPLACED_WITH_SETTER);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
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
		FieldAssignmentReplacedWithSetterInvocationDifference astNodeDifference =
				new FieldAssignmentReplacedWithSetterInvocationDifference(exp1, exp2, setter.getName().getIdentifier());
		
		Expression leftHandSide = assignment.getLeftHandSide();
		Expression rightHandSide = assignment.getRightHandSide();
		List arguments = setter.arguments();
		if(arguments.size() == 1) {
			int size = differences.size();
			boolean argumentRightHandSideMatch = safeSubtreeMatch(rightHandSide, arguments.get(0));
			for(int i=size; i<differences.size(); i++) {
				astNodeDifference.addArgumentDifference(differences.get(i));
			}
			ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
			astNodeDifference.setArgument1(new AbstractExpression(rightHandSide));
			ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
			astNodeDifference.setArgument2(new AbstractExpression((Expression)arguments.get(0)));
			if(leftHandSide instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)leftHandSide;
				SimpleName fieldAccessName = fieldAccess.getName();
				SimpleName setField = setterMethodForField(setter, fieldAccessName);
				if(setField != null && argumentRightHandSideMatch) {
					size = differences.size();
					safeSubtreeMatch(fieldAccess.getExpression(), setter.getExpression());
					for(int i=size; i<differences.size(); i++) {
						astNodeDifference.addInvokerDifference(differences.get(i));
					}
					if(fieldAccess.getExpression()==null && setter.getExpression()!=null) {
						Difference diff = new Difference("",setter.getExpression().toString(),DifferenceType.MISSING_METHOD_INVOCATION_EXPRESSION);
						astNodeDifference.addDifference(diff);
					}
					else if(fieldAccess.getExpression()!=null && setter.getExpression()==null) {
						Difference diff = new Difference(fieldAccess.getExpression().toString(),"",DifferenceType.MISSING_METHOD_INVOCATION_EXPRESSION);
						astNodeDifference.addDifference(diff);
					}
					if(fieldAccess.getExpression()!=null) {
						ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
						astNodeDifference.setInvoker1(new AbstractExpression(fieldAccess.getExpression()));
					}
					if(setter.getExpression()!=null) {
						ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
						astNodeDifference.setInvoker2(new AbstractExpression(setter.getExpression()));
					}
					PlainVariable field1 = new PlainVariable((IVariableBinding)fieldAccessName.resolveBinding());
					PlainVariable field2 = new PlainVariable((IVariableBinding)setField.resolveBinding());
					astNodeDifference.setField1(field1);
					astNodeDifference.setField2(field2);
					Difference diff = new Difference(assignment.toString(),setter.toString(),DifferenceType.FIELD_ASSIGNMENT_REPLACED_WITH_SETTER);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
					return true;
				}
			}
			else if(leftHandSide instanceof SimpleName) {
				SimpleName simpleName = (SimpleName)leftHandSide;
				SimpleName setField = setterMethodForField(setter, simpleName);
				if(setField != null && argumentRightHandSideMatch) {
					if(setter.getExpression() != null) {
						Difference diff = new Difference("",setter.getExpression().toString(),DifferenceType.MISSING_METHOD_INVOCATION_EXPRESSION);
						astNodeDifference.addDifference(diff);
						ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
						astNodeDifference.setInvoker2(new AbstractExpression(setter.getExpression()));
					}
					PlainVariable field1 = new PlainVariable((IVariableBinding)simpleName.resolveBinding());
					PlainVariable field2 = new PlainVariable((IVariableBinding)setField.resolveBinding());
					astNodeDifference.setField1(field1);
					astNodeDifference.setField2(field2);
					Difference diff = new Difference(assignment.toString(),setter.toString(),DifferenceType.FIELD_ASSIGNMENT_REPLACED_WITH_SETTER);
					astNodeDifference.addDifference(diff);
					addDifference(astNodeDifference);
					return true;
				}
			}
		}
		return false;
	}

	private boolean getterMethodForField(MethodInvocation methodInvocation, SimpleName fieldName) {
		IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
		ITypeBinding declaringClassTypeBinding = methodBinding.getDeclaringClass();
		ClassObject declaringClass = ASTReader.getSystemObject().getClassObject(declaringClassTypeBinding.getQualifiedName());
		if(declaringClass != null) {
			ListIterator<MethodObject> methodIterator = declaringClass.getMethodIterator();
			while(methodIterator.hasNext()) {
				MethodObject method = methodIterator.next();
				MethodDeclaration methodDeclaration = method.getMethodDeclaration();
				if(methodDeclaration.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
					SimpleName getField = MethodDeclarationUtility.isGetter(methodDeclaration);
					if(getField != null) {
						if(getField.resolveBinding().getKind() == IBinding.VARIABLE &&
								fieldName.resolveBinding().getKind() == IBinding.VARIABLE) {
							IVariableBinding getFieldBinding = (IVariableBinding)getField.resolveBinding();
							IVariableBinding fieldNameBinding = (IVariableBinding)fieldName.resolveBinding();
							if(getFieldBinding.isEqualTo(fieldNameBinding) ||
									(getField.getIdentifier().equals(fieldName.getIdentifier()) &&
									getFieldBinding.getType().isEqualTo(fieldNameBinding.getType()) && getFieldBinding.getType().getQualifiedName().equals(fieldNameBinding.getType().getQualifiedName()))) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private SimpleName setterMethodForField(MethodInvocation methodInvocation, SimpleName fieldName) {
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
									setFieldBinding.getType().isEqualTo(fieldNameBinding.getType()) && setFieldBinding.getType().getQualifiedName().equals(fieldNameBinding.getType().getQualifiedName()))) {
								return setField;
							}
						}
					}
				}
			}
		}
		return null;
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

	private boolean hasEmptyInitializer(VariableDeclaration variableDeclaration) {
		if(variableDeclaration.getInitializer() == null) {
			return true;
		}
		else {
			Expression initializer = variableDeclaration.getInitializer();
			if(initializer instanceof NullLiteral) {
				return true;
			}
		}
		return false;
	}

	private boolean loopMatch(AbstractLoop nodeLoop, Object other)
	{
		AbstractLoop otherLoop = generateAbstractLoop(other);
		if (otherLoop != null)
		{
			ConditionalLoopASTNodeMatcher matcher = new ConditionalLoopASTNodeMatcher(typeRoot1, typeRoot2);
			boolean loopMatch = nodeLoop.match(otherLoop, matcher);
			if (loopMatch)
			{
				ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
				reportAdditionalFragments(nodeLoop, this.additionallyMatchedFragments1);
				if (nodeLoop instanceof ConditionalLoop && otherLoop instanceof EnhancedForLoop)
				{
					ConditionalLoop nodeConditionalLoop  = (ConditionalLoop)nodeLoop;
					EnhancedForLoop otherEnhancedForLoop = (EnhancedForLoop)otherLoop;
					SimpleName enhancedForLoopParameter  = ((EnhancedForStatement)otherLoop.getLoopStatement()).getParameter().getName();
					Collection<AbstractControlVariable> nodeConditionControlVariables = nodeConditionalLoop.getConditionControlVariables().values();
					if (!nodeConditionControlVariables.isEmpty())
					{
						ControlVariable conditionalLoopControlVariable = (ControlVariable)nodeConditionControlVariables.toArray()[0];
						SimpleName variableInitializedUsingControlVariable = AbstractLoopUtilities.getVariableInitializedUsingControlVariable(conditionalLoopControlVariable, nodeConditionalLoop.getLoopBody());
						safeSubtreeMatch(variableInitializedUsingControlVariable, enhancedForLoopParameter);
						Expression conditionalLoopDataStructureExpression = conditionalLoopControlVariable.getDataStructureExpression();
						Expression enhancedForLoopDataStructureExpression = otherEnhancedForLoop.getControlVariable().getDataStructureExpression();
						matcher.compareTypes(conditionalLoopDataStructureExpression, enhancedForLoopDataStructureExpression);
						if (conditionalLoopDataStructureExpression instanceof SimpleName)
						{
							SimpleName simpleName = (SimpleName) conditionalLoopDataStructureExpression;
							List<SimpleName> occurrencesOfSimpleName = getOccurrencesOfSimpleName(nodeConditionalLoop.getLoopBody(), simpleName);
							for (SimpleName name : occurrencesOfSimpleName)
							{
								matcher.compareTypes(name, enhancedForLoopDataStructureExpression);
							}
						}
						if (enhancedForLoopDataStructureExpression instanceof SimpleName)
						{
							SimpleName simpleName = (SimpleName) enhancedForLoopDataStructureExpression;
							List<SimpleName> occurrencesOfSimpleName = getOccurrencesOfSimpleName(otherEnhancedForLoop.getLoopBody(), simpleName);
							for (SimpleName name : occurrencesOfSimpleName)
							{
								matcher.compareTypes(conditionalLoopDataStructureExpression, name);
							}
						}
					}
				}
				if (nodeLoop instanceof ConditionalLoop && otherLoop instanceof ConditionalLoop)
				{
					ConditionalLoop nodeConditionalLoop  = (ConditionalLoop)nodeLoop;
					ConditionalLoop otherConditionalLoop  = (ConditionalLoop)otherLoop;
					Collection<AbstractControlVariable> nodeConditionControlVariables = nodeConditionalLoop.getConditionControlVariables().values();
					Collection<AbstractControlVariable> otherConditionControlVariables = otherConditionalLoop.getConditionControlVariables().values();
					if (!nodeConditionControlVariables.isEmpty() && !otherConditionControlVariables.isEmpty())
					{
						ControlVariable nodeConditionalLoopControlVariable = (ControlVariable)nodeConditionControlVariables.toArray()[0];
						ControlVariable otherConditionalLoopControlVariable = (ControlVariable)otherConditionControlVariables.toArray()[0];
						Expression nodeConditionalLoopDataStructureExpression = nodeConditionalLoopControlVariable.getDataStructureExpression();
						Expression otherConditionalLoopDataStructureExpression = otherConditionalLoopControlVariable.getDataStructureExpression();
						matcher.compareTypes(nodeConditionalLoopDataStructureExpression, otherConditionalLoopDataStructureExpression);
						if (nodeConditionalLoopDataStructureExpression instanceof SimpleName)
						{
							SimpleName simpleName = (SimpleName) nodeConditionalLoopDataStructureExpression;
							List<SimpleName> occurrencesOfSimpleName = getOccurrencesOfSimpleName(nodeConditionalLoop.getLoopBody(), simpleName);
							for (SimpleName name : occurrencesOfSimpleName)
							{
								matcher.compareTypes(name, otherConditionalLoopDataStructureExpression);
							}
						}
						if (otherConditionalLoopDataStructureExpression instanceof SimpleName)
						{
							SimpleName simpleName = (SimpleName) otherConditionalLoopDataStructureExpression;
							List<SimpleName> occurrencesOfSimpleName = getOccurrencesOfSimpleName(otherConditionalLoop.getLoopBody(), simpleName);
							for (SimpleName name : occurrencesOfSimpleName)
							{
								matcher.compareTypes(nodeConditionalLoopDataStructureExpression, name);
							}
						}
						ASTNode nodeDataStructureAccessExpression = nodeConditionalLoopControlVariable.getDataStructureAccessExpression();
						ASTNode otherDataStructureAccessExpression = otherConditionalLoopControlVariable.getDataStructureAccessExpression();
						if (nodeDataStructureAccessExpression != null && otherDataStructureAccessExpression != null)
						{
							if (nodeDataStructureAccessExpression instanceof MethodInvocation &&
									otherDataStructureAccessExpression instanceof MethodInvocation)
							{
								MethodInvocation nodeDataStructureAccessMethodInvocation = (MethodInvocation)nodeDataStructureAccessExpression;
								MethodInvocation otherDataStructureAccessMethodInvocation = (MethodInvocation)otherDataStructureAccessExpression;
								if (nodeDataStructureAccessMethodInvocation.resolveMethodBinding().isEqualTo(otherDataStructureAccessMethodInvocation.resolveMethodBinding()))
								{
									safeSubtreeListMatch(nodeDataStructureAccessMethodInvocation.arguments(), otherDataStructureAccessMethodInvocation.arguments());
								}
							}
							else
							{
								SimpleName nodeVariableInitializedUsingControlVariable = AbstractLoopUtilities.getVariableInitializedUsingControlVariable(nodeConditionalLoopControlVariable, nodeConditionalLoop.getLoopBody());
								SimpleName otherVariableInitializedUsingControlVariable = AbstractLoopUtilities.getVariableInitializedUsingControlVariable(otherConditionalLoopControlVariable, otherConditionalLoop.getLoopBody());
								safeSubtreeMatch(nodeVariableInitializedUsingControlVariable, otherVariableInitializedUsingControlVariable);
							}
						}
					}
				}
				ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
				reportAdditionalFragments(otherLoop, this.additionallyMatchedFragments2);
				if (nodeLoop instanceof EnhancedForLoop && otherLoop instanceof ConditionalLoop)
				{
					EnhancedForLoop nodeEnhancedForLoop = (EnhancedForLoop)nodeLoop;
					ConditionalLoop otherConditionalLoop = (ConditionalLoop)otherLoop;
					SimpleName enhancedForLoopParameter  = ((EnhancedForStatement)nodeLoop.getLoopStatement()).getParameter().getName();
					Collection<AbstractControlVariable> otherConditionControlVariables = otherConditionalLoop.getConditionControlVariables().values();
					if (!otherConditionControlVariables.isEmpty())
					{
						ControlVariable conditionalLoopControlVariable = (ControlVariable)otherConditionControlVariables.toArray()[0];
						SimpleName variableInitializedUsingControlVariable = AbstractLoopUtilities.getVariableInitializedUsingControlVariable(conditionalLoopControlVariable, otherConditionalLoop.getLoopBody());
						safeSubtreeMatch(enhancedForLoopParameter, variableInitializedUsingControlVariable);
						Expression enhancedForLoopDataStructureExpression = nodeEnhancedForLoop.getControlVariable().getDataStructureExpression();
						Expression conditionalLoopDataStructureExpression = conditionalLoopControlVariable.getDataStructureExpression();
						matcher.compareTypes(enhancedForLoopDataStructureExpression, conditionalLoopDataStructureExpression);
						if (enhancedForLoopDataStructureExpression instanceof SimpleName)
						{
							SimpleName simpleName = (SimpleName) enhancedForLoopDataStructureExpression;
							List<SimpleName> occurrencesOfSimpleName = getOccurrencesOfSimpleName(nodeEnhancedForLoop.getLoopBody(), simpleName);
							for (SimpleName name : occurrencesOfSimpleName)
							{
								matcher.compareTypes(name, conditionalLoopDataStructureExpression);
							}
						}
						if (conditionalLoopDataStructureExpression instanceof SimpleName)
						{
							SimpleName simpleName = (SimpleName) conditionalLoopDataStructureExpression;
							List<SimpleName> occurrencesOfSimpleName = getOccurrencesOfSimpleName(otherConditionalLoop.getLoopBody(), simpleName);
							for (SimpleName name : occurrencesOfSimpleName)
							{
								matcher.compareTypes(enhancedForLoopDataStructureExpression, name);
							}
						}
					}
				}
				for (ASTNodeDifference currentDifference : matcher.getDifferences())
				{
					addDifference(currentDifference);
				}
				return true;
			}
		}
		return false;
	}

	private static List<SimpleName> getOccurrencesOfSimpleName(ASTNode node, SimpleName simpleName)
	{
		List<SimpleName> returnList = new ArrayList<SimpleName>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> simpleNames = new ArrayList<Expression>();
		if (node instanceof Expression)
		{
			simpleNames.addAll(expressionExtractor.getVariableInstructions((Expression)node));
		}
		else if (node instanceof Statement)
		{
			simpleNames.addAll(expressionExtractor.getVariableInstructions((Statement)node));
		}
		for (Expression currentExpression : simpleNames)
		{
			SimpleName currentSimpleName = (SimpleName)currentExpression;
			IBinding currentSimpleNameBinding = currentSimpleName.resolveBinding();
			if (currentSimpleNameBinding != null && currentSimpleNameBinding.isEqualTo(simpleName.resolveBinding()))
			{
				returnList.add(currentSimpleName);
			}
		}
		return returnList;
	}

	private static AbstractLoop generateAbstractLoop(Object object)
	{
		if (object instanceof ForStatement)
		{
			return new ConditionalLoop((ForStatement) object);
		}
		else if (object instanceof WhileStatement)
		{
			return new ConditionalLoop((WhileStatement) object);
		}
		else if (object instanceof DoStatement)
		{
			return new ConditionalLoop((DoStatement) object);
		}
		else if (object instanceof EnhancedForStatement)
		{
			return new EnhancedForLoop((EnhancedForStatement) object);
		}
		return null;
	}

	private void reportAdditionalFragments(AbstractLoop abstractLoop, List<AbstractMethodFragment> fragmentList)
	{
		List<ASTNode> additionalFragements = abstractLoop.getAdditionalFragments();
		reportAdditionalFragments(additionalFragements, fragmentList);
	}

	private boolean ifMatch(AbstractControlStructure nodeControlStructure, Object other)
	{
		AbstractControlStructure otherControlStructure = generateAbstractControlStructure(other);
		if (otherControlStructure != null)
		{
			boolean ifMatch = nodeControlStructure.match(otherControlStructure, this);
			if (ifMatch)
			{
				ASTInformationGenerator.setCurrentITypeRoot(typeRoot1);
				reportAdditionalFragments(nodeControlStructure, this.additionallyMatchedFragments1);
				ASTInformationGenerator.setCurrentITypeRoot(typeRoot2);
				reportAdditionalFragments(otherControlStructure, this.additionallyMatchedFragments2);
				return true;
			}
		}
		return false;
	}

	private static AbstractControlStructure generateAbstractControlStructure(Object object)
	{
		if (object instanceof IfStatement)
		{
			return new IfControlStructure((IfStatement) object);
		}
		else if (object instanceof SwitchStatement)
		{
			return new SwitchControlStructure((SwitchStatement) object);
		}
		else if (object instanceof ExpressionStatement)
		{
			ExpressionStatement expressionStatement = (ExpressionStatement) object;
			if (AbstractControlStructureUtilities.hasOneConditionalExpression(expressionStatement) != null)
			{
				return new TernaryControlStructure(expressionStatement);
			}
		}
		else if (object instanceof ReturnStatement)
		{
			ReturnStatement returnStatement = (ReturnStatement) object;
			if (returnStatement.getExpression() instanceof ConditionalExpression)
			{
				return new TernaryControlStructure(returnStatement);
			}
		}
		return null;
	}
	
	private void reportAdditionalFragments(AbstractControlStructure abstractControlStructure, List<AbstractMethodFragment> fragmentList)
	{
		List<ASTNode> additionalFragements = abstractControlStructure.getAdditionalFragments();
		reportAdditionalFragments(additionalFragements, fragmentList);
	}

	private void reportAdditionalFragments(List<ASTNode> additionalFragements, List<AbstractMethodFragment> fragmentList)
	{
		for(ASTNode currentFragment : additionalFragements)
		{
			ASTNode parent = currentFragment.getParent();
			if (currentFragment instanceof ExpressionStatement)
			{
				ExpressionStatement expressionStatement = (ExpressionStatement)currentFragment;
				StatementObject statementObject = new StatementObject(expressionStatement, StatementType.EXPRESSION, null);
				fragmentList.add(statementObject);
			}
			else if (currentFragment instanceof ReturnStatement)
			{
				ReturnStatement returnStatement = (ReturnStatement)currentFragment;
				StatementObject statementObject = new StatementObject(returnStatement, StatementType.RETURN, null);
				fragmentList.add(statementObject);
			}
			else if(parent instanceof ExpressionStatement)
			{
				ExpressionStatement expressionStatement = (ExpressionStatement)parent;
				StatementObject updaterStatementObject = new StatementObject(expressionStatement, StatementType.EXPRESSION, null);
				fragmentList.add(updaterStatementObject);
			}
			else if(parent instanceof VariableDeclarationStatement)
			{
				VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)parent;
				if (variableDeclarationStatement.fragments().size() == 1)
				{
					StatementObject updaterStatementObject = new StatementObject(variableDeclarationStatement, StatementType.VARIABLE_DECLARATION, null);
					fragmentList.add(updaterStatementObject);
				}
			}
			else if(parent instanceof CastExpression && parent.getParent() instanceof VariableDeclarationFragment)
			{
				List<ASTNode> nodes = new ArrayList<ASTNode>();
				nodes.add(parent.getParent());
				reportAdditionalFragments(nodes, fragmentList);
			}
			else if (currentFragment instanceof Expression)
			{
				Expression currentExpression = (Expression) currentFragment;
				AbstractExpression expressionObject = new AbstractExpression(currentExpression);
				fragmentList.add(expressionObject);
			}
		}
	}
}
