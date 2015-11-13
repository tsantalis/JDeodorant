package gr.uom.java.ast.decomposition.matching.loop;

import gr.uom.java.ast.decomposition.matching.conditional.AbstractControlStructureUtilities;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.StatementExtractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

@SuppressWarnings("unchecked")
public class AbstractLoopUtilities
{
	public static boolean isUpdatingVariable(Expression updater, SimpleName variable)
	{
		if (updater instanceof PrefixExpression)
		{
			PrefixExpression prefixExpression = (PrefixExpression) updater;
			return isSameVariable(prefixExpression.getOperand(), variable);
		}
		else if (updater instanceof PostfixExpression)
		{
			PostfixExpression postfixExpression = (PostfixExpression) updater;
			return isSameVariable(postfixExpression.getOperand(), variable);			
		}
		else if (updater instanceof Assignment)
		{
			Assignment assignment = (Assignment) updater;
			return isSameVariable(assignment.getLeftHandSide(), variable);
		}
		else if (updater instanceof MethodInvocation)
		{
			MethodInvocation methodInvocation = (MethodInvocation) updater;
			return isSameVariable(methodInvocation.getExpression(), variable);
		}
		return false;
	}
	
	private static boolean isSameVariable(Expression expression, SimpleName variable)
	{
		if (expression instanceof SimpleName)
		{
			SimpleName simpleNameOperand = (SimpleName) expression;
			return variable.resolveBinding().isEqualTo(simpleNameOperand.resolveBinding());
		}
		return false;
	}
	
	public static boolean isCollectionSizeInvocation(Expression expression)
	{
		if (expression instanceof MethodInvocation)
		{
			MethodInvocation methodInvocation = (MethodInvocation) expression;
			Expression callingExpression      = methodInvocation.getExpression();
			IMethodBinding methodBinding      = methodInvocation.resolveMethodBinding();
			if (methodBinding != null && callingExpression != null)
			{
				return (methodBinding.getName().equals("size") && AbstractLoopUtilities.isCollection(callingExpression.resolveTypeBinding()));
			}
		}
		return false;
	}
	
	public static boolean isDataStructureSizeInvocation(Expression expression)
	{
		if (expression instanceof MethodInvocation)
		{
			MethodInvocation methodInvocation = (MethodInvocation) expression;
			Expression callingExpression      = methodInvocation.getExpression();
			IMethodBinding methodBinding      = methodInvocation.resolveMethodBinding();
			if (methodBinding != null && callingExpression != null)
			{
				AbstractLoopBindingInformation bindingInformation = AbstractLoopBindingInformation.getInstance();
				return bindingInformation.dataStructureSizeMethodContains(methodBinding.getKey());
			}
		}
		return false;
	}
	
	public static boolean isLengthFieldAccess(Expression expression)
	{
		SimpleName name          = null;
		ITypeBinding typeBinding = null;
		if (expression instanceof QualifiedName)
		{
			QualifiedName qualifiedName = (QualifiedName) expression;
			name                        = qualifiedName.getName();
			Name qualifier              = qualifiedName.getQualifier();
			typeBinding                 = qualifier.resolveTypeBinding();
		}
		else if (expression instanceof FieldAccess)
		{
			FieldAccess fieldAccess           = (FieldAccess)expression;
			name                              = fieldAccess.getName();
			Expression fieldAsccessExpression = fieldAccess.getExpression();
			typeBinding                       = fieldAsccessExpression.resolveTypeBinding();
		}
		if (name != null && typeBinding != null)
		{
			IBinding nameBinding = name.resolveBinding();
			if (nameBinding != null && nameBinding.getKind() == IBinding.VARIABLE && typeBinding != null)
			{
				IVariableBinding nameVariableBinding = (IVariableBinding) nameBinding;
				return (nameVariableBinding.getName().equals("length") && typeBinding.isArray());
			}
		}
		return false;
	}
	
	public static boolean isCollection(ITypeBinding typeBinding)
	{
		return isSubclassOf(typeBinding, "java.util.AbstractCollection") || isSubinterfaceOf(typeBinding, "java.util.Collection") ||
				isSubinterfaceOf(typeBinding, "java.lang.Iterable");
	}
	
