package gr.uom.java.ast.decomposition;

import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.Statement;

public class TryStatementObject extends CompositeStatementObject {
	private List<CatchClauseObject> catchClauses;
	private CompositeStatementObject finallyClause;
	
	public TryStatementObject(Statement statement, AbstractMethodFragment parent) {
		super(statement, StatementType.TRY, parent);
		this.catchClauses = new ArrayList<CatchClauseObject>();
	}

	public List<AbstractStatement> getStatementsInsideTryBlock() {
		CompositeStatementObject tryBlock = (CompositeStatementObject)getStatements().get(0);
		return tryBlock.getStatements();
	}

	public boolean hasResources() {
		return !super.getExpressions().isEmpty();
	}

	public void addCatchClause(CatchClauseObject catchClause) {
		catchClauses.add(catchClause);
		catchClause.setParent(this);
	}

	public List<CatchClauseObject> getCatchClauses() {
		return catchClauses;
	}

	public void setFinallyClause(CompositeStatementObject finallyClause) {
		this.finallyClause = finallyClause;
	}

	public CompositeStatementObject getFinallyClause() {
		return finallyClause;
	}

	public List<AbstractExpression> getExpressions() {
		List<AbstractExpression> expressions = new ArrayList<AbstractExpression>();
		expressions.addAll(super.getExpressions());
		for(CatchClauseObject catchClause : catchClauses) {
			expressions.addAll(catchClause.getExpressions());
		}
		return expressions;
	}

	public List<String> stringRepresentation() {
		List<String> stringRepresentation = new ArrayList<String>();
		stringRepresentation.addAll(super.stringRepresentation());
		for(CatchClauseObject catchClause : catchClauses) {
			stringRepresentation.addAll(catchClause.stringRepresentation());
		}
		if(finallyClause != null) {
			stringRepresentation.addAll(finallyClause.stringRepresentation());
		}
		return stringRepresentation;
	}

	public List<CompositeStatementObject> getIfStatements() {
		List<CompositeStatementObject> ifStatements = new ArrayList<CompositeStatementObject>();
		ifStatements.addAll(super.getIfStatements());
		for(CatchClauseObject catchClause : catchClauses) {
			ifStatements.addAll(catchClause.getIfStatements());
		}
		if(finallyClause != null) {
			ifStatements.addAll(finallyClause.getIfStatements());
		}
		return ifStatements;
	}

	public List<CompositeStatementObject> getSwitchStatements() {
		List<CompositeStatementObject> switchStatements = new ArrayList<CompositeStatementObject>();
		switchStatements.addAll(super.getSwitchStatements());
		for(CatchClauseObject catchClause : catchClauses) {
			switchStatements.addAll(catchClause.getSwitchStatements());
		}
		if(finallyClause != null) {
			switchStatements.addAll(finallyClause.getSwitchStatements());
		}
		return switchStatements;
	}

	public List<TryStatementObject> getTryStatements() {
		List<TryStatementObject> tryStatements = new ArrayList<TryStatementObject>();
		tryStatements.addAll(super.getTryStatements());
		for(CatchClauseObject catchClause : catchClauses) {
			tryStatements.addAll(catchClause.getTryStatements());
		}
		if(finallyClause != null) {
			tryStatements.addAll(finallyClause.getTryStatements());
		}
		return tryStatements;
	}
	
	public boolean hasFinallyClauseClosingVariable(AbstractVariable variable) {
		if(finallyClause != null) {
			Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughLocalVariables = 
					finallyClause.getInvokedMethodsThroughLocalVariables();
			for(AbstractVariable key : invokedMethodsThroughLocalVariables.keySet()) {
				if(key.equals(variable)) {
					LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughLocalVariables.get(key);
					for(MethodInvocationObject methodInvocation : methodInvocations) {
						if(methodInvocation.getMethodName().equals("close")) {
							return true;
						}
					}
				}
			}
			Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughParameters = 
					finallyClause.getInvokedMethodsThroughParameters();
			for(AbstractVariable key : invokedMethodsThroughParameters.keySet()) {
				if(key.equals(variable)) {
					LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughParameters.get(key);
					for(MethodInvocationObject methodInvocation : methodInvocations) {
						if(methodInvocation.getMethodName().equals("close")) {
							return true;
						}
					}
				}
			}
			Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughFields = 
					finallyClause.getInvokedMethodsThroughFields();
			for(AbstractVariable key : invokedMethodsThroughFields.keySet()) {
				if(key.equals(variable)) {
					LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughFields.get(key);
					for(MethodInvocationObject methodInvocation : methodInvocations) {
						if(methodInvocation.getMethodName().equals("close")) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public boolean hasCatchClause() {
		return !catchClauses.isEmpty();
	}
}
