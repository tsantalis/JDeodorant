package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.decomposition.cfg.CFGBranchDoLoopNode;
import gr.uom.java.ast.decomposition.cfg.GraphEdge;
import gr.uom.java.ast.decomposition.cfg.PDGControlDependence;
import gr.uom.java.ast.decomposition.cfg.PDGControlPredicateNode;
import gr.uom.java.ast.decomposition.cfg.PDGDependence;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.TypeVisitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.ltk.core.refactoring.Refactoring;

public abstract class ExtractMethodFragmentRefactoring extends Refactoring {

	protected Set<TryStatement> tryStatementsToBeRemoved;
	protected Set<TryStatement> tryStatementsToBeCopied;
	protected Map<TryStatement, ListRewrite> tryStatementBodyRewriteMap;
	protected List<CFGBranchDoLoopNode> doLoopNodes;
	protected Set<LabeledStatement> labeledStatementsToBeRemoved;

	public ExtractMethodFragmentRefactoring() {
		this.tryStatementsToBeRemoved = new LinkedHashSet<TryStatement>();
		this.tryStatementsToBeCopied = new LinkedHashSet<TryStatement>();
		this.tryStatementBodyRewriteMap = new LinkedHashMap<TryStatement, ListRewrite>();
		this.doLoopNodes = new ArrayList<CFGBranchDoLoopNode>();
		this.labeledStatementsToBeRemoved = new LinkedHashSet<LabeledStatement>();
	}

