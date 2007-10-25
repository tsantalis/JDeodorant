package gr.uom.java.jdeodorant.refactoring.manipulators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class TypeCheckElimination {
	private Map<Expression, ArrayList<Statement>> typeCheckMap;
	private Map<Expression, VariableDeclarationFragment> staticFieldMap;
	private VariableDeclarationFragment typeField;
	private MethodDeclaration typeFieldSetterMethod;
	private Statement typeCheckCodeFragment;
	private MethodDeclaration typeCheckMethod;
	
	public TypeCheckElimination() {
		this.typeCheckMap = new LinkedHashMap<Expression, ArrayList<Statement>>();
		this.staticFieldMap = new LinkedHashMap<Expression, VariableDeclarationFragment>();
		this.typeField = null;
		this.typeFieldSetterMethod = null;
		this.typeCheckCodeFragment = null;
		this.typeCheckMethod = null;
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
	
	public void addStaticType(Expression expression, VariableDeclarationFragment fragment) {
		staticFieldMap.put(expression, fragment);
	}
	
	public Set<Expression> getTypeCheckExpressions() {
		return typeCheckMap.keySet();
	}
	
	public Collection<ArrayList<Statement>> getTypeCheckStatements() {
		return typeCheckMap.values();
	}
	
	public VariableDeclarationFragment getTypeField() {
		return typeField;
	}
	
	public void setTypeField(VariableDeclarationFragment typeField) {
		this.typeField = typeField;
	}
	
	public MethodDeclaration getTypeFieldSetterMethod() {
		return typeFieldSetterMethod;
	}

	public void setTypeFieldSetterMethod(MethodDeclaration typeFieldSetterMethod) {
		this.typeFieldSetterMethod = typeFieldSetterMethod;
	}

	public Statement getTypeCheckCodeFragment() {
		return typeCheckCodeFragment;
	}

	public void setTypeCheckCodeFragment(Statement typeCheckCodeFragment) {
		this.typeCheckCodeFragment = typeCheckCodeFragment;
	}

	public MethodDeclaration getTypeCheckMethod() {
		return typeCheckMethod;
	}

	public void setTypeCheckMethod(MethodDeclaration typeCheckMethod) {
		this.typeCheckMethod = typeCheckMethod;
	}

	public boolean allTypeChecksContainStaticField() {
		return typeCheckMap.keySet().size() == staticFieldMap.keySet().size();
	}
	
	public Type getAbstractMethodReturnType() {
		return typeCheckMethod.getReturnType2();
	}
	
	public String getAbstractMethodName() {
		return typeCheckMethod.getName().getIdentifier();
	}
	
	public String getAbstractClassName() {
		String typeFieldName = typeField.getName().getIdentifier();
		return typeFieldName.replaceFirst(Character.toString(typeFieldName.charAt(0)),
			Character.toString(Character.toUpperCase(typeFieldName.charAt(0))));
	}
	
	public List<String> getSubclassNames() {
		List<String> subclassNames = new ArrayList<String>();
		for(VariableDeclarationFragment fragment : staticFieldMap.values()) {
			String fragmentName = fragment.getName().getIdentifier();
			if(!fragmentName.contains("_")) {
				String subclassName = fragmentName.substring(0,1).toUpperCase() + 
				fragmentName.substring(1, fragmentName.length()).toLowerCase();
				subclassNames.add(subclassName);
			}
			else {
				subclassNames.add(fragmentName);
				// TODO
			}
		}
		return subclassNames;
	}
}
