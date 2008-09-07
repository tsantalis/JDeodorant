package gr.uom.java.ast.decomposition;

import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.SuperMethodInvocationObject;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.StatementExtractor;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckElimination;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class MethodBodyObject {
	
	private CompositeStatementObject compositeStatement;
	
	public MethodBodyObject(Block methodBody) {
		this.compositeStatement = new CompositeStatementObject(methodBody);
        List<Statement> statements = methodBody.statements();
		for(Statement statement : statements) {
			processStatement(compositeStatement, statement);
		}
	}

	public CompositeStatementObject getCompositeStatement() {
		return compositeStatement;
	}

	public List<TypeCheckElimination> generateTypeCheckEliminations() {
		List<TypeCheckElimination> typeCheckEliminations = new ArrayList<TypeCheckElimination>();
		StatementExtractor statementExtractor = new StatementExtractor();
		List<Statement> switchStatements = statementExtractor.getSwitchStatements(compositeStatement.getStatement());
		for(Statement statement : switchStatements) {
			SwitchStatement switchStatement = (SwitchStatement)statement;
			TypeCheckElimination typeCheckElimination = new TypeCheckElimination();
			typeCheckElimination.setTypeCheckCodeFragment(switchStatement);
			List<Statement> statements = switchStatement.statements();
			Expression switchCaseExpression = null;
			boolean isDefaultCase = false;
			Set<Expression> switchCaseExpressions = new LinkedHashSet<Expression>();
			for(Statement statement2 : statements) {
				if(statement2 instanceof SwitchCase) {
					SwitchCase switchCase = (SwitchCase)statement2;
					switchCaseExpression = switchCase.getExpression();
					isDefaultCase = switchCase.isDefault();
					if(!isDefaultCase)
						switchCaseExpressions.add(switchCaseExpression);
				}
				else {
					if(statement2 instanceof Block) {
						Block block = (Block)statement2;
						List<Statement> blockStatements = block.statements();
						for(Statement blockStatement : blockStatements) {
							if(!(blockStatement instanceof BreakStatement)) {
								for(Expression expression : switchCaseExpressions) {
									typeCheckElimination.addTypeCheck(expression, blockStatement);
								}
								if(isDefaultCase) {
									typeCheckElimination.addDefaultCaseStatement(blockStatement);
								}
							}
						}
						List<Statement> branchingStatements = statementExtractor.getBranchingStatements(statement2);
						if(branchingStatements.size() > 0) {
							for(Expression expression : switchCaseExpressions) {
								if(!typeCheckElimination.containsTypeCheckExpression(expression))
									typeCheckElimination.addEmptyTypeCheck(expression);
							}
							switchCaseExpressions.clear();
						}
					}
					else {
						if(!(statement2 instanceof BreakStatement)) {
							for(Expression expression : switchCaseExpressions) {
								typeCheckElimination.addTypeCheck(expression, statement2);
							}
							if(isDefaultCase) {
								typeCheckElimination.addDefaultCaseStatement(statement2);
							}
						}
						List<Statement> branchingStatements = statementExtractor.getBranchingStatements(statement2);
						if(statement2 instanceof BreakStatement || statement2 instanceof ReturnStatement || branchingStatements.size() > 0) {
							for(Expression expression : switchCaseExpressions) {
								if(!typeCheckElimination.containsTypeCheckExpression(expression))
									typeCheckElimination.addEmptyTypeCheck(expression);
							}
							switchCaseExpressions.clear();
						}
					}
				}
			}
			typeCheckEliminations.add(typeCheckElimination);
		}
		
		List<Statement> ifStatements = statementExtractor.getIfStatements(compositeStatement.getStatement());
		TypeCheckElimination typeCheckElimination = new TypeCheckElimination();
		int i = 0;
		for(Statement statement : ifStatements) {
			IfStatement ifStatement = (IfStatement)statement;
			Expression ifExpression = ifStatement.getExpression();
			Statement thenStatement = ifStatement.getThenStatement();
			if(thenStatement instanceof Block) {
				Block block = (Block)thenStatement;
				List<Statement> statements = block.statements();
				for(Statement statement2 : statements) {
					typeCheckElimination.addTypeCheck(ifExpression, statement2);
				}
			}
			else {
				typeCheckElimination.addTypeCheck(ifExpression, thenStatement);
			}
			Statement elseStatement = ifStatement.getElseStatement();
			if(elseStatement != null) {
				if(elseStatement instanceof Block) {
					Block block = (Block)elseStatement;
					List<Statement> statements = block.statements();
					for(Statement statement2 : statements) {
						typeCheckElimination.addDefaultCaseStatement(statement2);
					}
				}
				else if(!(elseStatement instanceof IfStatement)) {
					typeCheckElimination.addDefaultCaseStatement(elseStatement);
				}
			}
			if(ifStatements.size()-1 > i) {
				IfStatement nextIfStatement = (IfStatement)ifStatements.get(i+1);
				if(!ifStatement.getParent().equals(nextIfStatement)) {
					typeCheckElimination.setTypeCheckCodeFragment(ifStatement);
					typeCheckEliminations.add(typeCheckElimination);
					typeCheckElimination = new TypeCheckElimination();
				}
			}
			else {
				typeCheckElimination.setTypeCheckCodeFragment(ifStatement);
				typeCheckEliminations.add(typeCheckElimination);
			}
			i++;
		}
		return typeCheckEliminations;
	}

	public VariableDeclarationStatement getVariableDeclarationStatement(LocalVariableDeclarationObject lvdo) {
		return this.compositeStatement.getVariableDeclarationStatement(lvdo);
	}

	public VariableDeclarationExpression getVariableDeclarationExpression(LocalVariableDeclarationObject lvdo) {
		return this.compositeStatement.getVariableDeclarationExpression(lvdo);
	}

	public List<AbstractStatement> getMethodInvocationStatements(MethodInvocationObject methodInvocation) {
		return this.compositeStatement.getMethodInvocationStatements(methodInvocation);
	}

	public List<AbstractStatement> getSuperMethodInvocationStatements(SuperMethodInvocationObject superMethodInvocation) {
		return this.compositeStatement.getSuperMethodInvocationStatements(superMethodInvocation);
	}

	public List<FieldInstructionObject> getFieldInstructions() {
		return compositeStatement.getFieldInstructions();
	}

	public List<LocalVariableDeclarationObject> getLocalVariableDeclarations() {
		return compositeStatement.getLocalVariableDeclarations();
	}

	public List<LocalVariableInstructionObject> getLocalVariableInstructions() {
		return compositeStatement.getLocalVariableInstructions();
	}

	public List<MethodInvocationObject> getMethodInvocations() {
		return compositeStatement.getMethodInvocations();
	}

	public List<SuperMethodInvocationObject> getSuperMethodInvocations() {
		return compositeStatement.getSuperMethodInvocations();
	}

	public boolean containsMethodInvocation(MethodInvocationObject methodInvocation) {
		return compositeStatement.containsMethodInvocation(methodInvocation);
	}

	public boolean containsSuperMethodInvocation(SuperMethodInvocationObject superMethodInvocation) {
		return compositeStatement.containsSuperMethodInvocation(superMethodInvocation);
	}

	public List<AbstractStatement> getFieldAssignments(FieldInstructionObject fio) {
		return compositeStatement.getFieldAssignments(fio);
	}

	public boolean containsSuperMethodInvocation() {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> superMethodInvocations = expressionExtractor.getSuperMethodInvocations(compositeStatement.getStatement());
		if(!superMethodInvocations.isEmpty())
			return true;
		else
			return false;
	}

	private void processStatement(CompositeStatementObject parent, Statement statement) {
		if(statement instanceof Block) {
			Block block = (Block)statement;
			List<Statement> blockStatements = block.statements();
			CompositeStatementObject child = new CompositeStatementObject(block);
			parent.addStatement(child);
			for(Statement blockStatement : blockStatements) {
				processStatement(child, blockStatement);
			}
		}
		else if(statement instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(ifStatement);
			AbstractExpression abstractExpression = new AbstractExpression(ifStatement.getExpression());
			child.addExpression(abstractExpression);
			parent.addStatement(child);
			processStatement(child, ifStatement.getThenStatement());
			if(ifStatement.getElseStatement() != null) {
				processStatement(child, ifStatement.getElseStatement());
			}
		}
		else if(statement instanceof ForStatement) {
			ForStatement forStatement = (ForStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(forStatement);
			List<Expression> initializers = forStatement.initializers();
			for(Expression initializer : initializers) {
				AbstractExpression abstractExpression = new AbstractExpression(initializer);
				child.addExpression(abstractExpression);
			}
			Expression expression = forStatement.getExpression();
			if(expression != null) {
				AbstractExpression abstractExpression = new AbstractExpression(expression);
				child.addExpression(abstractExpression);
			}
			List<Expression> updaters = forStatement.updaters();
			for(Expression updater : updaters) {
				AbstractExpression abstractExpression = new AbstractExpression(updater);
				child.addExpression(abstractExpression);
			}
			parent.addStatement(child);
			processStatement(child, forStatement.getBody());
		}
		else if(statement instanceof EnhancedForStatement) {
			EnhancedForStatement enhancedForStatement = (EnhancedForStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(enhancedForStatement);
			SingleVariableDeclaration variableDeclaration = enhancedForStatement.getParameter();
			AbstractExpression variableDeclarationName = new AbstractExpression(variableDeclaration.getName());
			child.addExpression(variableDeclarationName);
			if(variableDeclaration.getInitializer() != null) {
				AbstractExpression variableDeclarationInitializer = new AbstractExpression(variableDeclaration.getInitializer());
				child.addExpression(variableDeclarationInitializer);
			}
			AbstractExpression abstractExpression = new AbstractExpression(enhancedForStatement.getExpression());
			child.addExpression(abstractExpression);
			parent.addStatement(child);
			processStatement(child, enhancedForStatement.getBody());
		}
		else if(statement instanceof WhileStatement) {
			WhileStatement whileStatement = (WhileStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(whileStatement);
			AbstractExpression abstractExpression = new AbstractExpression(whileStatement.getExpression());
			child.addExpression(abstractExpression);
			parent.addStatement(child);
			processStatement(child, whileStatement.getBody());
		}
		else if(statement instanceof DoStatement) {
			DoStatement doStatement = (DoStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(doStatement);
			AbstractExpression abstractExpression = new AbstractExpression(doStatement.getExpression());
			child.addExpression(abstractExpression);
			parent.addStatement(child);
			processStatement(child, doStatement.getBody());
		}
		else if(statement instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement = (ExpressionStatement)statement;
			StatementObject child = new StatementObject(expressionStatement);
			parent.addStatement(child);
		}
		else if(statement instanceof SwitchStatement) {
			SwitchStatement switchStatement = (SwitchStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(switchStatement);
			AbstractExpression abstractExpression = new AbstractExpression(switchStatement.getExpression());
			child.addExpression(abstractExpression);
			parent.addStatement(child);
			List<Statement> switchStatements = switchStatement.statements();
			for(Statement switchStatement2 : switchStatements)
				processStatement(child, switchStatement2);
		}
		else if(statement instanceof SwitchCase) {
			SwitchCase switchCase = (SwitchCase)statement;
			StatementObject child = new StatementObject(switchCase);
			parent.addStatement(child);
		}
		else if(statement instanceof AssertStatement) {
			AssertStatement assertStatement = (AssertStatement)statement;
			StatementObject child = new StatementObject(assertStatement);
			parent.addStatement(child);
		}
		else if(statement instanceof LabeledStatement) {
			LabeledStatement labeledStatement = (LabeledStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(labeledStatement);
			parent.addStatement(child);
			processStatement(child, labeledStatement.getBody());
		}
		else if(statement instanceof ReturnStatement) {
			ReturnStatement returnStatement = (ReturnStatement)statement;
			StatementObject child = new StatementObject(returnStatement);
			parent.addStatement(child);	
		}
		else if(statement instanceof SynchronizedStatement) {
			SynchronizedStatement synchronizedStatement = (SynchronizedStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(synchronizedStatement);
			AbstractExpression abstractExpression = new AbstractExpression(synchronizedStatement.getExpression());
			child.addExpression(abstractExpression);
			parent.addStatement(child);
			processStatement(child, synchronizedStatement.getBody());
		}
		else if(statement instanceof ThrowStatement) {
			ThrowStatement throwStatement = (ThrowStatement)statement;
			StatementObject child = new StatementObject(throwStatement);
			parent.addStatement(child);
		}
		else if(statement instanceof TryStatement) {
			TryStatement tryStatement = (TryStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(tryStatement);
			parent.addStatement(child);
			processStatement(child, tryStatement.getBody());
			List<CatchClause> catchClauses = tryStatement.catchClauses();
			for(CatchClause catchClause : catchClauses) {
				processStatement(child, catchClause.getBody());
			}
			Block finallyBlock = tryStatement.getFinally();
			if(finallyBlock != null)
				processStatement(child, finallyBlock);
		}
		else if(statement instanceof VariableDeclarationStatement) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
			StatementObject child = new StatementObject(variableDeclarationStatement);
			parent.addStatement(child);
		}
		else if(statement instanceof ConstructorInvocation) {
			ConstructorInvocation constructorInvocation = (ConstructorInvocation)statement;
			StatementObject child = new StatementObject(constructorInvocation);
			parent.addStatement(child);
		}
		else if(statement instanceof SuperConstructorInvocation) {
			SuperConstructorInvocation superConstructorInvocation = (SuperConstructorInvocation)statement;
			StatementObject child = new StatementObject(superConstructorInvocation);
			parent.addStatement(child);
		}
		else if(statement instanceof BreakStatement) {
			BreakStatement breakStatement = (BreakStatement)statement;
			StatementObject child = new StatementObject(breakStatement);
			parent.addStatement(child);
		}
		else if(statement instanceof ContinueStatement) {
			ContinueStatement continueStatement = (ContinueStatement)statement;
			StatementObject child = new StatementObject(continueStatement);
			parent.addStatement(child);
		}
		else if(statement instanceof EmptyStatement) {
			EmptyStatement emptyStatement = (EmptyStatement)statement;
			StatementObject child = new StatementObject(emptyStatement);
			parent.addStatement(child);
		}
	}

	public String toString() {
		return compositeStatement.toString();
	}
}
