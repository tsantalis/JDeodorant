package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.CatchClauseObject;
import gr.uom.java.ast.decomposition.TryStatementObject;

public class CFGTryNode extends CFGBlockNode {
	private List<String> handledExceptions;
	private boolean hasResources;
	public CFGTryNode(AbstractStatement statement) {
		super(statement);
		this.handledExceptions = new ArrayList<String>();
		TryStatementObject tryStatement = (TryStatementObject)statement;
		this.hasResources = tryStatement.hasResources();
		for(CatchClauseObject catchClause : tryStatement.getCatchClauses()) {
			handledExceptions.addAll(catchClause.getExceptionTypes());
		}
	}
	
	public boolean hasResources() {
		return hasResources;
	}

	public List<String> getHandledExceptions() {
		return handledExceptions;
	}
	
	public boolean hasFinallyClauseClosingVariable(AbstractVariable variable) {
		return ((TryStatementObject)getStatement()).hasFinallyClauseClosingVariable(variable);
	}

	public boolean hasCatchClause() {
		return ((TryStatementObject)getStatement()).hasCatchClause();
	}
}
