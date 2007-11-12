package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.inheritance.InheritanceTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class TypeCheckElimination {
	private Map<Expression, ArrayList<Statement>> typeCheckMap;
	private Map<Expression, SimpleName> staticFieldMap;
	private VariableDeclarationFragment typeField;
	private MethodDeclaration typeFieldGetterMethod;
	private MethodDeclaration typeFieldSetterMethod;
	private Statement typeCheckCodeFragment;
	private MethodDeclaration typeCheckMethod;
	private LinkedHashSet<VariableDeclarationFragment> accessedFields;
	private MethodInvocation typeMethodInvocation;
	private InheritanceTree existingInheritanceTree;
	
	public TypeCheckElimination() {
		this.typeCheckMap = new LinkedHashMap<Expression, ArrayList<Statement>>();
		this.staticFieldMap = new LinkedHashMap<Expression, SimpleName>();
		this.typeField = null;
		this.typeFieldGetterMethod = null;
		this.typeFieldSetterMethod = null;
		this.typeCheckCodeFragment = null;
		this.typeCheckMethod = null;
		this.accessedFields = new LinkedHashSet<VariableDeclarationFragment>();
		this.typeMethodInvocation = null;
		this.existingInheritanceTree = null;
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
	
	public void addStaticType(Expression expression, SimpleName simpleName) {
		staticFieldMap.put(expression, simpleName);
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
	
	public List<SimpleName> getStaticFields() {
		return new ArrayList<SimpleName>(staticFieldMap.values());
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

	public MethodInvocation getTypeMethodInvocation() {
		return typeMethodInvocation;
	}

	public void setTypeMethodInvocation(MethodInvocation typeMethodInvocation) {
		this.typeMethodInvocation = typeMethodInvocation;
	}

	public InheritanceTree getExistingInheritanceTree() {
		return existingInheritanceTree;
	}

	public void setExistingInheritanceTree(InheritanceTree existingInheritanceTree) {
		this.existingInheritanceTree = existingInheritanceTree;
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
		if(typeField != null) {
			String typeFieldName = typeField.getName().getIdentifier();
			return typeFieldName.substring(0, 1).toUpperCase() + typeFieldName.substring(1, typeFieldName.length());
		}
		else if(existingInheritanceTree != null) {
			DefaultMutableTreeNode root = existingInheritanceTree.getRootNode();
			return (String)root.getUserObject();
		}
		else {
			return null;
		}
	}
	
	public List<String> getSubclassNames() {
		List<String> subclassNames = new ArrayList<String>();
		for(SimpleName simpleName : staticFieldMap.values()) {
			String staticFieldName = simpleName.getIdentifier();
			//The case that the type field name is just one word : NAME
			if(!staticFieldName.contains("_")) {
				String subclassName = staticFieldName.substring(0, 1).toUpperCase() + 
				staticFieldName.substring(1, staticFieldName.length()).toLowerCase();
				if(existingInheritanceTree != null) {
					DefaultMutableTreeNode root = existingInheritanceTree.getRootNode();
					Enumeration<DefaultMutableTreeNode> enumeration = root.children();
					boolean found = false;
					while(enumeration.hasMoreElements()) {
						DefaultMutableTreeNode child = enumeration.nextElement();
						String childClassName = (String)child.getUserObject();
						if(childClassName.toLowerCase().contains(subclassName.toLowerCase())) {
							subclassNames.add(childClassName);
							found = true;
							break;
						}
					}
					if(!found)
						subclassNames.add(null);
				}
				else {
					subclassNames.add(subclassName);
				}
			}
			//In the case the static field name is like: STATIC_NAME_TEST we must remove the "_" 
			//and transform all letters to lower case, except the first letter of each word. 
			else {
				String finalName = "";
				StringTokenizer tokenizer = new StringTokenizer(staticFieldName,"_");
				while(tokenizer.hasMoreTokens()) {
					String tempName = tokenizer.nextToken().toLowerCase().toString();
					finalName += tempName.subSequence(0, 1).toString().toUpperCase() + 
									tempName.subSequence(1, tempName.length()).toString();
				}
				if(existingInheritanceTree != null) {
					DefaultMutableTreeNode root = existingInheritanceTree.getRootNode();
					Enumeration<DefaultMutableTreeNode> enumeration = root.children();
					boolean found = false;
					while(enumeration.hasMoreElements()) {
						DefaultMutableTreeNode child = enumeration.nextElement();
						String childClassName = (String)child.getUserObject();
						if(childClassName.toLowerCase().contains(finalName.toLowerCase())) {
							subclassNames.add(childClassName);
							found = true;
							break;
						}
					}
					if(!found)
						subclassNames.add(null);
				}
				else {
					subclassNames.add(finalName);
				}
			}
		}
		return subclassNames;
	}
}
