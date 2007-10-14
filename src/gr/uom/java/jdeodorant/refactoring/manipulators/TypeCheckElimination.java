package gr.uom.java.jdeodorant.refactoring.manipulators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class TypeCheckElimination {
	private Map<Expression, ArrayList<Statement>> typeCheckMap;
	private Map<Expression, VariableDeclarationFragment> staticFieldMap;
	private VariableDeclarationFragment typeField;
	
	public TypeCheckElimination() {
		this.typeCheckMap = new LinkedHashMap<Expression, ArrayList<Statement>>();
		this.staticFieldMap = new LinkedHashMap<Expression, VariableDeclarationFragment>();
		this.typeField = null;
	}
	
	public void addTypeCheck(Expression expression, Statement statement) {
		if(typeCheckMap.containsKey(expression)) {
			ArrayList<Statement> statements = typeCheckMap.get(expression);
			statements.add(statement);
		}
		else {
			ArrayList<Statement> statements = new ArrayList<Statement>();
			statements.add(statement);
			typeCheckMap.put(expression, statements);
		}
	}
	
	public Set<Expression> getTypeCheckExpressions() {
		return typeCheckMap.keySet();
	}
}
