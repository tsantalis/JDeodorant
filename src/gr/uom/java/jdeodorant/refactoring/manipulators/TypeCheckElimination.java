package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.inheritance.InheritanceTree;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.StatementExtractor;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class TypeCheckElimination {
	private Map<Expression, ArrayList<Statement>> typeCheckMap;
	private Map<Expression, SimpleName> staticFieldMap;
	private VariableDeclarationFragment typeField;
	private MethodDeclaration typeFieldGetterMethod;
	private MethodDeclaration typeFieldSetterMethod;
	private Statement typeCheckCodeFragment;
	private MethodDeclaration typeCheckMethod;
	private LinkedHashSet<VariableDeclarationFragment> accessedFields;
	private LinkedHashSet<SingleVariableDeclaration> accessedParameters;
	private LinkedHashSet<VariableDeclarationFragment> accessedLocalVariables;
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
		this.accessedParameters = new LinkedHashSet<SingleVariableDeclaration>();
		this.accessedLocalVariables = new LinkedHashSet<VariableDeclarationFragment>();
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
	
	public void addAccessedLocalVariable(VariableDeclarationFragment fragment){
		accessedLocalVariables.add(fragment);
	}
	
	public void addAccessedParameter(SingleVariableDeclaration parameter) {
		accessedParameters.add(parameter);
	}
	
	public LinkedHashSet<VariableDeclarationFragment> getAccessedLocalVariables() {
		return accessedLocalVariables;
	}

	public Set<VariableDeclarationFragment> getAccessedFields() {
		return accessedFields;
	}
	
	public Set<SingleVariableDeclaration> getAccessedParameters() {
		return accessedParameters;
	}
	
	public Set<Expression> getTypeCheckExpressions() {
		return typeCheckMap.keySet();
	}
	
	public List<ArrayList<Statement>> getTypeCheckStatements() {
		return new ArrayList<ArrayList<Statement>>(typeCheckMap.values());
	}
	
	public List<SimpleName> getStaticFields() {
		return new ArrayList<SimpleName>(staticFieldMap.values());
	}
	
	public List<String> getStaticFieldNames() {
		List<String> staticFieldNames = new ArrayList<String>();
		for(SimpleName simpleName : staticFieldMap.values()) {
			staticFieldNames.add(simpleName.getIdentifier());
		}
		return staticFieldNames;
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
		return (typeCheckMap.keySet().size() > 1) && (typeCheckMap.keySet().size() == staticFieldMap.keySet().size());
	}
	
	public Type getTypeCheckMethodReturnType() {
		return typeCheckMethod.getReturnType2();
	}
	
	public String getTypeCheckMethodName() {
		return typeCheckMethod.getName().getIdentifier();
	}
	
	public List<SingleVariableDeclaration> getTypeCheckMethodParameters() {
		return typeCheckMethod.parameters();
	}
	
	public VariableDeclarationFragment getTypeCheckMethodReturnedVariable() {
		StatementExtractor statementExtractor = new StatementExtractor();
		List<Statement> returnStatements = statementExtractor.getReturnStatements(typeCheckMethod.getBody());
		if(returnStatements.size() > 0) {
			ReturnStatement lastReturnStatement = (ReturnStatement)returnStatements.get(returnStatements.size()-1);
			if(lastReturnStatement.getExpression() instanceof SimpleName) {
				SimpleName returnExpression = (SimpleName)lastReturnStatement.getExpression();
				List<Statement> variableDeclarationStatements = statementExtractor.getVariableDeclarations(typeCheckMethod.getBody());
				for(Statement statement : variableDeclarationStatements) {
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
					List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
					for(VariableDeclarationFragment fragment : fragments) {
						if(fragment.getName().getIdentifier().equals(returnExpression.getIdentifier()))
							return fragment;
					}
				}
			}
		}
		return null;
	}
	
	public String getAbstractClassName() {
		if(typeField != null) {
			String typeFieldName = typeField.getName().getIdentifier().replaceAll("_", "");
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
		for(Expression expression : staticFieldMap.keySet()) {
			SimpleName simpleName = staticFieldMap.get(expression);
			String staticFieldName = simpleName.getIdentifier();
			Type castingType = isFirstStatementACastingVariableDeclaration(typeCheckMap.get(expression));
			//The case that the type field name is just one word : NAME
			if(!staticFieldName.contains("_")) {
				String subclassName = staticFieldName.substring(0, 1).toUpperCase() + 
				staticFieldName.substring(1, staticFieldName.length()).toLowerCase();
				if(existingInheritanceTree != null && castingType != null) {
					DefaultMutableTreeNode root = existingInheritanceTree.getRootNode();
					Enumeration<DefaultMutableTreeNode> enumeration = root.children();
					boolean found = false;
					while(enumeration.hasMoreElements()) {
						DefaultMutableTreeNode child = enumeration.nextElement();
						String childClassName = (String)child.getUserObject();
						if(castingType.resolveBinding().getQualifiedName().equals(childClassName)) {
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
				if(existingInheritanceTree != null && castingType != null) {
					DefaultMutableTreeNode root = existingInheritanceTree.getRootNode();
					Enumeration<DefaultMutableTreeNode> enumeration = root.children();
					boolean found = false;
					while(enumeration.hasMoreElements()) {
						DefaultMutableTreeNode child = enumeration.nextElement();
						String childClassName = (String)child.getUserObject();
						if(castingType.resolveBinding().getQualifiedName().equals(childClassName)) {
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
	
	private Type isFirstStatementACastingVariableDeclaration(ArrayList<Statement> typeCheckCodeFragment) {
		Statement firstStatement = null;
		if(typeCheckCodeFragment.get(0) instanceof Block) {
			Block block = (Block)typeCheckCodeFragment.get(0);
			List<Statement> blockStatements = block.statements();
			firstStatement = blockStatements.get(0);
		}
		else {
			firstStatement = typeCheckCodeFragment.get(0);
		}
		if(firstStatement instanceof VariableDeclarationStatement) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)firstStatement;
			List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
			if(fragments.size() == 1) {
				VariableDeclarationFragment fragment = fragments.get(0);
				if(fragment.getInitializer() instanceof CastExpression)
					return variableDeclarationStatement.getType();
			}
		}
		return null;
	}
	
	public Set<ITypeBinding> getThrownExceptions() {
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		StatementExtractor statementExtractor = new StatementExtractor();
		Set<ITypeBinding> thrownExceptions = new LinkedHashSet<ITypeBinding>();
		for(Expression key : typeCheckMap.keySet()) {
			ArrayList<Statement> statements = typeCheckMap.get(key);
			for(Statement typeCheckStatement : statements) {
				List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(typeCheckStatement);
				List<Expression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(typeCheckStatement);
				List<Statement> tryStatements = statementExtractor.getTryStatements(typeCheckStatement);
				Set<ITypeBinding> catchClauseExceptions = new LinkedHashSet<ITypeBinding>();
				for(Statement statement : tryStatements) {
					TryStatement tryStatement = (TryStatement)statement;
					List<CatchClause> catchClauses = tryStatement.catchClauses();
					for(CatchClause catchClause : catchClauses) {
						SingleVariableDeclaration exception = catchClause.getException();
						Type exceptionType = exception.getType();
						catchClauseExceptions.add(exceptionType.resolveBinding());
					}
				}
				for(Expression expression : methodInvocations) {
					if(expression instanceof MethodInvocation) {
						MethodInvocation methodInvocation = (MethodInvocation)expression;
						IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
						ITypeBinding[] typeBindings = methodBinding.getExceptionTypes();
						for(ITypeBinding typeBinding : typeBindings) {
							if(!catchClauseExceptions.contains(typeBinding))
								thrownExceptions.add(typeBinding);
						}
					}
				}
				for(Expression expression : classInstanceCreations) {
					ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)expression;
					IMethodBinding methodBinding = classInstanceCreation.resolveConstructorBinding();
					ITypeBinding[] typeBindings = methodBinding.getExceptionTypes();
					for(ITypeBinding typeBinding : typeBindings) {
						if(!catchClauseExceptions.contains(typeBinding))
							thrownExceptions.add(typeBinding);
					}
				}
			}
		}
		return thrownExceptions;
	}
}
