package gr.uom.java.ast.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class ExpressionExtractor {
	ExpressionInstanceChecker instanceChecker;
	
	// returns a List of SimpleName objects
	public List<Expression> getVariableInstructions(Statement statement) {
		instanceChecker = new InstanceOfSimpleName();
		return getExpressions(statement);
	}

	// returns a List of SimpleName objects
	public List<Expression> getVariableInstructions(Expression expression) {
		instanceChecker = new InstanceOfSimpleName();
		return getExpressions(expression);
	}
	
	// returns a List of MethodInvocation and SuperMethodInvocation objects
	public List<Expression> getMethodInvocations(Statement statement) {
		instanceChecker = new InstanceOfMethodInvocation();
		return getExpressions(statement);
	}
	
	// returns a List of SuperMethodInvocation objects
	public List<Expression> getSuperMethodInvocations(Statement statement) {
		instanceChecker = new InstanceOfSuperMethodInvocation();
		return getExpressions(statement);
	}
	
	// returns a List of MethodInvocation and SuperMethodInvocation objects
	public List<Expression> getMethodInvocations(Expression expression) {
		instanceChecker = new InstanceOfMethodInvocation();
		return getExpressions(expression);
	}
	
	// returns a List of SuperMethodInvocation objects
	public List<Expression> getSuperMethodInvocations(Expression expression) {
		instanceChecker = new InstanceOfSuperMethodInvocation();
		return getExpressions(expression);
	}

	// returns a List of FieldAccess objects
	public List<Expression> getFieldAccesses(Statement statement) {
		instanceChecker = new InstanceOfFieldAccess();
		return getExpressions(statement);
	}
	
	// returns a List of SuperFieldAccess objects
	public List<Expression> getSuperFieldAccesses(Statement statement) {
		instanceChecker = new InstanceOfSuperFieldAccess();
		return getExpressions(statement);
	}
	
	// returns a List of FieldAccess objects
	public List<Expression> getFieldAccesses(Expression expression) {
		instanceChecker = new InstanceOfFieldAccess();
		return getExpressions(expression);
	}
	
	// returns a List of SuperFieldAccess objects
	public List<Expression> getSuperFieldAccesses(Expression expression) {
		instanceChecker = new InstanceOfSuperFieldAccess();
		return getExpressions(expression);
	}
	
	// returns a List of ClassInstanceCreation objects
	public List<Expression> getClassInstanceCreations(Statement statement) {
		instanceChecker = new InstanceOfClassInstanceCreation();
		return getExpressions(statement);
	}
	
	// returns a List of ClassInstanceCreation objects
	public List<Expression> getClassInstanceCreations(Expression expression) {
		instanceChecker = new InstanceOfClassInstanceCreation();
		return getExpressions(expression);
	}
	
	// returns a List of ArrayCreation objects
	public List<Expression> getArrayCreations(Statement statement) {
		instanceChecker = new InstanceOfArrayCreation();
		return getExpressions(statement);
	}
	
	// returns a List of ArrayCreation objects
	public List<Expression> getArrayCreations(Expression expression) {
		instanceChecker = new InstanceOfArrayCreation();
		return getExpressions(expression);
	}
	
	// returns a List of ArrayAccess objects
	public List<Expression> getArrayAccesses(Statement statement) {
		instanceChecker = new InstanceOfArrayAccess();
		return getExpressions(statement);
	}
	
	// returns a List of ArrayAccess objects
	public List<Expression> getArrayAccesses(Expression expression) {
		instanceChecker = new InstanceOfArrayAccess();
		return getExpressions(expression);
	}
		
	// returns a List of ThisExpression objects
	public List<Expression> getThisExpressions(Statement statement) {
		instanceChecker = new InstanceOfThisExpression();
		return getExpressions(statement);
	}
	
	// returns a List of ThisExpression objects
	public List<Expression> getThisExpressions(Expression expression) {
		instanceChecker = new InstanceOfThisExpression();
		return getExpressions(expression);
	}
		
	// returns a List of TypeLiteral objects
	public List<Expression> getTypeLiterals(Statement statement) {
		instanceChecker = new InstanceOfTypeLiteral();
		return getExpressions(statement);
	}
	
	// returns a List of Type, String, Boolean, Character, Number, and Null Literal objects
	public List<Expression> getLiterals(Statement statement) {
		instanceChecker = new InstanceOfLiteral();
		return getExpressions(statement);
	}
	
	// returns a List of Type, String, Boolean, Character, Number, and Null Literal objects
	public List<Expression> getLiterals(Expression expression) {
		instanceChecker = new InstanceOfLiteral();
		return getExpressions(expression);
	}

	// returns a List of CastExpression objects
	public List<Expression> getCastExpressions(Statement statement) {
		instanceChecker = new InstanceOfCastExpression();
		return getExpressions(statement);
	}
	
	// returns a List of InfixExpression objects
	public List<Expression> getInfixExpressions(Statement statement) {
		instanceChecker = new InstanceOfInfixExpression();
		return getExpressions(statement);
	}
	
	// returns a List of InfixExpression objects
	public List<Expression> getInfixExpressions(Expression expression) {
		instanceChecker = new InstanceOfInfixExpression();
		return getExpressions(expression);
	}
	
	// returns a List of InstanceofExpression objects
	public List<Expression> getInstanceofExpressions(Statement statement) {
		instanceChecker = new InstanceOfInstanceofExpression();
		return getExpressions(statement);
	}
	
	// returns a List of InstanceofExpression objects
	public List<Expression> getInstanceofExpressions(Expression expression) {
		instanceChecker = new InstanceOfInstanceofExpression();
		return getExpressions(expression);
	}
	
	// returns a List of Assignment objects
	public List<Expression> getAssignments(Statement statement) {
		instanceChecker = new InstanceOfAssignment();
		return getExpressions(statement);
	}
	
	// returns a List of Assignment objects
	public List<Expression> getAssignments(Expression expression) {
		instanceChecker = new InstanceOfAssignment();
		return getExpressions(expression);
	}
	
	// returns a List of PostfixExpression objects
	public List<Expression> getPostfixExpressions(Statement statement) {
		instanceChecker = new InstanceOfPostfixExpression();
		return getExpressions(statement);
	}
	
	// returns a List of PostfixExpression objects
	public List<Expression> getPostfixExpressions(Expression expression) {
		instanceChecker = new InstanceOfPostfixExpression();
		return getExpressions(expression);
	}
	
	// returns a List of PrefixExpression objects
	public List<Expression> getPrefixExpressions(Statement statement) {
		instanceChecker = new InstanceOfPrefixExpression();
		return getExpressions(statement);
	}
	
	// returns a List of PrefixExpression objects
	public List<Expression> getPrefixExpressions(Expression expression) {
		instanceChecker = new InstanceOfPrefixExpression();
		return getExpressions(expression);
	}
	
	// returns a List of VariableDeclarationExpression objects
	public List<Expression> getVariableDeclarationExpressions(Statement statement) {
		instanceChecker = new InstanceOfVariableDeclarationExpression();
		return getExpressions(statement);
	}
	
	// returns a List of VariableDeclarationExpression objects
	public List<Expression> getVariableDeclarationExpressions(Expression expression) {
		instanceChecker = new InstanceOfVariableDeclarationExpression();
		return getExpressions(expression);
	}
	
	// returns a List of ConditionalExpression objects
	public List<Expression> getConditionalExpressions(Statement statement) {
		instanceChecker = new InstanceOfConditionalExpression();
		return getExpressions(statement);
	}
	
	// returns a List of VariableDeclarationExpression objects
	public List<Expression> getConditionalExpressions(Expression expression) {
		instanceChecker = new InstanceOfConditionalExpression();
		return getExpressions(expression);
	}
	
	// returns a List of variable updaters (i.e. Assignment, PrefixExpression, or PostfixExpression objects)
	public List<Expression> getVariableModifiers(Statement statement) {
		instanceChecker = new InstanceOfVariableModifier();
		return getExpressions(statement);
	}

	// returns a List of variable updaters (i.e. Assignment, PrefixExpression, or PostfixExpression objects)
	public List<Expression> getVariableModifiers(Expression expression) {
		instanceChecker = new InstanceOfVariableModifier();
		return getExpressions(expression);
	}

	public List<Expression> getAllExpressions(ASTNode node) {
		instanceChecker = new InstanceOfExpression();
		if(node instanceof Expression)
			return getExpressions((Expression)node);
		else if(node instanceof Statement)
			return getExpressions((Statement)node);
		return new ArrayList<Expression>();
	}

	private List<Expression> getExpressions(Statement statement) {
		List<Expression> expressionList = new ArrayList<Expression>();
		if(statement instanceof Block) {
			Block block = (Block)statement;
			List<Statement> blockStatements = block.statements();
			for(Statement blockStatement : blockStatements)
				expressionList.addAll(getExpressions(blockStatement));
		}
		else if(statement instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement)statement;
			Expression expression = ifStatement.getExpression();
			expressionList.addAll(getExpressions(expression));
			expressionList.addAll(getExpressions(ifStatement.getThenStatement()));
			if(ifStatement.getElseStatement() != null) {
				expressionList.addAll(getExpressions(ifStatement.getElseStatement()));
			}
		}
		else if(statement instanceof ForStatement) {
			ForStatement forStatement = (ForStatement)statement;
			List<Expression> initializers = forStatement.initializers();
			for(Expression initializer : initializers)
				expressionList.addAll(getExpressions(initializer));
			Expression expression = forStatement.getExpression();
			if(expression != null)
				expressionList.addAll(getExpressions(expression));
			List<Expression> updaters = forStatement.updaters();
			for(Expression updater : updaters)
				expressionList.addAll(getExpressions(updater));
			expressionList.addAll(getExpressions(forStatement.getBody()));
		}
		else if(statement instanceof EnhancedForStatement) {
			EnhancedForStatement enhancedForStatement = (EnhancedForStatement)statement;
			Expression expression = enhancedForStatement.getExpression();
			SingleVariableDeclaration variableDeclaration = enhancedForStatement.getParameter();
			expressionList.addAll(getExpressions(variableDeclaration.getName()));
			if(variableDeclaration.getInitializer() != null)
				expressionList.addAll(getExpressions(variableDeclaration.getInitializer()));
			expressionList.addAll(getExpressions(expression));
			expressionList.addAll(getExpressions(enhancedForStatement.getBody()));
		}
		else if(statement instanceof WhileStatement) {
			WhileStatement whileStatement = (WhileStatement)statement;
			Expression expression = whileStatement.getExpression();
			expressionList.addAll(getExpressions(expression));
			expressionList.addAll(getExpressions(whileStatement.getBody()));
		}
		else if(statement instanceof DoStatement) {
			DoStatement doStatement = (DoStatement)statement;
			Expression expression = doStatement.getExpression();
			expressionList.addAll(getExpressions(expression));
			expressionList.addAll(getExpressions(doStatement.getBody()));
		}
		else if(statement instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement = (ExpressionStatement)statement;
			Expression expression = expressionStatement.getExpression();
			expressionList.addAll(getExpressions(expression));
		}
		else if(statement instanceof SwitchStatement) {
			SwitchStatement switchStatement = (SwitchStatement)statement;
			Expression expression = switchStatement.getExpression();
			expressionList.addAll(getExpressions(expression));
			List<Statement> switchStatements = switchStatement.statements();
			for(Statement switchStatement2 : switchStatements)
				expressionList.addAll(getExpressions(switchStatement2));
		}
		else if(statement instanceof SwitchCase) {
			SwitchCase switchCase = (SwitchCase)statement;
			Expression expression = switchCase.getExpression();
			if(expression != null)
				expressionList.addAll(getExpressions(expression));
		}
		else if(statement instanceof AssertStatement) {
			AssertStatement assertStatement = (AssertStatement)statement;
			Expression expression = assertStatement.getExpression();
			expressionList.addAll(getExpressions(expression));
			Expression message = assertStatement.getMessage();
			if(message != null)
				expressionList.addAll(getExpressions(message));
		}
		else if(statement instanceof LabeledStatement) {
			LabeledStatement labeledStatement = (LabeledStatement)statement;
			if(labeledStatement.getLabel() != null)
				expressionList.addAll(getExpressions(labeledStatement.getLabel()));
			expressionList.addAll(getExpressions(labeledStatement.getBody()));
		}
		else if(statement instanceof ReturnStatement) {
			ReturnStatement returnStatement = (ReturnStatement)statement;
			Expression expression = returnStatement.getExpression();
			expressionList.addAll(getExpressions(expression));	
		}
		else if(statement instanceof SynchronizedStatement) {
			SynchronizedStatement synchronizedStatement = (SynchronizedStatement)statement;
			Expression expression = synchronizedStatement.getExpression();
			expressionList.addAll(getExpressions(expression));
			expressionList.addAll(getExpressions(synchronizedStatement.getBody()));
		}
		else if(statement instanceof ThrowStatement) {
			ThrowStatement throwStatement = (ThrowStatement)statement;
			Expression expression = throwStatement.getExpression();
			expressionList.addAll(getExpressions(expression));
		}
		else if(statement instanceof TryStatement) {
			TryStatement tryStatement = (TryStatement)statement;
			List<VariableDeclarationExpression> resources = tryStatement.resources();
			for(VariableDeclarationExpression expression : resources) {
				expressionList.addAll(getExpressions(expression));
			}
			expressionList.addAll(getExpressions(tryStatement.getBody()));
			List<CatchClause> catchClauses = tryStatement.catchClauses();
			for(CatchClause catchClause : catchClauses) {
				SingleVariableDeclaration variableDeclaration = catchClause.getException();
				expressionList.addAll(getExpressions(variableDeclaration.getName()));
				if(variableDeclaration.getInitializer() != null)
					expressionList.addAll(getExpressions(variableDeclaration.getInitializer()));
				expressionList.addAll(getExpressions(catchClause.getBody()));
			}
			Block finallyBlock = tryStatement.getFinally();
			if(finallyBlock != null)
				expressionList.addAll(getExpressions(finallyBlock));
		}
		else if(statement instanceof VariableDeclarationStatement) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
			List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				expressionList.addAll(getExpressions(fragment.getName()));
				expressionList.addAll(getExpressions(fragment.getInitializer()));
			}
		}
		else if(statement instanceof ConstructorInvocation) {
			ConstructorInvocation constructorInvocation = (ConstructorInvocation)statement;
			List<Expression> arguments = constructorInvocation.arguments();
			for(Expression argument : arguments)
				expressionList.addAll(getExpressions(argument));
		}
		else if(statement instanceof SuperConstructorInvocation) {
			SuperConstructorInvocation superConstructorInvocation = (SuperConstructorInvocation)statement;
			if(superConstructorInvocation.getExpression() != null)
				expressionList.addAll(getExpressions(superConstructorInvocation.getExpression()));
			List<Expression> arguments = superConstructorInvocation.arguments();
			for(Expression argument : arguments)
				expressionList.addAll(getExpressions(argument));
		}
		else if(statement instanceof BreakStatement) {
			BreakStatement breakStatement = (BreakStatement)statement;
			if(breakStatement.getLabel() != null)
				expressionList.addAll(getExpressions(breakStatement.getLabel()));
		}
		else if(statement instanceof ContinueStatement) {
			ContinueStatement continueStatement = (ContinueStatement)statement;
			if(continueStatement.getLabel() != null)
				expressionList.addAll(getExpressions(continueStatement.getLabel()));
		}
		
		return expressionList;
	}
	
	private List<Expression> getExpressions(Expression expression) {
		List<Expression> expressionList = new ArrayList<Expression>();
		if(expression instanceof MethodInvocation) {
			MethodInvocation methodInvocation = (MethodInvocation)expression;
			if(methodInvocation.getExpression() != null)
				expressionList.addAll(getExpressions(methodInvocation.getExpression()));
			List<Expression> arguments = methodInvocation.arguments();
			for(Expression argument : arguments)
				expressionList.addAll(getExpressions(argument));
			if(instanceChecker.instanceOf(methodInvocation))
				expressionList.add(methodInvocation);
		}
		else if(expression instanceof Assignment) {
			Assignment assignment = (Assignment)expression;
			expressionList.addAll(getExpressions(assignment.getLeftHandSide()));
			expressionList.addAll(getExpressions(assignment.getRightHandSide()));
			if(instanceChecker.instanceOf(assignment))
				expressionList.add(assignment);
		}
		else if(expression instanceof CastExpression) {
			CastExpression castExpression = (CastExpression)expression;
			expressionList.addAll(getExpressions(castExpression.getExpression()));
			if(instanceChecker.instanceOf(castExpression))
				expressionList.add(castExpression);
		}
		else if(expression instanceof ClassInstanceCreation) {
			ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)expression;
			if(classInstanceCreation.getExpression() != null)
				expressionList.addAll(getExpressions(classInstanceCreation.getExpression()));
			List<Expression> arguments = classInstanceCreation.arguments();
			for(Expression argument : arguments)
				expressionList.addAll(getExpressions(argument));
			if(instanceChecker.instanceOf(classInstanceCreation))
				expressionList.add(classInstanceCreation);
			AnonymousClassDeclaration anonymousClassDeclaration = classInstanceCreation.getAnonymousClassDeclaration();
			if(anonymousClassDeclaration != null) {
				expressionList.addAll(getExpressions(anonymousClassDeclaration));
			}
		}
		else if(expression instanceof ConditionalExpression) {
			ConditionalExpression conditionalExpression = (ConditionalExpression)expression;
			expressionList.addAll(getExpressions(conditionalExpression.getExpression()));
			expressionList.addAll(getExpressions(conditionalExpression.getThenExpression()));
			expressionList.addAll(getExpressions(conditionalExpression.getElseExpression()));
			if(instanceChecker.instanceOf(conditionalExpression))
				expressionList.add(conditionalExpression);
		}
		else if(expression instanceof FieldAccess) {
			FieldAccess fieldAccess = (FieldAccess)expression;
			expressionList.addAll(getExpressions(fieldAccess.getExpression()));
			expressionList.addAll(getExpressions(fieldAccess.getName()));
			if(instanceChecker.instanceOf(fieldAccess))
				expressionList.add(fieldAccess);
		}
		else if(expression instanceof InfixExpression) {
			InfixExpression infixExpression = (InfixExpression)expression;
			expressionList.addAll(getExpressions(infixExpression.getLeftOperand()));
			expressionList.addAll(getExpressions(infixExpression.getRightOperand()));
			List<Expression> extendedOperands = infixExpression.extendedOperands();
			for(Expression operand : extendedOperands)
				expressionList.addAll(getExpressions(operand));
			if(instanceChecker.instanceOf(infixExpression))
				expressionList.add(infixExpression);
		}
		else if(expression instanceof InstanceofExpression) {
			InstanceofExpression instanceofExpression = (InstanceofExpression)expression;
			expressionList.addAll(getExpressions(instanceofExpression.getLeftOperand()));
			if(instanceChecker.instanceOf(instanceofExpression))
				expressionList.add(instanceofExpression);
		}
		else if(expression instanceof ParenthesizedExpression) {
			ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression)expression;
			expressionList.addAll(getExpressions(parenthesizedExpression.getExpression()));
			if(instanceChecker.instanceOf(parenthesizedExpression))
				expressionList.add(parenthesizedExpression);
		}
		else if(expression instanceof PostfixExpression) {
			PostfixExpression postfixExpression = (PostfixExpression)expression;
			expressionList.addAll(getExpressions(postfixExpression.getOperand()));
			if(instanceChecker.instanceOf(postfixExpression))
				expressionList.add(postfixExpression);
		}
		else if(expression instanceof PrefixExpression) {
			PrefixExpression prefixExpression = (PrefixExpression)expression;
			expressionList.addAll(getExpressions(prefixExpression.getOperand()));
			if(instanceChecker.instanceOf(prefixExpression))
				expressionList.add(prefixExpression);
		}
		else if(expression instanceof SuperMethodInvocation) {
			SuperMethodInvocation superMethodInvocation = (SuperMethodInvocation)expression;
			List<Expression> arguments = superMethodInvocation.arguments();
			for(Expression argument : arguments)
				expressionList.addAll(getExpressions(argument));
			if(instanceChecker.instanceOf(superMethodInvocation))
				expressionList.add(superMethodInvocation);
		}
		else if(expression instanceof VariableDeclarationExpression) {
			VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)expression;
			List<VariableDeclarationFragment> fragments = variableDeclarationExpression.fragments();
			for(VariableDeclarationFragment fragment : fragments) {
				Expression nameExpression = fragment.getName();
				expressionList.addAll(getExpressions(nameExpression));
				Expression initializerExpression = fragment.getInitializer();
				expressionList.addAll(getExpressions(initializerExpression));
			}
			if(instanceChecker.instanceOf(variableDeclarationExpression))
				expressionList.add(variableDeclarationExpression);
		}
		else if(expression instanceof ArrayAccess) {
			ArrayAccess arrayAccess = (ArrayAccess)expression;
			expressionList.addAll(getExpressions(arrayAccess.getArray()));
			expressionList.addAll(getExpressions(arrayAccess.getIndex()));
			if(instanceChecker.instanceOf(arrayAccess))
				expressionList.add(arrayAccess);
		}
		else if(expression instanceof ArrayCreation) {
			ArrayCreation arrayCreation = (ArrayCreation)expression;
			List<Expression> dimensions = arrayCreation.dimensions();
			for(Expression dimension : dimensions)
				expressionList.addAll(getExpressions(dimension));
			expressionList.addAll(getExpressions(arrayCreation.getInitializer()));
			if(instanceChecker.instanceOf(arrayCreation))
				expressionList.add(arrayCreation);
		}
		else if(expression instanceof ArrayInitializer) {
			ArrayInitializer arrayInitializer = (ArrayInitializer)expression;
			List<Expression> expressions = arrayInitializer.expressions();
			for(Expression arrayInitializerExpression : expressions)
				expressionList.addAll(getExpressions(arrayInitializerExpression));
			if(instanceChecker.instanceOf(arrayInitializer))
				expressionList.add(arrayInitializer);
		}
		else if(expression instanceof SimpleName) {
			SimpleName simpleName = (SimpleName)expression;
			if(instanceChecker.instanceOf(simpleName))
				expressionList.add(simpleName);
		}
		else if(expression instanceof QualifiedName) {
			QualifiedName qualifiedName = (QualifiedName)expression;
			expressionList.addAll(getExpressions(qualifiedName.getQualifier()));
			expressionList.addAll(getExpressions(qualifiedName.getName()));
			if(instanceChecker.instanceOf(qualifiedName))
				expressionList.add(qualifiedName);
		}
		else if(expression instanceof SuperFieldAccess) {
			SuperFieldAccess superFieldAccess = (SuperFieldAccess)expression;
			expressionList.addAll(getExpressions(superFieldAccess.getName()));
			if(instanceChecker.instanceOf(superFieldAccess))
				expressionList.add(superFieldAccess);
		}
		else if(expression instanceof ThisExpression) {
			ThisExpression thisExpression = (ThisExpression)expression;
			if(thisExpression.getQualifier() != null)
				expressionList.addAll(getExpressions(thisExpression.getQualifier()));
			if(instanceChecker.instanceOf(thisExpression))
				expressionList.add(thisExpression);
		}
		else if(expression instanceof TypeLiteral) {
			TypeLiteral typeLiteral = (TypeLiteral)expression;
			if(instanceChecker.instanceOf(typeLiteral))
				expressionList.add(typeLiteral);
		}
		else if(expression instanceof StringLiteral) {
			StringLiteral stringLiteral = (StringLiteral)expression;
			if(instanceChecker.instanceOf(stringLiteral))
				expressionList.add(stringLiteral);
		}
		else if(expression instanceof NullLiteral) {
			NullLiteral nullLiteral = (NullLiteral)expression;
			if(instanceChecker.instanceOf(nullLiteral))
				expressionList.add(nullLiteral);
		}
		else if(expression instanceof NumberLiteral) {
			NumberLiteral numberLiteral = (NumberLiteral)expression;
			if(instanceChecker.instanceOf(numberLiteral))
				expressionList.add(numberLiteral);
		}
		else if(expression instanceof BooleanLiteral) {
			BooleanLiteral booleanLiteral = (BooleanLiteral)expression;
			if(instanceChecker.instanceOf(booleanLiteral))
				expressionList.add(booleanLiteral);
		}
		else if(expression instanceof CharacterLiteral) {
			CharacterLiteral characterLiteral = (CharacterLiteral)expression;
			if(instanceChecker.instanceOf(characterLiteral))
				expressionList.add(characterLiteral);
		}
		return expressionList;
	}
	
	private List<Expression> getExpressions(AnonymousClassDeclaration anonymousClassDeclaration) {
		List<Expression> expressionList = new ArrayList<Expression>();
		List<BodyDeclaration> bodyDeclarations = anonymousClassDeclaration.bodyDeclarations();
		for(BodyDeclaration bodyDeclaration : bodyDeclarations) {
			if(bodyDeclaration instanceof MethodDeclaration) {
				MethodDeclaration methodDeclaration = (MethodDeclaration)bodyDeclaration;
				Block body = methodDeclaration.getBody();
				if(body != null) {
					List<Statement> statements = body.statements();
					for(Statement statement : statements) {
						expressionList.addAll(getExpressions(statement));
					}
				}
			}
		}
		return expressionList;
	}
}
