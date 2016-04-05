package gr.uom.java.ast.decomposition.cfg;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gr.uom.java.ast.ClassInstanceCreationObject;
import gr.uom.java.ast.CreationObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.SuperMethodInvocationObject;
import gr.uom.java.ast.VariableDeclarationObject;
import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.CompositeStatementObject;

public class PDGControlPredicateNode extends PDGNode {
	
	public PDGControlPredicateNode(CFGNode cfgNode, Set<VariableDeclarationObject> variableDeclarationsInMethod,
			Set<FieldObject> fieldsAccessedInMethod) {
		super(cfgNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
		determineDefinedAndUsedVariables();
	}

	private void determineDefinedAndUsedVariables() {
		CFGNode cfgNode = getCFGNode();
		if(cfgNode.getStatement() instanceof CompositeStatementObject) {
			CompositeStatementObject composite = (CompositeStatementObject)cfgNode.getStatement();
			List<AbstractExpression> expressions = composite.getExpressions();
			for(AbstractExpression expression : expressions) {
				List<CreationObject> creations = expression.getCreations();
				for(CreationObject creation : creations) {
					createdTypes.add(creation);
					if(creation instanceof ClassInstanceCreationObject) {
						ClassInstanceCreationObject classInstanceCreation = (ClassInstanceCreationObject)creation;
						Map<PlainVariable, LinkedHashSet<ClassInstanceCreationObject>> variablesAssignedWithClassInstanceCreations = expression.getVariablesAssignedWithClassInstanceCreations();
						PlainVariable variable = null;
						for(PlainVariable key : variablesAssignedWithClassInstanceCreations.keySet()) {
							if(variablesAssignedWithClassInstanceCreations.get(key).contains(classInstanceCreation) &&
									(expression.getDefinedFieldsThroughThisReference().contains(key) || expression.getDefinedLocalVariables().contains(key) || expression.getDeclaredLocalVariables().contains(key))) {
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
		}
	}
}
