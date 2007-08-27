package gr.uom.java.ast.decomposition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import gr.uom.java.ast.LocalVariableDeclarationObject;

public class ExtractionBlock {
	private LocalVariableDeclarationObject returnVariableDeclaration;
	private VariableDeclarationStatement returnVariableDeclarationStatement;
	private List<AbstractStatement> statementsForExtraction;
	private AbstractStatement parentStatementForCopy;
	private Map<LocalVariableDeclarationObject, VariableDeclarationStatement> additionalRequiredVariableDeclarationStatementMap;
	
	public ExtractionBlock(LocalVariableDeclarationObject returnVariableDeclaration,
			VariableDeclarationStatement returnVariableDeclarationStatement,
			List<AbstractStatement> statementsForExtraction) {
		this.returnVariableDeclaration = returnVariableDeclaration;
		this.returnVariableDeclarationStatement = returnVariableDeclarationStatement;
		this.statementsForExtraction = statementsForExtraction;
		this.parentStatementForCopy = null;
		this.additionalRequiredVariableDeclarationStatementMap = new LinkedHashMap<LocalVariableDeclarationObject, VariableDeclarationStatement>();
	}

	public LocalVariableDeclarationObject getReturnVariableDeclaration() {
		return returnVariableDeclaration;
	}

	public VariableDeclarationStatement getReturnVariableDeclarationStatement() {
		return returnVariableDeclarationStatement;
	}

	public List<AbstractStatement> getStatementsForExtraction() {
		return statementsForExtraction;
	}

	public AbstractStatement getParentStatementForCopy() {
		return parentStatementForCopy;
	}

	public void setParentStatementForCopy(AbstractStatement parentStatementForCopy) {
		this.parentStatementForCopy = parentStatementForCopy;
	}

	public void addRequiredVariableDeclarationStatement(LocalVariableDeclarationObject key, VariableDeclarationStatement value) {
		this.additionalRequiredVariableDeclarationStatementMap.put(key, value);
	}
	
	public Set<LocalVariableDeclarationObject> getAdditionalRequiredVariableDeclarations() {
		return this.additionalRequiredVariableDeclarationStatementMap.keySet();
	}
	
	public VariableDeclarationStatement getAdditionalRequiredVariableDeclarationStatement(LocalVariableDeclarationObject key) {
		return this.additionalRequiredVariableDeclarationStatementMap.get(key);
	}
}
