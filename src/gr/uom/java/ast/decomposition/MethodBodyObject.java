package gr.uom.java.ast.decomposition;

import gr.uom.java.ast.CreationObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.SuperFieldInstructionObject;
import gr.uom.java.ast.SuperMethodInvocationObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.StatementExtractor;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckElimination;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class MethodBodyObject {
	
	private CompositeStatementObject compositeStatement;
	
	public MethodBodyObject(Block methodBody) {
		this.compositeStatement = new CompositeStatementObject(methodBody, "{");
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
		List<CompositeStatementObject> switchCompositeStatements = compositeStatement.getSwitchStatements();
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
			for(CompositeStatementObject composite : switchCompositeStatements) {
				if(composite.getStatement().toString().equals(switchStatement.toString())) {
					typeCheckElimination.setTypeCheckCompositeStatement(composite);
					break;
				}
			}
		}
		
		List<Statement> ifStatements = statementExtractor.getIfStatements(compositeStatement.getStatement());
		List<CompositeStatementObject> ifCompositeStatements = compositeStatement.getIfStatements();
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
					for(CompositeStatementObject composite : ifCompositeStatements) {
						if(composite.getStatement().toString().equals(ifStatement.toString())) {
							typeCheckElimination.setTypeCheckCompositeStatement(composite);
							break;
						}
					}
					typeCheckElimination = new TypeCheckElimination();
				}
			}
			else {
				typeCheckElimination.setTypeCheckCodeFragment(ifStatement);
				typeCheckEliminations.add(typeCheckElimination);
				for(CompositeStatementObject composite : ifCompositeStatements) {
					if(composite.getStatement().toString().equals(ifStatement.toString())) {
						typeCheckElimination.setTypeCheckCompositeStatement(composite);
						break;
					}
				}
			}
			i++;
		}
		return typeCheckEliminations;
	}

	public List<FieldInstructionObject> getFieldInstructions() {
		return compositeStatement.getFieldInstructions();
	}

	public List<SuperFieldInstructionObject> getSuperFieldInstructions() {
		return compositeStatement.getSuperFieldInstructions();
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

	public List<CreationObject> getCreations() {
		return compositeStatement.getCreations();
	}

	public boolean containsMethodInvocation(MethodInvocationObject methodInvocation) {
		return compositeStatement.containsMethodInvocation(methodInvocation);
	}

	public boolean containsFieldInstruction(FieldInstructionObject fieldInstruction) {
		return compositeStatement.containsFieldInstruction(fieldInstruction);
	}

	public boolean containsSuperMethodInvocation(SuperMethodInvocationObject superMethodInvocation) {
		return compositeStatement.containsSuperMethodInvocation(superMethodInvocation);
	}

	public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughFields() {
		return compositeStatement.getInvokedMethodsThroughFields();
	}

	public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughParameters() {
		return compositeStatement.getInvokedMethodsThroughParameters();
	}

	public Map<AbstractVariable, ArrayList<MethodInvocationObject>> getNonDistinctInvokedMethodsThroughFields() {
		return compositeStatement.getNonDistinctInvokedMethodsThroughFields();
	}

	public Map<AbstractVariable, ArrayList<MethodInvocationObject>> getNonDistinctInvokedMethodsThroughParameters() {
		return compositeStatement.getNonDistinctInvokedMethodsThroughParameters();
	}

	public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughLocalVariables() {
		return compositeStatement.getInvokedMethodsThroughLocalVariables();
	}

	public Set<MethodInvocationObject> getInvokedMethodsThroughThisReference() {
		return compositeStatement.getInvokedMethodsThroughThisReference();
	}

	public Set<MethodInvocationObject> getInvokedStaticMethods() {
		return compositeStatement.getInvokedStaticMethods();
	}

	public Set<AbstractVariable> getDefinedFieldsThroughFields() {
		return compositeStatement.getDefinedFieldsThroughFields();
	}

	public Set<AbstractVariable> getUsedFieldsThroughFields() {
		return compositeStatement.getUsedFieldsThroughFields();
	}

	public List<AbstractVariable> getNonDistinctDefinedFieldsThroughFields() {
		return compositeStatement.getNonDistinctDefinedFieldsThroughFields();
	}

	public List<AbstractVariable> getNonDistinctUsedFieldsThroughFields() {
		return compositeStatement.getNonDistinctUsedFieldsThroughFields();
	}

	public Set<AbstractVariable> getDefinedFieldsThroughParameters() {
		return compositeStatement.getDefinedFieldsThroughParameters();
	}

	public Set<AbstractVariable> getUsedFieldsThroughParameters() {
		return compositeStatement.getUsedFieldsThroughParameters();
	}

	public List<AbstractVariable> getNonDistinctDefinedFieldsThroughParameters() {
		return compositeStatement.getNonDistinctDefinedFieldsThroughParameters();
	}

	public List<AbstractVariable> getNonDistinctUsedFieldsThroughParameters() {
		return compositeStatement.getNonDistinctUsedFieldsThroughParameters();
	}

	public Set<AbstractVariable> getDefinedFieldsThroughLocalVariables() {
		return compositeStatement.getDefinedFieldsThroughLocalVariables();
	}

	public Set<AbstractVariable> getUsedFieldsThroughLocalVariables() {
		return compositeStatement.getUsedFieldsThroughLocalVariables();
	}

	public Set<PlainVariable> getDefinedFieldsThroughThisReference() {
		return compositeStatement.getDefinedFieldsThroughThisReference();
	}

	public Set<PlainVariable> getUsedFieldsThroughThisReference() {
		return compositeStatement.getUsedFieldsThroughThisReference();
	}

	public Set<PlainVariable> getDeclaredLocalVariables() {
		return compositeStatement.getDeclaredLocalVariables();
	}

	public Set<PlainVariable> getDefinedLocalVariables() {
		return compositeStatement.getDefinedLocalVariables();
	}

	public Set<PlainVariable> getUsedLocalVariables() {
		return compositeStatement.getUsedLocalVariables();
	}

	public Map<PlainVariable, LinkedHashSet<MethodInvocationObject>> getParametersPassedAsArgumentsInMethodInvocations() {
		return compositeStatement.getParametersPassedAsArgumentsInMethodInvocations();
	}

	public Map<PlainVariable, LinkedHashSet<SuperMethodInvocationObject>> getParametersPassedAsArgumentsInSuperMethodInvocations() {
		return compositeStatement.getParametersPassedAsArgumentsInSuperMethodInvocations();
	}

	public boolean containsSuperMethodInvocation() {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> superMethodInvocations = expressionExtractor.getSuperMethodInvocations(compositeStatement.getStatement());
		if(!superMethodInvocations.isEmpty())
			return true;
		else
			return false;
	}

	public boolean containsSuperFieldAccess() {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> superFieldAccesses = expressionExtractor.getSuperFieldAccesses(compositeStatement.getStatement());
		if(!superFieldAccesses.isEmpty())
			return true;
		else
			return false;
	}

	private void processStatement(CompositeStatementObject parent, Statement statement) {
		if(statement instanceof Block) {
			Block block = (Block)statement;
			List<Statement> blockStatements = block.statements();
			CompositeStatementObject child = new CompositeStatementObject(block, "{");
			parent.addStatement(child);
			for(Statement blockStatement : blockStatements) {
				processStatement(child, blockStatement);
			}
		}
		else if(statement instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(ifStatement, "if");
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
			CompositeStatementObject child = new CompositeStatementObject(forStatement, "for");
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
			CompositeStatementObject child = new CompositeStatementObject(enhancedForStatement, "for");
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
			CompositeStatementObject child = new CompositeStatementObject(whileStatement, "while");
			AbstractExpression abstractExpression = new AbstractExpression(whileStatement.getExpression());
			child.addExpression(abstractExpression);
			parent.addStatement(child);
			processStatement(child, whileStatement.getBody());
		}
		else if(statement instanceof DoStatement) {
			DoStatement doStatement = (DoStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(doStatement, "do");
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
			CompositeStatementObject child = new CompositeStatementObject(switchStatement, "switch");
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
			CompositeStatementObject child = new CompositeStatementObject(labeledStatement, labeledStatement.getLabel().getIdentifier());
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
			CompositeStatementObject child = new CompositeStatementObject(synchronizedStatement, "synchronized");
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
			TryStatementObject child = new TryStatementObject(tryStatement, "try");
			List<VariableDeclarationExpression> resources = tryStatement.resources();
			for(VariableDeclarationExpression expression : resources) {
				AbstractExpression variableDeclarationExpression = new AbstractExpression(expression);
				child.addExpression(variableDeclarationExpression);
			}
			parent.addStatement(child);
			processStatement(child, tryStatement.getBody());
			List<CatchClause> catchClauses = tryStatement.catchClauses();
			for(CatchClause catchClause : catchClauses) {
				CatchClauseObject catchClauseObject = new CatchClauseObject();
				Block catchClauseBody = catchClause.getBody();
				CompositeStatementObject catchClauseStatementObject = new CompositeStatementObject(catchClauseBody, "{");
				SingleVariableDeclaration variableDeclaration = catchClause.getException();
				Type variableDeclarationType = variableDeclaration.getType();
				if(variableDeclarationType instanceof UnionType) {
					UnionType unionType = (UnionType)variableDeclarationType;
					List<Type> types = unionType.types();
					for(Type type : types) {
						catchClauseObject.addExceptionType(type.resolveBinding().getQualifiedName());
					}
				}
				else {
					catchClauseObject.addExceptionType(variableDeclarationType.resolveBinding().getQualifiedName());
				}
				AbstractExpression variableDeclarationName = new AbstractExpression(variableDeclaration.getName());
				catchClauseObject.addExpression(variableDeclarationName);
				if(variableDeclaration.getInitializer() != null) {
					AbstractExpression variableDeclarationInitializer = new AbstractExpression(variableDeclaration.getInitializer());
					catchClauseObject.addExpression(variableDeclarationInitializer);
				}
				List<Statement> blockStatements = catchClauseBody.statements();
				for(Statement blockStatement : blockStatements) {
					processStatement(catchClauseStatementObject, blockStatement);
				}
				catchClauseObject.addStatement(catchClauseStatementObject);
				child.addCatchClause(catchClauseObject);
			}
			Block finallyBlock = tryStatement.getFinally();
			if(finallyBlock != null) {
				CompositeStatementObject finallyClauseStatementObject = new CompositeStatementObject(finallyBlock, "finally");
				List<Statement> blockStatements = finallyBlock.statements();
				for(Statement blockStatement : blockStatements) {
					processStatement(finallyClauseStatementObject, blockStatement);
				}
				child.setFinallyClause(finallyClauseStatementObject);
			}
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

	public List<String> stringRepresentation() {
		return compositeStatement.stringRepresentation();
	}
}