	public static boolean isSubclassOf(ITypeBinding typeBinding, String qualifiedName)
	{
		do
		{
			if (typeBinding.getQualifiedName().startsWith(qualifiedName))
			{
				return true;
			}
			typeBinding = typeBinding.getSuperclass();
		} while (typeBinding != null);
		return false;
	}
	
	public static boolean isSubinterfaceOf(ITypeBinding typeBinding, String qualifiedName)
	{
		if (typeBinding.getQualifiedName().startsWith(qualifiedName))
		{
			return true;
		}
		else
		{
			ITypeBinding[] superInterfaces = typeBinding.getInterfaces();
			for (ITypeBinding superInterface : superInterfaces)
			{
				if (isSubinterfaceOf(superInterface, qualifiedName))
				{
					return true;
				}
			}
			return false;
		}
	}

	public static VariableDeclaration getVariableDeclaration(SimpleName variable)
	{
		VariableDeclaration variableDeclaration        = null;
		MethodDeclaration method                       = findParentMethodDeclaration(variable);
		List<VariableDeclaration> variableDeclarations = getAllVariableDeclarations(method);
		// find the variable's initializer
		IBinding binding = variable.resolveBinding();
		if (binding.getKind() == IBinding.VARIABLE)
		{
			IVariableBinding variableBinding = (IVariableBinding) binding;
			for (VariableDeclaration currentVariableDeclaration : variableDeclarations)
			{
				IVariableBinding currentVariableDeclarationBinding = currentVariableDeclaration.resolveBinding();
				if (currentVariableDeclarationBinding.isEqualTo(variableBinding))
				{
					variableDeclaration = currentVariableDeclaration;
					break;
				}
			}
		}
		return variableDeclaration;
	}
	
	private static List<VariableDeclaration> getAllVariableDeclarations(MethodDeclaration method)
	{
		StatementExtractor statementExtractor            = new StatementExtractor();
		ExpressionExtractor expressionExtractor          = new ExpressionExtractor();
		List<SingleVariableDeclaration> methodParameters = method.parameters();
		Block methodBody                                 = method.getBody();
		List<Statement> variableDeclarationStatements    = statementExtractor.getVariableDeclarationStatements(methodBody);
		List<Expression> variableDeclarationExpressions  = expressionExtractor.getVariableDeclarationExpressions(methodBody);
		List<Statement> enhancedForStatements            = statementExtractor.getEnhancedForStatements(methodBody);
		List<VariableDeclaration> variableDeclarations   = new ArrayList<VariableDeclaration>(methodParameters);
		if (method.getParent() instanceof TypeDeclaration)
		{
			TypeDeclaration typeDeclaration = (TypeDeclaration) method.getParent();
			FieldDeclaration[] fieldDeclarations = typeDeclaration.getFields();
			for (FieldDeclaration fieldDeclaration : fieldDeclarations)
			{
				List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
				variableDeclarations.addAll(fragments);
			}
		}
		for (Statement currentStatement : variableDeclarationStatements)
		{
			if (currentStatement instanceof VariableDeclarationStatement)
			{
				VariableDeclarationStatement currentVariableDeclarationStatement = (VariableDeclarationStatement) currentStatement;
				variableDeclarations.addAll(currentVariableDeclarationStatement.fragments());
			}
		}
		for (Expression currentExpression : variableDeclarationExpressions)
		{
			if (currentExpression instanceof VariableDeclarationExpression)
			{
				VariableDeclarationExpression currentVariableDeclarationExpression = (VariableDeclarationExpression) currentExpression;
				variableDeclarations.addAll(currentVariableDeclarationExpression.fragments());
			}
		}
		for (Statement currentStatement : enhancedForStatements)
		{
			if (currentStatement instanceof EnhancedForStatement)
			{
				EnhancedForStatement currentEnhancedForStatement = (EnhancedForStatement) currentStatement;
				variableDeclarations.add(currentEnhancedForStatement.getParameter());
			}
		}
		return variableDeclarations;
	}

	public static MethodDeclaration findParentMethodDeclaration(ASTNode node)
	{
		MethodDeclaration parentMethodDeclaration = null;
		ASTNode parent = node.getParent();
		while (parent != null)
		{
			if (parent instanceof MethodDeclaration)
			{
				parentMethodDeclaration = (MethodDeclaration) parent;
				break;
			}
			parent = parent.getParent();
		}
		return parentMethodDeclaration;
	}
	