	protected List<Statement> getStatements(Statement statement) {
		List<Statement> statementList = new ArrayList<Statement>();
		if(statement instanceof Block) {
			Block block = (Block)statement;
			List<Statement> blockStatements = block.statements();
			for(Statement blockStatement : blockStatements)
				statementList.addAll(getStatements(blockStatement));
		}
		else if(statement instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement)statement;
			statementList.add(ifStatement);
			statementList.addAll(getStatements(ifStatement.getThenStatement()));
			if(ifStatement.getElseStatement() != null) {
				statementList.addAll(getStatements(ifStatement.getElseStatement()));
			}
		}
		else if(statement instanceof ForStatement) {
			ForStatement forStatement = (ForStatement)statement;
			statementList.add(forStatement);
			statementList.addAll(getStatements(forStatement.getBody()));
		}
		else if(statement instanceof EnhancedForStatement) {
			EnhancedForStatement enhancedForStatement = (EnhancedForStatement)statement;
			statementList.add(enhancedForStatement);
			statementList.addAll(getStatements(enhancedForStatement.getBody()));
		}
		else if(statement instanceof WhileStatement) {
			WhileStatement whileStatement = (WhileStatement)statement;
			statementList.add(whileStatement);
			statementList.addAll(getStatements(whileStatement.getBody()));
		}
		else if(statement instanceof DoStatement) {
			DoStatement doStatement = (DoStatement)statement;
			statementList.add(doStatement);
			statementList.addAll(getStatements(doStatement.getBody()));
		}
		else if(statement instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement = (ExpressionStatement)statement;
			statementList.add(expressionStatement);
		}
		else if(statement instanceof SwitchStatement) {
			SwitchStatement switchStatement = (SwitchStatement)statement;
			statementList.add(switchStatement);
			List<Statement> statements = switchStatement.statements();
			for(Statement statement2 : statements)
				statementList.addAll(getStatements(statement2));
		}
		else if(statement instanceof SwitchCase) {
			SwitchCase switchCase = (SwitchCase)statement;
			statementList.add(switchCase);
		}
		else if(statement instanceof AssertStatement) {
			AssertStatement assertStatement = (AssertStatement)statement;
			statementList.add(assertStatement);
		}
		else if(statement instanceof LabeledStatement) {
			LabeledStatement labeledStatement = (LabeledStatement)statement;
			//handling of LabeledStatement
			statementList.addAll(getStatements(labeledStatement.getBody()));
		}
		else if(statement instanceof ReturnStatement) {
			ReturnStatement returnStatement = (ReturnStatement)statement;
			statementList.add(returnStatement);
		}
		else if(statement instanceof SynchronizedStatement) {
			SynchronizedStatement synchronizedStatement = (SynchronizedStatement)statement;
			//handling of SynchronizedStatement
			statementList.addAll(getStatements(synchronizedStatement.getBody()));
		}
		else if(statement instanceof ThrowStatement) {
			ThrowStatement throwStatement = (ThrowStatement)statement;
			statementList.add(throwStatement);
		}
		else if(statement instanceof TryStatement) {
			TryStatement tryStatement = (TryStatement)statement;
			statementList.addAll(getStatements(tryStatement.getBody()));
			/*List<CatchClause> catchClauses = tryStatement.catchClauses();
			for(CatchClause catchClause : catchClauses) {
				statementList.addAll(getStatements(catchClause.getBody()));
			}
			Block finallyBlock = tryStatement.getFinally();
			if(finallyBlock != null)
				statementList.addAll(getStatements(finallyBlock));*/
		}
		else if(statement instanceof VariableDeclarationStatement) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
			statementList.add(variableDeclarationStatement);
		}
		else if(statement instanceof ConstructorInvocation) {
			ConstructorInvocation constructorInvocation = (ConstructorInvocation)statement;
			statementList.add(constructorInvocation);
		}
		else if(statement instanceof SuperConstructorInvocation) {
			SuperConstructorInvocation superConstructorInvocation = (SuperConstructorInvocation)statement;
			statementList.add(superConstructorInvocation);
		}
		else if(statement instanceof BreakStatement) {
			BreakStatement breakStatement = (BreakStatement)statement;
			statementList.add(breakStatement);
		}
		else if(statement instanceof ContinueStatement) {
			ContinueStatement continueStatement = (ContinueStatement)statement;
			statementList.add(continueStatement);
		}
		return statementList;
	}

	protected Set<ITypeBinding> getThrownExceptionTypes(Statement statement) {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		List<Expression> methodInvocations = new ArrayList<Expression>();
		List<Expression> classInstanceCreations = new ArrayList<Expression>();
		if(statement instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement)statement;
			Expression ifExpression = ifStatement.getExpression();
			methodInvocations.addAll(expressionExtractor.getMethodInvocations(ifExpression));
			classInstanceCreations.addAll(expressionExtractor.getClassInstanceCreations(ifExpression));
		}
		else if(statement instanceof WhileStatement) {
			WhileStatement whileStatement = (WhileStatement)statement;
			Expression whileExpression = whileStatement.getExpression();
			methodInvocations.addAll(expressionExtractor.getMethodInvocations(whileExpression));
			classInstanceCreations.addAll(expressionExtractor.getClassInstanceCreations(whileExpression));
		}
		else if(statement instanceof ForStatement) {
			ForStatement forStatement = (ForStatement)statement;
			List<Expression> initializers = forStatement.initializers();
			for(Expression expression : initializers) {
				methodInvocations.addAll(expressionExtractor.getMethodInvocations(expression));
				classInstanceCreations.addAll(expressionExtractor.getClassInstanceCreations(expression));
			}
			Expression forExpression = forStatement.getExpression();
			if(forExpression != null) {
				methodInvocations.addAll(expressionExtractor.getMethodInvocations(forExpression));
				classInstanceCreations.addAll(expressionExtractor.getClassInstanceCreations(forExpression));
			}
			List<Expression> updaters = forStatement.updaters();
			for(Expression expression : updaters) {
				methodInvocations.addAll(expressionExtractor.getMethodInvocations(expression));
				classInstanceCreations.addAll(expressionExtractor.getClassInstanceCreations(expression));
			}
		}
		else if(statement instanceof EnhancedForStatement) {
			EnhancedForStatement enhancedForStatement = (EnhancedForStatement)statement;
			Expression expression = enhancedForStatement.getExpression();
			methodInvocations.addAll(expressionExtractor.getMethodInvocations(expression));
			classInstanceCreations.addAll(expressionExtractor.getClassInstanceCreations(expression));
		}
		else if(statement instanceof DoStatement) {
			DoStatement doStatement = (DoStatement)statement;
			Expression doExpression = doStatement.getExpression();
			methodInvocations.addAll(expressionExtractor.getMethodInvocations(doExpression));
			classInstanceCreations.addAll(expressionExtractor.getClassInstanceCreations(doExpression));
		}
		else if(statement instanceof SwitchStatement) {
			SwitchStatement switchStatement = (SwitchStatement)statement;
			Expression switchExpression = switchStatement.getExpression();
			methodInvocations.addAll(expressionExtractor.getMethodInvocations(switchExpression));
			classInstanceCreations.addAll(expressionExtractor.getClassInstanceCreations(switchExpression));
		}
		else if(statement instanceof TryStatement) {
			
		}
		else {
			methodInvocations.addAll(expressionExtractor.getMethodInvocations(statement));
			classInstanceCreations.addAll(expressionExtractor.getClassInstanceCreations(statement));
		}
		Set<ITypeBinding> thrownExceptionTypes = new LinkedHashSet<ITypeBinding>();
		for(Expression expression : methodInvocations) {
			TryStatement surroundingTryBlock = surroundingTryBlock(expression);
			if(expression instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)expression;
				IMethodBinding methodInvocationBinding = methodInvocation.resolveMethodBinding();
				ITypeBinding[] exceptionTypes = methodInvocationBinding.getExceptionTypes();
				for(ITypeBinding typeBinding : exceptionTypes) {
					if(surroundingTryBlock == null || (surroundingTryBlock != null && !isNestedUnderAnonymousClassDeclaration(surroundingTryBlock))) {
						thrownExceptionTypes.add(typeBinding);
					}
				}
			}
			else if(expression instanceof SuperMethodInvocation) {
				SuperMethodInvocation superMethodInvocation = (SuperMethodInvocation)expression;
				IMethodBinding methodInvocationBinding = superMethodInvocation.resolveMethodBinding();
				ITypeBinding[] exceptionTypes = methodInvocationBinding.getExceptionTypes();
				for(ITypeBinding typeBinding : exceptionTypes) {
					if(surroundingTryBlock == null || (surroundingTryBlock != null && !isNestedUnderAnonymousClassDeclaration(surroundingTryBlock))) {
						thrownExceptionTypes.add(typeBinding);
					}
				}
			}
		}
		for(Expression expression : classInstanceCreations) {
			TryStatement surroundingTryBlock = surroundingTryBlock(expression);
			ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)expression;
			IMethodBinding constructorBinding = classInstanceCreation.resolveConstructorBinding();
			ITypeBinding[] exceptionTypes = constructorBinding.getExceptionTypes();
			for(ITypeBinding typeBinding : exceptionTypes) {
				if(surroundingTryBlock == null || (surroundingTryBlock != null && !isNestedUnderAnonymousClassDeclaration(surroundingTryBlock))) {
					thrownExceptionTypes.add(typeBinding);
				}
			}
		}
		if(statement instanceof ThrowStatement) {
			TryStatement surroundingTryBlock = surroundingTryBlock(statement);
			ThrowStatement throwStatement = (ThrowStatement)statement;
			TypeVisitor typeVisitor = new TypeVisitor();
			throwStatement.accept(typeVisitor);
			if(surroundingTryBlock == null || (surroundingTryBlock != null && !isNestedUnderAnonymousClassDeclaration(surroundingTryBlock))) {
				thrownExceptionTypes.addAll(typeVisitor.getTypeBindings());
			}
		}
		return thrownExceptionTypes;
	}

	private boolean isNestedUnderAnonymousClassDeclaration(ASTNode node) {
		ASTNode parent = node.getParent();
		while(parent != null) {
			if(parent instanceof AnonymousClassDeclaration) {
				return true;
			}
			parent = parent.getParent();
		}
		return false;
	}

	protected Statement processPredicateNode(PDGControlPredicateNode predicateNode, AST ast,
			ASTRewrite sourceRewriter, List<PDGNode> sliceNodes) {
		Statement oldPredicateStatement = predicateNode.getASTStatement();
		sliceNodes.remove(predicateNode);
		Statement newPredicateStatement = null;
		if(oldPredicateStatement instanceof IfStatement) {
			IfStatement oldIfStatement = (IfStatement)oldPredicateStatement;
			IfStatement newIfStatement = ast.newIfStatement();
			newPredicateStatement = newIfStatement;
			sourceRewriter.set(newIfStatement, IfStatement.EXPRESSION_PROPERTY, oldIfStatement.getExpression(), null);
			Iterator<GraphEdge> outgoingDependenceIterator = predicateNode.getOutgoingDependenceIterator();
			List<PDGNode> trueControlDependentChildren = new ArrayList<PDGNode>();
			List<PDGNode> falseControlDependentChildren = new ArrayList<PDGNode>();
			while(outgoingDependenceIterator.hasNext()) {
				PDGDependence dependence = (PDGDependence)outgoingDependenceIterator.next();
				if(dependence instanceof PDGControlDependence) {
					PDGControlDependence controlDependence = (PDGControlDependence)dependence;
					PDGNode dstPDGNode = (PDGNode)controlDependence.getDst();
					if(sliceNodes.contains(dstPDGNode)) {
						if(controlDependence.isTrueControlDependence()) {
							trueControlDependentChildren.add(dstPDGNode);
						}
						else if(controlDependence.isFalseControlDependence()) {
							falseControlDependentChildren.add(dstPDGNode);
						}
					}
				}
			}
			if(oldIfStatement.getThenStatement() instanceof Block || trueControlDependentChildren.size() > 1) {
				Block thenBlock = ast.newBlock();
				ListRewrite thenBodyRewrite = sourceRewriter.getListRewrite(thenBlock, Block.STATEMENTS_PROPERTY);
				for(PDGNode dstPDGNode : trueControlDependentChildren) {
					ListRewrite listRewrite = thenBodyRewrite;
					listRewrite = createTryStatementIfNeeded(sourceRewriter, ast, listRewrite, dstPDGNode);
					if(dstPDGNode instanceof PDGControlPredicateNode) {
						PDGControlPredicateNode dstPredicateNode = (PDGControlPredicateNode)dstPDGNode;
						listRewrite.insertLast(processPredicateNode(dstPredicateNode, ast, sourceRewriter, sliceNodes), null);
					}
					else {
						processStatementNode(listRewrite, dstPDGNode, ast, sourceRewriter);
						sliceNodes.remove(dstPDGNode);
					}
				}
				sourceRewriter.set(newIfStatement, IfStatement.THEN_STATEMENT_PROPERTY, thenBlock, null);
			}
			else if(trueControlDependentChildren.size() == 1) {
				PDGNode dstPDGNode = trueControlDependentChildren.get(0);
				if(dstPDGNode instanceof PDGControlPredicateNode) {
					PDGControlPredicateNode dstPredicateNode = (PDGControlPredicateNode)dstPDGNode;
					sourceRewriter.set(newIfStatement, IfStatement.THEN_STATEMENT_PROPERTY, processPredicateNode(dstPredicateNode, ast, sourceRewriter, sliceNodes), null);
				}
				else {
					sourceRewriter.set(newIfStatement, IfStatement.THEN_STATEMENT_PROPERTY, dstPDGNode.getASTStatement(), null);
					sliceNodes.remove(dstPDGNode);
				}
			}
			if(oldIfStatement.getElseStatement() instanceof Block || falseControlDependentChildren.size() > 1) {
				Block elseBlock = ast.newBlock();
				ListRewrite elseBodyRewrite = sourceRewriter.getListRewrite(elseBlock, Block.STATEMENTS_PROPERTY);
				for(PDGNode dstPDGNode : falseControlDependentChildren) {
					ListRewrite listRewrite = elseBodyRewrite;
					listRewrite = createTryStatementIfNeeded(sourceRewriter, ast, listRewrite, dstPDGNode);
					if(dstPDGNode instanceof PDGControlPredicateNode) {
						PDGControlPredicateNode dstPredicateNode = (PDGControlPredicateNode)dstPDGNode;
						listRewrite.insertLast(processPredicateNode(dstPredicateNode, ast, sourceRewriter, sliceNodes), null);
					}
					else {
						processStatementNode(listRewrite, dstPDGNode, ast, sourceRewriter);
						sliceNodes.remove(dstPDGNode);
					}
				}
				sourceRewriter.set(newIfStatement, IfStatement.ELSE_STATEMENT_PROPERTY, elseBlock, null);
			}
			else if(falseControlDependentChildren.size() == 1) {
				PDGNode dstPDGNode = falseControlDependentChildren.get(0);
				if(dstPDGNode instanceof PDGControlPredicateNode) {
					PDGControlPredicateNode dstPredicateNode = (PDGControlPredicateNode)dstPDGNode;
					sourceRewriter.set(newIfStatement, IfStatement.ELSE_STATEMENT_PROPERTY, processPredicateNode(dstPredicateNode, ast, sourceRewriter, sliceNodes), null);
				}
				else {
					sourceRewriter.set(newIfStatement, IfStatement.ELSE_STATEMENT_PROPERTY, dstPDGNode.getASTStatement(), null);
					sliceNodes.remove(dstPDGNode);
				}
			}	
		}
		else if(oldPredicateStatement instanceof SwitchStatement) {
			SwitchStatement oldSwitchStatement = (SwitchStatement)oldPredicateStatement;
			SwitchStatement newSwitchStatement = ast.newSwitchStatement();
			newPredicateStatement = newSwitchStatement;
			sourceRewriter.set(newSwitchStatement, SwitchStatement.EXPRESSION_PROPERTY, oldSwitchStatement.getExpression(), null);
			ListRewrite switchStatementsRewrite = sourceRewriter.getListRewrite(newSwitchStatement, SwitchStatement.STATEMENTS_PROPERTY);
			Iterator<GraphEdge> outgoingDependenceIterator = predicateNode.getOutgoingDependenceIterator();
			while(outgoingDependenceIterator.hasNext()) {
				ListRewrite bodyRewrite = switchStatementsRewrite;
				PDGDependence dependence = (PDGDependence)outgoingDependenceIterator.next();
				if(dependence instanceof PDGControlDependence) {
					PDGControlDependence controlDependence = (PDGControlDependence)dependence;
					PDGNode dstPDGNode = (PDGNode)controlDependence.getDst();
					if(sliceNodes.contains(dstPDGNode)) {
						bodyRewrite = createTryStatementIfNeeded(sourceRewriter, ast, bodyRewrite, dstPDGNode);
						if(dstPDGNode instanceof PDGControlPredicateNode) {
							PDGControlPredicateNode dstPredicateNode = (PDGControlPredicateNode)dstPDGNode;
							bodyRewrite.insertLast(processPredicateNode(dstPredicateNode, ast, sourceRewriter, sliceNodes), null);
						}
						else {
							processStatementNode(bodyRewrite, dstPDGNode, ast, sourceRewriter);
							sliceNodes.remove(dstPDGNode);
						}
					}
				}
			}
		}
		else {
			Block loopBlock = ast.newBlock();
			ListRewrite loopBodyRewrite = sourceRewriter.getListRewrite(loopBlock, Block.STATEMENTS_PROPERTY);
			Iterator<GraphEdge> outgoingDependenceIterator = predicateNode.getOutgoingDependenceIterator();
			while(outgoingDependenceIterator.hasNext()) {
				ListRewrite bodyRewrite = loopBodyRewrite;
				PDGDependence dependence = (PDGDependence)outgoingDependenceIterator.next();
				if(dependence instanceof PDGControlDependence) {
					PDGControlDependence controlDependence = (PDGControlDependence)dependence;
					PDGNode dstPDGNode = (PDGNode)controlDependence.getDst();
					if(sliceNodes.contains(dstPDGNode)) {
						bodyRewrite = createTryStatementIfNeeded(sourceRewriter, ast, bodyRewrite, dstPDGNode);
						if(dstPDGNode instanceof PDGControlPredicateNode) {
							PDGControlPredicateNode dstPredicateNode = (PDGControlPredicateNode)dstPDGNode;
							bodyRewrite.insertLast(processPredicateNode(dstPredicateNode, ast, sourceRewriter, sliceNodes), null);
						}
						else {
							processStatementNode(bodyRewrite, dstPDGNode, ast, sourceRewriter);
							sliceNodes.remove(dstPDGNode);
						}
					}
				}
			}
			if(oldPredicateStatement instanceof WhileStatement) {
				WhileStatement oldWhileStatement = (WhileStatement)oldPredicateStatement;
				WhileStatement newWhileStatement = ast.newWhileStatement();
				newPredicateStatement = newWhileStatement;
				sourceRewriter.set(newWhileStatement, WhileStatement.EXPRESSION_PROPERTY, oldWhileStatement.getExpression(), null);
				sourceRewriter.set(newWhileStatement, WhileStatement.BODY_PROPERTY, loopBlock, null);
			}
			else if(oldPredicateStatement instanceof ForStatement) {
				ForStatement oldForStatement = (ForStatement)oldPredicateStatement;
				ForStatement newForStatement = ast.newForStatement();
				newPredicateStatement = newForStatement;
				sourceRewriter.set(newForStatement, ForStatement.EXPRESSION_PROPERTY, oldForStatement.getExpression(), null);
				ListRewrite initializerRewrite = sourceRewriter.getListRewrite(newForStatement, ForStatement.INITIALIZERS_PROPERTY);
				List<Expression> initializers = oldForStatement.initializers();
				for(Expression expression : initializers)
					initializerRewrite.insertLast(expression, null);
				ListRewrite updaterRewrite = sourceRewriter.getListRewrite(newForStatement, ForStatement.UPDATERS_PROPERTY);
				List<Expression> updaters = oldForStatement.updaters();
				for(Expression expression : updaters)
					updaterRewrite.insertLast(expression, null);
				sourceRewriter.set(newForStatement, ForStatement.BODY_PROPERTY, loopBlock, null);
			}
			else if(oldPredicateStatement instanceof EnhancedForStatement) {
				EnhancedForStatement oldEnhancedForStatement = (EnhancedForStatement)oldPredicateStatement;
				EnhancedForStatement newEnhancedForStatement = ast.newEnhancedForStatement();
				newPredicateStatement = newEnhancedForStatement;
				sourceRewriter.set(newEnhancedForStatement, EnhancedForStatement.PARAMETER_PROPERTY, oldEnhancedForStatement.getParameter(), null);
				sourceRewriter.set(newEnhancedForStatement, EnhancedForStatement.EXPRESSION_PROPERTY, oldEnhancedForStatement.getExpression(), null);
				sourceRewriter.set(newEnhancedForStatement, EnhancedForStatement.BODY_PROPERTY, loopBlock, null);
			}
			else if(oldPredicateStatement instanceof DoStatement) {
				DoStatement oldDoStatement = (DoStatement)oldPredicateStatement;
				DoStatement newDoStatement = ast.newDoStatement();
				newPredicateStatement = newDoStatement;
				sourceRewriter.set(newDoStatement, DoStatement.EXPRESSION_PROPERTY, oldDoStatement.getExpression(), null);
				sourceRewriter.set(newDoStatement, DoStatement.BODY_PROPERTY, loopBlock, null);
			}
		}
		LabeledStatement labeled = belongsToLabeledStatement(predicateNode);
		if(labeled != null) {
			labeledStatementsToBeRemoved.add(labeled);
			LabeledStatement newLabeledStatement = ast.newLabeledStatement();
			sourceRewriter.set(newLabeledStatement, LabeledStatement.LABEL_PROPERTY, labeled.getLabel(), null);
			sourceRewriter.set(newLabeledStatement, LabeledStatement.BODY_PROPERTY, newPredicateStatement, null);
			newPredicateStatement = newLabeledStatement;
		}
		return newPredicateStatement;
	}

	protected void processStatementNode(ListRewrite bodyRewrite, PDGNode dstPDGNode, AST ast, ASTRewrite sourceRewriter) {
		bodyRewrite.insertLast(dstPDGNode.getASTStatement(), null);
	}

	private LabeledStatement belongsToLabeledStatement(PDGNode pdgNode) {
		Statement statement = pdgNode.getASTStatement();
		if(statement.getParent() instanceof LabeledStatement) {
			return (LabeledStatement) statement.getParent();
		}
		return null;
	}

	protected ListRewrite createTryStatementIfNeeded(ASTRewrite sourceRewriter, AST ast, ListRewrite bodyRewrite, PDGNode node) {
		Statement statement = node.getASTStatement();
		ASTNode statementParent = statement.getParent();
		if(statementParent != null && statementParent instanceof Block)
			statementParent = statementParent.getParent();
		if(statementParent != null && statementParent instanceof TryStatement) {
			TryStatement tryStatementParent = (TryStatement)statementParent;
			if(tryStatementsToBeRemoved.contains(tryStatementParent) || tryStatementsToBeCopied.contains(tryStatementParent)) {
				if(tryStatementBodyRewriteMap.containsKey(tryStatementParent)) {
					bodyRewrite = tryStatementBodyRewriteMap.get(tryStatementParent);
				}
				else {
					TryStatement newTryStatement = copyTryStatement(sourceRewriter, ast, tryStatementParent);
					Block tryMethodBody = ast.newBlock();
					sourceRewriter.set(newTryStatement, TryStatement.BODY_PROPERTY, tryMethodBody, null);
					ListRewrite tryBodyRewrite = sourceRewriter.getListRewrite(tryMethodBody, Block.STATEMENTS_PROPERTY);
					tryStatementBodyRewriteMap.put(tryStatementParent, tryBodyRewrite);
					bodyRewrite.insertLast(newTryStatement, null);
					bodyRewrite = tryBodyRewrite;
				}
			}
		}
		return bodyRewrite;
	}

	protected TryStatement copyTryStatement(ASTRewrite sourceRewriter, AST ast, TryStatement tryStatementParent) {
		TryStatement newTryStatement = ast.newTryStatement();
		ListRewrite resourceRewrite = sourceRewriter.getListRewrite(newTryStatement, TryStatement.RESOURCES_PROPERTY);
		List<VariableDeclarationExpression> resources = tryStatementParent.resources();
		for(VariableDeclarationExpression expression : resources) {
			resourceRewrite.insertLast(expression, null);
		}
		ListRewrite catchClauseRewrite = sourceRewriter.getListRewrite(newTryStatement, TryStatement.CATCH_CLAUSES_PROPERTY);
		List<CatchClause> catchClauses = tryStatementParent.catchClauses();
		for(CatchClause catchClause : catchClauses) {
			catchClauseRewrite.insertLast(catchClause, null);
		}
		if(tryStatementParent.getFinally() != null) {
			sourceRewriter.set(newTryStatement, TryStatement.FINALLY_PROPERTY, tryStatementParent.getFinally(), null);
		}
		return newTryStatement;
	}

	protected PDGControlPredicateNode isInsideDoLoop(PDGNode node) {
		for(CFGBranchDoLoopNode doLoopNode : doLoopNodes) {
			if(node.getId() >= doLoopNode.getJoinNode().getId() && node.getId() < doLoopNode.getId()) {
				PDGControlPredicateNode predicateNode = (PDGControlPredicateNode)doLoopNode.getPDGNode();
				return predicateNode;
			}
		}
		return null;
	}

	protected TryStatement surroundingTryBlock(ASTNode statement) {
		ASTNode parent = statement.getParent();
		while(!(parent instanceof MethodDeclaration)) {
			if(parent instanceof TryStatement)
				return (TryStatement)parent;
			parent = parent.getParent();
		}
		return null;
	}

	protected boolean tryBlockCatchesExceptionType(TryStatement tryStatement, ITypeBinding exceptionType) {
		List<CatchClause> catchClauses = tryStatement.catchClauses();
		for(CatchClause catchClause : catchClauses) {
			SingleVariableDeclaration exceptionDeclaration = catchClause.getException();
			Type exceptionDeclarationType = exceptionDeclaration.getType();
			if(exceptionDeclarationType instanceof UnionType) {
				UnionType unionType = (UnionType)exceptionDeclarationType;
				List<Type> types = unionType.types();
				for(Type type : types) {
					ITypeBinding exceptionDeclarationTypeBinding = type.resolveBinding();
					if(equalOrExtend(exceptionType, exceptionDeclarationTypeBinding))
						return true;
				}
			}
			else {
				ITypeBinding exceptionDeclarationTypeBinding = exceptionDeclarationType.resolveBinding();
				if(equalOrExtend(exceptionType, exceptionDeclarationTypeBinding))
					return true;
			}
		}
		return false;
	}

	private boolean equalOrExtend(ITypeBinding subTypeBinding, ITypeBinding superTypeBinding) {
		if(subTypeBinding.isEqualTo(superTypeBinding))
			return true;
		ITypeBinding superClassTypeBinding = subTypeBinding.getSuperclass();
		if(superClassTypeBinding != null) {
			return equalOrExtend(superClassTypeBinding, superTypeBinding);
		}
		else {
			return false;
		}
	}

	protected ITypeBinding extractTypeBinding(VariableDeclaration variableDeclaration) {
		IVariableBinding variableBinding = variableDeclaration.resolveBinding();
		return variableBinding.getType();
	}

	protected Expression generateDefaultValue(ASTRewrite sourceRewriter, AST ast, ITypeBinding returnTypeBinding) {
		Expression returnedExpression = null;
		if(returnTypeBinding.isPrimitive()) {
			if(returnTypeBinding.getQualifiedName().equals("boolean")) {
				returnedExpression = ast.newBooleanLiteral(false);
			}
			else if(returnTypeBinding.getQualifiedName().equals("char")) {
				CharacterLiteral characterLiteral = ast.newCharacterLiteral();
				sourceRewriter.set(characterLiteral, CharacterLiteral.ESCAPED_VALUE_PROPERTY, "\u0000", null);
				returnedExpression = characterLiteral;
			}
			else if(returnTypeBinding.getQualifiedName().equals("int") ||
					returnTypeBinding.getQualifiedName().equals("short") ||
					returnTypeBinding.getQualifiedName().equals("byte")) {
				returnedExpression = ast.newNumberLiteral("0");
			}
			else if(returnTypeBinding.getQualifiedName().equals("long")) {
				returnedExpression = ast.newNumberLiteral("0L");
			}
			else if(returnTypeBinding.getQualifiedName().equals("float")) {
				returnedExpression = ast.newNumberLiteral("0.0f");
			}
			else if(returnTypeBinding.getQualifiedName().equals("double")) {
				returnedExpression = ast.newNumberLiteral("0.0d");
			}
			else if(returnTypeBinding.getQualifiedName().equals("void")) {
				returnedExpression = null;
			}
		}
		else {
			returnedExpression = ast.newNullLiteral();
		}
		return returnedExpression;
	}
}
