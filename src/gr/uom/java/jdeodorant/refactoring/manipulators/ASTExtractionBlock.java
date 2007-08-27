package gr.uom.java.jdeodorant.refactoring.manipulators;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class ASTExtractionBlock {
	private VariableDeclarationFragment returnVariableDeclarationFragment;
	private VariableDeclarationStatement returnVariableDeclarationStatement;
	private List<Statement> statementsForExtraction;
	private Statement parentStatementForCopy;
	//includes variable declaration statements that should be added in extracted method's body
	//instead of being passed as parameters
	private Map<VariableDeclarationFragment, VariableDeclarationStatement> additionalRequiredVariableDeclarationStatementMap;
	//includes all variable declaration statements which are related with the extracted method
	private List<VariableDeclarationStatement> allVariableDeclarationStatements;
	
	public ASTExtractionBlock(VariableDeclarationFragment returnVariableDeclarationFragment, VariableDeclarationStatement returnVariableDeclarationStatement,
			List<Statement> statementsForExtraction, List<VariableDeclarationStatement> allVariableDeclarationStatements) {
		this.returnVariableDeclarationFragment = returnVariableDeclarationFragment;
		this.returnVariableDeclarationStatement = returnVariableDeclarationStatement;
		this.statementsForExtraction = statementsForExtraction;
		this.allVariableDeclarationStatements = allVariableDeclarationStatements;
		this.parentStatementForCopy = null;
		this.additionalRequiredVariableDeclarationStatementMap = new LinkedHashMap<VariableDeclarationFragment, VariableDeclarationStatement>();
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
}
