package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.CreationObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.SuperMethodInvocationObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.ast.decomposition.AbstractExpression;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.VariableDeclaration;

public class PDGExpression {
	private Set<AbstractVariable> declaredVariables;
	private Set<AbstractVariable> definedVariables;
	private Set<AbstractVariable> usedVariables;
	private Set<TypeObject> createdTypes;
	private Set<String> thrownExceptionTypes;
	private MethodCallAnalyzer methodCallAnalyzer;
	
	public PDGExpression(AbstractExpression expression, Set<VariableDeclaration> variableDeclarationsInMethod) {
		this.declaredVariables = new LinkedHashSet<AbstractVariable>();
		this.definedVariables = new LinkedHashSet<AbstractVariable>();
		this.usedVariables = new LinkedHashSet<AbstractVariable>();
		this.createdTypes = new LinkedHashSet<TypeObject>();
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
			createdTypes.add(creation.getType());
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
	}

	private void processArgumentsOfInternalMethodInvocation(MethodInvocationObject methodInvocationObject, AbstractVariable variable) {
		methodCallAnalyzer.processArgumentsOfInternalMethodInvocation(methodInvocationObject, variable);
	}
}