	// returns null if the main variable (LHS in an assignment, expression in postfix, prefix, or methodInvocation) is not being UPDATED
	public static Integer getUpdateValue(Expression updater)
	{
		if (updater instanceof PrefixExpression || updater instanceof PostfixExpression)
		{
			return AbstractLoopUtilities.getIncrementValue(updater);
		}
		else if (updater instanceof Assignment)
		{
			Assignment assignment = (Assignment) updater;
			return assignmentUpdateValue(assignment);
		}
		else if (updater instanceof MethodInvocation)
		{
			MethodInvocation methodInvocation = (MethodInvocation) updater;
			AbstractLoopBindingInformation bindingInformation = AbstractLoopBindingInformation.getInstance();
			return bindingInformation.getUpdateMethodValue(methodInvocation.resolveMethodBinding().getMethodDeclaration().getKey());
		}
		return null;
	}

	// returns null if specified expression is not a Prefix or PostfixExpression
	private static Integer getIncrementValue(Expression expression)
	{
		Integer incrementValue = null;
		String operator = null;
		if (expression instanceof PrefixExpression)
		{
			PrefixExpression prefixExpression  = (PrefixExpression) expression;
			operator = prefixExpression.getOperator().toString();
		}
		else if (expression instanceof PostfixExpression)
		{
			PostfixExpression postfixExpression = (PostfixExpression) expression;
			operator = postfixExpression.getOperator().toString();
		}
		if (operator != null && operator.equals("++"))
		{
			incrementValue = 1;
		}
		else if (operator != null && operator.equals("--"))
		{
			incrementValue = (-1);
		}
		return incrementValue;
	}

	// returns null if the assignment is not UPDATING the variable on the left hand side or if the updateValue cannot be evaluated to an integer
	private static Integer assignmentUpdateValue(Assignment assignment)
	{
		Integer updateValue          = null;
		Expression leftHandSide      = assignment.getLeftHandSide();
		Assignment.Operator operator = assignment.getOperator();
		Expression rightHandSide     = assignment.getRightHandSide();
		if (operator == Assignment.Operator.PLUS_ASSIGN)
		{			
			updateValue = AbstractLoopUtilities.getIntegerValue(rightHandSide);
		}
		else if (operator == Assignment.Operator.MINUS_ASSIGN)
		{
			Integer rightHandSideIntegerValue = AbstractLoopUtilities.getIntegerValue(rightHandSide);
			if (rightHandSideIntegerValue != null)
			{
				updateValue = (-1) * rightHandSideIntegerValue;
			}
		}
		else if (leftHandSide instanceof SimpleName && operator == Assignment.Operator.ASSIGN && rightHandSide instanceof InfixExpression)
		{
			SimpleName leftHandSideSimpleName      = (SimpleName) leftHandSide;
			IBinding leftHandSideBinding           = leftHandSideSimpleName.resolveBinding();
			InfixExpression infixExpression        = (InfixExpression) rightHandSide;
			InfixExpression.Operator infixOperator = infixExpression.getOperator();
			Expression rightOperand                = infixExpression.getRightOperand();
			Expression leftOperand                 = infixExpression.getLeftOperand();
			if (infixOperator.toString().equals("+") || infixOperator.toString().equals("-"))
			{
				if (leftOperand instanceof SimpleName)
				{
					SimpleName leftOperandSimpleName = (SimpleName) leftOperand;
					IBinding leftOperandBinding      = leftOperandSimpleName.resolveBinding();
					if (leftOperandBinding.isEqualTo(leftHandSideBinding))
					{
						Integer rightOperandIntegerValue = AbstractLoopUtilities.getIntegerValue(rightOperand);
						if (infixOperator.toString().equals("+") && rightOperandIntegerValue != null)
						{
							updateValue = rightOperandIntegerValue;
						}
						else if (infixOperator.toString().equals("-") && rightOperandIntegerValue != null)
						{
							updateValue = (-1) * rightOperandIntegerValue;
						}
					}
				}
				else if (rightOperand instanceof SimpleName)
				{
					SimpleName rightOperandSimpleName = (SimpleName) rightOperand;
					IBinding rightOperandBinding      = rightOperandSimpleName.resolveBinding();
					if (rightOperandBinding.isEqualTo(leftHandSideBinding))
					{
						Integer leftOperandIntegerValue = AbstractLoopUtilities.getIntegerValue(leftOperand);
						if (infixOperator.toString().equals("+") && leftOperandIntegerValue != null)
						{
							updateValue = leftOperandIntegerValue;
						}
					}
				}
			}
		}
		return updateValue;
	}
	
