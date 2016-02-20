package gr.uom.java.jdeodorant.refactoring.manipulators;

import gr.uom.java.ast.decomposition.CompositeStatementObject;
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

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
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
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class TypeCheckElimination implements Comparable<TypeCheckElimination> {
	private Map<Expression, ArrayList<Statement>> typeCheckMap;
	private ArrayList<Statement> defaultCaseStatements;
	private Map<Expression, List<SimpleName>> staticFieldMap;
	private Map<Expression, List<Type>> subclassTypeMap;
	private VariableDeclarationFragment typeField;
	private MethodDeclaration typeFieldGetterMethod;
	private MethodDeclaration typeFieldSetterMethod;
	private Statement typeCheckCodeFragment;
	private CompositeStatementObject typeCheckCompositeStatement;
	private MethodDeclaration typeCheckMethod;
	private TypeDeclaration typeCheckClass;
	private IFile typeCheckIFile;
	private LinkedHashSet<SimpleName> additionalStaticFields;
	private LinkedHashSet<VariableDeclarationFragment> accessedFields;
	private LinkedHashSet<VariableDeclarationFragment> assignedFields;
	private LinkedHashMap<VariableDeclarationFragment, MethodDeclaration> superAccessedFieldMap;
	private LinkedHashMap<IVariableBinding, IMethodBinding> superAccessedFieldBindingMap;
	private LinkedHashMap<VariableDeclarationFragment, MethodDeclaration> superAssignedFieldMap;
	private LinkedHashMap<IVariableBinding, IMethodBinding> superAssignedFieldBindingMap;
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
	private Map<SimpleName, String> staticFieldSubclassTypeMap;
	private Map<Expression, DefaultMutableTreeNode> remainingIfStatementExpressionMap;
	private String abstractMethodName;
	private volatile int hashCode = 0;
	private int groupSizeAtClassLevel;
	private double averageNumberOfStatements;
	private Integer userRate;
	
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
		this.superAccessedFieldMap = new LinkedHashMap<VariableDeclarationFragment, MethodDeclaration>();
		this.superAccessedFieldBindingMap = new LinkedHashMap<IVariableBinding, IMethodBinding>();
		this.superAssignedFieldMap = new LinkedHashMap<VariableDeclarationFragment, MethodDeclaration>();
		this.superAssignedFieldBindingMap = new LinkedHashMap<IVariableBinding, IMethodBinding>();
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
		this.staticFieldSubclassTypeMap = new LinkedHashMap<SimpleName, String>();
		this.remainingIfStatementExpressionMap = new LinkedHashMap<Expression, DefaultMutableTreeNode>();
		this.abstractMethodName = null;
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
	
	public void addSuperAccessedField(VariableDeclarationFragment fragment, MethodDeclaration method) {
		superAccessedFieldMap.put(fragment, method);
	}
	
	public void addSuperAccessedFieldBinding(IVariableBinding variableBinding, IMethodBinding methodBinding) {
		superAccessedFieldBindingMap.put(variableBinding, methodBinding);
	}
	
	public IMethodBinding getGetterMethodBindingOfSuperAccessedField(IVariableBinding variableBinding) {
		return superAccessedFieldBindingMap.get(variableBinding);
	}

	public Set<VariableDeclarationFragment> getSuperAccessedFields() {
		return superAccessedFieldMap.keySet();
	}
	
	public Set<IVariableBinding> getSuperAccessedFieldBindings() {
		return superAccessedFieldBindingMap.keySet();
	}
	
	public void addSuperAssignedField(VariableDeclarationFragment fragment, MethodDeclaration method) {
		superAssignedFieldMap.put(fragment, method);
	}
	
	public void addSuperAssignedFieldBinding(IVariableBinding variableBinding, IMethodBinding methodBinding) {
		superAssignedFieldBindingMap.put(variableBinding, methodBinding);
	}

	public IMethodBinding getSetterMethodBindingOfSuperAssignedField(IVariableBinding variableBinding) {
		return superAssignedFieldBindingMap.get(variableBinding);
	}

	public Set<VariableDeclarationFragment> getSuperAssignedFields() {
		return superAssignedFieldMap.keySet();
	}
	
	public Set<IVariableBinding> getSuperAssignedFieldBindings() {
		return superAssignedFieldBindingMap.keySet();
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

	public CompositeStatementObject getTypeCheckCompositeStatement() {
		return typeCheckCompositeStatement;
	}

	public void setTypeCheckCompositeStatement(CompositeStatementObject typeCheckCompositeStatement) {
		this.typeCheckCompositeStatement = typeCheckCompositeStatement;
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

	public IFile getTypeCheckIFile() {
		return typeCheckIFile;
	}

	public void setTypeCheckIFile(IFile typeCheckIFile) {
		this.typeCheckIFile = typeCheckIFile;
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

	public void putStaticFieldSubclassTypeMapping(SimpleName staticField, String subclassType) {
		staticFieldSubclassTypeMap.put(staticField, subclassType);
	}

	public boolean allTypeCheckingsContainStaticFieldOrSubclassType() {
		return (typeCheckMap.keySet().size() > 1 || (typeCheckMap.keySet().size() == 1 && !defaultCaseStatements.isEmpty())) && 
			(typeCheckMap.keySet().size() == (staticFieldMap.keySet().size() + subclassTypeMap.keySet().size()));
	}
	
	public boolean isApplicable() {
		if(!containsLocalVariableAssignment() && !containsBranchingStatement() && !containsSuperMethodInvocation() && !containsSuperFieldAccess() &&
				!isSubclassTypeAnInterface() && !returnStatementAfterTypeCheckCodeFragment())
			return true;
		else
			return false;
	}
	
	private boolean isSubclassTypeAnInterface() {
		for(List<Type> subTypes : subclassTypeMap.values()) {
			for(Type subType : subTypes) {
				if(subType.resolveBinding().isInterface())
					return true;
			}
		}
		return false;
	}
	
	private boolean returnStatementAfterTypeCheckCodeFragment() {
		//check if the type-check code fragment contains return statements having an expression
		StatementExtractor statementExtractor = new StatementExtractor();
		List<Statement> allReturnStatementsWithinTypeCheckCodeFragment = statementExtractor.getReturnStatements(typeCheckCodeFragment);
		List<ReturnStatement> returnStatementsHavingExpressionWithinTypeCheckCodeFragment = new ArrayList<ReturnStatement>();
		for(Statement statement : allReturnStatementsWithinTypeCheckCodeFragment) {
			ReturnStatement returnStatement = (ReturnStatement)statement;
			if(returnStatement.getExpression() != null)
				returnStatementsHavingExpressionWithinTypeCheckCodeFragment.add(returnStatement);
		}
		if(returnStatementsHavingExpressionWithinTypeCheckCodeFragment.isEmpty())
			return false;
		//get all return statements having an expression within method body
		List<Statement> allReturnStatementsWithinTypeCheckMethod = statementExtractor.getReturnStatements(typeCheckMethod.getBody());
		List<ReturnStatement> returnStatementsHavingExpressionWithinTypeCheckMethod = new ArrayList<ReturnStatement>();
		for(Statement statement : allReturnStatementsWithinTypeCheckMethod) {
			ReturnStatement returnStatement = (ReturnStatement)statement;
			if(returnStatement.getExpression() != null)
				returnStatementsHavingExpressionWithinTypeCheckMethod.add(returnStatement);
		}
		List<ReturnStatement> returnStatementsHavingExpressionOutsideTypeCheckMethod = new ArrayList<ReturnStatement>();
		returnStatementsHavingExpressionOutsideTypeCheckMethod.addAll(returnStatementsHavingExpressionWithinTypeCheckMethod);
		returnStatementsHavingExpressionOutsideTypeCheckMethod.removeAll(returnStatementsHavingExpressionWithinTypeCheckCodeFragment);
		for(ReturnStatement returnStatement : returnStatementsHavingExpressionOutsideTypeCheckMethod) {
			if(returnStatement.getStartPosition() > typeCheckCodeFragment.getStartPosition()+typeCheckCodeFragment.getLength())
				return true;
		}
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
			List<VariableDeclarationFragment> variableDeclarationFragmentsInsideBranch = new ArrayList<VariableDeclarationFragment>();
			for(Statement statement : typeCheckStatementList) {
				List<Statement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(statement);
				for(Statement statement2 : variableDeclarationStatements) {
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement2;
					List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
					variableDeclarationFragmentsInsideBranch.addAll(fragments);
				}
				List<Expression> variableDeclarationExpressions = expressionExtractor.getVariableDeclarationExpressions(statement);
				for(Expression expression : variableDeclarationExpressions) {
					VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)expression;
					List<VariableDeclarationFragment> fragments = variableDeclarationExpression.fragments();
					variableDeclarationFragmentsInsideBranch.addAll(fragments);
				}
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
						if(leftHandSideBinding != null && leftHandSideBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding leftHandSideVariableBinding = (IVariableBinding)leftHandSideBinding;
							if(!leftHandSideVariableBinding.isField()) {
								boolean variableIsDeclaredInsideBranch = false;
								for(VariableDeclarationFragment fragment : variableDeclarationFragmentsInsideBranch) {
									IVariableBinding fragmentVariableBinding = fragment.resolveBinding();
									if(fragmentVariableBinding.isEqualTo(leftHandSideVariableBinding)) {
										variableIsDeclaredInsideBranch = true;
										break;
									}
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
				List<Expression> postfixExpressions = expressionExtractor.getPostfixExpressions(statement);
				for(Expression expression : postfixExpressions) {
					PostfixExpression postfix = (PostfixExpression)expression;
					Expression operand = postfix.getOperand();
					SimpleName operandName = null;
					if(operand instanceof SimpleName) {
						operandName = (SimpleName)operand;
					}
					else if(operand instanceof QualifiedName) {
						QualifiedName qualifiedName = (QualifiedName)operand;
						operandName = qualifiedName.getName();
					}
					else if(operand instanceof FieldAccess) {
						FieldAccess fieldAccess = (FieldAccess)operand;
						operandName = fieldAccess.getName();
					}
					if(operandName != null) {
						IBinding operandBinding = operandName.resolveBinding();
						if(operandBinding != null && operandBinding.getKind() == IBinding.VARIABLE) {
							IVariableBinding operandVariableBinding = (IVariableBinding)operandBinding;
							if(!operandVariableBinding.isField()) {
								boolean variableIsDeclaredInsideBranch = false;
								for(VariableDeclarationFragment fragment : variableDeclarationFragmentsInsideBranch) {
									IVariableBinding fragmentVariableBinding = fragment.resolveBinding();
									if(fragmentVariableBinding.isEqualTo(operandVariableBinding)) {
										variableIsDeclaredInsideBranch = true;
										break;
									}
								}
								if(!variableIsDeclaredInsideBranch) {
									if(returnedVariableDeclaration == null) {
										return true;
									}
									else if(!returnedVariableDeclaration.resolveBinding().isEqualTo(operandVariableBinding)) {
										return true;
									}
								}
							}
						}
					}
				}
				List<Expression> prefixExpressions = expressionExtractor.getPrefixExpressions(statement);
				for(Expression expression : prefixExpressions) {
					PrefixExpression prefix = (PrefixExpression)expression;
					if(prefix.getOperator().equals(PrefixExpression.Operator.INCREMENT) ||
							prefix.getOperator().equals(PrefixExpression.Operator.DECREMENT)) {
						Expression operand = prefix.getOperand();
						SimpleName operandName = null;
						if(operand instanceof SimpleName) {
							operandName = (SimpleName)operand;
						}
						else if(operand instanceof QualifiedName) {
							QualifiedName qualifiedName = (QualifiedName)operand;
							operandName = qualifiedName.getName();
						}
						else if(operand instanceof FieldAccess) {
							FieldAccess fieldAccess = (FieldAccess)operand;
							operandName = fieldAccess.getName();
						}
						if(operandName != null) {
							IBinding operandBinding = operandName.resolveBinding();
							if(operandBinding != null && operandBinding.getKind() == IBinding.VARIABLE) {
								IVariableBinding operandVariableBinding = (IVariableBinding)operandBinding;
								if(!operandVariableBinding.isField()) {
									boolean variableIsDeclaredInsideBranch = false;
									for(VariableDeclarationFragment fragment : variableDeclarationFragmentsInsideBranch) {
										IVariableBinding fragmentVariableBinding = fragment.resolveBinding();
										if(fragmentVariableBinding.isEqualTo(operandVariableBinding)) {
											variableIsDeclaredInsideBranch = true;
											break;
										}
									}
									if(!variableIsDeclaredInsideBranch) {
										if(returnedVariableDeclaration == null) {
											return true;
										}
										else if(!returnedVariableDeclaration.resolveBinding().isEqualTo(operandVariableBinding)) {
											return true;
										}
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
				statementList.addAll(statementExtractor.getBreakStatements(statement));
				statementList.addAll(statementExtractor.getContinueStatements(statement));
				List<Statement> returnStatements = statementExtractor.getReturnStatements(statement);
				for(Statement statement2 : returnStatements) {
					ReturnStatement returnStatement = (ReturnStatement)statement2;
					if(returnStatement.getExpression() == null)
						statementList.add(returnStatement);
				}
				
				List<Statement> forStatements = statementExtractor.getForStatements(statement);
				for(Statement forStatement : forStatements) {
					statementList.removeAll(statementExtractor.getBreakStatements(forStatement));
		    		statementList.removeAll(statementExtractor.getContinueStatements(forStatement));
		    		statementList.removeAll(statementExtractor.getReturnStatements(forStatement));
				}
				List<Statement> whileStatements = statementExtractor.getWhileStatements(statement);
				for(Statement whileStatement : whileStatements) {
					statementList.removeAll(statementExtractor.getBreakStatements(whileStatement));
		    		statementList.removeAll(statementExtractor.getContinueStatements(whileStatement));
		    		statementList.removeAll(statementExtractor.getReturnStatements(whileStatement));
				}
				List<Statement> doStatements = statementExtractor.getDoStatements(statement);
				for(Statement doStatement : doStatements) {
					statementList.removeAll(statementExtractor.getBreakStatements(doStatement));
		    		statementList.removeAll(statementExtractor.getContinueStatements(doStatement));
		    		statementList.removeAll(statementExtractor.getReturnStatements(doStatement));
				}
				List<Statement> enchancedForStatements = statementExtractor.getEnhancedForStatements(statement);
				for(Statement enchancedForStatement : enchancedForStatements) {
					statementList.removeAll(statementExtractor.getBreakStatements(enchancedForStatement));
		    		statementList.removeAll(statementExtractor.getContinueStatements(enchancedForStatement));
		    		statementList.removeAll(statementExtractor.getReturnStatements(enchancedForStatement));
				}
				List<Statement> switchStatements = statementExtractor.getSwitchStatements(statement);
				for(Statement switchStatement : switchStatements) {
					statementList.removeAll(statementExtractor.getBreakStatements(switchStatement));
		    		statementList.removeAll(statementExtractor.getContinueStatements(switchStatement));
		    		statementList.removeAll(statementExtractor.getReturnStatements(switchStatement));
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

	private boolean containsSuperFieldAccess() {
		List<ArrayList<Statement>> typeCheckStatements = getTypeCheckStatements();
		if(!defaultCaseStatements.isEmpty())
			typeCheckStatements.add(defaultCaseStatements);
		
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		for(ArrayList<Statement> statements : typeCheckStatements) {
			for(Statement statement : statements) {
				List<Expression> superFieldAccesses = expressionExtractor.getSuperFieldAccesses(statement);
				if(!superFieldAccesses.isEmpty())
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
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
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
				List<Statement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(typeCheckMethod.getBody());
				for(Statement statement : variableDeclarationStatements) {
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
					List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
					for(VariableDeclarationFragment fragment : fragments) {
						if(fragment.resolveBinding().isEqualTo(returnExpression.resolveBinding()))
							return fragment;
					}
				}
				List<Expression> variableDeclarationExpressions = expressionExtractor.getVariableDeclarationExpressions(typeCheckMethod.getBody());
				for(Expression expression : variableDeclarationExpressions) {
					VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)expression;
					List<VariableDeclarationFragment> fragments = variableDeclarationExpression.fragments();
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
	
	public SimpleName getTypeVariableSimpleName() {
		if(typeField != null) {
			return typeField.getName();
		}
		else if(typeLocalVariable != null) {
			return typeLocalVariable.getName();
		}
		else if(foreignTypeField != null) {
			return foreignTypeField.getName();
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
			if(invoker != null)
				return invoker;
			else
				return typeMethodInvocation.getName();
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
	
	public String getAbstractClassType() {
		String abstractClassType = null;
		if(typeField != null) {
			FieldDeclaration fieldDeclaration = (FieldDeclaration)typeField.getParent();
			Type fieldType = fieldDeclaration.getType();
			if(!fieldType.isPrimitiveType())
				abstractClassType = fieldType.resolveBinding().getQualifiedName();
		}
		else if(typeLocalVariable != null) {
			if(typeLocalVariable instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)typeLocalVariable;
				Type localVariableType = singleVariableDeclaration.getType();
				if(!localVariableType.isPrimitiveType())
					abstractClassType = localVariableType.resolveBinding().getQualifiedName();
			}
			else if(typeLocalVariable instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)typeLocalVariable;
				Type localVariableType = null;
				if(variableDeclarationFragment.getParent() instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclarationFragment.getParent();
					localVariableType = variableDeclarationStatement.getType();
				}
				else if(variableDeclarationFragment.getParent() instanceof VariableDeclarationExpression) {
					VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)variableDeclarationFragment.getParent();
					localVariableType = variableDeclarationExpression.getType();
				}
				else if(variableDeclarationFragment.getParent() instanceof FieldDeclaration) {
					FieldDeclaration fieldDeclaration = (FieldDeclaration)variableDeclarationFragment.getParent();
					localVariableType = fieldDeclaration.getType();
				}
				if(!localVariableType.isPrimitiveType())
					abstractClassType = localVariableType.resolveBinding().getQualifiedName();
			}
		}
		else if(foreignTypeField != null) {
			FieldDeclaration fieldDeclaration = (FieldDeclaration)foreignTypeField.getParent();
			Type fieldType = fieldDeclaration.getType();
			if(!fieldType.isPrimitiveType())
				abstractClassType = fieldType.resolveBinding().getQualifiedName();
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
			if(invoker != null) {
				abstractClassType = invoker.resolveTypeBinding().getQualifiedName();
			}
		}
		if(abstractClassType == null) {
			Block typeCheckMethodBody = typeCheckMethod.getBody();
			List<Statement> statements = typeCheckMethodBody.statements();
			if(statements.size() > 0 && statements.get(0) instanceof SwitchStatement) {
				SwitchStatement switchStatement = (SwitchStatement)statements.get(0);
				List<Statement> statements2 = switchStatement.statements();
				ExpressionExtractor expressionExtractor = new ExpressionExtractor();
				List<ITypeBinding> superclassTypeBindings = new ArrayList<ITypeBinding>();
				for(Statement statement2 : statements2) {
					if(!(statement2 instanceof SwitchCase) && !(statement2 instanceof BreakStatement)) {
						List<Expression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(statement2);
						if(classInstanceCreations.size() == 1) {
							ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)classInstanceCreations.get(0);
							Type classInstanceCreationType = classInstanceCreation.getType();
							ITypeBinding classInstanceCreationTypeBinding = classInstanceCreationType.resolveBinding();
							superclassTypeBindings.add(classInstanceCreationTypeBinding.getSuperclass());
						}
					}
				}
				if(superclassTypeBindings.size() > 1) {
					for(ITypeBinding superclassTypeBinding : superclassTypeBindings) {
						if(superclassTypeBinding.getQualifiedName().equals("java.lang.Object"))
							return null;
					}
					if(equalTypeBindings(superclassTypeBindings)) {
						abstractClassType = superclassTypeBindings.get(0).getQualifiedName();
					}
					else {
						List<ITypeBinding> superclassTypeBindings2 = new ArrayList<ITypeBinding>();
						for(ITypeBinding classTypeBinding : superclassTypeBindings) {
							ITypeBinding superclassTypeBinding = classTypeBinding.getSuperclass();
							if(superclassTypeBinding.getQualifiedName().equals("java.lang.Object"))
								superclassTypeBindings2.add(classTypeBinding);
							else
								superclassTypeBindings2.add(superclassTypeBinding);
						}
						if(equalTypeBindings(superclassTypeBindings2)) {
							abstractClassType = superclassTypeBindings.get(0).getQualifiedName();
						}
					}
				}
			}
		}
		return abstractClassType;
	}
	
	private boolean equalTypeBindings(List<ITypeBinding> typeBindings) {
		ITypeBinding firstTypeBinding = typeBindings.get(0);
		for(int i=1; i<typeBindings.size(); i++) {
			ITypeBinding currentTypeBinding = typeBindings.get(i);
			if(!firstTypeBinding.isEqualTo(currentTypeBinding))
				return false;
		}
		return true;
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
					if(inheritanceTreeMatchingWithStaticTypes != null) {
						subclassNames.add(staticFieldSubclassTypeMap.get(simpleName));
					}
					else if(existingInheritanceTree != null) {
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
						if(exceptionType instanceof UnionType) {
							UnionType unionType = (UnionType)exceptionType;
							List<Type> types = unionType.types();
							for(Type type : types) {
								catchClauseExceptions.add(type.resolveBinding());
							}
						}
						else {
							catchClauseExceptions.add(exceptionType.resolveBinding());
						}
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
	
	public double getAverageNumberOfStatements() {
		if(averageNumberOfStatements == 0) {
			List<ArrayList<Statement>> typeCheckStatements = new ArrayList<ArrayList<Statement>>(getTypeCheckStatements());
			ArrayList<Statement> defaultCaseStatements = getDefaultCaseStatements();
			if(!defaultCaseStatements.isEmpty())
				typeCheckStatements.add(defaultCaseStatements);
			StatementExtractor statementExtractor = new StatementExtractor();
			int numberOfCases = typeCheckStatements.size();
			int totalNumberOfStatements = 0;
			for(ArrayList<Statement> statements : typeCheckStatements) {
				for(Statement statement : statements) {
					totalNumberOfStatements += statementExtractor.getTotalNumberOfStatements(statement);
				}
			}
			averageNumberOfStatements = (double)totalNumberOfStatements/(double)numberOfCases;
		}
		return averageNumberOfStatements;
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

	public int getGroupSizeAtClassLevel() {
		return groupSizeAtClassLevel;
	}

	public void setGroupSizeAtClassLevel(int groupSizeAtClassLevel) {
		this.groupSizeAtClassLevel = groupSizeAtClassLevel;
	}

	public boolean matchingStatesOrSubTypes(TypeCheckElimination other) {
		if(!this.staticFieldMap.isEmpty() && !other.staticFieldMap.isEmpty()) {
			Set<String> originalStaticFields = new LinkedHashSet<String>();
			for(List<SimpleName> staticFields : this.staticFieldMap.values()) {
				for(SimpleName staticField : staticFields)
					originalStaticFields.add(staticField.getIdentifier());
			}
			for(SimpleName staticField : this.additionalStaticFields) {
				originalStaticFields.add(staticField.getIdentifier());
			}
			for(List<SimpleName> staticFields : other.staticFieldMap.values()) {
				for(SimpleName staticField : staticFields) {
					if(originalStaticFields.contains(staticField.getIdentifier()))
						return true;
				}
			}
		}
		else if(!this.subclassTypeMap.isEmpty() && !other.subclassTypeMap.isEmpty()) {
			InheritanceTree tree = null;
			if(this.existingInheritanceTree != null)
				tree = this.existingInheritanceTree;
			if(this.inheritanceTreeMatchingWithStaticTypes != null && tree == null)
				tree = this.inheritanceTreeMatchingWithStaticTypes;
			for(List<Type> subTypes : other.subclassTypeMap.values()) {
				for(Type subType : subTypes) {
					if(tree.contains(subType.resolveBinding().getQualifiedName()))
						return true;
				}
			}
		}
		return false;
	}

	public Integer getUserRate() {
		return userRate;
	}

	public void setUserRate(Integer userRate) {
		this.userRate = userRate;
	}

	public int compareTo(TypeCheckElimination other) {
		int groupSizeAtClassLevel1 = this.getGroupSizeAtClassLevel();
		int groupSizeAtClassLevel2 = other.getGroupSizeAtClassLevel();
		double averageNumberOfStatements1 = this.getAverageNumberOfStatements();
		double averageNumberOfStatements2 = other.getAverageNumberOfStatements();
		String refactoringName1 = this.toString();
		String refactoringName2 = other.toString();
		
		if(groupSizeAtClassLevel1 > groupSizeAtClassLevel2)
			return -1;
		else if(groupSizeAtClassLevel1 < groupSizeAtClassLevel2)
			return 1;
		
		if(averageNumberOfStatements1 > averageNumberOfStatements2)
			return -1;
		else if(averageNumberOfStatements1 < averageNumberOfStatements2)
			return 1;
		
		return refactoringName1.compareTo(refactoringName2);
	}
}
