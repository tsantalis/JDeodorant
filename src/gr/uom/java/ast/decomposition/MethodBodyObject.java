package gr.uom.java.ast.decomposition;

import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.StatementExtractor;
import gr.uom.java.jdeodorant.refactoring.manipulators.TypeCheckElimination;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
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
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
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
			for(Statement statement2 : statements) {
				if(statement2 instanceof SwitchCase) {
					SwitchCase switchCase = (SwitchCase)statement2;
					switchCaseExpression = switchCase.getExpression();
					isDefaultCase = switchCase.isDefault();
				}
				else {
					if(!isDefaultCase) {
						if(!(statement2 instanceof BreakStatement))
							typeCheckElimination.addTypeCheck(switchCaseExpression, statement2);
					}
					else {
						if(!(statement2 instanceof BreakStatement))
							typeCheckElimination.addDefaultCaseStatement(statement2);
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

	public List<ExtractionBlock> generateExtractionBlocks() {
		//contains the assignments corresponding to each local variable declaration
		Map<LocalVariableDeclarationObject, List<AbstractStatement>> assignmentMap = 
			new LinkedHashMap<LocalVariableDeclarationObject, List<AbstractStatement>>();
		
		for(LocalVariableDeclarationObject lvdo : getLocalVariableDeclarations()) {
			List<AbstractStatement> localVariableAssignments = compositeStatement.getLocalVariableAssignments(lvdo);
			if(localVariableAssignments.size() > 0)
				assignmentMap.put(lvdo, localVariableAssignments);
		}
		
		List<ExtractionBlock> extractionBlockList = new ArrayList<ExtractionBlock>();
		
		for(LocalVariableDeclarationObject variableDeclaration : assignmentMap.keySet()) {
			List<AbstractStatement> localVariableAssignments = assignmentMap.get(variableDeclaration);
			List<String> assignmentOperators = new ArrayList<String>();
			//contains the local variable instructions found in the assignment statements
			Set<LocalVariableInstructionObject> localVariableInstructionsFoundInAssignments = new HashSet<LocalVariableInstructionObject>();
			for(AbstractStatement localVariableAssignment : localVariableAssignments) {
				assignmentOperators.add(getAssignmentOperator(localVariableAssignment));
				List<LocalVariableInstructionObject> instructions = localVariableAssignment.getLocalVariableInstructions();
				for(LocalVariableInstructionObject instruction : instructions) {
					if(assignmentMap.containsKey(instruction.generateLocalVariableDeclaration()))
						localVariableInstructionsFoundInAssignments.add(instruction);
				}
			}
			//contains the local variable assignments that are not accepted within the scope of the returned assignment
			Set<LocalVariableDeclarationObject> nonAcceptedVariableAssignments = new HashSet<LocalVariableDeclarationObject>(assignmentMap.keySet());
			for(LocalVariableInstructionObject variableInstruction : localVariableInstructionsFoundInAssignments) {
				LocalVariableDeclarationObject lvdo = variableInstruction.generateLocalVariableDeclaration();
				nonAcceptedVariableAssignments.remove(lvdo);
			}
			
			List<AbstractStatement> newLocalVariableAssignments = new ArrayList<AbstractStatement>();
			for(AbstractStatement statement : localVariableAssignments) {
				CompositeStatementObject parent = statement.getParent();
				AbstractStatement current = statement;
				while(parent != null && parent.getLocalVariableAssignments(nonAcceptedVariableAssignments).size() == 0 &&
						removeExceptionVariableDeclarations(parent.getLocalVariableDeclarations()).size() == 0 && !parent.containsLocalVariableInstruction(nonAcceptedVariableAssignments)) {
					current = parent;
					parent = current.getParent();
				}
				if(!newLocalVariableAssignments.contains(current) && current.getParent() != null)
					newLocalVariableAssignments.add(current);
			}
			//
			boolean blockStatementFound = false;
			for(AbstractStatement abstractStatement : newLocalVariableAssignments) {
				if(abstractStatement.getStatement() instanceof Block) {
					blockStatementFound = true;
					break;
				}
			}
			if(blockStatementFound)
				newLocalVariableAssignments.clear();
			if(!newLocalVariableAssignments.isEmpty()) {
				AbstractStatement currentStatement = newLocalVariableAssignments.get(0);
				AbstractStatement parent = currentStatement.getParent();
				if(parent.getParent() == null) {
					boolean onlyLocalVariableDeclarationsFound = true;
					AbstractStatement previousStatement = compositeStatement.getPreviousStatement(currentStatement);
					while(previousStatement != null) {
						if(!(previousStatement.getStatement() instanceof VariableDeclarationStatement)) {
							onlyLocalVariableDeclarationsFound = false;
							break;
						}
						previousStatement = compositeStatement.getPreviousStatement(previousStatement);
					}
					if(onlyLocalVariableDeclarationsFound)
						newLocalVariableAssignments.clear();
				}
			}
			//
			if( (newLocalVariableAssignments.size() > 1 && belongToTheSameBlock(newLocalVariableAssignments) && consecutive(newLocalVariableAssignments)) || 
					(newLocalVariableAssignments.size() == 1 && !(newLocalVariableAssignments.get(0) instanceof StatementObject)) ) {
				ExtractionBlock block = new ExtractionBlock(variableDeclaration, getVariableDeclarationStatement(variableDeclaration), newLocalVariableAssignments, assignmentOperators);
				for(LocalVariableInstructionObject variableInstruction : localVariableInstructionsFoundInAssignments) {
					LocalVariableDeclarationObject lvdo = variableInstruction.generateLocalVariableDeclaration();
					if(!lvdo.equals(variableDeclaration) && !hasAssignmentBeforeStatement(lvdo, newLocalVariableAssignments.get(0))) {
						VariableDeclarationStatement variableDeclarationStatement = getVariableDeclarationStatement(lvdo);
						if(variableDeclarationStatement != null)
							block.addRequiredVariableDeclarationStatement(lvdo, variableDeclarationStatement);
					}
				}
				extractionBlockList.add(block);
			}
			else if(newLocalVariableAssignments.size() > 1) {
				CompositeStatementObject parentStatement = haveRecursivelyTheSameParent(newLocalVariableAssignments);
				if(parentStatement != null && !(parentStatement.getStatement() instanceof Block)) {
					List<AbstractStatement> tempLocalVariableAssignments = new ArrayList<AbstractStatement>(newLocalVariableAssignments);
					for(AbstractStatement statement : tempLocalVariableAssignments) {
						int index = newLocalVariableAssignments.indexOf(statement);
						AbstractStatement previousStatement = this.compositeStatement.getPreviousStatement(statement);
						while(previousStatement != null) {
							if(previousStatement instanceof StatementObject) {
								StatementObject previousStatementObject = (StatementObject)previousStatement;
								if(!previousStatementObject.isLocalVariableAssignment(nonAcceptedVariableAssignments)) {
									if(!(previousStatementObject.getStatement() instanceof EmptyStatement)) {
										newLocalVariableAssignments.add(index, previousStatement);
										index = newLocalVariableAssignments.indexOf(previousStatement);
									}
									previousStatement = this.compositeStatement.getPreviousStatement(previousStatement);
								}
								else {
									break;
								}
							}
							else if(previousStatement instanceof CompositeStatementObject) {
								CompositeStatementObject previousCompositeStatement = (CompositeStatementObject)previousStatement;
								if(previousCompositeStatement.getLocalVariableAssignments(nonAcceptedVariableAssignments).size() == 0) {
									newLocalVariableAssignments.add(index, previousStatement);
									index = newLocalVariableAssignments.indexOf(previousStatement);
									previousStatement = this.compositeStatement.getPreviousStatement(previousStatement);
								}
								else {
									break;
								}
							}
						}
					}
					ExtractionBlock block = new ExtractionBlock(variableDeclaration, getVariableDeclarationStatement(variableDeclaration), newLocalVariableAssignments, assignmentOperators);
					for(LocalVariableInstructionObject variableInstruction : localVariableInstructionsFoundInAssignments) {
						LocalVariableDeclarationObject lvdo = variableInstruction.generateLocalVariableDeclaration();
						if(!lvdo.equals(variableDeclaration) && !hasAssignmentBeforeStatement(lvdo, newLocalVariableAssignments.get(0))) {
							VariableDeclarationStatement variableDeclarationStatement = getVariableDeclarationStatement(lvdo);
							if(variableDeclarationStatement != null)
								block.addRequiredVariableDeclarationStatement(lvdo, variableDeclarationStatement);
						}
					}
					block.setParentStatementForCopy(parentStatement);
					extractionBlockList.add(block);
				}
			}
		}
		return extractionBlockList;
	}

	private List<LocalVariableDeclarationObject> removeExceptionVariableDeclarations(List<LocalVariableDeclarationObject> list) {
		List<LocalVariableDeclarationObject> localVariableDeclarations = new ArrayList<LocalVariableDeclarationObject>(list);
		for(LocalVariableDeclarationObject localVariableDeclaration : list) {
			if(localVariableDeclaration.getType().getClassType().endsWith("Exception"))
				localVariableDeclarations.remove(localVariableDeclaration);
		}
		return localVariableDeclarations;
	}

	private boolean belongToTheSameBlock(List<AbstractStatement> list) {
		AbstractStatement statement = list.get(0);
		for(int i=1; i<list.size(); i++) {
			if(!statement.getParent().equals(list.get(i).getParent()))
				return false;
		}
		return true;
	}

	private boolean consecutive(List<AbstractStatement> list) {
		int currentPosition = this.compositeStatement.getStatementPosition(list.get(0));
		for(int i=1; i<list.size(); i++) {
			int position = this.compositeStatement.getStatementPosition(list.get(i));
			if(position != currentPosition + 1) {
				return false;
			}
			currentPosition = position;
		}
		return true;
	}

	private CompositeStatementObject haveRecursivelyTheSameParent(List<AbstractStatement> list) {
		List<AbstractStatement> copiedList = new ArrayList<AbstractStatement>(list);
		Map<AbstractStatement, Integer> depthOfNestingMap = new LinkedHashMap<AbstractStatement, Integer>();
		for(AbstractStatement statement : copiedList) {
			int depthOfNesting = 0;
			AbstractStatement parentStatement = statement.getParent();
			while(parentStatement != null) {
				parentStatement = parentStatement.getParent();
				depthOfNesting++;
			}
			depthOfNestingMap.put(statement, depthOfNesting);
		}
		TreeSet<Integer> depths = new TreeSet<Integer>();
		for(Integer depth : depthOfNestingMap.values()) {
			depths.add(depth);
		}
		int minimumDepthOfNesting = depths.first();
		if(minimumDepthOfNesting == 1) {
			return null;
		}
		else {
			for(AbstractStatement statement : depthOfNestingMap.keySet()) {
				int depth = depthOfNestingMap.get(statement);
				if(depth > minimumDepthOfNesting) {
					AbstractStatement parentStatement = statement;
					for(int i=0; i<depth-minimumDepthOfNesting; i++) {
						parentStatement = parentStatement.getParent();
					}
					int index = copiedList.indexOf(statement);
					copiedList.remove(index);
					copiedList.add(index, parentStatement);
				}
			}
		}
		List<AbstractStatement> parentStatements = new ArrayList<AbstractStatement>();
		for(AbstractStatement statement : copiedList) {
			parentStatements.add(statement.getParent());
		}
		AbstractStatement parentStatement = parentStatements.get(0);
		for(int i=1; i<parentStatements.size(); i++) {
			if(!parentStatement.equals(parentStatements.get(i))) {
				return haveRecursivelyTheSameParent(parentStatements);
			}	
		}
		if(parentStatement.getParent() != null)
			return (CompositeStatementObject)parentStatement;
		else
			return null;
	}

	private boolean hasAssignmentBeforeStatement(LocalVariableDeclarationObject lvdo, AbstractStatement statement) {
		CompositeStatementObject parent = statement.getParent();
		if(parent == null)
			return false;
		int statementPosition = parent.getStatementPosition(statement);
		List<AbstractStatement> parentStatements = parent.getStatements();
		for(int i=0; i<statementPosition; i++) {
			AbstractStatement abstractStatement = parentStatements.get(i);
			if(abstractStatement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)abstractStatement;
				if(statementObject.isLocalVariableAssignment(lvdo))
					return true;
			}
			else if(abstractStatement instanceof CompositeStatementObject) {
				CompositeStatementObject compositeStatementObject = (CompositeStatementObject)abstractStatement;
				if(compositeStatementObject.getLocalVariableAssignments(lvdo).size() > 0)
					return true;
			}
		}
		return hasAssignmentBeforeStatement(lvdo, parent);
	}

	private String getAssignmentOperator(AbstractStatement localVariableAssignment) {
		Statement statement = localVariableAssignment.getStatement();
		if(statement instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement = (ExpressionStatement)statement;
			Expression expression = expressionStatement.getExpression();
			if(expression instanceof Assignment) {
				Assignment assignment = (Assignment)expression;
				return assignment.getOperator().toString();
			}
			else if(expression instanceof PostfixExpression) {
				PostfixExpression postfixExpression = (PostfixExpression)expression;
				return postfixExpression.getOperator().toString();
			}
			else if(expression instanceof PrefixExpression) {
				PrefixExpression prefixExpression = (PrefixExpression)expression;
				return prefixExpression.getOperator().toString();
			}
		}
		return null;
	}

	public VariableDeclarationStatement getVariableDeclarationStatement(LocalVariableDeclarationObject lvdo) {
		return this.compositeStatement.getVariableDeclarationStatement(lvdo);
	}

	public List<AbstractStatement> getMethodInvocationStatements(MethodInvocationObject methodInvocation) {
		return this.compositeStatement.getMethodInvocationStatements(methodInvocation);
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

	public boolean containsMethodInvocation(MethodInvocationObject methodInvocation) {
		return compositeStatement.containsMethodInvocation(methodInvocation);
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