	// returns null if specified expression cannot be evaluated to an Integer
	public static Integer getIntegerValue(Expression expression)
	{
		Integer numberLiteralValue = null;
		if (expression instanceof NumberLiteral)
		{
			NumberLiteral numberLiteral = (NumberLiteral) expression;
			try
			{
				numberLiteralValue = Integer.parseInt(numberLiteral.getToken());
			}
			catch (NumberFormatException e) {}
		}
		else if (expression instanceof PrefixExpression)
		{
			PrefixExpression prefixExpression        = (PrefixExpression) expression;
			PrefixExpression.Operator prefixOperator = prefixExpression.getOperator();
			Expression operand                       = prefixExpression.getOperand();
			if (operand instanceof NumberLiteral && (prefixOperator.toString().equals("+") || prefixOperator.toString().equals("-")))
			{
				NumberLiteral numberLiteral = (NumberLiteral) operand;
				try
				{
					numberLiteralValue = Integer.parseInt(prefixOperator.toString() + numberLiteral.getToken());
				}
				catch (NumberFormatException e) {}
			}
		}
		return numberLiteralValue;
	}
	
	// returns a boolean indicating which side of the specified infixExpression the specified variable is on
	// returns null if it is on neither side
	public static boolean isVariableLeftOperand(SimpleName variable, InfixExpression infixExpression)
	{
		boolean leftOperandIsVariable     = false;
		Expression leftOperand            = infixExpression.getLeftOperand();
		//Expression rightOperand           = infixExpression.getRightOperand();
		IBinding infixVariableBinding     = variable.resolveBinding();
		if (leftOperand instanceof SimpleName)
		{
			SimpleName leftOperandSimpleName = (SimpleName) leftOperand;
			IBinding leftOperandBinding      = leftOperandSimpleName.resolveBinding();
			if (leftOperandBinding.isEqualTo(infixVariableBinding))
			{
				leftOperandIsVariable = true;
			}
		}
		else if (leftOperand instanceof InfixExpression)
		{
			InfixExpression infixLeftOperand = (InfixExpression) leftOperand;
			boolean left = isVariableLeftOperand(variable, infixLeftOperand);
			boolean right = isVariableRightOperand(variable, infixLeftOperand);
			List<Expression> extendedOperands = infixLeftOperand.extendedOperands();
			boolean variableFoundInExtendedOperands = false;
			for (Expression expression : extendedOperands)
			{
				if (expression instanceof SimpleName)
				{
					SimpleName simpleName = (SimpleName) expression;
					IBinding simpleNameBinding = simpleName.resolveBinding();
					if (simpleNameBinding.isEqualTo(infixVariableBinding))
					{
						variableFoundInExtendedOperands = true;
						break;
					}
				}
			}
			if (left || right || variableFoundInExtendedOperands)
			{
				leftOperandIsVariable = true;
			}
		}
		return leftOperandIsVariable;
	}
	
	private static boolean isVariableRightOperand(SimpleName variable, InfixExpression infixExpression)
	{
		boolean rightOperandIsVariable     = false;
		//Expression leftOperand            = infixExpression.getLeftOperand();
		Expression rightOperand           = infixExpression.getRightOperand();
		IBinding infixVariableBinding     = variable.resolveBinding();
		if (rightOperand instanceof SimpleName)
		{
			SimpleName rightOperandSimpleName = (SimpleName) rightOperand;
			IBinding rightOperandBinding      = rightOperandSimpleName.resolveBinding();
			if (rightOperandBinding.isEqualTo(infixVariableBinding))
			{
				rightOperandIsVariable = true;
			}
		}
		else if (rightOperand instanceof InfixExpression)
		{
			InfixExpression infixRightOperand = (InfixExpression) rightOperand;
			boolean left = isVariableLeftOperand(variable, infixRightOperand);
			boolean right = isVariableRightOperand(variable, infixRightOperand);
			List<Expression> extendedOperands = infixRightOperand.extendedOperands();
			boolean variableFoundInExtendedOperands = false;
			for (Expression expression : extendedOperands)
			{
				if (expression instanceof SimpleName)
				{
					SimpleName simpleName = (SimpleName) expression;
					IBinding simpleNameBinding = simpleName.resolveBinding();
					if (simpleNameBinding.isEqualTo(infixVariableBinding))
					{
						variableFoundInExtendedOperands = true;
						break;
					}
				}
			}
			if (left || right || variableFoundInExtendedOperands)
			{
				rightOperandIsVariable = true;
			}
		}
		return rightOperandIsVariable;
	}
	
