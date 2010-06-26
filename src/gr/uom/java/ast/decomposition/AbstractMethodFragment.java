package gr.uom.java.ast.decomposition;

import gr.uom.java.ast.ArrayCreationObject;
import gr.uom.java.ast.ClassInstanceCreationObject;
import gr.uom.java.ast.CreationObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.SuperFieldInstructionObject;
import gr.uom.java.ast.SuperMethodInvocationObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.util.MethodDeclarationUtility;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public abstract class AbstractMethodFragment {
	private List<MethodInvocationObject> methodInvocationList;
	private List<SuperMethodInvocationObject> superMethodInvocationList;
	private List<FieldInstructionObject> fieldInstructionList;
	private List<SuperFieldInstructionObject> superFieldInstructionList;
	private List<LocalVariableDeclarationObject> localVariableDeclarationList;
	private List<LocalVariableInstructionObject> localVariableInstructionList;
	private List<CreationObject> creationList;
	private Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughFields;
	private Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughParameters;
	private Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughLocalVariables;
	private Set<MethodInvocationObject> invokedMethodsThroughThisReference;
	private Set<MethodInvocationObject> invokedStaticMethods;
	
	private Set<AbstractVariable> definedFieldsThroughFields;
	private Set<AbstractVariable> usedFieldsThroughFields;
	private Set<AbstractVariable> definedFieldsThroughParameters;
	private Set<AbstractVariable> usedFieldsThroughParameters;
	private Set<AbstractVariable> definedFieldsThroughLocalVariables;
	private Set<AbstractVariable> usedFieldsThroughLocalVariables;
	private Set<PlainVariable> definedFieldsThroughThisReference;
	private Set<PlainVariable> usedFieldsThroughThisReference;
	
	private Set<PlainVariable> declaredLocalVariables;
	private Set<PlainVariable> definedLocalVariables;
	private Set<PlainVariable> usedLocalVariables;
	private Map<PlainVariable, LinkedHashSet<MethodInvocationObject>> parametersPassedAsArgumentsInMethodInvocations;

	protected AbstractMethodFragment() {
		this.methodInvocationList = new ArrayList<MethodInvocationObject>();
		this.superMethodInvocationList = new ArrayList<SuperMethodInvocationObject>();
		this.fieldInstructionList = new ArrayList<FieldInstructionObject>();
		this.superFieldInstructionList = new ArrayList<SuperFieldInstructionObject>();
		this.localVariableDeclarationList = new ArrayList<LocalVariableDeclarationObject>();
		this.localVariableInstructionList = new ArrayList<LocalVariableInstructionObject>();
		this.creationList = new ArrayList<CreationObject>();
		this.invokedMethodsThroughFields = new LinkedHashMap<AbstractVariable, LinkedHashSet<MethodInvocationObject>>();
		this.invokedMethodsThroughParameters = new LinkedHashMap<AbstractVariable, LinkedHashSet<MethodInvocationObject>>();
		this.invokedMethodsThroughLocalVariables = new LinkedHashMap<AbstractVariable, LinkedHashSet<MethodInvocationObject>>();
		this.invokedMethodsThroughThisReference = new LinkedHashSet<MethodInvocationObject>();
		this.invokedStaticMethods = new LinkedHashSet<MethodInvocationObject>();
		
		this.definedFieldsThroughFields = new LinkedHashSet<AbstractVariable>();
		this.usedFieldsThroughFields = new LinkedHashSet<AbstractVariable>();
		this.definedFieldsThroughParameters = new LinkedHashSet<AbstractVariable>();
		this.usedFieldsThroughParameters = new LinkedHashSet<AbstractVariable>();
		this.definedFieldsThroughLocalVariables = new LinkedHashSet<AbstractVariable>();
		this.usedFieldsThroughLocalVariables = new LinkedHashSet<AbstractVariable>();
		this.definedFieldsThroughThisReference = new LinkedHashSet<PlainVariable>();
		this.usedFieldsThroughThisReference = new LinkedHashSet<PlainVariable>();
		
		this.declaredLocalVariables = new LinkedHashSet<PlainVariable>();
		this.definedLocalVariables = new LinkedHashSet<PlainVariable>();
		this.usedLocalVariables = new LinkedHashSet<PlainVariable>();
		this.parametersPassedAsArgumentsInMethodInvocations = new LinkedHashMap<PlainVariable, LinkedHashSet<MethodInvocationObject>>();
	}

	protected void processVariables(List<Expression> variableInstructions, List<Expression> assignments,
			List<Expression> postfixExpressions, List<Expression> prefixExpressions) {
		for(Expression variableInstruction : variableInstructions) {
			SimpleName simpleName = (SimpleName)variableInstruction;
			IBinding binding = simpleName.resolveBinding();
			if(binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding)binding;
				if(variableBinding.isField()) {
					if(variableBinding.getDeclaringClass() != null) {
						String originClassName = variableBinding.getDeclaringClass().getQualifiedName();
						String qualifiedName = variableBinding.getType().getQualifiedName();
						TypeObject fieldType = TypeObject.extractTypeObject(qualifiedName);
						String fieldName = variableBinding.getName();
						if(!originClassName.equals("")) {
							if(simpleName.getParent() instanceof SuperFieldAccess) {
								SuperFieldInstructionObject superFieldInstruction = new SuperFieldInstructionObject(originClassName, fieldType, fieldName);
								superFieldInstruction.setSimpleName(simpleName);
								if((variableBinding.getModifiers() & Modifier.STATIC) != 0)
									superFieldInstruction.setStatic(true);
								superFieldInstructionList.add(superFieldInstruction);
							}
							else {
								FieldInstructionObject fieldInstruction = new FieldInstructionObject(originClassName, fieldType, fieldName);
								fieldInstruction.setSimpleName(simpleName);
								if((variableBinding.getModifiers() & Modifier.STATIC) != 0)
									fieldInstruction.setStatic(true);
								fieldInstructionList.add(fieldInstruction);
								Set<Assignment> fieldAssignments = getMatchingAssignments(simpleName, assignments);
								Set<PostfixExpression> fieldPostfixAssignments = getMatchingPostfixAssignments(simpleName, postfixExpressions);
								Set<PrefixExpression> fieldPrefixAssignments = getMatchingPrefixAssignments(simpleName, prefixExpressions);
								AbstractVariable variable = MethodDeclarationUtility.createVariable(simpleName, null);
								if(!fieldAssignments.isEmpty()) {
									handleDefinedField(variable);
									for(Assignment assignment : fieldAssignments) {
										Assignment.Operator operator = assignment.getOperator();
										if(!operator.equals(Assignment.Operator.ASSIGN))
											handleUsedField(variable);
									}
								}
								if(!fieldPostfixAssignments.isEmpty()) {
									handleDefinedField(variable);
									handleUsedField(variable);
								}
								if(!fieldPrefixAssignments.isEmpty()) {
									handleDefinedField(variable);
									handleUsedField(variable);
								}
								if(fieldAssignments.isEmpty() && fieldPostfixAssignments.isEmpty() && fieldPrefixAssignments.isEmpty()) {
									handleUsedField(variable);
								}
							}
						}
					}
				}
				else {
					if(variableBinding.getDeclaringClass() == null) {
						String variableBindingKey = variableBinding.getKey();
						String variableName = variableBinding.getName();
						String variableType = variableBinding.getType().getQualifiedName();
						TypeObject localVariableType = TypeObject.extractTypeObject(variableType);
						boolean isField = variableBinding.isField();
						boolean isParameter = variableBinding.isParameter();
						PlainVariable variable = new PlainVariable(variableBindingKey, variableName, variableType, isField, isParameter);
						if(simpleName.isDeclaration()) {
							LocalVariableDeclarationObject localVariable = new LocalVariableDeclarationObject(localVariableType, variableName);
							VariableDeclaration variableDeclaration = (VariableDeclaration)simpleName.getParent();
							localVariable.setVariableDeclaration(variableDeclaration);
							localVariableDeclarationList.add(localVariable);
							declaredLocalVariables.add(variable);
						}
						else {
							LocalVariableInstructionObject localVariable = new LocalVariableInstructionObject(localVariableType, variableName);
							localVariable.setSimpleName(simpleName);
							localVariableInstructionList.add(localVariable);
							Set<Assignment> localVariableAssignments = getMatchingAssignments(simpleName, assignments);
							Set<PostfixExpression> localVariablePostfixAssignments = getMatchingPostfixAssignments(simpleName, postfixExpressions);
							Set<PrefixExpression> localVariablePrefixAssignments = getMatchingPrefixAssignments(simpleName, prefixExpressions);
							if(!localVariableAssignments.isEmpty()) {
								definedLocalVariables.add(variable);
								for(Assignment assignment : localVariableAssignments) {
									Assignment.Operator operator = assignment.getOperator();
									if(!operator.equals(Assignment.Operator.ASSIGN))
										usedLocalVariables.add(variable);
								}
							}
							if(!localVariablePostfixAssignments.isEmpty()) {
								definedLocalVariables.add(variable);
								usedLocalVariables.add(variable);
							}
							if(!localVariablePrefixAssignments.isEmpty()) {
								definedLocalVariables.add(variable);
								usedLocalVariables.add(variable);
							}
							if(localVariableAssignments.isEmpty() && localVariablePostfixAssignments.isEmpty() && localVariablePrefixAssignments.isEmpty()) {
								usedLocalVariables.add(variable);
							}
						}
					}
				}
			}
		}
	}

	protected void processMethodInvocations(List<Expression> methodInvocations) {
		for(Expression expression : methodInvocations) {
			if(expression instanceof MethodInvocation) {
				MethodInvocation methodInvocation = (MethodInvocation)expression;
				IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
				String originClassName = methodBinding.getDeclaringClass().getQualifiedName();
				String methodInvocationName = methodBinding.getName();
				String qualifiedName = methodBinding.getReturnType().getQualifiedName();
				TypeObject returnType = TypeObject.extractTypeObject(qualifiedName);
				MethodInvocationObject methodInvocationObject = new MethodInvocationObject(originClassName, methodInvocationName, returnType);
				methodInvocationObject.setMethodInvocation(methodInvocation);
				ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
				for(ITypeBinding parameterType : parameterTypes) {
					String qualifiedParameterName = parameterType.getQualifiedName();
					TypeObject typeObject = TypeObject.extractTypeObject(qualifiedParameterName);
					methodInvocationObject.addParameter(typeObject);
				}
				if((methodBinding.getModifiers() & Modifier.STATIC) != 0)
					methodInvocationObject.setStatic(true);
				methodInvocationList.add(methodInvocationObject);
				AbstractVariable invoker = MethodDeclarationUtility.processMethodInvocationExpression(methodInvocation.getExpression());
				if(invoker != null) {
					PlainVariable initialVariable = invoker.getInitialVariable();
					if(initialVariable.isField())
						addInvokedMethodThroughField(invoker, methodInvocationObject);
					else if(initialVariable.isParameter())
						addInvokedMethodThroughParameter(invoker, methodInvocationObject);
					else
						addInvokedMethodThroughLocalVariable(invoker, methodInvocationObject);
				}
				else {
					if(methodInvocationObject.isStatic())
						addStaticallyInvokedMethod(methodInvocationObject);
					else
						addInvokedMethodThroughThisReference(methodInvocationObject);
				}
				List<Expression> arguments = methodInvocation.arguments();
				for(Expression argument : arguments) {
					if(argument instanceof SimpleName) {
						SimpleName argumentName = (SimpleName)argument;
						IBinding binding = argumentName.resolveBinding();
						if(binding.getKind() == IBinding.VARIABLE) {
							IVariableBinding variableBinding = (IVariableBinding)binding;
							if(variableBinding.isParameter()) {
								String variableBindingKey = variableBinding.getKey();
								String variableName = variableBinding.getName();
								String variableType = variableBinding.getType().getQualifiedName();
								boolean isField = variableBinding.isField();
								boolean isParameter = variableBinding.isParameter();
								PlainVariable variable = new PlainVariable(variableBindingKey, variableName, variableType, isField, isParameter);
								addParameterPassedAsArgumentInMethodInvocation(variable, methodInvocationObject);
							}
						}
					}
				}
			}
			else if(expression instanceof SuperMethodInvocation) {
				IMethodBinding methodBinding = ((SuperMethodInvocation)expression).resolveMethodBinding();
				String originClassName = methodBinding.getDeclaringClass().getQualifiedName();
				String methodInvocationName = methodBinding.getName();
				String qualifiedName = methodBinding.getReturnType().getQualifiedName();
				TypeObject returnType = TypeObject.extractTypeObject(qualifiedName);
				SuperMethodInvocationObject superMethodInvocationObject = new SuperMethodInvocationObject(originClassName, methodInvocationName, returnType);
				superMethodInvocationObject.setSuperMethodInvocation((SuperMethodInvocation)expression);
				ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
				for(ITypeBinding parameterType : parameterTypes) {
					String qualifiedParameterName = parameterType.getQualifiedName();
					TypeObject typeObject = TypeObject.extractTypeObject(qualifiedParameterName);
					superMethodInvocationObject.addParameter(typeObject);
				}
				superMethodInvocationList.add(superMethodInvocationObject);
			}
		}
	}

	protected void processClassInstanceCreations(List<Expression> classInctanceCreations) {
		for(Expression classInstanceCreationExpression : classInctanceCreations) {
			ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)classInstanceCreationExpression;
			IMethodBinding constructorBinding = classInstanceCreation.resolveConstructorBinding();
			Type type = classInstanceCreation.getType();
			ITypeBinding typeBinding = type.resolveBinding();
			String qualifiedTypeName = typeBinding.getQualifiedName();
			TypeObject typeObject = TypeObject.extractTypeObject(qualifiedTypeName);
			ClassInstanceCreationObject creationObject = new ClassInstanceCreationObject(typeObject);
			creationObject.setClassInstanceCreation(classInstanceCreation);
			ITypeBinding[] parameterTypes = constructorBinding.getParameterTypes();
			for(ITypeBinding parameterType : parameterTypes) {
				String qualifiedParameterName = parameterType.getQualifiedName();
				TypeObject parameterTypeObject = TypeObject.extractTypeObject(qualifiedParameterName);
				creationObject.addParameter(parameterTypeObject);
			}
			creationList.add(creationObject);
		}
	}

	protected void processArrayCreations(List<Expression> arrayCreations) {
		for(Expression arrayCreationExpression : arrayCreations) {
			ArrayCreation arrayCreation = (ArrayCreation)arrayCreationExpression;
			Type type = arrayCreation.getType();
			ITypeBinding typeBinding = type.resolveBinding();
			String qualifiedTypeName = typeBinding.getQualifiedName();
			TypeObject typeObject = TypeObject.extractTypeObject(qualifiedTypeName);
			ArrayCreationObject creationObject = new ArrayCreationObject(typeObject);
			creationObject.setArrayCreation(arrayCreation);
			creationList.add(creationObject);
		}
	}

	private void addInvokedMethodThroughField(AbstractVariable field, MethodInvocationObject methodInvocation) {
		if(invokedMethodsThroughFields.containsKey(field)) {
			LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughFields.get(field);
			methodInvocations.add(methodInvocation);
		}
		else {
			LinkedHashSet<MethodInvocationObject> methodInvocations = new LinkedHashSet<MethodInvocationObject>();
			methodInvocations.add(methodInvocation);
			invokedMethodsThroughFields.put(field, methodInvocations);
		}
	}

	private void addInvokedMethodThroughParameter(AbstractVariable parameter, MethodInvocationObject methodInvocation) {
		if(invokedMethodsThroughParameters.containsKey(parameter)) {
			LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughParameters.get(parameter);
			methodInvocations.add(methodInvocation);
		}
		else {
			LinkedHashSet<MethodInvocationObject> methodInvocations = new LinkedHashSet<MethodInvocationObject>();
			methodInvocations.add(methodInvocation);
			invokedMethodsThroughParameters.put(parameter, methodInvocations);
		}
	}

	private void addInvokedMethodThroughLocalVariable(AbstractVariable localVariable, MethodInvocationObject methodInvocation) {
		if(invokedMethodsThroughLocalVariables.containsKey(localVariable)) {
			LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughLocalVariables.get(localVariable);
			methodInvocations.add(methodInvocation);
		}
		else {
			LinkedHashSet<MethodInvocationObject> methodInvocations = new LinkedHashSet<MethodInvocationObject>();
			methodInvocations.add(methodInvocation);
			invokedMethodsThroughLocalVariables.put(localVariable, methodInvocations);
		}
	}

	private void addInvokedMethodThroughThisReference(MethodInvocationObject methodInvocation) {
		invokedMethodsThroughThisReference.add(methodInvocation);
	}

	private void addStaticallyInvokedMethod(MethodInvocationObject methodInvocation) {
		invokedStaticMethods.add(methodInvocation);
	}

	private void addParameterPassedAsArgumentInMethodInvocation(PlainVariable parameter, MethodInvocationObject methodInvocation) {
		if(parametersPassedAsArgumentsInMethodInvocations.containsKey(parameter)) {
			LinkedHashSet<MethodInvocationObject> methodInvocations = parametersPassedAsArgumentsInMethodInvocations.get(parameter);
			methodInvocations.add(methodInvocation);
		}
		else {
			LinkedHashSet<MethodInvocationObject> methodInvocations = new LinkedHashSet<MethodInvocationObject>();
			methodInvocations.add(methodInvocation);
			parametersPassedAsArgumentsInMethodInvocations.put(parameter, methodInvocations);
		}
	}

	private Set<Assignment> getMatchingAssignments(SimpleName simpleName, List<Expression> assignments) {
		Set<Assignment> matchingAssignments = new LinkedHashSet<Assignment>();
		for(Expression expression : assignments) {
			Assignment assignment = (Assignment)expression;
			Expression leftHandSide = assignment.getLeftHandSide();
			SimpleName leftHandSideName = MethodDeclarationUtility.getRightMostSimpleName(leftHandSide);
			if(leftHandSideName != null && leftHandSideName.equals(simpleName)) {
				matchingAssignments.add(assignment);
			}
		}
		return matchingAssignments;
	}

	private Set<PostfixExpression> getMatchingPostfixAssignments(SimpleName simpleName, List<Expression> postfixExpressions) {
		Set<PostfixExpression> matchingPostfixAssignments = new LinkedHashSet<PostfixExpression>();
		for(Expression expression : postfixExpressions) {
			PostfixExpression postfixExpression = (PostfixExpression)expression;
			Expression operand = postfixExpression.getOperand();
			SimpleName operandName = MethodDeclarationUtility.getRightMostSimpleName(operand);
			if(operandName != null && operandName.equals(simpleName)) {
				matchingPostfixAssignments.add(postfixExpression);
			}
		}
		return matchingPostfixAssignments;
	}

	private Set<PrefixExpression> getMatchingPrefixAssignments(SimpleName simpleName, List<Expression> prefixExpressions) {
		Set<PrefixExpression> matchingPrefixAssignments = new LinkedHashSet<PrefixExpression>();
		for(Expression expression : prefixExpressions) {
			PrefixExpression prefixExpression = (PrefixExpression)expression;
			Expression operand = prefixExpression.getOperand();
			PrefixExpression.Operator operator = prefixExpression.getOperator();
			SimpleName operandName = MethodDeclarationUtility.getRightMostSimpleName(operand);
			if(operandName != null && operandName.equals(simpleName) &&
					(operator.equals(PrefixExpression.Operator.INCREMENT) ||
					operator.equals(PrefixExpression.Operator.DECREMENT))) {
				matchingPrefixAssignments.add(prefixExpression);
			}
		}
		return matchingPrefixAssignments;
	}

	private void handleDefinedField(AbstractVariable variable) {
		if(variable != null) {
			PlainVariable initialVariable = variable.getInitialVariable();
			if(variable instanceof PlainVariable) {
				definedFieldsThroughThisReference.add((PlainVariable)variable);
			}
			else {
				if(initialVariable.isField())
					definedFieldsThroughFields.add(variable);
				else if(initialVariable.isParameter())
					definedFieldsThroughParameters.add(variable);
				else
					definedFieldsThroughLocalVariables.add(variable);
			}
		}
	}

	private void handleUsedField(AbstractVariable variable) {
		if(variable != null) {
			PlainVariable initialVariable = variable.getInitialVariable();
			if(variable instanceof PlainVariable) {
				usedFieldsThroughThisReference.add((PlainVariable)variable);
			}
			else {
				if(initialVariable.isField())
					usedFieldsThroughFields.add(variable);
				else if(initialVariable.isParameter())
					usedFieldsThroughParameters.add(variable);
				else
					usedFieldsThroughLocalVariables.add(variable);
			}
		}
	}

	public List<FieldInstructionObject> getFieldInstructions() {
		return fieldInstructionList;
	}

	public List<SuperFieldInstructionObject> getSuperFieldInstructions() {
		return superFieldInstructionList;
	}

	public List<LocalVariableDeclarationObject> getLocalVariableDeclarations() {
		return localVariableDeclarationList;
	}

	public List<LocalVariableInstructionObject> getLocalVariableInstructions() {
		return localVariableInstructionList;
	}

	public List<MethodInvocationObject> getMethodInvocations() {
		return methodInvocationList;
	}

	public List<SuperMethodInvocationObject> getSuperMethodInvocations() {
		return superMethodInvocationList;
	}

	public List<CreationObject> getCreations() {
		return creationList;
	}

	public boolean containsMethodInvocation(MethodInvocationObject methodInvocation) {
		return methodInvocationList.contains(methodInvocation);
	}

	public boolean containsSuperMethodInvocation(SuperMethodInvocationObject superMethodInvocation) {
		return superMethodInvocationList.contains(superMethodInvocation);
	}

	public boolean containsLocalVariableDeclaration(LocalVariableDeclarationObject lvdo) {
		return localVariableDeclarationList.contains(lvdo);
	}

	public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughFields() {
		return invokedMethodsThroughFields;
	}

	public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughParameters() {
		return invokedMethodsThroughParameters;
	}

	public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughLocalVariables() {
		return invokedMethodsThroughLocalVariables;
	}

	public Set<MethodInvocationObject> getInvokedMethodsThroughThisReference() {
		return invokedMethodsThroughThisReference;
	}

	public Set<MethodInvocationObject> getInvokedStaticMethods() {
		return invokedStaticMethods;
	}

	public Set<AbstractVariable> getDefinedFieldsThroughFields() {
		return definedFieldsThroughFields;
	}

	public Set<AbstractVariable> getUsedFieldsThroughFields() {
		return usedFieldsThroughFields;
	}

	public Set<AbstractVariable> getDefinedFieldsThroughParameters() {
		return definedFieldsThroughParameters;
	}

	public Set<AbstractVariable> getUsedFieldsThroughParameters() {
		return usedFieldsThroughParameters;
	}

	public Set<AbstractVariable> getDefinedFieldsThroughLocalVariables() {
		return definedFieldsThroughLocalVariables;
	}

	public Set<AbstractVariable> getUsedFieldsThroughLocalVariables() {
		return usedFieldsThroughLocalVariables;
	}

	public Set<PlainVariable> getDefinedFieldsThroughThisReference() {
		return definedFieldsThroughThisReference;
	}

	public Set<PlainVariable> getUsedFieldsThroughThisReference() {
		return usedFieldsThroughThisReference;
	}

	public Set<PlainVariable> getDeclaredLocalVariables() {
		return declaredLocalVariables;
	}

	public Set<PlainVariable> getDefinedLocalVariables() {
		return definedLocalVariables;
	}

	public Set<PlainVariable> getUsedLocalVariables() {
		return usedLocalVariables;
	}

	public Map<PlainVariable, LinkedHashSet<MethodInvocationObject>> getParametersPassedAsArgumentsInMethodInvocations() {
		return parametersPassedAsArgumentsInMethodInvocations;
	}
}
