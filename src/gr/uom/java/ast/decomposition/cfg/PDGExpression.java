package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassInstanceCreationObject;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.ConstructorObject;
import gr.uom.java.ast.CreationObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.SuperMethodInvocationObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.VariableDeclarationObject;
import gr.uom.java.ast.decomposition.AbstractExpression;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class PDGExpression {
	private Set<AbstractVariable> declaredVariables;
	private Set<AbstractVariable> definedVariables;
	private Set<AbstractVariable> usedVariables;
	private Set<CreationObject> createdTypes;
	private Set<String> thrownExceptionTypes;
	private MethodCallAnalyzer methodCallAnalyzer;
	
	public PDGExpression(AbstractExpression expression, Set<VariableDeclarationObject> variableDeclarationsInMethod) {
		this.declaredVariables = new LinkedHashSet<AbstractVariable>();
		this.definedVariables = new LinkedHashSet<AbstractVariable>();
		this.usedVariables = new LinkedHashSet<AbstractVariable>();
		this.createdTypes = new LinkedHashSet<CreationObject>();
		this.thrownExceptionTypes = new LinkedHashSet<String>();
		this.methodCallAnalyzer = new MethodCallAnalyzer(definedVariables, usedVariables, thrownExceptionTypes, variableDeclarationsInMethod);
		determineDefinedAndUsedVariables(expression);
	}

	public Iterator<AbstractVariable> getDeclaredVariableIterator() {
		return declaredVariables.iterator();
	}

	public Iterator<AbstractVariable> getDefinedVariableIterator() {
		return definedVariables.iterator();
	}

	public Iterator<AbstractVariable> getUsedVariableIterator() {
		return usedVariables.iterator();
	}

	public boolean definesLocalVariable(AbstractVariable variable) {
		return definedVariables.contains(variable);
	}

	public boolean usesLocalVariable(AbstractVariable variable) {
		return usedVariables.contains(variable);
	}

	public boolean throwsException() {
		if(!thrownExceptionTypes.isEmpty())
			return true;
		return false;
	}

	private void determineDefinedAndUsedVariables(AbstractExpression expression) {
		List<CreationObject> creations = expression.getCreations();
		for(CreationObject creation : creations) {
			createdTypes.add(creation);
			if(creation instanceof ClassInstanceCreationObject) {
				ClassInstanceCreationObject classInstanceCreation = (ClassInstanceCreationObject)creation;
				Map<PlainVariable, LinkedHashSet<ClassInstanceCreationObject>> variablesAssignedWithClassInstanceCreations = expression.getVariablesAssignedWithClassInstanceCreations();
				PlainVariable variable = null;
				for(PlainVariable key : variablesAssignedWithClassInstanceCreations.keySet()) {
					if(variablesAssignedWithClassInstanceCreations.get(key).contains(classInstanceCreation)/* &&
							(expression.getDefinedFieldsThroughThisReference().contains(key) || expression.getDefinedLocalVariables().contains(key) || expression.getDeclaredLocalVariables().contains(key))*/) {
						variable = key;
						break;
					}
				}
				if(variable != null) {
					processArgumentsOfInternalClassInstanceCreation(classInstanceCreation, variable);
				}
				thrownExceptionTypes.addAll(classInstanceCreation.getThrownExceptions());
			}
		}
		for(PlainVariable variable : expression.getDeclaredLocalVariables()) {
			declaredVariables.add(variable);
			definedVariables.add(variable);
		}
		for(PlainVariable variable : expression.getDefinedLocalVariables()) {
			definedVariables.add(variable);
		}
		for(PlainVariable variable : expression.getUsedLocalVariables()) {
			usedVariables.add(variable);
		}
		Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughLocalVariables = expression.getInvokedMethodsThroughLocalVariables();
		for(AbstractVariable variable : invokedMethodsThroughLocalVariables.keySet()) {
			LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughLocalVariables.get(variable);
			for(MethodInvocationObject methodInvocationObject : methodInvocations) {
				thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
				processArgumentsOfInternalMethodInvocation(methodInvocationObject, variable);
			}
		}
		Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughParameters = expression.getInvokedMethodsThroughParameters();
		for(AbstractVariable variable : invokedMethodsThroughParameters.keySet()) {
			LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughParameters.get(variable);
			for(MethodInvocationObject methodInvocationObject : methodInvocations) {
				thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
				processArgumentsOfInternalMethodInvocation(methodInvocationObject, variable);
			}
		}
		
		for(PlainVariable field : expression.getDefinedFieldsThroughThisReference()) {
			definedVariables.add(field);
		}
		for(PlainVariable field : expression.getUsedFieldsThroughThisReference()) {
			usedVariables.add(field);
		}
		for(AbstractVariable field : expression.getDefinedFieldsThroughFields()) {
			definedVariables.add(field);
		}
		for(AbstractVariable field : expression.getUsedFieldsThroughFields()) {
			usedVariables.add(field);
		}
		for(AbstractVariable field : expression.getDefinedFieldsThroughParameters()) {
			definedVariables.add(field);
		}
		for(AbstractVariable field : expression.getUsedFieldsThroughParameters()) {
			usedVariables.add(field);
		}
		for(AbstractVariable field : expression.getDefinedFieldsThroughLocalVariables()) {
			definedVariables.add(field);
		}
		for(AbstractVariable field : expression.getUsedFieldsThroughLocalVariables()) {
			usedVariables.add(field);
		}
		Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughFields = expression.getInvokedMethodsThroughFields();
		for(AbstractVariable variable : invokedMethodsThroughFields.keySet()) {
			LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughFields.get(variable);
			for(MethodInvocationObject methodInvocationObject : methodInvocations) {
				thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
				processArgumentsOfInternalMethodInvocation(methodInvocationObject, variable);
			}
		}
		for(MethodInvocationObject methodInvocationObject : expression.getInvokedMethodsThroughThisReference()) {
			thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
			processArgumentsOfInternalMethodInvocation(methodInvocationObject, null);
		}
		for(MethodInvocationObject methodInvocationObject : expression.getInvokedStaticMethods()) {
			thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
			processArgumentsOfInternalMethodInvocation(methodInvocationObject, null);
		}
		List<SuperMethodInvocationObject> superMethodInvocations = expression.getSuperMethodInvocations();
		for(SuperMethodInvocationObject superMethodInvocationObject : superMethodInvocations) {
			thrownExceptionTypes.addAll(superMethodInvocationObject.getThrownExceptions());
		}
		List<MethodInvocationObject> methodInvocations = expression.getMethodInvocations();
		for(MethodInvocationObject methodInvocationObject : methodInvocations) {
			thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
		}
	}

	private void processArgumentsOfInternalMethodInvocation(MethodInvocationObject methodInvocationObject, AbstractVariable variable) {
		SystemObject systemObject = ASTReader.getSystemObject();
		MethodInvocation methodInvocation = methodInvocationObject.getMethodInvocation();
		IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
		ClassObject classObject = systemObject.getClassObject(methodInvocationObject.getOriginClassName());
		MethodObject methodObject = null;
		if(classObject != null) {
			methodObject = classObject.getMethod(methodInvocationObject);
		}
		if(classObject == null || methodObject != null) {
			//classObject == null => external method call
			//methodObject != null => the internal method might not exist, in the case of built-in enumeration methods, such as values() and valueOf()
			methodCallAnalyzer.processArgumentsOfInternalMethodInvocation(classObject, methodObject, methodInvocation.arguments(), methodBinding, variable);
		}
	}

	private void processArgumentsOfInternalClassInstanceCreation(ClassInstanceCreationObject classInstanceCreationObject, AbstractVariable variable) {
		SystemObject systemObject = ASTReader.getSystemObject();
		ClassInstanceCreation classInstanceCreation = classInstanceCreationObject.getClassInstanceCreation();
		IMethodBinding methodBinding = classInstanceCreation.resolveConstructorBinding();
		ClassObject classObject = systemObject.getClassObject(classInstanceCreationObject.getType().getClassType());
		ConstructorObject constructorObject = null;
		if(classObject != null) {
			constructorObject = classObject.getConstructor(classInstanceCreationObject);
		}
		if((classObject == null && !methodBinding.getDeclaringClass().isAnonymous()) || constructorObject != null) {
			//classObject == null && !methodBinding.getDeclaringClass().isAnonymous() => external constructor call that is not an anonymous class declaration
			//constructorObject != null => the internal constructor might not exist, in the case the default constructor is called
			methodCallAnalyzer.processArgumentsOfInternalMethodInvocation(classObject, constructorObject, classInstanceCreation.arguments(), methodBinding, variable);
		}
	}

}
