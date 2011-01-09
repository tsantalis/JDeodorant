package gr.uom.java.ast.decomposition.cfg;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.CatchClauseObject;
import gr.uom.java.ast.decomposition.TryStatementObject;

public class CFGTryNode extends CFGNode {
	private List<String> handledExceptions;
	
	public CFGTryNode(AbstractStatement statement) {
		super(statement);
		this.handledExceptions = new ArrayList<String>();
		TryStatementObject tryStatement = (TryStatementObject)statement;
		for(CatchClauseObject catchClause : tryStatement.getCatchClauses()) {
			handledExceptions.add(catchClause.getExceptionType());
		}
	}

	public List<String> getHandledExceptions() {
		return handledExceptions;
	}
}
