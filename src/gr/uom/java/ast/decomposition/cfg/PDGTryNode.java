package gr.uom.java.ast.decomposition.cfg;

import gr.uom.java.ast.CreationObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.SuperMethodInvocationObject;
import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.TryStatementObject;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.VariableDeclaration;

public class PDGTryNode extends PDGNode {
	
	public PDGTryNode(CFGTryNode cfgTryNode, Set<VariableDeclaration> variableDeclarationsInMethod,
			Set<VariableDeclaration> fieldsAccessedInMethod) {
		super(cfgTryNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
		determineDefinedAndUsedVariables();
	}

	private void determineDefinedAndUsedVariables() {
		CFGNode cfgNode = getCFGNode();
		if(cfgNode.getStatement() instanceof TryStatementObject) {
			TryStatementObject tryStatement = (TryStatementObject)cfgNode.getStatement();
			List<AbstractExpression> expressions = tryStatement.getExpressions();
			for(AbstractExpression expression : expressions) {
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
		}
	}
}
