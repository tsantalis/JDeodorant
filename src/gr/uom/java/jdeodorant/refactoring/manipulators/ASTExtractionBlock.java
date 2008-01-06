package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.StatementExtractor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class ASTExtractionBlock {
	private String extractedMethodName;
	private VariableDeclarationFragment returnVariableDeclarationFragment;
	private VariableDeclarationStatement returnVariableDeclarationStatement;
	private List<Statement> statementsForExtraction;
	private List<String> assignmentOperators;
	private Statement parentStatementForCopy;
	//includes variable declaration statements that should be added in extracted method's body
	//instead of being passed as parameters
	private Map<VariableDeclarationFragment, VariableDeclarationStatement> additionalRequiredVariableDeclarationStatementMap;
	//includes all variable declaration statements which are related with the extracted method
	private List<VariableDeclarationStatement> allVariableDeclarationStatements;
	//includes all variable declaration expressions which are related with the extracted method
	private List<VariableDeclarationExpression> allVariableDeclarationExpressions;
	
	public ASTExtractionBlock(String extractedMethodName, VariableDeclarationFragment returnVariableDeclarationFragment, VariableDeclarationStatement returnVariableDeclarationStatement,
			List<Statement> statementsForExtraction, List<VariableDeclarationStatement> allVariableDeclarationStatements, List<VariableDeclarationExpression> allVariableDeclarationExpressions, List<String> assignmentOperators) {
		this.extractedMethodName = extractedMethodName;
		this.returnVariableDeclarationFragment = returnVariableDeclarationFragment;
		this.returnVariableDeclarationStatement = returnVariableDeclarationStatement;
		this.statementsForExtraction = statementsForExtraction;
		this.allVariableDeclarationStatements = allVariableDeclarationStatements;
		this.allVariableDeclarationExpressions = allVariableDeclarationExpressions;
		this.assignmentOperators = assignmentOperators;
		this.parentStatementForCopy = null;
		this.additionalRequiredVariableDeclarationStatementMap = new LinkedHashMap<VariableDeclarationFragment, VariableDeclarationStatement>();
	}

	public String getExtractedMethodName() {
		return extractedMethodName;
	}

	public VariableDeclarationFragment getReturnVariableDeclarationFragment() {
		return returnVariableDeclarationFragment;
	}

	public VariableDeclarationStatement getReturnVariableDeclarationStatement() {
		return returnVariableDeclarationStatement;
	}

	public List<Statement> getStatementsForExtraction() {
		return statementsForExtraction;
	}

	public Statement getParentStatementForCopy() {
		return parentStatementForCopy;
	}

	public List<VariableDeclarationStatement> getAllVariableDeclarationStatements() {
		return allVariableDeclarationStatements;
	}

	public List<VariableDeclarationExpression> getAllVariableDeclarationExpressions() {
		return allVariableDeclarationExpressions;
	}

	public void setParentStatementForCopy(Statement parentStatementForCopy) {
		this.parentStatementForCopy = parentStatementForCopy;
	}
	
	public void addRequiredVariableDeclarationStatement(VariableDeclarationFragment key, VariableDeclarationStatement value) {
		this.additionalRequiredVariableDeclarationStatementMap.put(key, value);
	}
	
	public Set<VariableDeclarationFragment> getAdditionalRequiredVariableDeclarationFragments() {
		return this.additionalRequiredVariableDeclarationStatementMap.keySet();
	}
	
	public VariableDeclarationStatement getAdditionalRequiredVariableDeclarationStatement(VariableDeclarationFragment fragment) {
		return this.additionalRequiredVariableDeclarationStatementMap.get(fragment);
	}
	
	public Set<ITypeBinding> getThrownExceptions() {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		StatementExtractor statementExtractor = new StatementExtractor();
		Set<ITypeBinding> thrownExceptions = new LinkedHashSet<ITypeBinding>();
		for(Statement statementForExtraction : statementsForExtraction) {
			List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(statementForExtraction);
			List<Expression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(statementForExtraction);
			List<Statement> tryStatements = statementExtractor.getTryStatements(statementForExtraction);
			Set<ITypeBinding> catchClauseExceptions = new LinkedHashSet<ITypeBinding>();
			for(Statement statement : tryStatements) {
				TryStatement tryStatement = (TryStatement)statement;
				List<CatchClause> catchClauses = tryStatement.catchClauses();
				for(CatchClause catchClause : catchClauses) {
					SingleVariableDeclaration exception = catchClause.getException();
					Type exceptionType = exception.getType();
					catchClauseExceptions.add(exceptionType.resolveBinding());
				}
			}
			for(Expression expression : methodInvocations) {
				if(expression instanceof MethodInvocation) {
					MethodInvocation methodInvocation = (MethodInvocation)expression;
					IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
					ITypeBinding[] typeBindings = methodBinding.getExceptionTypes();
					for(ITypeBinding typeBinding : typeBindings) {
						if(!catchClauseExceptions.contains(typeBinding))
							thrownExceptions.add(typeBinding);
					}
				}
			}
			for(Expression expression : classInstanceCreations) {
				ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)expression;
				IMethodBinding methodBinding = classInstanceCreation.resolveConstructorBinding();
				ITypeBinding[] typeBindings = methodBinding.getExceptionTypes();
				for(ITypeBinding typeBinding : typeBindings) {
					if(!catchClauseExceptions.contains(typeBinding))
						thrownExceptions.add(typeBinding);
				}
			}
		}
		return thrownExceptions;
	}
	
	public boolean allAssignmentOperatorsContainPlus() {
		for(String operator : assignmentOperators) {
			if(!operator.contains("+"))
				return false;
		}
		return true;
	}
	
	public boolean allAssignmentOperatorsContainMinus() {
		for(String operator : assignmentOperators) {
			if(!operator.contains("-"))
				return false;
		}
		return true;
	}
}
