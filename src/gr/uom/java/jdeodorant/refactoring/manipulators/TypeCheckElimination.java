package gr.uom.java.jdeodorant.refactoring.manipulators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class TypeCheckElimination {
	private Map<Expression, ArrayList<Statement>> typeCheckMap;
	private Map<Expression, VariableDeclarationFragment> staticFieldMap;
	private VariableDeclarationFragment typeField;
	private MethodDeclaration typeFieldGetterMethod;
	private MethodDeclaration typeFieldSetterMethod;
	private Statement typeCheckCodeFragment;
	private MethodDeclaration typeCheckMethod;
	private LinkedHashSet<VariableDeclarationFragment> accessedFields;
	
	public TypeCheckElimination() {
		this.typeCheckMap = new LinkedHashMap<Expression, ArrayList<Statement>>();
		this.staticFieldMap = new LinkedHashMap<Expression, VariableDeclarationFragment>();
		this.typeField = null;
		this.typeFieldGetterMethod = null;
		this.typeFieldSetterMethod = null;
		this.typeCheckCodeFragment = null;
		this.typeCheckMethod = null;
		this.accessedFields = new LinkedHashSet<VariableDeclarationFragment>();
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
	
	public void addAccessedField(VariableDeclarationFragment fragment) {
		accessedFields.add(fragment);
	}
	
	public Set<VariableDeclarationFragment> getAccessedFields() {
		return accessedFields;
	}
	
	public Set<Expression> getTypeCheckExpressions() {
		return typeCheckMap.keySet();
	}
	
	public Collection<ArrayList<Statement>> getTypeCheckStatements() {
		return typeCheckMap.values();
	}
	
	public List<VariableDeclarationFragment> getStaticFields() {
		return new ArrayList<VariableDeclarationFragment>(staticFieldMap.values());
	}
	
	public VariableDeclarationFragment getTypeField() {
		return typeField;
	}
	
	public void setTypeField(VariableDeclarationFragment typeField) {
		this.typeField = typeField;
	}
	
	public MethodDeclaration getTypeFieldGetterMethod() {
		return typeFieldGetterMethod;
	}

	public void setTypeFieldGetterMethod(MethodDeclaration typeFieldGetterMethod) {
		this.typeFieldGetterMethod = typeFieldGetterMethod;
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
			//The case that the type field name is just one word : NAME
			if(!fragmentName.contains("_")) {
				String subclassName = fragmentName.substring(0,1).toUpperCase() + 
				fragmentName.substring(1, fragmentName.length()).toLowerCase();
				subclassNames.add(subclassName);
			}
			//In the case the static field name is like: STATIC_NAME_TEST we must remove the "_" 
			//and transform all letters to lower case, except the first letter of each word. 
			else {
				String tempName = "";
				String finalName = "";
				StringTokenizer tokenizer = new StringTokenizer(fragmentName,"_");
				while(tokenizer.hasMoreTokens()) {
					tempName = tokenizer.nextToken().toLowerCase().toString();
					finalName += tempName.subSequence(0, 1).toString().toUpperCase() + 
									tempName.subSequence(1, tempName.length()).toString();
				}
				subclassNames.add(finalName);
			}
		}
		return subclassNames;
	}
}
