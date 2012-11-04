package gr.uom.java.ast.decomposition;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Statement;

public class TryStatementObject extends CompositeStatementObject {
	private List<CatchClauseObject> catchClauses;
	private CompositeStatementObject finallyClause;
	
	public TryStatementObject(Statement statement, String type) {
		super(statement, type);
		this.catchClauses = new ArrayList<CatchClauseObject>();
	}

	public boolean hasResources() {
		return !getExpressions().isEmpty();
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
			for(AbstractStatement statement : catchClause.getStatements()) {
				if(statement instanceof CompositeStatementObject) {
					CompositeStatementObject composite = (CompositeStatementObject)statement;
					ifStatements.addAll(composite.getIfStatements());
				}
			}
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
			for(AbstractStatement statement : catchClause.getStatements()) {
				if(statement instanceof CompositeStatementObject) {
					CompositeStatementObject composite = (CompositeStatementObject)statement;
					switchStatements.addAll(composite.getSwitchStatements());
				}
			}
		}
		if(finallyClause != null) {
			switchStatements.addAll(finallyClause.getSwitchStatements());
		}
		return switchStatements;
	}
}
