package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.inheritance.InheritanceTree;
import gr.uom.java.ast.util.ExpressionExtractor;
import gr.uom.java.ast.util.MethodDeclarationUtility;
import gr.uom.java.ast.util.StatementExtractor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class TypeCheckElimination {
	private Map<Expression, ArrayList<Statement>> typeCheckMap;
	private ArrayList<Statement> defaultCaseStatements;
	private Map<Expression, List<SimpleName>> staticFieldMap;
	private Map<Expression, List<Type>> subclassTypeMap;
	private VariableDeclarationFragment typeField;
	private MethodDeclaration typeFieldGetterMethod;
	private MethodDeclaration typeFieldSetterMethod;
	private Statement typeCheckCodeFragment;
	private MethodDeclaration typeCheckMethod;
	private TypeDeclaration typeCheckClass;
	private LinkedHashSet<SimpleName> additionalStaticFields;
	private LinkedHashSet<VariableDeclarationFragment> accessedFields;
	private LinkedHashSet<VariableDeclarationFragment> assignedFields;
	private LinkedHashSet<IVariableBinding> superAccessedFields;
	private LinkedHashSet<IVariableBinding> superAssignedFields;
	private LinkedHashSet<SingleVariableDeclaration> accessedParameters;
	private LinkedHashSet<SingleVariableDeclaration> assignedParameters;
	private LinkedHashSet<VariableDeclaration> accessedLocalVariables;
	private LinkedHashSet<VariableDeclaration> assignedLocalVariables;
	private LinkedHashSet<MethodDeclaration> accessedMethods;
	private LinkedHashSet<IMethodBinding> superAccessedMethods;
	private VariableDeclaration typeLocalVariable;
	private MethodInvocation typeMethodInvocation;
	private VariableDeclarationFragment foreignTypeField;
	private InheritanceTree existingInheritanceTree;
	private InheritanceTree inheritanceTreeMatchingWithStaticTypes;
	private Map<Expression, DefaultMutableTreeNode> remainingIfStatementExpressionMap;
	private String abstractMethodName;
	private volatile int hashCode = 0;
	
	public TypeCheckElimination() {
		this.typeCheckMap = new LinkedHashMap<Expression, ArrayList<Statement>>();
		this.defaultCaseStatements = new ArrayList<Statement>();
		this.staticFieldMap = new LinkedHashMap<Expression, List<SimpleName>>();
		this.subclassTypeMap = new LinkedHashMap<Expression, List<Type>>();
		this.typeField = null;
		this.typeFieldGetterMethod = null;
		this.typeFieldSetterMethod = null;
		this.typeCheckCodeFragment = null;
		this.typeCheckMethod = null;
		this.typeCheckClass = null;
		this.additionalStaticFields = new LinkedHashSet<SimpleName>();
		this.accessedFields = new LinkedHashSet<VariableDeclarationFragment>();
		this.assignedFields = new LinkedHashSet<VariableDeclarationFragment>();
		this.superAccessedFields = new LinkedHashSet<IVariableBinding>();
		this.superAssignedFields = new LinkedHashSet<IVariableBinding>();
		this.accessedParameters = new LinkedHashSet<SingleVariableDeclaration>();
		this.assignedParameters = new LinkedHashSet<SingleVariableDeclaration>();
		this.accessedLocalVariables = new LinkedHashSet<VariableDeclaration>();
		this.assignedLocalVariables = new LinkedHashSet<VariableDeclaration>();
		this.accessedMethods = new LinkedHashSet<MethodDeclaration>();
		this.superAccessedMethods = new LinkedHashSet<IMethodBinding>();
		this.typeLocalVariable = null;
		this.typeMethodInvocation = null;
		this.foreignTypeField = null;
		this.existingInheritanceTree = null;
		this.inheritanceTreeMatchingWithStaticTypes = null;
		this.remainingIfStatementExpressionMap = new LinkedHashMap<Expression, DefaultMutableTreeNode>();
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
	
	public void addEmptyTypeCheck(Expression expression) {
		if(!typeCheckMap.containsKey(expression)) {
			ArrayList<Statement> statements = new ArrayList<Statement>();
			typeCheckMap.put(expression, statements);
		}
	}
	
	public boolean containsTypeCheckExpression(Expression expression) {
		return typeCheckMap.containsKey(expression);
	}
	
	public void addDefaultCaseStatement(Statement statement) {
		defaultCaseStatements.add(statement);
	}
	
	public void addStaticType(Expression expression, List<SimpleName> simpleNameGroup) {
		staticFieldMap.put(expression, simpleNameGroup);
	}
	
	public void addSubclassType(Expression expression, List<Type> subclassTypeGroup) {
		subclassTypeMap.put(expression, subclassTypeGroup);
	}
	
	public void addRemainingIfStatementExpression(Expression expression, DefaultMutableTreeNode root) {
		remainingIfStatementExpressionMap.put(expression, root);
	}
	
	public void addAdditionalStaticField(SimpleName staticField) {
		additionalStaticFields.add(staticField);
	}
	
	public void addAccessedField(VariableDeclarationFragment fragment) {
		accessedFields.add(fragment);
	}
	
	public void addAssignedField(VariableDeclarationFragment fragment) {
		assignedFields.add(fragment);
	}

	public void addSuperAccessedField(IVariableBinding variableBinding) {
		superAccessedFields.add(variableBinding);
	}
	
	public void addSuperAssignedField(IVariableBinding variableBinding) {
		superAssignedFields.add(variableBinding);
	}
	
	public void addAccessedLocalVariable(VariableDeclaration fragment) {
		accessedLocalVariables.add(fragment);
	}

	public void addAssignedLocalVariable(VariableDeclaration fragment) {
		assignedLocalVariables.add(fragment);
	}
	
	public void addAccessedParameter(SingleVariableDeclaration parameter) {
		accessedParameters.add(parameter);
	}
	
	public void addAssignedParameter(SingleVariableDeclaration parameter) {
		assignedParameters.add(parameter);
	}
	
	public void addAccessedMethod(MethodDeclaration method) {
		accessedMethods.add(method);
	}

	public void addSuperAccessedMethod(IMethodBinding method) {
		superAccessedMethods.add(method);
	}
	
	public LinkedHashSet<VariableDeclaration> getAccessedLocalVariables() {
		return accessedLocalVariables;
	}

	public Set<VariableDeclarationFragment> getAccessedFields() {
		return accessedFields;
	}
	
	public Set<VariableDeclarationFragment> getAssignedFields() {
		return assignedFields;
	}
	
	public Set<SingleVariableDeclaration> getAccessedParameters() {
		return accessedParameters;
	}
	
	public Set<MethodDeclaration> getAccessedMethods() {
		return accessedMethods;
	}

	public Set<IMethodBinding> getSuperAccessedMethods() {
		return superAccessedMethods;
	}
	
	public Set<Expression> getTypeCheckExpressions() {
		return typeCheckMap.keySet();
	}
	
	public ArrayList<Statement> getTypeCheckStatements(Expression expression) {
		return typeCheckMap.get(expression);
	}
	
	public List<ArrayList<Statement>> getTypeCheckStatements() {
		return new ArrayList<ArrayList<Statement>>(typeCheckMap.values());
	}
	
	public ArrayList<Statement> getDefaultCaseStatements() {
		return defaultCaseStatements;
	}
	
	public List<SimpleName> getStaticFields(Expression expression) {
		return staticFieldMap.get(expression);
	}
	
	public List<SimpleName> getStaticFields() {
		ArrayList<SimpleName> staticFields = new ArrayList<SimpleName>();
		for(Expression expression : typeCheckMap.keySet()) {
			List<SimpleName> simpleNameGroup = staticFieldMap.get(expression);
			if(simpleNameGroup != null) {
				for(SimpleName simpleName : simpleNameGroup)
					staticFields.add(simpleName);
			}
		}
		return staticFields;
	}
	
	public Set<SimpleName> getAdditionalStaticFields() {
		return additionalStaticFields;
	}
	
	public DefaultMutableTreeNode getRemainingIfStatementExpression(Expression expression) {
		return remainingIfStatementExpressionMap.get(expression);
	}
	
	public Expression getExpressionCorrespondingToTypeCheckStatementList(ArrayList<Statement> statements) {
		for(Expression expression : typeCheckMap.keySet()) {
			if(statements.equals(typeCheckMap.get(expression)))
				return expression;
		}
		return null;
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
		this.abstractMethodName = typeCheckMethod.getName().getIdentifier();
	}

	public TypeDeclaration getTypeCheckClass() {
		return typeCheckClass;
	}

	public void setTypeCheckClass(TypeDeclaration typeCheckClass) {
		this.typeCheckClass = typeCheckClass;
	}

	public VariableDeclaration getTypeLocalVariable() {
		return typeLocalVariable;
	}

	public void setTypeLocalVariable(VariableDeclaration typeLocalVariable) {
		this.typeLocalVariable = typeLocalVariable;
	}

	public MethodInvocation getTypeMethodInvocation() {
		return typeMethodInvocation;
	}

	public void setTypeMethodInvocation(MethodInvocation typeMethodInvocation) {
		this.typeMethodInvocation = typeMethodInvocation;
	}

	public VariableDeclarationFragment getForeignTypeField() {
		return foreignTypeField;
	}

	public void setForeignTypeField(VariableDeclarationFragment foreignTypeField) {
		this.foreignTypeField = foreignTypeField;
	}

	public InheritanceTree getExistingInheritanceTree() {
		return existingInheritanceTree;
	}

	public void setExistingInheritanceTree(InheritanceTree existingInheritanceTree) {
		this.existingInheritanceTree = existingInheritanceTree;
	}

	public InheritanceTree getInheritanceTreeMatchingWithStaticTypes() {
		return inheritanceTreeMatchingWithStaticTypes;
	}

	public void setInheritanceTreeMatchingWithStaticTypes(InheritanceTree inheritanceTree) {
		this.inheritanceTreeMatchingWithStaticTypes = inheritanceTree;
	}

	public boolean allTypeCheckingsContainStaticFieldOrSubclassType() {
		return (typeCheckMap.keySet().size() > 1 || (typeCheckMap.keySet().size() == 1 && !defaultCaseStatements.isEmpty())) && 
			(typeCheckMap.keySet().size() == (staticFieldMap.keySet().size() + subclassTypeMap.keySet().size()));
	}
	
	public boolean isApplicable() {
		if(!containsLocalVariableAssignment() && !containsBranchingStatement() && !containsSuperMethodInvocation())
			return true;
		else
			return false;
	}
	
	private boolean containsLocalVariableAssignment() {
		VariableDeclaration returnedVariableDeclaration = getTypeCheckMethodReturnedVariable();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		StatementExtractor statementExtractor = new StatementExtractor();
		List<ArrayList<Statement>> allTypeCheckStatements = getTypeCheckStatements();
		if(!getDefaultCaseStatements().isEmpty()) {
			allTypeCheckStatements.add(getDefaultCaseStatements());
		}
		for(ArrayList<Statement> typeCheckStatementList : allTypeCheckStatements) {
			List<Statement> variableDeclarationsInsideBranch = new ArrayList<Statement>();
			for(Statement statement : typeCheckStatementList) {
				variableDeclarationsInsideBranch.addAll(statementExtractor.getVariableDeclarations(statement));
			}
			for(Statement statement : typeCheckStatementList) {
				List<Expression> assignments = expressionExtractor.getAssignments(statement);
				for(Expression expression : assignments) {
					Assignment assignment = (Assignment)expression;
					Expression leftHandSide = assignment.getLeftHandSide();
					SimpleName leftHandSideName = null;
					if(leftHandSide instanceof SimpleName) {
						leftHandSideName = (SimpleName)leftHandSide;
					}
					else if(leftHandSide instanceof QualifiedName) {
						QualifiedName leftHandSideQualifiedName = (QualifiedName)leftHandSide;
						leftHandSideName = leftHandSideQualifiedName.getName();
					}
					else if(leftHandSide instanceof FieldAccess) {
						FieldAccess leftHandSideFieldAccess = (FieldAccess)leftHandSide;
						leftHandSideName = leftHandSideFieldAccess.getName();
					}
					if(leftHandSideName != null) {
						IBinding leftHandSideBinding = leftHandSideName.resolveBinding();
						if(leftHandSideBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding leftHandSideVariableBinding = (IVariableBinding)leftHandSideBinding;
							if(!leftHandSideVariableBinding.isField()) {
								boolean variableIsDeclaredInsideBranch = false;
								for(Statement vDStatement : variableDeclarationsInsideBranch) {
									VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)vDStatement;
									List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
									for(VariableDeclarationFragment fragment : fragments) {
										IVariableBinding fragmentVariableBinding = fragment.resolveBinding();
										if(fragmentVariableBinding.isEqualTo(leftHandSideVariableBinding)) {
											variableIsDeclaredInsideBranch = true;
											break;
										}
									}
									if(variableIsDeclaredInsideBranch)
										break;
								}
								if(!variableIsDeclaredInsideBranch) {
									if(returnedVariableDeclaration == null) {
										return true;
									}
									else if(!returnedVariableDeclaration.resolveBinding().isEqualTo(leftHandSideVariableBinding)) {
										return true;
									}
								}
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	private boolean containsBranchingStatement() {
		List<Statement> statementList = new ArrayList<Statement>();
		StatementExtractor statementExtractor = new StatementExtractor();
		List<ArrayList<Statement>> typeCheckStatements = getTypeCheckStatements();
		if(!defaultCaseStatements.isEmpty())
			typeCheckStatements.add(defaultCaseStatements);
		for(ArrayList<Statement> statements : typeCheckStatements) {
			for(Statement statement : statements) {
				if(statement.getNodeType() != ASTNode.SWITCH_STATEMENT &&
						statement.getNodeType() != ASTNode.WHILE_STATEMENT &&
						statement.getNodeType() != ASTNode.FOR_STATEMENT &&
						statement.getNodeType() != ASTNode.DO_STATEMENT &&
						statement.getNodeType() != ASTNode.ENHANCED_FOR_STATEMENT) {
		    		statementList.addAll(statementExtractor.getBreakStatements(statement));
		    		statementList.addAll(statementExtractor.getContinueStatements(statement));
				}
			}
		}
		if(!statementList.isEmpty())
			return true;
		else
			return false;
	}
	
	private boolean containsSuperMethodInvocation() {
		List<ArrayList<Statement>> typeCheckStatements = getTypeCheckStatements();
		if(!defaultCaseStatements.isEmpty())
			typeCheckStatements.add(defaultCaseStatements);
		
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		for(ArrayList<Statement> statements : typeCheckStatements) {
			for(Statement statement : statements) {
				List<Expression> superMethodInvocations = expressionExtractor.getSuperMethodInvocations(statement);
				if(!superMethodInvocations.isEmpty())
					return true;
			}
		}
		return false;
	}
	
	public Type getTypeCheckMethodReturnType() {
		return typeCheckMethod.getReturnType2();
	}
	
	public List<SingleVariableDeclaration> getTypeCheckMethodParameters() {
		return typeCheckMethod.parameters();
	}
	
	public VariableDeclaration getTypeCheckMethodReturnedVariable() {
		StatementExtractor statementExtractor = new StatementExtractor();
		List<Statement> typeCheckCodeFragmentReturnStatements = statementExtractor.getReturnStatements(typeCheckCodeFragment);
		if(!typeCheckCodeFragmentReturnStatements.isEmpty()) {
			ReturnStatement firstReturnStatement = (ReturnStatement)typeCheckCodeFragmentReturnStatements.get(0);
			if(firstReturnStatement.getExpression() instanceof SimpleName) {
				SimpleName returnExpression = (SimpleName)firstReturnStatement.getExpression();
				List<SingleVariableDeclaration> parameters = typeCheckMethod.parameters();
				for(SingleVariableDeclaration parameter : parameters) {
					if(parameter.resolveBinding().isEqualTo(returnExpression.resolveBinding()))
						return parameter;
				}
				List<Statement> variableDeclarationStatements = statementExtractor.getVariableDeclarations(typeCheckMethod.getBody());
				for(Statement statement : variableDeclarationStatements) {
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
					List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
					for(VariableDeclarationFragment fragment : fragments) {
						if(fragment.resolveBinding().isEqualTo(returnExpression.resolveBinding()))
							return fragment;
					}
				}
			}
		}
		else {
			List<Statement> allReturnStatements = statementExtractor.getReturnStatements(typeCheckMethod.getBody());
			if(!allReturnStatements.isEmpty()) {
				ReturnStatement lastReturnStatement = (ReturnStatement)allReturnStatements.get(allReturnStatements.size()-1);
				if(lastReturnStatement.getExpression() instanceof SimpleName) {
					SimpleName returnExpression = (SimpleName)lastReturnStatement.getExpression();
					for(SingleVariableDeclaration assignedParameter : assignedParameters) {
						if(assignedParameter.resolveBinding().isEqualTo(returnExpression.resolveBinding()))
							return assignedParameter;
					}
					for(VariableDeclaration assignedLocalVariable : assignedLocalVariables) {
						if(assignedLocalVariable.resolveBinding().isEqualTo(returnExpression.resolveBinding()))
							return assignedLocalVariable;
					}
				}
			}
		}
		return null;
	}
	
	public String getAbstractClassName() {
		if(typeField != null && existingInheritanceTree == null && inheritanceTreeMatchingWithStaticTypes == null) {
			String typeFieldName = typeField.getName().getIdentifier().replaceAll("_", "");
			return typeFieldName.substring(0, 1).toUpperCase() + typeFieldName.substring(1, typeFieldName.length());
		}
		else if(typeLocalVariable != null && existingInheritanceTree == null && inheritanceTreeMatchingWithStaticTypes == null) {
			String typeLocalVariableName = typeLocalVariable.getName().getIdentifier().replaceAll("_", "");
			return typeLocalVariableName.substring(0, 1).toUpperCase() + typeLocalVariableName.substring(1, typeLocalVariableName.length());
		}
		else if(foreignTypeField != null && existingInheritanceTree == null && inheritanceTreeMatchingWithStaticTypes == null) {
			String foreignTypeFieldName = foreignTypeField.getName().getIdentifier().replaceAll("_", "");
			return foreignTypeFieldName.substring(0, 1).toUpperCase() + foreignTypeFieldName.substring(1, foreignTypeFieldName.length());
		}
		else if(existingInheritanceTree != null) {
			DefaultMutableTreeNode root = existingInheritanceTree.getRootNode();
			return (String)root.getUserObject();
		}
		else if(inheritanceTreeMatchingWithStaticTypes != null) {
			DefaultMutableTreeNode root = inheritanceTreeMatchingWithStaticTypes.getRootNode();
			String rootClassName = (String)root.getUserObject();
			if(rootClassName.contains("."))
				return rootClassName.substring(rootClassName.lastIndexOf(".")+1,rootClassName.length());
			else
				return rootClassName;
		}
		return null;
	}
	
	public List<String> getSubclassNames() {
		List<String> subclassNames = new ArrayList<String>();
		for(Expression expression : typeCheckMap.keySet()) {
			List<SimpleName> simpleNameGroup = staticFieldMap.get(expression);
			if(simpleNameGroup != null) {
				for(SimpleName simpleName : simpleNameGroup) {
					String staticFieldName = simpleName.getIdentifier();
					Type castingType = getCastingType(typeCheckMap.get(expression));
					String subclassName = null;
					if(!staticFieldName.contains("_")) {
						subclassName = staticFieldName.substring(0, 1).toUpperCase() + 
						staticFieldName.substring(1, staticFieldName.length()).toLowerCase();
					}
					else {
						subclassName = "";
						StringTokenizer tokenizer = new StringTokenizer(staticFieldName,"_");
						while(tokenizer.hasMoreTokens()) {
							String tempName = tokenizer.nextToken().toLowerCase().toString();
							subclassName += tempName.subSequence(0, 1).toString().toUpperCase() + 
							tempName.subSequence(1, tempName.length()).toString();
						}
					}
					if(existingInheritanceTree != null) {
						DefaultMutableTreeNode root = existingInheritanceTree.getRootNode();
						DefaultMutableTreeNode leaf = root.getFirstLeaf();
						while(leaf != null) {
							String childClassName = (String)leaf.getUserObject();
							if(childClassName.endsWith(subclassName)) {
								subclassNames.add(childClassName);
								break;
							}
							else if(castingType != null && castingType.resolveBinding().getQualifiedName().equals(childClassName)) {
								subclassNames.add(childClassName);
								break;
							}
							leaf = leaf.getNextLeaf();
						}
					}
					else if(castingType != null) {
						subclassNames.add(castingType.resolveBinding().getQualifiedName());
					}
					else {
						subclassNames.add(subclassName);
					}
				}
			}
			List<Type> typeGroup = subclassTypeMap.get(expression);
			if(typeGroup != null) {
				for(Type type : typeGroup)
					subclassNames.add(type.resolveBinding().getQualifiedName());
			}
		}
		return subclassNames;
	}
	
	private Type getCastingType(ArrayList<Statement> typeCheckCodeFragment) {
		List<Expression> castExpressions = new ArrayList<Expression>();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		for(Statement statement : typeCheckCodeFragment) {
			castExpressions.addAll(expressionExtractor.getCastExpressions(statement));
		}
		for(Expression expression : castExpressions) {
			CastExpression castExpression = (CastExpression)expression;
			Expression expressionOfCastExpression = castExpression.getExpression();
			SimpleName superTypeSimpleName = null;
			if(expressionOfCastExpression instanceof SimpleName) {
				superTypeSimpleName = (SimpleName)expressionOfCastExpression;
			}
			else if(expressionOfCastExpression instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)expressionOfCastExpression;
				superTypeSimpleName = fieldAccess.getName();
			}
			else if(expressionOfCastExpression instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)expressionOfCastExpression;
				if(typeFieldGetterMethod != null && typeFieldGetterMethod.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
					superTypeSimpleName = MethodDeclarationUtility.isGetter(typeFieldGetterMethod);
				}
			}
			if(superTypeSimpleName != null) {
				if(typeField != null) {
					if(typeField.resolveBinding().isEqualTo(superTypeSimpleName.resolveBinding()))
						return castExpression.getType();
				}
				else if(typeLocalVariable != null) {
					if(typeLocalVariable.resolveBinding().isEqualTo(superTypeSimpleName.resolveBinding()))
						return castExpression.getType();
				}
				else if(typeMethodInvocation != null) {
					Expression typeMethodInvocationExpression = typeMethodInvocation.getExpression();
					SimpleName invoker = null;
					if(typeMethodInvocationExpression instanceof SimpleName) {
						invoker = (SimpleName)typeMethodInvocationExpression;
					}
					else if(typeMethodInvocationExpression instanceof FieldAccess) {
						FieldAccess fieldAccess = (FieldAccess)typeMethodInvocationExpression;
						invoker = fieldAccess.getName();
					}
					if(invoker != null && invoker.resolveBinding().isEqualTo(superTypeSimpleName.resolveBinding()))
						return castExpression.getType();
				}
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
	
	public boolean allTypeCheckBranchesAreEmpty() {
		for(Expression key : typeCheckMap.keySet()) {
			ArrayList<Statement> statements = typeCheckMap.get(key);
			if(!statements.isEmpty())
				return false;
		}
		return true;
	}
	
	public boolean isTypeCheckMethodStateSetter() {
		InheritanceTree tree = null;
		if(existingInheritanceTree != null)
			tree = existingInheritanceTree;
		else if(inheritanceTreeMatchingWithStaticTypes != null)
			tree = inheritanceTreeMatchingWithStaticTypes;
		if(tree != null) {
			DefaultMutableTreeNode root = tree.getRootNode();
			DefaultMutableTreeNode leaf = root.getFirstLeaf();
			List<String> subclassNames = new ArrayList<String>();
			while(leaf != null) {
				subclassNames.add((String)leaf.getUserObject());
				leaf = leaf.getNextLeaf();
			}
			Block typeCheckMethodBody = typeCheckMethod.getBody();
			List<Statement> statements = typeCheckMethodBody.statements();
			if(statements.size() > 0 && statements.get(0) instanceof SwitchStatement) {
				SwitchStatement switchStatement = (SwitchStatement)statements.get(0);
				List<Statement> statements2 = switchStatement.statements();
				ExpressionExtractor expressionExtractor = new ExpressionExtractor();
				int matchCounter = 0;
				for(Statement statement2 : statements2) {
					if(!(statement2 instanceof SwitchCase) && !(statement2 instanceof BreakStatement)) {
						List<Expression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(statement2);
						if(classInstanceCreations.size() == 1) {
							ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)classInstanceCreations.get(0);
							Type classInstanceCreationType = classInstanceCreation.getType();
							if(subclassNames.contains(classInstanceCreationType.resolveBinding().getQualifiedName())) {
								matchCounter++;
							}
						}
					}
				}
				if(matchCounter == subclassNames.size())
					return true;
			}
		}
		return false;
	}
	
	public boolean typeCheckCodeFragmentContainsReturnStatement() {
		StatementExtractor statementExtractor = new StatementExtractor();
		List<Statement> typeCheckCodeFragmentReturnStatements = statementExtractor.getReturnStatements(typeCheckCodeFragment);
		if(typeCheckCodeFragmentReturnStatements.isEmpty())
			return false;
		else
			return true;
	}
	
	public String getAbstractMethodName() {
		return abstractMethodName;
	}

	public void setAbstractMethodName(String methodName) {
		abstractMethodName = methodName;
	}
	
	public boolean equals(Object o) {
		if(this == o) {
            return true;
        }
		
		if(o instanceof TypeCheckElimination) {
			TypeCheckElimination typeCheckElimination = (TypeCheckElimination)o;
			return this.typeCheckClass.equals(typeCheckElimination.typeCheckClass) &&
				this.typeCheckMethod.equals(typeCheckElimination.typeCheckMethod) &&
				this.typeCheckCodeFragment.equals(typeCheckElimination.typeCheckCodeFragment);
		}
		return false;
	}
	
	public int hashCode() {
		if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + typeCheckClass.hashCode();
    		result = 37*result + typeCheckMethod.hashCode();
    		result = 37*result + typeCheckCodeFragment.hashCode();
    		hashCode = result;
    	}
    	return hashCode;
	}
	
	public String toString() {
		return typeCheckClass.resolveBinding().getQualifiedName() + "::" +
			typeCheckMethod.resolveBinding().toString();
	}
}