	public static List<Statement> unBlock(List<Statement> statements)
	{
		List<Statement> returnList = new ArrayList<Statement>();
		for (Statement currentStatement : statements)
		{
			if (currentStatement instanceof Block)
			{
				List<Statement> subList = ((Block)currentStatement).statements();
				returnList.addAll(unBlock(subList));
			}
			else if (currentStatement instanceof TryStatement)
			{
				List<Statement> subList = ((TryStatement)currentStatement).getBody().statements();
				returnList.addAll(unBlock(subList));
			}
			else
			{
				returnList.add(currentStatement);
			}
		}
		return returnList;
	}
	
	// this method finds the first variable to be initialized (if any) using the control variable to access the data structure it is traversing (if any)
	public static SimpleName getVariableInitializedUsingControlVariable(ControlVariable controlVariable, Statement conditionalLoopBody)
	{
		SimpleName initializedVariableName                                 = null;
		List<ASTNode> variableDeclarationsAndAssignmentsContainingVariable = getVariableDeclarationsAndAssignmentsContainingAccessUsingVariable(conditionalLoopBody, controlVariable);
		// find the node in variableDeclarationsAndAssignmentsContainingVariable that has the smallest start position, compare new variable created to the enhancedForVariable and then return it
		if (variableDeclarationsAndAssignmentsContainingVariable.size() > 0)
		{
			Collections.sort(variableDeclarationsAndAssignmentsContainingVariable, new EarliestStartPositionComparator());
			ASTNode nodeContainingVariable = variableDeclarationsAndAssignmentsContainingVariable.get(0);
			if (nodeContainingVariable instanceof VariableDeclaration)
			{
				initializedVariableName = ((VariableDeclaration)nodeContainingVariable).getName();
			}
			else if (nodeContainingVariable instanceof Assignment && ((Assignment)nodeContainingVariable).getLeftHandSide() instanceof SimpleName)
			{
				initializedVariableName = (SimpleName)((Assignment)nodeContainingVariable).getLeftHandSide();
			}
		}
		return initializedVariableName;
	}
	
	public static List<ASTNode> getVariableDeclarationsAndAssignmentsContainingAccessUsingVariable(Statement body, ControlVariable variable)
	{
		StatementExtractor statementExtractor = new StatementExtractor();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		ArrayList<ASTNode> returnList = new ArrayList<ASTNode>();
		List<Statement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(body);
		List<Expression> assignments = expressionExtractor.getAssignments(body);
		for (Statement currentStatement : variableDeclarationStatements)
		{
			List<VariableDeclarationFragment> fragments = ((VariableDeclarationStatement)currentStatement).fragments();
			for (VariableDeclarationFragment fragment : fragments)
			{
				Expression initializer = fragment.getInitializer();
				if (isAccessUsingVariable(initializer, variable))
				{
					returnList.add(fragment);
				}
			}
		}
		for (Expression currentExpression : assignments)
		{
			Assignment currentAssignment = (Assignment)currentExpression;
			Expression rightHandSide = currentAssignment.getRightHandSide();
			if (isAccessUsingVariable(rightHandSide, variable))
			{
				returnList.add(currentAssignment);
			}
		}
		List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(body);
		for (Expression expression : methodInvocations)
		{
			if (expression instanceof MethodInvocation)
			{
				MethodInvocation methodInvocation = (MethodInvocation)expression;
				Expression methodInvocationExpression = methodInvocation.getExpression();
				if(methodInvocationExpression != null)
				{
					if (isAccessUsingVariable(methodInvocationExpression, variable))
					{
						returnList.add(methodInvocation);
					}
				}
			}
		}
		return returnList;
	}
	
	private static boolean isAccessUsingVariable(Expression expression, ControlVariable controlVariable)
	{
		List<SimpleName> variableOccurrences = getOccurrencesOfSimpleName(expression, controlVariable.getVariable());
		Expression controlVariableDataStructure = controlVariable.getDataStructureExpression();
		if (variableOccurrences.size() == 1)
		{
			SimpleName variableOccurrence = variableOccurrences.get(0);
			ASTNode firstParent = variableOccurrence.getParent();
			ASTNode secondParent = variableOccurrence.getParent().getParent();
			if (firstParent != null && secondParent != null)
			{
				// if expression is an array access
				if (expression instanceof ArrayAccess)
				{
					ArrayAccess arrayAccess = (ArrayAccess)expression;
					Expression array = arrayAccess.getArray();
					if ((firstParent.equals(arrayAccess) || (firstParent instanceof PostfixExpression && secondParent.equals(arrayAccess))))
					{
						if (array instanceof Name  && controlVariableDataStructure instanceof Name)
						{
							Name controlVariableDataStructureName = (Name)controlVariableDataStructure;
							return ((Name)array).resolveBinding().isEqualTo(controlVariableDataStructureName.resolveBinding());
						}
						else if (array instanceof MethodInvocation  && controlVariableDataStructure instanceof MethodInvocation)
						{
							MethodInvocation controlVariableDataStructureMethodInvocation = (MethodInvocation)controlVariableDataStructure;
							return ((MethodInvocation)array).resolveMethodBinding().isEqualTo(controlVariableDataStructureMethodInvocation.resolveMethodBinding());
						}
					}
				}
				// if expression is a method invocation
				else if (expression instanceof MethodInvocation)
				{
					MethodInvocation methodInvocation                 = (MethodInvocation)expression;
					IMethodBinding methodBinding                      = methodInvocation.resolveMethodBinding().getMethodDeclaration();
					AbstractLoopBindingInformation bindingInformation = AbstractLoopBindingInformation.getInstance();
					if (methodInvocation.getExpression() != null)
					{
						// if the variable is the expression of the method (ex: variable is an iterator)
						if (methodInvocation.getExpression().equals(variableOccurrence))
						{
							return bindingInformation.updateMethodValuesContains(methodBinding.getKey());
						}
						// if the variable is an argument of the method OR (the first parent is a postfix expression AND an argument of the method)
						else if (isExpressionAnArgument(variableOccurrence, methodInvocation) || (firstParent instanceof PostfixExpression && isExpressionAnArgument((PostfixExpression)firstParent, methodInvocation)))
						{
							if (bindingInformation.dataStructureAccessMethodsContains(methodBinding.getKey()))
							{
								// check that the expression is the variables traversed data structure
								if (methodInvocation.getExpression() instanceof Name && controlVariableDataStructure instanceof Name)
								{
									Name methodExpressionName = (Name)methodInvocation.getExpression();
									Name controlVariableDataStructureName = (Name)controlVariableDataStructure;
									return methodExpressionName.resolveBinding().isEqualTo(controlVariableDataStructureName.resolveBinding());
								}
								else if (methodInvocation.getExpression() instanceof MethodInvocation && controlVariableDataStructure instanceof MethodInvocation)
								{
									MethodInvocation methodExpressionMethodInvocation = (MethodInvocation)methodInvocation.getExpression();
									MethodInvocation controlVariableDataStructureMethodInvocation = (MethodInvocation)controlVariableDataStructure;
									return methodExpressionMethodInvocation.resolveMethodBinding().isEqualTo(controlVariableDataStructureMethodInvocation.resolveMethodBinding());
								}
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	private static List<SimpleName> getOccurrencesOfSimpleName(Expression expression, SimpleName simpleName)
	{
		List<SimpleName> returnList = new ArrayList<SimpleName>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> simpleNames = expressionExtractor.getVariableInstructions(expression);
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
	
	private static boolean isExpressionAnArgument(Expression expression, MethodInvocation methodInvocation)
	{
		List<Expression> arguments = methodInvocation.arguments();
		for (Expression currentArgument : arguments)
		{
			Expression unparenthesizedArgument = AbstractControlStructureUtilities.unparenthesize(currentArgument);
			if (currentArgument.equals(expression) || unparenthesizedArgument.equals(expression))
			{
				return true;
			}
		}
		return false;
	}
}
